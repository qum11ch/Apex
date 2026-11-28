package com.example.f1app;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;

/** All widgets of the app in one place: who exists, who is redrawn. */
final class widgetRegistry {

    private widgetRegistry() {
    }

    static boolean hasAny(Context context) {
        Context app = context.getApplicationContext();
        AppWidgetManager manager = AppWidgetManager.getInstance(app);

        return manager.getAppWidgetIds(new ComponentName(app, nextRaceWidgetProvider.class)).length > 0;
    }

    static void updateAll(Context context) {
        widgetNextRace.updateAll(context);
    }

    /** Called by the app: does nothing when there is no widget on the screen. */
    static void refreshFromApp(Context context, String season) {
        if (!hasAny(context)) {
            return;
        }

        Context app = context.getApplicationContext();
        widgetSnapshotBuilder.refresh(app, season, false, () -> updateAll(app));
    }
}