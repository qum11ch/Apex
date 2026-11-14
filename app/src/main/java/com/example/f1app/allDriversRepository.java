package com.example.f1app;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.AtomicFile;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;

/** Loads the public driver directory and keeps the last successful copy on disk. */
final class allDriversRepository {

    interface Callback<T> {
        void onResult(T value);
    }

    static final class Driver {
        final String id;
        final String givenName;
        final String familyName;
        final String team;
        final String constructorId;
        final String code;
        final String status;
        final String lastEntry;
        final String driverId;
        final int championships;
        final boolean legend;

        Driver(
                String id,
                String givenName,
                String familyName,
                String team,
                String constructorId,
                String code,
                String status,
                String lastEntry,
                String driverId,
                int championships,
                boolean legend
        ) {
            this.id = id;
            this.givenName = givenName;
            this.familyName = familyName;
            this.team = team;
            this.constructorId = constructorId;
            this.code = code;
            this.status = status;
            this.lastEntry = lastEntry;
            this.driverId = driverId;
            this.championships = championships;
            this.legend = legend;
        }
    }

    static final class Snapshot {
        final List<Driver> drivers;
        final long savedAt;

        Snapshot(List<Driver> drivers, long savedAt) {
            this.drivers = Collections.unmodifiableList(drivers);
            this.savedAt = savedAt;
        }
    }

    private static final String TAG = "allDriversRepository";
    private static final String CACHE_FILE = "all_drivers_cache_v1.json";
    private static final int CACHE_VERSION = 2;
    private static final long FRESH_MS = 10L * 60L * 1000L;

    private static final ExecutorService IO = Executors.newSingleThreadExecutor(task -> {
        Thread thread = new Thread(task, "all-drivers-io");
        thread.setDaemon(true);
        return thread;
    });

    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    /** The last snapshot of this process: a repeat visit shows it without waiting for the disk. */
    private static volatile Snapshot memory;

    static Snapshot memorySnapshot() {
        return memory;
    }

    private allDriversRepository() {
    }

    static boolean isFresh(@NonNull Snapshot snapshot, long now) {
        long age = now - snapshot.savedAt;
        return age >= 0 && age < FRESH_MS;
    }

    // ---------- disk cache ----------

    static void readCache(Context context, Callback<Snapshot> callback) {
        final Context app = context.getApplicationContext();

        IO.execute(() -> {
            Snapshot result = readCacheNow(app);
            if (result != null) {
                memory = result;
            }
            MAIN.post(() -> callback.onResult(result));
        });
    }

    static void writeCache(Context context, Snapshot snapshot) {
        final Context app = context.getApplicationContext();
        memory = snapshot;
        IO.execute(() -> writeCacheNow(app, snapshot));
    }

    @Nullable
    private static Snapshot readCacheNow(Context context) {
        AtomicFile file = new AtomicFile(new File(context.getFilesDir(), CACHE_FILE));

        try {
            if (!file.getBaseFile().exists()) {
                return null;
            }

            JSONObject root = new JSONObject(
                    new String(file.readFully(), StandardCharsets.UTF_8)
            );

            if (root.optInt("version", -1) != CACHE_VERSION) {
                return null;
            }

            JSONArray array = root.getJSONArray("drivers");
            List<Driver> drivers = new ArrayList<>();

            for (int i = 0; i < array.length(); i++) {
                JSONObject item = array.getJSONObject(i);
                String id = item.optString("id", "");
                String code = item.optString("code", "");

                if (id.isEmpty() || code.isEmpty()) {
                    continue;
                }

                drivers.add(new Driver(
                        id,
                        item.optString("givenName", ""),
                        item.optString("familyName", ""),
                        item.optString("team", ""),
                        item.optString("constructorId", ""),
                        code,
                        item.optString("status", ""),
                        item.has("lastEntry") ? item.optString("lastEntry", null) : null,
                        item.has("driverId") ? item.optString("driverId", null) : null,
                        item.optInt("championships", 0),
                        item.optBoolean("legend", false)
                ));
            }

            if (drivers.isEmpty()) {
                return null;
            }

            return new Snapshot(drivers, root.getLong("savedAt"));
        } catch (Exception error) {
            Log.w(TAG, "Ignoring unreadable drivers cache", error);
            return null;
        }
    }

