package com.example.f1app;

public class teamDriversResultsData {
    String raceName, firstDriverResult, firstDriverName,
        secondDriverResult, secondDriverName;
    Integer season;

    public teamDriversResultsData(String raceName, String firstDriverResult, String firstDriverName, String secondDriverResult, String secondDriverName, Integer season) {
        this.raceName = raceName;
        this.firstDriverResult = firstDriverResult;
        this.firstDriverName = firstDriverName;
        this.secondDriverResult = secondDriverResult;
        this.secondDriverName = secondDriverName;
        this.season = season;
    }

    public String getRaceName() {
        return raceName;
    }

    public void setRaceName(String raceName) {
        this.raceName = raceName;
    }

    public String getFirstDriverResult() {
        return firstDriverResult;
    }

    public void setFirstDriverResult(String firstDriverResult) {
        this.firstDriverResult = firstDriverResult;
    }

    public String getFirstDriverName() {
        return firstDriverName;
    }

    public void setFirstDriverName(String firstDriverName) {
        this.firstDriverName = firstDriverName;
    }

    public String getSecondDriverResult() {
        return secondDriverResult;
    }

    public void setSecondDriverResult(String secondDriverResult) {
        this.secondDriverResult = secondDriverResult;
    }

    public String getSecondDriverName() {
        return secondDriverName;
    }

    public void setSecondDriverName(String secondDriverName) {
        this.secondDriverName = secondDriverName;
    }

    public Integer getSeason() {
        return season;
    }

    public void setSeason(Integer season) {
        this.season = season;
    }
}
