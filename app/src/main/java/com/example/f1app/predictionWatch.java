package com.example.f1app;

import static com.example.f1app.MainActivity.APP_PREFERENCES;

import android.content.Context;

import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import java.util.HashSet;
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

    /** Debug: forget what was seen and run one check at once, even at night. */
    static void debugRun(Context context) {
        Context app = context.getApplicationContext();

        app.getSharedPreferences(APP_PREFERENCES, Context.MODE_PRIVATE).edit()
                .putStringSet(predictionWatchWorker.KEY_SEEN, new HashSet<>())
                .apply();

        WorkManager.getInstance(app).enqueue(new OneTimeWorkRequest.Builder(
                predictionWatchWorker.class)
                .setInputData(new Data.Builder().putBoolean("force", true).build())
                .build());
    }
}