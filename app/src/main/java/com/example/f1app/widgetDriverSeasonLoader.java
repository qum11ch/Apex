package com.example.f1app;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

final class widgetDriverSeasonLoader {

    enum State {
        READY, NO_SEASON_ENTRY, ERROR
    }

    interface Callback {
        void onLoaded(State state, JSONObject snapshot, boolean stale);
    }

    private static final String BASE = "https://api.jolpi.ca/ergast/f1/";
    private static final long TTL_MS = 3 * 60 * 60 * 1000L;

    private widgetDriverSeasonLoader() {
    }

    static void load(Context context, int season, String fullName,
                     boolean force, Callback callback) {
        Context app = context.getApplicationContext();

        if (Looper.myLooper() != Looper.getMainLooper()) {
            new Handler(Looper.getMainLooper()).post(
                    () -> load(app, season, fullName, force, callback));
            return;
        }

        if (season < 1950 || fullName == null || fullName.trim().isEmpty()) {
            callback.onLoaded(State.ERROR, null, false);
            return;
        }

        new Load(app, season, fullName.trim(), force, callback).start();
    }

    private static final class Load {
        final int season;
        final String fullName;
        final String key;
        final boolean force;
        final Callback callback;
        final SharedPreferences prefs;
        final JSONObject old;
        final Handler main = new Handler(Looper.getMainLooper());
        final Runnable timeout;

        boolean finished;
        JSONObject snapshot;
        String driverId;

        Load(Context app, int season, String fullName,
             boolean force, Callback callback) {
            this.season = season;
            this.fullName = fullName;
            this.force = force;
            this.callback = callback;
            this.key = season + ":" + fullName;
            this.prefs = app.getSharedPreferences(
                    "widget_driver_seasons", Context.MODE_PRIVATE);
            this.old = read(prefs.getString(key, null));
            this.timeout = this::fail;
        }

        void start() {
            long age = old == null ? Long.MAX_VALUE
                    : System.currentTimeMillis() - old.optLong("updatedAt");

            if (!force && old != null && age >= 0 && age < TTL_MS) {
                finish(State.READY, old, false);
                return;
            }

            main.postDelayed(timeout, 45_000L);

            get(BASE + season + "/driverstandings/?format=json&limit=100",
                    response -> {
                        JSONObject table = response.getJSONObject("MRData")
                                .getJSONObject("StandingsTable");

                        requireSeason(table);
                        JSONArray lists = table.getJSONArray("StandingsLists");
                        JSONObject entry = null;

                        for (int i = 0; i < lists.length(); i++) {
                            JSONArray rows = lists.getJSONObject(i)
                                    .getJSONArray("DriverStandings");

                            for (int j = 0; j < rows.length(); j++) {
                                JSONObject row = rows.getJSONObject(j);
                                JSONObject driver = row.getJSONObject("Driver");

                                String name = driver.getString("givenName")
                                        + " " + driver.getString("familyName");

                                if (normalize(name).equals(normalize(fullName))) {
                                    entry = row;
                                    break;
                                }
                            }

                            if (entry != null) {
                                break;
                            }
                        }

                        if (entry == null) {
                            prefs.edit().remove(key).apply();
                            finish(State.NO_SEASON_ENTRY, null, false);
                            return;
                        }

                        JSONObject driver = entry.getJSONObject("Driver");
                        driverId = driver.getString("driverId");

                        snapshot = new JSONObject();
                        snapshot.put("v", 1);
                        snapshot.put("season", season);
                        snapshot.put("fullName", fullName);
                        snapshot.put("driverId", driverId);
                        snapshot.put("code", driver.optString("code", ""));
                        snapshot.put("position", entry.getString("positionText"));
                        snapshot.put("points", entry.getString("points"));
                        snapshot.put("wins", entry.getInt("wins"));

                        loadResults();
                    });
        }

