package com.example.f1app;

public class userPredictionStats {

    private Long totalPoints;
    private Long predictionsCount;
    private Long correctPredictions;
    private Long incorrectPredictions;
    private Long pendingPredictions;
    private Long rank;

    public userPredictionStats() {
    }

    public Long getTotalPoints() {
        return totalPoints;
    }

    public Long getPredictionsCount() {
        return predictionsCount;
    }

    public Long getCorrectPredictions() {
        return correctPredictions;
    }

    public Long getIncorrectPredictions() {
        return incorrectPredictions;
    }

    public Long getPendingPredictions() {
        return pendingPredictions;
    }

    public Long getRank() {
        return rank;
    }
}