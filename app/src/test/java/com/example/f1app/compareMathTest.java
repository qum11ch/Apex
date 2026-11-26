package com.example.f1app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class compareMathTest {
    @Test
    public void lowerIsBetterGivesLeaderToSmaller() {
        assertEquals(compareMath.FIRST, compareMath.leader(1, 3, compareMath.Mode.LOWER));
    }

    @Test
    public void missingValueHasNoLeader() {
        assertEquals(compareMath.NONE, compareMath.leader(5, Double.NaN, compareMath.Mode.HIGHER));
    }

    @Test
    public void rateNeedsPositiveDenominator() {
        assertTrue(Double.isNaN(compareMath.rate(3, 0)));
        assertEquals(50.0, compareMath.rate(5, 10), 1e-9);
    }

    @Test
    public void sharesOfLowerAreInverted() {
        double[] s = compareMath.shares(1, 2, compareMath.Mode.LOWER);
        assertEquals(1.0, s[0], 1e-9);
        assertEquals(0.5, s[1], 1e-9);
    }
}