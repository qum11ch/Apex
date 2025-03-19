package com.example.f1app;

import static com.example.f1app.MainActivity.getConnectionType;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class savedRacesActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    ImageButton backButton;
    List<savedRacesData> datum;
    savedRacesAdapter adapter;
    private RelativeLayout emptySavedRaceLayout;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.saved_races);

        if (getConnectionType(getApplicationContext())==0){
            startActivity(connectionLostScreen.createShowSplashOnNetworkFailure(savedRacesActivity.this));
        }else{
            startActivity(connectionLostScreen.createIntentHideSplashOnNetworkRecovery(savedRacesActivity.this));
        }

        recyclerView = findViewById(R.id.recyclerview_savedRaces);
        recyclerView.setHasFixedSize(true);
        LinearLayoutManager linearLayoutManager3 = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(linearLayoutManager3);

        emptySavedRaceLayout = findViewById(R.id.emptySavedRace_layout);

        //putRaces();

        savedRacesAdapter adapter;

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

    @Override
    public void onResume() {
        super.onResume();
        putRaces();
    }

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
                            DateTimeFormatter formatterUpdate = DateTimeFormatter.ofPattern("d/MM/uuuu");
                            rootRef.child("savedRaces").addValueEventListener(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot snapshot) {
                                    if(!snapshot.hasChild(username)) {
                                        emptySavedRaceLayout.setVisibility(View.VISIBLE);
                                        recyclerView.setVisibility(View.INVISIBLE);
                                    }else {
                                        emptySavedRaceLayout.setVisibility(View.INVISIBLE);
                                        recyclerView.setVisibility(View.VISIBLE);
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
