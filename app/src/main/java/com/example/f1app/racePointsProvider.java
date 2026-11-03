package com.example.f1app;

import java.util.HashMap;
import java.util.Map;

public class racePointsProvider {

    private static final Map<Integer, Double> POINTS = new HashMap<>();

    static {
        POINTS.put(1, 25.0);
        POINTS.put(2, 18.0);
        POINTS.put(3, 15.0);
        POINTS.put(4, 12.0);
        POINTS.put(5, 10.0);
        POINTS.put(6, 8.0);
        POINTS.put(7, 6.0);
        POINTS.put(8, 4.0);
        POINTS.put(9, 2.0);
        POINTS.put(10, 1.0);
    }

    private racePointsProvider() {
    }

    public static double getPoints(int position) {
        Double points = POINTS.get(position);

        return points == null ? 0.0 : points;
    }
}