package com.example.f1app;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class concludedRaceFragment extends Fragment {
    List<concludedRaceData> datum;
    private concludedRaceAdapter adapter;
    private RecyclerView recyclerView;

    public concludedRaceFragment() {

    }
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.concluded_race_fragment, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        datum = new ArrayList<>();
        recyclerView = view.findViewById(R.id.recyclerview_concludedRaces);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getActivity().getApplicationContext());
        linearLayoutManager.setReverseLayout(true);
        linearLayoutManager.setStackFromEnd(true);
        recyclerView.setLayoutManager(linearLayoutManager);
        LocalDate currentDate = LocalDate.now();
        String currentYear = Integer.toString(currentDate.getYear());

        if (!getArguments().isEmpty()){
            ArrayList<String> concludedRaceRoundNumber = getArguments().getStringArrayList("raceRound");

            for (int i = 0; i < concludedRaceRoundNumber.size(); i++) {
                String raceRound = concludedRaceRoundNumber.get(i);

                DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference();
                rootRef.child("schedule/season/" + currentYear).orderByChild("round").equalTo(Integer.valueOf(raceRound)).addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        datum = new ArrayList<>();
                        for(DataSnapshot ds : snapshot.getChildren()) {
                            String winnerCode = ds.child("raceWinnerCode").getValue(String.class);
                            if (winnerCode.equals("N/A")){
                                continue;
                            }else{
                                String raceName = ds.child("raceName").getValue(String.class);
                                String dateStart = ds.child("firstPracticeDate").getValue(String.class);
                                String dateEnd = ds.child("raceDate").getValue(String.class);
                                String circuitId = ds.child("circuitId").getValue(String.class);
                                String secondCode = ds.child("raceSecondCode").getValue(String.class);
                                String thirdCode = ds.child("raceThirdCode").getValue(String.class);
                                rootRef.child("circuits/" + circuitId).addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                        String circuitName = dataSnapshot.child("circuitName").getValue(String.class);
                                        String raceCountry = dataSnapshot.child("country").getValue(String.class);
                                        String raceLocation = dataSnapshot.child("location").getValue(String.class);

                                        concludedRaceData concludedRace = new concludedRaceData(dateStart,
                                                dateEnd, raceName, raceRound, circuitName, raceCountry, raceLocation, winnerCode, secondCode,
                                                thirdCode);
                                        datum.add(concludedRace);
                                        adapter = new concludedRaceAdapter(getActivity(), datum);
                                        recyclerView.setAdapter(adapter);
                                    }
                                    @Override
                                    public void onCancelled(@NonNull DatabaseError error) {
                                        Log.e("concludedRaceFragmentFirebaseError", error.getMessage());
                                    }
                                });
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e("concludedRaceFragmentFirebaseError", error.getMessage());
                    }
                });
            }
        }else{
            Log.i("futureFragment", "Bundle is null");
        }

    }
}
