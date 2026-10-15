package com.example.f1app;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
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
import java.util.List;

public class teamResultsFragment extends Fragment {

    private teamDriversResultsAdapter adapter;
    private teamTripleDriversResultsAdapter adapterTriple;

    private RecyclerView recyclerView;

    private List<teamDriversResultsData> datum;
    private List<teamTripleDriversResultsData> datumTriple;

    private TextView firstDriverFamilyName;
    private TextView secondDriverFamilyName;
    private TextView thirdDriverFamilyName;
    private TextView raceNameHeader;

    private ImageView firstDriverImage;
    private ImageView secondDriverImage;
    private ImageView thirdDriverImage;

    private ShimmerFrameLayout shimmerFrameLayout;
    private ShimmerFrameLayout shimmerTripleFrameLayout;

    private LinearLayout seasonButtonsLayout;

    private RelativeLayout firstDriverLayout;
    private RelativeLayout secondDriverLayout;
    private RelativeLayout thirdDriverLayout;

    private String teamId = "";
    private String teamName = "";

    private ArrayList<String> currentSeasonDrivers = new ArrayList<>();

    private int selectedSeason = -1;
    private final Handler handler = new Handler(Looper.getMainLooper());

    private Runnable hideShimmerRunnable;

    private boolean isTripleDriver = false;

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

        firstDriverFamilyName = view.findViewById(R.id.firstDriverFamilyName);
        secondDriverFamilyName = view.findViewById(R.id.secondDriverFamilyName);
        thirdDriverFamilyName = view.findViewById(R.id.thirdDriverFamilyName);
        firstDriverImage = view.findViewById(R.id.firstDriver_image);
        secondDriverImage = view.findViewById(R.id.secondDriver_image);
        thirdDriverImage = view.findViewById(R.id.thirdDriver_image);
        shimmerFrameLayout = view.findViewById(R.id.shimmer_layout);
        shimmerTripleFrameLayout = view.findViewById(R.id.shimmerTriple_layout);
        seasonButtonsLayout = view.findViewById(R.id.seasonButtonsLayout);
        firstDriverLayout = view.findViewById(R.id.firstDriver_layout);
        secondDriverLayout = view.findViewById(R.id.secondDriver_layout);
        thirdDriverLayout = view.findViewById(R.id.thirdDriver_layout);
        raceNameHeader = view.findViewById(R.id.raceName_header);
        recyclerView = view.findViewById(R.id.drivers_results);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        datum = new ArrayList<>();
        datumTriple = new ArrayList<>();

        if (getArguments() == null) {
            return;
        }

        teamId = getArguments().getString("teamId", "");
        teamName = getArguments().getString("teamName", "");

        if (teamId.trim().isEmpty()) {
            return;
        }

        showLoading(false);

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

                        int latestSeason =
                                LocalDate.now().getYear();

                        if (earliestSeason > latestSeason) {
                            int temp = earliestSeason;
                            earliestSeason = latestSeason;
                            latestSeason = temp;
                        }

                        checkSeasons(
                                earliestSeason,
                                latestSeason
                        );
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        if (!isAdded()) {
                            return;
                        }

