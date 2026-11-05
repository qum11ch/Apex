package com.example.f1app;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class predictionBooleanHistoryLoader {
    private static final String TAG = "BooleanHistory";
    private static final String BASE_URL = "https://api.openf1.org/v1/";

    private static final int FIRST_YEAR = 2023;
    private static final int MAX_RACES = 5;
    private static final int MAX_RESPONSE_BYTES = 8 * 1024 * 1024;

    private static final long GAP_MS = 5 * 60 * 1000L;

    private static final ExecutorService IO = Executors.newSingleThreadExecutor();
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    public interface Callback {
        void onLoaded(Data data);

        void onError(String message);
    }

    public enum State {
        YES,
        NO,
        UNKNOWN
    }

    public static final class Row {
        public final int sessionKey;
        public final String circuit;
        public final String date;

        public State rain = State.UNKNOWN;
        public String rainReason;
        public String rainError;

        public State sc = State.UNKNOWN;
        public State vsc = State.UNKNOWN;
        public String safetyError;

        Row(int sessionKey, String circuit, String date) {
            this.sessionKey = sessionKey;
            this.circuit = circuit;
            this.date = date;
        }
    }

    public static final class Data {
        public final List<Row> rows;
        public final int failedRequests;
        public final List<Integer> unavailableYears;

        public Data(List<Row> rows, int failedRequests, List<Integer> unavailableYears) {
            this.rows = Collections.unmodifiableList(new ArrayList<>(rows));
            this.failedRequests = failedRequests;
            this.unavailableYears = Collections.unmodifiableList(new ArrayList<>(unavailableYears));
        }
    }

    public void load(String location, String circuitName, String circuitId, String firstPracticeDate, Callback callback) {
        IO.execute(() -> {
            try {
                Data data = loadSync(location, circuitName, circuitId, firstPracticeDate);

                MAIN.post(() -> callback.onLoaded(data));
            } catch (Exception error) {
                Log.e(TAG, "History load failed", error);

                MAIN.post(() -> callback.onError(error.toString()));
            }
        });
    }

    private Data loadSync(String location, String circuitName, String circuitId, String firstPracticeDate) throws Exception {
        if (firstPracticeDate == null || firstPracticeDate.trim().isEmpty()) {
            throw new IOException("First practice date is required");
        }

        Instant cutoff = parseCutoff(firstPracticeDate);
        Instant now = Instant.now();
        Instant upper = cutoff.isBefore(now) ? cutoff : now;

        int lastYear = upper.atOffset(ZoneOffset.UTC).getYear();

        List<JSONObject> candidates = new ArrayList<>();
        List<Integer> unavailableYears = new ArrayList<>();
        Set<Integer> circuitKeys = new HashSet<>();

        int loadedYears = 0;

        Log.d(TAG, "Lookup: id=" + circuitId
                + "; circuit=" + circuitName
                + "; location=" + location
                + "; before=" + upper);

        for (int year = FIRST_YEAR; year <= lastYear; year++) {
            JSONArray sessions;

            try {
                sessions = get("sessions?year=" + year + "&session_name=Race");

                loadedYears++;
            } catch (InterruptedException error) {
                Thread.currentThread().interrupt();

                throw error;
            } catch (Exception error) {
                unavailableYears.add(year);

                Log.w(TAG, "Sessions unavailable for year=" + year, error);

                continue;
            }

            for (int i = 0; i < sessions.length(); i++) {
                JSONObject session = sessions.getJSONObject(i);

                if (!"Race".equals(session.optString("session_name"))
                        || session.optBoolean("is_cancelled", false)) {
                    continue;
                }

                if (!matches(session, location, circuitName, circuitId)) {
                    continue;
                }

                Instant start = instant(session.getString("date_start"));
                Instant finish = instant(session.getString("date_end"));

                if (!finish.isBefore(upper) || !finish.isAfter(start)) {
                    continue;
                }

                int circuitKey = session.getInt("circuit_key");

                circuitKeys.add(circuitKey);
                candidates.add(session);

                Log.d(TAG, "Matched: session=" + session.getInt("session_key")
                        + "; key=" + circuitKey
                        + "; circuit=" + session.optString("circuit_short_name")
                        + "; location=" + session.optString("location")
                        + "; date=" + start);
            }
        }

        if (loadedYears == 0) {
            throw new IOException("OpenF1 sessions unavailable for all requested years");
        }

        if (circuitKeys.size() != 1) {
            throw new IOException("Circuit match is missing or ambiguous"
                    + "; id=" + circuitId
                    + "; location=" + location
                    + "; keys=" + circuitKeys);
        }

        candidates.sort((left, right) ->
                right.optString("date_start").compareTo(left.optString("date_start")));

        List<Row> rows = new ArrayList<>();
        Set<Integer> seenSessions = new HashSet<>();

        int failedRequests = unavailableYears.size();

        for (JSONObject session : candidates) {
            int sessionKey = session.getInt("session_key");

            if (!seenSessions.add(sessionKey)) {
                continue;
            }

            Instant start = instant(session.getString("date_start"));
            Instant finish = instant(session.getString("date_end"));

            Row row = new Row(
                    sessionKey,
                    session.optString("circuit_short_name", location),
                    start.atOffset(ZoneOffset.UTC).toLocalDate().toString()
            );

            try {
                predictionRaceWindowResolver.Window window =
                        predictionRaceWindowResolver.resolve(sessionKey, start, finish);

                Log.d(TAG, "Rain window: session=" + sessionKey
                        + "; resolved=" + window.resolved
                        + "; source=" + window.source
                        + "; reason=" + window.reason
                        + "; start=" + Instant.ofEpochMilli(window.startMs)
                        + "; end=" + Instant.ofEpochMilli(window.endMs)
                        + "; metaStart=" + start
                        + "; metaEnd=" + finish);

                if (!window.resolved) {
                    row.rain = State.UNKNOWN;
                    row.rainReason = window.reason;
                } else {
                    Instant rainStart = Instant.ofEpochMilli(window.startMs);
                    Instant rainEnd = Instant.ofEpochMilli(window.endMs);

                    JSONArray weather = get("weather?session_key=" + sessionKey);

                    logWeatherCoverage(sessionKey, weather, rainStart, rainEnd);

                    row.rain = rain(weather, rainStart, rainEnd);

                    if (row.rain == State.UNKNOWN) {
                        row.rainReason = "MEASUREMENT_GAP";
                    }
                }
            } catch (InterruptedException error) {
                Thread.currentThread().interrupt();

                throw error;
            } catch (Exception error) {
                row.rainError = error.toString();
                failedRequests++;

                Log.w(TAG, "Rain window or weather failed: session=" + sessionKey, error);
            }

            try {
                JSONArray control = get("race_control?session_key=" + sessionKey);

                safety(control, start, finish, row);
                logRaceControlBoundaries(sessionKey, control);
            } catch (InterruptedException error) {
                Thread.currentThread().interrupt();

                throw error;
            } catch (Exception error) {
                row.safetyError = error.toString();
                failedRequests++;

                Log.w(TAG, "Race control failed: session=" + sessionKey, error);
            }

            rows.add(row);

            Log.d(TAG, "Result: session=" + sessionKey
                    + "; circuit=" + row.circuit
                    + "; date=" + row.date
                    + "; rain=" + row.rain
                    + "; sc=" + row.sc
                    + "; vsc=" + row.vsc);

            if (rows.size() >= MAX_RACES) {
                break;
            }
        }

        return new Data(rows, failedRequests, unavailableYears);
    }

    private static void logRaceControlBoundaries(int sessionKey, JSONArray control) {
        Log.d(TAG, "RaceControlSummary"
                + "; session=" + sessionKey
                + "; entries=" + control.length());

        List<JSONObject> timedEntries = new ArrayList<>();

        for (int i = 0; i < control.length(); i++) {
            JSONObject item = control.optJSONObject(i);

            if (item == null) {
                continue;
            }

            try {
                instant(item.getString("date"));

                timedEntries.add(item);
            } catch (Exception error) {
                Log.w(TAG, "RaceControl invalid date"
                        + "; session=" + sessionKey
                        + "; item=" + item);
            }
        }

        timedEntries.sort((left, right) ->
                instant(left.optString("date")).compareTo(
                        instant(right.optString("date"))
                ));

        Set<Integer> selected = new HashSet<>();

        for (int i = 0; i < timedEntries.size(); i++) {
            JSONObject item = timedEntries.get(i);

            String flag = item.optString("flag", "");
            String message = item.optString("message", "").toUpperCase(Locale.ROOT);

            boolean boundaryCandidate = !flag.isEmpty()
                    || message.contains("CHEQUER")
                    || message.contains("CHECKER")
                    || message.contains("FINISH")
                    || message.contains("ENDED")
                    || message.contains("START");

            boolean edge = i < 5 || i >= timedEntries.size() - 10;

            if (boundaryCandidate || edge) {
                selected.add(i);
            }
        }

        for (int i = 0; i < timedEntries.size(); i++) {
            if (selected.contains(i)) {
                Log.d(TAG, "RaceControlBoundary"
                        + "; session=" + sessionKey
                        + "; item=" + timedEntries.get(i));
            }
        }
    }

    private static Instant parseCutoff(String value) {
        String date = value.trim();

        if (date.length() == 10) {
            return LocalDate.parse(date).atStartOfDay(ZoneOffset.UTC).toInstant();
        }

        return instant(date);
    }

    private static Instant instant(String value) {
        return OffsetDateTime.parse(value).toInstant();
    }

    private static boolean matches(JSONObject session, String location, String circuitName, String circuitId) {
        String sessionLocation = normal(session.optString("location"));
        String sessionCircuit = normal(session.optString("circuit_short_name"));

        String[] localNames = {location, circuitName, circuitId};

        for (String name : localNames) {
            String normalized = normal(name);

            if (!normalized.isEmpty()
                    && (normalized.equals(sessionLocation) || normalized.equals(sessionCircuit))) {
                return true;
            }
        }

        return false;
    }

    private static void logWeatherCoverage(int sessionKey, JSONArray weather, Instant start, Instant end) {
        List<Long> drySamples = new ArrayList<>();

        int wetSamples = 0;
        int invalidSamples = 0;
        int outsideSamples = 0;

        for (int i = 0; i < weather.length(); i++) {
            JSONObject item = weather.optJSONObject(i);

            if (item == null || item.isNull("date") || item.isNull("rainfall")) {
                invalidSamples++;

                continue;
            }

            try {
                Instant time = instant(item.getString("date"));

                if (time.isBefore(start) || time.isAfter(end)) {
                    outsideSamples++;

                    continue;
                }

                Object raw = item.get("rainfall");

                if (!(raw instanceof Number)) {
                    invalidSamples++;

                    continue;
                }

                double value = ((Number) raw).doubleValue();

                if (value == 1) {
                    wetSamples++;
                } else if (value == 0) {
                    drySamples.add(time.toEpochMilli());
                } else {
                    invalidSamples++;
                }
            } catch (Exception error) {
                invalidSamples++;
            }
        }

        Collections.sort(drySamples);

        long maxGapMs = 0;

        for (int i = 1; i < drySamples.size(); i++) {
            maxGapMs = Math.max(maxGapMs, drySamples.get(i) - drySamples.get(i - 1));
        }

        String firstDry = drySamples.isEmpty()
                ? "none"
                : Instant.ofEpochMilli(drySamples.get(0)).toString();

        String lastDry = drySamples.isEmpty()
                ? "none"
                : Instant.ofEpochMilli(drySamples.get(drySamples.size() - 1)).toString();

        String startGapMs = drySamples.isEmpty()
                ? "none"
                : String.valueOf(drySamples.get(0) - start.toEpochMilli());

        String endGapMs = drySamples.isEmpty()
                ? "none"
                : String.valueOf(end.toEpochMilli() - drySamples.get(drySamples.size() - 1));

        Log.d(TAG, "WeatherCoverage"
                + "; session=" + sessionKey
                + "; start=" + start
                + "; end=" + end
                + "; responseSamples=" + weather.length()
                + "; drySamples=" + drySamples.size()
                + "; wetSamples=" + wetSamples
                + "; invalidSamples=" + invalidSamples
                + "; outsideSamples=" + outsideSamples
                + "; firstDry=" + firstDry
                + "; lastDry=" + lastDry
                + "; startGapMs=" + startGapMs
                + "; endGapMs=" + endGapMs
                + "; maxDryGapMs=" + maxGapMs);
    }

    private static String normal(String value) {
        if (value == null) {
            return "";
        }

        return Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]", "");
    }

    private static State rain(JSONArray weather, Instant start, Instant end) {
        List<Long> drySamples = new ArrayList<>();

        for (int i = 0; i < weather.length(); i++) {
            JSONObject item = weather.optJSONObject(i);

            if (item == null || item.isNull("date") || item.isNull("rainfall")) {
                continue;
            }

            try {
                Instant time = instant(item.getString("date"));

                if (time.isBefore(start) || time.isAfter(end)) {
                    continue;
                }

                Object raw = item.get("rainfall");

                if (!(raw instanceof Number)) {
                    continue;
                }

                double value = ((Number) raw).doubleValue();

                if (value == 1) {
                    return State.YES;
                }

                if (value == 0) {
                    drySamples.add(time.toEpochMilli());
                }
            } catch (Exception ignored) {
            }
        }

        if (drySamples.size() < 2) {
            return State.UNKNOWN;
        }

        Collections.sort(drySamples);

        if (drySamples.get(0) - start.toEpochMilli() > GAP_MS
                || end.toEpochMilli() - drySamples.get(drySamples.size() - 1) > GAP_MS) {
            return State.UNKNOWN;
        }

        for (int i = 1; i < drySamples.size(); i++) {
            if (drySamples.get(i) - drySamples.get(i - 1) > GAP_MS) {
                return State.UNKNOWN;
            }
        }

        return State.NO;
    }

    private static void safety(JSONArray control, Instant metaStart, Instant metaEnd, Row row) {
        row.sc = State.UNKNOWN;
        row.vsc = State.UNKNOWN;

        Instant raceStart = null;
        Instant raceEnd = null;

        Instant earliest = metaStart.minusSeconds(300);
        Instant now = Instant.now();

        List<JSONObject> entries = new ArrayList<>();

        int invalidCount = 0;

        for (int i = 0; i < control.length(); i++) {
            JSONObject item = control.optJSONObject(i);

            if (item == null) {
                invalidCount++;

                continue;
            }

            try {
                Instant time = instant(item.getString("date"));

                if (time.isBefore(earliest) || time.isAfter(now)) {
                    continue;
                }

                entries.add(item);

                String message = item.optString("message")
                        .toUpperCase(Locale.ROOT)
                        .trim()
                        .replaceAll("\\s+", " ");

                boolean startMessage = "SESSION STARTED".equals(message)
                        || "RACE STARTED".equals(message);

                if (startMessage && item.optInt("lap_number", -1) == 1) {
                    if (raceStart == null || time.isBefore(raceStart)) {
                        raceStart = time;
                    }
                }
            } catch (Exception error) {
                invalidCount++;

                Log.w(TAG, "SafetyInvalid"
                        + "; session=" + row.sessionKey
                        + "; item=" + item, error);
            }
        }

        if (raceStart != null) {
            for (JSONObject item : entries) {
                Instant time = instant(item.optString("date"));

                String message = item.optString("message")
                        .toUpperCase(Locale.ROOT)
                        .trim()
                        .replaceAll("\\s+", " ");

                boolean trackScope = "Track".equalsIgnoreCase(item.optString("scope"));

                boolean chequered = "CHEQUERED".equalsIgnoreCase(item.optString("flag"))
                        || "CHEQUERED FLAG".equals(message);

                if (trackScope && chequered && time.isAfter(raceStart)) {
                    if (raceEnd == null || time.isBefore(raceEnd)) {
                        raceEnd = time;
                    }
                }
            }
        }

        boolean bounded = raceStart != null
                && raceEnd != null
                && raceEnd.isAfter(raceStart)
                && raceEnd.toEpochMilli() - raceStart.toEpochMilli()
                <= 8 * 60 * 60 * 1000L;

        Instant evidenceStart = raceStart == null ? metaStart : raceStart;
        Instant evidenceEnd = bounded ? raceEnd : now;

        boolean ambiguousSc = false;
        boolean ambiguousVsc = false;

        int insideCount = 0;

        for (JSONObject item : entries) {
            Instant time = instant(item.optString("date"));

            if (time.isBefore(evidenceStart) || time.isAfter(evidenceEnd)) {
                continue;
            }

            insideCount++;

            String message = item.optString("message")
                    .toUpperCase(Locale.ROOT)
                    .trim()
                    .replaceAll("\\s+", " ");

            String category = item.optString("category");

            boolean virtual = message.contains("VIRTUAL SAFETY CAR");

            boolean vscEvidence = virtual
                    && (message.contains("DEPLOYED") || message.contains("ENDING"));

            boolean scEvidence = !virtual
                    && ("SAFETY CAR DEPLOYED".equals(message)
                    || "SAFETY CAR IN THIS LAP".equals(message));

            boolean knownScMessage = !virtual
                    && "SAFETY CAR THROUGH THE PIT LANE".equals(message);

            if (vscEvidence) {
                row.vsc = State.YES;
            }

            if (scEvidence) {
                row.sc = State.YES;
            }

            if ("SafetyCar".equalsIgnoreCase(category)
                    && !vscEvidence
                    && !scEvidence
                    && !knownScMessage) {
                if (virtual) {
                    ambiguousVsc = true;
                } else if (message.contains("SAFETY CAR")) {
                    ambiguousSc = true;
                } else {
                    ambiguousSc = true;
                    ambiguousVsc = true;
                }

                Log.d(TAG, "SafetyAmbiguous"
                        + "; session=" + row.sessionKey
                        + "; item=" + item);
            }
        }

        boolean canInferAbsence = bounded && invalidCount == 0;

        if (canInferAbsence) {
            if (row.sc != State.YES && !ambiguousSc) {
                row.sc = State.NO;
            }

            if (row.vsc != State.YES && !ambiguousVsc) {
                row.vsc = State.NO;
            }
        }

        Log.d(TAG, "SafetyDecision"
                + "; session=" + row.sessionKey
                + "; bounded=" + bounded
                + "; raceStart=" + raceStart
                + "; raceEnd=" + raceEnd
                + "; metaStart=" + metaStart
                + "; metaEnd=" + metaEnd
                + "; entries=" + control.length()
                + "; inside=" + insideCount
                + "; invalid=" + invalidCount
                + "; ambiguousSc=" + ambiguousSc
                + "; ambiguousVsc=" + ambiguousVsc
                + "; sc=" + row.sc
                + "; vsc=" + row.vsc);
    }

    private static JSONArray get(String path) throws Exception {
        String address = BASE_URL + path;
        IOException failure = null;

        for (int attempt = 0; attempt < 3; attempt++) {
            long retryDelayMs = 2000L * (attempt + 1);

            HttpURLConnection connection =
                    (HttpURLConnection) new URL(address).openConnection();

            connection.setConnectTimeout(15000);
            connection.setReadTimeout(30000);
            connection.setRequestProperty("Accept", "application/json");

            try {
                Log.d(TAG, "HTTP request"
                        + "; attempt=" + (attempt + 1)
                        + "; url=" + address);

                int status = connection.getResponseCode();

                if (status == 429) {
                    failure = new IOException("OpenF1 HTTP 429; " + address);

                    if (attempt == 2) {
                        throw failure;
                    }

                    String retryAfter = connection.getHeaderField("Retry-After");

                    if (retryAfter != null) {
                        try {
                            long seconds = Long.parseLong(retryAfter.trim());

                            if (seconds < 0 || seconds > 30) {
                                throw new IOException(
                                        "OpenF1 retry deferred; Retry-After="
                                                + retryAfter + "; " + address
                                );
                            }

                            retryDelayMs = Math.max(retryDelayMs, seconds * 1000L);
                        } catch (NumberFormatException error) {
                            throw new IOException(
                                    "OpenF1 retry deferred; Retry-After="
                                            + retryAfter + "; " + address,
                                    error
                            );
                        }
                    }

                    Log.w(TAG, "HTTP rate limit"
                            + "; attempt=" + (attempt + 1)
                            + "; retryDelayMs=" + retryDelayMs
                            + "; url=" + address);
                } else {
                    if (status != 200) {
                        throw new IOException("OpenF1 HTTP " + status + "; " + address);
                    }

                    JSONArray result;

                    try (InputStream input = connection.getInputStream();
                         ByteArrayOutputStream output = new ByteArrayOutputStream()) {
                        byte[] buffer = new byte[8192];
                        int read;

                        while ((read = input.read(buffer)) != -1) {
                            if (output.size() + read > MAX_RESPONSE_BYTES) {
                                throw new IOException(
                                        "OpenF1 response too large; " + address
                                );
                            }

                            output.write(buffer, 0, read);
                        }

                        result = new JSONArray(
                                new String(output.toByteArray(), StandardCharsets.UTF_8)
                        );
                    }

                    Log.d(TAG, "HTTP success"
                            + "; attempt=" + (attempt + 1)
                            + "; entries=" + result.length()
                            + "; url=" + address);

                    Thread.sleep(1000);

                    return result;
                }
            } catch (SocketTimeoutException error) {
                failure = new IOException(
                        "OpenF1 timeout; attempt=" + (attempt + 1)
                                + "; " + address,
                        error
                );

                Log.w(TAG, failure.getMessage());

                if (attempt == 2) {
                    throw failure;
                }
            } finally {
                connection.disconnect();
            }

            Thread.sleep(retryDelayMs);
        }

        throw failure == null
                ? new IOException("OpenF1 request failed; " + address)
                : failure;
    }
}