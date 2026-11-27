package com.example.f1app;

import static com.example.f1app.MainActivity.APP_PREFERENCES;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

final class reminderSettings {
    static final String KEY_ENABLED = "reminders_enabled";
    static final String KEY_LEAD = "reminder_lead_minutes";
    static final String KEY_COUNT = "reminder_alarm_count";
    static final int DEFAULT_LEAD = 15;
    static final int[] OPTIONS = {5, 10, 15, 30, 60};
    static final String CHANNEL_ID = "channelID2";

    private reminderSettings() {
    }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext()
                .getSharedPreferences(APP_PREFERENCES, Context.MODE_PRIVATE);
    }

    static boolean enabled(Context context) {
        return prefs(context).getBoolean(KEY_ENABLED, true);
    }

    static int leadMinutes(Context context) {
        int value = prefs(context).getInt(KEY_LEAD, DEFAULT_LEAD);

        for (int option : OPTIONS) {
            if (option == value) {
                return value;
            }
        }

        return DEFAULT_LEAD;
    }

    static boolean debugFire(Context context, int seconds) {
        Context app = context.getApplicationContext();
        String eventsJson = prefs(app).getString("events_json", "[]");
        String circuitsJson = prefs(app).getString("circuits_json", "[]");

        try {
            org.json.JSONArray events = new org.json.JSONArray(eventsJson);
            org.json.JSONArray circuits = new org.json.JSONArray(circuitsJson);

            if (events.length() == 0 || circuits.length() == 0) {
                return false;
            }

            String[] race = events.getString(0).split("%")[0].split("\\$");

            Intent intent = new Intent(app, NotifyReceiver.class);
            intent.setAction("TAKE_THIS_NOTIFICATION_RIGHT_NOW");
            intent.putExtra("channelId", CHANNEL_ID);
            intent.putExtra("season", race[0]);
            intent.putExtra("raceName", race[1]);
            intent.putExtra("circuitId", circuits.getString(0));
            intent.putExtra("title", app.getString(R.string.reminder_title));
            intent.putExtra("body", "Test");

            PendingIntent pending = PendingIntent.getBroadcast(
                    app, 9999, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

            AlarmManager alarms = (AlarmManager) app.getSystemService(Context.ALARM_SERVICE);
            long when = System.currentTimeMillis() + seconds * 1000L;

            try {
                alarms.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, when, pending);
            } catch (SecurityException error) {
                alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, when, pending);
            }

            return true;
        } catch (org.json.JSONException | RuntimeException error) {
            return false;
        }
    }

    static void save(Context context, boolean enabled, int leadMinutes) {
        prefs(context).edit()
                .putBoolean(KEY_ENABLED, enabled)
                .putInt(KEY_LEAD, leadMinutes)
                .apply();
    }

    static void saveAlarmCount(Context context, int count) {
        prefs(context).edit().putInt(KEY_COUNT, count).apply();
    }

    /** Starts the scheduling service again: it cancels the old alarms and sets the new ones. */
    static void reschedule(Context context) {
        Intent intent = new Intent(context.getApplicationContext(), BootService.class);
        intent.putExtra("channelId", CHANNEL_ID);
        context.getApplicationContext().startService(intent);
    }

    static void cancelAll(Context context) {
        Context app = context.getApplicationContext();
        AlarmManager alarms = (AlarmManager) app.getSystemService(Context.ALARM_SERVICE);
        int count = prefs(app).getInt(KEY_COUNT, 0);

        for (int code = 0; code < count; code++) {
            Intent intent = new Intent(app, NotifyReceiver.class);
            intent.setAction("TAKE_THIS_NOTIFICATION_RIGHT_NOW");

            PendingIntent pending = PendingIntent.getBroadcast(
                    app, code, intent,
                    PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE);

            if (pending != null) {
                alarms.cancel(pending);
                pending.cancel();
            }
        }

        prefs(app).edit().putInt(KEY_COUNT, 0).apply();
    }
}