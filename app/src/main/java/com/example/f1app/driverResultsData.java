package com.example.f1app;

public class driverResultsData {
    String raceName, driverResult, driverName;
    Integer season;

    public driverResultsData(String raceName, String driverResult, String driverName, Integer season) {
        this.raceName = raceName;
        this.driverResult = driverResult;
        this.driverName = driverName;
        this.season = season;
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
}
