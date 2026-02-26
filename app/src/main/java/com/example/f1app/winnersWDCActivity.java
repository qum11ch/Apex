package com.example.f1app;

import static com.example.f1app.MainActivity.checkLightTheme;
import static com.example.f1app.MainActivity.getStringByName;
import static com.example.f1app.MainActivity.hideShimmer;

import android.os.Bundle;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

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

import java.util.ArrayList;
import java.util.List;

public class winnersWDCActivity extends AppCompatActivity {
    private static final int POINTS_FOR_SPRINT = 8 + 25;
    private static final int POINTS_FOR_CONVENTIONAL = 25;
    private List<driversWDCPointsList> datum;
    private RecyclerView recyclerView;
    private TextView round, raceName, convEventsCount, sprintEventsCount, maxPointsCount;
    private ShimmerFrameLayout shimmerFrameLayout;
    private winnersWDCAdapter adapter;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.winners_wdc_page);

        recyclerView = findViewById(R.id.recyclerview_winnersWDC);
        recyclerView.setHasFixedSize(true);
        LinearLayoutManager linearLayoutManager3 = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(linearLayoutManager3);

        datum = new ArrayList<>();
        adapter = new winnersWDCAdapter(winnersWDCActivity.this, datum);
        recyclerView.setAdapter(adapter);

        round = findViewById(R.id.round);
        raceName = findViewById(R.id.raceName);
        convEventsCount = findViewById(R.id.conv_events_count);
        sprintEventsCount = findViewById(R.id.sprint_events_count);
        maxPointsCount = findViewById(R.id.max_points_count);

        shimmerFrameLayout = findViewById(R.id.shimmer_layout);
        shimmerFrameLayout.startShimmer();

        RelativeLayout fastestLapLayout = findViewById(R.id.poleLap_layout);
        if (!checkLightTheme(winnersWDCActivity.this)){
            fastestLapLayout.setBackground(ContextCompat.getDrawable(winnersWDCActivity.this, R.drawable.background_striped_lines_item_night));
        }

        String currentSeason = getIntent().getStringExtra("currentSeason");

        DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference();
        rootRef.child("status/last_update").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Integer mRound = snapshot.child("last_round").getValue(Integer.class);
                String lastRace = snapshot.child("last_race").getValue(String.class);

                String[] lastRaceSplit = lastRace.split(" ");
                if (lastRaceSplit[0].equals(currentSeason)){
                    round.setText(String.valueOf(mRound));
                    calculateMaxPoints(mRound, currentSeason);

                    String[] separateRaceName = lastRace.split("\\s+");
                    String year = separateRaceName[0];

                    String mRaceName = lastRace.substring(5);
                    String localeRaceName = mRaceName.toLowerCase().replaceAll("\\s+", "_");
                    String lastRaceName = getString(getStringByName(localeRaceName + "_text")) + " " + year;

                    raceName.setText(lastRaceName);
                }else{
                    mRound = 0;
                    round.setText(String.valueOf(mRound));
                    calculateMaxPoints(mRound, currentSeason);
                    String mRaceName = "Australian Grand Prix";

                    String localeRaceName = mRaceName.toLowerCase().replaceAll("\\s+", "_");
                    String lastRaceName = getString(getStringByName(localeRaceName + "_text")) + " " + currentSeason;
                    raceName.setText(lastRaceName);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(winnersWDCActivity.this, error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

        ImageButton backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> finish());

        WindowInsetsControllerCompat windowInsetsController =
                WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        windowInsetsController.setAppearanceLightStatusBars(false);
    }

    private void calculateMaxPoints(Integer lastRound, String currentSeason){
        DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference();
        rootRef.child("schedule/season/" + currentSeason).orderByChild("round").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                int sprintRacesCount = 0;
                int convRacesCount = 0;

                for (DataSnapshot ds : snapshot.getChildren()) {
                    Integer round = ds.child("round").getValue(Integer.class);

                    if (round > lastRound){
                        String sprintDate = ds.child("Sprint/sprintRaceDate").getValue(String.class);
                        if (sprintDate.equals("N/A")){
                            convRacesCount += 1;
                        }else{
                            sprintRacesCount += 1;
                        }
                    }
                }
                int maxPoints = sprintRacesCount * POINTS_FOR_SPRINT + convRacesCount * POINTS_FOR_CONVENTIONAL;

                convEventsCount.setText(String.valueOf(convRacesCount));
                sprintEventsCount.setText(String.valueOf(sprintRacesCount));
                String mMaxPointsEvents = maxPoints  + " " + getApplicationContext().getString(R.string.pts_header).toLowerCase();
                maxPointsCount.setText(mMaxPointsEvents);
                getDriverStandings(currentSeason, maxPoints);
            }@Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(winnersWDCActivity.this, error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void getDriverStandings(String season, int maxPoints) {
        RequestQueue queue = Volley.newRequestQueue(winnersWDCActivity.this);
        String url2 = "https://api.jolpi.ca/ergast/f1/" + season + "/driverstandings/?format=json";
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
                            if (!total.equals("0")) {
                                JSONObject StandingsTable = MRData.getJSONObject("StandingsTable");
                                JSONArray StandingsLists = StandingsTable.getJSONArray("StandingsLists");
                                for (int i = 0; i < StandingsLists.length(); i++) {
                                    JSONArray DriverStandings = StandingsLists.getJSONObject(i)
                                            .getJSONArray("DriverStandings");
                                    int leaderPoints = 0;
                                    for (int j = 0; j < DriverStandings.length(); j++) {
                                        String pointsText = DriverStandings.getJSONObject(j).getString("points");
                                        int points = Integer.parseInt(pointsText);
                                        String placement = DriverStandings.getJSONObject(j).getString("positionText");
                                        String driverName = DriverStandings.getJSONObject(j)
                                                .getJSONObject("Driver").getString("givenName");
                                        String driverFamilyName = DriverStandings.getJSONObject(j)
                                                .getJSONObject("Driver").getString("familyName");

                                        JSONArray Constructors = DriverStandings.getJSONObject(j).getJSONArray("Constructors");
                                        String constructorsName = Constructors.getJSONObject(Constructors.length() - 1).getString("name");
                                        String constructorId = Constructors.getJSONObject(Constructors.length() - 1).getString("constructorId");

                                        if (j == 0){
                                            leaderPoints = points;
                                        }

                                        int driverMaxPoints = points + maxPoints;
                                        boolean canWin = driverMaxPoints > leaderPoints;

                                        driversWDCPointsList smth = new driversWDCPointsList(driverName,
                                                driverFamilyName, constructorsName, constructorId,
                                                season, points, driverMaxPoints, canWin, placement);
                                        datum.add(smth);
                                    }
                                }
                                hideShimmer(recyclerView, shimmerFrameLayout);
                                adapter.notifyItemChanged(datum.size() - 1);
                            }else{
                                DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference();
                                final int[] placementHolder = {1};
                                rootRef.child("constructors").orderByChild("lastSeasonPos").addValueEventListener(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                                        for (DataSnapshot child: snapshot.getChildren()) {
                                            String constructorId = child.child("constructorId").getValue(String.class);
                                            String constructorsName = child.child("name").getValue(String.class);
                                            rootRef.child("driverLineUp/season/" + season + "/" + constructorId).addValueEventListener(new ValueEventListener() {
                                                @Override
                                                public void onDataChange(@NonNull DataSnapshot snapshot) {
                                                    datum.clear();
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
                                                                driversWDCPointsList smth = new driversWDCPointsList(driverName,
                                                                        driverFamilyName, constructorsName, constructorId,
                                                                        season, 0, maxPoints, true, String.valueOf(placementHolder[0]));
                                                                datum.add(smth);
                                                                hideShimmer(recyclerView, shimmerFrameLayout);
                                                                adapter.notifyItemChanged(datum.size() - 1);
                                                                placementHolder[0]++;
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
                            e.printStackTrace();
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Toast.makeText(winnersWDCActivity.this, error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
        queue.add(jsonObjectRequest2);
    }
}
