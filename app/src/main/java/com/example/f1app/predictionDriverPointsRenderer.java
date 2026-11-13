package com.example.f1app;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.List;
import java.util.Locale;

public final class predictionDriverPointsRenderer {
    private predictionDriverPointsRenderer() {}

    public static void render(ViewGroup container, String codeA, String nameA,
                              String codeB, String nameB, List<raceDuelEvent> events) {
        Context context = container.getContext();
        container.removeAllViews();
        View root = LayoutInflater.from(context).inflate(
                R.layout.view_prediction_driver_points, container, false);
        container.addView(root);
        set(root, R.id.dpCodeA, codeA);
        set(root, R.id.dpCodeB, codeB);
        set(root, R.id.dpNameA, nameA);
        set(root, R.id.dpNameB, nameB);
        List<raceDuelEvent> comparable = new java.util.ArrayList<>();
        double sumA = 0, sumB = 0;
        for (raceDuelEvent event : events) {
            if (event == null || !valid(event.getPointsA()) || !valid(event.getPointsB())) continue;
            comparable.add(event);
            sumA += event.getPointsA();
            sumB += event.getPointsB();
        }
        set(root, R.id.dpPointsA, number(sumA));
        set(root, R.id.dpPointsB, number(sumB));
        set(root, R.id.dpCount, context.getString(R.string.dp_count, comparable.size()));
        LinearLayout metrics = root.findViewById(R.id.dpMetrics);
        row(metrics, context.getString(R.string.dp_metric), codeA, codeB, true);
        row(metrics, context.getString(R.string.dp_average),
                comparable.isEmpty() ? "—" : decimal(sumA / comparable.size()),
                comparable.isEmpty() ? "—" : decimal(sumB / comparable.size()), false);
        LinearLayout recent = root.findViewById(R.id.dpRows);
        row(recent, context.getString(R.string.dp_race), codeA, codeB, true);
        for (int i = comparable.size() - 1; i >= Math.max(0, comparable.size() - 5); i--) {
            raceDuelEvent event = comparable.get(i);
            row(recent, predictionRaceNameFormatter.getName(context, event.getRaceName()),
                    number(event.getPointsA()), number(event.getPointsB()), false);
        }
        root.findViewById(R.id.dpEmpty).setVisibility(comparable.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private static boolean valid(Double value) {
        return value != null && !Double.isNaN(value) && !Double.isInfinite(value) && value >= 0;
    }
    private static String decimal(double value) {
        return String.format(Locale.getDefault(), "%.1f", value);
    }
    private static String number(double value) {
        return value == Math.rint(value)
                ? String.format(Locale.getDefault(), "%.0f", value) : decimal(value);
    }
    private static void set(View root, int id, String value) {
        ((TextView) root.findViewById(id)).setText(value == null ? "—" : value);
    }
    private static void row(LinearLayout parent, String label, String a, String b, boolean header) {
        View view = LayoutInflater.from(parent.getContext()).inflate(
                R.layout.item_prediction_driver_points, parent, false);
        set(view, R.id.dpRowLabel, label);
        set(view, R.id.dpRowA, a);
        set(view, R.id.dpRowB, b);
        if (header) {
            for (int id : new int[]{R.id.dpRowLabel, R.id.dpRowA, R.id.dpRowB}) {
                TextView text = view.findViewById(id);
                androidx.core.widget.TextViewCompat.setTextAppearance(text, R.style.Widget_Apex_Pred_Caption);
            }
        }
        parent.addView(view);
    }
}
