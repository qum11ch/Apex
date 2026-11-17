package com.example.f1app;

import android.util.Log;

import androidx.fragment.app.Fragment;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Drivers' standings of one season: place, points and wins of every driver. */
final class seasonDriverTable {

    private static final String TAG = "SeasonDriverTable";
    private static final String DASH = "\u2013";

    private seasonDriverTable() {
    }

    static final class Entry {
        String code = "";
        String given = "";
        String family = "";
        String place = DASH;
        String points = DASH;
        String wins = DASH;
    }

    interface Callback {
        void onLoaded(List<Entry> entries);
    }

    static void load(Fragment host, Object tag, String season, Callback callback) {
        RequestQueue queue = apiQueue.get();

        String url = "https://api.jolpi.ca/ergast/f1/" + season + "/driverstandings/?format=json";

        JsonObjectRequest request = new cachedJsonRequest(
                Request.Method.GET,
                url,
                null,
                response -> {
                    if (!host.isAdded()) {
                        return;
                    }

                    try {
                        callback.onLoaded(read(response));
                    } catch (JSONException error) {
                        Log.w(TAG, "Unexpected standings response for " + season, error);
                    }
                },
                error -> Log.w(TAG, "Standings are not available for " + season, error)
        );

        request.setTag(tag);
        queue.add(request);
    }

    private static List<Entry> read(JSONObject response) throws JSONException {
        List<Entry> result = new ArrayList<>();

        JSONArray lists = response
                .getJSONObject("MRData")
                .getJSONObject("StandingsTable")
                .optJSONArray("StandingsLists");

        if (lists == null) {
            return result;
        }

        for (int i = 0; i < lists.length(); i++) {
            JSONArray entries = lists.getJSONObject(i).optJSONArray("DriverStandings");

            if (entries == null) {
                continue;
            }

            for (int j = 0; j < entries.length(); j++) {
                JSONObject entry = entries.getJSONObject(j);
                JSONObject driver = entry.optJSONObject("Driver");

                if (driver == null) {
                    continue;
                }

                Entry item = new Entry();
                item.code = driver.optString("code", "");
                item.given = driver.optString("givenName", "");
                item.family = driver.optString("familyName", "");
                item.place = entry.optString("positionText", DASH);
                item.points = entry.optString("points", DASH);
                item.wins = entry.optString("wins", DASH);

                result.add(item);
            }
        }

        return result;
    }

    /** The code first; without it the family name, and the given name for namesakes. */
    static Entry find(List<Entry> entries, String code, String given, String family) {
        if (code != null && !code.isEmpty()) {
            for (Entry entry : entries) {
                if (!entry.code.isEmpty() && entry.code.equalsIgnoreCase(code)) {
                    return entry;
                }
            }
        }

        for (Entry entry : entries) {
            if (family != null && entry.family.equalsIgnoreCase(family)) {
                String a = given == null ? "" : given.toLowerCase(Locale.ROOT);
                String b = entry.given.toLowerCase(Locale.ROOT);

                if (a.isEmpty() || b.isEmpty() || a.contains(b) || b.contains(a)) {
                    return entry;
                }
            }
        }

        return null;
    }
}