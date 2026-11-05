package com.example.f1app;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class predictionSingleCardRenderer {
    private predictionSingleCardRenderer() {
    }

    public static void render(ViewGroup container, predictionSingleCardLoader.Data data) {
        Context context = container.getContext();

        container.removeAllViews();

        View root = LayoutInflater.from(context).inflate(R.layout.view_prediction_single_stats, container, false);

        container.addView(root);

        set(root, R.id.singleName, data.title);
        set(root, R.id.singleContext, period(context, data));
        set(root, R.id.singlePoints, number(data.points));
        set(root, R.id.singleWins, String.valueOf(data.wins));
        set(root, R.id.singlePodiums, String.valueOf(data.podiums));
        set(root, R.id.singleRetirements, String.valueOf(data.retirements));
        set(root, R.id.singleCount, context.getString(R.string.pf_races, data.rows.size()));

        renderHistory(context, root, data);
        renderRecentRaces(context, root, data);
    }

    private static String period(Context context, predictionSingleCardLoader.Data data) {
        String period = context.getString(R.string.pf_period, data.season, data.throughRound);

        if (data.targetRace == null) {
            return period;
        }

        String targetRace = predictionRaceNameFormatter.getNameWithSeason(context, data.targetRace, data.season);

        return targetRace + "\n" + context.getString(R.string.pf_before) + " · " + period;
    }

    private static void renderHistory(Context context, View root, predictionSingleCardLoader.Data data) {
        LinearLayout weekendRows = root.findViewById(R.id.singleWeekendRows);

        if (data.history == null) {
            set(root, R.id.singleAverage, context.getString(data.historyError ? R.string.pf_history_error : R.string.pf_loading));

            root.findViewById(R.id.singleWeekendCaption).setVisibility(View.GONE);
            root.findViewById(R.id.singleCoverage).setVisibility(View.GONE);
            root.findViewById(R.id.singleCalendarWarning).setVisibility(View.GONE);
            root.findViewById(R.id.singleTotalWarning).setVisibility(View.GONE);

            return;
        }

        int historySize = data.history.rows.size();
        Double average = predictionWeekendRules.average(data.history.sumA(), historySize);

        set(
                root,
                R.id.singleAverage,
                context.getString(
                        R.string.pf_average,
                        average == null ? "—" : String.format(Locale.getDefault(), "%.1f", average),
                        historySize
                )
        );

        renderWeekendRows(context, weekendRows, data.history);

        renderCoverage(context, root, data.history);
        renderCalendarWarning(context, root, data.history);
        renderTotalWarning(root, data.history, data.points);
    }

    private static void renderWeekendRows(Context context, LinearLayout weekendRows, predictionWeekendPointsLoader.Data history) {
        int historySize = history.rows.size();

        for (int i = historySize - 1; i >= Math.max(0, historySize - 5); i--) {
            predictionWeekendPointsLoader.Row row = history.rows.get(i);

            String value = number(row.totalA());

            if (row.hasSprint) {
                value = context.getString(R.string.pf_parts, value, number(row.raceA), number(row.sprintA));
            }

            add(weekendRows, predictionRaceNameFormatter.getName(context, row.raceName), value);
        }

        if (historySize == 0) {
            add(weekendRows, context.getString(R.string.pf_empty), "—");
        }
    }

    private static void renderCoverage(Context context, View root, predictionWeekendPointsLoader.Data history) {
        if (history.skipped > 0) {
            set(root, R.id.singleCoverage, context.getString(R.string.pf_skipped, history.skipped));

            return;
        }

        root.findViewById(R.id.singleCoverage).setVisibility(View.GONE);
    }

    private static void renderCalendarWarning(Context context, View root, predictionWeekendPointsLoader.Data history) {
        if (history.calendarMismatches > 0) {
            set(root, R.id.singleCalendarWarning, context.getString(R.string.pf_calendar_mismatch, history.calendarMismatches));

            return;
        }

        root.findViewById(R.id.singleCalendarWarning).setVisibility(View.GONE);
    }

    private static void renderTotalWarning(View root, predictionWeekendPointsLoader.Data history, double points) {
        boolean mismatch = Math.abs(history.sumA() - points) > 1e-6;

        root.findViewById(R.id.singleTotalWarning).setVisibility(mismatch ? View.VISIBLE : View.GONE);
    }

    private static void renderRecentRaces(Context context, View root, predictionSingleCardLoader.Data data) {
        LinearLayout raceRows = root.findViewById(R.id.singleRecentRows);

        for (int i = data.rows.size() - 1; i >= Math.max(0, data.rows.size() - 5); i--) {
            predictionSingleCardLoader.Row row = data.rows.get(i);
            List<String> values = new ArrayList<>();

            for (String finish : row.results) {
                values.add(finish(context, finish));
            }

            add(raceRows, predictionRaceNameFormatter.getName(context, row.raceName), TextUtils.join(" / ", values));
        }

        if (data.rows.isEmpty()) {
            add(raceRows, context.getString(R.string.pf_empty), "—");
        }
    }

    private static String finish(Context context, String value) {
        switch (value) {
            case "R":
                return context.getString(R.string.ret_text);
            case "W":
                return context.getString(R.string.wd_text);
            case "D":
                return context.getString(R.string.dsq_text);
            case "NC":
                return context.getString(R.string.pf_nc);
            default:
                return value;
        }
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

    private static void add(LinearLayout parent, String label, String value) {
        View row = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_prediction_single_result, parent, false);

        set(row, R.id.singleRaceName, label);
        set(row, R.id.singleRaceValue, value);

        parent.addView(row);
    }
}