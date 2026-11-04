package com.example.f1app;

import com.google.firebase.database.*;
import java.util.ArrayList;
import java.util.List;

/** Period-limited history for driver duels; does not modify database state. */
public final class predictionDriverHistoryLoader {
    private final DatabaseReference root = FirebaseDatabase.getInstance().getReference();

    public void loadRace(String season, Integer beforeRound,
                         String nameA, String nameB,
                         predictionStatsLoader.raceStatsCallback callback) {
        if (!valid(season, beforeRound, nameA, nameB)) { callback.onError(); return; }
        root.child("schedule").child("season").child(season)
                .addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(DataSnapshot calendar) {
                List<Race> races = new ArrayList<>();
                for (DataSnapshot race : calendar.getChildren()) {
                    Long round = race.child("round").getValue(Long.class);
                    if (round == null || round < 1 || round > Integer.MAX_VALUE
                            || race.getKey() == null
                            || (beforeRound != null && round >= beforeRound)) continue;
                    races.add(new Race(round.intValue(), race.getKey()));
                }
                races.sort((a, b) -> Integer.compare(a.round, b.round));
                root.child("results").child("season").child(season)
                        .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override public void onDataChange(DataSnapshot snapshot) {
                        List<raceDuelEvent> events = new ArrayList<>();
                        for (Race race : races) {
                            String a = snapshot.child(nameA).child(race.name).child("Result")
                                    .getValue(String.class);
                            String b = snapshot.child(nameB).child(race.name).child("Result")
                                    .getValue(String.class);
                            String finishA = completedFinish(a);
                            String finishB = completedFinish(b);
                            if (finishA == null || finishB == null) continue;
                            Integer pa = position(finishA), pb = position(finishB);
                            events.add(new raceDuelEvent(race.name, a.trim(), b.trim(), pa, pb,
                                    finishA, finishB, points(pa), points(pb)));
                        }
                        callback.onLoaded(events);
                    }
                    @Override public void onCancelled(DatabaseError error) { callback.onError(); }
                });
            }
            @Override public void onCancelled(DatabaseError error) { callback.onError(); }
        });
    }

    public void loadQualifying(String season, Integer beforeRound,
                               String codeA, String codeB,
                               predictionStatsLoader.qualifyingStatsCallback callback) {
        if (!valid(season, beforeRound, codeA, codeB)) { callback.onError(); return; }
        root.child("qualifying").child(season)
                .addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(DataSnapshot snapshot) {
                List<QualifyingRace> races = new ArrayList<>();
                for (DataSnapshot race : snapshot.getChildren()) {
                    String rawRound = race.child("round").getValue(String.class);
                    int round;
                    try {
                        if (rawRound == null) continue;
                        round = Integer.parseInt(rawRound.trim());
                    } catch (NumberFormatException error) { continue; }
                    if (round < 1 || (beforeRound != null && round >= beforeRound)) continue;
                    String name = race.child("raceName").getValue(String.class);
                    if (name == null || name.trim().isEmpty()) continue;
                    Integer a = findPosition(race.child("results"), codeA);
                    Integer b = findPosition(race.child("results"), codeB);
                    if (a == null || b == null) continue;
                    races.add(new QualifyingRace(round, new qualifyingDuelEvent(name, a, b)));
                }
                races.sort((a, b) -> Integer.compare(a.round, b.round));
                List<qualifyingDuelEvent> events = new ArrayList<>();
                for (QualifyingRace race : races) events.add(race.event);
                callback.onLoaded(events);
            }
            @Override public void onCancelled(DatabaseError error) { callback.onError(); }
        });
    }

    private static boolean valid(String season, Integer round, String a, String b) {
        return season != null && !season.trim().isEmpty()
                && a != null && !a.trim().isEmpty()
                && b != null && !b.trim().isEmpty() && !a.equals(b)
                && (round == null || round > 0);
    }
    private static Integer findPosition(DataSnapshot results, String code) {
        for (DataSnapshot result : results.getChildren()) {
            if (!code.equals(result.child("driverCode").getValue(String.class))) continue;
            Long value = result.child("position").getValue(Long.class);
            return value != null && value > 0 && value <= Integer.MAX_VALUE
                    ? value.intValue() : null;
        }
        return null;
    }
    private static String completedFinish(String raw) {
        if (raw == null) return null;
        String[] parts = raw.trim().split("-", 2);
        if (parts.length != 2) return null;
        String finish = parts[1].trim();
        return position(finish) != null || "R".equals(finish) || "W".equals(finish)
                || "D".equals(finish) || "NC".equals(finish) ? finish : null;
    }
    private static Integer position(String finish) {
        try {
            int value = Integer.parseInt(finish);
            return value > 0 ? value : null;
        } catch (NumberFormatException error) { return null; }
    }
    private static double points(Integer position) {
        if (position == null) return 0.0;
        int[] scale = {25,18,15,12,10,8,6,4,2,1};
        return position <= scale.length ? scale[position - 1] : 0.0;
    }
    private static final class Race {
        final int round; final String name;
        Race(int round, String name) { this.round = round; this.name = name; }
    }
    private static final class QualifyingRace {
        final int round; final qualifyingDuelEvent event;
        QualifyingRace(int round, qualifyingDuelEvent event) {
            this.round = round; this.event = event;
        }
    }
}
