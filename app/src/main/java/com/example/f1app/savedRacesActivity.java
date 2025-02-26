package com.example.f1app;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class savedRacesActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private ImageButton backButton;
    Button showDriverButton, showDriverStanding, showTeams, showHomePage, showAccount;
    List<raceData> datum;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.saved_races);

        recyclerView = findViewById(R.id.recyclerview_currentTeams);
        recyclerView.setHasFixedSize(true);
        LinearLayoutManager linearLayoutManager3 = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(linearLayoutManager3);
        datum = new ArrayList<>();

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

        String username = prefs.getString("username","");
        getText2(username);

        showDriverButton = (Button) findViewById(R.id.showDriver);
        showDriverButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(savedRacesActivity.this, driversStandingsActivity.class);
                savedRacesActivity.this.startActivity(intent);
            }
        });

        showDriverStanding = (Button) findViewById(R.id.showStandingsDriver);
        showDriverStanding.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(savedRacesActivity.this, schuduleActivity.class);
                savedRacesActivity.this.startActivity(intent);
            }
        });

        showHomePage = (Button) findViewById(R.id.showHomePage);
        showHomePage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(savedRacesActivity.this, MainActivity.class);
                savedRacesActivity.this.startActivity(intent);
            }
        });

        showTeams = (Button) findViewById(R.id.showTeams);
        showTeams.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(savedRacesActivity.this, teamsStandingsActivity.class);
                savedRacesActivity.this.startActivity(intent);
            }
        });

        showAccount = (Button) findViewById(R.id.showAccount);
        showAccount.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(savedRacesActivity.this, logInPageActivity.class);
                savedRacesActivity.this.startActivity(intent);
            }
        });

        backButton = (ImageButton) findViewById(R.id.backButton);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }
    public void getText2(String username) {
        if (!username.equals("")) {
            Handler handler = new Handler(Looper.getMainLooper());
            handler.post(new Runnable() {
                @Override
                public void run() {
                    GetRaceData putData = new GetRaceData(
                            "http://192.168.56.1/login/getRace.php",
                            "GET", username);
                    if (putData.startPut()) {
                        if (putData.onComplete()) {
                            JSONArray resultGet = putData.getResult();
                            try {
                                for (int i = 0; i < resultGet.length(); i++){
                                    if (!resultGet.getJSONObject(i).getString("id").equals("null")) {
                                        JSONObject query = resultGet.getJSONObject(i);
                                        String id = query.getString("id");
                                        String raceName = query.getString("raceName");
                                        String raceDate = query.getString("raceDate");
                                        String circuitName = query.getString("circuitName");
                                        String round = query.getString("round");
                                        String country = query.getString("country");
                                        String src = query.getString("src");
                                        String circuitId = query.getString("circuitId");
                                        String winnerDriver = query.getString("winnerDriver");
                                        String winnerConstructor = query.getString("winnerConstructor");
                                        String secondDriver = query.getString("secondDriver");
                                        String secondConstructor = query.getString("secondConstructor");
                                        String thirdDriver = query.getString("thirdDriver");
                                        String thirdConstructor = query.getString("thirdConstructor");
                                        raceData raceData = new raceData(raceName, raceDate, circuitName, round,
                                                country, src, circuitId, winnerDriver, winnerConstructor, secondDriver,
                                                secondConstructor, thirdDriver, thirdConstructor);
                                        raceData.setId(id);
                                        datum.add(raceData);
                                    }
                                }
                                raceDataAdapter adapter = new raceDataAdapter(savedRacesActivity.this, datum);
                                recyclerView.setAdapter(adapter);
                            } catch (JSONException e) {
                                throw new RuntimeException(e);
                            }
                        }
                    }
                }
            });
        }else{
            Toast.makeText(getApplicationContext(),"All fields required!",Toast.LENGTH_SHORT).show();
        }
    }
}
