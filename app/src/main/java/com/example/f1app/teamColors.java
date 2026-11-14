package com.example.f1app;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;

import androidx.annotation.NonNull;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * Team colours that survive a screen change. A colour read once is kept in memory and on disk, so a repeat
 * visit (or a freshly built list) shows the right colours at once instead of the grey default.
 */
final class teamColors {

    interface Callback {
        void onColor(String color); // null when the colour could not be read
    }

    private static final String PREFS = "team_colors";
    private static final String DEFAULT = "#303030";
    private static final Pattern HEX = Pattern.compile("#[0-9a-fA-F]{6}([0-9a-fA-F]{2})?");
    private static final Map<String, String> MEMORY = new ConcurrentHashMap<>();

    private teamColors() {
    }

    /** "ff0000", "#ff0000" or garbage ("null", "") in; a valid "#rrggbb" or null out. */
    static String normalize(String raw) {
        if (raw == null) {
            return null;
        }

        String color = raw.trim();

        if (color.isEmpty() || "null".equalsIgnoreCase(color)) {
            return null;
        }

        if (!color.startsWith("#")) {
            color = "#" + color;
        }

        return HEX.matcher(color).matches() ? color : null;
    }

    /** Never null and never "#null": a valid colour or the grey default. */
    static String hex(String raw) {
        String color = normalize(raw);
        return color != null ? color : DEFAULT;
    }

    static int parse(String raw) {
        try {
            return Color.parseColor(hex(raw));
        } catch (IllegalArgumentException error) {
            return Color.parseColor(DEFAULT);
        }
    }

    static String get(String constructorId) {
        if (constructorId == null || constructorId.isEmpty()) {
            return null;
        }

        String color = MEMORY.get(constructorId);

        if (color != null) {
            return color;
        }

        SharedPreferences prefs = prefs();

        if (prefs != null) {
            color = normalize(prefs.getString(constructorId, null));

            if (color != null) {
                MEMORY.put(constructorId, color);
            }
        }

        return color;
    }

    /** Remembers a colour and returns it normalised (null when it was not a colour). */
    static String put(String constructorId, String raw) {
        String color = normalize(raw);

        if (constructorId == null || constructorId.isEmpty() || color == null) {
            return color;
        }

        MEMORY.put(constructorId, color);

        SharedPreferences prefs = prefs();

        if (prefs != null) {
            prefs.edit().putString(constructorId, color).apply();
        }

        return color;
    }

    static void fetch(DatabaseReference root, String constructorId, Callback callback) {
        root.child("constructors").child(constructorId).child("darkColor")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        callback.onColor(put(constructorId, snapshot.getValue(String.class)));
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        callback.onColor(null);
                    }
                });
    }

    private static SharedPreferences prefs() {
        Context context = ApexApp.context();
        return context == null ? null : context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}
