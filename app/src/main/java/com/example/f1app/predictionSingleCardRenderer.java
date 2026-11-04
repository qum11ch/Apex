package com.example.f1app;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.Locale;

public final class predictionSingleCardRenderer {
    private predictionSingleCardRenderer() {}

    public static void render(ViewGroup container, predictionSingleCardLoader.Data data) {
        Context context = container.getContext();
        container.removeAllViews();
        View view = LayoutInflater.from(context).inflate(
                R.layout.view_prediction_single_stats, container, false);
        container.addView(view);
        text(view, R.id.singleName, data.title);
        String period = data.targetRace == null
                ? context.getString(R.string.ps_season, data.season)
                : predictionRaceNameFormatter.getNameWithSeason(context, data.targetRace, data.season)
                    + (data.targetDate == null ? "" : " · " + data.targetDate)
                    + "\n" + context.getString(R.string.ps_before_event);
        text(view, R.id.singleContext, period);
        text(view, R.id.singlePoints, String.valueOf(data.points));
        text(view, R.id.singleWins, String.valueOf(data.wins));
        text(view, R.id.singlePodiums, String.valueOf(data.podiums));
        text(view, R.id.singleRetirements, String.valueOf(data.retirements));
        String average = data.rows.isEmpty() ? "—" : String.format(
                Locale.getDefault(), "%.1f", (double) data.points / data.rows.size());
        text(view, R.id.singleAverage, context.getString(R.string.ps_average, average));
        text(view, R.id.singleCount, context.getString(R.string.ps_events, data.rows.size()));
        text(view, R.id.singleResultHeader, context.getString(
                data.team ? R.string.ps_race_points : R.string.ps_finish));
        text(view, R.id.singleFootnote, context.getString(
                data.team ? R.string.ps_team_note : R.string.ps_driver_note));
        LinearLayout recent = view.findViewById(R.id.singleRecentRows);
        for (int i = data.rows.size() - 1; i >= Math.max(0, data.rows.size() - 5); i--) {
            predictionSingleCardLoader.Row row = data.rows.get(i);
            View item = LayoutInflater.from(context).inflate(
                    R.layout.item_prediction_single_result, recent, false);
            text(item, R.id.singleRaceName,
                    predictionRaceNameFormatter.getName(context, row.raceName));
            text(item, R.id.singleRaceValue,
                    data.team ? String.valueOf(row.points) : finish(context, row.result));
            recent.addView(item);
        }
        view.findViewById(R.id.singleEmpty).setVisibility(
                data.rows.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private static void text(View root, int id, String value) {
        ((TextView) root.findViewById(id)).setText(value == null ? "—" : value);
    }

    private static String finish(Context context, String raw) {
        if (raw == null) return "—";
        String[] parts = raw.trim().split("-", 2);
        if (parts.length != 2) return "—";
        String finish = parts[1].trim();
        switch (finish) {
            case "R": return context.getString(R.string.ret_text);
            case "W": return context.getString(R.string.wd_text);
            case "D": return context.getString(R.string.dsq_text);
            default: return finish;
        }
    }
}
