package com.example.f1app;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

public final class predictionBooleanHistoryRenderer {
    private predictionBooleanHistoryRenderer() {
    }

    public static void message(ViewGroup container, String description, int messageId) {
        View root = create(container, description);

        root.findViewById(R.id.bhStats).setVisibility(View.GONE);
        set(root, R.id.bhMessage, container.getContext().getString(messageId));
    }

    public static void render(ViewGroup container, String type, String description, predictionBooleanHistoryLoader.Data data) {
        Context context = container.getContext();
        View root = create(container, description);

        root.findViewById(R.id.bhMessage).setVisibility(View.GONE);

        boolean isRain = "rain".equals(type);

        int yes = 0;
        int no = 0;
        int unknown = 0;

        int virtualYes = 0;
        int virtualNo = 0;
        int virtualUnknown = 0;

        for (predictionBooleanHistoryLoader.Row row : data.rows) {
            predictionBooleanHistoryLoader.State state = isRain ? row.rain : row.sc;

            if (state == predictionBooleanHistoryLoader.State.YES) {
                yes++;
            } else if (state == predictionBooleanHistoryLoader.State.NO) {
                no++;
            } else {
                unknown++;
            }

            if (row.vsc == predictionBooleanHistoryLoader.State.YES) {
                virtualYes++;
            } else if (row.vsc == predictionBooleanHistoryLoader.State.NO) {
                virtualNo++;
            } else {
                virtualUnknown++;
            }
        }

        set(root, R.id.bhRatio, String.valueOf(yes));
        set(root, R.id.bhCaption, context.getString(isRain ? R.string.bh3_rain_caption : R.string.bh3_sc_caption));

        String coverage = context.getString(
                R.string.bh3_counts,
                no,
                unknown,
                data.rows.size(),
                data.failedRequests
        );

        if (!data.unavailableYears.isEmpty()) {
            String unavailableYears = TextUtils.join(", ", data.unavailableYears);

            coverage += "\n" + context.getString(R.string.bh4_unavailable_years, unavailableYears);
        }

        set(root, R.id.bhUnknown, coverage);

        root.findViewById(R.id.bhVirtual).setVisibility(isRain ? View.GONE : View.VISIBLE);
        set(root, R.id.bhVirtual, context.getString(R.string.bh3_vsc_counts, virtualYes, virtualNo, virtualUnknown));
        set(root, R.id.bhRule, context.getString(isRain ? R.string.bh3_rain_rule : R.string.bh2_sc_rule));

        LinearLayout rows = root.findViewById(R.id.bhRows);

        for (predictionBooleanHistoryLoader.Row item : data.rows) {
            View row = LayoutInflater.from(context).inflate(R.layout.item_prediction_boolean_history, rows, false);

            set(row, R.id.bhRaceDate, item.circuit + " · " + item.date);
            set(row, R.id.bhOutcome, outcome(context, isRain, item));

            rows.addView(row);
        }

        root.findViewById(R.id.bhEmpty).setVisibility(data.rows.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private static String outcome(Context context, boolean isRain, predictionBooleanHistoryLoader.Row item) {
        if (isRain) {
            if (item.rainError != null) {
                return context.getString(R.string.bh3_network_error);
            }

            if (item.rain == predictionBooleanHistoryLoader.State.UNKNOWN) {
                return rainUnknown(context, item.rainReason);
            }

            return state(context, item.rain);
        }

        if (item.safetyError != null) {
            return context.getString(R.string.bh3_network_error);
        }

        return context.getString(
                R.string.bh_sc_vsc_row,
                state(context, item.sc),
                state(context, item.vsc)
        );
    }

    private static String rainUnknown(Context context, String reason) {
        if ("EDGE_GAP".equals(reason) || "MEASUREMENT_GAP".equals(reason)) {
            return context.getString(R.string.bh3_coverage_gap);
        }

        if (reason != null && reason.startsWith("RACE_")) {
            return context.getString(R.string.bh3_window_unknown);
        }

        return context.getString(R.string.bh_no_data);
    }

    private static View create(ViewGroup container, String description) {
        container.removeAllViews();

        View root = LayoutInflater.from(container.getContext()).inflate(R.layout.view_prediction_boolean_history, container, false);

        container.addView(root);
        root.findViewById(R.id.bhHeading).setVisibility(View.GONE);

        return root;
    }

    private static String state(Context context, predictionBooleanHistoryLoader.State state) {
        if (state == predictionBooleanHistoryLoader.State.YES) {
            return context.getString(R.string.bh_yes);
        }

        if (state == predictionBooleanHistoryLoader.State.NO) {
            return context.getString(R.string.bh_no);
        }

        return context.getString(R.string.bh_no_data);
    }

    private static void set(View root, int id, String value) {
        ((TextView) root.findViewById(id)).setText(value);
    }
}