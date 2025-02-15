package com.example.f1app;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


public class previousGPActivity extends AppCompatActivity {
    Button showDriverButton, showDriverStanding, showTeams, showHomePage, showAccount;
    private TextView raceName, raceDate, circuitName, roundCount, raceCountry, polePosition,
    raceWinnerDriver, raceWinnerConstructor, secondPlaceDriver, secondPlaceConstructor,
            thirdPlaceDriver, thirdPlaceConstructor, infoText, poleTime;
    private ImageView imageView;
    private Spinner spinner;
    private ToggleButton toggleButton;
    private TableLayout tab;
    private ConstraintLayout constraintLayout;
    private ScrollView scrollView;
    private LinearLayout button_layout1, button_layout2;
    private ImageButton backButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.previous);
        Bundle prevBundle = getIntent().getExtras();

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

        String username = prefs.getString("username","");
        boolean isloged = prefs.getBoolean("Islogin", false);

        String[] years = {"2023", "2022","2021","2020","2019","2018","2017","2016","2015","2014"};

        spinner = (Spinner) findViewById(R.id.spinner);
        raceName = (TextView) findViewById(R.id.raceName);
        raceDate = (TextView) findViewById(R.id.raceDate);
        circuitName = (TextView) findViewById(R.id.circuitName);
        roundCount = (TextView) findViewById(R.id.roundCount);
        raceCountry = (TextView) findViewById(R.id.raceCountry);
        polePosition = (TextView)findViewById(R.id.qualy_driver);
        poleTime = (TextView) findViewById(R.id.qualy_time);
        infoText = (TextView) findViewById(R.id.infoText);
        raceWinnerDriver = (TextView) findViewById(R.id.raceWinner);
        raceWinnerConstructor = (TextView) findViewById(R.id.winnerConstructor);
        secondPlaceDriver = (TextView) findViewById(R.id.secondPlaceDriver);
        secondPlaceConstructor = (TextView) findViewById(R.id.secondPlaceConstructor);
        thirdPlaceDriver = (TextView) findViewById(R.id.thirdPlaceDriver);
        thirdPlaceConstructor = (TextView) findViewById(R.id.thirdPlaceConstructor);
        imageView = (ImageView) findViewById(R.id.poster_image);
        tab = (TableLayout) findViewById(R.id.tab);
        constraintLayout = (ConstraintLayout) findViewById(R.id.raceResults_layout);

        scrollView = (ScrollView) findViewById(R.id.scrollView);
        scrollView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                return true;
            }
        });

        String mCircuitId = prevBundle.getString("circuitId");


        toggleButton = (ToggleButton) findViewById(R.id.toggleButton);

        ArrayAdapter<String> adapter = new ArrayAdapter(this, android.R.layout.simple_spinner_item,
                years);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);

        AdapterView.OnItemSelectedListener itemSelectedListener = new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String item = (String)parent.getItemAtPosition(position);
                getPreviousRaceResult(item, mCircuitId);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        };
        spinner.setOnItemSelectedListener(itemSelectedListener);


        showDriverButton = (Button) findViewById(R.id.showDriver);
        showDriverButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(previousGPActivity.this, driversStandingsActivity.class);
                previousGPActivity.this.startActivity(intent);
            }
        });

        showDriverStanding = (Button) findViewById(R.id.showStandingsDriver);
        showDriverStanding.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(previousGPActivity.this, schuduleActivity.class);
                previousGPActivity.this.startActivity(intent);
            }
        });

        showHomePage = (Button) findViewById(R.id.showHomePage);
        showHomePage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(previousGPActivity.this, MainActivity.class);
                previousGPActivity.this.startActivity(intent);
            }
        });

        showTeams = (Button) findViewById(R.id.showTeams);
        showTeams.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(previousGPActivity.this, teamsStandingsActivity.class);
                previousGPActivity.this.startActivity(intent);
            }
        });

        showAccount = (Button) findViewById(R.id.showAccount);
        showAccount.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(previousGPActivity.this, LogInActivity.class);
                previousGPActivity.this.startActivity(intent);
            }
        });

        backButton = (ImageButton) findViewById(R.id.backButton);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        WindowInsetsControllerCompat windowInsetsController =
                WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        windowInsetsController.setAppearanceLightStatusBars(false);

    }


    private void getPreviousRaceResult(String year, String circuitId){
        RequestQueue queue = Volley.newRequestQueue(previousGPActivity.this);
        String url = "https://api.jolpi.ca/ergast/f1/" + year + "/circuits/" + circuitId + "/results/?format=json";
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(
                Request.Method.GET,
                url,
                null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            JSONObject MRData = response.getJSONObject("MRData");
                            String total = MRData.getString("total");
                            if (!total.equals("0")){
                                scrollView.setOnTouchListener(null);
                                infoText.setVisibility(View.INVISIBLE);
                                constraintLayout.setVisibility(View.VISIBLE);
                                JSONObject RaceTable = MRData.getJSONObject("RaceTable");
                                JSONArray Races = RaceTable.getJSONArray("Races");
                                String prev_raceName = "";
                                String prev_raceDate = "";
                                String prev_circuitName = "";
                                String prev_roundCount = "";
                                String prev_raceCountry = "";
                                String driver;
                                String[] driversResult = new String[30];
                                String[] constructorResult = new String[30];
                                String[] positionList = new String[30];
                                for (int i = 0; i < Races.length(); i++)
                                {
                                    prev_roundCount = Races.getJSONObject(i).getString("round");
                                    prev_raceCountry = Races.getJSONObject(i).getJSONObject("Circuit")
                                            .getJSONObject("Location").getString("country") +
                                            ", " + Races.getJSONObject(i).getJSONObject("Circuit")
                                            .getJSONObject("Location").getString("locality");
                                    prev_raceName = Races.getJSONObject(i).getString("raceName");
                                    prev_circuitName = Races.getJSONObject(i).getJSONObject("Circuit")
                                            .getString("circuitName");
                                    prev_raceDate = Races.getJSONObject(i).getString("date");
                                    JSONArray Results = Races.getJSONObject(i).getJSONArray("Results");
                                    for (int j = 0; j < Results.length(); j++)
                                    {
                                        positionList[j] = Results.getJSONObject(j).getString("positionText");
                                        JSONObject Driver = Results.getJSONObject(j).getJSONObject("Driver");
                                        driver = Driver.getString("givenName") + " " + Driver.getString("familyName");
                                        driversResult[j] = driver;
                                        JSONObject Constructor = Results.getJSONObject(j).getJSONObject("Constructor");
                                        constructorResult[j] = Constructor.getString("name");
                                    }
                                    raceName.setText(prev_raceName);
                                    circuitName.setText(prev_circuitName);
                                    raceDate.setText(prev_raceDate);
                                    roundCount.setText(prev_roundCount);
                                    raceCountry.setText(prev_raceCountry);
                                    raceWinnerDriver.setText(driversResult[0]);
                                    raceWinnerConstructor.setText(constructorResult[0]);
                                    secondPlaceDriver.setText(driversResult[1]);
                                    secondPlaceConstructor.setText(constructorResult[1]);
                                    thirdPlaceDriver.setText(driversResult[2]);
                                    thirdPlaceConstructor.setText(constructorResult[2]);
                                    setImg(prev_raceName);
                                    getPole(prev_roundCount, year);
                                    toggleButton.setOnClickListener(new View.OnClickListener() {
                                        @Override
                                        public void onClick(View v) {
                                            if(toggleButton.isChecked()){
                                                createTable(driversResult, constructorResult, positionList);
                                            }
                                            else{
                                                tab.removeAllViews();
                                            }
                                        }
                                    });
                                }
                            }
                            else{
                                constraintLayout.setVisibility(View.INVISIBLE);
                                infoText.setVisibility(View.VISIBLE);
                                scrollView.setOnTouchListener(new View.OnTouchListener() {
                                    @Override
                                    public boolean onTouch(View view, MotionEvent motionEvent) {
                                        return true;
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
                Toast.makeText(previousGPActivity.this, error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
        queue.add(jsonObjectRequest);
    }

    public void setImg(String name){
        RequestQueue volleyQueue2 = Volley.newRequestQueue(previousGPActivity.this);
        String url2 = "https://en.wikipedia.org/w/api.php?action=query&format=json&formatversion=2&prop=pageimages|pageterms&piprop=thumbnail&pithumbsize=600&titles="
                + name;
        JsonObjectRequest jsonObjectRequest2 = new JsonObjectRequest(
                Request.Method.GET,
                url2,
                null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response2) {
                        try {
                            JSONObject query = response2.getJSONObject("query");
                            JSONArray pages = query.getJSONArray("pages");
                            String source;

                            for (int i = 0; i < pages.length(); i++) {
                                if (pages.getJSONObject(i).has("thumbnail")) {
                                    JSONObject thumbnail = pages.getJSONObject(i).
                                            getJSONObject("thumbnail");
                                    source = thumbnail.getString("source");
                                    GlideApp.with(previousGPActivity.this)
                                            .load(source)
                                            .placeholder(R.drawable.f1)
                                            .into(imageView);
                                } else {
                                    GlideApp.with(previousGPActivity.this)
                                            .load(R.drawable.f1)
                                            .into(imageView);
                                }
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Toast.makeText(previousGPActivity.this, error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
        volleyQueue2.add(jsonObjectRequest2);
    }

    public void getPole(String roundNumber, String year){
        RequestQueue queue = Volley.newRequestQueue(previousGPActivity.this);
        String url2 = "https://api.jolpi.ca/ergast/f1/" + year + "/"+roundNumber+"/qualifying/?format=json";
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
                            String time = QualifyingResults.getJSONObject(0).getString("Q3");
                            polePosition.setText(driverName);
                            poleTime.setText(time);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Toast.makeText(previousGPActivity.this, error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
        queue.add(jsonObjectRequest2);
    }

    public void createTable(String[] driverRes, String[] constructor, String[] positionList) {
        String[] points = {"12", "10", "8", "6", "4", "2", "1"};
        for (int i = 3; i < driverRes.length; i++) {
            TableRow tr = new TableRow(previousGPActivity.this);

            TableLayout.LayoutParams params = new TableLayout.LayoutParams(new TableLayout.LayoutParams(
                    TableLayout.LayoutParams.MATCH_PARENT,
                    TableLayout.LayoutParams.WRAP_CONTENT));
            params.setMargins(5, 0,5,0);
            tr.setLayoutParams(params);
            tr.setWeightSum(8f);
            if (driverRes[i] != null) {
                TextView b = new TextView(previousGPActivity.this);
                String str = String.valueOf(positionList[i]);
                b.setLayoutParams(new TableRow.LayoutParams( 0,
                        TableRow.LayoutParams.WRAP_CONTENT, 1f));
                b.setText(str);
                b.setGravity(Gravity.CENTER);
                b.setTextColor(Color.BLACK);
                b.setTextSize(18);
                tr.addView(b);

                TextView b1 = new TextView(previousGPActivity.this);
                b1.setLayoutParams(new TableRow.LayoutParams( 0,
                        TableRow.LayoutParams.WRAP_CONTENT, 3f));
                b1.setGravity(Gravity.CENTER);
                b1.setTextSize(18);
                String str1 = driverRes[i];
                b1.setText(str1);
                b1.setTextColor(Color.BLACK);
                tr.addView(b1);

                TextView b2 = new TextView(previousGPActivity.this);
                b2.setLayoutParams(new TableRow.LayoutParams( 0,
                        TableRow.LayoutParams.WRAP_CONTENT, 3f));
                String str2 = constructor[i];
                b2.setGravity(Gravity.CENTER);
                b2.setText(str2);
                b2.setTextColor(Color.BLACK);
                b2.setTextSize(18);
                tr.addView(b2);

                TextView b3 = new TextView(previousGPActivity.this);
                b3.setLayoutParams(new TableRow.LayoutParams( 0,
                        TableRow.LayoutParams.WRAP_CONTENT, 1f));
                String str3 = "0";
                if (i < 10){
                    str3 = points[i-3];
                }
                else {
                    str3 = "0";
                }
                b3.setGravity(Gravity.CENTER);
                b3.setText(str3);
                b3.setTextColor(Color.BLACK);
                b3.setTextSize(18);
                tr.addView(b3);

                if (i % 2 == 0) {
                    tr.setBackgroundColor(ContextCompat.getColor(previousGPActivity.this,
                            R.color.light_blue));
                }
                tab.addView(tr);
            }
        }
    }
}
