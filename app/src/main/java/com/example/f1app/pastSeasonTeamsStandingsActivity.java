package com.example.f1app;

import static com.example.f1app.MainActivity.checkConnection;
import static com.example.f1app.MainActivity.hideShimmer;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class pastSeasonTeamsStandingsActivity extends AppCompatActivity {
    private List<teamsList> datum;
    private RecyclerView recyclerView;
    private pastSeasonTeamsStandingsAdapter adapter;
    private ShimmerFrameLayout shimmerFrameLayout;
    private SwipeRefreshLayout swipeLayout;
    private String mCurrentSeason;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.past_season_teams_standing_page);

        if (!checkConnection(getApplicationContext())){
            startActivity(connectionLostScreen.createShowSplashOnNetworkFailure(pastSeasonTeamsStandingsActivity.this));
        }else{
            startActivity(connectionLostScreen.createIntentHideSplashOnNetworkRecovery(pastSeasonTeamsStandingsActivity.this));
        }

        if(!getIntent().getExtras().isEmpty()) {
            Bundle bundle = getIntent().getExtras();
            String mSeason = bundle.getString("season");
            mCurrentSeason = bundle.getString("currentSeason");

            shimmerFrameLayout = findViewById(R.id.shimmer_layout);
            shimmerFrameLayout.startShimmer();

            TextView teamsHeader = findViewById(R.id.teamsHeader);
            String headerText;
            if (Locale.getDefault().getLanguage().equals("ru")){
                headerText = getString(R.string.past_season_teams) + " " + mSeason;
            }else{
                headerText = mSeason + " " + getString(R.string.past_season_teams);
            }
            teamsHeader.setText(headerText);

            recyclerView = findViewById(R.id.recyclerview_currentTeams);
            recyclerView.setHasFixedSize(true);
            LinearLayoutManager linearLayoutManager3 = new LinearLayoutManager(this);
            recyclerView.setLayoutManager(linearLayoutManager3);

            datum = new ArrayList<>();

            adapter = new pastSeasonTeamsStandingsAdapter(pastSeasonTeamsStandingsActivity.this, datum);
            recyclerView.setAdapter(adapter);

            swipeLayout = findViewById(R.id.swipe_layout);
            swipeLayout.setOnRefreshListener(() -> {
                recyclerView.setVisibility(View.GONE);
                shimmerFrameLayout.setVisibility(View.VISIBLE);
                shimmerFrameLayout.startShimmer();
                datum = new ArrayList<>();
                getTeamStanding(mSeason);
                swipeLayout.setRefreshing(false);
                adapter.notifyDataSetChanged();
            });


            ImageButton backButton = findViewById(R.id.backButton);
            backButton.setOnClickListener(v -> finish());

            getTeamStanding(mSeason);

            WindowInsetsControllerCompat windowInsetsController =
                    WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
            windowInsetsController.setAppearanceLightStatusBars(false);

        }
    }

    private void getTeamStanding(String year){
        RequestQueue queue = Volley.newRequestQueue(pastSeasonTeamsStandingsActivity.this);
        String url2 = "https://api.jolpi.ca/ergast/f1/" + year + "/constructorstandings/?format=json";
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
                                    //if (constructorName.equals("Sauber")){
                                    //        constructorName = "Kick Sauber";
                                    //}
                                    DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference();
                                    rootRef.child("driverLineUp/season/" + year + "/" + constructorId).addValueEventListener(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                                            ArrayList<String> teamDrivers = new ArrayList<>();
                                            for (DataSnapshot driverDataSnapshot : snapshot.child("drivers").getChildren()) {
                                                String driverFullname = driverDataSnapshot.getKey();
                                                teamDrivers.add(driverFullname);
                                            }
                                            teamsList smth = new teamsList(constructorName, position, points, constructorId, false);
                                            smth.setSeason(year);
                                            smth.setDrivers(teamDrivers);
                                            smth.setCurrentSeason(mCurrentSeason);
                                            datum.add(smth);
                                            hideShimmer(recyclerView, shimmerFrameLayout);
                                            adapter.notifyItemChanged(datum.size() - 1);
                                        }

                                        @Override
                                        public void onCancelled(@NonNull DatabaseError error) {
                                            Log.e("teamStandingsError", error.getMessage());
                                        }
                                    });
                                }
                            }

                        }
                    } catch (JSONException e) {
                        Toast.makeText(pastSeasonTeamsStandingsActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }, error -> Toast.makeText(pastSeasonTeamsStandingsActivity.this, error.getMessage(), Toast.LENGTH_SHORT).show());
        queue.add(jsonObjectRequest2);
    }
}
