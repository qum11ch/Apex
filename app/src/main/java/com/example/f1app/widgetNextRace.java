package com.example.f1app;

import static com.example.f1app.MainActivity.getStringByName;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import android.view.View;
import android.widget.RemoteViews;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Locale;

/**
 * The "next stage" widget. The countdown of the last 24 hours is a Chronometer that the launcher
 * draws by itself; a longer time is a text redrawn only when the state changes (see nextBoundary).
 * Colours come only from the layouts: the code switches views and drawables, so the system theme
 * is followed at once. What is shown depends on the size of the widget.
 */
final class widgetNextRace {

    private static final int WIDE_MIN_DP = 200;
    private static final int TIMELINE_MIN_DP = 150;
    private static final int EXTRA_MIN_DP = 190;
    private static final long HOUR_MS = 3_600_000L;
    private static final long DAY_MS = 24 * HOUR_MS;

    private static final int[] COL = {R.id.col_1, R.id.col_2, R.id.col_3,
            R.id.col_4, R.id.col_5, R.id.col_6};
    private static final int[] BAR = {R.id.bar_1, R.id.bar_2, R.id.bar_3,
            R.id.bar_4, R.id.bar_5, R.id.bar_6};
    private static final int[] LBL = {R.id.lbl_1, R.id.lbl_2, R.id.lbl_3,
            R.id.lbl_4, R.id.lbl_5, R.id.lbl_6};
    private static final int[] TM = {R.id.tm_1, R.id.tm_2, R.id.tm_3,
            R.id.tm_4, R.id.tm_5, R.id.tm_6};

    private widgetNextRace() {
    }

    static void updateAll(Context context) {
        Context app = context.getApplicationContext();
        AppWidgetManager manager = AppWidgetManager.getInstance(app);
        int[] ids = manager.getAppWidgetIds(new ComponentName(app, nextRaceWidgetProvider.class));

        for (int id : ids) {
            update(app, manager, id);
        }
    }

