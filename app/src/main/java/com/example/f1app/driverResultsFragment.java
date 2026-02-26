package com.example.f1app;

import static com.example.f1app.MainActivity.hideShimmer;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.CheckBox;

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

import java.util.ArrayList;
import java.util.List;

public class driverResultsFragment extends Fragment {
    private driverResultsAdapter adapter;
    private RecyclerView recyclerView;
    private List<driverResultsData> datum;
    private CheckBox radioButton_2025, radioButton_2024, radioButton_2026;
    private ShimmerFrameLayout shimmerFrameLayout;
    private NestedScrollView scrollView;
    private boolean isInitialLoad = false;

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

        scrollView = view.findViewById(R.id.scrollView);
        radioButton_2025 = view.findViewById(R.id.radioButton_2025);
        radioButton_2024 = view.findViewById(R.id.radioButton_2024);
        radioButton_2026 = view.findViewById(R.id.radioButton_2026);
        shimmerFrameLayout = view.findViewById(R.id.shimmer_layout);
        recyclerView = view.findViewById(R.id.driver_results);

        datum = new ArrayList<>();

        LinearLayoutManager mLayoutManager = new LinearLayoutManager(getActivity());
        recyclerView.setLayoutManager(mLayoutManager);
        adapter = new driverResultsAdapter(getActivity(), datum);
        recyclerView.setAdapter(adapter);

        shimmerFrameLayout.startShimmer();

        if (!getArguments().isEmpty()) {
            String mDriverName = getArguments().getString("driverName");
            String mDriverFamilyName = getArguments().getString("driverFamilyName");

            DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference();
            rootRef.child("drivers").child(mDriverName + " " + mDriverFamilyName).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    String mLastEntry = snapshot.child("lastEntry").getValue(String.class);
                    String mFirstEntry = snapshot.child("firstEntry").getValue(String.class);
                    String status = snapshot.child("status").getValue(String.class);

                    String[] mLastGPparse = mLastEntry.split("\\s+");
                    String mLastSeason = mLastGPparse[0];
                    String[] mFirstGPparse = mFirstEntry.split("\\s+");
                    String mFirstSeason = mFirstGPparse[0];


                    switch(status){
                        case "retired":
                        case "reserve":
                            if (Integer.parseInt(mFirstSeason) > 2024 || Integer.parseInt(mLastSeason) < 2024){
                                radioButton_2024.setVisibility(View.GONE);
                            }
                            if (Integer.parseInt(mFirstSeason) > 2025 || Integer.parseInt(mLastSeason) < 2025) {
                                radioButton_2025.setVisibility(View.GONE);
                            }
                            if (Integer.parseInt(mFirstSeason) > 2026 || Integer.parseInt(mLastSeason) < 2026){
                                radioButton_2026.setVisibility(View.GONE);
                            }
                            switch (mLastSeason){
                                case "2025":
                                    radioButton_2025.setChecked(true);
                                    getResults("2025", mDriverName, mDriverFamilyName);
                                    break;
                                case "2024":
                                    radioButton_2024.setChecked(true);
                                    getResults("2024", mDriverName, mDriverFamilyName);
                                    break;
                            }
                            break;
                        default:
                            if (Integer.parseInt(mFirstSeason) > 2024){
                                radioButton_2024.setVisibility(View.GONE);
                            }
                            if (Integer.parseInt(mFirstSeason) > 2025) {
                                radioButton_2025.setVisibility(View.GONE);
                            }
                            if (Integer.parseInt(mFirstSeason) > 2026){
                                radioButton_2026.setVisibility(View.GONE);
                            }
                            radioButton_2026.setChecked(true);
                            getResults("2026", mDriverName, mDriverFamilyName);
                            break;
                    }

                    radioButton_2025.setOnClickListener(view1 -> {
                        if (!radioButton_2025.isChecked()) {
                            radioButton_2026.setChecked(false);
                            radioButton_2025.setChecked(true);
                            radioButton_2024.setChecked(false);
                        }
                        radioButton_2024.setChecked(false);
                        radioButton_2026.setChecked(false);
                    });

                    radioButton_2026.setOnClickListener(view2 -> {
                        if (!radioButton_2026.isChecked()) {
                            radioButton_2026.setChecked(true);
                            radioButton_2025.setChecked(false);
                            radioButton_2024.setChecked(false);
                        }
                        radioButton_2024.setChecked(false);
                        radioButton_2025.setChecked(false);
                    });

                    radioButton_2024.setOnClickListener(view3 -> {
                        if (!radioButton_2024.isChecked()) {
                            radioButton_2025.setChecked(false);
                            radioButton_2026.setChecked(false);
                            radioButton_2024.setChecked(true);
                        }
                        radioButton_2025.setChecked(false);
                        radioButton_2026.setChecked(false);
                    });

                    radioButton_2026.setOnCheckedChangeListener((compoundButton, b) -> {
                        if (radioButton_2026.isChecked()) {
                            recyclerView.setVisibility(View.GONE);
                            shimmerFrameLayout.setVisibility(View.VISIBLE);
                            shimmerFrameLayout.startShimmer();
                            getResults("2026", mDriverName, mDriverFamilyName);
                        }
                    });

                    radioButton_2025.setOnCheckedChangeListener((compoundButton, b) -> {
                        if (radioButton_2025.isChecked()) {
                            recyclerView.setVisibility(View.GONE);
                            shimmerFrameLayout.setVisibility(View.VISIBLE);
                            shimmerFrameLayout.startShimmer();
                            getResults("2025", mDriverName, mDriverFamilyName);
                        }
                    });

                    radioButton_2024.setOnCheckedChangeListener((compoundButton, b) -> {
                        if (radioButton_2024.isChecked()) {
                            recyclerView.setVisibility(View.GONE);
                            shimmerFrameLayout.setVisibility(View.VISIBLE);
                            shimmerFrameLayout.startShimmer();
                            getResults("2024", mDriverName, mDriverFamilyName);
                        }
                    });

                    //setupSeasonRadioButtons(mFirstSeason, mLastSeason, mDriverName, mDriverFamilyName);
                }
                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e("driverPageActivity", "Driver information getting error:" + error.getMessage());
                }
            });
        }
    }

    private void getResults(String season, String driverName, String driverFamilyName){
        datum.clear();
        String fullDriverName = driverName + " " + driverFamilyName;
        DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference();
        rootRef.child("schedule/season/" + season + "/").orderByChild("round").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot ds : snapshot.getChildren()) {
                    String raceName = ds.getKey();
                    rootRef.child("results/season/" + season).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            if (snapshot.hasChild(fullDriverName)){
                                String driverResult = snapshot.child(fullDriverName)
                                        .child(raceName).child("Result").getValue(String.class);
                                driverResultsData results = new driverResultsData(raceName,
                                        driverResult, fullDriverName, Integer.parseInt(season));
                                datum.add(results);

                                hideShimmer(recyclerView, shimmerFrameLayout);
                                adapter.notifyDataSetChanged();
                            }
                            else{
                                String driverResult = getResources().getString(R.string.np_text);
                                driverResultsData results = new driverResultsData(raceName,
                                        driverResult, fullDriverName, Integer.parseInt(season));
                                datum.add(results);
                                hideShimmer(recyclerView, shimmerFrameLayout);
                                adapter.notifyDataSetChanged();
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