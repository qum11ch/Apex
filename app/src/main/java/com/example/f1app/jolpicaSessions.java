package com.example.f1app;

import android.content.Context;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONObject;

import android.graphics.Typeface;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;

import android.os.SystemClock;
import java.util.LinkedHashMap;
import java.util.Map;

public final class jolpicaSessions {

    public enum Session {
        QUALIFYING("qualifying", "QualifyingResults"),
        SPRINT("sprint", "SprintResults"),
        RACE("results", "Results");

        final String endpoint;
        final String listName;

        Session(String endpoint, String listName) {
            this.endpoint = endpoint;
            this.listName = listName;
        }
    }

    public interface Callback {
        void onResults(Session session, JSONArray results);
        void onNoResults(Session session);
        void onError(Session session, Exception error);
    }

    private static RequestQueue queue;

    private jolpicaSessions() {}

    private static synchronized RequestQueue queue(Context context) {
        if (queue == null) {
            queue = Volley.newRequestQueue(context.getApplicationContext());
        }
        return queue;
    }

    private static final long CACHE_TTL_MS = 60 * 60 * 1000L;
    private static final int CACHE_LIMIT = 32;

    private static final Map<String, CachedJson> CACHE =
            new LinkedHashMap<>(16, 0.75f, true);

    private static final class CachedJson {
        final JSONObject response;
        final long storedAtMs;

        CachedJson(JSONObject response) {
            this.response = response;
            this.storedAtMs = SystemClock.elapsedRealtime();
        }
    }

    public static void load(
            Context context,
            String season,
            String round,
            Session session,
            Callback callback) {

        String url = "https://api.jolpi.ca/ergast/f1/"
                + season + "/" + round + "/" + session.endpoint
                + "/?format=json&limit=100";

        synchronized (CACHE) {
            CachedJson cached = CACHE.get(url);

            if (cached != null
                    && SystemClock.elapsedRealtime() - cached.storedAtMs < CACHE_TTL_MS) {
                deliverCached(session, cached.response, callback);
                return;
            }

            CACHE.remove(url);
        }

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.GET, url, null,
                response -> {
                    try {
                        JSONArray races = response
                                .getJSONObject("MRData")
                                .getJSONObject("RaceTable")
                                .getJSONArray("Races");

                        synchronized (CACHE) {
                            CACHE.put(url, new CachedJson(response));

                            while (CACHE.size() > CACHE_LIMIT) {
                                String oldestKey = CACHE.keySet().iterator().next();
                                CACHE.remove(oldestKey);
                            }
                        }

                        if (races.length() == 0) {
                            callback.onNoResults(session);
                            return;
                        }

                        JSONObject race = races.getJSONObject(0);
                        JSONArray results = race.optJSONArray(session.listName);

                        if (results == null || results.length() == 0) {
                            callback.onNoResults(session);
                        } else {
                            callback.onResults(session, results);
                        }
                    } catch (Exception e) {
                        callback.onError(session, e);
                    }
                },
                error -> callback.onError(session, error)
        );

        queue(context).add(request);
    }

    private static void deliverCached(
            Session session,
            JSONObject response,
            Callback callback
    ) {
        try {
            JSONArray races = response
                    .getJSONObject("MRData")
                    .getJSONObject("RaceTable")
                    .getJSONArray("Races");

            if (races.length() == 0) {
                callback.onNoResults(session);
                return;
            }

            JSONObject race = races.getJSONObject(0);
            JSONArray results = race.optJSONArray(session.listName);

            if (results == null || results.length() == 0) {
                callback.onNoResults(session);
            } else {
                callback.onResults(session, results);
            }
        } catch (Exception error) {
            callback.onError(session, error);
        }
    }


    public static CharSequence podiumText(JSONArray results, int codeColor) {
        SpannableStringBuilder sb = new SpannableStringBuilder();

        for (int i = 0; i < Math.min(3, results.length()); i++) {
            JSONObject item = results.optJSONObject(i);
            if (item == null) continue;

            JSONObject driver = item.optJSONObject("Driver");
            String code = driver != null ? driver.optString("code", "?") : "?";
            String position = item.optString("position", String.valueOf(i + 1));

            if (sb.length() > 0) sb.append("    ");

            sb.append(position).append(". ");

            int start = sb.length();
            sb.append(code);
            int end = sb.length();

            sb.setSpan(new StyleSpan(Typeface.BOLD), start, end,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            sb.setSpan(new ForegroundColorSpan(codeColor), start, end,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        return sb;
    }
}