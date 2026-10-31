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
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.viewpager2.widget.ViewPager2;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Locale;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class pastSeasonScheduleActivity extends AppCompatActivity {
    private SwipeRefreshLayout swipeLayout;
    private String mSeason;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        setContentView(R.layout.past_season_schedule_page);
        setupPagerScreenInsets();

        if (!checkConnection(getApplicationContext())){
            startActivity(connectionLostScreen.createShowSplashOnNetworkFailure(pastSeasonScheduleActivity.this));
        }else{
            startActivity(connectionLostScreen.createIntentHideSplashOnNetworkRecovery(pastSeasonScheduleActivity.this));
        }

        Bundle intentBundle = getIntent().getExtras();
        mSeason = intentBundle.getString("season");

        TextView pageHeader = findViewById(R.id.pageHeader);

        String headerText;
        if (Locale.getDefault().getLanguage().equals("ru")){
            headerText = getString(R.string.past_season_result) + " " + mSeason;
        }else{
            headerText = mSeason + " " + getString(R.string.past_season_result);
        }
        pageHeader.setText(headerText);


        getSchedule();


        swipeLayout = findViewById(R.id.swipe_layout);
        swipeLayout.setOnRefreshListener(() -> {
            getSchedule();
            swipeLayout.setRefreshing(false);
        });

        ImageButton backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> finish());

        WindowInsetsControllerCompat windowInsetsController =
                WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        windowInsetsController.setAppearanceLightStatusBars(false);
    }

    private void setupPagerScreenInsets() {
        View root = findViewById(R.id.main_layout);
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
                    rootBottom
            );

            header.setPadding(
                    headerLeft,
                    headerTop + safeInsets.top,
                    headerRight,
                    headerBottom
            );

            return windowInsets;
        });

        ViewCompat.requestApplyInsets(root);
    }

    private void getSchedule(){
        DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference();
        rootRef.child("schedule/season/" + mSeason).orderByChild("round").addValueEventListener(new ValueEventListener() {
        //rootRef.child("schedule/season/" + "2024").orderByChild("round").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                ArrayList<String> concludedRoundNumber = new ArrayList<>();
                Bundle concludedFragmentBundle = new Bundle();

                for (DataSnapshot ds : snapshot.getChildren()) {
                    Integer round = ds.child("round").getValue(Integer.class);
                    //String dateStart = ds.child("FirstPractice/firstPracticeDate").getValue(String.class);
                    //String dateEnd = ds.child("raceDate").getValue(String.class);
                    //String raceName = ds.child("Circuit/raceName").getValue(String.class);
                    //String circuitId = ds.child("Circuit/circuitId").getValue(String.class);

                    concludedRoundNumber.add(round.toString());
                }

                concludedFragmentBundle.putStringArrayList("raceRound", concludedRoundNumber);
                concludedFragmentBundle.putString("season", mSeason);
                concludedFragmentBundle.putString("parent", "pastSeasonSchedule");
                init(concludedFragmentBundle);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("scheduleActivityFirebaseError", error.getMessage());
            }
        });
    }


    private void init(Bundle concludedRaceBundle) {
        ViewPager2 myViewPager2 = findViewById(R.id.viewPager2);
        viewPagerAdapter raceAdapter = new viewPagerAdapter(this);
        concludedRaceFragment concludedRaceFragment = new concludedRaceFragment();
        concludedRaceFragment.setArguments(concludedRaceBundle);
        raceAdapter.addFragment(concludedRaceFragment);
        myViewPager2.setAdapter(raceAdapter);

        View child = myViewPager2.getChildAt(0);
        if (child instanceof RecyclerView) {
            child.setOverScrollMode(View.OVER_SCROLL_NEVER);
        }
    }
}
