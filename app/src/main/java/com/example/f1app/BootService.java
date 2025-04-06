package com.example.f1app;

import static com.example.f1app.MainActivity.APP_PREFERENCES;
import static com.example.f1app.MainActivity.getStringByName;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;


import org.json.JSONArray;
import org.json.JSONException;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class BootService extends Service {
    SharedPreferences mPrefs;

    private final IBinder mBinder = new LocalBinder();

    public class LocalBinder extends Binder {
        public BootService getService() {
            return BootService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopForeground(true);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        if (intent!=null){
            Bundle bundle = intent.getExtras();
            mPrefs = getApplicationContext().getSharedPreferences(APP_PREFERENCES, Context.MODE_PRIVATE);
            JSONArray jsonArrayEvents;
            JSONArray jsonArrayCircuits;
            assert bundle != null;
            String mChannelId = bundle.getString("channelId");
            int iterator = 0;
            try {
                jsonArrayEvents = new JSONArray(mPrefs.getString("events_json", "[]"));
                jsonArrayCircuits = new JSONArray(mPrefs.getString("circuits_json", "[]"));
                for (int i = 0; i < jsonArrayEvents.length(); i++) {
                    String circuitId = jsonArrayCircuits.get(i).toString();
                    String stroke = jsonArrayEvents.get(i).toString();
                    String[] value = stroke.split("%");
                    String raceFullName = value[0];
                    String[] raceValue = raceFullName.split("\\$");
                    String season = raceValue[0];
                    String raceName = raceValue[1];
                    String localeRaceName = raceName.toLowerCase().replaceAll("\\s+", "_");
                    String title = this.getString(getStringByName(localeRaceName + "_text")) + " " + season;
                    for(int j = 0; j < value.length - 1; j++){
                        String event = value[j + 1];
                        String[] eventValue = event.split("\\$");
                        String eventName = eventValue[0];
                        String eventStartDate = eventValue[1].replaceAll("\\s+", "T");
                        String eventEndDate = eventValue[2].replaceAll("\\s+", "T");
                        Instant dateInstStart = Instant.parse(eventStartDate);
                        ZonedDateTime dateTimeStart = dateInstStart.atZone(ZoneId.systemDefault());
                        Instant dateInstEnd = Instant.parse(eventEndDate);
                        ZonedDateTime dateTimeEnd = dateInstEnd.atZone(ZoneId.systemDefault());

                        DateTimeFormatter  fullDateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z", Locale.ENGLISH);
                        String newDateStart = dateTimeStart.format(fullDateFormatter);
                        String newDateEnd = dateTimeEnd.format(fullDateFormatter);

                        String body = getApplicationContext().getString(getStringByName(eventName + "_text"));
                        String bodyStart = body + " " + getString(R.string.notify_start_text);
                        String bodyEnd = body + " " + getString(R.string.notify_end_text);
                        Date current = Calendar.getInstance().getTime();
                        String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";
                        SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT, Locale.getDefault());
                        dateFormat.setTimeZone(TimeZone.getDefault());
                        try {
                            Date eventStart_date = dateFormat.parse(newDateStart);
                            Date eventEnd_date = dateFormat.parse(newDateEnd);
                            long diffStart = eventStart_date.getTime() - current.getTime();
                            long diffEnd = eventEnd_date.getTime() - current.getTime();
                            if (diffStart >= -100){
                                pushNotification(season, raceName, circuitId, mChannelId, eventStart_date, title, bodyStart, iterator);
                            }
                            if (diffEnd >= -100){
                                pushNotification(season, raceName, circuitId, mChannelId, eventEnd_date, title, bodyEnd, iterator + 1);
                            }
                        }catch(ParseException e){
                            Log.e("BootService", " " +  e.getMessage());
                        }
                        iterator += 2;
                    }
                }
                return START_STICKY;

            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
        }
        return START_STICKY;
    }

    public void pushNotification(String season, String raceName, String circuitId, String channelId, Date date, String title, String body, Integer iterator) {
        Intent intentStart = new Intent(getApplicationContext(), NotifyReceiver.class);
        intentStart.setAction("TAKE_THIS_NOTIFICATION_RIGHT_NOW");
        intentStart.putExtra("channelId", channelId);
        intentStart.putExtra("season", season);
        intentStart.putExtra("raceName", raceName);
        intentStart.putExtra("circuitId", circuitId);
        intentStart.putExtra("title", title);
        intentStart.putExtra("body", body);
        PendingIntent pendingIntentStart = PendingIntent.getBroadcast(getApplicationContext(), iterator, intentStart, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        AlarmManager alarmManagerStart = (AlarmManager) this.getSystemService(ALARM_SERVICE);
        alarmManagerStart.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, date.getTime(), pendingIntentStart);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return super.onUnbind(intent);
    }
}