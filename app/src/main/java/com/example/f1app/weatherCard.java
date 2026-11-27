package com.example.f1app;

import android.content.Context;

import androidx.annotation.NonNull;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Weather of a race weekend: only data, no views. */
final class weatherCard {

    private static final String DASH = "\u2013";

    static final class Race {
        String name = "";
        String circuitId = "";
        boolean canceled;
        long raceStart;
        List<raceWeekendStatus.SessionInfo> sessions = new ArrayList<>();
    }

    static final class Place {
        double lat = Double.NaN;
        double lng = Double.NaN;
    }

    static final class Result {
        Race race;
        weatherForecast.Data data;
        LocalDate availableFrom;
        final Map<String, weatherForecast.Hour> sessions = new HashMap<>();
    }

    interface Listener {
        void onResult(Result result);
    }

    private int token = 0;

    void cancel() {
        token++;
    }

    void fetchRace(String season, String raceName, Listener listener) {
        final int mine = ++token;

        FirebaseDatabase.getInstance().getReference().child("schedule").child("season")
                .child(season).child(raceName)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (mine != token) {
                            return;
                        }

                        Race race = parse(snapshot);

                        if (race != null && !race.canceled) {
                            proceed(race, mine, listener);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                    }
                });
    }

    private static Race parse(DataSnapshot s) {
        Race race = new Race();
        race.name = s.getKey() == null ? "" : s.getKey();
        race.circuitId = string(s.child("Circuit").child("circuitId"));
        race.canceled = raceWeekendStatus.isCanceled(s);
        race.sessions = raceWeekendStatus.sessions(s);

        for (raceWeekendStatus.SessionInfo session : race.sessions) {
            if ("race_event".equals(session.key)) {
                race.raceStart = session.start;
            }
        }

        return race.raceStart > 0 && !race.circuitId.isEmpty() && !race.sessions.isEmpty() ? race : null;
    }

    private static String string(DataSnapshot snapshot) {
        String value = snapshot.getValue(String.class);
        return value == null ? "" : value;
    }

    private void proceed(Race race, int mine, Listener listener) {
        FirebaseDatabase.getInstance().getReference().child("circuits").child(race.circuitId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (mine != token) {
                            return;
                        }

                        Place place = new Place();
                        Object lat = snapshot.child("lat").getValue();
                        Object lng = snapshot.child("lng").getValue();

                        if (lat instanceof Number && lng instanceof Number) {
                            place.lat = ((Number) lat).doubleValue();
                            place.lng = ((Number) lng).doubleValue();
                        }

                        loadWeather(race, place, mine, listener);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        if (mine == token) {
                            finish(race, null, null, listener);
                        }
                    }
                });
    }

    private void loadWeather(Race race, Place place, int mine, Listener listener) {
        if (Double.isNaN(place.lat)) {
            finish(race, null, null, listener);
            return;
        }

        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        LocalDate horizon = weatherForecast.horizonEnd(today);
        LocalDate firstDay = day(race.sessions.get(0).start);
        LocalDate lastDay = day(race.raceStart);

        LocalDate from = null;
        LocalDate to = null;

        if (!firstDay.isAfter(horizon)) {
            from = firstDay;
            to = lastDay.isAfter(horizon) ? horizon : lastDay;
        }

        final LocalDate availableFrom = from == null
                ? firstDay.minusDays(weatherForecast.HORIZON_DAYS - 1) : null;

        weatherForecast.load(place.lat, place.lng, from, to, data -> {
            if (mine == token) {
                finish(race, data, availableFrom, listener);
            }
        });
    }

    private static void finish(Race race, weatherForecast.Data data, LocalDate availableFrom,
                               Listener listener) {
        Result result = new Result();
        result.race = race;
        result.data = data;
        result.availableFrom = availableFrom;

        if (data != null) {
            for (raceWeekendStatus.SessionInfo session : race.sessions) {
                weatherForecast.Hour hour = data.at(session.start);

                if (hour != null) {
                    result.sessions.put(session.key, hour);
                }
            }
        }

        listener.onResult(result);
    }

    private static LocalDate day(long millis) {
        return Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate();
    }

    // ---------- helpers for the screens ----------

    /** "27\u00B0, rain 0%, 4 m/s" */
    static String summary(Context context, weatherForecast.Hour hour) {
        List<String> parts = new ArrayList<>();
        parts.add(degrees(hour.temp));

        if (hour.rain >= 0) {
            parts.add(context.getString(R.string.weather_rain_short, hour.rain));
        }

        parts.add(windText(context, hour.wind));
        return String.join(", ", parts);
    }

    /** A short line about the source and its age; may be empty. */
    static String note(Context context, Result result) {
        List<String> parts = new ArrayList<>();

        if (result == null || result.data == null) {
            if (result != null && result.availableFrom != null) {
                parts.add(context.getString(R.string.weather_available_from, date(result.availableFrom)));
            } else {
                parts.add(context.getString(R.string.weather_unavailable));
            }

            return String.join(", ", parts);
        }

        if (result.availableFrom != null) {
            parts.add(context.getString(R.string.weather_available_from, date(result.availableFrom)));
        }

        if (result.data.stale) {
            long hours = Math.max(1, (System.currentTimeMillis() - result.data.fetchedAt) / 3_600_000L);
            parts.add(context.getString(R.string.weather_updated_ago, hours));
        }

        parts.add("Open-Meteo.com");
        return String.join(", ", parts);
    }

    static int iconFor(int code) {
        switch (weatherForecast.category(code)) {
            case weatherForecast.CLEAR: return R.drawable.ic_weather_clear;
            case weatherForecast.PARTLY: return R.drawable.ic_weather_partly;
            case weatherForecast.FOG: return R.drawable.ic_weather_fog;
            case weatherForecast.RAIN: return R.drawable.ic_weather_rain;
            case weatherForecast.SNOW: return R.drawable.ic_weather_snow;
            case weatherForecast.STORM: return R.drawable.ic_weather_storm;
            default: return R.drawable.ic_weather_cloud;
        }
    }

    static String degrees(double value) {
        return Double.isNaN(value) ? DASH : String.format(Locale.getDefault(), "%.0f\u00B0", value);
    }

    static String windText(Context context, double kmh) {
        if (Double.isNaN(kmh)) {
            return DASH;
        }

        boolean metres = "ru".equals(Locale.getDefault().getLanguage());
        double value = metres ? kmh / 3.6 : kmh;

        return context.getString(R.string.weather_wind, String.format(Locale.getDefault(), "%.0f", value));
    }

    static String conditionText(Context context, int code) {
        int res;

        switch (weatherForecast.category(code)) {
            case weatherForecast.CLEAR: res = R.string.weather_clear; break;
            case weatherForecast.PARTLY: res = R.string.weather_partly; break;
            case weatherForecast.FOG: res = R.string.weather_fog; break;
            case weatherForecast.RAIN: res = R.string.weather_rain_condition; break;
            case weatherForecast.SNOW: res = R.string.weather_snow; break;
            case weatherForecast.STORM: res = R.string.weather_storm; break;
            default: res = R.string.weather_cloud; break;
        }

        return context.getString(res);
    }

    private static String date(LocalDate date) {
        return date.format(DateTimeFormatter.ofPattern("d MMM", Locale.getDefault()));
    }
}