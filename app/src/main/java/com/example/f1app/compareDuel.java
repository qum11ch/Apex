package com.example.f1app;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

final class compareDuel {

    /** One driver at one round. */
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

    /** One team at one round: the best car, the points of both cars, the retirements. */
    static final class Entry {
        final String name;
        final int best;
        final double points;
        final int retired;

        Entry(String name, int best, double points, int retired) {
            this.name = name;
            this.best = best;
            this.points = points;
            this.retired = retired;
        }
    }

    static final class Round {
        final int round;
        final String name;
        final int raceA;
        final int raceB;
        final int qualiA;
        final int qualiB;
        final double pointsA;
        final double pointsB;

        Round(int round, String name, int raceA, int raceB, int qualiA, int qualiB,
              double pointsA, double pointsB) {
            this.round = round;
            this.name = name;
            this.raceA = raceA;
            this.raceB = raceB;
            this.qualiA = qualiA;
            this.qualiB = qualiB;
            this.pointsA = pointsA;
            this.pointsB = pointsB;
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
        double pointsA = Double.NaN;
        double pointsB = Double.NaN;
    }

    interface Callback {
        void onLoaded(Result result);
    }

    static final Result EMPTY = new Result();

    private compareDuel() {
    }

    // ---------- loading ----------

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

