package com.example.f1app;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.ColorUtils;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.PagerSnapHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** The team of every season of the driver: the car, the team and the constructors' results of that year. */
final class driverTeamSeasonsSection {

    private static final String TAG = "DriverTeamSeasons";
    private static final String DASH = "\u2013";
    private static final int FIRST_F1_SEASON = 1950;

    private final Fragment host;
    private final View root;
    private final String driverCode;
    private final String givenName;
    private final String familyName;
    private final int firstSeason;
    private final int lastSeason;

    private final List<Integer> years = new ArrayList<>();
    /** year -> {team name, team id} as Jolpica names them that season. */
    private final Map<Integer, String[]> teams = new HashMap<>();
    private final Map<Integer, Map<String, String[]>> yearResults = new HashMap<>();
    private final Map<String, String> teamColors = new HashMap<>();
    private final Map<Integer, Uri> uris = new HashMap<>();
    private final Set<Integer> photoChecked = new HashSet<>();
    private final Map<String, String[]> dbTeams = new HashMap<>();
    private final Set<String> activeTeams = new HashSet<>();

    private Integer defaultColor;
    private RequestQueue queue;

    private RecyclerView pager;
    private LinearLayoutManager layoutManager;
    private View prev;
    private View next;
    private TeamAdapter adapter;

    private int pendingYears;
    private final String currentSeason;

    driverTeamSeasonsSection(Fragment host, View root, String driverCode, String givenName,
                             String familyName, int firstSeason, int lastSeason,
                             String currentSeason) {
        this.host = host;
        this.root = root;
        this.driverCode = driverCode;
        this.givenName = givenName;
        this.familyName = familyName;
        this.firstSeason = firstSeason;
        this.lastSeason = lastSeason;
        this.currentSeason = currentSeason;
    }

    void setDefaultColor(Integer color) {
        defaultColor = color;

        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }

    void stop() {
    }

