package com.example.f1app;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.core.content.pm.PackageInfoCompat;

import com.google.firebase.database.FirebaseDatabase;

public final class neuroAccess {
    private neuroAccess() {}

    public static void open(Activity activity, String event, String gpName, String season) {
        open(activity, event, gpName, season, null);
    }

    public static void open(Activity activity, String event, String gpName, String season, Runnable onDone) {
        if (activity == null || event == null || gpName == null || season == null) {
            if (onDone != null) onDone.run();
            return;
        }

        FirebaseDatabase.getInstance()
                .getReference("appConfig/android/neuroPredictionMinVersionCode")
                .get()
                .addOnCompleteListener(task -> {
                    if (onDone != null) onDone.run();

                    if (activity.isFinishing() || activity.isDestroyed()) return;

                    if (!task.isSuccessful()) {
                        android.util.Log.w("neuroAccess", "version check failed", task.getException());
                        Toast.makeText(activity, R.string.update_reqired_version_task_error, Toast.LENGTH_LONG).show();
                        return;
                    }

                    Object raw = task.getResult().getValue();
                    if (!(raw instanceof Number) || ((Number) raw).longValue() < 1) {
                        Toast.makeText(activity, R.string.neuro_prediction_access_blocked, Toast.LENGTH_LONG).show();
                        return;
                    }

                    try {
                        PackageInfo info = activity.getPackageManager()
                                .getPackageInfo(activity.getPackageName(), 0);
                        long installed = PackageInfoCompat.getLongVersionCode(info);

                        if (installed < ((Number) raw).longValue()) {
                            showUpdateRequiredDialog(activity);
                            return;
                        }

                        Intent intent = new Intent(activity, neuroPredictionResultActivity.class);
                        intent.putExtra("event", event);
                        intent.putExtra("gp", gpName);
                        intent.putExtra("currentSeason", season);
                        activity.startActivity(intent);
                    } catch (Exception e) {
                        Toast.makeText(activity, R.string.update_required_version_error, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private static void showUpdateRequiredDialog(Activity activity) {
        if (activity.isFinishing() || activity.isDestroyed()) return;

        View dialogView = activity.getLayoutInflater().inflate(R.layout.dialog_update_required, null);

        AlertDialog dialog = new AlertDialog.Builder(activity)
                .setView(dialogView)
                .create();

        Button updateButton = dialogView.findViewById(R.id.updateButton);
        Button stayButton = dialogView.findViewById(R.id.stayButton);

        updateButton.setOnClickListener(v -> {
            Intent intent = new Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://www.rustore.ru/catalog/app/" + activity.getPackageName())
            );

            try {
                activity.startActivity(intent);
                dialog.dismiss();
            } catch (ActivityNotFoundException e) {
                Toast.makeText(activity, activity.getString(R.string.update_required_url_error), Toast.LENGTH_LONG).show();
            }
        });

        stayButton.setOnClickListener(v -> dialog.dismiss());

        dialog.setCancelable(true);
        dialog.setCanceledOnTouchOutside(false);
        dialog.show();

        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

            int horizontalMargin = Math.round(24 * activity.getResources().getDisplayMetrics().density);
            int screenWidth = activity.getResources().getDisplayMetrics().widthPixels;

            window.setLayout(screenWidth - horizontalMargin * 2, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }
}