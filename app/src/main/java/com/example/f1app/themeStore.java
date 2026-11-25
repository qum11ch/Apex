package com.example.f1app;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;

import androidx.appcompat.app.AppCompatDelegate;

final class themeStore {
    static final String MODE_SYSTEM = "system";
    static final String MODE_LIGHT = "light";
    static final String MODE_DARK = "dark";

    private static final String KEY_THEME_MODE = "theme_mode";

    private themeStore() {}

    static String get(Context context) {
        SharedPreferences prefs = context.getApplicationContext()
                .getSharedPreferences(MainActivity.APP_PREFERENCES, Context.MODE_PRIVATE);
        String mode = prefs.getString(KEY_THEME_MODE, MODE_SYSTEM);
        return MODE_LIGHT.equals(mode) || MODE_DARK.equals(mode) ? mode : MODE_SYSTEM;
    }

    static void apply(Context context) {
        String mode = get(context);
        int delegateMode;
        if (MODE_LIGHT.equals(mode)) {
            delegateMode = AppCompatDelegate.MODE_NIGHT_NO;
        } else if (MODE_DARK.equals(mode)) {
            delegateMode = AppCompatDelegate.MODE_NIGHT_YES;
        } else {
            delegateMode = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
        }
        AppCompatDelegate.setDefaultNightMode(delegateMode);
    }

    /** Saves the choice and applies it; AppCompat recreates the open activities itself. */
    static void set(Context context, String mode) {
        context.getApplicationContext()
                .getSharedPreferences(MainActivity.APP_PREFERENCES, Context.MODE_PRIVATE)
                .edit().putString(KEY_THEME_MODE, mode).apply();
        apply(context);
    }

    /** For plain Activity screens (splash) that AppCompat does not manage. */
    static boolean isDark(Context context) {
        String mode = get(context);
        if (MODE_DARK.equals(mode)) return true;
        if (MODE_LIGHT.equals(mode)) return false;
        int night = Resources.getSystem().getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_MASK;
        return night == Configuration.UI_MODE_NIGHT_YES;
    }
}