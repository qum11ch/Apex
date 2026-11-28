package com.example.f1app;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import androidx.core.app.TaskStackBuilder;

final class widgetNavigation {

    private widgetNavigation() {
    }

    static PendingIntent open(Context context, Intent destination,
                              String route, String season) {
        Intent home = new Intent(context, MainActivity.class);

        if (season != null && !season.trim().isEmpty()) {
            home.putExtra("currentSeason", season);
        }

        Intent target = new Intent(destination);

        // Flags are controlled by TaskStackBuilder, not the destination.
        target.setFlags(0);
        target.putExtra("fromWidget", true);
        target.setData(new Uri.Builder()
                .scheme("apex-widget")
                .authority("navigation")
                .appendPath(route)
                .build());

        return TaskStackBuilder.create(context)
                .addNextIntent(home)
                .addNextIntent(target)
                .getPendingIntent(
                        141,
                        PendingIntent.FLAG_UPDATE_CURRENT
                                | PendingIntent.FLAG_IMMUTABLE
                );
    }
}