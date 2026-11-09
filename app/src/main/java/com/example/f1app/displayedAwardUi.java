package com.example.f1app;

import android.content.Intent;
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

    private static void observe(AppCompatActivity activity, boolean own, awardIconView icon, TextView title, TextView state) {
        activity.getLifecycle().addObserver(new DefaultLifecycleObserver() {
            private boolean started;
            private boolean hasValue;
            private int generation;
            private int session;
            private String uid;
            private String key;
            private DatabaseReference watched;
            private ValueEventListener listener;

            private void load() {
                if (!started || uid == null) {
                    return;
                }

                final int request = ++generation;
                final int expectedSession = session;
                final String requested = uid;

                displayedAwardApi.get(uid, (selection, error) -> {
                    if (!started || generation != request || session != expectedSession
                            || activity.isFinishing() || activity.isDestroyed() || !requested.equals(uid)) {
                        return;
                    }

                    if (own && !requested.equals(FirebaseAuth.getInstance().getUid())) {
                        return;
                    }

                    if (error != null) {
                        android.util.Log.w("DisplayedAward", "Selection unavailable", error);
                        if (title != null) {
                            if (!hasValue) {
                                title.setText(R.string.displayed_award_load_error);
                            }
                            state.setText(R.string.displayed_award_load_error);
                        }
                        return;
                    }

                    hasValue = true;
                    icon.clear();

                    if (selection.id == null) {
                        icon.setVisibility(View.GONE);
                        if (title != null) {
                            title.setText(R.string.displayed_award_none);
                        }
                    } else {
                        icon.setVisibility(View.VISIBLE);
                        awardCatalogue.Entry entry = awardCatalogue.find(selection.id);
                        String caption = activity.getString(profileAwardPresentation.title(selection.id));
                        String name = profileAwardPresentation.string(selection.award, "name");

                        catalogueAwardIcons.bind(
                                icon,
                                entry,
                                selection.award,
                                caption + (name.isEmpty() ? "" : ": " + name),
                                () -> started && generation == request && session == expectedSession && requested.equals(uid)
                        );

                        if (title != null) {
                            title.setText(caption);
                        }
                    }
                    if (state != null)
                        state.setText(R.string.displayed_award_subtitle);
                });
            }

            @Override
            public void onStart(@NonNull LifecycleOwner owner) {
                started=true;
                session++;
                String current = own ? FirebaseAuth.getInstance().getUid()
                        : activity.getIntent().getStringExtra("profileUid");
                if (!Objects.equals(current, uid)){
                    uid = current;
                    hasValue = false;
                    icon.clear();
                    icon.setVisibility(View.GONE);
                }

                if (uid == null || uid.isEmpty() || uid.matches(".*[.#$\\[\\]/\\p{Cntrl}].*"))
                    return;

                key = null;
                watched = FirebaseDatabase.getInstance().getReference(own
                        ? "users"
                        : "publicProfiles").child(uid);

                listener = new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        String next = String.valueOf(snapshot.child("displayedAwardId").getValue());
                        if (own)
                            next += "|" + String.valueOf(snapshot.child("choiceDriver").getValue())
                                    + "|" + String.valueOf(snapshot.child("choiceTeam").getValue());
                        if (!Objects.equals(key, next)){
                            key = next;
                            load();
                        }
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error){
                        load();
                    }
                };
                watched.addValueEventListener(listener);
            }

            @Override
            public void onStop(@NonNull LifecycleOwner owner) {
                started = false;
                session++;
                generation++;
                if (watched != null && listener != null)
                    watched.removeEventListener(listener);

                watched = null;
                listener = null;
            }

            @Override
            public void onDestroy(@NonNull LifecycleOwner owner){
                icon.clear();
            }
        });
    }
}
