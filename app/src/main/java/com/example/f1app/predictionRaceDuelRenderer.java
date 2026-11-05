package com.example.f1app;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.List;
import java.util.Locale;

public final class predictionRaceDuelRenderer {
    private predictionRaceDuelRenderer() {
    }

    public static void render(ViewGroup container, String codeA, String nameA, String codeB, String nameB, List<raceDuelEvent> events) {
        Context context = container.getContext();
        View root = inflate(container);

        predictionRaceDuelSummary summary = predictionRaceDuelSummary.calculate(events);

        names(root, codeA, nameA, codeB, nameB);

        text(root, R.id.duelScoreA, String.valueOf(summary.winsA));
        text(root, R.id.duelScoreB, String.valueOf(summary.winsB));
        text(root, R.id.duelScoreCaption, context.getString(R.string.pred_duel_wins));
        text(root, R.id.duelRecentTitle, context.getString(R.string.pred_duel_recent));
        text(root, R.id.duelResultsNote, context.getString(R.string.rd_race_note));

        LinearLayout metrics = root.findViewById(R.id.duelMetrics);

        row(metrics, "", codeA, codeB, true);
        row(metrics, context.getString(R.string.rd_average_finish), decimal(summary.averageA()), decimal(summary.averageB()), false);
        row(metrics, context.getString(R.string.rd_numeric_finishes), String.valueOf(summary.numericA), String.valueOf(summary.numericB), false);
        row(metrics, context.getString(R.string.pred_duel_retirements), String.valueOf(summary.retirementsA), String.valueOf(summary.retirementsB), false);

        LinearLayout recentRows = root.findViewById(R.id.duelRecentRows);

        row(recentRows, context.getString(R.string.pred_duel_race), codeA, codeB, true);

        int shown = 0;

        for (int i = events.size() - 1; i >= 0 && shown < 5; i--) {
            raceDuelEvent event = events.get(i);

            if (event == null) {
                continue;
            }

            row(
                    recentRows,
                    predictionRaceNameFormatter.getName(context, event.getRaceName()),
                    finish(context, event.getResultA()),
                    finish(context, event.getResultB()),
                    false
            );

            shown++;
        }
    }

    public static void renderQualifying(ViewGroup container, String codeA, String nameA, String codeB, String nameB, List<qualifyingDuelEvent> events) {
        Context context = container.getContext();
        View root = inflate(container);

        int winsA = 0;
        int winsB = 0;
        int numeric = 0;

        long positionSumA = 0;
        long positionSumB = 0;

        for (qualifyingDuelEvent event : events) {
            if (!both(event)) {
                continue;
            }

            int positionA = event.getPositionA();
            int positionB = event.getPositionB();

            if (positionA < positionB) {
                winsA++;
            } else if (positionB < positionA) {
                winsB++;
            }

            positionSumA += positionA;
            positionSumB += positionB;
            numeric++;
        }

        names(root, codeA, nameA, codeB, nameB);

        text(root, R.id.duelScoreA, String.valueOf(winsA));
        text(root, R.id.duelScoreB, String.valueOf(winsB));
        text(root, R.id.duelScoreCaption, context.getString(R.string.pred_quali_duel_wins));
        text(root, R.id.duelRecentTitle, context.getString(R.string.pred_quali_duel_recent));
        text(root, R.id.duelResultsNote, context.getString(R.string.pred_quali_duel_note));

        LinearLayout metrics = root.findViewById(R.id.duelMetrics);

        row(metrics, "", codeA, codeB, true);
        row(
                metrics,
                context.getString(R.string.pred_quali_duel_average),
                numeric == 0 ? "—" : decimal((double) positionSumA / numeric),
                numeric == 0 ? "—" : decimal((double) positionSumB / numeric),
                false
        );

        LinearLayout recentRows = root.findViewById(R.id.duelRecentRows);

        row(recentRows, context.getString(R.string.pred_duel_race), codeA, codeB, true);

        int shown = 0;

        for (int i = events.size() - 1; i >= 0 && shown < 5; i--) {
            qualifyingDuelEvent event = events.get(i);

            if (!both(event)) {
                continue;
            }

            row(
                    recentRows,
                    predictionRaceNameFormatter.getName(context, event.getRaceName()),
                    String.valueOf(event.getPositionA()),
                    String.valueOf(event.getPositionB()),
                    false
            );

            shown++;
        }
    }

    private static boolean both(qualifyingDuelEvent event) {
        return event != null
                && event.getPositionA() != null && event.getPositionA() > 0
                && event.getPositionB() != null && event.getPositionB() > 0;
    }

    private static View inflate(ViewGroup container) {
        container.removeAllViews();

        View root = LayoutInflater.from(container.getContext()).inflate(R.layout.view_prediction_duel_stats, container, false);

        container.addView(root);

        return root;
    }

    private static void names(View root, String codeA, String nameA, String codeB, String nameB) {
        text(root, R.id.duelCodeA, codeA);
        text(root, R.id.duelNameA, nameA);
        text(root, R.id.duelCodeB, codeB);
        text(root, R.id.duelNameB, nameB);
    }

    private static String decimal(Double value) {
        return value == null ? "—" : String.format(Locale.getDefault(), "%.1f", value);
    }

    private static String finish(Context context, String raw) {
        if (raw == null) {
            return "—";
        }

        String value = raw.trim();

        if ("N/C".equals(value)) {
            return context.getString(R.string.n_c_text);
        }

        if ("CND".equals(value)) {
            return context.getString(R.string.cnd_text);
        }

        if ("NP".equals(value)) {
            return context.getString(R.string.dns_text);
        }

        String[] parts = value.split("-", 2);

        if (parts.length != 2) {
            return "—";
        }

        value = parts[1].trim();

        switch (value) {
            case "R":
                return context.getString(R.string.ret_text);
            case "W":
                return context.getString(R.string.wd_text);
            case "D":
                return context.getString(R.string.dsq_text);
            case "NC":
                return context.getString(R.string.rd_not_classified);
            default:
                try {
                    int position = Integer.parseInt(value);

                    return position > 0 ? String.valueOf(position) : "—";
                } catch (Exception exception) {
                    return "—";
                }
        }
    }

    private static void text(View root, int id, String value) {
        ((TextView) root.findViewById(id)).setText(value == null ? "—" : value);
    }

    private static void row(LinearLayout parent, String label, String valueA, String valueB, boolean header) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_prediction_comparison_row, parent, false);

        text(view, R.id.comparisonLabel, label);
        text(view, R.id.comparisonValueA, valueA);
        text(view, R.id.comparisonValueB, valueB);

        if (header) {
            int[] headerIds = {R.id.comparisonLabel, R.id.comparisonValueA, R.id.comparisonValueB};

            for (int id : headerIds) {
                TextView textView = view.findViewById(id);

                textView.setTextSize(12);
                textView.setAlpha(0.65f);
            }
        }

        parent.addView(view);
    }
}