package com.example.f1app;

public class userData {
    String userId, userEmail, choiceDriver, choiceTeam;

    public userData(String userId, String userEmail, String choiceDriver, String choiceTeam) {
        this.userId = userId;
        this.userEmail = userEmail;
        this.choiceDriver = choiceDriver;
        this.choiceTeam = choiceTeam;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }

    public String getChoiceDriver() {
        return choiceDriver;
    }

    public void setChoiceDriver(String choiceDriver) {
        this.choiceDriver = choiceDriver;
    }

    public String getChoiceTeam() {
        return choiceTeam;
    }

    public void setChoiceTeam(String choiceTeam) {
        this.choiceTeam = choiceTeam;
    }
}
