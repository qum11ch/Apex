package com.example.f1app;

public class raceResultsRaceData {
    String driverPosition, driverTeam, driverCode,
            driverTime, driverPoints, season;

    public raceResultsRaceData(String driverPosition, String driverTeam, String driverCode, String driverTime, String driverPoints, String season) {
        this.driverPosition = driverPosition;
        this.driverTeam = driverTeam;
        this.driverCode = driverCode;
        this.driverTime = driverTime;
        this.driverPoints = driverPoints;
        this.season = season;
    }

    public String getDriverPosition() {
        return driverPosition;
    }

    public void setDriverPosition(String driverPosition) {
        this.driverPosition = driverPosition;
    }

    public String getDriverTeam() {
        return driverTeam;
    }

    public void setDriverTeam(String driverTeam) {
        this.driverTeam = driverTeam;
    }

    public String getDriverCode() {
        return driverCode;
    }

    public void setDriverCode(String driverCode) {
        this.driverCode = driverCode;
    }

    public String getDriverTime() {
        return driverTime;
    }

    public void setDriverTime(String driverTime) {
        this.driverTime = driverTime;
    }

    public String getDriverPoints() {
        return driverPoints;
    }

    public void setDriverPoints(String driverPoints) {
        this.driverPoints = driverPoints;
    }

    public String getSeason() {
        return season;
    }

    public void setSeason(String season) {
        this.season = season;
    }
}
