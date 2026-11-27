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
import java.util.Locale;

/**
 * The "next stage" widget. The countdown of the last 24 hours is a Chronometer that the launcher
 * draws by itself, so the app does not wake up every second; a longer time is a text that is redrawn
 * only when the state changes (see nextBoundary).
 */
final class widgetNextRace {
    private static final int WIDE_MIN_DP = 200;
    private static final long HOUR_MS = 3_600_000L;
    private static final long DAY_MS = 24 * HOUR_MS;
    private static final int[] SEGMENTS = {R.id.seg_1, R.id.seg_2, R.id.seg_3,
            R.id.seg_4, R.id.seg_5, R.id.seg_6};

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
        int minWidth = manager.getAppWidgetOptions(id)
                .getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 250);

        manager.updateAppWidget(id, build(context, widgetSnapshotStore.load(context), minWidth,
                System.currentTimeMillis()));
    }

    static RemoteViews build(Context context, widgetSnapshot s, int minWidthDp, long now) {
        boolean wide = minWidthDp >= WIDE_MIN_DP;
        RemoteViews v = new RemoteViews(context.getPackageName(),
                wide ? R.layout.widget_next_race_wide : R.layout.widget_next_race_small);

        PendingIntent open = openApp(context);

        if (open != null) {
            v.setOnClickPendingIntent(R.id.widget_root, open);
        }

        if (s == null || s.raceName.isEmpty() || s.sessions.isEmpty()) {
            message(context, v, s == null || s.drivers.isEmpty()
                    ? R.string.widget_no_data : R.string.widget_season_over);
            return v;
        }

        v.setTextViewText(R.id.race_name, predictionRaceNameFormatter.getName(context, s.raceName));

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
            message(context, v, R.string.awaiting_results_text);
            v.setTextViewText(R.id.race_name,
                    predictionRaceNameFormatter.getName(context, s.raceName));
            return v;
        }

        v.setInt(R.id.widget_root, "setBackgroundResource",
                live != null ? R.drawable.widget_background_live : R.drawable.widget_background);

        int nameRes = getStringByName(shown.key + "_text");
        String name = nameRes == 0 ? shown.key : context.getString(nameRes);

        if (wide) {
            String time = DateTimeFormatter.ofPattern("EEE, HH:mm", Locale.getDefault())
                    .withZone(ZoneId.systemDefault())
                    .format(Instant.ofEpochMilli(shown.start));
            name = name + " \u00B7 " + time;
        }

        v.setTextViewText(R.id.session_name, name);
        v.setViewVisibility(R.id.session_name, View.VISIBLE);
        v.setViewVisibility(R.id.live_badge, live != null ? View.VISIBLE : View.GONE);

        long left = (live != null ? live.end : shown.start) - now;
        long base = SystemClock.elapsedRealtime() + left;

        if (live != null || left < DAY_MS) {
            int on = live != null ? R.id.countdown_live : R.id.countdown;
            int off = live != null ? R.id.countdown : R.id.countdown_live;

            v.setChronometer(on, base, null, true);
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

        if (wide) {
            int icon = shown.hasWeather() ? shown.iconRes(context) : 0;

            v.setViewVisibility(R.id.weather_chip, shown.hasWeather() ? View.VISIBLE : View.GONE);

            if (shown.hasWeather()) {
                v.setTextViewText(R.id.weather_text, shown.temp + ", " + shown.rain);
                v.setViewVisibility(R.id.weather_icon, icon != 0 ? View.VISIBLE : View.GONE);

                if (icon != 0) {
                    v.setImageViewResource(R.id.weather_icon, icon);
                }
            }

            v.setViewVisibility(R.id.segments, View.VISIBLE);

            for (int i = 0; i < SEGMENTS.length; i++) {
                if (i >= s.sessions.size()) {
                    v.setViewVisibility(SEGMENTS[i], View.GONE);
                    continue;
                }

                widgetSnapshot.Session x = s.sessions.get(i);
                int background = now >= x.end ? R.drawable.widget_seg_done
                        : now >= x.start ? R.drawable.widget_seg_live : R.drawable.widget_seg_future;

                v.setViewVisibility(SEGMENTS[i], View.VISIBLE);
                v.setInt(SEGMENTS[i], "setBackgroundResource", background);
            }
        }

        return v;
    }

    private static void message(Context context, RemoteViews v, int text) {
        v.setInt(R.id.widget_root, "setBackgroundResource", R.drawable.widget_background);
        v.setTextViewText(R.id.race_name, context.getString(text));
        v.setViewVisibility(R.id.session_name, View.GONE);
        v.setViewVisibility(R.id.live_badge, View.GONE);
        v.setViewVisibility(R.id.countdown, View.GONE);
        v.setViewVisibility(R.id.countdown_live, View.GONE);
        v.setViewVisibility(R.id.countdown_text, View.GONE);
        v.setViewVisibility(R.id.weather_chip, View.GONE);
        v.setViewVisibility(R.id.segments, View.GONE);
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

    private static PendingIntent openApp(Context context) {
        Intent intent = context.getPackageManager().getLaunchIntentForPackage(context.getPackageName());

        if (intent == null) {
            return null;
        }

        return PendingIntent.getActivity(context, 0, intent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
    }
}