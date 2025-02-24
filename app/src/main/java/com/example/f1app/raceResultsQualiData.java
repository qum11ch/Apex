package com.example.f1app;

public class raceResultsQualiData {
    String driverPosition, driverTeam, driverCode,
            Q1time, Q2time, Q3time, season;

    public raceResultsQualiData(String driverPosition, String driverTeam, String driverCode, String q1time, String q2time, String q3time, String season) {
        this.driverPosition = driverPosition;
        this.driverTeam = driverTeam;
        this.driverCode = driverCode;
        Q1time = q1time;
        Q2time = q2time;
        Q3time = q3time;
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

    public String getQ1time() {
        return Q1time;
    }

    public void setQ1time(String q1time) {
        Q1time = q1time;
    }

    public String getQ2time() {
        return Q2time;
    }

    public void setQ2time(String q2time) {
        Q2time = q2time;
    }

    public String getQ3time() {
        return Q3time;
    }

    public void setQ3time(String q3time) {
        Q3time = q3time;
    }

    public String getSeason() {
        return season;
    }

    public void setSeason(String season) {
        this.season = season;
    }
}
