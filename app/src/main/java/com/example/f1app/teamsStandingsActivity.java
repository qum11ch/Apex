package com.example.f1app;

import static com.example.f1app.MainActivity.checkConnection;
import static com.example.f1app.driverStatsFragment.getCountryCode;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

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

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class teamsStandingsActivity extends AppCompatActivity {
    Button showDriverButton, showDriverStanding, showHomePage, showAccount;

    private List<teamsList> datum;
    private RecyclerView recyclerView;
    private Button pastSeasonTeamsStandings;
    private teamsStandingsAdapter adapter;
    private ShimmerFrameLayout shimmerFrameLayout;
    private SwipeRefreshLayout swipeLayout;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.teams_standing_page);

        if (!checkConnection(getApplicationContext())){
            startActivity(connectionLostScreen.createShowSplashOnNetworkFailure(teamsStandingsActivity.this));
        }else{
            startActivity(connectionLostScreen.createIntentHideSplashOnNetworkRecovery(teamsStandingsActivity.this));
        }

        shimmerFrameLayout = findViewById(R.id.shimmer_layout);
        shimmerFrameLayout.startShimmer();

        pastSeasonTeamsStandings = findViewById(R.id.pastSeasonTeamsStandings);
        String buttonText;
        if (Locale.getDefault().getLanguage().equals("ru")){
            buttonText = getText(R.string.past_season_teams) + " 2024";
        }else{
            buttonText = "2024 " + getText(R.string.past_season_teams);
        }
        pastSeasonTeamsStandings.setText(buttonText);
        pastSeasonTeamsStandings.setOnClickListener(v -> {
            Intent intent = new Intent(teamsStandingsActivity.this, pastSeasonTeamsStandingsActivity.class);
            teamsStandingsActivity.this.startActivity(intent);
            overridePendingTransition(0, 0);
        });

        recyclerView = findViewById(R.id.recyclerview_currentTeams);
        recyclerView.setHasFixedSize(true);
        LinearLayoutManager linearLayoutManager3 = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(linearLayoutManager3);

        datum = new ArrayList<>();
        LocalDate currentDate = LocalDate.now();

        swipeLayout = findViewById(R.id.swipe_layout);
        swipeLayout.setOnRefreshListener(() -> {
            recyclerView.setVisibility(View.GONE);
            pastSeasonTeamsStandings.setVisibility(View.GONE);
            shimmerFrameLayout.setVisibility(View.VISIBLE);
            shimmerFrameLayout.startShimmer();
            datum = new ArrayList<>();
            getTeamStanding(Integer.toString(currentDate.getYear()));
            swipeLayout.setRefreshing(false);
        });

        showDriverButton = findViewById(R.id.showDriver);
        showDriverButton.setOnClickListener(v -> {
            Intent intent = new Intent(teamsStandingsActivity.this, driversStandingsActivity.class);
            teamsStandingsActivity.this.startActivity(intent);
            overridePendingTransition(0, 0);
        });

        showDriverStanding = findViewById(R.id.showSchedule);
        showDriverStanding.setOnClickListener(v -> {
            Intent intent = new Intent(teamsStandingsActivity.this, scheduleActivity.class);
            teamsStandingsActivity.this.startActivity(intent);
            overridePendingTransition(0, 0);
        });

        showHomePage = findViewById(R.id.showHomePage);
        showHomePage.setOnClickListener(v -> {
            Intent intent = new Intent(teamsStandingsActivity.this, MainActivity.class);
            teamsStandingsActivity.this.startActivity(intent);
            overridePendingTransition(0, 0);
        });

        showAccount = findViewById(R.id.showAccount);
        showAccount.setOnClickListener(v -> {
            Intent intent = new Intent(teamsStandingsActivity.this, logInPageActivity.class);
            teamsStandingsActivity.this.startActivity(intent);
            overridePendingTransition(0, 0);
        });


        ImageButton backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> finish());

        getTeamStanding(Integer.toString(currentDate.getYear()));

        WindowInsetsControllerCompat windowInsetsController =
                WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        windowInsetsController.setAppearanceLightStatusBars(false);

    }

    private void getTeamStanding(String currentYear){
        RequestQueue queue = Volley.newRequestQueue(teamsStandingsActivity.this);
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
                                for(int j = 0; j < ConstructorStandings.length(); j++){
                                    String constructorName = ConstructorStandings.getJSONObject(j)
                                            .getJSONObject("Constructor").getString("name");
                                    String position = ConstructorStandings.getJSONObject(j).getString("positionText");
                                    String points = ConstructorStandings.getJSONObject(j).getString("points");
                                    String constructorId = ConstructorStandings.getJSONObject(j)
                                            .getJSONObject("Constructor").getString("constructorId");
                                    //if(currentYear.equals("2024")){
                                    //    if (constructorName.equals("Sauber")){
                                    //        constructorName = "Kick Sauber";
                                    //    }
                                    //}
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
                                            datum.add(smth);
                                            Handler handler = new Handler();
                                            handler.postDelayed(()->{
                                                recyclerView.setVisibility(View.VISIBLE);
                                                pastSeasonTeamsStandings.setVisibility(View.VISIBLE);
                                                shimmerFrameLayout.setVisibility(View.GONE);
                                                shimmerFrameLayout.stopShimmer();
                                            },500);
                                            adapter = new teamsStandingsAdapter(teamsStandingsActivity.this, datum);
                                            recyclerView.setAdapter(adapter);
                                        }

                                        @Override
                                        public void onCancelled(@NonNull DatabaseError error) {
                                            Log.e("teamStandingsError", error.getMessage());
                                        }
                                    });
                                }
                            }

                        }
                        else{
                            DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference();
                            rootRef.child("constructors").orderByChild("lastSeasonPos").addValueEventListener(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot snapshot) {
                                    ArrayList<String> firstTeam = new ArrayList<>();
                                    firstTeam.add("");
                                    firstTeam.add("");
                                    teamsList first = new teamsList("","","","", true);
                                    first.setDrivers(firstTeam);
                                    datum.add(first);

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
                                                    recyclerView.setVisibility(View.VISIBLE);
                                                    pastSeasonTeamsStandings.setVisibility(View.VISIBLE);
                                                    shimmerFrameLayout.setVisibility(View.GONE);
                                                    shimmerFrameLayout.stopShimmer();
                                                },500);
                                                datum.add(smth);
                                                adapter = new teamsStandingsAdapter(teamsStandingsActivity.this, datum);
                                                recyclerView.setAdapter(adapter);
                                            }
                                            @Override
                                            public void onCancelled(@NonNull DatabaseError error) {
                                                Log.e("teamStandingsError", error.getMessage());
                                            }
                                        });
                                    }

                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError error) {
                                    Log.e("teamStandingsError", error.getMessage());
                                }
                            });

                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }, error -> Toast.makeText(teamsStandingsActivity.this, error.getMessage(), Toast.LENGTH_SHORT).show());
        queue.add(jsonObjectRequest2);
    }

    public static String getLocalizedCity(String cityName) {
        Map<String, String> cityTranslations = new HashMap<>();
        cityTranslations.put("Melbourne", "Мельбурн");
        cityTranslations.put("Austin", "Остин");
        cityTranslations.put("Sakhir", "Сахир");
        cityTranslations.put("Baku", "Баку");
        cityTranslations.put("Barcelona", "Барселона");
        cityTranslations.put("Hockenheim", "Хоккенхайм");
        cityTranslations.put("Budapest", "Будапешт");
        cityTranslations.put("Imola", "Имола");
        cityTranslations.put("Sao Paulo", "Сан-Паулу");
        cityTranslations.put("Istanbul", "Стамбул");
        cityTranslations.put("Jeddah", "Джедда");
        cityTranslations.put("Lusail", "Лусаил");
        cityTranslations.put("Magny-Cours", "Маньи-Кур");
        cityTranslations.put("Singapore", "Сингапур");
        cityTranslations.put("Miami", "Майами");
        cityTranslations.put("Monaco", "Монако");
        cityTranslations.put("Monza", "Монца");
        cityTranslations.put("Scarperia e San Piero", "Скарперия и Сан-Пьеро");
        cityTranslations.put("Nürburg", "Нюрбург");
        cityTranslations.put("Portimão", "Портиман");
        cityTranslations.put("Spielberg", "Шпильберг");
        cityTranslations.put("Le Castellet", "Ле Кастелле");
        cityTranslations.put("Mexico City", "Мехико");
        cityTranslations.put("Sepang", "Сепанг");
        cityTranslations.put("Shanghai", "Шанхай");
        cityTranslations.put("Silverstone", "Сильверстоун");
        cityTranslations.put("Spa Francorchamps", "Спа-Франкоршам");
        cityTranslations.put("Sochi", "Сочи");
        cityTranslations.put("Suzuka", "Судзука");
        cityTranslations.put("Las Vegas", "Лас-Вегас");
        cityTranslations.put("Montreal", "Монреаль");
        cityTranslations.put("Yas Marina", "Абу-Даби");
        cityTranslations.put("Zandvoort", "Зандворт");
        return cityTranslations.getOrDefault(cityName, cityName);
    }

    public static ArrayList<String> localizeLocality(String locality, String country, Context context){
        ArrayList<String> results = new ArrayList<>();
        String fullPlace;
        String cityName;

        cityName = getLocalizedCity(locality);
        String countryCode = getCountryCode(country.toLowerCase());
        Locale locale = new Locale("ru", "RU");
        String localizedCountry = new Locale("", countryCode).getDisplayCountry(locale);

        if (locality.equals(country)){
            fullPlace = cityName;
        }else{
            fullPlace = cityName + ", " + localizedCountry;
        }

        results.add(localizedCountry);
        results.add(cityName);
        results.add(fullPlace);
        return results;
    }
}
