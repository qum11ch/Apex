package com.example.f1app;

import static com.example.f1app.MainActivity.getStringByName;

import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
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

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class predictResultPage extends AppCompatActivity {
    private List<raceResultsQualiData> datum;
    private raceResultsQualiAdapter adapter;
    private RecyclerView recyclerView;
    private ShimmerFrameLayout shimmerFrameLayout, shimmerDriverLayout;
    private TextView poleLapDriverName, poleLapTime, eventInfo, Q1TimeText, Q2TimeText, Q3TimeText;
    private RelativeLayout poleLapDriverLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.predict_results_page);

        datum = new ArrayList<>();

        ImageButton backButton = (ImageButton) findViewById(R.id.backButton);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        recyclerView = findViewById(R.id.quali_results);
        LinearLayoutManager mLayoutManager = new LinearLayoutManager(predictResultPage.this);
        recyclerView.setLayoutManager(mLayoutManager);

        poleLapDriverName = findViewById(R.id.poleLapDriverName);
        poleLapTime = findViewById(R.id.poleLapTime);
        eventInfo = findViewById(R.id.event_info);
        poleLapDriverLayout = findViewById(R.id.poleLapDriver_layout);

        shimmerFrameLayout = findViewById(R.id.shimmer_layout);
        shimmerDriverLayout = findViewById(R.id.shimmer_layout_driver);
        shimmerDriverLayout.startShimmer();
        shimmerFrameLayout.startShimmer();

        Q1TimeText = findViewById(R.id.Q1_time);
        Q2TimeText = findViewById(R.id.Q2_time);
        Q3TimeText = findViewById(R.id.Q3_time);

        WindowInsetsControllerCompat windowInsetsController =
                WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        windowInsetsController.setAppearanceLightStatusBars(false);

        if(!getIntent().getExtras().isEmpty()) {
            Bundle bundle = getIntent().getExtras();
            String gpName = bundle.getString("gp");
            String event = bundle.getString("event");

            if (event.equals("SQ")){
                Q1TimeText.setText(R.string.sq1_header);
                Q2TimeText.setText(R.string.sq2_header);
                Q3TimeText.setText(R.string.sq3_header);
            }else{
                Q1TimeText.setText(R.string.q1_header);
                Q2TimeText.setText(R.string.q2_header);
                Q3TimeText.setText(R.string.q3_header);
            }

            DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference();
            rootRef.child("status/last_update")
                    .addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            String lastRace = snapshot.child("last_race").getValue(String.class);
                            String year = lastRace.substring(0, 4);
                            String raceName = lastRace.substring(5, lastRace.length());

                            rootRef.child("schedule/season/" + year).child(raceName).addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot snapshot) {
                                    Integer roundNumber = snapshot.child("round").getValue(Integer.class);
                                    getPridiction(gpName, roundNumber, event);
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError error) {
                                    Log.e("predictPageActivity", error.getMessage());
                                }
                            });
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            Log.e("predictPageActivity", error.getMessage());
                        }
                    });
        }
    }

    private void getPridiction(String gpName, Integer lastGPRound, String event){
        DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference();
        rootRef.child("driverLineUp/season/2025")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        ArrayList<String> driversList = new ArrayList<>();
                        ArrayList<String> teamIdsList = new ArrayList<>();
                        for (DataSnapshot teamDataSnapshot: snapshot.getChildren()){
                            String teamId = teamDataSnapshot.getKey();
                            for (DataSnapshot driverDataSnapshot : teamDataSnapshot.child("drivers").getChildren()) {
                                String driverFullname = driverDataSnapshot.getKey();
                                driversList.add(driverFullname);
                                teamIdsList.add(teamId);
                            }
                        }

                        ArrayList<String> teamNamesList = correctTeamNames(teamIdsList);
                        ArrayList<String> driversCodesList = correctDriversCodes(driversList);

                        rootRef.child("schedule/season/2025").child(gpName)
                                .addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                                        String circuitId = snapshot.child("Circuit").child("circuitId")
                                                .getValue(String.class);

                                        String raceQualiDate = snapshot.child("Qualifying").child("raceQualiDate")
                                                .getValue(String.class);
                                        String raceQualiTime = snapshot.child("Qualifying").child("raceQualiTime")
                                                .getValue(String.class);

                                        String sprintQualiDate = snapshot.child("SprintQualifying").child("sprintQualiDate")
                                                .getValue(String.class);
                                        String sprintQualiTime;

                                        if (sprintQualiDate.equals("N/A")){
                                            sprintQualiDate = snapshot.child("SecondPractice").child("secondPracticeDate")
                                                    .getValue(String.class);
                                            sprintQualiTime = snapshot.child("SecondPractice").child("secondPracticeTime")
                                                    .getValue(String.class);
                                        }else{
                                            sprintQualiTime = snapshot.child("SprintQualifying").child("sprintQualiTime")
                                                    .getValue(String.class);
                                        }

                                        Integer gpRound = snapshot.child("round").getValue(Integer.class);

                                        if (gpRound > lastGPRound){
                                            gpRound = lastGPRound;
                                        }else{
                                            gpRound = snapshot.child("round").getValue(Integer.class);
                                        }

                                        // Fixing Time for "Q"
                                        String[] parsedTime = raceQualiTime.split(":");
                                        int int_hours = Integer.parseInt(parsedTime[0]);
                                        int int_minutes = Integer.parseInt(parsedTime[1]);

                                        if (int_minutes >= 30){
                                            int_hours += 1;
                                        }

                                        String minutes = "00";
                                        String hours = Integer.toString(int_hours);
                                        if (int_hours < 10){
                                            hours = "0" + hours;
                                        }
                                        String fixedTime = hours + ":" + minutes + ":" + parsedTime[2];
                                        String eventTime = raceQualiDate + " " + fixedTime;

                                        // Fixing Time for "Q"
                                        String[] parsedTimeSprint = sprintQualiTime.split(":");
                                        int int_hours_sprint = Integer.parseInt(parsedTimeSprint[0]);
                                        int int_minutes_sprint = Integer.parseInt(parsedTimeSprint[1]);

                                        if (int_minutes_sprint >= 30){
                                            int_hours_sprint += 1;
                                        }

                                        String minutes_sprint = "00";
                                        String hours_sprint = Integer.toString(int_hours);
                                        if (int_hours_sprint < 10){
                                            hours_sprint = "0" + hours_sprint;
                                        }
                                        String fixedTimeSprint = hours_sprint + ":" + minutes_sprint + ":" + parsedTimeSprint[2];

                                        String sprintEventTime = sprintQualiDate + " " + fixedTimeSprint;

                                        Integer isStreetCircuit = isStreetCircuit(circuitId);

                                        Integer finalGpRound = gpRound;
                                        rootRef.child("circuits").child(circuitId)
                                                .addListenerForSingleValueEvent(new ValueEventListener() {
                                                    @Override
                                                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                                                        String circuitLength = snapshot.child("length").getValue(String.class);
                                                        String circuitCorners = snapshot.child("turnsCount").getValue(String.class);

                                                        Double lat = snapshot.child("lat").getValue(Double.class);
                                                        Double lng = snapshot.child("lng").getValue(Double.class);

                                                        if (event.equals("Q")){
                                                            getWeather(lat, lng, eventTime, gpName, driversCodesList.toArray(new String[0]),
                                                                    teamNamesList.toArray(new String[0]),
                                                                    event, finalGpRound, Integer.parseInt(circuitCorners),
                                                                    Double.parseDouble(circuitLength), isStreetCircuit);
                                                        }else{
                                                            getWeather(lat, lng, sprintEventTime,gpName, driversCodesList.toArray(new String[0]),
                                                                    teamNamesList.toArray(new String[0]),
                                                                    event, finalGpRound, Integer.parseInt(circuitCorners),
                                                                    Double.parseDouble(circuitLength), isStreetCircuit);
                                                        }
                                                    }
                                                    @Override
                                                    public void onCancelled(@NonNull DatabaseError error) {
                                                        Log.e("predictPageActivity", error.getMessage());
                                                    }
                                                });

                                    }
                                    @Override
                                    public void onCancelled(@NonNull DatabaseError error) {
                                        Log.e("predictPageActivity", error.getMessage());
                                    }
                                });

                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e("predictPageActivity", error.getMessage());
                    }
                });
    }

    private void getWeather(Double lat, Double lon,
                            String eventTime, String gpName,
                            String[] driverCodes, String[] teamNames, String event, int gpRound,
                            int circuitsCorners, Double circuitsLength, int isStreetCircuit){

        String isoDateStr = eventTime.replace(" ", "T");

        ZonedDateTime targetDate = ZonedDateTime.parse(isoDateStr);
        ZonedDateTime now = ZonedDateTime.now(ZoneId.of("UTC"));

        long daysBetween = ChronoUnit.DAYS.between(now.toLocalDate(), targetDate.toLocalDate());

        String[] fullDate = eventTime.split(" ");
        String date = fullDate[0];
        String time = fullDate[1];

        String targetTime = time.replace("Z", "");
        targetTime = time.substring(0, targetTime.length() - 3);
        String targetDatetime = date + "T" + targetTime;

        RequestQueue queue = Volley.newRequestQueue(predictResultPage.this);

        String url;
        ZonedDateTime oneYearEarlier;

        if (daysBetween > 365){
            oneYearEarlier = targetDate.minusYears(2);
        }else{
            oneYearEarlier = targetDate.minusYears(1);
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        String prevYearDate = oneYearEarlier.format(formatter);


        boolean prevYear = false;

        if ((daysBetween <= 16) && (daysBetween >= -2)){
            url = "https://api.open-meteo.com/v1/forecast?latitude="
                    + lat + "&longitude=" + lon + "&hourly=temperature_2m,relative_humidity_2m,pressure_msl,precipitation,rain&timezone=GMT&start_date="
                    + date + "&end_date=" + date;
        } else if (daysBetween > 16) {
            daysBetween = daysBetween - 365;
            if (daysBetween >= -2){
                url = "https://api.open-meteo.com/v1/forecast?latitude="
                        + lat + "&longitude=" + lon + "&hourly=temperature_2m,relative_humidity_2m,pressure_msl,precipitation,rain&timezone=GMT&start_date="
                        + prevYearDate + "&end_date=" + prevYearDate;
            }else{
                url = "https://archive-api.open-meteo.com/v1/archive?latitude="
                        + lat + "&longitude=" + lon + "&hourly=temperature_2m,relative_humidity_2m,pressure_msl,precipitation,rain&timezone=GMT&start_date="
                        + prevYearDate + "&end_date=" + prevYearDate;
            }
            prevYear = true;
        } else{
            url = "https://archive-api.open-meteo.com/v1/archive?latitude="
                    + lat + "&longitude=" + lon + "&hourly=temperature_2m,relative_humidity_2m,pressure_msl,precipitation,rain&timezone=GMT&start_date="
                    + date + "&end_date=" + date;
        }

        String finalTargetTime;
        if (prevYear){
            finalTargetTime = prevYearDate + "T" + targetTime;
        } else {
            finalTargetTime = targetDatetime;
        }

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(
                Request.Method.GET,
                url,
                null,
                response -> {
                    try{
                        JSONObject hourly = response.getJSONObject("hourly");

                        JSONArray timeArray = hourly.getJSONArray("time");
                        JSONArray tempArray = hourly.getJSONArray("temperature_2m");
                        JSONArray pressureArray = hourly.getJSONArray("pressure_msl");
                        JSONArray humidityArray = hourly.getJSONArray("relative_humidity_2m");
                        JSONArray precipitationArray = hourly.getJSONArray("precipitation");

                        int targetIndex = -1;

                        for (int i = 0; i < timeArray.length(); i++) {
                            if (timeArray.getString(i).equals(finalTargetTime)) {
                                targetIndex = i;
                                break;
                            }
                        }

                        if (targetIndex != -1) {
                            double temp = tempArray.getDouble(targetIndex);
                            double pressure = pressureArray.getDouble(targetIndex);
                            double humidity = humidityArray.getDouble(targetIndex);
                            double precipitation = precipitationArray.getDouble(targetIndex);
                            boolean isRainfall = precipitation > 0.0;

                            int rainfall = 0;
                            if (isRainfall){
                                rainfall = 1;
                            }

                            DateTimeFormatter yearFormatter = DateTimeFormatter.ofPattern("yyyy");
                            String year = targetDate.format(yearFormatter);

                            predictRequest(gpName, Integer.parseInt(year), temp, pressure, humidity, rainfall, driverCodes,
                                    teamNames, event, gpRound, circuitsCorners, circuitsLength,
                                    isStreetCircuit);

                        } else {
                            Log.e("WeatherPredict", "Time " + finalTargetTime + " not found in response.");
                        }


                    }catch (JSONException e) {
                        Log.e("predictPageActivity", " " + e.getMessage());
                    }
                }, error -> Toast.makeText(predictResultPage.this, error.getMessage(), Toast.LENGTH_SHORT).show());

        queue.add(jsonObjectRequest);
    }


    private void predictRequest(String gpName, int year, Double temp, Double pres, Double hum, int rain,
                                String[] driverCodes, String[] teamNames, String event, int gpRound,
                                int circuitsCorners, Double circuitsLength, int isStreetCircuit){
        String url = "https://py-functions-qnqkgv3l5a-uc.a.run.app/";

        RequestQueue queue = Volley.newRequestQueue(predictResultPage.this);

        JSONObject requestData = new JSONObject();
        try {
            requestData.put("year", year);
            requestData.put("airTemp", temp);
            requestData.put("pressure", pres);
            requestData.put("humidity", hum);
            requestData.put("rainfall", rain);
            requestData.put("event", event);
            requestData.put("gpRound", gpRound);
            requestData.put("circuitCorners", circuitsCorners);
            requestData.put("circuitLength", circuitsLength);
            requestData.put("isStreetCircuit", isStreetCircuit);

            JSONArray drivers = new JSONArray();

            for (String code : driverCodes) {
                JSONObject d = new JSONObject();
                d.put("Driver", code);
                drivers.put(d);
            }
            requestData.put("drivers", drivers);

            JSONArray teams = new JSONArray();
            for (String name : teamNames) {
                JSONObject t = new JSONObject();
                t.put("Team", name);
                teams.put(t);
            }
            requestData.put("teams", teams);

        } catch (JSONException e) {
            e.printStackTrace();
        }

        Log.i("py_predict_results", " " + requestData.toString());

        JsonObjectRequest jsonRequest = new JsonObjectRequest(
                Request.Method.POST,
                url,
                requestData,
                response -> {
                    try {
                        JSONArray results = response.getJSONArray("results");
                        for(int i = 0; i < results.length(); i++){
                            JSONObject qualiItem =  results.getJSONObject(i);
                            String driverCode = qualiItem.getString("Driver");
                            String driverTeam = qualiItem.getString("Team");
                            String Q1_time = qualiItem.getString(event + "1");
                            String Q2_time = qualiItem.getString(event + "2");
                            String Q3_time = qualiItem.getString(event + "3");


                            if (i == 0){
                                String fullDriverName = getDriverName(driverCode);
                                String[] parseDriverName = fullDriverName.split(" ");
                                String driverName;
                                String driverFamilyName;
                                if (fullDriverName.equals("Andrea Kimi Antonelli")){
                                    driverName = parseDriverName[1];
                                    driverFamilyName = parseDriverName[2];
                                }else{
                                    driverName = parseDriverName[0];
                                    driverFamilyName = parseDriverName[1];
                                }
                                String abrDriverName = driverName.substring(0,1) + ". " + driverFamilyName;

                                poleLapDriverName.setText(abrDriverName);
                                poleLapTime.setText(Q3_time);

                                String eventType;
                                if (event.equals("Q")){
                                    eventType = getString(R.string.qualifying_text);
                                }else{
                                    eventType = getString(R.string.sprint_qualifying_text);
                                }

                                String preprocessedGPName = gpName.toLowerCase();
                                preprocessedGPName = preprocessedGPName.replace(" ", "_");
                                String localizedGpName = getString(getStringByName(preprocessedGPName + "_text"));
                                String fullEventName = year + " " + localizedGpName + " " + eventType;
                                eventInfo.setText(fullEventName);

                                Handler handler = new Handler();
                                handler.postDelayed(()->{
                                    poleLapDriverLayout.setVisibility(View.VISIBLE);
                                    shimmerDriverLayout.setVisibility(View.GONE);
                                    shimmerDriverLayout.stopShimmer();
                                },500);
                            }

                            if (Q2_time.equals("null")){
                                Q2_time = "--";
                            }
                            if (Q1_time.equals("null")){
                                Q1_time = "--";
                            }
                            if (Q3_time.equals("null")){
                                Q3_time = "--";
                            }

                            int position = i + 1;

                            String teamId = getTeamId(driverTeam);
                            raceResultsQualiData predictResults = new raceResultsQualiData(Integer.toString(position),
                                    teamId, driverCode, Q1_time, Q2_time, Q3_time, Integer.toString(year));
                            datum.add(predictResults);
                        }



                        Handler handler = new Handler();
                        handler.postDelayed(()->{
                            recyclerView.setVisibility(View.VISIBLE);
                            shimmerFrameLayout.setVisibility(View.GONE);
                            shimmerFrameLayout.stopShimmer();
                        },500);
                        adapter = new raceResultsQualiAdapter(predictResultPage.this, datum);
                        recyclerView.setAdapter(adapter);
                    } catch (JSONException e) {
                        throw new RuntimeException(e);
                    }
                },
                error -> {
                    if (error.networkResponse != null) {
                        String errorText = "Code: " + error.networkResponse.statusCode + "Data: " +  new String(error.networkResponse.data);
                        Toast.makeText(predictResultPage.this, errorText, Toast.LENGTH_SHORT).show();
                    }else{
                        Toast.makeText(predictResultPage.this, error.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }
        );

        jsonRequest.setRetryPolicy(new DefaultRetryPolicy(
                20000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        ));

        queue.add(jsonRequest);
    }

    public static Integer isStreetCircuit(String circuitId){
        String[] street_circuits_list = {"albert_park", "baku", "monaco", "villeneuve", "jeddah",
                "vegas", "miami", "marina_bay"};
        if (Arrays.asList(street_circuits_list).contains(circuitId)){
            return 1;
        }else{
            return 0;
        }
    }

    public static ArrayList<String> correctTeamNames(ArrayList<String> teamIds){
        HashMap<String, String> teamNames = new HashMap<>();
        teamNames.put("alpine", "Alpine");
        teamNames.put("aston_martin", "Aston Martin");
        teamNames.put("ferrari", "Ferrari");
        teamNames.put("haas", "Haas F1 Team");
        teamNames.put("mclaren", "McLaren");
        teamNames.put("mercedes", "Mercedes");
        teamNames.put("rb", "Racing Bulls");
        teamNames.put("red_bull", "Red Bull Racing");
        teamNames.put("sauber", "Kick Sauber");
        teamNames.put("williams", "Williams");

        ArrayList<String> results = new ArrayList<>();
        for(int i = 0; i < teamIds.size(); i++){
            String teamId = teamIds.get(i);
            String teamName = teamNames.get(teamId);

            results.add(teamName);
        }
        return results;
    }

    public static ArrayList<String> correctDriversCodes(ArrayList<String> drivers){
        HashMap<String, String> driversCodes = new HashMap<>();
        driversCodes.put("Franco Colapinto", "COL");
        driversCodes.put("Pierre Gasly", "GAS");
        driversCodes.put("Fernando Alonso", "ALO");
        driversCodes.put("Lance Stroll", "STR");
        driversCodes.put("Charles Leclerc", "LEC");
        driversCodes.put("Lewis Hamilton", "HAM");
        driversCodes.put("Esteban Ocon", "OCO");
        driversCodes.put("Oliver Bearman", "BEA");
        driversCodes.put("Lando Norris", "NOR");
        driversCodes.put("Oscar Piastri", "PIA");
        driversCodes.put("Andrea Kimi Antonelli", "ANT");
        driversCodes.put("George Russell", "RUS");
        driversCodes.put("Isack Hadjar", "HAD");
        driversCodes.put("Liam Lawson", "LAW");
        driversCodes.put("Max Verstappen", "VER");
        driversCodes.put("Yuki Tsunoda", "TSU");
        driversCodes.put("Gabriel Bortoleto", "BOR");
        driversCodes.put("Nico Hülkenberg", "HUL");
        driversCodes.put("Alexander Albon", "ALB");
        driversCodes.put("Carlos Sainz", "SAI");

        ArrayList<String> results = new ArrayList<>();
        for(int i = 0; i < drivers.size(); i++){
            String driverName = drivers.get(i);
            String driverCode = driversCodes.get(driverName);

            results.add(driverCode);
        }
        return results;
    }

    public static String getDriverName(String driverCode){
        HashMap<String, String> driversCodes = new HashMap<>();
        driversCodes.put("COL","Franco Colapinto");
        driversCodes.put("GAS", "Pierre Gasly");
        driversCodes.put("ALO", "Fernando Alonso");
        driversCodes.put("STR", "Lance Stroll");
        driversCodes.put("LEC", "Charles Leclerc");
        driversCodes.put("HAM", "Lewis Hamilton");
        driversCodes.put("OCO", "Esteban Ocon");
        driversCodes.put("BEA", "Oliver Bearman");
        driversCodes.put("NOR", "Lando Norris");
        driversCodes.put("PIA", "Oscar Piastri");
        driversCodes.put("ANT", "Andrea Kimi Antonelli");
        driversCodes.put("RUS", "George Russell");
        driversCodes.put("HAD", "Isack Hadjar");
        driversCodes.put("LAW", "Liam Lawson");
        driversCodes.put("VER", "Max Verstappen");
        driversCodes.put("TSU", "Yuki Tsunoda");
        driversCodes.put("BOR", "Gabriel Bortoleto");
        driversCodes.put("HUL", "Nico Hülkenberg");
        driversCodes.put("ALB", "Alexander Albon");
        driversCodes.put("SAI", "Carlos Sainz");

        return driversCodes.get(driverCode);
    }

    public static String getTeamId(String teamName){
        HashMap<String, String> teamIds = new HashMap<>();
        teamIds.put("Alpine", "alpine");
        teamIds.put("Aston Martin", "aston_martin");
        teamIds.put("Ferrari", "ferrari");
        teamIds.put("Haas F1 Team", "haas");
        teamIds.put("McLaren", "mclaren");
        teamIds.put("Mercedes", "mercedes");
        teamIds.put("Racing Bulls", "rb");
        teamIds.put("Red Bull Racing", "red_bull");
        teamIds.put("Kick Sauber", "sauber");
        teamIds.put("Williams", "williams");

        return teamIds.get(teamName);
    }
}
