package com.example.f1app;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.graphics.Typeface;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.AbsoluteSizeSpan;
import android.text.style.StyleSpan;

import java.util.List;
import java.util.Locale;

public class predictionDuelStatsCalculator {

    private static final int LAST_EVENTS_LIMIT = 5;

    public predictionDuelStatsCalculator() {
    }

    public String buildRaceDuelBody(
            Context context,
            String choiceATitle,
            String choiceBTitle,
            List<raceDuelEvent> events
    ) {
        int winsA = 0;
        int winsB = 0;
        int retirementsA = 0;
        int retirementsB = 0;

        double pointsA = 0;
        double pointsB = 0;
        int pointsEventsA = 0;
        int pointsEventsB = 0;

        List<String> recent = new ArrayList<>();

        for (raceDuelEvent event : events) {
            Integer positionA = event.getPositionA();
            Integer positionB = event.getPositionB();

            boolean finishedA = isFinished(positionA);
            boolean finishedB = isFinished(positionB);

            if (finishedA && finishedB) {
                if (positionA < positionB) {
                    winsA++;
                } else if (positionB < positionA) {
                    winsB++;
                }
            } else if (finishedA) {
                winsA++;
            } else if (finishedB) {
                winsB++;
            }

            if (isRetirement(event.getStatusA())) {
                retirementsA++;
            }

            if (isRetirement(event.getStatusB())) {
                retirementsB++;
            }

            if (event.getPointsA() != null) {
                pointsA += event.getPointsA();
                pointsEventsA++;
            }

            if (event.getPointsB() != null) {
                pointsB += event.getPointsB();
                pointsEventsB++;
            }
        }

        double averageA = pointsEventsA == 0
                ? 0
                : pointsA / pointsEventsA;

        double averageB = pointsEventsB == 0
                ? 0
                : pointsB / pointsEventsB;

        StringBuilder builder = new StringBuilder();

        builder.append(choiceATitle)
                .append(" — ")
                .append(choiceBTitle)
                .append("\n\n");

        builder.append("Счёт: ")
                .append(winsA)
                .append(":")
                .append(winsB)
                .append("\n");

        builder.append("Средние очки: ")
                .append(formatOneDecimal(averageA))
                .append(" : ")
                .append(formatOneDecimal(averageB))
                .append("\n");

        builder.append("Сходы: ")
                .append(retirementsA)
                .append(" : ")
                .append(retirementsB);

        appendRecentEvents(
                context,
                builder,
                events,
                "Последние 5 совместных гонок"
        );

        return builder.toString();
    }

    public String buildQualifyingDuelBody(
            String choiceATitle,
            String choiceBTitle,
            List<qualifyingDuelEvent> events
    ) {
        int winsA = 0;
        int winsB = 0;

        int positionsA = 0;
        int positionsB = 0;
        int countedA = 0;
        int countedB = 0;

        for (qualifyingDuelEvent event : events) {
            Integer positionA = event.getPositionA();
            Integer positionB = event.getPositionB();

            if (positionA != null && positionB != null) {
                if (positionA < positionB) {
                    winsA++;
                } else if (positionB < positionA) {
                    winsB++;
                }
            } else if (positionA != null) {
                winsA++;
            } else if (positionB != null) {
                winsB++;
            }

            if (positionA != null) {
                positionsA += positionA;
                countedA++;
            }

            if (positionB != null) {
                positionsB += positionB;
                countedB++;
            }
        }

        double averageA = countedA == 0
                ? 0
                : (double) positionsA / countedA;

        double averageB = countedB == 0
                ? 0
                : (double) positionsB / countedB;

        StringBuilder builder = new StringBuilder();

        builder.append(choiceATitle)
                .append(" — ")
                .append(choiceBTitle)
                .append("\n\n");

        builder.append("Счёт: ")
                .append(winsA)
                .append(":")
                .append(winsB)
                .append("\n");

        builder.append("Средняя позиция: ")
                .append(formatOneDecimal(averageA))
                .append(" : ")
                .append(formatOneDecimal(averageB));

        appendRecentQualifyingEvents(
                builder,
                events,
                "Последние 5 квалификаций"
        );

        return builder.toString();
    }

    private void appendRecentEvents(
            Context context,
            StringBuilder builder,
            List<raceDuelEvent> events,
            String header
    ) {
        builder.append("\n\n")
                .append(header)
                .append(":\n");

        int shown = 0;

        for (int i = events.size() - 1;
             i >= 0 && shown < LAST_EVENTS_LIMIT;
             i--) {
            raceDuelEvent event = events.get(i);

            builder.append(
                            predictionRaceNameFormatter.getName(
                                    context,
                                    event.getRaceName()
                            )
                    )
                    .append(" — ")
                    .append(formatRaceFinish(
                            context,
                            event.getResultA()
                    ))
                    .append(" : ")
                    .append(formatRaceFinish(
                            context,
                            event.getResultB()
                    ))
                    .append("\n");

            shown++;
        }
    }

