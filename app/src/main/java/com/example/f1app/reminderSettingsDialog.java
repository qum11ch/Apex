package com.example.f1app;

import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.provider.Settings;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.appcompat.widget.SwitchCompat;
import androidx.core.content.ContextCompat;

final class reminderSettingsDialog {

    private reminderSettingsDialog() {
    }

    static void show(Activity activity) {
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

        for (int minutes : reminderSettings.OPTIONS) {
            RadioButton option = new RadioButton(activity);
            option.setId(View.generateViewId());
            option.setTag(minutes);
            option.setText(activity.getResources()
                    .getQuantityString(R.plurals.reminder_option, minutes, minutes));
            option.setTextColor(ContextCompat.getColor(activity, R.color.text_color_main));
            group.addView(option);

            if (minutes == wasLead) {
                group.check(option.getId());
            }
        }

        toggle.setChecked(wasEnabled);
        applyEnabled(group, label, wasEnabled);

        toggle.setOnCheckedChangeListener((button, checked) -> {
            applyEnabled(group, label, checked);

            if (checked && !hasNotificationPermission(activity)) {
                openSystemNotificationSettings(activity);
            }
        });

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
        });

        dialog.show();
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