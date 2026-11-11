package com.example.f1app;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.AtomicFile;
import android.util.Log;

import androidx.annotation.NonNull;

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
import java.security.MessageDigest;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Loads the user's saved races and joins them with schedule and circuit data in a few bulk reads
 * (one per season plus one for circuits) instead of two reads per card.
 */
final class savedRacesRepository {

    interface Callback<T> {
        void onResult(T value);
    }

    static final class Result {
        final List<savedRaceCard> races;
        final long savedAt;
        /** False when the schedule or circuits could not be read; such a result must not replace the cache. */
        final boolean complete;

        Result(List<savedRaceCard> races, long savedAt, boolean complete) {
            this.races = races;
            this.savedAt = savedAt;
            this.complete = complete;
        }
    }

    private static final String TAG = "savedRacesRepository";
    private static final int CACHE_VERSION = 1;
    private static final long FRESH_MS = 5L * 60L * 1000L;

    private static final ExecutorService IO = Executors.newSingleThreadExecutor(task -> {
        Thread thread = new Thread(task, "saved-races-io");
        thread.setDaemon(true);
        return thread;
    });

    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    private savedRacesRepository() {
    }

    static boolean isFresh(@NonNull Result result, long now) {
        long age = now - result.savedAt;
        return age >= 0 && age < FRESH_MS;
    }

    // ---------- disk cache (one file per account) ----------

    private static File cacheFile(Context context, String uid) {
        return new File(context.getFilesDir(), "saved_races_cache_v1_" + hash(uid) + ".json");
    }

    private static String hash(String value) {
        try {
            byte[] bytes = MessageDigest.getInstance("SHA-1")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder();

            for (int i = 0; i < 8; i++) {
                result.append(String.format("%02x", bytes[i]));
            }

            return result.toString();
        } catch (Exception error) {
            return Integer.toHexString(value.hashCode());
        }
    }

    static void readCache(Context context, String uid, Callback<Result> callback) {
        final Context app = context.getApplicationContext();

        IO.execute(() -> {
            Result result = readCacheNow(app, uid);
            MAIN.post(() -> callback.onResult(result));
        });
    }

    static void writeCache(Context context, String uid, Result result) {
        final Context app = context.getApplicationContext();
        IO.execute(() -> writeCacheNow(app, uid, result));
    }

    private static Result readCacheNow(Context context, String uid) {
        AtomicFile file = new AtomicFile(cacheFile(context, uid));

        try {
            if (!file.getBaseFile().exists()) {
                return null;
            }

            JSONObject root = new JSONObject(
                    new String(file.readFully(), StandardCharsets.UTF_8)
            );

            if (root.optInt("version", -1) != CACHE_VERSION
                    || !uid.equals(root.optString("uid", ""))) {
                return null;
            }

            JSONArray array = root.getJSONArray("races");
            List<savedRaceCard> races = new ArrayList<>();

            for (int i = 0; i < array.length(); i++) {
                races.add(savedRaceCard.fromJson(array.getJSONObject(i)));
            }

            return new Result(races, root.getLong("savedAt"), true);
        } catch (Exception error) {
            Log.w(TAG, "Ignoring unreadable saved races cache", error);
            return null;
        }
    }

    private static void writeCacheNow(Context context, String uid, Result result) {
        AtomicFile file = new AtomicFile(cacheFile(context, uid));
        FileOutputStream stream = null;

        try {
            JSONArray array = new JSONArray();

            for (savedRaceCard race : result.races) {
                array.put(race.toJson());
            }

            JSONObject root = new JSONObject();
            root.put("version", CACHE_VERSION);
            root.put("uid", uid);
            root.put("savedAt", result.savedAt);
            root.put("races", array);

            stream = file.startWrite();
            stream.write(root.toString().getBytes(StandardCharsets.UTF_8));
            file.finishWrite(stream);
        } catch (Exception error) {
            Log.w(TAG, "Could not save saved races cache", error);

            if (stream != null) {
                file.failWrite(stream);
            }
        }
    }

    // ---------- network ----------

    /** Both callbacks run on the main thread. */
    static void fetch(String uid, Callback<Result> success, Callback<Exception> failure) {
        DatabaseReference root = FirebaseDatabase.getInstance().getReference();

        root.child("savedRaces").child(uid).get().addOnCompleteListener(savedTask -> {
            if (!savedTask.isSuccessful()) {
                Exception error = savedTask.getException();
                failure.onResult(error != null
                        ? error
                        : new IllegalStateException("Saved races request failed"));
                return;
            }

            List<savedRaceCard> races = parseSaved(savedTask.getResult());

            if (races.isEmpty()) {
                success.onResult(new Result(races, System.currentTimeMillis(), true));
                return;
            }

            Set<String> seasons = new LinkedHashSet<>();

            for (savedRaceCard race : races) {
                seasons.add(race.raceSeason);
            }

            Map<String, Task<DataSnapshot>> scheduleTasks = new LinkedHashMap<>();
            List<Task<?>> all = new ArrayList<>();

            for (String season : seasons) {
                Task<DataSnapshot> task = root.child("schedule").child("season").child(season).get();
                scheduleTasks.put(season, task);
                all.add(task);
            }

            Task<DataSnapshot> circuitsTask = root.child("circuits").get();
            all.add(circuitsTask);

            Tasks.whenAllComplete(all).addOnCompleteListener(done -> {
                boolean complete = circuitsTask.isSuccessful();
                DataSnapshot circuits = complete ? circuitsTask.getResult() : null;

                try {
                    for (savedRaceCard race : races) {
                        Task<DataSnapshot> task = scheduleTasks.get(race.raceSeason);

                        if (task == null || !task.isSuccessful()) {
                            complete = false;
                            continue;
                        }

                        DataSnapshot schedule = task.getResult().child(race.raceName);

                        if (schedule.exists()) {
                            applySchedule(race, schedule, circuits);
                        }
                    }
                } catch (RuntimeException error) {
                    failure.onResult(error);
                    return;
                }

                success.onResult(new Result(races, System.currentTimeMillis(), complete));
            });
        });
    }

