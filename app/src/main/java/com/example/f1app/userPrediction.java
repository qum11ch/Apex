package com.example.f1app;

public class userPrediction {
    private String season;
    private Integer raceRound;
    private LocalizedText title;
    private LocalizedText description;
    private Double maxPoints;
    private Long deadline;
    private String status;
    private Long createdAt;
    private Long predictionUpdatedAt;
    private String constructorId;
    private String driverAId;
    private String driverATitle;
    private String driverBId;
    private String driverBTitle;
    private String resultType;
    private String correctOptionId;
    private String answerOptionId;
    private String answerOptionTitle;
    private Long submittedAt;
    private Long answerUpdatedAt;
    private Double answerPoints;
    private String answerResult;
    private String predictionId;
    private String category;
    private String predictionType;
    private String optionType;


    public userPrediction() {}

    public String getPredictionId() {
        return predictionId;
    }

    public void setPredictionId(String predictionId) {
        this.predictionId = predictionId;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getPredictionType() {
        return predictionType;
    }

    public void setPredictionType(String predictionType) {
        this.predictionType = predictionType;
    }

    public String getOptionType() {
        return optionType;
    }

    public void setOptionType(String optionType) {
        this.optionType = optionType;
    }

    public String getSeason() {
        return season;
    }

    public void setSeason(String season) {
        this.season = season;
    }

    public Integer getRaceRound() {
        return raceRound;
    }

    public void setRaceRound(Integer raceRound) {
        this.raceRound = raceRound;
    }

    public LocalizedText getTitle() {
        return title;
    }

    public void setTitle(LocalizedText title) {
        this.title = title;
    }

    public LocalizedText getDescription() {
        return description;
    }

    public void setDescription(LocalizedText description) {
        this.description = description;
    }

    public Double getMaxPoints() {
        return maxPoints;
    }

    public void setMaxPoints(Double maxPoints) {
        this.maxPoints = maxPoints;
    }

    public Long getDeadline() {
        return deadline;
    }

    public void setDeadline(Long deadline) {
        this.deadline = deadline;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Long createdAt) {
        this.createdAt = createdAt;
    }

    public Long getPredictionUpdatedAt() {
        return predictionUpdatedAt;
    }

    public void setPredictionUpdatedAt(Long predictionUpdatedAt) {
        this.predictionUpdatedAt = predictionUpdatedAt;
    }

    public String getConstructorId() {
        return constructorId;
    }

    public void setConstructorId(String constructorId) {
        this.constructorId = constructorId;
    }

    public String getDriverAId() {
        return driverAId;
    }

    public void setDriverAId(String driverAId) {
        this.driverAId = driverAId;
    }

    public String getDriverATitle() {
        return driverATitle;
    }

    public void setDriverATitle(String driverATitle) {
        this.driverATitle = driverATitle;
    }

    public String getDriverBId() {
        return driverBId;
    }

    public void setDriverBId(String driverBId) {
        this.driverBId = driverBId;
    }

    public String getDriverBTitle() {
        return driverBTitle;
    }

    public void setDriverBTitle(String driverBTitle) {
        this.driverBTitle = driverBTitle;
    }

    public String getResultType() {
        return resultType;
    }

    public void setResultType(String resultType) {
        this.resultType = resultType;
    }

    public String getCorrectOptionId() {
        return correctOptionId;
    }

    public void setCorrectOptionId(String correctOptionId) {
        this.correctOptionId = correctOptionId;
    }

    public String getAnswerOptionId() {
        return answerOptionId;
    }

    public void setAnswerOptionId(String answerOptionId) {
        this.answerOptionId = answerOptionId;
    }

    public String getAnswerOptionTitle() {
        return answerOptionTitle;
    }

    public void setAnswerOptionTitle(String answerOptionTitle) {
        this.answerOptionTitle = answerOptionTitle;
    }

    public Long getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(Long submittedAt) {
        this.submittedAt = submittedAt;
    }

    public Long getAnswerUpdatedAt() {
        return answerUpdatedAt;
    }

    public void setAnswerUpdatedAt(Long answerUpdatedAt) {
        this.answerUpdatedAt = answerUpdatedAt;
    }

    public Double getAnswerPoints() {
        return answerPoints;
    }

    public void setAnswerPoints(Double answerPoints) {
        this.answerPoints = answerPoints;
    }

    public String getAnswerResult() {
        return answerResult;
    }

    public void setAnswerResult(String answerResult) {
        this.answerResult = answerResult;
    }


    public boolean isWeekend(){
        return "weekend".equals(category);
    }

    public boolean isSeason(){
        return "season".equals(category);
    }

    public boolean isHeadToHead(){
        return "race_head_to_head".equals(predictionType)
                ||"qualifying_head_to_head".equals(predictionType)
                ||"team_points_head_to_head".equals(predictionType);
    }

    public boolean isBooleanPrediction(){
        return "boolean".equals(optionType);
    }

    public boolean isOpen(){
        return "open".equals(status);
    }

    public boolean isClosed(){
        return "closed".equals(status);
    }

    public boolean canBeChanged(){
        return isOpen() && "pending".equals(answerResult);
    }

    public String getLocalizedTitle(){
        return localized(title, predictionId);
    }

    public String getLocalizedDescription(){
        return localized(description, "");
    }

    private String localized(LocalizedText text,String fallback){
        if(text == null)
            return fallback;

        boolean ru = "ru".equals(java.util.Locale.getDefault().getLanguage());
        String v = ru?text.getRu():text.getEn();
        if(v == null || v.trim().isEmpty())
            v = ru?text.getEn():text.getRu();
        return v == null?fallback:v;
    }
}