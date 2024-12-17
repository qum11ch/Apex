package com.example.f1app;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;


public class concludedRaceActivity extends AppCompatActivity {
    Button showDriverButton, showDriverStanding, showTeams, showHomePage, showAccount;
    ToggleButton button;
    private TextView polePosition, poleTime;
    private TableLayout tab;
    private Button saveRace;
    private ImageButton backButton;
    private LinearLayout button_layout1, button_layout2;
    List<resultsData> fullResults = new ArrayList<>();
    private String mRound = "";
    private ImageView imageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.concluded_race_page);
        TextView raceName = findViewById(R.id.raceName);
        TextView raceDate = findViewById(R.id.raceDate);
        TextView circuitName = findViewById(R.id.circuitName);
        TextView round = findViewById(R.id.roundCount);
        TextView country = findViewById(R.id.raceCountry);
        TextView raceWinner = findViewById(R.id.raceWinner);
        polePosition = (TextView)findViewById(R.id.qualy_driver);
        poleTime = (TextView) findViewById(R.id.qualy_time);
        imageView = findViewById(R.id.poster_image);
        button = findViewById(R.id.toggleButton);
        tab = (TableLayout)findViewById(R.id.tab);

        Button openSaved = findViewById(R.id.openSaved);
        Button previousGP = findViewById(R.id.previousGP);
        Button previousGP2 = findViewById(R.id.previousGP2);
        saveRace = (Button) findViewById(R.id.saveRace);
        button_layout1 = (LinearLayout) findViewById(R.id.button_layout1);
        button_layout2 = (LinearLayout) findViewById(R.id.button_layout2);

        Bundle bundle = getIntent().getExtras();

        String mCircuitId = bundle.getString("circuitId");

        previousGP.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(concludedRaceActivity.this , previousGPActivity.class);
                intent.putExtra("circuitId", mCircuitId);
                startActivity(intent);
            }
        });
        previousGP2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(concludedRaceActivity.this , previousGPActivity.class);
                intent.putExtra("circuitId", mCircuitId);
                startActivity(intent);
            }
        });

        String mRaceName = bundle.getString("raceName");
        String mRaceDate = bundle.getString("raceDate");
        String mCircuitName = bundle.getString("circuitName");
        mRound = bundle.getString("raceRound");
        String mCountry = bundle.getString("raceCountry");
        String mRaceWinner = bundle.getString("raceWinner");

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);


        String username = prefs.getString("username","");
        boolean isloged = prefs.getBoolean("Islogin", false);


        openSaved.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(concludedRaceActivity.this, savedRacesActivity.class);
                concludedRaceActivity.this.startActivity(intent);
            }
        });

        saveRace.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(isloged){
                    Handler handler = new Handler(Looper.getMainLooper());
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            String[] field = new String[14];
                            field[0] = "username";
                            field[1] = "raceName";
                            field[2] = "raceDate";
                            field[3] = "circuitName";
                            field[4] = "round";
                            field[5] = "country";
                            //field[6] = "src";
                            field[7] = "circuitId";
                            field[8] = "winnerDriver";
                            //field[9] = "winnerConstructor";
                            //field[10] = "secondDriver";
                            //field[11] = "secondConstructor";
                            //field[12] = "thirdDriver";
                            //field[13] = "thirdConstructor";
                            String[] data = new String[14];
                            data[0] = username;
                            data[1] = mRaceName;
                            data[2] = mRaceDate;
                            data[3] = mCircuitName;
                            data[4] = mRound;
                            data[5] = mCountry;
                            //data[6] = mSrc;
                            data[7] = mCircuitId;
                            data[8] = mRaceWinner;
                            //data[9] = constructorRes[0];
                            //data[10] = driversRes[1];
                            //data[11] = constructorRes[1];
                            //data[12] = driversRes[2];
                            //data[13] = constructorRes[2];
                            PutData putData = new PutData(
                                    "http://192.168.56.1/login/setRace.php",
                                    "POST", field, data);
                            if (putData.startPut()) {
                                if (putData.onComplete()) {
                                    String result = putData.getResult();
                                    if (result.equals("Success")) {
                                        Toast.makeText(getApplicationContext(), result,
                                                Toast.LENGTH_LONG).show();
                                        //Log.i("resultCustom", " " + result);
                                    } else {
                                        Toast.makeText(getApplicationContext(), "You have already save this race!",
                                                Toast.LENGTH_LONG).show();
                                        //Log.i("resultCustom", " " + result);
                                    }
                                }
                            }
                        }
                    });
                    button_layout1.setVisibility(View.INVISIBLE);
                    button_layout2.setVisibility(View.VISIBLE);
                }else{
                    Toast.makeText(getApplicationContext(),"Log In to do that!",
                            Toast.LENGTH_LONG).show();
                    button_layout2.setVisibility(View.INVISIBLE);
                    button_layout1.setVisibility(View.VISIBLE);
                }
            }
        });


        raceName.setText(mRaceName);
        raceDate.setText(mRaceDate);
        circuitName.setText(mCircuitName);
        round.setText(mRound);
        country.setText(mCountry);
        raceWinner.setText(mRaceWinner);

        getPole(mRound);
        getImage(mRaceName);

        showDriverButton = (Button) findViewById(R.id.showDriver);
        showDriverButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(concludedRaceActivity.this, driversStandingsActivity.class);
                concludedRaceActivity.this.startActivity(intent);
            }
        });

        showDriverStanding = (Button) findViewById(R.id.showStandingsDriver);
        showDriverStanding.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(concludedRaceActivity.this, schuduleActivity.class);
                concludedRaceActivity.this.startActivity(intent);
            }
        });

        showHomePage = (Button) findViewById(R.id.showHomePage);
        showHomePage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(concludedRaceActivity.this, MainActivity.class);
                concludedRaceActivity.this.startActivity(intent);
            }
        });

        showTeams = (Button) findViewById(R.id.showTeams);
        showTeams.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(concludedRaceActivity.this, teamsStandingsActivity.class);
                concludedRaceActivity.this.startActivity(intent);
            }
        });

        showAccount = (Button) findViewById(R.id.showAccount);
        showAccount.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(concludedRaceActivity.this, LogInActivity.class);
                concludedRaceActivity.this.startActivity(intent);
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

    @Override
    protected void onResume(){
        super.onResume();
        getResults(mRound, new VolleyCallback() {
            @Override
            public void onSuccess(List<resultsData> fullResults) {
                String[] positionList = new String[fullResults.size()];
                String[] driverList = new String[fullResults.size()];
                String[] constructorList = new String[fullResults.size()];
                for (int i = 0; i < fullResults.size(); i++){
                    positionList[i] = fullResults.get(i).getPositionText();
                    driverList[i] = fullResults.get(i).getDriverName();
                    constructorList[i] = fullResults.get(i).getConstructorName();
                }
                TextView winnerConstructor = findViewById(R.id.winnerConstructor);
                TextView secondPlaceDriver = findViewById(R.id.secondPlaceDriver);
                TextView secondPlaceConstructor = findViewById(R.id.secondPlaceConstructor);
                TextView thirdPlaceDriver = findViewById(R.id.thirdPlaceDriver);
                TextView thirdPlaceConstructor = findViewById(R.id.thirdPlaceConstructor);
                winnerConstructor.setText(constructorList[0]);
                secondPlaceDriver.setText(driverList[1]);
                secondPlaceConstructor.setText(constructorList[1]);
                thirdPlaceDriver.setText(driverList[2]);
                thirdPlaceConstructor.setText(constructorList[2]);
                button = findViewById(R.id.toggleButton);
                button.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if(button.isChecked()){
                            createTable(driverList, constructorList, positionList);
                        }
                        else{
                            tab.removeAllViews();
                        }
                    }
                });
            }
        });
    }

    public void getResults (String roundNumber, final VolleyCallback callback){
        RequestQueue queue = Volley.newRequestQueue(concludedRaceActivity.this);
        String url2 = "https://api.jolpi.ca/ergast/f1/2024/" + roundNumber + "/results/?format=json";
        JsonObjectRequest jsonObjectRequest2 = new JsonObjectRequest(
                Request.Method.GET,
                url2,
                null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            String driverName = "";
                            String positionText = "";
                            String constructorName = "";
                            JSONObject MRData = response.getJSONObject("MRData");
                            JSONObject RaceTable = MRData.getJSONObject("RaceTable");
                            JSONArray Races = RaceTable.getJSONArray("Races");
                            JSONArray Results = Races.getJSONObject(0).getJSONArray("Results");
                            for (int j = 0; j < Results.length(); j++)
                            {
                                positionText = Results.getJSONObject(j).getString("positionText");
                                JSONObject Driver = Results.getJSONObject(j).getJSONObject("Driver");
                                driverName = Driver.getString("givenName") + " " + Driver.getString("familyName");
                                JSONObject Constructor = Results.getJSONObject(j).getJSONObject("Constructor");
                                constructorName = Constructor.getString("name");
                                resultsData resData = new resultsData(positionText, driverName, constructorName);
                                fullResults.add(resData);
                            }
                            callback.onSuccess(fullResults);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Toast.makeText(concludedRaceActivity.this, error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
        queue.add(jsonObjectRequest2);
    }

    public void createTable(String[] driverRes, String[] constructor, String[] positionText) {
        String[] points = {"12", "10", "8", "6", "4", "2", "1"};
        for (int i = 3; i < driverRes.length; i++) {
            TableRow tr = new TableRow(concludedRaceActivity.this);

            TableLayout.LayoutParams params = new TableLayout.LayoutParams(new TableLayout.LayoutParams(
                    TableLayout.LayoutParams.MATCH_PARENT,
                    TableLayout.LayoutParams.WRAP_CONTENT));
            params.setMargins(5, 0,5,0);
            tr.setLayoutParams(params);
            tr.setWeightSum(8f);
            if (driverRes[i] != null) {
                TextView b = new TextView(concludedRaceActivity.this);
                String str = String.valueOf(positionText[i]);
                b.setLayoutParams(new TableRow.LayoutParams( 0,
                        TableRow.LayoutParams.WRAP_CONTENT, 1f));
                b.setText(str);
                b.setGravity(Gravity.CENTER);
                b.setTextColor(Color.BLACK);
                b.setTextSize(18);
                tr.addView(b);

                TextView b1 = new TextView(concludedRaceActivity.this);
                b1.setLayoutParams(new TableRow.LayoutParams( 0,
                        TableRow.LayoutParams.WRAP_CONTENT, 3f));
                b1.setGravity(Gravity.CENTER);
                b1.setTextSize(18);
                String str1 = driverRes[i];
                b1.setText(str1);
                b1.setTextColor(Color.BLACK);
                tr.addView(b1);

                TextView b2 = new TextView(concludedRaceActivity.this);
                b2.setLayoutParams(new TableRow.LayoutParams( 0,
                        TableRow.LayoutParams.WRAP_CONTENT, 3f));
                String str2 = constructor[i];
                b2.setGravity(Gravity.CENTER);
                b2.setText(str2);
                b2.setTextColor(Color.BLACK);
                b2.setTextSize(18);
                tr.addView(b2);

                TextView b3 = new TextView(concludedRaceActivity.this);
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
                    tr.setBackgroundColor(ContextCompat.getColor(concludedRaceActivity.this,
                            R.color.light_blue));
                }
                tab.addView(tr);
            }
        }
    }

    public void getImage(String raceName){
        RequestQueue volleyQueue2 = Volley.newRequestQueue(concludedRaceActivity.this);
        String url2 = "https://en.wikipedia.org/w/api.php?action=query&format=json&formatversion=2&prop=pageimages|pageterms&piprop=thumbnail&pithumbsize=600&titles="
                + raceName;
        JsonObjectRequest jsonObjectRequest2 = new JsonObjectRequest(
                Request.Method.GET,
                url2,
                null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response2) {
                        try{
                            JSONObject query = response2.getJSONObject("query");
                            JSONArray pages = query.getJSONArray("pages");
                            String source;
                            for (int i = 0; i < pages.length(); i++)
                            {
                                if(pages.getJSONObject(i).has("thumbnail"))
                                {
                                    JSONObject thumbnail = pages.getJSONObject(i).
                                            getJSONObject("thumbnail");
                                    source = thumbnail.getString("source");
                                    GlideApp.with(concludedRaceActivity.this)
                                            .load(source)
                                            .transition(DrawableTransitionOptions.withCrossFade())
                                            .error(R.drawable.f1)
                                            .into(imageView);

                                }
                                else{
                                    GlideApp.with(concludedRaceActivity.this)
                                            .load(R.drawable.f1)
                                            .transition(DrawableTransitionOptions.withCrossFade())
                                            .into(imageView);
                                }
                            }
                        }catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Toast.makeText(concludedRaceActivity.this, error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
        volleyQueue2.add(jsonObjectRequest2);
    }

    public void getPole(String roundNumber){
        RequestQueue queue = Volley.newRequestQueue(concludedRaceActivity.this);
        String url2 = "https://ergast.com/api/f1/2024/"+roundNumber+"/qualifying.json";
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
                Toast.makeText(concludedRaceActivity.this, error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
        queue.add(jsonObjectRequest2);
    }

    public interface VolleyCallback{
        void onSuccess(List<resultsData> fullResults);
    }
}
