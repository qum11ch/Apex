package com.example.f1app;

import static com.example.f1app.MainActivity.checkConnection;
import static com.example.f1app.MainActivity.checkLightTheme;
import static com.example.f1app.MainActivity.getStringByName;
import static com.example.f1app.MainActivity.hideShimmer;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

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

import com.google.firebase.functions.FirebaseFunctions;
import com.google.firebase.functions.HttpsCallableReference;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.example.f1app.BuildConfig;


public class neuroPredictionResultActivity extends AppCompatActivity {
    private List<raceResultsQualiData> datum;
    private raceResultsQualiAdapter adapter;
    private RecyclerView recyclerView;
    private ShimmerFrameLayout shimmerFrameLayout, shimmerDriverLayout, shimmerUpdateLayour;
    private TextView poleLapDriverName;
    private TextView poleLapTime;
    private TextView eventInfo;
    private TextView updateDate;
    private RelativeLayout updateDateLayout;
    private RelativeLayout poleLapDriverLayout;
    private LinearLayout loadingWeatherLayout;
    private ProgressBar weatherProgress;
    private TextView loadingWeatherText;
    private TextView loadingWeatherPercent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.neuro_predictions_results_activity);

        setupNeuroResultsInsets();
        EdgeInsets.applyBottom(findViewById(R.id.scrollView));

        recyclerView = findViewById(R.id.quali_results);
        LinearLayoutManager mLayoutManager = new LinearLayoutManager(neuroPredictionResultActivity.this);
        recyclerView.setLayoutManager(mLayoutManager);

        datum = new ArrayList<>();
        adapter = new raceResultsQualiAdapter(neuroPredictionResultActivity.this, datum);
        recyclerView.setAdapter(adapter);

        ImageButton backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> finish());

        if (!checkConnection(getApplicationContext())){
            startActivity(connectionLostScreen.createShowSplashOnNetworkFailure(neuroPredictionResultActivity.this));
        }else{
            startActivity(connectionLostScreen.createIntentHideSplashOnNetworkRecovery(neuroPredictionResultActivity.this));
        }

        RelativeLayout fastestLapLayout = findViewById(R.id.poleLap_layout);
        if (!checkLightTheme(neuroPredictionResultActivity.this)){
            fastestLapLayout.setBackground(ContextCompat.getDrawable(neuroPredictionResultActivity.this, R.drawable.background_striped_lines_item_night));
        }

        poleLapDriverName = findViewById(R.id.poleLapDriverName);
        poleLapTime = findViewById(R.id.poleLapTime);
        eventInfo = findViewById(R.id.event_info);
        updateDate = findViewById(R.id.last_update_time);
        poleLapDriverLayout = findViewById(R.id.poleLapDriver_layout);
        updateDateLayout = findViewById(R.id.update_date_layout);

        loadingWeatherLayout = findViewById(R.id.loading_weather_layout);
        weatherProgress = findViewById(R.id.weather_progress);
        loadingWeatherText = findViewById(R.id.loading_weather_text);
        loadingWeatherPercent = findViewById(R.id.loading_weather_percent);

        shimmerFrameLayout = findViewById(R.id.shimmer_layout);
        shimmerDriverLayout = findViewById(R.id.shimmer_layout_driver);
        shimmerUpdateLayour = findViewById(R.id.shimmer_layout_last_update);
        shimmerDriverLayout.startShimmer();
        shimmerFrameLayout.startShimmer();
        shimmerUpdateLayour.startShimmer();

        TextView q1TimeText = findViewById(R.id.Q1_time);
        TextView q2TimeText = findViewById(R.id.Q2_time);
        TextView q3TimeText = findViewById(R.id.Q3_time);

        WindowInsetsControllerCompat windowInsetsController =
                WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        windowInsetsController.setAppearanceLightStatusBars(false);

        if(!getIntent().getExtras().isEmpty()) {
            Bundle bundle = getIntent().getExtras();
            String gpName = bundle.getString("gp");
            String event = bundle.getString("event");
            String currentSeason = bundle.getString("currentSeason");

            setProgressStage(0, getString(R.string.neuro_prediction_progress_start));

            if (event.equals("SQ")){
                q1TimeText.setText(R.string.sq1_header);
                q2TimeText.setText(R.string.sq2_header);
                q3TimeText.setText(R.string.sq3_header);
            }else{
                q1TimeText.setText(R.string.q1_header);
                q2TimeText.setText(R.string.q2_header);
                q3TimeText.setText(R.string.q3_header);
            }

            DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference();
            rootRef.child("status/last_update")
                    .addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            Integer lastRound = snapshot.child("last_round").getValue(Integer.class);
                            String lastRace = snapshot.child("last_race").getValue(String.class);
                            String year = lastRace.substring(0, 4);
                            if (year.equals(currentSeason)){
                                getPrediction(gpName, lastRound, event, currentSeason);
                            }else{
                                getPrediction(gpName, 0, event, currentSeason);
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            Log.e("predictPageActivity", error.getMessage());
                        }
                    });
        }
    }

    private void setupNeuroResultsInsets() {
        View root = findViewById(R.id.neuro_results_root);
        View header = findViewById(R.id.header);

        final int rootLeft = root.getPaddingLeft();
        final int rootTop = root.getPaddingTop();
        final int rootRight = root.getPaddingRight();
        final int rootBottom = root.getPaddingBottom();

        final int headerLeft = header.getPaddingLeft();
        final int headerTop = header.getPaddingTop();
        final int headerRight = header.getPaddingRight();
        final int headerBottom = header.getPaddingBottom();

        ViewCompat.setOnApplyWindowInsetsListener(root, (view, windowInsets) -> {
            Insets safeInsets = windowInsets.getInsets(
                    WindowInsetsCompat.Type.systemBars()
                            | WindowInsetsCompat.Type.displayCutout()
            );

            view.setPadding(
                    rootLeft + safeInsets.left,
                    rootTop,
                    rootRight + safeInsets.right,
                    rootBottom
            );

            header.setPadding(
                    headerLeft,
                    headerTop + safeInsets.top,
                    headerRight,
                    headerBottom
            );

            return windowInsets;
        });

        ViewCompat.requestApplyInsets(root);
    }

    private void getPrediction(String gpName, Integer lastGPRound, String event,
                               String currentSeason){

        setProgressStage(0, getString(R.string.neuro_prediction_progress_data_start));

        DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference();
        rootRef.child("driverLineUp/season/" + currentSeason)
                .addListenerForSingleValueEvent(new ValueEventListener() {
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

                        rootRef.child("schedule/season/" + currentSeason).child(gpName)
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

                                                        setProgressStage(15, getString(R.string.neuro_prediction_progress_data_finish));

                                                        if (event.equals("Q")){
                                                            getWeather(lat, lng, eventTime, gpName, driversCodesList.toArray(new String[0]),
                                                                    teamNamesList.toArray(new String[0]),
                                                                    event, finalGpRound, Integer.parseInt(circuitCorners),
                                                                    Double.parseDouble(circuitLength), isStreetCircuit,
                                                                    circuitId);
                                                        }else{
                                                            getWeather(lat, lng, sprintEventTime,gpName, driversCodesList.toArray(new String[0]),
                                                                    teamNamesList.toArray(new String[0]),
                                                                    event, finalGpRound, Integer.parseInt(circuitCorners),
                                                                    Double.parseDouble(circuitLength), isStreetCircuit,
                                                                    circuitId);
                                                        }
                                                    }
                                                    @Override
                                                    public void onCancelled(@NonNull DatabaseError error) {
                                                        Log.e("predictPageActivity", error.getMessage());
                                                        showProgressError(getString(R.string.neuro_prediction_progress_data_error));
                                                    }
                                                });
                                    }
                                    @Override
                                    public void onCancelled(@NonNull DatabaseError error) {
                                        Log.e("predictPageActivity", error.getMessage());
                                        showProgressError(getString(R.string.neuro_prediction_progress_data_error));
                                    }
                                });
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e("predictPageActivity", error.getMessage());
                        showProgressError(getString(R.string.neuro_prediction_progress_data_error));
                    }
                });
    }

    private void getWeatherOld(Double lat, Double lon,
                            String eventTime, String gpName,
                            String[] driverCodes, String[] teamNames, String event, int gpRound,
                            int circuitsCorners, Double circuitsLength, int isStreetCircuit,
                            String circuitId){

        String isoDateStr = eventTime.replace(" ", "T");
        Log.i("WeatherDate", " " + eventTime);

        ZonedDateTime targetDate = ZonedDateTime.parse(isoDateStr);
        ZonedDateTime now = ZonedDateTime.now(ZoneId.of("UTC"));

        long daysBetween = ChronoUnit.DAYS.between(now.toLocalDate(), targetDate.toLocalDate());

        String[] fullDate = eventTime.split(" ");
        String date = fullDate[0];
        String time = fullDate[1];

        String targetTime = time.replace("Z", "");
        targetTime = time.substring(0, targetTime.length() - 3);
        String targetDatetime = date + "T" + targetTime;

        RequestQueue queue = Volley.newRequestQueue(neuroPredictionResultActivity.this);

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

        if ((daysBetween <= 15) && (daysBetween >= -2)){
            url = "https://api.open-meteo.com/v1/forecast?latitude="
                    + lat + "&longitude=" + lon + "&hourly=temperature_2m,relative_humidity_2m,pressure_msl,precipitation,rain&timezone=GMT&start_date="
                    + date + "&end_date=" + date;
        } else if (daysBetween > 15) {
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

        Log.i("FatalError", " " + url);
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
                                    isStreetCircuit, circuitId);

                        } else {
                            Log.e("WeatherPredict", "Time " + finalTargetTime + " not found in response.");
                        }


                    }catch (JSONException e) {
                        Log.e("predictPageActivity", "Wheather error " + e.getMessage());
                    }
                }, error -> {
                    Toast.makeText(neuroPredictionResultActivity.this, "Wheather error " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.d ("fatal", "Wheather error  " + error.getMessage());
        });

        queue.add(jsonObjectRequest);
    }

    private void getWeather(
            Double lat,
            Double lon,
            String eventTime,
            String gpName,
            String[] driverCodes,
            String[] teamNames,
            String event,
            int gpRound,
            int circuitsCorners,
            Double circuitsLength,
            int isStreetCircuit,
            String circuitId
    ) {

        setProgressStage(15, getString(R.string.neuro_prediction_progress_weather_start));

        String isoDateStr = eventTime.replace(" ", "T");
        ZonedDateTime targetDateTime = ZonedDateTime.parse(isoDateStr);
        ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
        LocalDate eventDate = targetDateTime.toLocalDate();

        int seasonYear = eventDate.getYear();

        long daysBetween = ChronoUnit.DAYS.between(now.toLocalDate(), eventDate);

        boolean usePreviousYear = daysBetween > 15;

        LocalDate weatherDate;

        if (usePreviousYear) {
            weatherDate = eventDate.minusYears(1);
        } else {
            weatherDate = eventDate;
        }

        String weatherDateString = weatherDate.toString();

        String targetTime = targetDateTime.toLocalTime().format(
                DateTimeFormatter.ofPattern(
                        "HH:mm:ss"
                )
        );

        String apiKey = BuildConfig.VISUAL_CROSSING_API_KEY;

        if (apiKey == null || apiKey.isEmpty()) {
            Log.e(
                    "WeatherPredict",
                    "VISUAL_CROSSING_API_KEY не найден"
            );
            return;
        }

        String url =
                "https://weather.visualcrossing.com"
                        + "/VisualCrossingWebServices/rest/services/timeline/"
                        + lat
                        + ","
                        + lon
                        + "/"
                        + weatherDateString
                        + "?unitGroup=metric"
                        + "&include=hours"
                        + "&timezone=Z"
                        + "&key="
                        + apiKey
                        + "&contentType=json";

        Log.d("WeatherPredict", "URL: " + url);

        Log.d("WeatherPredict", "Искомое время: " + targetTime);

        RequestQueue queue = Volley.newRequestQueue(neuroPredictionResultActivity.this);

        JsonObjectRequest request =
                new JsonObjectRequest(
                        Request.Method.GET,
                        url,
                        null,
                        response -> {
                            try {
                                JSONObject hourlyDay =
                                        response.getJSONArray("days").getJSONObject(0);

                                JSONArray hours =
                                        hourlyDay.getJSONArray("hours");

                                JSONObject selectedHour = null;

                                for (int i = 0; i < hours.length(); i++) {
                                    JSONObject hour = hours.getJSONObject(i);

                                    String apiTime = hour.getString("datetime");

                                    if (apiTime.equals(targetTime)) {
                                        selectedHour = hour;
                                        break;
                                    }
                                }

                                if (selectedHour == null) {
                                    Log.e("WeatherPredict",
                                            "Время " + targetTime + " не найдено");

                                    return;
                                }

                                setProgressStage(
                                        45,
                                        getString(R.string.neuro_prediction_progress_weather_finish)
                                );

                                double temp = selectedHour.getDouble("temp");

                                double pressure = selectedHour.getDouble("pressure");
                                double humidity = selectedHour.getDouble("humidity");
                                double precipitation = selectedHour.getDouble("precip");

                                int rainfall = precipitation > 0.0 ? 1 : 0;

                                predictRequest(
                                        gpName,
                                        seasonYear,
                                        temp,
                                        pressure,
                                        humidity,
                                        rainfall,
                                        driverCodes,
                                        teamNames,
                                        event,
                                        gpRound,
                                        circuitsCorners,
                                        circuitsLength,
                                        isStreetCircuit,
                                        circuitId
                                );

                            } catch (JSONException e) {
                                Log.e("WeatherPredict", "Ошибка обработки JSON", e);
                            }
                        },
                        error -> {
                            Log.e("WeatherPredict", "Ошибка получения погоды", error
                            );

                            Toast.makeText(
                                    neuroPredictionResultActivity.this,
                                    "Ошибка получения погоды",
                                    Toast.LENGTH_SHORT
                            ).show();

                            showProgressError(
                                    getString(R.string.neuro_prediction_progress_weather_error));
                        }
                );

        setProgressStage(35, getString(R.string.neuro_prediction_progress_weather_pending));
        queue.add(request);
    }


    private void predictRequest(
            String gpName,
            int year,
            Double temp,
            Double pres,
            Double hum,
            int rain,
            String[] driverCodes,
            String[] teamNames,
            String event,
            int gpRound,
            int circuitsCorners,
            Double circuitsLength,
            int isStreetCircuit,
            String circuitId
    ) {

        setProgressStage(
                45,
                getString(R.string.neuro_prediction_progress_model_start)
        );

        Map<String, Object> requestData = new HashMap<>();

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
        requestData.put("circuitId", circuitId);

        ArrayList<Map<String, String>> drivers = new ArrayList<>();

        for (String code : driverCodes) {
            Map<String, String> driver = new HashMap<>();
            driver.put("Driver", code);
            drivers.add(driver);
        }

        requestData.put("drivers", drivers);

        ArrayList<Map<String, String>> teams = new ArrayList<>();

        for (String name : teamNames) {
            Map<String, String> team = new HashMap<>();
            team.put("Team", name);
            teams.add(team);
        }

        requestData.put("teams", teams);

        setProgressStage(
                60,
                getString(R.string.neuro_prediction_progress_model_put)
        );

        Log.i("py_predict_results", "Calling Firebase Callable Function");

        FirebaseFunctions functions = FirebaseFunctions.getInstance("us-central1");

        HttpsCallableReference callable = functions.getHttpsCallable("py_functions_callable");

        setProgressStage(
                75,
                getString(R.string.neuro_prediction_progress_model_pending)
        );

        callable.call(requestData)
                .addOnSuccessListener(
                        result -> {
                            setProgressStage(
                                    85,
                                    getString(R.string.neuro_prediction_progress_model_processing)
                            );

                            try {
                                Object rawData = result.getData();

                                if (!(rawData instanceof Map)) {
                                    throw new JSONException(
                                            "Callable response is not an object"
                                    );
                                }

                                setProgressStage(
                                        90,
                                        getString(R.string.neuro_prediction_progress_model_results)
                                );

                                Map<?, ?> responseMap = (Map<?, ?>) rawData;
                                Object updatedAtValue = responseMap.get("updated_at");
                                String updateDateText = updatedAtValue == null ? "" : String.valueOf(updatedAtValue);
                                Object dataValue = responseMap.get("data");
                                Object staleValue = responseMap.get("stale");

                                boolean isStale = Boolean.TRUE.equals(staleValue);

                                if (isStale) {
                                    Toast.makeText(
                                            neuroPredictionResultActivity.this,
                                            getString(R.string.neuro_prediction_stale),
                                            Toast.LENGTH_LONG
                                    ).show();
                                }

                                if (!(dataValue instanceof List)) {
                                    throw new JSONException(
                                            "Response data is not a list"
                                    );
                                }

                                List<?> resultList = (List<?>) dataValue;

                                Log.i("py_predict_results", "Result: " + resultList);

                                datum.clear();

                                setProgressStage(
                                        98,
                                        getString(R.string.neuro_prediction_progress_model_table)
                                );

                                for (int i = 0; i < resultList.size(); i++) {
                                    Object item = resultList.get(i);

                                    if (!(item instanceof Map)) {
                                        continue;
                                    }

                                    Map<?, ?> qualiItem = (Map<?, ?>) item;

                                    String driverCode = readString(qualiItem, "Driver");
                                    String driverTeam = readString(qualiItem, "Team");
                                    String q1Time = readNullableString(qualiItem, event + "1");
                                    String q2Time = readNullableString(qualiItem, event + "2");
                                    String q3Time = readNullableString(qualiItem, event + "3");

                                    if (q1Time.isEmpty()) {
                                        q1Time = "--";
                                    }

                                    if (q2Time.isEmpty()) {
                                        q2Time = "--";
                                    }

                                    if (q3Time.isEmpty()) {
                                        q3Time = "--";
                                    }

                                    if (i == 0) {
                                        setPoleDriver(
                                                driverCode,
                                                q3Time,
                                                gpName,
                                                event,
                                                year,
                                                updateDateText
                                        );
                                    }

                                    int position = i + 1;
                                    String teamId = getTeamId(driverTeam);

                                    raceResultsQualiData
                                            predictResults =
                                            new raceResultsQualiData(
                                                    Integer.toString(position),
                                                    teamId,
                                                    driverCode,
                                                    q1Time,
                                                    q2Time,
                                                    q3Time,
                                                    Integer.toString(year)
                                            );

                                    datum.add(predictResults);
                                }

                                hideShimmer(recyclerView, shimmerFrameLayout);

                                adapter.notifyDataSetChanged();

                                finishProgress();

                            } catch (Exception error) {
                                Log.e("py_predict_results",
                                        "Response parsing error",
                                        error
                                );

                                Toast.makeText(
                                        neuroPredictionResultActivity.this,
                                        getString(R.string.neuro_prediction_parse_error),
                                        Toast.LENGTH_SHORT
                                ).show();

                                showProgressError(
                                        getString(R.string.neuro_prediction_parse_error)
                                );
                            }
                        }
                )
                .addOnFailureListener(
                        error -> {
                            Log.e(
                                    "py_predict_results",
                                    "Callable function failed",
                                    error
                            );

                            hideShimmer(recyclerView, shimmerFrameLayout
                            );

                            String message = error.getMessage();

                            if (message == null || message.isEmpty()) {
                                message = getString(R.string.neuro_prediction_response_error);
                            }

                            Toast.makeText(
                                    neuroPredictionResultActivity.this,
                                    message,
                                    Toast.LENGTH_LONG
                            ).show();

                            showProgressError(
                                    getString(R.string.neuro_prediction_progress_model_error)
                            );
                        }
                );
    }

    private static String readString(
            Map<?, ?> map,
            String key
    ) {
        Object value = map.get(key);

        if (value == null) {
            return "";
        }

        return String.valueOf(value);
    }

    private static String readNullableString(
            Map<?, ?> map,
            String key
    ) {
        Object value = map.get(key);

        if (value == null) {
            return "";
        }

        String stringValue = String.valueOf(value);

        if (stringValue.equals("null")
                || stringValue.equals("NaN")) {
            return "";
        }

        return stringValue;
    }

    private void setPoleDriver(
            String driverCode,
            String q3Time,
            String gpName,
            String event,
            int year,
            String updateDateRawText
    ) {
        String fullDriverName =
                getDriverName(driverCode);

        if (fullDriverName == null
                || fullDriverName.trim().isEmpty()) {
            return;
        }

        String[] nameParts =
                fullDriverName.split(" ");

        String driverName;
        String familyName;

        if (fullDriverName.equals(
                "Andrea Kimi Antonelli"
        )) {
            driverName = nameParts[1];
            familyName = nameParts[2];
        } else {
            driverName = nameParts[0];
            familyName = nameParts[nameParts.length - 1];
        }

        String abbreviatedName =
                driverName.substring(0, 1)
                        + ". "
                        + familyName;

        poleLapDriverName.setText(
                abbreviatedName
        );

        poleLapTime.setText(q3Time);

        String eventType;

        if (event.equals("Q")) {
            eventType =
                    getString(
                            R.string.qualifying_text
                    );
        } else {
            eventType =
                    getString(
                            R.string.sprint_qualifying_text
                    );
        }

        String normalizedGpName = gpName.toLowerCase().replace(" ", "_");

        String localizedGpName = getString( getStringByName( normalizedGpName + "_text") );

        String fullEventName = year + " " + localizedGpName + " " + eventType;

        eventInfo.setText(fullEventName);

        String updateDateText = formatDateForUser(updateDateRawText);
        updateDate.setText(updateDateText);

        shimmerUpdateLayour.animate()
                .setDuration(500)
                .withEndAction(() -> {
                    updateDateLayout
                            .setVisibility(View.VISIBLE);

                    shimmerUpdateLayour
                            .setVisibility(View.GONE);

                    shimmerUpdateLayour
                            .stopShimmer();
                })
                .start();

        shimmerDriverLayout.animate()
                .setDuration(500)
                .withEndAction(() -> {
                    poleLapDriverLayout
                            .setVisibility(View.VISIBLE);

                    shimmerDriverLayout
                            .setVisibility(View.GONE);

                    shimmerDriverLayout
                            .stopShimmer();
                })
                .start();
    }

    public static Integer isStreetCircuit(String circuitId){
        String[] street_circuits_list = {"albert_park", "baku", "monaco", "villeneuve", "jeddah",
                "vegas", "miami", "marina_bay", "madring"};
        if (Arrays.asList(street_circuits_list).contains(circuitId)){
            return 1;
        }else{
            return 0;
        }
    }

    private String formatDateForUser(String utcDateTime) {
        if (utcDateTime == null
                || utcDateTime.trim().isEmpty()) {
            return "";
        }

        ZonedDateTime dateTime =
                ZonedDateTime.parse(utcDateTime);

        ZonedDateTime userDateTime =
                dateTime.withZoneSameInstant(
                        ZoneId.systemDefault()
                );

        DateTimeFormatter formatter =
                DateTimeFormatter.ofPattern(
                        "dd-MM-yyyy HH:mm:ss",
                        Locale.getDefault()
                );

        return userDateTime.format(formatter);
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
        //teamNames.put("sauber", "Kick Sauber");
        teamNames.put("williams", "Williams");
        teamNames.put("cadillac", "Cadillac");
        teamNames.put("audi", "Audi");

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
        driversCodes.put("Gabriel Bortoleto", "BOR");
        driversCodes.put("Nico Hülkenberg", "HUL");
        driversCodes.put("Alexander Albon", "ALB");
        driversCodes.put("Carlos Sainz", "SAI");
        driversCodes.put("Valtteri Bottas", "BOT");
        driversCodes.put("Arvid Lindblad", "LIN");
        driversCodes.put("Sergio Pérez", "PER");

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
        driversCodes.put("BOR", "Gabriel Bortoleto");
        driversCodes.put("HUL", "Nico Hülkenberg");
        driversCodes.put("ALB", "Alexander Albon");
        driversCodes.put("SAI", "Carlos Sainz");
        driversCodes.put("BOT", "Valtteri Bottas");
        driversCodes.put("LIN", "Arvid Lindblad");
        driversCodes.put("PER", "Sergio Pérez");

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
        //teamIds.put("Kick Sauber", "sauber");
        teamIds.put("Williams", "williams");
        teamIds.put("Cadillac", "cadillac");
        teamIds.put("Audi", "audi");

        return teamIds.get(teamName);
    }

    private void setProgressStage(
            int progress,
            String message
    ) {
        if (loadingWeatherLayout == null
                || weatherProgress == null
                || loadingWeatherText == null
                || loadingWeatherPercent == null) {
            return;
        }

        loadingWeatherLayout.setVisibility(
                View.VISIBLE
        );

        weatherProgress.setMax(100);
        weatherProgress.setProgress(progress);

        loadingWeatherPercent.setText(
                progress + "%"
        );

        loadingWeatherText.setText(
                message
        );
    }

    private void finishProgress() {
        weatherProgress.setProgress(100);

        loadingWeatherPercent.setText("100%");
        loadingWeatherText.setText(getString(R.string.neuro_prediction_progress_finish));

        loadingWeatherLayout.postDelayed(
                () -> loadingWeatherLayout.setVisibility(View.GONE),
                500
        );
    }

    private void showProgressError(
            String message
    ) {
        if (loadingWeatherLayout != null) {
            loadingWeatherLayout.setVisibility(
                    View.VISIBLE
            );
        }

        if (weatherProgress != null) {
            weatherProgress.setProgress(0);
        }

        if (loadingWeatherText != null) {
            loadingWeatherText.setText(getString(R.string.neuro_prediction_progress_error));
        }

        Toast.makeText(
                neuroPredictionResultActivity.this,
                message,
                Toast.LENGTH_SHORT
        ).show();
    }
}
