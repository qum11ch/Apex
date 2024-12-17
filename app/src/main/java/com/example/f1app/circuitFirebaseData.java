package com.example.f1app;

public class circuitFirebaseData {
    String altitude, circuitId, circuitName, country, firstGPyear, lapRecordDriver,
            lapRecordTime, lapRecordYear, lapsCount, length, location, opened, raceDistance,
            turnsCount;

    long id;

    public circuitFirebaseData() {
    }

    public circuitFirebaseData(String altitude, String circuitId, String circuitName, String country,
                               String firstGPyear, long id, String lapRecordDriver, String lapRecordTime,
                               String lapRecordYear, String lapsCount, String length, String location,
                               String opened, String raceDistance, String turnsCount) {
        this.altitude = altitude;
        this.circuitId = circuitId;
        this.circuitName = circuitName;
        this.country = country;
        this.firstGPyear = firstGPyear;
        this.id = id;
        this.lapRecordDriver = lapRecordDriver;
        this.lapRecordTime = lapRecordTime;
        this.lapRecordYear = lapRecordYear;
        this.lapsCount = lapsCount;
        this.length = length;
        this.location = location;
        this.opened = opened;
        this.raceDistance = raceDistance;
        this.turnsCount = turnsCount;
    }

    public String getAltitude() {
        return altitude;
    }

    public String getCircuitId() {
        return circuitId;
    }

    public String getCircuitName() {
        return circuitName;
    }

    public String getCountry() {
        return country;
    }

    public String getFirstGPyear() {
        return firstGPyear;
    }

    public long getId() {
        return id;
    }

    public String getLapRecordDriver() {
        return lapRecordDriver;
    }

    public String getLapRecordTime() {
        return lapRecordTime;
    }

    public String getLapRecordYear() {
        return lapRecordYear;
    }

    public String getLapsCount() {
        return lapsCount;
    }

    public String getLength() {
        return length;
    }

    public String getLocation() {
        return location;
    }

    public String getOpened() {
        return opened;
    }

    public String getRaceDistance() {
        return raceDistance;
    }

    public String getTurnsCount() {
        return turnsCount;
    }
}