    void start() {
        int first = Math.max(firstSeason, FIRST_F1_SEASON);

        if (lastSeason < first) {
            return;
        }

        if (currentSeason != null) {
            FirebaseDatabase.getInstance().getReference()
                    .child("driverLineUp").child("season").child(currentSeason)
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            if (!host.isAdded()) {
                                return;
                            }

                            for (DataSnapshot team : snapshot.getChildren()) {
                                if (team.getKey() != null) {
                                    activeTeams.add(team.getKey());
                                }
                            }

                            if (adapter != null) {
                                adapter.notifyDataSetChanged();
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            Log.w(TAG, "Current line-up is not available", error.toException());
                        }
                    });
        }

        loadColors();

        for (int year = lastSeason; year >= first; year--) {
            loadDriverStanding(year);
        }
    }

    // ---------- colours of the teams ----------

    private void loadColors() {
        FirebaseDatabase.getInstance().getReference().child("constructors")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (!host.isAdded()) {
                            return;
                        }

                        for (DataSnapshot team : snapshot.getChildren()) {
                            String dbName = team.child("name").getValue(String.class);

                            if (team.getKey() != null) {
                                dbTeams.put(teamCarCatalog.normalize(team.getKey()),
                                        new String[]{team.getKey(), dbName != null ? dbName : team.getKey()});
                            }

                            String color = team.child("color").getValue(String.class);

                            if (team.getKey() != null && color != null) {
                                teamColors.put(teamCarCatalog.normalize(team.getKey()), color);
                            }
                        }

                        if (adapter != null) {
                            adapter.notifyDataSetChanged();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.w(TAG, "Team colours are not available", error.toException());
                    }
                });
    }

    private void openTeam(String[] db, int year) {
        if (!host.isAdded()) {
            return;
        }

        android.content.Intent intent =
                new android.content.Intent(host.requireContext(), teamPageActivity.class);

        intent.putExtra("teamId", db[0]);
        intent.putExtra("teamName", db[1]);
        intent.putExtra("currentSeason", currentSeason);

        host.startActivity(intent);
    }

    private int colorFor(int year, View view) {
        String[] team = teams.get(year);

        if (team != null && !team[1].isEmpty()) {
            String hex = pastTeamPalette.resolve(
                    team[1], teamColors.get(teamCarCatalog.normalize(team[1])));

            try {
                return Color.parseColor(hex);
            } catch (IllegalArgumentException ignored) {
                // falls through to the default colour
            }
        }

        return defaultColor != null
                ? defaultColor
                : ContextCompat.getColor(view.getContext(), R.color.grey);
    }

    // ---------- which team the driver raced for each year ----------

    private void loadDriverStanding(int year) {
        jolpicaSeasons.driverStandings(year, response -> {
            if (!host.isAdded() || response == null) {
                return;
            }

            try {
                readDriverStanding(year, response);
            } catch (JSONException error) {
                Log.w(TAG, "Unexpected standings response for " + year, error);
            }

            if (teams.containsKey(year)) {
                addYear(year);
            }
        });
    }

    /** A season is added the moment its team is known: newer ones first, older ones to the right. */
    private void addYear(int year) {
        if (years.contains(year)) {
            return;
        }

        int index = 0;

        while (index < years.size() && years.get(index) > year) {
            index++;
        }

        years.add(index, year);

        if (adapter == null) {
            show();
        } else {
            adapter.notifyItemInserted(index);
            updateArrows();
        }

        if (year >= 1958) {
            loadConstructorYear(year);
        }

        if (year >= teamCarCatalog.FIRST_PHOTO_YEAR) {
            probePhoto(year, 0, new ArrayList<>());
        } else {
            photoChecked.add(year);
        }
    }

    private void readDriverStanding(int year, JSONObject response) throws JSONException {
        JSONArray lists = response
                .getJSONObject("MRData")
                .getJSONObject("StandingsTable")
                .optJSONArray("StandingsLists");

        if (lists == null) {
            return;
        }

        for (int i = 0; i < lists.length(); i++) {
            JSONArray entries = lists.getJSONObject(i).optJSONArray("DriverStandings");

            if (entries == null) {
                continue;
            }

            for (int j = 0; j < entries.length(); j++) {
                JSONObject entry = entries.getJSONObject(j);
                JSONObject driver = entry.optJSONObject("Driver");

                if (driver == null || !isThisDriver(driver)) {
                    continue;
                }

                JSONArray constructors = entry.optJSONArray("Constructors");

                if (constructors == null || constructors.length() == 0) {
                    return;
                }

                JSONObject last = constructors.getJSONObject(constructors.length() - 1);

                teams.put(year, new String[]{
                        last.optString("name", DASH),
                        last.optString("constructorId", "")
                });

                return;
            }
        }
    }

    private boolean isThisDriver(JSONObject driver) {
        return jolpicaSeasons.isDriver(driver, driverCode, givenName, familyName);
    }

    // ---------- the results of the team (constructors' standings of one season) ----------

    private void loadConstructorYear(int year) {
        jolpicaSeasons.constructorStandings(year, response -> {
            if (!host.isAdded()) {
                return;
            }

            if (response == null) {
                Log.w(TAG, "Constructor standings are not available for " + year);
                return;
            }

            try {
                readConstructorYear(year, response);
            } catch (JSONException error) {
                Log.w(TAG, "Unexpected constructor standings for " + year, error);
            }

            refresh(year);
        });
    }

    private void readConstructorYear(int year, JSONObject response) throws JSONException {
        JSONArray lists = response
                .getJSONObject("MRData")
                .getJSONObject("StandingsTable")
                .optJSONArray("StandingsLists");

        if (lists == null) {
            return;
        }

        Map<String, String[]> byTeam = new HashMap<>();

        for (int i = 0; i < lists.length(); i++) {
            JSONArray entries = lists.getJSONObject(i).optJSONArray("ConstructorStandings");

            if (entries == null) {
                continue;
            }

            for (int j = 0; j < entries.length(); j++) {
                JSONObject entry = entries.getJSONObject(j);
                JSONObject constructor = entry.optJSONObject("Constructor");

                if (constructor == null) {
                    continue;
                }

                byTeam.put(
                        teamCarCatalog.normalize(constructor.optString("constructorId")),
                        new String[]{
                                entry.optString("positionText", DASH),
                                entry.optString("points", DASH),
                                entry.optString("wins", DASH)
                        });
            }
        }

        yearResults.put(year, byTeam);
    }

    // ---------- photos of the cars (2021 and later) ----------

    private void probePhoto(int year, int index, List<String> tried) {
        String[] team = teams.get(year);
        List<String> ids = team == null || team[1].isEmpty()
                ? new ArrayList<>()
                : teamCarCatalog.candidates(team[1], year);

        if (index >= ids.size()) {
            photoChecked.add(year);
            Log.w(TAG, "NO PHOTO " + (team == null ? "" : team[1]) + " " + year + ", tried " + tried);
            refresh(year);
            return;
        }

        String path = "teams/" + ids.get(index) + "_" + year + ".png";
        tried.add(path);

        FirebaseStorage.getInstance().getReference().child(path).getDownloadUrl()
                .addOnSuccessListener(uri -> {
                    if (!host.isAdded()) {
                        return;
                    }

                    uris.put(year, uri);
                    photoChecked.add(year);
                    refresh(year);
                })
                .addOnFailureListener(error -> {
                    if (!host.isAdded()) {
                        return;
                    }

                    probePhoto(year, index + 1, tried);
                });
    }

    private void refresh(int year) {
        if (adapter == null) {
            return;
        }

        int position = years.indexOf(year);

        if (position >= 0) {
            adapter.notifyItemChanged(position);
        }
    }

    // ---------- pager and arrows ----------

    private void show() {
        root.findViewById(R.id.team_seasons_header).setVisibility(View.VISIBLE);
        root.findViewById(R.id.team_season_pager_host).setVisibility(View.VISIBLE);

        pager = root.findViewById(R.id.team_season_pager);
        prev = root.findViewById(R.id.team_season_prev);
        next = root.findViewById(R.id.team_season_next);

        layoutManager = new LinearLayoutManager(
                host.requireContext(), LinearLayoutManager.HORIZONTAL, false);

        pager.setLayoutManager(layoutManager);
        new PagerSnapHelper().attachToRecyclerView(pager);

        adapter = new TeamAdapter();
        pager.setAdapter(adapter);

        pager.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                updateArrows();
            }

            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                updateArrows();
            }
        });

        // left = the newer season, right = the earlier one
        prev.setOnClickListener(v -> scrollBy(-1));
        next.setOnClickListener(v -> scrollBy(1));

        updateArrows();
    }

    private int currentPosition() {
        int position = layoutManager.findFirstCompletelyVisibleItemPosition();

        if (position == RecyclerView.NO_POSITION) {
            position = layoutManager.findFirstVisibleItemPosition();
        }

        return Math.max(position, 0);
    }

    private void scrollBy(int step) {
        int target = currentPosition() + step;

        if (target >= 0 && target < years.size()) {
            pager.smoothScrollToPosition(target);
        }
    }

    private void updateArrows() {
        int position = currentPosition();

        setArrow(prev, position > 0);
        setArrow(next, position < years.size() - 1);
    }

    private void setArrow(View arrow, boolean enabled) {
        arrow.setEnabled(enabled);
        arrow.setAlpha(enabled ? 1f : 0.3f);
    }

    // ---------- pages ----------

    private final class TeamAdapter extends RecyclerView.Adapter<TeamAdapter.Holder> {

        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            View view = inflater.inflate(R.layout.item_team_car_page, parent, false);

            ViewGroup.LayoutParams page = view.getLayoutParams();
            if (page == null) {
                page = new RecyclerView.LayoutParams(
                        parent.getMeasuredWidth(), ViewGroup.LayoutParams.WRAP_CONTENT);
            } else {
                page.width = parent.getMeasuredWidth();
                page.height = ViewGroup.LayoutParams.WRAP_CONTENT;
            }
            view.setLayoutParams(page);

            Holder holder = new Holder(view);

            int[] labels = {R.string.team_cars_position, R.string.team_results_points, R.string.wins};

            for (int i = 0; i < labels.length; i++) {
                View tile = inflater.inflate(
                        R.layout.item_team_results_summary, holder.results, false);

                ((TextView) tile.findViewById(R.id.summary_label)).setText(labels[i]);
                holder.values[i] = tile.findViewById(R.id.summary_value);

                holder.results.addView(tile, new LinearLayout.LayoutParams(
                        0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            }

            return holder;
        }

        @Override
        public void onBindViewHolder(@NonNull Holder holder, int position) {
            int year = years.get(position);
            String[] team = teams.get(year);

            holder.year.setText(String.valueOf(year));
            holder.caption.setText(team != null ? team[0] : "");

            paint(holder, colorFor(year, holder.itemView));

            String[] db = team == null ? null : dbTeams.get(teamCarCatalog.normalize(team[1]));
            boolean clickable = db != null && activeTeams.contains(db[0]);

            holder.itemView.setOnClickListener(clickable ? v -> openTeam(db, year) : null);
            holder.itemView.setClickable(clickable);

            Uri uri = uris.get(year);

            holder.missing.setVisibility(View.GONE);

            if (uri != null) {
                GlideApp.with(holder.photo)
                        .load(uri)
                        .transition(DrawableTransitionOptions.withCrossFade())
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .into(holder.photo);
            } else {
                GlideApp.with(holder.photo).clear(holder.photo);

                if (photoChecked.contains(year)) {
                    holder.photo.setImageResource(R.drawable.placeholder_car);
                } else {
                    holder.photo.setImageDrawable(null);
                }
            }

            Map<String, String[]> byTeam = yearResults.get(year);
            String[] values = (team == null || byTeam == null)
                    ? null
                    : byTeam.get(teamCarCatalog.normalize(team[1]));

            if (team != null && byTeam != null && values == null) {
                Log.w(TAG, "TEAM NOT IN STANDINGS " + year + " " + team[0] + " (" + team[1] + ")");
            }

            for (int i = 0; i < holder.values.length; i++) {
                holder.values[i].setText(values != null ? values[i] : DASH);
            }
        }

        @Override
        public int getItemCount() {
            return years.size();
        }

        private void paint(Holder holder, int color) {
            float density = holder.itemView.getResources().getDisplayMetrics().density;

            GradientDrawable background = new GradientDrawable();
            background.setColor(ContextCompat.getColor(
                    holder.itemView.getContext(), R.color.colorPrimary));
            background.setCornerRadius(16 * density);
            background.setStroke(Math.round(2 * density), color);

            holder.card.setBackground(background);

            int soft = ColorUtils.compositeColors(
                    ColorUtils.setAlphaComponent(color, 31), Color.WHITE);

            ViewCompat.setBackgroundTintList(holder.photoBlock, ColorStateList.valueOf(soft));
        }

        final class Holder extends RecyclerView.ViewHolder {
            final View card;
            final View photoBlock;
            final ImageView photo;
            final View missing;
            final TextView year;
            final TextView caption;
            final LinearLayout results;
            final TextView[] values = new TextView[3];

            Holder(@NonNull View itemView) {
                super(itemView);
                card = itemView.findViewById(R.id.car_card);
                photoBlock = itemView.findViewById(R.id.car_photo_block);
                photo = itemView.findViewById(R.id.car_photo);
                missing = itemView.findViewById(R.id.car_missing);
                year = itemView.findViewById(R.id.car_year);
                caption = itemView.findViewById(R.id.car_caption);
                results = itemView.findViewById(R.id.car_results);
            }
        }
    }
}