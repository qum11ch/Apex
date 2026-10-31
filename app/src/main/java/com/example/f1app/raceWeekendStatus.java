package com.example.f1app;

import com.google.firebase.database.DataSnapshot;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public final class raceWeekendStatus {

    public enum State {
        FUTURE, ONGOING, AWAITING_RESULTS, CONCLUDED, CANCELED
    }

    private static final long RACE_WINDOW_MS = 3L * 60 * 60 * 1000;
    private static final DateTimeFormatter DATE =
            DateTimeFormatter.ofPattern("yyyy-M-d");

    private raceWeekendStatus() {}

    public static State resolve(DataSnapshot ds, long nowMs) {
        Long weekendStart = toMillis(
                ds.child("FirstPractice/firstPracticeDate").getValue(String.class),
                ds.child("FirstPractice/firstPracticeTime").getValue(String.class)
        );
        Long raceStart = toMillis(
                ds.child("raceDate").getValue(String.class),
                ds.child("raceTime").getValue(String.class)
        );

        boolean started = weekendStart != null && nowMs >= weekendStart;

        if (isCanceled(ds)) {
            return started ? State.CANCELED : State.FUTURE;
        }

        if (hasRaceResults(ds)) {
            return State.CONCLUDED;
        }

        if (weekendStart == null || raceStart == null || !started) {
            return State.FUTURE;
        }

        return nowMs < raceStart + RACE_WINDOW_MS
                ? State.ONGOING
                : State.AWAITING_RESULTS;
    }

    public static boolean isCanceled(DataSnapshot ds) {
        return ds.child("Canceled").getValue() != null;
    }

    public static boolean hasRaceResults(DataSnapshot ds) {
        return isValidCode(
                ds.child("RaceResults/raceWinnerCode").getValue(String.class)
        );
    }

    public static boolean isValidCode(String code) {
        if (code == null) return false;
        String c = code.trim();
        return !c.isEmpty() && !c.equals("N/A") && !c.equals("NA");
    }

    private static Long toMillis(String date, String time) {
        if (date == null || time == null) return null;
        try {
            String t = time.trim();
            if (t.endsWith("Z")) t = t.substring(0, t.length() - 1);
            return LocalDate.parse(date.trim(), DATE)
                    .atTime(LocalTime.parse(t))
                    .toInstant(ZoneOffset.UTC)
                    .toEpochMilli();
        } catch (Exception e) {
            return null;
        }
    }

    public static final class SessionInfo {
        public final String key;
        public final long start;
        public final long end;

        SessionInfo(String key, long start, long end) {
            this.key = key;
            this.start = start;
            this.end = end;
        }
    }

    private static final long HOUR_MS = 60L * 60 * 1000;
    private static final long SPRINT_QUALI_MS = 44L * 60 * 1000;

    public static List<SessionInfo> sessions(DataSnapshot ds) {
        List<SessionInfo> list = new ArrayList<>();

        addSession(list, "first_practice_event",
                ds.child("FirstPractice/firstPracticeDate").getValue(String.class),
                ds.child("FirstPractice/firstPracticeTime").getValue(String.class),
                HOUR_MS);

        String sprintDate = ds.child("Sprint/sprintRaceDate").getValue(String.class);
        boolean isSprintWeekend = sprintDate != null && !sprintDate.equals("N/A");

        if (isSprintWeekend) {
            addSession(list, "sprint_quali_event",
                    ds.child("SprintQualifying/sprintQualiDate").getValue(String.class),
                    ds.child("SprintQualifying/sprintQualiTime").getValue(String.class),
                    SPRINT_QUALI_MS);
            addSession(list, "sprint_event",
                    sprintDate,
                    ds.child("Sprint/sprintRaceTime").getValue(String.class),
                    HOUR_MS);
        } else {
            addSession(list, "second_practice_event",
                    ds.child("SecondPractice/secondPracticeDate").getValue(String.class),
                    ds.child("SecondPractice/secondPracticeTime").getValue(String.class),
                    HOUR_MS);
            addSession(list, "third_practice_event",
                    ds.child("ThirdPractice/thirdPracticeDate").getValue(String.class),
                    ds.child("ThirdPractice/thirdPracticeTime").getValue(String.class),
                    HOUR_MS);
        }

        addSession(list, "quali_event",
                ds.child("Qualifying/raceQualiDate").getValue(String.class),
                ds.child("Qualifying/raceQualiTime").getValue(String.class),
                HOUR_MS);

        addSession(list, "race_event",
                ds.child("raceDate").getValue(String.class),
                ds.child("raceTime").getValue(String.class),
                2 * HOUR_MS);

        list.sort((a, b) -> Long.compare(a.start, b.start));
        return list;
    }

    private static void addSession(List<SessionInfo> list, String key,
                                   String date, String time, long durationMs) {
        Long start = toMillis(date, time);
        if (start != null) {
            list.add(new SessionInfo(key, start, start + durationMs));
        }
    }

    public static SessionInfo liveSession(List<SessionInfo> sessions, long now) {
        for (SessionInfo s : sessions) {
            if (now >= s.start && now < s.end) return s;
        }
        return null;
    }

    public static SessionInfo nextSession(List<SessionInfo> sessions, long now) {
        for (SessionInfo s : sessions) {
            if (s.start > now) return s;
        }
        return null;
    }
}