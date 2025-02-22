package com.example.f1app;

public class concludedRacesData {
    String dateStart, dateEnd, raceName, roundNumber, circuitName, country, locality,
            winnerDriverCode, secondPlaceCode, thirdPlaceCode;

    public concludedRacesData(String dateStart, String dateEnd, String raceName, String roundNumber,
                              String circuitName, String country, String locality, String winnerDriverCode,
                              String secondPlaceCode, String thirdPlaceCode) {
        this.dateStart = dateStart;
        this.dateEnd = dateEnd;
        this.raceName = raceName;
        this.roundNumber = roundNumber;
        this.circuitName = circuitName;
        this.country = country;
        this.locality = locality;
        this.winnerDriverCode = winnerDriverCode;
        this.secondPlaceCode = secondPlaceCode;
        this.thirdPlaceCode = thirdPlaceCode;
    }

    public String getDateStart() {
        return dateStart;
    }

    public void setDateStart(String dateStart) {
        this.dateStart = dateStart;
    }

    public String getDateEnd() {
        return dateEnd;
    }

    public void setDateEnd(String dateEnd) {
        this.dateEnd = dateEnd;
    }

    public String getRaceName() {
        return raceName;
    }

    public void setRaceName(String raceName) {
        this.raceName = raceName;
    }

    public String getRoundNumber() {
        return roundNumber;
    }

    public void setRoundNumber(String roundNumber) {
        this.roundNumber = roundNumber;
    }

    public String getCircuitName() {
        return circuitName;
    }

    public void setCircuitName(String circuitName) {
        this.circuitName = circuitName;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getLocality() {
        return locality;
    }

    public void setLocality(String locality) {
        this.locality = locality;
    }

    public String getWinnerDriverCode() {
        return winnerDriverCode;
    }

    public void setWinnerDriverCode(String winnerDriverCode) {
        this.winnerDriverCode = winnerDriverCode;
    }

    public String getSecondPlaceCode() {
        return secondPlaceCode;
    }

    public void setSecondPlaceCode(String secondPlaceCode) {
        this.secondPlaceCode = secondPlaceCode;
    }

    public String getThirdPlaceCode() {
        return thirdPlaceCode;
    }

    public void setThirdPlaceCode(String thirdPlaceCode) {
        this.thirdPlaceCode = thirdPlaceCode;
    }
}
