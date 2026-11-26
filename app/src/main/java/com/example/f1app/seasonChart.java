package com.example.f1app;

import static com.example.f1app.MainActivity.getStringByName;

import android.content.Context;
import android.graphics.Color;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/** The points chart card of the season results screens (driver and team pages). */
final class seasonChart {

    private interface ColorCallback {
        void onColor(int color);
    }

    private static final Map<String, String> COLORS = new HashMap<>();
    private static boolean colorsLoaded = false;

    private final Fragment host;
    private final View card;
    private final TextView info;
    private final pointsChartView chart;
    private int token = 0;

    seasonChart(Fragment host, View root) {
        this.host = host;
        card = root.findViewById(R.id.season_chart_card);
        info = root.findViewById(R.id.season_chart_info);
        chart = root.findViewById(R.id.season_chart);
    }

    void load(compareParticipant participant, int year, int fallbackColor) {
        final int mine = ++token;
        card.setVisibility(View.GONE);

        compareSeries.loadOne(participant, year, data -> {
            if (mine != token || !host.isAdded() || data == null || data.a.length == 0) {
                return;
            }

            withColor(data.teamId, fallbackColor, color -> {
                if (mine == token && host.isAdded()) {
                    show(data, color);
                }
            });
        });
    }

    private void show(compareSeries.Data data, int color) {
        Context context = host.requireContext();

        chart.set(data.a, new double[0], color, color,
                ContextCompat.getColor(context, R.color.neuro_divider),
                ContextCompat.getColor(context, R.color.text_color_thirdly),
                index -> info.setText(context.getString(R.string.compare_chart_info_one,
                        index + 1, raceTitle(context, data.names[index]), format(data.a[index]))));

        card.setVisibility(View.VISIBLE);
    }

    private void withColor(String jolpicaTeamId, int fallback, ColorCallback callback) {
        if (jolpicaTeamId == null || jolpicaTeamId.isEmpty()) {
            callback.onColor(fallback);
            return;
        }

        if (colorsLoaded) {
            callback.onColor(resolve(jolpicaTeamId, fallback));
            return;
        }

        FirebaseDatabase.getInstance().getReference().child("constructors")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        for (DataSnapshot team : snapshot.getChildren()) {
                            String color = team.child("color").getValue(String.class);

                            if (team.getKey() != null && color != null) {
                                COLORS.put(teamCarCatalog.normalize(team.getKey()), color);
                            }
                        }

                        colorsLoaded = true;
                        callback.onColor(resolve(jolpicaTeamId, fallback));
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        callback.onColor(resolve(jolpicaTeamId, fallback));
                    }
                });
    }

    private static int resolve(String jolpicaTeamId, int fallback) {
        try {
            return Color.parseColor(pastTeamPalette.resolve(jolpicaTeamId,
                    COLORS.get(teamCarCatalog.normalize(jolpicaTeamId))));
        } catch (RuntimeException error) {
            return fallback;
        }
    }

    static String raceTitle(Context context, String raceName) {
        if (raceName == null || raceName.isEmpty()) {
            return "\u2013";
        }

        int id = getStringByName(raceName.toLowerCase(Locale.ROOT).replaceAll("\\s+", "_") + "_text");
        return id != 0 ? context.getString(id) : raceName;
    }

    private static String format(double value) {
        return value == Math.rint(value)
                ? String.format(Locale.getDefault(), "%d", (long) value)
                : String.format(Locale.getDefault(), "%.1f", value);
    }
}