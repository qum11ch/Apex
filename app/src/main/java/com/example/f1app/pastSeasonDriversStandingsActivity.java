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
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class pastSeasonDriversStandingsActivity extends AppCompatActivity {

    private List<driversList> datum;
    private RecyclerView recyclerView;
    private pastSeasonDriversStandingsAdapter adapter;
    private ShimmerFrameLayout shimmerFrameLayout;
    private SwipeRefreshLayout swipeLayout;
    private String mCurrentSeason;
    private FirebaseStorage storage;
    private StorageReference storageRef;
    private DatabaseReference rootRef;

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

        if(!getIntent().getExtras().isEmpty()) {
            Bundle bundle = getIntent().getExtras();
            String mSeason = bundle.getString("season");
            mCurrentSeason = bundle.getString("currentSeason");

            shimmerFrameLayout = findViewById(R.id.shimmer_layout);
            shimmerFrameLayout.startShimmer();

            recyclerView = findViewById(R.id.recyclerview_currentDrivers);
            recyclerView.setHasFixedSize(true);

            storage = FirebaseStorage.getInstance();
            storageRef = storage.getReference();
            rootRef = FirebaseDatabase.getInstance().getReference();

            TextView driversHeader = findViewById(R.id.driversHeader);

            String headerText;
            if (Locale.getDefault().getLanguage().equals("ru")){
                headerText = getString(R.string.past_season_drivers) + " " + mSeason;
            }else{
                headerText = mSeason + " " + getString(R.string.past_season_drivers);
            }
            driversHeader.setText(headerText);

            datum = new ArrayList<>();

            adapter = new pastSeasonDriversStandingsAdapter(pastSeasonDriversStandingsActivity.this, datum);
            recyclerView.setAdapter(adapter);

            swipeLayout = findViewById(R.id.swipe_layout);
            swipeLayout.setOnRefreshListener(() -> {
                recyclerView.setVisibility(View.GONE);
                shimmerFrameLayout.setVisibility(View.VISIBLE);
                shimmerFrameLayout.startShimmer();
                datum = new ArrayList<>();
                getStanding(mSeason);
                swipeLayout.setRefreshing(false);
            });
            LinearLayoutManager linearLayoutManager3 = new LinearLayoutManager(this);
            recyclerView.setLayoutManager(linearLayoutManager3);

            ImageButton backButton = findViewById(R.id.backButton);
            backButton.setOnClickListener(v -> finish());

            getStanding(mSeason);

            WindowInsetsControllerCompat windowInsetsController =
                    WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
            windowInsetsController.setAppearanceLightStatusBars(false);
        }
    }


    public void getStanding(String seasonYear){
        RequestQueue queue = Volley.newRequestQueue(pastSeasonDriversStandingsActivity.this);
        String url2 = "https://api.jolpi.ca/ergast/f1/" + seasonYear + "/driverstandings/?format=json";
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

                                    StorageReference mDriverImage = storageRef.child("drivers/" + driverCode.toLowerCase() + "_"  + seasonYear + ".png");

                                    driversList smth = new driversList(driverName, driverFamilyName, constructorsName, constructorId, points, placement, driverCode, false, seasonYear);
                                    smth.setImageUrl(mDriverImage);
                                    smth.setCurrentSeason(mCurrentSeason);
                                    rootRef.child("constructors").child(constructorId).addListenerForSingleValueEvent(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                                            String mTeamColor = "#" + snapshot.child("darkColor").getValue(String.class);
                                            smth.setTeamColor(mTeamColor);
                                        }

                                        @Override
                                        public void onCancelled(@NonNull DatabaseError error) {
                                            smth.setTeamColor("#ffffff");
                                            Log.e(String.valueOf(pastSeasonDriversStandingsActivity.this), "Team color information getting error:" + error.getMessage());
                                        }
                                    });
                                    datum.add(smth);
                                }
                            }
                            hideShimmer(recyclerView, shimmerFrameLayout);
                            adapter.notifyItemChanged(datum.size() - 1);
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }, error -> Toast.makeText(pastSeasonDriversStandingsActivity.this, error.getMessage(), Toast.LENGTH_SHORT).show());
        queue.add(jsonObjectRequest2);
    }
}
