package com.example.f1app;

import static com.example.f1app.MainActivity.APP_PREFERENCES;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.core.app.NotificationManagerCompat;

import org.json.JSONArray;
import org.json.JSONException;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

public class BootService extends Service {
    private final IBinder mBinder = new LocalBinder();

    public class LocalBinder extends Binder {
        public BootService getService() {
            return BootService.this;
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.getBooleanExtra("disable", false)) {
            reminderSettings.save(this, false, reminderSettings.leadMinutes(this));
            reminderSettings.cancelAll(this);
            NotificationManagerCompat.from(this)
                    .cancel(intent.getIntExtra("notificationId", 0));
            stopSelf(startId);
            return START_NOT_STICKY;
        }

        reminderSettings.cancelAll(this);

        if (!reminderSettings.enabled(this)) {
            stopSelf(startId);
            return START_NOT_STICKY;
        }

        SharedPreferences prefs =
                getApplicationContext().getSharedPreferences(APP_PREFERENCES, Context.MODE_PRIVATE);

        int leadMinutes = reminderSettings.leadMinutes(this);
        long leadMillis = TimeUnit.MINUTES.toMillis(leadMinutes);
        long afterEndMillis = TimeUnit.MINUTES.toMillis(5);
        long now = System.currentTimeMillis();
        int iterator = 0;

        try {
            JSONArray events = new JSONArray(prefs.getString("events_json", "[]"));
            JSONArray circuits = new JSONArray(prefs.getString("circuits_json", "[]"));

            for (int i = 0; i < events.length(); i++) {
                String circuitId = circuits.optString(i, "");
                String[] value = events.getString(i).split("%");
                String[] race = value[0].split("\\$");

                if (race.length < 2 || circuitId.isEmpty()) {
                    iterator += 2 * Math.max(0, value.length - 1);
                    continue;
                }

                for (int j = 1; j < value.length; j++) {
                    try {
                        String[] event = value[j].split("\\$");
                        String eventKey = event[0];
                        long start = Instant.parse(event[1].replaceAll("\\s+", "T")).toEpochMilli();
                        long end = Instant.parse(event[2].replaceAll("\\s+", "T")).toEpochMilli();

                        if (start - leadMillis - now >= -100) {
                            pushNotification(race[0], race[1], circuitId, start - leadMillis,
                                    eventKey, true, start, leadMinutes, iterator);
                        }

                        if (end + afterEndMillis - now >= -100) {
                            pushNotification(race[0], race[1], circuitId, end + afterEndMillis,
                                    eventKey, false, start, leadMinutes, iterator + 1);
                        }
                    } catch (RuntimeException error) {
                        Log.w("BootService", "Bad event: " + value[j], error);
                    }

                    iterator += 2;
                }
            }
        } catch (JSONException error) {
            Log.e("BootService", "Cannot read saved events", error);
        }

        reminderSettings.saveAlarmCount(this, iterator);
        stopSelf(startId);
        return START_NOT_STICKY;
    }

    private void pushNotification(String season, String raceName, String circuitId, long when,
                                  String eventKey, boolean start, long startMillis,
                                  int leadMinutes, int iterator) {
        Intent intent = new Intent(getApplicationContext(), NotifyReceiver.class);
        intent.setAction("TAKE_THIS_NOTIFICATION_RIGHT_NOW");
        intent.putExtra("season", season);
        intent.putExtra("raceName", raceName);
        intent.putExtra("circuitId", circuitId);
        intent.putExtra("eventKey", eventKey);
        intent.putExtra("start", start);
        intent.putExtra("startMillis", startMillis);
        intent.putExtra("leadMinutes", leadMinutes);
        intent.putExtra("notificationId", 1000 + iterator);

        PendingIntent pending = PendingIntent.getBroadcast(getApplicationContext(), iterator,
                intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        AlarmManager alarms = (AlarmManager) getSystemService(ALARM_SERVICE);

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                    && !alarms.canScheduleExactAlarms()) {
                alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, when, pending);
            } else {
                alarms.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, when, pending);
            }
        } catch (SecurityException error) {
            alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, when, pending);
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }
}