        void loadResults() {
            String url = BASE + season + "/drivers/"
                    + android.net.Uri.encode(driverId)
                    + "/results/?format=json&limit=100";

            get(url, response -> {
                JSONArray races = races(response);
                List<JSONObject> results = new ArrayList<>();
                int podiums = 0;

                for (int i = 0; i < races.length(); i++) {
                    JSONObject race = races.getJSONObject(i);
                    JSONArray rows = race.getJSONArray("Results");

                    if (rows.length() != 1) {
                        throw new JSONException("Expected one driver result");
                    }

                    JSONObject row = rows.getJSONObject(0);
                    int position = row.getInt("position");

                    if (position >= 1 && position <= 3) {
                        podiums++;
                    }

                    JSONObject result = new JSONObject();
                    result.put("round", race.getInt("round"));
                    result.put("raceName", race.getString("raceName"));
                    result.put("position", position);
                    result.put("positionText", row.getString("positionText"));
                    result.put("status", row.getString("status"));
                    results.add(result);
                }

                results.sort((a, b) ->
                        Integer.compare(a.optInt("round"), b.optInt("round")));

                JSONArray recent = new JSONArray();

                for (int i = Math.max(0, results.size() - 5);
                     i < results.size(); i++) {
                    recent.put(results.get(i));
                }

                snapshot.put("podiums", podiums);
                snapshot.put("raceResultsCount", results.size());
                snapshot.put("recent", recent);

                loadQualifying();
            });
        }

        void loadQualifying() {
            String url = BASE + season + "/drivers/"
                    + android.net.Uri.encode(driverId)
                    + "/qualifying/?format=json&limit=100";

            get(url, response -> {
                JSONArray races = races(response);
                int poles = 0;

                for (int i = 0; i < races.length(); i++) {
                    JSONArray rows = races.getJSONObject(i)
                            .getJSONArray("QualifyingResults");

                    if (rows.length() != 1) {
                        throw new JSONException("Expected one qualifying result");
                    }

                    if (rows.getJSONObject(0).getInt("position") == 1) {
                        poles++;
                    }
                }

                snapshot.put("poles", poles);
                snapshot.put("updatedAt", System.currentTimeMillis());

                prefs.edit().putString(key, snapshot.toString()).apply();
                finish(State.READY, snapshot, false);
            });
        }

        JSONArray races(JSONObject response) throws JSONException {
            JSONObject data = response.getJSONObject("MRData");
            JSONObject table = data.getJSONObject("RaceTable");
            requireSeason(table);

            JSONArray races = table.getJSONArray("Races");

            if (data.getInt("total") > races.length()) {
                throw new JSONException("Incomplete result page");
            }

            return races;
        }

        void requireSeason(JSONObject table) throws JSONException {
            if (!String.valueOf(season).equals(table.getString("season"))) {
                throw new JSONException("Unexpected season");
            }
        }

        void get(String url, JsonCallback next) {
            JsonObjectRequest request = new JsonObjectRequest(
                    Request.Method.GET, url, null,
                    response -> {
                        if (finished) {
                            return;
                        }

                        try {
                            next.accept(response);
                        } catch (JSONException | RuntimeException error) {
                            android.util.Log.w(
                                    "WidgetDriverSeason", "Invalid season data", error);
                            fail();
                        }
                    },
                    error -> {
                        if (!finished) {
                            android.util.Log.w(
                                    "WidgetDriverSeason", "Request failed", error);
                            fail();
                        }
                    });

            request.setShouldCache(false);
            request.setTag(this);
            apiQueue.get().add(request);
        }

        void fail() {
            finish(old == null ? State.ERROR : State.READY, old, old != null);
        }

        void finish(State state, JSONObject data, boolean stale) {
            if (finished) {
                return;
            }

            finished = true;
            main.removeCallbacks(timeout);
            apiQueue.get().cancelAll(this);
            callback.onLoaded(state, data, stale);
        }
    }

    private interface JsonCallback {
        void accept(JSONObject value) throws JSONException;
    }

    private static JSONObject read(String raw) {
        if (raw == null) {
            return null;
        }

        try {
            JSONObject value = new JSONObject(raw);
            return value.optInt("v") == 1 ? value : null;
        } catch (JSONException error) {
            return null;
        }
    }

    private static String normalize(String value) {
        return java.text.Normalizer.normalize(
                        value, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase(java.util.Locale.ROOT)
                .trim()
                .replaceAll("\\s+", " ");
    }
}