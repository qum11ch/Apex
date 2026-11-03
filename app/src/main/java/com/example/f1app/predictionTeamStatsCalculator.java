package com.example.f1app;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class predictionTeamStatsCalculator {

    public String buildTeamDuelBody(
            String teamATitle,
            String teamBTitle,
            List<teamRaceStats> teamA,
            List<teamRaceStats> teamB
    ) {
        int pointsA = calculateSeasonPoints(teamA);
        int pointsB = calculateSeasonPoints(teamB);

        int winsA = calculateWins(teamA);
        int winsB = calculateWins(teamB);

        double averageA = calculateAveragePoints(teamA);
        double averageB = calculateAveragePoints(teamB);

        StringBuilder builder = new StringBuilder();

        builder.append(teamATitle)
                .append(" — ")
                .append(teamBTitle)
                .append("\n\n");

        builder.append("Очки в сезоне: ")
                .append(pointsA)
                .append(" : ")
                .append(pointsB)
                .append("\n");

        builder.append("Средние очки за гонку: ")
                .append(formatOneDecimal(averageA))
                .append(" : ")
                .append(formatOneDecimal(averageB))
                .append("\n");

        builder.append("Победы команд: ")
                .append(winsA)
                .append(" : ")
                .append(winsB);

        appendRecentRaces(
                builder,
                teamATitle,
                teamBTitle,
                teamA,
                teamB
        );

        return builder.toString();
    }

    public String buildSingleTeamBody(
            String teamTitle,
            List<teamRaceStats> team,
            Map<String, Integer> allTeamPoints,
            String teamId
    ) {
        int points = calculateSeasonPoints(team);
        int wins = calculateWins(team);
        double average = calculateAveragePoints(team);

        StringBuilder builder = new StringBuilder();

        builder.append(teamTitle)
                .append("\n\n");

        builder.append("Очки в сезоне: ")
                .append(points)
                .append("\n");

        builder.append("Позиция в кубке конструкторов: ")
                .append(calculateChampionshipPosition(
                        team,
                        allTeamPoints,
                        teamId
                ))
                .append(" из ")
                .append(allTeamPoints.size())
                .append("\n");

        builder.append("Средние очки за гонку: ")
                .append(formatOneDecimal(average))
                .append("\n");

        builder.append("Победы команды: ")
                .append(wins);

        return builder.toString();
    }

    private int calculateSeasonPoints(List<teamRaceStats> team) {
        int points = 0;

        for (teamRaceStats race : team) {
            points += race.getPoints();
        }

        return points;
    }

    private int calculateWins(List<teamRaceStats> team) {
        int wins = 0;

        for (teamRaceStats race : team) {
            if (race.hasWin()) {
                wins++;
            }
        }

        return wins;
    }

    private double calculateAveragePoints(List<teamRaceStats> team) {
        if (team.isEmpty()) {
            return 0;
        }

        return (double) calculateSeasonPoints(team) / team.size();
    }

    private int calculateChampionshipPosition(
            List<teamRaceStats> team,
            Map<String, Integer> allTeamPoints,
            String teamId
    ) {
        int teamPoints = allTeamPoints.getOrDefault(teamId, 0);

        int position = 1;

        for (Map.Entry<String, Integer> entry : allTeamPoints.entrySet()) {
            if (!entry.getKey().equals(teamId)
                    && entry.getValue() > teamPoints) {
                position++;
            }
        }

        return position;
    }

    private void appendRecentRaces(
            StringBuilder builder,
            String teamATitle,
            String teamBTitle,
            List<teamRaceStats> teamA,
            List<teamRaceStats> teamB
    ) {
        Map<String, teamRaceStats> racesB = new HashMap<>();

        for (teamRaceStats race : teamB) {
            racesB.put(race.getRaceName(), race);
        }

        int limit = Math.min(5, teamA.size());

        builder.append("\n\nПоследние 5 гонок:\n");

        for (int i = teamA.size() - limit; i < teamA.size(); i++) {
            teamRaceStats raceA = teamA.get(i);
            teamRaceStats raceB = racesB.get(raceA.getRaceName());

            builder.append(raceA.getRaceName())
                    .append(" — ")
                    .append(raceA.getPoints())
                    .append(" : ")
                    .append(raceB == null ? "—" : raceB.getPoints())
                    .append("\n");
        }
    }

    private String formatOneDecimal(double value) {
        return String.format(java.util.Locale.US, "%.1f", value);
    }
}