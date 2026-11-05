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
    private predictionStandingsPointsRenderer() {
    }

    public static void render(
            ViewGroup container,
            boolean teams,
            String season,
            String codeA,
            String nameA,
            String codeB,
            String nameB,
            jolpicaPointsLoader.Result total
    ) {
        render(container, teams, season, codeA, nameA, codeB, nameB, total, null, false);
    }

    public static void render(
            ViewGroup container,
            boolean teams,
            String season,
            String codeA,
            String nameA,
            String codeB,
            String nameB,
            jolpicaPointsLoader.Result total,
            predictionWeekendPointsLoader.Data history,
            boolean error
    ) {
        Context context = container.getContext();

        container.removeAllViews();

        View root = LayoutInflater.from(context).inflate(R.layout.view_prediction_standings_points, container, false);

        container.addView(root);

        String labelA = teams ? context.getString(R.string.spw_a) : codeA;
        String labelB = teams ? context.getString(R.string.spw_b) : codeB;

        set(root, R.id.spwContext, context.getString(R.string.spw_period, season, total.round));
        set(root, R.id.spwCodeA, labelA);
        set(root, R.id.spwCodeB, labelB);
        set(root, R.id.spwNameA, nameA);
        set(root, R.id.spwNameB, nameB);
        set(root, R.id.spwPointsA, number(total.pointsA));
        set(root, R.id.spwPointsB, number(total.pointsB));
        set(root, R.id.spwSource, context.getString(teams ? R.string.spw_teams_source : R.string.spw_drivers_source));
        set(root, R.id.spwSideA, labelA);
        set(root, R.id.spwSideB, labelB);

        if (history == null) {
            root.findViewById(R.id.spwHistory).setVisibility(View.GONE);
            set(root, R.id.spwHistoryState, context.getString(error ? R.string.spw_history_error : R.string.spw_loading));

            return;
        }

        root.findViewById(R.id.spwHistoryState).setVisibility(View.GONE);
        root.findViewById(R.id.spwHistory).setVisibility(View.VISIBLE);

        int historySize = history.rows.size();

        set(root, R.id.spwAverageA, average(history.sumA(), historySize));
        set(root, R.id.spwAverageB, average(history.sumB(), historySize));
        set(root, R.id.spwCount, context.getString(R.string.pf_period_weekends, historySize));

        root.findViewById(R.id.spwEmpty).setVisibility(historySize == 0 ? View.VISIBLE : View.GONE);

        renderCoverage(context, root, history);
        renderMismatchWarning(root, history, total);
        renderRows(context, root, history, historySize);
    }

    private static void renderCoverage(Context context, View root, predictionWeekendPointsLoader.Data history) {
        String coverage = "";

        if (history.skipped > 0) {
            coverage = context.getString(R.string.pf_skipped, history.skipped);
        }

        if (history.calendarMismatches > 0) {
            String calendarWarning = context.getString(R.string.pf_calendar_mismatch, history.calendarMismatches);

            coverage += coverage.isEmpty() ? "" : "\n" + calendarWarning;
        }

        root.findViewById(R.id.spwSkipped).setVisibility(coverage.isEmpty() ? View.GONE : View.VISIBLE);
        set(root, R.id.spwSkipped, coverage);
    }

    private static void renderMismatchWarning(View root, predictionWeekendPointsLoader.Data history, jolpicaPointsLoader.Result total) {
        boolean mismatch = Math.abs(history.sumA() - total.pointsA) > 1e-6
                || Math.abs(history.sumB() - total.pointsB) > 1e-6;

        root.findViewById(R.id.spwMismatch).setVisibility(mismatch ? View.VISIBLE : View.GONE);
    }

    private static void renderRows(Context context, View root, predictionWeekendPointsLoader.Data history, int historySize) {
        LinearLayout rows = root.findViewById(R.id.spwRows);

        for (int i = historySize - 1; i >= Math.max(0, historySize - 5); i--) {
            predictionWeekendPointsLoader.Row item = history.rows.get(i);

            View row = LayoutInflater.from(context).inflate(R.layout.item_prediction_weekend_points, rows, false);

            set(row, R.id.spwRace, predictionRaceNameFormatter.getName(context, item.raceName));
            set(row, R.id.spwValueA, value(context, item.hasSprint, item.raceA, item.sprintA));
            set(row, R.id.spwValueB, value(context, item.hasSprint, item.raceB, item.sprintB));

            rows.addView(row);
        }
    }

    private static String value(Context context, boolean hasSprint, double race, double sprint) {
        if (!hasSprint) {
            return number(race);
        }

        return context.getString(
                R.string.spw_total_parts,
                number(race + sprint),
                number(race),
                number(sprint)
        );
    }

    private static String average(double total, int count) {
        Double value = predictionWeekendRules.average(total, count);

        return value == null ? "—" : String.format(Locale.getDefault(), "%.1f", value);
    }

    private static String number(double value) {
        NumberFormat format = NumberFormat.getNumberInstance(Locale.getDefault());

        format.setGroupingUsed(false);
        format.setMaximumFractionDigits(3);

        return format.format(value);
    }

    private static void set(View root, int id, String value) {
        ((TextView) root.findViewById(id)).setText(value == null ? "—" : value);
    }
}