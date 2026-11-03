package com.example.f1app;

public class circuitInfoData {

    private final String raceName;
    private final String circuitName;
    private final String country;
    private final String location;
    private final int lapsCount;
    private final int turnsCount;
    private final double length;
    private final double raceDistance;
    private final String lapRecordDriver;
    private final double lapRecordTime;
    private final int lapRecordYear;

    public circuitInfoData(
            String raceName,
            String circuitName,
            String country,
            String location,
            int lapsCount,
            int turnsCount,
            double length,
            double raceDistance,
            String lapRecordDriver,
            double lapRecordTime,
            int lapRecordYear
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

    public int getLapsCount() {
        return lapsCount;
    }

    public int getTurnsCount() {
        return turnsCount;
    }

    public double getLength() {
        return length;
    }

    public double getRaceDistance() {
        return raceDistance;
    }

    public String getLapRecordDriver() {
        return lapRecordDriver;
    }

    public double getLapRecordTime() {
        return lapRecordTime;
    }

    public int getLapRecordYear() {
        return lapRecordYear;
    }
}