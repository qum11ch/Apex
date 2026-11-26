package com.example.f1app;

import android.content.Context;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/** Open-Meteo forecast for a circuit: one request, a disk cache and a stale fallback. */
final class weatherForecast {

    static final int HORIZON_DAYS = 16;

    static final int CLEAR = 0;
    static final int PARTLY = 1;
    static final int CLOUD = 2;
    static final int FOG = 3;
    static final int RAIN = 4;
    static final int SNOW = 5;
    static final int STORM = 6;

    private static final long HOUR_MS = 3_600_000L;
    private static final long FORECAST_FRESH_MS = 3 * HOUR_MS;
    private static final long CURRENT_FRESH_MS = 30 * 60_000L;
    private static final long USABLE_MS = 24 * HOUR_MS;

    private static final String HOURLY = "temperature_2m,apparent_temperature,"
            + "precipitation_probability,relative_humidity_2m,wind_speed_10m,weather_code";
    private static final String CURRENT = "temperature_2m,apparent_temperature,"
            + "relative_humidity_2m,wind_speed_10m,weather_code";

    static final class Hour {
        final long time;
        final double temp;
        final double feels;
        final double wind;
        final int rain;
        final int humidity;
        final int code;

        Hour(long time, double temp, double feels, double wind, int rain, int humidity, int code) {
            this.time = time;
            this.temp = temp;
            this.feels = feels;
            this.wind = wind;
            this.rain = rain;
            this.humidity = humidity;
            this.code = code;
        }
    }

    static final class Data {
        final Map<Long, Hour> hours = new HashMap<>();
        Hour current;
        long fetchedAt;
        boolean stale;

        Hour at(long millis) {
            return hours.get(hourKey(millis));
        }
    }

    interface Callback {
        /** The data, or null when there is neither a fresh answer nor a usable saved one. */
        void onResult(Data data);
    }

    private weatherForecast() {
    }

    // ---------- pure helpers ----------

    static LocalDate horizonEnd(LocalDate todayUtc) {
        return todayUtc.plusDays(HORIZON_DAYS - 1);
    }

    static long hourKey(long millis) {
        return Math.round(millis / (double) HOUR_MS) * HOUR_MS;
    }

    /** WMO weather code to one of the categories above. */
    static int category(int code) {
        if (code == 0) return CLEAR;
        if (code == 1 || code == 2) return PARTLY;
        if (code == 3) return CLOUD;
        if (code == 45 || code == 48) return FOG;
        if (code >= 51 && code <= 67) return RAIN;
        if ((code >= 71 && code <= 77) || code == 85 || code == 86) return SNOW;
        if (code >= 80 && code <= 82) return RAIN;
        if (code >= 95) return STORM;
        return CLOUD;
    }

    static String forecastUrl(double lat, double lng, LocalDate from, LocalDate to) {
        return String.format(Locale.US,
                "https://api.open-meteo.com/v1/forecast?latitude=%.2f&longitude=%.2f"
                        + "&hourly=%s&current=%s&timezone=GMT&start_date=%s&end_date=%s",
                lat, lng, HOURLY, CURRENT, from, to);
    }

    static String currentUrl(double lat, double lng) {
        return String.format(Locale.US,
                "https://api.open-meteo.com/v1/forecast?latitude=%.2f&longitude=%.2f"
                        + "&current=%s&timezone=GMT",
                lat, lng, CURRENT);
    }

    // ---------- loading ----------

