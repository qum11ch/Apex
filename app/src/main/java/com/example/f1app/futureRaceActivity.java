package com.example.f1app;

import static com.example.f1app.MainActivity.checkConnection;
import static com.example.f1app.MainActivity.getStringByName;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

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

        if (!checkConnection(getApplicationContext())){
            startActivity(connectionLostScreen.createShowSplashOnNetworkFailure(futureRaceActivity.this));
        }else{
            startActivity(connectionLostScreen.createIntentHideSplashOnNetworkRecovery(futureRaceActivity.this));
        }

        futureRaceTitle = findViewById(R.id.raceTitile);


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

            if (getIntent().hasExtra("fromNotify")){
                backButton = (ImageButton) findViewById(R.id.backButton);
                backButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent(futureRaceActivity.this, MainActivity.class);
                        futureRaceActivity.this.startActivity(intent);
                        overridePendingTransition(0, 0);
                    }
                });
            }else{
                backButton = (ImageButton) findViewById(R.id.backButton);
                backButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        finish();
                    }
                });
            }


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

            String localeRaceName = mRaceName.toLowerCase().replaceAll("\\s+", "_");
            String futureRaceName = this.getString(getStringByName(localeRaceName + "_text"));

            futureRaceTitle.setText(futureRaceName);
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
                    tab.setText(R.string.schedule_text);
                }
                else{
                    tab.setText(R.string.circuit_text);
                }
            }
        });
        tabLayoutMediator.attach();

        View child = myViewPager2.getChildAt(0);
        if (child instanceof RecyclerView) {
            child.setOverScrollMode(View.OVER_SCROLL_NEVER);
        }
    }
}