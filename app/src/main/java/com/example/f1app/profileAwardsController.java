package com.example.f1app;

import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.functions.FirebaseFunctions;

import java.util.Collections;
import java.util.Objects;

/**
 * Own-profile awards card.
 *
 * Stored awards are shown at once; the server is asked only when the stored
 * collection is stale or something may have changed. While an already shown
 * card is refreshed, only the small indicator is visible, and a failed
 * refresh keeps the collection on screen.
 */
public final class profileAwardsController implements DefaultLifecycleObserver {

    private static final String TAG = "apex.profile.awards.stage1";
    private static final long LOADING_DELAY_MS = 120L;
    private static final long SYNC_INTERVAL_MS = 2L * 60L * 1000L;

    private static String lastSyncAccount;
    private static long lastSyncAt;

    private final AppCompatActivity activity;
    private final profileAwardsCard card;
    private final Handler main = new Handler(Looper.getMainLooper());

    private boolean started;
    private boolean syncing;
    private boolean favoritesSeen;
    private String uid;
    private String lastDriver;
    private String lastTeam;
    private int session;
    private int readGeneration;
    private DatabaseReference favorites;
    private ValueEventListener listener;

    public static void attach(AppCompatActivity activity) {
        View anchor = activity.findViewById(R.id.createdAt);

        if (anchor == null || !(anchor.getParent() instanceof LinearLayout)) {
            throw new IllegalStateException(
                    "Profile awards: expected account_page createdAt anchor"
            );
        }

        LinearLayout parent = (LinearLayout) anchor.getParent();

        if (parent.findViewWithTag(TAG) != null) {
            return;
        }

        View insertion = anchor;
        View stats = activity.findViewById(R.id.prediction_count);

        if (stats != null) {
            View candidate = stats;

            while (candidate.getParent() instanceof View && candidate.getParent() != parent) {
                candidate = (View) candidate.getParent();
            }

            if (candidate.getParent() == parent) {
                insertion = candidate;
            }
        }

        profileAwardsCard card = new profileAwardsCard(activity);
        card.setTag(TAG);

        float density = activity.getResources().getDisplayMetrics().density;
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.topMargin = Math.round(16 * density);
        params.bottomMargin = Math.round(4 * density);

        parent.addView(card, parent.indexOfChild(insertion) + 1, params);

        card.setCollectionAction(() -> activity.startActivity(
                new Intent(activity, awardsCollectionActivity.class)
        ));

        activity.getLifecycle().addObserver(new profileAwardsController(activity, card));
    }

    private profileAwardsController(AppCompatActivity activity, profileAwardsCard card) {
        this.activity = activity;
        this.card = card;

        card.setRefreshAction(() -> sync(true));
    }

    // ------------------------------------------------------------------ state

    private boolean active(int expectedSession, String expectedUid) {
        return started
                && session == expectedSession
                && !activity.isFinishing()
                && !activity.isDestroyed()
                && expectedUid != null
                && expectedUid.equals(uid)
                && expectedUid.equals(FirebaseAuth.getInstance().getUid());
    }

    private boolean isCurrentRead(int expectedSession, String expectedUid, int request) {
        return active(expectedSession, expectedUid) && request == readGeneration;
    }

    private void scheduleLoading(
            final int expectedSession,
            final String expectedUid,
            final int request
    ) {
        main.postDelayed(() -> {
            if (isCurrentRead(expectedSession, expectedUid, request)) {
                card.showLoading();
            }
        }, LOADING_DELAY_MS);
    }

    // ------------------------------------------------------------------ reads

    /**
     * Shows the stored collection first. The server is asked when the stored
     * one is missing or stale, or when {@code force} is set.
     */
    private void read(final boolean force, final boolean syncWarning) {
        if (!started || uid == null) {
            return;
        }

        final int expectedSession = session;
        final int request = ++readGeneration;
        final String requested = uid;

        if (force) {
            awardsRepository.invalidate(requested);
        }

        awardsRepository.peek(requested, snapshot -> {
            if (!isCurrentRead(expectedSession, requested, request)) {
                return;
            }

            if (snapshot != null) {
                card.showData(snapshot.badges);
            }

            if (!force && snapshot != null && snapshot.fresh) {
                if (syncWarning) {
                    card.showSyncWarning();
                }

                return;
            }

            refresh(expectedSession, requested, request, syncWarning);
        });
    }

