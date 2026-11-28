package com.example.f1app;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.os.Bundle;

public class favoriteDriverWidgetProvider extends AppWidgetProvider {

    @Override
    public void onEnabled(Context context) {
        widgetRefreshWorker.schedule(context);
        widgetRefreshWorker.runNow(context);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager manager, int[] ids) {
        for (int id : ids) {
            widgetFavoriteDriver.update(context, manager, id);
        }

        widgetRefreshWorker.schedule(context);
        widgetRefreshWorker.runNow(context);
    }

    @Override
    public void onAppWidgetOptionsChanged(Context context,
                                          AppWidgetManager manager,
                                          int id, Bundle options) {
        widgetFavoriteDriver.update(context, manager, id);
    }

    @Override
    public void onDisabled(Context context) {
        if (!widgetRegistry.hasAny(context)) {
            widgetRefreshWorker.cancel(context);
        }
    }
}