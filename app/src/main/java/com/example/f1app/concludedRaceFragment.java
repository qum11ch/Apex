package com.example.f1app;

import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ScrollView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class concludedRaceFragment extends Fragment {
    List<concludedRacesData> datum;
    private concludedRacesAdapter adapter;
    private RecyclerView recyclerView;
    private ShimmerFrameLayout shimmerFrameLayout;

    public concludedRaceFragment() {

    }
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        if (!getArguments().isEmpty()){
            return inflater.inflate(R.layout.concluded_race_fragment, container, false);
        }else{
            return inflater.inflate(R.layout.concluded_race_fragment_empty, container, false);
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (!getArguments().isEmpty()){
            shimmerFrameLayout = view.findViewById(R.id.shimmer_layout);
            shimmerFrameLayout.startShimmer();
            datum = new ArrayList<>();
            recyclerView = view.findViewById(R.id.recyclerview_concludedRaces);
            LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getActivity().getApplicationContext());
            linearLayoutManager.setReverseLayout(true);
            linearLayoutManager.setStackFromEnd(true);
            recyclerView.setLayoutManager(linearLayoutManager);
            LocalDate currentDate = LocalDate.now();
            String currentYear = Integer.toString(currentDate.getYear());

            ArrayList<String> concludedRaceRoundNumber = getArguments().getStringArrayList("raceRound");

            for (int i = 0; i < concludedRaceRoundNumber.size(); i++) {
                String raceRound = concludedRaceRoundNumber.get(i);

                DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference();
                rootRef.child("schedule/season/" + currentYear).orderByChild("round").equalTo(Integer.valueOf(raceRound)).addValueEventListener(new ValueEventListener() {
                //rootRef.child("schedule/season/" + "2024").orderByChild("round").equalTo(Integer.valueOf(raceRound)).addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        datum = new ArrayList<>();
                        for(DataSnapshot ds : snapshot.getChildren()) {
                            String winnerCode = ds.child("RaceResults/raceWinnerCode").getValue(String.class);
                            if (winnerCode.equals("N/A")){
                                continue;
                            }else{
                                String raceName = ds.child("Circuit/raceName").getValue(String.class);
                                String dateStart = ds.child("FirstPractice/firstPracticeDate").getValue(String.class);
                                String dateEnd = ds.child("raceDate").getValue(String.class);
                                String circuitId = ds.child("Circuit/circuitId").getValue(String.class);
                                String secondCode = ds.child("RaceResults/raceSecondCode").getValue(String.class);
                                String thirdCode = ds.child("RaceResults/raceThirdCode").getValue(String.class);
                                rootRef.child("circuits/" + circuitId).addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                        String circuitName = dataSnapshot.child("circuitName").getValue(String.class);
                                        String raceCountry = dataSnapshot.child("country").getValue(String.class);
                                        String raceLocation = dataSnapshot.child("location").getValue(String.class);

                                        concludedRacesData concludedRace = new concludedRacesData(dateStart,
                                                dateEnd, raceName, raceRound, circuitName, raceCountry, raceLocation, winnerCode, secondCode,
                                                thirdCode);
                                        datum.add(concludedRace);
                                        Handler handler = new Handler();
                                        handler.postDelayed(()->{
                                            recyclerView.setVisibility(View.VISIBLE);
                                            shimmerFrameLayout.setVisibility(View.GONE);
                                            shimmerFrameLayout.stopShimmer();
                                        },500);
                                        adapter = new concludedRacesAdapter(getActivity(), datum);
                                        recyclerView.setAdapter(adapter);
                                    }
                                    @Override
                                    public void onCancelled(@NonNull DatabaseError error) {
                                        Log.e("concludedRaceFragmentFirebaseError", error.getMessage());
                                    }
                                });
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e("concludedRaceFragmentFirebaseError", error.getMessage());
                    }
                });
            }
        }
        else{
            LockableNestedScrollView scrollView = view.findViewById(R.id.scrollView);

            scrollView.setScrollingEnabled(false);

        }

    }


}
