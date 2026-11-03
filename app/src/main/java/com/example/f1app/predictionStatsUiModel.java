package com.example.f1app;

public class predictionStatsUiModel {

    private final String title;
    private final String body;
    private final boolean canOpenWeekend;

    public predictionStatsUiModel(
            String title,
            String body,
            boolean canOpenWeekend
    ) {
        this.title = title;
        this.body = body;
        this.canOpenWeekend = canOpenWeekend;
    }

    public String getTitle() {
        return title;
    }

    public String getBody() {
        return body;
    }

    public boolean canOpenWeekend() {
        return canOpenWeekend;
    }
}