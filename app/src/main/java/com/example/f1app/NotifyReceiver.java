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
    private final String channelId = "channelId";

    @SuppressLint("UnsafeProtectedBroadcastReceiver")
    @Override
    public void onReceive(Context context, Intent intent) {
        if(intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)){
            Intent intentStart = new Intent(context, BootService.class);
            intentStart.putExtra("channelId", channelId);
            context.startService(intentStart);
            Bundle bundle = intent.getExtras();
            if(bundle.getString("channelId") != null) {
                String channelId = bundle.getString("channelId");
                String season = bundle.getString("season");
                String raceName = bundle.getString("raceName");
                String circuitId = bundle.getString("circuitId");
                String title = bundle.getString("title");
                String body = bundle.getString("body");
                createNotificationChannel(context, channelId);
                showNotification(context, season, raceName, circuitId, channelId, title, body);
            }
        } else{
            Bundle bundle = intent.getExtras();
            if(bundle.getString("channelId") != null) {
                String channelId = bundle.getString("channelId");
                String season = bundle.getString("season");
                String raceName = bundle.getString("raceName");
                String circuitId = bundle.getString("circuitId");
                String title = bundle.getString("title");
                String body = bundle.getString("body");
                createNotificationChannel(context, channelId);
                showNotification(context, season, raceName, circuitId, channelId, title, body);
            }
        }
    }

    private void showNotification(Context context, String season, String raceName, String circuitId, String channelId, String title, String body){
        Intent notificationIntent = new Intent(context, splashNotificationActivity.class);
        Bundle bundle = new Bundle();
        FirebaseApp.initializeApp(context);
        DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference();
        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference storageRef = storage.getReference();
        rootRef.child("/schedule/season/" + season + "/" + raceName).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String dateStart_string = snapshot.child("FirstPractice").child("firstPracticeDate").getValue(String.class);
                String dateEnd_string = snapshot.child("raceDate").getValue(String.class);
                Integer round = snapshot.child("round").getValue(Integer.class);


                DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-M-d");
                LocalDate dateStart = LocalDate.parse(dateStart_string, dateFormatter);
                String dayStart = dateStart.format(DateTimeFormatter.ofPattern("dd")).toString();

                LocalDate dateEnd = LocalDate.parse(dateEnd_string, dateFormatter);
                String dayEnd = dateEnd.format(DateTimeFormatter.ofPattern("dd")).toString();

                String monthStart = dateStart.format(DateTimeFormatter.ofPattern("MMM")).toString();
                String monthEnd = dateEnd.format(DateTimeFormatter.ofPattern("MMM")).toString();

                rootRef.child("circuits/" + circuitId)
                        .addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                String raceCountry = snapshot.child("country").getValue(String.class);

                                bundle.putString("raceName" , raceName);
                                bundle.putString("futureRaceStartDay" , dayStart);
                                bundle.putString("futureRaceEndDay" , dayEnd);
                                bundle.putString("futureRaceStartMonth" , monthStart);
                                bundle.putString("futureRaceEndMonth" , monthEnd);
                                bundle.putString("roundCount" , String.valueOf(round));
                                bundle.putString("raceCountry" , raceCountry);
                                bundle.putString("circuitId", circuitId);
                                bundle.putString("dateStart", dateStart_string);
                                bundle.putBoolean("fromNotify", true);
                                notificationIntent.putExtras(bundle);

                                notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                                PendingIntent intent2 = PendingIntent.getActivity(context, 1, notificationIntent, PendingIntent.FLAG_IMMUTABLE);

                                StorageReference mCircuits = storageRef.child("circuits/" + circuitId + ".png");

                                GlideApp.with(context).asBitmap().load(mCircuits).into(new CustomTarget<Bitmap>() {
                                    @Override
                                    public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                                        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channelId)
                                                .setSmallIcon(R.mipmap.ic_launcher)
                                                .setLargeIcon(resource)
                                                .setContentTitle(title)
                                                .setContentIntent(intent2)
                                                .setContentText(body)
                                                .setAutoCancel(true)
                                                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

                                        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                                        notificationManager.notify(1, builder.build());
                                    }

                                    @Override
                                    public void onLoadCleared(@Nullable Drawable placeholder) {
                                    }
                                });
                            }
                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {

                            }
                        });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void createNotificationChannel(final Context context, String mChannelId) {
        String name = "Schedule Alerts";
        String des = "Race schedule alerts for user";
        int importance = NotificationManager.IMPORTANCE_DEFAULT;
        NotificationChannel channel = new NotificationChannel(mChannelId, name, importance);
        channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        channel.setDescription(des);
        NotificationManager manager = (NotificationManager) context.getSystemService(NotificationManager.class);
        manager.createNotificationChannel(channel);
    }
}
