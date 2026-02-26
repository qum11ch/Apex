package com.example.f1app;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.facebook.shimmer.ShimmerFrameLayout;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.List;

public class teamResultsFragment extends Fragment {
    private teamDriversResultsAdapter adapter;
    private teamTripleDriversResultsAdapter adapterTriple;
    private RecyclerView recyclerView;
    private List<teamDriversResultsData> datum;
    private List<teamTripleDriversResultsData> datumTriple;
    private TextView firstDriverFamilyName, secondDriverFamilyName, thirdDriverFamilyName, raceNameHeader;
    private ImageView firstDriver_image, secondDriver_image, thirdDriver_image;
    private CheckBox radioButton_2025, radioButton_2024, radioButton_2026;
    private ShimmerFrameLayout shimmerFrameLayout, shimmerTripleFrameLayout;
    private String teamId = " ";
    private String teamName = " ";
    private RelativeLayout firstDriverLayout, secondDriverLayout, thirdDriverLayout;

    private Handler handler = new Handler(Looper.getMainLooper());
    private Runnable hideShimmerRunnable;

    private int loadedRacesCount = 0;
    private int totalRacesToLoad = 0;
    private boolean isTripleDriver = false;

    public teamResultsFragment() {
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    public void onResume() {
        super.onResume();
        if (getView() != null) {
            getView().requestLayout();
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.team_page_results_fragment, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        firstDriverFamilyName = view.findViewById(R.id.firstDriverFamilyName);
        secondDriverFamilyName = view.findViewById(R.id.secondDriverFamilyName);
        thirdDriverFamilyName = view.findViewById(R.id.thirdDriverFamilyName);
        firstDriver_image = view.findViewById(R.id.firstDriver_image);
        secondDriver_image = view.findViewById(R.id.secondDriver_image);
        thirdDriver_image = view.findViewById(R.id.thirdDriver_image);
        radioButton_2025 = view.findViewById(R.id.radioButton_2025);
        radioButton_2026 = view.findViewById(R.id.radioButton_2026);
        radioButton_2024 = view.findViewById(R.id.radioButton_2024);
        shimmerFrameLayout = view.findViewById(R.id.shimmer_layout);
        shimmerTripleFrameLayout = view.findViewById(R.id.shimmerTriple_layout);
        firstDriverLayout = view.findViewById(R.id.firstDriver_layout);
        secondDriverLayout = view.findViewById(R.id.secondDriver_layout);
        thirdDriverLayout = view.findViewById(R.id.thirdDriver_layout);
        raceNameHeader = view.findViewById(R.id.raceName_header);

        recyclerView = view.findViewById(R.id.drivers_results);
        LinearLayoutManager mLayoutManager = new LinearLayoutManager(getActivity());
        recyclerView.setLayoutManager(mLayoutManager);

        if (!getArguments().isEmpty()) {
            String mTeamId = getArguments().getString("teamId");
            teamId = mTeamId;
            teamName = getArguments().getString("teamName");
            ArrayList<String> driversList = getArguments().getStringArrayList("teamDrivers");
            String mCurrentSeason = getArguments().getString("currentSeason");

            shimmerFrameLayout.startShimmer();
            shimmerTripleFrameLayout.startShimmer();
            radioButton_2026.setChecked(true);

            assert driversList != null;

            Log.e("fatal", "DriversList: " + mTeamId + " - " + driversList);

            loadDriversInfo(driversList, "2026");
            getResults("2026", driversList);

            DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference();
            rootRef.child("constructors").child(teamId).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    Integer enterYear = snapshot.child("enterYear").getValue(Integer.class);
                    if (enterYear > 2024){
                        radioButton_2024.setVisibility(View.GONE);
                    }
                    if (enterYear > 2025) {
                        radioButton_2025.setVisibility(View.GONE);
                    }
                    if (enterYear > 2026){
                        radioButton_2026.setVisibility(View.GONE);
                    }
                }
                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e("driverPageActivity", "Driver information getting error:" + error.getMessage());
                }
            });

            radioButton_2025.setOnClickListener(view1 -> {
                if (!radioButton_2025.isChecked()) {
                    radioButton_2026.setChecked(false);
                    radioButton_2025.setChecked(true);
                    radioButton_2024.setChecked(false);
                }
                radioButton_2024.setChecked(false);
                radioButton_2026.setChecked(false);
            });

