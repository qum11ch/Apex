package com.example.f1app;

import org.json.JSONArray;
import org.json.JSONObject;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Both participants on one circuit, year by year. */
final class compareTrack {

    static final class Circuit {
        final String id;
        final String name;
        final String place;

        Circuit(String id, String name, String place) {
            this.id = id;
            this.name = name;
            this.place = place;
        }
    }

    static final class Setup {
        boolean teams;
        String idA;
        String idB;
        final List<Circuit> circuits = new ArrayList<>();
        String defaultCircuit = "";

        Circuit find(String id) {
            for (Circuit circuit : circuits) {
                if (circuit.id.equals(id)) {
                    return circuit;
                }
            }
            return null;
        }
    }

    interface Callback<T> {
        void onLoaded(T result);
    }

    private compareTrack() {
    }

    static void prepare(compareParticipant a, compareParticipant b, Callback<Setup> callback) {
        int now = LocalDate.now().getYear();
        String[] ids = new String[2];
        String[] next = {""};
        int[] pending = {3};

        Runnable proceed = () -> {
            if (--pending[0] > 0) {
                return;
            }

            if (ids[0] == null || ids[1] == null) {
                callback.onLoaded(null);
                return;
            }

            loadCircuits(a.type == compareParticipant.TEAM, ids[0], ids[1], next[0], callback);
        };

        resolveId(a, Math.min(Math.max(a.lastYear, 1950), now), id -> {
            ids[0] = id;
            proceed.run();
        });
        resolveId(b, Math.min(Math.max(b.lastYear, 1950), now), id -> {
            ids[1] = id;
            proceed.run();
        });

        jolpicaSeasons.nextRace(response -> {
            next[0] = nextCircuit(response);
            proceed.run();
        });
    }

    private static void resolveId(compareParticipant p, int season, Callback<String> callback) {
        if (p.type == compareParticipant.TEAM) {
            jolpicaSeasons.constructorStandings(season,
                    s -> callback.onLoaded(compareDuel.constructorId(s, p)));
        } else {
            jolpicaSeasons.driverStandings(season,
                    s -> callback.onLoaded(compareDuel.driverId(s, p)));
        }
    }

    private static String nextCircuit(JSONObject response) {
        JSONObject data = response == null ? null : response.optJSONObject("MRData");
        JSONObject table = data == null ? null : data.optJSONObject("RaceTable");
        JSONArray races = table == null ? null : table.optJSONArray("Races");
        JSONObject race = races == null ? null : races.optJSONObject(0);
        JSONObject circuit = race == null ? null : race.optJSONObject("Circuit");

        return circuit == null ? "" : circuit.optString("circuitId", "");
    }

    private static void loadCircuits(boolean teams, String idA, String idB, String next,
                                     Callback<Setup> callback) {
        JSONObject[] box = new JSONObject[2];
        int[] pending = {2};

        jolpicaSeasons.Callback first = r -> circuitsDone(box, 0, r, pending, teams, idA, idB, next, callback);
        jolpicaSeasons.Callback second = r -> circuitsDone(box, 1, r, pending, teams, idA, idB, next, callback);

        if (teams) {
            jolpicaSeasons.constructorCircuits(idA, first);
            jolpicaSeasons.constructorCircuits(idB, second);
        } else {
            jolpicaSeasons.driverCircuits(idA, first);
            jolpicaSeasons.driverCircuits(idB, second);
        }
    }

    private static void circuitsDone(JSONObject[] box, int index, JSONObject response, int[] pending,
                                     boolean teams, String idA, String idB, String next,
                                     Callback<Setup> callback) {
        box[index] = response;

        if (--pending[0] > 0) {
            return;
        }

        if (box[0] == null || box[1] == null) {
            callback.onLoaded(null);
            return;
        }

        List<Circuit> first = circuits(box[0]);
        Set<String> second = new HashSet<>();

        for (Circuit circuit : circuits(box[1])) {
            second.add(circuit.id);
        }

        Setup setup = new Setup();
        setup.teams = teams;
        setup.idA = idA;
        setup.idB = idB;

        for (Circuit circuit : first) {
            if (second.contains(circuit.id)) {
                setup.circuits.add(circuit);
            }
        }

        Collections.sort(setup.circuits, (x, y) -> x.name.compareToIgnoreCase(y.name));

        if (setup.find(next) != null) {
            setup.defaultCircuit = next;
        } else if (!setup.circuits.isEmpty()) {
            setup.defaultCircuit = setup.circuits.get(0).id;
        }

        callback.onLoaded(setup);
    }

    private static List<Circuit> circuits(JSONObject response) {
        List<Circuit> result = new ArrayList<>();

        JSONObject data = response == null ? null : response.optJSONObject("MRData");
        JSONObject table = data == null ? null : data.optJSONObject("CircuitTable");
        JSONArray list = table == null ? null : table.optJSONArray("Circuits");

        if (list == null) {
            return result;
        }

        for (int i = 0; i < list.length(); i++) {
            JSONObject item = list.optJSONObject(i);

            if (item == null) {
                continue;
            }

            JSONObject location = item.optJSONObject("Location");
            String place = location == null ? "" : location.optString("locality", "");

            result.add(new Circuit(item.optString("circuitId", ""),
                    item.optString("circuitName", ""), place));
        }

        return result;
    }

    static void load(Setup setup, String circuitId, Callback<compareDuel.Result> callback) {
        JSONObject[] box = new JSONObject[4];
        int[] pending = {4};

        jolpicaSeasons.Callback r0 = r -> done(box, 0, r, pending, setup.teams, callback);
        jolpicaSeasons.Callback r1 = r -> done(box, 1, r, pending, setup.teams, callback);
        jolpicaSeasons.Callback q0 = r -> done(box, 2, r, pending, setup.teams, callback);
        jolpicaSeasons.Callback q1 = r -> done(box, 3, r, pending, setup.teams, callback);

        if (setup.teams) {
            jolpicaSeasons.circuitConstructorResults(circuitId, setup.idA, r0);
            jolpicaSeasons.circuitConstructorResults(circuitId, setup.idB, r1);
            jolpicaSeasons.circuitConstructorQualifying(circuitId, setup.idA, q0);
            jolpicaSeasons.circuitConstructorQualifying(circuitId, setup.idB, q1);
        } else {
            jolpicaSeasons.circuitDriverResults(circuitId, setup.idA, r0);
            jolpicaSeasons.circuitDriverResults(circuitId, setup.idB, r1);
            jolpicaSeasons.circuitDriverQualifying(circuitId, setup.idA, q0);
            jolpicaSeasons.circuitDriverQualifying(circuitId, setup.idB, q1);
        }
    }

    private static void done(JSONObject[] box, int index, JSONObject response, int[] pending,
                             boolean teams, Callback<compareDuel.Result> callback) {
        box[index] = response;

        if (--pending[0] > 0) {
            return;
        }

        if (box[0] == null || box[1] == null) {
            callback.onLoaded(null);
            return;
        }

        if (teams) {
            callback.onLoaded(compareDuel.computeTeams(
                    compareDuel.parseTeam(box[0], false, "season"),
                    compareDuel.parseTeam(box[1], false, "season"),
                    compareDuel.parseTeam(box[2], true, "season"),
                    compareDuel.parseTeam(box[3], true, "season")));
        } else {
            callback.onLoaded(compareDuel.compute(
                    compareDuel.parse(box[0], false, "season"),
                    compareDuel.parse(box[1], false, "season"),
                    compareDuel.parse(box[2], true, "season"),
                    compareDuel.parse(box[3], true, "season")));
        }
    }
}