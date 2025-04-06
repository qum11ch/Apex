package com.example.f1app;


import static com.example.f1app.MainActivity.getStringByName;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.lang.reflect.Field;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

public class futureRaceScheduleFragment extends Fragment {
    private List<scheduleData> datum;
    private final String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";
    private TextView days_countdown, hrs_countdown, mns_countdown,  countdown_header,
            infoRaceName;
    private scheduleAdapter adapter;
    private RecyclerView recyclerView;
    private long startTime;
    private long diffStart, diffEnd;
    private ToggleButton saveRace;
    private LocalDate currentDate;
    private String fullRaceName_key, mRaceName, mYear, mCircuitId;
    private static final long HOUR = 3600*1000;
    private static final long SPRINT_QUALI_DIFF = 44*60*1000;


    public futureRaceScheduleFragment() {
        // required empty public constructor.
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.future_race_schedule_fragment, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        days_countdown = view.findViewById(R.id.days_countdown);
        hrs_countdown = view.findViewById(R.id.hrs_countdown);
        mns_countdown = view.findViewById(R.id.mns_countdown);
        countdown_header = view.findViewById(R.id.countdown_header);
        infoRaceName = view.findViewById(R.id.infoRaceName);

        saveRace = view.findViewById(R.id.saveRace);

        recyclerView = view.findViewById(R.id.recyclerview_schedule);
        recyclerView.setHasFixedSize(true);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getActivity().getApplicationContext());
        linearLayoutManager.setReverseLayout(true);
        linearLayoutManager.setStackFromEnd(true);
        recyclerView.setLayoutManager(linearLayoutManager);

