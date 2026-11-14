package com.example.f1app;

import static com.example.f1app.MainActivity.checkLightTheme;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.ColorUtils;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.facebook.shimmer.ShimmerFrameLayout;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;


public class teamResultsFragment extends Fragment {
    private View accentLine;
    private View summaryCard;
    private LinearLayout summaryRow;

    private teamResultsAdapter resultsAdapter;
    private final List<teamRaceRow> rows = new ArrayList<>();
    private final Map<String, Integer> raceCounts = new HashMap<>();

    private RecyclerView recyclerView;
    private LinearLayout headerDrivers;
    private ShimmerFrameLayout shimmerFrameLayout;
    private LinearLayout seasonButtonsLayout;

    private String teamId = "";
    private String teamName = "";

    private final ArrayList<String> currentSeasonDrivers = new ArrayList<>();

    private int selectedSeason = -1;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable hideShimmerRunnable;

    private RequestQueue queue;
    private LinearLayout summaryTop;
    private TextView pointsValue;
    private TextView positionValue;

    private List<Integer> seasonsShown;
    private boolean showingRemembered = false;

    /** Everything one season of the team page shows, kept so a repeat visit needs no shimmer. */
    private static final class SeasonSnapshot {
        final ArrayList<String> drivers;
        final Map<String, Integer> counts;
        final List<teamRaceRow> rows;

        SeasonSnapshot(ArrayList<String> drivers, Map<String, Integer> counts, List<teamRaceRow> rows) {
            this.drivers = new ArrayList<>(drivers);
            this.counts = new HashMap<>(counts);
            this.rows = new ArrayList<>(rows);
        }
    }

