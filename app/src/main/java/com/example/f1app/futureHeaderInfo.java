package com.example.f1app;

import android.content.Context;
import android.view.View;
import android.widget.TextView;

import com.android.volley.Request;

import org.json.JSONArray;
import org.json.JSONObject;

/** The tiles under the countdown of an upcoming weekend: race forecast, last year's winner, laps. */
final class futureHeaderInfo {

    private futureHeaderInfo() {
    }

    static void bindWeather(View root, sessionWeather weather) {
        View tile = root.findViewById(R.id.infoForecast);

        if (weather == null) {
            tile.setVisibility(View.GONE);
        } else {
            ((TextView) root.findViewById(R.id.infoForecastValue)).setText(weather.temp + ", " + weather.rain);
            tile.setVisibility(View.VISIBLE);
        }

        refresh(root);
    }

    static void loadHistory(View root, String season, String circuitId) {
        if (season == null || circuitId == null) {
            return;
        }

        final int previous;

        try {
            previous = Integer.parseInt(season) - 1;
        } catch (NumberFormatException error) {
            return;
        }

        Context context = root.getContext();
        String url = "https://api.jolpi.ca/ergast/f1/" + previous + "/circuits/" + circuitId
                + "/results/1/?format=json";

        apiQueue.get().add(new cachedJsonRequest(Request.Method.GET, url, null,
                response -> {
                    JSONObject data = response.optJSONObject("MRData");
                    JSONObject table = data == null ? null : data.optJSONObject("RaceTable");
                    JSONArray races = table == null ? null : table.optJSONArray("Races");

                    if (races == null || races.length() == 0) {
                        return;
                    }

                    JSONArray results = races.optJSONObject(0).optJSONArray("Results");
                    JSONObject winner = results == null ? null : results.optJSONObject(0);
                    JSONObject driver = winner == null ? null : winner.optJSONObject("Driver");

                    if (driver == null) {
                        return;
                    }

                    ((TextView) root.findViewById(R.id.infoWinnerCaption))
                            .setText(context.getString(R.string.info_winner_prev, String.valueOf(previous)));
                    ((TextView) root.findViewById(R.id.infoWinnerValue)).setText(driver.optString("familyName"));
                    root.findViewById(R.id.infoWinner).setVisibility(View.VISIBLE);

                    int laps = winner.optInt("laps");

                    if (laps > 0) {
                        ((TextView) root.findViewById(R.id.infoLapsValue)).setText(String.valueOf(laps));
                        root.findViewById(R.id.infoLaps).setVisibility(View.VISIBLE);
                    }

                    refresh(root);
                },
                error -> {
                }));
    }

    private static void refresh(View root) {
        boolean any = root.findViewById(R.id.infoForecast).getVisibility() == View.VISIBLE
                || root.findViewById(R.id.infoWinner).getVisibility() == View.VISIBLE
                || root.findViewById(R.id.infoLaps).getVisibility() == View.VISIBLE;

        root.findViewById(R.id.weekendInfo).setVisibility(any ? View.VISIBLE : View.GONE);
    }
}