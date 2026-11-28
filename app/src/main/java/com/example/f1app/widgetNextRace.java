package com.example.f1app;

import static com.example.f1app.MainActivity.getStringByName;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.View;
import android.widget.RemoteViews;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Locale;

/**
 * The "next stage" widget in the style of the home card: the same card, fonts, pills and session
 * plates. The countdown of the last 24 hours is a Chronometer that the launcher draws by itself.
 * Colours come only from the layouts (the code switches views and drawables), so the system theme
 * is followed at once. The compact layout is for up to three columns, the wide one for more, and
 * what the wide one shows grows with its height.
 */
final class widgetNextRace {

    private static final int WIDE_MIN_DP = 220;
    private static final int TIMELINE_MIN_DP = 230;
    private static final int FOOTER_MIN_DP = 270;
    private static final long HOUR_MS = 3_600_000L;
    private static final long DAY_MS = 24 * HOUR_MS;

    private static final int[] COL = {R.id.col_1, R.id.col_2, R.id.col_3,
            R.id.col_4, R.id.col_5, R.id.col_6};
    private static final int[] LBL = {R.id.lbl_1, R.id.lbl_2, R.id.lbl_3,
            R.id.lbl_4, R.id.lbl_5, R.id.lbl_6};
    private static final int[] DY = {R.id.dy_1, R.id.dy_2, R.id.dy_3,
            R.id.dy_4, R.id.dy_5, R.id.dy_6};
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
        Bundle options = manager.getAppWidgetOptions(id);
        int minWidth = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 180);
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
            return v;
        }

        String raceTitle = predictionRaceNameFormatter.getName(context, s.raceName);

        v.setInt(R.id.widget_root, "setBackgroundResource",
                live != null ? R.drawable.widget_background_live : R.drawable.bg_hero_card);
        v.setViewVisibility(R.id.live_badge, live != null ? View.VISIBLE : View.GONE);
        v.setViewVisibility(R.id.round_pill, s.round > 0 ? View.VISIBLE : View.GONE);

        if (s.round > 0) {
            v.setTextViewText(R.id.round_pill,
                    context.getString(R.string.round_pill, String.valueOf(s.round)));
        }

        int nameRes = getStringByName(shown.key + "_text");
        String sessionName = nameRes == 0 ? shown.key : context.getString(nameRes);

        v.setTextViewText(R.id.countdown_label, context.getString(
                live != null ? R.string.countdown_ends : R.string.countdown_starts, sessionName));
        v.setViewVisibility(R.id.countdown_label, View.VISIBLE);

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

        // the weather of the session on the countdown
        v.setViewVisibility(R.id.weather_chip, shown.hasWeather() ? View.VISIBLE : View.GONE);

        if (shown.hasWeather()) {
            int icon = shown.iconRes(context);

            v.setTextViewText(R.id.weather_text, wide ? shown.temp + ", " + shown.rain : shown.temp);
            v.setViewVisibility(R.id.weather_icon, icon != 0 ? View.VISIBLE : View.GONE);

            if (icon != 0) {
                v.setImageViewResource(R.id.weather_icon, icon);
            }
        }

        if (!wide) {
            v.setTextViewText(R.id.race_name, raceTitle);
            return v;
        }

        // the wide card: the same lines as the card of the home screen
        ZoneId zone = ZoneId.systemDefault();
        LocalDate first = Instant.ofEpochMilli(s.sessions.get(0).start).atZone(zone).toLocalDate();
        LocalDate last = Instant.ofEpochMilli(s.sessions.get(s.sessions.size() - 1).end)
                .atZone(zone).toLocalDate();

        v.setTextViewText(R.id.dates, mainFutureRaceAdapter.dateRange(first, last));
        v.setTextViewText(R.id.headline, s.location.isEmpty()
                ? raceTitle : place(context, s.location, s.country));
        v.setViewVisibility(R.id.headline, View.VISIBLE);
        v.setTextViewText(R.id.race_name,
                predictionRaceNameFormatter.getNameWithSeason(context, s.raceName, s.season));
        v.setViewVisibility(R.id.race_name, View.VISIBLE);

        int circuitRes = getStringByName(s.circuitId + "_text");
        v.setViewVisibility(R.id.circuit_name, circuitRes == 0 ? View.GONE : View.VISIBLE);

        if (circuitRes != 0) {
            v.setTextViewText(R.id.circuit_name, context.getString(circuitRes));
        }

        boolean timeline = minHeightDp >= TIMELINE_MIN_DP;
        v.setViewVisibility(R.id.timeline, timeline ? View.VISIBLE : View.GONE);

        if (timeline) {
            DateTimeFormatter day = DateTimeFormatter.ofPattern("EEE", Locale.getDefault())
                    .withZone(zone);
            DateTimeFormatter time = DateTimeFormatter.ofPattern("HH:mm").withZone(zone);

            for (int i = 0; i < COL.length; i++) {
                if (i >= s.sessions.size()) {
                    v.setViewVisibility(COL[i], View.GONE);
                    continue;
                }

                widgetSnapshot.Session x = s.sessions.get(i);
                boolean selected = x.key.equals(shown.key);
                boolean done = now >= x.end;
                int labelRes = getStringByName("session_short_" + x.key);

                v.setViewVisibility(COL[i], View.VISIBLE);
                v.setTextViewText(LBL[i], labelRes == 0 ? x.key : context.getString(labelRes));
                v.setTextViewText(DY[i], day.format(Instant.ofEpochMilli(x.start)));
                v.setTextViewText(TM[i], time.format(Instant.ofEpochMilli(x.start)));
                v.setInt(COL[i], "setBackgroundResource",
                        selected ? R.drawable.widget_chip_selected : R.drawable.widget_chip);
                v.setFloat(COL[i], "setAlpha", done && !selected ? 0.45f : 1f);
            }
        }

        boolean hasLeader = minHeightDp >= FOOTER_MIN_DP && s.drivers.size() >= 2;
        boolean hasRecord = minHeightDp >= FOOTER_MIN_DP && s.recordSeconds > 0;

        v.setViewVisibility(R.id.footer, hasLeader || hasRecord ? View.VISIBLE : View.GONE);
        v.setViewVisibility(R.id.leader_text, hasLeader ? View.VISIBLE : View.GONE);
        v.setViewVisibility(R.id.record_text, hasRecord ? View.VISIBLE : View.GONE);

        if (hasLeader) {
            widgetSnapshot.Standing a = s.drivers.get(0);
            widgetSnapshot.Standing b = s.drivers.get(1);

            v.setTextViewText(R.id.leader_text, context.getString(R.string.widget_leader,
                    a.label, a.points, points(number(a.points) - number(b.points))));
        }

        if (hasRecord) {
            v.setTextViewText(R.id.record_text,
                    context.getString(R.string.widget_record, lapTime(s.recordSeconds)));
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
        v.setInt(R.id.widget_root, "setBackgroundResource", R.drawable.bg_hero_card);
        v.setViewVisibility(R.id.live_badge, View.GONE);
        v.setViewVisibility(R.id.round_pill, View.GONE);
        v.setViewVisibility(R.id.countdown_label, View.GONE);
        v.setViewVisibility(R.id.countdown, View.GONE);
        v.setViewVisibility(R.id.countdown_live, View.GONE);
        v.setViewVisibility(R.id.countdown_text, View.GONE);
        v.setViewVisibility(R.id.weather_chip, View.GONE);

        if (wide) {
            v.setTextViewText(R.id.headline, context.getString(text));
            v.setViewVisibility(R.id.headline, View.VISIBLE);
            v.setViewVisibility(R.id.race_name, View.GONE);
            v.setViewVisibility(R.id.circuit_name, View.GONE);
            v.setViewVisibility(R.id.timeline, View.GONE);
            v.setViewVisibility(R.id.footer, View.GONE);
        } else {
            v.setTextViewText(R.id.race_name, context.getString(text));
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