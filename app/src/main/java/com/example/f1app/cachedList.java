package com.example.f1app;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;

import androidx.recyclerview.widget.RecyclerView;

import com.facebook.shimmer.ShimmerFrameLayout;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A list for a RecyclerView that remembers what the user has already seen (in memory, while the app process lives).
 *
 * - First visit: behaves like a plain ArrayList (the screen shows its shimmer, data arrives, it is remembered).
 * - Next visits: the remembered items are there at once; {@link #restore} hides the shimmer. The screen still
 *   reloads, but the fresh items are collected on the side and swapped in one go, so nothing flickers.
 * - If the reload fails, the remembered items simply stay on screen.
 */
public class cachedList<T> extends ArrayList<T> {

    private static final Map<String, List<?>> STORE = new HashMap<>();
    private static final Map<String, String[]> EXTRAS = new HashMap<>();
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static final long COMMIT_IDLE_MS = 1200;
    private static final long SAVE_IDLE_MS = 1500;

    private final String key;
    private final Runnable commitRunnable = this::commit;
    private final Runnable saveRunnable = this::save;

    private boolean fromCache;
    private int previousSize;
    private List<T> staging;
    private Comparator<? super T> sorter;
    private RecyclerView.Adapter<?> adapter;

    public cachedList(String key) {
        this.key = key;

        List<T> saved = load(key);

        if (saved != null) {
            super.addAll(saved);
            fromCache = true;
            previousSize = saved.size();
        }
    }

    public static String key(String name, Bundle args, String... argKeys) {
        StringBuilder builder = new StringBuilder(name);

        for (String argKey : argKeys) {
            builder.append('|').append(args == null ? "" : args.getString(argKey, ""));
        }

        return builder.toString();
    }

    /** Call once, right after the shimmer view is found. Shows the remembered list and hides the shimmer. */
    public static void restore(List<?> list, RecyclerView.Adapter<?> adapter,
                               RecyclerView recyclerView, ShimmerFrameLayout shimmer) {
        if (!(list instanceof cachedList)) {
            return;
        }

        cachedList<?> cached = (cachedList<?>) list;
        cached.adapter = adapter;

        if (cached.fromCache) {
            adapter.notifyDataSetChanged();

            if (shimmer != null) {
                shimmer.stopShimmer();
                shimmer.setVisibility(View.GONE);
            }

            recyclerView.setVisibility(View.VISIBLE);
        }
    }

    public static void putExtra(String key, String... values) {
        EXTRAS.put(key, values);
    }

    public static String[] getExtra(String key) {
        return EXTRAS.get(key);
    }

    public static void clearAll() {
        STORE.clear();
        EXTRAS.clear();
    }

    @SuppressWarnings("unchecked")
    private static <E> List<E> load(String key) {
        List<?> saved = STORE.get(key);

        return saved == null ? null : new ArrayList<>((List<E>) saved);
    }

    private void stage(T item, int index) {
        if (staging == null) {
            staging = new ArrayList<>();
        }

        if (index < 0 || index > staging.size()) {
            staging.add(item);
        } else {
            staging.add(index, item);
        }

        MAIN.removeCallbacks(commitRunnable);

        if (staging.size() >= previousSize) {
            commit();
        } else {
            MAIN.postDelayed(commitRunnable, COMMIT_IDLE_MS);
        }
    }

    private void commit() {
        MAIN.removeCallbacks(commitRunnable);

        if (!fromCache || staging == null) {
            return;
        }

        List<T> fresh = staging;
        staging = null;
        fromCache = false;

        if (sorter != null) {
            fresh.sort(sorter);
        }

        super.clear();
        super.addAll(fresh);

        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }

        scheduleSave();
    }

    private void scheduleSave() {
        MAIN.removeCallbacks(saveRunnable);
        MAIN.postDelayed(saveRunnable, SAVE_IDLE_MS);
    }

    private void save() {
        if (fromCache || super.isEmpty()) {
            return;
        }

        STORE.put(key, new ArrayList<>(this));
    }

    @Override
    public boolean add(T item) {
        if (fromCache) {
            stage(item, -1);
            return true;
        }

        boolean added = super.add(item);
        scheduleSave();
        return added;
    }

    @Override
    public void add(int index, T item) {
        if (fromCache) {
            stage(item, index);
            return;
        }

        super.add(index, item);
        scheduleSave();
    }

    @Override
    public boolean addAll(Collection<? extends T> items) {
        if (fromCache) {
            for (T item : items) {
                stage(item, -1);
            }

            return !items.isEmpty();
        }

        boolean added = super.addAll(items);
        scheduleSave();
        return added;
    }

    @Override
    public void clear() {
        if (fromCache) {
            // keep the remembered items on screen until the fresh ones arrive
            staging = new ArrayList<>();
            MAIN.removeCallbacks(commitRunnable);
            return;
        }

        super.clear();
    }

    @Override
    public void sort(Comparator<? super T> comparator) {
        sorter = comparator;

        if (!fromCache) {
            super.sort(comparator);
        } else if (staging != null) {
            staging.sort(comparator);
        }
    }
}
