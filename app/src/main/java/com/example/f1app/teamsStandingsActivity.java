package com.example.f1app;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class teamsStandingsActivity extends AppCompatActivity {
    Button showDriverButton, showDriverStanding, showTeams, showHomePage, showAccount;

    private List<teamsList> datum;
    private RecyclerView recyclerView;
    private ImageButton backButton;

    private teamsAdapter adapter;

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
                Intent intent = new Intent(teamsStandingsActivity.this, LogInActivity.class);
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

    }

    public void getTeamStanding(String currentYear){
        getDriverStandings(currentYear, new VolleyCallback() {
            @Override
            public void onSuccess(HashMap<String, String> res) {
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
                                    JSONObject StandingsTable = MRData.getJSONObject("StandingsTable");
                                    JSONArray StandingsLists = StandingsTable.getJSONArray("StandingsLists");
                                    for(int i = 0; i < StandingsLists.length(); i++){
                                        JSONArray ConstructorStandings = StandingsLists.getJSONObject(i)
                                                .getJSONArray("ConstructorStandings");
                                        for(int j = 0; j < ConstructorStandings.length(); j++){
                                            ArrayList<String> teamDrivers = new ArrayList<>();
                                            String constructorName = ConstructorStandings.getJSONObject(j)
                                                    .getJSONObject("Constructor").getString("name");
                                            String position = ConstructorStandings.getJSONObject(j).getString("positionText");
                                            String points = ConstructorStandings.getJSONObject(j).getString("points");
                                            String constructorId = ConstructorStandings.getJSONObject(j)
                                                    .getJSONObject("Constructor").getString("constructorId");
                                            for(Map.Entry<String, String> entry: res.entrySet()){
                                                if(entry.getValue().equals(constructorName)){
                                                    teamDrivers.add(entry.getKey());
                                                }
                                            }
                                            if(currentYear.equals("2024")){
                                                switch (constructorName){
                                                    case ("Haas F1 Team"):
                                                        teamDrivers.clear();
                                                        teamDrivers.add("Nico Hülkenberg");
                                                        teamDrivers.add("Kevin Magnussen");
                                                        break;
                                                    case ("RB F1 Team"):
                                                        teamDrivers.clear();
                                                        teamDrivers.add("Yuki Tsunoda");
                                                        teamDrivers.add("Liam Lawson");
                                                        break;
                                                    case ("Williams"):
                                                        teamDrivers.clear();
                                                        teamDrivers.add("Alexander Albon");
                                                        teamDrivers.add("Franco Colapinto");
                                                }
                                                if (constructorName.equals("Sauber")){
                                                    constructorName = "Kick Sauber";
                                                }
                                            }
                                            teamsList smth = new teamsList(constructorName, position, points, constructorId);
                                            smth.setDrivers(teamDrivers);
                                            datum.add(smth);
                                        }
                                    }
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                                adapter = new teamsAdapter(teamsStandingsActivity.this, datum);
                                recyclerView.setAdapter(adapter);
                            }
                        }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Toast.makeText(teamsStandingsActivity.this, error.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
                queue.add(jsonObjectRequest2);
            }
        });
    }

    public void getDriverStandings(String currentYear, VolleyCallback volleyCallback2){
        RequestQueue queue = Volley.newRequestQueue(teamsStandingsActivity.this);
        String url2 = "https://api.jolpi.ca/ergast/f1/" + currentYear + "/driverstandings/?format=json";
        JsonObjectRequest jsonObjectRequest2 = new JsonObjectRequest(
                Request.Method.GET,
                url2,
                null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            JSONObject MRData = response.getJSONObject("MRData");
                            JSONObject StandingsTable = MRData.getJSONObject("StandingsTable");
                            JSONArray StandingsLists = StandingsTable.getJSONArray("StandingsLists");
                            HashMap<String, String> driver = new HashMap<>();
                            for (int i = 0; i < StandingsLists.length(); i++) {
                                JSONArray DriverStandings = StandingsLists.getJSONObject(i)
                                        .getJSONArray("DriverStandings");
                                for (int j = 0; j < DriverStandings.length(); j++) {
                                    String driverName = DriverStandings.getJSONObject(j)
                                            .getJSONObject("Driver").getString("givenName") + " " +
                                            DriverStandings.getJSONObject(j)
                                                    .getJSONObject("Driver").getString("familyName");
                                    JSONArray Constructors = DriverStandings.getJSONObject(j).getJSONArray("Constructors");
                                    String constructorsName = Constructors.getJSONObject(Constructors.length() - 1).getString("name");
                                    driver.put(driverName, constructorsName);
                                }
                            }
                            volleyCallback2.onSuccess(driver);
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

    public interface VolleyCallback{
        void onSuccess(HashMap<String, String> res);
    }
}
