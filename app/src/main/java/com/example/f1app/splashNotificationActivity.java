package com.example.f1app;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

public class splashNotificationActivity extends Activity {

    @Override
    public void onStart() {
        super.onStart();

        setContentView(R.layout.splash_screen);
        new Handler().postDelayed(new Runnable() {
            public void run() {
                Intent notificationIntent = new Intent(splashNotificationActivity.this, futureRaceActivity.class);

                Bundle bundle = getIntent().getExtras();

                String mCircuitId = bundle.getString("circuitId");
                String mRaceName = bundle.getString("raceName");
                String mFutureRaceStartDay = bundle.getString("futureRaceStartDay");
                String mFutureRaceEndDay = bundle.getString("futureRaceEndDay");
                String mFutureRaceStartMonth = bundle.getString("futureRaceStartMonth");
                String mFutureRaceEndMonth = bundle.getString("futureRaceEndMonth");
                String mRound = bundle.getString("roundCount");
                String mCountry = bundle.getString("raceCountry");
                String mDate = bundle.getString("dateStart");

                Bundle notificationBundle = new Bundle();
                notificationBundle.putString("raceName" , mRaceName);
                notificationBundle.putString("futureRaceStartDay" , mFutureRaceStartDay);
                notificationBundle.putString("futureRaceEndDay" , mFutureRaceEndDay);
                notificationBundle.putString("futureRaceStartMonth" , mFutureRaceStartMonth);
                notificationBundle.putString("futureRaceEndMonth" , mFutureRaceEndMonth);
                notificationBundle.putString("roundCount" , mRound);
                notificationBundle.putString("raceCountry" , mCountry);
                notificationBundle.putString("circuitId", mCircuitId);
                notificationBundle.putString("dateStart", mDate);
                notificationBundle.putBoolean("fromNotify", true);

                notificationIntent.putExtras(notificationBundle);

                startActivity(notificationIntent);
                overridePendingTransition(0, 0);
            }
        }, 2500);
    }
}