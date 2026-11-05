package com.example.f1app;

import android.os.Handler;
import android.os.Looper;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONArray;
import org.json.JSONObject;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class predictionWeekendPointsLoader {
    public interface Callback {
        void onLoaded(Data data);

        void onError(String message);
    }

    public static final class Row {
        public final String raceName;
        public final int round;
        public final boolean hasSprint;

        public final double raceA;
        public final double sprintA;
        public final double raceB;
        public final double sprintB;

        Row(String raceName, int round, boolean hasSprint, double raceA, double sprintA, double raceB, double sprintB) {
            this.raceName = raceName;
            this.round = round;
            this.hasSprint = hasSprint;
            this.raceA = raceA;
            this.sprintA = sprintA;
            this.raceB = raceB;
            this.sprintB = sprintB;

            predictionWeekendRules.weekend(raceA, sprintA, hasSprint);
            predictionWeekendRules.weekend(raceB, sprintB, hasSprint);
        }

        public double totalA() {
            return predictionWeekendRules.weekend(raceA, sprintA, hasSprint);
        }

        public double totalB() {
            return predictionWeekendRules.weekend(raceB, sprintB, hasSprint);
        }
    }

    public static final class Data {
        public final List<Row> rows;
        public final int skipped;
        public final int calendarMismatches;

        public Data() {
            this(Collections.emptyList(), 0, 0);
        }

        Data(List<Row> rows, int skipped, int calendarMismatches) {
            this.rows = Collections.unmodifiableList(new ArrayList<>(rows));
            this.skipped = skipped;
            this.calendarMismatches = calendarMismatches;
        }

        public double sumA() {
            double sum = 0;

            for (Row row : rows) {
                sum += row.totalA();
            }

            return sum;
        }

        public double sumB() {
            double sum = 0;

            for (Row row : rows) {
                sum += row.totalB();
            }

            return sum;
        }
    }

    private static final class Race {
        final int round;
        final String name;
        final Boolean sprint;

        Race(int round, String name, Boolean sprint) {
            this.round = round;
            this.name = name;
            this.sprint = sprint;
        }
    }

    private static final class Session {
        final Map<String, JSONObject> entries = new LinkedHashMap<>();
    }

    private static final ExecutorService IO = Executors.newFixedThreadPool(2);
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    public void load(String season, int throughRound, boolean teams, String idA, String idB, Callback callback) {
        if (idA != null && idA.equals(idB)) {
            MAIN.post(() -> callback.onError("Equal participants"));

            return;
        }

        start(season, throughRound, teams, idA, idB, callback);
    }

    public void loadSingle(String season, int throughRound, boolean team, String id, Callback callback) {
        start(season, throughRound, team, id, id, callback);
    }

    private void start(String season, int throughRound, boolean teams, String idA, String idB, Callback callback) {
        if (season == null || !season.matches("\\d{4}") || throughRound < 0) {
            MAIN.post(() -> callback.onError("Invalid period"));

            return;
        }

        if (throughRound == 0) {
            MAIN.post(() -> callback.onLoaded(new Data()));

            return;
        }

        if (idA == null || idA.isEmpty() || idB == null || idB.isEmpty()) {
            MAIN.post(() -> callback.onError("Missing Jolpica IDs"));

            return;
        }

        FirebaseDatabase.getInstance().getReference("schedule").child("season").child(season)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        try {
                            List<Race> calendar = collectCalendar(snapshot, throughRound);

                            IO.execute(() -> fetch(season, throughRound, teams, idA, idB, calendar, callback));
                        } catch (Exception error) {
                            callback.onError(error.toString());
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        callback.onError(error.getMessage());
                    }
                });
    }

    private static List<Race> collectCalendar(DataSnapshot snapshot, int throughRound) throws Exception {
        List<Race> calendar = new ArrayList<>();
        Set<Integer> seenRounds = new HashSet<>();

        for (DataSnapshot race : snapshot.getChildren()) {
            Long round = race.child("round").getValue(Long.class);

            if (round == null || round < 1 || round > throughRound || race.getKey() == null) {
                continue;
            }

            if (!seenRounds.add(round.intValue())) {
                throw new Exception("Duplicate calendar round " + round);
            }

            calendar.add(new Race(round.intValue(), race.getKey(), sprintFlag(race)));
        }

        // A number gap may be deliberate cancellation; retain a strict error rather than silently call it full.
        for (int round = 1; round <= throughRound; round++) {
            if (!seenRounds.contains(round)) {
                throw new Exception("Calendar missing round " + round);
            }
        }

        calendar.sort((left, right) -> Integer.compare(left.round, right.round));

        return calendar;
    }

    private static Boolean sprintFlag(DataSnapshot race) {
        String raw = race.child("Sprint").child("sprintRaceDate").getValue(String.class);

        if (raw == null || raw.trim().isEmpty()) {
            return null;
        }

        String value = raw.trim();

        if (value.equalsIgnoreCase("N/A") || value.equalsIgnoreCase("NA")) {
            return false;
        }

        try {
            LocalDate.parse(value);

            return true;
        } catch (Exception exception) {
            return null;
        }
    }

    private static void fetch(
            String season,
            int throughRound,
            boolean teams,
            String idA,
            String idB,
            List<Race> calendar,
            Callback callback
    ) {
        try {
            String base = "https://api.jolpi.ca/ergast/f1/" + season + "/";
            String filter = teams ? "constructors/" : "drivers/";

            Map<Integer, Session> raceA = sessions(base + filter + idA + "/results/", "Results", season, throughRound);
            Map<Integer, Session> raceB = idA.equals(idB) ? raceA : sessions(base + filter + idB + "/results/", "Results", season, throughRound);

            Map<Integer, Session> sprintA = sessions(base + filter + idA + "/sprint/", "SprintResults", season, throughRound);
            Map<Integer, Session> sprintB = idA.equals(idB) ? sprintA : sessions(base + filter + idB + "/sprint/", "SprintResults", season, throughRound);

            // Small shared responses establish session existence even when both selected drivers did not compete.
            Map<Integer, Session> completedRaces = sessions(base + "results/1/", "Results", season, throughRound);
            Map<Integer, Session> completedSprints = sessions(base + "sprint/1/", "SprintResults", season, throughRound);

            List<Row> rows = new ArrayList<>();
            int skipped = 0;
            int mismatches = 0;

            for (Race race : calendar) {
                boolean raceComplete = present(completedRaces, race.round);
                boolean sprintComplete = present(completedSprints, race.round);

                predictionWeekendRules.Decision decision = predictionWeekendRules.decide(raceComplete, race.sprint, sprintComplete);

                if (!decision.included) {
                    skipped++;

                    continue;
                }

                if (decision.calendarMismatch) {
                    mismatches++;
                }

                rows.add(new Row(
                        race.name,
                        race.round,
                        decision.hasSprint,
                        points(raceA.get(race.round)),
                        decision.hasSprint ? points(sprintA.get(race.round)) : 0,
                        points(raceB.get(race.round)),
                        decision.hasSprint ? points(sprintB.get(race.round)) : 0
                ));
            }

            Data data = new Data(rows, skipped, mismatches);

            MAIN.post(() -> callback.onLoaded(data));
        } catch (Exception error) {
            MAIN.post(() -> callback.onError(error.toString()));
        }
    }

    private static boolean present(Map<Integer, Session> sessions, int round) {
        Session session = sessions.get(round);

        return session != null && !session.entries.isEmpty();
    }

    private static double points(Session session) throws Exception {
        double sum = 0;

        if (session == null) {
            // Complete filtered response: no entry means no awarded points
            return sum;
        }

        for (JSONObject entry : session.entries.values()) {
            double points = entry.getDouble("points");

            if (!Double.isFinite(points)) {
                throw new Exception("Invalid points");
            }

            sum += points;
        }

        return sum;
    }

    private static Map<Integer, Session> sessions(String url, String field, String season, int throughRound) throws Exception {
        Map<Integer, Session> result = new TreeMap<>();
        int offset = 0;

        while (true) {
            JSONObject response = predictionPointsJson.object(url + "?format=json&limit=100&offset=" + offset);
            JSONObject metadata = response.getJSONObject("MRData");
            JSONArray races = metadata.getJSONObject("RaceTable").getJSONArray("Races");

            collectSessions(races, field, season, throughRound, result);

            int limit = metadata.getInt("limit");
            int total = metadata.getInt("total");

            if (limit <= 0 || (races.length() == 0 && offset < total)) {
                throw new Exception("Incomplete results pagination");
            }

            if (offset + limit >= total) {
                break;
            }

            offset += limit;
        }

        return result;
    }

    private static void collectSessions(
            JSONArray races,
            String field,
            String season,
            int throughRound,
            Map<Integer, Session> result
    ) throws Exception {
        for (int i = 0; i < races.length(); i++) {
            JSONObject race = races.getJSONObject(i);

            if (!season.equals(race.getString("season"))) {
                throw new Exception("Season mismatch");
            }

            int round = race.getInt("round");

            if (round > throughRound) {
                continue;
            }

            Session session = result.get(round);

            if (session == null) {
                session = new Session();

                result.put(round, session);
            }

            JSONArray entries = race.getJSONArray(field);

            for (int j = 0; j < entries.length(); j++) {
                JSONObject entry = entries.getJSONObject(j);
                String id = entry.getJSONObject("Driver").getString("driverId");

                JSONObject previous = session.entries.get(id);

                if (previous != null && Double.compare(previous.getDouble("points"), entry.getDouble("points")) != 0) {
                    throw new Exception("Points changed between pages; refresh");
                }

                session.entries.put(id, entry);
            }
        }
    }
}