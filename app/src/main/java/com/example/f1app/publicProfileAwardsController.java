package com.example.f1app;

import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import com.google.firebase.auth.FirebaseAuth;
import java.util.Objects;

public final class publicProfileAwardsController implements DefaultLifecycleObserver {
    private static final String TAG = "apex.public.profile.awards.stage1";
    private final AppCompatActivity activity;
    private final profileAwardsCard card;
    private final Handler main = new Handler(Looper.getMainLooper());
    private boolean started;
    private String uid, actor;
    private int session, generation;

    public static void attach(AppCompatActivity activity) {
        View anchor = activity.findViewById(R.id.createdAt);
        if (anchor == null || !(anchor.getParent() instanceof LinearLayout)) {
            throw new IllegalStateException("Public awards require the createdAt LinearLayout anchor");
        }
        LinearLayout parent = (LinearLayout) anchor.getParent();
        if (parent.findViewWithTag(TAG) != null) return;
        View insertion = anchor;
        View stats = activity.findViewById(R.id.prediction_count);
        if (stats != null) {
            View candidate = stats;
            while (candidate.getParent() instanceof View && candidate.getParent() != parent) {
                candidate = (View) candidate.getParent();
            }
            if (candidate.getParent() == parent) insertion = candidate;
        }
        profileAwardsCard card = new profileAwardsCard(activity);
        card.setTag(TAG);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        float density = activity.getResources().getDisplayMetrics().density;
        params.topMargin = Math.round(16 * density);
        params.bottomMargin = Math.round(4 * density);
        parent.addView(card, parent.indexOfChild(insertion) + 1, params);
        // Do not open AwardsCollectionActivity: it reads the authenticated user's collection.
        // Without a collection action the shared card expands earned public awards in place.
        activity.getLifecycle().addObserver(new publicProfileAwardsController(activity, card));
    }

    private publicProfileAwardsController(AppCompatActivity activity, profileAwardsCard card) {
        this.activity = activity;
        this.card = card;
        card.setRefreshAction(() -> read(true));
    }

    private static boolean validUid(String value) {
        return value != null && !value.isEmpty()
                && !value.matches(".*[.#$\\[\\]/\\p{Cntrl}].*");
    }

    private boolean active(int expectedSession, String expectedUid, String expectedActor) {
        return started && session == expectedSession
                && !activity.isFinishing() && !activity.isDestroyed()
                && Objects.equals(expectedUid, uid)
                && Objects.equals(expectedUid, activity.getIntent().getStringExtra("profileUid"))
                && Objects.equals(expectedActor, FirebaseAuth.getInstance().getUid());
    }

    private void read(boolean force) {
        if (!started || !validUid(uid)) return;
        final int expectedSession = session;
        final int request = ++generation;
        final String requested = uid, requestedActor = actor;
        if (!active(expectedSession, requested, requestedActor)) return;
        if (force) awardsRepository.invalidate(requested);
        card.setBusy(true);
        main.postDelayed(() -> {
            if (active(expectedSession, requested, requestedActor) && request == generation) {
                card.showLoading();
            }
        }, 120);
        awardsRepository.load(requested, (badges, error) -> {
            if (!active(expectedSession, requested, requestedActor) || request != generation) return;
            generation++;
            if (error != null || badges == null) {
                android.util.Log.w("PublicProfileAwards", "Public awards unavailable", error);
                card.showError();
                return;
            }
            card.showData(badges);
        });
    }

    @Override public void onStart(@NonNull LifecycleOwner owner) {
        started = true;
        session++;
        String current = activity.getIntent().getStringExtra("profileUid");
        String currentActor = FirebaseAuth.getInstance().getUid();
        if (!Objects.equals(uid, current) || !Objects.equals(actor, currentActor)) card.resetOwner();
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

    @Override public void onStop(@NonNull LifecycleOwner owner) {
        started = false;
        session++;
        generation++;
        main.removeCallbacksAndMessages(null);
        card.finishLoading();
    }

    @Override public void onDestroy(@NonNull LifecycleOwner owner) {
        started = false;
        session++;
        generation++;
        main.removeCallbacksAndMessages(null);
        card.release();
    }
}
