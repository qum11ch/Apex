package com.example.f1app;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class compareDuelTest {
    private static Map<Integer, compareDuel.Race> map(int[] positions, String[] texts) {
        Map<Integer, compareDuel.Race> result = new HashMap<>();
        for (int i = 0; i < positions.length; i++) {
            result.put(i + 1, new compareDuel.Race("GP" + (i + 1), positions[i], texts[i]));
        }
        return result;
    }

    @Test
    public void countsDuelsAndRetirements() {
        Map<Integer, compareDuel.Race> a = map(new int[]{1, 5, 9}, new String[]{"1", "5", "R"});
        Map<Integer, compareDuel.Race> b = map(new int[]{2, 3, 4}, new String[]{"2", "3", "4"});

        compareDuel.Result r = compareDuel.compute(a, b, new HashMap<>(), new HashMap<>());

        assertEquals(1, r.raceA);
        assertEquals(2, r.raceB);
        assertEquals(1, r.retiredA);
        assertEquals(0, r.retiredB);
        assertEquals(5.0, r.avgRaceA, 1e-9);
    }

    @Test
    public void skipsRoundsWithoutStart() {
        Map<Integer, compareDuel.Race> a = map(new int[]{20}, new String[]{"W"});
        Map<Integer, compareDuel.Race> b = map(new int[]{3}, new String[]{"3"});

        compareDuel.Result r = compareDuel.compute(a, b, new HashMap<>(), new HashMap<>());

        assertEquals(0, r.rounds.size());
    }

    @Test
    public void teamBestCarDecidesAndPointsAreSummed() {
        Map<Integer, compareDuel.Entry> a = new HashMap<>();
        a.put(1, new compareDuel.Entry("GP1", 2, 18 + 10, 0));
        Map<Integer, compareDuel.Entry> b = new HashMap<>();
        b.put(1, new compareDuel.Entry("GP1", 1, 25, 1));

        compareDuel.Result r = compareDuel.computeTeams(a, b, new HashMap<>(), new HashMap<>());

        assertEquals(0, r.raceA);
        assertEquals(1, r.raceB);
        assertEquals(28.0, r.pointsA, 1e-9);
        assertEquals(1, r.retiredB);
    }
}