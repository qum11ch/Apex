package com.example.f1app;

import static com.example.f1app.MainActivity.hideShimmer;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.facebook.shimmer.ShimmerFrameLayout;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class driverResultsFragment extends Fragment {

    private driverResultsAdapter adapter;
    private final List<driverResultsData> datum = new ArrayList<>();

    private RecyclerView recyclerView;
    private ShimmerFrameLayout shimmerFrameLayout;
    private NestedScrollView scrollView;
    private LinearLayout seasonButtonsLayout;
    private TextView noResultsText;

    private View resultsLayout;
    private View legendTitle;
    private View keyMapLayout;
    private View summaryCard;
    private LinearLayout summaryTop;
    private LinearLayout summaryRow;
    private TextView pointsValue;
    private TextView positionValue;

    private String driverFullName;
    private String driverName;
    private String driverFamilyName;
    private String driverCode;

    private int latestSeason;
    private int earliestSeason;

    private Integer selectedSeason;
    private RequestQueue queue;

    public driverResultsFragment() {
    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {

        return inflater.inflate(R.layout.driver_page_results_fragment, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        EdgeInsets.applyBottom(view.findViewById(R.id.scrollView));

        scrollView = view.findViewById(R.id.scrollView);
        shimmerFrameLayout = view.findViewById(R.id.shimmer_layout);
        noResultsText = view.findViewById(R.id.noResultsText);
        resultsLayout = view.findViewById(R.id.results_layout);
        legendTitle = view.findViewById(R.id.legend_title);
        keyMapLayout = view.findViewById(R.id.keyMapLayout);
        summaryCard = view.findViewById(R.id.summary_card);
        summaryTop = view.findViewById(R.id.summary_top);
        summaryRow = view.findViewById(R.id.summary_row);
        seasonButtonsLayout = view.findViewById(R.id.seasonButtonsLayout);
        recyclerView = view.findViewById(R.id.driver_results);

        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        adapter = new driverResultsAdapter(requireContext(), datum);
        recyclerView.setAdapter(adapter);

        buildSummary();
        resultsLegend.build(
                requireContext(),
                view.findViewById(R.id.legend_left),
                view.findViewById(R.id.legend_right));

        shimmerFrameLayout.startShimmer();

        Bundle arguments = getArguments();

        if (arguments == null) {
            showNoResults();
            return;
        }

        driverName = arguments.getString("driverName");
        driverFamilyName = arguments.getString("driverFamilyName");
        driverCode = arguments.getString("driverCode");

        latestSeason = arguments.getInt("latestSeason", 0);
        earliestSeason = arguments.getInt("earliestSeason", 2018);

        if (driverName == null || driverFamilyName == null || latestSeason < earliestSeason) {
            showNoResults();
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

        scrollView.getViewTreeObserver().addOnGlobalLayoutListener(
                new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        scrollView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                        scrollView.smoothScrollTo(0, 0);
                    }
                });

        if (getView() != null) {
            getView().requestLayout();
        }
    }

    @Override
    public void onDestroy() {
        if (queue != null) {
            queue.cancelAll(this);
        }

        super.onDestroy();
    }

    private void loadAvailableSeasons() {
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
                                showAvailableSeasons(availableSeasons);
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            if (!isAdded()) {
                                return;
                            }

                            Log.e("DriverResults", "Failed to check season " + season,
                                    error.toException());

                            completedChecks[0]++;

                            if (completedChecks[0] == totalChecks) {
                                showAvailableSeasons(availableSeasons);
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
            seasonButtonsLayout.addView(createSeasonButton(season));
        }

        selectSeason(availableSeasons.get(0));
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
        boolean seasonChanged = selectedSeason == null || selectedSeason != season;

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
        datum.clear();
        adapter.notifyDataSetChanged();

        recyclerView.setVisibility(View.GONE);

        shimmerFrameLayout.setVisibility(View.VISIBLE);
        shimmerFrameLayout.startShimmer();

        noResultsText.setVisibility(View.GONE);
        resultsLayout.setVisibility(View.VISIBLE);
        summaryCard.setVisibility(View.GONE);
        setLegendVisible(false);

        loadSeasonPoints(season);

        DatabaseReference root = FirebaseDatabase.getInstance().getReference();

        root.child("schedule")
                .child("season")
                .child(String.valueOf(season))
                .orderByChild("round")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot scheduleSnapshot) {
                        if (!isAdded() || selectedSeason == null || selectedSeason != season) {
                            return;
                        }

                        if (!scheduleSnapshot.exists()) {
                            hideShimmer(recyclerView, shimmerFrameLayout);
                            showNoResults();
                            return;
                        }

                        // One read for the whole season: the rows keep the schedule order.
                        root.child("results")
                                .child("season")
                                .child(String.valueOf(season))
                                .child(driverFullName)
                                .addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot resultSnapshot) {
                                        if (!isAdded() || selectedSeason == null
                                                || selectedSeason != season) {
                                            return;
                                        }

                                        ArrayList<driverResultsData> loaded = new ArrayList<>();

                                        for (DataSnapshot race : scheduleSnapshot.getChildren()) {
                                            String raceName = race.getKey();

                                            if (raceName == null) {
                                                continue;
                                            }

                                            String result = resultSnapshot
                                                    .child(raceName)
                                                    .child("Result")
                                                    .getValue(String.class);

                                            if (result == null || result.trim().isEmpty()) {
                                                result = "NP";
                                            }

                                            loaded.add(new driverResultsData(
                                                    raceName,
                                                    result,
                                                    driverFullName,
                                                    season,
                                                    readRound(race)));
                                        }

                                        showLoadedResults(loaded);
                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError error) {
                                        if (!isAdded()) {
                                            return;
                                        }

                                        Log.e("DriverResults", "Failed to load results",
                                                error.toException());

                                        hideShimmer(recyclerView, shimmerFrameLayout);
                                        showNoResults();
                                    }
                                });
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        if (!isAdded()) {
                            return;
                        }

                        Log.e("DriverResults", "Failed to load schedule", error.toException());

                        hideShimmer(recyclerView, shimmerFrameLayout);
                        showNoResults();
                    }
                });
    }

    private int readRound(DataSnapshot race) {
        Object value = race.child("round").getValue();

        if (value == null) {
            return 0;
        }

        try {
            return Integer.parseInt(String.valueOf(value).trim());
        } catch (NumberFormatException error) {
            return 0;
        }
    }

    private void showLoadedResults(ArrayList<driverResultsData> loaded) {
        if (!isAdded()) {
            return;
        }

        datum.clear();
        datum.addAll(loaded);

        adapter.notifyDataSetChanged();

        noResultsText.setVisibility(View.GONE);
        resultsLayout.setVisibility(View.VISIBLE);
        setLegendVisible(true);

        updateSummary(datum);

        hideShimmer(recyclerView, shimmerFrameLayout);
    }

    private void showNoResults() {
        if (!isAdded()) {
            return;
        }

        shimmerFrameLayout.stopShimmer();
        shimmerFrameLayout.setVisibility(View.GONE);

        recyclerView.setVisibility(View.GONE);

        summaryCard.setVisibility(View.GONE);
        resultsLayout.setVisibility(View.GONE);
        setLegendVisible(false);
        noResultsText.setVisibility(View.VISIBLE);
    }

    private void setLegendVisible(boolean visible) {
        int visibility = visible ? View.VISIBLE : View.GONE;

        legendTitle.setVisibility(visibility);
        keyMapLayout.setVisibility(visibility);
    }

    // ---------- summary ----------

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
                R.string.team_results_from_pole,
                R.string.team_results_retirements
        };

        for (int label : labels) {
            View tile = inflater.inflate(R.layout.item_team_results_summary, summaryRow, false);

            ((TextView) tile.findViewById(R.id.summary_label)).setText(label);

            summaryRow.addView(tile, new LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        }
    }

    /** Counted from the same codes the table shows. */
    private void updateSummary(List<driverResultsData> results) {
        int wins = 0;
        int podiums = 0;
        int poles = 0;
        int retirements = 0;

        for (driverResultsData item : results) {
            String code = item.getDriverResult();

            if (code == null) {
                continue;
            }

            String[] parts = code.trim().split("-");

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

        int[] values = {wins, podiums, poles, retirements};

        for (int i = 0; i < summaryRow.getChildCount() && i < values.length; i++) {
            ((TextView) summaryRow.getChildAt(i).findViewById(R.id.summary_value))
                    .setText(String.valueOf(values[i]));
        }

        summaryCard.setVisibility(results.isEmpty() ? View.GONE : View.VISIBLE);
    }

    /** Season points and the place in the drivers' standings (Jolpica, same source as the tables). */
    private void loadSeasonPoints(int season) {
        pointsValue.setText("\u2013");
        positionValue.setText("\u2013");

        if (queue == null) {
            queue = Volley.newRequestQueue(requireContext().getApplicationContext());
        }

        String url = "https://api.jolpi.ca/ergast/f1/" + season + "/driverstandings/?format=json";

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.GET,
                url,
                null,
                response -> {
                    if (!isAdded() || selectedSeason == null || selectedSeason != season) {
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

                        for (int i = 0; i < lists.length(); i++) {
                            JSONArray standings = lists.getJSONObject(i)
                                    .optJSONArray("DriverStandings");

                            if (standings == null) {
                                continue;
                            }

                            for (int j = 0; j < standings.length(); j++) {
                                JSONObject standing = standings.getJSONObject(j);
                                JSONObject driver = standing.optJSONObject("Driver");

                                if (driver != null && isThisDriver(driver)) {
                                    pointsValue.setText(standing.optString("points", "\u2013"));
                                    positionValue.setText(
                                            standing.optString("positionText", "\u2013"));
                                    return;
                                }
                            }
                        }
                    } catch (JSONException error) {
                        Log.w("DriverResults", "Unexpected standings response", error);
                    }
                },
                error -> Log.w("DriverResults", "Driver points are not available", error)
        );

        request.setTag(this);
        queue.add(request);
    }

    private boolean isThisDriver(JSONObject driver) {
        String code = driver.optString("code", "");

        if (driverCode != null && !driverCode.isEmpty() && !code.isEmpty()) {
            return driverCode.equalsIgnoreCase(code);
        }

        return driverFamilyName != null
                && driverFamilyName.equalsIgnoreCase(driver.optString("familyName", ""));
    }
}
