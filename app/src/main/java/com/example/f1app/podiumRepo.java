package com.example.f1app;

import com.android.volley.Request;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Podiums of a whole season in three requests (P1, P2, P3 of every race), kept for a few minutes. */
final class podiumRepo {

    interface Callback {
        void onLoaded(podiumEntry[] podium); // null when nothing is known
    }

    private static final String BASE = "https://api.jolpi.ca/ergast/f1/";
    private static final long KEEP_MS = 5 * 60 * 1000L;

    private static final class Waiter {
        final String round;
        final Callback callback;

        Waiter(String round, Callback callback) {
            this.round = round;
            this.callback = callback;
        }
    }

    private static final Map<String, Map<String, podiumEntry[]>> DONE = new HashMap<>();
    private static final Map<String, Long> STAMP = new HashMap<>();
    private static final Map<String, List<Waiter>> WAITING = new HashMap<>();

    private podiumRepo() {
    }

    static void load(String season, String round, Callback callback) {
        Map<String, podiumEntry[]> ready = DONE.get(season);
        Long stamp = STAMP.get(season);

        if (ready != null && stamp != null && System.currentTimeMillis() - stamp < KEEP_MS) {
            callback.onLoaded(ready.get(round));
            return;
        }

        List<Waiter> waiting = WAITING.get(season);
        boolean first = waiting == null;

        if (first) {
            waiting = new ArrayList<>();
            WAITING.put(season, waiting);
        }

        waiting.add(new Waiter(round, callback));

        if (first) {
            fetch(season);
        }
    }

    private static void fetch(String season) {
        final Map<String, podiumEntry[]> result = new HashMap<>();
        final int[] left = {3};
        final boolean[] any = {false};

        for (int position = 1; position <= 3; position++) {
            final int index = position - 1;
            String url = BASE + season + "/results/" + position + "/?format=json&limit=100";

            apiQueue.get().add(new cachedJsonRequest(Request.Method.GET, url, null,
                    response -> {
                        if (parse(response, index, result)) {
                            any[0] = true;
                        }

                        finish(season, result, left, any);
                    },
                    error -> finish(season, result, left, any)));
        }
    }

    private static boolean parse(JSONObject response, int index, Map<String, podiumEntry[]> result) {
        JSONObject table = response.optJSONObject("MRData") == null ? null
                : response.optJSONObject("MRData").optJSONObject("RaceTable");

        if (table == null) {
            return false;
        }

        JSONArray races = table.optJSONArray("Races");
        boolean found = false;

        for (int i = 0; races != null && i < races.length(); i++) {
            JSONObject race = races.optJSONObject(i);
            JSONArray results = race == null ? null : race.optJSONArray("Results");

            if (results == null || results.length() == 0) {
                continue;
            }

            String round = race.optString("round");
            podiumEntry[] podium = result.get(round);

            if (podium == null) {
                podium = new podiumEntry[3];
                result.put(round, podium);
            }

            podium[index] = podiumEntry.from(results.optJSONObject(0));
            found = true;
        }

        return found;
    }

    private static void finish(String season, Map<String, podiumEntry[]> result, int[] left,
                               boolean[] any) {
        if (--left[0] > 0) {
            return;
        }

        if (any[0]) {
            DONE.put(season, result);
            STAMP.put(season, System.currentTimeMillis());
        }

        List<Waiter> waiting = WAITING.remove(season);

        if (waiting != null) {
            for (Waiter waiter : waiting) {
                waiter.callback.onLoaded(any[0] ? result.get(waiter.round) : null);
            }
        }
    }
}