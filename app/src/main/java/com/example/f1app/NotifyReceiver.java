package com.example.f1app;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.google.firebase.FirebaseApp;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class NotifyReceiver extends BroadcastReceiver {

    @SuppressLint("UnsafeProtectedBroadcastReceiver")
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            try {
                reminderSettings.reschedule(context);
            } catch (RuntimeException error) {
                Log.w("NotifyReceiver", "Cannot restart reminders after boot", error);
            }
            return;
        }

        Bundle bundle = intent.getExtras();

        if (bundle == null || bundle.getString("channelId") == null) {
            return;
        }

        String channelId = bundle.getString("channelId");

        createNotificationChannel(context, channelId);

        PendingResult result = goAsync();

        showNotification(
                context,
                result,
                bundle.getString("season"),
                bundle.getString("raceName"),
                bundle.getString("circuitId"),
                channelId,
                bundle.getString("title"),
                bundle.getString("body"),
                bundle.getInt("notificationId", 1)
        );
    }

    private void showNotification(Context context, PendingResult result, String season,
                                  String raceName, String circuitId, String channelId,
                                  String title, String body, int notificationId) {
        Intent notificationIntent = new Intent(context, splashNotificationActivity.class);
        Bundle bundle = new Bundle();
        FirebaseApp.initializeApp(context);
        DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference();
        StorageReference storageRef = FirebaseStorage.getInstance().getReference();

        rootRef.child("schedule").child("season").child(season).child(raceName)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        String dateStart_string = snapshot.child("FirstPractice")
                                .child("firstPracticeDate").getValue(String.class);
                        String dateEnd_string = snapshot.child("raceDate").getValue(String.class);
                        Integer round = snapshot.child("round").getValue(Integer.class);

                        if (dateStart_string == null || dateEnd_string == null || round == null) {
                            result.finish();
                            return;
                        }

                        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-M-d");
                        LocalDate dateStart = LocalDate.parse(dateStart_string, dateFormatter);
                        LocalDate dateEnd = LocalDate.parse(dateEnd_string, dateFormatter);

                        String dayStart = dateStart.format(DateTimeFormatter.ofPattern("dd"));
                        String dayEnd = dateEnd.format(DateTimeFormatter.ofPattern("dd"));
                        String monthStart = dateStart.format(DateTimeFormatter.ofPattern("MMM"));
                        String monthEnd = dateEnd.format(DateTimeFormatter.ofPattern("MMM"));

                        boolean isCanceled = Boolean.TRUE.equals(
                                snapshot.child("Canceled").getValue(Boolean.class));

                        rootRef.child("circuits").child(circuitId)
                                .addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot circuit) {
                                        String raceCountry = circuit.child("country")
                                                .getValue(String.class);

                                        bundle.putString("raceName", raceName);
                                        bundle.putString("futureRaceStartDay", dayStart);
                                        bundle.putString("futureRaceEndDay", dayEnd);
                                        bundle.putString("futureRaceStartMonth", monthStart);
                                        bundle.putString("futureRaceEndMonth", monthEnd);
                                        bundle.putString("roundCount", String.valueOf(round));
                                        bundle.putString("raceCountry", raceCountry);
                                        bundle.putString("circuitId", circuitId);
                                        bundle.putString("dateStart", dateStart_string);
                                        bundle.putString("dateEnd", dateEnd_string);
                                        bundle.putBoolean("isCanceled", isCanceled);
                                        bundle.putBoolean("fromNotify", true);
                                        notificationIntent.putExtras(bundle);
                                        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                                                | Intent.FLAG_ACTIVITY_SINGLE_TOP);

                                        PendingIntent content = PendingIntent.getActivity(
                                                context, notificationId, notificationIntent,
                                                PendingIntent.FLAG_IMMUTABLE
                                                        | PendingIntent.FLAG_UPDATE_CURRENT);

                                        StorageReference circuitImage =
                                                storageRef.child("circuits/" + circuitId + ".png");

                                        GlideApp.with(context).asBitmap().load(circuitImage)
                                                .into(new CustomTarget<Bitmap>() {
                                                    @Override
                                                    public void onResourceReady(
                                                            @NonNull Bitmap resource,
                                                            @Nullable Transition<? super Bitmap> t) {
                                                        post(context, channelId, title, body,
                                                                content, resource, notificationId);
                                                        result.finish();
                                                    }

                                                    @Override
                                                    public void onLoadFailed(
                                                            @Nullable Drawable errorDrawable) {
                                                        post(context, channelId, title, body,
                                                                content, null, notificationId);
                                                        result.finish();
                                                    }

                                                    @Override
                                                    public void onLoadCleared(
                                                            @Nullable Drawable placeholder) {
                                                    }
                                                });
                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError error) {
                                        result.finish();
                                    }
                                });
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        result.finish();
                    }
                });
    }

    private void post(Context context, String channelId, String title, String body,
                      PendingIntent content, @Nullable Bitmap largeIcon, int notificationId) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentIntent(content)
                .setContentText(body)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        if (largeIcon != null) {
            builder.setLargeIcon(largeIcon);
        }

        NotificationManager manager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        manager.notify(notificationId, builder.build());
    }

    private void createNotificationChannel(final Context context, String channelId) {
        NotificationChannel channel = new NotificationChannel(
                channelId, "Schedule Alerts", NotificationManager.IMPORTANCE_DEFAULT);
        channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        channel.setDescription("Race schedule alerts for user");
        context.getSystemService(NotificationManager.class).createNotificationChannel(channel);
    }
}