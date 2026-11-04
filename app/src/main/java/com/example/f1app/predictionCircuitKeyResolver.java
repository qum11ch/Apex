package com.example.f1app;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class predictionCircuitKeyResolver {
    private predictionCircuitKeyResolver(){}

    public static List<Integer> resolve(
            String circuitId,
            String circuitName,
            String location,
            List<Integer> candidateKeys
    ) {
        if (candidateKeys == null || candidateKeys.isEmpty()) {
            return Collections.emptyList();
        }

        List<Integer> result = new ArrayList<>(candidateKeys);

        Integer exact = exactKey(circuitId);
        if (exact != null) {
            return contains(result, exact)
                    ? Collections.singletonList(exact)
                    : Collections.emptyList();
        }

        List<Integer> known = knownKeys(circuitId, circuitName, location);

        if (!known.isEmpty()) {
            List<Integer> filtered = new ArrayList<>();

            for (Integer key : result) {
                if (known.contains(key)) {
                    filtered.add(key);
                }
            }

            if (!filtered.isEmpty()) {
                return filtered;
            }
        }

        return result;
    }

    private static Integer exactKey(String circuitId) {
        if (circuitId == null) {
            return null;
        }

        switch (circuitId.trim().toLowerCase(Locale.ROOT)) {
            case "interlagos":
                return 63;
            case "monza":
                return 14;
            case "silverstone":
                return 9;
            case "spa":
                return 13;
            case "monaco":
                return 6;
            case "suzuka":
                return 22;
            default:
                return null;
        }
    }

    private static List<Integer> knownKeys(
            String circuitId,
            String circuitName,
            String location
    ) {
        String id = normalize(circuitId);
        String name = normalize(circuitName);
        String place = normalize(location);

        Set<Integer> keys = new LinkedHashSet<>();

        if ("interlagos".equals(id)
                || containsAny(name, "interlagos", "jose carlos pace")
                || containsAny(place, "sao paulo", "são paulo")) {
            keys.add(63);
        }

        if ("monza".equals(id)
                || containsAny(name, "monza", "nazionale monza")
                || containsAny(place, "monza")) {
            keys.add(14);
        }

        if ("silverstone".equals(id)
                || containsAny(name, "silverstone")
                || containsAny(place, "silverstone")) {
            keys.add(9);
        }

        if ("spa".equals(id)
                || containsAny(name, "spa-francorchamps", "spa francorchamps")
                || containsAny(place, "spa", "stavelot")) {
            keys.add(13);
        }

        if ("monaco".equals(id)
                || containsAny(name, "monaco", "monte carlo")
                || containsAny(place, "monaco", "monte carlo")) {
            keys.add(6);
        }

        if ("suzuka".equals(id)
                || containsAny(name, "suzuka")
                || containsAny(place, "suzuka")) {
            keys.add(22);
        }

        return new ArrayList<>(keys);
    }

    private static boolean contains(
            List<Integer> values,
            Integer value
    ) {
        return values != null && values.contains(value);
    }

    private static boolean containsAny(
            String value,
            String... needles
    ) {
        if (value == null || value.isEmpty()) {
            return false;
        }

        for (String needle : needles) {
            if (value.contains(needle)) {
                return true;
            }
        }

        return false;
    }

    private static String normalize(String value) {
        return value == null
                ? ""
                : value.trim().toLowerCase(Locale.ROOT);
    }
}
