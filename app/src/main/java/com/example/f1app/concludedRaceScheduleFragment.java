package com.example.f1app;

import android.content.Intent;
import android.media.Image;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

public class concludedRaceScheduleFragment extends Fragment {
    private List<scheduleData> datum;
    private final String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";
    private TextView secondPlace_code, firstPlace_code, thirdPlace_code,
            infoRaceName, infoSeason, raceName, circuitName, day_start, day_end, month,
            show_results;
    private ImageView secondPlace_image, firstPlace_image, thirdPlace_image;
    private scheduleAdapter adapter;
    private RecyclerView recyclerView;
    private long startTime;
    private long diff;


    public concludedRaceScheduleFragment() {
        // required empty public constructor.
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.concluded_race_schedule_fragment, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        secondPlace_code = view.findViewById(R.id.secondPlace_code);
        firstPlace_code = view.findViewById(R.id.firstPlace_code);
        thirdPlace_code = view.findViewById(R.id.thirdPlace_code);
        infoSeason = view.findViewById(R.id.infoSeason);
        infoRaceName = view.findViewById(R.id.infoRaceName);
        secondPlace_image = view.findViewById(R.id.secondPlace_image);
        firstPlace_image = view.findViewById(R.id.firstPlace_image);
        thirdPlace_image = view.findViewById(R.id.thirdPlace_image);
        raceName = view.findViewById(R.id.raceName);
        circuitName = view.findViewById(R.id.circuitName);
        day_start = view.findViewById(R.id.day_start);
        day_end = view.findViewById(R.id.day_end);
        month = view.findViewById(R.id.month);
        show_results = view.findViewById(R.id.show_results);

        recyclerView = view.findViewById(R.id.recyclerview_schedule);
        recyclerView.setHasFixedSize(true);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getActivity().getApplicationContext());
        linearLayoutManager.setReverseLayout(true);
        linearLayoutManager.setStackFromEnd(true);
        recyclerView.setLayoutManager(linearLayoutManager);


        String mCircuitId = getArguments().getString("circuitId");
        String mRaceName = getArguments().getString("raceName");
        String mRaceStartDay = getArguments().getString("raceStartDay");
        String mRaceEndDay = getArguments().getString("raceEndDay");
        String mRaceStartMonth = getArguments().getString("raceStartMonth");
        String mRaceEndMonth = getArguments().getString("raceEndMonth");
        String mRound = getArguments().getString("roundCount");
        String mCountry = getArguments().getString("raceCountry");
        String mYear = getArguments().getString("gpYear");
        String mFirstPlaceCode = getArguments().getString("firstPlaceCode");
        String mSecondPlaceCode = getArguments().getString("secondPlaceCode");
        String mThirdPlaceCode = getArguments().getString("thirdPlaceCode");

        show_results.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(requireContext() , raceResultsActivity.class);
                Bundle bundle = new Bundle();
                bundle.putString("raceName", mRaceName);
                bundle.putString("circuitId", mCircuitId);
                bundle.putString("season", mYear);
                intent.putExtras(bundle);
                requireContext().startActivity(intent);
            }
        });

        int resourceId_firstDriverImage = requireContext().getResources().getIdentifier(mFirstPlaceCode.toLowerCase(), "drawable",
                requireContext().getPackageName());
        Glide.with(requireContext())
                .load(resourceId_firstDriverImage)
                .transition(DrawableTransitionOptions.withCrossFade())
                .error(R.drawable.f1)
                .into(firstPlace_image);

        int resourceId_secondDriverImage = requireContext().getResources().getIdentifier(mSecondPlaceCode.toLowerCase(), "drawable",
                requireContext().getPackageName());
        Glide.with(requireContext())
                .load(resourceId_secondDriverImage)
                .transition(DrawableTransitionOptions.withCrossFade())
                .error(R.drawable.f1)
                .into(secondPlace_image);

        int resourceId_thirdDriverImage = requireContext().getResources().getIdentifier(mThirdPlaceCode.toLowerCase(), "drawable",
                requireContext().getPackageName());
        Glide.with(requireContext())
                .load(resourceId_thirdDriverImage)
                .transition(DrawableTransitionOptions.withCrossFade())
                .error(R.drawable.f1)
                .into(thirdPlace_image);

        DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference();
        rootRef.child("drivers").orderByChild("driversCode").equalTo(mFirstPlaceCode).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot ds: snapshot.getChildren()){
                    String driver = ds.getKey();
                    String[] driverFullName = driver.split(" ");
                    String driverFamilyName;
                    if (driver.equals("Andrea Kimi Antonelli")){
                        driverFamilyName = driverFullName[2];
                    }else{
                        driverFamilyName = driverFullName[1];
                    }
                    firstPlace_code.setText(driverFamilyName);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("concludedRacePage", "Drivers error:" + error.getMessage());
            }
        });

        rootRef.child("drivers").orderByChild("driversCode").equalTo(mSecondPlaceCode).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot ds: snapshot.getChildren()){
                    String driver = ds.getKey();
                    String[] driverFullName = driver.split(" ");
                    String driverFamilyName;
                    if (driver.equals("Andrea Kimi Antonelli")){
                        driverFamilyName = driverFullName[2];
                    }else{
                        driverFamilyName = driverFullName[1];
                    }
                    secondPlace_code.setText(driverFamilyName);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("concludedRacePage", "Drivers error:" + error.getMessage());
            }
        });

        rootRef.child("drivers").orderByChild("driversCode").equalTo(mSecondPlaceCode).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot ds: snapshot.getChildren()){
                    String driver = ds.getKey();
                    String[] driverFullName = driver.split(" ");
                    String driverFamilyName;
                    if (driver.equals("Andrea Kimi Antonelli")){
                        driverFamilyName = driverFullName[2];
                    }else{
                        driverFamilyName = driverFullName[1];
                    }
                    thirdPlace_code.setText(driverFamilyName);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("concludedRacePage", "Drivers error:" + error.getMessage());
            }
        });


        infoSeason.setText(mYear);
        infoRaceName.setText(mRaceName);

        if(mRaceStartMonth.equals(mRaceEndMonth)){
            month.setText(mRaceStartMonth);
        }
        else{
            String monthAll = mRaceStartMonth + "-" + mRaceEndMonth;
            month.setText(monthAll);
        }
        raceName.setText(mRaceName);

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
        day_start.setText(mRaceStartDay);
        day_end.setText(mRaceEndDay);

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
                adapter = new scheduleAdapter(getActivity(), datum);
                recyclerView.setAdapter(adapter);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("concludedActivityFirebaseError", error.getMessage());
            }
        });
    }


}
