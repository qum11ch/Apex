package com.example.f1app;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
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

/** Races of one past season: cards or tiles, newest round first, filtered by the host's search text. */
public class pastRacesFragment extends Fragment {

    /** Implemented by the activity that owns the search field and the mode button. */
    interface Host {
        String currentQuery();

        int currentViewMode();
    }

    private static final String TAG = "pastRaces";
    private static final String ARG_SEASON = "season";

    private RecyclerView recyclerView;
    private ShimmerFrameLayout shimmer;
    private TextView message;
    private pastRacesAdapter adapter;
    private LinearLayoutManager listManager;
    private GridLayoutManager tilesManager;

    private int loadToken = 0;
    private boolean loaded = false;

    static pastRacesFragment newInstance(String season) {
        pastRacesFragment fragment = new pastRacesFragment();
        Bundle arguments = new Bundle();
        arguments.putString(ARG_SEASON, season);
        fragment.setArguments(arguments);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.past_races_fragment, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recyclerView = view.findViewById(R.id.recyclerview_pastRaces);
        shimmer = view.findViewById(R.id.shimmer_layout);
        message = view.findViewById(R.id.races_message);

        EdgeInsets.applyBottom(recyclerView);

        adapter = new pastRacesAdapter(requireActivity());
        listManager = new LinearLayoutManager(requireContext());
        tilesManager = new GridLayoutManager(requireContext(), 2);
        recyclerView.setLayoutManager(listManager);
        recyclerView.setAdapter(adapter);

        shimmer.startShimmer();
        loadSeason();
    }

    @Override
    public void onDestroyView() {
        loadToken++; // ignore answers that arrive after the view is gone

        if (shimmer != null) {
            shimmer.stopShimmer();
        }

        recyclerView = null;
        super.onDestroyView();
    }

    boolean canScrollUp() {
        return recyclerView != null && recyclerView.canScrollVertically(-1);
    }

    /** Reads the search text and the mode from the host again. */
    void applyFilters() {
        if (adapter == null || recyclerView == null) {
            return;
        }

        int mode = pastRacesAdapter.MODE_LIST;
        String query = "";

        if (getActivity() instanceof Host) {
            Host host = (Host) getActivity();
            mode = host.currentViewMode();
            query = host.currentQuery();
        }

        if (adapter.getMode() != mode) {
            boolean tiles = mode == pastRacesAdapter.MODE_TILES;
            recyclerView.setLayoutManager(tiles ? tilesManager : listManager);
            adapter.setMode(mode);
        }

        adapter.setQuery(query);

        if (loaded) {
            updateMessage();
            recyclerView.scrollToPosition(0);
        }
    }

    // ---------- loading ----------

    private void loadSeason() {
        final int token = ++loadToken;
        final String season = getArguments() != null ? getArguments().getString(ARG_SEASON) : null;

        if (season == null) {
            showError();
            return;
        }

        DatabaseReference root = FirebaseDatabase.getInstance().getReference();

        // The whole circuits node is small (about 35 entries); one read replaces a query per race.
        root.child("circuits").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot circuits) {
                if (token == loadToken && isAdded()) {
                    readRaces(root, season, circuits, token);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.w(TAG, "Circuits are not available", error.toException());

                if (token == loadToken && isAdded()) {
                    readRaces(root, season, null, token);
                }
            }
        });
    }

    private void readRaces(DatabaseReference root, String season, DataSnapshot circuits, int token) {
        // No orderByChild: it needs an index in the rules, and the rounds are sorted here anyway.
        root.child("schedule").child("season").child(season)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot races) {
                        if (token != loadToken || !isAdded()) {
                            return;
                        }

                        showRaces(buildItems(season, races, circuits));
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, "Schedule request failed", error.toException());

                        if (token == loadToken && isAdded()) {
                            showError();
                        }
                    }
                });
    }

    private List<pastRaceItem> buildItems(String season, DataSnapshot races, DataSnapshot circuits) {
        List<pastRaceItem> items = new ArrayList<>();

        for (DataSnapshot race : races.getChildren()) {
            Integer round = race.child("round").getValue(Integer.class);
            String winner = race.child("RaceResults/raceWinnerCode").getValue(String.class);
            Boolean canceledFlag = race.child("Canceled").getValue(Boolean.class);

            boolean played = winner != null && !winner.equals("N/A");

            // Only finished or cancelled races belong to a past season (same rule as before).
            if (round == null || (!played && canceledFlag == null)) {
                continue;
            }

            String raceDate = race.child("raceDate").getValue(String.class);
            String firstPractice = race.child("FirstPractice/firstPracticeDate").getValue(String.class);
            String circuitId = race.child("Circuit/circuitId").getValue(String.class);

            DataSnapshot circuit = circuits != null && circuitId != null ? circuits.child(circuitId) : null;

            concludedRacesData data = new concludedRacesData(
                    firstPractice != null ? firstPractice : raceDate,
                    raceDate,
                    race.child("Circuit/raceName").getValue(String.class),
                    String.valueOf(round),
                    circuit != null ? circuit.child("circuitName").getValue(String.class) : null,
                    circuit != null ? circuit.child("country").getValue(String.class) : null,
                    circuit != null ? circuit.child("location").getValue(String.class) : null,
                    pastRaceItem.nz(winner),
                    pastRaceItem.nz(race.child("RaceResults/raceSecondCode").getValue(String.class)),
                    pastRaceItem.nz(race.child("RaceResults/raceThirdCode").getValue(String.class)),
                    season
            );
            data.setCanceled(canceledFlag != null);

            items.add(pastRaceItem.create(requireContext(), data, circuitId));
        }

        // The latest round first; the order the database answers in does not matter.
        Collections.sort(items, (a, b) -> Integer.compare(b.round, a.round));
        return items;
    }

    // ---------- states ----------

    private void showRaces(List<pastRaceItem> items) {
        loaded = true;
        adapter.setItems(items);

        shimmer.stopShimmer();
        shimmer.setVisibility(View.GONE);
        recyclerView.setVisibility(View.VISIBLE);

        applyFilters();
        updateMessage();
    }

    private void updateMessage() {
        if (adapter.totalCount() == 0) {
            message.setText(R.string.ps_no_data);
            message.setVisibility(View.VISIBLE);
        } else if (adapter.getItemCount() == 0) {
            message.setText(R.string.ps_nothing_found);
            message.setVisibility(View.VISIBLE);
        } else {
            message.setVisibility(View.GONE);
        }
    }

    private void showError() {
        loaded = true;
        shimmer.stopShimmer();
        shimmer.setVisibility(View.GONE);
        recyclerView.setVisibility(View.GONE);
        message.setText(R.string.ps_load_error);
        message.setVisibility(View.VISIBLE);
    }
}
