package com.example.f1app;

import android.content.Context;
import android.graphics.Color;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.view.View;
import android.widget.TextView;

/** The podium of the race page: dark-glass cards plus the facts of the race. */
final class racePodiumBlock {

    private final Context context;
    private final podiumCards cards;
    private final View facts, tilePole, tileFastest, tileClimber, tileRetired;
    private final TextView factPole, factFastest, factClimber, factRetired, subtitle;
    private final View tileWeather;
    private final TextView factWeather;
    private final android.widget.ImageView factWeatherIcon;

    private String boundKey = "";
    private int token = 0;

    racePodiumBlock(View root) {
        context = root.getContext();
        cards = new podiumCards(root.findViewById(R.id.podiumRow), false, true);

        subtitle = root.findViewById(R.id.podiumSubtitle);
        facts = root.findViewById(R.id.raceFacts);
        tilePole = root.findViewById(R.id.tilePole);
        tileFastest = root.findViewById(R.id.tileFastest);
        tileClimber = root.findViewById(R.id.tileClimber);
        tileRetired = root.findViewById(R.id.tileRetired);
        factPole = root.findViewById(R.id.factPole);
        factFastest = root.findViewById(R.id.factFastest);
        factClimber = root.findViewById(R.id.factClimber);
        factRetired = root.findViewById(R.id.factRetired);
        tileWeather = root.findViewById(R.id.tileWeather);
        factWeather = root.findViewById(R.id.factWeather);
        factWeatherIcon = root.findViewById(R.id.factWeatherIcon);
    }

    void setRaceWeather(sessionWeather weather) {
        if (weather == null) {
            tileWeather.setVisibility(View.GONE);
            return;
        }

        factWeather.setText(weather.text);
        factWeatherIcon.setImageResource(weather.icon);
        factWeatherIcon.setColorFilter(Color.WHITE);
        tileWeather.setVisibility(View.VISIBLE);
        facts.setVisibility(View.VISIBLE);
    }

    void showBasics(String season, String... codes) {
        cards.showBasics(season, codes);
    }

    void bind(String season, String round, String first, String second, String third) {
        String key = season + "|" + round + "|" + first + "|" + second + "|" + third;

        if (key.equals(boundKey)) {
            return;
        }

        boundKey = key;
        final int mine = ++token;

        cards.showBasics(season, first, second, third);
        facts.setVisibility(View.GONE);
        subtitle.setVisibility(View.GONE);

        raceSummary.load(season, round, summary -> {
            if (mine == token && summary != null) {
                apply(summary);
            }
        });
    }

    private void apply(raceSummary s) {
        cards.show(s.podium);

        if (s.laps > 0) {
            subtitle.setText(context.getString(R.string.race_laps_total, s.laps));
            subtitle.setVisibility(View.VISIBLE);
        }

        boolean any = false;

        any |= fill(tilePole, factPole, s.pole, "");
        any |= fill(tileFastest, factFastest, s.fastest, s.fastestTime);
        any |= fill(tileClimber, factClimber, s.climber, s.climberRoute);

        if (s.total > 0) {
            factRetired.setText(context.getString(R.string.fact_retired_value, s.retired, s.total));
            tileRetired.setVisibility(View.VISIBLE);
            any = true;
        } else {
            tileRetired.setVisibility(View.GONE);
        }

        facts.setVisibility(any ? View.VISIBLE : View.GONE);
    }

    private boolean fill(View tile, TextView value, String main, String detail) {
        if (main == null || main.isEmpty()) {
            tile.setVisibility(View.INVISIBLE);
            return false;
        }

        SpannableStringBuilder text = new SpannableStringBuilder(main);

        if (detail != null && !detail.isEmpty()) {
            int start = text.length();
            text.append("\n").append(detail);
            text.setSpan(new RelativeSizeSpan(0.8f), start, text.length(), 0);
            text.setSpan(new ForegroundColorSpan(Color.parseColor("#B3FFFFFF")),
                    start, text.length(), 0);
        }

        value.setText(text);
        tile.setVisibility(View.VISIBLE);
        return true;
    }
}