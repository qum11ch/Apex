package com.example.f1app;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Remembers the tiles/list choice separately for every screen.
 * Each screen has its own key, so choosing tiles in one screen never changes another one.
 * A new screen only needs its own constant here.
 */
final class viewModeStore {

    static final String SCREEN_ALL_DRIVERS = "all_drivers";
    static final String SCREEN_SAVED_RACES = "saved_races";

    private static final String PREFS = "view_mode_prefs";

    // Where the drivers screen stored its choice before this store existed.
    private static final String LEGACY_ALL_DRIVERS_PREFS = "all_drivers_prefs";
    private static final String LEGACY_ALL_DRIVERS_KEY = "view_mode";

    private viewModeStore() {
    }

    static String getName(Context context, String screen, String fallbackName) {
        Context app = context.getApplicationContext();
        SharedPreferences prefs = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String stored = prefs.getString(screen, null);

        if (stored != null) {
            return stored;
        }

        if (SCREEN_ALL_DRIVERS.equals(screen)) {
            String legacy = app
                    .getSharedPreferences(LEGACY_ALL_DRIVERS_PREFS, Context.MODE_PRIVATE)
                    .getString(LEGACY_ALL_DRIVERS_KEY, null);

            if (legacy != null) {
                prefs.edit().putString(screen, legacy).apply();
                return legacy;
            }
        }

        return fallbackName;
    }

    static void setName(Context context, String screen, String name) {
        context.getApplicationContext()
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit()
                .putString(screen, name)
                .apply();
    }
}
