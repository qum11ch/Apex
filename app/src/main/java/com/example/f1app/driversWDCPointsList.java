package com.example.f1app;

public class driversWDCPointsList {
    private String driverName;
    private String driverFamilyName;
    private String driverTeam;
    private String constructorId;
    private String season;
    private int currentPoints;
    private int maxPoints;
    private boolean canWin;
    private String placement;

    public driversWDCPointsList(String driverName, String driverFamilyName, String driverTeam, String constructorId, String season, int currentPoints, int maxPoints, boolean canWin, String placement) {
        this.driverName = driverName;
        this.driverFamilyName = driverFamilyName;
        this.driverTeam = driverTeam;
        this.constructorId = constructorId;
        this.season = season;
        this.currentPoints = currentPoints;
        this.maxPoints = maxPoints;
        this.canWin = canWin;
        this.placement = placement;
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

    public String getSeason() {
        return season;
    }

    public void setSeason(String season) {
        this.season = season;
    }

    public int getCurrentPoints() {
        return currentPoints;
    }

    public int getMaxPoints() {
        return maxPoints;
    }

    public boolean isCanWin() {
        return canWin;
    }

    public String getPlacement() {
        return placement;
    }
}
