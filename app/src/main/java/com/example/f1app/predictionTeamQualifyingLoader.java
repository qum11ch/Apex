package com.example.f1app;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class predictionTeamQualifyingLoader {
    public interface Callback {
        void onLoaded(List<Event> events);
        void onError();
    }

    public static final class Event {
        public final int round;
        public final String raceName;
        public final List<Integer> a, b;
        Event(int round, String raceName, List<Integer> a, List<Integer> b) {
            this.round = round;
            this.raceName = raceName;
            this.a = Collections.unmodifiableList(new ArrayList<>(a));
            this.b = Collections.unmodifiableList(new ArrayList<>(b));
        }
    }

    public void load(String season, Integer beforeRound, String teamA,
                     String teamB, Callback callback) {
        if (season == null || teamA == null || teamB == null || teamA.equals(teamB)
                || (beforeRound != null && beforeRound < 1)) {
            callback.onError();
            return;
        }
        FirebaseDatabase.getInstance().getReference().child("qualifying").child(season)
                .addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(DataSnapshot snapshot) {
                List<Event> events = new ArrayList<>();
                for (DataSnapshot race : snapshot.getChildren()) {
                    String rawRound = race.child("round").getValue(String.class);
                    int round;
                    try {
                        if (rawRound == null) continue;
                        round = Integer.parseInt(rawRound.trim());
                    } catch (NumberFormatException error) { continue; }
                    if (round < 1 || (beforeRound != null && round >= beforeRound)) continue;
                    String raceName = race.child("raceName").getValue(String.class);
                    if (raceName == null || raceName.trim().isEmpty()) continue;
                    List<Integer> a = positions(race.child("results"), teamA);
                    List<Integer> b = positions(race.child("results"), teamB);
                    if (a.size() != 2 || b.size() != 2) continue;
                    events.add(new Event(round, raceName, a, b));
                }
                events.sort((a, b) -> Integer.compare(a.round, b.round));
                callback.onLoaded(events);
            }
            @Override public void onCancelled(DatabaseError error) { callback.onError(); }
        });
    }

    private static List<Integer> positions(DataSnapshot results, String teamId) {
        List<Integer> values = new ArrayList<>();
        for (DataSnapshot result : results.getChildren()) {
            if (!teamId.equals(result.child("constructorId").getValue(String.class))) continue;
            Long position = result.child("position").getValue(Long.class);
            if (position != null && position > 0 && position <= Integer.MAX_VALUE) {
                values.add(position.intValue());
            }
        }
        Collections.sort(values);
        return values;
    }
}
