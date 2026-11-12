package com.example.f1app;

public class driverResultsData {
    String raceName, driverResult, driverName;
    Integer season;
    /** Round number from the schedule, 0 when unknown. */
    int round;

    public driverResultsData(String raceName, String driverResult, String driverName, Integer season) {
        this(raceName, driverResult, driverName, season, 0);
    }

    public driverResultsData(String raceName, String driverResult, String driverName,
                             Integer season, int round) {
        this.raceName = raceName;
        this.driverResult = driverResult;
        this.driverName = driverName;
        this.season = season;
        this.round = round;
    }

    public String getRaceName() {
        return raceName;
    }

    public void setRaceName(String raceName) {
        this.raceName = raceName;
    }

    public String getDriverResult() {
        return driverResult;
    }

    public void setDriverResult(String driverResult) {
        this.driverResult = driverResult;
    }

    public String getDriverName() {
        return driverName;
    }

    public void setDriverName(String driverName) {
        this.driverName = driverName;
    }

    public Integer getSeason() {
        return season;
    }

    public void setSeason(Integer season) {
        this.season = season;
    }

    public int getRound() {
        return round;
    }

    public void setRound(int round) {
        this.round = round;
    }
}
