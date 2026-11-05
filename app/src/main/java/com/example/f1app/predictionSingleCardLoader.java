package com.example.f1app;

import android.util.Log;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class predictionSingleCardLoader {
    public interface Callback {
        void onLoaded(Data data);

        void onError();
    }

    public static final class Row {
        public final String raceName;
        public final List<String> results;

        Row(String raceName, List<String> results) {
            this.raceName = raceName;
            this.results = Collections.unmodifiableList(new ArrayList<>(results));
        }
    }

    public static final class Data {
        public String title;
        public String season;
        public String targetRace;
        public String targetDate;

        public boolean team;
        public boolean historyLoading;
        public boolean historyError;

        public double points;

        public int wins;
        public int podiums;
        public int retirements;
        public int throughRound;

        public final List<Row> rows = new ArrayList<>();

        public predictionWeekendPointsLoader.Data history;
    }

    private static final List<String> NON_NUMERIC_RESULTS = Arrays.asList("R", "W", "D", "NC");

    private final DatabaseReference root = FirebaseDatabase.getInstance().getReference();
    private final jolpicaPointsLoader standings = new jolpicaPointsLoader();
    private final predictionWeekendPointsLoader weekends = new predictionWeekendPointsLoader();

    public void load(String season, Integer beforeRound, boolean team, String id, String title, Callback callback) {
        if (beforeRound != null && beforeRound < 1) {
            callback.onError();

            return;
        }

        Integer standingsRound = beforeRound == null ? null : beforeRound - 1;

        standings.loadSingle(team, season, standingsRound, id, title, new jolpicaPointsLoader.Callback() {
            @Override
            public void onError(String message) {
                Log.e("SingleCard", "standings failed: " + message);
                callback.onError();
            }

            @Override
            public void onLoaded(jolpicaPointsLoader.Result total) {
                Data data = createData(title, season, team, total);

                loadCalendar(season, beforeRound, team, id, title, total, data, callback);
            }
        });
    }

    private static Data createData(String title, String season, boolean team, jolpicaPointsLoader.Result total) {
        Data data = new Data();

        data.title = title;
        data.season = season;
        data.team = team;
        data.points = total.pointsA;
        data.throughRound = total.round;

        return data;
    }

    private void loadCalendar(
            String season,
            Integer beforeRound,
            boolean team,
            String id,
            String title,
            jolpicaPointsLoader.Result total,
            Data data,
            Callback callback
    ) {
        root.child("schedule").child("season").child(season).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot calendar) {
                List<DataSnapshot> races = collectRaces(calendar, beforeRound, total, data);

                if (beforeRound != null && data.targetRace == null) {
                    Log.e("SingleCard", "target race not found: round=" + beforeRound);
                    callback.onError();

                    return;
                }

                loadResults(season, team, id, title, total, data, callback, races);
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Log.e("SingleCard", "firebase cancelled: " + error.getMessage());
                callback.onError();
            }
        });
    }

    private static List<DataSnapshot> collectRaces(DataSnapshot calendar, Integer beforeRound, jolpicaPointsLoader.Result total, Data data) {
        List<DataSnapshot> races = new ArrayList<>();

        for (DataSnapshot race : calendar.getChildren()) {
            Long round = race.child("round").getValue(Long.class);

            if (round == null || race.getKey() == null) {
                continue;
            }

            if (beforeRound != null && round.longValue() == beforeRound.longValue()) {
                data.targetRace = race.getKey();
                data.targetDate = race.child("raceDate").getValue(String.class);
            }

            if (round > 0 && round <= total.round) {
                races.add(race);
            }
        }

        races.sort((left, right) -> Long.compare(
                left.child("round").getValue(Long.class),
                right.child("round").getValue(Long.class)
        ));

        return races;
    }

    private void loadResults(
            String season,
            boolean team,
            String id,
            String title,
            jolpicaPointsLoader.Result total,
            Data data,
            Callback callback,
            List<DataSnapshot> races
    ) {
        root.child("results").child("season").child(season).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot results) {
                for (DataSnapshot race : races) {
                    String raceName = race.getKey();
                    List<String> finishes = collectFinishes(results, raceName, team, id, title, data);

                    if (!finishes.isEmpty()) {
                        data.rows.add(new Row(raceName, finishes));
                    }
                }

                finishLoading(season, team, total, data, callback);
            }

            @Override
            public void onCancelled(DatabaseError error) {
                callback.onError();
            }
        });
    }

    private static List<String> collectFinishes(
            DataSnapshot results,
            String raceName,
            boolean team,
            String id,
            String title,
            Data data
    ) {
        List<String> finishes = new ArrayList<>();

        for (DataSnapshot driver : results.getChildren()) {
            if (!team && !title.equals(driver.getKey())) {
                continue;
            }

            DataSnapshot record = driver.child(raceName);

            if (team && !id.equals(record.child("TeamId").getValue(String.class))) {
                continue;
            }

            String rawResult = record.child("Result").getValue(String.class);

            if (rawResult == null) {
                continue;
            }

            String[] parts = rawResult.trim().split("-", 2);

            if (parts.length != 2) {
                continue;
            }

            String finish = parts[1].trim();
            Integer position = position(finish);

            if (position == null && !NON_NUMERIC_RESULTS.contains(finish)) {
                continue;
            }

            finishes.add(finish);

            if (position != null && position == 1) {
                data.wins++;
            }

            if (position != null && position <= 3) {
                data.podiums++;
            }

            if ("R".equals(finish)) {
                data.retirements++;
            }
        }

        return finishes;
    }

    private void finishLoading(String season, boolean team, jolpicaPointsLoader.Result total, Data data, Callback callback) {
        data.historyLoading = total.round > 0;

        if (total.round == 0) {
            data.history = new predictionWeekendPointsLoader.Data();

            callback.onLoaded(data);

            return;
        }

        callback.onLoaded(data);

        weekends.loadSingle(season, total.round, team, total.idA, new predictionWeekendPointsLoader.Callback() {
            @Override
            public void onLoaded(predictionWeekendPointsLoader.Data history) {
                data.historyLoading = false;
                data.history = history;

                callback.onLoaded(data);
            }

            @Override
            public void onError(String message) {
                data.historyLoading = false;
                data.historyError = true;

                callback.onLoaded(data);
            }
        });
    }

    private static Integer position(String value) {
        try {
            int position = Integer.parseInt(value);

            return position > 0 ? position : null;
        } catch (Exception exception) {
            return null;
        }
    }
}