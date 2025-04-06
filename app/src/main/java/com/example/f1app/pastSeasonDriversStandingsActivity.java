package com.example.f1app;

import static com.example.f1app.MainActivity.checkConnection;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
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
import java.util.List;
import java.util.Locale;

public class pastSeasonDriversStandingsActivity extends AppCompatActivity {
    Button showTeamsButton, showSchedule, showTeams, showHomePage, showAccount;

    private List<driversList> datum;
    private RecyclerView recyclerView;
    private ImageButton backButton;
    private Toolbar toolbar;
    private pastSeasonDriversStandingsAdapter adapter;
    private ShimmerFrameLayout shimmerFrameLayout;
    private SwipeRefreshLayout swipeLayout;
    private TextView driversHeader;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.past_season_drivers_standing_page);

        if (!checkConnection(getApplicationContext())){
            startActivity(connectionLostScreen.createShowSplashOnNetworkFailure(pastSeasonDriversStandingsActivity.this));
        }else{
            startActivity(connectionLostScreen.createIntentHideSplashOnNetworkRecovery(pastSeasonDriversStandingsActivity.this));
        }

        shimmerFrameLayout = findViewById(R.id.shimmer_layout);
        shimmerFrameLayout.startShimmer();

        recyclerView = findViewById(R.id.recyclerview_currentDrivers);
        recyclerView.setHasFixedSize(true);

        driversHeader = (TextView) findViewById(R.id.driversHeader);
        String headerText = " ";
        if (Locale.getDefault().getLanguage().equals("ru")){
            headerText = getString(R.string.past_season_drivers) + " 2024";
        }else{
            headerText = "2024 " + getString(R.string.past_season_drivers);
        }
        driversHeader.setText(headerText);

        datum = new ArrayList<>();
        LocalDate currentDate = LocalDate.now();

        swipeLayout = findViewById(R.id.swipe_layout);
        swipeLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                recyclerView.setVisibility(View.GONE);
                shimmerFrameLayout.setVisibility(View.VISIBLE);
                shimmerFrameLayout.startShimmer();
                datum = new ArrayList<>();
                getStanding("2024");
                swipeLayout.setRefreshing(false);
            }
        });
        LinearLayoutManager linearLayoutManager3 = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(linearLayoutManager3);

        backButton = (ImageButton) findViewById(R.id.backButton);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });


        getStanding("2024");


        WindowInsetsControllerCompat windowInsetsController =
                WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        windowInsetsController.setAppearanceLightStatusBars(false);
    }


    public void getStanding(String currentYear){
        RequestQueue queue = Volley.newRequestQueue(pastSeasonDriversStandingsActivity.this);
        String url2 = "https://api.jolpi.ca/ergast/f1/" + currentYear + "/driverstandings/?format=json";
        JsonObjectRequest jsonObjectRequest2 = new JsonObjectRequest(
                Request.Method.GET,
                url2,
                null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            JSONObject MRData = response.getJSONObject("MRData");
                            String total = MRData.getString("total");
                            if (!total.equals("0")){
                                JSONObject StandingsTable = MRData.getJSONObject("StandingsTable");
                                JSONArray StandingsLists = StandingsTable.getJSONArray("StandingsLists");
                                for(int i = 0; i < StandingsLists.length(); i++){
                                    JSONArray DriverStandings = StandingsLists.getJSONObject(i)
                                            .getJSONArray("DriverStandings");
                                    for(int j = 0; j < DriverStandings.length(); j++) {
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
                                        datum.add(smth);
                                    }
                                }
                                Handler handler = new Handler();
                                handler.postDelayed(()->{
                                    recyclerView.setVisibility(View.VISIBLE);
                                    shimmerFrameLayout.setVisibility(View.GONE);
                                    shimmerFrameLayout.stopShimmer();
                                },500);
                                adapter = new pastSeasonDriversStandingsAdapter(pastSeasonDriversStandingsActivity.this, datum);
                                recyclerView.setAdapter(adapter);
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Toast.makeText(pastSeasonDriversStandingsActivity.this, error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
        queue.add(jsonObjectRequest2);
    }
}
