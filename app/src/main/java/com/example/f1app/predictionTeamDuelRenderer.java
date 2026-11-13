package com.example.f1app;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.List;
import java.util.Locale;

public final class predictionTeamDuelRenderer {

    private predictionTeamDuelRenderer() {
    }

    public static void render(
            ViewGroup container,
            String teamATitle,
            String teamBTitle,
            List<teamRaceStats> teamA,
            List<teamRaceStats> teamB
    ) {
        Context context = container.getContext();

        container.removeAllViews();

        View content = LayoutInflater.from(context).inflate(
                R.layout.view_prediction_duel_stats,
                container,
                false
        );

        container.addView(content);

        long pointsA = totalPoints(teamA);
        long pointsB = totalPoints(teamB);

        setText(content, R.id.duelCodeA, "A");
        setText(content, R.id.duelCodeB, "B");

        setText(content, R.id.duelNameA, teamATitle);
        setText(content, R.id.duelNameB, teamBTitle);

        setText(content, R.id.duelScoreA, String.valueOf(pointsA));
        setText(content, R.id.duelScoreB, String.valueOf(pointsB));

        setText(
                content,
                R.id.duelScoreCaption,
                context.getString(R.string.pred_team_main_race_points)
        );

        setText(
                content,
                R.id.duelRecentTitle,
                context.getString(R.string.pred_team_recent)
        );

        setText(
                content,
                R.id.duelResultsNote,
                context.getString(R.string.pred_team_points_note)
        );

        LinearLayout metrics = content.findViewById(R.id.duelMetrics);

        addRow(
                metrics,
                context.getString(R.string.pred_team_comparison_header),
                "A",
                "B",
                true
        );

        addRow(
                metrics,
                context.getString(R.string.pred_team_average_points),
                average(pointsA, teamA.size()),
                average(pointsB, teamB.size()),
                false
        );

        addRow(
                metrics,
                context.getString(R.string.pred_team_gp_wins),
                String.valueOf(wins(teamA)),
                String.valueOf(wins(teamB)),
                false
        );

        LinearLayout recent = content.findViewById(R.id.duelRecentRows);

        addRow(
                recent,
                context.getString(R.string.pred_duel_race),
                "A",
                "B",
                true
        );

        int shown = 0;

        for (int i = teamA.size() - 1; i >= 0 && shown < 5; i--) {
            teamRaceStats a = teamA.get(i);
            teamRaceStats b = findRace(teamB, a.getRaceName());

            if (b == null) {
                continue;
            }

            addRow(
                    recent,
                    predictionRaceNameFormatter.getName(
                            context,
                            a.getRaceName()
                    ),
                    String.valueOf(a.getPoints()),
                    String.valueOf(b.getPoints()),
                    false
            );

            shown++;
        }
    }

    private static long totalPoints(List<teamRaceStats> races) {
        long total = 0;

        for (teamRaceStats race : races) {
            total += race.getPoints();
        }

        return total;
    }

    private static int wins(List<teamRaceStats> races) {
        int count = 0;

        for (teamRaceStats race : races) {
            if (race.hasWin()) {
                count++;
            }
        }

        return count;
    }

    private static String average(long points, int races) {
        if (races == 0) {
            return "—";
        }

        return String.format(
                Locale.getDefault(),
                "%.1f",
                (double) points / races
        );
    }

    private static teamRaceStats findRace(
            List<teamRaceStats> races,
            String raceName
    ) {
        for (teamRaceStats race : races) {
            if (raceName.equals(race.getRaceName())) {
                return race;
            }
        }

        return null;
    }

    private static void setText(View root, int id, String text) {
        TextView view = root.findViewById(id);
        view.setText(text == null ? "—" : text);
    }

    private static void addRow(
            LinearLayout parent,
            String label,
            String valueA,
            String valueB,
            boolean header
    ) {
        View row = LayoutInflater.from(parent.getContext()).inflate(
                R.layout.item_prediction_comparison_row,
                parent,
                false
        );

        TextView labelView = row.findViewById(R.id.comparisonLabel);
        TextView a = row.findViewById(R.id.comparisonValueA);
        TextView b = row.findViewById(R.id.comparisonValueB);

        labelView.setText(label);
        a.setText(valueA);
        b.setText(valueB);

        if (header) {
            androidx.core.widget.TextViewCompat.setTextAppearance(labelView, R.style.Widget_Apex_Pred_Caption);
            androidx.core.widget.TextViewCompat.setTextAppearance(a, R.style.Widget_Apex_Pred_Caption);
            androidx.core.widget.TextViewCompat.setTextAppearance(b, R.style.Widget_Apex_Pred_Caption);
        }

        parent.addView(row);
    }
}