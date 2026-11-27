package com.example.f1app;

import android.content.Context;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.view.View;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.android.volley.Request;

import org.json.JSONArray;
import org.json.JSONObject;

/** The podium of the last race on the home screen: cards plus the pole and the fastest lap. */
final class mainPodiumBlock {

    private static final String BASE = "https://api.jolpi.ca/ergast/f1/";

    private final Context context;
    private final podiumCards cards;
    private final View stats;
    private final TextView poleValue, fastestValue;

    private String boundKey = "";
    private int token = 0;

    mainPodiumBlock(View root) {
        context = root.getContext();
        cards = new podiumCards(root.findViewById(R.id.podiumRow), false);
        stats = root.findViewById(R.id.podiumStats);
        poleValue = root.findViewById(R.id.statPole);
        fastestValue = root.findViewById(R.id.statFastest);
    }

    void bind(concludedRacesData datum, String season) {
        String key = season + "|" + datum.getRoundNumber();

        if (key.equals(boundKey)) {
            return;
        }

        boundKey = key;
        final int mine = ++token;

        cards.showBasics(season, datum.getWinnerDriverCode(), datum.getSecondPlaceCode(),
                datum.getThirdPlaceCode());
        stats.setVisibility(View.GONE);

        String url = BASE + season + "/" + datum.getRoundNumber() + "/results/?format=json";

        apiQueue.get().add(new cachedJsonRequest(Request.Method.GET, url, null,
                response -> {
                    if (mine == token) {
                        parse(response);
                    }
                },
                error -> {
                }));
    }

    private void parse(JSONObject response) {
        JSONObject data = response.optJSONObject("MRData");
        JSONObject table = data == null ? null : data.optJSONObject("RaceTable");
        JSONArray races = table == null ? null : table.optJSONArray("Races");

        if (races == null || races.length() == 0) {
            return;
        }

        JSONArray results = races.optJSONObject(0).optJSONArray("Results");

        if (results == null) {
            return;
        }

        podiumEntry[] podium = new podiumEntry[3];
        String pole = null;
        String fastest = null;
        String fastestTime = null;

        for (int i = 0; i < results.length(); i++) {
            JSONObject result = results.optJSONObject(i);

            if (result == null) {
                continue;
            }

            JSONObject driver = result.optJSONObject("Driver");
            String surname = driver == null ? "" : driver.optString("familyName");

            if (i < 3) {
                podium[i] = podiumEntry.from(result);
            }

            if (pole == null && result.optInt("grid") == 1) {
                pole = surname;
            }

            JSONObject lap = result.optJSONObject("FastestLap");

            if (lap != null && "1".equals(lap.optString("rank"))) {
                fastest = surname;
                JSONObject time = lap.optJSONObject("Time");
                fastestTime = time == null ? null : time.optString("time");
            }
        }

        cards.show(podium);
        showStats(pole, fastest, fastestTime);
    }

    private void showStats(String pole, String fastest, String fastestTime) {
        if ((pole == null || pole.isEmpty()) && (fastest == null || fastest.isEmpty())) {
            return;
        }

        poleValue.setText(pole == null || pole.isEmpty() ? "-" : pole);

        SpannableStringBuilder text = new SpannableStringBuilder(
                fastest == null || fastest.isEmpty() ? "-" : fastest);

        if (fastestTime != null && !fastestTime.isEmpty()) {
            int start = text.length();
            text.append("\n").append(fastestTime);
            text.setSpan(new RelativeSizeSpan(0.8f), start, text.length(), 0);
            text.setSpan(new ForegroundColorSpan(
                    ContextCompat.getColor(context, R.color.hero_text_secondary)), start, text.length(), 0);
        }

        fastestValue.setText(text);
        stats.setVisibility(View.VISIBLE);
    }
}