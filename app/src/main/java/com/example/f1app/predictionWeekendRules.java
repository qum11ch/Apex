package com.example.f1app;

public final class predictionWeekendRules {
    private predictionWeekendRules() {}
    public static final class Decision {
        public final boolean included, hasSprint, calendarMismatch;
        Decision(boolean included, boolean hasSprint, boolean mismatch) {
            this.included=included; this.hasSprint=hasSprint; this.calendarMismatch=mismatch;
        }
    }
    public static Decision decide(boolean raceCompleted, Boolean calendarSprint, boolean sprintCompleted) {
        if (!raceCompleted) return new Decision(false, sprintCompleted, false);
        if (sprintCompleted) return new Decision(true, true, !Boolean.TRUE.equals(calendarSprint));
        if (Boolean.FALSE.equals(calendarSprint)) return new Decision(true, false, false);
        return new Decision(false, Boolean.TRUE.equals(calendarSprint), false);
    }
    public static double weekend(double race, double sprint, boolean hasSprint) {
        if (!Double.isFinite(race) || !Double.isFinite(sprint)) throw new IllegalArgumentException("Invalid points");
        if (!hasSprint && sprint != 0) throw new IllegalArgumentException("Hidden sprint points");
        return race+sprint;
    }
    public static Double average(double sum, int weekends) {
        if (weekends<0) throw new IllegalArgumentException("Invalid count");
        return weekends==0 ? null : sum/weekends;
    }
}
