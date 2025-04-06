package com.example.f1app;

import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

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

public class futureRaceFragment extends Fragment {
    List<futureRaceData> datum;
    private futureRaceAdapter adapter;
    private RecyclerView recyclerView;
    private ShimmerFrameLayout shimmerFrameLayout;

    public futureRaceFragment() {
        // required empty public constructor.
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        if (!getArguments().isEmpty()){
            return inflater.inflate(R.layout.future_race_fragment, container, false);
        }else{
            return inflater.inflate(R.layout.future_race_fragment_empty, container, false);
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (!getArguments().isEmpty()){
            shimmerFrameLayout = view.findViewById(R.id.shimmer_layout);
            shimmerFrameLayout.startShimmer();

            recyclerView = view.findViewById(R.id.recyclerview_futureRaces);

            LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getActivity().getApplicationContext());
            recyclerView.setLayoutManager(linearLayoutManager);
            LocalDate currentDate = LocalDate.now();
            String currentYear = Integer.toString(currentDate.getYear());
            ArrayList<String> futureRaceRoundNumber = getArguments().getStringArrayList("raceRound");

            for (int i = 0; i < futureRaceRoundNumber.size(); i++){
                String raceRound = futureRaceRoundNumber.get(i);

                DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference();
                rootRef.child("schedule/season/" + currentYear).orderByChild("round")
                        .equalTo(Integer.valueOf(raceRound)).addValueEventListener(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                datum = new ArrayList<>();
                                for(DataSnapshot ds : dataSnapshot.getChildren()) {
                                    String raceName = ds.child("Circuit/raceName").getValue(String.class);
                                    String dateStart = ds.child("FirstPractice/firstPracticeDate").getValue(String.class);
                                    String dateEnd = ds.child("raceDate").getValue(String.class);
                                    String circuitId = ds.child("Circuit/circuitId").getValue(String.class);

                                    rootRef.child("circuits/" + circuitId).addListenerForSingleValueEvent(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                            String circuitName = dataSnapshot.child("circuitName").getValue(String.class);
                                            String raceCountry = dataSnapshot.child("country").getValue(String.class);
                                            String raceLocation = dataSnapshot.child("location").getValue(String.class);
                                            futureRaceData futureRaceData = new futureRaceData(raceName, dateStart, dateEnd,
                                                    circuitName, raceRound, raceCountry, circuitId);
                                            futureRaceData.setLocality(raceLocation);
                                            datum.add(futureRaceData);
                                            Handler handler = new Handler();
                                            handler.postDelayed(()->{
                                                recyclerView.setVisibility(View.VISIBLE);
                                                shimmerFrameLayout.setVisibility(View.GONE);
                                                shimmerFrameLayout.stopShimmer();
                                            },800);
                                            adapter = new futureRaceAdapter(getActivity(), datum);
                                            recyclerView.setAdapter(adapter);
                                        }
                                        @Override
                                        public void onCancelled(@NonNull DatabaseError error) {
                                            Log.e("futureRaceFragmentFirebaseError", error.getMessage());
                                        }
                                    });
                                }

                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {
                                Log.e("futureRaceFragmentFirebaseError", error.getMessage());
                            }
                        });
            }
        }else{
            LockableNestedScrollView scrollView = view.findViewById(R.id.scrollView);

            scrollView.setScrollingEnabled(false);

        }
    }
}
