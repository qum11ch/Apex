package com.example.f1app;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class compareSeriesTest {
    @Test
    public void accumulatesPointsAndCarriesForwardMissingRounds() {
        Map<Integer, Double> a = new HashMap<>();
        a.put(1, 25.0);
        a.put(3, 18.0);
        Map<Integer, Double> b = new HashMap<>();
        b.put(2, 12.5);

        compareSeries.Data data = compareSeries.compute(a, b, new HashMap<>());

        assertEquals(3, data.a.length);
        assertEquals(43.0, data.a[2], 1e-9);
        assertEquals(12.5, data.b[2], 1e-9);
        assertEquals(0.0, data.b[0], 1e-9);
    }
}