package com.example.f1app;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
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
        datum = new ArrayList<>();

        //firstDriverName = (TextView) view.findViewById(R.id.firstDriverName);
        firstDriverFamilyName = (TextView) view.findViewById(R.id.firstDriverFamilyName);
        //secondDriverName = (TextView) view.findViewById(R.id.secondDriverName);
        secondDriverFamilyName = (TextView) view.findViewById(R.id.secondDriverFamilyName);
        firstDriver_image = (ImageView) view.findViewById(R.id.firstDriver_image);
        secondDriver_image = (ImageView) view.findViewById(R.id.secondDriver_image);

        recyclerView = view.findViewById(R.id.drivers_results);
        LinearLayoutManager mLayoutManager = new LinearLayoutManager(getActivity());
        recyclerView.setLayoutManager(mLayoutManager);

        DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference();
        if (!getArguments().isEmpty()){
            String mTeamId = getArguments().getString("teamId");
            String mTeamName = getArguments().getString("teamName");
            ArrayList<String> driversList = getArguments().getStringArrayList("teamDrivers");

            rootRef.child("schedule/season/2024/").orderByChild("round").addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    for (DataSnapshot ds: snapshot.getChildren())
                    {
                        String raceName = ds.getKey();
                        rootRef.child("results/season/2024").addValueEventListener(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                String firstDriverName = driversList.get(0);
                                String secondDriverName = driversList.get(1);
                                String firstDriverResult = snapshot.child(firstDriverName)
                                        .child(raceName).getValue(String.class);
                                String secondDriverResult = snapshot.child(secondDriverName)
                                        .child(raceName).getValue(String.class);
                                teamDriversResultsData results = new teamDriversResultsData(raceName,
                                        firstDriverResult, firstDriverName, secondDriverResult,
                                        secondDriverName, 2025);
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


            for (int i = 0; i < driversList.size(); i++){
                String[] driverFullname = driversList.get(i).split(" ");
                String mDriverName, mDriverFamilyName;
                if(driversList.get(i).equals("Andrea Kimi Antonelli")){
                    mDriverName = driverFullname[0] + " " + driverFullname[1];
                    mDriverFamilyName = driverFullname[2];
                }else{
                    mDriverName = driverFullname[0];
                    mDriverFamilyName = driverFullname[1];
                }
                int finalI = i;
                rootRef.child("drivers").child(driversList.get(i)).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        String driversCode = snapshot.child("driversCode").getValue(String.class);
                        int resourceId_driverImage = requireContext().getResources().getIdentifier(driversCode.toLowerCase(), "drawable",
                                requireContext().getPackageName());
                        if (finalI == 0){
                            //firstDriverName.setText(mDriverName);
                            firstDriverFamilyName.setText(mDriverFamilyName);
                            Glide.with(requireContext())
                                    .load(resourceId_driverImage)
                                    .transition(DrawableTransitionOptions.withCrossFade())
                                    .error(R.drawable.f1)
                                    .into(firstDriver_image);
                        }else{
                            //secondDriverName.setText(mDriverName);
                            secondDriverFamilyName.setText(mDriverFamilyName);
                            Glide.with(requireContext())
                                    .load(resourceId_driverImage)
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
}