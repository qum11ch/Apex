package com.example.f1app;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class teamResultsFragment extends Fragment {
    private teamDriversResultsAdapter adapter;
    private RecyclerView recyclerView;
    private List<teamDriversResultsData> datum;
    private TextView firstDriverName, firstDriverFamilyName,
            secondDriverName, secondDriverFamilyName;
    private ImageView firstDriver_image, secondDriver_image;
    private CheckBox radioButton_2025, radioButton_2024;

    public teamResultsFragment() {
        // required empty public constructor.
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
        firstDriverFamilyName = (TextView) view.findViewById(R.id.firstDriverFamilyName);
        //secondDriverName = (TextView) view.findViewById(R.id.secondDriverName);
        secondDriverFamilyName = (TextView) view.findViewById(R.id.secondDriverFamilyName);
        firstDriver_image = (ImageView) view.findViewById(R.id.firstDriver_image);
        secondDriver_image = (ImageView) view.findViewById(R.id.secondDriver_image);
        radioButton_2025 = (CheckBox) view.findViewById(R.id.radioButton_2025);
        radioButton_2024 = (CheckBox) view.findViewById(R.id.radioButton_2024);

        recyclerView = view.findViewById(R.id.drivers_results);
        LinearLayoutManager mLayoutManager = new LinearLayoutManager(getActivity());
        recyclerView.setLayoutManager(mLayoutManager);

        if (!getArguments().isEmpty()){
            String mTeamId = getArguments().getString("teamId");
            String mTeamName = getArguments().getString("teamName");
            ArrayList<String> driversList = getArguments().getStringArrayList("teamDrivers");

            radioButton_2025.setChecked(true);
            getResults("2025", driversList);

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
                        getResults("2025", driversList);
                    }
                }
            });

            radioButton_2024.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
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
                                        getResults("2024", driversLineUp);
                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError error) {
                                        Log.e("error", "" + error);
                                    }
                                });
                    }
                }
            });
        }
    }
    private void getResults(String season, ArrayList<String> drivers){
        String mSeason = season;
        datum = new ArrayList<>();
        DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference();
        rootRef.child("schedule/season/" + mSeason + "/").orderByChild("round").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot ds: snapshot.getChildren())
                {
                    String raceName = ds.getKey();
                    rootRef.child("results/season/").child(mSeason).addValueEventListener(new ValueEventListener() {
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
            String mDriverName, mDriverFamilyName;
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
                    int resourceId_driverImage = requireContext().getResources().getIdentifier(driversCode.toLowerCase(), "drawable",
                            requireContext().getPackageName());
                    if (finalI == 0){
                        firstDriverFamilyName.setText(mDriverFamilyName);
                        Glide.with(requireContext())
                                .load(resourceId_driverImage)
                                .transition(DrawableTransitionOptions.withCrossFade())
                                .diskCacheStrategy(DiskCacheStrategy.NONE)
                                .transition(DrawableTransitionOptions.withCrossFade())
                                .error(R.drawable.f1)
                                .into(firstDriver_image);
                    }else{
                        secondDriverFamilyName.setText(mDriverFamilyName);
                        Glide.with(requireContext())
                                .load(resourceId_driverImage)
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