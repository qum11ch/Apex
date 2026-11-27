package com.example.f1app;

import android.content.Context;

import androidx.annotation.NonNull;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONArray;
import org.json.JSONObject;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The weather of every session of a weekend, past or future.
 * The model (Open-Meteo) covers every session; for finished sessions the trackside measurements
 * of F1 (OpenF1) replace the model when they are available.
 */
final class weekendWeather {

    interface Callback {
        void onLoaded(Map<String, sessionWeather> byKey, String note);
    }

    private static final int PAST_LIMIT_DAYS = 90;
    private static final int FUTURE_LIMIT_DAYS = 15;
    private static final long HOUR_MS = 3_600_000L;
    private static final long KEEP_MS = 30 * 60 * 1000L;
    private static final long MATCH_MS = 40 * 60 * 1000L;
    private static final String OPENF1 = "https://api.openf1.org/v1/";

    private static final Map<String, JSONObject> RAW = new HashMap<>();
    private static final Map<String, Long> STAMP = new HashMap<>();
    private static final Map<String, JSONArray> F1 = new HashMap<>();

    private weekendWeather() {
    }

    /** The same weather for sessions and coordinates the caller has already read. */
    static void load(Context context, List<raceWeekendStatus.SessionInfo> sessions,
                     double lat, double lng, Callback callback) {
        if (sessions == null || sessions.isEmpty() || Double.isNaN(lat) || Double.isNaN(lng)) {
            callback.onLoaded(new HashMap<>(), "");
            return;
        }

        request(context.getApplicationContext(), lat, lng, sessions, callback);
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

    // ---------- the model ----------

    private static void request(Context app, double lat, double lng,
                                List<raceWeekendStatus.SessionInfo> sessions, Callback callback) {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        LocalDate first = day(sessions.get(0).start);
        LocalDate last = day(sessions.get(sessions.size() - 1).end);

        if (first.isAfter(today.plusDays(FUTURE_LIMIT_DAYS))) {
            LocalDate from = first.minusDays(FUTURE_LIMIT_DAYS);
            callback.onLoaded(new HashMap<>(), app.getString(R.string.weather_available_from,
                    from.format(DateTimeFormatter.ofPattern("d MMM", Locale.getDefault()))));
            return;
        }

        boolean archive = first.isBefore(today.minusDays(PAST_LIMIT_DAYS));
        String hourly = "temperature_2m,precipitation,weather_code,wind_speed_10m,relative_humidity_2m"
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
            overlay(app, sessions, parseModel(app, known, sessions), callback);
            return;
        }

        apiQueue.get().add(new JsonObjectRequest(Request.Method.GET, key, null,
                response -> {
                    RAW.put(key, response);
                    STAMP.put(key, System.currentTimeMillis());
                    overlay(app, sessions, parseModel(app, response, sessions), callback);
                },
                error -> overlay(app, sessions, new HashMap<>(), callback)));
    }

    private static Map<String, sessionWeather> parseModel(Context app, JSONObject response,
                                                          List<raceWeekendStatus.SessionInfo> sessions) {
        Map<String, sessionWeather> result = new HashMap<>();
        JSONObject hourly = response.optJSONObject("hourly");
        JSONArray times = hourly == null ? null : hourly.optJSONArray("time");
        JSONArray temp = hourly == null ? null : hourly.optJSONArray("temperature_2m");

        if (times == null || temp == null) {
            return result;
        }

        JSONArray rain = hourly.optJSONArray("precipitation");
        JSONArray prob = hourly.optJSONArray("precipitation_probability");
        JSONArray code = hourly.optJSONArray("weather_code");
        JSONArray wind = hourly.optJSONArray("wind_speed_10m");
        JSONArray humidityArray = hourly.optJSONArray("relative_humidity_2m");

        int n = times.length();
        long[] stamps = new long[n];

        for (int i = 0; i < n; i++) {
            try {
                stamps[i] = LocalDateTime.parse(times.optString(i)).toInstant(ZoneOffset.UTC).toEpochMilli();
            } catch (RuntimeException error) {
                stamps[i] = Long.MIN_VALUE;
            }
        }

        long now = System.currentTimeMillis();

        for (raceWeekendStatus.SessionInfo s : sessions) {
            long mid = (s.start + s.end) / 2;
            int nearest = -1;
            long best = Long.MAX_VALUE;
            double mm = 0;
            boolean haveRain = false;
            double wettest = -1;
            int wettestIndex = -1;
            double chance = Double.NaN;

            for (int i = 0; i < n; i++) {
                long t = stamps[i];

                if (t == Long.MIN_VALUE) {
                    continue;
                }

                long distance = Math.abs(t - mid);

                if (distance < best) {
                    best = distance;
                    nearest = i;
                }

                // an hourly value is the sum of the PRECEDING hour: take the hours that overlap the session
                if (t > s.start && t - HOUR_MS < s.end) {
                    double value = rain == null ? Double.NaN : rain.optDouble(i, Double.NaN);

                    if (!Double.isNaN(value)) {
                        mm += value;
                        haveRain = true;

                        if (value > wettest) {
                            wettest = value;
                            wettestIndex = i;
                        }
                    }

                    double p = prob == null ? Double.NaN : prob.optDouble(i, Double.NaN);

                    if (!Double.isNaN(p) && (Double.isNaN(chance) || p > chance)) {
                        chance = p;
                    }
                }
            }

            if (nearest < 0 || best > 2 * HOUR_MS || temp.isNull(nearest)) {
                continue;
            }

            boolean future = s.start > now;
            int codeIndex = haveRain && mm >= 0.1 && wettestIndex >= 0 ? wettestIndex : nearest;
            int wmo = code == null ? 0 : code.optInt(codeIndex, 0);
            int category = weatherForecast.category(wmo);
            boolean iconRain = category == weatherForecast.RAIN || category == weatherForecast.STORM;

            String rainText;
            boolean wet;

            if (future && !Double.isNaN(chance)) {
                rainText = Math.round(chance) + "%";
                wet = chance >= 50;
            } else if (haveRain && mm >= 0.1) {
                rainText = app.getString(R.string.weather_precip_mm,
                        String.format(Locale.getDefault(), "%.1f", mm));
                wet = true;
            } else {
                rainText = app.getString(R.string.weather_dry);
                wet = false;
            }

            double windKmh = wind == null ? Double.NaN : wind.optDouble(nearest, Double.NaN);

            sessionWeather weather = new sessionWeather(
                    weatherCard.iconFor(wmo),
                    iconRain,
                    weatherCard.degrees(temp.optDouble(nearest, Double.NaN)),
                    rainText,
                    weatherCard.windText(app, windKmh),
                    wet);
            weather.condition = weatherCard.conditionText(app, wmo);
            weather.humidity = humidityArray == null ? Double.NaN : humidityArray.optDouble(nearest, Double.NaN);
            result.put(s.key, weather);
        }

        return result;
    }

