package com.example.f1app;

import android.content.Context;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class predictionStatsLoader {

    public interface raceStatsCallback {
        void onLoaded(List<raceDuelEvent> events);

        void onError();
    }

    public interface qualifyingStatsCallback {
        void onLoaded(List<qualifyingDuelEvent> events);

        void onError();
    }

    public interface weekendCallback {
        void onLoaded(weekendRaceData race);

        void onError();
    }

    private final DatabaseReference rootRef;

    public predictionStatsLoader() {
        this.rootRef = FirebaseDatabase.getInstance().getReference();
    }

    public void loadRaceDuelEvents(
            Context context,
            String season,
            String choiceACode,
            String choiceBCode,
            raceStatsCallback callback
    ) {
        if (season == null || choiceACode == null || choiceBCode == null) {
            callback.onError();
            return;
        }

        rootRef.child("drivers")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot driversSnapshot) {
                        String nameA = findDriverName(
                                driversSnapshot,
                                choiceACode
                        );

                        String nameB = findDriverName(
                                driversSnapshot,
                                choiceBCode
                        );

                        if (nameA == null || nameB == null) {
                            callback.onError();
                            return;
                        }

                        loadRaceEventsByDriverNames(
                                season,
                                nameA,
                                nameB,
                                callback
                        );
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        callback.onError();
                    }
                });
    }

    public void loadQualifyingDuelEvents(
            Context context,
            String season,
            Integer raceRound,
            String choiceACode,
            String choiceBCode,
            qualifyingStatsCallback callback
    ) {
        if (season == null
                || raceRound == null
                || choiceACode == null
                || choiceBCode == null) {
            callback.onError();
            return;
        }

        rootRef.child("qualifying")
                .child(season)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot seasonSnapshot) {
                        List<qualifyingDuelEvent> events = new ArrayList<>();

                        for (DataSnapshot roundSnapshot :
                                seasonSnapshot.getChildren()) {
                            Long round = roundSnapshot
                                    .child("round")
                                    .getValue(Long.class);

                            if (round == null || round > raceRound) {
                                continue;
                            }

                            DataSnapshot results = roundSnapshot
                                    .child("results");

                            Integer positionA = findQualifyingPosition(
                                    results,
                                    choiceACode
                            );

                            Integer positionB = findQualifyingPosition(
                                    results,
                                    choiceBCode
                            );

                            if (positionA == null && positionB == null) {
                                continue;
                            }

                            events.add(new qualifyingDuelEvent(
                                    roundSnapshot.child("raceName")
                                            .getValue(String.class),
                                    positionA,
                                    positionB
                            ));
                        }

                        callback.onLoaded(events);
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        callback.onError();
                    }
                });
    }

    public void loadWeekendRace(
            Context context,
            String season,
            Integer raceRound,
            weekendCallback callback
    ) {
        if (season == null || raceRound == null) {
            callback.onError();
            return;
        }

        rootRef.child("schedule")
                .child("season")
                .child(season)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        for (DataSnapshot child : snapshot.getChildren()) {
                            Long round = child.child("round").getValue(Long.class);

                            if (round == null
                                    || raceRound != round.intValue()) {
                                continue;
                            }

                            weekendRaceData race = createWeekendRace(child);

                            if (race != null) {
                                callback.onLoaded(race);
                                return;
                            }
                        }

                        callback.onError();
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        callback.onError();
                    }
                });
    }

    private void loadRaceEventsByDriverNames(
            String season,
            String nameA,
            String nameB,
            raceStatsCallback callback
    ) {
        rootRef.child("schedule")
                .child("season")
                .child(season)
                .orderByChild("round")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot scheduleSnapshot) {
                        List<String> raceOrder = new ArrayList<>();

                        for (DataSnapshot raceSnapshot :
                                scheduleSnapshot.getChildren()) {
                            String raceName = raceSnapshot.getKey();

                            if (raceName != null) {
                                raceOrder.add(raceName);
                            }
                        }

                        rootRef.child("results")
                                .child("season")
                                .child(season)
                                .addListenerForSingleValueEvent(
                                        new ValueEventListener() {
                                            @Override
                                            public void onDataChange(
                                                    DataSnapshot resultsSnapshot
                                            ) {
                                                List<raceDuelEvent> events =
                                                        new ArrayList<>();

                                                for (String raceName : raceOrder) {
                                                    raceDuelEvent event =
                                                            createRaceEvent(
                                                                    resultsSnapshot,
                                                                    raceName,
                                                                    nameA,
                                                                    nameB
                                                            );

                                                    if (event != null) {
                                                        events.add(event);
                                                    }
                                                }

                                                callback.onLoaded(events);
                                            }

                                            @Override
                                            public void onCancelled(
                                                    DatabaseError error
                                            ) {
                                                callback.onError();
                                            }
                                        }
                                );
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        callback.onError();
                    }
                });
    }

    private static class scheduledRace {
        private final String raceName;
        private final int round;

        private scheduledRace(String raceName, int round) {
            this.raceName = raceName;
            this.round = round;
        }
    }

    private boolean isCompletedParticipation(String raw) {
        if (raw == null) {
            return false;
        }

        String value = raw.trim();

        if (value.isEmpty()
                || "N/C".equals(value)
                || "CND".equals(value)
                || "NP".equals(value)) {
            return false;
        }

        String[] parts = value.split("-", 2);

        if (parts.length != 2) {
            return false;
        }

        String finish = parts[1].trim();

        if ("R".equals(finish)
                || "W".equals(finish)
                || "D".equals(finish)
                || "NC".equals(finish)) {
            return true;
        }

        try {
            return Integer.parseInt(finish) > 0;
        } catch (NumberFormatException error) {
            return false;
        }
    }

    private raceDuelEvent createRaceEvent(
            DataSnapshot resultsSnapshot,
            String raceName,
            String nameA,
            String nameB
    ) {
        String rawA = resultsSnapshot
                .child(nameA)
                .child(raceName)
                .child("Result")
                .getValue(String.class);

        String rawB = resultsSnapshot
                .child(nameB)
                .child(raceName)
                .child("Result")
                .getValue(String.class);

        if (!isCompletedParticipation(rawA)
                || !isCompletedParticipation(rawB)) {
            return null;
        }

        rawA = rawA.trim();
        rawB = rawB.trim();

        return new raceDuelEvent(
                raceName,
                rawA,
                rawB,
                parseRacePosition(rawA),
                parseRacePosition(rawB),
                parseRaceStatus(rawA),
                parseRaceStatus(rawB),
                calculateRacePoints(rawA),
                calculateRacePoints(rawB)
        );
    }

    private String findDriverName(
            DataSnapshot driversSnapshot,
            String driverCode
    ) {
        for (DataSnapshot driverSnapshot : driversSnapshot.getChildren()) {
            String code = driverSnapshot
                    .child("driversCode")
                    .getValue(String.class);

            if (driverCode.equals(code)) {
                return driverSnapshot.getKey();
            }
        }

        return null;
    }

    private Integer findQualifyingPosition(
            DataSnapshot resultsSnapshot,
            String driverCode
    ) {
        for (DataSnapshot resultSnapshot : resultsSnapshot.getChildren()) {
            String code = resultSnapshot
                    .child("driverCode")
                    .getValue(String.class);

            if (driverCode.equals(code)) {
                Long position = resultSnapshot
                        .child("position")
                        .getValue(Long.class);

                return position == null ? null : position.intValue();
            }
        }

        return null;
    }

    private weekendRaceData createWeekendRace(DataSnapshot snapshot) {
        String raceName = snapshot.getKey();

        DataSnapshot circuit = snapshot.child("Circuit");

        String circuitId = circuit.child("circuitId").getValue(String.class);
        String circuitRaceName = circuit.child("raceName")
                .getValue(String.class);

        String raceDate = snapshot.child("raceDate").getValue(String.class);
        String firstPracticeDate = snapshot
                .child("FirstPractice")
                .child("firstPracticeDate")
                .getValue(String.class);

        Long round = snapshot.child("round").getValue(Long.class);

        String country = circuit.child("country").getValue(String.class);

        if (raceName == null
                || circuitId == null
                || raceDate == null
                || firstPracticeDate == null
                || round == null) {
            return null;
        }

        LocalDate start = LocalDate.parse(
                firstPracticeDate,
                DateTimeFormatter.ofPattern("yyyy-M-d")
        );

        LocalDate end = LocalDate.parse(
                raceDate,
                DateTimeFormatter.ofPattern("yyyy-M-d")
        );

        return new weekendRaceData(
                raceName,
                circuitId,
                circuitRaceName == null ? raceName : circuitRaceName,
                raceDate,
                firstPracticeDate,
                round.intValue(),
                country == null ? "" : country,
                String.valueOf(start.getDayOfMonth()),
                String.valueOf(end.getDayOfMonth()),
                start.getMonthValue() + "",
                end.getMonthValue() + ""
        );
    }

    private Integer parseRacePosition(String result) {
        if (result == null) {
            return null;
        }

        String[] parts = result.split("-");

        if (parts.length < 2) {
            return null;
        }

        try {
            int position = Integer.parseInt(parts[1]);

            return position > 0 ? position : null;
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private String parseRaceStatus(String result) {
        if (result == null) {
            return null;
        }

        String[] parts = result.split("-");

        return parts.length < 2 ? result : parts[1];
    }

    private Double calculateRacePoints(String result) {
        if (!isCompletedParticipation(result)) {
            return null;
        }

        Integer position = parseRacePosition(result);

        return position == null
                ? 0.0
                : racePointsProvider.getPoints(position);
    }

    public interface singleStatsCallback {
        void onLoaded(
                List<raceDuelEvent> events,
                int seasonPoints,
                int championshipPosition,
                int driversCount
        );

        void onError();
    }

    public void loadDriverSeasonStats(
            Context context,
            String season,
            String driverCode,
            singleStatsCallback callback
    ) {
        if (season == null || driverCode == null) {
            callback.onError();
            return;
        }

        rootRef.child("drivers")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot driversSnapshot) {
                        String driverName = findDriverName(
                                driversSnapshot,
                                driverCode
                        );

                        if (driverName == null) {
                            callback.onError();
                            return;
                        }

                        rootRef.child("results")
                                .child("season")
                                .child(season)
                                .addListenerForSingleValueEvent(
                                        new ValueEventListener() {
                                            @Override
                                            public void onDataChange(
                                                    DataSnapshot seasonSnapshot
                                            ) {
                                                List<raceDuelEvent> events =
                                                        new ArrayList<>();

                                                int seasonPoints = 0;
                                                int driversCount =
                                                        (int) seasonSnapshot
                                                                .getChildrenCount();

                                                for (DataSnapshot driverSnapshot :
                                                        seasonSnapshot.getChildren()) {
                                                    int driverPoints = 0;

                                                    for (DataSnapshot raceSnapshot :
                                                            driverSnapshot
                                                                    .getChildren()) {
                                                        String result = raceSnapshot
                                                                .child("Result")
                                                                .getValue(String.class);

                                                        if (result == null) {
                                                            continue;
                                                        }

                                                        Integer position =
                                                                parseRacePosition(
                                                                        result
                                                                );

                                                        if (position != null) {
                                                            driverPoints +=
                                                                    racePointsProvider
                                                                            .getPoints(
                                                                                    position
                                                                            );
                                                        }

                                                        if (driverName.equals(
                                                                driverSnapshot.getKey()
                                                        )) {
                                                            events.add(
                                                                    new raceDuelEvent(
                                                                            raceSnapshot.getKey(),
                                                                            result,
                                                                            result,
                                                                            position,
                                                                            position,
                                                                            parseRaceStatus(result),
                                                                            parseRaceStatus(result),
                                                                            calculateRacePoints(result),
                                                                            calculateRacePoints(result)
                                                                    )
                                                            );
                                                        }
                                                    }

                                                    if (driverName.equals(
                                                            driverSnapshot.getKey()
                                                    )) {
                                                        seasonPoints = driverPoints;
                                                    }
                                                }

                                                int championshipPosition =
                                                        calculateChampionshipPosition(
                                                                seasonSnapshot,
                                                                driverName
                                                        );

                                                callback.onLoaded(
                                                        events,
                                                        seasonPoints,
                                                        championshipPosition,
                                                        driversCount
                                                );
                                            }

                                            @Override
                                            public void onCancelled(
                                                    DatabaseError error
                                            ) {
                                                callback.onError();
                                            }
                                        }
                                );
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        callback.onError();
                    }
                });
    }

    private int calculateChampionshipPosition(
            DataSnapshot seasonSnapshot,
            String driverName
    ) {
        List<DriverSeasonPoints> standings = new ArrayList<>();

        for (DataSnapshot driverSnapshot : seasonSnapshot.getChildren()) {
            int points = 0;

            for (DataSnapshot raceSnapshot : driverSnapshot.getChildren()) {
                String result = raceSnapshot
                        .child("Result")
                        .getValue(String.class);

                Integer position = parseRacePosition(result);

                if (position != null) {
                    points += racePointsProvider.getPoints(position);
                }
            }

            standings.add(new DriverSeasonPoints(
                    driverSnapshot.getKey(),
                    points
            ));
        }

        standings.sort((a, b) -> Integer.compare(
                b.getPoints(),
                a.getPoints()
        ));

        for (int i = 0; i < standings.size(); i++) {
            if (driverName.equals(standings.get(i).getDriverName())) {
                return i + 1;
            }
        }

        return 0;
    }

    private static class DriverSeasonPoints {

        private final String driverName;
        private final int points;

        private DriverSeasonPoints(
                String driverName,
                int points
        ) {
            this.driverName = driverName;
            this.points = points;
        }

        private String getDriverName() {
            return driverName;
        }

        private int getPoints() {
            return points;
        }
    }

    public interface teamStatsCallback {
        void onLoaded(
                List<teamRaceStats> teamA,
                List<teamRaceStats> teamB
        );

        void onError();
    }

    public void loadTeamDuelStats(
            Context context,
            String season,
            String teamAId,
            String teamBId,
            teamStatsCallback callback
    ) {
        if (season == null || teamAId == null || teamBId == null) {
            callback.onError();
            return;
        }

        rootRef.child("results")
                .child("season")
                .child(season)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot seasonSnapshot) {
                        List<teamRaceStats> teamA = new ArrayList<>();
                        List<teamRaceStats> teamB = new ArrayList<>();

                        for (DataSnapshot raceSnapshot :
                                seasonSnapshot.getChildren()) {
                            String raceName = raceSnapshot.getKey();

                            teamRaceStats statsA = calculateTeamRaceStats(
                                    raceSnapshot,
                                    raceName,
                                    teamAId
                            );

                            teamRaceStats statsB = calculateTeamRaceStats(
                                    raceSnapshot,
                                    raceName,
                                    teamBId
                            );

                            if (statsA != null) {
                                teamA.add(statsA);
                            }

                            if (statsB != null) {
                                teamB.add(statsB);
                            }
                        }

                        if (teamA.isEmpty() && teamB.isEmpty()) {
                            callback.onError();
                            return;
                        }

                        callback.onLoaded(teamA, teamB);
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        callback.onError();
                    }
                });
    }

    private teamRaceStats calculateTeamRaceStats(
            DataSnapshot seasonSnapshot,
            String raceName,
            String teamId
    ) {
        int points = 0;
        boolean hasWin = false;
        boolean hasTeamResult = false;

        for (DataSnapshot driverSnapshot : seasonSnapshot.getChildren()) {
            DataSnapshot raceSnapshot = driverSnapshot.child(raceName);

            if (!raceSnapshot.exists()) {
                continue;
            }

            String raceTeamId = raceSnapshot
                    .child("TeamId")
                    .getValue(String.class);

            if (!teamId.equals(raceTeamId)) {
                continue;
            }

            String result = raceSnapshot
                    .child("Result")
                    .getValue(String.class);

            Integer position = parseRacePosition(result);

            if (position != null) {
                hasTeamResult = true;
                points += racePointsProvider.getPoints(position);

                if (position == 1) {
                    hasWin = true;
                }
            }
        }

        if (!hasTeamResult) {
            return null;
        }

        return new teamRaceStats(
                raceName,
                points,
                hasWin
        );
    }

    public interface singleTeamStatsCallback {
        void onLoaded(
                List<teamRaceStats> team,
                Map<String, Integer> allTeamPoints
        );

        void onError();
    }

    public void loadSingleTeamStats(
            Context context,
            String season,
            String teamId,
            singleTeamStatsCallback callback
    ) {
        if (season == null || teamId == null) {
            callback.onError();
            return;
        }

        rootRef.child("results")
                .child("season")
                .child(season)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot seasonSnapshot) {
                        List<teamRaceStats> team = new ArrayList<>();
                        Map<String, Integer> allTeamPoints = new HashMap<>();

                        for (DataSnapshot raceSnapshot :
                                seasonSnapshot.getChildren()) {
                            String raceName = raceSnapshot.getKey();

                            teamRaceStats stats = calculateTeamRaceStats(
                                    raceSnapshot,
                                    raceName,
                                    teamId
                            );

                            if (stats != null) {
                                team.add(stats);
                            }

                            Map<String, Integer> raceTeamPoints =
                                    calculateAllTeamRacePoints(
                                            raceSnapshot,
                                            raceName
                                    );

                            for (Map.Entry<String, Integer> entry :
                                    raceTeamPoints.entrySet()) {
                                int current = allTeamPoints
                                        .getOrDefault(entry.getKey(), 0);

                                allTeamPoints.put(
                                        entry.getKey(),
                                        current + entry.getValue()
                                );
                            }
                        }

                        if (team.isEmpty()) {
                            callback.onError();
                            return;
                        }

                        callback.onLoaded(team, allTeamPoints);
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        callback.onError();
                    }
                });
    }

    private Map<String, Integer> calculateAllTeamRacePoints(
            DataSnapshot raceSnapshot,
            String raceName
    ) {
        Map<String, Integer> points = new HashMap<>();

        for (DataSnapshot driverSnapshot : raceSnapshot.getChildren()) {
            String teamId = driverSnapshot
                    .child("TeamId")
                    .getValue(String.class);

            String result = driverSnapshot
                    .child("Result")
                    .getValue(String.class);

            if (teamId == null || result == null) {
                continue;
            }

            Integer position = parseRacePosition(result);

            if (position == null) {
                continue;
            }

            int racePoints = (int) racePointsProvider.getPoints(position);

            points.put(
                    teamId,
                    points.getOrDefault(teamId, 0) + racePoints
            );
        }

        return points;
    }

    public interface circuitInfoCallback {
        void onLoaded(circuitInfoData circuit);

        void onError();
    }

    public void loadCircuitInfo(
            Context context,
            String season,
            Integer raceRound,
            circuitInfoCallback callback
    ) {
        loadWeekendRace(
                context,
                season,
                raceRound,
                new weekendCallback() {
                    @Override
                    public void onLoaded(weekendRaceData race) {
                        rootRef.child("circuits")
                                .child(race.getCircuitId())
                                .addListenerForSingleValueEvent(
                                        new ValueEventListener() {
                                            @Override
                                            public void onDataChange(
                                                    DataSnapshot snapshot
                                            ) {
                                                circuitInfoData circuit =
                                                        createCircuitInfo(
                                                                snapshot,
                                                                race
                                                        );

                                                if (circuit == null) {
                                                    callback.onError();
                                                    return;
                                                }

                                                callback.onLoaded(circuit);
                                            }

                                            @Override
                                            public void onCancelled(
                                                    DatabaseError error
                                            ) {
                                                callback.onError();
                                            }
                                        }
                                );
                    }

                    @Override
                    public void onError() {
                        callback.onError();
                    }
                });
    }

    private circuitInfoData createCircuitInfo(
            DataSnapshot snapshot,
            weekendRaceData race
    ) {
        String circuitName = snapshot.child("circuitName")
                .getValue(String.class);

        String country = snapshot.child("country").getValue(String.class);
        String location = snapshot.child("location").getValue(String.class);

        Long lapsCount = snapshot.child("lapsCount").getValue(Long.class);
        Long turnsCount = snapshot.child("turnsCount").getValue(Long.class);

        Double length = snapshot.child("length").getValue(Double.class);
        Double raceDistance = snapshot.child("raceDistance")
                .getValue(Double.class);

        String lapRecordDriver = snapshot.child("lapRecordDriver")
                .getValue(String.class);

        Double lapRecordTime = snapshot.child("lapRecordTime")
                .getValue(Double.class);

        Long lapRecordYear = snapshot.child("lapRecordYear")
                .getValue(Long.class);

        if (circuitName == null) {
            return null;
        }

        return new circuitInfoData(
                race.getRaceName(),
                circuitName,
                country == null ? "" : country,
                location == null ? "" : location,
                lapsCount == null ? 0 : lapsCount.intValue(),
                turnsCount == null ? 0 : turnsCount.intValue(),
                length == null ? 0 : length,
                raceDistance == null ? 0 : raceDistance,
                lapRecordDriver == null ? "" : lapRecordDriver,
                lapRecordTime == null ? 0 : lapRecordTime,
                lapRecordYear == null ? 0 : lapRecordYear.intValue()
        );
    }
}