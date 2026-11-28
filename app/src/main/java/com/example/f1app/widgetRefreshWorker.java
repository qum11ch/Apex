package com.example.f1app;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.concurrent.futures.CallbackToFutureAdapter;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.ExistingWorkPolicy;
import androidx.work.ListenableWorker;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.WorkerParameters;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/** Refreshes the data of the widgets in the background, only while a widget exists. */
public class widgetRefreshWorker extends ListenableWorker {

    private static final String PERIODIC = "widget_refresh_periodic";
    private static final String NOW = "widget_refresh_now";
    private static final long GIVE_UP_MS = 45_000L;

    public widgetRefreshWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public ListenableFuture<Result> startWork() {
        final Context app = getApplicationContext();

        return CallbackToFutureAdapter.getFuture(completer -> {
            Handler main = new Handler(Looper.getMainLooper());
            AtomicBoolean finished = new AtomicBoolean(false);

            Runnable giveUp = () -> {
                if (finished.compareAndSet(false, true)) {
                    completer.set(Result.retry());
                }
            };

            main.postDelayed(giveUp, GIVE_UP_MS);

            // the builder works on the main thread, like the rest of the loaders of the app
            main.post(() -> widgetSnapshotBuilder.refresh(app, null, false, () -> {
                if (finished.compareAndSet(false, true)) {
                    main.removeCallbacks(giveUp);
                    widgetRegistry.updateAll(app);
                    completer.set(Result.success());
                }
            }));

            return "widget refresh";
        });
    }

    static void schedule(Context context) {
        PeriodicWorkRequest request = new PeriodicWorkRequest.Builder(
                widgetRefreshWorker.class, 3, TimeUnit.HOURS, 30, TimeUnit.MINUTES)
                .setConstraints(network())
                .build();

        WorkManager.getInstance(context.getApplicationContext())
                .enqueueUniquePeriodicWork(PERIODIC, ExistingPeriodicWorkPolicy.KEEP, request);
    }

    static void runNow(Context context) {
        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(widgetRefreshWorker.class)
                .setConstraints(network())
                .build();

        WorkManager.getInstance(context.getApplicationContext())
                .enqueueUniqueWork(NOW, ExistingWorkPolicy.KEEP, request);
    }

    static void cancel(Context context) {
        WorkManager.getInstance(context.getApplicationContext()).cancelUniqueWork(PERIODIC);
    }

    private static Constraints network() {
        return new Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build();
    }
}