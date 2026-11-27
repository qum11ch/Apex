package com.example.f1app;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.android.volley.Request;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * "Season is over": when every race of the season is concluded or canceled, the card with the top 3
 * of the drivers and of the teams replaces the "upcoming race" block.
 */
final class seasonFinishedBlock {

    interface Actions {
        void openDrivers();

        void openTeams();
    }

    private static final String BASE = "https://api.jolpi.ca/ergast/f1/";
    private static final int NEUTRAL = 0xFF8E8E93;
    private static final Map<String, Integer> BRIGHT = new HashMap<>();

    private static final class Row {
        String position = "";
        String name = "";
        String points = "";
        String teamId = "";
    }

    private final Context context;
    private final View card, futureHeader, futureLayout, predictButton;
    private final TextView caption;
    private final LinearLayout drivers, teams;
    private final View driversLabel, teamsLabel;
    private final List<Row> driverRows = new ArrayList<>();
    private final List<Row> teamRows = new ArrayList<>();
    private int token = 0;

    private final View[] hidden;

    seasonFinishedBlock(View card, View futureHeader, View futureLayout, View predictButton,
                        Actions actions, View... hidden) {
        this.context = card.getContext();
        this.card = card;
        this.futureHeader = futureHeader;
        this.futureLayout = futureLayout;
        this.predictButton = predictButton;
        this.hidden = hidden;

        caption = card.findViewById(R.id.finishedCaption);
        drivers = card.findViewById(R.id.finishedDrivers);
        teams = card.findViewById(R.id.finishedTeams);
        driversLabel = card.findViewById(R.id.finishedDriversLabel);
        teamsLabel = card.findViewById(R.id.finishedTeamsLabel);

        driversLabel.setOnClickListener(v -> actions.openDrivers());
        drivers.setOnClickListener(v -> actions.openDrivers());
        teamsLabel.setOnClickListener(v -> actions.openTeams());
        teams.setOnClickListener(v -> actions.openTeams());
    }

