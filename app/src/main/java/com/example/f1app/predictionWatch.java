package com.example.f1app;

import android.content.Context;

import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import java.util.concurrent.TimeUnit;

final class predictionWatch {
    private static final String WORK_NAME = "prediction_watch";

    private predictionWatch() {
    }

    /** Starts or stops the periodic check according to the user's setting. */
    static void apply(Context context) {
        WorkManager manager = WorkManager.getInstance(context.getApplicationContext());

        if (!reminderSettings.predictionAlertsEnabled(context)) {
            manager.cancelUniqueWork(WORK_NAME);
            return;
        }

        PeriodicWorkRequest request = new PeriodicWorkRequest.Builder(
                predictionWatchWorker.class, 4, TimeUnit.HOURS, 1, TimeUnit.HOURS)
                .setConstraints(new Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build())
                .build();

        manager.enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request);
    }
}