package com.example.f1app;

import static com.example.f1app.MainActivity.checkConnection;
import static com.example.f1app.MainActivity.getStringByName;

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
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONException;
import org.json.JSONObject;

public class raceResultsActivity extends AppCompatActivity {

    private ImageButton backButton;
    private TextView raceTitle;
    private ViewPager2 myViewPager2;
    private viewPagerAdapter adapter ;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.race_results_page);

        raceTitle = findViewById(R.id.raceTitle);

        if (!checkConnection(getApplicationContext())){
            startActivity(connectionLostScreen.createShowSplashOnNetworkFailure(raceResultsActivity.this));
        }else{
            startActivity(connectionLostScreen.createIntentHideSplashOnNetworkRecovery(raceResultsActivity.this));
        }

        if(!getIntent().getExtras().isEmpty()){
            Bundle bundle = getIntent().getExtras();

            String mCircuitId = bundle.getString("circuitId");
            String mSeason = bundle.getString("season");
            String mRaceName = bundle.getString("raceName");

            Bundle resultsBundle = new Bundle();
            resultsBundle.putString("circuitId", mCircuitId);
            resultsBundle.putString("raceName", mRaceName);
            resultsBundle.putString("season", mSeason);

            String localeRaceName = mRaceName.toLowerCase().replaceAll("\\s+", "_");
            String pastRaceName = this.getString(getStringByName(localeRaceName + "_text")) + " " + mSeason;

            raceTitle.setText(pastRaceName);

            backButton = (ImageButton) findViewById(R.id.backButton);
            backButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    finish();
                }
            });

            WindowInsetsControllerCompat windowInsetsController =
                    WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
            windowInsetsController.setAppearanceLightStatusBars(false);

            DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference();

            Integer year = Integer.parseInt(mSeason);

            if (year < 2024){
                RequestQueue queue = Volley.newRequestQueue(raceResultsActivity.this);
                String url2 = "https://api.jolpi.ca/ergast/f1/" + mSeason + "/circuits/" + mCircuitId + "/sprint/?format=json";
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
                                        myViewPager2 = findViewById(R.id.viewPager2);
                                        adapter = new viewPagerAdapter(raceResultsActivity.this);
                                        raceResultsRaceFragment raceFragment = new raceResultsRaceFragment();
                                        raceFragment.setArguments(resultsBundle);
                                        adapter.addFragment(raceFragment);
                                        raceResultsQualiFragment qualiFragment = new raceResultsQualiFragment();
                                        qualiFragment.setArguments(resultsBundle);
                                        adapter.addFragment(qualiFragment);
                                        raceResultsSprintFragment sprintFragment = new raceResultsSprintFragment();
                                        sprintFragment.setArguments(resultsBundle);
                                        adapter.addFragment(sprintFragment);
                                        myViewPager2.setAdapter(adapter);
                                        TabLayout tabLayout = findViewById(R.id.tab_layout);
                                        TabLayoutMediator tabLayoutMediator= new TabLayoutMediator(tabLayout, myViewPager2, new TabLayoutMediator.TabConfigurationStrategy(){
                                            @Override
                                            public void onConfigureTab(TabLayout.Tab tab, int position) {
                                                switch(position){
                                                    case 0:
                                                        tab.setText(R.string.race_text);
                                                        break;
                                                    case 1:
                                                        tab.setText(R.string.quali_text);
                                                        break;
                                                    case 2:
                                                        tab.setText(R.string.sprint_text);
                                                        break;
                                                }
                                            }
                                        });
                                        tabLayoutMediator.attach();
                                    }else{
                                        init(resultsBundle);
                                    }
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            }
                        }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Toast.makeText(raceResultsActivity.this, error.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
                queue.add(jsonObjectRequest2);
            }else{
                rootRef.child("schedule/season/" + mSeason).child(mRaceName).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        String sprintRaceDate = snapshot.child("Sprint").child("sprintRaceDate").getValue(String.class);
                        if (!sprintRaceDate.equals("N/A")){
                            myViewPager2 = findViewById(R.id.viewPager2);
                            adapter = new viewPagerAdapter(raceResultsActivity.this);
                            raceResultsRaceFragment raceFragment = new raceResultsRaceFragment();
                            raceFragment.setArguments(resultsBundle);
                            adapter.addFragment(raceFragment);
                            raceResultsQualiFragment qualiFragment = new raceResultsQualiFragment();
                            qualiFragment.setArguments(resultsBundle);
                            adapter.addFragment(qualiFragment);
                            raceResultsSprintFragment sprintFragment = new raceResultsSprintFragment();
                            sprintFragment.setArguments(resultsBundle);
                            adapter.addFragment(sprintFragment);
                            myViewPager2.setAdapter(adapter);
                            TabLayout tabLayout = findViewById(R.id.tab_layout);
                            TabLayoutMediator tabLayoutMediator= new TabLayoutMediator(tabLayout, myViewPager2, new TabLayoutMediator.TabConfigurationStrategy(){
                                @Override
                                public void onConfigureTab(TabLayout.Tab tab, int position) {
                                    switch(position){
                                        case 0:
                                            tab.setText(R.string.race_text);
                                            break;
                                        case 1:
                                            tab.setText(R.string.quali_text);
                                            break;
                                        case 2:
                                            tab.setText(R.string.sprint_text);
                                            break;
                                    }
                                }
                            });
                            tabLayoutMediator.attach();
                        }else{
                            init(resultsBundle);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e("raceResultsQualiAdapter: Fatal error in Firebase getting team color", " " + error.getMessage());
                    }
                });
            }
        }
    }


    private void init(Bundle raceBundle) {
        myViewPager2 = findViewById(R.id.viewPager2);
        adapter = new viewPagerAdapter(this);
        raceResultsRaceFragment raceFragment = new raceResultsRaceFragment();
        raceFragment.setArguments(raceBundle);
        adapter.addFragment(raceFragment);
        raceResultsQualiFragment qualiFragment = new raceResultsQualiFragment();
        qualiFragment.setArguments(raceBundle);
        adapter.addFragment(qualiFragment);
        myViewPager2.setAdapter(adapter);

        View child = myViewPager2.getChildAt(0);
        if (child instanceof RecyclerView) {
            child.setOverScrollMode(View.OVER_SCROLL_NEVER);
        }

        TabLayout tabLayout = findViewById(R.id.tab_layout);
        TabLayoutMediator tabLayoutMediator= new TabLayoutMediator(tabLayout, myViewPager2, new TabLayoutMediator.TabConfigurationStrategy(){
            @Override
            public void onConfigureTab(TabLayout.Tab tab, int position) {
                if (position == 0){
                    tab.setText(R.string.race_text);
                }
                else{
                    tab.setText(R.string.quali_text);
                }
            }
        });
        tabLayoutMediator.attach();
    }
}