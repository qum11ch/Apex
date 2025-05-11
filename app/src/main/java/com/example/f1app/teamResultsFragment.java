package com.example.f1app;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
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
    private CheckBox radioButton_2025, radioButton_2024;
    private ShimmerFrameLayout shimmerFrameLayout, shimmerTripleFrameLayout;
    private String teamId = " ";
    private String teamName = " ";
    private RelativeLayout firstDriverLayout, secondDriverLayout, thirdDriverLayout;

    public teamResultsFragment() {
        // required empty public constructor.
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    public void onResume(){
        super.onResume();
        getView().requestLayout();
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

        if (!getArguments().isEmpty()){
            String mTeamId = getArguments().getString("teamId");
            teamId = mTeamId;
            teamName = getArguments().getString("teamName");
            ArrayList<String> driversList = getArguments().getStringArrayList("teamDrivers");

            shimmerFrameLayout.startShimmer();
            shimmerTripleFrameLayout.startShimmer();
            radioButton_2025.setChecked(true);

            radioButton_2025.setChecked(true);
            assert driversList != null;
            getResults("2025", driversList);

            radioButton_2025.setOnClickListener(view1 -> {
                if (!radioButton_2025.isChecked()){
                    radioButton_2025.setChecked(true);
                    radioButton_2024.setChecked(false);
                }
                radioButton_2024.setChecked(false);
            });

            radioButton_2024.setOnClickListener(view2 -> {
                if (!radioButton_2024.isChecked()){
                    radioButton_2025.setChecked(false);
                    radioButton_2024.setChecked(true);
                }
                radioButton_2025.setChecked(false);
            });

            radioButton_2025.setOnCheckedChangeListener((compoundButton, b) -> {
                if (radioButton_2025.isChecked()){
                    recyclerView.setVisibility(View.GONE);
                    shimmerFrameLayout.setVisibility(View.VISIBLE);
                    shimmerFrameLayout.startShimmer();
                    getResults("2025", driversList);
                }
            });

            radioButton_2024.setOnCheckedChangeListener((compoundButton, b) -> {
                if (radioButton_2024.isChecked()){

                    ArrayList<String> teamDrivers = new ArrayList<>();
                    DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference();
                    rootRef.child("results/season/" + "2024").addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            for (DataSnapshot driverSnap: snapshot.getChildren()){
                                String driverName = driverSnap.getKey();
                                for (DataSnapshot raceSnaps: driverSnap.getChildren()){
                                    if (raceSnaps.child("TeamId").getValue(String.class).equals(mTeamId)){
                                        teamDrivers.add(driverName);
                                        break;
                                    }
                                }
                            }
                            recyclerView.setVisibility(View.GONE);
                            shimmerFrameLayout.setVisibility(View.VISIBLE);
                            shimmerFrameLayout.startShimmer();
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
    private void getResults(String season, ArrayList<String> drivers){
        datum = new ArrayList<>();
        datumTriple = new ArrayList<>();
        DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference();
        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference storageRef = storage.getReference();
        rootRef.child("schedule/season/" + season + "/").orderByChild("round").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot ds: snapshot.getChildren())
                {
                    String raceName = ds.getKey();
                    rootRef.child("results/season/").child(season).addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot resultSnapshot) {
                            String firstDriverName = drivers.get(0);
                            String secondDriverName = drivers.get(1);

                            int marginEnd = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 10, requireContext().getResources().getDisplayMetrics());

                            assert raceName != null;
                            String firstDriverResult = resultSnapshot.child(firstDriverName)
                                    .child(raceName).child("Result").getValue(String.class);
                            String firstDriverResultTeam = resultSnapshot.child(firstDriverName)
                                    .child(raceName).child("TeamId").getValue(String.class);
                            assert firstDriverResult != null;
                            String finalFirstDriverResult;
                            if (!firstDriverResult.equals("N/C")){
                                assert firstDriverResultTeam != null;
                                if (!firstDriverResultTeam.equals(teamId)){
                                    finalFirstDriverResult = "null";
                                }else{
                                    finalFirstDriverResult = firstDriverResult;
                                }
                            }else{
                                finalFirstDriverResult = firstDriverResult;
                            }
                            String secondDriverResult = resultSnapshot.child(secondDriverName)
                                    .child(raceName).child("Result").getValue(String.class);
                            String secondDriverResultTeam = resultSnapshot.child(secondDriverName)
                                    .child(raceName).child("TeamId").getValue(String.class);
                            assert secondDriverResult != null;
                            String finalSecondDriverResult;
                            if (!secondDriverResult.equals("N/C")){
                                assert secondDriverResultTeam != null;
                                if (!secondDriverResultTeam.equals(teamId)){
                                    finalSecondDriverResult = "null";
                                }else{
                                    finalSecondDriverResult = secondDriverResult;
                                }
                            }else{
                                finalSecondDriverResult = secondDriverResult;
                            }

                            if (drivers.size() > 2){
                                shimmerFrameLayout.setVisibility(View.GONE);
                                shimmerTripleFrameLayout.setVisibility(View.VISIBLE);
                                thirdDriverLayout.setVisibility(View.VISIBLE);
                                LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.55f);
                                layoutParams.gravity = Gravity.BOTTOM;
                                layoutParams.setMargins(0,0,marginEnd,0);
                                raceNameHeader.setLayoutParams(layoutParams);

                                String thirdDriverName = drivers.get(2);
                                String thirdDriverResult = resultSnapshot.child(thirdDriverName)
                                        .child(raceName).child("Result").getValue(String.class);
                                String thirdDriverResultTeam = resultSnapshot.child(thirdDriverName)
                                        .child(raceName).child("TeamId").getValue(String.class);
                                assert thirdDriverResult != null;
                                String finalThirdDriverResult;
                                if (!thirdDriverResult.equals("N/C")){
                                    assert thirdDriverResultTeam != null;
                                    if (!thirdDriverResultTeam.equals(teamId)){
                                        finalThirdDriverResult = "null";
                                    }else{
                                        finalThirdDriverResult = thirdDriverResult;
                                    }
                                }else{
                                    finalThirdDriverResult = thirdDriverResult;
                                }
                                teamTripleDriversResultsData results = new teamTripleDriversResultsData(raceName,
                                        finalFirstDriverResult, firstDriverName, finalSecondDriverResult,
                                        secondDriverName, finalThirdDriverResult, thirdDriverName, Integer.parseInt(season));
                                Handler handler = new Handler();
                                handler.postDelayed(()->{
                                    recyclerView.setVisibility(View.VISIBLE);
                                    shimmerTripleFrameLayout.setVisibility(View.GONE);
                                    shimmerTripleFrameLayout.stopShimmer();
                                },500);
                                datumTriple.add(results);
                                adapterTriple = new teamTripleDriversResultsAdapter(getActivity(), datumTriple);
                                recyclerView.setAdapter(adapterTriple);
                            }else{
                                shimmerFrameLayout.setVisibility(View.VISIBLE);
                                shimmerTripleFrameLayout.setVisibility(View.GONE);
                                thirdDriverLayout.setVisibility(View.GONE);
                                LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 2.7f);
                                layoutParams.gravity = Gravity.BOTTOM;
                                layoutParams.setMargins(0,0,marginEnd,0);
                                raceNameHeader.setLayoutParams(layoutParams);

                                teamDriversResultsData results = new teamDriversResultsData(raceName,
                                        finalFirstDriverResult, firstDriverName, finalSecondDriverResult,
                                        secondDriverName, Integer.parseInt(season));
                                Handler handler = new Handler();
                                handler.postDelayed(()->{
                                    recyclerView.setVisibility(View.VISIBLE);
                                    shimmerFrameLayout.setVisibility(View.GONE);
                                    shimmerFrameLayout.stopShimmer();
                                },500);
                                datum.add(results);
                                adapter = new teamDriversResultsAdapter(getActivity(), datum);
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

        for (int i = 0; i < drivers.size(); i++){
            String[] driverFullname = drivers.get(i).split(" ");
            String mDriverFamilyName;
            String mDriverName;
            if(drivers.get(i).equals("Andrea Kimi Antonelli")){
                mDriverName = driverFullname[0] + " " + driverFullname[1];
                mDriverFamilyName = driverFullname[2];
            }else{
                mDriverName = driverFullname[0];
                mDriverFamilyName = driverFullname[1];
            }
            int finalI = i;
            rootRef.child("drivers").child(drivers.get(i)).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    String driversCode = snapshot.child("driversCode").getValue(String.class);
                    StorageReference mDriverImage;
                    if (season.equals("2024")) {
                        mDriverImage = storageRef.child("drivers/" + driversCode.toLowerCase() + "_2024.png");
                    } else {
                        mDriverImage = storageRef.child("drivers/" + driversCode.toLowerCase() + ".png");
                    }
                    if (finalI == 0) {
                        firstDriverFamilyName.setText(mDriverFamilyName);
                        GlideApp.with(requireContext())
                                .load(mDriverImage)
                                .transition(DrawableTransitionOptions.withCrossFade())
                                .diskCacheStrategy(DiskCacheStrategy.NONE)
                                .transition(DrawableTransitionOptions.withCrossFade())
                                .error(R.drawable.f1)
                                .into(firstDriver_image);

                        firstDriverLayout.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                rootRef.child("drivers").child(drivers.get(finalI)).addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                                        String mDriverCode = snapshot.child("driversCode").getValue(String.class);
                                        Intent intent = new Intent(requireContext(), driverPageActivity.class);
                                        Bundle bundle = new Bundle();
                                        bundle.putString("driverName", mDriverName);
                                        bundle.putString("driverFamilyName", mDriverFamilyName);
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
                        });
                    } else if (finalI == 1) {
                        secondDriverFamilyName.setText(mDriverFamilyName);
                        GlideApp.with(requireContext())
                                .load(mDriverImage)
                                .transition(DrawableTransitionOptions.withCrossFade())
                                .diskCacheStrategy(DiskCacheStrategy.NONE)
                                .transition(DrawableTransitionOptions.withCrossFade())
                                .error(R.drawable.f1)
                                .into(secondDriver_image);

                        secondDriverLayout.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                rootRef.child("drivers").child(drivers.get(finalI)).addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                                        String mDriverCode = snapshot.child("driversCode").getValue(String.class);
                                        Intent intent = new Intent(requireContext(), driverPageActivity.class);
                                        Bundle bundle = new Bundle();
                                        bundle.putString("driverName", mDriverName);
                                        bundle.putString("driverFamilyName", mDriverFamilyName);
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
                        });
                    } else if (finalI == 2) {
                        thirdDriverFamilyName.setText(mDriverFamilyName);
                        GlideApp.with(requireContext())
                                .load(mDriverImage)
                                .transition(DrawableTransitionOptions.withCrossFade())
                                .diskCacheStrategy(DiskCacheStrategy.NONE)
                                .transition(DrawableTransitionOptions.withCrossFade())
                                .error(R.drawable.f1)
                                .into(thirdDriver_image);
                        thirdDriverLayout.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                rootRef.child("drivers").child(drivers.get(finalI)).addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                                        String mDriverCode = snapshot.child("driversCode").getValue(String.class);
                                        Intent intent = new Intent(requireContext(), driverPageActivity.class);
                                        Bundle bundle = new Bundle();
                                        bundle.putString("driverName", mDriverName);
                                        bundle.putString("driverFamilyName", mDriverFamilyName);
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
                        });
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e("teamPageActivity", "Drivers error:" + error.getMessage());
                }
            });
        }
    }
}