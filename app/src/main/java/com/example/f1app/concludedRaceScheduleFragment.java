package com.example.f1app;

import static com.example.f1app.MainActivity.getStringByName;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

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
import java.util.List;

public class concludedRaceScheduleFragment extends Fragment {
    private List<scheduleData> datum;
    private TextView secondPlace_code;
    private TextView firstPlace_code;
    private TextView thirdPlace_code;
    private scheduleAdapter adapter;
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
        TextView infoRaceName = view.findViewById(R.id.infoRaceName);
        ImageView secondPlace_image = view.findViewById(R.id.secondPlace_image);
        ImageView firstPlace_image = view.findViewById(R.id.firstPlace_image);
        ImageView thirdPlace_image = view.findViewById(R.id.thirdPlace_image);
        ImageView backgroundImage = view.findViewById(R.id.background_img);
        TextView raceStatus = view.findViewById(R.id.raceStatus);
        TextView day_start = view.findViewById(R.id.day_start);
        TextView day_end = view.findViewById(R.id.day_end);
        TextView month = view.findViewById(R.id.month);
        TextView show_results = view.findViewById(R.id.show_results);
        saveRace = view.findViewById(R.id.saveRace);
        LinearLayout podiumLayout = view.findViewById(R.id.podium_layout);
        TextView dash = view.findViewById(R.id.dash);

        RecyclerView recyclerView = view.findViewById(R.id.recyclerview_schedule);
        recyclerView.setHasFixedSize(true);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(requireContext());
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
            //String mRound = getArguments().getString("roundCount");
            //String mCountry = getArguments().getString("raceCountry");
            String mRaceStartDate = getArguments().getString("dateStart");
            String mRaceEndDate = getArguments().getString("dateEnd");

            mYear = getArguments().getString("gpYear");
            String mFirstPlaceCode = getArguments().getString("firstPlaceCode");
            String mSecondPlaceCode = getArguments().getString("secondPlaceCode");
            String mThirdPlaceCode = getArguments().getString("thirdPlaceCode");
            boolean isCanceled = getArguments().getBoolean("isCanceled");

            CollapsingToolbarLayout collapsingToolbarLayout = view.findViewById(R.id.collapsing_toolbar);
            AppBarLayout appBarLayout = view.findViewById(R.id.appbar);
            appBarLayout.setExpanded(true,true);

            View toolbar = view.findViewById(R.id.toolbar);

            if (isCanceled){
                raceStatus.setText(R.string.race_canceled_text);
                raceStatus.setTextColor(ContextCompat.getColor(getContext(), R.color.orange));
                show_results.setVisibility(View.GONE);
                podiumLayout.setVisibility(View.GONE);
                backgroundImage.setVisibility(View.GONE);

                day_start.setTextColor(ContextCompat.getColor(getContext(), R.color. text_color_main));
                day_end.setTextColor(ContextCompat.getColor(getContext(), R.color. text_color_main));
                dash.setTextColor(ContextCompat.getColor(getContext(), R.color. text_color_main));
                month.setTextColor(ContextCompat.getColor(getContext(), R.color. text_color_main));


                raceStatus.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
                day_start.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
                day_end.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
                dash.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
                month.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);

