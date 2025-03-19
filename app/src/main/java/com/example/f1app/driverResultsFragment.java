package com.example.f1app;

import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.facebook.shimmer.ShimmerFrameLayout;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class driverResultsFragment extends Fragment {
    private driverResultsAdapter adapter;
    private RecyclerView recyclerView;
    private List<driverResultsData> datum;
    private CheckBox radioButton_2025, radioButton_2024;
    private ShimmerFrameLayout shimmerFrameLayout;
    private ScrollView scrollView;

    public driverResultsFragment() {
        // required empty public constructor.
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    public void onResume(){
        super.onResume();
        scrollView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                scrollView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                scrollView.smoothScrollTo(0, 0);
            }
        });
        getView().requestLayout();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.driver_page_results_fragment, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        scrollView = (ScrollView) view.findViewById(R.id.scrollView);
        radioButton_2025 = (CheckBox) view.findViewById(R.id.radioButton_2025);
        radioButton_2024 = (CheckBox) view.findViewById(R.id.radioButton_2024);

        shimmerFrameLayout = view.findViewById(R.id.shimmer_layout);

        recyclerView = view.findViewById(R.id.driver_results);
        LinearLayoutManager mLayoutManager = new LinearLayoutManager(getActivity());
        recyclerView.setLayoutManager(mLayoutManager);

        if (!getArguments().isEmpty()) {
            String mDriverTeam = getArguments().getString("driverTeam");
            String mDriverName = getArguments().getString("driverName");
            String mDriverCode = getArguments().getString("driverCode");
            String mDriverFamilyName = getArguments().getString("driverFamilyName");

            shimmerFrameLayout.startShimmer();
            radioButton_2025.setChecked(true);
            getResults("2025", mDriverName, mDriverFamilyName);

            radioButton_2025.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (!radioButton_2025.isChecked()){
                        radioButton_2025.setChecked(true);
                        radioButton_2024.setChecked(false);
                    }
                    radioButton_2024.setChecked(false);
                }
            });

            radioButton_2024.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (!radioButton_2024.isChecked()){
                        radioButton_2025.setChecked(false);
                        radioButton_2024.setChecked(true);
                    }
                    radioButton_2025.setChecked(false);
                }
            });

            radioButton_2025.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                    if (radioButton_2025.isChecked()){
                        recyclerView.setVisibility(View.GONE);
                        shimmerFrameLayout.setVisibility(View.VISIBLE);
                        shimmerFrameLayout.startShimmer();
                        getResults("2025", mDriverName, mDriverFamilyName);
                    }
                }
            });

            radioButton_2024.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                    if (radioButton_2024.isChecked()){
                        recyclerView.setVisibility(View.GONE);
                        shimmerFrameLayout.setVisibility(View.VISIBLE);
                        shimmerFrameLayout.startShimmer();
                        getResults("2024", mDriverName, mDriverFamilyName);
                    }
                }
            });
        }
    }

    private void getResults(String season, String driverName, String driverFamilyName){
        String fullDriverName = driverName + " " + driverFamilyName;
        datum = new ArrayList<>();
        DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference();
        rootRef.child("schedule/season/" + season + "/").orderByChild("round").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot ds : snapshot.getChildren()) {
                    String raceName = ds.getKey();
                    rootRef.child("results/season/" + season).addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            if (snapshot.hasChild(fullDriverName)){
                                String driverResult = snapshot.child(fullDriverName)
                                        .child(raceName).getValue(String.class);
                                driverResultsData results = new driverResultsData(raceName,
                                        driverResult, fullDriverName, Integer.parseInt(season));
                                datum.add(results);
                                Handler handler = new Handler();
                                handler.postDelayed(()->{
                                    recyclerView.setVisibility(View.VISIBLE);
                                    shimmerFrameLayout.setVisibility(View.GONE);
                                    shimmerFrameLayout.stopShimmer();
                                },500);
                                adapter = new driverResultsAdapter(getActivity(), datum);
                                recyclerView.setAdapter(adapter);
                            }
                            else{
                                String driverResult = getResources().getString(R.string.np_text);
                                driverResultsData results = new driverResultsData(raceName,
                                        driverResult, fullDriverName, Integer.parseInt(season));
                                datum.add(results);
                                Handler handler = new Handler();
                                handler.postDelayed(()->{
                                    recyclerView.setVisibility(View.VISIBLE);
                                    shimmerFrameLayout.setVisibility(View.GONE);
                                    shimmerFrameLayout.stopShimmer();
                                },500);
                                adapter = new driverResultsAdapter(getActivity(), datum);
                                recyclerView.setAdapter(adapter);
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            Log.e("error", "" + error);
                        }
                    });

                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("error", "" + error);
            }
        });
    }
}