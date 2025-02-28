package com.example.f1app;

import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

public class futureRaceScheduleFragment extends Fragment {
    private List<scheduleData> datum;
    private final String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";
    private TextView days_countdown, hrs_countdown, mns_countdown,  countdown_header,
            infoRaceName, infoSeason;
    private scheduleAdapter adapter;
    private RecyclerView recyclerView;
    private long startTime;
    private long diff;
    private ToggleButton saveRace;
    private LocalDate currentDate;
    private String fullRaceName_key, mRaceName, mYear;


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
        infoSeason = view.findViewById(R.id.infoSeason);
        infoRaceName = view.findViewById(R.id.infoRaceName);

        saveRace = view.findViewById(R.id.saveRace);

        recyclerView = view.findViewById(R.id.recyclerview_schedule);
        recyclerView.setHasFixedSize(true);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getActivity().getApplicationContext());
        linearLayoutManager.setReverseLayout(true);
        linearLayoutManager.setStackFromEnd(true);
        recyclerView.setLayoutManager(linearLayoutManager);


        String mCircuitId = getArguments().getString("circuitId");
        mRaceName = getArguments().getString("raceName");
        String mFutureRaceStartDay = getArguments().getString("futureRaceStartDay");
        String mFutureRaceEndDay = getArguments().getString("futureRaceEndDay");
        String mFutureRaceStartMonth = getArguments().getString("futureRaceStartMonth");
        String mFutureRaceEndMonth = getArguments().getString("futureRaceEndMonth");
        String mRound = getArguments().getString("roundCount");
        String mCountry = getArguments().getString("raceCountry");
        mYear = getArguments().getString("gpYear");

        TextView raceName = (TextView) view.findViewById(R.id.raceName);
        TextView circuitName = (TextView) view.findViewById(R.id.circuitName);
        TextView day_start = (TextView) view.findViewById(R.id.day_start);
        TextView day_end = (TextView) view.findViewById(R.id.day_end);
        TextView month = (TextView) view.findViewById(R.id.month);

        fullRaceName_key = mYear + "_" + mRaceName.replace(" ", "");

        isSaved(fullRaceName_key);
        currentDate = LocalDate.now();

        saveRace.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(saveRace.isChecked()){
                    savedRace(currentDate);
                }else{
                    deleteRace(fullRaceName_key);
                }
            }
        });

        //saveRace.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
        //    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        //        if (buttonView.isPressed()) {
        //            // The toggle is enabled
        //            deleteRace(fullRaceName_key);
        //        } else {
        //            // The toggle is disable
        //            saveRace(currentDate);
        //        }
        //    }
        //});

        infoSeason.setText(mYear);
        infoRaceName.setText(mRaceName);

        if(mFutureRaceStartMonth.equals(mFutureRaceEndMonth)){
            month.setText(mFutureRaceStartMonth);
        }
        else{
            String monthAll = mFutureRaceStartMonth + "-" + mFutureRaceEndMonth;
            month.setText(monthAll);
        }


        raceName.setText(mRaceName);

        DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference();
        rootRef.child("circuits/" + mCircuitId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String mcircuitName = snapshot.child("circuitName").getValue(String.class);
                circuitName.setText(mcircuitName);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("futureActivityFirebaseError", error.getMessage());
            }
        });
        day_start.setText(mFutureRaceStartDay);
        day_end.setText(mFutureRaceEndDay);



        LocalDate currentDate = LocalDate.now();
        String currentYear = Integer.toString(currentDate.getYear());

        datum = new ArrayList<>();
        getRaceSchedule(mRaceName, currentYear);

    }

    private void getRaceSchedule(String raceName, String currentYear){
        LinkedHashMap<String, String> eventsCountdown = new LinkedHashMap<>();
        DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference();
        rootRef.child("/schedule/season/" + currentYear + "/" + raceName).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String firstPractice = snapshot.child("FirstPractice/firstPracticeDate").getValue(String.class) +
                        " " + snapshot.child("FirstPractice/firstPracticeTime").getValue(String.class);
                scheduleData firstPracticeEvent = new scheduleData(firstPractice, "Practice 1");

                String race = snapshot.child("raceDate").getValue(String.class) +
                        " " + snapshot.child("raceTime").getValue(String.class);
                scheduleData raceEvent = new scheduleData(race, "Race");

                String raceQuali = snapshot.child("Qualifying/raceQualiDate").getValue(String.class) +
                        " " + snapshot.child("Qualifying/raceQualiTime").getValue(String.class);
                scheduleData qualiEvent = new scheduleData(raceQuali, "Qualifying");

                datum.add(firstPracticeEvent);

                String sprintDate = snapshot.child("Sprint/sprintRaceDate").getValue(String.class);
                if (sprintDate.equals("N/A")){
                    String secondPractice = snapshot.child("SecondPractice/secondPracticeDate").getValue(String.class) +
                            " " + snapshot.child("SecondPractice/secondPracticeTime").getValue(String.class);
                    scheduleData secondPracticeEvent = new scheduleData(secondPractice, "Practice 2");

                    String thirdPractice = snapshot.child("ThirdPractice/thirdPracticeDate").getValue(String.class) +
                            " " + snapshot.child("ThirdPractice/thirdPracticeTime").getValue(String.class);
                    scheduleData thirdPracticeEvent = new scheduleData(thirdPractice, "Practice 3");

                    datum.add(secondPracticeEvent);
                    datum.add(thirdPracticeEvent);

                    eventsCountdown.put("Practice 1", firstPractice);
                    eventsCountdown.put("Practice 2", secondPractice);
                    eventsCountdown.put("Practice 3", thirdPractice);
                    eventsCountdown.put("Qualifying", raceQuali);
                    eventsCountdown.put("Race", race);
                }else{
                    String sprintQuali = snapshot.child("SprintQualifying/sprintQualiDate").getValue(String.class) +
                            " " + snapshot.child("SprintQualifying/sprintQualiTime").getValue(String.class);
                    scheduleData sprintQualiEvent = new scheduleData(sprintQuali, "Sprint Qualifying");

                    String sprint = sprintDate +
                            " " + snapshot.child("Sprint/sprintRaceTime").getValue(String.class);
                    scheduleData sprintEvent = new scheduleData(sprint, "Sprint");

                    datum.add(sprintQualiEvent);
                    datum.add(sprintEvent);

                    eventsCountdown.put("Practice 1", firstPractice);
                    eventsCountdown.put("Sprint Qualifying", sprintQuali);
                    eventsCountdown.put("Sprint", sprint);
                    eventsCountdown.put("Qualifying", raceQuali);
                    eventsCountdown.put("Race", race);
                }
                datum.add(qualiEvent);
                datum.add(raceEvent);
                countDownStart(eventsCountdown);
                adapter = new scheduleAdapter(getActivity(), datum);
                recyclerView.setAdapter(adapter);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("futureActivityFirebaseError", error.getMessage());
            }
        });
    }

    private void savedRace(LocalDate currentDate){
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
                                        Toast.makeText(requireContext(), "This race saved!", Toast.LENGTH_LONG).show();
                                        //saveRace.setChecked(true);
                                    }else{
                                        Toast.makeText(requireContext(), "You can have maximum 32 saved races. Please clear your saved races list!", Toast.LENGTH_LONG).show();
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
                            Toast.makeText(requireContext(), "This race is deleted from saved races list!", Toast.LENGTH_LONG).show();
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

    private void countDownStart(LinkedHashMap<String, String> events) {

        if (events.isEmpty()){
            days_countdown.setText("00");
            hrs_countdown.setText("00");
            mns_countdown.setText("00");
        }else{

            long milliseconds = 0;

            Map.Entry<String, String> entry = events.entrySet().iterator().next();
            String key = entry.getKey();
            String value = entry.getValue();

            if(!key.equals("Practice 1")){
                countdown_header.setText(key);
            }

            String EVENT_DATE_TIME = value;

            SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT);
            dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));

            try {
                Date event_date = dateFormat.parse(EVENT_DATE_TIME);
                milliseconds = event_date.getTime();
                startTime = System.currentTimeMillis();

                diff = milliseconds - startTime;

                CountDownTimer mCountDownTimer = new CountDownTimer(diff, 1000) {
                    @Override
                    public void onTick(long millisUntilFinished) {
                        long millis = millisUntilFinished;
                        String daysLeft = String.valueOf(TimeUnit.MILLISECONDS.toDays(millis));
                        String hoursLeft = String.valueOf((TimeUnit.MILLISECONDS.toHours(millis) - TimeUnit.DAYS.toHours(TimeUnit.MILLISECONDS.toDays(millis))));
                        String minutesLeft = String.valueOf((TimeUnit.MILLISECONDS.toMinutes(millis) - TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(millis))));
                        days_countdown.setText(daysLeft);
                        hrs_countdown.setText(hoursLeft);
                        mns_countdown.setText(minutesLeft);
                    }

                    @Override
                    public void onFinish() {
                        events.remove(key);
                        Log.i("countDown", "finished");
                        countDownStart(events);

                    }
                }.start();
            }catch (ParseException e){
                e.printStackTrace();
            }
        }

    }


}
