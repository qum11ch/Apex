package com.example.f1app;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;

import com.google.firebase.auth.FirebaseAuth;

import java.util.Objects;

/**
 * Awards card of another user's profile.
 *
 * Same behaviour as the own-profile card: stored awards first, server check in
 * the background, errors never clear what is shown. It never opens the
 * authenticated user's collection and never starts an award sync.
 */
public final class publicProfileAwardsController implements DefaultLifecycleObserver {

    private static final String TAG = "apex.public.profile.awards.stage1";
    private static final long LOADING_DELAY_MS = 120L;

    private final AppCompatActivity activity;
    private final profileAwardsCard card;
    private final Handler main = new Handler(Looper.getMainLooper());

    private boolean started;
    private String uid;
    private String actor;
    private int session;
    private int generation;

    public static void attach(AppCompatActivity activity) {
        View anchor = activity.findViewById(R.id.createdAt);

        if (anchor == null || !(anchor.getParent() instanceof LinearLayout)) {
            throw new IllegalStateException(
                    "Public awards require the createdAt LinearLayout anchor"
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

        // No collection action: AwardsCollectionActivity reads the signed-in user's
        // collection, so the shared card expands earned public awards in place.
        activity.getLifecycle().addObserver(new publicProfileAwardsController(activity, card));
    }

    private publicProfileAwardsController(AppCompatActivity activity, profileAwardsCard card) {
        this.activity = activity;
        this.card = card;

        card.setRefreshAction(() -> read(true));
    }

    private static boolean validUid(String value) {
        return value != null
                && !value.isEmpty()
                && !value.matches(".*[.#$\\[\\]/\\p{Cntrl}].*");
    }

    private boolean active(int expectedSession, String expectedUid, String expectedActor) {
        return started
                && session == expectedSession
                && !activity.isFinishing()
                && !activity.isDestroyed()
                && Objects.equals(expectedUid, uid)
                && Objects.equals(expectedUid, activity.getIntent().getStringExtra("profileUid"))
                && Objects.equals(expectedActor, FirebaseAuth.getInstance().getUid());
    }

    private boolean isCurrentRead(
            int expectedSession,
            String expectedUid,
            String expectedActor,
            int request
    ) {
        return active(expectedSession, expectedUid, expectedActor) && request == generation;
    }

    private void read(final boolean force) {
        if (!started || !validUid(uid)) {
            return;
        }

        final int expectedSession = session;
        final int request = ++generation;
        final String requested = uid;
        final String requestedActor = actor;

        if (!active(expectedSession, requested, requestedActor)) {
            return;
        }

        if (force) {
            awardsRepository.invalidate(requested);
        }

        awardsRepository.peek(requested, snapshot -> {
            if (!isCurrentRead(expectedSession, requested, requestedActor, request)) {
                return;
            }

            if (snapshot != null) {
                card.showData(snapshot.badges);
            }

            if (!force && snapshot != null && snapshot.fresh) {
                return;
            }

            refresh(expectedSession, requested, requestedActor, request);
        });
    }

    private void refresh(
            final int expectedSession,
            final String requested,
            final String requestedActor,
            final int request
    ) {
        card.setBusy(true);

        main.postDelayed(() -> {
            if (isCurrentRead(expectedSession, requested, requestedActor, request)) {
                card.showLoading();
            }
        }, LOADING_DELAY_MS);

        awardsRepository.load(requested, (badges, error) -> {
            if (!isCurrentRead(expectedSession, requested, requestedActor, request)) {
                return;
            }

            // Cancels the pending loading transition before showing the result.
            generation++;

            if (error != null || badges == null) {
                Log.w("PublicProfileAwards", "Public awards unavailable", error);
                card.showError();
                return;
            }

            card.showData(badges);
        });
    }

    @Override
    public void onStart(@NonNull LifecycleOwner owner) {
        started = true;
        session++;

        String current = activity.getIntent().getStringExtra("profileUid");
        String currentActor = FirebaseAuth.getInstance().getUid();

        if (!Objects.equals(uid, current) || !Objects.equals(actor, currentActor)) {
            card.resetOwner();
        }

        uid = current;
        actor = currentActor;

        if (!validUid(uid)) {
            card.resetOwner();
            card.setVisibility(View.GONE);
            return;
        }

        card.setVisibility(View.VISIBLE);
        read(false);
    }

    @Override
    public void onStop(@NonNull LifecycleOwner owner) {
        started = false;
        session++;
        generation++;
        main.removeCallbacksAndMessages(null);
        card.finishLoading();
    }

    @Override
    public void onDestroy(@NonNull LifecycleOwner owner) {
        started = false;
        session++;
        generation++;
        main.removeCallbacksAndMessages(null);
        card.release();
    }
}
