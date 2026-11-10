package com.example.f1app;

import android.content.Intent;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

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

import java.util.Objects;

public final class displayedAwardUi {

    private displayedAwardUi() {
    }

    public static void attachProfile(AppCompatActivity activity, boolean own) {
        awardIconView icon = activity.findViewById(R.id.profileDisplayedAward);

        if (icon == null) {
            return;
        }

        observe(activity, own, icon, null, null);
    }

    public static void attachSettings(AppCompatActivity activity) {
        View card = activity.findViewById(R.id.displayedAwardCard);
        awardIconView icon = activity.findViewById(R.id.displayedAwardIcon);
        TextView title = activity.findViewById(R.id.displayedAwardTitle);
        TextView state = activity.findViewById(R.id.displayedAwardState);

        if (card == null || icon == null || title == null || state == null) {
            return;
        }

        card.setOnClickListener(view -> activity.startActivity(
                new Intent(activity, displayedAwardPickerActivity.class)
        ));

        observe(activity, true, icon, title, state);
    }

    /**
     * Shows the stored selection immediately, then confirms it with the server.
     * A failed refresh never clears what is already on screen.
     */
    private static void observe(
            final AppCompatActivity activity,
            final boolean own,
            final awardIconView icon,
            final TextView title,
            final TextView state
    ) {
        activity.getLifecycle().addObserver(new DefaultLifecycleObserver() {

            private boolean started;
            private boolean hasValue;
            private int generation;
            private int session;
            private int bindToken;
            private String uid;
            private String key;
            private displayedAwardApi.Selection shown;
            private DatabaseReference watched;
            private ValueEventListener listener;

            private boolean isCurrent(int request, int expectedSession, String requested) {
                if (!started
                        || generation != request
                        || session != expectedSession
                        || activity.isFinishing()
                        || activity.isDestroyed()
                        || !requested.equals(uid)) {
                    return false;
                }

                return !own || requested.equals(FirebaseAuth.getInstance().getUid());
            }

            private void showCached(final int expectedSession, final String requested) {
                displayedAwardRepository.peek(requested, cached -> {
                    if (cached == null || hasValue) {
                        return;
                    }

                    if (!started
                            || session != expectedSession
                            || activity.isFinishing()
                            || activity.isDestroyed()
                            || !requested.equals(uid)) {
                        return;
                    }

                    if (own && !requested.equals(FirebaseAuth.getInstance().getUid())) {
                        return;
                    }

                    show(cached.selection, requested);
                });
            }

            private void load(boolean force, boolean matchRemote, String remoteId) {
                if (!started || uid == null) {
                    return;
                }

                final int request = ++generation;
                final int expectedSession = session;
                final String requested = uid;

                displayedAwardApi.Callback callback = (selection, error) -> {
                    if (!isCurrent(request, expectedSession, requested)) {
                        return;
                    }

                    if (error != null) {
                        Log.w("DisplayedAward", "Selection unavailable", error);

                        if (!hasValue) {
                            if (title != null) {
                                title.setText(R.string.displayed_award_load_error);
                            }

                            if (state != null) {
                                state.setText(R.string.displayed_award_load_error);
                            }
                        }

                        return;
                    }

                    show(selection, requested);
                };

                if (matchRemote) {
                    displayedAwardRepository.loadMatching(requested, remoteId, callback);
                } else {
                    displayedAwardRepository.load(requested, force, callback);
                }
            }

            private void show(displayedAwardApi.Selection selection, final String requested) {
                if (hasValue && displayedAwardRepository.sameSelection(shown, selection)) {
                    if (state != null) {
                        state.setText(R.string.displayed_award_subtitle);
                    }

                    return;
                }

                hasValue = true;
                shown = selection;

                final int token = ++bindToken;
                icon.clear();

                awardCatalogue.Entry entry = selection.id == null
                        ? null
                        : awardCatalogue.find(selection.id);

                if (entry == null) {
                    icon.setVisibility(View.GONE);

                    if (title != null) {
                        title.setText(R.string.displayed_award_none);
                    }
                } else {
                    icon.setVisibility(View.VISIBLE);

                    String caption = activity.getString(
                            profileAwardPresentation.title(selection.id)
                    );
                    String name = profileAwardPresentation.string(selection.award, "name");

                    catalogueAwardIcons.bind(
                            icon,
                            entry,
                            selection.award,
                            caption + (name.isEmpty() ? "" : ": " + name),
                            () -> started && bindToken == token && requested.equals(uid)
                    );

                    if (title != null) {
                        title.setText(caption);
                    }
                }

                if (state != null) {
                    state.setText(R.string.displayed_award_subtitle);
                }
            }

            @Override
            public void onStart(@NonNull LifecycleOwner owner) {
                started = true;
                session++;

                String current = own
                        ? FirebaseAuth.getInstance().getUid()
                        : activity.getIntent().getStringExtra("profileUid");

                if (!Objects.equals(current, uid)) {
                    uid = current;
                    hasValue = false;
                    shown = null;
                    bindToken++;
                    icon.clear();
                    icon.setVisibility(View.GONE);
                }

                if (uid == null
                        || uid.isEmpty()
                        || uid.matches(".*[.#$\\[\\]/\\p{Cntrl}].*")) {
                    return;
                }

                key = null;
                showCached(session, uid);

                watched = FirebaseDatabase.getInstance()
                        .getReference(own ? "users" : "publicProfiles")
                        .child(uid);

                listener = new ValueEventListener() {

                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        Object remote = snapshot.child("displayedAwardId").getValue();
                        String remoteId = remote instanceof String ? (String) remote : null;
                        String next = String.valueOf(remote);

                        if (own) {
                            next += "|" + snapshot.child("choiceDriver").getValue()
                                    + "|" + snapshot.child("choiceTeam").getValue();
                        }

                        boolean first = key == null;
                        boolean changed = !first && !Objects.equals(key, next);
                        key = next;

                        if (changed) {
                            load(true, false, null);
                        } else if (first) {
                            load(false, true, remoteId);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        load(false, false, null);
                    }
                };

                watched.addValueEventListener(listener);
            }

            @Override
            public void onStop(@NonNull LifecycleOwner owner) {
                started = false;
                session++;
                generation++;

                // Forget the bound view state so the next start rebinds the icon.
                shown = null;

                if (watched != null && listener != null) {
                    watched.removeEventListener(listener);
                }

                watched = null;
                listener = null;
            }

            @Override
            public void onDestroy(@NonNull LifecycleOwner owner) {
                icon.clear();
            }
        });
    }
}
