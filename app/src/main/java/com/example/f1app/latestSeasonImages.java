package com.example.f1app;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.os.SystemClock;
import android.util.Log;
import android.widget.ImageView;

import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleEventObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ViewTreeLifecycleOwner;

import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

final class latestSeasonImages {

    private static final String TAG = "LatestSeasonImages";
    private static final long KEEP_MS = 5 * 60_000L;

    private interface Callback {
        void onReady(List<StorageReference> files);
    }

    private static final class Inventory {
        List<StorageReference> files;
        long loadedAt;
        boolean loading;
        final List<Callback> waiting = new ArrayList<>();
    }

    private static final Map<String, Inventory> INVENTORIES =
            new HashMap<>();

    private latestSeasonImages() {
    }

    static void loadDriver(
            ImageView target,
            String code,
            String driverId,
            int latestYear
    ) {
        List<String> ids = new ArrayList<>();
        addId(ids, driverId);
        addId(ids, code);

        load(target, "drivers", ids, latestYear,
                R.drawable.placeholder_driver);
    }

    static void loadTeam(
            ImageView target,
            String teamId,
            int latestYear
    ) {
        List<String> ids = new ArrayList<>();
        addId(ids, teamId);

        load(target, "teams", ids, latestYear,
                R.drawable.placeholder_car);
    }

    private static void addId(List<String> ids, String raw) {
        if (raw == null) {
            return;
        }

        String id = raw.trim().toLowerCase(Locale.ROOT);

        if (!id.isEmpty()
                && !"n/a".equals(id)
                && !"null".equals(id)
                && !ids.contains(id)) {
            ids.add(id);
        }
    }

    private static boolean isScreenAlive(ImageView image) {
        Context context = image.getContext();

        while (context instanceof ContextWrapper) {
            if (context instanceof Activity) {
                Activity activity = (Activity) context;
                return !activity.isFinishing() && !activity.isDestroyed();
            }

            Context base = ((ContextWrapper) context).getBaseContext();

            if (base == context) {
                break;
            }

            context = base;
        }

        return true;
    }

    private static void load(
            ImageView image,
            String folder,
            List<String> ids,
            int latestYear,
            int placeholder
    ) {
        if (!isScreenAlive(image)) {
            return;
        }

        LifecycleOwner owner = ViewTreeLifecycleOwner.get(image);
        final Lifecycle lifecycle = owner != null
                ? owner.getLifecycle()
                : null;

        if (lifecycle != null
                && lifecycle.getCurrentState() == Lifecycle.State.DESTROYED) {
            return;
        }

        Object token = new Object();
        image.setTag(R.id.latest_season_image_token, token);

        if (ids.isEmpty() || latestYear <= 0) {
            GlideApp.with(image).clear(image);
            image.setImageResource(placeholder);
            return;
        }

        final WeakReference<ImageView> imageRef =
                new WeakReference<>(image);

        final LifecycleEventObserver destroyObserver = (source, event) -> {
            if (event == Lifecycle.Event.ON_DESTROY) {
                ImageView current = imageRef.get();

                if (current != null
                        && current.getTag(R.id.latest_season_image_token)
                        == token) {
                    current.setTag(R.id.latest_season_image_token, null);
                }
            }
        };

        if (lifecycle != null) {
            lifecycle.addObserver(destroyObserver);
        }

        inventory(folder, files -> {
            if (lifecycle != null) {
                lifecycle.removeObserver(destroyObserver);
            }

            ImageView target = imageRef.get();

            if (target == null
                    || target.getTag(R.id.latest_season_image_token) != token
                    || !isScreenAlive(target)
                    || (lifecycle != null
                    && lifecycle.getCurrentState()
                    == Lifecycle.State.DESTROYED)) {
                return;
            }

            // Ошибка списка не должна стирать уже показанное фото.

            // Ошибка списка не должна стирать уже показанное фото.
            if (files == null) {
                if (target.getDrawable() == null) {
                    target.setImageResource(placeholder);
                }
                return;
            }

            StorageReference best = null;
            int bestYear = -1;
            int bestPriority = Integer.MAX_VALUE;

            for (StorageReference file : files) {
                String name = file.getName();

                if (!name.endsWith(".png")) {
                    continue;
                }

                int separator = name.lastIndexOf('_');

                if (separator <= 0) {
                    continue;
                }

                String id = name.substring(0, separator)
                        .toLowerCase(Locale.ROOT);

                int priority = ids.indexOf(id);

                if (priority < 0) {
                    continue;
                }

                int year;

                try {
                    year = Integer.parseInt(name.substring(
                            separator + 1, name.length() - 4));
                } catch (NumberFormatException error) {
                    continue;
                }

                if (year > latestYear || year < 1950) {
                    continue;
                }

                if (year > bestYear
                        || (year == bestYear && priority < bestPriority)) {
                    best = file;
                    bestYear = year;
                    bestPriority = priority;
                }
            }

            if (best == null) {
                GlideApp.with(target).clear(target);
                target.setImageResource(placeholder);
                return;
            }

            GlideApp.with(target)
                    .load(best)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .error(placeholder)
                    .dontAnimate()
                    .into(target);
        });
    }

    private static void inventory(String folder, Callback callback) {
        Inventory inventory = INVENTORIES.get(folder);

        if (inventory == null) {
            inventory = new Inventory();
            INVENTORIES.put(folder, inventory);
        }

        long now = SystemClock.elapsedRealtime();

        if (inventory.files != null
                && now - inventory.loadedAt < KEEP_MS) {
            callback.onReady(inventory.files);
            return;
        }

        inventory.waiting.add(callback);

        if (inventory.loading) {
            return;
        }

        inventory.loading = true;
        final Inventory state = inventory;

        FirebaseStorage.getInstance().getReference()
                .child(folder)
                .listAll()
                .addOnSuccessListener(result -> {
                    state.files = new ArrayList<>(result.getItems());
                    state.loadedAt = SystemClock.elapsedRealtime();
                    complete(state, state.files);
                })
                .addOnFailureListener(error -> {
                    Log.w(TAG, "Cannot list Storage folder: " + folder,
                            error);

                    // Если список уже был получен, используем его.
                    complete(state, state.files);
                });
    }

    private static void complete(
            Inventory state,
            List<StorageReference> files
    ) {
        state.loading = false;

        List<Callback> callbacks = new ArrayList<>(state.waiting);
        state.waiting.clear();

        for (Callback callback : callbacks) {
            callback.onReady(files);
        }
    }
}