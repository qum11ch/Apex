package com.example.f1app;

import static com.example.f1app.MainActivity.getStringByName;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.SizeF;
import android.view.View;
import android.widget.RemoteViews;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import android.util.Log;

/**
 * The "next stage" widget: two cards. SMALL fills the cells of the launcher and shows less on a lower
 * widget; WIDE is the full card that grows with the height. The launcher itself picks the variant that
 * fits the real size of the widget (Android 12 and newer).
 */
final class widgetNextRace {

    static final String ACTION_TICK = "com.example.f1app.WIDGET_TICK";

    private static final int WIDE_MIN_DP = 240;
    private static final int TIMELINE_MIN_DP = 245;
    private static final int SUBTITLE_MIN_DP = 285;
    private static final int FOOTER_MIN_DP = 310;
    private static final long MINUTE_MS = 60_000L;
    private static final long HOUR_MS = 3_600_000L;
    private static final long DAY_MS = 24 * HOUR_MS;
    private static final int SMALL_TITLE_MIN_DP = 140;
    private static final int SMALL_CHIP_MIN_DP = 190;

    /** Sizes (width, height in dp) at which the card changes; the launcher picks the one that fits. */
    private static final int[][] VARIANTS = {
            {188, 100}, {188, 140}, {188, 190},
            {240, 175}, {240, 245}, {240, 285}, {240, 310}
    };

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

        if (ids.length == 0) {
            cancelTick(app);
            return;
        }

