package com.example.f1app;

import android.content.Context;

import androidx.annotation.NonNull;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONArray;
import org.json.JSONObject;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The weather of every session of a weekend, past or future: one request per weekend.
 * Recent and future days come from the forecast API, old ones from the archive.
 */
final class weekendWeather {

    interface Callback {
        void onLoaded(Map<String, sessionWeather> byKey, String note);
    }

    private static final int PAST_LIMIT_DAYS = 90;
    private static final int FUTURE_LIMIT_DAYS = 15;
    private static final long KEEP_MS = 30 * 60 * 1000L;
    private static final DateTimeFormatter HOUR_KEY = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");

    private static final Map<String, JSONObject> RAW = new HashMap<>();
    private static final Map<String, Long> STAMP = new HashMap<>();

    private weekendWeather() {
    }

    static void load(Context context, String season, String raceName, Callback callback) {
        final Context app = context.getApplicationContext();

        FirebaseDatabase.getInstance().getReference().child("schedule").child("season")
                .child(season).child(raceName)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        List<raceWeekendStatus.SessionInfo> sessions = raceWeekendStatus.sessions(snapshot);
                        String circuitId = snapshot.child("Circuit").child("circuitId").getValue(String.class);

                        if (sessions.isEmpty() || circuitId == null) {
                            callback.onLoaded(new HashMap<>(), "");
                            return;
                        }

                        loadCircuit(app, circuitId, sessions, callback);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        callback.onLoaded(new HashMap<>(), "");
                    }
                });
    }

    private static void loadCircuit(Context app, String circuitId,
                                    List<raceWeekendStatus.SessionInfo> sessions, Callback callback) {
        FirebaseDatabase.getInstance().getReference().child("circuits").child(circuitId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        Object lat = snapshot.child("lat").getValue();
                        Object lng = snapshot.child("lng").getValue();

                        if (!(lat instanceof Number) || !(lng instanceof Number)) {
                            callback.onLoaded(new HashMap<>(), "");
                            return;
                        }

                        request(app, ((Number) lat).doubleValue(), ((Number) lng).doubleValue(),
                                sessions, callback);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        callback.onLoaded(new HashMap<>(), "");
                    }
                });
    }

    private static void request(Context app, double lat, double lng,
                                List<raceWeekendStatus.SessionInfo> sessions, Callback callback) {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        LocalDate first = day(sessions.get(0).start);
        LocalDate last = day(sessions.get(sessions.size() - 1).start);

        if (first.isAfter(today.plusDays(FUTURE_LIMIT_DAYS))) {
            LocalDate from = first.minusDays(FUTURE_LIMIT_DAYS);
            callback.onLoaded(new HashMap<>(), app.getString(R.string.weather_available_from,
                    from.format(DateTimeFormatter.ofPattern("d MMM", Locale.getDefault()))));
            return;
        }

        boolean archive = first.isBefore(today.minusDays(PAST_LIMIT_DAYS));
        String hourly = "temperature_2m,precipitation,weather_code,wind_speed_10m"
                + (archive ? "" : ",precipitation_probability");

        StringBuilder url = new StringBuilder(archive
                ? "https://archive-api.open-meteo.com/v1/archive"
                : "https://api.open-meteo.com/v1/forecast");
        url.append("?latitude=").append(String.format(Locale.US, "%.4f", lat));
        url.append("&longitude=").append(String.format(Locale.US, "%.4f", lng));
        url.append("&hourly=").append(hourly).append("&timezone=GMT");

        if (archive) {
            url.append("&start_date=").append(first).append("&end_date=").append(last);
        } else {
            long pastDays = Math.max(0, ChronoUnit.DAYS.between(first, today));
            long forecastDays = Math.min(16, Math.max(1, ChronoUnit.DAYS.between(today, last) + 1));
            url.append("&past_days=").append(pastDays).append("&forecast_days=").append(forecastDays);
        }

        final String key = url.toString();
        JSONObject known = RAW.get(key);
        Long at = STAMP.get(key);

        if (known != null && at != null && (archive || System.currentTimeMillis() - at < KEEP_MS)) {
            callback.onLoaded(parse(app, known, sessions), "Open-Meteo.com");
            return;
        }

        apiQueue.get().add(new JsonObjectRequest(Request.Method.GET, key, null,
                response -> {
                    RAW.put(key, response);
                    STAMP.put(key, System.currentTimeMillis());
                    callback.onLoaded(parse(app, response, sessions), "Open-Meteo.com");
                },
                error -> callback.onLoaded(new HashMap<>(), "")));
    }

    private static Map<String, sessionWeather> parse(Context app, JSONObject response,
                                                     List<raceWeekendStatus.SessionInfo> sessions) {
        Map<String, sessionWeather> result = new HashMap<>();
        JSONObject hourly = response.optJSONObject("hourly");
        JSONArray times = hourly == null ? null : hourly.optJSONArray("time");

        if (times == null) {
            return result;
        }

        Map<String, Integer> index = new HashMap<>();

        for (int i = 0; i < times.length(); i++) {
            index.put(times.optString(i), i);
        }

        JSONArray temp = hourly.optJSONArray("temperature_2m");
        JSONArray rain = hourly.optJSONArray("precipitation");
        JSONArray prob = hourly.optJSONArray("precipitation_probability");
        JSONArray code = hourly.optJSONArray("weather_code");
        JSONArray wind = hourly.optJSONArray("wind_speed_10m");
        long now = System.currentTimeMillis();

        for (raceWeekendStatus.SessionInfo session : sessions) {
            String hour = Instant.ofEpochMilli(session.start).atZone(ZoneOffset.UTC)
                    .truncatedTo(ChronoUnit.HOURS).format(HOUR_KEY);
            Integer i = index.get(hour);

            if (i == null || temp == null || temp.isNull(i)) {
                continue;
            }

            boolean future = session.start > now;
            double mm = rain == null ? Double.NaN : rain.optDouble(i, Double.NaN);
            double chance = prob == null || prob.isNull(i) ? Double.NaN : prob.optDouble(i, Double.NaN);

            List<String> parts = new ArrayList<>();
            boolean wet;

            if (future && !Double.isNaN(chance)) {
                parts.add(Math.round(chance) + "%");
                wet = chance >= 50;
            } else if (!Double.isNaN(mm) && mm >= 0.1) {
                parts.add(app.getString(R.string.weather_precip_mm,
                        String.format(Locale.getDefault(), "%.1f", mm)));
                wet = true;
            } else {
                parts.add(app.getString(R.string.weather_dry));
                wet = false;
            }

            parts.add(weatherCard.windText(app, wind == null ? Double.NaN : wind.optDouble(i, Double.NaN)));

            result.put(session.key, new sessionWeather(
                    weatherCard.iconFor(code == null ? 0 : code.optInt(i, 0)),
                    weatherCard.degrees(temp.optDouble(i, Double.NaN)),
                    String.join(", ", parts),
                    wet));
        }

        return result;
    }

    private static LocalDate day(long millis) {
        return Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate();
    }
}