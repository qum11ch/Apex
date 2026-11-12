package com.example.f1app;

import static com.example.f1app.MainActivity.getStringByName;
import static com.example.f1app.teamsStandingsActivity.localizeLocality;

import android.content.Context;

import java.text.Normalizer;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Locale;

/** One race of a past season with everything the list and tile layouts need, resolved once. */
final class pastRaceItem {

    final concludedRacesData race;
    final String circuitId;
    final String title;
    final String circuitTitle;
    final String place;
    final String dayStart;
    final String dayEnd;
    final String monthStart;
    final String monthEnd;
    final String searchText;
    final int round;

    private pastRaceItem(concludedRacesData race, String circuitId, String title, String circuitTitle,
                         String place, String dayStart, String dayEnd, String monthStart,
                         String monthEnd, String searchText, int round) {
        this.race = race;
        this.circuitId = circuitId;
        this.title = title;
        this.circuitTitle = circuitTitle;
        this.place = place;
        this.dayStart = dayStart;
        this.dayEnd = dayEnd;
        this.monthStart = monthStart;
        this.monthEnd = monthEnd;
        this.searchText = searchText;
        this.round = round;
    }

    static pastRaceItem create(Context context, concludedRacesData race, String circuitId) {
        String grandPrix = localizedGrandPrix(context, race.getRaceName());
        String title = grandPrix + " " + race.getSeason();
        String circuitTitle = circuitId == null ? "" : localized(context, circuitId + "_text", race.getCircuitName());
        String place = place(context, race);

        String dayStart = "";
        String dayEnd = "";
        String monthStart = "";
        String monthEnd = "";

        try {
            DateTimeFormatter parser = DateTimeFormatter.ofPattern("yyyy-M-d");
            LocalDate start = LocalDate.parse(race.getDateStart(), parser);
            LocalDate end = LocalDate.parse(race.getDateEnd(), parser);
            dayStart = start.format(DateTimeFormatter.ofPattern("dd"));
            dayEnd = end.format(DateTimeFormatter.ofPattern("dd"));
            monthStart = start.format(DateTimeFormatter.ofPattern("MMM"));
            monthEnd = end.format(DateTimeFormatter.ofPattern("MMM"));
        } catch (RuntimeException ignored) {
            // a race without a usable date still belongs in the list
        }

        int round = 0;

        try {
            round = Integer.parseInt(race.getRoundNumber());
        } catch (RuntimeException ignored) {
            // keeps 0
        }

        String search = plain(String.join(" ",
                grandPrix, nz(race.getRaceName()), circuitTitle, nz(race.getCircuitName()), place,
                nz(race.getCountry()), nz(race.getLocality()), nz(race.getWinnerDriverCode()),
                nz(race.getSecondPlaceCode()), nz(race.getThirdPlaceCode()), nz(race.getRoundNumber())));

        return new pastRaceItem(race, circuitId, title, circuitTitle, place, dayStart, dayEnd,
                monthStart, monthEnd, search, round);
    }

    /** "Brazilian Grand Prix" to the translated name from strings.xml; the English name when it is missing. */
    static String localizedGrandPrix(Context context, String raceName) {
        String name = nz(raceName);
        return localized(context, name.toLowerCase().replaceAll("\\s+", "_") + "_text", name);
    }

    private static String localized(Context context, String key, String fallback) {
        try {
            int id = getStringByName(key);
            return id > 0 ? context.getString(id) : nz(fallback);
        } catch (Exception error) {
            return nz(fallback);
        }
    }

    private static String place(Context context, concludedRacesData race) {
        String locality = nz(race.getLocality());
        String country = nz(race.getCountry());

        try {
            if (Locale.getDefault().getLanguage().equals("ru")) {
                ArrayList<String> localized = localizeLocality(locality, country, context);
                return localized.get(2);
            }
        } catch (RuntimeException ignored) {
            // falls back to the English form below
        }

        if (locality.equals("Monaco") || locality.equals("Singapore") || locality.isEmpty()) {
            return country;
        }

        return locality + ", " + country;
    }

    static String nz(String value) {
        return value == null ? "" : value;
    }

    /** Lower case without accents, so "sao paulo" finds "São Paulo". */
    static String plain(String text) {
        String decomposed = Normalizer.normalize(nz(text), Normalizer.Form.NFD);
        return decomposed.replaceAll("\\p{M}+", "").toLowerCase(Locale.ROOT).trim();
    }
}
