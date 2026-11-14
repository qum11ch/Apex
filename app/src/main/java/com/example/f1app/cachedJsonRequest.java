package com.example.f1app;

import android.content.Context;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.toolbox.JsonObjectRequest;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * A drop-in JsonObjectRequest. Every good answer from the Jolpica API is also saved to disk;
 * when the server cannot be reached the saved answer is delivered instead of an error (once, no double delivery).
 * Only GET requests to api.jolpi.ca are saved, everything else behaves exactly like JsonObjectRequest.
 */
public class cachedJsonRequest extends JsonObjectRequest {

    private static final String HOST_MARK = "://api.jolpi.ca/";
    private static final long MAX_AGE_MS = 30L * 24 * 60 * 60 * 1000;
    private static final int MAX_CHARS = 2 * 1024 * 1024;
    private static final ExecutorService IO = Executors.newSingleThreadExecutor();
    private static boolean pruned = false;

    public cachedJsonRequest(int method, String url, JSONObject jsonRequest,
                             Response.Listener<JSONObject> listener,
                             Response.ErrorListener errorListener) {
        super(method, url, jsonRequest,
                remember(method, url, listener),
                fallback(method, url, listener, errorListener));

        // a dead mobile connection should fall back to the saved answer in seconds, not minutes
        setRetryPolicy(new DefaultRetryPolicy(8000, 1, 1f));
    }

    public cachedJsonRequest(String url, JSONObject jsonRequest,
                             Response.Listener<JSONObject> listener,
                             Response.ErrorListener errorListener) {
        this(jsonRequest == null ? Request.Method.GET : Request.Method.POST,
                url, jsonRequest, listener, errorListener);
    }

    private static boolean cacheable(int method, String url) {
        return method == Request.Method.GET && url != null && url.contains(HOST_MARK);
    }

    private static Response.Listener<JSONObject> remember(int method, String url,
                                                          Response.Listener<JSONObject> listener) {
        if (!cacheable(method, url)) {
            return listener;
        }

        return response -> {
            store(url, response);
            listener.onResponse(response);
        };
    }

    private static Response.ErrorListener fallback(int method, String url,
                                                   Response.Listener<JSONObject> listener,
                                                   Response.ErrorListener errorListener) {
        if (!cacheable(method, url)) {
            return errorListener;
        }

        return error -> {
            JSONObject saved = read(url);

            if (saved == null) {
                if (errorListener != null) {
                    errorListener.onErrorResponse(error);
                }
                return;
            }

            networkNotice.showStale();
            listener.onResponse(saved);
        };
    }

    private static File dir(Context context) {
        File dir = new File(context.getFilesDir(), "api_json");

        if (!dir.exists()) {
            dir.mkdirs();
        }

        return dir;
    }

    private static String fileName(String url) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-1").digest(url.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder();

            for (byte b : hash) {
                builder.append(String.format("%02x", b));
            }

            return builder.toString();
        } catch (Exception error) {
            return Integer.toHexString(url.hashCode());
        }
    }

    private static void store(String url, JSONObject json) {
        final Context context = ApexApp.context();

        if (context == null || json == null) {
            return;
        }

        final String body = json.toString();

        if (body.length() > MAX_CHARS) {
            return;
        }

        IO.execute(() -> {
            try {
                File dir = dir(context);
                prune(dir);

                File target = new File(dir, fileName(url));
                File temp = new File(dir, target.getName() + ".tmp");

                try (OutputStream out = new FileOutputStream(temp)) {
                    out.write(body.getBytes(StandardCharsets.UTF_8));
                }

                if (!temp.renameTo(target)) {
                    target.delete();
                    temp.renameTo(target);
                }
            } catch (IOException | RuntimeException ignored) {
            }
        });
    }

    private static void prune(File dir) {
        if (pruned) {
            return;
        }

        pruned = true;
        File[] files = dir.listFiles();

        if (files == null) {
            return;
        }

        long limit = System.currentTimeMillis() - MAX_AGE_MS;

        for (File file : files) {
            if (file.lastModified() < limit) {
                file.delete();
            }
        }
    }

    private static JSONObject read(String url) {
        Context context = ApexApp.context();

        if (context == null) {
            return null;
        }

        File file = new File(dir(context), fileName(url));

        if (!file.isFile() || System.currentTimeMillis() - file.lastModified() > MAX_AGE_MS) {
            return null;
        }

        try (InputStream in = new FileInputStream(file)) {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            byte[] chunk = new byte[8192];
            int count;

            while ((count = in.read(chunk)) > 0) {
                buffer.write(chunk, 0, count);
            }

            return new JSONObject(buffer.toString("UTF-8"));
        } catch (IOException | JSONException | RuntimeException error) {
            return null;
        }
    }
}
