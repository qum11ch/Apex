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
 * The one list for every RecyclerView screen. It remembers what the user has already seen
 * (in memory, while the app process lives), so a repeat visit shows the list at once, without a shimmer.
 * Main thread only.
 *
 * First visit : behaves like a plain ArrayList; the screen shows its shimmer, data arrives, the list is remembered.
 * Next visits : the remembered items are there at once ({@link #restore} hides the shimmer). The screen still
 *               reloads, but the fresh items are collected on the side and swapped in one go:
 *               - as soon as {@link #expect} items have arrived (when the screen knows the exact number), or
 *               - when {@link #finish} is called (the screen knows the load is over), or
 *               - when as many items as before have arrived, or after a short pause.
 * Failed reload: the remembered items simply stay on screen.
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

    private boolean fromCache;        // the visible items are old; fresh ones are being collected
    private boolean restoredAtStart;  // the list was filled from memory when the screen opened
    private boolean saveEmpty;        // a fresh load really came back empty: remember that too
    private int previousSize;
    private int expected;             // exact number of fresh items, 0 = unknown
    private List<T> staging;
    private Comparator<? super T> sorter;
    private RecyclerView.Adapter<?> adapter;
    private Runnable onCommit;

    public cachedList(String key) {
        this.key = key;

        List<T> saved = load(key);

        if (saved != null) {
            super.addAll(saved);
            fromCache = true;
            restoredAtStart = true;
            previousSize = saved.size();
        }
    }

    // ---------- screen API ----------

    public static String key(String name, Bundle args, String... argKeys) {
        StringBuilder builder = new StringBuilder(name);

        for (String argKey : argKeys) {
            builder.append('|').append(args == null ? "" : args.getString(argKey, ""));
        }

        return builder.toString();
    }

    /** True when the list was filled from memory at creation (a repeat visit). */
    public static boolean hasCache(List<?> list) {
        return list instanceof cachedList && ((cachedList<?>) list).restoredAtStart;
    }

    /** Call once the adapter and the shimmer view exist. Shows the remembered list and hides the shimmer. */
    public static void restore(List<?> list, RecyclerView.Adapter<?> adapter,
                               RecyclerView recyclerView, ShimmerFrameLayout shimmer) {
        if (!(list instanceof cachedList)) {
            return;
        }

        cachedList<?> cached = (cachedList<?>) list;
        cached.adapter = adapter;

        if (cached.restoredAtStart && cached.fromCache) {
            if (adapter != null) {
                adapter.notifyDataSetChanged();
            }

            if (shimmer != null) {
                shimmer.stopShimmer();
                shimmer.setVisibility(View.GONE);
            }

            recyclerView.setVisibility(View.VISIBLE);
        }
    }

    /** The exact number of items a reload will bring. The swap happens when they have all arrived. */
    public static void expect(List<?> list, int count) {
        if (list instanceof cachedList && count > 0 && count != Integer.MAX_VALUE) {
            ((cachedList<?>) list).expected = count;
        }
    }

    /** The reload is over: swap the freshly collected items in now (does nothing when none were collected). */
    public static void finish(List<?> list) {
        if (list instanceof cachedList) {
            ((cachedList<?>) list).commit();
        }
    }

    /** Pull-to-refresh: keep what is on screen, collect the fresh items on the side. */
    public static void beginReload(List<?> list) {
        if (!(list instanceof cachedList)) {
            return;
        }

        cachedList<?> cached = (cachedList<?>) list;

        if (!cached.fromCache && !cached.isEmpty()) {
            cached.fromCache = true;
            cached.previousSize = cached.size();
        }

        if (cached.fromCache) {
            MAIN.removeCallbacks(cached.commitRunnable);
            cached.staging = null;
            cached.expected = 0;
        }
    }

    /** Runs after the fresh items replaced the remembered ones (stop a refresh spinner here). */
    public static void onCommit(List<?> list, Runnable action) {
        if (list instanceof cachedList) {
            ((cachedList<?>) list).onCommit = action;
        }
    }

    // ---------- plain remembering (screens that build the whole list at once) ----------

    public static <E> void remember(String key, List<E> items) {
        if (key != null && items != null) {
            STORE.put(key, new ArrayList<>(items));
        }
    }

    public static <E> List<E> recall(String key) {
        return key == null ? null : load(key);
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

    // ---------- inside ----------

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

        int target = expected > 0 ? expected : previousSize;

        if (staging.size() >= target) {
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
        expected = 0;
        saveEmpty = fresh.isEmpty();

        if (sorter != null) {
            fresh.sort(sorter);
        }

        super.clear();
        super.addAll(fresh);

        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }

        scheduleSave();

        Runnable done = onCommit;

        if (done != null) {
            done.run();
        }
    }

    private void scheduleSave() {
        MAIN.removeCallbacks(saveRunnable);
        MAIN.postDelayed(saveRunnable, SAVE_IDLE_MS);
    }

    private void save() {
        if (fromCache || (super.isEmpty() && !saveEmpty)) {
            return;
        }

        STORE.put(key, new ArrayList<>(this));
        saveEmpty = false;
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
    public T remove(int index) {
        T removed = super.remove(index);
        afterRemove();
        return removed;
    }

    @Override
    public boolean remove(Object item) {
        boolean removed = super.remove(item);
        afterRemove();
        return removed;
    }

    private void afterRemove() {
        if (fromCache) {
            return;
        }

        if (super.isEmpty()) {
            saveEmpty = true; // the user deleted the last item: remember the empty list
        }

        scheduleSave();
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
