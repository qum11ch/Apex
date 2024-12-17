package com.example.f1app;

public class scheduleData {
    String eventDate, eventName;

    public scheduleData(String eventDate, String eventName) {
        this.eventDate = eventDate;
        this.eventName = eventName;
    }

    public String getEventDate() {
        return eventDate;
    }

    public void setEventDate(String eventDate) {
        this.eventDate = eventDate;
    }

    public String getEventName() {
        return eventName;
    }

    public void setEventName(String eventName) {
        this.eventName = eventName;
    }
}
