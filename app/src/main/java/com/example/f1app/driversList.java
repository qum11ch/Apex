package com.example.f1app;

public class driversList {
    private String driverName;
    private String driverFamilyName;
    private String driverTeam;
    private String driverPoints;
    private String driverPlacement;
    private String driverCode;

    public String getConstructorId() {
        return constructorId;
    }

    public void setConstructorId(String constructorId) {
        this.constructorId = constructorId;
    }

    @Override
    public String toString() {
        return "driversList{" +
                "driverName='" + driverName + '\'' +
                ", driverTeam='" + driverTeam + '\'' +
                ", driverPoints='" + driverPoints + '\'' +
                ", driverPlacement='" + driverPlacement + '\'' +
                ", driverCode='" + driverCode + '\'' +
                ", constructorId='" + constructorId + '\'' +
                '}';
    }

    private String constructorId;

    public driversList(String driverName, String driverFamilyName, String driverTeam, String constructorId, String driverPoints, String driverPlacement, String driverCode) {
        this.driverFamilyName = driverFamilyName;
        this.driverName = driverName;
        this.driverTeam = driverTeam;
        this.driverPoints = driverPoints;
        this.driverPlacement = driverPlacement;
        this.driverCode = driverCode;
        this.constructorId = constructorId;
    }

    public String getDriverFamilyName() {
        return driverFamilyName;
    }

    public void setDriverFamilyName(String driverFamilyName) {
        this.driverFamilyName = driverFamilyName;
    }

    public String getDriverName() {
        return driverName;
    }

    public void setDriverName(String driverName) {
        this.driverName = driverName;
    }

    public String getDriverTeam() {
        return driverTeam;
    }

    public void setDriverTeam(String driverTeam) {
        this.driverTeam = driverTeam;
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
