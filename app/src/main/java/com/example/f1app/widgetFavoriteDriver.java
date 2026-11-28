package com.example.f1app;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.SizeF;
import android.view.View;
import android.widget.RemoteViews;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.LinkedHashMap;
import java.util.Map;

final class widgetFavoriteDriver {

    private static final String PREFS = "widget_favorite_driver";
    private static final String KEY = "state";

    private widgetFavoriteDriver() {
    }

    static boolean exists(Context context) {
        return ids(context).length > 0;
    }

    private static int[] ids(Context context) {
        return AppWidgetManager.getInstance(context).getAppWidgetIds(
                new ComponentName(context, favoriteDriverWidgetProvider.class));
    }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext()
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    private static String uid() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        return user == null ? "" : user.getUid();
    }

    /**
     * Loads profile choice and public season statistics.
     * The completion runs after the widget has been redrawn.
     */
    static void refresh(Context context, Runnable done) {
        Context app = context.getApplicationContext();

        if (!exists(app)) {
            done.run();
            return;
        }

        widgetFavoritesLoader.load(favorites -> {
            if (favorites.state == widgetFavoritesLoader.State.SIGNED_OUT) {
                prefs(app).edit().remove(KEY).apply();
                updateAll(app);
                done.run();
                return;
            }

            if (favorites.state != widgetFavoritesLoader.State.LOADED) {
                updateAll(app);
                done.run();
                return;
            }

            String owner = favorites.uid;

            if (!owner.equals(uid())) {
                updateAll(app);
                done.run();
                return;
            }

            widgetSnapshot common = widgetSnapshotStore.load(app);
            int season;

            try {
                season = common == null ? 0 : Integer.parseInt(common.season);
            } catch (NumberFormatException error) {
                season = 0;
            }

            if (!favorites.hasDriver()) {
                save(app, owner, "", season, "choose", null, false);
                updateAll(app);
                done.run();
                return;
            }

            final int requestedSeason = season;

            // Immediately remove statistics belonging to a previous choice.
            JSONObject previous = read(app);
            boolean same = previous != null
                    && owner.equals(previous.optString("uid"))
                    && favorites.driver.equals(previous.optString("driver"))
                    && requestedSeason == previous.optInt("season");

            if (!same) {
                save(app, owner, favorites.driver, requestedSeason,
                        "loading", null, false);
                updateAll(app);
            }

            if (requestedSeason < 1950) {
                save(app, owner, favorites.driver, requestedSeason,
                        "error", null, false);
                updateAll(app);
                done.run();
                return;
            }

            widgetDriverSeasonLoader.load(
                    app, requestedSeason, favorites.driver, false,
                    (state, snapshot, stale) -> {
                        if (owner.equals(uid())) {
                            String status;

                            switch (state) {
                                case READY:
                                    status = "ready";
                                    break;
                                case NO_SEASON_ENTRY:
                                    status = "no_entry";
                                    break;
                                default:
                                    status = "error";
                            }

                            save(app, owner, favorites.driver, requestedSeason,
                                    status, snapshot, stale);
                        }

                        updateAll(app);
                        done.run();
                    });
        });
    }

    private static void save(Context context, String uid, String driver,
                             int season, String status, JSONObject data,
                             boolean stale) {
        try {
            JSONObject state = new JSONObject();
            state.put("uid", uid);
            state.put("driver", driver);
            state.put("season", season);
            state.put("status", status);
            state.put("data", data == null ? JSONObject.NULL : data);
            state.put("stale", stale);

            prefs(context).edit().putString(KEY, state.toString()).apply();
        } catch (JSONException error) {
            android.util.Log.e("FavoriteDriverWidget", "Cannot save state", error);
        }
    }

    private static JSONObject read(Context context) {
        try {
            String raw = prefs(context).getString(KEY, null);
            return raw == null ? null : new JSONObject(raw);
        } catch (JSONException error) {
            return null;
        }
    }

    static void updateAll(Context context) {
        Context app = context.getApplicationContext();
        AppWidgetManager manager = AppWidgetManager.getInstance(app);

        for (int id : ids(app)) {
            update(app, manager, id);
        }
    }

    static void update(Context context, AppWidgetManager manager, int id) {
        JSONObject state = read(context);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Map<SizeF, RemoteViews> views = new LinkedHashMap<>();
            views.put(new SizeF(180, 140), build(context, state, 180, 140));
            views.put(new SizeF(180, 230), build(context, state, 180, 230));
            views.put(new SizeF(240, 230), build(context, state, 240, 230));
            views.put(new SizeF(360, 230), build(context, state, 360, 230));
            manager.updateAppWidget(id, new RemoteViews(views));
            return;
        }

        Bundle options = manager.getAppWidgetOptions(id);
        boolean landscape = context.getResources().getConfiguration().orientation
                == Configuration.ORIENTATION_LANDSCAPE;

        int width = options.getInt(landscape
                ? AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH
                : AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 240);
        int height = options.getInt(landscape
                ? AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT
                : AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 230);

        manager.updateAppWidget(id, build(context, state, width, height));
    }

    private static RemoteViews build(Context context, JSONObject state,
                                     int width, int height) {
        RemoteViews v = new RemoteViews(
                context.getPackageName(), R.layout.widget_favorite_driver);

        int[] hidden = {
                R.id.wd_stats, R.id.wd_extra, R.id.wd_recent_group,
                R.id.wd_action, R.id.wd_stale, R.id.wd_name, R.id.wd_season
        };

        for (int id : hidden) {
            v.setViewVisibility(id, View.GONE);
        }

        v.setViewVisibility(R.id.wd_message, View.VISIBLE);
        v.setOnClickPendingIntent(R.id.wd_root, null);

        String currentUid = uid();

        if (currentUid.isEmpty()) {
            v.setTextViewText(R.id.wd_message,
                    context.getString(R.string.wd_signin_message));
            action(context, v, R.string.wd_signin,
                    new Intent(context, logInPageActivity.class));
            return v;
        }

        if (state == null || !currentUid.equals(state.optString("uid"))) {
            v.setTextViewText(R.id.wd_message,
                    context.getString(R.string.wd_loading));
            return v;
        }

        String status = state.optString("status");

        if ("choose".equals(status)) {
            v.setTextViewText(R.id.wd_message,
                    context.getString(R.string.wd_choose_message));
            action(context, v, R.string.wd_choose,
                    new Intent(context, accountPageActivity.class)
                            .putExtra("currentSeason",
                                    String.valueOf(state.optInt("season"))));
            return v;
        }

        if (!"ready".equals(status)) {
            int text = "loading".equals(status)
                    ? R.string.wd_loading : R.string.wd_error;

            v.setTextViewText(R.id.wd_message, "no_entry".equals(status)
                    ? context.getString(R.string.wd_no_entry, state.optInt("season"))
                    : context.getString(text));
            return v;
        }

        JSONObject data = state.optJSONObject("data");

        if (data == null) {
            v.setTextViewText(R.id.wd_message,
                    context.getString(R.string.wd_error));
            return v;
        }

        v.setViewVisibility(R.id.wd_message, View.GONE);
        v.setViewVisibility(R.id.wd_stats, View.VISIBLE);
        v.setViewVisibility(R.id.wd_name, View.VISIBLE);
        v.setViewVisibility(R.id.wd_season, View.VISIBLE);

        String fullName = state.optString("driver");
        image(context, v, R.id.wd_name, fullName,
                R.font.good_timing_bd, 17, width - 24, 2);

        v.setTextViewText(R.id.wd_season,
                context.getString(R.string.wd_season, state.optInt("season")));

        metric(context, v, R.id.wd_position, data.optString("position", "—"),
                width);
        metric(context, v, R.id.wd_points, data.optString("points", "—"),
                width);
        metric(context, v, R.id.wd_wins, data.optString("wins", "—"),
                width);

        boolean extras = width >= 240 && height >= 230;
        v.setViewVisibility(R.id.wd_extra, extras ? View.VISIBLE : View.GONE);

        if (extras) {
            metric(context, v, R.id.wd_podiums,
                    data.optString("podiums", "—"), width);
            metric(context, v, R.id.wd_poles,
                    data.optString("poles", "—"), width);
        }

        JSONArray recentRows = data.optJSONArray("recent");
        boolean recent = width >= 360
                && height >= 230
                && recentRows != null
                && recentRows.length() > 0;

        v.setViewVisibility(
                R.id.wd_recent_group,
                recent ? View.VISIBLE : View.GONE
        );

        // Always clear children: the host may reuse the previous layout.
        v.removeAllViews(R.id.wd_recent);

        if (recent) {
            drawRecent(context, v, recentRows, width);
        }

        v.setViewVisibility(R.id.wd_stale,
                state.optBoolean("stale") ? View.VISIBLE : View.GONE);

        int split = fullName.lastIndexOf(' ');

        if (split > 0) {
            Intent open = new Intent(context, driverPageActivity.class)
                    .putExtra("driverName", fullName.substring(0, split))
                    .putExtra("driverFamilyName", fullName.substring(split + 1))
                    .putExtra("driverCode", data.optString("code"))
                    .putExtra("currentSeason",
                            String.valueOf(state.optInt("season")));

            v.setOnClickPendingIntent(R.id.wd_root,
                    pending(context, open, "driver/" + data.optString("driverId")));
        }

        return v;
    }

    private static void drawRecent(Context context, RemoteViews parent,
                                   JSONArray rows, int widthDp) {
        int first = Math.max(0, rows.length() - 5);
        int count = rows.length() - first;
        int textWidth = Math.max(
                24,
                (widthDp - 24) / Math.max(1, count) - 12
        );

        for (int i = first; i < rows.length(); i++) {
            JSONObject row = rows.optJSONObject(i);

            if (row == null) {
                continue;
            }

            RemoteViews chip = new RemoteViews(
                    context.getPackageName(),
                    R.layout.widget_driver_result
            );

            int position = row.optInt("position", 0);
            String positionText = row.optString("positionText", "—");
            String result = resultLabel(positionText, position);
            boolean podium = position >= 1
                    && position <= 3
                    && String.valueOf(position).equals(positionText);

            chip.setTextViewText(
                    R.id.wdr_round,
                    context.getString(
                            R.string.wd_result_round,
                            row.optInt("round")
                    )
            );

            chip.setInt(
                    R.id.wdr_root,
                    "setBackgroundResource",
                    podium
                            ? R.drawable.widget_chip_selected
                            : R.drawable.widget_chip
            );

            chip.setImageViewBitmap(
                    R.id.wdr_result,
                    widgetGlyphs.fit(
                            context,
                            result,
                            R.font.audiowide,
                            18,
                            textWidth
                    )
            );

            widgetGlyphs.tint(
                    context,
                    chip,
                    R.id.wdr_result,
                    podium ? R.color.hero_accent : R.color.widget_text
            );

            String raceName = predictionRaceNameFormatter.getName(
                    context,
                    row.optString("raceName")
            );

            chip.setContentDescription(
                    R.id.wdr_root,
                    context.getString(
                            R.string.wd_result_accessibility,
                            raceName,
                            result,
                            row.optString("status", "")
                    )
            );

            parent.addView(R.id.wd_recent, chip);
        }
    }

    private static String resultLabel(String positionText, int position) {
        String value = positionText == null ? "" : positionText.trim();

        // Keep non-numeric classifications instead of presenting them
        // as a normal finishing position.
        if ("R".equalsIgnoreCase(value)) {
            return "DNF";
        }

        if ("D".equalsIgnoreCase(value)) {
            return "DSQ";
        }

        if ("W".equalsIgnoreCase(value)) {
            return "DNS";
        }

        if (!value.isEmpty() && !"null".equalsIgnoreCase(value)) {
            return value;
        }

        return position > 0 ? String.valueOf(position) : "—";
    }

    private static void metric(Context context, RemoteViews v, int id,
                               String text, int width) {
        boolean secondRow = id == R.id.wd_podiums || id == R.id.wd_poles;
        int columns = secondRow ? 2 : 3;
        int room = Math.max(32, (width - 24) / columns - 8);

        image(
                context,
                v,
                id,
                text,
                R.font.audiowide,
                23,
                room,
                1
        );
    }

    private static void image(Context context, RemoteViews v, int id,
                              String text, int font, int sp, int maxDp,
                              int lines) {
        v.setImageViewBitmap(id, lines == 1
                ? widgetGlyphs.fit(context, text, font, sp, maxDp)
                : widgetGlyphs.text(context, text, font, sp, maxDp, lines, false));
        widgetGlyphs.tint(context, v, id, R.color.widget_text);
        v.setContentDescription(id, text);
    }

    private static void action(Context context, RemoteViews v,
                               int label, Intent intent) {
        v.setViewVisibility(R.id.wd_action, View.VISIBLE);
        v.setTextViewText(R.id.wd_action, context.getString(label));
        v.setOnClickPendingIntent(R.id.wd_action,
                pending(context, intent, "action/" + label));
    }

    private static PendingIntent pending(Context context, Intent intent,
                                         String route) {
        return widgetNavigation.open(
                context,
                intent,
                "favorite-driver/" + route,
                intent.getStringExtra("currentSeason")
        );
    }
}