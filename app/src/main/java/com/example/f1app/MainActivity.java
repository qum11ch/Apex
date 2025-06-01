package com.example.f1app;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Build;
import android.os.Bundle;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.reflect.Field;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.facebook.shimmer.ShimmerFrameLayout;
import com.google.android.material.appbar.AppBarLayout;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


public class MainActivity extends AppCompatActivity {
    Button showDriverButton, showSchedule, showTeams, showAccount;
    FirebaseDatabase database;
    private LinearLayout futureLayout, pastLayout;
    public static final String APP_PREFERENCES = "mysettings";
    private List<driversList> datumDrivers;
    private List<teamsList> datumTeams;
    private List<concludedRacesData> datumPast;
    private List<futureRaceData> datumFuture;
    private RecyclerView rvFuture, rvPast, rvDrivers, rvTeams;
    private ShimmerFrameLayout sfFuture, sfPast, sfDrivers, sfTeams, sfProgressBar;
    private ProgressBar raceProgress;
    private TextView raceProgressText;
    SharedPreferences mPrefs;
    private static final long HOUR = 3600*1000;
    private static final long SPRINT_QUALI_DIFF = 44*60*1000;
    private static final int REQUEST_CODE_NOTIFICATIONS = 1001;
    private static final String KEY_NOTIFICATIONS_ENABLED = "notifications_enabled";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (!checkConnection(this)){
            startActivity(connectionLostScreen.createShowSplashOnNetworkFailure(MainActivity.this));
        }else{
            startActivity(connectionLostScreen.createIntentHideSplashOnNetworkRecovery(MainActivity.this));
        }

        LocalDate currentDate = LocalDate.now();
        database = FirebaseDatabase.getInstance();
        datumDrivers = new ArrayList<>();
        datumTeams = new ArrayList<>();

        futureLayout = findViewById(R.id.main_future);
        pastLayout = findViewById(R.id.main_past);

        rvFuture = findViewById(R.id.recyclerView_future);
        rvPast = findViewById(R.id.recyclerView_past);
        rvDrivers = findViewById(R.id.recyclerView_drivers);
        rvTeams = findViewById(R.id.recyclerView_teams);
        raceProgress = findViewById(R.id.race_progress);
        raceProgressText = findViewById(R.id.race_progress_text);

        sfFuture = findViewById(R.id.shimmerFuture_layout);
        sfPast = findViewById(R.id.shimmerPast_layout);
        sfDrivers = findViewById(R.id.shimmerDrivers_layout);
        sfTeams = findViewById(R.id.shimmerTeams_layout);
        sfProgressBar = findViewById(R.id.shimmerProgress_layout);

        Button predict = findViewById(R.id.predict);

