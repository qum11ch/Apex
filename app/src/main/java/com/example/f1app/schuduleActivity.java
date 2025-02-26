package com.example.f1app;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.ToggleButton;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import androidx.viewpager2.widget.ViewPager2;

public class schuduleActivity extends AppCompatActivity {
    List<concludedRacesData> datum;
    private RecyclerView recyclerView;
    Button showDriverButton, showTeams, showHomePage, showAccount;
    private ToggleButton showConcluded, showFuture;
    private concludedRacesAdapter adapter;
    private Toolbar toolbar;
    private ImageButton backButton;
    private ViewPager2 myViewPager2;
    private viewPagerAdapter raceAdapter ;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);

        RelativeLayout main_layout = (RelativeLayout) findViewById(R.id.main_layout);
        setContentView(R.layout.schudule_page);
        LocalDate currentDate = LocalDate.now();

        recyclerView = findViewById(R.id.recyclerview_concludedRaces);

        setSupportActionBar(toolbar);

        showDriverButton = (Button) findViewById(R.id.showDriver);
        showDriverButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(schuduleActivity.this, driversStandingsActivity.class);
                schuduleActivity.this.startActivity(intent);
            }
        });

        showHomePage = (Button) findViewById(R.id.showHomePage);
        showHomePage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(schuduleActivity.this, MainActivity.class);
                schuduleActivity.this.startActivity(intent);
            }
        });

        showTeams = (Button) findViewById(R.id.showTeams);
        showTeams.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(schuduleActivity.this, teamsStandingsActivity.class);
                schuduleActivity.this.startActivity(intent);
            }
        });

        showAccount = (Button) findViewById(R.id.showAccount);
        showAccount.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(schuduleActivity.this, logInPageActivity.class);
                schuduleActivity.this.startActivity(intent);
            }
        });

        backButton = (ImageButton) findViewById(R.id.backButton);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        datum = new ArrayList<>();
        String currentYear = Integer.toString(currentDate.getYear());

        getSchedule(currentYear, currentDate);

        WindowInsetsControllerCompat windowInsetsController =
                WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        windowInsetsController.setAppearanceLightStatusBars(false);;
    }

    private void getSchedule(String year, LocalDate currentDate){
        DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference();
        //rootRef.child("schedule/season/" + year).orderByChild("round").addValueEventListener(new ValueEventListener() {
        rootRef.child("schedule/season/" + "2024").orderByChild("round").addValueEventListener(new ValueEventListener() {
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


                    String currentDateString = currentDate.toString();
                    SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
                    boolean future = false;
                    boolean isOnGoing = false;
                    boolean concluded = true;

                    try{
                        Date start = formatter.parse(dateStart);
                        Date end = formatter.parse(dateEnd);
                        Date current = formatter.parse(currentDateString);
                        if(start.after(current) && end.after(current))
                        {
                            concluded = false;
                            future = true;
                            isOnGoing = false;
                        }
                        else if (current.equals(start) || current.equals(end)){
                            concluded = false;
                            isOnGoing = true;
                        }
                        else if (current.after(start) && current.before(end)){
                            concluded = false;
                            isOnGoing = true;
                        }
                        else{
                            concluded = true;
                            future = false;
                            isOnGoing = false;
                        }
                    } catch (ParseException e){
                        Log.d("ParseExeption", "" + e);
                    }

                    if(concluded){
                        concludedRoundNumber.add(round.toString());
                    }
                    if (future){
                        futureRaceRoundNumber.add(round.toString());
                    }
                    if (isOnGoing){
                        CardView cardView = (CardView) findViewById(R.id.cardView);
                        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
                                RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
                        params.addRule(RelativeLayout.BELOW, R.id.header);
                        cardView.setLayoutParams(params);
                        TextView roundOngoing = (TextView) findViewById(R.id.round);
                        TextView dayStartOngoing = (TextView) findViewById(R.id.day_start);
                        TextView dayEndOngoing = (TextView) findViewById(R.id.day_end);
                        TextView raceMonthOngoing = (TextView) findViewById(R.id.raceMonth);
                        TextView raceCountryOngoing = (TextView) findViewById(R.id.raceCountry);
                        TextView raceNameOngoing = (TextView) findViewById(R.id.raceName);
                        TextView circuitNameOngoing = (TextView) findViewById(R.id.circuitName);

                        String finalRaceName = raceName + " " + year;

                        rootRef.child("circuits/" + circuitId).addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                String circuitName = snapshot.child("circuitName").getValue(String.class);
                                String raceCountry = snapshot.child("country").getValue(String.class);
                                String raceLocation = snapshot.child("location").getValue(String.class);
                                circuitNameOngoing.setText(circuitName);
                                String locale = raceLocation + ", " + raceCountry;
                                raceCountryOngoing.setText(locale);
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {
                                Log.e("scheduleActivityFirebaseError", error.getMessage());
                            }
                        });

                        roundOngoing.setText(Integer.valueOf(round).toString());

                        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-M-d");
                        LocalDate startDateOngoing = LocalDate.parse(dateStart, dateFormatter);
                        String dayStart = startDateOngoing.format(DateTimeFormatter.ofPattern("dd")).toString();

                        LocalDate endDateOngoing = LocalDate.parse(dateEnd, dateFormatter);
                        String dayEnd = endDateOngoing.format(DateTimeFormatter.ofPattern("dd")).toString();

                        String monthStart = startDateOngoing.format(DateTimeFormatter.ofPattern("MMM")).toString();
                        String monthEnd = endDateOngoing.format(DateTimeFormatter.ofPattern("MMM")).toString();

                        if(monthStart.equals(monthEnd)){
                            raceMonthOngoing.setText(monthStart);
                        }
                        else{
                            String month = monthStart + "-" + monthEnd;
                            raceMonthOngoing.setText(month);
                        }

                        raceNameOngoing.setText(finalRaceName);


                        dayStartOngoing.setText(dayStart);
                        dayEndOngoing.setText(dayEnd);
                        cardView.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                Intent intent = new Intent(schuduleActivity.this , futureRaceActivity.class);

                                Bundle bundle = new Bundle();
                                bundle.putString("raceName" , raceName);
                                bundle.putString("futureRaceStartDay" , dayStart);
                                bundle.putString("futureRaceEndDay" , dayEnd);
                                bundle.putString("futureRaceStartMonth" , monthStart);
                                bundle.putString("futureRaceEndMonth" , monthEnd);
                                bundle.putString("circuitId", circuitId);
                                rootRef.child("circuits/" + circuitId).addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                                        String circuitName = snapshot.child("circuitName").getValue(String.class);
                                        String raceCountry = snapshot.child("country").getValue(String.class);
                                        bundle.putString("circuitName" , circuitName);
                                        bundle.putString("raceCountry" , raceCountry);
                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError error) {
                                        Log.e("scheduleActivityFirebaseError", error.getMessage());
                                    }
                                });

                                bundle.putString("roundCount" , round.toString());
                                bundle.putString("dateStart", dateStart);
                                intent.putExtras(bundle);

                                schuduleActivity.this.startActivity(intent);
                            }
                        });
                    }
                }

                if(!concludedRoundNumber.isEmpty()){
                    concludedFragmentBundle.putStringArrayList("raceRound", concludedRoundNumber);
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
        myViewPager2 = findViewById(R.id.viewPager2);
        raceAdapter = new viewPagerAdapter(this);
        futureRaceFragment futureRaceFragment = new futureRaceFragment();
        futureRaceFragment.setArguments(futureRaceBundle);
        raceAdapter.addFragment(futureRaceFragment);
        concludedRaceFragment concludedRaceFragment = new concludedRaceFragment();
        concludedRaceFragment.setArguments(concludedRaceBundle);
        raceAdapter.addFragment(concludedRaceFragment);
        myViewPager2.setAdapter(raceAdapter);
        TabLayout tabLayout = findViewById(R.id.tab_layout);
        TabLayoutMediator tabLayoutMediator= new TabLayoutMediator(tabLayout, myViewPager2, new TabLayoutMediator.TabConfigurationStrategy(){
            @Override
            public void onConfigureTab(TabLayout.Tab tab, int position) {
                if (position == 0){
                    tab.setText("Upcoming");
                }
                else{
                    tab.setText("Concluded");
                }
            }
        });
        tabLayoutMediator.attach();
    }
}
