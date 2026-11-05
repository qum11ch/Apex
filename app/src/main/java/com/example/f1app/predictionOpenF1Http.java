package com.example.f1app;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;

public final class predictionOpenF1Http {
    private static final int CACHE_LIMIT = 64;
    private static final int CACHE_TTL_MS = 10 * 60 * 1000;
    private static final int MAX_RESPONSE_BYTES = 8 * 1024 * 1024;
    private static final int MAX_RETRY_DELAY_MS = 30000;

    private static final Map<String, Entry> CACHE = new LinkedHashMap<>(16, 0.75f, true);
    private static final ConcurrentHashMap<String, CompletableFuture<String>> FLIGHT = new ConcurrentHashMap<>();

    private static long lastStart;

    private predictionOpenF1Http() {
    }

    private static final class Entry {
        final String body;
        final long createdAt = System.nanoTime();

        Entry(String body) {
            this.body = body;
        }
    }

    public static void clearCache() {
        synchronized (CACHE) {
            CACHE.clear();
        }
    }

    public static void invalidate(String key) {
        synchronized (CACHE) {
            CACHE.remove(key);
        }
    }

    public static String get(String address) throws Exception {
        synchronized (CACHE) {
            Entry entry = CACHE.get(address);

            if (entry != null && (System.nanoTime() - entry.createdAt) / 1000000 < CACHE_TTL_MS) {
                return entry.body;
            }

            CACHE.remove(address);
        }

        CompletableFuture<String> own = new CompletableFuture<>();
        CompletableFuture<String> existing = FLIGHT.putIfAbsent(address, own);

        if (existing != null) {
            try {
                return existing.get();
            } catch (ExecutionException exception) {
                throw new IOException(exception.getCause());
            }
        }

        try {
            String body = download(address);

            synchronized (CACHE) {
                CACHE.put(address, new Entry(body));

                while (CACHE.size() > CACHE_LIMIT) {
                    CACHE.remove(CACHE.keySet().iterator().next());
                }
            }

            own.complete(body);

            return body;
        } catch (Exception exception) {
            own.completeExceptionally(exception);

            throw exception;
        } finally {
            FLIGHT.remove(address, own);
        }
    }

    private static synchronized void pace() throws InterruptedException {
        long wait = 1000 - (System.nanoTime() - lastStart) / 1000000;

        if (wait > 0) {
            Thread.sleep(wait);

            lastStart = System.nanoTime();
        }
    }

    private static String download(String address) throws Exception {
        IOException failure = null;

        for (int attempt = 0; attempt < 2; attempt++) {
            pace();

            long delay = 2000;

            HttpURLConnection connection = (HttpURLConnection) new URL(address).openConnection();

            connection.setConnectTimeout(10000);
            connection.setReadTimeout(15000);
            connection.setRequestProperty("Accept", "application/json");

            try {
                int status = connection.getResponseCode();

                if (status != 200) {
                    boolean retryable = status == 429 || status == 502 || status == 503 || status == 504;

                    if (!retryable || attempt == 1) {
                        throw new IOException("OpenF1 HTTP " + status + "; " + address);
                    }

                    delay = retryDelay(connection.getHeaderField("Retry-After"), delay);

                    if (delay > MAX_RETRY_DELAY_MS) {
                        throw new IOException("OpenF1 retry later; " + address);
                    }

                    failure = new IOException("OpenF1 HTTP " + status);
                } else {
                    return readBody(connection);
                }
            } catch (SocketTimeoutException exception) {
                failure = new IOException("OpenF1 timeout; " + address, exception);

                if (attempt == 1) {
                    throw failure;
                }
            } finally {
                connection.disconnect();
            }

            Thread.sleep(delay);
        }

        throw failure == null ? new IOException("OpenF1 request failed") : failure;
    }

    private static long retryDelay(String retryAfter, long defaultDelay) throws IOException {
        if (retryAfter == null) {
            return defaultDelay;
        }

        try {
            return Math.max(defaultDelay, Long.parseLong(retryAfter) * 1000);
        } catch (Exception exception) {
            throw new IOException("Retry later: " + retryAfter);
        }
    }

    private static String readBody(HttpURLConnection connection) throws IOException {
        try (InputStream input = connection.getInputStream(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int read;

            while ((read = input.read(buffer)) != -1) {
                if (output.size() + read > MAX_RESPONSE_BYTES) {
                    throw new IOException("OpenF1 response too large");
                }

                output.write(buffer, 0, read);
            }

            return new String(output.toByteArray(), StandardCharsets.UTF_8);
        }
    }
}