    void refresh(String season) {
        final int mine = ++token;

        FirebaseDatabase.getInstance().getReference().child("schedule").child("season").child(season)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (mine != token) {
                            return;
                        }

                        long now = System.currentTimeMillis();
                        int races = 0;
                        boolean finished = true;

                        for (DataSnapshot race : snapshot.getChildren()) {
                            races++;
                            raceWeekendStatus.State state = raceWeekendStatus.resolve(race, now);

                            if (state != raceWeekendStatus.State.CONCLUDED
                                    && state != raceWeekendStatus.State.CANCELED) {
                                finished = false;
                                break;
                            }
                        }

                        if (races > 0 && finished) {
                            showFinished(season);
                        } else {
                            showRegular();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        if (mine == token) {
                            showRegular();
                        }
                    }
                });
    }

    private void showRegular() {
        card.setVisibility(View.GONE);
        futureHeader.setVisibility(View.VISIBLE);

        for (View view : hidden) {
            view.setVisibility(View.VISIBLE);
        }
    }

    private void showFinished(String season) {
        futureHeader.setVisibility(View.GONE);
        futureLayout.setVisibility(View.GONE);
        predictButton.setVisibility(View.GONE);

        for (View view : hidden) {
            view.setVisibility(View.GONE);
        }

        caption.setText(context.getString(R.string.finished_caption, season));
        card.setVisibility(View.VISIBLE);

        driverRows.clear();
        teamRows.clear();
        render();

        loadDrivers(season);
        loadTeams(season);
        loadColors();
    }

    // ---------- data ----------

    private void loadDrivers(String season) {
        String url = BASE + season + "/driverstandings/?format=json";

        apiQueue.get().add(new cachedJsonRequest(Request.Method.GET, url, null,
                response -> {
                    JSONArray list = standings(response, "DriverStandings");

                    driverRows.clear();

                    for (int i = 0; list != null && i < Math.min(3, list.length()); i++) {
                        JSONObject o = list.optJSONObject(i);

                        if (o == null) {
                            continue;
                        }

                        JSONObject driver = o.optJSONObject("Driver");
                        JSONArray constructors = o.optJSONArray("Constructors");
                        JSONObject team = constructors == null || constructors.length() == 0
                                ? null : constructors.optJSONObject(constructors.length() - 1);

                        Row row = new Row();
                        row.position = o.optString("position", String.valueOf(i + 1));
                        row.name = driver == null ? "" : driver.optString("familyName");
                        row.points = points(o.optString("points"));
                        row.teamId = team == null ? "" : team.optString("constructorId");
                        driverRows.add(row);
                    }

                    render();
                },
                error -> {
                }));
    }

    private void loadTeams(String season) {
        String url = BASE + season + "/constructorstandings/?format=json";

        apiQueue.get().add(new cachedJsonRequest(Request.Method.GET, url, null,
                response -> {
                    JSONArray list = standings(response, "ConstructorStandings");

                    teamRows.clear();

                    for (int i = 0; list != null && i < Math.min(3, list.length()); i++) {
                        JSONObject o = list.optJSONObject(i);

                        if (o == null) {
                            continue;
                        }

                        JSONObject team = o.optJSONObject("Constructor");

                        Row row = new Row();
                        row.position = o.optString("position", String.valueOf(i + 1));
                        row.name = team == null ? "" : team.optString("name");
                        row.points = points(o.optString("points"));
                        row.teamId = team == null ? "" : team.optString("constructorId");
                        teamRows.add(row);
                    }

                    render();
                },
                error -> {
                }));
    }

    private void loadColors() {
        FirebaseDatabase.getInstance().getReference().child("constructors")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        for (DataSnapshot team : snapshot.getChildren()) {
                            if (team.getKey() != null) {
                                BRIGHT.put(team.getKey(),
                                        colorOf(team.getKey(), team.child("color").getValue(String.class)));
                            }
                        }

                        render();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        // the colours of the palette stay
                    }
                });
    }

    private static JSONArray standings(JSONObject response, String key) {
        JSONObject data = response.optJSONObject("MRData");
        JSONObject table = data == null ? null : data.optJSONObject("StandingsTable");
        JSONArray lists = table == null ? null : table.optJSONArray("StandingsLists");
        JSONObject first = lists == null || lists.length() == 0 ? null : lists.optJSONObject(0);

        return first == null ? null : first.optJSONArray(key);
    }

    private static String points(String value) {
        return value.endsWith(".0") ? value.substring(0, value.length() - 2) : value;
    }

    private static int colorOf(String teamId, String hex) {
        try {
            return Color.parseColor(pastTeamPalette.resolve(teamId, hex));
        } catch (RuntimeException error) {
            return NEUTRAL;
        }
    }

    // ---------- the card ----------

    private void render() {
        fill(drivers, driverRows);
        fill(teams, teamRows);

        driversLabel.setVisibility(driverRows.isEmpty() ? View.GONE : View.VISIBLE);
        teamsLabel.setVisibility(teamRows.isEmpty() ? View.GONE : View.VISIBLE);
    }

    private void fill(LinearLayout parent, List<Row> rows) {
        parent.removeAllViews();

        for (Row row : rows) {
            Integer known = BRIGHT.get(row.teamId);
            int color = known != null ? known : colorOf(row.teamId, null);

            LinearLayout line = new LinearLayout(context);
            line.setOrientation(LinearLayout.HORIZONTAL);
            line.setGravity(Gravity.CENTER_VERTICAL);
            line.setPadding(0, dp(8), 0, dp(8));

            TextView position = text(row.position, 15, R.color.hero_text_secondary, true);
            position.setGravity(Gravity.CENTER);
            line.addView(position, new LinearLayout.LayoutParams(dp(28), ViewGroup.LayoutParams.WRAP_CONTENT));

            View bar = new View(context);
            android.graphics.drawable.GradientDrawable shape = new android.graphics.drawable.GradientDrawable();
            shape.setColor(color);
            shape.setCornerRadius(dp(2));
            bar.setBackground(shape);
            LinearLayout.LayoutParams barParams = new LinearLayout.LayoutParams(dp(4), dp(28));
            barParams.setMarginStart(dp(4));
            barParams.setMarginEnd(dp(12));
            line.addView(bar, barParams);

            TextView name = text(row.name, 16, R.color.hero_text, true);
            line.addView(name, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

            line.addView(text(row.points + " " + context.getString(R.string.pts_header),
                    15, R.color.hero_text, true));

            parent.addView(line, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        }
    }

    private TextView text(String value, int size, int colorRes, boolean bold) {
        TextView view = new TextView(context);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(ContextCompat.getColor(context, colorRes));
        view.setMaxLines(1);

        if (bold) {
            view.setTypeface(view.getTypeface(), Typeface.BOLD);
        }

        return view;
    }

    private int dp(float value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }
}