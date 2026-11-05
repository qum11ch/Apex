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

public final class predictionPointsHttp {
    private static final long TTL_MS = 60 * 60 * 1000L;
    private static final int MAX_ENTRIES = 96;
    private static final int MAX_RESPONSE_BYTES = 8 * 1024 * 1024;
    private static final int MAX_RETRY_DELAY_MS = 30000;
    private static final int PACE_DELAY_MS = 400;

    private static final Map<String, Entry> CACHE = new LinkedHashMap<>(16, 0.75f, true);
    private static final ConcurrentHashMap<String, CompletableFuture<String>> FLIGHT = new ConcurrentHashMap<>();
    private static final Object PACE = new Object();

    private static long lastStartNanos;

    private predictionPointsHttp() {
    }

    private static final class Entry {
        final String body;
        final long createdAt;

        Entry(String body) {
            this.body = body;
            this.createdAt = System.nanoTime();
        }
    }

    public static void invalidate(String url) {
        synchronized (CACHE) {
            CACHE.remove(url);
        }
    }

    public static void clearCache() {
        synchronized (CACHE) {
            CACHE.clear();
        }
    }

    public static String get(String address) throws Exception {
        synchronized (CACHE) {
            Entry entry = CACHE.get(address);

            if (entry != null && (System.nanoTime() - entry.createdAt) / 1000000L < TTL_MS) {
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

                while (CACHE.size() > MAX_ENTRIES) {
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

    private static void pace() throws InterruptedException {
        synchronized (PACE) {
            long wait = PACE_DELAY_MS - (System.nanoTime() - lastStartNanos) / 1000000L;

            if (wait > 0) {
                Thread.sleep(wait);
            }

            lastStartNanos = System.nanoTime();
        }
    }

    private static String download(String address) throws Exception {
        IOException failure = null;

        for (int attempt = 0; attempt < 2; attempt++) {
            pace();

            long delay = 1500;

            HttpURLConnection connection = (HttpURLConnection) new URL(address).openConnection();

            connection.setConnectTimeout(10000);
            connection.setReadTimeout(10000);
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestProperty("Connection", "close");

            try {
                int status = connection.getResponseCode();

                if (status != 200) {
                    boolean retryable = status == 429 || status == 502 || status == 503 || status == 504;

                    if (!retryable || attempt == 1) {
                        throw new IOException("HTTP " + status + "; " + address);
                    }

                    delay = retryDelay(connection.getHeaderField("Retry-After"), delay, status, address);

                    if (delay > MAX_RETRY_DELAY_MS) {
                        throw new IOException("Retry later; HTTP " + status + "; " + address);
                    }

                    failure = new IOException("HTTP " + status + "; " + address);
                } else {
                    return readBody(connection, address);
                }
            } catch (SocketTimeoutException exception) {
                failure = new IOException("Timeout; " + address, exception);

                if (attempt == 1) {
                    throw failure;
                }
            } finally {
                connection.disconnect();
            }

            Thread.sleep(delay);
        }

        throw failure == null ? new IOException("Request failed; " + address) : failure;
    }

    private static long retryDelay(String retryAfter, long defaultDelay, int status, String address) throws IOException {
        if (retryAfter == null) {
            return defaultDelay;
        }

        try {
            return Math.max(defaultDelay, Long.parseLong(retryAfter.trim()) * 1000L);
        } catch (NumberFormatException exception) {
            throw new IOException("HTTP " + status + "; Retry-After=" + retryAfter + "; " + address);
        }
    }

    private static String readBody(HttpURLConnection connection, String address) throws IOException {
        try (InputStream input = connection.getInputStream(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int read;

            while ((read = input.read(buffer)) != -1) {
                if (output.size() + read > MAX_RESPONSE_BYTES) {
                    throw new IOException("Response too large; " + address);
                }

                output.write(buffer, 0, read);
            }

            return new String(output.toByteArray(), StandardCharsets.UTF_8);
        }
    }
}