                NestedScrollView nestedScrollView = view.findViewById(R.id.nestedScrollView);
                nestedScrollView.setNestedScrollingEnabled(false);
            }else{
                show_results.setVisibility(View.VISIBLE);
                podiumLayout.setVisibility(View.VISIBLE);
                backgroundImage.setVisibility(View.VISIBLE);

                raceStatus.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
                day_start.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
                day_end.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
                dash.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
                month.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);


                raceStatus.setTextColor(ContextCompat.getColor(getContext(), R.color.white));
                day_start.setTextColor(ContextCompat.getColor(getContext(), R.color. white));
                day_end.setTextColor(ContextCompat.getColor(getContext(), R.color. white));
                dash.setTextColor(ContextCompat.getColor(getContext(), R.color. white));
                month.setTextColor(ContextCompat.getColor(getContext(), R.color. white));

                show_results.setOnClickListener(view1 -> {
                    Intent intent = new Intent(requireContext() , raceResultsActivity.class);
                    Bundle bundle = new Bundle();
                    bundle.putString("raceName", mRaceName);
                    bundle.putString("circuitId", mCircuitId);
                    bundle.putString("season", mYear);
                    intent.putExtras(bundle);
                    requireContext().startActivity(intent);
                });

                DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference();

                FirebaseStorage storage = FirebaseStorage.getInstance();
                StorageReference storageRef = storage.getReference();

                StorageReference mWinnerImage = storageRef.child("drivers/" + mFirstPlaceCode.toLowerCase() + "_" + mYear + ".png");
                StorageReference mSecondImage = storageRef.child("drivers/" + mSecondPlaceCode.toLowerCase() + "_" + mYear + ".png");
                StorageReference mThirdImage = storageRef.child("drivers/" + mThirdPlaceCode.toLowerCase() + "_" + mYear + ".png");

                GlideApp.with(requireContext())
                        .load(mWinnerImage)
                        .transition(DrawableTransitionOptions.withCrossFade())
                        .error(R.drawable.placeholder_driver)
                        .into(firstPlace_image);

                GlideApp.with(requireContext())
                        .load(mSecondImage)
                        .transition(DrawableTransitionOptions.withCrossFade())
                        .error(R.drawable.placeholder_driver)
                        .into(secondPlace_image);

                GlideApp.with(requireContext())
                        .load(mThirdImage)
                        .transition(DrawableTransitionOptions.withCrossFade())
                        .error(R.drawable.placeholder_driver)
                        .into(thirdPlace_image);

                rootRef.child("drivers").orderByChild("driversCode").equalTo(mFirstPlaceCode).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        for (DataSnapshot ds: snapshot.getChildren()){
                            String driver = ds.getKey();
                            String[] driverFullName = driver.split(" ");
                            //String driverName;
                            String driverFamilyName;
                            if (driver.equals("Andrea Kimi Antonelli")){
                                //    driverName = driverFullName[1];
                                driverFamilyName = driverFullName[2];
                            }else{
                                //    driverName = driverFullName[0];
                                driverFamilyName = driverFullName[1];
                            }
                            //String mDriverName = driverName.charAt(0) + ". " + driverFamilyName;
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
                            //String mDriverName = driverName.charAt(0) + ". " + driverFamilyName;
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
                            //String driverName;
                            String driverFamilyName;
                            if (driver.equals("Andrea Kimi Antonelli")){
                                //    driverName = driverFullName[1];
                                driverFamilyName = driverFullName[2];
                            }else{
                                //    driverName = driverFullName[0];
                                driverFamilyName = driverFullName[1];
                            }
                            //String mDriverName = driverName.charAt(0) + ". " + driverFamilyName;
                            thirdPlace_code.setText(driverFamilyName);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e("concludedRacePage", "Drivers error:" + error.getMessage());
                    }
                });
            }

            String localeRaceName = mRaceName.toLowerCase().replaceAll("\\s+", "_");
            String pastRaceName = requireContext().getString(getStringByName(localeRaceName + "_text")) + " " + mYear;

            fullRaceName_key = mYear + "_" + mRaceName.replace(" ", "");

            currentDate = LocalDate.now();
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user!=null){
                isSaved(fullRaceName_key);
                saveRace.setOnClickListener(view2 -> {
                    if(saveRace.isChecked()){
                        saveRace(currentDate);
                    }else{
                        deleteRace(fullRaceName_key);
                    }
                });
            }else{
                saveRace.setOnClickListener(view3 -> {
                    saveRace.setChecked(false);
                    Toast.makeText(requireContext(), getString(R.string.race_save_error_login_text), Toast.LENGTH_LONG).show();

                });
            }

            infoRaceName.setText(pastRaceName);

            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-M-d");
            LocalDate dateStart = LocalDate.parse(mRaceStartDate, dateFormatter);
            LocalDate dateEnd = LocalDate.parse(mRaceEndDate, dateFormatter);

            String monthStartFull = dateStart.format(DateTimeFormatter.ofPattern("MMMM"));
            String monthEndFull = dateEnd.format(DateTimeFormatter.ofPattern("MMMM"));

            //if(mRaceStartMonth.equals(mRaceEndMonth)){
            //    month.setText(mRaceStartMonth);
            //}
            //else{
            //    String monthAll = mRaceStartMonth + "-" + mRaceEndMonth;
            //    month.setText(monthAll);
            //}

            if(monthStartFull.equals(monthEndFull)){
                month.setText(monthStartFull);
            }
            else{
                String monthAll = monthStartFull + " - " + monthEndFull;
                month.setText(monthAll);
            }

            day_start.setText(mRaceStartDay);
            day_end.setText(mRaceEndDay);

            datum = new ArrayList<>();

            adapter = new scheduleAdapter(getActivity(), datum, true);
            recyclerView.setAdapter(adapter);

            getRaceSchedule(mRaceName, mYear);
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
                Log.e("fatal", raceName + " " + raceQuali + " " + sprintDate);
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

                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("futureActivityFirebaseError", error.getMessage());
            }
        });
    }

}
