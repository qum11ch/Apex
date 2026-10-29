package com.example.f1app;

import static com.example.f1app.MainActivity.checkConnection;
import static com.example.f1app.MainActivity.getStringByName;

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
import androidx.recyclerview.widget.RecyclerView;
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

import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class concludedRaceActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.race_page);
        setupRaceInsets();

        TextView raceTitle = findViewById(R.id.raceTitile);

        WindowInsetsControllerCompat windowInsetsController =
                WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        windowInsetsController.setAppearanceLightStatusBars(false);

        if (!checkConnection(getApplicationContext())){
            startActivity(connectionLostScreen.createShowSplashOnNetworkFailure(concludedRaceActivity.this));
        }else{
            startActivity(connectionLostScreen.createIntentHideSplashOnNetworkRecovery(concludedRaceActivity.this));
        }

        ImageButton backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> finish());

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
            String mEndDate = bundle.getString("dateEnd");
            String mFirstPlaceCode = bundle.getString("firstPlaceCode");
            String mSecondPlaceCode = bundle.getString("secondPlaceCode");
            String mThirdPlaceCode = bundle.getString("thirdPlaceCode");
            Boolean isCanceled = bundle.getBoolean("isCanceled");


            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-M-d");
            LocalDate dateStart = LocalDate.parse(mDate, dateFormatter);
            String gpYear = dateStart.format(DateTimeFormatter.ofPattern("yyyy"));

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
                                scheduleBundle.putBoolean("isCanceled", isCanceled);
                                scheduleBundle.putString("dateStart", mDate);
                                scheduleBundle.putString("dateEnd", mEndDate);

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
            String localeRaceName = mRaceName.toLowerCase().replaceAll("\\s+", "_");
            String pastRaceName = this.getString(getStringByName(localeRaceName + "_text"));
            raceTitle.setText(pastRaceName);
        }
    }

    private void setupRaceInsets() {
        View root = findViewById(R.id.race_root);
        View header = findViewById(R.id.header);

        final int rootLeft = root.getPaddingLeft();
        final int rootTop = root.getPaddingTop();
        final int rootRight = root.getPaddingRight();
        final int rootBottom = root.getPaddingBottom();

        final int headerLeft = header.getPaddingLeft();
        final int headerTop = header.getPaddingTop();
        final int headerRight = header.getPaddingRight();
        final int headerBottom = header.getPaddingBottom();

        ViewCompat.setOnApplyWindowInsetsListener(root, (view, windowInsets) -> {
            Insets safeInsets = windowInsets.getInsets(
                    WindowInsetsCompat.Type.systemBars()
                            | WindowInsetsCompat.Type.displayCutout()
            );

            view.setPadding(
                    rootLeft + safeInsets.left,
                    rootTop,
                    rootRight + safeInsets.right,
                    rootBottom + safeInsets.bottom
            );

            header.setPadding(
                    headerLeft,
                    headerTop + safeInsets.top,
                    headerRight,
                    headerBottom
            );

            return WindowInsetsCompat.CONSUMED;
        });

        ViewCompat.requestApplyInsets(root);
    }

    private void init(Bundle scheduleBundle, Bundle circuitBundle) {
        ViewPager2 myViewPager2 = findViewById(R.id.viewPager2);
        viewPagerAdapter adapter = new viewPagerAdapter(this);
        concludedRaceScheduleFragment scheduleFragment = new concludedRaceScheduleFragment();
        scheduleFragment.setArguments(scheduleBundle);
        adapter.addFragment(scheduleFragment);
        raceCircuitFragment circuitFragment = new raceCircuitFragment();
        circuitFragment.setArguments(circuitBundle);
        adapter.addFragment(circuitFragment);
        myViewPager2.setAdapter(adapter);

        View child = myViewPager2.getChildAt(0);
        if (child instanceof RecyclerView) {
            child.setOverScrollMode(View.OVER_SCROLL_NEVER);
        }

        TabLayout tabLayout = findViewById(R.id.tab_layout);
        TabLayoutMediator tabLayoutMediator= new TabLayoutMediator(tabLayout, myViewPager2, (tab, position) -> {
            if (position == 0){
                tab.setText(R.string.schedule_text);
            }
            else{
                tab.setText(R.string.circuit_text);
            }
        });
        tabLayoutMediator.attach();
    }
}