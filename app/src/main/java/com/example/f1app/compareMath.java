package com.example.f1app;

final class compareMath {
    enum Mode { HIGHER, LOWER, NEUTRAL }

    static final int NONE = 0;
    static final int FIRST = 1;
    static final int SECOND = 2;

    private compareMath() {
    }

    static double rate(double part, double whole) {
        if (Double.isNaN(part) || Double.isNaN(whole) || whole <= 0) {
            return Double.NaN;
        }
        return part / whole * 100.0;
    }

    static int leader(double a, double b, Mode mode) {
        if (mode == Mode.NEUTRAL || Double.isNaN(a) || Double.isNaN(b) || a == b) {
            return NONE;
        }
        boolean firstBetter = mode == Mode.HIGHER ? a > b : a < b;
        return firstBetter ? FIRST : SECOND;
    }

    /** Bar lengths 0..1; for "lower is better" the inverse value is compared. */
    static double[] shares(double a, double b, Mode mode) {
        double va = value(a, mode);
        double vb = value(b, mode);
        double max = Math.max(va, vb);
        if (max <= 0) {
            return new double[]{0, 0};
        }
        return new double[]{va / max, vb / max};
    }

    private static double value(double v, Mode mode) {
        if (Double.isNaN(v) || v <= 0) {
            return 0;
        }
        return mode == Mode.LOWER ? 1.0 / v : v;
    }
}