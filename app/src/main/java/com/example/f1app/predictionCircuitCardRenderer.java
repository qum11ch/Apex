package com.example.f1app;

import static com.example.f1app.MainActivity.getStringByName;
import static com.example.f1app.driverStatsFragment.getCountryCode;
import static com.example.f1app.teamsStandingsActivity.getLocalizedCity;

import android.content.Context;
import android.content.res.Resources;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.core.os.ConfigurationCompat;

import java.util.Locale;

public final class predictionCircuitCardRenderer {
    private predictionCircuitCardRenderer() {
    }

    public static void render(ViewGroup container, predictionCircuitCardLoader.Data data) {
        Context context = container.getContext();

        container.removeAllViews();

        View view = LayoutInflater.from(context).inflate(
                R.layout.view_prediction_circuit_stats,
                container,
                false
        );

        container.addView(view);

        String race = predictionRaceNameFormatter.getNameWithSeason(
                context,
                data.raceName,
                data.season
        );

        if (data.raceDate != null) {
            race += " · " + data.raceDate;
        }

        set(view, R.id.circuitRace, race);
        set(view, R.id.circuitName, circuitName(context, data.circuitId, data.circuitName));
        set(view, R.id.circuitPlace, place(context, data.location, data.country));

        set(view, R.id.circuitLaps, data.laps);
        set(view, R.id.circuitTurns, data.turns);
        set(view, R.id.circuitLength, unit(context, R.string.pc_km, data.length));
        set(view, R.id.circuitDistance, unit(context, R.string.pc_km, data.raceDistance));
        set(view, R.id.circuitOpened, data.opened);
        set(view, R.id.circuitAltitude, unit(context, R.string.pc_m, data.altitude));

        set(view, R.id.circuitRecordTime, data.recordTime);

        String who = data.recordDriver == null ? "—" : data.recordDriver;

        if (data.recordYear != null) {
            who += " · " + data.recordYear;
        }

        set(view, R.id.circuitRecordWho, who);
    }

    private static String circuitName(Context context, String circuitId, String fallback) {
        String id = clean(circuitId);

        if (id != null) {
            int resourceId = getStringByName(id.toLowerCase(Locale.ROOT) + "_text");

            if (resourceId != 0) {
                try {
                    return context.getString(resourceId);
                } catch (Resources.NotFoundException ignored) {
                    // Show the original name if the resource cannot be read.
                }
            }
        }

        return clean(fallback);
    }

    private static String place(Context context, String location, String country) {
        String originalCity = clean(location);
        String originalCountry = clean(country);

        Locale locale = ConfigurationCompat.getLocales(
                context.getResources().getConfiguration()
        ).get(0);

        if (locale == null) {
            locale = Locale.getDefault();
        }

        String city = originalCity;
        String countryName = originalCountry;

        if ("ru".equals(locale.getLanguage())) {
            if (originalCity != null) {
                city = getLocalizedCity(originalCity);
            }

            if (originalCountry != null) {
                String code = countryCode(originalCountry);

                if (code != null) {
                    String translated = clean(
                            new Locale("", code).getDisplayCountry(locale)
                    );

                    if (translated != null && !translated.equalsIgnoreCase(code)) {
                        countryName = translated;
                    }
                }
            }
        }

        if (originalCity != null
                && originalCountry != null
                && originalCity.equalsIgnoreCase(originalCountry)) {
            return countryName;
        }

        return join(city, countryName);
    }

    private static String countryCode(String country) {
        String code = clean(getCountryCode(country));

        if (code == null) {
            return null;
        }

        for (String isoCode : Locale.getISOCountries()) {
            if (isoCode.equalsIgnoreCase(code)) {
                return isoCode;
            }
        }

        return null;
    }

    private static String join(String city, String country) {
        String first = clean(city);
        String second = clean(country);

        if (first == null) {
            return second;
        }

        if (second == null || first.equalsIgnoreCase(second)) {
            return first;
        }

        return first + ", " + second;
    }

    private static String clean(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();

        return trimmed.isEmpty() ? null : trimmed;
    }

    private static String unit(Context context, int resource, String value) {
        return value == null ? null : context.getString(resource, value);
    }

    private static void set(View root, int id, String value) {
        ((TextView) root.findViewById(id)).setText(value == null ? "—" : value);
    }
}