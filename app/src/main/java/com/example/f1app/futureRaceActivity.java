package com.example.f1app;

import static android.app.PendingIntent.getActivity;

import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.viewpager2.widget.ViewPager2;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class futureRaceActivity extends AppCompatActivity {

    private ImageButton backButton;
    private TextView futureRaceTitle;
    private ViewPager2 myViewPager2;
    private viewPagerAdapter adapter ;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.race_page);

        WindowInsetsControllerCompat windowInsetsController =
                WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        windowInsetsController.setAppearanceLightStatusBars(false);

        futureRaceTitle = findViewById(R.id.raceTitile);

        backButton = (ImageButton) findViewById(R.id.backButton);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });


        if (!getIntent().getExtras().isEmpty()){
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
        }
    }

    private void init(Bundle scheduleBundle, Bundle circuitBundle) {
        myViewPager2 = findViewById(R.id.viewPager2);
        adapter = new viewPagerAdapter(this);
        futureRaceScheduleFragment scheduleFragment = new futureRaceScheduleFragment();
        scheduleFragment.setArguments(scheduleBundle);
        adapter.addFragment(scheduleFragment);
        raceCircuitFragment circuitFragment = new raceCircuitFragment();
        circuitFragment.setArguments(circuitBundle);
        adapter.addFragment(circuitFragment);
        myViewPager2.setAdapter(adapter);
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