package com.example.f1app;

public class driversList {
    private String driverName;
    private String driverFamilyName;
    private String driverTeam;
    private String constructorId;
    private String driverPoints;
    private String driverPlacement;
    private String driverCode;
    private boolean startSeason;
    private String season;

    public driversList(String driverName, String driverFamilyName, String driverTeam, String constructorId, String driverPoints, String driverPlacement, String driverCode, boolean startSeason, String season) {
        this.driverName = driverName;
        this.driverFamilyName = driverFamilyName;
        this.driverTeam = driverTeam;
        this.constructorId = constructorId;
        this.driverPoints = driverPoints;
        this.driverPlacement = driverPlacement;
        this.driverCode = driverCode;
        this.startSeason = startSeason;
        this.season = season;
    }

    public String getDriverName() {
        return driverName;
    }

    public void setDriverName(String driverName) {
        this.driverName = driverName;
    }

    public String getDriverFamilyName() {
        return driverFamilyName;
    }

    public void setDriverFamilyName(String driverFamilyName) {
        this.driverFamilyName = driverFamilyName;
    }

    public String getDriverTeam() {
        return driverTeam;
    }

    public void setDriverTeam(String driverTeam) {
        this.driverTeam = driverTeam;
    }

    public String getConstructorId() {
        return constructorId;
    }

    public void setConstructorId(String constructorId) {
        this.constructorId = constructorId;
    }

    public String getDriverPoints() {
        return driverPoints;
    }

    public void setDriverPoints(String driverPoints) {
        this.driverPoints = driverPoints;
    }

    public String getDriverPlacement() {
        return driverPlacement;
    }

    public void setDriverPlacement(String driverPlacement) {
        this.driverPlacement = driverPlacement;
    }

    public String getDriverCode() {
        return driverCode;
    }

    public void setDriverCode(String driverCode) {
        this.driverCode = driverCode;
    }

    public boolean isStartSeason() {
        return startSeason;
    }

    public void setStartSeason(boolean startSeason) {
        this.startSeason = startSeason;
    }

    public String getSeason() {
        return season;
    }

    public void setSeason(String season) {
        this.season = season;
    }
}
