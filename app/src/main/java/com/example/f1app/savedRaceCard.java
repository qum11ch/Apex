package com.example.f1app;

import org.json.JSONException;
import org.json.JSONObject;

import java.time.DateTimeException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/** One saved race together with the schedule and circuit facts needed to draw its card. */
public class savedRaceCard {

    public enum State {
        UPCOMING,        // before the race weekend
        IN_PROGRESS,     // the weekend has started and the race is not over yet
        RESULTS_PENDING, // the race time has passed but the results are not in the database yet
        CONCLUDED,       // results are available
        CANCELED
    }

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-M-d");
    private static final Duration RACE_LENGTH = Duration.ofHours(3);

    // what the user saved
    public final String key;
    public final String raceName;
    public final String raceSeason;
    public final String saveDate;

    // schedule/season/{season}/{race}; scheduleKnown is false when it could not be found
    public boolean scheduleKnown;
    public int round;
    public String raceDate;
    public String raceTime;
    public String firstPracticeDate;
    public String firstPracticeTime;
    public String winnerCode;
    public String secondCode;
    public String thirdCode;
    public String fastestLapDriver;
    public String fastestLapTime;
    public String sprintDate;
    public String sprintWinnerCode;
    public boolean canceled;

    // circuits/{circuitId}
    public String circuitId;
    public String circuitName;
    public String country;
    public String length;
    public String lapsCount;
    public String turnsCount;
    public String lapRecordTime;
    public String lapRecordDriver;

    public savedRaceCard(String key, String raceName, String raceSeason, String saveDate) {
        this.key = key;
        this.raceName = raceName;
        this.raceSeason = raceSeason;
        this.saveDate = saveDate;
    }

    public String getRaceName() {
        return raceName;
    }

    public String getRaceSeason() {
        return raceSeason;
    }

    public String getSaveDate() {
        return saveDate;
    }

    // ---------- values ----------

