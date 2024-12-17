package com.example.f1app;

import java.util.ArrayList;

public class teamsList {
    private String team, points, position, teamId;
    private ArrayList<String> drivers;

    public ArrayList<String> getDrivers() {
        return drivers;
    }

    public void setDrivers(ArrayList<String> drivers) {
        this.drivers = drivers;
    }

    public String getTeam() {
        return team;
    }

    public String getPosition() {
        return position;
    }

    public String getPoints() {
        return points;
    }

    public String getTeamId(){
        return teamId;
    }

    public teamsList(String team, String position, String points, String teamId) {
        this.team = team;
        this.position = position;
        this.points = points;
        this.teamId = teamId;
    }

}
