package com.example.f1app;

import java.util.List;

public class predictionSingleStatsCalculator {

    public String buildDriverSeasonBody(
            String driverTitle,
            List<raceDuelEvent> events,
            int seasonPoints,
            int championshipPosition,
            int driversCount
    ) {
        int wins = 0;
        int podiums = 0;
        int retirements = 0;

        for (raceDuelEvent event : events) {
            Integer position = event.getPositionA();

            if (position != null) {
                if (position == 1) {
                    wins++;
                }

                if (position <= 3) {
                    podiums++;
                }
            } else if (isRetirement(event.getStatusA())) {
                retirements++;
            }
        }

        StringBuilder builder = new StringBuilder();

        builder.append(driverTitle)
                .append("\n\n");

        builder.append("Очки в сезоне: ")
                .append(seasonPoints)
                .append("\n");

        if (driversCount > 0) {
            builder.append("Позиция в чемпионате: ")
                    .append(championshipPosition)
                    .append(" из ")
                    .append(driversCount)
                    .append("\n");
        }

        builder.append("Побед в сезоне: ")
                .append(wins)
                .append("\n");

        builder.append("Подиумов в сезоне: ")
                .append(podiums)
                .append("\n");

        builder.append("Сходов в сезоне: ")
                .append(retirements);

        appendRecentRaces(builder, events);

        return builder.toString();
    }

    private void appendRecentRaces(
            StringBuilder builder,
            List<raceDuelEvent> events
    ) {
        int from = Math.max(0, events.size() - 5);

        builder.append("\n\nПоследние 5 гонок:\n");

        for (int i = from; i < events.size(); i++) {
            raceDuelEvent event = events.get(i);

            builder.append(event.getRaceName())
                    .append(" — ")
                    .append(event.getResultA())
                    .append("\n");
        }
    }

    private boolean isRetirement(String status) {
        return "R".equals(status);
    }
}