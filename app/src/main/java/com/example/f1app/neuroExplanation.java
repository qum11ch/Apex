package com.example.f1app;

import android.content.Context;

import com.google.firebase.database.DataSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

final class neuroExplanation {
    private neuroExplanation() {}

    static final int MIN_ACCURACY_RACES = 5;

    static final class Accuracy {
        final int races, poleHits;
        final double within1;

        Accuracy(int races, int poleHits, double within1) {
            this.races = races;
            this.poleHits = poleHits;
            this.within1 = within1;
        }

        static Accuracy fromSnapshot(DataSnapshot s) {
            if (s == null || !s.exists()) return null;
            Long races = s.child("races").getValue(Long.class);
            if (races == null) return null;
            Long poles = s.child("pole_hits").getValue(Long.class);
            Double within1 = s.child("within1_share").getValue(Double.class);
            return new Accuracy(races.intValue(), poles == null ? 0 : poles.intValue(),
                    within1 == null ? 0.0 : within1);
        }
    }

    static final class Conditions {
        final double airTemp, humidity, pressure, lengthKm;
        final boolean rain, street;
        final int corners;

        Conditions(double airTemp, double humidity, double pressure, boolean rain,
                   boolean street, double lengthKm, int corners) {
            this.airTemp = airTemp;
            this.humidity = humidity;
            this.pressure = pressure;
            this.rain = rain;
            this.street = street;
            this.lengthKm = lengthKm;
            this.corners = corners;
        }

        /** predictions/<key>/params; null when any field is missing. */
        static Conditions fromSnapshot(DataSnapshot params) {
            if (params == null || !params.exists()) return null;
            Double t = number(params, "airTemp");
            Double h = number(params, "humidity");
            Double p = number(params, "pressure");
            Double r = number(params, "rainfall");
            Double s = number(params, "isStreetCircuit");
            Double l = number(params, "circuitLength");
            Double c = number(params, "circuitCorners");
            if (t == null || h == null || p == null || r == null
                    || s == null || l == null || c == null) return null;
            return new Conditions(t, h, p, r > 0, s > 0, l, (int) Math.round(c));
        }

        private static Double number(DataSnapshot parent, String name) {
            Object v = parent.child(name).getValue();
            if (v instanceof Number) return ((Number) v).doubleValue();
            if (v instanceof Boolean) return ((Boolean) v) ? 1.0 : 0.0;
            return null;
        }
    }

    static final class Text {
        final List<String> summary;
        final List<String> details;

        Text(List<String> summary, List<String> details) {
            this.summary = summary;
            this.details = details;
        }
    }

    static Text build(Context context, List<String> codes, List<String> q1,
                      List<String> q2, List<String> q3, String[] labels,
                      Conditions c, boolean previousYearWeather, boolean inputsMayDiffer,
                      Accuracy accuracy) {
        List<String> summary = new ArrayList<>();
        List<String> details = new ArrayList<>();

        summary.add(context.getString(R.string.neuro_explain_conditions_short,
                format(c.airTemp, 1), format(c.humidity, 0), format(c.pressure, 0),
                context.getString(c.rain ? R.string.neuro_explain_rain_yes
                        : R.string.neuro_explain_rain_no)));

        summary.add(context.getString(R.string.neuro_explain_circuit_short,
                format(c.lengthKm, 3), c.corners,
                context.getString(c.street ? R.string.neuro_explain_street
                        : R.string.neuro_explain_permanent)));

        if (previousYearWeather) {
            summary.add(context.getString(R.string.neuro_explain_weather_last_year));
        } else {
            details.add(context.getString(R.string.neuro_explain_weather_forecast));
        }

        if (inputsMayDiffer) {
            summary.add(context.getString(R.string.neuro_explain_cache_note));
        }

        int first = -1, second = -1;
        for (int i = 0; i < q3.size(); i++) {
            double t = seconds(q3.get(i));
            if (Double.isNaN(t)) continue;
            if (first < 0 || t < seconds(q3.get(first))) {
                second = first;
                first = i;
            } else if (second < 0 || t < seconds(q3.get(second))) {
                second = i;
            }
        }
        if (first >= 0 && second >= 0) {
            double gap = seconds(q3.get(second)) - seconds(q3.get(first));
            summary.add(context.getString(R.string.neuro_explain_pole_short,
                    codes.get(first), q3.get(first), format(gap, 3)));
        }

        summary.add(context.getString(R.string.neuro_explain_phases_short,
                labels[0], count(q1), labels[1], count(q2), labels[2], count(q3)));

        if (accuracy != null) {
            if (accuracy.races < MIN_ACCURACY_RACES) {
                summary.add(context.getString(R.string.neuro_explain_accuracy_collecting,
                        accuracy.races, MIN_ACCURACY_RACES));
            } else {
                summary.add(context.getString(R.string.neuro_explain_accuracy,
                        accuracy.races, accuracy.poleHits, format(accuracy.within1 * 100, 0)));
            }
        }

        details.add(context.getString(R.string.neuro_explain_disclaimer));
        return new Text(summary, details);
    }

    private static int count(List<String> times) {
        int n = 0;
        for (String t : times) {
            if (!Double.isNaN(seconds(t))) n++;
        }
        return n;
    }

    /** "1:25.668" -> 85.668; NaN for empty or "--". */
    static double seconds(String time) {
        if (time == null) return Double.NaN;
        String s = time.trim();
        if (s.isEmpty() || s.equals("--")) return Double.NaN;
        try {
            int colon = s.indexOf(':');
            if (colon < 0) return Double.parseDouble(s);
            return Integer.parseInt(s.substring(0, colon)) * 60
                    + Double.parseDouble(s.substring(colon + 1));
        } catch (NumberFormatException e) {
            return Double.NaN;
        }
    }

    private static String format(double value, int digits) {
        return String.format(Locale.getDefault(), "%." + digits + "f", value);
    }
}