package com.example.f1app;

import android.content.Context;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;

public final class userPredictionRules {

    private userPredictionRules() {
    }

    public static void showIfNeeded(AppCompatActivity activity) {
        boolean seen = activity
                .getSharedPreferences(
                        userPredictionRulesDialog.PREFS,
                        Context.MODE_PRIVATE
                )
                .getBoolean(
                        userPredictionRulesDialog.SEEN,
                        false
                );

        if (!seen) {
            show(activity);
        }
    }

    public static void show(AppCompatActivity activity) {
        if (activity.isFinishing() || activity.isDestroyed()) {
            return;
        }

        FragmentManager manager =
                activity.getSupportFragmentManager();

        if (manager.isStateSaved()) {
            return;
        }

        if (manager.findFragmentByTag(
                userPredictionRulesDialog.TAG
        ) != null) {
            return;
        }

        new userPredictionRulesDialog().showNow(
                manager,
                userPredictionRulesDialog.TAG
        );
    }
}