package com.example.f1app;

import org.json.JSONArray;

public class scheduleData {
    String eventDate, eventName;
    CharSequence resultsText;
    JSONArray results; // the whole classification of the session, once it is loaded

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

    public CharSequence getResultsText() { return resultsText; }
    public void setResultsText(CharSequence resultsText) { this.resultsText = resultsText; }

    public JSONArray getResults() { return results; }
    public void setResults(JSONArray results) { this.results = results; }
}