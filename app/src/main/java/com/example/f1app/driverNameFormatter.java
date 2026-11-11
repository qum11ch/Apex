package com.example.f1app;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Splits a full driver name without losing words and builds display names. */
final class driverNameFormatter {

    private static final Set<String> PARTICLES = new HashSet<>(Arrays.asList(
            "de", "da", "di", "del", "della", "van", "von", "der", "den",
            "du", "le", "la", "dos", "das", "ten", "ter"
    ));

    private static final Map<String, String> DISPLAY_GIVEN_NAMES = new HashMap<>();

    static {
        DISPLAY_GIVEN_NAMES.put("Andrea Kimi", "Kimi");
    }

    private driverNameFormatter() {
    }

    /** Returns {givenName, familyName}; given + " " + family always equals the cleaned full name. */
    static String[] split(String fullName) {
        String normalized = fullName == null
                ? ""
                : fullName.trim().replaceAll("\\s+", " ");

        if (normalized.isEmpty()) {
            return new String[]{"", ""};
        }

        String[] tokens = normalized.split(" ");

        if (tokens.length == 1) {
            return new String[]{tokens[0], ""};
        }

        int familyStart = tokens.length - 1;

        for (int i = 1; i < tokens.length - 1; i++) {
            if (PARTICLES.contains(tokens[i].toLowerCase(Locale.ROOT))) {
                familyStart = i;
                break;
            }
        }

        return new String[]{
                join(tokens, 0, familyStart),
                join(tokens, familyStart, tokens.length)
        };
    }

    static String fullName(String givenName, String familyName) {
        String given = givenName == null ? "" : givenName.trim();
        String family = familyName == null ? "" : familyName.trim();
        return (given + " " + family).trim();
    }

    static String displayGivenName(String givenName) {
        String given = givenName == null ? "" : givenName.trim();
        String display = DISPLAY_GIVEN_NAMES.get(given);
        return display != null ? display : given;
    }

    private static String join(String[] tokens, int start, int end) {
        StringBuilder result = new StringBuilder();

        for (int i = start; i < end; i++) {
            if (result.length() > 0) {
                result.append(' ');
            }
            result.append(tokens[i]);
        }

        return result.toString();
    }
}
