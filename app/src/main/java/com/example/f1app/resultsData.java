package com.example.f1app;

public class resultsData {
    public String positionText;
    public String driverName;

    public String getConstructorName() {
        return constructorName;
    }

    public void setConstructorName(String constructorName) {
        this.constructorName = constructorName;
    }

    public String getDriverName() {
        return driverName;
    }

    public void setDriverName(String driverName) {
        this.driverName = driverName;
    }

    public String getPositionText() {
        return positionText;
    }

    public void setPositionText(String positionText) {
        this.positionText = positionText;
    }

    public String constructorName;

    public resultsData(String positionText, String driverName, String constructorName) {
        this.positionText = positionText;
        this.driverName = driverName;
        this.constructorName = constructorName;
    }

}
