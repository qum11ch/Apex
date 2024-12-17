package com.example.f1app;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.AuthFailureError;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class teamPageActivity extends AppCompatActivity {
    Button showDriverButton, showDriverStanding, showTeams, showHomePage, showAccount;
    private TextView teamName, location, wins, firstTeamEntry, worldChamps, polePositions,
    teamPrincipal, engine;
    private ImageView posterImage;

    private ImageButton backButton;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.team_page);


        teamName = (TextView) findViewById(R.id.teamName);
        location = (TextView) findViewById(R.id.location);
        wins = (TextView) findViewById(R.id.wins);
        firstTeamEntry = (TextView) findViewById(R.id.firstTeamEntry);
        worldChamps = (TextView) findViewById(R.id.worldChamps);
        polePositions = (TextView) findViewById(R.id.polePositions);
        teamPrincipal = (TextView) findViewById(R.id.teamPrincipal);
        engine = (TextView) findViewById(R.id.engine);
        posterImage = (ImageView) findViewById(R.id.poster_image);

        Bundle bundle = getIntent().getExtras();
        String teamNameCur = bundle.getString("teamName");

        if (teamNameCur.equals("RB F1 Team")){
            teamNameCur = "Visa Cash App RB Formula One Team";
        }
        else if (teamNameCur.equals("Sauber")){
            teamNameCur = "Stake F1 Team Kick Sauber";
        }
        teamName.setText(teamNameCur);
        requestWithSomeHttpHeaders(teamNameCur);

        showDriverButton = (Button) findViewById(R.id.showDriver);
        showDriverButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(teamPageActivity.this, driversStandingsActivity.class);
                teamPageActivity.this.startActivity(intent);
            }
        });

        showDriverStanding = (Button) findViewById(R.id.showStandingsDriver);
        showDriverStanding.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(teamPageActivity.this, schuduleActivity.class);
                teamPageActivity.this.startActivity(intent);
            }
        });

        showHomePage = (Button) findViewById(R.id.showHomePage);
        showHomePage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(teamPageActivity.this, MainActivity.class);
                teamPageActivity.this.startActivity(intent);
            }
        });

        showTeams = (Button) findViewById(R.id.showTeams);

        showAccount = (Button) findViewById(R.id.showAccount);
        showAccount.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(teamPageActivity.this, LogInActivity.class);
                teamPageActivity.this.startActivity(intent);
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

    public void requestWithSomeHttpHeaders(String teamNameEnter) {
        RequestQueue queue = Volley.newRequestQueue(teamPageActivity.this);
        String url = "https://v1.formula-1.api-sports.io/teams?search=" + teamNameEnter;
        JsonObjectRequest getRequest = new JsonObjectRequest(
                url,
                new Response.Listener<JSONObject>()
                {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            ViewGroup.LayoutParams params = posterImage.getLayoutParams();
                            params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
                            posterImage.setLayoutParams(params);
                            JSONArray response_val = response.getJSONArray("response");
                            for(int i = 0; i < response_val.length(); i++){
                                JSONObject team = response_val.getJSONObject(i);
                                teamName.setText(team.getString("name"));
                                location.setText(team.getString("base"));
                                if(team.getJSONObject("highest_race_finish").getString("position").equals("1")){
                                    wins.setText(team.getJSONObject("highest_race_finish").getString("number"));
                                }
                                else{
                                    wins.setText("0");
                                }
                                firstTeamEntry.setText(team.getString("first_team_entry"));
                                worldChamps.setText(team.getString("world_championships"));
                                polePositions.setText(team.getString("pole_positions"));
                                teamPrincipal.setText(team.getString("director"));
                                engine.setText(team.getString("engine"));

                                GlideApp.with(teamPageActivity.this)
                                        .load(team.getString("logo"))
                                        .placeholder(R.drawable.f1)
                                        .into(posterImage);
                            }

                        } catch (JSONException e) {
                            throw new RuntimeException(e);
                        }
                        //Log.i("Response", response.toString());
                    }
                },
                new Response.ErrorListener()
                {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        // TODO Auto-generated method stub
                        Log.d("ERROR","error => "+error.toString());

                    }
                }
        ) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String>  params = new HashMap<String, String>();
                params.put("x-rapidapi-key", "bc49e4741eb4558816f488a229f67b1d");
                params.put("x-rapidapi-host", "v1.formula-1.api-sports.io");

                return params;
            }
        };
        queue.add(getRequest);
    }

}
