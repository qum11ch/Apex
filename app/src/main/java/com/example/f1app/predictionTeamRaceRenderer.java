package com.example.f1app;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.List;
import java.util.Locale;

public final class predictionTeamRaceRenderer {
    private predictionTeamRaceRenderer() {}

    public static void render(ViewGroup container, String nameA, String nameB,
                              List<predictionTeamRaceLoader.Event> events) {
        Context context = container.getContext();
        container.removeAllViews();
        View root = LayoutInflater.from(context).inflate(
                R.layout.view_prediction_team_race, container, false);
        container.addView(root);
        set(root, R.id.trNameA, nameA);
        set(root, R.id.trNameB, nameB);
        long sumA = 0, sumB = 0;
        int countA = 0, countB = 0, retiredA = 0, retiredB = 0;
        for (predictionTeamRaceLoader.Event event : events) {
            for (predictionTeamRaceLoader.Finish f : event.a) {
                if (f.position != null) { sumA += f.position; countA++; }
                if ("R".equals(f.rawFinish)) retiredA++;
            }
            for (predictionTeamRaceLoader.Finish f : event.b) {
                if (f.position != null) { sumB += f.position; countB++; }
                if ("R".equals(f.rawFinish)) retiredB++;
            }
        }
        set(root, R.id.trAverageA, mean(sumA, countA));
        set(root, R.id.trAverageB, mean(sumB, countB));
        set(root, R.id.trRetirementsA, String.valueOf(retiredA));
        set(root, R.id.trRetirementsB, String.valueOf(retiredB));
        set(root, R.id.trCount, context.getString(R.string.tr_count, events.size()));
        set(root, R.id.trSamples, context.getString(R.string.tr_samples, countA, countB));
        LinearLayout rows = root.findViewById(R.id.trRows);
        for (int i = events.size() - 1; i >= Math.max(0, events.size() - 5); i--) {
            predictionTeamRaceLoader.Event event = events.get(i);
            View row = LayoutInflater.from(context).inflate(
                    R.layout.item_prediction_team_race, rows, false);
            set(row, R.id.trRace, predictionRaceNameFormatter.getName(context, event.raceName));
            set(row, R.id.trFinishesA, pair(context, event.a));
            set(row, R.id.trFinishesB, pair(context, event.b));
            rows.addView(row);
        }
        root.findViewById(R.id.trEmpty).setVisibility(events.isEmpty() ? View.VISIBLE : View.GONE);
    }
    private static String mean(long sum, int count) {
        return count == 0 ? "—" : String.format(Locale.getDefault(), "%.1f", (double) sum / count);
    }
    private static String pair(Context context, List<predictionTeamRaceLoader.Finish> values) {
        return finish(context, values.get(0)) + " / " + finish(context, values.get(1));
    }
    private static String finish(Context context, predictionTeamRaceLoader.Finish value) {
        if (value.position != null) return String.valueOf(value.position);
        switch (value.rawFinish) {
            case "R": return context.getString(R.string.ret_text);
            case "W": return context.getString(R.string.wd_text);
            case "D": return context.getString(R.string.dsq_text);
            case "NC": return context.getString(R.string.tr_not_classified);
            default: return "—";
        }
    }
    private static void set(View root, int id, String value) {
        ((TextView) root.findViewById(id)).setText(value == null ? "—" : value);
    }
}
