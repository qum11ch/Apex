package com.example.f1app;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class predictionBooleanHistoryLoader {
    private static final String TAG = "BooleanHistory";
    private static final int MAX_RACES = 10;
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
        public final State rain;
        public final String rainReason;
        public final String rainError;
        public final State sc;
        public final State vsc;
        public final String safetyError;

        Row(
                int sessionKey,
                String circuit,
                String date,
                State rain,
                String rainReason,
                String rainError,
                State sc,
                State vsc,
                String safetyError
        ) {
            this.sessionKey = sessionKey;
            this.circuit = circuit;
            this.date = date;
            this.rain = rain;
            this.rainReason = rainReason;
            this.rainError = rainError;
            this.sc = sc;
            this.vsc = vsc;
            this.safetyError = safetyError;
        }
    }

    public static final class Data {
        public final List<Row> rows;
        public final int failedRequests;
        public final List<Integer> unavailableYears;

        public Data(
                List<Row> rows,
                int failedRequests,
                List<Integer> unavailableYears
        ) {
            this.rows = Collections.unmodifiableList(new ArrayList<>(rows));
            this.failedRequests = failedRequests;
            this.unavailableYears = Collections.unmodifiableList(
                    new ArrayList<>(unavailableYears)
            );
        }
    }

    private final Map<Integer, Data> cache = new HashMap<>();

    public void load(
            String location,
            String circuitName,
            String circuitId,
            String firstPracticeDate,
            Callback callback
    ) {
        new Thread(() -> {
            try {
                Data data = loadSync(
                        location,
                        circuitName,
                        circuitId,
                        firstPracticeDate
                );

                MAIN.post(() -> callback.onLoaded(data));

            } catch (Exception error) {
                Log.e(TAG, "History load failed", error);
                MAIN.post(() -> callback.onError(error.toString()));
            }
        }).start();
    }

    private Data loadSync(
            String location,
            String circuitName,
            String circuitId,
            String firstPracticeDate
    ) throws Exception {
        if (firstPracticeDate == null || firstPracticeDate.trim().isEmpty()) {
            throw new IOException("First practice date is required");
        }

        Instant upper = Instant.parse(firstPracticeDate.trim());
        int cacheKey = upper.hashCode();

        Data cached = cache.get(cacheKey);

        if (cached != null) {
            return cached;
        }

        List<Integer> unavailableYears = new ArrayList<>();
        List<JSONObject> candidates = new ArrayList<>();
        Set<Integer> keys = new LinkedHashSet<>();
        int loadedYears = 0;

        for (int year = 2025; year >= 2023; year--) {
            JSONArray list;

            try {
                list = get("sessions?year=" + year + "&session_name=Race");
                loadedYears++;

            } catch (Exception error) {
                unavailableYears.add(year);
                Log.w(TAG, "Sessions unavailable for year=" + year, error);
                continue;
            }

            for (int i = 0; i < list.length(); i++) {
                JSONObject session = list.getJSONObject(i);

                if (!"Race".equals(session.optString("session_name"))
                        || session.optBoolean("is_cancelled", false)) {
                    continue;
                }

                if (!match(session, location, circuitName, circuitId)) {
                    continue;
                }

                Instant finish = Instant.parse(session.getString("date_end"));

                if (!finish.isBefore(upper)) {
                    continue;
                }

                keys.add(session.getInt("circuit_key"));
                candidates.add(session);
            }
        }

        if (loadedYears == 0) {
            throw new IOException(
                    "OpenF1 sessions unavailable for all requested years"
            );
        }

        List<Integer> resolvedKeys = predictionCircuitKeyResolver.resolve(
                circuitId,
                circuitName,
                location,
                new ArrayList<>(keys)
        );

        if (resolvedKeys.isEmpty()) {
            if (!unavailableYears.isEmpty()) {
                throw new IOException(
                        "Circuit lookup incomplete; unavailable years="
                                + unavailableYears
                );
            }

            Data empty = new Data(Collections.emptyList(), 0, unavailableYears);
            cache.put(cacheKey, empty);
            return empty;
        }

        List<Row> rows = new ArrayList<>();
        Set<Integer> seen = new HashSet<>();
        int failed = unavailableYears.size();

        for (JSONObject session : candidates) {
            int key = session.getInt("circuit_key");

            if (!resolvedKeys.contains(key) || !seen.add(key)) {
                continue;
            }

            String sessionKey = String.valueOf(session.getInt("session_key"));
            Instant finish = Instant.parse(session.getString("date_end"));
            Row row = rainRow(session, sessionKey, finish);

            if (row != null) {
                rows.add(row);
            }

            if (rows.size() >= MAX_RACES) {
                break;
            }
        }

        rows.sort((left, right) -> right.date.compareTo(left.date));

        Data data = new Data(rows, failed, unavailableYears);
        cache.put(cacheKey, data);
        return data;
    }

    private Row rainRow(
            JSONObject session,
            String sessionKey,
            Instant finish
    ) {
        String circuit = session.optString("circuit_short_name", "");
        String date = session.optString("date_start", "");

        try {
            JSONArray weather = get(
                    "weather?session_key=" + sessionKey
            );

            boolean rainSeen = false;
            int samples = 0;

            for (int i = 0; i < weather.length(); i++) {
                JSONObject item = weather.getJSONObject(i);
                Instant time = Instant.parse(item.getString("date"));

                if (time.isAfter(finish)
                        || time.isBefore(finish.minus(Duration.ofHours(6)))) {
                    continue;
                }

                double rainfall = item.optDouble("rainfall", 0);
                samples++;

                if (rainfall > 0) {
                    rainSeen = true;
                }
            }

            if (samples == 0) {
                return new Row(
                        Integer.parseInt(sessionKey),
                        circuit,
                        date,
                        State.UNKNOWN,
                        "MEASUREMENT_GAP",
                        null,
                        State.UNKNOWN,
                        State.UNKNOWN,
                        null
                );
            }

            return new Row(
                    Integer.parseInt(sessionKey),
                    circuit,
                    date,
                    rainSeen ? State.YES : State.NO,
                    null,
                    null,
                    State.UNKNOWN,
                    State.UNKNOWN,
                    null
            );

        } catch (Exception error) {
            Log.e(TAG, "Rain/window failed: session=" + sessionKey, error);

            return new Row(
                    Integer.parseInt(sessionKey),
                    circuit,
                    date,
                    State.UNKNOWN,
                    null,
                    error.toString(),
                    State.UNKNOWN,
                    State.UNKNOWN,
                    error.toString()
            );
        }
    }

    private boolean match(
            JSONObject session,
            String location,
            String circuitName,
            String circuitId
    ) {
        String sessionLocation = session.optString("location", "");
        String sessionCircuit = session.optString("circuit_short_name", "");
        String sessionCountry = session.optString("country_name", "");

        return equalsIgnoreCase(sessionLocation, location)
                || equalsIgnoreCase(sessionCircuit, circuitName)
                || equalsIgnoreCase(sessionCountry, circuitId);
    }

    private boolean equalsIgnoreCase(String left, String right) {
        return left != null
                && right != null
                && left.trim().equalsIgnoreCase(right.trim());
    }

    private JSONArray get(String path) throws Exception {
        return new JSONArray(predictionOpenF1Http.get(path));
    }
}
