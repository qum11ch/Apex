package com.example.f1app;

import static com.example.f1app.MainActivity.checkConnection;
import static com.example.f1app.MainActivity.getStringByName;
import static com.example.f1app.driversStandingsActivity.startActivity_seasonData;
import static com.example.f1app.teamsStandingsActivity.localizeLocality;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.viewpager2.widget.ViewPager2;

public class scheduleActivity extends AppCompatActivity {
    ImageButton showDrivers, showTeams, showHomePage, showAccount;
    private Toolbar toolbar;
    private ViewPager2 myViewPager2;
    private CardView cardView;
    private SwipeRefreshLayout swipeLayout;
    private String mCurrentSeason;
    private static final long HOUR = 3600*1000;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);

        if (!checkConnection(getApplicationContext())){
            startActivity(connectionLostScreen.createShowSplashOnNetworkFailure(scheduleActivity.this));
        }else{
            startActivity(connectionLostScreen.createIntentHideSplashOnNetworkRecovery(scheduleActivity.this));
        }
        setContentView(R.layout.schudule_page);

        cardView = findViewById(R.id.cardView);
        cardView.setVisibility(View.GONE);

        LocalDate currentDate = LocalDate.now();
        String currentYear = Integer.toString(currentDate.getYear());


        getSchedule(currentYear);
        myViewPager2 = findViewById(R.id.viewPager2);

        Bundle intentBundle = getIntent().getExtras();
        mCurrentSeason = intentBundle.getString("currentSeason");

        swipeLayout = findViewById(R.id.swipe_layout);
        swipeLayout.setOnRefreshListener(() -> {
            cardView.setVisibility(View.GONE);
            getSchedule(currentYear);
            swipeLayout.setRefreshing(false);
        });

        setSupportActionBar(toolbar);

        showDrivers = findViewById(R.id.showDriver);
        showDrivers.setOnClickListener(v -> startActivity_seasonData(scheduleActivity.this, driversStandingsActivity.class, mCurrentSeason));

        showHomePage = findViewById(R.id.showHomePage);
        showHomePage.setOnClickListener(v -> {
            Intent intent = new Intent(scheduleActivity.this, MainActivity.class);
            scheduleActivity.this.startActivity(intent);
            overridePendingTransition(0, 0);
        });

        showTeams = findViewById(R.id.showTeams);
        showTeams.setOnClickListener(v -> startActivity_seasonData(scheduleActivity.this, teamsStandingsActivity.class, mCurrentSeason));

        showAccount = findViewById(R.id.showAccount);
        showAccount.setOnClickListener(v -> startActivity_seasonData(scheduleActivity.this, logInPageActivity.class, mCurrentSeason));

        ImageButton backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> finish());

        WindowInsetsControllerCompat windowInsetsController =
                WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        windowInsetsController.setAppearanceLightStatusBars(false);
    }

    private void getSchedule(String year){
        DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference();
        rootRef.child("schedule/season/" + year).orderByChild("round").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                ArrayList<String> concludedRoundNumber = new ArrayList<>();
                ArrayList<String> futureRaceRoundNumber = new ArrayList<>();

                Bundle concludedFragmentBundle = new Bundle();
                Bundle futureFragmentBundle = new Bundle();

                for (DataSnapshot ds : snapshot.getChildren()) {
                    Integer round = ds.child("round").getValue(Integer.class);
                    String dateStart = ds.child("FirstPractice/firstPracticeDate").getValue(String.class);
                    String dateEnd = ds.child("raceDate").getValue(String.class);
                    String raceName = ds.child("Circuit/raceName").getValue(String.class);
                    String circuitId = ds.child("Circuit/circuitId").getValue(String.class);
                    //String raceTime = ds.child("raceTime").getValue(String.class);
                    Boolean hasCanceled = ds.child("Canceled").getValue(Boolean.class);

                    boolean isCanceled;
                    isCanceled = hasCanceled != null;

                    boolean future = false;
                    boolean isOnGoing = false;
                    boolean concluded = true;

                    String eventEndTime = ds.child("raceDate").getValue(String.class) +
                            " " + ds.child("raceTime").getValue(String.class);
                    String eventStartTime = ds.child("FirstPractice/firstPracticeDate").getValue(String.class) +
                            " " + ds.child("FirstPractice/firstPracticeTime").getValue(String.class);

                    try{
                        String eventEndTimeString = eventEndTime.replaceAll("\\s+", "T");
                        String eventStartTimeString = eventStartTime.replaceAll("\\s+", "T");

                        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                        DateTimeFormatter  fullDateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z", Locale.ENGLISH);

                        Date currentDate = Calendar.getInstance().getTime();

                        Instant endDateInst = Instant.parse(eventEndTimeString);
                        ZonedDateTime endDateTime = endDateInst.atZone(ZoneId.systemDefault());
                        String endDate = endDateTime.format(fullDateFormatter);

                        Instant startDateInst = Instant.parse(eventStartTimeString);
                        ZonedDateTime startDateTime = startDateInst.atZone(ZoneId.systemDefault());
                        String startDate = startDateTime.format(fullDateFormatter);

                        Date endValue = formatter.parse(endDate);
                        Date eventEnd = new Date(endValue.getTime() + 2 * HOUR);

                        Date startValue = formatter.parse(startDate);
                        Date eventStart = new Date(startValue.getTime());

                        if(eventStart.after(currentDate) && eventEnd.after(currentDate))
                        {
                            concluded = false;
                            future = true;
                        }
                        else if (currentDate.equals(eventStart) || currentDate.equals(eventEnd)){
                            concluded = false;
                            isOnGoing = true;
                        }
                        else if (currentDate.after(eventStart) && currentDate.before(eventEnd)){
                            concluded = false;
                            isOnGoing = true;
                        }

                    } catch (ParseException e){
                        Log.d("ParseExeption", "" + e);
                    }

                    if (isCanceled){
                        isOnGoing = false;
                    }

                    if(concluded){
                        concludedRoundNumber.add(round.toString());
                    }
                    if (future){
                        futureRaceRoundNumber.add(round.toString());
                    }
                    if (isOnGoing){
                        cardView.setVisibility(View.VISIBLE);
                        TextView roundOngoing = findViewById(R.id.round);
                        TextView dayStartOngoing = findViewById(R.id.day_start);
                        TextView dayEndOngoing = findViewById(R.id.day_end);
                        TextView raceMonthOngoing = findViewById(R.id.raceMonth);
                        TextView raceCountryOngoing = findViewById(R.id.raceCountry);
                        TextView raceNameOngoing = findViewById(R.id.raceName);
                        TextView circuitNameOngoing = findViewById(R.id.circuitName);

                        rootRef.child("circuits/" + circuitId).addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                String raceCountry = snapshot.child("country").getValue(String.class);
                                String raceLocation = snapshot.child("location").getValue(String.class);
                                circuitNameOngoing.setText(getString(getStringByName(circuitId + "_text")));
                                String locale;
                                if (Locale.getDefault().getLanguage().equals("ru")){
                                    ArrayList<String> localizedData = localizeLocality(raceLocation, raceCountry, scheduleActivity.this);
                                    //String country = localizedData.get(0);
                                    //String cityName = localizedData.get(1);
                                    locale = localizedData.get(2);
                                }else{
                                    String cityName = raceLocation;
                                    switch (cityName){
                                        case "Monaco":
                                        case "Singapore":
                                            cityName = null;
                                            break;
                                        default:
                                            break;
                                    }
                                    if (cityName!=null){
                                        locale = cityName + ", " + raceCountry;
                                    }else{
                                        locale = raceCountry;
                                    }

                                }
                                raceCountryOngoing.setText(locale);
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {
                                Log.e("scheduleActivityFirebaseError", error.getMessage());
                            }
                        });

                        String ongoingRoundText = String.valueOf(round);
                        roundOngoing.setText(ongoingRoundText);

                        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-M-d");
                        LocalDate startDateOngoing = LocalDate.parse(dateStart, dateFormatter);
                        String dayStart = startDateOngoing.format(DateTimeFormatter.ofPattern("dd"));

                        LocalDate endDateOngoing = LocalDate.parse(dateEnd, dateFormatter);
                        String dayEnd = endDateOngoing.format(DateTimeFormatter.ofPattern("dd"));

                        String monthStart = startDateOngoing.format(DateTimeFormatter.ofPattern("MMM"));
                        String monthEnd = endDateOngoing.format(DateTimeFormatter.ofPattern("MMM"));

                        if(monthStart.equals(monthEnd)){
                            raceMonthOngoing.setText(monthStart);
                        }
                        else{
                            String month = monthStart + "-" + monthEnd;
                            raceMonthOngoing.setText(month);
                        }

                        String localeRaceName = raceName.toLowerCase().replaceAll("\\s+", "_");
                        String futureRaceName = getString(getStringByName(localeRaceName + "_text")) + " " + year;
                        raceNameOngoing.setText(futureRaceName);


                        dayStartOngoing.setText(dayStart);
                        dayEndOngoing.setText(dayEnd);
                        cardView.setOnClickListener(v -> rootRef.child("circuits/" + circuitId).addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot1) {
                                String circuitName = snapshot1.child("circuitName").getValue(String.class);
                                String raceCountry = snapshot1.child("country").getValue(String.class);

                                Intent intent = new Intent(scheduleActivity.this , futureRaceActivity.class);

                                Bundle bundle = new Bundle();
                                bundle.putString("raceName" , raceName);
                                bundle.putString("futureRaceStartDay" , dayStart);
                                bundle.putString("futureRaceEndDay" , dayEnd);
                                bundle.putString("futureRaceStartMonth" , monthStart);
                                bundle.putString("futureRaceEndMonth" , monthEnd);
                                bundle.putString("circuitId", circuitId);
                                bundle.putString("circuitName" , circuitName);
                                bundle.putString("raceCountry" , raceCountry);
                                bundle.putString("roundCount" , String.valueOf(round));
                                bundle.putString("dateStart", dateStart);
                                bundle.putString("dateEnd", dateEnd);
                                bundle.putBoolean("isCanceled", false);
                                intent.putExtras(bundle);

                                scheduleActivity.this.startActivity(intent);
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {
                                Log.e("scheduleActivityFirebaseError", error.getMessage());
                            }
                        }));
                    }
                }

                if(!concludedRoundNumber.isEmpty()){
                    concludedFragmentBundle.putStringArrayList("raceRound", concludedRoundNumber);
                    concludedFragmentBundle.putString("season", mCurrentSeason);
                    concludedFragmentBundle.putString("parent", "schedule");
                }
                if(!futureRaceRoundNumber.isEmpty()){
                    futureFragmentBundle.putStringArrayList("raceRound", futureRaceRoundNumber);
                }

                init(concludedFragmentBundle, futureFragmentBundle);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("scheduleActivityFirebaseError", error.getMessage());
            }
        });
    }


    private void init(Bundle concludedRaceBundle, Bundle futureRaceBundle) {
        int prevFragment = myViewPager2.getCurrentItem();
        viewPagerAdapter raceAdapter = new viewPagerAdapter(this);
        futureRaceFragment futureRaceFragment = new futureRaceFragment();
        futureRaceFragment.setArguments(futureRaceBundle);
        raceAdapter.addFragment(futureRaceFragment);
        concludedRaceFragment concludedRaceFragment = new concludedRaceFragment();
        concludedRaceFragment.setArguments(concludedRaceBundle);
        raceAdapter.addFragment(concludedRaceFragment);
        myViewPager2.setAdapter(raceAdapter);

        View child = myViewPager2.getChildAt(0);
        if (child instanceof RecyclerView) {
            child.setOverScrollMode(View.OVER_SCROLL_NEVER);
        }

        if (prevFragment==1){
            myViewPager2.setCurrentItem(myViewPager2.getAdapter().getItemCount() - 1, false);
        }else{
            myViewPager2.setCurrentItem(myViewPager2.getCurrentItem(), false);
        }
        TabLayout tabLayout = findViewById(R.id.tab_layout);
        TabLayoutMediator tabLayoutMediator= new TabLayoutMediator(tabLayout, myViewPager2, (tab, position) -> {
            if (position == 0){
                tab.setText(R.string.upcoming_text);
            }
            else{
                tab.setText(R.string.concluded_text);
            }
        });
        tabLayoutMediator.attach();
    }
}
