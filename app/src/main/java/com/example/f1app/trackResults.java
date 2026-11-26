package com.example.f1app;

import androidx.annotation.NonNull;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Results of a driver or a team grouped by circuit, from the same database nodes as the season tables. */
final class trackResults {

    static final class Row {
        final int year;
        final String raceName;
        final String circuitId;
        final List<String> codes;
        final List<String> drivers;

        Row(int year, String raceName, String circuitId, List<String> codes) {
            this(year, raceName, circuitId, codes, Collections.nCopies(codes.size(), ""));
        }

        Row(int year, String raceName, String circuitId, List<String> codes, List<String> drivers) {
            this.year = year;
            this.raceName = raceName;
            this.circuitId = circuitId;
            this.codes = codes;
            this.drivers = drivers;
        }
    }

    static final class Stat {
        final String circuitId;
        String raceName = "";
        int latestYear;
        final List<Row> rows = new ArrayList<>();
        int starts;
        int wins;
        int podiums;
        int best;

        Stat(String circuitId) {
            this.circuitId = circuitId;
        }
    }

    interface Callback {
        void onLoaded(List<Stat> stats);
    }

    private trackResults() {
    }

    /** The finishing position of a "start-finish" code; -1 when it is not a position. */
    static int finish(String code) {
        if (code == null) {
            return -1;
        }

        String[] parts = code.trim().split("-");

        if (parts.length != 2) {
            return -1;
        }

        try {
            return Integer.parseInt(parts[1]);
        } catch (NumberFormatException error) {
            return -1;
        }
    }

    /** A race that was started: two parts and not withdrawn. */
    static boolean started(String code) {
        if (code == null) {
            return false;
        }

        String[] parts = code.trim().split("-");
        return parts.length == 2 && !"W".equals(parts[1]);
    }

    static List<Stat> build(List<Row> rows) {
        List<Row> ordered = new ArrayList<>(rows);
        Collections.sort(ordered, (x, y) -> Integer.compare(y.year, x.year));

        Map<String, Stat> map = new LinkedHashMap<>();

        for (Row row : ordered) {
            Stat stat = map.get(row.circuitId);

            if (stat == null) {
                stat = new Stat(row.circuitId);
                stat.raceName = row.raceName;
                stat.latestYear = row.year;
                map.put(row.circuitId, stat);
            }

            stat.rows.add(row);

            boolean any = false;

            for (String code : row.codes) {
                if (!started(code)) {
                    continue;
                }

                any = true;
                int finish = finish(code);

                if (finish == 1) {
                    stat.wins++;
                }

                if (finish > 0 && finish <= 3) {
                    stat.podiums++;
                }

                if (finish > 0 && (stat.best == 0 || finish < stat.best)) {
                    stat.best = finish;
                }
            }

            if (any) {
                stat.starts++;
            }
        }

        List<Stat> result = new ArrayList<>(map.values());

        Collections.sort(result, (x, y) -> {
            if (x.starts != y.starts) {
                return Integer.compare(y.starts, x.starts);
            }

            if (x.best != y.best) {
                if (x.best == 0) return 1;
                if (y.best == 0) return -1;
                return Integer.compare(x.best, y.best);
            }

            return x.raceName.compareTo(y.raceName);
        });

        return result;
    }

    static void load(boolean team, String key, List<Integer> seasons, Callback callback) {
        if (seasons == null || seasons.isEmpty()) {
            callback.onLoaded(new ArrayList<>());
            return;
        }

        final int count = seasons.size();
        final DataSnapshot[] schedule = new DataSnapshot[count];
        final DataSnapshot[] results = new DataSnapshot[count];
        final int[] pending = {count * 2};

        DatabaseReference root = FirebaseDatabase.getInstance().getReference();

        for (int i = 0; i < count; i++) {
            final int index = i;
            String season = String.valueOf(seasons.get(i));

            root.child("schedule").child("season").child(season)
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            schedule[index] = snapshot;
                            finish(team, key, seasons, schedule, results, pending, callback);
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            finish(team, key, seasons, schedule, results, pending, callback);
                        }
                    });

            DatabaseReference resultRef = root.child("results").child("season").child(season);

            if (!team) {
                resultRef = resultRef.child(key);
            }

            resultRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    results[index] = snapshot;
                    finish(team, key, seasons, schedule, results, pending, callback);
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    finish(team, key, seasons, schedule, results, pending, callback);
                }
            });
        }
    }

    private static void finish(boolean team, String key, List<Integer> seasons,
                               DataSnapshot[] schedule, DataSnapshot[] results,
                               int[] pending, Callback callback) {
        if (--pending[0] > 0) {
            return;
        }

        List<Row> rows = new ArrayList<>();

        for (int i = 0; i < seasons.size(); i++) {
            DataSnapshot races = schedule[i];
            DataSnapshot season = results[i];

            if (races == null || season == null) {
                continue;
            }

            for (DataSnapshot race : races.getChildren()) {
                String raceName = race.getKey();
                String circuit = race.child("Circuit").child("circuitId").getValue(String.class);

                if (raceName == null || circuit == null) {
                    continue;
                }

                List<String> codes = new ArrayList<>();
                List<String> names = new ArrayList<>();

                if (team) {
                    for (DataSnapshot driver : season.getChildren()) {
                        DataSnapshot entry = driver.child(raceName);

                        if (key.equals(entry.child("TeamId").getValue(String.class))) {
                            addIfStarted(codes, names, entry.child("Result").getValue(String.class),
                                    driver.getKey());
                        }
                    }
                } else {
                    addIfStarted(codes, names, season.child(raceName).child("Result").getValue(String.class), key);
                }

                if (!codes.isEmpty()) {
                    sortByFinish(codes, names);
                    rows.add(new Row(seasons.get(i), raceName, circuit, codes, names));
                }
            }
        }

        callback.onLoaded(build(rows));
    }

    private static void addIfStarted(List<String> codes, List<String> names, String code, String name) {
        if (started(code)) {
            codes.add(code.trim());
            names.add(name == null ? "" : name);
        }
    }

    /** Better finishes first; the names keep their pairs. */
    private static void sortByFinish(List<String> codes, List<String> names) {
        List<Integer> order = new ArrayList<>();

        for (int i = 0; i < codes.size(); i++) {
            order.add(i);
        }

        Collections.sort(order, (x, y) -> {
            int fx = finish(codes.get(x));
            int fy = finish(codes.get(y));

            if (fx > 0 && fy > 0) return Integer.compare(fx, fy);
            if (fx > 0) return -1;
            if (fy > 0) return 1;
            return 0;
        });

        List<String> sortedCodes = new ArrayList<>();
        List<String> sortedNames = new ArrayList<>();

        for (int index : order) {
            sortedCodes.add(codes.get(index));
            sortedNames.add(names.get(index));
        }

        codes.clear();
        codes.addAll(sortedCodes);
        names.clear();
        names.addAll(sortedNames);
    }
}