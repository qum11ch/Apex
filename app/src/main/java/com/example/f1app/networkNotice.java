package com.example.f1app;

import android.app.Activity;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.SystemClock;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;

import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Replaces the old full-screen "no connection" block.
 * The screen opens as usual (remembered lists / saved API answers fill it), the user gets a short notice,
 * and the screen reloads itself once the network is back.
 */
public final class networkNotice {

    private static final long NOTICE_GAP_MS = 20_000L;
    private static long lastNoticeAt = 0L;

    private networkNotice() {
    }

    public static void showIfOffline(Activity activity) {
        Context app = activity.getApplicationContext();

        if (MainActivity.checkConnection(app)) {
            return;
        }

        notice(app, text(
                "Нет подключения к сети. Показаны сохранённые данные.",
                "No connection. Showing saved data."));

        reloadWhenOnline(activity);
    }

    /** Called when the server could not be reached and a saved answer was used instead. */
    static void showStale() {
        Context app = ApexApp.context();

        if (app != null) {
            notice(app, text(
                    "Сервер недоступен. Показаны сохранённые данные.",
                    "Server unavailable. Showing saved data."));
        }
    }

    private static void notice(Context app, String message) {
        long now = SystemClock.elapsedRealtime();

        if (lastNoticeAt != 0L && now - lastNoticeAt < NOTICE_GAP_MS) {
            return;
        }

        lastNoticeAt = now;
        Toast.makeText(app, message, Toast.LENGTH_LONG).show();
    }

    private static String text(String ru, String en) {
        return "ru".equals(Locale.getDefault().getLanguage()) ? ru : en;
    }

    private static void reloadWhenOnline(Activity activity) {
        if (!(activity instanceof LifecycleOwner)) {
            return;
        }

        final ConnectivityManager manager =
                (ConnectivityManager) activity.getSystemService(Context.CONNECTIVITY_SERVICE);

        if (manager == null) {
            return;
        }

        final Context app = activity.getApplicationContext();
        final AtomicBoolean active = new AtomicBoolean(true);

        final ConnectivityManager.NetworkCallback callback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(@NonNull Network network) {
                check();
            }

            @Override
            public void onCapabilitiesChanged(@NonNull Network network,
                                              @NonNull NetworkCapabilities capabilities) {
                check();
            }

            private void check() {
                if (!active.get() || !MainActivity.checkConnection(app)) {
                    return;
                }

                if (!active.compareAndSet(true, false)) {
                    return;
                }

                try {
                    manager.unregisterNetworkCallback(this);
                } catch (RuntimeException ignored) {
                }

                activity.runOnUiThread(() -> {
                    if (!activity.isFinishing() && !activity.isDestroyed()) {
                        activity.recreate();
                    }
                });
            }
        };

        try {
            manager.registerDefaultNetworkCallback(callback);
        } catch (RuntimeException error) {
            return;
        }

        ((LifecycleOwner) activity).getLifecycle().addObserver(new DefaultLifecycleObserver() {
            @Override
            public void onDestroy(@NonNull LifecycleOwner owner) {
                if (active.compareAndSet(true, false)) {
                    try {
                        manager.unregisterNetworkCallback(callback);
                    } catch (RuntimeException ignored) {
                    }
                }
            }
        });
    }
}