    /** from == null: only the conditions now; otherwise the hours from..to (inside the horizon). */
    static void load(double lat, double lng, LocalDate from, LocalDate to, Callback callback) {
        Context context = ApexApp.context();

        if (context == null) {
            callback.onResult(null);
            return;
        }

        final boolean forecast = from != null && to != null;
        final String url = forecast ? forecastUrl(lat, lng, from, to) : currentUrl(lat, lng);
        final long ttl = forecast ? FORECAST_FRESH_MS : CURRENT_FRESH_MS;
        final long now = System.currentTimeMillis();
        final Data cached = read(context, url);

        if (cached != null && now - cached.fetchedAt < ttl) {
            callback.onResult(cached);
            return;
        }

        final Runnable fallback = () -> callback.onResult(usable(cached, now));

        if (!networkNotice.isOnline(context)) {
            fallback.run();
            return;
        }

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    Data data = parse(response);

                    if (data == null) {
                        fallback.run();
                        return;
                    }

                    data.fetchedAt = System.currentTimeMillis();
                    write(context, url, response, data.fetchedAt);
                    callback.onResult(data);
                },
                error -> fallback.run());

        request.setRetryPolicy(new DefaultRetryPolicy(8000, 1, 1f));
        request.setShouldCache(false);
        apiQueue.get().add(request);
    }

    private static Data usable(Data cached, long now) {
        if (cached == null || now - cached.fetchedAt > USABLE_MS) {
            return null;
        }

        cached.stale = true;
        return cached;
    }

    // ---------- parsing ----------

    static Data parse(JSONObject response) {
        Data data = new Data();

        try {
            JSONObject hourly = response.optJSONObject("hourly");

            if (hourly != null) {
                JSONArray times = hourly.optJSONArray("time");
                JSONArray temp = hourly.optJSONArray("temperature_2m");
                JSONArray feels = hourly.optJSONArray("apparent_temperature");
                JSONArray rain = hourly.optJSONArray("precipitation_probability");
                JSONArray humidity = hourly.optJSONArray("relative_humidity_2m");
                JSONArray wind = hourly.optJSONArray("wind_speed_10m");
                JSONArray code = hourly.optJSONArray("weather_code");

                for (int i = 0; times != null && i < times.length(); i++) {
                    long time = parseTime(times.getString(i));

                    data.hours.put(time, new Hour(time, number(temp, i), number(feels, i),
                            number(wind, i), whole(rain, i), whole(humidity, i), whole(code, i)));
                }
            }

            JSONObject current = response.optJSONObject("current");

            if (current != null && current.has("time")) {
                data.current = new Hour(parseTime(current.getString("time")),
                        current.optDouble("temperature_2m", Double.NaN),
                        current.optDouble("apparent_temperature", Double.NaN),
                        current.optDouble("wind_speed_10m", Double.NaN),
                        -1,
                        current.optInt("relative_humidity_2m", -1),
                        current.optInt("weather_code", -1));
            }
        } catch (JSONException | RuntimeException error) {
            return null;
        }

        return data.hours.isEmpty() && data.current == null ? null : data;
    }

    private static long parseTime(String value) {
        return LocalDateTime.parse(value).toInstant(ZoneOffset.UTC).toEpochMilli();
    }

    private static double number(JSONArray array, int index) {
        return array == null ? Double.NaN : array.optDouble(index, Double.NaN);
    }

    private static int whole(JSONArray array, int index) {
        return array == null || array.isNull(index) ? -1 : array.optInt(index, -1);
    }

    // ---------- disk cache ----------

    private static File dir(Context context) {
        File dir = new File(context.getFilesDir(), "weather_json");

        if (!dir.exists()) {
            dir.mkdirs();
        }

        return dir;
    }

    private static String fileName(String url) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-1").digest(url.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder();

            for (byte b : hash) {
                builder.append(String.format("%02x", b));
            }

            return builder.toString();
        } catch (Exception error) {
            return Integer.toHexString(url.hashCode());
        }
    }

    private static void write(Context context, String url, JSONObject response, long fetchedAt) {
        try {
            JSONObject wrapper = new JSONObject();
            wrapper.put("t", fetchedAt);
            wrapper.put("d", response);

            File target = new File(dir(context), fileName(url));
            File temp = new File(dir(context), target.getName() + ".tmp");

            try (OutputStream out = new FileOutputStream(temp)) {
                out.write(wrapper.toString().getBytes(StandardCharsets.UTF_8));
            }

            if (!temp.renameTo(target)) {
                target.delete();
                temp.renameTo(target);
            }
        } catch (IOException | JSONException | RuntimeException ignored) {
        }
    }

    private static Data read(Context context, String url) {
        File file = new File(dir(context), fileName(url));

        if (!file.isFile()) {
            return null;
        }

        try (InputStream in = new FileInputStream(file)) {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            byte[] chunk = new byte[8192];
            int count;

            while ((count = in.read(chunk)) > 0) {
                buffer.write(chunk, 0, count);
            }

            JSONObject wrapper = new JSONObject(buffer.toString("UTF-8"));
            Data data = parse(wrapper.getJSONObject("d"));

            if (data != null) {
                data.fetchedAt = wrapper.optLong("t", 0L);
            }

            return data;
        } catch (IOException | JSONException | RuntimeException error) {
            return null;
        }
    }
}