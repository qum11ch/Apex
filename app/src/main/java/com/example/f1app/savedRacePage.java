package com.example.f1app;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class savedRacePage extends AppCompatActivity {
    Button showDriverButton, showDriverStanding, showTeams, showHomePage, showAccount, deleteRace;
    private ScrollView scrollView;
    private TextView polePosition;
    private Button saveRace;
    private ImageButton backButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.saved_race_page);
        TextView raceName = findViewById(R.id.raceName);
        TextView raceDate = findViewById(R.id.raceDate);
        TextView circuitName = findViewById(R.id.circuitName);
        TextView round = findViewById(R.id.roundCount);
        TextView country = findViewById(R.id.raceCountry);
        TextView raceWinner = findViewById(R.id.raceWinner);
        TextView winnerConstructor = findViewById(R.id.winnerConstructor);
        TextView secondPlaceDriver = findViewById(R.id.secondPlaceDriver);
        TextView secondPlaceConstructor = findViewById(R.id.secondPlaceConstructor);
        TextView thirdPlaceDriver = findViewById(R.id.thirdPlaceDriver);
        TextView thirdPlaceConstructor = findViewById(R.id.thirdPlaceConstructor);
        polePosition = (TextView)findViewById(R.id.qualy);
        ImageView imageView = findViewById(R.id.poster_image);
        scrollView = (ScrollView) findViewById(R.id.scrollView);

        Button previousGP = findViewById(R.id.previousGP);
        //saveRace = (Button) findViewById(R.id.saveRace);


        Bundle bundle = getIntent().getExtras();

        String mRaceName = bundle.getString("raceName");
        String mRaceDate = bundle.getString("raceDate");
        String mCircuitName = bundle.getString("circuitName");
        String mRound = bundle.getString("raceRound");
        String mCountry = bundle.getString("raceCountry");
        String mRaceWinner = bundle.getString("raceWinner");
        String mWinnerConstructor = bundle.getString("winnerConstructor");
        String mSecondPlaceDriver = bundle.getString("secondPlaceDriver");
        String mSecondPlaceConstructor = bundle.getString("secondPlaceConstructor");
        String mThirdPlaceDriver = bundle.getString("thirdPlaceDriver");
        String mThirdPlaceConstructor = bundle.getString("thirdPlaceConstructor");
        String mSrc = bundle.getString("src");
        String mId = bundle.getString("id");

        Log.i("mId", "" + mId);


        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String username = prefs.getString("username","");
        boolean isloged = prefs.getBoolean("Islogin", false);


        raceName.setText(mRaceName);
        raceDate.setText(mRaceDate);
        circuitName.setText(mCircuitName);
        round.setText(mRound);
        country.setText(mCountry);
        raceWinner.setText(mRaceWinner);
        winnerConstructor.setText(mWinnerConstructor);
        secondPlaceDriver.setText(mSecondPlaceDriver);
        secondPlaceConstructor.setText(mSecondPlaceConstructor);
        thirdPlaceDriver.setText(mThirdPlaceDriver);
        thirdPlaceConstructor.setText(mThirdPlaceConstructor);

        String[] splitDate = mRaceDate.split("-");

        getPole(mRound, splitDate[0]);

        GlideApp.with(this)
                .load(mSrc)
                .placeholder(R.drawable.f1)
                .into(imageView);

        showDriverButton = (Button) findViewById(R.id.showDriver);
        showDriverButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(savedRacePage.this, driversStandingsActivity.class);
                savedRacePage.this.startActivity(intent);
            }
        });

        showDriverStanding = (Button) findViewById(R.id.showStandingsDriver);
        showDriverStanding.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(savedRacePage.this, schuduleActivity.class);
                savedRacePage.this.startActivity(intent);
            }
        });

        showHomePage = (Button) findViewById(R.id.showHomePage);
        showHomePage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(savedRacePage.this, MainActivity.class);
                savedRacePage.this.startActivity(intent);
            }
        });

        showTeams = (Button) findViewById(R.id.showTeams);
        showTeams.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(savedRacePage.this, teamsStandingsActivity.class);
                savedRacePage.this.startActivity(intent);
            }
        });

        showAccount = (Button) findViewById(R.id.showAccount);
        showAccount.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(savedRacePage.this, LogInActivity.class);
                savedRacePage.this.startActivity(intent);
            }
        });

        backButton = (ImageButton) findViewById(R.id.backButton);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });


        deleteRace = (Button) findViewById(R.id.deleteRace);
        deleteRace.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Handler handler = new Handler(Looper.getMainLooper());
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        String[] field = new String[1];
                        field[0] = "id";
                        String[] data = new String[1];
                        data[0] = mId;
                        PutData putData = new PutData(
                                "http://192.168.56.1/login/deleteRace.php",
                                "POST", field, data);
                        if (putData.startPut()) {
                            if (putData.onComplete()) {
                                String result = putData.getResult();
                                if (result.equals("1")) {
                                    Toast.makeText(getApplicationContext(), "Delete success",
                                            Toast.LENGTH_LONG).show();
                                    Intent intent = new Intent(savedRacePage.this, savedRacesActivity.class);
                                    savedRacePage.this.startActivity(intent);
                                    finish();
                                } else {
                                    Toast.makeText(getApplicationContext(), result,
                                            Toast.LENGTH_LONG).show();
                                    //Log.i("resultCustom", " " + result);
                                }
                            }
                        }
                    }
                });
            }
        });
    }


    public void getPole(String roundNumber, String roundYear){
        RequestQueue queue = Volley.newRequestQueue(savedRacePage.this);
        String url2 = "https://api.jolpi.ca/ergast/f1/" + roundYear + "/" + roundNumber + "/qualifying/?format=json";
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
                            JSONArray QualifyingResults = Races.getJSONObject(0).getJSONArray("QualifyingResults");
                            JSONObject Driver = QualifyingResults.getJSONObject(0).getJSONObject("Driver");
                            String driverName = Driver.getString("givenName") + " " + Driver.getString("familyName");
                            polePosition.setText(driverName);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Toast.makeText(savedRacePage.this, error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
        queue.add(jsonObjectRequest2);
    }
}