        predict.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, predictPageActivity.class);
            MainActivity.this.startActivity(intent);

        });

        sfProgressBar.startShimmer();
        sfFuture.startShimmer();
        sfPast.startShimmer();
        sfDrivers.startShimmer();
        sfTeams.startShimmer();

        rvFuture.setHasFixedSize(true);
        rvPast.setHasFixedSize(true);
        rvDrivers.setHasFixedSize(true);
        rvTeams.setHasFixedSize(true);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        rvFuture.setLayoutManager(linearLayoutManager);
        LinearLayoutManager linearLayoutManager2 = new LinearLayoutManager(this);
        rvPast.setLayoutManager(linearLayoutManager2);
        LinearLayoutManager linearLayoutManager3 = new LinearLayoutManager(this);
        rvDrivers.setLayoutManager(linearLayoutManager3);
        LinearLayoutManager linearLayoutManager4 = new LinearLayoutManager(this);
        rvTeams.setLayoutManager(linearLayoutManager4);


        showDriverButton = findViewById(R.id.showDriver);
        showDriverButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, driversStandingsActivity.class);
            MainActivity.this.startActivity(intent);
            overridePendingTransition(0, 0);
        });

        showSchedule = findViewById(R.id.showSchedule);
        showSchedule.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, scheduleActivity.class);
            MainActivity.this.startActivity(intent);
            overridePendingTransition(0, 0);
        });

        showTeams = findViewById(R.id.showTeams);
        showTeams.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, teamsStandingsActivity.class);
            MainActivity.this.startActivity(intent);
            overridePendingTransition(0, 0);
        });

        showAccount = findViewById(R.id.showAccount);
        showAccount.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, logInPageActivity.class);
            MainActivity.this.startActivity(intent);
            overridePendingTransition(0, 0);
        });

        String currentYear = Integer.toString(currentDate.getYear());
        getTeamStanding(currentYear);
        getDriversStanding(currentYear);
        getSchedule(currentYear, currentDate);

        AppBarLayout appBarLayout = findViewById(R.id.appbar);
        appBarLayout.setExpanded(true,true);

        mPrefs = getSharedPreferences(APP_PREFERENCES, Activity.MODE_PRIVATE);
        checkNotificationPermission();
    }

    private void checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                if (!ActivityCompat.shouldShowRequestPermissionRationale(this,
                        Manifest.permission.POST_NOTIFICATIONS)) {
                    ActivityCompat.requestPermissions(this,
                            new String[]{Manifest.permission.POST_NOTIFICATIONS},
                            REQUEST_CODE_NOTIFICATIONS);
                }
            } else {
                saveNotificationPreference(true);
            }
        } else {
            saveNotificationPreference(true);
        }
    }


    private void saveNotificationPreference(boolean isEnabled) {
        mPrefs.edit().putBoolean(KEY_NOTIFICATIONS_ENABLED, isEnabled).apply();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_CODE_NOTIFICATIONS) {
            saveNotificationPreference(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED);
        }
    }

    private void postProgress(int progress) {
        String strProgress = progress + " из 24";
        if (progress>10 && progress<=13){
            raceProgressText.setTextColor(getColor(R.color.dark_grey));
        } else if (progress>13) {
            raceProgressText.setTextColor(getColor(R.color.white));
        }
        Handler handler = new Handler();
        handler.postDelayed(()->{
            raceProgress.setVisibility(View.VISIBLE);
            sfProgressBar.setVisibility(View.GONE);
            sfProgressBar.stopShimmer();

            raceProgress.setProgress(progress);
            raceProgressText.setText(strProgress);
        },500);
    }

    private void getDriversStanding(String currentYear){
        RequestQueue queue = Volley.newRequestQueue(MainActivity.this);
        String url2 = "https://api.jolpi.ca/ergast/f1/" + currentYear + "/driverstandings/?format=json";
        JsonObjectRequest jsonObjectRequest2 = new JsonObjectRequest(
                Request.Method.GET,
                url2,
                null,
                response -> {
                    try {
                        JSONObject MRData = response.getJSONObject("MRData");
                        String total = MRData.getString("total");
                        if (!total.equals("0")){
                            JSONObject StandingsTable = MRData.getJSONObject("StandingsTable");
                            JSONArray StandingsLists = StandingsTable.getJSONArray("StandingsLists");
                            for(int i = 0; i < StandingsLists.length(); i++){
                                JSONArray DriverStandings = StandingsLists.getJSONObject(i)
                                        .getJSONArray("DriverStandings");
                                for(int j = 0; j < 3; j++) {
                                    String placement = DriverStandings.getJSONObject(j).getString("positionText");
                                    String points = DriverStandings.getJSONObject(j).getString("points");
                                    String driverName = DriverStandings.getJSONObject(j)
                                            .getJSONObject("Driver").getString("givenName");
                                    String driverFamilyName = DriverStandings.getJSONObject(j)
                                            .getJSONObject("Driver").getString("familyName");
                                    String driverCode = DriverStandings.getJSONObject(j)
                                            .getJSONObject("Driver").getString("code");
                                    JSONArray Constructors = DriverStandings.getJSONObject(j).getJSONArray("Constructors");
                                    String constructorsName = Constructors.getJSONObject(Constructors.length() - 1).getString("name");
                                    String constructorId = Constructors.getJSONObject(Constructors.length() - 1).getString("constructorId");
                                    driversList smth = new driversList(driverName, driverFamilyName, constructorsName, constructorId, points, placement, driverCode, false, currentYear);
                                    datumDrivers.add(smth);
                                }
                            }
                            Handler handler = new Handler();
                            handler.postDelayed(()->{
                                rvDrivers.setVisibility(View.VISIBLE);
                                sfDrivers.setVisibility(View.GONE);
                                sfDrivers.stopShimmer();
                            },500);
                            mainDriversStandingsAdapter adapter = new mainDriversStandingsAdapter(MainActivity.this, datumDrivers);
                            rvDrivers.setAdapter(adapter);
                        }else{
                            DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference();
                            rootRef.child("constructors").orderByChild("lastSeasonPos").limitToFirst(3).addValueEventListener(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot snapshot) {
                                    datumDrivers.add(new driversList("","","","","","","",
                                            true, currentYear));
                                    for (DataSnapshot child: snapshot.getChildren()) {
                                        String constructorId = child.child("constructorId").getValue(String.class);
                                        String constructorsName = child.child("name").getValue(String.class);

                                        rootRef.child("driverLineUp/season/" + currentYear + "/" + constructorId).addValueEventListener(new ValueEventListener() {
                                            @Override
                                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                                for (DataSnapshot driverDataSnapshot : snapshot.child("drivers").getChildren()) {
                                                    String driverFullname = driverDataSnapshot.getKey();
                                                    DatabaseReference driversRef = rootRef.child("drivers");
                                                    DatabaseReference driverRef = driversRef.child(driverFullname);

                                                    ValueEventListener driversValueEventListener = new ValueEventListener() {
                                                        @Override
                                                        public void onDataChange(DataSnapshot dataSnapshot) {
                                                            String[] parts = driverFullname.split(" ");
                                                            String driverName, driverFamilyName;
                                                            if(driverFullname.equals("Andrea Kimi Antonelli")){
                                                                driverName = parts[0] + " " + parts[1];
                                                                driverFamilyName = parts[2];
                                                            }else{
                                                                driverName = parts[0];
                                                                driverFamilyName = parts[1];
                                                            }
                                                            String driverCode = dataSnapshot.child("driversCode").getValue(String.class);
                                                            driversList smth = new driversList(driverName, driverFamilyName, constructorsName, constructorId, "", "", driverCode,
                                                                    true, currentYear);
                                                            datumDrivers.add(smth);
                                                            mainDriversStandingsAdapter adapter = new mainDriversStandingsAdapter(MainActivity.this, datumDrivers);
                                                            Handler handler = new Handler();
                                                            handler.postDelayed(()->{
                                                                rvDrivers.setVisibility(View.VISIBLE);
                                                                sfDrivers.setVisibility(View.GONE);
                                                                sfDrivers.stopShimmer();
                                                            },500);
                                                            rvDrivers.setAdapter(adapter);
                                                        }

                                                        @Override
                                                        public void onCancelled(@NonNull DatabaseError databaseError) {
                                                            Log.e("driverStandingsError", databaseError.getMessage()); //Don't ignore errors!
                                                        }
                                                    };
                                                    driverRef.addListenerForSingleValueEvent(driversValueEventListener);
                                                }
                                            }

                                            @Override
                                            public void onCancelled(@NonNull DatabaseError error) {
                                                Log.e("driverStandingsError", error.getMessage()); //Don't ignore errors!
                                            }
                                        });
                                    }
                                }
                                @Override
                                public void onCancelled(@NonNull DatabaseError error) {
                                    Log.e("driverStandingsError", error.getMessage()); //Don't ignore errors!
                                }
                            });
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }, error -> Toast.makeText(MainActivity.this, error.getMessage(), Toast.LENGTH_SHORT).show());
        queue.add(jsonObjectRequest2);
    }

    private void getTeamStanding(String currentYear){
        RequestQueue queue = Volley.newRequestQueue(MainActivity.this);
        String url2 = "https://api.jolpi.ca/ergast/f1/" + currentYear + "/constructorstandings/?format=json";
        JsonObjectRequest jsonObjectRequest2 = new JsonObjectRequest(
                Request.Method.GET,
                url2,
                null,
                response -> {
                    try {
                        JSONObject MRData = response.getJSONObject("MRData");
                        String total = MRData.getString("total");
                        if (!total.equals("0")){
                            JSONObject StandingsTable = MRData.getJSONObject("StandingsTable");
                            JSONArray StandingsLists = StandingsTable.getJSONArray("StandingsLists");
                            for(int i = 0; i < StandingsLists.length(); i++){
                                JSONArray ConstructorStandings = StandingsLists.getJSONObject(i)
                                        .getJSONArray("ConstructorStandings");
                                for(int j = 0; j < 3; j++){
                                    String constructorName = ConstructorStandings.getJSONObject(j)
                                            .getJSONObject("Constructor").getString("name");
                                    String position = ConstructorStandings.getJSONObject(j).getString("positionText");
                                    String points = ConstructorStandings.getJSONObject(j).getString("points");
                                    String constructorId = ConstructorStandings.getJSONObject(j)
                                            .getJSONObject("Constructor").getString("constructorId");
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
                                            datumTeams.add(smth);
                                            Handler handler = new Handler();
                                            handler.postDelayed(()->{
                                                rvTeams.setVisibility(View.VISIBLE);
                                                sfTeams.setVisibility(View.GONE);
                                                sfTeams.stopShimmer();
                                            },500);
                                            mainTeamsStandingsAdapter adapter = new mainTeamsStandingsAdapter(MainActivity.this, datumTeams);
                                            rvTeams.setAdapter(adapter);
                                        }

                                        @Override
                                        public void onCancelled(@NonNull DatabaseError error) {
                                            Log.e("MainActivityTeams", error.getMessage());
                                        }
                                    });
                                }
                            }

                        }
                        else{
                            DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference();
                            rootRef.child("constructors").orderByChild("lastSeasonPos").limitToFirst(3).addValueEventListener(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot snapshot) {
                                    ArrayList<String> firstTeam = new ArrayList<>();
                                    firstTeam.add("");
                                    firstTeam.add("");
                                    teamsList first = new teamsList("","","","", true);
                                    first.setDrivers(firstTeam);
                                    datumTeams.add(first);

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
                                                Handler handler = new Handler();
                                                handler.postDelayed(()->{
                                                    rvTeams.setVisibility(View.VISIBLE);
                                                    sfTeams.setVisibility(View.GONE);
                                                    sfTeams.stopShimmer();
                                                },500);
                                                datumTeams.add(smth);
                                                mainTeamsStandingsAdapter adapter = new mainTeamsStandingsAdapter(MainActivity.this, datumTeams);
                                                rvTeams.setAdapter(adapter);
                                            }
                                            @Override
                                            public void onCancelled(@NonNull DatabaseError error) {
                                                Log.e("MainActivityTeams", error.getMessage());
                                            }
                                        });
                                    }

                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError error) {
                                    Log.e("MainActivityTeams", error.getMessage());
                                }
                            });

                        }
                    } catch (JSONException e) {
                        Log.e("MainActivityTeams", " " + e.getMessage());
                    }
                }, error -> Toast.makeText(MainActivity.this, error.getMessage(), Toast.LENGTH_SHORT).show());
        queue.add(jsonObjectRequest2);
    }


    private void getSchedule(String year, LocalDate currentDate){
        ArrayList<String> concludedRoundNumber = new ArrayList<>();
        ArrayList<String> futureRaceRoundNumber = new ArrayList<>();
        ArrayList<String> raceNames = new ArrayList<>();
        ArrayList<String> circuitsId = new ArrayList<>();
        DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference();
        rootRef.child("schedule/season/" + year).orderByChild("round").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot ds : snapshot.getChildren()) {
                    Integer round = ds.child("round").getValue(Integer.class);
                    String dateStart = ds.child("FirstPractice/firstPracticeDate").getValue(String.class);
                    String dateEnd = ds.child("raceDate").getValue(String.class);

                    String raceName = ds.child("Circuit/raceName").getValue(String.class);
                    String circuitId = ds.child("Circuit/circuitId").getValue(String.class);

                    raceNames.add(raceName);
                    circuitsId.add(circuitId);

                    String currentDateString = currentDate.toString();
                    SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                    boolean future = false;
                    boolean isOnGoing = false;
                    boolean concluded = true;

                    try{
                        Date start = formatter.parse(dateStart);
                        Date end = formatter.parse(dateEnd);
                        Date current = formatter.parse(currentDateString);
                        if(start.after(current) && end.after(current))
                        {
                            concluded = false;
                            future = true;
                        }
                        else if (current.equals(start) || current.equals(end)){
                            concluded = false;
                            isOnGoing = true;
                        }
                        else if (current.after(start) && current.before(end)){
                            concluded = false;
                            isOnGoing = true;
                        }
                    } catch (ParseException e){
                        Log.d("ParseExeption", "" + e);
                    }
                    if(concluded){
                        concludedRoundNumber.add(round.toString());
                    }
                    if (future || isOnGoing){
                        String newRound;
                        if (isOnGoing){
                           newRound = round + " " + getString(R.string.is_ongoing);
                        }else{
                            newRound = round.toString();
                        }
                        futureRaceRoundNumber.add(newRound.toUpperCase());
                    }
                }
                if(!concludedRoundNumber.isEmpty()){
                    pastLayout.setVisibility(View.VISIBLE);
                    getPastRace(year, concludedRoundNumber.get(concludedRoundNumber.size()-1));
                    postProgress(Integer.parseInt(concludedRoundNumber.get(concludedRoundNumber.size()-1)));
                }else{
                    pastLayout.setVisibility(View.GONE);
                }
                if(!futureRaceRoundNumber.isEmpty()){
                    futureLayout.setVisibility(View.VISIBLE);
                    getFutureRace(year, futureRaceRoundNumber.get(0));
                }else{
                    futureLayout.setVisibility(View.GONE);
                }

                boolean notificationsEnabled = mPrefs.getBoolean(KEY_NOTIFICATIONS_ENABLED, false);

                if(notificationsEnabled){
                    getRaceSchedule(raceNames, year, circuitsId);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("scheduleActivityFirebaseError", error.getMessage());
            }
        });
    }

    private void getPastRace(String currentYear, String round){
        DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference();
        rootRef.child("schedule/season/" + currentYear).orderByChild("round").equalTo(Integer.parseInt(round)).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                datumPast = new ArrayList<>();
                for(DataSnapshot ds : snapshot.getChildren()) {
                    String winnerCode = ds.child("RaceResults/raceWinnerCode").getValue(String.class);
                    if (!winnerCode.equals("N/A")){
                        String raceName = ds.child("Circuit/raceName").getValue(String.class);
                        String dateStart = ds.child("FirstPractice/firstPracticeDate").getValue(String.class);
                        String dateEnd = ds.child("raceDate").getValue(String.class);
                        String circuitId = ds.child("Circuit/circuitId").getValue(String.class);
                        String secondCode = ds.child("RaceResults/raceSecondCode").getValue(String.class);
                        String thirdCode = ds.child("RaceResults/raceThirdCode").getValue(String.class);
                        rootRef.child("circuits/" + circuitId).addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                String circuitName = dataSnapshot.child("circuitName").getValue(String.class);
                                String raceCountry = dataSnapshot.child("country").getValue(String.class);
                                String raceLocation = dataSnapshot.child("location").getValue(String.class);

                                concludedRacesData concludedRace = new concludedRacesData(dateStart,
                                        dateEnd, raceName, round, circuitName, raceCountry, raceLocation, winnerCode, secondCode,
                                        thirdCode, currentYear);
                                datumPast.add(concludedRace);
                                Handler handler = new Handler();
                                handler.postDelayed(()->{
                                    rvPast.setVisibility(View.VISIBLE);
                                    sfPast.setVisibility(View.GONE);
                                    sfPast.stopShimmer();
                                },500);
                                mainPastRaceAdapter adapter = new mainPastRaceAdapter(MainActivity.this, datumPast);
                                rvPast.setAdapter(adapter);
                            }
                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {
                                Log.e("MainActivityTeams", error.getMessage());
                            }
                        });
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("MainActivityTeams", error.getMessage());
            }
        });
    }

    private void getFutureRace(String currentYear, String round){
        DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference();
        String newRound;
        if (round.contains(getString(R.string.is_ongoing).toUpperCase())){
            String[] roundArray = round.split("\\s+");
            newRound = roundArray[0];
        }else{
            newRound = round;
        }
        rootRef.child("schedule/season/" + currentYear).orderByChild("round")
                .equalTo(Integer.parseInt(newRound)).addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        datumFuture = new ArrayList<>();
                        for(DataSnapshot ds : dataSnapshot.getChildren()) {
                            String raceName = ds.child("Circuit/raceName").getValue(String.class);
                            String dateStart = ds.child("FirstPractice/firstPracticeDate").getValue(String.class);
                            String dateEnd = ds.child("raceDate").getValue(String.class);
                            String circuitId = ds.child("Circuit/circuitId").getValue(String.class);

                            rootRef.child("circuits/" + circuitId).addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                    String circuitName = dataSnapshot.child("circuitName").getValue(String.class);
                                    String raceCountry = dataSnapshot.child("country").getValue(String.class);
                                    String raceLocation = dataSnapshot.child("location").getValue(String.class);
                                    futureRaceData futureRaceData = new futureRaceData(raceName, dateStart, dateEnd,
                                            circuitName, round, raceCountry, circuitId);
                                    futureRaceData.setLocality(raceLocation);
                                    datumFuture.add(futureRaceData);
                                    Handler handler = new Handler();
                                    handler.postDelayed(()->{
                                        rvFuture.setVisibility(View.VISIBLE);
                                        sfFuture.setVisibility(View.GONE);
                                        sfFuture.stopShimmer();
                                    },500);
                                    futureRaceAdapter adapter = new futureRaceAdapter(MainActivity.this, datumFuture);
                                    rvFuture.setAdapter(adapter);
                                }
                                @Override
                                public void onCancelled(@NonNull DatabaseError error) {
                                    Log.e("futureRaceFragmentFirebaseError", error.getMessage());
                                }
                            });
                        }

                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e("futureRaceFragmentFirebaseError", error.getMessage());
                    }
                });
    }

    private void getRaceSchedule(ArrayList<String> raceNames, String currentYear, ArrayList<String> circuitsId){
        ArrayList<LinkedHashMap<String, String>> eventsPerRace = new ArrayList<>();
        for (int i = 0; i < raceNames.size(); i++) {
            String raceName = raceNames.get(i);
            LinkedHashMap<String, String> eventsCountdown = new LinkedHashMap<>();
            DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference();
            int finalI = i;
            rootRef.child("/schedule/season/" + currentYear + "/" + raceName).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    String firstPractice = snapshot.child("FirstPractice/firstPracticeDate").getValue(String.class) +
                            " " + snapshot.child("FirstPractice/firstPracticeTime").getValue(String.class);
                    String race = snapshot.child("raceDate").getValue(String.class) +
                            " " + snapshot.child("raceTime").getValue(String.class);

                    String raceQuali = snapshot.child("Qualifying/raceQualiDate").getValue(String.class) +
                            " " + snapshot.child("Qualifying/raceQualiTime").getValue(String.class);

                    String sprintDate = snapshot.child("Sprint/sprintRaceDate").getValue(String.class);
                    if (sprintDate.equals("N/A")){
                        String secondPractice = snapshot.child("SecondPractice/secondPracticeDate").getValue(String.class) +
                                " " + snapshot.child("SecondPractice/secondPracticeTime").getValue(String.class);

                        String thirdPractice = snapshot.child("ThirdPractice/thirdPracticeDate").getValue(String.class) +
                                " " + snapshot.child("ThirdPractice/thirdPracticeTime").getValue(String.class);

                        eventsCountdown.put("first_practice_event", firstPractice);
                        eventsCountdown.put("second_practice_event", secondPractice);
                        eventsCountdown.put("third_practice_event", thirdPractice);
                        eventsCountdown.put("quali_event", raceQuali);
                        eventsCountdown.put("race_event", race);
                    }else{
                        String sprintQuali = snapshot.child("SprintQualifying/sprintQualiDate").getValue(String.class) +
                                " " + snapshot.child("SprintQualifying/sprintQualiTime").getValue(String.class);
                        String sprint = sprintDate +
                                " " + snapshot.child("Sprint/sprintRaceTime").getValue(String.class);

                        eventsCountdown.put("first_practice_event", firstPractice);
                        eventsCountdown.put("sprint_quali_event", sprintQuali);
                        eventsCountdown.put("sprint_event", sprint);
                        eventsCountdown.put("quali_event", raceQuali);
                        eventsCountdown.put("race_event", race);
                    }
                    eventsPerRace.add(eventsCountdown);
                    if(finalI == raceNames.size() - 1){
                        scheduleNotification(eventsPerRace, raceNames, currentYear, circuitsId);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e("futureActivityFirebaseError", error.getMessage());
                }
            });

        }
    }

    private void scheduleNotification(ArrayList<LinkedHashMap<String, String>> eventsPerRace, ArrayList<String> raceNames, String currentYear, ArrayList<String> circuitsId) {
        ArrayList<String> eventsStr = new ArrayList<>();
        SharedPreferences.Editor editor = mPrefs.edit();
        for (int i = 0; i < raceNames.size(); i++){
            LinkedHashMap<String, String> events = eventsPerRace.get(i);
            String raceName = raceNames.get(i);
            if (!events.isEmpty()) {

                String stringForSharedPrefs = currentYear + "$" + raceName;
                for(Map.Entry<String, String> entry : events.entrySet()){
                    String key = entry.getKey();
                    String value = entry.getValue();
                    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                    dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
                    try {
                        Date eventStart_date = dateFormat.parse(value);
                        Date endTime = new Date(eventStart_date.getTime() + HOUR);
                        String endTimeStr = dateFormat.format(endTime);
                        Date endTime_sprintQ = new Date(eventStart_date.getTime() + SPRINT_QUALI_DIFF);
                        String endTimeStr_sprintQ = dateFormat.format(endTime_sprintQ);
                        Date endTime_race = new Date(eventStart_date.getTime() + 2 * HOUR);
                        String endTimeStr_race = dateFormat.format(endTime_race);
                        String eventEndStr_date;
                        switch (key) {
                            case "race_event":
                                eventEndStr_date = endTimeStr_race + "Z";
                                break;
                            case "sprint_quali_event":
                                eventEndStr_date = endTimeStr_sprintQ + "Z";
                                break;
                            default:
                                eventEndStr_date = endTimeStr + "Z";
                                break;
                        }
                        stringForSharedPrefs += "%" + key + "$" + value + "$" + eventEndStr_date;

                    }catch(ParseException e){
                        e.printStackTrace();
                    }
                }
                eventsStr.add(stringForSharedPrefs);
            }

        }
        JSONArray jsonArrayEvents = new JSONArray(eventsStr);
        JSONArray jsonArrayCircuits = new JSONArray(circuitsId);
        editor.putString("events_json", jsonArrayEvents.toString());
        editor.putString("circuits_json", jsonArrayCircuits.toString());
        editor.apply();

        Intent intentStart = new Intent(this, BootService.class);
        String channelId = "channelID2";
        intentStart.putExtra("channelId", channelId);
        startService(intentStart);
    }


    public static boolean checkConnection(Context context) {
        try {
            ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            Network nw = connectivityManager.getActiveNetwork();
            if (nw == null) return false;
            NetworkCapabilities actNw = connectivityManager.getNetworkCapabilities(nw);
            return actNw != null && (actNw.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
                    || actNw.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
                    || actNw.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
                    || actNw.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH));
        } catch (Exception e) {
            return false;
        }
    }

    public static int getStringByName(String name) {
        int stringId = 0;

        try {
            Class<R.string> res = R.string.class;
            Field field = res.getField(name);
            stringId = field.getInt(null);
        } catch (Exception e) {
            Log.e("getStringByName", " " + e.getMessage());
        }

        return stringId;
    }
}