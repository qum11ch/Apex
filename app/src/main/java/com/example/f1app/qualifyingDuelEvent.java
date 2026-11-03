package com.example.f1app;

public class qualifyingDuelEvent {

    private final String raceName;
    private final Integer positionA;
    private final Integer positionB;

    public qualifyingDuelEvent(
            String raceName,
            Integer positionA,
            Integer positionB
    ) {
        this.raceName = raceName;
        this.positionA = positionA;
        this.positionB = positionB;
    }

    public String getRaceName() {
        return raceName;
    }

    public Integer getPositionA() {
        return positionA;
    }

    public Integer getPositionB() {
        return positionB;
    }
}