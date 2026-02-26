package com.example.f1app;

import static com.example.f1app.MainActivity.checkConnection;
import static com.example.f1app.MainActivity.hideShimmer;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
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

public class driversStandingsActivity extends AppCompatActivity {
    Button showTeams, showSchedule, showHomePage, showAccount;
    private View driverStndLine;
    private List<driversList> datum;
    private RecyclerView recyclerView;
    private driversStandingsAdapter adapter;
    private ShimmerFrameLayout shimmerFrameLayout;
    private SwipeRefreshLayout swipeLayout;
    private Button driversStandings_2024, driversStandings_2025;
    private String mCurrentSeason;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.drivers_standing_page);

        if (!checkConnection(getApplicationContext())){
            startActivity(connectionLostScreen.createShowSplashOnNetworkFailure(driversStandingsActivity.this));
        }else{
            startActivity(connectionLostScreen.createIntentHideSplashOnNetworkRecovery(driversStandingsActivity.this));
        }

        Bundle intentBundle = getIntent().getExtras();
        mCurrentSeason = intentBundle.getString("currentSeason");

        shimmerFrameLayout = findViewById(R.id.shimmer_layout);
        shimmerFrameLayout.startShimmer();

        recyclerView = findViewById(R.id.recyclerview_currentDrivers);
        recyclerView.setHasFixedSize(true);
        driverStndLine = findViewById(R.id.driverStndLine);

        datum = new ArrayList<>();

        adapter = new driversStandingsAdapter(driversStandingsActivity.this, datum);
        recyclerView.setAdapter(adapter);

        LocalDate currentDate = LocalDate.now();
        driversStandings_2024 = (Button) findViewById(R.id.driversStandings_2024);
        driversStandings_2025 = (Button) findViewById(R.id.driversStandings_2025);

        swipeLayout = findViewById(R.id.swipe_layout);
        swipeLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                recyclerView.setVisibility(View.GONE);
                driversStandings_2024.setVisibility(View.GONE);
                driversStandings_2025.setVisibility(View.GONE);
                driverStndLine.setVisibility(View.GONE);
                shimmerFrameLayout.setVisibility(View.VISIBLE);
                shimmerFrameLayout.startShimmer();
                datum = new ArrayList<>();
                getStanding(Integer.toString(currentDate.getYear()));

                swipeLayout.setRefreshing(false);
                adapter.notifyDataSetChanged();
            }
        });
        LinearLayoutManager linearLayoutManager3 = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(linearLayoutManager3);

        String buttonText_2024, buttonText_2025;
        if (Locale.getDefault().getLanguage().equals("ru")){
            buttonText_2024 = getText(R.string.past_season_drivers) + " 2024";
            buttonText_2025 = getText(R.string.past_season_drivers) + " 2025";
        }else{
            buttonText_2024 = "2024 " + getText(R.string.past_season_drivers);
            buttonText_2025 = "2025 " + getText(R.string.past_season_drivers);
        }
        driversStandings_2024.setText(buttonText_2024);
        driversStandings_2024.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(driversStandingsActivity.this,
                        pastSeasonDriversStandingsActivity.class);
                Bundle bundle = new Bundle();
                bundle.putString("season", "2024");
                bundle.putString("currentSeason", mCurrentSeason);
                intent.putExtras(bundle);
                driversStandingsActivity.this.startActivity(intent);
                overridePendingTransition(0, 0);
            }
        });

        driversStandings_2025.setText(buttonText_2025);
        driversStandings_2025.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(driversStandingsActivity.this,
                        pastSeasonDriversStandingsActivity.class);
                Bundle bundle = new Bundle();
                bundle.putString("season", "2025");
                bundle.putString("currentSeason", mCurrentSeason);
                intent.putExtras(bundle);
                driversStandingsActivity.this.startActivity(intent);
                overridePendingTransition(0, 0);
            }
        });

        showTeams = (Button) findViewById(R.id.showTeams);
        showTeams.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity_seasonData(driversStandingsActivity.this, teamsStandingsActivity.class, mCurrentSeason);
            }
        });

        showSchedule = (Button) findViewById(R.id.showSchedule);
        showSchedule.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity_seasonData(driversStandingsActivity.this, scheduleActivity.class, mCurrentSeason);
            }
        });

        showHomePage = (Button) findViewById(R.id.showHomePage);
        showHomePage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(driversStandingsActivity.this, MainActivity.class);
                driversStandingsActivity.this.startActivity(intent);
                overridePendingTransition(0, 0);
            }
        });

        showAccount = (Button) findViewById(R.id.showAccount);
        showAccount.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity_seasonData(driversStandingsActivity.this, logInPageActivity.class, mCurrentSeason);
            }
        });


        ImageButton backButton = (ImageButton) findViewById(R.id.backButton);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });


        getStanding(mCurrentSeason);


        WindowInsetsControllerCompat windowInsetsController =
                WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        windowInsetsController.setAppearanceLightStatusBars(false);
    }

    public static void startActivity_seasonData(Activity activity, Class<?> className, String currentSeason){
        Intent intent = new Intent(activity, className);
        Bundle bundle = new Bundle();
        bundle.putString("currentSeason" , currentSeason);
        intent.putExtras(bundle);
        activity.startActivity(intent);
        activity.overridePendingTransition(0, 0);
    }


    public void getStanding(String currentSeason){
        RequestQueue queue = Volley.newRequestQueue(driversStandingsActivity.this);
        String url2 = "https://api.jolpi.ca/ergast/f1/" + currentSeason + "/driverstandings/?format=json";
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
                                        driversList smth = new driversList(driverName, driverFamilyName, constructorsName, constructorId, points, placement, driverCode, false, currentSeason);
                                        datum.add(smth);
                                    }
                                }
                                hideShimmer(recyclerView, shimmerFrameLayout);
                                driversStandings_2024.animate()
                                        .setDuration(500)
                                        .withEndAction(() -> {
                                            driversStandings_2024.setVisibility(View.VISIBLE);
                                        })
                                        .start();

                                driverStndLine.animate()
                                        .setDuration(500)
                                        .withEndAction(() -> {
                                            driverStndLine.setVisibility(View.VISIBLE);
                                        })
                                        .start();

                                driversStandings_2025.animate()
                                        .setDuration(500)
                                        .withEndAction(() -> {
                                            driversStandings_2025.setVisibility(View.VISIBLE);
                                        })
                                        .start();

                                adapter.notifyItemInserted(datum.size() - 1);
                            }else{
                                DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference();
                                rootRef.child("constructors").orderByChild("lastSeasonPos").addValueEventListener(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                                        datum.add(new driversList("","","","","","","",
                                            true, currentSeason));
                                        for (DataSnapshot child: snapshot.getChildren()) {
                                            String constructorId = child.child("constructorId").getValue(String.class);
                                            String constructorsName = child.child("name").getValue(String.class);

                                            rootRef.child("driverLineUp/season/" + currentSeason + "/" + constructorId).addValueEventListener(new ValueEventListener() {
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
                                                                        true, currentSeason);
                                                                datum.add(smth);

                                                                hideShimmer(recyclerView, shimmerFrameLayout);
                                                                driversStandings_2024.animate()
                                                                        .setDuration(500)
                                                                        .withEndAction(() -> {
                                                                            driversStandings_2024.setVisibility(View.VISIBLE);
                                                                        })
                                                                        .start();

                                                                driverStndLine.animate()
                                                                        .setDuration(500)
                                                                        .withEndAction(() -> {
                                                                            driverStndLine.setVisibility(View.VISIBLE);
                                                                        })
                                                                        .start();

                                                                driversStandings_2025.animate()
                                                                        .setDuration(500)
                                                                        .withEndAction(() -> {
                                                                            driversStandings_2025.setVisibility(View.VISIBLE);
                                                                        })
                                                                        .start();

                                                                adapter.notifyItemInserted(datum.size() - 1);
                                                            }

                                                            @Override
                                                            public void onCancelled(@NonNull DatabaseError databaseError) {
                                                                Log.e("driverStandingsError", databaseError.getMessage());
                                                            }
                                                        };
                                                        driverRef.addListenerForSingleValueEvent(driversValueEventListener);
                                                    }
                                                }

                                                @Override
                                                public void onCancelled(@NonNull DatabaseError error) {
                                                    Log.e("driverStandingsError", error.getMessage());
                                                }
                                            });
                                        }
                                    }
                                    @Override
                                    public void onCancelled(@NonNull DatabaseError error) {
                                        Log.e("driverStandingsError", error.getMessage());
                                    }
                                });
                            }
                        } catch (JSONException e) {
                            Log.e("driverStandingsError", e.getMessage());
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Toast.makeText(driversStandingsActivity.this, error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
        queue.add(jsonObjectRequest2);
    }
}
