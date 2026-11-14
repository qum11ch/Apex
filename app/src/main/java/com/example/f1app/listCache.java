package com.example.f1app;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The lists the user has already seen, kept in memory while the app process lives.
 * A screen restores its list from here at once (no shimmer) and refreshes it silently.
 */
final class listCache {

    private static final Map<String, List<?>> STORE = new HashMap<>();

    private listCache() {
    }

    static synchronized <T> void put(String key, List<T> items) {
        if (items == null || items.isEmpty()) {
            return;
        }

        STORE.put(key, new ArrayList<>(items));
    }

    @SuppressWarnings("unchecked")
    static synchronized <T> List<T> get(String key) {
        List<?> items = STORE.get(key);

        return items == null ? null : new ArrayList<>((List<T>) items);
    }

    static synchronized void clear() {
        STORE.clear();
    }
}
