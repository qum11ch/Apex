package com.example.f1app;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

/** Draws from the stored snapshot only; the network is the business of the refresh worker. */
public class nextRaceWidgetProvider extends AppWidgetProvider {

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);

        String action = intent.getAction();

        if (widgetNextRace.ACTION_TICK.equals(action)) {
            widgetNextRace.updateAll(context);
        } else if (Intent.ACTION_LOCALE_CHANGED.equals(action)) {
            // the texts of the snapshot are in the old language: it is built again
            widgetSnapshotStore.clear(context);
            widgetNextRace.updateAll(context);
            widgetRefreshWorker.runNow(context);
        } else if (Intent.ACTION_BOOT_COMPLETED.equals(action)) {
            widgetNextRace.updateAll(context);
            widgetRefreshWorker.schedule(context);
        }
    }

    @Override
    public void onEnabled(Context context) {
        widgetRefreshWorker.schedule(context);
        widgetRefreshWorker.runNow(context);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager manager, int[] ids) {
        for (int id : ids) {
            widgetNextRace.update(context, manager, id);
        }
    }

    @Override
    public void onAppWidgetOptionsChanged(Context context, AppWidgetManager manager, int id,
                                          Bundle options) {
        widgetNextRace.update(context, manager, id);
    }

    @Override
    public void onDisabled(Context context) {
        widgetNextRace.cancelTick(context);

        if (!widgetRegistry.hasAny(context)) {
            widgetRefreshWorker.cancel(context);
        }
    }
}