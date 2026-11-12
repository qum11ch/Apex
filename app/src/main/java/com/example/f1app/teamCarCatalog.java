package com.example.f1app;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Where the car photos of a team live in Storage ("teams/{id}_{year}.png") when the team
 * raced under another name: Alfa Romeo and Sauber before Audi, AlphaTauri before RB.
 * The first id that exists wins; the rest are fallbacks.
 */
final class teamCarCatalog {

    static final int FIRST_PHOTO_YEAR = 2021;

    private teamCarCatalog() {
    }

    static String normalize(String id) {
        return id == null ? "" : id.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }

    static int firstYear(String teamId) {
        // a team that joined later has no cars before its first season
        return "cadillac".equals(normalize(teamId)) ? 2026 : FIRST_PHOTO_YEAR;
    }

    /** Storage ids to try for this team and season, best guess first. */
    static List<String> candidates(String teamId, int year) {
        List<String> result = new ArrayList<>();
        String key = normalize(teamId);

        switch (key) {
            case "audi":
                if (year <= 2023) add(result, "alfa");
                else if (year <= 2025) add(result, "sauber");
                else add(result, "audi");
                break;
            case "rb":
                if (year <= 2023) add(result, "alphatauri");
                else add(result, "rb");
                break;
            case "redbull":
                add(result, "red_bull");
                add(result, "redbull");
                break;
            case "astonmartin":
                add(result, "aston_martin");
                add(result, "astonmartin");
                break;
            default:
                break;
        }

        // the id the app already uses for this team is always the last resort
        add(result, teamId == null ? "" : teamId.toLowerCase(Locale.ROOT));

        return result;
    }

    /** A name for the card caption when the team raced under another name that season, else null. */
    static String formerName(String storageId) {
        switch (storageId) {
            case "alfa":
                return "Alfa Romeo";
            case "sauber":
                return "Sauber";
            case "alphatauri":
                return "AlphaTauri";
            default:
                return null;
        }
    }

    private static void add(List<String> list, String id) {
        if (!id.isEmpty() && !list.contains(id)) {
            list.add(id);
        }
    }
}
