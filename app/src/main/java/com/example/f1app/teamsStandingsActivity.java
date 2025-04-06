package com.example.f1app;

import static com.example.f1app.MainActivity.checkConnection;
import static com.example.f1app.driverStatsFragment.getCountryCode;

import android.content.Context;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
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

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

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

    public static ArrayList<String> localizeLocality(String locality, String country, Context context){
        ArrayList<String> results = new ArrayList<>();
        String locale;
        Locale driverCountryLocale = new Locale(Locale.getDefault().getLanguage(), getCountryCode(country));
        String localeCountry;
        if (driverCountryLocale.getDisplayCountry().equals("Соединенные Штаты")){
            localeCountry = "США";
        }else{
            localeCountry = driverCountryLocale.getDisplayCountry();
        }

        List<Address> addresses;
        String cityName;

        String address = locality + ", " + country;
        addresses = geocodeWithRetry(address, 10, context);
        cityName = addresses.get(0).getLocality();


        if (cityName!=null){
            switch (cityName){
                case "Шпильберг-Книттельфельд":
                    cityName = "Шпильберг";
                    break;
                case  "Stavelot":
                    cityName = "Спа";
                    break;
                case "Abu Dhabi":
                    cityName = "Абу-Даби";
                    break;
                default:
                    break;
            }
            locale =  cityName + ", " + localeCountry;
        }else{
            if (localeCountry.equals("Бахрейн")){
                cityName = "Сахир";
                locale =  cityName + ", " + localeCountry;
            }else{
                locale = localeCountry;
            }
        }

        results.add(localeCountry);
        results.add(cityName);
        results.add(locale);
        return results;
    }

    public static List<Address> geocodeWithRetry(String address, int maxRetries, Context context) {
        Geocoder geocoder = new Geocoder(context, Locale.getDefault());
        int retryCount = 0;

        while (retryCount < maxRetries) {
            try {
                List<Address> addresses = geocoder.getFromLocationName(address, 1);
                if (addresses != null && !addresses.isEmpty()) {
                    return addresses;
                }
            } catch (IOException e) {
                if (e.getMessage().contains("DEADLINE_EXCEEDED")) {
                    retryCount++;
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException ignored) {}
                } else {
                    break;
                }
            }
        }
        return null;
    }
}