    // ---------- the measurements of F1 for finished sessions ----------

    private static void overlay(Context app, List<raceWeekendStatus.SessionInfo> sessions,
                                Map<String, sessionWeather> base, Callback callback) {
        long now = System.currentTimeMillis();
        List<raceWeekendStatus.SessionInfo> past = new ArrayList<>();

        for (raceWeekendStatus.SessionInfo s : sessions) {
            if (s.end < now) {
                past.add(s);
            }
        }

        if (past.isEmpty()) {
            callback.onLoaded(base, base.isEmpty() ? "" : "Open-Meteo.com");
            return;
        }

        LocalDate from = day(sessions.get(0).start);
        LocalDate to = day(sessions.get(sessions.size() - 1).end).plusDays(1);
        // ">=" and "<=" are sent percent-encoded: the server reads them as the operator of the key
        final String url = OPENF1 + "sessions?date_start%3E=" + from + "&date_start%3C=" + to;

        JSONArray known = F1.get(url);

        if (known != null) {
            match(app, past, known, base, callback);
            return;
        }

        apiQueue.get().add(new JsonArrayRequest(Request.Method.GET, url, null,
                response -> {
                    F1.put(url, response);
                    match(app, past, response, base, callback);
                },
                error -> callback.onLoaded(base, base.isEmpty() ? "" : "Open-Meteo.com")));
    }

    private static void match(Context app, List<raceWeekendStatus.SessionInfo> past, JSONArray f1Sessions,
                              Map<String, sessionWeather> base, Callback callback) {
        final Map<String, sessionWeather> result = new HashMap<>(base);
        final List<Object[]> jobs = new ArrayList<>();

        for (raceWeekendStatus.SessionInfo s : past) {
            JSONObject found = null;
            long best = Long.MAX_VALUE;

            for (int i = 0; i < f1Sessions.length(); i++) {
                JSONObject item = f1Sessions.optJSONObject(i);
                long start = item == null ? -1 : parseTime(item.optString("date_start"));

                if (start < 0) {
                    continue;
                }

                long distance = Math.abs(start - s.start);

                if (distance < best && distance <= MATCH_MS) {
                    best = distance;
                    found = item;
                }
            }

            if (found != null) {
                jobs.add(new Object[]{s, found.optInt("session_key"),
                        parseTime(found.optString("date_start")),
                        parseTime(found.optString("date_end"))});
            }
        }

        final String note = "Open-Meteo.com";

        if (jobs.isEmpty()) {
            callback.onLoaded(base, base.isEmpty() ? "" : "Open-Meteo.com");
            return;
        }

        final AtomicInteger left = new AtomicInteger(jobs.size());

        for (Object[] job : jobs) {
            final raceWeekendStatus.SessionInfo session = (raceWeekendStatus.SessionInfo) job[0];
            final long from = (Long) job[2];
            final long to = (Long) job[3];
            final String url = OPENF1 + "weather?session_key=" + job[1];

            Runnable done = () -> {
                if (left.decrementAndGet() == 0) {
                    callback.onLoaded(result, note);
                }
            };

            JSONArray known = F1.get(url);

            if (known != null) {
                apply(app, session, known, from, to, result);
                done.run();
                continue;
            }

            apiQueue.get().add(new JsonArrayRequest(Request.Method.GET, url, null,
                    response -> {
                        F1.put(url, response);
                        apply(app, session, response, from, to, result);
                        done.run();
                    },
                    error -> done.run()));
        }
    }

