package com.example.f1app;

public class futureRaceData {
    private String futureRaceName, futureRaceStartDate, futureRaceEndDate,
            futureCircuitName, futureRaceRound, futureRaceCountry, circuitId,
            locality;
    boolean isCanceled;

    private raceWeekendStatus.State weekendState = raceWeekendStatus.State.FUTURE;
    private java.util.List<raceWeekendStatus.SessionInfo> sessions;

    public boolean isCanceled() {
        return isCanceled;
    }

    public void setCanceled(boolean canceled) {
        isCanceled = canceled;
    }

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

    public raceWeekendStatus.State getWeekendState() { return weekendState; }
    public void setWeekendState(raceWeekendStatus.State state) { this.weekendState = state; }

    public java.util.List<raceWeekendStatus.SessionInfo> getSessions() { return sessions; }
    public void setSessions(java.util.List<raceWeekendStatus.SessionInfo> sessions) { this.sessions = sessions; }

    public boolean isActiveWeekend() {
        return weekendState == raceWeekendStatus.State.ONGOING
                || weekendState == raceWeekendStatus.State.AWAITING_RESULTS;
    }
}
