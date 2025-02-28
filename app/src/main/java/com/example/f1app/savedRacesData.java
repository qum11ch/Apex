package com.example.f1app;

public class savedRacesData {
    String raceName, raceSeason, saveDate;

    public savedRacesData(String raceName, String raceSeason, String saveDate) {
        this.raceName = raceName;
        this.raceSeason = raceSeason;
        this.saveDate = saveDate;
    }

    public String getRaceName() {
        return raceName;
    }

    public void setRaceName(String raceName) {
        this.raceName = raceName;
    }

    public String getRaceSeason() {
        return raceSeason;
    }

    public void setRaceSeason(String raceSeason) {
        this.raceSeason = raceSeason;
    }

    public String getSaveDate() {
        return saveDate;
    }

    public void setSaveDate(String saveDate) {
        this.saveDate = saveDate;
    }
}

