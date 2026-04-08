package com.example.f1app;

import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;

public class teamsList {
    private String team, points, position, teamId, season;
    private ArrayList<String> drivers;
    private boolean startSeason;
    private String currentSeason;
    private String teamColor;


    public String getTeamColor() {
        return teamColor;
    }

    public void setTeamColor(String teamColor) {
        this.teamColor = teamColor;
    }

    public StorageReference getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(StorageReference imageUrl) {
        this.imageUrl = imageUrl;
    }

    private StorageReference imageUrl;

    public String getCurrentSeason(){
        return currentSeason;
    }

    public void setCurrentSeason(String currentSeason){
        this.currentSeason = currentSeason;
    }

    public void setSeason(String season){
        this.season = season;
    }

    public String getSeason(){
        return season;
    }

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

    public boolean getStartSeasonInfo(){return startSeason;}
    //public void setStartSeasonInfo(boolean startSeason){this.startSeason = startSeason;}

    public teamsList(String team, String position, String points, String teamId,
                     boolean startSeason) {
        this.team = team;
        this.position = position;
        this.points = points;
        this.teamId = teamId;
        this.startSeason = startSeason;
    }

}
