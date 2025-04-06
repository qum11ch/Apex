package com.example.f1app;

import static com.example.f1app.MainActivity.getStringByName;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

public class concludedRaceScheduleFragment extends Fragment {
    private List<scheduleData> datum;
    private TextView secondPlace_code, firstPlace_code, thirdPlace_code,
            infoRaceName, day_start, day_end, month,
            show_results;
    private ImageView secondPlace_image, firstPlace_image, thirdPlace_image;
    private scheduleAdapter adapter;
    private RecyclerView recyclerView;
    private ToggleButton saveRace;
    private LocalDate currentDate;
    private String fullRaceName_key, mRaceName, mYear;


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
        infoRaceName = view.findViewById(R.id.infoRaceName);
        secondPlace_image = view.findViewById(R.id.secondPlace_image);
        firstPlace_image = view.findViewById(R.id.firstPlace_image);
        thirdPlace_image = view.findViewById(R.id.thirdPlace_image);
        day_start = view.findViewById(R.id.day_start);
        day_end = view.findViewById(R.id.day_end);
        month = view.findViewById(R.id.month);
        show_results = view.findViewById(R.id.show_results);
        saveRace = view.findViewById(R.id.saveRace);

        recyclerView = view.findViewById(R.id.recyclerview_schedule);
        recyclerView.setHasFixedSize(true);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getActivity().getApplicationContext());
        linearLayoutManager.setReverseLayout(true);
        linearLayoutManager.setStackFromEnd(true);
        recyclerView.setLayoutManager(linearLayoutManager);

        if(!getArguments().isEmpty()){
            String mCircuitId = getArguments().getString("circuitId");
            mRaceName = getArguments().getString("raceName");
            String mRaceStartDay = getArguments().getString("raceStartDay");
            String mRaceEndDay = getArguments().getString("raceEndDay");
            String mRaceStartMonth = getArguments().getString("raceStartMonth");
            String mRaceEndMonth = getArguments().getString("raceEndMonth");
            String mRound = getArguments().getString("roundCount");
            String mCountry = getArguments().getString("raceCountry");
            mYear = getArguments().getString("gpYear");
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

            String localeRaceName = mRaceName.toLowerCase().replaceAll("\\s+", "_");
            String pastRaceName = requireContext().getString(getStringByName(localeRaceName + "_text")) + " " + mYear;

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

            DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference();

            FirebaseStorage storage = FirebaseStorage.getInstance();
            StorageReference storageRef = storage.getReference();


            StorageReference mWinnerImage;
            StorageReference mSecondImage;
            StorageReference mThirdImage;
            if (mYear.equals("2024")){
                mWinnerImage = storageRef.child("drivers/" + mFirstPlaceCode.toLowerCase() + "_2024.png");
                mSecondImage = storageRef.child("drivers/" + mSecondPlaceCode.toLowerCase() + "_2024.png");
                mThirdImage = storageRef.child("drivers/" + mThirdPlaceCode.toLowerCase() + "_2024.png");
            }else{
                mWinnerImage = storageRef.child("drivers/" + mFirstPlaceCode.toLowerCase() + ".png");
                mSecondImage = storageRef.child("drivers/" + mSecondPlaceCode.toLowerCase() + ".png");
                mThirdImage = storageRef.child("drivers/" + mThirdPlaceCode.toLowerCase() + ".png");
            }

            GlideApp.with(requireContext())
                    .load(mWinnerImage)
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .error(R.drawable.f1)
                    .into(firstPlace_image);

            GlideApp.with(requireContext())
                    .load(mSecondImage)
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .error(R.drawable.f1)
                    .into(secondPlace_image);

            GlideApp.with(requireContext())
                    .load(mThirdImage)
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .error(R.drawable.f1)
                    .into(thirdPlace_image);

            rootRef.child("drivers").orderByChild("driversCode").equalTo(mFirstPlaceCode).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    for (DataSnapshot ds: snapshot.getChildren()){
                        String driver = ds.getKey();
                        String[] driverFullName = driver.split(" ");
                        String driverName;
                        String driverFamilyName;
                        if (driver.equals("Andrea Kimi Antonelli")){
                            driverName = driverFullName[1];
                            driverFamilyName = driverFullName[2];
                        }else{
                            driverName = driverFullName[0];
                            driverFamilyName = driverFullName[1];
                        }
                        String mDriverName = driverName.charAt(0) + ". " + driverFamilyName;
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
                        String driverName;
                        String driverFamilyName;
                        if (driver.equals("Andrea Kimi Antonelli")){
                            driverName = driverFullName[1];
                            driverFamilyName = driverFullName[2];
                        }else{
                            driverName = driverFullName[0];
                            driverFamilyName = driverFullName[1];
                        }
                        String mDriverName = driverName.charAt(0) + ". " + driverFamilyName;
                        secondPlace_code.setText(driverFamilyName);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e("concludedRacePage", "Drivers error:" + error.getMessage());
                }
            });

            rootRef.child("drivers").orderByChild("driversCode").equalTo(mThirdPlaceCode).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    for (DataSnapshot ds: snapshot.getChildren()){
                        String driver = ds.getKey();
                        String[] driverFullName = driver.split(" ");
                        String driverName;
                        String driverFamilyName;
                        if (driver.equals("Andrea Kimi Antonelli")){
                            driverName = driverFullName[1];
                            driverFamilyName = driverFullName[2];
                        }else{
                            driverName = driverFullName[0];
                            driverFamilyName = driverFullName[1];
                        }
                        String mDriverName = driverName.charAt(0) + ". " + driverFamilyName;
                        thirdPlace_code.setText(driverFamilyName);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e("concludedRacePage", "Drivers error:" + error.getMessage());
                }
            });
            infoRaceName.setText(pastRaceName);

            if(mRaceStartMonth.equals(mRaceEndMonth)){
                month.setText(mRaceStartMonth);
            }
            else{
                String monthAll = mRaceStartMonth + "-" + mRaceEndMonth;
                month.setText(monthAll);
            }

            day_start.setText(mRaceStartDay);
            day_end.setText(mRaceEndDay);

            String currentYear = Integer.toString(currentDate.getYear());

            datum = new ArrayList<>();
            getRaceSchedule(mRaceName, currentYear);

            CollapsingToolbarLayout collapsingToolbarLayout = (CollapsingToolbarLayout) view.findViewById(R.id.collapsing_toolbar);
            AppBarLayout appBarLayout = (AppBarLayout) view.findViewById(R.id.appbar);
            appBarLayout.setExpanded(true,true);
        }
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

    private void deleteRace(String fullRaceName_key){
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

    private void getRaceSchedule(String raceName, String currentYear){
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
                }else{
                    String sprintQuali = snapshot.child("SprintQualifying/sprintQualiDate").getValue(String.class) +
                            " " + snapshot.child("SprintQualifying/sprintQualiTime").getValue(String.class);
                    scheduleData sprintQualiEvent = new scheduleData(sprintQuali, "sprint_quali_event");

                    String sprint = sprintDate +
                            " " + snapshot.child("Sprint/sprintRaceTime").getValue(String.class);
                    scheduleData sprintEvent = new scheduleData(sprint, "sprint_event");

                    datum.add(sprintQualiEvent);
                    datum.add(sprintEvent);
                }
                datum.add(qualiEvent);
                datum.add(raceEvent);
                adapter = new scheduleAdapter(getActivity(), datum, true);
                recyclerView.setAdapter(adapter);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("futureActivityFirebaseError", error.getMessage());
            }
        });
    }

}
