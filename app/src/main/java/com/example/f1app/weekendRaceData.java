package com.example.f1app;

public class weekendRaceData {

    private final String raceName;
    private final String circuitId;
    private final String circuitName;
    private final String raceDate;
    private final String firstPracticeDate;
    private final int round;
    private final String country;
    private final String startDay;
    private final String endDay;
    private final String startMonth;
    private final String endMonth;

    public weekendRaceData(
            String raceName,
            String circuitId,
            String circuitName,
            String raceDate,
            String firstPracticeDate,
            int round,
            String country,
            String startDay,
            String endDay,
            String startMonth,
            String endMonth
    ) {
        this.raceName = raceName;
        this.circuitId = circuitId;
        this.circuitName = circuitName;
        this.raceDate = raceDate;
        this.firstPracticeDate = firstPracticeDate;
        this.round = round;
        this.country = country;
        this.startDay = startDay;
        this.endDay = endDay;
        this.startMonth = startMonth;
        this.endMonth = endMonth;
    }

    public String getRaceName() {
        return raceName;
    }

    public String getCircuitId() {
        return circuitId;
    }

    public String getCircuitName() {
        return circuitName;
    }

    public String getRaceDate() {
        return raceDate;
    }

    public String getFirstPracticeDate() {
        return firstPracticeDate;
    }

    public int getRound() {
        return round;
    }

    public String getCountry() {
        return country;
    }

    public String getStartDay() {
        return startDay;
    }

    public String getEndDay() {
        return endDay;
    }

    public String getStartMonth() {
        return startMonth;
    }

    public String getEndMonth() {
        return endMonth;
    }
}