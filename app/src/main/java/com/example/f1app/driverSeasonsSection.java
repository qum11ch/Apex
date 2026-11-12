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
import com.android.volley.toolbox.Volley;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.FirebaseDatabase;
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

final class driverSeasonsSection {

    private static final String TAG = "DriverSeasons";
    private static final String DASH = "\u2013";
    private static final int FIRST_PHOTO_YEAR = 2021;

    private final Fragment host;
    private final View root;
    private final String driverFullName;
    private final String driverCode;
    private final String driverId;
    private final String familyName;
    private final int latest;

    private final List<Integer> years = new ArrayList<>();
    private final Map<Integer, Uri> uris = new HashMap<>();
    private final Set<Integer> photoChecked = new HashSet<>();
    /** {place, points, wins, team name, team id}; only the seasons the driver took part in. */
    private final Map<Integer, String[]> standings = new HashMap<>();
    private final Map<String, String> teamColors = new HashMap<>();

    private Integer defaultColor;
    private RequestQueue queue;

    private RecyclerView pager;
    private LinearLayoutManager layoutManager;
    private View prev;
    private View next;
    private SeasonAdapter adapter;

    private int pendingYears;

    driverSeasonsSection(Fragment host, View root, String driverFullName, String driverCode,
                         String driverId, String familyName, int latestSeason) {

        this.host = host;
        this.root = root;
        this.driverFullName = driverFullName;
        this.driverCode = driverCode;
        this.driverId = driverId;
        this.familyName = familyName;
        this.latest = latestSeason;
    }

    void setDefaultColor(Integer color) {
        defaultColor = color;

        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }

    void stop() {
        if (queue != null) {
            queue.cancelAll(this);
        }
    }

    void start() {
        if (latest < FIRST_PHOTO_YEAR) {
            return;
        }

        pendingYears = latest - FIRST_PHOTO_YEAR + 1;

        for (int year = latest; year >= FIRST_PHOTO_YEAR; year--) {
            loadStanding(year);
        }

        FirebaseDatabase.getInstance().getReference().child("constructors").get()
                .addOnCompleteListener(task -> {
                    if (!host.isAdded() || !task.isSuccessful()) {
                        return;
                    }

                    for (DataSnapshot team : task.getResult().getChildren()) {
                        String color = team.child("color").getValue(String.class);

                        if (team.getKey() != null && color != null) {
                            teamColors.put(teamCarCatalog.normalize(team.getKey()), color);
                        }
                    }

                    if (adapter != null) {
                        adapter.notifyDataSetChanged();
                    }
                });
    }

    /** Called once for every requested year, whatever the outcome. */
    private void yearDone() {
        pendingYears--;

        if (pendingYears > 0 || !host.isAdded()) {
            return;
        }

        years.addAll(standings.keySet());
        Collections.sort(years, Collections.reverseOrder());

        show();
    }

    private void show() {
        if (years.isEmpty()) {
            return;
        }

        root.findViewById(R.id.seasons_header).setVisibility(View.VISIBLE);
        root.findViewById(R.id.season_pager_host).setVisibility(View.VISIBLE);

        pager = root.findViewById(R.id.season_pager);
        prev = root.findViewById(R.id.season_prev);
        next = root.findViewById(R.id.season_next);

        layoutManager = new LinearLayoutManager(
                host.requireContext(), LinearLayoutManager.HORIZONTAL, false);

        pager.setLayoutManager(layoutManager);
        new PagerSnapHelper().attachToRecyclerView(pager);

        adapter = new SeasonAdapter();
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

        for (int year : years) {
            probePhoto(year, 0, new ArrayList<>());
        }
    }

    // ---------- arrows ----------

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

    // ---------- portraits ----------

    private List<String> photoIds() {
        List<String> ids = new ArrayList<>();

        if (driverId != null && !driverId.trim().isEmpty()) {
            ids.add(driverId.trim());
        }

        if (driverCode != null && !driverCode.trim().isEmpty()
                && !ids.contains(driverCode.trim().toLowerCase())) {
            ids.add(driverCode.trim().toLowerCase());
        }

        return ids;
    }