    private static void writeCacheNow(Context context, Snapshot snapshot) {
        AtomicFile file = new AtomicFile(new File(context.getFilesDir(), CACHE_FILE));
        FileOutputStream stream = null;

        try {
            JSONArray array = new JSONArray();

            for (Driver driver : snapshot.drivers) {
                JSONObject item = new JSONObject();
                item.put("id", driver.id);
                item.put("givenName", driver.givenName);
                item.put("familyName", driver.familyName);
                item.put("team", driver.team);
                item.put("constructorId", driver.constructorId);
                item.put("code", driver.code);
                item.put("status", driver.status);
                item.put("championships", driver.championships);
                item.put("legend", driver.legend);

                if (driver.lastEntry != null) {
                    item.put("lastEntry", driver.lastEntry);
                }
                if (driver.driverId != null) {
                    item.put("driverId", driver.driverId);
                }

                array.put(item);
            }

            JSONObject root = new JSONObject();
            root.put("version", CACHE_VERSION);
            root.put("savedAt", snapshot.savedAt);
            root.put("drivers", array);

            stream = file.startWrite();
            stream.write(root.toString().getBytes(StandardCharsets.UTF_8));
            file.finishWrite(stream);
        } catch (Exception error) {
            Log.w(TAG, "Could not save drivers cache", error);

            if (stream != null) {
                file.failWrite(stream);
            }
        }
    }

    // ---------- network ----------

    /** Both callbacks run on the main thread. A failed request never produces an empty snapshot. */
    static void fetch(Callback<Snapshot> success, Callback<Exception> failure) {
        DatabaseReference root = FirebaseDatabase.getInstance().getReference();

        Task<DataSnapshot> driversTask = root.child("drivers").get();

        // Constructors only improve team matching, so their failure must not block the screen.
        Task<Map<String, String>> teamsTask = root.child("constructors").get()
                .continueWith(task -> task.isSuccessful()
                        ? parseTeams(task.getResult())
                        : Collections.<String, String>emptyMap());

        Tasks.whenAllComplete(driversTask, teamsTask).addOnCompleteListener(done -> {
            if (!driversTask.isSuccessful()) {
                Exception error = driversTask.getException();
                failure.onResult(error != null
                        ? error
                        : new IllegalStateException("Drivers request failed"));
                return;
            }

            try {
                Map<String, String> teams = teamsTask.isSuccessful()
                        ? teamsTask.getResult()
                        : Collections.<String, String>emptyMap();

                List<Driver> drivers = parseDrivers(driversTask.getResult(), teams);

                if (drivers.isEmpty()) {
                    failure.onResult(new IllegalStateException("Drivers list is empty"));
                    return;
                }

                success.onResult(new Snapshot(drivers, System.currentTimeMillis()));
            } catch (RuntimeException error) {
                failure.onResult(error);
            }
        });
    }

    private static Map<String, String> parseTeams(DataSnapshot snapshot) {
        Map<String, String> idsByName = new HashMap<>();

        for (DataSnapshot team : snapshot.getChildren()) {
            String name = team.child("name").getValue(String.class);
            String id = team.child("constructorId").getValue(String.class);

            if (id == null || id.trim().isEmpty()) {
                id = team.getKey();
            }

            if (name != null && id != null && !name.trim().isEmpty()) {
                idsByName.put(name.trim().toLowerCase(Locale.ROOT), id.trim());
            }
        }

        return idsByName;
    }

