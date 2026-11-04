package com.example.f1app;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.List;
import java.util.Locale;

public final class predictionTeamQualifyingRenderer {
    private predictionTeamQualifyingRenderer() {}

    public static void render(ViewGroup container, String nameA, String nameB,
                              List<predictionTeamQualifyingLoader.Event> events) {
        Context context = container.getContext();
        container.removeAllViews();
        View root = LayoutInflater.from(context).inflate(
                R.layout.view_prediction_team_qualifying, container, false);
        container.addView(root);
        set(root, R.id.tqNameA, nameA);
        set(root, R.id.tqNameB, nameB);
        long sumA = 0, sumB = 0;
        for (predictionTeamQualifyingLoader.Event e : events) {
            sumA += e.a.get(0) + e.a.get(1);
            sumB += e.b.get(0) + e.b.get(1);
        }
        set(root, R.id.tqAverageA, average(sumA, events.size()));
        set(root, R.id.tqAverageB, average(sumB, events.size()));
        set(root, R.id.tqCount, context.getString(R.string.tq_count, events.size()));
        LinearLayout rows = root.findViewById(R.id.tqRows);
        for (int i = events.size() - 1; i >= Math.max(0, events.size() - 5); i--) {
            predictionTeamQualifyingLoader.Event event = events.get(i);
            View row = LayoutInflater.from(context).inflate(
                    R.layout.item_prediction_team_qualifying, rows, false);
            set(row, R.id.tqRace, predictionRaceNameFormatter.getName(context, event.raceName));
            set(row, R.id.tqPositionsA, positions(event.a));
            set(row, R.id.tqPositionsB, positions(event.b));
            rows.addView(row);
        }
        root.findViewById(R.id.tqEmpty).setVisibility(events.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private static String average(long sum, int count) {
        return count == 0 ? "—" : String.format(Locale.getDefault(), "%.1f", sum / (2.0 * count));
    }
    private static String positions(List<Integer> p) { return p.get(0) + " / " + p.get(1); }
    private static void set(View root, int id, String value) {
        ((TextView) root.findViewById(id)).setText(value == null ? "—" : value);
    }
}
