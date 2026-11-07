package com.example.f1app;

import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

public final class AwardsPreview {
    private AwardsPreview() {}

    public static void show(AppCompatActivity activity) {
        if ((activity.getApplicationInfo().flags
                & android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) == 0) return;
        if (activity.isFinishing() || activity.isDestroyed()) return;

        LinearLayout content = new LinearLayout(activity);
        content.setOrientation(LinearLayout.VERTICAL);
        int padding = dp(activity, 16);
        content.setPadding(padding, padding, padding, padding);
        content.setBackgroundColor(ContextCompat.getColor(activity, R.color.colorPrimary));
        android.widget.ScrollView scroll = new android.widget.ScrollView(activity);
        scroll.addView(content);

        TextView explanation = new TextView(activity);
        explanation.setTextColor(ContextCompat.getColor(activity, R.color.neuro_secondary));
        content.addView(explanation);

        LinearLayout fans = row(activity, content);

        AwardIconView driver = item(
                activity,
                fans,
                R.string.awards_fan_driver
        );

        driver.showDriver(
                "",
                activity.getString(R.string.awards_fan_driver)
        );

        loadFavoriteDriverPreview(activity, driver);

        AwardIconView team = item(
                activity,
                fans,
                R.string.awards_fan_team
        );

        team.showTeam(
                null,
                "",
                activity.getString(R.string.awards_fan_team)
        );

        loadFavoriteTeamPreview(activity, team);

        LinearLayout medals = row(activity, content);
        item(activity, medals, R.string.awards_first_correct).showMedal(
                R.drawable.pit_pass_first_correct, activity.getString(R.string.awards_first_correct));
        item(activity, medals, R.string.awards_correct_10).showMedal(
                R.drawable.pit_pass_correct_10_v2, activity.getString(R.string.awards_correct_10));
        item(activity, medals, R.string.awards_preview_member).showMedal(
                R.drawable.pit_pass_season_participant_2026, activity.getString(R.string.awards_preview_member));

        LinearLayout medals2 = row(activity, content);
        item(activity, medals2, R.string.awards_season_place_1).showMedal(
                R.drawable.pit_pass_season_place_1, activity.getString(R.string.awards_season_place_1));
        item(activity, medals2, R.string.awards_season_place_2).showMedal(
                R.drawable.pit_pass_season_place_2, activity.getString(R.string.awards_season_place_2));
        item(activity, medals2, R.string.awards_season_place_3).showMedal(
                R.drawable.pit_pass_season_place_3, activity.getString(R.string.awards_season_place_3));

        LinearLayout medals3 = row(activity, content);
        item(activity, medals3, R.string.awards_correct_10).showMedal(
                R.drawable.pit_pass_correct_10_v2, activity.getString(R.string.awards_correct_10));
        item(activity, medals3, R.string.awards_correct_50).showMedal(
                R.drawable.pit_pass_correct_50, activity.getString(R.string.awards_correct_50));
        item(activity, medals3, R.string.awards_correct_100).showMedal(
                R.drawable.pit_pass_correct_100, activity.getString(R.string.awards_correct_100));

        LinearLayout medals4 = row(activity, content);
        item(activity, medals4, R.string.awards_member_1_year).showMedal(
                R.drawable.pit_pass_one_year, activity.getString(R.string.awards_member_1_year));
        item(activity, medals4, R.string.awards_member_3_year).showMedal(
                R.drawable.pit_pass_three_years, activity.getString(R.string.awards_member_3_year));
        item(activity, medals4, R.string.awards_member_5_year).showMedal(
                R.drawable.pit_pass_five_years, activity.getString(R.string.awards_member_5_year));

        LinearLayout sizes = row(activity, content);
        for (int size : new int[]{24, 32, 48}) {
            LinearLayout cell = new LinearLayout(activity);
            cell.setOrientation(LinearLayout.VERTICAL);
            cell.setGravity(Gravity.CENTER);
            sizes.addView(cell, new LinearLayout.LayoutParams(0, -2, 1f));
            AwardIconView badge = new AwardIconView(activity);
            badge.showMedal(R.drawable.pit_pass_correct_50,
                    activity.getString(R.string.awards_correct_50));
            cell.addView(badge, new LinearLayout.LayoutParams(dp(activity, size), dp(activity, size)));
            TextView label = new TextView(activity);
            label.setText(activity.getString(R.string.awards_preview_size, size));
            label.setTextColor(ContextCompat.getColor(activity, R.color.neuro_secondary));
            cell.addView(label);
        }

        AlertDialog dialog = new AlertDialog.Builder(activity)
                .setTitle(R.string.awards_preview_title)
                .setView(scroll)
                .setPositiveButton(android.R.string.ok, null)
                .create();
        dialog.setOnDismissListener(ignored -> clear(content));
        dialog.show();
    }

