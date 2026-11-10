package com.example.f1app;

import android.os.Handler;
import android.os.Looper;

import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Single source of "which award this user shows next to their name".
 *
 * Network access still goes through {@link displayedAwardApi}; that class
 * reports every successful read and save here via {@link #remember}. All state
 * is main-thread only; disk work runs on {@link awardsCacheStore}'s thread.
 */
final class displayedAwardRepository {

    static final class Cached {

        final displayedAwardApi.Selection selection;
        final long savedAt;
        final boolean fresh;

        Cached(displayedAwardApi.Selection selection, long savedAt, boolean fresh) {
            this.selection = selection;
            this.savedAt = savedAt;
            this.fresh = fresh;
        }
    }

    interface PeekCallback {
        void accept(Cached cached);
    }

    private static final class Stored {

        final displayedAwardApi.Selection selection;
        final long savedAt;

        Stored(displayedAwardApi.Selection selection, long savedAt) {
            this.selection = selection;
            this.savedAt = savedAt;
        }
    }

    private static final long FRESH_MS = 2L * 60L * 1000L;
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
    private static final Map<String, List<displayedAwardApi.Callback>> PENDING =
            new HashMap<>();

    private static boolean accountKnown;
    private static String account;
    private static int epoch;
    private static long staleBefore;

    private displayedAwardRepository() {
    }

    // ---------------------------------------------------------------- account

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

        List<List<displayedAwardApi.Callback>> orphaned =
                new ArrayList<>(PENDING.values());
        PENDING.clear();

        final String bound = current;
        awardsCacheStore.execute(() -> awardsCacheStore.bindAccount(bound));

        for (List<displayedAwardApi.Callback> callbacks : orphaned) {
            for (displayedAwardApi.Callback callback : callbacks) {
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

    /** The stored selection is kept; the next load will ask the server. */
    static void markStale(String owner) {
        currentAccount();
        STALE_AT.put(owner, System.currentTimeMillis());
    }

    static void markAllStale() {
        currentAccount();
        staleBefore = System.currentTimeMillis();
    }

    // ------------------------------------------------------------------- read

    static void peek(final String owner, final PeekCallback callback) {
        final String actor = currentAccount();

        if (actor == null || owner == null || owner.isEmpty()) {
            callback.accept(null);
            return;
        }

        Stored stored = MEMORY.get(owner);

        if (stored != null) {
            callback.accept(cached(owner, stored));
            return;
        }

        final int requestEpoch = epoch;

        awardsCacheStore.execute(() -> {
            final awardsCacheStore.SelectionRecord record =
                    awardsCacheStore.readSelection(actor, owner);

            MAIN.post(() -> {
                if (requestEpoch != epoch || !isUsable(record)) {
                    callback.accept(null);
                    return;
                }

                Stored existing = MEMORY.get(owner);

                if (existing == null) {
                    existing = new Stored(
                            new displayedAwardApi.Selection(record.id, record.award),
                            record.savedAt
                    );
                    MEMORY.put(owner, existing);
                }

                callback.accept(cached(owner, existing));
            });
        });
    }

    private static boolean isUsable(awardsCacheStore.SelectionRecord record) {
        if (record == null) {
            return false;
        }

        if (record.id == null) {
            return record.award == null;
        }

        return record.award != null && awardCatalogue.find(record.id) != null;
    }

    private static Cached cached(String owner, Stored stored) {
        return new Cached(stored.selection, stored.savedAt, isFresh(owner, stored.savedAt));
    }

    /** Delivers the selection: stored one while fresh, otherwise from the server. */
    static void load(String owner, boolean force, displayedAwardApi.Callback callback) {
        if (force) {
            markStale(owner);
        }

        resolve(owner, null, false, callback);
    }

    /**
     * Same as {@link #load}, but a fresh stored selection is only accepted when
     * its id equals the id the server just reported in the user's profile node.
     */
    static void loadMatching(
            String owner,
            String remoteId,
            displayedAwardApi.Callback callback
    ) {
        resolve(owner, remoteId, true, callback);
    }

    private static void resolve(
            final String owner,
            final String remoteId,
            final boolean matchRemote,
            final displayedAwardApi.Callback callback
    ) {
        if (currentAccount() == null || owner == null || owner.isEmpty()) {
            callback.accept(null, new IllegalStateException("Not signed in"));
            return;
        }

        peek(owner, cached -> {
            boolean usable = cached != null
                    && cached.fresh
                    && (!matchRemote || Objects.equals(cached.selection.id, remoteId));

            if (usable) {
                callback.accept(cached.selection, null);
            } else {
                fetch(owner, callback);
            }
        });
    }

    // ---------------------------------------------------------------- network

    private static void fetch(final String owner, displayedAwardApi.Callback callback) {
        List<displayedAwardApi.Callback> waiting = PENDING.get(owner);

        if (waiting != null) {
            waiting.add(callback);
            return;
        }

        waiting = new ArrayList<>();
        waiting.add(callback);
        PENDING.put(owner, waiting);

        final int requestEpoch = epoch;

        displayedAwardApi.get(owner, (selection, error) -> {
            if (requestEpoch != epoch) {
                // Waiting callers were already failed when the account changed.
                return;
            }

            List<displayedAwardApi.Callback> callbacks = PENDING.remove(owner);

            if (callbacks == null) {
                return;
            }

            for (displayedAwardApi.Callback item : callbacks) {
                item.accept(selection, error);
            }
        });
    }

    // ------------------------------------------------------------------ write

    /**
     * Called by {@link displayedAwardApi} after a successful read or save.
     * Ignored when the signed-in account is no longer the one that made the request.
     */
    static void remember(
            final String requestAccount,
            final String owner,
            displayedAwardApi.Selection selection
    ) {
        String current = currentAccount();

        if (requestAccount == null
                || owner == null
                || selection == null
                || !requestAccount.equals(current)) {
            return;
        }

        final displayedAwardApi.Selection stored = new displayedAwardApi.Selection(
                selection.id,
                awardsCacheStore.canonicalMap(selection.award)
        );
        final long savedAt = nextSavedAt(owner);

        MEMORY.put(owner, new Stored(stored, savedAt));

        awardsCacheStore.execute(() -> awardsCacheStore.writeSelection(
                requestAccount,
                owner,
                stored.id,
                stored.award,
                savedAt
        ));
    }

    // ---------------------------------------------------------------- compare

    static boolean sameSelection(
            displayedAwardApi.Selection first,
            displayedAwardApi.Selection second
    ) {
        if (first == null || second == null) {
            return first == second;
        }

        return Objects.equals(first.id, second.id)
                && awardsCacheStore.fingerprint(first.award)
                .equals(awardsCacheStore.fingerprint(second.award));
    }
}