    private void refresh(
            final int expectedSession,
            final String requested,
            final int request,
            final boolean syncWarning
    ) {
        card.setBusy(true);
        scheduleLoading(expectedSession, requested, request);

        awardsRepository.load(requested, (badges, error) -> {
            if (!isCurrentRead(expectedSession, requested, request)) {
                return;
            }

            // Cancels the pending loading transition before showing the result.
            readGeneration++;

            if (error != null || badges == null) {
                Log.w("ProfileAwards", "Awards read failed", error);
                card.showError();
                return;
            }

            card.showData(badges);

            if (syncWarning) {
                card.showSyncWarning();
            }
        });
    }

    // ------------------------------------------------------------------- sync

    private static boolean syncDue(String account) {
        if (!Objects.equals(lastSyncAccount, account)) {
            return true;
        }

        return SystemClock.elapsedRealtime() - lastSyncAt >= SYNC_INTERVAL_MS;
    }

    private void sync(boolean manual) {
        if (!started || uid == null) {
            return;
        }

        if (syncing) {
            if (manual) {
                card.showLoading();
            }

            return;
        }

        if (!manual && !syncDue(uid)) {
            return;
        }

        syncing = true;

        final int expectedSession = session;
        final String requested = uid;

        if (manual) {
            int request = ++readGeneration;
            card.setBusy(true);
            scheduleLoading(expectedSession, requested, request);
        }

        FirebaseFunctions.getInstance("us-central1")
                .getHttpsCallable("syncMyAwards")
                .call(Collections.emptyMap())
                .addOnCompleteListener(task -> {
                    if (!active(expectedSession, requested)) {
                        return;
                    }

                    syncing = false;

                    if (task.isSuccessful()) {
                        lastSyncAccount = requested;
                        lastSyncAt = SystemClock.elapsedRealtime();
                    } else {
                        Log.w("ProfileAwards", "Achievement check failed", task.getException());
                    }

                    read(true, !task.isSuccessful());
                });
    }

    // -------------------------------------------------------------- lifecycle

    @Override
    public void onStart(@NonNull LifecycleOwner owner) {
        started = true;
        session++;

        String current = FirebaseAuth.getInstance().getUid();

        if (!Objects.equals(uid, current)) {
            card.resetOwner();
            uid = current;
        }

        if (uid == null) {
            card.setVisibility(View.GONE);
            return;
        }

        card.setVisibility(View.VISIBLE);
        read(false, false);
        sync(false);

        favoritesSeen = false;
        favorites = FirebaseDatabase.getInstance().getReference("users").child(uid);

        final int expectedSession = session;
        final String requested = uid;

        listener = new ValueEventListener() {

            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!active(expectedSession, requested)) {
                    return;
                }

                String driver = String.valueOf(snapshot.child("choiceDriver").getValue());
                String team = String.valueOf(snapshot.child("choiceTeam").getValue());

                boolean changed = favoritesSeen
                        && (!Objects.equals(lastDriver, driver)
                        || !Objects.equals(lastTeam, team));

                lastDriver = driver;
                lastTeam = team;
                favoritesSeen = true;

                if (changed) {
                    read(true, false);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.w("ProfileAwards", "Favorite listener cancelled", error.toException());
            }
        };

        favorites.addValueEventListener(listener);
    }

    @Override
    public void onStop(@NonNull LifecycleOwner owner) {
        started = false;
        syncing = false;
        session++;
        readGeneration++;
        main.removeCallbacksAndMessages(null);

        if (favorites != null && listener != null) {
            favorites.removeEventListener(listener);
        }

        favorites = null;
        listener = null;
        card.finishLoading();
    }

    @Override
    public void onDestroy(@NonNull LifecycleOwner owner) {
        started = false;
        syncing = false;
        session++;
        readGeneration++;
        main.removeCallbacksAndMessages(null);
        card.release();
    }
}
