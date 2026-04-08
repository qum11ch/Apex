package com.example.f1app;

import static com.example.f1app.MainActivity.checkLightTheme;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.facebook.shimmer.ShimmerFrameLayout;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

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

        if (!getArguments().isEmpty()) {
            shimmerFrameLayout = view.findViewById(R.id.shimmer_layout);
            shimmerFrameLayout.startShimmer();

            datum = new ArrayList<>();
            recyclerView = view.findViewById(R.id.recyclerview_concludedRaces);

            LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getActivity().getApplicationContext());
            linearLayoutManager.setReverseLayout(true);
            linearLayoutManager.setStackFromEnd(true);
            recyclerView.setLayoutManager(linearLayoutManager);

            Button seasonsResults_2024 = view.findViewById(R.id.seasonResults_2024);
            Button seasonsResults_2025 = view.findViewById(R.id.seasonResults_2025);
            String buttonText_2024, buttonText_2025;
            if (Locale.getDefault().getLanguage().equals("ru")) {
                buttonText_2024 = getText(R.string.past_season_result) + " 2024";
                buttonText_2025 = getText(R.string.past_season_result) + " 2025";
            } else {
                buttonText_2024 = "2024 " + getText(R.string.past_season_result);
                buttonText_2025 = "2025 " + getText(R.string.past_season_result);
            }

            String season = getArguments().getString("season");
            String parent = getArguments().getString("parent");
            //String mCurrentSeason = getArguments().getString("season");
            ArrayList<String> concludedRaceRoundNumber = getArguments().getStringArrayList("raceRound");
            adapterType = parent;

            if (parent.equals("schedule")) {
                seasonsResults_2024.setText(buttonText_2024);
                seasonsResults_2024.setOnClickListener(v -> {
                    Intent intent = new Intent(requireActivity(), pastSeasonScheduleActivity.class);
                    intent.putExtra("season", "2024");
                    startActivity(intent);
                    getActivity().overridePendingTransition(0, 0);
                });

                seasonsResults_2025.setText(buttonText_2025);
                seasonsResults_2025.setOnClickListener(v -> {
                    Intent intent = new Intent(requireActivity(), pastSeasonScheduleActivity.class);
                    intent.putExtra("season", "2025");
                    startActivity(intent);
                    getActivity().overridePendingTransition(0, 0);
                });

                concludedAdapter = new concludedRacesAdapter(getActivity(), datum);
                recyclerView.setAdapter(concludedAdapter);
            } else {
                seasonsResults_2024.setVisibility(View.GONE);
                seasonsResults_2025.setVisibility(View.GONE);
                pastSeasonAdapter = new pastSeasonsRacesAdapter(getActivity(), datum);
                recyclerView.setAdapter(pastSeasonAdapter);
            }
            totalItemsToLoad = concludedRaceRoundNumber.size();
            loadedItemsCount = 0;

            for (int i = 0; i < concludedRaceRoundNumber.size(); i++) {
                String raceRound = concludedRaceRoundNumber.get(i);

                DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference();
                rootRef.child("schedule/season/" + season)
                        .orderByChild("round")
                        .equalTo(Integer.parseInt(raceRound))
                        .addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                for (DataSnapshot ds : snapshot.getChildren()) {
                                    String winnerCode = ds.child("RaceResults/raceWinnerCode").getValue(String.class);
                                    Boolean isCanceled = ds.child("Canceled").getValue(Boolean.class);
                                    if ((winnerCode != null && !winnerCode.equals("N/A")) || isCanceled != null) {
                                        String raceName = ds.child("Circuit/raceName").getValue(String.class);
                                        String dateStart = ds.child("FirstPractice/firstPracticeDate").getValue(String.class);
                                        String dateEnd = ds.child("raceDate").getValue(String.class);
                                        String circuitId = ds.child("Circuit/circuitId").getValue(String.class);
                                        String secondCode = ds.child("RaceResults/raceSecondCode").getValue(String.class);
                                        String thirdCode = ds.child("RaceResults/raceThirdCode").getValue(String.class);

                                        rootRef.child("circuits/" + circuitId)
                                                .addListenerForSingleValueEvent(new ValueEventListener() {
                                                    @Override
                                                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                                        String circuitName = dataSnapshot.child("circuitName").getValue(String.class);
                                                        String raceCountry = dataSnapshot.child("country").getValue(String.class);
                                                        String raceLocation = dataSnapshot.child("location").getValue(String.class);

                                                        concludedRacesData concludedRace = new concludedRacesData(
                                                                dateStart, dateEnd, raceName, raceRound,
                                                                circuitName, raceCountry, raceLocation,
                                                                winnerCode, secondCode, thirdCode, season
                                                        );
                                                        concludedRace.setCanceled(isCanceled != null);

                                                        datum.add(concludedRace);
                                                        if (adapterType.equals("schedule")) {
                                                            concludedAdapter.notifyItemInserted(datum.size() - 1);
                                                        } else {
                                                            pastSeasonAdapter.notifyItemInserted(datum.size() - 1);
                                                        }
                                                        loadedItemsCount++;
                                                        if (loadedItemsCount >= totalItemsToLoad) {
                                                            hideShimmer();
                                                        }
                                                    }

                                                    @Override
                                                    public void onCancelled(@NonNull DatabaseError error) {
                                                        Log.e("concludedRaceFragmentFirebaseError", error.getMessage());
                                                    }
                                                });
                                    } else {
                                        loadedItemsCount++;
                                        if (loadedItemsCount >= totalItemsToLoad) {
                                            hideShimmer();
                                        }
                                    }
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {
                                Log.e("concludedRaceFragmentFirebaseError", error.getMessage());
                                loadedItemsCount++;
                                if (loadedItemsCount >= totalItemsToLoad) {
                                    hideShimmer();
                                }
                            }
                        });
            }
        } else {
            ShapeableImageView stripedImage = view.findViewById(R.id.stripedImage);
            if (!checkLightTheme(requireContext())){
                stripedImage.setBackground(ContextCompat.getDrawable(requireContext(), R.drawable.background_striped_lines_item_night));
            }

            LockableNestedScrollView scrollView = view.findViewById(R.id.scrollView);
            Button seasonsResults_2024 = view.findViewById(R.id.seasonResults_2024);
            Button seasonsResults_2025 = view.findViewById(R.id.seasonResults_2025);

            String buttonText_2024, buttonText_2025;
            if (Locale.getDefault().getLanguage().equals("ru")) {
                buttonText_2024 = getText(R.string.past_season_result) + " 2024";
                buttonText_2025 = getText(R.string.past_season_result) + " 2025";
            } else {
                buttonText_2024 = "2024 " + getText(R.string.past_season_result);
                buttonText_2025 = "2025 " + getText(R.string.past_season_result);
            }

            seasonsResults_2024.setText(buttonText_2024);
            seasonsResults_2024.setOnClickListener(v -> {
                Intent intent = new Intent(requireActivity(), pastSeasonScheduleActivity.class);
                intent.putExtra("season", "2024");
                startActivity(intent);
                getActivity().overridePendingTransition(0, 0);
            });

            seasonsResults_2025.setText(buttonText_2025);
            seasonsResults_2025.setOnClickListener(v -> {
                Intent intent = new Intent(requireActivity(), pastSeasonScheduleActivity.class);
                intent.putExtra("season", "2025");
                startActivity(intent);
                getActivity().overridePendingTransition(0, 0);
            });

            scrollView.setScrollingEnabled(false);
        }
    }

    private void hideShimmer() {
        if (hideShimmerRunnable != null) {
            handler.removeCallbacks(hideShimmerRunnable);
        }

        hideShimmerRunnable = () -> {
            if (isAdded()) {
                recyclerView.setVisibility(View.VISIBLE);
                shimmerFrameLayout.setVisibility(View.GONE);
                shimmerFrameLayout.stopShimmer();
            }
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
}