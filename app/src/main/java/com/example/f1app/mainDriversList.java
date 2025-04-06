package com.example.f1app;

public class mainDriversList {
    private String driverName;
    private String driverFamilyName;
    private String driverPoints;
    private String driverPlacement;
    private String driverCode;
    private boolean startSeason;

    @Override
    public String toString() {
        return "driversList{" +
                "driverName='" + driverName + '\'' +
                ", driverPoints='" + driverPoints + '\'' +
                ", driverPlacement='" + driverPlacement + '\'' +
                ", driverCode='" + driverCode + '\'' +
                '}';
    }

    private String constructorId;

    public mainDriversList(String driverName, String driverFamilyName, String driverPoints, String driverPlacement, String driverCode,
                           boolean startSeason) {
        this.driverFamilyName = driverFamilyName;
        this.driverName = driverName;
        this.driverPoints = driverPoints;
        this.driverPlacement = driverPlacement;
        this.driverCode = driverCode;
        this.startSeason = startSeason;
    }

    public String getDriverFamilyName() {
        return driverFamilyName;
    }

    public void setDriverFamilyName(String driverFamilyName) {
        this.driverFamilyName = driverFamilyName;
    }

    public boolean getStartSeasonInfo(){return startSeason;}
    public void setStartSeasonInfo(boolean startSeason){this.startSeason = startSeason;}

    public String getDriverName() {
        return driverName;
    }

    public void setDriverName(String driverName) {
        this.driverName = driverName;
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
}
