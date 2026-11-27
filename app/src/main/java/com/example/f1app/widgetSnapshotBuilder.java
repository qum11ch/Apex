package com.example.f1app;

import android.content.Context;

import androidx.annotation.NonNull;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Fills the snapshot of the widgets from what the app already knows how to load. Main thread only.
 * Every part is refreshed only when it is old enough, and a part that fails keeps its previous value.
 */
final class widgetSnapshotBuilder {

    private static final int DRIVERS = 5;
    private static final int TEAMS = 3;
    private static final long HOUR_MS = 3_600_000L;
    private static final long STANDINGS_TTL_MS = 3 * HOUR_MS;
    private static final long WEATHER_NEAR_TTL_MS = 3 * HOUR_MS;
    private static final long WEATHER_FAR_TTL_MS = 6 * HOUR_MS;
    private static final long NEAR_MS = 3 * 24 * HOUR_MS;

    private static final List<Runnable> WAITING = new ArrayList<>();
    private static boolean running = false;

    private widgetSnapshotBuilder() {
    }

    private static final class Result {
        Map<String, sessionWeather> weather;   // null: failed or not asked
        List<widgetSnapshot.Standing> drivers; // null: failed or not asked
        List<widgetSnapshot.Standing> teams;
    }

    /** season may be null: then it is read the way the main screen reads it. */
    static void refresh(Context context, String season, boolean force, Runnable done) {
        final Context app = context.getApplicationContext();

        if (done != null) {
            WAITING.add(done);
        }

        if (running) {
            return;
        }

        running = true;

        if (season != null && !season.trim().isEmpty()) {
            load(app, season.trim(), force);
            return;
        }

        FirebaseDatabase.getInstance().getReference().child("status").child("season")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        String fromStatus = snapshot.getValue(String.class);

                        if (fromStatus == null || fromStatus.trim().isEmpty()) {
                            finish();
                            return;
                        }

                        seasonResolver.resolve(fromStatus, resolved -> load(app, resolved, force));
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        finish();
                    }
                });
    }

    private static void finish() {
        running = false;

        List<Runnable> callbacks = new ArrayList<>(WAITING);
        WAITING.clear();

        for (Runnable callback : callbacks) {
            callback.run();
        }
    }

    private static void load(Context app, String season, boolean force) {
        final widgetSnapshot old = widgetSnapshotStore.load(app);

        FirebaseDatabase.getInstance().getReference().child("schedule").child("season").child(season)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        build(app, season, snapshot, old, force);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        finish();
                    }
                });
    }

    private static void build(Context app, String season, DataSnapshot schedule,
                              widgetSnapshot old, boolean force) {
        final long now = System.currentTimeMillis();

        DataSnapshot pick = null;
        List<raceWeekendStatus.SessionInfo> pickSessions = null;
        long pickStart = Long.MAX_VALUE;

        for (DataSnapshot race : schedule.getChildren()) {
            raceWeekendStatus.State state = raceWeekendStatus.resolve(race, now);

            if (raceWeekendStatus.isCanceled(race)
                    || state == raceWeekendStatus.State.CONCLUDED
                    || state == raceWeekendStatus.State.CANCELED) {
                continue;
            }

            List<raceWeekendStatus.SessionInfo> list = raceWeekendStatus.sessions(race);

            if (list.isEmpty()) {
                continue;
            }

            long raceStart = list.get(list.size() - 1).start;

            if (raceStart < pickStart) {
                pickStart = raceStart;
                pick = race;
                pickSessions = list;
            }
        }

        final widgetSnapshot next = new widgetSnapshot();
        next.season = season;
        next.updatedAt = now;

        final boolean sameSeason = old != null && season.equals(old.season);
        final String raceName = pick == null ? "" : pick.getKey();
        final boolean sameRace = sameSeason && old.raceName.equals(raceName);

        if (pick != null) {
            next.raceName = raceName;
            next.circuitId = pick.child("Circuit").child("circuitId").getValue(String.class) == null
                    ? "" : pick.child("Circuit").child("circuitId").getValue(String.class);
        }

        final boolean askWeather = pick != null && !next.circuitId.isEmpty() && (force || !sameRace
                || old.weatherAt == 0
                || now - old.weatherAt >= (pickStart - now < NEAR_MS
                ? WEATHER_NEAR_TTL_MS : WEATHER_FAR_TTL_MS));
        final boolean askStandings = force || !sameSeason || old.standingsAt == 0
                || old.drivers.isEmpty() || old.teams.isEmpty()
                || now - old.standingsAt >= STANDINGS_TTL_MS;

        final Result result = new Result();
        final List<raceWeekendStatus.SessionInfo> sessions = pickSessions;
        final AtomicInteger left = new AtomicInteger(3);

        Runnable join = () -> {
            if (left.decrementAndGet() == 0) {
                assemble(app, next, old, sameSeason, sameRace, sessions, result, now, askWeather,
                        askStandings);
            }
        };

        if (askWeather) {
            loadWeather(app, sessions, next.circuitId, result, join);
        } else {
            join.run();
        }

        if (askStandings) {
            int year = year(season);

            jolpicaSeasons.driverStandings(year, response -> {
                result.drivers = parse(response, false, DRIVERS);
                join.run();
            });
            jolpicaSeasons.constructorStandings(year, response -> {
                result.teams = parse(response, true, TEAMS);
                join.run();
            });
        } else {
            join.run();
            join.run();
        }
    }

    private static void loadWeather(Context app, List<raceWeekendStatus.SessionInfo> sessions,
                                    String circuitId, Result result, Runnable join) {
        FirebaseDatabase.getInstance().getReference().child("circuits").child(circuitId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        Object lat = snapshot.child("lat").getValue();
                        Object lng = snapshot.child("lng").getValue();

                        if (!(lat instanceof Number) || !(lng instanceof Number)) {
                            join.run();
                            return;
                        }

                        weekendWeather.load(app, sessions, ((Number) lat).doubleValue(),
                                ((Number) lng).doubleValue(), (map, note) -> {
                                    result.weather = map;
                                    join.run();
                                });
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        join.run();
                    }
                });
    }

    private static void assemble(Context app, widgetSnapshot next, widgetSnapshot old,
                                 boolean sameSeason, boolean sameRace,
                                 List<raceWeekendStatus.SessionInfo> sessions, Result result,
                                 long now, boolean askWeather, boolean askStandings) {
        // sessions of the race and their weather (a failed part keeps the old one)
        if (sessions != null) {
            for (raceWeekendStatus.SessionInfo s : sessions) {
                widgetSnapshot.Session x = new widgetSnapshot.Session();
                x.key = s.key;
                x.start = s.start;
                x.end = s.end;

                sessionWeather w = result.weather == null ? null : result.weather.get(s.key);

                if (w != null) {
                    x.icon = iconName(app, w.icon);
                    x.temp = w.temp;
                    x.rain = w.rain;
                    x.wet = w.wet;
                    x.measured = w.measured;
                } else if (sameRace) {
                    widgetSnapshot.Session before = find(old, s.key);

                    if (before != null) {
                        x.icon = before.icon;
                        x.temp = before.temp;
                        x.rain = before.rain;
                        x.wet = before.wet;
                        x.measured = before.measured;
                    }
                }

                next.sessions.add(x);
            }
        }

        boolean weatherFresh = result.weather != null && !result.weather.isEmpty();
        next.weatherAt = weatherFresh ? now : (sameRace ? old.weatherAt : 0);

        if (result.drivers != null && result.teams != null) {
            next.drivers.addAll(result.drivers);
            next.teams.addAll(result.teams);
            next.standingsAt = now;
        } else if (sameSeason) {
            next.drivers.addAll(result.drivers != null ? result.drivers : old.drivers);
            next.teams.addAll(result.teams != null ? result.teams : old.teams);
            next.standingsAt = old.standingsAt;
        }

        fillColors(next, () -> {
            widgetSnapshotStore.save(app, next);
            finish();
        });
    }

    private static widgetSnapshot.Session find(widgetSnapshot snapshot, String key) {
        for (widgetSnapshot.Session s : snapshot.sessions) {
            if (s.key.equals(key)) {
                return s;
            }
        }

        return null;
    }

    private static String iconName(Context app, int icon) {
        try {
            return app.getResources().getResourceEntryName(icon);
        } catch (RuntimeException error) {
            return "";
        }
    }

    private static int year(String season) {
        try {
            return Integer.parseInt(season.trim());
        } catch (NumberFormatException error) {
            return LocalDate.now().getYear();
        }
    }

    /** Null when the answer is missing or unreadable; an empty list when the season has no table yet. */
    private static List<widgetSnapshot.Standing> parse(JSONObject response, boolean teams, int limit) {
        if (response == null) {
            return null;
        }

        List<widgetSnapshot.Standing> out = new ArrayList<>();

        try {
            JSONArray lists = response.getJSONObject("MRData").getJSONObject("StandingsTable")
                    .optJSONArray("StandingsLists");

            if (lists == null || lists.length() == 0) {
                return out;
            }

            JSONArray rows = lists.getJSONObject(0)
                    .optJSONArray(teams ? "ConstructorStandings" : "DriverStandings");

            for (int i = 0; rows != null && i < rows.length() && out.size() < limit; i++) {
                JSONObject row = rows.getJSONObject(i);
                widgetSnapshot.Standing s = new widgetSnapshot.Standing();
                s.position = row.optInt("position", i + 1);
                s.points = row.optString("points", "");

                JSONObject constructor;

                if (teams) {
                    constructor = row.optJSONObject("Constructor");
                    s.label = constructor == null ? "" : constructor.optString("name", "");
                } else {
                    JSONObject driver = row.optJSONObject("Driver");
                    String code = driver == null ? "" : driver.optString("code", "");

                    if (code.isEmpty() && driver != null) {
                        code = driver.optString("familyName", "").toUpperCase(Locale.ROOT);
                    }

                    s.label = code;

                    JSONArray all = row.optJSONArray("Constructors");
                    constructor = all == null || all.length() == 0
                            ? null : all.optJSONObject(all.length() - 1);
                }

                s.constructorId = constructor == null ? "" : constructor.optString("constructorId", "");

                String known = s.constructorId.isEmpty() ? null : teamColors.get(s.constructorId);
                s.color = known == null ? "" : known;
                out.add(s);
            }
        } catch (JSONException error) {
            return null;
        }

        return out;
    }

    /** The colours the app has not met yet are read once from the database. */
    private static void fillColors(widgetSnapshot snapshot, Runnable done) {
        List<widgetSnapshot.Standing> all = new ArrayList<>(snapshot.drivers);
        all.addAll(snapshot.teams);

        Set<String> missing = new LinkedHashSet<>();

        for (widgetSnapshot.Standing s : all) {
            if (s.color.isEmpty() && !s.constructorId.isEmpty()) {
                missing.add(s.constructorId);
            }
        }

        if (missing.isEmpty()) {
            done.run();
            return;
        }

        DatabaseReference root = FirebaseDatabase.getInstance().getReference();
        AtomicInteger left = new AtomicInteger(missing.size());

        for (String id : missing) {
            teamColors.fetch(root, id, color -> {
                if (color != null) {
                    for (widgetSnapshot.Standing s : all) {
                        if (id.equals(s.constructorId)) {
                            s.color = color;
                        }
                    }
                }

                if (left.decrementAndGet() == 0) {
                    done.run();
                }
            });
        }
    }
}