    private void probePhoto(int year, int index, List<String> tried) {
        List<String> ids = photoIds();

        if (index >= ids.size()) {
            photoChecked.add(year);
            Log.w(TAG, "NO PHOTO " + driverFullName + " " + year + ", tried " + tried);
            refresh(year);
            return;
        }

        String path = "drivers/" + ids.get(index) + "_" + year + ".png";
        tried.add(path);

        FirebaseStorage.getInstance().getReference().child(path).getDownloadUrl()
                .addOnSuccessListener(uri -> {
                    if (!host.isAdded()) {
                        return;
                    }

                    Log.d(TAG, "found " + path);

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

    // ---------- season results (the same source as the standings tables) ----------

    private void loadStanding(int year) {
        if (queue == null) {
            queue = Volley.newRequestQueue(host.requireContext().getApplicationContext());
        }

        String url = "https://api.jolpi.ca/ergast/f1/" + year + "/driverstandings/?format=json";

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.GET,
                url,
                null,
                response -> {
                    if (host.isAdded()) {
                        try {
                            readStanding(year, response);
                        } catch (JSONException error) {
                            Log.w(TAG, "Unexpected standings response for " + year, error);
                        }
                    }

                    yearDone();
                },
                error -> {
                    Log.w(TAG, "Standings are not available for " + year, error);
                    yearDone();
                }
        );

        request.setTag(this);
        queue.add(request);
    }

    private void readStanding(int year, JSONObject response) throws JSONException {
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

                String teamName = DASH;
                String teamId = "";
                JSONArray constructors = entry.optJSONArray("Constructors");

                if (constructors != null && constructors.length() > 0) {
                    JSONObject last = constructors.getJSONObject(constructors.length() - 1);

                    teamName = last.optString("name", DASH);
                    teamId = last.optString("constructorId", "");
                }

                standings.put(year, new String[]{
                        entry.optString("positionText", DASH),
                        entry.optString("points", DASH),
                        entry.optString("wins", DASH),
                        teamName,
                        teamId
                });

                return;
            }
        }
    }

    private boolean isThisDriver(JSONObject driver) {
        String code = driver.optString("code", "");

        if (driverCode != null && !driverCode.isEmpty() && !code.isEmpty()) {
            return driverCode.equalsIgnoreCase(code);
        }

        return familyName != null && familyName.equalsIgnoreCase(driver.optString("familyName", ""));
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

    /** Colour of the team of that season: the database first, then the palette of past teams. */
    private int colorFor(int year, View view) {
        String[] data = standings.get(year);

        if (data != null && !data[4].isEmpty()) {
            String hex = pastTeamPalette.resolve(
                    data[4], teamColors.get(teamCarCatalog.normalize(data[4])));

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

    // ---------- pages ----------

    private final class SeasonAdapter extends RecyclerView.Adapter<SeasonAdapter.Holder> {

        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            View view = inflater.inflate(R.layout.item_driver_season_page, parent, false);
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

            holder.year.setText(String.valueOf(year));

            String[] data = standings.get(year);
            holder.team.setText(data != null ? data[3] : "");

            paint(holder, colorFor(year, holder.itemView));

            Uri uri = uris.get(year);

            if (uri != null) {
                holder.missing.setVisibility(View.GONE);

                GlideApp.with(holder.photo)
                        .load(uri)
                        .transition(DrawableTransitionOptions.withCrossFade())
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .into(holder.photo);
            } else {
                GlideApp.with(holder.photo).clear(holder.photo);
                holder.photo.setImageDrawable(null);

                // still checking: nothing to say yet; checked and not found: say it
                holder.missing.setVisibility(photoChecked.contains(year) ? View.VISIBLE : View.GONE);
            }

            for (int i = 0; i < holder.values.length; i++) {
                holder.values[i].setText(data != null ? data[i] : DASH);
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
            final TextView team;
            final LinearLayout results;
            final TextView[] values = new TextView[3];

            Holder(@NonNull View itemView) {
                super(itemView);
                card = itemView.findViewById(R.id.season_card);
                photoBlock = itemView.findViewById(R.id.season_photo_block);
                photo = itemView.findViewById(R.id.season_photo);
                missing = itemView.findViewById(R.id.season_missing);
                year = itemView.findViewById(R.id.season_year);
                team = itemView.findViewById(R.id.season_team);
                results = itemView.findViewById(R.id.season_results);
            }
        }
    }
}
