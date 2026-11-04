package com.example.f1app;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.text.NumberFormat;
import java.util.Locale;

public final class predictionStandingsPointsRenderer {
    private predictionStandingsPointsRenderer() {}

    public static void render(ViewGroup container, boolean teams, String season,
                              String codeA, String nameA, String codeB, String nameB,
                              jolpicaPointsLoader.Result result) {
        render(container, teams, season, codeA, nameA, codeB, nameB, result, null, false);
    }

    public static void render(ViewGroup container, boolean teams, String season,
                              String codeA, String nameA, String codeB, String nameB,
                              jolpicaPointsLoader.Result result,
                              predictionWeekendPointsLoader.Data history, boolean historyError) {
        Context c = container.getContext();
        container.removeAllViews();
        View root = LayoutInflater.from(c).inflate(R.layout.view_prediction_standings_points, container, false);
        container.addView(root);
        String a = teams ? c.getString(R.string.spw_a) : codeA;
        String b = teams ? c.getString(R.string.spw_b) : codeB;
        set(root, R.id.spwContext, c.getString(R.string.spw_period, season, result.round));
        set(root, R.id.spwCodeA, a); set(root, R.id.spwCodeB, b);
        set(root, R.id.spwNameA, nameA); set(root, R.id.spwNameB, nameB);
        set(root, R.id.spwPointsA, number(result.pointsA));
        set(root, R.id.spwPointsB, number(result.pointsB));
        set(root, R.id.spwSource, c.getString(teams ? R.string.spw_teams_source : R.string.spw_drivers_source));
        set(root, R.id.spwSideA, a); set(root, R.id.spwSideB, b);
        if (history == null) {
            root.findViewById(R.id.spwHistory).setVisibility(View.GONE);
            set(root, R.id.spwHistoryState, c.getString(
                    historyError ? R.string.spw_history_error : R.string.spw_loading));
            return;
        }
        root.findViewById(R.id.spwHistoryState).setVisibility(View.GONE);
        root.findViewById(R.id.spwHistory).setVisibility(View.VISIBLE);
        int count = history.rows.size();
        set(root, R.id.spwAverageA, count == 0 ? "—" : decimal(history.sumA() / count));
        set(root, R.id.spwAverageB, count == 0 ? "—" : decimal(history.sumB() / count));
        set(root, R.id.spwCount, c.getString(R.string.spw_count, count));
        root.findViewById(R.id.spwEmpty).setVisibility(count == 0 ? View.VISIBLE : View.GONE);
        TextView skipped = root.findViewById(R.id.spwSkipped);
        skipped.setVisibility(history.skipped > 0 ? View.VISIBLE : View.GONE);
        skipped.setText(c.getString(R.string.spw_skipped, history.skipped));
        boolean mismatch = Math.abs(history.sumA() - result.pointsA) > 0.000001
                || Math.abs(history.sumB() - result.pointsB) > 0.000001;
        root.findViewById(R.id.spwMismatch).setVisibility(mismatch ? View.VISIBLE : View.GONE);
        LinearLayout rows = root.findViewById(R.id.spwRows);
        for (int i = count - 1; i >= Math.max(0, count - 5); i--) {
            predictionWeekendPointsLoader.Row item = history.rows.get(i);
            View row = LayoutInflater.from(c).inflate(R.layout.item_prediction_weekend_points, rows, false);
            set(row, R.id.spwRace, predictionRaceNameFormatter.getName(c, item.raceName));
            set(row, R.id.spwValueA, value(c, item.hasSprint, item.raceA, item.sprintA));
            set(row, R.id.spwValueB, value(c, item.hasSprint, item.raceB, item.sprintB));
            rows.addView(row);
        }
    }
    private static String value(Context c, boolean sprint, double race, double sp) {
        return sprint ? c.getString(R.string.spw_total_parts, number(race + sp), number(race), number(sp))
                : number(race);
    }
    private static String number(double value) {
        NumberFormat f = NumberFormat.getNumberInstance(Locale.getDefault());
        f.setGroupingUsed(false); f.setMaximumFractionDigits(3); return f.format(value);
    }
    private static String decimal(double value) { return String.format(Locale.getDefault(), "%.1f", value); }
    private static void set(View root, int id, String value) {
        ((TextView) root.findViewById(id)).setText(value == null ? "—" : value);
    }
}