    private String formatRaceFinish(Context context, String rawResult) {
        if (rawResult == null || rawResult.trim().isEmpty()) {
            return "—";
        }

        String result = rawResult.trim();

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

    private void appendRecentQualifyingEvents(
            StringBuilder builder,
            List<qualifyingDuelEvent> events,
            String header
    ) {
        int from = Math.max(0, events.size() - LAST_EVENTS_LIMIT);

        builder.append("\n\n")
                .append(header)
                .append(":\n");

        for (int i = from; i < events.size(); i++) {
            qualifyingDuelEvent event = events.get(i);

            builder.append(event.getRaceName())
                    .append(" — ")
                    .append(formatPosition(event.getPositionA()))
                    .append(" : ")
                    .append(formatPosition(event.getPositionB()))
                    .append("\n");
        }
    }

    private boolean isFinished(Integer position) {
        return position != null && position > 0;
    }

    private boolean isRetirement(String status) {
        return "R".equals(status);
    }

    private String formatPosition(Integer position) {
        return position == null ? "—" : "P" + position;
    }

    private String formatOneDecimal(double value) {
        return String.format(java.util.Locale.US, "%.1f", value);
    }


    public CharSequence buildRaceDuelStyledBody(
            Context context,
            List<raceDuelEvent> events
    ) {
        int winsA = 0;
        int winsB = 0;
        int retirementsA = 0;
        int retirementsB = 0;

        double pointsA = 0;
        double pointsB = 0;

        int countedA = 0;
        int countedB = 0;

        for (raceDuelEvent event : events) {
            Integer positionA = event.getPositionA();
            Integer positionB = event.getPositionB();

            if (positionA != null && positionB != null) {
                if (positionA < positionB) {
                    winsA++;
                } else if (positionB < positionA) {
                    winsB++;
                }
            } else if (positionA != null) {
                winsA++;
            } else if (positionB != null) {
                winsB++;
            }

            if ("R".equals(event.getStatusA())) {
                retirementsA++;
            }

            if ("R".equals(event.getStatusB())) {
                retirementsB++;
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

        double averageA = countedA == 0 ? 0 : pointsA / countedA;
        double averageB = countedB == 0 ? 0 : pointsB / countedB;

        SpannableStringBuilder text = new SpannableStringBuilder();

        appendStyled(
                text,
                winsA + " : " + winsB,
                30,
                true
        );

        text.append("\n");

        appendStyled(
                text,
                context.getString(R.string.prediction_stats_score_caption),
                13,
                false
        );

        text.append("\n\n");

        appendStyled(
                text,
                context.getString(
                        R.string.prediction_stats_average_points_row,
                        String.format(Locale.getDefault(), "%.1f", averageA),
                        String.format(Locale.getDefault(), "%.1f", averageB)
                ),
                15,
                false
        );

        text.append("\n");

        appendStyled(
                text,
                context.getString(
                        R.string.prediction_stats_retirements_row,
                        retirementsA,
                        retirementsB
                ),
                15,
                false
        );

        text.append("\n\n");

        appendStyled(
                text,
                context.getString(R.string.prediction_stats_recent_races),
                15,
                true
        );

        int shown = 0;

        for (int i = events.size() - 1;
             i >= 0 && shown < LAST_EVENTS_LIMIT;
             i--) {
            raceDuelEvent event = events.get(i);

            text.append("\n\n");

            appendStyled(
                    text,
                    predictionRaceNameFormatter.getName(
                            context,
                            event.getRaceName()
                    ),
                    14,
                    false
            );

            text.append("\n");

            appendStyled(
                    text,
                    formatRaceFinish(context, event.getResultA())
                            + " : "
                            + formatRaceFinish(context, event.getResultB()),
                    18,
                    true
            );

            shown++;
        }

        return text;
    }

    private void appendStyled(
            SpannableStringBuilder builder,
            String value,
            int sizeSp,
            boolean bold
    ) {
        int start = builder.length();

        builder.append(value);

        int end = builder.length();

        builder.setSpan(
                new AbsoluteSizeSpan(sizeSp, true),
                start,
                end,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        );

        if (bold) {
            builder.setSpan(
                    new StyleSpan(Typeface.BOLD),
                    start,
                    end,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            );
        }
    }
}