package com.example.f1app;

import static com.example.f1app.MainActivity.getStringByName;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.blongho.country_data.World;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Locale;

public class driverStatsFragment extends Fragment {
    private TextView firstGP, GPcount, wins, podiums, poles, totalPoints,
            championships, championshipsText, teamName, driverCountry, birthdate, totalFastestLaps,
            driverAge,lastEntry, lineupHeader;
    private ImageView teamCar_image, flag, arrow;
    private RelativeLayout team_layout, driverInfo_layout;
    private LinearLayout driversTeam_layout;
    private ScrollView scrollView;


    public driverStatsFragment() {
        // required empty public constructor.
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    public void onResume(){
        super.onResume();
        scrollView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                scrollView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                scrollView.smoothScrollTo(0, 0);
            }
        });
        getView().requestLayout();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.driver_page_stats_fragment, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        scrollView = (ScrollView) view.findViewById(R.id.scrollView);
        firstGP = (TextView) view.findViewById(R.id.firstGP);
        GPcount = (TextView) view.findViewById(R.id.GP_count);
        lastEntry = (TextView) view.findViewById(R.id.lastEntry);
        wins = (TextView) view.findViewById(R.id.wins);
        podiums = (TextView) view.findViewById(R.id.podiums);
        poles = (TextView) view.findViewById(R.id.poles);
        totalPoints = (TextView) view.findViewById(R.id.totalPoints);
        championships = (TextView) view.findViewById(R.id.championships);
        championshipsText = (TextView) view.findViewById(R.id.championshipsText);
        teamName = (TextView) view.findViewById(R.id.teamName);
        driverCountry = (TextView) view.findViewById(R.id.country);
        birthdate = (TextView) view.findViewById(R.id.birthdate);
        totalFastestLaps = (TextView) view.findViewById(R.id.totalFastestLaps);
        driverAge = (TextView) view.findViewById(R.id.driverAge);
        lineupHeader = (TextView) view.findViewById(R.id.lineup_header);


        teamCar_image = (ImageView) view.findViewById(R.id.teamCar);
        flag = (ImageView) view.findViewById(R.id.flag);
        arrow = (ImageView) view.findViewById(R.id.arrow);

        team_layout = (RelativeLayout) view.findViewById(R.id.team_layout);
        driverInfo_layout = (RelativeLayout) view.findViewById(R.id.driverInfo_layout);
        driversTeam_layout = (LinearLayout) view.findViewById(R.id.driversTeam_layout);

        DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference();
        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference storageRef = storage.getReference();
        if (!getArguments().isEmpty()){
            String mDriverName = getArguments().getString("driverName");
            String mDriverFamilyName = getArguments().getString("driverFamilyName");
            String mDriverTeam = getArguments().getString("driverTeam");
            String mDriverCode = getArguments().getString("driverCode");
            String mTeamId = getArguments().getString("driverTeamId");

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
                    String mLastEntry = snapshot.child("lastEntry").getValue(String.class);

                    String[] mFirstGPparse = mFirstGP.split("\\s+");
                    String mFirstSeason = mFirstGPparse[0];
                    String mFirstRaceName = mFirstGP.substring(5);

                    String localeRaceName = mFirstRaceName.toLowerCase().replaceAll("\\s+", "_");
                    String firstRaceName = requireContext().getString(getStringByName(localeRaceName + "_text")) + " " + mFirstSeason;

                    String[] mLastGPparse = mLastEntry.split("\\s+");
                    String mLastSeason = mLastGPparse[0];
                    String mLastRaceName = mLastEntry.substring(5);

                    String localeLastRaceName = mLastRaceName.toLowerCase().replaceAll("\\s+", "_");
                    String lastRaceName = requireContext().getString(getStringByName(localeLastRaceName + "_text")) + " " + mLastSeason;

                    firstGP.setText(firstRaceName);
                    //firstGPseason.setText(mFirstSeason);
                    GPcount.setText(mGPcount);
                    wins.setText(mWins);
                    podiums.setText(mPodiums);
                    poles.setText(mPoles);
                    totalPoints.setText(mTotalPoints);
                    lastEntry.setText(lastRaceName);

                    if (!mLastSeason.equals(currentYear)){
                        String mLineUpHeader = getString(R.string.last_text) + " " + getString(R.string.team);
                        lineupHeader.setText(mLineUpHeader);
                    }
                    //driverCountry.setText(mDriverCountry);

                    Locale driverCountryLocale = new Locale(Locale.getDefault().getLanguage(), getCountryCode(mDriverCountry));
                    //Locale currentC = ConfigurationCompat.getLocales(getResources().getConfiguration()).get(0);
                    driverCountry.setText(driverCountryLocale.getDisplayCountry());
                    totalFastestLaps.setText(mTotalFastestLaps);

                    teamName.setText(mDriverTeam);
                    driversTeam_layout.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            rootRef.child("constructors").child(mTeamId).addListenerForSingleValueEvent(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                                            String mTeamId = snapshot.child("constructorId").getValue(String.class);
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

                    String mDriverAge = " ";
                    String mChampionshipsText = " ";

                    if(Locale.getDefault().getLanguage().equals("ru")){
                        if (age%10 < 5 && age%10!=0){
                            if (age % 10 == 1){
                                mDriverAge = " (" + age + " " + getString(R.string.age_text) + ")";
                            }else{
                                mDriverAge = " (" + age + " " + getString(R.string.age_text) + "a)";
                            }

                        }else{
                            mDriverAge =  " (" + age + " " + getString(R.string.age_alt_text) + ")";
                        }
                        assert mChampionships != null;
                        int champCount = Integer.parseInt(mChampionships);
                        if (champCount % 10 < 5 && champCount % 10!= 0){
                            mChampionshipsText = getString(R.string.times) + "a";
                        }else{
                            mChampionshipsText = getString(R.string.times);
                        }
                        championshipsText.setText(mChampionshipsText);
                    }else{
                        mDriverAge = " (" + age + " " + getString(R.string.age_text) + ")";
                    }
                    birthdate.setText(mBirthdate);
                    driverAge.setText(mDriverAge);

                    championships.setText(mChampionships);

                    World.init(requireContext());
                    flag.setImageResource(World.getFlagOf(getCountryCode(mDriverCountry)));


                    rootRef.child("constructors").child(mTeamId).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            String mTeamId = snapshot.child("constructorId").getValue(String.class);
                            String mTeamColor = "#" + snapshot.child("color").getValue(String.class);

                            StorageReference mWinnerImage = storageRef.child("teams/" + mTeamId.toLowerCase() + ".png");

                            GlideApp.with(requireContext())
                                    .load(mWinnerImage)
                                    .transition(DrawableTransitionOptions.withCrossFade())
                                    .diskCacheStrategy(DiskCacheStrategy.NONE)
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

    private String localeToEmoji(Locale locale) {
        String countryCode = locale.getCountry();
        int firstLetter = Character.codePointAt(countryCode, 0) - 0x41 + 0x1F1E6;
        int secondLetter = Character.codePointAt(countryCode, 1) - 0x41 + 0x1F1E6;
        return new String(Character.toChars(firstLetter)) + new String(Character.toChars(secondLetter));

    }

    public static String getCountryCode(String countryName) {
        String[] isoCountryCodes = Locale.getISOCountries();
        switch (countryName.toLowerCase()) {
            case "usa":
                return "us";
            case "uk":
                return "gb";
            case "uae":
                return "ae";
            case "german":
                return "de";
            default:
                for (String countryCode : isoCountryCodes) {
                    Locale locale = new Locale("en", countryCode);
                    String iso = locale.getISO3Country();
                    String code = locale.getCountry();
                    String name = locale.getDisplayCountry(new Locale("en", iso));
                    if (countryName.equalsIgnoreCase(name)) {
                        return code;
                    }
                }
                break;
        }
        return " ";
    }
}