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

import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
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

    private RecyclerView pager;
    private LinearLayoutManager layoutManager;
    private View prev;
    private View next;
    private SeasonAdapter adapter;

    private int pendingYears;
    private final String givenName;
    private final int firstSeason;

    private static final String PICKER_KEY = "driver_seasons_year";

    driverSeasonsSection(Fragment host, View root, String driverFullName, String driverCode,
                         String driverId, String givenName, String familyName,
                         int firstSeason, int latestSeason) {

        this.host = host;
        this.root = root;
        this.driverFullName = driverFullName;
        this.driverCode = driverCode;
        this.driverId = driverId;
        this.givenName = givenName;
        this.familyName = familyName;
        this.firstSeason = firstSeason;
        this.latest = latestSeason;
    }

    void setDefaultColor(Integer color) {
        defaultColor = color;

        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }

    void stop() {
        // requests are shared and finish on their own
    }

    void start() {
        int first = Math.max(firstSeason, 1950);

        if (latest < first) {
            return;
        }

        for (int year = latest; year >= first; year--) {
            loadStanding(year);
        }

        FirebaseDatabase.getInstance().getReference().child("constructors")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (!host.isAdded()) {
                            return;
                        }

                        for (DataSnapshot team : snapshot.getChildren()) {
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

    private void show() {
        if (years.isEmpty()) {
            return;
        }

        for (int year : years) {
            if (year >= FIRST_PHOTO_YEAR) {
                probePhoto(year, 0, new ArrayList<>());
            } else {
                photoChecked.add(year);
            }
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

        host.getChildFragmentManager().setFragmentResultListener(
                PICKER_KEY,
                host.getViewLifecycleOwner(),
                (key, result) -> scrollToYear(result.getInt(yearPickerSheet.YEAR)));

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

    private void openYearPicker(int selectedYear) {
        if (!host.isAdded()) {
            return;
        }

        yearPickerSheet.show(host.getChildFragmentManager(),
                new ArrayList<>(years), selectedYear, PICKER_KEY);
    }

    private void scrollToYear(int year) {
        int position = years.indexOf(year);

        if (pager != null && position >= 0) {
            pager.scrollToPosition(position);
            updateArrows();
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
        jolpicaSeasons.driverStandings(year, response -> {
            if (!host.isAdded() || response == null) {
                return;
            }

            JSONObject entry = jolpicaSeasons.driverEntry(response, driverCode, givenName, familyName);

            if (entry == null) {
                return;
            }

            JSONArray constructors = entry.optJSONArray("Constructors");

            if (constructors == null || constructors.length() <= 1) {
                store(year, entry, constructors == null || constructors.length() == 0
                        ? null
                        : constructors.optJSONObject(0));
                return;
            }

            JSONObject driver = entry.optJSONObject("Driver");
            String driverId = driver == null ? "" : driver.optString("driverId", "");

            jolpicaSeasons.driverResults(year, driverId, results -> {
                if (host.isAdded()) {
                    store(year, entry, jolpicaSeasons.mainConstructor(constructors, results));
                }
            });
        });
    }

    private void store(int year, JSONObject entry, JSONObject team) {
        standings.put(year, new String[]{
                entry.optString("positionText", DASH),
                entry.optString("points", DASH),
                entry.optString("wins", DASH),
                team != null ? team.optString("name", DASH) : DASH,
                team != null ? team.optString("constructorId", "") : ""
        });

        addYear(year);
    }

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

        probePhoto(year, 0, new ArrayList<>());
    }

    private void refresh(int year) {
        if (pager == null) {
            return;
        }

        pager.post(() -> {
            if (!host.isAdded() || adapter == null) {
                return;
            }

            int position = years.indexOf(year);

            if (position >= 0) {
                adapter.notifyItemChanged(position);
            }
        });
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

            holder.year.setText(String.valueOf(year));
            holder.year.setOnClickListener(v -> openYearPicker(year));

            String[] data = standings.get(year);
            holder.team.setText(data != null ? data[3] : "");

            paint(holder, colorFor(year, holder.itemView));

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
                    holder.photo.setImageResource(R.drawable.placeholder_driver);
                } else {
                    holder.photo.setImageDrawable(null);
                }
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
