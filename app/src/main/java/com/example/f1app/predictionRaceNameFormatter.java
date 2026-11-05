package com.example.f1app;

import static com.example.f1app.MainActivity.getStringByName;

import android.content.Context;

import java.util.Locale;

public final class predictionRaceNameFormatter {
    private predictionRaceNameFormatter() {
    }

    public static String getName(Context context, String raceName) {
        if (raceName == null || raceName.trim().isEmpty()) {
            return "—";
        }

        String key = raceName.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", "_");
        int resourceId = getStringByName(key + "_text");

        if (resourceId == 0) {
            return raceName;
        }

        return context.getString(resourceId);
    }

    public static String getNameWithSeason(Context context, String raceName, String season) {
        String name = getName(context, raceName);

        if (season == null || season.trim().isEmpty()) {
            return name;
        }

        return name + " " + season;
    }
}