package com.example.f1app;

import com.android.volley.Request;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

/** What one race result tells: the podium, pole, fastest lap, the biggest climber, retirements. */
final class raceSummary {

    interface Callback {
        void onLoaded(raceSummary summary); // null when nothing is known
    }

    private static final String BASE = "https://api.jolpi.ca/ergast/f1/";
    private static final long KEEP_MS = 10 * 60 * 1000L;
    private static final Map<String, raceSummary> CACHE = new HashMap<>();
    private static final Map<String, Long> STAMP = new HashMap<>();

    final podiumEntry[] podium = new podiumEntry[3];
    String pole = "";
    String fastest = "";
    String fastestTime = "";
    String climber = "";
    String climberRoute = "";
    int laps;
    int retired;
    int total;

    private raceSummary() {
    }

    static void load(String season, String round, Callback callback) {
        String key = season + "|" + round;
        raceSummary known = CACHE.get(key);
        Long at = STAMP.get(key);

        if (known != null && at != null && System.currentTimeMillis() - at < KEEP_MS) {
            callback.onLoaded(known);
            return;
        }

        String url = BASE + season + "/" + round + "/results/?format=json";

        apiQueue.get().add(new cachedJsonRequest(Request.Method.GET, url, null,
                response -> {
                    raceSummary summary = parse(response);

                    if (summary != null) {
                        CACHE.put(key, summary);
                        STAMP.put(key, System.currentTimeMillis());
                    }

                    callback.onLoaded(summary);
                },
                error -> callback.onLoaded(null)));
    }

    static raceSummary parse(JSONObject response) {
        JSONObject data = response.optJSONObject("MRData");
        JSONObject table = data == null ? null : data.optJSONObject("RaceTable");
        JSONArray races = table == null ? null : table.optJSONArray("Races");

        if (races == null || races.length() == 0) {
            return null;
        }

        JSONObject race = races.optJSONObject(0);
        JSONArray results = race == null ? null : race.optJSONArray("Results");

        if (results == null || results.length() == 0) {
            return null;
        }

        raceSummary summary = new raceSummary();
        summary.total = results.length();
        int bestGain = 0;

        for (int i = 0; i < results.length(); i++) {
            JSONObject result = results.optJSONObject(i);

            if (result == null) {
                continue;
            }

            JSONObject driver = result.optJSONObject("Driver");
            String surname = driver == null ? "" : driver.optString("familyName");
            String status = result.optString("status");
            boolean finished = "Finished".equals(status) || status.startsWith("+")
                    || "Lapped".equals(status);

            if (!finished) {
                summary.retired++;
            }

            if (i < 3) {
                summary.podium[i] = podiumEntry.from(result);
            }

            if (i == 0) {
                summary.laps = result.optInt("laps");
            }

            int grid = result.optInt("grid");
            int position = result.optInt("position");

            if (summary.pole.isEmpty() && grid == 1) {
                summary.pole = surname;
            }

            if (finished && grid > 0 && position > 0 && grid - position > bestGain) {
                bestGain = grid - position;
                summary.climber = surname;
                summary.climberRoute = "P" + grid + " \u2192 P" + position;
            }

            JSONObject lap = result.optJSONObject("FastestLap");

            if (lap != null && "1".equals(lap.optString("rank"))) {
                summary.fastest = surname;
                JSONObject time = lap.optJSONObject("Time");
                summary.fastestTime = time == null ? "" : time.optString("time");
            }
        }

        return summary;
    }
}