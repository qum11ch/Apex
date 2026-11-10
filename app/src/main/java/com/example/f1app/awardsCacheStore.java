package com.example.f1app;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.firebase.FirebaseApp;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Disk storage for award collections and displayed-award selections.
 *
 * Only one signed-in account is stored at a time: switching or signing out
 * wipes the file. Every record also carries its account and owner UID and is
 * rejected on mismatch. Disk methods may block, so call them through
 * {@link #execute(Runnable)}, never from the main thread.
 */
final class awardsCacheStore {

    static final class AwardsRecord {

        final List<Map<String, Object>> badges;
        final long savedAt;

        AwardsRecord(List<Map<String, Object>> badges, long savedAt) {
            this.badges = badges;
            this.savedAt = savedAt;
        }
    }

    static final class SelectionRecord {

        final String id;
        final Map<String, Object> award;
        final long savedAt;

        SelectionRecord(String id, Map<String, Object> award, long savedAt) {
            this.id = id;
            this.award = award;
            this.savedAt = savedAt;
        }
    }

    private static final String TAG = "AwardsCacheStore";
    private static final String FILE_NAME = "apex_awards_cache";
    private static final String KEY_ACCOUNT = "account";
    private static final String AWARDS_PREFIX = "awards|";
    private static final String SELECTION_PREFIX = "selection|";
    private static final String TIME_PREFIX = "time|";
    private static final int SCHEMA = 1;
    private static final int MAX_RECORDS = 300;
    private static final double MAX_EXACT_INTEGER = 9.0e15;

    /** One shared thread keeps all disk operations in the order they were queued. */
    private static final ExecutorService DISK = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "awards-cache");
        thread.setDaemon(true);
        return thread;
    });

    private awardsCacheStore() {
    }

    static void execute(Runnable task) {
        DISK.execute(task);
    }

    private static SharedPreferences preferences() {
        Context context = FirebaseApp.getInstance().getApplicationContext();
        return context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE);
    }

    private static String awardsKey(String owner) {
        return AWARDS_PREFIX + owner;
    }

    private static String selectionKey(String owner) {
        return SELECTION_PREFIX + owner;
    }

    private static boolean belongsTo(SharedPreferences preferences, String account) {
        return account != null && account.equals(preferences.getString(KEY_ACCOUNT, null));
    }

    // ---------------------------------------------------------------- account

    /** Wipes the file unless it already belongs to {@code account}. */
    static synchronized void bindAccount(String account) {
        SharedPreferences preferences = preferences();
        String stored = preferences.getString(KEY_ACCOUNT, null);

        if (account != null && account.equals(stored)) {
            return;
        }

        SharedPreferences.Editor editor = preferences.edit();
        editor.clear();

        if (account != null) {
            editor.putString(KEY_ACCOUNT, account);
        }

        editor.apply();
    }

    // ----------------------------------------------------------------- awards

    static synchronized AwardsRecord readAwards(String account, String owner) {
        SharedPreferences preferences = preferences();

        if (!belongsTo(preferences, account)) {
            return null;
        }

        String key = awardsKey(owner);
        String raw = preferences.getString(key, null);

        if (raw == null) {
            return null;
        }

        try {
            JSONObject json = new JSONObject(raw);

            if (!isValid(json, account, owner)) {
                remove(preferences, key);
                return null;
            }

            JSONArray array = json.getJSONArray("badges");
            List<Map<String, Object>> badges = new ArrayList<>();

            for (int index = 0; index < array.length(); index++) {
                Object value = fromJson(array.get(index));

                if (!(value instanceof Map)) {
                    throw new JSONException("Invalid badge");
                }

                badges.add(asMap(value));
            }

            return new AwardsRecord(badges, json.getLong("savedAt"));
        } catch (JSONException | RuntimeException error) {
            Log.w(TAG, "Dropping unreadable awards record", error);
            remove(preferences, key);
            return null;
        }
    }

    static synchronized void writeAwards(
            String account,
            String owner,
            List<Map<String, Object>> badges,
            long savedAt
    ) {
        SharedPreferences preferences = preferences();

        if (!belongsTo(preferences, account)) {
            return;
        }

        try {
            JSONObject json = header(account, owner, savedAt);
            json.put("badges", encode(canonical(badges)));
            save(preferences, awardsKey(owner), json, savedAt);
        } catch (JSONException | RuntimeException error) {
            Log.w(TAG, "Awards record was not saved", error);
        }
    }

    // -------------------------------------------------------------- selection

    static synchronized SelectionRecord readSelection(String account, String owner) {
        SharedPreferences preferences = preferences();

        if (!belongsTo(preferences, account)) {
            return null;
        }

        String key = selectionKey(owner);
        String raw = preferences.getString(key, null);

        if (raw == null) {
            return null;
        }

        try {
            JSONObject json = new JSONObject(raw);

            if (!isValid(json, account, owner)) {
                remove(preferences, key);
                return null;
            }

            String id = json.isNull("id") ? null : json.getString("id");
            Map<String, Object> award = null;

            if (!json.isNull("award")) {
                award = asMap(fromJson(json.getJSONObject("award")));
            }

            if (id != null && award == null) {
                throw new JSONException("Selection without award");
            }

            return new SelectionRecord(id, award, json.getLong("savedAt"));
        } catch (JSONException | RuntimeException error) {
            Log.w(TAG, "Dropping unreadable selection record", error);
            remove(preferences, key);
            return null;
        }
    }

    static synchronized void writeSelection(
            String account,
            String owner,
            String id,
            Map<String, Object> award,
            long savedAt
    ) {
        SharedPreferences preferences = preferences();

        if (!belongsTo(preferences, account)) {
            return;
        }

        try {
            JSONObject json = header(account, owner, savedAt);
            json.put("id", id == null ? JSONObject.NULL : id);
            json.put("award", award == null ? JSONObject.NULL : encode(canonical(award)));
            save(preferences, selectionKey(owner), json, savedAt);
        } catch (JSONException | RuntimeException error) {
            Log.w(TAG, "Selection record was not saved", error);
        }
    }

    // ---------------------------------------------------------- canonical form

    /**
     * Deep copy with a single numeric representation (Long or Double), so a value
     * read from disk is equal to the same value received from the network.
     */
    static Object canonical(Object value) {
        if (value == null) {
            return null;
        }

        if (value instanceof Map) {
            Map<String, Object> result = new HashMap<>();

            for (Map.Entry<?, ?> entry : ((Map<?, ?>) value).entrySet()) {
                if (entry.getKey() instanceof String) {
                    result.put((String) entry.getKey(), canonical(entry.getValue()));
                }
            }

            return result;
        }

        if (value instanceof List) {
            List<Object> result = new ArrayList<>();

            for (Object item : (List<?>) value) {
                result.add(canonical(item));
            }

            return result;
        }

        if (value instanceof Number) {
            return canonicalNumber((Number) value);
        }

        if (value instanceof String || value instanceof Boolean) {
            return value;
        }

        return String.valueOf(value);
    }

    static List<Map<String, Object>> canonicalBadges(List<Map<String, Object>> raw) {
        List<Map<String, Object>> result = new ArrayList<>();

        for (Map<String, Object> badge : raw) {
            result.add(asMap(canonical(badge)));
        }

        return result;
    }

    static Map<String, Object> canonicalMap(Map<String, Object> raw) {
        return raw == null ? null : asMap(canonical(raw));
    }

    /** Order-independent text form used to compare two values. */
    static String fingerprint(Object value) {
        try {
            return String.valueOf(encode(canonical(value)));
        } catch (JSONException error) {
            return String.valueOf(value);
        }
    }

    private static Object canonicalNumber(Number number) {
        if (number instanceof Integer
                || number instanceof Long
                || number instanceof Short
                || number instanceof Byte) {
            return number.longValue();
        }

        double value = number.doubleValue();

        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return null;
        }

        if (value == Math.rint(value) && Math.abs(value) < MAX_EXACT_INTEGER) {
            return (long) value;
        }

        return value;
    }

    // ------------------------------------------------------------------- JSON

    private static Object encode(Object value) throws JSONException {
        if (value == null) {
            return JSONObject.NULL;
        }

        if (value instanceof Map) {
            JSONObject object = new JSONObject();
            Map<String, Object> sorted = new TreeMap<>(asMap(value));

            for (Map.Entry<String, Object> entry : sorted.entrySet()) {
                object.put(entry.getKey(), encode(entry.getValue()));
            }

            return object;
        }

        if (value instanceof List) {
            JSONArray array = new JSONArray();

            for (Object item : (List<?>) value) {
                array.put(encode(item));
            }

            return array;
        }

        return value;
    }

    private static Object fromJson(Object value) throws JSONException {
        if (value == null || JSONObject.NULL.equals(value)) {
            return null;
        }

        if (value instanceof JSONObject) {
            JSONObject object = (JSONObject) value;
            Map<String, Object> result = new HashMap<>();
            Iterator<String> keys = object.keys();

            while (keys.hasNext()) {
                String key = keys.next();
                result.put(key, fromJson(object.get(key)));
            }

            return result;
        }

        if (value instanceof JSONArray) {
            JSONArray array = (JSONArray) value;
            List<Object> result = new ArrayList<>();

            for (int index = 0; index < array.length(); index++) {
                result.add(fromJson(array.get(index)));
            }

            return result;
        }

        if (value instanceof Number) {
            return canonicalNumber((Number) value);
        }

        return value;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> asMap(Object value) {
        return (Map<String, Object>) value;
    }

    private static JSONObject header(String account, String owner, long savedAt)
            throws JSONException {
        JSONObject json = new JSONObject();
        json.put("v", SCHEMA);
        json.put("account", account);
        json.put("owner", owner);
        json.put("savedAt", savedAt);
        return json;
    }

    private static boolean isValid(JSONObject json, String account, String owner) {
        return json.optInt("v", -1) == SCHEMA
                && account.equals(json.optString("account", ""))
                && owner.equals(json.optString("owner", ""));
    }

    // ----------------------------------------------------------------- storage

    private static void save(
            SharedPreferences preferences,
            String key,
            JSONObject json,
            long savedAt
    ) {
        preferences.edit()
                .putString(key, json.toString())
                .putLong(TIME_PREFIX + key, savedAt)
                .apply();

        trim(preferences);
    }

    private static void remove(SharedPreferences preferences, String key) {
        preferences.edit()
                .remove(key)
                .remove(TIME_PREFIX + key)
                .apply();
    }

    /** Removes the oldest records once the file grows past {@link #MAX_RECORDS}. */
    private static void trim(SharedPreferences preferences) {
        final Map<String, ?> all = preferences.getAll();
        List<String> keys = new ArrayList<>();

        for (String key : all.keySet()) {
            if (key.startsWith(AWARDS_PREFIX) || key.startsWith(SELECTION_PREFIX)) {
                keys.add(key);
            }
        }

        if (keys.size() <= MAX_RECORDS) {
            return;
        }

        Collections.sort(keys, (first, second) ->
                Long.compare(timeOf(all, first), timeOf(all, second)));

        SharedPreferences.Editor editor = preferences.edit();
        int excess = keys.size() - MAX_RECORDS;

        for (int index = 0; index < excess; index++) {
            String key = keys.get(index);
            editor.remove(key);
            editor.remove(TIME_PREFIX + key);
        }

        editor.apply();
    }

    private static long timeOf(Map<String, ?> all, String key) {
        Object value = all.get(TIME_PREFIX + key);
        return value instanceof Long ? (Long) value : 0L;
    }
}