        if (!getArguments().isEmpty()){
            mCircuitId = getArguments().getString("circuitId");
            mRaceName = getArguments().getString("raceName");

            String mFutureRaceStartDay = getArguments().getString("futureRaceStartDay");
            String mFutureRaceEndDay = getArguments().getString("futureRaceEndDay");
            String mFutureRaceStartMonth = getArguments().getString("futureRaceStartMonth");
            String mFutureRaceEndMonth = getArguments().getString("futureRaceEndMonth");
            //String mRound = getArguments().getString("roundCount");
            //String mCountry = getArguments().getString("raceCountry");
            mYear = getArguments().getString("gpYear");

            String localeRaceName = mRaceName.toLowerCase().replaceAll("\\s+", "_");
            String futureRaceName = requireContext().getString(getStringByName(localeRaceName + "_text")) + " " + mYear;

            TextView day_start = (TextView) view.findViewById(R.id.day_start);
            TextView day_end = (TextView) view.findViewById(R.id.day_end);
            TextView month = (TextView) view.findViewById(R.id.month);

            fullRaceName_key = mYear + "_" + mRaceName.replace(" ", "");

            fullRaceName_key = mYear + "_" + mRaceName.replace(" ", "");

            currentDate = LocalDate.now();
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user!=null){
                isSaved(fullRaceName_key);
                saveRace.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if(saveRace.isChecked()){
                            saveRace(currentDate);
                        }else{
                            deleteRace(fullRaceName_key);
                        }
                    }
                });
            }else{
                saveRace.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        saveRace.setChecked(false);
                        Toast.makeText(requireContext(), getString(R.string.race_save_error_login_text), Toast.LENGTH_LONG).show();
                    }
                });
            }
            infoRaceName.setText(futureRaceName);

            if(mFutureRaceStartMonth.equals(mFutureRaceEndMonth)){
                month.setText(mFutureRaceStartMonth);
            }
            else{
                String monthAll = mFutureRaceStartMonth + "-" + mFutureRaceEndMonth;
                month.setText(monthAll);
            }

            day_start.setText(mFutureRaceStartDay);
            day_end.setText(mFutureRaceEndDay);



            LocalDate currentDate = LocalDate.now();
            String currentYear = Integer.toString(currentDate.getYear());

            datum = new ArrayList<>();
            getRaceSchedule(mRaceName, currentYear);

            CollapsingToolbarLayout collapsingToolbarLayout = (CollapsingToolbarLayout) view.findViewById(R.id.collapsing_toolbar);
            AppBarLayout appBarLayout = (AppBarLayout) view.findViewById(R.id.appbar);
            appBarLayout.setExpanded(true,true);
        }
    }

    private void getRaceSchedule(String raceName, String currentYear){
        LinkedHashMap<String, String> eventsCountdown = new LinkedHashMap<>();
        DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference();
        rootRef.child("/schedule/season/" + currentYear + "/" + raceName).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String firstPractice = snapshot.child("FirstPractice/firstPracticeDate").getValue(String.class) +
                        " " + snapshot.child("FirstPractice/firstPracticeTime").getValue(String.class);
                scheduleData firstPracticeEvent = new scheduleData(firstPractice, "first_practice_event");

                String race = snapshot.child("raceDate").getValue(String.class) +
                        " " + snapshot.child("raceTime").getValue(String.class);
                scheduleData raceEvent = new scheduleData(race, "race_event");

                String raceQuali = snapshot.child("Qualifying/raceQualiDate").getValue(String.class) +
                        " " + snapshot.child("Qualifying/raceQualiTime").getValue(String.class);
                scheduleData qualiEvent = new scheduleData(raceQuali, "quali_event");

                datum.add(firstPracticeEvent);

                String sprintDate = snapshot.child("Sprint/sprintRaceDate").getValue(String.class);
                if (sprintDate.equals("N/A")){
                    String secondPractice = snapshot.child("SecondPractice/secondPracticeDate").getValue(String.class) +
                            " " + snapshot.child("SecondPractice/secondPracticeTime").getValue(String.class);
                    scheduleData secondPracticeEvent = new scheduleData(secondPractice, "second_practice_event");

                    String thirdPractice = snapshot.child("ThirdPractice/thirdPracticeDate").getValue(String.class) +
                            " " + snapshot.child("ThirdPractice/thirdPracticeTime").getValue(String.class);
                    scheduleData thirdPracticeEvent = new scheduleData(thirdPractice, "third_practice_event");

                    datum.add(secondPracticeEvent);
                    datum.add(thirdPracticeEvent);

                    eventsCountdown.put("first_practice_event", firstPractice);
                    eventsCountdown.put("second_practice_event", secondPractice);
                    eventsCountdown.put("third_practice_event", thirdPractice);
                    eventsCountdown.put("quali_event", raceQuali);
                    eventsCountdown.put("race_event", race);
                }else{
                    String sprintQuali = snapshot.child("SprintQualifying/sprintQualiDate").getValue(String.class) +
                            " " + snapshot.child("SprintQualifying/sprintQualiTime").getValue(String.class);
                    scheduleData sprintQualiEvent = new scheduleData(sprintQuali, "sprint_quali_event");

                    String sprint = sprintDate +
                            " " + snapshot.child("Sprint/sprintRaceTime").getValue(String.class);
                    scheduleData sprintEvent = new scheduleData(sprint, "sprint_event");

                    datum.add(sprintQualiEvent);
                    datum.add(sprintEvent);

                    eventsCountdown.put("first_practice_event", firstPractice);
                    eventsCountdown.put("sprint_quali_event", sprintQuali);
                    eventsCountdown.put("sprint_event", sprint);
                    eventsCountdown.put("quali_event", raceQuali);
                    eventsCountdown.put("race_event", race);
                }
                datum.add(qualiEvent);
                datum.add(raceEvent);
                countDownStart(eventsCountdown);
                adapter = new scheduleAdapter(getActivity(), datum, false);
                recyclerView.setAdapter(adapter);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("futureActivityFirebaseError", error.getMessage());
            }
        });
    }

    private void saveRace(LocalDate currentDate){
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference();
        rootRef.child("users").orderByChild("userId")
                .equalTo(user.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        for(DataSnapshot userSnap: snapshot.getChildren()){
                            String username = userSnap.getKey();
                            DateTimeFormatter formatterUpdate = DateTimeFormatter.ofPattern("d/MM/uuuu");
                            String saveDate = currentDate.format(formatterUpdate);
                            savedRacesData savedRacesData = new savedRacesData(mRaceName, mYear, saveDate);
                            rootRef.child("savedRaces").child(username).addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot snapshot) {
                                    if(snapshot.getChildrenCount()<32){
                                        rootRef.child("savedRaces").child(username).child(fullRaceName_key).setValue(savedRacesData);
                                        Toast.makeText(requireContext(), getString(R.string.race_save_succ_text), Toast.LENGTH_LONG).show();
                                        //saveRace.setChecked(true);
                                    }else{
                                        Toast.makeText(requireContext(), getString(R.string.race_save_error_limit_text), Toast.LENGTH_LONG).show();
                                    }
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

    public void deleteRace(String fullRaceName_key){
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference();
        rootRef.child("users").orderByChild("userId").equalTo(user.getUid())
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        for(DataSnapshot userSnap: snapshot.getChildren()){
                            String username = userSnap.getKey();
                            rootRef.child("savedRaces").child(username).child(fullRaceName_key).removeValue();
                            Toast.makeText(requireContext(), getString(R.string.race_delete_succ_text), Toast.LENGTH_LONG).show();
                            //saveRace.setChecked(false);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e("concludedRacePage", "Drivers error:" + error.getMessage());
                    }
                });
    }

    private void isSaved(String fullRaceName_key){
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference();
        rootRef.child("users").orderByChild("userId")
                .equalTo(user.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        for(DataSnapshot userSnap: snapshot.getChildren()){
                            String username = userSnap.getKey();
                            rootRef.child("savedRaces").child(username).addValueEventListener(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot snapshot) {
                                    saveRace.setChecked(snapshot.hasChild(fullRaceName_key));
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

    @SuppressLint("SetTextI18n")
    private void countDownStart(LinkedHashMap<String, String> events) {

        if (events.isEmpty()){
            countdown_header.setText(getString(R.string.weekend_ends_text));
            days_countdown.setText("00");
            hrs_countdown.setText("00");
            mns_countdown.setText("00");
        }else{

            long milliseconds = 0;

            Map.Entry<String, String> entry = events.entrySet().iterator().next();
            String key = entry.getKey();
            String value = entry.getValue();

            if(!key.equals("first_practice_event")){
                countdown_header.setText(getString(getStringByName(key + "_text")));
            }

            SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT);
            dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
            try {
                Date eventStart_date = dateFormat.parse(value);
                milliseconds = eventStart_date.getTime();
                startTime = System.currentTimeMillis();

                Date endTime = new Date(eventStart_date.getTime() + HOUR);
                dateFormat.setTimeZone(TimeZone.getDefault());
                String endTimeEvent2 = dateFormat.format(endTime);

                Date endTime_sprintQ = new Date(eventStart_date.getTime() + SPRINT_QUALI_DIFF);
                dateFormat.setTimeZone(TimeZone.getDefault());
                String endTimeEvent3 = dateFormat.format(endTime_sprintQ);

                Date endTime_race = new Date(eventStart_date.getTime() + 2 * HOUR);
                dateFormat.setTimeZone(TimeZone.getDefault());
                String endTimeEvent4 = dateFormat.format(endTime_race);

                Date eventEnd_date;
                diffStart = milliseconds - startTime;

                switch (key) {
                    case "race_event":
                        eventEnd_date = dateFormat.parse(endTimeEvent4);
                        diffEnd = eventEnd_date.getTime() - startTime;
                        break;
                    case "sprint_quali_event":
                        eventEnd_date = dateFormat.parse(endTimeEvent3);
                        diffEnd = eventEnd_date.getTime() - startTime;
                        break;
                    default:
                        eventEnd_date = dateFormat.parse(endTimeEvent2);
                        diffEnd = eventEnd_date.getTime() - startTime;
                        break;
                }

                if(diffEnd<0){
                    events.remove(key);
                    countDownStart(events);
                }else{
                    CountDownTimer mCountDownTimer = new CountDownTimer(diffStart, 1000) {
                        @Override
                        public void onTick(long millisUntilFinished) {
                            long millisStart = millisUntilFinished;
                            String daysLeft = String.valueOf(TimeUnit.MILLISECONDS.toDays(millisStart));
                            String hoursLeft = String.valueOf((TimeUnit.MILLISECONDS.toHours(millisStart) - TimeUnit.DAYS.toHours(TimeUnit.MILLISECONDS.toDays(millisStart))));
                            String minutesLeft = String.valueOf((TimeUnit.MILLISECONDS.toMinutes(millisStart) - TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(millisStart))));
                            days_countdown.setText(daysLeft);
                            hrs_countdown.setText(hoursLeft);
                            mns_countdown.setText(minutesLeft);
                        }

                        @Override
                        public void onFinish() {
                            if (Locale.getDefault().getLanguage().equals("ru")){
                                String countdownHeader = getString(R.string.is_running) + " " + getString(getStringByName(key + "_text"));
                                countdown_header.setText(countdownHeader);
                            }else{
                                String countdownHeader = getString(getStringByName(key + "_text")) + " " + getString(R.string.is_running);
                                countdown_header.setText(countdownHeader);
                            }

                            CountDownTimer mCountDownTimer = new CountDownTimer(diffEnd, 1000) {
                                @Override
                                public void onTick(long millisUntilFinished) {
                                    long millisEnd = millisUntilFinished;
                                    String daysLeft = String.valueOf(TimeUnit.MILLISECONDS.toDays(millisEnd));
                                    String hoursLeft = String.valueOf((TimeUnit.MILLISECONDS.toHours(millisEnd) - TimeUnit.DAYS.toHours(TimeUnit.MILLISECONDS.toDays(millisEnd))));
                                    String minutesLeft = String.valueOf((TimeUnit.MILLISECONDS.toMinutes(millisEnd) - TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(millisEnd))));
                                    days_countdown.setText(daysLeft);
                                    hrs_countdown.setText(hoursLeft);
                                    mns_countdown.setText(minutesLeft);
                                }

                                @Override
                                public void onFinish() {
                                    events.remove(key);
                                    countDownStart(events);
                                }
                            }.start();
                        }
                    }.start();
                }
            }catch (ParseException e){
                e.printStackTrace();
            }
        }

    }

}
