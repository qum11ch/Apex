package com.example.f1app;

import static android.app.PendingIntent.getActivity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.util.Log;
import android.util.TimeUtils;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

public class futureRaceActivity extends AppCompatActivity {
    Button showDriverButton, showDriverStanding, showTeams, showHomePage, showAccount;

    private List<scheduleData> datum;
    private ImageButton backButton;
    private ImageView imageView;
    private Runnable runnable;
    private Handler handler = new Handler();
    private TextView futureRaceTitle;
    private ViewPager2 myViewPager2;
    private raceViewPagerAdapter raceAdapter ;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.future_race_page);

        futureRaceTitle = findViewById(R.id.futureRaceTitle);
        Bundle bundle = getIntent().getExtras();

        String mCircuitId = bundle.getString("circuitId");
        String mRaceName = bundle.getString("raceName");
        String mFutureRaceStartDay = bundle.getString("futureRaceStartDay");
        String mFutureRaceEndDay = bundle.getString("futureRaceEndDay");
        String mFutureRaceStartMonth = bundle.getString("futureRaceStartMonth");
        String mFutureRaceEndMonth = bundle.getString("futureRaceEndMonth");
        String mRound = bundle.getString("roundCount");
        String mCountry = bundle.getString("raceCountry");
        String mDate = bundle.getString("dateStart");


        Log.i("mDate", "" + mDate);
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-M-d");
        LocalDate dateStart = LocalDate.parse(mDate, dateFormatter);
        String gpYear = dateStart.format(DateTimeFormatter.ofPattern("yyyy")).toString();


        Bundle scheduleBundle = new Bundle();
        scheduleBundle.putString("circuitId", mCircuitId);
        scheduleBundle.putString("raceName", mRaceName);
        scheduleBundle.putString("futureRaceStartDay", mFutureRaceStartDay);
        scheduleBundle.putString("futureRaceEndDay", mFutureRaceEndDay);
        scheduleBundle.putString("futureRaceStartMonth", mFutureRaceStartMonth);
        scheduleBundle.putString("futureRaceEndMonth", mFutureRaceEndMonth);
        scheduleBundle.putString("roundCount", mRound);
        scheduleBundle.putString("raceCountry", mCountry);
        scheduleBundle.putString("gpYear", gpYear);

        Bundle circuitBundle = new Bundle();
        circuitBundle.putString("circuitId", mCircuitId);
        circuitBundle.putString("raceName", mRaceName);
        circuitBundle.putString("gpYear", gpYear);
        circuitBundle.putString("raceCountry", mCountry);

        init(scheduleBundle, circuitBundle);

        futureRaceTitle.setText(mRaceName);


        showDriverButton = (Button) findViewById(R.id.showDriver);
        showDriverButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(futureRaceActivity.this, driversStandingsActivity.class);
                futureRaceActivity.this.startActivity(intent);
            }
        });

        showDriverStanding = (Button) findViewById(R.id.showStandingsDriver);
        showDriverStanding.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(futureRaceActivity.this, schuduleActivity.class);
                futureRaceActivity.this.startActivity(intent);
            }
        });

        showHomePage = (Button) findViewById(R.id.showHomePage);
        showHomePage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(futureRaceActivity.this, MainActivity.class);
                futureRaceActivity.this.startActivity(intent);
            }
        });

        showTeams = (Button) findViewById(R.id.showTeams);
        showTeams.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(futureRaceActivity.this, teamsStandingsActivity.class);
                futureRaceActivity.this.startActivity(intent);
            }
        });

        showAccount = (Button) findViewById(R.id.showAccount);
        showAccount.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(futureRaceActivity.this, LogInActivity.class);
                futureRaceActivity.this.startActivity(intent);
            }
        });

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

    }

    protected void onStop() {
        super.onStop();
        handler.removeCallbacks(runnable);
    }

    public void getImage(String raceName) {
        RequestQueue volleyQueue2 = Volley.newRequestQueue(futureRaceActivity.this);
        String url2 = "https://en.wikipedia.org/w/api.php?action=query&format=json&formatversion=2&prop=pageimages|pageterms&piprop=thumbnail&pithumbsize=600&titles="
                + raceName;
        JsonObjectRequest jsonObjectRequest2 = new JsonObjectRequest(
                Request.Method.GET,
                url2,
                null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response2) {
                        try {
                            JSONObject query = response2.getJSONObject("query");
                            JSONArray pages = query.getJSONArray("pages");
                            String source;
                            for (int i = 0; i < pages.length(); i++) {
                                if (pages.getJSONObject(i).has("thumbnail")) {
                                    JSONObject thumbnail = pages.getJSONObject(i).
                                            getJSONObject("thumbnail");
                                    source = thumbnail.getString("source");
                                    GlideApp.with(futureRaceActivity.this)
                                            .load(source)
                                            .placeholder(R.drawable.f1)
                                            .into(imageView);
                                } else {
                                    GlideApp.with(futureRaceActivity.this)
                                            .load(R.drawable.f1)
                                            .into(imageView);
                                }
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Toast.makeText(futureRaceActivity.this, error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
        volleyQueue2.add(jsonObjectRequest2);
    }


    private void init(Bundle scheduleBundle, Bundle circuitBundle) {
        myViewPager2 = findViewById(R.id.viewPager2);
        raceAdapter = new raceViewPagerAdapter(this);
        futureRaceScheduleFragment scheduleFragment = new futureRaceScheduleFragment();
        scheduleFragment.setArguments(scheduleBundle);
        raceAdapter.addFragment(scheduleFragment);
        futureRaceCircuitFragment circuitFragment = new futureRaceCircuitFragment();
        circuitFragment.setArguments(circuitBundle);
        raceAdapter.addFragment(circuitFragment);
        myViewPager2.setAdapter(raceAdapter);
        TabLayout tabLayout = findViewById(R.id.tab_layout);
        TabLayoutMediator tabLayoutMediator= new TabLayoutMediator(tabLayout, myViewPager2, new TabLayoutMediator.TabConfigurationStrategy(){
            @Override
            public void onConfigureTab(TabLayout.Tab tab, int position) {
                if (position == 0){
                    tab.setText("Schedule");
                }
                else{
                    tab.setText("Circuit");
                }
            }
        });
        tabLayoutMediator.attach();
    }
}