    static List<Driver> parseDrivers(DataSnapshot snapshot, Map<String, String> teamIdsByName) {
        List<Driver> drivers = new ArrayList<>();

        for (DataSnapshot item : snapshot.getChildren()) {
            String fullName = text(item.child("driverName").getValue());
            String code = text(item.child("driversCode").getValue());

            if (fullName == null || code == null) {
                continue;
            }

            String id = item.getKey() != null ? item.getKey() : fullName;
            String[] parts = driverNameFormatter.split(fullName);

            String givenName = text(item.child("givenName").getValue());
            String familyName = text(item.child("familyName").getValue());

            if (givenName == null || familyName == null) {
                givenName = parts[0];
                familyName = parts[1];
            }

            String team = text(item.child("driversTeam").getValue());
            String teamName = team != null ? team : "";

            drivers.add(new Driver(
                    id,
                    givenName,
                    familyName,
                    teamName,
                    resolveConstructorId(item, teamName, teamIdsByName),
                    code,
                    normalizeStatus(item.child("status").getValue()),
                    text(item.child("lastEntry").getValue()),
                    text(item.child("driverId").getValue()),
                    parseChampionships(item.child("championshipsCount").getValue()),
                    isTrue(item.child("legendStatus").getValue())
            ));
        }

        Collator collator = Collator.getInstance(Locale.ENGLISH);
        collator.setStrength(Collator.PRIMARY);

        Collections.sort(drivers, new Comparator<Driver>() {
            @Override
            public int compare(Driver a, Driver b) {
                int byStatus = Integer.compare(statusRank(a.status), statusRank(b.status));
                if (byStatus != 0) return byStatus;

                int byYear = Integer.compare(
                        lastEntryYear(b.lastEntry),
                        lastEntryYear(a.lastEntry)
                );
                if (byYear != 0) return byYear;

                int byFamily = collator.compare(a.familyName, b.familyName);
                if (byFamily != 0) return byFamily;

                int byGiven = collator.compare(a.givenName, b.givenName);
                if (byGiven != 0) return byGiven;

                return a.id.compareTo(b.id);
            }
        });

        return drivers;
    }

    private static String resolveConstructorId(
            DataSnapshot driver,
            String teamName,
            Map<String, String> teamIdsByName
    ) {
        String byName = teamIdsByName.get(teamName.trim().toLowerCase(Locale.ROOT));

        if (byName != null && !byName.isEmpty()) {
            return byName;
        }

        for (DataSnapshot team : driver.child("team").getChildren()) {
            if (isTrue(team.getValue()) && team.getKey() != null) {
                return team.getKey();
            }
        }

        return "";
    }

    private static final Pattern YEAR = Pattern.compile("\\b(\\d{4})\\b");

    /** Year of the last race ("2026 Mexico City Grand Prix" -> 2026); 0 when unknown, so such drivers go last. */
    private static int lastEntryYear(String lastEntry) {
        if (lastEntry == null) {
            return 0;
        }

        java.util.regex.Matcher matcher = YEAR.matcher(lastEntry);
        return matcher.find() ? Integer.parseInt(matcher.group(1)) : 0;
    }

    private static int statusRank(String status) {
        if ("active".equals(status)) return 0;
        if ("reserve".equals(status)) return 1;
        if ("retired".equals(status)) return 2;
        return 3;
    }

    private static String normalizeStatus(Object value) {
        String status = text(value);
        return status != null ? status.toLowerCase(Locale.ROOT) : "";
    }

    private static boolean isTrue(Object value) {
        return Boolean.TRUE.equals(value)
                || (value instanceof String && "true".equalsIgnoreCase(((String) value).trim()));
    }

    @Nullable
    private static String text(Object value) {
        if (value == null) {
            return null;
        }

        String result = value.toString().trim();
        return result.isEmpty() ? null : result;
    }

    private static int parseChampionships(Object value) {
        if (value == null) {
            return 0;
        }

        if (value instanceof Number) {
            return ((Number) value).intValue();
        }

        try {
            return Integer.parseInt(value.toString().trim());
        } catch (NumberFormatException error) {
            return 0;
        }
    }
}