        for (int id : ids) {
            update(app, manager, id);
        }
    }

    static void update(Context context, AppWidgetManager manager, int id) {
        long now = System.currentTimeMillis();
        widgetSnapshot snapshot = widgetSnapshotStore.load(context);

        manager.updateAppWidget(id,
                create(context, snapshot, manager.getAppWidgetOptions(id), now));
        scheduleTick(context, snapshot, now);
    }

    private static RemoteViews create(Context context, widgetSnapshot s, Bundle options, long now) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Map<SizeF, RemoteViews> variants = new LinkedHashMap<>();

            for (int[] size : VARIANTS) {
                try {
                    variants.put(new SizeF(size[0], size[1]), build(context, s, size[0], size[1], now));
                } catch (RuntimeException error) {
                    Log.e("NextRaceWidget", "variant " + size[0] + "x" + size[1] + " failed", error);
                }
            }

            if (variants.isEmpty()) {
                return build(context, s, 190, 250, now);
            }

            return new RemoteViews(variants);
        }

        boolean portrait = context.getResources().getConfiguration().orientation
                != Configuration.ORIENTATION_LANDSCAPE;
        int width = options.getInt(portrait
                ? AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH
                : AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, 190);
        int height = options.getInt(portrait
                ? AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT
                : AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 250);

        return build(context, s, width, height, now);
    }

    static RemoteViews build(Context context, widgetSnapshot s, int widthDp, int heightDp,
                             long now) {
        boolean wide = widthDp >= WIDE_MIN_DP;
        RemoteViews v = new RemoteViews(context.getPackageName(),
                wide ? R.layout.widget_next_race_wide : R.layout.widget_next_race_small);
        resetViews(v, wide);

        PendingIntent open = openApp(context);

        if (open != null) {
            v.setOnClickPendingIntent(R.id.widget_root, open);
        }

        if (s == null || s.raceName.isEmpty() || s.sessions.isEmpty()) {
            message(context, v, wide, widthDp, s == null || s.drivers.isEmpty()
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
            message(context, v, wide, widthDp, R.string.awaiting_results_text);
            return v;
        }

        String raceTitle = predictionRaceNameFormatter.getName(context, s.raceName);

        v.setInt(R.id.widget_root, "setBackgroundResource",
                live != null ? R.drawable.widget_glass_live : R.drawable.widget_glass);
        v.setViewVisibility(R.id.live_badge, live != null ? View.VISIBLE : View.GONE);
        v.setViewVisibility(R.id.round_pill, s.round > 0 ? View.VISIBLE : View.GONE);

        if (s.round > 0) {
            v.setImageViewBitmap(R.id.round_pill_text, widgetGlyphs.text(context,
                    context.getString(R.string.round_pill, String.valueOf(s.round)),
                    R.font.audiowide, wide ? 12 : 11, 120, 1, false));
            widgetGlyphs.tint(context, v, R.id.round_pill_text, R.color.hero_accent);
        }

        int nameRes = getStringByName(shown.key + "_text");
        String sessionName = nameRes == 0 ? shown.key : context.getString(nameRes);

        v.setTextViewText(R.id.countdown_label, context.getString(
                live != null ? R.string.countdown_ends : R.string.countdown_starts, sessionName));
        v.setViewVisibility(R.id.countdown_label, View.VISIBLE);

        // the time
        long left = (live != null ? live.end : shown.start) - now;
        boolean hasWeather = shown.hasWeather();
        int countdownMax = wide ? widthDp - 32 - (hasWeather ? 130 : 0) : widthDp - 24;
        String countdown = countdownText(context, left, live != null);

        v.setImageViewBitmap(R.id.countdown_img, widgetGlyphs.fit(context, countdown,
                R.font.audiowide, wide ? 32 : (heightDp >= 230 ? 28 : 24), Math.max(60, countdownMax)));
        v.setContentDescription(R.id.countdown_img, countdown);
        widgetGlyphs.tint(context, v, R.id.countdown_img,
                live != null ? R.color.hero_accent : R.color.hero_text);

        // the weather of the session on the countdown
        v.setViewVisibility(R.id.weather_chip, hasWeather ? View.VISIBLE : View.GONE);

        if (hasWeather) {
            int icon = shown.iconRes(context);

            v.setTextViewText(R.id.weather_text, shown.temp + ", " + shown.rain);
            v.setViewVisibility(R.id.weather_icon, icon != 0 ? View.VISIBLE : View.GONE);

            if (icon != 0) {
                v.setImageViewResource(R.id.weather_icon, icon);
                widgetGlyphs.tint(context, v, R.id.weather_icon, R.color.hero_text);
            }
        }

        if (!wide) {
            boolean showTitle = heightDp >= SMALL_TITLE_MIN_DP;
            boolean showChip = hasWeather && heightDp >= SMALL_CHIP_MIN_DP;

            v.setViewVisibility(R.id.title_img, showTitle ? View.VISIBLE : View.GONE);
            v.setViewVisibility(R.id.weather_chip, showChip ? View.VISIBLE : View.GONE);

            if (showTitle) {
                v.setImageViewBitmap(R.id.title_img, widgetGlyphs.text(context, raceTitle,
                        R.font.good_timing_bd, 16, Math.max(60, widthDp - 28), 2, false));
                v.setContentDescription(R.id.title_img, raceTitle);
                widgetGlyphs.tint(context, v, R.id.title_img, R.color.hero_text);
            }

            return v;
        }

        // the wide card: the same lines as the card of the home screen
        ZoneId zone = ZoneId.systemDefault();
        LocalDate first = Instant.ofEpochMilli(s.sessions.get(0).start).atZone(zone).toLocalDate();
        LocalDate last = Instant.ofEpochMilli(s.sessions.get(s.sessions.size() - 1).end)
                .atZone(zone).toLocalDate();
        String range = mainFutureRaceAdapter.dateRange(first, last);
        String head = s.location.isEmpty() ? raceTitle : place(context, s.location, s.country);

        v.setImageViewBitmap(R.id.dates,
                widgetGlyphs.text(context, range, R.font.audiowide, 12, 160, 1, true));
        widgetGlyphs.tint(context, v, R.id.dates, R.color.hero_text_secondary);

        boolean subtitles = heightDp >= SUBTITLE_MIN_DP;

        v.setImageViewBitmap(R.id.headline, widgetGlyphs.text(context, head, R.font.good_timing_bd,
                20, widthDp - 32, subtitles ? 2 : 1, false));
        v.setContentDescription(R.id.headline, head);
        widgetGlyphs.tint(context, v, R.id.headline, R.color.hero_text);
        v.setViewVisibility(R.id.headline, View.VISIBLE);

        v.setViewVisibility(R.id.race_name, subtitles ? View.VISIBLE : View.GONE);

        if (subtitles) {
            v.setTextViewText(R.id.race_name,
                    predictionRaceNameFormatter.getNameWithSeason(context, s.raceName, s.season));
        }

        int circuitRes = getStringByName(s.circuitId + "_text");
        v.setViewVisibility(R.id.circuit_name, subtitles && circuitRes != 0 ? View.VISIBLE : View.GONE);

        if (subtitles && circuitRes != 0) {
            v.setTextViewText(R.id.circuit_name, context.getString(circuitRes));
        }

        boolean timeline = heightDp >= TIMELINE_MIN_DP;
        v.setViewVisibility(R.id.timeline, timeline ? View.VISIBLE : View.GONE);
        v.setViewVisibility(R.id.spacer_bottom, timeline ? View.VISIBLE : View.GONE);

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

        boolean hasLeader = heightDp >= FOOTER_MIN_DP && s.drivers.size() >= 2;
        boolean hasRecord = heightDp >= FOOTER_MIN_DP && s.recordSeconds > 0;

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

    /** The host may draw the new actions over the old state: every view is set explicitly, nothing is left over. */
    private static void resetViews(RemoteViews v, boolean wide) {
        int[] shown = wide
                ? new int[]{R.id.dates, R.id.round_pill, R.id.headline, R.id.race_name,
                R.id.circuit_name, R.id.countdown_label, R.id.countdown_img,
                R.id.weather_chip, R.id.weather_icon, R.id.spacer_bottom,
                R.id.timeline, R.id.footer, R.id.leader_text, R.id.record_text}
                : new int[]{R.id.round_pill, R.id.title_img, R.id.countdown_label,
                R.id.countdown_img, R.id.weather_chip, R.id.weather_icon};

        for (int id : shown) {
            v.setViewVisibility(id, View.VISIBLE);
        }

        v.setViewVisibility(R.id.live_badge, View.GONE);
    }

    // ---------- the time and the quiet alarm ----------

    private static String countdownText(Context context, long left, boolean live) {
        if (!live && left > DAY_MS) {
            long hours = ceilDiv(left, HOUR_MS);
            return context.getString(R.string.widget_days_hours, hours / 24, hours % 24);
        }

        long minutes = Math.max(0, ceilDiv(left, MINUTE_MS));

        return minutes < 60
                ? context.getString(R.string.widget_minutes, minutes)
                : context.getString(R.string.widget_hours_minutes, minutes / 60, minutes % 60);
    }

    private static long ceilDiv(long value, long unit) {
        return (value + unit - 1) / unit;
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

    /** Milliseconds up to the moment when the shown text changes, or 0 when nothing is left. */
    private static long nextDelay(widgetSnapshot s, long now) {
        if (s == null || s.sessions.isEmpty()) {
            return 0;
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

        long boundary = nextBoundary(s, now);
        long delay = boundary > 0 ? boundary - now + 500 : 0;

        if (live == null && next == null) {
            return delay;
        }

        long left = (live != null ? live.end : next.start) - now;
        long unit = live != null || left <= DAY_MS ? MINUTE_MS : HOUR_MS;
        long step = (left - 1) % unit + 1 + 500;

        return delay > 0 ? Math.min(delay, step) : Math.max(1000, step);
    }

    static void scheduleTick(Context context, widgetSnapshot s, long now) {
        AlarmManager alarms = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        if (alarms == null) {
            return;
        }

        long delay = nextDelay(s, now);

        if (delay <= 0) {
            alarms.cancel(tickIntent(context));
            return;
        }

        // not a waking alarm: a sleeping phone is not woken up, the widget is redrawn when it wakes
        alarms.set(AlarmManager.ELAPSED_REALTIME,
                SystemClock.elapsedRealtime() + Math.max(1000, delay), tickIntent(context));
    }

    static void cancelTick(Context context) {
        AlarmManager alarms = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        if (alarms != null) {
            alarms.cancel(tickIntent(context));
        }
    }

    private static PendingIntent tickIntent(Context context) {
        Intent intent = new Intent(context, nextRaceWidgetProvider.class).setAction(ACTION_TICK);

        return PendingIntent.getBroadcast(context, 1, intent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
    }

    // ---------- helpers ----------

    private static void message(Context context, RemoteViews v, boolean wide, int widthDp,
                                int text) {
        v.setInt(R.id.widget_root, "setBackgroundResource", R.drawable.widget_glass);
        v.setViewVisibility(R.id.live_badge, View.GONE);
        v.setViewVisibility(R.id.round_pill, View.GONE);
        v.setViewVisibility(R.id.countdown_label, View.GONE);
        v.setViewVisibility(R.id.countdown_img, View.GONE);
        v.setViewVisibility(R.id.weather_chip, View.GONE);

        String message = context.getString(text);

        if (wide) {
            v.setImageViewBitmap(R.id.headline, widgetGlyphs.text(context, message,
                    R.font.good_timing_bd, 20, widthDp - 32, 2, false));
            widgetGlyphs.tint(context, v, R.id.headline, R.color.hero_text);
            v.setViewVisibility(R.id.headline, View.VISIBLE);
            v.setViewVisibility(R.id.spacer_bottom, View.GONE);
            v.setViewVisibility(R.id.race_name, View.GONE);
            v.setViewVisibility(R.id.circuit_name, View.GONE);
            v.setViewVisibility(R.id.timeline, View.GONE);
            v.setViewVisibility(R.id.footer, View.GONE);
        } else {
            v.setViewVisibility(R.id.title_img, View.VISIBLE);
            v.setImageViewBitmap(R.id.title_img, widgetGlyphs.text(context, message,
                    R.font.good_timing_bd, 15, Math.max(60, widthDp - 24), 3, false));
            widgetGlyphs.tint(context, v, R.id.title_img, R.color.hero_text);
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