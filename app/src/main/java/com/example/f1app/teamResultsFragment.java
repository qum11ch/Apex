package com.example.f1app;

import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
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
    private RecyclerView recyclerView;
    private List<teamDriversResultsData> datum;
    private TextView firstDriverFamilyName, secondDriverFamilyName;
    private ImageView firstDriver_image, secondDriver_image;
    private CheckBox radioButton_2025, radioButton_2024;
    private ShimmerFrameLayout shimmerFrameLayout;

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

        //firstDriverName = (TextView) view.findViewById(R.id.firstDriverName);
        firstDriverFamilyName = view.findViewById(R.id.firstDriverFamilyName);
        //secondDriverName = (TextView) view.findViewById(R.id.secondDriverName);
        secondDriverFamilyName = view.findViewById(R.id.secondDriverFamilyName);
        firstDriver_image = view.findViewById(R.id.firstDriver_image);
        secondDriver_image = view.findViewById(R.id.secondDriver_image);
        radioButton_2025 = view.findViewById(R.id.radioButton_2025);
        radioButton_2024 = view.findViewById(R.id.radioButton_2024);
        shimmerFrameLayout = view.findViewById(R.id.shimmer_layout);

        recyclerView = view.findViewById(R.id.drivers_results);
        LinearLayoutManager mLayoutManager = new LinearLayoutManager(getActivity());
        recyclerView.setLayoutManager(mLayoutManager);

        if (!getArguments().isEmpty()){
            String mTeamId = getArguments().getString("teamId");
            //String mTeamName = getArguments().getString("teamName");
            ArrayList<String> driversList = getArguments().getStringArrayList("teamDrivers");

            shimmerFrameLayout.startShimmer();
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
                    ArrayList<String> driversLineUp = new ArrayList<>();
                    DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference();
                    rootRef.child("driverLineUp/season/2024").child(mTeamId).child("drivers")
                            .addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot snapshot) {
                                    for (DataSnapshot driverSnapshot: snapshot.getChildren()){
                                        String driverName = driverSnapshot.getKey();
                                        driversLineUp.add(driverName);
                                    }
                                    recyclerView.setVisibility(View.GONE);
                                    shimmerFrameLayout.setVisibility(View.VISIBLE);
                                    shimmerFrameLayout.startShimmer();
                                    getResults("2024", driversLineUp);
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
                            String firstDriverResult = resultSnapshot.child(firstDriverName)
                                    .child(raceName).getValue(String.class);
                            String secondDriverResult = resultSnapshot.child(secondDriverName)
                                    .child(raceName).getValue(String.class);
                            teamDriversResultsData results = new teamDriversResultsData(raceName,
                                    firstDriverResult, firstDriverName, secondDriverResult,
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
            if(drivers.get(i).equals("Andrea Kimi Antonelli")){
                mDriverFamilyName = driverFullname[2];
            }else{
                mDriverFamilyName = driverFullname[1];
            }
            int finalI = i;
            rootRef.child("drivers").child(drivers.get(i)).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    String driversCode = snapshot.child("driversCode").getValue(String.class);
                    StorageReference mDriverImage;
                    if (season.equals("2024")){
                        mDriverImage = storageRef.child("drivers/" + driversCode.toLowerCase() + "_2024.png");
                    }else{
                        mDriverImage = storageRef.child("drivers/" + driversCode.toLowerCase() + ".png");
                    }
                    if (finalI == 0){
                        firstDriverFamilyName.setText(mDriverFamilyName);
                        GlideApp.with(requireContext())
                                .load(mDriverImage)
                                .transition(DrawableTransitionOptions.withCrossFade())
                                .diskCacheStrategy(DiskCacheStrategy.NONE)
                                .transition(DrawableTransitionOptions.withCrossFade())
                                .error(R.drawable.f1)
                                .into(firstDriver_image);
                    }else{
                        secondDriverFamilyName.setText(mDriverFamilyName);
                        GlideApp.with(requireContext())
                                .load(mDriverImage)
                                .transition(DrawableTransitionOptions.withCrossFade())
                                .diskCacheStrategy(DiskCacheStrategy.NONE)
                                .transition(DrawableTransitionOptions.withCrossFade())
                                .error(R.drawable.f1)
                                .into(secondDriver_image);
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