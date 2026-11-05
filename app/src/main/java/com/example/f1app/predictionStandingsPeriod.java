package com.example.f1app;

public final class predictionStandingsPeriod {
    private predictionStandingsPeriod() {
    }

    public static int resolve(Integer afterRound, int latestRound) {
        if (latestRound < 0 || (afterRound != null && afterRound < 0)) {
            throw new IllegalArgumentException("Invalid round");
        }

        return afterRound == null ? latestRound : Math.min(afterRound, latestRound);
    }
}