package com.example.f1app;

import androidx.annotation.NonNull;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class leaderboardLoader {
    public static final int LIMIT = 50;

    public interface Callback {
        void onLoaded(List<leaderboardEntry> top, leaderboardEntry me);
        void onError();
    }

    private static int toInt(Object value) {
        if (value instanceof Number) return ((Number) value).intValue();
        if (value instanceof String) {
            try { return (int) Math.round(Double.parseDouble((String) value)); }
            catch (NumberFormatException ignored) { }
        }
        return 0;
    }

    private static leaderboardEntry parse(DataSnapshot snap, String myUid) {
        leaderboardEntry e = new leaderboardEntry();
        e.uid = snap.getKey();
        e.rank = toInt(snap.child("rank").getValue());
        e.points = toInt(snap.child("totalPoints").getValue());
        e.correct = toInt(snap.child("correctPredictions").getValue());
        e.isMe = e.uid != null && e.uid.equals(myUid);
        return e;
    }

    public void load(String scope, String myUid, Callback callback) {
        FirebaseDatabase db = FirebaseDatabase.getInstance();

        db.getReference("leaderboard").child(scope)
                .orderByChild("rank").limitToFirst(LIMIT)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        List<leaderboardEntry> top = new ArrayList<>();
                        for (DataSnapshot child : snapshot.getChildren()) {
                            top.add(parse(child, myUid));
                        }
                        Collections.sort(top, (a, b) -> Integer.compare(a.rank, b.rank));
                        loadProfilesAndMe(db, scope, myUid, top, callback);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        callback.onError();
                    }
                });
    }

    private void loadProfilesAndMe(FirebaseDatabase db, String scope, String myUid,
                                   List<leaderboardEntry> top, Callback callback) {
        boolean meInTop = false;
        for (leaderboardEntry e : top) {
            if (e.isMe) { meInTop = true; break; }
        }

        final boolean needMe = myUid != null && !meInTop;
        final leaderboardEntry[] me = {null};
        final int[] pending = {top.size() + (needMe ? 1 : 0)};

        if (pending[0] == 0) {
            callback.onLoaded(top, null);
            return;
        }

        Runnable done = () -> {
            pending[0]--;
            if (pending[0] == 0) {
                callback.onLoaded(top, me[0]);
            }
        };

        for (leaderboardEntry e : top) {
            db.getReference("publicProfiles").child(e.uid).child("username")
                    .get().addOnCompleteListener(task -> {
                        if (task.isSuccessful() && task.getResult().getValue() != null) {
                            e.username = String.valueOf(task.getResult().getValue());
                        }
                        done.run();
                    });
        }

        if (needMe) {
            db.getReference("leaderboard").child(scope).child(myUid)
                    .get().addOnCompleteListener(task -> {
                        if (task.isSuccessful() && task.getResult().exists()) {
                            leaderboardEntry entry = parse(task.getResult(), myUid);
                            db.getReference("publicProfiles").child(myUid).child("username")
                                    .get().addOnCompleteListener(nameTask -> {
                                        if (nameTask.isSuccessful()
                                                && nameTask.getResult().getValue() != null) {
                                            entry.username =
                                                    String.valueOf(nameTask.getResult().getValue());
                                        }
                                        me[0] = entry;
                                        done.run();
                                    });
                        } else {
                            done.run();
                        }
                    });
        }
    }
}