package com.example.f1app;

public final class circuitInfoData {

    private final String raceName;
    private final String circuitName;
    private final String country;
    private final String location;
    private final String lapsCount;
    private final String turnsCount;
    private final String length;
    private final String raceDistance;
    private final String lapRecordDriver;
    private final String lapRecordTime;
    private final Long lapRecordYear;

    public circuitInfoData(
            String raceName,
            String circuitName,
            String country,
            String location,
            String lapsCount,
            String turnsCount,
            String length,
            String raceDistance,
            String lapRecordDriver,
            String lapRecordTime,
            Long lapRecordYear
    ) {
        this.raceName = raceName;
        this.circuitName = circuitName;
        this.country = country;
        this.location = location;
        this.lapsCount = lapsCount;
        this.turnsCount = turnsCount;
        this.length = length;
        this.raceDistance = raceDistance;
        this.lapRecordDriver = lapRecordDriver;
        this.lapRecordTime = lapRecordTime;
        this.lapRecordYear = lapRecordYear;
    }

    public String getRaceName() {
        return raceName;
    }

    public String getCircuitName() {
        return circuitName;
    }

    public String getCountry() {
        return country;
    }

    public String getLocation() {
        return location;
    }

    public String getLapsCount() {
        return lapsCount;
    }

    public String getTurnsCount() {
        return turnsCount;
    }

    public String getLength() {
        return length;
    }

    public String getRaceDistance() {
        return raceDistance;
    }

    public String getLapRecordDriver() {
        return lapRecordDriver;
    }

    public String getLapRecordTime() {
        return lapRecordTime;
    }

    public Long getLapRecordYear() {
        return lapRecordYear;
    }
}