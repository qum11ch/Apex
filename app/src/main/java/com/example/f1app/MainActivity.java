package com.example.f1app;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;

import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

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


public class MainActivity extends AppCompatActivity {
    Button showDriverButton, showSchedule, showTeams, showHomePage, showAccount;
    FirebaseDatabase database;
    private static final String TAG = "FirebaseERROR";
    SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        LocalDate currentDate = LocalDate.now();
        database = FirebaseDatabase.getInstance();

        showDriverButton = (Button) findViewById(R.id.showDriver);
        showDriverButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, driversStandingsActivity.class);
                MainActivity.this.startActivity(intent);
                overridePendingTransition(0, 0);
            }
        });

        showSchedule = (Button) findViewById(R.id.showSchedule);
        showSchedule.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, schuduleActivity.class);
                MainActivity.this.startActivity(intent);
                overridePendingTransition(0, 0);
            }
        });

        showTeams = (Button) findViewById(R.id.showTeams);
        showTeams.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, teamsStandingsActivity.class);
                MainActivity.this.startActivity(intent);
                overridePendingTransition(0, 0);
            }
        });

        showAccount = (Button) findViewById(R.id.showAccount);
        showAccount.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, logInPageActivity.class);
                MainActivity.this.startActivity(intent);
                overridePendingTransition(0, 0);
            }
        });

        updateData(currentDate);
        updateSprintData(currentDate);

        WindowInsetsControllerCompat windowInsetsController =
                WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        windowInsetsController.setAppearanceLightStatusBars(false);


        if (getConnectionType(getApplicationContext())==0){
            startActivity(connectionLostScreen.createShowSplashOnNetworkFailure(MainActivity.this));
        }else{
            startActivity(connectionLostScreen.createIntentHideSplashOnNetworkRecovery(MainActivity.this));
        }
    }

    public static int getConnectionType(Context context) {
        int result = 0; // Returns connection type. 0: none; 1: mobile data; 2: wifi; 3: vpn
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm != null) {
            NetworkCapabilities capabilities = cm.getNetworkCapabilities(cm.getActiveNetwork());
            if (capabilities != null) {
                if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                    result = 2;
                } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                    result = 1;
                } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) {
                    result = 3;
                }
            }
        }
        return result;
    }

    private void updateSprintData(LocalDate currentDate){
        //String lastSprintUpdate = prefs.getString("lastSprintUpdate", "null");
        DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference();
        DatabaseReference last_sprint = rootRef.child("status/last_update");
        last_sprint.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String last_sprint_race_update = snapshot.child("last_sprint").getValue(String.class);
                String last_race_update = snapshot.child("last_race").getValue(String.class);
                String currentYear = Integer.toString(currentDate.getYear());
                RequestQueue queue = Volley.newRequestQueue(MainActivity.this);
                String url3 = "https://api.jolpi.ca/ergast/f1/" + currentYear + "/last/sprint/?format=json";
                JsonObjectRequest sprintJsonObjectRequest = new JsonObjectRequest(
                        Request.Method.GET,
                        url3,
                        null,
                        new Response.Listener<JSONObject>() {
                            @Override
                            public void onResponse(JSONObject response) {
                                try {
                                    JSONObject MRData = response.getJSONObject("MRData");
                                    JSONObject RaceTable = MRData.getJSONObject("RaceTable");
                                    JSONArray Races = RaceTable.getJSONArray("Races");
                                    for (int k = 0; k < Races.length(); k++){
                                        String sprintName = Races.getJSONObject(k).getString("season") + " " +
                                                Races.getJSONObject(k).getString("raceName");
                                        String raceName = Races.getJSONObject(k).getString("raceName");
                                        if (!last_sprint_race_update.equals(sprintName)){
                                            if (sprintName.equals(last_race_update)){
                                                //prefs.edit().putString("lastSprintUpdate", sprintName).apply();
                                                rootRef.child("status/last_update")
                                                        .child("last_sprint")
                                                        .setValue(sprintName);
                                                JSONArray SprintResults = Races.getJSONObject(k).getJSONArray("SprintResults");
                                                for (int l = 0; l < SprintResults.length(); l++){
                                                    String sprintPoints = SprintResults.getJSONObject(l).getString("points");
                                                    String sprintDriverName = SprintResults
                                                            .getJSONObject(l).getJSONObject("Driver")
                                                            .getString("givenName") + " " +
                                                            SprintResults
                                                                    .getJSONObject(l).getJSONObject("Driver")
                                                                    .getString("familyName");
                                                    String driverCode = SprintResults.getJSONObject(l).getJSONObject("Driver").getString("code");
                                                    String position = SprintResults.getJSONObject(l).getString("positionText");
                                                    DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference();
                                                    rootRef.child("drivers/" + sprintDriverName).
                                                            addListenerForSingleValueEvent(new ValueEventListener() {
                                                                @Override
                                                                public void onDataChange(@NonNull DataSnapshot snapshot) {
                                                                    String sprintPointsTotal = Double.toString(
                                                                            Double.parseDouble(Objects.requireNonNull(snapshot
                                                                                    .child("totalPoints")
                                                                                    .getValue(String.class))) + Integer.parseInt(sprintPoints));
                                                                    Log.i("sprintPointsTotal", sprintDriverName + " " + sprintPointsTotal);
                                                                    rootRef.child("drivers/" + sprintDriverName)
                                                                            .child("totalPoints")
                                                                            .setValue(sprintPointsTotal);
                                                                }
                                                                @Override
                                                                public void onCancelled(@NonNull DatabaseError error) {
                                                                    Log.e("updateSprintDataError", error.getMessage());
                                                                }
                                                            });

                                                    rootRef.child("schedule/season/" + currentYear + "/" + raceName)
                                                            .addListenerForSingleValueEvent(new ValueEventListener() {
                                                                @Override
                                                                public void onDataChange(@NonNull DataSnapshot snapshot) {
                                                                    switch (position){
                                                                        case "1":
                                                                            rootRef.child("schedule/season/"
                                                                                            + currentYear + "/" + raceName)
                                                                                    .child("Sprint/sprintWinnerCode")
                                                                                    .setValue(driverCode);
                                                                            break;
                                                                        case "2":
                                                                            rootRef.child("schedule/season/"
                                                                                            + currentYear + "/" + raceName)
                                                                                    .child("Sprint/sprintSecondCode")
                                                                                    .setValue(driverCode);
                                                                            break;
                                                                        case "3":
                                                                            rootRef.child("schedule/season/"
                                                                                            + currentYear + "/" + raceName)
                                                                                    .child("Sprint/sprintThirdCode")
                                                                                    .setValue(driverCode);
                                                                            break;
                                                                        default:
                                                                            break;
                                                                    }
                                                                }
                                                                @Override
                                                                public void onCancelled(@NonNull DatabaseError error) {
                                                                    Log.e("updateDataError", error.getMessage());
                                                                }
                                                            });
                                                }
                                            }
                                            Log.i("updateState", "Sprint data updated successfully");
                                            DateTimeFormatter formatterUpdate = DateTimeFormatter.ofPattern("d/MM/uuuu");
                                            String updateDate = currentDate.format(formatterUpdate);
                                            rootRef.child("status/last_update")
                                                    .child("update_date")
                                                    .setValue(updateDate);
                                        }else{
                                            Log.i("updateState", "Sprint data updated earlier");
                                        }
                                    }
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }

                            }
                        }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e("sprintJsonObjectRequestError:", " " + error.getMessage());
                    }
                });
                queue.add(sprintJsonObjectRequest);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void updateData(LocalDate currentDate) {
        //String lastRaceUpdate = prefs.getString("lastRaceUpdate", "null");
        DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference();
        DatabaseReference lastRaceUpdate = rootRef.child("status/last_update").child("last_race");
        lastRaceUpdate.addListenerForSingleValueEvent(
                new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        String lastRaceUpdate = snapshot.getValue(String.class);
                        String currentYear = Integer.toString(currentDate.getYear());
                        RequestQueue queue = Volley.newRequestQueue(MainActivity.this);
                        String url2 = "https://api.jolpi.ca/ergast/f1/" + currentYear + "/last/results/?format=json";
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
                                            for (int i = 0; i < Races.length(); i++){
                                                String gpName = Races.getJSONObject(i).getString("season") + " " +
                                                        Races.getJSONObject(i).getString("raceName");
                                                String raceName = Races.getJSONObject(i).getString("raceName");
                                                String circuitId = Races.getJSONObject(i).getJSONObject("Circuit")
                                                        .getString("circuitId");
                                                if (!lastRaceUpdate.equals(gpName)){
                                                    JSONArray Results = Races.getJSONObject(i).getJSONArray("Results");
                                                    for (int j = 0; j < Results.length(); j++){
                                                        String positionText = Results.getJSONObject(j).getString("positionText");
                                                        String gridText = Results.getJSONObject(j).getString("grid");
                                                        String result = gridText + "-" + positionText;
                                                        String position = Results.getJSONObject(j).getString("position");
                                                        String points = Results.getJSONObject(j).getString("points");
                                                        JSONObject Driver = Results.getJSONObject(j).getJSONObject("Driver");
                                                        String driverName = Driver.getString("givenName") + " " + Driver.getString("familyName");
                                                        String driverCode = Driver.getString("code");
                                                        
                                                        boolean hasFastestLap = false;
                                                        String rank = "", time = " ";
                                                        if (Results.getJSONObject(j).has("FastestLap")){
                                                            JSONObject FastestLap = Results.getJSONObject(j).getJSONObject("FastestLap");
                                                            rank = FastestLap.getString("rank");
                                                            time = FastestLap.getJSONObject("Time").getString("time");
                                                            hasFastestLap = true;
                                                        }
                                                        
                                                        JSONObject Constructor = Results.getJSONObject(j).getJSONObject("Constructor");
                                                        String teamName = Constructor.getString("name");
                                                        String constructorId = Constructor.getString("constructorId");

                                                        rootRef.child("status/last_update")
                                                                .child("last_race")
                                                                .setValue(gpName);

                                                        int finalJ = j;
                                                        boolean finalHasFastestLap = hasFastestLap;
                                                        String finalRank = rank;
                                                        rootRef.child("drivers/").orderByChild("driversCode").equalTo(driverCode)
                                                                .addListenerForSingleValueEvent(new ValueEventListener() {
                                                                    @Override
                                                                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                                                                        for (DataSnapshot driverSnaps: snapshot.getChildren()){
                                                                            Log.i("updateDrivers", " " + finalJ + " " + driverCode + " is updating");

                                                                            String totalWinsCount = driverSnaps
                                                                                    .child("totalWins")
                                                                                    .getValue(String.class);
                                                                            String totalPodiumsCount = driverSnaps
                                                                                    .child("totalPodiums")
                                                                                    .getValue(String.class);

                                                                            if(Integer.parseInt(position) <= 3){
                                                                                if (position.equals("1")){
                                                                                    totalWinsCount = Integer.toString(
                                                                                            Integer.parseInt(totalWinsCount) + 1);
                                                                                }
                                                                                totalPodiumsCount = Integer.toString(
                                                                                        Integer.parseInt(totalPodiumsCount) + 1);
                                                                            }

                                                                            String totalPoles = driverSnaps
                                                                                    .child("polesCount")
                                                                                    .getValue(String.class);
                                                                            
                                                                            if (gridText.equals("1")){
                                                                                totalPoles = String.valueOf(Integer.parseInt(totalPoles) + 1);
                                                                            }

                                                                            String countGPEntered = Integer.toString(
                                                                                    Integer.parseInt(Objects.requireNonNull(driverSnaps
                                                                                            .child("gpEntered")
                                                                                            .getValue(String.class))) + 1);
                                                                            String totalPointsCount = Double.toString(
                                                                                    Double.parseDouble(Objects.requireNonNull(driverSnaps
                                                                                            .child("totalPoints")
                                                                                            .getValue(String.class))) + Integer.parseInt(points));

                                                                            String fastestLapsTotal = driverSnaps
                                                                                    .child("fastestLapCount")
                                                                                    .getValue(String.class);

                                                                            if(finalHasFastestLap){
                                                                                if(finalRank.equals("1")){
                                                                                    fastestLapsTotal = String.valueOf(Integer.parseInt(fastestLapsTotal) + 1);
                                                                                }
                                                                            }
                                                                            rootRef.child("drivers/" + driverName)
                                                                                    .child("fastestLapCount")
                                                                                    .setValue(fastestLapsTotal);
                                                                            rootRef.child("drivers/" + driverName)
                                                                                    .child("totalWins")
                                                                                    .setValue(totalWinsCount);
                                                                            rootRef.child("drivers/" + driverName)
                                                                                    .child("totalPodiums")
                                                                                    .setValue(totalPodiumsCount);
                                                                            rootRef.child("drivers/" + driverName)
                                                                                    .child("gpEntered")
                                                                                    .setValue(countGPEntered);
                                                                            rootRef.child("drivers/" + driverName)
                                                                                    .child("totalPoints")
                                                                                    .setValue(totalPointsCount);

                                                                            rootRef.child("drivers/" + driverName)
                                                                                    .child("polesCount")
                                                                                    .setValue(totalPoles);
                                                                        }
                                                                    }

                                                                    @Override
                                                                    public void onCancelled(@NonNull DatabaseError error) {
                                                                        Log.e("updateDataError", error.getMessage());
                                                                    }
                                                                });

                                                        rootRef.child("drivers/" + driverName)
                                                                .child("lastEntry")
                                                                .setValue(gpName);
                                                        rootRef.child("drivers/" + driverName)
                                                                .child("driversTeam")
                                                                .setValue(teamName);

                                                        rootRef.child("constructors/" + constructorId)
                                                                .addListenerForSingleValueEvent(new ValueEventListener() {
                                                                    @Override
                                                                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                                                                        if(finalHasFastestLap) {
                                                                            if (finalRank.equals("1")) {
                                                                                String fastestLapsTotal = snapshot
                                                                                        .child("totalFastestLaps")
                                                                                        .getValue(String.class);

                                                                                fastestLapsTotal = Integer.toString(Integer.parseInt(fastestLapsTotal) + 1);

                                                                                rootRef.child("constructors/" + constructorId)
                                                                                        .child("totalFastestLaps")
                                                                                        .setValue(fastestLapsTotal);
                                                                            }
                                                                        }

                                                                        if(Integer.parseInt(position) <= 3){
                                                                            if (position.equals("1")){
                                                                                String totalWinsCount = snapshot
                                                                                        .child("totalWins")
                                                                                        .getValue(String.class);

                                                                                totalWinsCount = Integer.toString(
                                                                                        Integer.parseInt(totalWinsCount) + 1);

                                                                                rootRef.child("constructors/" + constructorId)
                                                                                        .child("totalWins")
                                                                                        .setValue(totalWinsCount);
                                                                            }
                                                                            String totalPodiumsCount = snapshot
                                                                                    .child("totalPodiums")
                                                                                    .getValue(String.class);

                                                                            totalPodiumsCount = Integer.toString(
                                                                                    Integer.parseInt(totalPodiumsCount) + 1);

                                                                            rootRef.child("constructors/" + constructorId)
                                                                                    .child("totalPodiums")
                                                                                    .setValue(totalPodiumsCount);
                                                                        }

                                                                        if (gridText.equals("1")){
                                                                            String totalPoles = snapshot
                                                                                    .child("totalPoles")
                                                                                    .getValue(String.class);

                                                                            totalPoles = Integer.toString(Integer.parseInt(totalPoles) + 1);

                                                                            rootRef.child("constructors/" + constructorId)
                                                                                    .child("totalPoles")
                                                                                    .setValue(totalPoles);
                                                                        }
                                                                    }
                                                                    @Override
                                                                    public void onCancelled(@NonNull DatabaseError error) {
                                                                        Log.e("updateDataError", error.getMessage());
                                                                    }
                                                                });

                                                        String finalTime = time;
                                                        rootRef.child("circuits/" + circuitId)
                                                                .addListenerForSingleValueEvent(new ValueEventListener() {
                                                                    @Override
                                                                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                                                                        String currentFastLap = snapshot
                                                                                .child("lapRecordTime")
                                                                                .getValue(String.class);

                                                                        if(finalHasFastestLap) {
                                                                            if (finalRank.equals("1")) {
                                                                                SimpleDateFormat formatterFastLap = new SimpleDateFormat("mm:ss.SSS");
                                                                                try {
                                                                                    Date fastestLap = formatterFastLap.parse(finalTime);
                                                                                    Date currentFastestLap = formatterFastLap.parse(currentFastLap);
                                                                                    if (fastestLap.getTime() < currentFastestLap.getTime()) {
                                                                                        rootRef.child("circuits/" + circuitId)
                                                                                                .child("lapRecordDriver")
                                                                                                .setValue(driverName);
                                                                                        rootRef.child("circuits/" + circuitId)
                                                                                                .child("lapRecordTime")
                                                                                                .setValue(finalTime);
                                                                                        rootRef.child("circuits/" + circuitId)
                                                                                                .child("lapRecordYear")
                                                                                                .setValue(currentYear);
                                                                                    }
                                                                                } catch (
                                                                                        ParseException e) {
                                                                                    Log.d("ParseExeption", "" + e);
                                                                                }
                                                                            }
                                                                        }
                                                                    }

                                                                    @Override
                                                                    public void onCancelled(@NonNull DatabaseError error) {
                                                                        Log.e("updateDataError", error.getMessage());
                                                                    }
                                                                });

                                                        rootRef.child("results/season/" + currentYear +
                                                                        "/" + driverName)
                                                                .child(raceName)
                                                                .setValue(result);

                                                        rootRef.child("schedule/season/" + currentYear + "/" + raceName)
                                                                .addListenerForSingleValueEvent(new ValueEventListener() {
                                                                    @Override
                                                                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                                                                        if(finalHasFastestLap) {
                                                                            if (finalRank.equals("1")) {
                                                                                rootRef.child("schedule/season/"
                                                                                                + currentYear + "/" + raceName)
                                                                                        .child("RaceResults/fastestLapDriver")
                                                                                        .setValue(driverName);
                                                                                rootRef.child("schedule/season/"
                                                                                                + currentYear + "/" + raceName)
                                                                                        .child("RaceResults/fastestLapTime")
                                                                                        .setValue(finalTime);
                                                                            }
                                                                        }
                                                                        switch (position){
                                                                            case "1":
                                                                                rootRef.child("schedule/season/"
                                                                                                + currentYear + "/" + raceName)
                                                                                        .child("RaceResults/raceWinnerCode")
                                                                                        .setValue(driverCode);
                                                                                break;
                                                                            case "2":
                                                                                rootRef.child("schedule/season/"
                                                                                                + currentYear + "/" + raceName)
                                                                                        .child("RaceResults/raceSecondCode")
                                                                                        .setValue(driverCode);
                                                                                break;
                                                                            case "3":
                                                                                rootRef.child("schedule/season/"
                                                                                                + currentYear + "/" + raceName)
                                                                                        .child("RaceResults/raceThirdCode")
                                                                                        .setValue(driverCode);
                                                                                break;
                                                                            default:
                                                                                break;
                                                                        }
                                                                    }

                                                                    @Override
                                                                    public void onCancelled(@NonNull DatabaseError error) {
                                                                        Log.e("updateDataError", error.getMessage());
                                                                    }
                                                                });
                                                        DateTimeFormatter formatterUpdate = DateTimeFormatter.ofPattern("d/MM/uuuu");
                                                        String updateDate = currentDate.format(formatterUpdate);
                                                        rootRef.child("status/last_update")
                                                                .child("update_date")
                                                                .setValue(updateDate);
                                                        Log.i("updateState", "Race data updated successfully");
                                                    }
                                                }
                                                else{
                                                    Log.i("updateState", "Race data updated earlier");
                                                }

                                            }
                                        } catch (JSONException e) {
                                            e.printStackTrace();
                                        }

                                    }
                                }, new Response.ErrorListener() {
                            @Override
                            public void onErrorResponse(VolleyError error) {
                                Toast.makeText(MainActivity.this, error.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        });
                        queue.add(jsonObjectRequest2);
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e("updateRace_firebase", error.getMessage());
                    }
                }
        );
    }

    public String loadJSONFromAsset() {
        String json = null;
        try {
            InputStream is = getApplicationContext().getAssets().open("f1-circuits.geojson");
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            json = new String(buffer, "UTF-8");
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
        return json;
    }


}