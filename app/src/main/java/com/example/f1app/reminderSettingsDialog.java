package com.example.f1app;

import android.Manifest;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.net.Uri;
import android.os.Build;
import android.os.PowerManager;
import android.provider.Settings;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.appcompat.widget.SwitchCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.ColorUtils;
import androidx.core.widget.CompoundButtonCompat;

final class reminderSettingsDialog {

    private reminderSettingsDialog() {
    }

    private static void requestExactAlarmAccess(Activity activity) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            return;
        }

        AlarmManager alarms = (AlarmManager) activity.getSystemService(Activity.ALARM_SERVICE);

        if (alarms.canScheduleExactAlarms()) {
            return;
        }

        Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                Uri.parse("package:" + activity.getPackageName()));
        activity.startActivity(intent);
    }

    static void show(Activity activity, Runnable onDismiss) {
        Dialog dialog = new Dialog(activity);
        dialog.setContentView(R.layout.dialog_reminder_settings);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            dialog.getWindow().setBackgroundDrawable(
                    activity.getDrawable(R.drawable.custom_dialog_bg));
        }

        SwitchCompat toggle = dialog.findViewById(R.id.reminderSwitch);
        TextView label = dialog.findViewById(R.id.reminderLeadLabel);
        RadioGroup group = dialog.findViewById(R.id.reminderLeadGroup);

        final boolean wasEnabled = reminderSettings.enabled(activity);
        final int wasLead = reminderSettings.leadMinutes(activity);

        int accent = ContextCompat.getColor(activity, R.color.hero_accent);
        int idle = ContextCompat.getColor(activity, R.color.grey);

        int[][] states = {{android.R.attr.state_checked}, {}};

        ColorStateList thumbTint = new ColorStateList(states, new int[]{accent, idle});
        ColorStateList trackTint = new ColorStateList(states, new int[]{
                ColorUtils.setAlphaComponent(accent, 90),
                ColorUtils.setAlphaComponent(idle, 90)});

        toggle.setThumbTintList(thumbTint);
        toggle.setTrackTintList(trackTint);

        for (int minutes : reminderSettings.OPTIONS) {
            RadioButton option = new RadioButton(activity);
            option.setId(View.generateViewId());
            option.setTag(minutes);
            option.setText(activity.getResources()
                    .getQuantityString(R.plurals.reminder_option, minutes, minutes));
            option.setTextColor(ContextCompat.getColor(activity, R.color.text_color_main));
            CompoundButtonCompat.setButtonTintList(option, thumbTint);
            group.addView(option);

            if (minutes == wasLead) {
                group.check(option.getId());
            }
        }

        toggle.setChecked(wasEnabled);
        applyEnabled(group, label, wasEnabled);

        toggle.setOnCheckedChangeListener((button, checked) -> {
            applyEnabled(group, label, checked);

            if (checked) {
                if (!hasNotificationPermission(activity)) {
                    openSystemNotificationSettings(activity);
                } else {
                    requestExactAlarmAccess(activity);
                }
            }
        });

        SwitchCompat alerts = dialog.findViewById(R.id.predictionAlertsSwitch);
        final boolean wasAlerts = reminderSettings.predictionAlertsEnabled(activity);

        alerts.setChecked(wasAlerts);
        alerts.setThumbTintList(thumbTint);
        alerts.setTrackTintList(trackTint);
        alerts.setOnCheckedChangeListener((button, checked) -> {
            if (checked && !hasNotificationPermission(activity)) {
                openSystemNotificationSettings(activity);
            }
        });

        TextView batteryHint = dialog.findViewById(R.id.reminderBatteryHint);
        PowerManager power = (PowerManager) activity.getSystemService(Context.POWER_SERVICE);

        if (power != null && power.isIgnoringBatteryOptimizations(activity.getPackageName())) {
            batteryHint.setVisibility(View.GONE);
        } else {
            batteryHint.setOnClickListener(v -> openBatterySettings(activity));
        }

        dialog.setOnDismissListener(d -> {
            boolean enabled = toggle.isChecked();
            int lead = wasLead;
            View selected = dialog.findViewById(group.getCheckedRadioButtonId());

            if (selected != null && selected.getTag() instanceof Integer) {
                lead = (Integer) selected.getTag();
            }

            if (enabled != wasEnabled || lead != wasLead) {
                reminderSettings.save(activity, enabled, lead);
                reminderSettings.reschedule(activity);
            }

            if (alerts.isChecked() != wasAlerts) {
                reminderSettings.savePredictionAlerts(activity, alerts.isChecked());
                predictionWatch.apply(activity);
            }

            if (onDismiss != null) {
                onDismiss.run();
            }
        });

        dialog.show();
    }

    private static void openBatterySettings(Activity activity) {
        try {
            activity.startActivity(new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS));
        } catch (ActivityNotFoundException error) {
            try {
                activity.startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.parse("package:" + activity.getPackageName())));
            } catch (ActivityNotFoundException ignored) {
                // this device has neither screen
            }
        }
    }

    private static void applyEnabled(RadioGroup group, TextView label, boolean enabled) {
        label.setAlpha(enabled ? 1f : 0.4f);
        group.setAlpha(enabled ? 1f : 0.4f);

        for (int i = 0; i < group.getChildCount(); i++) {
            group.getChildAt(i).setEnabled(enabled);
        }
    }

    private static boolean hasNotificationPermission(Activity activity) {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
                || ContextCompat.checkSelfPermission(
                activity, Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED;
    }

    private static void openSystemNotificationSettings(Activity activity) {
        Intent intent = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
        intent.putExtra(Settings.EXTRA_APP_PACKAGE, activity.getPackageName());
        activity.startActivity(intent);
    }
}