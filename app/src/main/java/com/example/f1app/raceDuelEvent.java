package com.example.f1app;

public class raceDuelEvent {

    private final String raceName;
    private final String resultA;
    private final String resultB;
    private final Integer positionA;
    private final Integer positionB;
    private final String statusA;
    private final String statusB;
    private final Double pointsA;
    private final Double pointsB;

    public raceDuelEvent(
            String raceName,
            String resultA,
            String resultB,
            Integer positionA,
            Integer positionB,
            String statusA,
            String statusB,
            Double pointsA,
            Double pointsB
    ) {
        this.raceName = raceName;
        this.resultA = resultA;
        this.resultB = resultB;
        this.positionA = positionA;
        this.positionB = positionB;
        this.statusA = statusA;
        this.statusB = statusB;
        this.pointsA = pointsA;
        this.pointsB = pointsB;
    }

    public String getRaceName() {
        return raceName;
    }

    public String getResultA() {
        return resultA;
    }

    public String getResultB() {
        return resultB;
    }

    public Integer getPositionA() {
        return positionA;
    }

    public Integer getPositionB() {
        return positionB;
    }

    public String getStatusA() {
        return statusA;
    }

    public String getStatusB() {
        return statusB;
    }

    public Double getPointsA() {
        return pointsA;
    }

    public Double getPointsB() {
        return pointsB;
    }
}