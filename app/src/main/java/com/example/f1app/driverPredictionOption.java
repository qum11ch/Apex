package com.example.f1app;

import java.util.Map;

public class driverPredictionOption {

    private String driverName;
    private String driversCode;
    private String driversTeam;
    private String status;
    private Map<String, Boolean> team;

    public driverPredictionOption() {
    }

    public String getDriverName() {
        return driverName;
    }

    public void setDriverName(String driverName) {
        this.driverName = driverName;
    }

    public String getDriversCode() {
        return driversCode;
    }

    public String getDriversTeam() {
        return driversTeam;
    }

    public String getStatus() {
        return status;
    }

    public Map<String, Boolean> getTeam() {
        return team;
    }

    public boolean isAvailableForPrediction() {
        return "active".equalsIgnoreCase(status)
                && hasActiveTeam()
                && driversCode != null
                && !driversCode.trim().isEmpty();
    }

    private boolean hasActiveTeam() {
        if (team == null || team.isEmpty()) {
            return false;
        }

        for (Boolean value : team.values()) {
            if (Boolean.TRUE.equals(value)) {
                return true;
            }
        }

        return false;
    }
}