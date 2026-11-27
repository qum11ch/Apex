package com.example.f1app;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;

import com.android.volley.Cache;
import com.android.volley.Network;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.BasicNetwork;
import com.android.volley.toolbox.DiskBasedCache;
import com.android.volley.toolbox.HurlStack;

import java.io.File;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * One Volley queue for the whole app. Requests to the Jolpica API leave it with a pause between them:
 * the API allows 4 requests per second, and a screen that starts five requests at once gets 429.
 */
final class apiQueue {

    private static RequestQueue queue;

    private apiQueue() {
    }

    static synchronized RequestQueue get() {
        if (queue == null) {
            Context app = ApexApp.context();

            if (app == null) {
                throw new IllegalStateException("ApexApp is not registered in AndroidManifest.xml");
            }

            RequestQueue created = new PacedQueue(
                    new DiskBasedCache(new File(app.getCacheDir(), "volley"), 5 * 1024 * 1024),
                    new BasicNetwork(new HurlStack()));
            created.start();
            queue = created;
        }

        return queue;
    }

    private static final class PacedQueue extends RequestQueue {

        private static final long GAP_MS = 300L;

        private final Handler handler = new Handler(Looper.getMainLooper());
        private final List<Request<?>> waiting = new CopyOnWriteArrayList<>();
        private long nextSlot = 0L;
        private int counter = 0;

        PacedQueue(Cache cache, Network network) {
            super(cache, network);
        }

        @Override
        public <T> Request<T> add(Request<T> request) {
            String url = request.getUrl();

            if (url == null || !url.contains("://api.jolpi.ca/")) {
                return super.add(request);
            }

            long delay;

            synchronized (this) {
                long now = SystemClock.elapsedRealtime();
                long slot = Math.max(now, nextSlot);
                nextSlot = slot + GAP_MS;
                delay = slot - now;
                counter++;
            }

            Log.d("apiQueue", "jolpica request #" + counter + " in " + delay + " ms: " + url);

            if (delay <= 0) {
                return super.add(request);
            }

            waiting.add(request);
            handler.postDelayed(() -> {
                waiting.remove(request);

                if (!request.isCanceled()) {
                    addNow(request);
                }
            }, delay);

            return request;
        }

        private <T> void addNow(Request<T> request) {
            super.add(request);
        }

        @Override
        public void cancelAll(Object tag) {
            // a request that waits for its pause is not in the queue yet
            for (Request<?> request : waiting) {
                if (tag != null && tag.equals(request.getTag())) {
                    request.cancel();
                }
            }

            super.cancelAll(tag);
        }
    }
}