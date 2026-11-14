package com.example.f1app;

import android.app.Activity;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.SystemClock;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;

import com.google.android.material.snackbar.Snackbar;

import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * The network, seen by the screens. Nothing here blocks a screen:
 * - showIfOffline: content screens open as usual, the user gets a short notice, the screen reloads when the network is back;
 * - watch: actions that need the server are dimmed while there is no network (the screen keeps working);
 * - ensureOnline: the guard at the top of an action that needs the server;
 * - runWhenOnline: do something once, as soon as the network is back.
 */
public final class networkNotice {

    private static final long NOTICE_GAP_MS = 20_000L;
    private static long lastNoticeAt = 0L;

    private networkNotice() {
    }

    // ---------------------------------------------------------------- content screens

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

    // ---------------------------------------------------------------- actions that need the server

    public static boolean isOnline(Context context) {
        return MainActivity.checkConnection(context.getApplicationContext());
    }

    /** Offline: tells the user why nothing happens and returns false. Put it first in a handler that needs the server. */
    public static boolean ensureOnline(Activity activity) {
        if (isOnline(activity)) {
            return true;
        }

        tellOfflineAction(activity);
        return false;
    }

    /** While there is no network the given views look unavailable; they come back by themselves. */
    public static void watch(Activity activity, View... actions) {
        if (!(activity instanceof LifecycleOwner)) {
            return;
        }

        ((LifecycleOwner) activity).getLifecycle().addObserver(new Watcher(activity, actions));
    }

    /** Runs the action once, on the main thread, when the network is back (at once when it is already there). */
    public static void runWhenOnline(Activity activity, Runnable action) {
        if (isOnline(activity)) {
            action.run();
            return;
        }

        final ConnectivityManager manager =
                (ConnectivityManager) activity.getSystemService(Context.CONNECTIVITY_SERVICE);

        if (manager == null || !(activity instanceof LifecycleOwner)) {
            return;
        }

        final AtomicBoolean waiting = new AtomicBoolean(true);

        final ConnectivityManager.NetworkCallback callback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onCapabilitiesChanged(@NonNull Network network, @NonNull NetworkCapabilities caps) {
                if (!hasInternet(caps) || !waiting.compareAndSet(true, false)) {
                    return;
                }

                try {
                    manager.unregisterNetworkCallback(this);
                } catch (RuntimeException ignored) {
                }

                activity.runOnUiThread(() -> {
                    if (!activity.isFinishing() && !activity.isDestroyed()) {
                        action.run();
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
                if (waiting.compareAndSet(true, false)) {
                    try {
                        manager.unregisterNetworkCallback(callback);
                    } catch (RuntimeException ignored) {
                    }
                }
            }
        });
    }

    // ---------------------------------------------------------------- inside

    private static boolean hasInternet(NetworkCapabilities caps) {
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
    }

    private static void tellOfflineAction(Activity activity) {
        View root = activity.findViewById(android.R.id.content);
        String message = text(
                "Нет подключения к сети. Это действие станет доступно, когда сеть появится.",
                "No connection. This action needs the network.");

        try {
            if (root != null) {
                Snackbar snackbar = Snackbar.make(root, message, Snackbar.LENGTH_LONG);
                raiseAboveSystemBars(snackbar, root);
                snackbar.show();
                return;
            }
        } catch (RuntimeException error) {
            // the app theme is not a Material one: a toast says the same
        }

        Toast.makeText(activity.getApplicationContext(), message, Toast.LENGTH_LONG).show();
    }

    /** The app draws edge to edge: lift the notice over the navigation bar and the rounded screen corners. */
    private static void raiseAboveSystemBars(Snackbar snackbar, View root) {
        int bottom = 0;
        WindowInsetsCompat insets = ViewCompat.getRootWindowInsets(root);

        if (insets != null) {
            bottom = insets.getInsets(WindowInsetsCompat.Type.systemBars()
                    | WindowInsetsCompat.Type.displayCutout()).bottom;
        }

        final int gap = Math.round(12 * root.getResources().getDisplayMetrics().density);
        View view = snackbar.getView();
        ViewGroup.LayoutParams params = view.getLayoutParams();

        if (params instanceof ViewGroup.MarginLayoutParams) {
            ((ViewGroup.MarginLayoutParams) params).bottomMargin += bottom + gap;
            view.setLayoutParams(params);
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

    /** Dims the actions while the screen is visible and the network is gone. */
    private static final class Watcher extends ConnectivityManager.NetworkCallback
            implements DefaultLifecycleObserver {

        private final Activity activity;
        private final View[] views;
        private final float[] alphas;
        private final ConnectivityManager manager;
        private boolean registered = false;
        private boolean dimmed = false;

        Watcher(Activity activity, View[] views) {
            this.activity = activity;
            this.views = views;
            this.alphas = new float[views.length];
            this.manager = (ConnectivityManager) activity.getSystemService(Context.CONNECTIVITY_SERVICE);
        }

        @Override
        public void onStart(@NonNull LifecycleOwner owner) {
            if (manager == null || registered) {
                return;
            }

            apply(!isOnline(activity));

            try {
                manager.registerDefaultNetworkCallback(this);
                registered = true;
            } catch (RuntimeException ignored) {
            }
        }

        @Override
        public void onStop(@NonNull LifecycleOwner owner) {
            if (manager != null && registered) {
                try {
                    manager.unregisterNetworkCallback(this);
                } catch (RuntimeException ignored) {
                }

                registered = false;
            }
        }

        @Override
        public void onCapabilitiesChanged(@NonNull Network network, @NonNull NetworkCapabilities caps) {
            final boolean offline = !hasInternet(caps);
            activity.runOnUiThread(() -> apply(offline));
        }

        @Override
        public void onLost(@NonNull Network network) {
            activity.runOnUiThread(() -> apply(true));
        }

        private void apply(boolean offline) {
            if (activity.isFinishing() || activity.isDestroyed()) {
                return;
            }

            if (offline && !dimmed) {
                for (int i = 0; i < views.length; i++) {
                    if (views[i] != null) {
                        alphas[i] = views[i].getAlpha();
                        views[i].setAlpha(0.5f);
                    }
                }

                dimmed = true;
                tellOfflineAction(activity);
            } else if (!offline && dimmed) {
                for (int i = 0; i < views.length; i++) {
                    if (views[i] != null) {
                        views[i].setAlpha(alphas[i]);
                    }
                }

                dimmed = false;
            }
        }
    }
}
