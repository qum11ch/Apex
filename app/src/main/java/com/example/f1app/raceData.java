package com.example.f1app;

public class raceData {
    private String id, raceName, raceDate, circuitName, round, country, src, circuitId, winnerDriver, winnerConstructor, secondDriver, secondConstructor,
            thirdDriver, thirdConstructor;
    public raceData(){

    }

    public raceData(String raceName, String raceDate, String circuitName, String round, String country, String src, String circuitId, String winnerDriver, String winnerConstructor, String secondDriver, String secondConstructor, String thirdDriver, String thirdConstructor) {
        this.raceName = raceName;
        this.raceDate = raceDate;
        this.circuitName = circuitName;
        this.round = round;
        this.country = country;
        this.src = src;
        this.circuitId = circuitId;
        this.winnerDriver = winnerDriver;
        this.winnerConstructor = winnerConstructor;
        this.secondDriver = secondDriver;
        this.secondConstructor = secondConstructor;
        this.thirdDriver = thirdDriver;
        this.thirdConstructor = thirdConstructor;
    }

    public void setRaceName(String raceName) {
        this.raceName = raceName;
    }

    public void setRaceDate(String raceDate) {
        this.raceDate = raceDate;
    }

    public void setCircuitName(String circuitName) {
        this.circuitName = circuitName;
    }

    public void setRound(String round) {
        this.round = round;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public void setSrc(String src) {
        this.src = src;
    }

    public void setCircuitId(String circuitId) {
        this.circuitId = circuitId;
    }

    public void setWinnerDriver(String winnerDriver) {
        this.winnerDriver = winnerDriver;
    }

    public void setWinnerConstructor(String winnerConstructor) {
        this.winnerConstructor = winnerConstructor;
    }

    public void setSecondDriver(String secondDriver) {
        this.secondDriver = secondDriver;
    }

    public void setSecondConstructor(String secondConstructor) {
        this.secondConstructor = secondConstructor;
    }

    public void setThirdDriver(String thirdDriver) {
        this.thirdDriver = thirdDriver;
    }

    public void setThirdConstructor(String thirdConstructor) {
        this.thirdConstructor = thirdConstructor;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getRaceName() {
        return raceName;
    }

    public String getRaceDate() {
        return raceDate;
    }

    public String getCircuitName() {
        return circuitName;
    }

    public String getRound() {
        return round;
    }

    public String getCountry() {
        return country;
    }

    public String getSrc() {
        return src;
    }

    public String getCircuitId() {
        return circuitId;
    }

    public String getWinnerDriver() {
        return winnerDriver;
    }

    public String getWinnerConstructor() {
        return winnerConstructor;
    }

    public String getSecondDriver() {
        return secondDriver;
    }

    public String getSecondConstructor() {
        return secondConstructor;
    }

    public String getThirdDriver() {
        return thirdDriver;
    }

    public String getThirdConstructor() {
        return thirdConstructor;
    }
}