    private static void apply(Context app, raceWeekendStatus.SessionInfo session, JSONArray samples,
                              long from, long to, Map<String, sessionWeather> result) {
        sessionWeather measured = measure(app, samples, from > 0 ? from : session.start,
                to > 0 ? to : session.end, result.get(session.key));

        if (measured != null) {
            result.put(session.key, measured);
        }
    }

    private static sessionWeather measure(Context app, JSONArray samples, long from, long to,
                                          sessionWeather model) {
        List<double[]> rows = new ArrayList<>();

        for (int i = 0; i < samples.length(); i++) {
            JSONObject o = samples.optJSONObject(i);
            long t = o == null ? -1 : parseTime(o.optString("date"));

            if (t < from - 5 * 60_000L || t > to + 5 * 60_000L) {
                continue;
            }

            rows.add(new double[]{t,
                    o.optDouble("air_temperature", Double.NaN),
                    o.optDouble("track_temperature", Double.NaN),
                    o.optDouble("humidity", Double.NaN),
                    o.optDouble("wind_speed", Double.NaN),
                    o.optDouble("wind_direction", Double.NaN),
                    o.optInt("rainfall") > 0 ? 1 : 0});
        }

        int n = rows.size();

        if (n == 0) {
            return null;
        }

        rows.sort((a, b) -> Double.compare(a[0], b[0]));

        double[] sum = new double[5];
        int[] count = new int[5];
        double sin = 0;
        double cos = 0;
        int rainy = 0;
        int rainyFirst = 0;
        int rainySecond = 0;

        for (int i = 0; i < n; i++) {
            double[] r = rows.get(i);

            for (int k = 0; k < 4; k++) {
                if (!Double.isNaN(r[1 + k])) {
                    sum[k] += r[1 + k];
                    count[k]++;
                }
            }

            if (!Double.isNaN(r[5])) {
                sin += Math.sin(Math.toRadians(r[5]));
                cos += Math.cos(Math.toRadians(r[5]));
                count[4]++;
            }

            if (r[6] > 0) {
                rainy++;

                if (i < n / 2) {
                    rainyFirst++;
                } else {
                    rainySecond++;
                }
            }
        }

        double air = count[0] == 0 ? Double.NaN : sum[0] / count[0];
        double track = count[1] == 0 ? Double.NaN : sum[1] / count[1];
        double humidity = count[2] == 0 ? Double.NaN : sum[2] / count[2];
        double windMs = count[3] == 0 ? Double.NaN : sum[3] / count[3];

        if (Double.isNaN(air)) {
            return null;
        }

        double share = rainy / (double) n;
        double firstShare = rainyFirst / (double) Math.max(1, n / 2);
        double secondShare = rainySecond / (double) Math.max(1, n - n / 2);

        String rainText;
        String state;

        if (share >= 0.25) {
            rainText = app.getString(R.string.weather_raining);
        } else if (share > 0) {
            rainText = app.getString(R.string.weather_rain_brief);
        } else {
            rainText = app.getString(R.string.weather_dry);
        }

        if (share >= 0.6) {
            state = app.getString(R.string.track_wet);
        } else if (firstShare >= 0.3 && secondShare < 0.15) {
            state = app.getString(R.string.track_drying);
        } else if (share > 0) {
            state = app.getString(R.string.track_showers);
        } else {
            state = app.getString(R.string.track_dry);
        }

        String wind = weatherCard.windText(app, windMs * 3.6);
        String windFull = wind;

        if (count[4] > 0) {
            double degrees = (Math.toDegrees(Math.atan2(sin, cos)) + 360) % 360;
            String[] names = app.getResources().getStringArray(R.array.wind_directions);
            windFull = wind + ", " + names[(int) Math.round(degrees / 45.0) % 8];
        }

        boolean rainIcon = share >= 0.25;
        int icon = rainIcon ? R.drawable.ic_weather_rain
                : model != null && !model.iconRain ? model.icon : R.drawable.ic_weather_cloud;

        sessionWeather measured = new sessionWeather(icon, rainIcon, weatherCard.degrees(air), rainText, wind,
                share >= 0.25, true, air, track, humidity, windFull, state);
        measured.condition = rainIcon ? app.getString(R.string.weather_rain_condition)
                : model != null ? model.condition : "";
        return measured;
    }

    // ---------- helpers ----------

    private static long parseTime(String value) {
        try {
            return OffsetDateTime.parse(value).toInstant().toEpochMilli();
        } catch (RuntimeException error) {
            return -1;
        }
    }

    private static LocalDate day(long millis) {
        return Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate();
    }
}