    private static LinearLayout row(AppCompatActivity activity, LinearLayout parent) {
        LinearLayout row = new LinearLayout(activity);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(activity, 18), 0, dp(activity, 8));
        row.setBaselineAligned(false);
        parent.addView(row, new LinearLayout.LayoutParams(-1, -2));
        return row;
    }

    private static void loadFavoriteTeamPreview(
            AppCompatActivity activity,
            AwardIconView badge
    ) {
        com.google.firebase.auth.FirebaseUser user =
                com.google.firebase.auth.FirebaseAuth
                        .getInstance()
                        .getCurrentUser();

        if (user == null) return;

        final String uid = user.getUid();

        com.google.firebase.database.FirebaseDatabase.getInstance()
                .getReference("users")
                .child(uid)
                .child("choiceTeam")
                .get()
                .addOnSuccessListener(choiceSnapshot -> {
                    if (!previewActive(activity, badge, uid)) return;

                    Object raw = choiceSnapshot.getValue();

                    if (!(raw instanceof String)) {
                        showPreviewMessage(
                                activity,
                                R.string.public_favorite_team_empty
                        );
                        return;
                    }

                    String teamName = ((String) raw).trim();

                    if (teamName.isEmpty()
                            || "null".equalsIgnoreCase(teamName)) {
                        showPreviewMessage(
                                activity,
                                R.string.public_favorite_team_empty
                        );
                        return;
                    }

                    com.google.firebase.database.FirebaseDatabase
                            .getInstance()
                            .getReference("constructors")
                            .orderByChild("name")
                            .equalTo(teamName)
                            .get()
                            .addOnSuccessListener(teamsSnapshot -> {
                                if (!previewActive(
                                        activity, badge, uid
                                )) return;

                                String teamId = null;

                                for (com.google.firebase.database.DataSnapshot
                                        child : teamsSnapshot.getChildren()) {

                                    Object value = child
                                            .child("constructorId")
                                            .getValue();

                                    if (value instanceof String
                                            && !((String) value)
                                            .trim().isEmpty()) {
                                        teamId = ((String) value)
                                                .trim()
                                                .toLowerCase(
                                                        java.util.Locale.ROOT
                                                );
                                        break;
                                    }
                                }

                                if (teamId == null) {
                                    showPreviewMessage(
                                            activity,
                                            R.string.public_favorite_load_error
                                    );
                                    return;
                                }

                                String suffix;

                                switch (teamId) {
                                    case "alpine":
                                    case "williams":
                                    case "cadillac":
                                    case "audi":
                                        suffix = "_logo_alt.png";
                                        break;

                                    default:
                                        suffix = "_logo.png";
                                }

                                String imagePath =
                                        "teams/" + teamId + suffix;

                                com.google.firebase.storage.FirebaseStorage
                                        .getInstance()
                                        .getReference()
                                        .child(imagePath)
                                        .getDownloadUrl()
                                        .addOnSuccessListener(uri -> {
                                            if (!previewActive(
                                                    activity, badge, uid
                                            )) return;

                                            String fallback = teamName
                                                    .substring(
                                                            0,
                                                            teamName
                                                                    .offsetByCodePoints(
                                                                            0, 1
                                                                    )
                                                    )
                                                    .toUpperCase(
                                                            java.util.Locale.ROOT
                                                    );

                                            badge.showTeam(
                                                    uri,
                                                    fallback,
                                                    activity.getString(
                                                            R.string.awards_fan_team
                                                    ) + ": " + teamName
                                            );
                                        })
                                        .addOnFailureListener(error -> {
                                            if (!previewActive(
                                                    activity, badge, uid
                                            )) return;

                                            android.util.Log.w(
                                                    "AwardsPreview",
                                                    "Logo unavailable: "
                                                            + imagePath,
                                                    error
                                            );

                                            showPreviewMessage(
                                                    activity,
                                                    R.string.public_favorite_image_unavailable
                                            );
                                        });
                            })
                            .addOnFailureListener(error -> {
                                if (!previewActive(
                                        activity, badge, uid
                                )) return;

                                android.util.Log.w(
                                        "AwardsPreview",
                                        "Team lookup failed",
                                        error
                                );

                                showPreviewMessage(
                                        activity,
                                        R.string.public_favorite_load_error
                                );
                            });
                })
                .addOnFailureListener(error -> {
                    if (!previewActive(activity, badge, uid)) return;

                    android.util.Log.w(
                            "AwardsPreview",
                            "Favorite team lookup failed",
                            error
                    );

                    showPreviewMessage(
                            activity,
                            R.string.public_favorite_load_error
                    );
                });
    }

    private static void loadFavoriteDriverPreview(
            AppCompatActivity activity,
            AwardIconView badge
    ) {
        com.google.firebase.auth.FirebaseUser user =
                com.google.firebase.auth.FirebaseAuth
                        .getInstance()
                        .getCurrentUser();

        if (user == null) return;

        final String uid = user.getUid();

        com.google.firebase.database.FirebaseDatabase.getInstance()
                .getReference("users")
                .child(uid)
                .child("choiceDriver")
                .get()
                .addOnSuccessListener(choiceSnapshot -> {
                    if (!previewActive(activity, badge, uid)) return;

                    Object raw = choiceSnapshot.getValue();

                    if (!(raw instanceof String)) {
                        showPreviewMessage(
                                activity,
                                R.string.public_favorite_driver_empty
                        );
                        return;
                    }

                    String driverName = ((String) raw).trim();

                    if (driverName.isEmpty()
                            || "null".equalsIgnoreCase(driverName)) {
                        showPreviewMessage(
                                activity,
                                R.string.public_favorite_driver_empty
                        );
                        return;
                    }

                    com.google.firebase.database.FirebaseDatabase
                            .getInstance()
                            .getReference("drivers")
                            .child(driverName)
                            .child("driversCode")
                            .get()
                            .addOnSuccessListener(codeSnapshot -> {
                                if (!previewActive(
                                        activity, badge, uid
                                )) return;

                                Object codeValue = codeSnapshot.getValue();

                                if (!(codeValue instanceof String)
                                        || ((String) codeValue)
                                        .trim().isEmpty()) {

                                    android.util.Log.w(
                                            "AwardsPreview",
                                            "Driver code missing: "
                                                    + driverName
                                    );

                                    showPreviewMessage(
                                            activity,
                                            R.string.public_favorite_load_error
                                    );
                                    return;
                                }

                                String driverCode =
                                        ((String) codeValue)
                                                .trim()
                                                .toUpperCase(
                                                        java.util.Locale.ROOT
                                                );

                                badge.showDriver(
                                        driverCode,
                                        activity.getString(
                                                R.string.awards_fan_driver
                                        ) + ": " + driverName
                                );
                            })
                            .addOnFailureListener(error -> {
                                if (!previewActive(
                                        activity, badge, uid
                                )) return;

                                android.util.Log.w(
                                        "AwardsPreview",
                                        "Driver code lookup failed: "
                                                + driverName,
                                        error
                                );

                                showPreviewMessage(
                                        activity,
                                        R.string.public_favorite_load_error
                                );
                            });
                })
                .addOnFailureListener(error -> {
                    if (!previewActive(activity, badge, uid)) return;

                    android.util.Log.w(
                            "AwardsPreview",
                            "Favorite driver lookup failed",
                            error
                    );

                    showPreviewMessage(
                            activity,
                            R.string.public_favorite_load_error
                    );
                });
    }

    private static boolean previewActive(
            AppCompatActivity activity,
            AwardIconView badge,
            String uid
    ) {
        com.google.firebase.auth.FirebaseUser current =
                com.google.firebase.auth.FirebaseAuth
                        .getInstance()
                        .getCurrentUser();

        return !activity.isFinishing()
                && !activity.isDestroyed()
                && badge.isAttachedToWindow()
                && current != null
                && uid.equals(current.getUid());
    }

    private static void showPreviewMessage(
            AppCompatActivity activity,
            int stringId
    ) {
        android.widget.Toast.makeText(
                activity,
                stringId,
                android.widget.Toast.LENGTH_SHORT
        ).show();
    }

    private static AwardIconView item(AppCompatActivity activity, LinearLayout row, int labelRes) {
        LinearLayout cell = new LinearLayout(activity);
        cell.setOrientation(LinearLayout.VERTICAL);
        cell.setGravity(Gravity.CENTER_HORIZONTAL);
        cell.setPadding(dp(activity, 4), 0, dp(activity, 4), 0);
        row.addView(cell, new LinearLayout.LayoutParams(0, -2, 1f));
        AwardIconView badge = new AwardIconView(activity);
        cell.addView(badge, new LinearLayout.LayoutParams(dp(activity, 64), dp(activity, 64)));
        TextView label = new TextView(activity);
        label.setText(labelRes);
        label.setTextSize(12);
        label.setGravity(Gravity.CENTER);
        label.setTextColor(ContextCompat.getColor(activity, R.color.text_color_main));
        cell.addView(label, new LinearLayout.LayoutParams(-1, -2));
        return badge;
    }

    private static void clear(android.view.View view) {
        if (view instanceof AwardIconView) {
            ((AwardIconView) view).clear();
        } else if (view instanceof android.view.ViewGroup) {
            android.view.ViewGroup group = (android.view.ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) clear(group.getChildAt(i));
        }
    }

    private static int dp(AppCompatActivity activity, int value) {
        return Math.round(value * activity.getResources().getDisplayMetrics().density);
    }
}
