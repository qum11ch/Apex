package com.example.f1app;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * One saved race together with what the card needs to show.
 * It is NOT what is written to the database: saving a race still uses {@link savedRacesData}.
 */
public class savedRaceCard {

    // Same value as RACE_WINDOW_MS in raceWeekendStatus.
    private static final long RACE_WINDOW_MS = 3L * 60 * 60 * 1000;
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-M-d");

    // what the user saved
    public final String key;
    public final String raceName;
    public final String raceSeason;
    public final String saveDate;

    // schedule/season/{season}/{race}; scheduleKnown is false when it could not be found
    public boolean scheduleKnown;
    public int round;
    public String raceDate;
    public String firstPracticeDate;
    public String winnerCode;
    public String secondCode;
    public String thirdCode;
    public String fastestLapDriver;
    public String fastestLapTime;
    public String sprintDate;
    public boolean canceled;
    public List<raceWeekendStatus.SessionInfo> sessions = new ArrayList<>();

    // circuits/{circuitId}
    public String circuitId;
    public String circuitName;
    public String country;
    public String locality;
    public String length;
    public String lapsCount;
    public String turnsCount;

    public savedRaceCard(String key, String raceName, String raceSeason, String saveDate) {
        this.key = key;
        this.raceName = raceName;
        this.raceSeason = raceSeason;
        this.saveDate = saveDate;
    }

    // ---------- values ----------

    public static boolean isMissing(String value) {
        if (value == null) {
            return true;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() || "n/a".equalsIgnoreCase(trimmed);
    }

    public boolean hasResult() {
        return raceWeekendStatus.isValidCode(winnerCode);
    }

    public boolean hasPodium() {
        return raceWeekendStatus.isValidCode(winnerCode)
                && raceWeekendStatus.isValidCode(secondCode)
                && raceWeekendStatus.isValidCode(thirdCode);
    }

    public boolean isSprintWeekend() {
        return !isMissing(sprintDate);
    }

    // ---------- state ----------

    /** Start of the race session in milliseconds, or null when the schedule has no race time. */
    public Long raceStartMs() {
        for (raceWeekendStatus.SessionInfo session : sessions) {
            if ("race_event".equals(session.key)) {
                return session.start;
            }
        }

        return null;
    }

    /**
     * The same rules as raceWeekendStatus.resolve, evaluated from the stored sessions so that
     * the state follows the clock without reading the database again.
     */
    public raceWeekendStatus.State stateAt(long nowMs) {
        Long weekendStart = sessions.isEmpty() ? null : sessions.get(0).start;
        Long raceStart = raceStartMs();
        boolean started = weekendStart != null && nowMs >= weekendStart;

        if (canceled) {
            return started ? raceWeekendStatus.State.CANCELED : raceWeekendStatus.State.FUTURE;
        }

        if (hasResult()) {
            return raceWeekendStatus.State.CONCLUDED;
        }

        if (weekendStart == null || raceStart == null || !started) {
            return raceWeekendStatus.State.FUTURE;
        }

        return nowMs < raceStart + RACE_WINDOW_MS
                ? raceWeekendStatus.State.ONGOING
                : raceWeekendStatus.State.AWAITING_RESULTS;
    }

    // ---------- dates ----------

    public LocalDate raceLocalDate() {
        return parseDate(raceDate);
    }

    /** First day of the weekend, or the race day when practice is unknown. */
    public LocalDate weekendStartDate() {
        LocalDate practice = parseDate(firstPracticeDate);
        return practice != null ? practice : raceLocalDate();
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
            put(json, "firstPracticeDate", firstPracticeDate);
            put(json, "winnerCode", winnerCode);
            put(json, "secondCode", secondCode);
            put(json, "thirdCode", thirdCode);
            put(json, "fastestLapDriver", fastestLapDriver);
            put(json, "fastestLapTime", fastestLapTime);
            put(json, "sprintDate", sprintDate);
            json.put("canceled", canceled);
            put(json, "circuitId", circuitId);
            put(json, "circuitName", circuitName);
            put(json, "country", country);
            put(json, "locality", locality);
            put(json, "length", length);
            put(json, "lapsCount", lapsCount);
            put(json, "turnsCount", turnsCount);

            JSONArray array = new JSONArray();

            for (raceWeekendStatus.SessionInfo session : sessions) {
                JSONObject item = new JSONObject();
                item.put("k", session.key);
                item.put("s", session.start);
                item.put("e", session.end);
                array.put(item);
            }

            json.put("sessions", array);
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
        race.firstPracticeDate = opt(json, "firstPracticeDate");
        race.winnerCode = opt(json, "winnerCode");
        race.secondCode = opt(json, "secondCode");
        race.thirdCode = opt(json, "thirdCode");
        race.fastestLapDriver = opt(json, "fastestLapDriver");
        race.fastestLapTime = opt(json, "fastestLapTime");
        race.sprintDate = opt(json, "sprintDate");
        race.canceled = json.optBoolean("canceled", false);
        race.circuitId = opt(json, "circuitId");
        race.circuitName = opt(json, "circuitName");
        race.country = opt(json, "country");
        race.locality = opt(json, "locality");
        race.length = opt(json, "length");
        race.lapsCount = opt(json, "lapsCount");
        race.turnsCount = opt(json, "turnsCount");

        JSONArray array = json.optJSONArray("sessions");

        if (array != null) {
            for (int i = 0; i < array.length(); i++) {
                JSONObject item = array.getJSONObject(i);
                race.sessions.add(new raceWeekendStatus.SessionInfo(
                        item.getString("k"),
                        item.getLong("s"),
                        item.getLong("e")
                ));
            }
        }

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
