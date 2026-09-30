package com.example.f1app;

public class userPredictionAnswer {

    private String optionId;
    private String optionTitle;

    private Long submittedAt;
    private Long updatedAt;

    private Double points;
    private String result;

    public userPredictionAnswer() {
        // Требуется Firebase Realtime Database
    }

    public String getOptionId() {
        return optionId;
    }

    public void setOptionId(String optionId) {
        this.optionId = optionId;
    }

    public String getOptionTitle() {
        return optionTitle;
    }

    public void setOptionTitle(String optionTitle) {
        this.optionTitle = optionTitle;
    }

    public Long getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(Long submittedAt) {
        this.submittedAt = submittedAt;
    }

    public Long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Long updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Double getPoints() {
        return points;
    }

    public void setPoints(Double points) {
        this.points = points;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public boolean isPending() {
        return "pending".equals(result);
    }

    public boolean isCorrect() {
        return "correct".equals(result);
    }

    public boolean isIncorrect() {
        return "incorrect".equals(result);
    }

    public boolean isTie() {
        return "tie".equals(result);
    }
}