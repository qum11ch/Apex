package com.example.f1app;

public class teamRaceStats {

    private final String raceName;
    private final int points;
    private final boolean hasWin;

    public teamRaceStats(
            String raceName,
            int points,
            boolean hasWin
    ) {
        this.raceName = raceName;
        this.points = points;
        this.hasWin = hasWin;
    }

    public String getRaceName() {
        return raceName;
    }

    public int getPoints() {
        return points;
    }

    public boolean hasWin() {
        return hasWin;
    }
}