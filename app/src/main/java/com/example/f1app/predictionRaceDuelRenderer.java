package com.example.f1app;

import android.content.Context;
import android.graphics.Typeface;
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

    public static void render(
            ViewGroup container,
            String codeA,
            String nameA,
            String codeB,
            String nameB,
            List<raceDuelEvent> events
    ) {
        Context context = container.getContext();
        LayoutInflater inflater = LayoutInflater.from(context);

        container.removeAllViews();

        View content = inflater.inflate(
                R.layout.view_prediction_duel_stats,
                container,
                false
        );

        container.addView(content);

        Summary summary = calculate(events);

        setText(content, R.id.duelCodeA, codeA);
        setText(content, R.id.duelCodeB, codeB);
        setText(content, R.id.duelNameA, nameA);
        setText(content, R.id.duelNameB, nameB);

        setText(
                content,
                R.id.duelScoreA,
                String.valueOf(summary.winsA)
        );

        setText(
                content,
                R.id.duelScoreB,
                String.valueOf(summary.winsB)
        );

        LinearLayout metrics = content.findViewById(R.id.duelMetrics);

        addRow(metrics, "", codeA, codeB, true);

        addRow(
                metrics,
                context.getString(R.string.pred_duel_average),
                decimal(summary.averageA),
                decimal(summary.averageB),
                false
        );

        addRow(
                metrics,
                context.getString(R.string.pred_duel_retirements),
                String.valueOf(summary.retirementsA),
                String.valueOf(summary.retirementsB),
                false
        );

        LinearLayout recent = content.findViewById(R.id.duelRecentRows);

        addRow(
                recent,
                context.getString(R.string.pred_duel_race),
                codeA,
                codeB,
                true
        );

        int shown = 0;

        for (int i = events.size() - 1; i >= 0 && shown < 5; i--) {
            raceDuelEvent event = events.get(i);

            addRow(
                    recent,
                    predictionRaceNameFormatter.getName(
                            context,
                            event.getRaceName()
                    ),
                    formatFinish(context, event.getResultA()),
                    formatFinish(context, event.getResultB()),
                    false
            );

            shown++;
        }
    }

    private static Summary calculate(List<raceDuelEvent> events) {
        Summary result = new Summary();

        double pointsA = 0;
        double pointsB = 0;
        int countedA = 0;
        int countedB = 0;

        for (raceDuelEvent event : events) {
            Integer a = event.getPositionA();
            Integer b = event.getPositionB();

            if (a != null && b != null) {
                if (a < b) {
                    result.winsA++;
                } else if (b < a) {
                    result.winsB++;
                }
            } else if (a != null) {
                result.winsA++;
            } else if (b != null) {
                result.winsB++;
            }

            if ("R".equals(event.getStatusA())) {
                result.retirementsA++;
            }

            if ("R".equals(event.getStatusB())) {
                result.retirementsB++;
            }

            if (event.getPointsA() != null) {
                pointsA += event.getPointsA();
                countedA++;
            }

            if (event.getPointsB() != null) {
                pointsB += event.getPointsB();
                countedB++;
            }
        }

        result.averageA = countedA == 0 ? 0 : pointsA / countedA;
        result.averageB = countedB == 0 ? 0 : pointsB / countedB;

        return result;
    }

    private static void addRow(
            LinearLayout parent,
            String label,
            String valueA,
            String valueB,
            boolean header
    ) {
        View row = LayoutInflater.from(parent.getContext()).inflate(
                R.layout.item_prediction_comparison_row,
                parent,
                false
        );

        TextView labelView = row.findViewById(R.id.comparisonLabel);
        TextView a = row.findViewById(R.id.comparisonValueA);
        TextView b = row.findViewById(R.id.comparisonValueB);

        labelView.setText(label);
        a.setText(valueA);
        b.setText(valueB);

        if (header) {
            labelView.setTextSize(12);
            a.setTextSize(12);
            b.setTextSize(12);

            labelView.setAlpha(0.65f);
            a.setAlpha(0.65f);
            b.setAlpha(0.65f);

            labelView.setTypeface(
                    labelView.getTypeface(),
                    Typeface.NORMAL
            );
        }

        parent.addView(row);
    }

    private static void setText(View root, int id, String value) {
        TextView view = root.findViewById(id);
        view.setText(value == null ? "—" : value);
    }

    private static String decimal(double value) {
        return String.format(Locale.getDefault(), "%.1f", value);
    }

    private static String formatFinish(Context context, String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return "—";
        }

        String result = raw.trim();

        switch (result) {
            case "N/C":
                return context.getString(R.string.n_c_text);
            case "CND":
                return context.getString(R.string.cnd_text);
            case "NP":
                return context.getString(R.string.dns_text);
            default:
                break;
        }

        String[] parts = result.split("-", 2);

        if (parts.length != 2) {
            return "—";
        }

        String finish = parts[1].trim();

        switch (finish) {
            case "R":
                return context.getString(R.string.ret_text);
            case "W":
                return context.getString(R.string.wd_text);
            case "D":
                return context.getString(R.string.dsq_text);
            default:
                try {
                    int position = Integer.parseInt(finish);
                    return position > 0
                            ? String.valueOf(position)
                            : "—";
                } catch (NumberFormatException error) {
                    return "—";
                }
        }
    }

    private static final class Summary {
        int winsA;
        int winsB;
        int retirementsA;
        int retirementsB;
        double averageA;
        double averageB;
    }

    public static void renderQualifying(
            ViewGroup container,
            String codeA,
            String nameA,
            String codeB,
            String nameB,
            List<qualifyingDuelEvent> events
    ) {
        Context context = container.getContext();

        container.removeAllViews();

        View content = LayoutInflater.from(context).inflate(
                R.layout.view_prediction_duel_stats,
                container,
                false
        );

        container.addView(content);

        int winsA = 0;
        int winsB = 0;

        long positionsA = 0;
        long positionsB = 0;
        int jointEvents = 0;

        for (qualifyingDuelEvent event : events) {
            if (!hasBothQualifyingPositions(event)) {
                continue;
            }

            int positionA = event.getPositionA();
            int positionB = event.getPositionB();

            if (positionA < positionB) {
                winsA++;
            } else if (positionB < positionA) {
                winsB++;
            }

            positionsA += positionA;
            positionsB += positionB;
            jointEvents++;
        }

        setText(content, R.id.duelCodeA, codeA);
        setText(content, R.id.duelCodeB, codeB);
        setText(content, R.id.duelNameA, nameA);
        setText(content, R.id.duelNameB, nameB);

        setText(content, R.id.duelScoreA, String.valueOf(winsA));
        setText(content, R.id.duelScoreB, String.valueOf(winsB));

        setText(
                content,
                R.id.duelScoreCaption,
                context.getString(R.string.pred_quali_duel_wins)
        );

        setText(
                content,
                R.id.duelRecentTitle,
                context.getString(R.string.pred_quali_duel_recent)
        );

        setText(
                content,
                R.id.duelResultsNote,
                context.getString(R.string.pred_quali_duel_note)
        );

        LinearLayout metrics = content.findViewById(R.id.duelMetrics);

        addRow(metrics, "", codeA, codeB, true);

        addRow(
                metrics,
                context.getString(R.string.pred_quali_duel_average),
                jointEvents == 0
                        ? "—"
                        : decimal((double) positionsA / jointEvents),
                jointEvents == 0
                        ? "—"
                        : decimal((double) positionsB / jointEvents),
                false
        );

        LinearLayout recent = content.findViewById(R.id.duelRecentRows);

        addRow(
                recent,
                context.getString(R.string.pred_duel_race),
                codeA,
                codeB,
                true
        );

        int shown = 0;

        for (int i = events.size() - 1; i >= 0 && shown < 5; i--) {
            qualifyingDuelEvent event = events.get(i);

            if (!hasBothQualifyingPositions(event)) {
                continue;
            }

            addRow(
                    recent,
                    predictionRaceNameFormatter.getName(
                            context,
                            event.getRaceName()
                    ),
                    String.valueOf(event.getPositionA()),
                    String.valueOf(event.getPositionB()),
                    false
            );

            shown++;
        }
    }

    private static boolean hasBothQualifyingPositions(
            qualifyingDuelEvent event
    ) {
        return event != null
                && event.getPositionA() != null
                && event.getPositionA() > 0
                && event.getPositionB() != null
                && event.getPositionB() > 0;
    }
}