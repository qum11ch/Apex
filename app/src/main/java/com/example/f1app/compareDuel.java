package com.example.f1app;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

final class compareDuel {

    static final class Race {
        final String name;
        final int position;
        final String text;

        Race(String name, int position, String text) {
            this.name = name;
            this.position = position;
            this.text = text;
        }
    }

    static final class Round {
        final int round;
        final String name;
        final int raceA;
        final int raceB;
        final int qualiA;
        final int qualiB;

        Round(int round, String name, int raceA, int raceB, int qualiA, int qualiB) {
            this.round = round;
            this.name = name;
            this.raceA = raceA;
            this.raceB = raceB;
            this.qualiA = qualiA;
            this.qualiB = qualiB;
        }
    }

    static final class Result {
        final List<Round> rounds = new ArrayList<>();
        int raceA;
        int raceB;
        int qualiA;
        int qualiB;
        int retiredA;
        int retiredB;
        double avgRaceA = Double.NaN;
        double avgRaceB = Double.NaN;
        double avgQualiA = Double.NaN;
        double avgQualiB = Double.NaN;
    }

    interface Callback {
        void onLoaded(Result result);
    }

    static final Result EMPTY = new Result();

    private compareDuel() {
    }

    static void load(compareParticipant a, compareParticipant b, int year, Callback callback) {
        jolpicaSeasons.driverStandings(year, standings -> {
            String idA = driverId(standings, a);
            String idB = driverId(standings, b);

            if (idA == null || idB == null) {
                callback.onLoaded(null);
                return;
            }

            JSONObject[] box = new JSONObject[4];
            int[] pending = {4};

            jolpicaSeasons.driverResults(year, idA, r -> done(box, 0, r, pending, callback));
            jolpicaSeasons.driverResults(year, idB, r -> done(box, 1, r, pending, callback));
            jolpicaSeasons.driverQualifying(year, idA, r -> done(box, 2, r, pending, callback));
            jolpicaSeasons.driverQualifying(year, idB, r -> done(box, 3, r, pending, callback));
        });
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

        callback.onLoaded(compute(
                parse(box[0], false), parse(box[1], false),
                parse(box[2], true), parse(box[3], true)));
    }

    private static String driverId(JSONObject standings, compareParticipant p) {
        if (standings == null) {
            return null;
        }

        JSONObject entry = jolpicaSeasons.driverEntry(standings, p.code, p.given, p.family);
        JSONObject driver = entry == null ? null : entry.optJSONObject("Driver");
        String id = driver == null ? "" : driver.optString("driverId", "");

        return id.isEmpty() ? null : id;
    }

    static Map<Integer, Race> parse(JSONObject response, boolean qualifying) {
        Map<Integer, Race> result = new TreeMap<>();

        if (response == null) {
            return result;
        }

        JSONObject data = response.optJSONObject("MRData");
        JSONObject table = data == null ? null : data.optJSONObject("RaceTable");
        JSONArray races = table == null ? null : table.optJSONArray("Races");

        if (races == null) {
            return result;
        }

        for (int i = 0; i < races.length(); i++) {
            JSONObject race = races.optJSONObject(i);
            JSONArray rows = race == null
                    ? null : race.optJSONArray(qualifying ? "QualifyingResults" : "Results");
            JSONObject row = rows == null ? null : rows.optJSONObject(0);

            if (row == null) {
                continue;
            }

            result.put(race.optInt("round", 0), new Race(
                    race.optString("raceName", ""),
                    row.optInt("position", 0),
                    row.optString("positionText", "")));
        }

        return result;
    }

    static Result compute(Map<Integer, Race> raceA, Map<Integer, Race> raceB,
                          Map<Integer, Race> qualiA, Map<Integer, Race> qualiB) {
        Result result = new Result();

        TreeSet<Integer> rounds = new TreeSet<>();
        rounds.addAll(raceA.keySet());
        rounds.addAll(raceB.keySet());
        rounds.addAll(qualiA.keySet());
        rounds.addAll(qualiB.keySet());

        double sumRaceA = 0, sumRaceB = 0, sumQualiA = 0, sumQualiB = 0;
        int races = 0, qualis = 0;

        for (int round : rounds) {
            Race ra = raceA.get(round);
            Race rb = raceB.get(round);
            Race qa = qualiA.get(round);
            Race qb = qualiB.get(round);

            boolean raceBoth = ra != null && rb != null && started(ra) && started(rb);
            boolean qualiBoth = qa != null && qb != null && qa.position > 0 && qb.position > 0;

            if (!raceBoth && !qualiBoth) {
                continue;
            }

            String name = firstName(ra, rb, qa, qb);
            int rpa = 0, rpb = 0, qpa = 0, qpb = 0;

            if (raceBoth) {
                rpa = ra.position;
                rpb = rb.position;

                if (rpa < rpb) {
                    result.raceA++;
                } else if (rpb < rpa) {
                    result.raceB++;
                }

                sumRaceA += rpa;
                sumRaceB += rpb;
                races++;

                if ("R".equals(ra.text)) {
                    result.retiredA++;
                }
                if ("R".equals(rb.text)) {
                    result.retiredB++;
                }
            }

            if (qualiBoth) {
                qpa = qa.position;
                qpb = qb.position;

                if (qpa < qpb) {
                    result.qualiA++;
                } else if (qpb < qpa) {
                    result.qualiB++;
                }

                sumQualiA += qpa;
                sumQualiB += qpb;
                qualis++;
            }

            result.rounds.add(new Round(round, name, rpa, rpb, qpa, qpb));
        }

        if (races > 0) {
            result.avgRaceA = sumRaceA / races;
            result.avgRaceB = sumRaceB / races;
        }

        if (qualis > 0) {
            result.avgQualiA = sumQualiA / qualis;
            result.avgQualiB = sumQualiB / qualis;
        }

        return result;
    }

    private static boolean started(Race race) {
        return race.position > 0 && !"W".equals(race.text) && !"F".equals(race.text);
    }

    private static String firstName(Race... races) {
        for (Race race : races) {
            if (race != null && !race.name.isEmpty()) {
                return race.name;
            }
        }
        return "";
    }
}