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

public class driverResultsFragment extends Fragment {
    private driverResultsAdapter adapter;
    private RecyclerView recyclerView;
    private List<driverResultsData> datum;

    public driverResultsFragment() {
        // required empty public constructor.
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.driver_page_results_fragment, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        datum = new ArrayList<>();

        recyclerView = view.findViewById(R.id.driver_results);
        LinearLayoutManager mLayoutManager = new LinearLayoutManager(getActivity());
        recyclerView.setLayoutManager(mLayoutManager);

        DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference();
        if (!getArguments().isEmpty()) {
            String mDriverTeam = getArguments().getString("driverTeam");
            String mDriverName = getArguments().getString("driverName");
            String mDriverCode = getArguments().getString("driverCode");
            String mDriverFamilyName = getArguments().getString("driverFamilyName");

            String fullDriverName = mDriverName + " " + mDriverFamilyName;

            rootRef.child("schedule/season/2024/").orderByChild("round").addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    for (DataSnapshot ds : snapshot.getChildren()) {
                        String raceName = ds.getKey();
                        rootRef.child("results/season/2024").addValueEventListener(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                String driverResult = snapshot.child(fullDriverName)
                                        .child(raceName).getValue(String.class);
                                driverResultsData results = new driverResultsData(raceName,
                                        driverResult, fullDriverName, 2025);
                                datum.add(results);
                                adapter = new driverResultsAdapter(getActivity(), datum);
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
        }
    }
}