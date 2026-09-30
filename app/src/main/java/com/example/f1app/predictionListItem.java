package com.example.f1app;

public class predictionListItem {

    private String predictionId;
    private String category;
    private String predictionType;
    private String optionType;

    private String season;
    private Integer raceRound;

    private LocalizedText title;
    private LocalizedText description;

    private Double points;
    private Long deadline;
    private String status;

    private String constructorId;

    private String driverAId;
    private String driverATitle;
    private String driverBId;
    private String driverBTitle;

    public predictionListItem() {
    }

    public String getPredictionId() {
        return predictionId;
    }

    public void setPredictionId(String value) {
        predictionId = value;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String value) {
        category = value;
    }

    public String getPredictionType() {
        return predictionType;
    }

    public void setPredictionType(String value) {
        predictionType = value;
    }

    public String getOptionType() {
        return optionType;
    }

    public void setOptionType(String value) {
        optionType = value;
    }

    public String getSeason() {
        return season;
    }

    public void setSeason(String value) {
        season = value;
    }

    public Integer getRaceRound() {
        return raceRound;
    }

    public void setRaceRound(Integer value) {
        raceRound = value;
    }

    public LocalizedText getTitle() {
        return title;
    }

    public void setTitle(LocalizedText value) {
        title = value;
    }

    public LocalizedText getDescription() {
        return description;
    }

    public void setDescription(LocalizedText value) {
        description = value;
    }

    public Double getPoints() {
        return points;
    }

    public void setPoints(Double value) {
        points = value;
    }

    public Long getDeadline() {
        return deadline;
    }

    public void setDeadline(Long value) {
        deadline = value;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String value) {
        status = value;
    }

    public String getConstructorId() {
        return constructorId;
    }

    public void setConstructorId(String value) {
        constructorId = value;
    }

    public String getDriverAId() {
        return driverAId;
    }

    public void setDriverAId(String value) {
        driverAId = value;
    }

    public String getDriverATitle() {
        return driverATitle;
    }

    public void setDriverATitle(String value) {
        driverATitle = value;
    }

    public String getDriverBId() {
        return driverBId;
    }

    public void setDriverBId(String value) {
        driverBId = value;
    }

    public String getDriverBTitle() {
        return driverBTitle;
    }

    public void setDriverBTitle(String value) {
        driverBTitle = value;
    }

    public boolean isWeekend() {
        return "weekend".equals(category);
    }

    public boolean isClosed() {
        return "closed".equals(status);
    }

    public boolean isHeadToHead() {
        return "race_head_to_head".equals(predictionType)
                || "qualifying_head_to_head".equals(predictionType)
                || "team_points_head_to_head".equals(predictionType);
    }

    public boolean isBooleanPrediction() {
        return "boolean".equals(optionType);
    }

    public String getLocalizedTitle() {
        return localized(title, predictionId);
    }

    public String getLocalizedDescription() {
        return localized(description, "");
    }

    private String localized(
            LocalizedText value,
            String fallback
    ) {
        if (value == null) {
            return fallback;
        }

        boolean russian = "ru".equals(
                java.util.Locale.getDefault().getLanguage()
        );

        String result = russian ? value.getRu() : value.getEn();

        if (result == null || result.trim().isEmpty()) {
            result = russian ? value.getEn() : value.getRu();
        }

        return result == null ? fallback : result;
    }
}