package com.example.f1app;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;
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

public class teamsStandingsActivity extends AppCompatActivity {
    Button showDriverButton, showDriverStanding, showTeams, showHomePage, showAccount;

    private List<teamsList> datum;
    private RecyclerView recyclerView;
    private ImageButton backButton;

    private teamsStandingsAdapter adapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.teams_standing);

        recyclerView = findViewById(R.id.recyclerview_currentTeams);
        recyclerView.setHasFixedSize(true);
        LinearLayoutManager linearLayoutManager3 = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(linearLayoutManager3);

        showDriverButton = (Button) findViewById(R.id.showDriver);
        showDriverButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(teamsStandingsActivity.this, driversStandingsActivity.class);
                teamsStandingsActivity.this.startActivity(intent);
            }
        });

        showDriverStanding = (Button) findViewById(R.id.showStandingsDriver);
        showDriverStanding.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(teamsStandingsActivity.this, schuduleActivity.class);
                teamsStandingsActivity.this.startActivity(intent);
            }
        });

        showHomePage = (Button) findViewById(R.id.showHomePage);
        showHomePage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(teamsStandingsActivity.this, MainActivity.class);
                teamsStandingsActivity.this.startActivity(intent);
            }
        });

        showAccount = (Button) findViewById(R.id.showAccount);
        showAccount.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(teamsStandingsActivity.this, logInPageActivity.class);
                teamsStandingsActivity.this.startActivity(intent);
            }
        });


        backButton = (ImageButton) findViewById(R.id.backButton);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        LocalDate currentDate = LocalDate.now();
        datum = new ArrayList<>();
        getTeamStanding(Integer.toString(currentDate.getYear()));

        WindowInsetsControllerCompat windowInsetsController =
                WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        windowInsetsController.setAppearanceLightStatusBars(false);

    }

    public void getTeamStanding(String currentYear){
        RequestQueue queue = Volley.newRequestQueue(teamsStandingsActivity.this);
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
                                        //if(currentYear.equals("2024")){
                                        //    if (constructorName.equals("Sauber")){
                                        //        constructorName = "Kick Sauber";
                                        //    }
                                        //}
                                        DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference();
                                        rootRef.child("driverLineUp/season/" + currentYear + "/" + constructorId).addValueEventListener(new ValueEventListener() {
                                            @Override
                                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                                ArrayList<String> teamDrivers = new ArrayList<>();
                                                for (DataSnapshot driverDataSnapshot : snapshot.child("drivers").getChildren()) {
                                                    String driverFullname = driverDataSnapshot.getKey();
                                                    teamDrivers.add(driverFullname);
                                                }
                                                teamsList smth = new teamsList(constructorName, position, points, constructorId, false);
                                                smth.setDrivers(teamDrivers);
                                                datum.add(smth);
                                                adapter = new teamsStandingsAdapter(teamsStandingsActivity.this, datum);
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
                            else{
                                DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference();
                                rootRef.child("constructors").orderByChild("lastSeasonPos").addValueEventListener(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                                        ArrayList<String> firstDrivers = new ArrayList<>();
                                        firstDrivers.add("");
                                        firstDrivers.add("");
                                        teamsList first = new teamsList("","","","", true);
                                        first.setDrivers(firstDrivers);
                                        datum.add(first);
                                        for (DataSnapshot child: snapshot.getChildren()) {

                                            String constructorId = child.child("constructorId").getValue(String.class);
                                            String constructorsName = child.child("name").getValue(String.class);

                                            DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference();
                                            rootRef.child("driverLineUp/season/" + currentYear + "/" + constructorId).addValueEventListener(new ValueEventListener() {
                                                @Override
                                                public void onDataChange(@NonNull DataSnapshot snapshot) {
                                                    ArrayList<String> teamDrivers = new ArrayList<>();
                                                    for (DataSnapshot driverDataSnapshot : snapshot.child("drivers").getChildren()) {
                                                        String driverFullname = driverDataSnapshot.getKey();
                                                        teamDrivers.add(driverFullname);
                                                    }
                                                    teamsList smth = new teamsList(constructorsName, "", "", constructorId, true);
                                                    smth.setDrivers(teamDrivers);
                                                    datum.add(smth);
                                                    adapter = new teamsStandingsAdapter(teamsStandingsActivity.this, datum);
                                                    recyclerView.setAdapter(adapter);
                                                }
                                                @Override
                                                public void onCancelled(@NonNull DatabaseError error) {
                                                    Log.e("teamStandingsError", error.getMessage());
                                                }
                                            });
                                        }

                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError error) {
                                        Log.e("teamStandingsError", error.getMessage());
                                    }
                                });

                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Toast.makeText(teamsStandingsActivity.this, error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
        queue.add(jsonObjectRequest2);
    }

}