            jolpicaSeasons.driverResults(year, idA, r -> done(box, 0, r, pending, callback, false));
            jolpicaSeasons.driverResults(year, idB, r -> done(box, 1, r, pending, callback, false));
            jolpicaSeasons.driverQualifying(year, idA, r -> done(box, 2, r, pending, callback, false));
            jolpicaSeasons.driverQualifying(year, idB, r -> done(box, 3, r, pending, callback, false));
        });
    }

    static void loadTeams(compareParticipant a, compareParticipant b, int year, Callback callback) {
        jolpicaSeasons.constructorStandings(year, standings -> {
            String idA = constructorId(standings, a);
            String idB = constructorId(standings, b);

            if (idA == null || idB == null) {
                callback.onLoaded(null);
                return;
            }

            JSONObject[] box = new JSONObject[4];
            int[] pending = {4};

            jolpicaSeasons.constructorResults(year, idA, r -> done(box, 0, r, pending, callback, true));
            jolpicaSeasons.constructorResults(year, idB, r -> done(box, 1, r, pending, callback, true));
            jolpicaSeasons.constructorQualifying(year, idA, r -> done(box, 2, r, pending, callback, true));
            jolpicaSeasons.constructorQualifying(year, idB, r -> done(box, 3, r, pending, callback, true));
        });
    }

    private static void done(JSONObject[] box, int index, JSONObject response, int[] pending,
                             Callback callback, boolean teams) {
        box[index] = response;

        if (--pending[0] > 0) {
            return;
        }

        if (box[0] == null || box[1] == null) {
            callback.onLoaded(null);
            return;
        }

        if (teams) {
            callback.onLoaded(computeTeams(
                    parseTeam(box[0], false), parseTeam(box[1], false),
                    parseTeam(box[2], true), parseTeam(box[3], true)));
        } else {
            callback.onLoaded(compute(
                    parse(box[0], false), parse(box[1], false),
                    parse(box[2], true), parse(box[3], true)));
        }
    }

    static String driverId(JSONObject standings, compareParticipant p) {
        if (standings == null) {
            return null;
        }

        JSONObject entry = jolpicaSeasons.driverEntry(standings, p.code, p.given, p.family);
        JSONObject driver = entry == null ? null : entry.optJSONObject("Driver");
        String id = driver == null ? "" : driver.optString("driverId", "");

        return id.isEmpty() ? null : id;
    }

    static String constructorId(JSONObject standings, compareParticipant p) {
        if (standings == null) {
            return null;
        }

        JSONObject entry = compareLoader.constructorEntry(standings, p.teamId);
        JSONObject constructor = entry == null ? null : entry.optJSONObject("Constructor");
        String id = constructor == null ? "" : constructor.optString("constructorId", "");

        return id.isEmpty() ? null : id;
    }

    // ---------- parsing ----------

    private static JSONArray races(JSONObject response) {
        JSONObject data = response == null ? null : response.optJSONObject("MRData");
        JSONObject table = data == null ? null : data.optJSONObject("RaceTable");

        return table == null ? null : table.optJSONArray("Races");
    }

    static Map<Integer, Race> parse(JSONObject response, boolean qualifying) {
        Map<Integer, Race> result = new TreeMap<>();
        JSONArray races = races(response);

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

    static Map<Integer, Entry> parseTeam(JSONObject response, boolean qualifying) {
        Map<Integer, Entry> result = new TreeMap<>();
        JSONArray races = races(response);

        if (races == null) {
            return result;
        }

        for (int i = 0; i < races.length(); i++) {
            JSONObject race = races.optJSONObject(i);
            JSONArray rows = race == null
                    ? null : race.optJSONArray(qualifying ? "QualifyingResults" : "Results");

            if (rows == null || rows.length() == 0) {
                continue;
            }

            int best = 0;
            double points = 0;
            int retired = 0;

            for (int j = 0; j < rows.length(); j++) {
                JSONObject row = rows.optJSONObject(j);

                if (row == null) {
                    continue;
                }

                int position = row.optInt("position", 0);
                String text = row.optString("positionText", "");
                boolean started = position > 0 && !"W".equals(text) && !"F".equals(text);

                if (started && (best == 0 || position < best)) {
                    best = position;
                }

                points += row.optDouble("points", 0);

                if ("R".equals(text)) {
                    retired++;
                }
            }

            result.put(race.optInt("round", 0),
                    new Entry(race.optString("raceName", ""), best, points, retired));
        }

        return result;
    }

    // ---------- counting ----------

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

            result.rounds.add(new Round(round, name, rpa, rpb, qpa, qpb, Double.NaN, Double.NaN));
        }

        average(result, sumRaceA, sumRaceB, races, sumQualiA, sumQualiB, qualis);
        return result;
    }

    /** Teams: the best car of the round decides; points and retirements count both cars. */
    static Result computeTeams(Map<Integer, Entry> raceA, Map<Integer, Entry> raceB,
                               Map<Integer, Entry> qualiA, Map<Integer, Entry> qualiB) {
        Result result = new Result();

        TreeSet<Integer> rounds = new TreeSet<>();
        rounds.addAll(raceA.keySet());
        rounds.addAll(raceB.keySet());
        rounds.addAll(qualiA.keySet());
        rounds.addAll(qualiB.keySet());

        double sumRaceA = 0, sumRaceB = 0, sumQualiA = 0, sumQualiB = 0;
        double pointsA = 0, pointsB = 0;
        int races = 0, qualis = 0;

        for (int round : rounds) {
            Entry ra = raceA.get(round);
            Entry rb = raceB.get(round);
            Entry qa = qualiA.get(round);
            Entry qb = qualiB.get(round);

            boolean raceBoth = ra != null && rb != null && ra.best > 0 && rb.best > 0;
            boolean qualiBoth = qa != null && qb != null && qa.best > 0 && qb.best > 0;

            if (!raceBoth && !qualiBoth) {
                continue;
            }

            String name = ra != null ? ra.name : rb != null ? rb.name
                    : qa != null ? qa.name : qb.name;
            int rpa = 0, rpb = 0, qpa = 0, qpb = 0;
            double ppa = Double.NaN, ppb = Double.NaN;

            if (raceBoth) {
                rpa = ra.best;
                rpb = rb.best;

                if (rpa < rpb) {
                    result.raceA++;
                } else if (rpb < rpa) {
                    result.raceB++;
                }

                sumRaceA += rpa;
                sumRaceB += rpb;
                races++;

                ppa = ra.points;
                ppb = rb.points;
                pointsA += ppa;
                pointsB += ppb;

                result.retiredA += ra.retired;
                result.retiredB += rb.retired;
            }

            if (qualiBoth) {
                qpa = qa.best;
                qpb = qb.best;

                if (qpa < qpb) {
                    result.qualiA++;
                } else if (qpb < qpa) {
                    result.qualiB++;
                }

                sumQualiA += qpa;
                sumQualiB += qpb;
                qualis++;
            }

            result.rounds.add(new Round(round, name, rpa, rpb, qpa, qpb, ppa, ppb));
        }

        average(result, sumRaceA, sumRaceB, races, sumQualiA, sumQualiB, qualis);

        if (races > 0) {
            result.pointsA = pointsA;
            result.pointsB = pointsB;
        }

        return result;
    }

    private static void average(Result result, double sumRaceA, double sumRaceB, int races,
                                double sumQualiA, double sumQualiB, int qualis) {
        if (races > 0) {
            result.avgRaceA = sumRaceA / races;
            result.avgRaceB = sumRaceB / races;
        }

        if (qualis > 0) {
            result.avgQualiA = sumQualiA / qualis;
            result.avgQualiB = sumQualiB / qualis;
        }
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