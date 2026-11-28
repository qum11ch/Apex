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
        themeStore.apply(this);
        com.google.firebase.auth.FirebaseAuth.getInstance()
                .addAuthStateListener(auth -> {
                    widgetFavoriteDriver.updateAll(getApplicationContext());

                    if (widgetFavoriteDriver.exists(getApplicationContext())) {
                        widgetRefreshWorker.runNow(getApplicationContext());
                    }
                });
    }

    static Context context() {
        return appContext;
    }
}
