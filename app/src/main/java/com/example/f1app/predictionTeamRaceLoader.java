package com.example.f1app;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public final class predictionTeamRaceLoader {
    public interface Callback {
        void onLoaded(List<Event> events);
        void onError();
    }

    public static final class Finish {
        public final String driverName, rawFinish;
        public final Integer position;
        Finish(String driverName, String rawFinish, Integer position) {
            this.driverName = driverName;
            this.rawFinish = rawFinish;
            this.position = position;
        }
    }

    public static final class Event {
        public final int round;
        public final String raceName;
        public final List<Finish> a, b;
        Event(int round, String raceName, List<Finish> a, List<Finish> b) {
            this.round = round;
            this.raceName = raceName;
            this.a = java.util.Collections.unmodifiableList(new ArrayList<>(a));
            this.b = java.util.Collections.unmodifiableList(new ArrayList<>(b));
        }
    }

    private static final class Race {
        final int round;
        final String name;
        Race(int round, String name) { this.round = round; this.name = name; }
    }

    public void load(String season, Integer beforeRound, String teamA, String teamB,
                     Callback callback) {
        if (season == null || teamA == null || teamB == null || teamA.equals(teamB)
                || (beforeRound != null && beforeRound < 1)) {
            callback.onError();
            return;
        }
        DatabaseReference root = FirebaseDatabase.getInstance().getReference();
        root.child("schedule").child("season").child(season)
                .addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(DataSnapshot schedule) {
                List<Race> races = new ArrayList<>();
                for (DataSnapshot race : schedule.getChildren()) {
                    Long round = race.child("round").getValue(Long.class);
                    if (round == null || round < 1 || round > Integer.MAX_VALUE
                            || race.getKey() == null
                            || (beforeRound != null && round >= beforeRound)) continue;
                    races.add(new Race(round.intValue(), race.getKey()));
                }
                races.sort((a, b) -> Integer.compare(a.round, b.round));
                root.child("results").child("season").child(season)
                        .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override public void onDataChange(DataSnapshot results) {
                        List<Event> events = new ArrayList<>();
                        for (Race race : races) {
                            List<Finish> a = finishes(results, race.name, teamA);
                            List<Finish> b = finishes(results, race.name, teamB);
                            if (a.size() != 2 || b.size() != 2) continue;
                            events.add(new Event(race.round, race.name, a, b));
                        }
                        callback.onLoaded(events);
                    }
                    @Override public void onCancelled(DatabaseError error) { callback.onError(); }
                });
            }
            @Override public void onCancelled(DatabaseError error) { callback.onError(); }
        });
    }

    private static List<Finish> finishes(DataSnapshot results, String race, String teamId) {
        List<Finish> values = new ArrayList<>();
        for (DataSnapshot driver : results.getChildren()) {
            DataSnapshot record = driver.child(race);
            if (!teamId.equals(record.child("TeamId").getValue(String.class))) continue;
            String raw = record.child("Result").getValue(String.class);
            if (raw == null) continue;
            String[] parts = raw.trim().split("-", 2);
            if (parts.length != 2) continue;
            String finish = parts[1].trim();
            Integer position = null;
            try {
                int p = Integer.parseInt(finish);
                if (p <= 0) continue;
                position = p;
            } catch (NumberFormatException error) {
                if (!"R".equals(finish) && !"W".equals(finish)
                        && !"D".equals(finish) && !"NC".equals(finish)) continue;
            }
            values.add(new Finish(driver.getKey(), finish, position));
        }
        values.sort((a, b) -> {
            if (a.position != null && b.position != null) return Integer.compare(a.position, b.position);
            if (a.position != null) return -1;
            if (b.position != null) return 1;
            return a.rawFinish.compareTo(b.rawFinish);
        });
        return values;
    }
}
