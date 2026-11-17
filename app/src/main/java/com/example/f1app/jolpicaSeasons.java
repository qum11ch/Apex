package com.example.f1app;

import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;

import com.android.volley.Cache;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.PriorityQueue;

/**
 * Standings of one season from Jolpica: the API allows bursts of 4 requests per second and 500 per hour,
 * and answers 429 above that. Network requests go one by one with a pause; what is already in the disk
 * cache goes out at once; a failed request is repeated. Constructors' standings (priority 0) go before
 * drivers' standings (priority 1). Everything runs on the main thread.
 */
final class jolpicaSeasons {

    interface Callback {
        /** The answer, or null when it could not be loaded. */
        void onResult(JSONObject response);
    }

    private static final class Job {
        final String url;
        final int priority;
        final long seq;
        int attempt = 0;

        Job(String url, int priority, long seq) {
            this.url = url;
            this.priority = priority;
            this.seq = seq;
        }
    }

    private static final String TAG = "JolpicaSeasons";
    private static final String REQUEST_TAG = "jolpicaSeasons";
    private static final long GAP_MS = 300L;
    private static final int MAX_ATTEMPTS = 3;

    private static final Handler HANDLER = new Handler(Looper.getMainLooper());
    private static final Map<String, JSONObject> MEMORY = new HashMap<>();
    private static final Map<String, List<Callback>> WAITING = new HashMap<>();
    private static final PriorityQueue<Job> QUEUE = new PriorityQueue<>((a, b) ->
            a.priority != b.priority
                    ? Integer.compare(a.priority, b.priority)
                    : Long.compare(a.seq, b.seq));

    private static boolean pumping = false;
    private static long nextSlot = 0L;
    private static long sequence = 0L;

    private jolpicaSeasons() {
    }

    static void driverStandings(int year, Callback callback) {
        get(url(year, "driverstandings"), 1, callback);
    }

    static void constructorStandings(int year, Callback callback) {
        get(url(year, "constructorstandings"), 0, callback);
    }

    private static String url(int year, String table) {
        return "https://api.jolpi.ca/ergast/f1/" + year + "/" + table + "/?format=json";
    }

    private static void get(String url, int priority, Callback callback) {
        JSONObject known = MEMORY.get(url);

        if (known != null) {
            callback.onResult(known);
            return;
        }

        List<Callback> waiting = WAITING.get(url);

        if (waiting != null) {
            waiting.add(callback);
            return;
        }

        waiting = new ArrayList<>();
        waiting.add(callback);
        WAITING.put(url, waiting);

        Job job = new Job(url, priority, ++sequence);

        if (isCached(url)) {
            send(job);
        } else {
            QUEUE.add(job);
            pump();
        }
    }

    private static boolean isCached(String url) {
        Cache cache = apiQueue.get().getCache();
        Cache.Entry entry = cache == null ? null : cache.get(url);

        return entry != null && !entry.isExpired();
    }

    private static void pump() {
        if (pumping || QUEUE.isEmpty()) {
            return;
        }

        pumping = true;

        long wait = Math.max(0L, nextSlot - SystemClock.uptimeMillis());

        HANDLER.postDelayed(() -> {
            pumping = false;

            Job job = QUEUE.poll();

            if (job != null) {
                nextSlot = SystemClock.uptimeMillis() + GAP_MS;
                send(job);
            }

            pump();
        }, wait);
    }

    private static void send(Job job) {
        RequestQueue queue = apiQueue.get();

        JsonObjectRequest request = new cachedJsonRequest(
                Request.Method.GET,
                job.url,
                null,
                response -> finish(job.url, response),
                error -> {
                    Log.w(TAG, "Attempt " + (job.attempt + 1) + " failed: " + job.url, error);

                    job.attempt++;

                    if (job.attempt < MAX_ATTEMPTS) {
                        HANDLER.postDelayed(() -> {
                            QUEUE.add(job);
                            pump();
                        }, 1500L * job.attempt);
                    } else {
                        finish(job.url, null);
                    }
                }
        );

        request.setTag(REQUEST_TAG);
        queue.add(request);
    }

    private static void finish(String url, JSONObject response) {
        if (response != null) {
            MEMORY.put(url, response);
        }

        List<Callback> waiting = WAITING.remove(url);

        if (waiting != null) {
            for (Callback callback : waiting) {
                callback.onResult(response);
            }
        }
    }

    /** The code first; without it the family name, and the given name for namesakes and brothers. */
    static boolean isDriver(JSONObject driver, String code, String given, String family) {
        String jolpicaCode = driver.optString("code", "");

        if (code != null && !code.isEmpty() && !jolpicaCode.isEmpty()) {
            return code.equalsIgnoreCase(jolpicaCode);
        }

        String jolpicaFamily = driver.optString("familyName", "");

        if (family == null || !family.equalsIgnoreCase(jolpicaFamily)) {
            return false;
        }

        String a = given == null ? "" : given.toLowerCase(Locale.ROOT);
        String b = driver.optString("givenName", "").toLowerCase(Locale.ROOT);

        return a.isEmpty() || b.isEmpty() || a.contains(b) || b.contains(a);
    }
}