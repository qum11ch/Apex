package com.example.f1app;

import static com.example.f1app.MainActivity.getStringByName;

import android.content.Context;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** The weather card of the next Grand Prix: compact on the home screen, per session on the race page. */
final class weatherCard {

    private static final long RACE_LENGTH_MS = 3 * 3_600_000L;
    private static final int RAIN_WARNING = 50;
    private static final String DASH = "\u2013";

    private static final class Session {
        final String key;
        final long start;

        Session(String key, long start) {
            this.key = key;
            this.start = start;
        }
    }

    private static final class Race {
        String name = "";
        String circuitId = "";
        boolean canceled;
        long raceStart;
        final List<Session> sessions = new ArrayList<>();
    }

    private static final class Place {
        double lat = Double.NaN;
        double lng = Double.NaN;
        String location = "";
    }

    private final ViewGroup container;
    private final boolean compact;
    private int token = 0;

    weatherCard(ViewGroup container, boolean compact) {
        this.container = container;
        this.compact = compact;
        container.setVisibility(View.GONE);
    }

    // ---------- finding the race ----------

    /** The first race that is not over and not cancelled. */
    void showNext(String season) {
        final int mine = ++token;

        FirebaseDatabase.getInstance().getReference().child("schedule").child("season").child(season)
                .orderByChild("round").addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (mine != token) {
                            return;
                        }

                        long now = System.currentTimeMillis();

                        for (DataSnapshot item : snapshot.getChildren()) {
                            Race race = parse(item);

                            if (race != null && !race.canceled && race.raceStart + RACE_LENGTH_MS > now) {
                                proceed(race, mine);
                                return;
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                    }
                });
    }

    void showRace(String season, String raceName) {
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
                            proceed(race, mine);
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
        race.canceled = s.child("Canceled").getValue() != null;

        String sprintDate = s.child("Sprint").child("sprintRaceDate").getValue(String.class);
        boolean sprint = sprintDate != null && !"N/A".equals(sprintDate);

        add(race, "first_practice_event", s.child("FirstPractice").child("firstPracticeDate"),
                s.child("FirstPractice").child("firstPracticeTime"));

        if (sprint) {
            add(race, "sprint_quali_event", s.child("SprintQualifying").child("sprintQualiDate"),
                    s.child("SprintQualifying").child("sprintQualiTime"));
            add(race, "sprint_event", s.child("Sprint").child("sprintRaceDate"),
                    s.child("Sprint").child("sprintRaceTime"));
        } else {
            add(race, "second_practice_event", s.child("SecondPractice").child("secondPracticeDate"),
                    s.child("SecondPractice").child("secondPracticeTime"));
            add(race, "third_practice_event", s.child("ThirdPractice").child("thirdPracticeDate"),
                    s.child("ThirdPractice").child("thirdPracticeTime"));
        }

        add(race, "quali_event", s.child("Qualifying").child("raceQualiDate"),
                s.child("Qualifying").child("raceQualiTime"));
        add(race, "race_event", s.child("raceDate"), s.child("raceTime"));

        for (Session session : race.sessions) {
            if ("race_event".equals(session.key)) {
                race.raceStart = session.start;
            }
        }

        return race.raceStart > 0 && !race.circuitId.isEmpty() && !race.sessions.isEmpty() ? race : null;
    }

    private static void add(Race race, String key, DataSnapshot date, DataSnapshot time) {
        long start = start(date.getValue(String.class), time.getValue(String.class));

        if (start > 0) {
            race.sessions.add(new Session(key, start));
        }
    }

    private static long start(String date, String time) {
        if (date == null || time == null) {
            return -1;
        }

        try {
            LocalDate day = LocalDate.parse(date.trim(), DateTimeFormatter.ofPattern("yyyy-M-d"));
            LocalTime clock = LocalTime.parse(time.trim().replace("Z", ""));

            return LocalDateTime.of(day, clock).toInstant(ZoneOffset.UTC).toEpochMilli();
        } catch (RuntimeException error) {
            return -1;
        }
    }

    private static String string(DataSnapshot snapshot) {
        String value = snapshot.getValue(String.class);
        return value == null ? "" : value;
    }

    // ---------- the circuit and the weather ----------

    private void proceed(Race race, int mine) {
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

                        place.location = string(snapshot.child("location"));

                        if (!Double.isNaN(place.lat)) {
                            loadWeather(race, place, mine);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                    }
                });
    }

    private void loadWeather(Race race, Place place, int mine) {
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

        final LocalDate availableFrom = firstDay.minusDays(weatherForecast.HORIZON_DAYS - 1);
        final boolean forecastRequested = from != null;

        weatherForecast.load(place.lat, place.lng, from, to, data -> {
            if (mine != token) {
                return;
            }

            render(race, place, data, forecastRequested ? null : availableFrom);
        });
    }

    private static LocalDate day(long millis) {
        return Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate();
    }

    // ---------- drawing ----------

    private void render(Race race, Place place, weatherForecast.Data data, LocalDate availableFrom) {
        container.removeAllViews();
        container.setVisibility(View.VISIBLE);

        LinearLayout card = card();
        container.addView(card);

        if (data == null) {
            card.addView(header(compact ? R.string.weather_title_now : R.string.weather_title_weekend,
                    place.location));
            card.addView(message(R.string.weather_unavailable));
            return;
        }

        if (compact) {
            renderCompact(card, race, place, data, availableFrom);
        } else {
            renderWeekend(card, race, place, data, availableFrom);
        }
    }

    private void renderCompact(LinearLayout card, Race race, Place place, weatherForecast.Data data,
                               LocalDate availableFrom) {
        weatherForecast.Hour hero = data.at(race.raceStart);
        boolean forecast = hero != null;

        if (hero == null) {
            hero = data.current;
        }

        if (hero == null) {
            card.addView(header(R.string.weather_title_now, place.location));
            card.addView(message(R.string.weather_unavailable));
            return;
        }

        card.addView(header(forecast ? R.string.weather_title_race : R.string.weather_title_now,
                place.location));

        LinearLayout body = new LinearLayout(context());
        body.setOrientation(LinearLayout.HORIZONTAL);
        body.setGravity(Gravity.CENTER_VERTICAL);

        ImageView icon = icon(hero.code, 48);
        body.addView(icon, new LinearLayout.LayoutParams(dp(48), dp(48)));

        LinearLayout main = new LinearLayout(context());
        main.setOrientation(LinearLayout.VERTICAL);
        main.setPadding(dp(12), 0, dp(8), 0);

        TextView temp = new TextView(context());
        temp.setText(degrees(hero.temp));
        temp.setTextAppearance(R.style.TextStyle_Audiowide_ForCountdown);
        temp.setTextSize(30);
        temp.setTextColor(color(R.color.text_color_main));
        main.addView(temp);

        TextView condition = line(conditionText(hero.code)
                        + (Double.isNaN(hero.feels) ? "" : " \u00B7 "
                        + context().getString(R.string.weather_feels, degrees(hero.feels))), 13,
                R.color.text_color_secondary);
        main.addView(condition);

        body.addView(main, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        LinearLayout stats = new LinearLayout(context());
        stats.setOrientation(LinearLayout.VERTICAL);
        stats.setGravity(Gravity.END);

        if (hero.rain >= 0) {
            TextView rain = line(context().getString(R.string.weather_rain, hero.rain), 13,
                    hero.rain >= RAIN_WARNING ? R.color.red : R.color.text_color_main);

            if (hero.rain >= RAIN_WARNING) {
                rain.setTypeface(rain.getTypeface(), Typeface.BOLD);
            }

            stats.addView(rain);
        }

        stats.addView(line(wind(hero.wind), 13, R.color.text_color_main));

        if (hero.humidity >= 0) {
            stats.addView(line(context().getString(R.string.weather_humidity, hero.humidity), 13,
                    R.color.text_color_main));
        }

        body.addView(stats);

        LinearLayout.LayoutParams bodyParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        bodyParams.topMargin = dp(10);
        card.addView(body, bodyParams);

        card.addView(footnote(data, forecast ? null : availableFrom));
    }

    private void renderWeekend(LinearLayout card, Race race, Place place, weatherForecast.Data data,
                               LocalDate availableFrom) {
        card.addView(header(R.string.weather_title_weekend, place.location));

        boolean any = false;

        for (Session session : race.sessions) {
            any |= data.at(session.start) != null;
        }

        if (!any) {
            TextView text = availableFrom != null
                    ? line(context().getString(R.string.weather_available_from, date(availableFrom)),
                    14, R.color.text_color_main)
                    : line(context().getString(R.string.weather_unavailable), 14, R.color.text_color_main);

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            params.topMargin = dp(10);
            card.addView(text, params);
            card.addView(footnote(data, null));
            return;
        }

        for (Session session : race.sessions) {
            card.addView(sessionRow(session, data.at(session.start)));
        }

        card.addView(footnote(data, null));
    }

    private View sessionRow(Session session, weatherForecast.Hour hour) {
        LinearLayout row = new LinearLayout(context());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(8), 0, dp(8));

        LinearLayout names = new LinearLayout(context());
        names.setOrientation(LinearLayout.VERTICAL);
        names.addView(line(sessionName(session.key), 14, R.color.text_color_main));
        names.addView(line(time(session.start), 12, R.color.text_color_secondary));
        row.addView(names, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        if (hour == null) {
            TextView dash = line(DASH, 14, R.color.text_color_secondary);
            dash.setGravity(Gravity.END);
            row.addView(dash);
            return row;
        }

        row.addView(icon(hour.code, 24), new LinearLayout.LayoutParams(dp(24), dp(24)));

        TextView temp = line(degrees(hour.temp), 14, R.color.text_color_main);
        temp.setGravity(Gravity.END);
        temp.setTypeface(temp.getTypeface(), Typeface.BOLD);
        row.addView(temp, new LinearLayout.LayoutParams(dp(44), ViewGroup.LayoutParams.WRAP_CONTENT));

        boolean wet = hour.rain >= RAIN_WARNING;
        TextView rain = line(hour.rain >= 0 ? hour.rain + "%" : DASH, 13,
                wet ? R.color.red : R.color.text_color_secondary);
        rain.setGravity(Gravity.END);
        row.addView(rain, new LinearLayout.LayoutParams(dp(44), ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView wind = line(wind(hour.wind), 13, R.color.text_color_secondary);
        wind.setGravity(Gravity.END);
        row.addView(wind, new LinearLayout.LayoutParams(dp(64), ViewGroup.LayoutParams.WRAP_CONTENT));

        return row;
    }

    private View footnote(weatherForecast.Data data, LocalDate availableFrom) {
        List<String> parts = new ArrayList<>();

        if (availableFrom != null) {
            parts.add(context().getString(R.string.weather_available_from, date(availableFrom)));
        }

        if (data.stale) {
            long hours = Math.max(1, (System.currentTimeMillis() - data.fetchedAt) / 3_600_000L);
            parts.add(context().getString(R.string.weather_updated_ago, hours));
        }

        parts.add("Open-Meteo.com");

        TextView view = line(String.join(" \u00B7 ", parts), 11, R.color.text_color_secondary);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.topMargin = dp(8);
        view.setLayoutParams(params);

        return view;
    }

    private View header(int title, String location) {
        LinearLayout row = new LinearLayout(context());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);

        TextView caption = line(context().getString(title), 12, R.color.text_color_secondary);
        caption.setAllCaps(true);
        caption.setLetterSpacing(0.06f);
        caption.setTypeface(caption.getTypeface(), Typeface.BOLD);
        row.addView(caption, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        if (!location.isEmpty()) {
            TextView place = line(location, 12, R.color.text_color_secondary);
            place.setGravity(Gravity.END);
            row.addView(place);
        }

        return row;
    }

    private View message(int text) {
        TextView view = line(context().getString(text), 14, R.color.text_color_main);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.topMargin = dp(10);
        view.setLayoutParams(params);

        return view;
    }

    private LinearLayout card() {
        LinearLayout card = new LinearLayout(context());
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundResource(R.drawable.bg_results_card);
        card.setPadding(dp(14), dp(12), dp(14), dp(12));
        return card;
    }

    private ImageView icon(int code, int sizeDp) {
        ImageView view = new ImageView(context());
        view.setImageResource(iconFor(weatherForecast.category(code)));
        view.setColorFilter(color(R.color.text_color_main));
        view.setContentDescription(conditionText(code));
        return view;
    }

    private TextView line(String value, int size, int colorRes) {
        TextView view = new TextView(context());
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color(colorRes));
        return view;
    }

    // ---------- texts ----------

    private static int iconFor(int category) {
        switch (category) {
            case weatherForecast.CLEAR: return R.drawable.ic_weather_clear;
            case weatherForecast.PARTLY: return R.drawable.ic_weather_partly;
            case weatherForecast.FOG: return R.drawable.ic_weather_fog;
            case weatherForecast.RAIN: return R.drawable.ic_weather_rain;
            case weatherForecast.SNOW: return R.drawable.ic_weather_snow;
            case weatherForecast.STORM: return R.drawable.ic_weather_storm;
            default: return R.drawable.ic_weather_cloud;
        }
    }

    private String conditionText(int code) {
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

        return context().getString(res);
    }

    private String sessionName(String key) {
        int id = getStringByName(key + "_text");
        return id != 0 ? context().getString(id) : key;
    }

    private String degrees(double value) {
        return Double.isNaN(value) ? DASH : String.format(Locale.getDefault(), "%.0f\u00B0", value);
    }

    private String wind(double kmh) {
        if (Double.isNaN(kmh)) {
            return DASH;
        }

        boolean metres = "ru".equals(Locale.getDefault().getLanguage());
        double value = metres ? kmh / 3.6 : kmh;

        return context().getString(R.string.weather_wind, String.format(Locale.getDefault(), "%.0f", value));
    }

    private String time(long millis) {
        ZonedDateTime zoned = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault());
        return zoned.format(DateTimeFormatter.ofPattern("EEE HH:mm", Locale.getDefault()));
    }

    private String date(LocalDate date) {
        return date.format(DateTimeFormatter.ofPattern("d MMM", Locale.getDefault()));
    }

    private Context context() {
        return container.getContext();
    }

    private int color(int res) {
        return ContextCompat.getColor(context(), res);
    }

    private int dp(float value) {
        return Math.round(value * container.getResources().getDisplayMetrics().density);
    }
}