package com.example.f1app;

import android.annotation.SuppressLint;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.google.firebase.FirebaseApp;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class NotifyReceiver extends BroadcastReceiver {

    @SuppressLint("UnsafeProtectedBroadcastReceiver")
    @Override
    public void onReceive(Context context, Intent intent) {
        String mCircuitId = intent.getStringExtra("circuitId");
        String mTitle = intent.getStringExtra("title");
        String mBody = intent.getStringExtra("body");
        String mChannelId = intent.getStringExtra("channelId");
        String mSeason = intent.getStringExtra("season");
        String mRaceName = intent.getStringExtra("raceName");

        Intent notificationIntent = new Intent(context, futureRaceActivity.class);
        Bundle bundle = new Bundle();

        FirebaseApp.initializeApp(context);
        DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference();
        rootRef.child("/schedule/season/" + mSeason + "/" + mRaceName).addListenerForSingleValueEvent(new ValueEventListener() {
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

                rootRef.child("circuits/" + mCircuitId)
                        .addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                String raceCountry = snapshot.child("country").getValue(String.class);

                                bundle.putString("raceName" , mRaceName);
                                bundle.putString("futureRaceStartDay" , dayStart);
                                bundle.putString("futureRaceEndDay" , dayEnd);
                                bundle.putString("futureRaceStartMonth" , monthStart);
                                bundle.putString("futureRaceEndMonth" , monthEnd);
                                bundle.putString("roundCount" , String.valueOf(round));
                                bundle.putString("raceCountry" , raceCountry);
                                bundle.putString("circuitId", mCircuitId);
                                bundle.putString("dateStart", dateStart_string);
                                notificationIntent.putExtras(bundle);

                                notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                                PendingIntent intent2 = PendingIntent.getActivity(context, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);

                                int resourceId_driverTeam = context.getResources().getIdentifier(mCircuitId, "drawable",
                                        context.getPackageName());

                                NotificationCompat.Builder builder = new NotificationCompat.Builder(context, mChannelId)
                                        .setSmallIcon(R.mipmap.ic_launcher)
                                        .setLargeIcon(BitmapFactory.decodeResource(context.getResources(),resourceId_driverTeam))
                                        .setContentTitle(mTitle)
                                        .setContentIntent(intent2)
                                        .setContentText(mBody)
                                        .setAutoCancel(true)
                                        .setPriority(NotificationCompat.PRIORITY_DEFAULT);

                                NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                                notificationManager.notify(1, builder.build());
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
}
