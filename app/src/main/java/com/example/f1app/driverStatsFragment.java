package com.example.f1app;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.blongho.country_data.World;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Locale;

public class driverStatsFragment extends Fragment {
    private TextView firstGP, GPcount, wins, podiums, poles, totalPoints,
            championships, teamName, driverCountry, birthdate, totalFastestLaps;
    private ImageView teamCar_image, flag, arrow;
    private RelativeLayout team_layout, driverInfo_layout;
    private LinearLayout driversTeam_layout;


    public driverStatsFragment() {
        // required empty public constructor.
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.driver_page_stats_fragment, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        firstGP = (TextView) view.findViewById(R.id.firstGP);
        GPcount = (TextView) view.findViewById(R.id.GP_count);
        wins = (TextView) view.findViewById(R.id.wins);
        podiums = (TextView) view.findViewById(R.id.podiums);
        poles = (TextView) view.findViewById(R.id.poles);
        totalPoints = (TextView) view.findViewById(R.id.totalPoints);
        championships = (TextView) view.findViewById(R.id.championships);
        teamName = (TextView) view.findViewById(R.id.teamName);
        driverCountry = (TextView) view.findViewById(R.id.country);
        birthdate = (TextView) view.findViewById(R.id.birthdate);
        totalFastestLaps = (TextView) view.findViewById(R.id.totalFastestLaps);

        teamCar_image = (ImageView) view.findViewById(R.id.teamCar);
        flag = (ImageView) view.findViewById(R.id.flag);
        arrow = (ImageView) view.findViewById(R.id.arrow);

        team_layout = (RelativeLayout) view.findViewById(R.id.team_layout);
        driverInfo_layout = (RelativeLayout) view.findViewById(R.id.driverInfo_layout);
        driversTeam_layout = (LinearLayout) view.findViewById(R.id.driversTeam_layout);

        DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference();
        if (!getArguments().isEmpty()){
            String mDriverName = getArguments().getString("driverName");
            String mDriverFamilyName = getArguments().getString("driverFamilyName");
            String mDriverTeam = getArguments().getString("driverTeam");
            String mDriverCode = getArguments().getString("driverCode");

            LocalDate currentDate = LocalDate.now();
            String currentYear = Integer.toString(currentDate.getYear());

            String driverName = mDriverName + " " + mDriverFamilyName;
            rootRef.child("drivers").child(driverName).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {

                    String mFirstGP = snapshot.child("firstEntry").getValue(String.class);
                    String mGPcount = snapshot.child("gpEntered").getValue(String.class);
                    String mWins = snapshot.child("totalWins").getValue(String.class);
                    String mPodiums = snapshot.child("totalPodiums").getValue(String.class);
                    String mPoles = snapshot.child("polesCount").getValue(String.class);
                    String mTotalPoints = snapshot.child("totalPoints").getValue(String.class);
                    String mChampionships = snapshot.child("championshipsCount").getValue(String.class);
                    String mDriverCountry = snapshot.child("driverCountry").getValue(String.class);
                    String mBirthdate = snapshot.child("birthdayDate").getValue(String.class);
                    String mTotalFastestLaps = snapshot.child("fastestLapCount").getValue(String.class);

                    firstGP.setText(mFirstGP);
                    GPcount.setText(mGPcount);
                    wins.setText(mWins);
                    podiums.setText(mPodiums);
                    poles.setText(mPoles);
                    totalPoints.setText(mTotalPoints);
                    championships.setText(mChampionships);
                    driverCountry.setText(mDriverCountry);
                    totalFastestLaps.setText(mTotalFastestLaps);

                    teamName.setText(mDriverTeam);
                    driversTeam_layout.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            rootRef.child("constructors").orderByChild("name")
                                    .equalTo(mDriverTeam).addValueEventListener(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                                            for (DataSnapshot postSnapshot: snapshot.getChildren()){
                                                String mTeamId = postSnapshot.child("constructorId").getValue(String.class);
                                                DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference();
                                                rootRef.child("driverLineUp/season/" + currentYear + "/" + mTeamId).addValueEventListener(new ValueEventListener() {
                                                    @Override
                                                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                                                        ArrayList<String> teamDrivers = new ArrayList<>();
                                                        for (DataSnapshot driverDataSnapshot : snapshot.child("drivers").getChildren()) {
                                                            String driverName = driverDataSnapshot.getKey();
                                                            teamDrivers.add(driverName);
                                                        }
                                                        Intent intent = new Intent(requireContext(), teamPageActivity.class);
                                                        Bundle bundle = new Bundle();
                                                        bundle.putString("teamName", mDriverTeam);
                                                        bundle.putString("teamId", mTeamId);
                                                        bundle.putStringArrayList("teamDrivers", teamDrivers);
                                                        intent.putExtras(bundle);
                                                        requireContext().startActivity(intent);
                                                    }
                                                    @Override
                                                    public void onCancelled(@NonNull DatabaseError error) {
                                                        Log.e("driverPageActivity error while opening driver`s team page. ERROR: ", error.getMessage());
                                                    }
                                                });
                                            }

                                        }
                                        @Override
                                        public void onCancelled(@NonNull DatabaseError error) {
                                            Log.e("driverPageActivity", "Driver`s team information getting error: " + error.getMessage());
                                        }
                                    });
                        }
                    });

                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                    LocalDate LDbirthdate = LocalDate.parse(mBirthdate, formatter);
                    LocalDate currentDate = LocalDate.now();
                    int age = calculateAge(LDbirthdate, currentDate);

                    String driverAge = mBirthdate + " (" + age + " years)";
                    birthdate.setText(driverAge);

                    World.init(requireContext());
                    flag.setImageResource(World.getFlagOf(getCountryCode(mDriverCountry)));


                    rootRef.child("constructors").orderByChild("name")
                            .equalTo(mDriverTeam).addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            for (DataSnapshot postSnapshot: snapshot.getChildren()){
                                String mTeamId = postSnapshot.child("constructorId").getValue(String.class);
                                String mTeamColor = "#" + postSnapshot.child("color").getValue(String.class);

                                int resourceId_carImage = requireContext().getResources()
                                        .getIdentifier(mTeamId, "drawable",
                                                requireContext().getPackageName());

                                Glide.with(requireContext())
                                        .load(resourceId_carImage)
                                        .transition(DrawableTransitionOptions.withCrossFade())
                                        .error(R.drawable.f1)
                                        .into(teamCar_image);

                                GradientDrawable gd = new GradientDrawable();
                                gd.setColor(ContextCompat.getColor(requireContext(),R.color.white));
                                gd.setCornerRadii(new float[] {0, 0, 30, 30, 0, 0, 0, 0});
                                gd.setStroke(12, Color.parseColor(mTeamColor));
                                team_layout.setBackground(gd);

                                GradientDrawable gd1 = new GradientDrawable();
                                gd1.setColor(ContextCompat.getColor(requireContext(),R.color.white));
                                gd1.setCornerRadii(new float[] {0, 0, 30, 30, 0, 0, 0, 0});
                                gd1.setStroke(12, Color.parseColor(mTeamColor));
                                driverInfo_layout.setBackground(gd1);

                                arrow.setColorFilter(Color.parseColor(mTeamColor));
                            }

                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            Log.e("driverPageActivity", "Driver`s team information getting error:" + error.getMessage());
                        }
                    });
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e("driverPageActivity", "Driver information getting error:" + error.getMessage());
                }
            });
        }else{
            Log.e("driverPageActivity", "Error: Bundle from teamsAdapter is empty!");
        }

    }

    public static int calculateAge(LocalDate birthdate, LocalDate currentDate) {
        Period period = Period.between(birthdate, currentDate);

        return period.getYears();
    }

    public String getCountryCode(String countryName) {
        String[] isoCountryCodes = Locale.getISOCountries();
        if(countryName.equals("USA")){
            return "us";
        } else if (countryName.equals("UK")) {
            return "gb";
        }
        for (String code : isoCountryCodes) {
            Locale locale = new Locale("", code);
            if (countryName.equalsIgnoreCase(locale.getDisplayCountry())) {
                return code;
            }
        }
        return "";
    }
}