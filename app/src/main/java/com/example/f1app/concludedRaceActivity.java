package com.example.f1app;

import static com.example.f1app.MainActivity.checkConnection;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class concludedRaceActivity extends AppCompatActivity {

    private ImageButton backButton;
    private TextView raceTitle;
    private ViewPager2 myViewPager2;
    private viewPagerAdapter adapter ;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.race_page);

        raceTitle = findViewById(R.id.raceTitile);

        WindowInsetsControllerCompat windowInsetsController =
                WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        windowInsetsController.setAppearanceLightStatusBars(false);

        if (!checkConnection(getApplicationContext())){
            startActivity(connectionLostScreen.createShowSplashOnNetworkFailure(concludedRaceActivity.this));
        }else{
            startActivity(connectionLostScreen.createIntentHideSplashOnNetworkRecovery(concludedRaceActivity.this));
        }

        backButton = (ImageButton) findViewById(R.id.backButton);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        if (!getIntent().getExtras().isEmpty()){
            Bundle bundle = getIntent().getExtras();
            String mCircuitName = bundle.getString("circuitName");
            String mRaceName = bundle.getString("raceName");
            String mRaceStartDay = bundle.getString("raceStartDay");
            String mRaceEndDay = bundle.getString("raceEndDay");
            String mRaceStartMonth = bundle.getString("raceStartMonth");
            String mRaceEndMonth = bundle.getString("raceEndMonth");
            String mRound = bundle.getString("roundCount");
            String mCountry = bundle.getString("raceCountry");
            String mDate = bundle.getString("dateStart");
            String mFirstPlaceCode = bundle.getString("firstPlaceCode");
            String mSecondPlaceCode = bundle.getString("secondPlaceCode");
            String mThirdPlaceCode = bundle.getString("thirdPlaceCode");


            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-M-d");
            LocalDate dateStart = LocalDate.parse(mDate, dateFormatter);
            String gpYear = dateStart.format(DateTimeFormatter.ofPattern("yyyy")).toString();

            DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference();
            rootRef.child("circuits").orderByChild("circuitName").equalTo(mCircuitName)
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            for (DataSnapshot ds: snapshot.getChildren()){
                                String mCircuitId = ds.getKey();

                                Bundle scheduleBundle = new Bundle();
                                scheduleBundle.putString("circuitId", mCircuitId);
                                scheduleBundle.putString("raceName", mRaceName);
                                scheduleBundle.putString("raceStartDay", mRaceStartDay);
                                scheduleBundle.putString("raceEndDay", mRaceEndDay);
                                scheduleBundle.putString("raceStartMonth", mRaceStartMonth);
                                scheduleBundle.putString("raceEndMonth", mRaceEndMonth);
                                scheduleBundle.putString("roundCount", mRound);
                                scheduleBundle.putString("raceCountry", mCountry);
                                scheduleBundle.putString("gpYear", gpYear);
                                scheduleBundle.putString("firstPlaceCode", mFirstPlaceCode);
                                scheduleBundle.putString("secondPlaceCode", mSecondPlaceCode);
                                scheduleBundle.putString("thirdPlaceCode", mThirdPlaceCode);


                                Bundle circuitBundle = new Bundle();
                                circuitBundle.putString("circuitId", mCircuitId);
                                circuitBundle.putString("raceName", mRaceName);
                                circuitBundle.putString("gpYear", gpYear);
                                circuitBundle.putString("raceCountry", mCountry);

                                init(scheduleBundle, circuitBundle);
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            Log.e("futureActivityFirebaseError", error.getMessage());
                        }
                    });

            raceTitle.setText(mRaceName);
        }
    }

    private void init(Bundle scheduleBundle, Bundle circuitBundle) {
        myViewPager2 = findViewById(R.id.viewPager2);
        adapter = new viewPagerAdapter(this);
        concludedRaceScheduleFragment scheduleFragment = new concludedRaceScheduleFragment();
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
    }
}