            radioButton_2026.setOnClickListener(view1 -> {
                if (!radioButton_2026.isChecked()) {
                    radioButton_2026.setChecked(true);
                    radioButton_2025.setChecked(false);
                    radioButton_2024.setChecked(false);
                }
                radioButton_2024.setChecked(false);
                radioButton_2025.setChecked(false);
            });

            radioButton_2024.setOnClickListener(view2 -> {
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
                    shimmerTripleFrameLayout.setVisibility(View.VISIBLE);
                    shimmerFrameLayout.startShimmer();
                    shimmerTripleFrameLayout.startShimmer();
                    loadDriversInfo(driversList, "2026");
                    getResults("2026", driversList);
                }
            });

            radioButton_2025.setOnCheckedChangeListener((compoundButton, b) -> {
                if (radioButton_2025.isChecked()) {
                    ArrayList<String> teamDrivers = new ArrayList<>();

                    rootRef.child("results/season/2025").addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            for (DataSnapshot driverSnap : snapshot.getChildren()) {
                                String driverName = driverSnap.getKey();
                                for (DataSnapshot raceSnaps : driverSnap.getChildren()) {
                                    String teamIdFromDb = raceSnaps.child("TeamId").getValue(String.class);
                                    if (teamIdFromDb != null && teamIdFromDb.equals(mTeamId)) {
                                        teamDrivers.add(driverName);
                                        break;
                                    }
                                }
                            }
                            recyclerView.setVisibility(View.GONE);
                            shimmerFrameLayout.setVisibility(View.VISIBLE);
                            shimmerTripleFrameLayout.setVisibility(View.VISIBLE);
                            shimmerFrameLayout.startShimmer();
                            shimmerTripleFrameLayout.startShimmer();
                            loadDriversInfo(teamDrivers, "2025");
                            getResults("2025", teamDrivers);
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            Log.e("error", "" + error);
                        }
                    });
                }
            });

            radioButton_2024.setOnCheckedChangeListener((compoundButton, b) -> {
                if (radioButton_2024.isChecked()) {
                    ArrayList<String> teamDrivers = new ArrayList<>();

                    rootRef.child("results/season/2024").addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            for (DataSnapshot driverSnap : snapshot.getChildren()) {
                                String driverName = driverSnap.getKey();
                                for (DataSnapshot raceSnaps : driverSnap.getChildren()) {
                                    String teamIdFromDb = raceSnaps.child("TeamId").getValue(String.class);
                                    if (teamIdFromDb != null && teamIdFromDb.equals(mTeamId)) {
                                        teamDrivers.add(driverName);
                                        break;
                                    }
                                }
                            }
                            recyclerView.setVisibility(View.GONE);
                            shimmerFrameLayout.setVisibility(View.VISIBLE);
                            shimmerTripleFrameLayout.setVisibility(View.VISIBLE);
                            shimmerFrameLayout.startShimmer();
                            shimmerTripleFrameLayout.startShimmer();
                            loadDriversInfo(teamDrivers, "2024");
                            getResults("2024", teamDrivers);
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            Log.e("error", "" + error);
                        }
                    });
                }
            });
        }
    }

    private void loadDriversInfo(ArrayList<String> drivers, String season) {
        DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference();
        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference storageRef = storage.getReference();

        for (int i = 0; i < drivers.size(); i++) {
            String[] driverFullname = drivers.get(i).split(" ");
            String mDriverFamilyName;
            String mDriverName;

            if (drivers.get(i).equals("Andrea Kimi Antonelli")) {
                mDriverName = driverFullname[0] + " " + driverFullname[1];
                mDriverFamilyName = driverFullname[2];
            } else {
                mDriverName = driverFullname[0];
                mDriverFamilyName = driverFullname[1];
            }

            int finalI = i;
            String finalDriverName = mDriverName;
            String finalDriverFamilyName = mDriverFamilyName;

            rootRef.child("drivers").child(drivers.get(i)).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    String driversCode = snapshot.child("driversCode").getValue(String.class);
                    StorageReference mDriverImage = storageRef.child("drivers/" + driversCode.toLowerCase() + "_" + season + ".png");
                    //if (season.equals("2024")) {
                    //    mDriverImage = storageRef.child("drivers/" + driversCode.toLowerCase() + "_2024.png");
                    //} else {
                    //    mDriverImage = storageRef.child("drivers/" + driversCode.toLowerCase() + ".png");
                    //}

                    if (finalI == 0) {
                        firstDriverFamilyName.setText(finalDriverFamilyName);
                        GlideApp.with(requireContext())
                                .load(mDriverImage)
                                .transition(DrawableTransitionOptions.withCrossFade())
                                .diskCacheStrategy(DiskCacheStrategy.NONE)
                                .error(R.drawable.placeholder_driver)
                                .into(firstDriver_image);

                        firstDriverLayout.setOnClickListener(v ->
                                navigateToDriverPage(drivers.get(finalI), finalDriverName, finalDriverFamilyName)
                        );

                    } else if (finalI == 1) {
                        secondDriverFamilyName.setText(finalDriverFamilyName);
                        GlideApp.with(requireContext())
                                .load(mDriverImage)
                                .transition(DrawableTransitionOptions.withCrossFade())
                                .diskCacheStrategy(DiskCacheStrategy.NONE)
                                .error(R.drawable.placeholder_driver)
                                .into(secondDriver_image);

                        secondDriverLayout.setOnClickListener(v ->
                                navigateToDriverPage(drivers.get(finalI), finalDriverName, finalDriverFamilyName)
                        );

                    } else if (finalI == 2) {
                        thirdDriverFamilyName.setText(finalDriverFamilyName);
                        GlideApp.with(requireContext())
                                .load(mDriverImage)
                                .transition(DrawableTransitionOptions.withCrossFade())
                                .diskCacheStrategy(DiskCacheStrategy.NONE)
                                .error(R.drawable.placeholder_driver)
                                .into(thirdDriver_image);

                        thirdDriverLayout.setOnClickListener(v ->
                                navigateToDriverPage(drivers.get(finalI), finalDriverName, finalDriverFamilyName)
                        );
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e("teamPageActivity", "Drivers error:" + error.getMessage());
                }
            });
        }
    }

    private void navigateToDriverPage(String driverFullName, String driverName, String driverFamilyName) {
        DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference();
        rootRef.child("drivers").child(driverFullName).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String mDriverCode = snapshot.child("driversCode").getValue(String.class);
                Intent intent = new Intent(requireContext(), driverPageActivity.class);
                Bundle bundle = new Bundle();
                bundle.putString("driverName", driverName);
                bundle.putString("driverFamilyName", driverFamilyName);
                bundle.putString("driverTeam", teamName);
                bundle.putString("driverCode", mDriverCode);
                bundle.putString("driverTeamId", teamId);
                intent.putExtras(bundle);
                requireContext().startActivity(intent);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("teamPageActivity", "Drivers error:" + error.getMessage());
            }
        });
    }

    private void getResults(String season, ArrayList<String> drivers) {
        datum = new ArrayList<>();
        datumTriple = new ArrayList<>();
        loadedRacesCount = 0;

        adapter = null;
        adapterTriple = null;
        recyclerView.setAdapter(null);

        DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference();

        rootRef.child("schedule/season/" + season)
                .orderByChild("round")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        totalRacesToLoad = (int) snapshot.getChildrenCount();

                        if (totalRacesToLoad == 0) {
                            hideShimmer();
                            return;
                        }

                        isTripleDriver = drivers.size() > 2;

                        rootRef.child("results/season/" + season)
                                .addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot resultSnapshot) {
                                        for (DataSnapshot ds : snapshot.getChildren()) {
                                            String raceName = ds.getKey();
                                            processRaceResult(raceName, resultSnapshot, drivers, season);
                                        }
                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError error) {
                                        Log.e("error", "" + error);
                                    }
                                });
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e("error", "" + error);
                    }
                });
    }

    private void processRaceResult(String raceName, DataSnapshot resultSnapshot, ArrayList<String> drivers, String season) {
        String firstDriverName = drivers.get(0);
        String secondDriverName = drivers.get(1);

        String firstDriverResult = resultSnapshot.child(firstDriverName)
                .child(raceName).child("Result").getValue(String.class);
        String firstDriverResultTeam = resultSnapshot.child(firstDriverName)
                .child(raceName).child("TeamId").getValue(String.class);

        String finalFirstDriverResult = (firstDriverResult != null &&
                !firstDriverResult.equals("N/C") &&
                firstDriverResultTeam != null &&
                firstDriverResultTeam.equals(teamId)) ? firstDriverResult :
                (firstDriverResult != null && firstDriverResult.equals("N/C")) ? "N/C" : "null";

        String secondDriverResult = resultSnapshot.child(secondDriverName)
                .child(raceName).child("Result").getValue(String.class);
        String secondDriverResultTeam = resultSnapshot.child(secondDriverName)
                .child(raceName).child("TeamId").getValue(String.class);

        String finalSecondDriverResult = (secondDriverResult != null &&
                !secondDriverResult.equals("N/C") &&
                secondDriverResultTeam != null &&
                secondDriverResultTeam.equals(teamId)) ? secondDriverResult :
                (secondDriverResult != null && secondDriverResult.equals("N/C")) ? "N/C" : "null";

        if (isTripleDriver) {
            String thirdDriverName = drivers.get(2);
            String thirdDriverResult = resultSnapshot.child(thirdDriverName)
                    .child(raceName).child("Result").getValue(String.class);
            String thirdDriverResultTeam = resultSnapshot.child(thirdDriverName)
                    .child(raceName).child("TeamId").getValue(String.class);

            String finalThirdDriverResult = (thirdDriverResult != null &&
                    !thirdDriverResult.equals("N/C") &&
                    thirdDriverResultTeam != null &&
                    thirdDriverResultTeam.equals(teamId)) ? thirdDriverResult :
                    (thirdDriverResult != null && thirdDriverResult.equals("N/C")) ? "N/C" : "null";

            teamTripleDriversResultsData results = new teamTripleDriversResultsData(
                    raceName, finalFirstDriverResult, firstDriverName,
                    finalSecondDriverResult, secondDriverName,
                    finalThirdDriverResult, thirdDriverName, Integer.parseInt(season)
            );
            datumTriple.add(results);
        } else {
            teamDriversResultsData results = new teamDriversResultsData(
                    raceName, finalFirstDriverResult, firstDriverName,
                    finalSecondDriverResult, secondDriverName, Integer.parseInt(season)
            );
            datum.add(results);
        }

        loadedRacesCount++;

        if (isTripleDriver) {
            if (adapterTriple == null) {
                adapterTriple = new teamTripleDriversResultsAdapter(getActivity(), datumTriple);
                recyclerView.setAdapter(adapterTriple);
            } else {
                adapterTriple.notifyItemInserted(datumTriple.size() - 1);
            }
        } else {
            if (adapter == null) {
                adapter = new teamDriversResultsAdapter(getActivity(), datum);
                recyclerView.setAdapter(adapter);
            } else {
                adapter.notifyItemInserted(datum.size() - 1);
            }
        }

        if (loadedRacesCount >= totalRacesToLoad) {
            updateLayoutParams();
            hideShimmer();
        }
    }

    private void updateLayoutParams() {
        int marginEnd = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 10, requireContext().getResources().getDisplayMetrics());

        if (isTripleDriver) {
            shimmerFrameLayout.setVisibility(View.GONE);
            shimmerTripleFrameLayout.setVisibility(View.VISIBLE);
            thirdDriverLayout.setVisibility(View.VISIBLE);
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.55f);
            layoutParams.gravity = Gravity.BOTTOM;
            layoutParams.setMargins(0, 0, marginEnd, 0);
            raceNameHeader.setLayoutParams(layoutParams);
        } else {
            shimmerFrameLayout.setVisibility(View.VISIBLE);
            shimmerTripleFrameLayout.setVisibility(View.GONE);
            thirdDriverLayout.setVisibility(View.GONE);
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 2.7f);
            layoutParams.gravity = Gravity.BOTTOM;
            layoutParams.setMargins(0, 0, marginEnd, 0);
            raceNameHeader.setLayoutParams(layoutParams);
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
                shimmerTripleFrameLayout.setVisibility(View.GONE);
                shimmerFrameLayout.stopShimmer();
                shimmerTripleFrameLayout.stopShimmer();
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