package com.example.f1app;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class trackResultsTest {
    @Test
    public void groupsByCircuitAndCountsPodiums() {
        List<trackResults.Row> rows = new ArrayList<>();
        rows.add(new trackResults.Row(2023, "Australian Grand Prix", "albert_park",
                Collections.singletonList("5-4")));
        rows.add(new trackResults.Row(2024, "Australian Grand Prix", "albert_park",
                Collections.singletonList("3-1")));
        rows.add(new trackResults.Row(2024, "Monaco Grand Prix", "monaco",
                Arrays.asList("1-R")));

        List<trackResults.Stat> stats = trackResults.build(rows);

        assertEquals("albert_park", stats.get(0).circuitId);
        assertEquals(2, stats.get(0).starts);
        assertEquals(1, stats.get(0).wins);
        assertEquals(1, stats.get(0).podiums);
        assertEquals(1, stats.get(0).best);
        assertEquals(2024, stats.get(0).latestYear);
        assertEquals(0, stats.get(1).best);
    }
}