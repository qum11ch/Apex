package com.example.f1app;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.facebook.shimmer.ShimmerFrameLayout;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class concludedRaceFragment extends Fragment {
    private List<concludedRacesData> datum;
    private RecyclerView recyclerView;
    private ShimmerFrameLayout shimmerFrameLayout;
    private Handler handler = new Handler(Looper.getMainLooper());
    private Runnable hideShimmerRunnable;
    private int loadedItemsCount = 0;
    private int totalItemsToLoad = 0;
    private concludedRacesAdapter concludedAdapter;
    private pastSeasonsRacesAdapter pastSeasonAdapter;
    private String adapterType;
    private int loadGeneration = 0;

    private final java.util.Set<Integer> finishedRequests =
            new java.util.HashSet<>();

    public concludedRaceFragment() {
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        if (!getArguments().isEmpty()) {
            return inflater.inflate(R.layout.concluded_race_fragment, container, false);
        } else {
            return inflater.inflate(R.layout.concluded_race_fragment_empty, container, false);
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        final int generation = ++loadGeneration;
        finishedRequests.clear();
        setupFooterClearance(view);

        LockableNestedScrollView scrollView = view.findViewById(R.id.scrollView);

        if (scrollView != null) {
            scrollView.setScrollingEnabled(true);
        }

        if (!getArguments().isEmpty()) {
            shimmerFrameLayout = view.findViewById(R.id.shimmer_layout);
            shimmerFrameLayout.startShimmer();

            datum = new cachedList<>("concluded_races|" + getArguments().getString("parent") + "|"
                    + getArguments().getString("season") + "|" + getArguments().getStringArrayList("raceRound"));
            recyclerView = view.findViewById(R.id.recyclerview_concludedRaces);

            LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getActivity().getApplicationContext());
            linearLayoutManager.setReverseLayout(true);
            linearLayoutManager.setStackFromEnd(true);
            recyclerView.setLayoutManager(linearLayoutManager);

            String season = getArguments().getString("season");
            String parent = getArguments().getString("parent");
            //String mCurrentSeason = getArguments().getString("season");
            ArrayList<String> concludedRaceRoundNumber = getArguments().getStringArrayList("raceRound");
            adapterType = parent;

            if ("schedule".equals(adapterType)) {
                concludedAdapter = new concludedRacesAdapter(
                        requireActivity(), datum);

                pastSeasonAdapter = null;
                recyclerView.setAdapter(concludedAdapter);
            } else {
                pastSeasonAdapter = new pastSeasonsRacesAdapter(
                        requireActivity(), datum);

                concludedAdapter = null;
                recyclerView.setAdapter(pastSeasonAdapter);
            }

            cachedList.restore(
                    datum,
                    recyclerView.getAdapter(),
                    recyclerView,
                    shimmerFrameLayout
            );

            totalItemsToLoad = concludedRaceRoundNumber != null
                    ? concludedRaceRoundNumber.size()
                    : 0;

            loadedItemsCount = 0;

            if (totalItemsToLoad == 0) {
                hideShimmer();
                return;
            }

            DatabaseReference rootRef =
                    FirebaseDatabase.getInstance().getReference();

            for (int i = 0; i < concludedRaceRoundNumber.size(); i++) {
                final int requestId = i;
                final String raceRound = concludedRaceRoundNumber.get(i);
                final int round;

                try {
                    round = Integer.parseInt(raceRound);
                } catch (RuntimeException error) {
                    Log.w("concludedRaceFragment",
                            "Invalid round: " + raceRound, error);

                    finishRequest(generation, requestId);
                    continue;
                }

                rootRef.child("schedule")
                        .child("season")
                        .child(season)
                        .orderByChild("round")
                        .equalTo(round)
                        .limitToFirst(1)
                        .addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(
                                    @NonNull DataSnapshot snapshot
                            ) {
                                if (!isLoadActive(generation)) {
                                    return;
                                }

                                if (!snapshot.hasChildren()) {
                                    finishRequest(generation, requestId);
                                    return;
                                }

                                DataSnapshot race =
                                        snapshot.getChildren().iterator().next();

                                final String winnerCode = race
                                        .child("RaceResults/raceWinnerCode")
                                        .getValue(String.class);

                                final boolean canceled = Boolean.TRUE.equals(
                                        race.child("Canceled")
                                                .getValue(Boolean.class));

                                boolean hasWinner = winnerCode != null
                                        && !winnerCode.trim().isEmpty()
                                        && !"N/A".equalsIgnoreCase(
                                        winnerCode.trim());

                                if (!hasWinner && !canceled) {
                                    finishRequest(generation, requestId);
                                    return;
                                }

                                final String raceName = race
                                        .child("Circuit/raceName")
                                        .getValue(String.class);

                                final String dateStart = race
                                        .child("FirstPractice/firstPracticeDate")
                                        .getValue(String.class);

                                final String dateEnd = race
                                        .child("raceDate")
                                        .getValue(String.class);

                                final String circuitId = race
                                        .child("Circuit/circuitId")
                                        .getValue(String.class);

                                final String secondCode = race
                                        .child("RaceResults/raceSecondCode")
                                        .getValue(String.class);

                                final String thirdCode = race
                                        .child("RaceResults/raceThirdCode")
                                        .getValue(String.class);

                                if (circuitId == null
                                        || circuitId.trim().isEmpty()) {
                                    Log.w("concludedRaceFragment",
                                            "Missing circuit for round "
                                                    + raceRound);

                                    finishRequest(generation, requestId);
                                    return;
                                }

                                rootRef.child("circuits")
                                        .child(circuitId)
                                        .addListenerForSingleValueEvent(
                                                new ValueEventListener() {
                                                    @Override
                                                    public void onDataChange(
                                                            @NonNull DataSnapshot circuit
                                                    ) {
                                                        if (!isLoadActive(generation)) {
                                                            return;
                                                        }

                                                        if (!circuit.exists()) {
                                                            finishRequest(
                                                                    generation,
                                                                    requestId);
                                                            return;
                                                        }

                                                        String circuitName = circuit
                                                                .child("circuitName")
                                                                .getValue(String.class);

                                                        String country = circuit
                                                                .child("country")
                                                                .getValue(String.class);

                                                        String location = circuit
                                                                .child("location")
                                                                .getValue(String.class);

                                                        concludedRacesData result =
                                                                new concludedRacesData(
                                                                        dateStart,
                                                                        dateEnd,
                                                                        raceName,
                                                                        raceRound,
                                                                        circuitName,
                                                                        country,
                                                                        location,
                                                                        winnerCode,
                                                                        secondCode,
                                                                        thirdCode,
                                                                        season);

                                                        result.setCanceled(canceled);
                                                        datum.add(result);

                                                        RecyclerView.Adapter<?> adapter =
                                                                recyclerView.getAdapter();

                                                        if (adapter != null) {
                                                            adapter.notifyDataSetChanged();
                                                        }

                                                        finishRequest(
                                                                generation, requestId);
                                                    }

                                                    @Override
                                                    public void onCancelled(
                                                            @NonNull DatabaseError error
                                                    ) {
                                                        Log.w("concludedRaceFragment",
                                                                "Circuit load failed",
                                                                error.toException());

                                                        finishRequest(
                                                                generation, requestId);
                                                    }
                                                });
                            }

                            @Override
                            public void onCancelled(
                                    @NonNull DatabaseError error
                            ) {
                                Log.w("concludedRaceFragment",
                                        "Round load failed",
                                        error.toException());

                                finishRequest(generation, requestId);
                            }
                        });
            }
        } else {
            TextView caption = view.findViewById(R.id.emptyCaption);
            caption.setText(getString(R.string.empty_caption, String.valueOf(LocalDate.now().getYear())));

            view.findViewById(R.id.emptyPastSeasons).setOnClickListener(v -> {
                Intent intent = new Intent(requireActivity(), pastSeasonScheduleActivity.class);
                intent.putExtra("currentSeason", requireActivity().getIntent().getStringExtra("currentSeason"));
                startActivity(intent);
                requireActivity().overridePendingTransition(0, 0);
            });
        }
    }

    private boolean isLoadActive(int generation) {
        return generation == loadGeneration
                && isAdded()
                && getView() != null
                && recyclerView != null
                && shimmerFrameLayout != null;
    }

    private void finishRequest(int generation, int requestId) {
        if (!isLoadActive(generation)
                || !finishedRequests.add(requestId)) {
            return;
        }

        loadedItemsCount++;

        if (loadedItemsCount >= totalItemsToLoad) {
            hideShimmer();
        }
    }

    private void setupFooterClearance(View pageRoot) {
        View content = pageRoot.findViewById(R.id.schedule_page_content);
        View footer = requireActivity().findViewById(R.id.footer_container);

        if (content == null) {
            return;
        }

        if (footer == null) {
            // Экран без футера (прошлые сезоны): только системный отступ
            EdgeInsets.applyBottom(content);
            return;
        }

        final int left = content.getPaddingLeft();
        final int top = content.getPaddingTop();
        final int right = content.getPaddingRight();

        final int extraGap = Math.round(
                12 * getResources().getDisplayMetrics().density
        );

        Runnable updatePadding = () -> {
            ViewGroup.LayoutParams layoutParams = footer.getLayoutParams();

            int bottomMargin = 0;

            if (layoutParams instanceof ViewGroup.MarginLayoutParams) {
                bottomMargin =
                        ((ViewGroup.MarginLayoutParams) layoutParams).bottomMargin;
            }

            int bottomPadding = footer.getHeight() + bottomMargin + extraGap;

            if (content.getPaddingBottom() != bottomPadding) {
                content.setPadding(left, top, right, bottomPadding);
            }
        };

        View.OnLayoutChangeListener listener =
                (view, l, t, r, b, oldL, oldT, oldR, oldB) ->
                        updatePadding.run();

        footer.addOnLayoutChangeListener(listener);

        pageRoot.addOnAttachStateChangeListener(
                new View.OnAttachStateChangeListener() {
                    @Override
                    public void onViewAttachedToWindow(View view) {
                        footer.removeOnLayoutChangeListener(listener);
                        footer.addOnLayoutChangeListener(listener);
                        view.post(updatePadding);
                    }

                    @Override
                    public void onViewDetachedFromWindow(View view) {
                        footer.removeOnLayoutChangeListener(listener);
                    }
                }
        );

        pageRoot.post(updatePadding);
    }

    private void hideShimmer() {
        final int generation = loadGeneration;

        if (!isLoadActive(generation)) {
            return;
        }

        cachedList.finish(datum);

        if (hideShimmerRunnable != null) {
            handler.removeCallbacks(hideShimmerRunnable);
        }

        hideShimmerRunnable = () -> {
            if (!isLoadActive(generation)) {
                return;
            }

            recyclerView.setVisibility(View.VISIBLE);
            shimmerFrameLayout.stopShimmer();
            shimmerFrameLayout.setVisibility(View.GONE);
        };

        handler.postDelayed(hideShimmerRunnable, 500);
    }

    @Override
    public void onDestroyView() {
        ++loadGeneration;

        if (hideShimmerRunnable != null) {
            handler.removeCallbacks(hideShimmerRunnable);
            hideShimmerRunnable = null;
        }

        if (shimmerFrameLayout != null) {
            shimmerFrameLayout.stopShimmer();
        }

        if (recyclerView != null) {
            recyclerView.setAdapter(null);
        }

        concludedAdapter = null;
        pastSeasonAdapter = null;
        recyclerView = null;
        shimmerFrameLayout = null;
        finishedRequests.clear();

        super.onDestroyView();
    }
}