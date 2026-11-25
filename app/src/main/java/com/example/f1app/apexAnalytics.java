package com.example.f1app;

import android.content.Context;
import android.os.Bundle;

import com.google.firebase.analytics.FirebaseAnalytics;

final class apexAnalytics {
    private apexAnalytics() {}

    static void themeChanged(Context context, String mode) {
        Bundle params = new Bundle();
        params.putString("mode", mode); // system / light / dark, no personal data
        FirebaseAnalytics.getInstance(context.getApplicationContext())
                .logEvent("theme_changed", params);
    }
}