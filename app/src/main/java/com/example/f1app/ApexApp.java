package com.example.f1app;

import android.app.Application;
import android.content.Context;

/** Only keeps the application context for the on-disk API cache (see cachedJsonRequest). */
public class ApexApp extends Application {

    private static Context appContext;

    @Override
    public void onCreate() {
        super.onCreate();
        appContext = getApplicationContext();
    }

    static Context context() {
        return appContext;
    }
}
