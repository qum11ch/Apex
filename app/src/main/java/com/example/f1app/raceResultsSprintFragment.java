package com.example.f1app;

import android.os.Bundle;
import android.os.Handler;
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
import com.facebook.shimmer.ShimmerFrameLayout;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class raceResultsSprintFragment extends Fragment {
    private raceResultsRaceAdapter adapter;
    private RecyclerView recyclerView;
    private List<raceResultsRaceData> datum;
    private ShimmerFrameLayout shimmerFrameLayout;

    public raceResultsSprintFragment() {
        // required empty public constructor.
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.race_results_sprint_fragment, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        datum = new ArrayList<>();

        recyclerView = view.findViewById(R.id.race_results);
        LinearLayoutManager mLayoutManager = new LinearLayoutManager(requireContext());
        recyclerView.setLayoutManager(mLayoutManager);

        shimmerFrameLayout = view.findViewById(R.id.shimmer_layout);
        shimmerFrameLayout.startShimmer();


        if (!getArguments().isEmpty()){
            String mCircuitId = getArguments().getString("circuitId");
            String mRaceName = getArguments().getString("raceName");
            String mSeason = getArguments().getString("season");

            datum = new ArrayList<>();
            getSprintData(mCircuitId, mSeason);
        }
    }

    public void getSprintData(String circuitId, String season){
        RequestQueue queue = Volley.newRequestQueue(requireContext());
        String url2 = "https://api.jolpi.ca/ergast/f1/" + season + "/circuits/" + circuitId + "/sprint/?format=json";
        JsonObjectRequest jsonObjectRequest2 = new JsonObjectRequest(
                Request.Method.GET,
                url2,
                null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            JSONObject MRData = response.getJSONObject("MRData");
                            JSONObject RaceTable = MRData.getJSONObject("RaceTable");
                            JSONArray Races = RaceTable.getJSONArray("Races");
                            for(int i = 0; i < Races.length(); i++) {
                                JSONArray QualifyingResults = Races.getJSONObject(i)
                                        .getJSONArray("SprintResults");
                                for (int j = 0; j < QualifyingResults.length(); j++) {
                                    JSONObject Result = QualifyingResults.getJSONObject(j);
                                    String positionText = Result.getString("positionText");
                                    String driverCode = Result.getJSONObject("Driver")
                                            .getString("code");
                                    String constructorId = Result.getJSONObject("Constructor")
                                            .getString("constructorId");
                                    String time;
                                    String points = Result.getString("points");
                                    String position = positionText;;
                                    if (positionText.equals("R")){
                                        time = getResources().getString(R.string.dnf_text);
                                        position = getResources().getString(R.string.nc_text);
                                    }else{
                                        if (Result.has("Time")){
                                            time = Result.getJSONObject("Time")
                                                    .getString("time");
                                            if (!positionText.equals("1")) {
                                                time += getResources().getString(R.string.seconds_text);
                                            }
                                        }else{
                                            String status = Result.getString("status");
                                            if (status.contains("Lap")){
                                                time = Result.getString("status");
                                            }else{
                                                time = getResources().getString(R.string.dnf_text);
                                                position = getResources().getString(R.string.nc_text);
                                            }
                                        }
                                    }
                                    raceResultsRaceData results = new raceResultsRaceData(position,
                                            constructorId, driverCode, time, points, season);
                                    datum.add(results);
                                }
                                Handler handler = new Handler();
                                handler.postDelayed(()->{
                                    recyclerView.setVisibility(View.VISIBLE);
                                    shimmerFrameLayout.setVisibility(View.GONE);
                                    shimmerFrameLayout.stopShimmer();
                                },500);
                                adapter = new raceResultsRaceAdapter(requireActivity(), datum);
                                recyclerView.setAdapter(adapter);
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Toast.makeText(requireContext(), error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
        queue.add(jsonObjectRequest2);
    }
}