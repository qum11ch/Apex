package com.example.f1app;

public class userPredictionStats {

    private Double totalPoints;
    private Long predictionsCount;
    private Long correctPredictions;
    private Long incorrectPredictions;
    private Long pendingPredictions;
    private Long tiePredictions;

    public userPredictionStats() {
    }

    public Double getTotalPoints() {
        return totalPoints;
    }

    public Long getPredictionsCount() {
        return predictionsCount;
    }

    public Long getTiePredictions(){
        return tiePredictions;
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
}