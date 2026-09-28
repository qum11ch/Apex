package com.example.f1app;

public class predictionListItem {
    private String id;
    private String title;
    private String description;
    private String category;
    private String type;
    private String season;
    private String raceId;
    private String raceName;
    private String status;
    private long deadline;
    private int points;

    public predictionListItem() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id){
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getCategory() {
        return category;
    }

    public String getType() {
        return type;
    }

    public String getSeason() {
        return season;
    }

    public String getRaceId() {
        return raceId;
    }

    public String getRaceName() {
        return raceName;
    }

    public String getStatus() {
        return status;
    }

    public long getDeadline() {
        return deadline;
    }

    public int getPoints() {
        return points;
    }
}