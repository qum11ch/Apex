package com.example.f1app;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;

import com.google.firebase.auth.FirebaseAuth;

public final class AuthSessionGate {

    private static final String PREFS = "auth_session_gate";

    private static final String KEY_AUTH_SCHEMA =
            "accepted_auth_schema";

    private static final String KEY_RELOGIN_NOTICE =
            "show_relogin_notice";

    // Увеличиваем только когда нужен новый принудительный вход.
    private static final int REQUIRED_AUTH_SCHEMA = 1;

    private AuthSessionGate() {
    }

    public static boolean redirectIfRequired(
            Activity activity,
            String currentSeason) {

        SharedPreferences preferences =
                activity.getSharedPreferences(
                        PREFS,
                        Activity.MODE_PRIVATE
                );

        int acceptedSchema = preferences.getInt(
                KEY_AUTH_SCHEMA,
                0
        );

        if (acceptedSchema >= REQUIRED_AUTH_SCHEMA) {
            return false;
        }

        FirebaseAuth auth = FirebaseAuth.getInstance();

        boolean hadSession = auth.getCurrentUser() != null;

        SharedPreferences.Editor editor = preferences.edit()
                .putInt(
                        KEY_AUTH_SCHEMA,
                        REQUIRED_AUTH_SCHEMA
                );

        if (hadSession) {
            editor.putBoolean(KEY_RELOGIN_NOTICE, true);
        }

        editor.apply();

        // На чистой установке или без авторизации
        // принудительно открывать вход не нужно.
        if (!hadSession) {
            return false;
        }

        auth.signOut();

        Intent intent = new Intent(
                activity,
                logInPageActivity.class
        );

        if (currentSeason != null) {
            intent.putExtra("currentSeason", currentSeason);
        }

        intent.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TASK
        );

        activity.startActivity(intent);
        activity.finish();

        return true;
    }

    public static boolean shouldShowReloginNotice(
            Activity activity) {

        return activity.getSharedPreferences(
                PREFS,
                Activity.MODE_PRIVATE
        ).getBoolean(KEY_RELOGIN_NOTICE, false);
    }

    public static void clearReloginNotice(Activity activity) {
        activity.getSharedPreferences(
                        PREFS,
                        Activity.MODE_PRIVATE
                ).edit()
                .remove(KEY_RELOGIN_NOTICE)
                .apply();
    }
}