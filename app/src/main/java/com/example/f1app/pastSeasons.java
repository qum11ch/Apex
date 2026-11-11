package com.example.f1app;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Which past seasons the "past season" screens offer, and which one opens first. */
final class pastSeasons {

    /**
     * First season with standings. They come from Jolpica, which has them for every year,
     * but driver and team pages need entries in the database (see the README).
     */
    static final int FIRST_STANDINGS_SEASON = 2021;

    /**
     * First season of the race schedule and results. These come from the database
     * (schedule/season, results/season), which currently starts in 2024.
     * Lower this number when older seasons are imported.
     */
    static final int FIRST_SCHEDULE_SEASON = 2024;

    private pastSeasons() {
    }

    static int parse(String value, int fallback) {
        try {
            return Integer.parseInt(value.trim());
        } catch (RuntimeException error) {
            return fallback;
        }
    }

    /** The newest past season: the one before the season the app is showing now. */
    static int lastPastSeason(String currentSeason) {
        return parse(currentSeason, LocalDate.now().getYear()) - 1;
    }

    /** Newest first, like the season buttons on the driver page. Empty when last < first. */
    static List<Integer> seasons(int first, int last) {
        List<Integer> result = new ArrayList<>();

        for (int season = last; season >= first; season--) {
            result.add(season);
        }

        return result;
    }

    /** The requested season when it is in range, otherwise the newest past season. */
    static int initial(String requested, int first, int last) {
        int season = parse(requested, last);
        return Math.max(first, Math.min(last, season));
    }
}
