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

        Data(double[] a, double[] b, String[] names) {
            this.a = a;
            this.b = b;
            this.names = names;
        }
    }

    interface Callback {
        void onLoaded(Data data);
    }

    static final Data EMPTY = new Data(new double[0], new double[0], new String[0]);

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

        return new Data(a, b, labels);
    }
}