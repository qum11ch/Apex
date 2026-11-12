package com.example.f1app;

import android.content.Intent;
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
import android.widget.ToggleButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.facebook.shimmer.ShimmerFrameLayout;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class teamResultsFragment extends Fragment {

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

        if (getArguments() == null) {
            return;
        }

        teamId = getArguments().getString("teamId", "");
        teamName = getArguments().getString("teamName", "");

        if (teamId.trim().isEmpty()) {
            return;
        }

        showLoading();

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

        seasonButtonsLayout.removeAllViews();

        for (int season : availableSeasons) {
            seasonButtonsLayout.addView(createSeasonButton(season));
        }

        selectSeason(availableSeasons.get(0));
    }

    private ToggleButton createSeasonButton(int season) {
        ToggleButton seasonButton = (ToggleButton) LayoutInflater.from(requireContext())
                .inflate(R.layout.item_season_toggle, seasonButtonsLayout, false);

        String seasonText = String.valueOf(season);

        seasonButton.setTextOn(seasonText);
        seasonButton.setTextOff(seasonText);
        seasonButton.setChecked(false);
        seasonButton.setText(seasonText);
        seasonButton.setTag(season);

        seasonButton.setOnClickListener(view -> selectSeason((Integer) view.getTag()));

        return seasonButton;
    }

    private void selectSeason(int season) {
        boolean seasonChanged = selectedSeason != season;

        selectedSeason = season;

        for (int index = 0; index < seasonButtonsLayout.getChildCount(); index++) {
            View child = seasonButtonsLayout.getChildAt(index);

            if (!(child instanceof ToggleButton)) {
                continue;
            }

            ToggleButton button = (ToggleButton) child;
            button.setChecked((Integer) button.getTag() == season);
        }

        if (seasonChanged) {
            loadSeasonData(season);
        }
    }

    private void loadSeasonData(int season) {
        currentSeasonDrivers.clear();
        raceCounts.clear();
        rows.clear();
        resultsAdapter = null;

        recyclerView.setAdapter(null);
        recyclerView.setVisibility(View.GONE);

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

                        showLoading();

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

        rows.clear();
        resultsAdapter = null;
        recyclerView.setAdapter(null);

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

                                        buildRows(scheduleSnapshot, resultSnapshot, drivers);

                                        resultsAdapter = new teamResultsAdapter(
                                                requireActivity(), rows, drivers.size());

                                        recyclerView.setAdapter(resultsAdapter);

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

    /** One row per race in schedule order; one code per driver in the header order. */
    private void buildRows(
            DataSnapshot scheduleSnapshot,
            DataSnapshot resultSnapshot,
            ArrayList<String> drivers) {

        for (DataSnapshot raceSnapshot : scheduleSnapshot.getChildren()) {
            String raceName = raceSnapshot.getKey();

            if (raceName == null) {
                continue;
            }

            ArrayList<String> codes = new ArrayList<>();

            for (String driver : drivers) {
                codes.add(getDriverRaceResult(resultSnapshot, driver, raceName));
            }

            rows.add(new teamRaceRow(raceName, codes));
        }
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