                        Log.e(
                                "TeamResults",
                                "Failed to load constructor",
                                error.toException()
                        );
                    }
                });
    }

    private void checkSeasons(
            int earliestSeason,
            int latestSeason) {

        ArrayList<Integer> seasonCandidates = new ArrayList<>();

        for (int season = latestSeason;
             season >= earliestSeason;
             season--) {

            seasonCandidates.add(season);
        }

        ArrayList<Integer> availableSeasons = new ArrayList<>();

        final int totalChecks = seasonCandidates.size();
        final int[] completedChecks = {0};

        DatabaseReference resultsRef =
                FirebaseDatabase.getInstance()
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
                                showAvailableSeasons(
                                        availableSeasons
                                );
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            if (!isAdded()) {
                                return;
                            }

                            Log.e(
                                    "TeamResults",
                                    "Failed to check season "
                                            + season,
                                    error.toException()
                            );

                            completedChecks[0]++;

                            if (completedChecks[0] == totalChecks) {
                                showAvailableSeasons(
                                        availableSeasons
                                );
                            }
                        }
                    });
        }
    }

    private boolean teamHasResults(DataSnapshot seasonSnapshot) {
        for (DataSnapshot driverSnapshot : seasonSnapshot.getChildren()) {
            for (DataSnapshot raceSnapshot : driverSnapshot.getChildren()) {
                String raceTeamId = raceSnapshot
                        .child("TeamId")
                        .getValue(String.class);

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
            seasonButtonsLayout.addView(
                    createSeasonButton(season)
            );
        }

        selectSeason(availableSeasons.get(0));
    }

    private ToggleButton createSeasonButton(int season) {
        ToggleButton seasonButton =
                (ToggleButton) LayoutInflater.from(requireContext())
                        .inflate(
                                R.layout.item_season_toggle,
                                seasonButtonsLayout,
                                false
                        );

        String seasonText = String.valueOf(season);

        seasonButton.setTextOn(seasonText);
        seasonButton.setTextOff(seasonText);
        seasonButton.setChecked(false);
        seasonButton.setText(seasonText);
        seasonButton.setTag(season);

        seasonButton.setOnClickListener(view -> {
            int clickedSeason = (Integer) view.getTag();

            selectSeason(clickedSeason);
        });

        return seasonButton;
    }

    private void selectSeason(int season) {
        boolean seasonChanged = selectedSeason != season;

        selectedSeason = season;

        for (int index = 0;
             index < seasonButtonsLayout.getChildCount();
             index++) {

            View child = seasonButtonsLayout.getChildAt(index);

            if (!(child instanceof ToggleButton)) {
                continue;
            }

            ToggleButton button = (ToggleButton) child;
            int buttonSeason = (Integer) button.getTag();

            button.setChecked(buttonSeason == season);
        }

        if (seasonChanged) {
            loadSeasonData(season);
        }
    }

    private void loadSeasonData(int season) {
        currentSeasonDrivers.clear();

        datum.clear();
        datumTriple.clear();

        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }

        if (adapterTriple != null) {
            adapterTriple.notifyDataSetChanged();
        }

        recyclerView.setVisibility(View.GONE);

        FirebaseDatabase.getInstance()
                .getReference()
                .child("results")
                .child("season")
                .child(String.valueOf(season))
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {

                        if (!isAdded()) {
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

                            boolean droveForTeam = false;

                            for (DataSnapshot raceSnapshot : driverSnapshot.getChildren()) {

                                String raceTeamId = raceSnapshot
                                        .child("TeamId")
                                        .getValue(String.class);

                                if (teamId.equals(raceTeamId)) {
                                    droveForTeam = true;
                                    break;
                                }
                            }

                            if (droveForTeam) {
                                currentSeasonDrivers.add(
                                        driverName
                                );
                            }
                        }

                        if (currentSeasonDrivers.isEmpty()) {
                            hideShimmer();
                            return;
                        }

                        showLoading(isTripleDriver);

                        loadDriversInfo(
                                currentSeasonDrivers,
                                season
                        );

                        getResults(
                                season,
                                currentSeasonDrivers
                        );
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        if (!isAdded()) {
                            return;
                        }

                        Log.e(
                                "TeamResults",
                                "Failed to load season drivers",
                                error.toException()
                        );

                        hideShimmer();
                    }
                });
    }

    private void loadDriversInfo(ArrayList<String> drivers, int season) {
        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference storageRef = storage.getReference();

        for (int index = 0; index < drivers.size() && index < 3; index++) {

            String driverFullName = drivers.get(index);

            String[] nameParts = driverFullName.split(" ");

            String driverName;
            String driverFamilyName;

            if (driverFullName.equals("Andrea Kimi Antonelli")) {
                driverName = nameParts[0] + " " + nameParts[1];
                driverFamilyName = nameParts[2];
            } else if (nameParts.length >= 2) {
                driverName = nameParts[0];
                driverFamilyName = nameParts[1];
            } else {
                driverName = driverFullName;
                driverFamilyName = "";
            }

            int finalIndex = index;
            String finalDriverName = driverName;
            String finalDriverFamilyName = driverFamilyName;

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

                            String driversCode = snapshot
                                    .child("driversCode")
                                    .getValue(String.class);

                            if (driversCode == null) {
                                return;
                            }

                            StorageReference driverImageRef =
                                    storageRef.child(
                                            "drivers/"
                                                    + driversCode
                                                    .toLowerCase()
                                                    + "_"
                                                    + season
                                                    + ".png"
                                    );

                            if (finalIndex == 0) {
                                firstDriverFamilyName.setText(
                                        finalDriverFamilyName
                                );

                                GlideApp.with(requireContext())
                                        .load(driverImageRef)
                                        .transition(
                                                DrawableTransitionOptions
                                                        .withCrossFade()
                                        )
                                        .diskCacheStrategy(
                                                DiskCacheStrategy.ALL
                                        )
                                        .error(
                                                R.drawable.placeholder_driver
                                        )
                                        .into(firstDriverImage);

                                firstDriverLayout.setOnClickListener(
                                        v -> navigateToDriverPage(
                                                driverFullName,
                                                finalDriverName,
                                                finalDriverFamilyName
                                        )
                                );
                            } else if (finalIndex == 1) {
                                secondDriverFamilyName.setText(
                                        finalDriverFamilyName
                                );

                                GlideApp.with(requireContext())
                                        .load(driverImageRef)
                                        .transition(
                                                DrawableTransitionOptions
                                                        .withCrossFade()
                                        )
                                        .diskCacheStrategy(
                                                DiskCacheStrategy.ALL
                                        )
                                        .error(
                                                R.drawable.placeholder_driver
                                        )
                                        .into(secondDriverImage);

                                secondDriverLayout.setOnClickListener(
                                        v -> navigateToDriverPage(
                                                driverFullName,
                                                finalDriverName,
                                                finalDriverFamilyName
                                        )
                                );
                            } else if (finalIndex == 2) {
                                thirdDriverFamilyName.setText(
                                        finalDriverFamilyName
                                );

                                GlideApp.with(requireContext())
                                        .load(driverImageRef)
                                        .transition(
                                                DrawableTransitionOptions
                                                        .withCrossFade()
                                        )
                                        .diskCacheStrategy(
                                                DiskCacheStrategy.ALL
                                        )
                                        .error(
                                                R.drawable.placeholder_driver
                                        )
                                        .into(thirdDriverImage);

                                thirdDriverLayout.setOnClickListener(
                                        v -> navigateToDriverPage(
                                                driverFullName,
                                                finalDriverName,
                                                finalDriverFamilyName
                                        )
                                );
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            if (!isAdded()) {
                                return;
                            }

                            Log.e(
                                    "TeamResults",
                                    "Failed to load driver info",
                                    error.toException()
                            );
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

                        String driverCode = snapshot
                                .child("driversCode")
                                .getValue(String.class);

                        Intent intent = new Intent(
                                requireContext(),
                                driverPageActivity.class
                        );

                        Bundle bundle = new Bundle();

                        bundle.putString(
                                "driverName",
                                driverName
                        );

                        bundle.putString(
                                "driverFamilyName",
                                driverFamilyName
                        );

                        bundle.putString(
                                "driverTeam",
                                teamName
                        );

                        bundle.putString(
                                "driverCode",
                                driverCode
                        );

                        bundle.putString(
                                "driverTeamId",
                                teamId
                        );

                        intent.putExtras(bundle);

                        startActivity(intent);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        if (!isAdded()) {
                            return;
                        }

                        Log.e(
                                "TeamResults",
                                "Failed to open driver page",
                                error.toException()
                        );
                    }
                });
    }

    private void getResults(int season, ArrayList<String> drivers) {
        if (drivers.size() < 2) {
            hideShimmer();
            return;
        }

        datum.clear();
        datumTriple.clear();

        adapter = null;
        adapterTriple = null;

        recyclerView.setAdapter(null);

        isTripleDriver = drivers.size() > 2;

        FirebaseDatabase.getInstance()
                .getReference()
                .child("schedule")
                .child("season")
                .child(String.valueOf(season))
                .orderByChild("round")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot scheduleSnapshot) {
                        if (!isAdded()) {
                            return;
                        }

                        int totalRaces =
                                (int) scheduleSnapshot
                                        .getChildrenCount();

                        if (totalRaces == 0) {
                            hideShimmer();
                            return;
                        }

                        FirebaseDatabase.getInstance()
                                .getReference()
                                .child("results")
                                .child("season")
                                .child(String.valueOf(season))
                                .addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot resultSnapshot) {
                                        if (!isAdded()) {
                                            return;
                                        }

                                        final int[] loadedRaces = {0};

                                        for (DataSnapshot raceSnapshot :
                                                scheduleSnapshot
                                                        .getChildren()) {

                                            String raceName =
                                                    raceSnapshot.getKey();

                                            if (raceName == null) {
                                                loadedRaces[0]++;
                                                continue;
                                            }

                                            processRaceResult(
                                                    raceName,
                                                    resultSnapshot,
                                                    drivers,
                                                    season,
                                                    totalRaces,
                                                    loadedRaces
                                            );
                                        }
                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError error) {
                                        if (!isAdded()) {
                                            return;
                                        }

                                        Log.e(
                                                "TeamResults",
                                                "Failed to load results",
                                                error.toException()
                                        );

                                        hideShimmer();
                                    }
                                });
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        if (!isAdded()) {
                            return;
                        }

                        Log.e(
                                "TeamResults",
                                "Failed to load schedule",
                                error.toException()
                        );

                        hideShimmer();
                    }
                });
    }

    private void processRaceResult(
            String raceName,
            DataSnapshot resultSnapshot,
            ArrayList<String> drivers,
            int season,
            int totalRaces,
            int[] loadedRaces) {

        String firstDriverName = drivers.get(0);
        String secondDriverName = drivers.get(1);

        String firstResult = getDriverRaceResult(
                resultSnapshot,
                firstDriverName,
                raceName
        );

        String secondResult = getDriverRaceResult(
                resultSnapshot,
                secondDriverName,
                raceName
        );

        if (isTripleDriver && drivers.size() > 2) {
            String thirdDriverName = drivers.get(2);

            String thirdResult = getDriverRaceResult(
                    resultSnapshot,
                    thirdDriverName,
                    raceName
            );

            datumTriple.add(new teamTripleDriversResultsData(
                    raceName,
                    firstResult,
                    firstDriverName,
                    secondResult,
                    secondDriverName,
                    thirdResult,
                    thirdDriverName,
                    season
            ));

            if (adapterTriple == null) {
                adapterTriple =
                        new teamTripleDriversResultsAdapter(
                                requireActivity(),
                                datumTriple
                        );

                recyclerView.setAdapter(adapterTriple);
            } else {
                adapterTriple.notifyDataSetChanged();
            }
        } else {
            datum.add(new teamDriversResultsData(
                    raceName,
                    firstResult,
                    firstDriverName,
                    secondResult,
                    secondDriverName,
                    season
            ));

            if (adapter == null) {
                adapter = new teamDriversResultsAdapter(
                        requireActivity(),
                        datum
                );

                recyclerView.setAdapter(adapter);
            } else {
                adapter.notifyDataSetChanged();
            }
        }

        loadedRaces[0]++;

        if (loadedRaces[0] >= totalRaces) {
            updateLayoutParams();
            hideShimmer();
        }
    }

    private String getDriverRaceResult(
            DataSnapshot resultSnapshot,
            String driverFullName,
            String raceName) {

        DataSnapshot raceResult = resultSnapshot
                .child(driverFullName)
                .child(raceName);

        String result = raceResult
                .child("Result")
                .getValue(String.class);

        String resultTeamId = raceResult
                .child("TeamId")
                .getValue(String.class);

        if ("N/C".equals(result)) {
            return "N/C";
        }

        if (result != null && teamId.equals(resultTeamId)) {
            return result;
        }

        return "null";
    }

    private void updateLayoutParams() {
        int marginEnd = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                10,
                requireContext().getResources().getDisplayMetrics()
        );

        thirdDriverLayout.setVisibility(
                isTripleDriver ? View.VISIBLE : View.GONE
        );

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        isTripleDriver ? 1.55f : 2.7f
                );

        params.gravity = Gravity.BOTTOM;
        params.setMargins(0, 0, marginEnd, 0);

        raceNameHeader.setLayoutParams(params);
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

            shimmerTripleFrameLayout.stopShimmer();
            shimmerTripleFrameLayout.setVisibility(View.GONE);
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

    private void showLoading(boolean tripleDrivers) {
        if (hideShimmerRunnable != null) {
            handler.removeCallbacks(hideShimmerRunnable);
            hideShimmerRunnable = null;
        }

        recyclerView.setVisibility(View.GONE);

        shimmerFrameLayout.stopShimmer();
        shimmerFrameLayout.setVisibility(View.GONE);

        shimmerTripleFrameLayout.stopShimmer();
        shimmerTripleFrameLayout.setVisibility(View.GONE);

        ShimmerFrameLayout activeShimmer = tripleDrivers
                ? shimmerTripleFrameLayout
                : shimmerFrameLayout;

        activeShimmer.setVisibility(View.VISIBLE);
        activeShimmer.startShimmer();
    }
}