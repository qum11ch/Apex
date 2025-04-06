package com.example.f1app;

import static com.example.f1app.MainActivity.checkConnection;
import static com.example.f1app.driverStatsFragment.getCountryCode;

import android.content.Context;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.facebook.shimmer.ShimmerFrameLayout;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class pastSeasonTeamsStandingsActivity extends AppCompatActivity {
    Button showDriverButton, showDriverStanding, showTeams, showHomePage, showAccount;

    private List<teamsList> datum;
    private RecyclerView recyclerView;
    private ImageButton backButton;
    private TextView teamsHeader;
    private pastSeasonTeamsStandingsAdapter adapter;
    private ShimmerFrameLayout shimmerFrameLayout;
    private SwipeRefreshLayout swipeLayout;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.past_season_teams_standing_page);

        if (!checkConnection(getApplicationContext())){
            startActivity(connectionLostScreen.createShowSplashOnNetworkFailure(pastSeasonTeamsStandingsActivity.this));
        }else{
            startActivity(connectionLostScreen.createIntentHideSplashOnNetworkRecovery(pastSeasonTeamsStandingsActivity.this));
        }

        shimmerFrameLayout = findViewById(R.id.shimmer_layout);
        shimmerFrameLayout.startShimmer();

        teamsHeader = (TextView) findViewById(R.id.teamsHeader);
        String headerText = " ";
        if (Locale.getDefault().getLanguage().equals("ru")){
            headerText = getString(R.string.past_season_drivers) + " 2024";
        }else{
            headerText = "2024 " + getString(R.string.past_season_drivers);
        }
        teamsHeader.setText(headerText);

        recyclerView = findViewById(R.id.recyclerview_currentTeams);
        recyclerView.setHasFixedSize(true);
        LinearLayoutManager linearLayoutManager3 = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(linearLayoutManager3);

        datum = new ArrayList<>();
        LocalDate currentDate = LocalDate.now();

        swipeLayout = findViewById(R.id.swipe_layout);
        swipeLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                recyclerView.setVisibility(View.GONE);
                shimmerFrameLayout.setVisibility(View.VISIBLE);
                shimmerFrameLayout.startShimmer();
                datum = new ArrayList<>();
                getTeamStanding("2024");
                swipeLayout.setRefreshing(false);
            }
        });


        backButton = (ImageButton) findViewById(R.id.backButton);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        getTeamStanding("2024");

        WindowInsetsControllerCompat windowInsetsController =
                WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        windowInsetsController.setAppearanceLightStatusBars(false);

    }

    private void getTeamStanding(String currentYear){
        RequestQueue queue = Volley.newRequestQueue(pastSeasonTeamsStandingsActivity.this);
        String url2 = "https://api.jolpi.ca/ergast/f1/" + currentYear + "/constructorstandings/?format=json";
        JsonObjectRequest jsonObjectRequest2 = new JsonObjectRequest(
                Request.Method.GET,
                url2,
                null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            JSONObject MRData = response.getJSONObject("MRData");
                            String total = MRData.getString("total");
                            if (!total.equals("0")){
                                JSONObject StandingsTable = MRData.getJSONObject("StandingsTable");
                                JSONArray StandingsLists = StandingsTable.getJSONArray("StandingsLists");
                                for(int i = 0; i < StandingsLists.length(); i++){
                                    JSONArray ConstructorStandings = StandingsLists.getJSONObject(i)
                                            .getJSONArray("ConstructorStandings");
                                    for(int j = 0; j < ConstructorStandings.length(); j++){
                                        String constructorName = ConstructorStandings.getJSONObject(j)
                                                .getJSONObject("Constructor").getString("name");
                                        String position = ConstructorStandings.getJSONObject(j).getString("positionText");
                                        String points = ConstructorStandings.getJSONObject(j).getString("points");
                                        String constructorId = ConstructorStandings.getJSONObject(j)
                                                .getJSONObject("Constructor").getString("constructorId");
                                        if (constructorName.equals("Sauber")){
                                                constructorName = "Kick Sauber";
                                        }
                                        DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference();
                                        String finalConstructorName = constructorName;
                                        rootRef.child("driverLineUp/season/" + currentYear + "/" + constructorId).addValueEventListener(new ValueEventListener() {
                                            @Override
                                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                                ArrayList<String> teamDrivers = new ArrayList<>();
                                                for (DataSnapshot driverDataSnapshot : snapshot.child("drivers").getChildren()) {
                                                    String driverFullname = driverDataSnapshot.getKey();
                                                    teamDrivers.add(driverFullname);
                                                }
                                                teamsList smth = new teamsList(finalConstructorName, position, points, constructorId, false);
                                                smth.setDrivers(teamDrivers);
                                                datum.add(smth);
                                                Handler handler = new Handler();
                                                handler.postDelayed(()->{
                                                    recyclerView.setVisibility(View.VISIBLE);
                                                    shimmerFrameLayout.setVisibility(View.GONE);
                                                    shimmerFrameLayout.stopShimmer();
                                                },500);
                                                adapter = new pastSeasonTeamsStandingsAdapter(pastSeasonTeamsStandingsActivity.this, datum);
                                                recyclerView.setAdapter(adapter);
                                            }

                                            @Override
                                            public void onCancelled(@NonNull DatabaseError error) {
                                                Log.e("teamStandingsError", error.getMessage());
                                            }
                                        });
                                    }
                                }

                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Toast.makeText(pastSeasonTeamsStandingsActivity.this, error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
        queue.add(jsonObjectRequest2);
    }
}