    static void update(Context context, AppWidgetManager manager, int id) {
        android.os.Bundle options = manager.getAppWidgetOptions(id);
        int minWidth = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 250);
        int minHeight = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 110);

        manager.updateAppWidget(id, build(context, widgetSnapshotStore.load(context), minWidth,
                minHeight, System.currentTimeMillis()));
    }

    static RemoteViews build(Context context, widgetSnapshot s, int minWidthDp, int minHeightDp,
                             long now) {
        boolean wide = minWidthDp >= WIDE_MIN_DP;
        RemoteViews v = new RemoteViews(context.getPackageName(),
                wide ? R.layout.widget_next_race_wide : R.layout.widget_next_race_small);

        PendingIntent open = openApp(context);

        if (open != null) {
            v.setOnClickPendingIntent(R.id.widget_root, open);
        }

        if (s == null || s.raceName.isEmpty() || s.sessions.isEmpty()) {
            message(context, v, wide, s == null || s.drivers.isEmpty()
                    ? R.string.widget_no_data : R.string.widget_season_over);
            return v;
        }

        String raceTitle = predictionRaceNameFormatter.getName(context, s.raceName);
        v.setTextViewText(R.id.race_name, raceTitle);

        widgetSnapshot.Session live = null;
        widgetSnapshot.Session next = null;

        for (widgetSnapshot.Session x : s.sessions) {
            if (now >= x.start && now < x.end) {
                live = x;
                break;
            }

            if (x.start > now && next == null) {
                next = x;
            }
        }

        widgetSnapshot.Session shown = live != null ? live : next;

        if (shown == null) {
            message(context, v, wide, R.string.awaiting_results_text);
            v.setTextViewText(R.id.race_name, raceTitle);
            return v;
        }

        v.setInt(R.id.widget_root, "setBackgroundResource",
                live != null ? R.drawable.widget_background_live : R.drawable.widget_background);

        DateTimeFormatter dayTime = DateTimeFormatter.ofPattern("EEE HH:mm", Locale.getDefault())
                .withZone(ZoneId.systemDefault());

        int nameRes = getStringByName(shown.key + "_text");
        String name = nameRes == 0 ? shown.key : context.getString(nameRes);

        v.setTextViewText(R.id.session_name, name);
        v.setViewVisibility(R.id.session_name, View.VISIBLE);
        v.setViewVisibility(R.id.live_badge, live != null ? View.VISIBLE : View.GONE);

        long left = (live != null ? live.end : shown.start) - now;

        if (live != null || left < DAY_MS) {
            int on = live != null ? R.id.countdown_live : R.id.countdown;
            int off = live != null ? R.id.countdown : R.id.countdown_live;

            v.setChronometer(on, SystemClock.elapsedRealtime() + left, null, true);
            v.setChronometerCountDown(on, true);
            v.setViewVisibility(on, View.VISIBLE);
            v.setChronometer(off, SystemClock.elapsedRealtime(), null, false);
            v.setViewVisibility(off, View.GONE);
            v.setViewVisibility(R.id.countdown_text, View.GONE);
        } else {
            v.setChronometer(R.id.countdown, SystemClock.elapsedRealtime(), null, false);
            v.setChronometer(R.id.countdown_live, SystemClock.elapsedRealtime(), null, false);
            v.setViewVisibility(R.id.countdown, View.GONE);
            v.setViewVisibility(R.id.countdown_live, View.GONE);
            v.setTextViewText(R.id.countdown_text, context.getString(R.string.widget_in_days_hours,
                    left / DAY_MS, (left % DAY_MS) / HOUR_MS));
            v.setViewVisibility(R.id.countdown_text, View.VISIBLE);
        }

        if (!wide) {
            return v;
        }

        v.setTextViewText(R.id.session_time, dayTime.format(Instant.ofEpochMilli(shown.start)));

        // weather of the shown session
        int icon = shown.hasWeather() ? shown.iconRes(context) : 0;

        v.setViewVisibility(R.id.weather_chip, shown.hasWeather() ? View.VISIBLE : View.GONE);

        if (shown.hasWeather()) {
            v.setTextViewText(R.id.weather_text, shown.temp + ", " + shown.rain);
            v.setViewVisibility(R.id.weather_icon, icon != 0 ? View.VISIBLE : View.GONE);

            if (icon != 0) {
                v.setImageViewResource(R.id.weather_icon, icon);
            }
        }

        // the whole weekend
        boolean timeline = minHeightDp >= TIMELINE_MIN_DP;
        v.setViewVisibility(R.id.timeline, timeline ? View.VISIBLE : View.GONE);

        if (timeline) {
            for (int i = 0; i < COL.length; i++) {
                if (i >= s.sessions.size()) {
                    v.setViewVisibility(COL[i], View.GONE);
                    continue;
                }

                widgetSnapshot.Session x = s.sessions.get(i);
                boolean done = now >= x.end;
                boolean going = now >= x.start && !done;
                int labelRes = getStringByName("session_short_" + x.key);

                v.setViewVisibility(COL[i], View.VISIBLE);
                v.setTextViewText(LBL[i], labelRes == 0 ? x.key : context.getString(labelRes));
                v.setTextViewText(TM[i], dayTime.format(Instant.ofEpochMilli(x.start)));
                v.setInt(BAR[i], "setBackgroundResource", going ? R.drawable.widget_seg_live
                        : done ? R.drawable.widget_seg_done : R.drawable.widget_seg_future);
                v.setFloat(COL[i], "setAlpha", done ? 0.45f : 1f);
            }
        }

        // race number, the place, the leader and the lap record
        boolean extra = minHeightDp >= EXTRA_MIN_DP;

        v.setViewVisibility(R.id.round_chip, s.round > 0 ? View.VISIBLE : View.GONE);

        if (s.round > 0) {
            v.setTextViewText(R.id.round_chip, context.getString(R.string.widget_round,
                    s.round, Math.max(s.totalRounds, s.round)));
        }

        boolean hasPlace = extra && s.laps > 0 && !s.location.isEmpty();
        v.setViewVisibility(R.id.subtitle_row, hasPlace ? View.VISIBLE : View.GONE);

        if (hasPlace) {
            v.setTextViewText(R.id.subtitle, place(context, s.location, s.country));
            v.setTextViewText(R.id.laps_text, context.getString(R.string.widget_laps, s.laps));
            v.setTextViewText(R.id.length_text, context.getString(R.string.widget_length,
                    String.format(Locale.getDefault(), "%.3f", s.lengthKm)));
        }

        boolean hasLeader = extra && s.drivers.size() >= 2;
        boolean hasRecord = extra && s.recordSeconds > 0;
        v.setViewVisibility(R.id.footer, hasLeader || hasRecord ? View.VISIBLE : View.GONE);
        v.setViewVisibility(R.id.leader_text, hasLeader ? View.VISIBLE : View.GONE);
        v.setViewVisibility(R.id.record_text, hasRecord ? View.VISIBLE : View.GONE);

        if (hasLeader) {
            widgetSnapshot.Standing first = s.drivers.get(0);
            widgetSnapshot.Standing second = s.drivers.get(1);
            double gap = number(first.points) - number(second.points);

            v.setTextViewText(R.id.leader_text, context.getString(R.string.widget_leader,
                    first.label, first.points, points(gap)));
        }

        if (hasRecord) {
            v.setTextViewText(R.id.record_text, context.getString(R.string.widget_record,
                    lapTime(s.recordSeconds)));
        }

        return v;
    }

    /** The next moment at which what the widget shows changes, or 0 when nothing is left. */
    static long nextBoundary(widgetSnapshot s, long now) {
        long best = 0;

        if (s == null) {
            return 0;
        }

        for (widgetSnapshot.Session x : s.sessions) {
            long[] moments = {x.start - DAY_MS, x.start, x.end};

            for (long t : moments) {
                if (t > now && (best == 0 || t < best)) {
                    best = t;
                }
            }
        }

        return best;
    }

    private static void message(Context context, RemoteViews v, boolean wide, int text) {
        v.setInt(R.id.widget_root, "setBackgroundResource", R.drawable.widget_background);
        v.setTextViewText(R.id.race_name, context.getString(text));
        v.setViewVisibility(R.id.session_name, View.GONE);
        v.setViewVisibility(R.id.live_badge, View.GONE);
        v.setViewVisibility(R.id.countdown, View.GONE);
        v.setViewVisibility(R.id.countdown_live, View.GONE);
        v.setViewVisibility(R.id.countdown_text, View.GONE);

        if (wide) {
            v.setViewVisibility(R.id.round_chip, View.GONE);
            v.setViewVisibility(R.id.subtitle_row, View.GONE);
            v.setViewVisibility(R.id.session_time, View.GONE);
            v.setViewVisibility(R.id.weather_chip, View.GONE);
            v.setViewVisibility(R.id.timeline, View.GONE);
            v.setViewVisibility(R.id.footer, View.GONE);
        }
    }

    /** The same rule as the schedule lists: the city and the country, no city for Monaco and Singapore. */
    private static String place(Context context, String city, String country) {
        if (country.isEmpty()) {
            return city;
        }

        if (Locale.getDefault().getLanguage().equals("ru")) {
            try {
                ArrayList<String> localized =
                        teamsStandingsActivity.localizeLocality(city, country, context);

                if (localized.size() > 2) {
                    return localized.get(2);
                }
            } catch (RuntimeException ignored) {
                // fall through to the plain text
            }
        }

        if ("Monaco".equals(city) || "Singapore".equals(city) || city.isEmpty()) {
            return country;
        }

        return city + ", " + country;
    }

    private static double number(String value) {
        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException error) {
            return 0;
        }
    }

    private static String points(double value) {
        return value == Math.rint(value) ? String.valueOf((long) value)
                : String.format(Locale.US, "%.1f", value);
    }

    private static String lapTime(double seconds) {
        int minutes = (int) (seconds / 60);
        return String.format(Locale.US, "%d:%06.3f", minutes, seconds - minutes * 60);
    }

    private static PendingIntent openApp(Context context) {
        Intent intent = context.getPackageManager().getLaunchIntentForPackage(context.getPackageName());

        if (intent == null) {
            return null;
        }

        return PendingIntent.getActivity(context, 0, intent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
    }
}