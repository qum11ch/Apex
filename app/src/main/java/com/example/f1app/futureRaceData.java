package com.example.f1app;

public class futureRaceData {
    private String futureRaceName, futureRaceStartDate, futureRaceEndDate,
            futureCircuitName, futureRaceRound, futureRaceCountry, circuitId,
            locality;

    public futureRaceData(String futureRaceName, String futureRaceStartDate, String futureRaceEndDate,
                          String futureCircuitName, String futureRaceRound, String futureRaceCountry,
                          String futureCircuitId) {
        this.futureRaceName = futureRaceName;
        this.futureRaceStartDate = futureRaceStartDate;
        this.futureRaceEndDate = futureRaceEndDate;
        this.futureCircuitName = futureCircuitName;
        this.futureRaceRound = futureRaceRound;
        this.futureRaceCountry = futureRaceCountry;
        this.circuitId = futureCircuitId;
    }
    public String getLocale(){return  locality;}
    public void setLocality(String locality){ this.locality = locality; }
    public String getFutureRaceName() {
        return futureRaceName;
    }

    public String getFutureRaceStartDate() {
        return futureRaceStartDate;
    }

    public String getFutureRaceEndDate() {
        return futureRaceEndDate;
    }

    public String getFutureCircuitName() {
        return futureCircuitName;
    }

    public String getFutureRaceRound() {
        return futureRaceRound;
    }

    public String getFutureRaceCountry() {
        return futureRaceCountry;
    }

    public String getCircuitId(){ return circuitId;}
}
