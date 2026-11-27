package com.example.f1app;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Toast;

import com.google.android.material.snackbar.Snackbar;

import ru.rustore.sdk.appupdate.listener.InstallStateUpdateListener;
import ru.rustore.sdk.appupdate.manager.RuStoreAppUpdateManager;
import ru.rustore.sdk.appupdate.manager.factory.RuStoreAppUpdateManagerFactory;
import ru.rustore.sdk.appupdate.model.AppUpdateInfo;
import ru.rustore.sdk.appupdate.model.AppUpdateOptions;
import ru.rustore.sdk.appupdate.model.AppUpdateType;
import ru.rustore.sdk.appupdate.model.InstallStatus;
import ru.rustore.sdk.appupdate.model.UpdateAvailability;

import android.util.Log;

/** Updates the app through RuStore; opens the store page when that is not possible. */
final class appUpdater {
    private static final String TAG = "AppUpdater";
    private static final String STORE_URL = "https://www.rustore.ru/catalog/app/";

    private appUpdater() {
    }

    static void start(Activity activity) {
        Log.d(TAG, "start");

        RuStoreAppUpdateManager manager =
                RuStoreAppUpdateManagerFactory.INSTANCE.create(activity.getApplicationContext());

        final boolean[] answered = {false};
        final Handler handler = new Handler(Looper.getMainLooper());

        final Runnable timeout = () -> {
            if (!answered[0]) {
                answered[0] = true;
                Log.w(TAG, "RuStore did not answer in time");
                openStore(activity);
            }
        };

        handler.postDelayed(timeout, 4000);

        manager.getAppUpdateInfo()
                .addOnSuccessListener(info -> {
                    if (answered[0]) {
                        return;
                    }

                    answered[0] = true;
                    handler.removeCallbacks(timeout);
                    Log.d(TAG, "availability=" + info.getUpdateAvailability());

                    if (activity.isFinishing() || activity.isDestroyed()) {
                        return;
                    }

                    if (info.getUpdateAvailability() == UpdateAvailability.UPDATE_AVAILABLE) {
                        begin(activity, manager, info);
                    } else {
                        Toast.makeText(activity, R.string.update_latest, Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(error -> {
                    if (answered[0]) {
                        return;
                    }

                    answered[0] = true;
                    handler.removeCallbacks(timeout);
                    Log.w(TAG, "RuStore update check failed", error);

                    if (!activity.isFinishing() && !activity.isDestroyed()) {
                        openStore(activity);
                    }
                });
    }

    private static void begin(Activity activity, RuStoreAppUpdateManager manager,
                              AppUpdateInfo info) {
        final InstallStateUpdateListener[] holder = new InstallStateUpdateListener[1];

        holder[0] = state -> {
            int status = state.getInstallStatus();

            if (status == InstallStatus.DOWNLOADED) {
                manager.unregisterListener(holder[0]);
                activity.runOnUiThread(() -> offerInstall(activity, manager));
            } else if (status == InstallStatus.FAILED) {
                manager.unregisterListener(holder[0]);
            }
        };

        manager.registerListener(holder[0]);

        manager.startUpdateFlow(info, new AppUpdateOptions.Builder().build())
                .addOnSuccessListener(code -> {
                    if (code == Activity.RESULT_CANCELED) {
                        manager.unregisterListener(holder[0]);
                    }
                })
                .addOnFailureListener(error -> {
                    manager.unregisterListener(holder[0]);
                    openStore(activity);
                });
    }

    private static void offerInstall(Activity activity, RuStoreAppUpdateManager manager) {
        if (activity.isFinishing() || activity.isDestroyed()) {
            return;
        }

        View root = activity.findViewById(android.R.id.content);

        Snackbar.make(root, R.string.update_ready, Snackbar.LENGTH_INDEFINITE)
                .setAction(R.string.update_install, v -> manager
                        .completeUpdate(new AppUpdateOptions.Builder()
                                .appUpdateType(AppUpdateType.FLEXIBLE)
                                .build())
                        .addOnFailureListener(error -> openStore(activity)))
                .show();
    }

    private static void openStore(Activity activity) {
        try {
            activity.startActivity(new Intent(Intent.ACTION_VIEW,
                    Uri.parse(STORE_URL + activity.getPackageName())));
        } catch (ActivityNotFoundException error) {
            Log.w(TAG, "Nothing can open the store link", error);
            Toast.makeText(activity, R.string.update_store_error, Toast.LENGTH_LONG).show();
        }
    }
}