    private static List<savedRaceCard> parseSaved(DataSnapshot snapshot) {
        List<savedRaceCard> races = new ArrayList<>();

        for (DataSnapshot item : snapshot.getChildren()) {
            String key = item.getKey();
            String raceName = text(item.child("raceName").getValue());
            String raceSeason = text(item.child("raceSeason").getValue());
            String saveDate = text(item.child("saveDate").getValue());

            if (key == null || raceName == null || raceSeason == null) {
                continue;
            }

            races.add(new savedRaceCard(key, raceName, raceSeason, saveDate != null ? saveDate : ""));
        }

        return races;
    }

    private static void applySchedule(savedRaceCard race, DataSnapshot schedule, DataSnapshot circuits) {
        race.scheduleKnown = true;
        race.round = number(schedule.child("round").getValue());
        race.raceDate = text(schedule.child("raceDate").getValue());
        race.raceTime = text(schedule.child("raceTime").getValue());
        race.firstPracticeDate = text(schedule.child("FirstPractice").child("firstPracticeDate").getValue());
        race.firstPracticeTime = text(schedule.child("FirstPractice").child("firstPracticeTime").getValue());

        DataSnapshot results = schedule.child("RaceResults");
        race.winnerCode = text(results.child("raceWinnerCode").getValue());
        race.secondCode = text(results.child("raceSecondCode").getValue());
        race.thirdCode = text(results.child("raceThirdCode").getValue());
        race.fastestLapDriver = text(results.child("fastestLapDriver").getValue());
        race.fastestLapTime = text(results.child("fastestLapTime").getValue());

        race.sprintDate = text(schedule.child("Sprint").child("sprintRaceDate").getValue());
        race.sprintWinnerCode = text(schedule.child("SprintResults").child("sprintWinnerCode").getValue());

        DataSnapshot canceled = schedule.child("Canceled");
        race.canceled = canceled.exists() && !Boolean.FALSE.equals(canceled.getValue());

        race.circuitId = text(schedule.child("Circuit").child("circuitId").getValue());

        if (circuits != null && race.circuitId != null) {
            DataSnapshot circuit = circuits.child(race.circuitId);
            race.circuitName = text(circuit.child("circuitName").getValue());
            race.country = text(circuit.child("country").getValue());
            race.length = text(circuit.child("length").getValue());
            race.lapsCount = text(circuit.child("lapsCount").getValue());
            race.turnsCount = text(circuit.child("turnsCount").getValue());
            race.lapRecordTime = text(circuit.child("lapRecordTime").getValue());
            race.lapRecordDriver = text(circuit.child("lapRecordDriver").getValue());
        }
    }

    // ---------- ordering ----------

    /**
     * Running and upcoming races first (the nearest one on top), then finished ones with the newest on top.
     * States depend on the current time, so the order is computed again every time the list is shown.
     */
    static void sortForDisplay(List<savedRaceCard> races, Instant now) {
        Collections.sort(races, new Comparator<savedRaceCard>() {
            @Override
            public int compare(savedRaceCard a, savedRaceCard b) {
                int byGroup = Integer.compare(group(a, now), group(b, now));
                if (byGroup != 0) return byGroup;

                long timeA = millis(a);
                long timeB = millis(b);
                int byTime = group(a, now) == 3
                        ? Long.compare(timeB, timeA)
                        : Long.compare(timeA, timeB);

                return byTime != 0 ? byTime : a.key.compareTo(b.key);
            }
        });
    }

    private static int group(savedRaceCard race, Instant now) {
        switch (race.stateAt(now)) {
            case IN_PROGRESS:
                return 0;
            case RESULTS_PENDING:
                return 1;
            case UPCOMING:
                return 2;
            default:
                return 3;
        }
    }

    private static long millis(savedRaceCard race) {
        Instant start = race.raceStart();
        return start != null ? start.toEpochMilli() : Long.MAX_VALUE;
    }

    // ---------- values ----------

    private static String text(Object value) {
        if (value == null) {
            return null;
        }

        String result = value.toString().trim();
        return result.isEmpty() ? null : result;
    }

    private static int number(Object value) {
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }

        try {
            return value != null ? Integer.parseInt(value.toString().trim()) : 0;
        } catch (NumberFormatException error) {
            return 0;
        }
    }
}
