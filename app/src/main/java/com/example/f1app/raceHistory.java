package com.example.f1app;

import androidx.annotation.NonNull;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Finds the earlier runnings of a Grand Prix that have results in the database. */
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

    static void load(String raceName, String gpYear, Callback callback) {
        int gp;

        try {
            gp = Integer.parseInt(gpYear);
        } catch (RuntimeException error) {
            callback.onLoaded(new ArrayList<>());
            return;
        }

        final String earlier = earlierName(raceName, gpYear);
        final int total = gp - FIRST_SEASON;

        if (total <= 0) {
            callback.onLoaded(new ArrayList<>());
            return;
        }

        final List<Entry> found = new ArrayList<>();
        final int[] pending = {total};

        for (int year = gp - 1; year >= FIRST_SEASON; year--) {
            final int season = year;

            FirebaseDatabase.getInstance().getReference()
                    .child("schedule").child("season").child(String.valueOf(season)).child(earlier)
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot race) {
                            boolean cancelled = race.child("Canceled").getValue(Boolean.class) != null;

                            if (race.hasChildren() && !cancelled) {
                                found.add(new Entry(
                                        season,
                                        earlier,
                                        pastRaceItem.nz(race.child("RaceResults/raceWinnerCode").getValue(String.class)),
                                        pastRaceItem.nz(race.child("RaceResults/raceSecondCode").getValue(String.class)),
                                        pastRaceItem.nz(race.child("RaceResults/raceThirdCode").getValue(String.class))
                                ));
                            }

                            finish();
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            finish();
                        }

                        private void finish() {
                            pending[0]--;

                            if (pending[0] == 0) {
                                Collections.sort(found, (a, b) -> Integer.compare(b.year, a.year));
                                callback.onLoaded(found);
                            }
                        }
                    });
        }
    }
}
