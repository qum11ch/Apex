package com.example.f1app;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.functions.FirebaseFunctions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Single source of award collections.
 *
 * Reads go memory, then disk, then the server. A stored collection is never
 * deleted just because it is old or because a request failed: "stale" only
 * means "check the server again". All state below is touched on the main
 * thread; disk work runs on {@link awardsCacheStore}'s thread.
 */
final class awardsRepository {

    static final class Snapshot {

        final List<Map<String, Object>> badges;
        final long savedAt;
        final boolean fresh;

        Snapshot(List<Map<String, Object>> badges, long savedAt, boolean fresh) {
            this.badges = badges;
            this.savedAt = savedAt;
            this.fresh = fresh;
        }
    }

    interface Callback {
        void accept(List<Map<String, Object>> badges, Exception error);
    }

    interface SnapshotCallback {
        void accept(Snapshot snapshot);
    }

    private static final class Stored {

        final List<Map<String, Object>> badges;
        final long savedAt;

        Stored(List<Map<String, Object>> badges, long savedAt) {
            this.badges = badges;
            this.savedAt = savedAt;
        }
    }

    private static final String TAG = "AwardsRepository";
    private static final long FRESH_MS = 5L * 60L * 1000L;
    private static final long BATCH_DELAY_MS = 35L;
    private static final int BATCH_SIZE = 20;
    private static final int MAX_MEMORY = 200;

    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    private static final Map<String, Stored> MEMORY =
            new LinkedHashMap<String, Stored>(16, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, Stored> eldest) {
                    return size() > MAX_MEMORY;
                }
            };

    private static final Map<String, Long> STALE_AT = new HashMap<>();
    private static final Map<String, Integer> VERSIONS = new HashMap<>();
    private static final Map<String, List<Callback>> WAITING = new LinkedHashMap<>();

    private static boolean scheduled;
    private static boolean accountKnown;
    private static String account;
    private static int epoch;
    private static long staleBefore;

    private awardsRepository() {
    }

    // ---------------------------------------------------------------- account

    /**
     * Returns the signed-in account. When it differs from the previous one,
     * memory is reset, waiting callers get an error and the disk file is rebound.
     */
    private static String currentAccount() {
        String current = FirebaseAuth.getInstance().getUid();

        if (accountKnown && Objects.equals(account, current)) {
            return current;
        }

        accountKnown = true;
        account = current;
        epoch++;

        MEMORY.clear();
        STALE_AT.clear();
        staleBefore = 0L;

        List<List<Callback>> orphaned = new ArrayList<>(WAITING.values());
        WAITING.clear();

        final String bound = current;
        awardsCacheStore.execute(() -> awardsCacheStore.bindAccount(bound));

        for (List<Callback> callbacks : orphaned) {
            for (Callback callback : callbacks) {
                callback.accept(null, new IllegalStateException("Account changed"));
            }
        }

        return current;
    }

    // -------------------------------------------------------------- freshness

    private static boolean isFresh(String owner, long savedAt) {
        long age = System.currentTimeMillis() - savedAt;

        if (age < 0L || age >= FRESH_MS) {
            return false;
        }

        Long ownerStale = STALE_AT.get(owner);
        long barrier = Math.max(staleBefore, ownerStale == null ? 0L : ownerStale);

        return savedAt > barrier;
    }

    private static long nextSavedAt(String owner) {
        Long ownerStale = STALE_AT.get(owner);
        long barrier = Math.max(staleBefore, ownerStale == null ? 0L : ownerStale);

        return Math.max(System.currentTimeMillis(), barrier + 1L);
    }

    private static int version(String owner) {
        Integer value = VERSIONS.get(owner);
        return value == null ? 0 : value;
    }

    /** Marks one owner as needing a server check. The stored collection is kept. */
    static void invalidate(String owner) {
        currentAccount();
        STALE_AT.put(owner, System.currentTimeMillis());
        VERSIONS.put(owner, version(owner) + 1);
    }

    static void invalidateAll() {
        currentAccount();
        staleBefore = System.currentTimeMillis();
    }

    // ------------------------------------------------------------------- read

    /**
     * Delivers the best collection known without touching the network, or null.
     * Called synchronously for memory hits, asynchronously for disk hits.
     */
    static void peek(final String owner, final SnapshotCallback callback) {
        final String actor = currentAccount();

        if (actor == null || owner == null || owner.isEmpty()) {
            callback.accept(null);
            return;
        }

        Stored stored = MEMORY.get(owner);

        if (stored != null) {
            callback.accept(snapshot(owner, stored));
            return;
        }

        final int requestEpoch = epoch;

        awardsCacheStore.execute(() -> {
            final awardsCacheStore.AwardsRecord record =
                    awardsCacheStore.readAwards(actor, owner);

            MAIN.post(() -> {
                if (requestEpoch != epoch || record == null) {
                    callback.accept(null);
                    return;
                }

                Stored existing = MEMORY.get(owner);

                if (existing == null) {
                    existing = new Stored(record.badges, record.savedAt);
                    MEMORY.put(owner, existing);
                }

                callback.accept(snapshot(owner, existing));
            });
        });
    }

    private static Snapshot snapshot(String owner, Stored stored) {
        return new Snapshot(stored.badges, stored.savedAt, isFresh(owner, stored.savedAt));
    }

    /** Delivers a fresh collection: from memory or disk when still fresh, else from the server. */
    static void load(final String owner, final Callback callback) {
        String actor = currentAccount();

        if (actor == null || owner == null || owner.isEmpty()) {
            callback.accept(null, new IllegalStateException("Awards unavailable: no account"));
            return;
        }

        Stored stored = MEMORY.get(owner);

        if (stored != null) {
            if (isFresh(owner, stored.savedAt)) {
                callback.accept(stored.badges, null);
            } else {
                enqueue(owner, callback);
            }

            return;
        }

        peek(owner, snapshot -> {
            if (snapshot != null && snapshot.fresh) {
                callback.accept(snapshot.badges, null);
            } else {
                enqueue(owner, callback);
            }
        });
    }

    // ---------------------------------------------------------------- network

    private static void enqueue(String owner, Callback callback) {
        if (currentAccount() == null) {
            callback.accept(null, new IllegalStateException("Awards unavailable: no account"));
            return;
        }

        List<Callback> callbacks = WAITING.get(owner);

        if (callbacks == null) {
            callbacks = new ArrayList<>();
            WAITING.put(owner, callbacks);
        }

        callbacks.add(callback);

        if (!scheduled) {
            scheduled = true;
            MAIN.postDelayed(awardsRepository::flush, BATCH_DELAY_MS);
        }
    }

    private static void flush() {
        scheduled = false;

        final String requestAccount = currentAccount();
        final int requestEpoch = epoch;
        List<String> all = new ArrayList<>(WAITING.keySet());

        for (int offset = 0; offset < all.size(); offset += BATCH_SIZE) {
            final List<String> uids = new ArrayList<>(
                    all.subList(offset, Math.min(all.size(), offset + BATCH_SIZE))
            );
            final Map<String, List<Callback>> calls = new HashMap<>();
            final Map<String, Integer> versions = new HashMap<>();

            for (String uid : uids) {
                calls.put(uid, WAITING.remove(uid));
                versions.put(uid, version(uid));
            }

            if (requestAccount == null) {
                fail(calls, new IllegalStateException("Awards unavailable: no account"));
                continue;
            }

            Map<String, Object> request = new HashMap<>();
            request.put("uids", uids);

            FirebaseFunctions.getInstance("us-central1")
                    .getHttpsCallable("getPublicAwards")
                    .call(request)
                    .addOnSuccessListener(result -> handleResult(
                            result.getData(),
                            uids,
                            calls,
                            versions,
                            requestAccount,
                            requestEpoch
                    ))
                    .addOnFailureListener(error -> fail(calls, error));
        }
    }

    private static void handleResult(
            Object raw,
            List<String> uids,
            Map<String, List<Callback>> calls,
            Map<String, Integer> versions,
            String requestAccount,
            int requestEpoch
    ) {
        if (requestEpoch != epoch) {
            fail(calls, new IllegalStateException("Account changed"));
            return;
        }

        try {
            Map<String, List<Map<String, Object>>> parsed = parse(raw);

            for (String uid : uids) {
                List<Callback> callbacks = calls.remove(uid);

                if (callbacks == null) {
                    continue;
                }

                if (versions.get(uid) != version(uid)) {
                    // Invalidated while the request was in flight: ask again.
                    for (Callback callback : callbacks) {
                        load(uid, callback);
                    }

                    continue;
                }

                List<Map<String, Object>> received = parsed.get(uid);

                if (received == null) {
                    for (Callback callback : callbacks) {
                        callback.accept(null, new IllegalStateException("Awards unavailable"));
                    }

                    continue;
                }

                List<Map<String, Object>> badges = awardsCacheStore.canonicalBadges(received);
                remember(requestAccount, uid, badges);

                for (Callback callback : callbacks) {
                    callback.accept(badges, null);
                }
            }
        } catch (RuntimeException error) {
            Log.w(TAG, "Invalid awards response", error);
            fail(calls, error);
        }
    }

    private static Map<String, List<Map<String, Object>>> parse(Object raw) {
        if (!(raw instanceof Map)) {
            throw new IllegalStateException("Invalid awards response");
        }

        Object profiles = ((Map<?, ?>) raw).get("profiles");

        if (!(profiles instanceof List)) {
            throw new IllegalStateException("Missing profiles");
        }

        Map<String, List<Map<String, Object>>> parsed = new HashMap<>();

        for (Object item : (List<?>) profiles) {
            if (!(item instanceof Map)) {
                throw new IllegalStateException("Invalid profile");
            }

            Map<?, ?> profile = (Map<?, ?>) item;

            if (!(profile.get("uid") instanceof String)) {
                throw new IllegalStateException("Missing UID");
            }

            if (!Boolean.TRUE.equals(profile.get("available"))) {
                continue;
            }

            Object values = profile.get("badges");

            if (!(values instanceof List)) {
                throw new IllegalStateException("Missing badge list");
            }

            List<Map<String, Object>> list = new ArrayList<>();

            for (Object value : (List<?>) values) {
                if (!(value instanceof Map)) {
                    throw new IllegalStateException("Invalid badge");
                }

                Map<String, Object> badge = new HashMap<>();

                for (Map.Entry<?, ?> field : ((Map<?, ?>) value).entrySet()) {
                    if (field.getKey() instanceof String) {
                        badge.put((String) field.getKey(), field.getValue());
                    }
                }

                list.add(badge);
            }

            parsed.put((String) profile.get("uid"), list);
        }

        return parsed;
    }

    private static void remember(
            final String requestAccount,
            final String owner,
            final List<Map<String, Object>> badges
    ) {
        final long savedAt = nextSavedAt(owner);
        MEMORY.put(owner, new Stored(badges, savedAt));

        awardsCacheStore.execute(() ->
                awardsCacheStore.writeAwards(requestAccount, owner, badges, savedAt));
    }

    /** A failed request never touches stored data. */
    private static void fail(Map<String, List<Callback>> calls, Exception error) {
        for (List<Callback> callbacks : calls.values()) {
            if (callbacks == null) {
                continue;
            }

            for (Callback callback : callbacks) {
                callback.accept(null, error);
            }
        }
    }
}
