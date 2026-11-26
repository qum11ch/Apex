package com.example.f1app;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

/** Points of two participants after every round of a season. */
final class compareSeries {

    static final class Data {
        final double[] a;
        final double[] b;
        final String[] names;
        final String teamId;

        Data(double[] a, double[] b, String[] names, String teamId) {
            this.a = a;
            this.b = b;
            this.names = names;
            this.teamId = teamId;
        }
    }

    static final Data EMPTY = new Data(new double[0], new double[0], new String[0], "");

    interface Callback {
        void onLoaded(Data data);
    }

    private compareSeries() {
    }

    static void load(compareParticipant a, compareParticipant b, int year, Callback callback) {
        boolean teams = a.type == compareParticipant.TEAM;

        if (teams) {
            jolpicaSeasons.constructorStandings(year, standings ->
                    start(compareDuel.constructorId(standings, a),
                            compareDuel.constructorId(standings, b), year, true, callback));
        } else {
            jolpicaSeasons.driverStandings(year, standings ->
                    start(compareDuel.driverId(standings, a),
                            compareDuel.driverId(standings, b), year, false, callback));
        }
    }

    private static void start(String idA, String idB, int year, boolean teams, Callback callback) {
        if (idA == null || idB == null) {
            callback.onLoaded(null);
            return;
        }

        JSONObject[] box = new JSONObject[4];
        int[] pending = {4};

        if (teams) {
            jolpicaSeasons.constructorResults(year, idA, r -> done(box, 0, r, pending, callback));
            jolpicaSeasons.constructorResults(year, idB, r -> done(box, 1, r, pending, callback));
            jolpicaSeasons.constructorSprint(year, idA, r -> done(box, 2, r, pending, callback));
            jolpicaSeasons.constructorSprint(year, idB, r -> done(box, 3, r, pending, callback));
        } else {
            jolpicaSeasons.driverResults(year, idA, r -> done(box, 0, r, pending, callback));
            jolpicaSeasons.driverResults(year, idB, r -> done(box, 1, r, pending, callback));
            jolpicaSeasons.driverSprint(year, idA, r -> done(box, 2, r, pending, callback));
            jolpicaSeasons.driverSprint(year, idB, r -> done(box, 3, r, pending, callback));
        }
    }

    private static void done(JSONObject[] box, int index, JSONObject response, int[] pending,
                             Callback callback) {
        box[index] = response;

        if (--pending[0] > 0) {
            return;
        }

        if (box[0] == null || box[1] == null) {
            callback.onLoaded(null);
            return;
        }

        Map<Integer, Double> pointsA = new HashMap<>();
        Map<Integer, Double> pointsB = new HashMap<>();
        Map<Integer, String> names = new HashMap<>();

        add(pointsA, names, box[0], "Results");
        add(pointsB, names, box[1], "Results");
        add(pointsA, names, box[2], "SprintResults");
        add(pointsB, names, box[3], "SprintResults");

        callback.onLoaded(compute(pointsA, pointsB, names));
    }

    private static void add(Map<Integer, Double> points, Map<Integer, String> names,
                            JSONObject response, String rowsKey) {
        JSONObject data = response == null ? null : response.optJSONObject("MRData");
        JSONObject table = data == null ? null : data.optJSONObject("RaceTable");
        JSONArray races = table == null ? null : table.optJSONArray("Races");

        if (races == null) {
            return;
        }

        for (int i = 0; i < races.length(); i++) {
            JSONObject race = races.optJSONObject(i);
            JSONArray rows = race == null ? null : race.optJSONArray(rowsKey);

            if (rows == null) {
                continue;
            }

            int round = race.optInt("round", 0);
            double sum = 0;

            for (int j = 0; j < rows.length(); j++) {
                JSONObject row = rows.optJSONObject(j);

                if (row != null) {
                    sum += row.optDouble("points", 0);
                }
            }

            if (round > 0) {
                points.merge(round, sum, Double::sum);
                names.putIfAbsent(round, race.optString("raceName", ""));
            }
        }
    }

    static Data compute(Map<Integer, Double> pointsA, Map<Integer, Double> pointsB,
                        Map<Integer, String> names) {
        int rounds = 0;

        for (int round : pointsA.keySet()) {
            rounds = Math.max(rounds, round);
        }
        for (int round : pointsB.keySet()) {
            rounds = Math.max(rounds, round);
        }

        if (rounds == 0) {
            return EMPTY;
        }

        double[] a = new double[rounds];
        double[] b = new double[rounds];
        String[] labels = new String[rounds];
        double sumA = 0;
        double sumB = 0;

        for (int round = 1; round <= rounds; round++) {
            sumA += pointsA.getOrDefault(round, 0.0);
            sumB += pointsB.getOrDefault(round, 0.0);
            a[round - 1] = sumA;
            b[round - 1] = sumB;
            labels[round - 1] = names.getOrDefault(round, "");
        }

        return new Data(a, b, labels, "");
    }

    static void loadOne(compareParticipant p, int year, Callback callback) {
        if (p.type == compareParticipant.TEAM) {
            jolpicaSeasons.constructorStandings(year, standings ->
                    startOne(compareDuel.constructorId(standings, p), "", year, true, callback));
            return;
        }

        jolpicaSeasons.driverStandings(year, standings -> {
            JSONObject entry = standings == null ? null
                    : jolpicaSeasons.driverEntry(standings, p.code, p.given, p.family);
            JSONObject driver = entry == null ? null : entry.optJSONObject("Driver");
            String id = driver == null ? "" : driver.optString("driverId", "");

            JSONArray teams = entry == null ? null : entry.optJSONArray("Constructors");
            JSONObject last = teams == null || teams.length() == 0
                    ? null : teams.optJSONObject(teams.length() - 1);
            String teamId = last == null ? "" : last.optString("constructorId", "");

            startOne(id.isEmpty() ? null : id, teamId, year, false, callback);
        });
    }

    private static void startOne(String id, String teamId, int year, boolean team, Callback callback) {
        if (id == null) {
            callback.onLoaded(null);
            return;
        }

        JSONObject[] box = new JSONObject[2];
        int[] pending = {2};

        jolpicaSeasons.Callback results = r -> doneOne(box, 0, r, pending, teamId, callback);
        jolpicaSeasons.Callback sprint = r -> doneOne(box, 1, r, pending, teamId, callback);

        if (team) {
            jolpicaSeasons.constructorResults(year, id, results);
            jolpicaSeasons.constructorSprint(year, id, sprint);
        } else {
            jolpicaSeasons.driverResults(year, id, results);
            jolpicaSeasons.driverSprint(year, id, sprint);
        }
    }

    private static void doneOne(JSONObject[] box, int index, JSONObject response, int[] pending,
                                String teamId, Callback callback) {
        box[index] = response;

        if (--pending[0] > 0) {
            return;
        }

        if (box[0] == null) {
            callback.onLoaded(null);
            return;
        }

        Map<Integer, Double> points = new HashMap<>();
        Map<Integer, String> names = new HashMap<>();

        add(points, names, box[0], "Results");
        add(points, names, box[1], "SprintResults");

        Data data = compute(points, new HashMap<>(), names);
        callback.onLoaded(new Data(data.a, new double[0], data.names, teamId));
    }
}