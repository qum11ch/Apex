package com.example.f1app;

import static com.example.f1app.MainActivity.getStringByName;
import static com.example.f1app.teamPageActivity.showMessage;

import android.app.Activity;
import android.content.Context;
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
import android.widget.ToggleButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.AppBarLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class concludedRaceScheduleFragment extends Fragment {
    private List<scheduleData> datum;
    private scheduleAdapter adapter;
    private ToggleButton saveRaceButton;
    private LocalDate currentDate;
    private String fullRaceName_key, mRaceName, mYear;
    private racePodiumBlock podium;
    private String mFirstPlaceCode, mSecondPlaceCode, mThirdPlaceCode;


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
        super.onViewCreated(view, savedInstanceState);
        EdgeInsets.applyBottom(view.findViewById(R.id.nestedScrollView));

        TextView infoRaceName = view.findViewById(R.id.infoRaceName);
        ImageView backgroundImage = view.findViewById(R.id.background_img);
        TextView raceStatus = view.findViewById(R.id.raceStatus);
        TextView day_start = view.findViewById(R.id.day_start);
        TextView day_end = view.findViewById(R.id.day_end);
        TextView month = view.findViewById(R.id.month);
        TextView show_results = view.findViewById(R.id.show_results);
        saveRaceButton = view.findViewById(R.id.saveRace);
        LinearLayout podiumLayout = view.findViewById(R.id.podium_layout);
        TextView dash = view.findViewById(R.id.dash);

        podium = new racePodiumBlock(view);

        RecyclerView recyclerView = view.findViewById(R.id.recyclerview_schedule);
        // recyclerView.setHasFixedSize(true);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(requireContext());
        recyclerView.setLayoutManager(linearLayoutManager);

        if(!getArguments().isEmpty()){
            String mCircuitId = getArguments().getString("circuitId");
            mRaceName = getArguments().getString("raceName");
            String mRaceStartDay = getArguments().getString("raceStartDay");
            String mRaceEndDay = getArguments().getString("raceEndDay");
            String mRaceStartDate = getArguments().getString("dateStart");
            String mRaceEndDate = getArguments().getString("dateEnd");

            mYear = getArguments().getString("gpYear");
            mFirstPlaceCode = getArguments().getString("firstPlaceCode");
            mSecondPlaceCode = getArguments().getString("secondPlaceCode");
            mThirdPlaceCode = getArguments().getString("thirdPlaceCode");
            boolean isCanceled = getArguments().getBoolean("isCanceled");

            AppBarLayout appBarLayout = view.findViewById(R.id.appbar);
            appBarLayout.setExpanded(true,true);

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

                // photos and codes at once; surnames, colours and gaps arrive with the round number
                podium.showBasics(mYear, mFirstPlaceCode, mSecondPlaceCode, mThirdPlaceCode);
            }

            String localeRaceName = mRaceName.toLowerCase().replaceAll("\\s+", "_");
            String pastRaceName = requireContext().getString(getStringByName(localeRaceName + "_text")) + " " + mYear;

            fullRaceName_key = mYear + "_" + mRaceName.replace(" ", "");

            currentDate = LocalDate.now();
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

            if (user!=null){
                isSavedRace(fullRaceName_key, saveRaceButton, getContext());
                saveRaceButton.setOnClickListener(view2 -> {
                    if(saveRaceButton.isChecked()){
                        saveRace(currentDate,
                                fullRaceName_key,
                                mRaceName,
                                mYear,
                                saveRaceButton,
                                getContext());
                    }else{
                        deleteRace(fullRaceName_key, saveRaceButton, getContext());
                    }
                });
            }

            infoRaceName.setText(pastRaceName);

            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-M-d");
            LocalDate dateStart = LocalDate.parse(mRaceStartDate, dateFormatter);
            LocalDate dateEnd = LocalDate.parse(mRaceEndDate, dateFormatter);

            String monthStartFull = dateStart.format(DateTimeFormatter.ofPattern("MMMM"));
            String monthEndFull = dateEnd.format(DateTimeFormatter.ofPattern("MMMM"));

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
            adapter.setOnWeatherClick((key, weather) -> weatherSheet.show(
                    requireContext(), getString(getStringByName(key + "_text")), weather));
            adapter.setOnSessionClick(sessionKey -> {
                for (scheduleData item : datum) {
                    if (sessionKey.equals(item.getEventName()) && item.getResults() != null) {
                        sessionResultsSheet.show(getParentFragmentManager(),
                                getString(getStringByName(sessionKey + "_text")), sessionKey, item.getResults());
                        return;
                    }
                }
            });
            loadWeekendWeather(view);

            getRaceSchedule(mRaceName, mYear);
        }
    }

    private void loadWeekendWeather(View root) {
        TextView note = root.findViewById(R.id.weather_note);

        weekendWeather.load(requireContext(), mYear, mRaceName, (byKey, text) -> {
            if (!isAdded()) return;

            adapter.setWeather(byKey);
            note.setText(text);
            note.setVisibility(text.isEmpty() ? View.GONE : View.VISIBLE);

            if (podium != null) {
                podium.setRaceWeather(byKey.get("race_event"));
            }
        });
    }

    public static void saveRace(LocalDate currentDate,
                                String fullRaceName_key,
                                String raceName,
                                String raceSeason,
                                ToggleButton toggleButton,
                                Context context) {

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user == null) {
            toggleButton.setOnClickListener(view -> {
                toggleButton.setChecked(false);
            });

            showMessage(context, context.getString(R.string.race_save_error_login_text));
            return;
        }

        String uid = user.getUid();

        DatabaseReference savedRacesRef = FirebaseDatabase.getInstance()
                .getReference()
                .child("savedRaces")
                .child(uid);

        DateTimeFormatter formatterUpdate = DateTimeFormatter.ofPattern("d/MM/uuuu");
        String saveDate = currentDate.format(formatterUpdate);
        savedRacesData savedRacesData = new savedRacesData(raceName, raceSeason, saveDate);

        savedRacesRef.child(fullRaceName_key).setValue(savedRacesData)
                .addOnSuccessListener(unused ->
                        showMessage(
                                context,
                                context.getString(R.string.race_save_succ_text))
                ).addOnFailureListener(error -> {
                    if (context instanceof Activity
                            && (((Activity) context).isFinishing()
                            || ((Activity) context).isDestroyed())) {
                        return;
                    }
                    showMessage(
                            context,
                            context.getString(R.string.smth_wrong_text)
                    );
                });
    }

    public static void deleteRace(String fullRaceName_key,
                                  ToggleButton toggleButton, Context context) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user == null) {
            toggleButton.setOnClickListener(view -> {
                toggleButton.setChecked(false);
            });

            showMessage(context, context.getString(R.string.race_save_error_login_text));
            return;
        }

        FirebaseDatabase.getInstance()
                .getReference()
                .child("savedRaces")
                .child(user.getUid())
                .child(fullRaceName_key)
                .removeValue()
                .addOnSuccessListener(unused -> {
                    if (context instanceof Activity
                            && (((Activity) context).isFinishing()
                            || ((Activity) context).isDestroyed())) {
                        return;
                    }

                    showMessage(context,
                            context.getString(R.string.race_delete_succ_text));
                })
                .addOnFailureListener(error -> {
                    if (context instanceof Activity
                            && (((Activity) context).isFinishing()
                            || ((Activity) context).isDestroyed())) {
                        return;
                    }

                    Log.e(
                            context.getPackageName(),
                            "Failed to delete race",
                            error
                    );

                    showMessage(context,
                            context.getString(R.string.smth_wrong_text));
                });
    }

    public static void isSavedRace(String fullRaceName_key,
                                   ToggleButton toggleButton,
                                   Context context){
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user == null) {
            toggleButton.setChecked(false);
            return;
        }

        String uid = user.getUid();

        DatabaseReference savedRacesRef = FirebaseDatabase.getInstance()
                .getReference()
                .child("savedRaces")
                .child(uid);

        savedRacesRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                toggleButton.setChecked(snapshot.hasChild(fullRaceName_key));
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("Firebase", "Ошибка чтения: " + error.getMessage());
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

                Integer round = snapshot.child("round").getValue(Integer.class);
                Log.d("schedule_error", "exists=" + snapshot.exists() + " round=" + round + " isAdded=" + isAdded() + " key=" + raceName + " year=" + currentYear);

                if (round != null && isAdded()) {
                    // the round is known now: surnames, team colours and gaps of the podium
                    if (podium != null) {
                        podium.bind(currentYear, String.valueOf(round),
                                mFirstPlaceCode, mSecondPlaceCode, mThirdPlaceCode);
                    }

                    sessionResultsLoader.load(
                            concludedRaceScheduleFragment.this,
                            currentYear,
                            String.valueOf(round),
                            datum,
                            adapter
                    );
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("futureActivityFirebaseError", error.getMessage());
            }
        });
    }

}