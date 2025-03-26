package com.example.f1app;

import static com.example.f1app.MainActivity.checkConnection;

import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.RelativeLayout;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.facebook.shimmer.ShimmerFrameLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class savedRacesActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    ImageButton backButton;
    List<savedRacesData> datum;
    savedRacesAdapter adapter;
    private RelativeLayout emptySavedRaceLayout;
    SwipeRefreshLayout swipeLayout;
    ShimmerFrameLayout shimmerFrameLayout;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.saved_races);

        if (!checkConnection(getApplicationContext())){
            startActivity(connectionLostScreen.createShowSplashOnNetworkFailure(savedRacesActivity.this));
        }else{
            startActivity(connectionLostScreen.createIntentHideSplashOnNetworkRecovery(savedRacesActivity.this));
        }

        shimmerFrameLayout = findViewById(R.id.shimmer_layout);
        shimmerFrameLayout.startShimmer();

        recyclerView = findViewById(R.id.recyclerview_savedRaces);
        recyclerView.setHasFixedSize(true);
        LinearLayoutManager linearLayoutManager3 = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(linearLayoutManager3);

        swipeLayout = findViewById(R.id.swipe_layout);
        swipeLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                emptySavedRaceLayout.setVisibility(View.GONE);
                recyclerView.setVisibility(View.GONE);
                shimmerFrameLayout.setVisibility(View.VISIBLE);
                shimmerFrameLayout.startShimmer();
                putRaces();
                swipeLayout.setRefreshing(false);
            }
        });

        putRaces();

        emptySavedRaceLayout = findViewById(R.id.emptySavedRace_layout);

        //putRaces();

        WindowInsetsControllerCompat windowInsetsController =
                WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        windowInsetsController.setAppearanceLightStatusBars(false);

        backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    //@Override
    //public void onResume() {
    //    super.onResume();
    //    putRaces();
    //}

    private void putRaces(){
        datum = new ArrayList<>();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference();
        rootRef.child("users").orderByChild("userId")
                .equalTo(user.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        for(DataSnapshot userSnap: snapshot.getChildren()){
                            String username = userSnap.getKey();
                            rootRef.child("savedRaces").addValueEventListener(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot snapshot) {
                                    if(!snapshot.hasChild(username)) {
                                        Handler handler = new Handler();
                                        handler.postDelayed(()->{
                                            recyclerView.setVisibility(View.GONE);
                                            shimmerFrameLayout.setVisibility(View.GONE);
                                            shimmerFrameLayout.stopShimmer();
                                            emptySavedRaceLayout.setVisibility(View.VISIBLE);
                                        },500);
                                    }else {
                                        Handler handler = new Handler();
                                        handler.postDelayed(()->{
                                            shimmerFrameLayout.setVisibility(View.GONE);
                                            shimmerFrameLayout.stopShimmer();
                                            emptySavedRaceLayout.setVisibility(View.GONE);
                                            recyclerView.setVisibility(View.VISIBLE);
                                        },500);
                                    }
                                    for (DataSnapshot savedRaceSnap: snapshot.child(username).getChildren()){
                                            String raceName = savedRaceSnap.child("raceName").getValue(String.class);
                                            String raceSeason = savedRaceSnap.child("raceSeason").getValue(String.class);
                                            String saveDate = savedRaceSnap.child("saveDate").getValue(String.class);
                                            savedRacesData savedRacesData = new savedRacesData(raceName, raceSeason, saveDate);
                                            datum.add(savedRacesData);
                                    }
                                    adapter = new savedRacesAdapter(savedRacesActivity.this, datum);
                                    recyclerView.setAdapter(adapter);
                                }
                                @Override
                                public void onCancelled(@NonNull DatabaseError error) {
                                    Log.e("concludedRacePage", "Drivers error:" + error.getMessage());
                                }
                            });
                        }
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e("concludedRacePage", "Drivers error:" + error.getMessage());
                    }
                });
    }
}
