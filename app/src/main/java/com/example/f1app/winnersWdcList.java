package com.example.f1app;

public class winnersWdcList {

    public static final int DRIVER = 0;
    public static final int SEPARATOR = 1;

    private final int type;
    private final driversWDCPointsList driver;

    private winnersWdcList(int type, driversWDCPointsList driver) {
        this.type = type;
        this.driver = driver;
    }

    public static winnersWdcList driver(driversWDCPointsList driver) {
        return new winnersWdcList(DRIVER, driver);
    }

    public static winnersWdcList separator() {
        return new winnersWdcList(SEPARATOR, null);
    }

    public int getType() {
        return type;
    }

    public driversWDCPointsList getDriver() {
        return driver;
    }
}