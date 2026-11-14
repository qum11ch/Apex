package com.example.f1app;

import android.content.Context;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;

/**
 * One Volley queue for the whole app (the setup Volley's own documentation recommends).
 * A queue made per request starts its own threads that never stop. Requests of a screen are still cancelled
 * by tag: queue.cancelAll(this).
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

            queue = Volley.newRequestQueue(app);
        }

        return queue;
    }
}