    /** The database uses "N/A" (and the old code expected "N/C") for values that do not exist yet. */
    public static boolean isMissing(String value) {
        if (value == null) {
            return true;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty()
                || "n/a".equalsIgnoreCase(trimmed)
                || "n/c".equalsIgnoreCase(trimmed);
    }

    public boolean hasResult() {
        return !isMissing(winnerCode);
    }

    public boolean hasPodium() {
        return !isMissing(winnerCode) && !isMissing(secondCode) && !isMissing(thirdCode);
    }

    public boolean isSprintWeekend() {
        return !isMissing(sprintDate);
    }

    public boolean hasRaceTime() {
        return !isMissing(raceTime) && parseDate(raceDate) != null;
    }

    // ---------- time ----------

    public LocalDate raceLocalDate() {
        return parseDate(raceDate);
    }

    /** First day of the weekend, or the race day when practice is unknown. */
    public LocalDate weekendStartDate() {
        LocalDate practice = parseDate(firstPracticeDate);
        return practice != null ? practice : raceLocalDate();
    }

    public Instant raceStart() {
        return parseInstant(raceDate, raceTime);
    }

    public Instant weekendStart() {
        Instant practice = parseInstant(firstPracticeDate, firstPracticeTime);
        return practice != null ? practice : raceStart();
    }

    public State stateAt(Instant now) {
        if (canceled) {
            return State.CANCELED;
        }

        if (hasResult()) {
            return State.CONCLUDED;
        }

        Instant raceStart = raceStart();

        if (raceStart == null) {
            return State.UPCOMING;
        }

        Instant weekendStart = weekendStart();

        if (weekendStart != null && now.isBefore(weekendStart)) {
            return State.UPCOMING;
        }

        return now.isBefore(raceStart.plus(RACE_LENGTH))
                ? State.IN_PROGRESS
                : State.RESULTS_PENDING;
    }

    private static LocalDate parseDate(String value) {
        if (isMissing(value)) {
            return null;
        }

        try {
            return LocalDate.parse(value.trim(), DATE);
        } catch (DateTimeException error) {
            return null;
        }
    }

    private static Instant parseInstant(String date, String time) {
        LocalDate day = parseDate(date);

        if (day == null) {
            return null;
        }

        if (!isMissing(time)) {
            try {
                return Instant.parse(day + "T" + time.trim());
            } catch (DateTimeException ignored) {
                // fall back to the start of the day
            }
        }

        return day.atStartOfDay(ZoneOffset.UTC).toInstant();
    }

    // ---------- comparison and cache ----------

    /** Used by DiffUtil: two cards look the same when all displayed values are equal. */
    public String fingerprint() {
        return toJson().toString();
    }

    public JSONObject toJson() {
        JSONObject json = new JSONObject();

        try {
            json.put("key", key);
            json.put("raceName", raceName);
            json.put("raceSeason", raceSeason);
            put(json, "saveDate", saveDate);
            json.put("scheduleKnown", scheduleKnown);
            json.put("round", round);
            put(json, "raceDate", raceDate);
            put(json, "raceTime", raceTime);
            put(json, "firstPracticeDate", firstPracticeDate);
            put(json, "firstPracticeTime", firstPracticeTime);
            put(json, "winnerCode", winnerCode);
            put(json, "secondCode", secondCode);
            put(json, "thirdCode", thirdCode);
            put(json, "fastestLapDriver", fastestLapDriver);
            put(json, "fastestLapTime", fastestLapTime);
            put(json, "sprintDate", sprintDate);
            put(json, "sprintWinnerCode", sprintWinnerCode);
            json.put("canceled", canceled);
            put(json, "circuitId", circuitId);
            put(json, "circuitName", circuitName);
            put(json, "country", country);
            put(json, "length", length);
            put(json, "lapsCount", lapsCount);
            put(json, "turnsCount", turnsCount);
            put(json, "lapRecordTime", lapRecordTime);
            put(json, "lapRecordDriver", lapRecordDriver);
        } catch (JSONException error) {
            throw new IllegalStateException(error);
        }

        return json;
    }

    public static savedRaceCard fromJson(JSONObject json) throws JSONException {
        savedRaceCard race = new savedRaceCard(
                json.getString("key"),
                json.getString("raceName"),
                json.getString("raceSeason"),
                json.has("saveDate") ? json.getString("saveDate") : ""
        );

        race.scheduleKnown = json.optBoolean("scheduleKnown", false);
        race.round = json.optInt("round", 0);
        race.raceDate = opt(json, "raceDate");
        race.raceTime = opt(json, "raceTime");
        race.firstPracticeDate = opt(json, "firstPracticeDate");
        race.firstPracticeTime = opt(json, "firstPracticeTime");
        race.winnerCode = opt(json, "winnerCode");
        race.secondCode = opt(json, "secondCode");
        race.thirdCode = opt(json, "thirdCode");
        race.fastestLapDriver = opt(json, "fastestLapDriver");
        race.fastestLapTime = opt(json, "fastestLapTime");
        race.sprintDate = opt(json, "sprintDate");
        race.sprintWinnerCode = opt(json, "sprintWinnerCode");
        race.canceled = json.optBoolean("canceled", false);
        race.circuitId = opt(json, "circuitId");
        race.circuitName = opt(json, "circuitName");
        race.country = opt(json, "country");
        race.length = opt(json, "length");
        race.lapsCount = opt(json, "lapsCount");
        race.turnsCount = opt(json, "turnsCount");
        race.lapRecordTime = opt(json, "lapRecordTime");
        race.lapRecordDriver = opt(json, "lapRecordDriver");
        return race;
    }

    private static void put(JSONObject json, String name, String value) throws JSONException {
        if (value != null) {
            json.put(name, value);
        }
    }

    private static String opt(JSONObject json, String name) {
        return json.has(name) ? json.optString(name, null) : null;
    }
}
