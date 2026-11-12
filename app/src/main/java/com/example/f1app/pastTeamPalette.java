package com.example.f1app;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;


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

    static String idForName(String teamName) {
        if (teamName == null) {
            return null;
        }

        String name = teamName.toLowerCase(Locale.ROOT).trim();

        if (name.contains("alfa")) return "alfa";
        if (name.contains("alphatauri") || name.contains("alpha tauri")) return "alphatauri";
        if (name.contains("toro rosso")) return "toro_rosso";
        if (name.contains("sauber")) return "sauber";
        if (name.contains("force india")) return "force_india";
        if (name.contains("racing point")) return "racing_point";
        if (name.contains("renault")) return "renault";

        return null;
    }

    private static String normalize(String hex) {
        return hex.startsWith("#") ? hex : "#" + hex;
    }
}
