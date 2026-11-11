package com.example.f1app;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Dark team colours for teams that are not in the constructors node (they left the grid before 2024).
 * The values are approximations of the livery colours: adjust them freely.
 * The constructors node stays untouched, because the current standings list every team in it.
 */
final class pastTeamPalette {

    private static final String FALLBACK = "#303030";
    private static final Pattern HEX = Pattern.compile("^#?[0-9A-Fa-f]{6}$");
    private static final Map<String, String> PALETTE = new HashMap<>();

    static {
        PALETTE.put("renault", "5C4F00");
        PALETTE.put("force_india", "5B2A44");
        PALETTE.put("racing_point", "5B2A44");
        PALETTE.put("toro_rosso", "1B2A5A");
        PALETTE.put("alphatauri", "1B2A3A");
        PALETTE.put("alfa", "5C0A15");
        PALETTE.put("sauber", "5C0A15");
    }

    private pastTeamPalette() {
    }

    /** The colour from the database when it is valid, else the palette, else a neutral grey. Always "#RRGGBB". */
    static String resolve(String constructorId, String databaseColor) {
        if (databaseColor != null && HEX.matcher(databaseColor.trim()).matches()) {
            return normalize(databaseColor.trim());
        }

        if (constructorId != null) {
            String known = PALETTE.get(constructorId.toLowerCase());

            if (known != null) {
                return normalize(known);
            }
        }

        return FALLBACK;
    }

    private static String normalize(String hex) {
        return hex.startsWith("#") ? hex : "#" + hex;
    }
}
