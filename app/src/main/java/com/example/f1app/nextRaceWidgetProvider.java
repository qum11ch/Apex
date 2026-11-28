package com.example.f1app;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

/** Draws from the stored snapshot only: no network here. */
public class nextRaceWidgetProvider extends AppWidgetProvider {

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);

        if (widgetNextRace.ACTION_TICK.equals(intent.getAction())) {
            widgetNextRace.updateAll(context);
        }
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
    }
}