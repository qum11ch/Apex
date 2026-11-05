package com.example.f1app;

import java.util.List;

public final class predictionRaceDuelSummary {
    public int winsA;
    public int winsB;
    public int retirementsA;
    public int retirementsB;
    public int numericA;
    public int numericB;
    public int events;

    private long positionSumA;
    private long positionSumB;

    private predictionRaceDuelSummary() {
    }

    public Double averageA() {
        return numericA == 0 ? null : (double) positionSumA / numericA;
    }

    public Double averageB() {
        return numericB == 0 ? null : (double) positionSumB / numericB;
    }

    public static predictionRaceDuelSummary calculate(List<raceDuelEvent> data) {
        predictionRaceDuelSummary result = new predictionRaceDuelSummary();

        for (raceDuelEvent event : data) {
            if (event == null) {
                continue;
            }

            result.events++;

            Integer positionA = positive(event.getPositionA());
            Integer positionB = positive(event.getPositionB());

            updateWins(result, positionA, positionB);
            updatePositions(result, positionA, positionB);
            updateRetirements(result, event);
        }

        return result;
    }

    private static void updateWins(predictionRaceDuelSummary result, Integer positionA, Integer positionB) {
        if (positionA != null && positionB != null) {
            if (positionA < positionB) {
                result.winsA++;
            } else if (positionB < positionA) {
                result.winsB++;
            }

            return;
        }

        if (positionA != null) {
            result.winsA++;
        } else if (positionB != null) {
            result.winsB++;
        }
    }

    private static void updatePositions(predictionRaceDuelSummary result, Integer positionA, Integer positionB) {
        if (positionA != null) {
            result.positionSumA += positionA;
            result.numericA++;
        }

        if (positionB != null) {
            result.positionSumB += positionB;
            result.numericB++;
        }
    }

    private static void updateRetirements(predictionRaceDuelSummary result, raceDuelEvent event) {
        if ("R".equals(event.getStatusA())) {
            result.retirementsA++;
        }

        if ("R".equals(event.getStatusB())) {
            result.retirementsB++;
        }
    }

    private static Integer positive(Integer value) {
        return value != null && value > 0 ? value : null;
    }
}