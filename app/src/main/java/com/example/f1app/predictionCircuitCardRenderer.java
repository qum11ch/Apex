package com.example.f1app;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public final class predictionCircuitCardRenderer {
    private predictionCircuitCardRenderer() {}

    public static void render(ViewGroup container, predictionCircuitCardLoader.Data d) {
        Context context = container.getContext();
        container.removeAllViews();
        View view = LayoutInflater.from(context).inflate(
                R.layout.view_prediction_circuit_stats, container, false);
        container.addView(view);

        String race = predictionRaceNameFormatter.getNameWithSeason(
                context, d.raceName, d.season);
        if (d.raceDate != null) race += " · " + d.raceDate;
        set(view, R.id.circuitRace, race);
        set(view, R.id.circuitName, d.circuitName);
        set(view, R.id.circuitPlace, join(d.location, d.country));

        set(view, R.id.circuitLaps, d.laps);
        set(view, R.id.circuitTurns, d.turns);
        set(view, R.id.circuitLength, unit(context, R.string.pc_km, d.length));
        set(view, R.id.circuitDistance, unit(context, R.string.pc_km, d.raceDistance));
        set(view, R.id.circuitOpened, d.opened);
        set(view, R.id.circuitAltitude, unit(context, R.string.pc_m, d.altitude));

        set(view, R.id.circuitRecordTime, d.recordTime);
        String who = d.recordDriver == null ? "—" : d.recordDriver;
        if (d.recordYear != null) who += " · " + d.recordYear;
        set(view, R.id.circuitRecordWho, who);
    }

    private static String join(String a, String b) {
        if (a == null && b == null) return null;
        if (a == null) return b;
        if (b == null) return a;
        return a + ", " + b;
    }

    private static String unit(Context context, int resource, String value) {
        return value == null ? null : context.getString(resource, value);
    }

    private static void set(View root, int id, String value) {
        ((TextView) root.findViewById(id)).setText(value == null ? "—" : value);
    }
}
