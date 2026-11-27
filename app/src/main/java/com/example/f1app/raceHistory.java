package com.example.f1app;

import androidx.annotation.NonNull;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Finds the other runnings of a Grand Prix (earlier and later seasons) that have results in the database. */
final class raceHistory {

    /** The oldest season that is looked at. Years without data simply do not appear. */
    static final int FIRST_SEASON = 2021;

    static final class Entry {
        final int year;
        final String raceName;
        final String first;
        final String second;
        final String third;

        Entry(int year, String raceName, String first, String second, String third) {
            this.year = year;
            this.raceName = raceName;
            this.first = first;
            this.second = second;
            this.third = third;
        }
    }

    interface Callback {
        /** Newest year first; empty when there is nothing to show. Called on the main thread. */
        void onLoaded(List<Entry> entries);
    }

    private raceHistory() {
    }

    /** The name the race had in earlier seasons (the schedule keys are race names). */
    static String earlierName(String raceName, String gpYear) {
        switch (raceName) {
            case "Brazilian Grand Prix":
                return "São Paulo Grand Prix";
            case "Spanish Grand Prix":
                return "2026".equals(gpYear) ? "Madrid Grand Prix" : raceName;
            case "Barcelona Grand Prix":
                return "2026".equals(gpYear) ? "Spanish Grand Prix" : raceName;
            default:
                return raceName;
        }
    }

    /** The keys the race can have in the season of the target year, the most likely one first. */
    private static List<String> candidates(String raceName, String gpYear, int target, int gp) {
        String earlier = earlierName(raceName, gpYear);
        List<String> names = new ArrayList<>();

        if (target < gp) {
            names.add(earlier);
            names.add(raceName);
        } else {
            names.add(raceName);
            names.add(earlier);
        }

        List<String> unique = new ArrayList<>();

        for (String name : names) {
            if (!unique.contains(name)) {
                unique.add(name);
            }
        }

        return unique;
    }

    static void load(String raceName, String gpYear, Callback callback) {
        final int gp;

        try {
            gp = Integer.parseInt(gpYear);
        } catch (RuntimeException error) {
            callback.onLoaded(new ArrayList<>());
            return;
        }

        final int last = Math.max(LocalDate.now().getYear(), gp);
        final List<Integer> years = new ArrayList<>();

        for (int year = last; year >= FIRST_SEASON; year--) {
            if (year != gp) {
                years.add(year);
            }
        }

        if (years.isEmpty()) {
            callback.onLoaded(new ArrayList<>());
            return;
        }

        final List<Entry> found = new ArrayList<>();
        final int[] pending = {years.size()};

        for (int season : years) {
            probe(season, candidates(raceName, gpYear, season, gp), 0, found, () -> {
                pending[0]--;

                if (pending[0] == 0) {
                    Collections.sort(found, (a, b) -> Integer.compare(b.year, a.year));
                    callback.onLoaded(found);
                }
            });
        }
    }

    /** Tries the keys one by one until the season has the race; a race without a winner is not listed. */
    private static void probe(int season, List<String> names, int index, List<Entry> found, Runnable done) {
        if (index >= names.size()) {
            done.run();
            return;
        }

        final String name = names.get(index);

        FirebaseDatabase.getInstance().getReference()
                .child("schedule").child("season").child(String.valueOf(season)).child(name)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot race) {
                        if (!race.hasChildren()) {
                            probe(season, names, index + 1, found, done);
                            return;
                        }

                        boolean cancelled = race.child("Canceled").getValue(Boolean.class) != null;
                        String winner = race.child("RaceResults/raceWinnerCode").getValue(String.class);

                        if (!cancelled && raceWeekendStatus.isValidCode(winner)) {
                            found.add(new Entry(
                                    season,
                                    name,
                                    pastRaceItem.nz(winner),
                                    pastRaceItem.nz(race.child("RaceResults/raceSecondCode").getValue(String.class)),
                                    pastRaceItem.nz(race.child("RaceResults/raceThirdCode").getValue(String.class))
                            ));
                        }

                        done.run();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        done.run();
                    }
                });
    }
}