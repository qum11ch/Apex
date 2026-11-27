package com.example.f1app;

import android.content.Context;

final class widgetSnapshotStore {

    private static final String PREFS = "widget_snapshot";
    private static final String KEY = "json";

    private widgetSnapshotStore() {
    }

    static widgetSnapshot load(Context context) {
        String raw = context.getApplicationContext()
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, null);

        return raw == null ? null : widgetSnapshot.fromJson(raw);
    }

    static void save(Context context, widgetSnapshot snapshot) {
        context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit().putString(KEY, snapshot.toJson()).apply();
    }

    static void clear(Context context) {
        context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit().remove(KEY).apply();
    }
}