    public teamResultsFragment() {
    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {

        return inflater.inflate(
                R.layout.team_page_results_fragment,
                container,
                false
        );
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        EdgeInsets.applyBottom(view.findViewById(R.id.scrollView));

        headerDrivers = view.findViewById(R.id.header_drivers);
        shimmerFrameLayout = view.findViewById(R.id.shimmer_layout);
        seasonButtonsLayout = view.findViewById(R.id.seasonButtonsLayout);
        recyclerView = view.findViewById(R.id.drivers_results);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        buildLegend(view);

        accentLine = view.findViewById(R.id.line);
        summaryCard = view.findViewById(R.id.summary_card);
        summaryRow = view.findViewById(R.id.summary_row);
        summaryTop = view.findViewById(R.id.summary_top);

        buildSummary();

        if (getArguments() == null) {
            return;
        }

        teamId = getArguments().getString("teamId", "");
        teamName = getArguments().getString("teamName", "");

        if (teamId.trim().isEmpty()) {
            return;
        }

        final List<Integer> rememberedSeasons = cachedList.recall("team_seasons|" + teamId);

        if (rememberedSeasons != null && !rememberedSeasons.isEmpty()) {
            showAvailableSeasons(new ArrayList<>(rememberedSeasons)); // seen before: no shimmer
        } else {
            showLoading();
        }

        loadAvailableSeasons();
    }

    private void loadAvailableSeasons() {
        FirebaseDatabase.getInstance()
                .getReference()
                .child("constructors")
                .child(teamId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (!isAdded()) {
                            return;
                        }

                        Integer enterYear = snapshot
                                .child("enterYear")
                                .getValue(Integer.class);

                        applyAccent(pastTeamPalette.resolve(
                                teamId, snapshot.child("color").getValue(String.class)));


                        int earliestSeason = enterYear != null
                                ? enterYear
                                : LocalDate.now().getYear();

                        int latestSeason = LocalDate.now().getYear();

                        if (earliestSeason > latestSeason) {
                            int temp = earliestSeason;
                            earliestSeason = latestSeason;
                            latestSeason = temp;
                        }

                        checkSeasons(earliestSeason, latestSeason);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        if (!isAdded()) {
                            return;
                        }

                        Log.e("TeamResults", "Failed to load constructor", error.toException());
                    }
                });
    }

    private void checkSeasons(int earliestSeason, int latestSeason) {
        ArrayList<Integer> seasonCandidates = new ArrayList<>();

        for (int season = latestSeason; season >= earliestSeason; season--) {
            seasonCandidates.add(season);
        }

        ArrayList<Integer> availableSeasons = new ArrayList<>();

        final int totalChecks = seasonCandidates.size();
        final int[] completedChecks = {0};

        DatabaseReference resultsRef = FirebaseDatabase.getInstance()
                .getReference()
                .child("results")
                .child("season");

        for (Integer season : seasonCandidates) {
            resultsRef.child(String.valueOf(season))
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            if (!isAdded()) {
                                return;
                            }

                            if (teamHasResults(snapshot)) {
                                availableSeasons.add(season);
                            }

                            completedChecks[0]++;

                            if (completedChecks[0] == totalChecks) {
                                showAvailableSeasons(availableSeasons);
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            if (!isAdded()) {
                                return;
                            }

                            Log.e("TeamResults", "Failed to check season " + season,
                                    error.toException());

                            completedChecks[0]++;

                            if (completedChecks[0] == totalChecks) {
                                showAvailableSeasons(availableSeasons);
                            }
                        }
                    });
        }
    }

    private boolean teamHasResults(DataSnapshot seasonSnapshot) {
        for (DataSnapshot driverSnapshot : seasonSnapshot.getChildren()) {
            for (DataSnapshot raceSnapshot : driverSnapshot.getChildren()) {
                String raceTeamId = raceSnapshot.child("TeamId").getValue(String.class);

                if (teamId.equals(raceTeamId)) {
                    return true;
                }
            }
        }

        return false;
    }

    private void showAvailableSeasons(ArrayList<Integer> availableSeasons) {
        if (!isAdded()) {
            return;
        }

        if (availableSeasons.isEmpty()) {
            hideShimmer();
            return;
        }

        Collections.sort(availableSeasons);
        Collections.reverse(availableSeasons);

        cachedList.remember("team_seasons|" + teamId, availableSeasons);

        if (availableSeasons.equals(seasonsShown)) {
            return; // same buttons as before
        }

        seasonsShown = new ArrayList<>(availableSeasons);

        seasonButtonsLayout.removeAllViews();

        for (int season : availableSeasons) {
            seasonButtonsLayout.addView(createSeasonButton(season));
        }

        selectSeason(availableSeasons.contains(selectedSeason)
                ? selectedSeason
                : availableSeasons.get(0));
    }

    private TextView createSeasonButton(int season) {
        TextView button = (TextView) LayoutInflater.from(requireContext())
                .inflate(R.layout.item_public_profile_segment, seasonButtonsLayout, false);

        button.setText(String.valueOf(season));
        button.setTag(season);
        button.setOnClickListener(view -> selectSeason((Integer) view.getTag()));

        return button;
    }

    private void selectSeason(int season) {
        boolean seasonChanged = selectedSeason != season;

        selectedSeason = season;

        int selectedColor = ContextCompat.getColor(requireContext(), R.color.text_color_main);
        int idleColor = ContextCompat.getColor(requireContext(), R.color.grey);

        for (int index = 0; index < seasonButtonsLayout.getChildCount(); index++) {
            View child = seasonButtonsLayout.getChildAt(index);

            if (!(child instanceof TextView) || child.getTag() == null) {
                continue;
            }

            boolean selected = (Integer) child.getTag() == season;

            child.setSelected(selected);
            ((TextView) child).setTextColor(selected ? selectedColor : idleColor);
        }

        if (seasonChanged) {
            loadSeasonData(season);
        }
    }


    private void loadSeasonData(int season) {
        final List<SeasonSnapshot> remembered = cachedList.recall(snapshotKey(season));
        showingRemembered = remembered != null && !remembered.isEmpty();

        if (showingRemembered) {
            showRememberedSeason(remembered.get(0), season); // seen before: no shimmer, refresh quietly
        } else {
            currentSeasonDrivers.clear();
            summaryCard.setVisibility(View.GONE);
            raceCounts.clear();
            rows.clear();
            resultsAdapter = null;

            recyclerView.setAdapter(null);
            recyclerView.setVisibility(View.GONE);
        }

        FirebaseDatabase.getInstance()
                .getReference()
                .child("results")
                .child("season")
                .child(String.valueOf(season))
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (!isAdded() || season != selectedSeason) {
                            return;
                        }

                        if (!snapshot.exists()) {
                            hideShimmer();
                            return;
                        }

                        currentSeasonDrivers.clear();
                        raceCounts.clear();

                        for (DataSnapshot driverSnapshot : snapshot.getChildren()) {
                            String driverName = driverSnapshot.getKey();

                            if (driverName == null) {
                                continue;
                            }

                            int races = 0;

                            for (DataSnapshot raceSnapshot : driverSnapshot.getChildren()) {
                                String raceTeamId = raceSnapshot
                                        .child("TeamId")
                                        .getValue(String.class);

                                if (teamId.equals(raceTeamId)) {
                                    races++;
                                }
                            }

                            if (races > 0) {
                                currentSeasonDrivers.add(driverName);
                                raceCounts.put(driverName, races);
                            }
                        }

                        if (currentSeasonDrivers.isEmpty()) {
                            hideShimmer();
                            return;
                        }

                        // More races for the team means further left; ties go alphabetically.
                        Collections.sort(currentSeasonDrivers, (a, b) -> {
                            int byRaces = Integer.compare(
                                    raceCounts.getOrDefault(b, 0),
                                    raceCounts.getOrDefault(a, 0));

                            return byRaces != 0 ? byRaces : a.compareTo(b);
                        });

                        if (!showingRemembered) {
                            showLoading();
                        }

                        loadSeasonPoints(season);

                        ArrayList<String> seasonDrivers = new ArrayList<>(currentSeasonDrivers);
                        loadDriversInfo(seasonDrivers, season);
                        getResults(season, seasonDrivers);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        if (!isAdded()) {
                            return;
                        }

                        Log.e("TeamResults", "Failed to load season drivers", error.toException());

                        hideShimmer();
                    }
                });
    }

    private String snapshotKey(int season) {
        return "team_results|" + teamId + "|" + season;
    }

    private void showRememberedSeason(SeasonSnapshot snapshot, int season) {
        currentSeasonDrivers.clear();
        currentSeasonDrivers.addAll(snapshot.drivers);
        raceCounts.clear();
        raceCounts.putAll(snapshot.counts);
        rows.clear();
        rows.addAll(snapshot.rows);

        loadDriversInfo(new ArrayList<>(snapshot.drivers), season);
        loadSeasonPoints(season);
        updateSummary(rows);

        resultsAdapter = new teamResultsAdapter(requireActivity(), rows, snapshot.drivers.size());
        recyclerView.setAdapter(resultsAdapter);
        recyclerView.setVisibility(View.VISIBLE);

        shimmerFrameLayout.stopShimmer();
        shimmerFrameLayout.setVisibility(View.GONE);
    }

    private void loadDriversInfo(ArrayList<String> drivers, int season) {
        headerDrivers.removeAllViews();

        LinearLayout.LayoutParams headerParams =
                (LinearLayout.LayoutParams) headerDrivers.getLayoutParams();
        headerParams.weight = drivers.size();
        headerDrivers.setLayoutParams(headerParams);

        StorageReference storageRef = FirebaseStorage.getInstance().getReference();

        int maxRaces = 0;

        for (String name : drivers) {
            maxRaces = Math.max(maxRaces, raceCounts.getOrDefault(name, 0));
        }

        for (String driverFullName : drivers) {
            String[] names = driverNameFormatter.split(driverFullName);
            String driverName = names[0];
            String driverFamilyName = names[1];

            View column = LayoutInflater.from(requireContext())
                    .inflate(R.layout.item_team_results_driver, headerDrivers, false);

            headerDrivers.addView(column, new LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

            TextView family = column.findViewById(R.id.driver_family_name);
            TextView caption = column.findViewById(R.id.driver_races_caption);
            ImageView photo = column.findViewById(R.id.driver_photo);

            ShapeableImageView stripedImage = column.findViewById(R.id.shapeableStriped);

            if (!checkLightTheme(getContext())){
                stripedImage.setBackground(ContextCompat
                        .getDrawable(getContext(), R.drawable.background_striped_lines_item_night));
            }else{
                stripedImage.setBackground(ContextCompat
                        .getDrawable(getContext(), R.drawable.background_striped_lines_white));
            }

            family.setText(driverFamilyName);

            int races = raceCounts.getOrDefault(driverFullName, 0);
            caption.setText(races < maxRaces
                    ? getString(R.string.team_results_races_caption, races)
                    : "");

            column.setOnClickListener(v ->
                    navigateToDriverPage(driverFullName, driverName, driverFamilyName));

            FirebaseDatabase.getInstance()
                    .getReference()
                    .child("drivers")
                    .child(driverFullName)
                    .child("driversCode")
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            if (!isAdded() || season != selectedSeason) {
                                return;
                            }

                            String code = snapshot.getValue(String.class);

                            if (code == null) {
                                return;
                            }

                            GlideApp.with(requireContext())
                                    .load(storageRef.child(
                                            "drivers/" + code.toLowerCase() + "_" + season + ".png"))
                                    .transition(DrawableTransitionOptions.withCrossFade())
                                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                                    .error(R.drawable.placeholder_driver)
                                    .into(photo);
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            if (!isAdded()) {
                                return;
                            }

                            Log.e("TeamResults", "Failed to load driver code", error.toException());
                        }
                    });
        }
    }

    private void navigateToDriverPage(
            String driverFullName,
            String driverName,
            String driverFamilyName) {

        FirebaseDatabase.getInstance()
                .getReference()
                .child("drivers")
                .child(driverFullName)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (!isAdded()) {
                            return;
                        }

                        String driverCode = snapshot.child("driversCode").getValue(String.class);

                        Intent intent = new Intent(requireContext(), driverPageActivity.class);

                        Bundle bundle = new Bundle();
                        bundle.putString("driverName", driverName);
                        bundle.putString("driverFamilyName", driverFamilyName);
                        bundle.putString("driverTeam", teamName);
                        bundle.putString("driverCode", driverCode);
                        bundle.putString("driverTeamId", teamId);

                        intent.putExtras(bundle);

                        startActivity(intent);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        if (!isAdded()) {
                            return;
                        }

                        Log.e("TeamResults", "Failed to open driver page", error.toException());
                    }
                });
    }

    private void getResults(int season, ArrayList<String> drivers) {
        if (drivers.isEmpty()) {
            hideShimmer();
            return;
        }

        if (!showingRemembered) {
            rows.clear();
            resultsAdapter = null;
            recyclerView.setAdapter(null);
        }

        FirebaseDatabase database = FirebaseDatabase.getInstance();

        database.getReference()
                .child("schedule")
                .child("season")
                .child(String.valueOf(season))
                .orderByChild("round")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot scheduleSnapshot) {
                        if (!isAdded() || season != selectedSeason) {
                            return;
                        }

                        if (scheduleSnapshot.getChildrenCount() == 0) {
                            hideShimmer();
                            return;
                        }

                        database.getReference()
                                .child("results")
                                .child("season")
                                .child(String.valueOf(season))
                                .addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot resultSnapshot) {
                                        if (!isAdded() || season != selectedSeason) {
                                            return;
                                        }

                                        rows.clear();
                                        buildRows(scheduleSnapshot, resultSnapshot, drivers);

                                        updateSummary(rows);

                                        resultsAdapter = new teamResultsAdapter(
                                                requireActivity(), rows, drivers.size());

                                        recyclerView.setAdapter(resultsAdapter);

                                        cachedList.remember(snapshotKey(season),
                                                java.util.Collections.singletonList(
                                                        new SeasonSnapshot(drivers, raceCounts, rows)));
                                        showingRemembered = false;

                                        hideShimmer();
                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError error) {
                                        if (!isAdded()) {
                                            return;
                                        }

                                        Log.e("TeamResults", "Failed to load results",
                                                error.toException());

                                        hideShimmer();
                                    }
                                });
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        if (!isAdded()) {
                            return;
                        }

                        Log.e("TeamResults", "Failed to load schedule", error.toException());

                        hideShimmer();
                    }
                });
    }


    private void buildRows(
            DataSnapshot scheduleSnapshot,
            DataSnapshot resultSnapshot,
            ArrayList<String> drivers) {

        for (DataSnapshot raceSnapshot : scheduleSnapshot.getChildren()) {
            String raceName = raceSnapshot.getKey();

            if (raceName == null) {
                continue;
            }

            int round = 0;
            Object roundValue = raceSnapshot.child("round").getValue();

            if (roundValue != null) {
                try {
                    round = Integer.parseInt(String.valueOf(roundValue).trim());
                } catch (NumberFormatException ignored) {
                    // round stays 0 and the label is hidden
                }
            }

            ArrayList<String> codes = new ArrayList<>();

            for (String driver : drivers) {
                codes.add(getDriverRaceResult(resultSnapshot, driver, raceName));
            }

            rows.add(new teamRaceRow(raceName, round, codes));
        }
    }

    private void buildLegend(View root) {
        resultsLegend.build(
                requireContext(),
                root.findViewById(R.id.legend_left),
                root.findViewById(R.id.legend_right));
    }

    private void oldBuildLegend(View root) {
        LinearLayout left = root.findViewById(R.id.legend_left);
        LinearLayout right = root.findViewById(R.id.legend_right);

        // 0 = без заливки ("не участвовал")
        int[] fills = {
                R.color.light_gold, R.color.light_silver, R.color.light_bronze,
                R.color.light_green, R.color.light_purple, R.color.light_grey,
                R.color.pink, R.color.white, R.color.black,
                0, R.color.light_grey, R.color.orange
        };

        int[] labels = {
                R.string._1st_place_header, R.string._2nd_place_header, R.string._3rd_place_header,
                R.string.points_finish, R.string.out_of_points_finish, R.string.pole,
                R.string.retired, R.string.dns, R.string.dsq,
                R.string.wd, R.string.not_concluded, R.string.cnd
        };

        String[] samples = {
                "1", "2", "3", "5", "15", "1",
                "", "", "", "-", getString(R.string.n_c_text), ""
        };

        float density = getResources().getDisplayMetrics().density;
        LayoutInflater inflater = LayoutInflater.from(requireContext());

        for (int i = 0; i < fills.length; i++) {
            LinearLayout column = i < 6 ? left : right;
            View entry = inflater.inflate(R.layout.item_results_legend_entry, column, false);

            TextView swatch = entry.findViewById(R.id.legend_swatch);
            TextView label = entry.findViewById(R.id.legend_label);

            boolean pole = labels[i] == R.string.pole;
            boolean empty = fills[i] == 0;

            GradientDrawable background = new GradientDrawable();
            background.setCornerRadius(6 * density);
            background.setColor(empty
                    ? Color.TRANSPARENT
                    : ContextCompat.getColor(requireContext(), fills[i]));

            if (pole) {
                background.setStroke(Math.round(2 * density),
                        ContextCompat.getColor(requireContext(), R.color.purple));
            } else if (fills[i] == R.color.white) {
                background.setStroke(Math.round(density),
                        ContextCompat.getColor(requireContext(), R.color.grey));
            }

            swatch.setBackground(background);
            swatch.setText(samples[i]);
            swatch.setTextColor(ContextCompat.getColor(requireContext(),
                    empty ? R.color.grey
                            : (fills[i] == R.color.black || fills[i] == R.color.orange
                            ? R.color.white : R.color.dark_grey)));

            label.setText(labels[i]);

            column.addView(entry);
        }
    }

    private void applyAccent(String hex) {
        try {
            int color = Color.parseColor(hex);
            accentLine.setBackgroundColor(ColorUtils.setAlphaComponent(color, 110));
        } catch (IllegalArgumentException ignored) {
            // keep the neutral separator
        }
    }

    private void buildSummary() {
        LayoutInflater inflater = LayoutInflater.from(requireContext());

        View pointsTile = inflater.inflate(R.layout.item_team_results_summary, summaryTop, false);
        ((TextView) pointsTile.findViewById(R.id.summary_label)).setText(R.string.team_results_points);
        pointsValue = pointsTile.findViewById(R.id.summary_value);
        summaryTop.addView(pointsTile, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        View positionTile = inflater.inflate(R.layout.item_team_results_summary, summaryTop, false);
        ((TextView) positionTile.findViewById(R.id.summary_label)).setText(R.string.team_results_standing);
        positionValue = positionTile.findViewById(R.id.summary_value);
        summaryTop.addView(positionTile, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        int[] labels = {
                R.string.wins,
                R.string.podiums,
                R.string.poles,
                R.string.team_results_retirements
        };

        for (int label : labels) {
            View tile = inflater.inflate(R.layout.item_team_results_summary, summaryRow, false);

            ((TextView) tile.findViewById(R.id.summary_label)).setText(label);

            summaryRow.addView(tile, new LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        }
    }



    private void updateSummary(List<teamRaceRow> seasonRows) {
        int wins = 0;
        int podiums = 0;
        int poles = 0;
        int retirements = 0;

        for (teamRaceRow row : seasonRows) {
            for (String code : row.results) {
                if (code == null) {
                    continue;
                }

                String[] parts = code.split("-");

                if (parts.length != 2) {
                    continue;
                }

                if ("1".equals(parts[0])) {
                    poles++;
                }

                switch (parts[1]) {
                    case "R":
                        retirements++;
                        break;
                    case "W":
                    case "D":
                        break;
                    default:
                        try {
                            int position = Integer.parseInt(parts[1]);

                            if (position == 1) wins++;
                            if (position <= 3) podiums++;
                        } catch (NumberFormatException ignored) {
                            // not a finishing position
                        }
                        break;
                }
            }
        }

        int[] values = {wins, podiums, poles, retirements};

        for (int i = 0; i < summaryRow.getChildCount() && i < values.length; i++) {
            ((TextView) summaryRow.getChildAt(i).findViewById(R.id.summary_value))
                    .setText(String.valueOf(values[i]));
        }

        summaryCard.setVisibility(seasonRows.isEmpty() ? View.GONE : View.VISIBLE);
    }

    private void loadSeasonPoints(int season) {
        pointsValue.setText("\u2013");
        positionValue.setText("\u2013");

        if (queue == null) {
            queue = apiQueue.get();
        }

        String url = "https://api.jolpi.ca/ergast/f1/" + season + "/constructorstandings/?format=json";

        JsonObjectRequest request = new cachedJsonRequest(
                Request.Method.GET,
                url,
                null,
                response -> {
                    if (!isAdded() || season != selectedSeason) {
                        return;
                    }

                    try {
                        JSONArray lists = response
                                .getJSONObject("MRData")
                                .getJSONObject("StandingsTable")
                                .optJSONArray("StandingsLists");

                        if (lists == null) {
                            return;
                        }

                        String wanted = normalizeTeamId(teamId);

                        for (int i = 0; i < lists.length(); i++) {
                            JSONArray standings = lists.getJSONObject(i)
                                    .optJSONArray("ConstructorStandings");

                            if (standings == null) {
                                continue;
                            }

                            for (int j = 0; j < standings.length(); j++) {
                                JSONObject standing = standings.getJSONObject(j);
                                JSONObject constructor = standing.optJSONObject("Constructor");

                                if (constructor != null && wanted.equals(
                                        normalizeTeamId(constructor.optString("constructorId")))) {

                                    pointsValue.setText(standing.optString("points", "\u2013"));
                                    positionValue.setText(standing.optString("positionText", "\u2013"));
                                    return;
                                }
                            }
                        }
                    } catch (JSONException error) {
                        Log.w("TeamResults", "Unexpected standings response", error);
                    }
                },
                error -> Log.w("TeamResults", "Team points are not available", error)
        );

        request.setTag(this);
        queue.add(request);
    }

    /** "red_bull" (API) and "redbull" (database) become the same id. */
    private String normalizeTeamId(String id) {
        return id == null ? "" : id.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }


    private String getDriverRaceResult(
            DataSnapshot resultSnapshot,
            String driverFullName,
            String raceName) {

        DataSnapshot raceResult = resultSnapshot.child(driverFullName).child(raceName);
        String result = raceResult.child("Result").getValue(String.class);
        String resultTeamId = raceResult.child("TeamId").getValue(String.class);

        if ("N/C".equals(result)) return "N/C";
        if (result != null && teamId.equals(resultTeamId)) return result;

        return "null";
    }

    private void hideShimmer() {
        if (hideShimmerRunnable != null) {
            handler.removeCallbacks(hideShimmerRunnable);
        }

        hideShimmerRunnable = () -> {
            if (!isAdded()) {
                return;
            }

            recyclerView.setVisibility(View.VISIBLE);

            shimmerFrameLayout.stopShimmer();
            shimmerFrameLayout.setVisibility(View.GONE);
        };

        handler.postDelayed(hideShimmerRunnable, 500);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (queue != null) {
            queue.cancelAll(this);
        }

        if (hideShimmerRunnable != null) {
            handler.removeCallbacks(hideShimmerRunnable);
        }
    }

    private void showLoading() {
        if (hideShimmerRunnable != null) {
            handler.removeCallbacks(hideShimmerRunnable);
            hideShimmerRunnable = null;
        }

        recyclerView.setVisibility(View.GONE);

        shimmerFrameLayout.setVisibility(View.VISIBLE);
        shimmerFrameLayout.startShimmer();
    }
}
