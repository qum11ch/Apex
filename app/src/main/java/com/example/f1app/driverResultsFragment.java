package com.example.f1app;

import static com.example.f1app.MainActivity.hideShimmer;

import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.CheckBox;
import android.widget.CompoundButton;

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
    private CheckBox checkBox_2025, checkBox_2024, checkBox_2026;
    private ShimmerFrameLayout shimmerFrameLayout;
    private NestedScrollView scrollView;

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
        checkBox_2025 = view.findViewById(R.id.radioButton_2025);
        checkBox_2024 = view.findViewById(R.id.radioButton_2024);
        checkBox_2026 = view.findViewById(R.id.radioButton_2026);
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
            checkBox_2026.setChecked(true);

            DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference();
            rootRef.child("drivers").child(mDriverName + " " + mDriverFamilyName).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    String mLastEntry = snapshot.child("lastEntry").getValue(String.class);
                    String mFirstEntry = snapshot.child("firstEntry").getValue(String.class);

                    String[] mLastGPparse = mLastEntry.split("\\s+");
                    String mLastSeason = mLastGPparse[0];
                    String[] mFirstGPparse = mFirstEntry.split("\\s+");
                    String mFirstSeason = mFirstGPparse[0];

                    setupSeasonRadioButtons(mFirstSeason, mLastSeason, mDriverName, mDriverFamilyName);
                }
                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e("driverPageActivity", "Driver information getting error:" + error.getMessage());
                }
            });
        }
    }

    private void setupSeasonRadioButtons(String firstSeason, String lastSeason,
                                         String driverName, String driverFamilyName) {
        checkBox_2024.setChecked(false);
        checkBox_2025.setChecked(false);
        checkBox_2026.setChecked(false);

        checkBox_2024.setVisibility(View.GONE);
        checkBox_2025.setVisibility(View.GONE);
        checkBox_2026.setVisibility(View.GONE);

        int firstYear = Integer.parseInt(firstSeason);
        int lastYear = Integer.parseInt(lastSeason);

        boolean has2024 = firstYear <= 2024 && lastYear >= 2024;
        boolean has2025 = firstYear <= 2025 && lastYear >= 2025;
        boolean has2026 = firstYear <= 2026 && lastYear >= 2026;

        if (has2024) checkBox_2024.setVisibility(View.VISIBLE);
        if (has2025) checkBox_2025.setVisibility(View.VISIBLE);
        if (has2026) checkBox_2026.setVisibility(View.VISIBLE);

        String defaultSeason = String.valueOf(lastYear);
        if (has2025) defaultSeason = "2025";

        switch (defaultSeason) {
            case "2024":
                checkBox_2024.setChecked(true);
                getResults("2024", driverName, driverFamilyName);
                break;
            case "2025":
                checkBox_2025.setChecked(true);
                getResults("2025", driverName, driverFamilyName);
                break;
            case "2026":
                checkBox_2026.setChecked(true);
                getResults("2026", driverName, driverFamilyName);
                break;
        }

        CompoundButton.OnCheckedChangeListener listener = (view, isChecked) -> {
            if (!isChecked) return;

            checkBox_2024.setChecked(false);
            checkBox_2025.setChecked(false);
            checkBox_2026.setChecked(false);
            ((CompoundButton) view).setChecked(true);

            recyclerView.setVisibility(View.GONE);
            shimmerFrameLayout.setVisibility(View.VISIBLE);
            shimmerFrameLayout.startShimmer();

            int id = view.getId();
            if (id == R.id.radioButton_2024) {
                getResults("2024", driverName, driverFamilyName);
            } else if (id == R.id.radioButton_2025) {
                getResults("2025", driverName, driverFamilyName);
            } else if (id == R.id.radioButton_2026) {
                getResults("2026", driverName, driverFamilyName);
            }
        };

        checkBox_2024.setOnCheckedChangeListener(listener);
        checkBox_2025.setOnCheckedChangeListener(listener);
        checkBox_2026.setOnCheckedChangeListener(listener);
    }

    private void getResults(String season, String driverName, String driverFamilyName){
        String fullDriverName = driverName + " " + driverFamilyName;
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
                                        .child(raceName).child("Result").getValue(String.class);
                                driverResultsData results = new driverResultsData(raceName,
                                        driverResult, fullDriverName, Integer.parseInt(season));
                                datum.add(results);

                                hideShimmer(recyclerView, shimmerFrameLayout);
                                adapter.notifyItemChanged(datum.size() - 1);
                            }
                            else{
                                String driverResult = getResources().getString(R.string.np_text);
                                driverResultsData results = new driverResultsData(raceName,
                                        driverResult, fullDriverName, Integer.parseInt(season));
                                datum.add(results);
                                hideShimmer(recyclerView, shimmerFrameLayout);
                                adapter.notifyItemChanged(datum.size() - 1);
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