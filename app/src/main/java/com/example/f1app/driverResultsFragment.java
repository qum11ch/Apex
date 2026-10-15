package com.example.f1app;

import static com.example.f1app.MainActivity.hideShimmer;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.ToggleButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.facebook.shimmer.ShimmerFrameLayout;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class driverResultsFragment extends Fragment {

    private driverResultsAdapter adapter;

    private RecyclerView recyclerView;
    private List<driverResultsData> datum;

    private ShimmerFrameLayout shimmerFrameLayout;
    private NestedScrollView scrollView;

    private LinearLayout seasonButtonsLayout;

    private String driverFullName;
    private String driverName;
    private String driverFamilyName;

    private int latestSeason;
    private int earliestSeason;

    private TextView noResultsText;

    private Integer selectedSeason;
    private RelativeLayout keyMapLayout;
    private RelativeLayout resultsLayout;

    public driverResultsFragment() {
    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {

        return inflater.inflate(
                R.layout.driver_page_results_fragment,
                container,
                false
        );
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {

        super.onViewCreated(view, savedInstanceState);

        scrollView = view.findViewById(R.id.scrollView);
        shimmerFrameLayout = view.findViewById(R.id.shimmer_layout);

        noResultsText = view.findViewById(R.id.noResultsText);
        keyMapLayout = view.findViewById(R.id.keyMapLayout);
        resultsLayout = view.findViewById(R.id.results_layout);

        recyclerView = view.findViewById(R.id.driver_results);

        seasonButtonsLayout = view.findViewById(
                R.id.seasonButtonsLayout
        );

        datum = new ArrayList<>();

        recyclerView.setLayoutManager(
                new LinearLayoutManager(requireContext())
        );

        adapter = new driverResultsAdapter(
                (Activity) requireContext(),
                datum
        );

        recyclerView.setAdapter(adapter);

        shimmerFrameLayout.startShimmer();

        Bundle arguments = getArguments();

        if (arguments == null) {
            showNoResults();
            return;
        }

        driverName = arguments.getString("driverName");
        driverFamilyName = arguments.getString("driverFamilyName");

        latestSeason = arguments.getInt("latestSeason", 0);
        earliestSeason = arguments.getInt("earliestSeason", 2018);

        if (driverName == null || driverFamilyName == null) {
            showNoResults();
            return;
        }

        driverFullName = driverName + " " + driverFamilyName;

        if (latestSeason < earliestSeason) {
            showNoResults();
            return;
        }

        loadAvailableSeasons();

        driverName = getArguments().getString("driverName");
        driverFamilyName = getArguments().getString("driverFamilyName");
        latestSeason = getArguments().getInt("latestSeason", 0);
        earliestSeason = getArguments().getInt("earliestSeason", 2018);

        if (driverName == null || driverFamilyName == null || latestSeason < earliestSeason) {
            hideShimmer(recyclerView, shimmerFrameLayout);
            return;
        }

        driverFullName = driverName + " " + driverFamilyName;
        loadAvailableSeasons();
    }

    @Override
    public void onResume() {
        super.onResume();

        if (scrollView == null) {
            return;
        }

        scrollView.getViewTreeObserver()
                .addOnGlobalLayoutListener(
                        new ViewTreeObserver.OnGlobalLayoutListener() {
                            @Override
                            public void onGlobalLayout() {
                                scrollView.getViewTreeObserver()
                                        .removeOnGlobalLayoutListener(
                                                this
                                        );

                                scrollView.smoothScrollTo(0, 0);
                            }
                        }
                );

        if (getView() != null) {
            getView().requestLayout();
        }
    }

    private void loadAvailableSeasons() {
        ArrayList<Integer> seasonCandidates = new ArrayList<>();

        for (int season = latestSeason; season >= earliestSeason; season--) {
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
                    .child(driverFullName)
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {

                            if (!isAdded()) {
                                return;
                            }

                            if (snapshot.exists()) {
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
                                    "DriverResults",
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

    private void showAvailableSeasons(ArrayList<Integer> availableSeasons) {
        if (!isAdded()) {
            return;
        }

        if (availableSeasons.isEmpty()) {
            hideShimmer(recyclerView, shimmerFrameLayout);
            showNoResults();
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
        boolean seasonChanged =
                selectedSeason == null
                        || selectedSeason.intValue() != season;

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
        datum.clear();
        adapter.notifyDataSetChanged();

        recyclerView.setVisibility(View.GONE);

        shimmerFrameLayout.setVisibility(View.VISIBLE);
        shimmerFrameLayout.startShimmer();

        noResultsText.setVisibility(View.GONE);
        resultsLayout.setVisibility(View.VISIBLE);
        keyMapLayout.setVisibility(View.GONE);

        DatabaseReference scheduleRef =
                FirebaseDatabase.getInstance()
                        .getReference()
                        .child("schedule")
                        .child("season")
                        .child(String.valueOf(season));

        DatabaseReference resultsRef =
                FirebaseDatabase.getInstance()
                        .getReference()
                        .child("results")
                        .child("season")
                        .child(String.valueOf(season))
                        .child(driverFullName);

        scheduleRef.orderByChild("round")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot scheduleSnapshot) {

                        if (!isAdded()) {
                            return;
                        }

                        if (!scheduleSnapshot.exists()) {
                            hideShimmer(
                                    recyclerView,
                                    shimmerFrameLayout
                            );

                            showNoResults();
                            return;
                        }

                        ArrayList<driverResultsData> loadedResults = new ArrayList<>();
                        final int totalRaces = (int) scheduleSnapshot.getChildrenCount();
                        final int[] completedRaces = {0};

                        for (DataSnapshot raceSnapshot : scheduleSnapshot.getChildren()) {
                            String raceName = raceSnapshot.getKey();

                            resultsRef.child(raceName)
                                    .addListenerForSingleValueEvent(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(
                                                @NonNull DataSnapshot resultSnapshot) {

                                            if (!isAdded()) {
                                                return;
                                            }

                                            String result;

                                            if (resultSnapshot.exists()) {
                                                result = resultSnapshot
                                                        .child("Result")
                                                        .getValue(String.class);
                                            } else {
                                                result = getString(
                                                        R.string.np_text
                                                );
                                            }

                                            loadedResults.add(
                                                    new driverResultsData(
                                                            raceName,
                                                            result,
                                                            driverFullName,
                                                            season
                                                    )
                                            );

                                            completedRaces[0]++;

                                            if (completedRaces[0] == totalRaces) {

                                                showLoadedResults(
                                                        loadedResults
                                                );
                                            }
                                        }

                                        @Override
                                        public void onCancelled(@NonNull DatabaseError error) {
                                            if (!isAdded()) {
                                                return;
                                            }

                                            Log.e(
                                                    "DriverResults",
                                                    "Failed to load result for "
                                                            + raceName,
                                                    error.toException()
                                            );

                                            loadedResults.add(
                                                    new driverResultsData(
                                                            raceName,
                                                            getString(
                                                                    R.string.np_text
                                                            ),
                                                            driverFullName,
                                                            season
                                                    )
                                            );

                                            completedRaces[0]++;

                                            if (completedRaces[0] == totalRaces) {
                                                showLoadedResults(
                                                        loadedResults
                                                );
                                            }
                                        }
                                    });
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        if (!isAdded()) {
                            return;
                        }

                        Log.e(
                                "DriverResults",
                                "Failed to load schedule",
                                error.toException()
                        );

                        hideShimmer(
                                recyclerView,
                                shimmerFrameLayout
                        );

                        showNoResults();
                    }
                });
    }

    private void showLoadedResults(ArrayList<driverResultsData> loadedResults) {
        if (!isAdded()) {
            return;
        }

        if (selectedSeason == null) {
            return;
        }

        boolean seasonMatches = false;

        for (driverResultsData item : loadedResults) {
            if (item.getSeason() != null
                    && item.getSeason().equals(selectedSeason)) {

                seasonMatches = true;
                break;
            }
        }

        if (!seasonMatches) {
            Log.w(
                    "DriverResults",
                    "Ignoring outdated result response for season "
                            + selectedSeason
            );

            return;
        }

        datum.clear();
        datum.addAll(loadedResults);

        adapter.notifyDataSetChanged();

        hideShimmer(recyclerView, shimmerFrameLayout);

        noResultsText.setVisibility(View.GONE);
        resultsLayout.setVisibility(View.VISIBLE);
        keyMapLayout.setVisibility(View.VISIBLE);

        hideShimmer(recyclerView, shimmerFrameLayout);
    }

    private void showNoResults() {
        if (!isAdded()) {
            return;
        }

        shimmerFrameLayout.stopShimmer();
        shimmerFrameLayout.setVisibility(View.GONE);

        recyclerView.setVisibility(View.GONE);

        keyMapLayout.setVisibility(View.GONE);
        resultsLayout.setVisibility(View.GONE);
        noResultsText.setVisibility(View.VISIBLE);
    }

}