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
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;

import com.blongho.country_data.World;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
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
            driverAge,lastEntry, lineupHeader, deathdate;
    private ImageView teamCar_image, flag, arrow;
    private RelativeLayout team_layout, driverInfo_layout, deathDateLayout;
    private LinearLayout driversTeam_layout;
    private NestedScrollView scrollView;


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

        scrollView = view.findViewById(R.id.scrollView);

        firstGP = view.findViewById(R.id.firstGP);
        GPcount = view.findViewById(R.id.GP_count);
        lastEntry = view.findViewById(R.id.lastEntry);
        wins = view.findViewById(R.id.wins);
        podiums = view.findViewById(R.id.podiums);
        poles = view.findViewById(R.id.poles);
        totalPoints = view.findViewById(R.id.totalPoints);
        championships = view.findViewById(R.id.championships);
        championshipsText = view.findViewById(R.id.championshipsText);

        teamName = view.findViewById(R.id.teamName);
        driverCountry = view.findViewById(R.id.country);
        birthdate = view.findViewById(R.id.birthdate);
        totalFastestLaps = view.findViewById(R.id.totalFastestLaps);

        deathdate = view.findViewById(R.id.deathdate);
        deathDateLayout = view.findViewById(R.id.deathDateLayout);

        driverAge = view.findViewById(R.id.driverAge);
        lineupHeader = view.findViewById(R.id.lineup_header);

        teamCar_image = view.findViewById(R.id.teamCar);
        flag = view.findViewById(R.id.flag);
        arrow = view.findViewById(R.id.arrow);

        team_layout = view.findViewById(R.id.team_layout);
        driverInfo_layout = view.findViewById(R.id.driverInfo_layout);

        driversTeam_layout = view.findViewById(R.id.driversTeam_layout);

        if (getArguments() == null) {
            Log.e(
                    "DriverStats",
                    "Missing fragment arguments"
            );

            return;
        }

        String mDriverName = getArguments().getString("driverName");
        String mDriverFamilyName = getArguments().getString("driverFamilyName");
        String mDriverTeam = getArguments().getString("driverTeam");
        String mTeamId = getArguments().getString("driverTeamId");
        String mCurrentSeason = getArguments().getString("currentSeason");

        if (mDriverName == null
                || mDriverFamilyName == null
                || mDriverTeam == null
                || mCurrentSeason == null) {

            Log.e(
                    "DriverStats",
                    "Missing required driver arguments"
            );

            return;
        }

        String driverFullName = mDriverName + " " + mDriverFamilyName;

        FirebaseDatabase.getInstance()
                .getReference()
                .child("drivers")
                .child(driverFullName)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (!isAdded()) {
                            return;
                        }

                        if (!snapshot.exists()) {
                            Log.e(
                                    "DriverStats",
                                    "Driver not found: "
                                            + driverFullName
                            );

                            return;
                        }

                        String mFirstGP = snapshot
                                .child("firstEntry")
                                .getValue(String.class);

                        String mLastEntry = snapshot
                                .child("lastEntry")
                                .getValue(String.class);

                        String mGPcount = snapshot
                                .child("gpEntered")
                                .getValue(String.class);

                        String mWins = snapshot
                                .child("totalWins")
                                .getValue(String.class);

                        String mPodiums = snapshot
                                .child("totalPodiums")
                                .getValue(String.class);

                        String mPoles = snapshot
                                .child("polesCount")
                                .getValue(String.class);

                        String mTotalPoints = snapshot
                                .child("totalPoints")
                                .getValue(String.class);

                        String mChampionships = snapshot
                                .child("championshipsCount")
                                .getValue(String.class);

                        String mDriverCountry = snapshot
                                .child("driverCountry")
                                .getValue(String.class);

                        String mBirthdate = snapshot
                                .child("birthdayDate")
                                .getValue(String.class);

                        String mTotalFastestLaps = snapshot
                                .child("fastestLapCount")
                                .getValue(String.class);

                        String mDeathdate = snapshot
                                .child("deathDate")
                                .getValue(String.class);

                        if (mFirstGP == null
                                || mLastEntry == null
                                || mGPcount == null
                                || mWins == null
                                || mPodiums == null
                                || mPoles == null
                                || mTotalPoints == null
                                || mChampionships == null
                                || mDriverCountry == null
                                || mBirthdate == null
                                || mTotalFastestLaps == null) {

                            Log.e(
                                    "DriverStats",
                                    "Incomplete driver data"
                            );

                            return;
                        }

                        Integer firstSeason = extractYear(mFirstGP);
                        Integer lastSeason = extractYear(mLastEntry);

                        if (firstSeason == null || lastSeason == null) {
                            Log.e(
                                    "DriverStats",
                                    "Invalid entry format"
                            );

                            return;
                        }

                        firstGP.setText(formatRaceEntry(mFirstGP));

                        lastEntry.setText(formatRaceEntry(mLastEntry));

                        GPcount.setText(mGPcount);
                        wins.setText(mWins);
                        podiums.setText(mPodiums);
                        poles.setText(mPoles);
                        totalPoints.setText(mTotalPoints);
                        totalFastestLaps.setText(mTotalFastestLaps);

                        int currentYear = LocalDate.now().getYear();

                        if (lastSeason != currentYear) {

                            String lineupHeaderText =
                                    getString(R.string.last_text)
                                            + " "
                                            + getString(R.string.team);
                            lineupHeader.setText(lineupHeaderText);
                        }

                        setupCountry(mDriverCountry);
                        setupBirthdate(mBirthdate, mDeathdate);
                        setupChampionships(mChampionships);

                        teamName.setText(mDriverTeam);

                        if (mTeamId != null && !mTeamId.trim().isEmpty()) {

                            setupTeamCard(
                                    mTeamId,
                                    mDriverTeam,
                                    mCurrentSeason,
                                    lastSeason
                            );

                            driversTeam_layout.setOnClickListener(
                                    view1 -> openTeamPage(
                                            mTeamId,
                                            mDriverTeam,
                                            mCurrentSeason,
                                            lastSeason
                                    )
                            );
                        } else {
                            applyDefaultTeamCard();
                        }
                    }

                    @Override
                    public void onCancelled(
                            @NonNull DatabaseError error) {

                        if (!isAdded()) {
                            return;
                        }

                        Log.e(
                                "DriverStats",
                                "Failed to load driver",
                                error.toException()
                        );
                    }
                });
    }

    private void setupTeamCard(
            String teamId,
            String driverTeam,
            String currentSeason,
            int lastSeason) {

        FirebaseDatabase.getInstance()
                .getReference()
                .child("constructors")
                .child(teamId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(
                            @NonNull DataSnapshot snapshot) {

                        if (!isAdded()) {
                            return;
                        }

                        if (!snapshot.exists()) {
                            applyDefaultTeamCard();
                            return;
                        }

                        String actualTeamId = snapshot
                                .child("constructorId")
                                .getValue(String.class);

                        String teamColor = snapshot
                                .child("color")
                                .getValue(String.class);

                        if (actualTeamId == null) {
                            applyDefaultTeamCard();
                            return;
                        }

                        applyTeamColor(teamColor);

                        loadTeamCar(
                                actualTeamId,
                                lastSeason
                        );
                    }

                    @Override
                    public void onCancelled(
                            @NonNull DatabaseError error) {

                        if (!isAdded()) {
                            return;
                        }

                        Log.e(
                                "DriverStats",
                                "Failed to load team",
                                error.toException()
                        );

                        applyDefaultTeamCard();
                    }
                });
    }

    private void loadTeamCar(String teamId, int season) {
        String imagePath =
                "teams/"
                        + teamId.toLowerCase()
                        + "_"
                        + season
                        + ".png";

        StorageReference teamCarReference =
                FirebaseStorage.getInstance()
                        .getReference()
                        .child(imagePath);

        teamCarReference.getDownloadUrl()
                .addOnSuccessListener(uri -> {
                    if (!isAdded()) {
                        return;
                    }

                    GlideApp.with(requireContext())
                            .load(uri)
                            .transition(
                                    DrawableTransitionOptions
                                            .withCrossFade()
                            )
                            .diskCacheStrategy(
                                    DiskCacheStrategy.ALL
                            )
                            .error(R.drawable.placeholder_car)
                            .into(teamCar_image);
                })
                .addOnFailureListener(error -> {
                    if (!isAdded()) {
                        return;
                    }

                    Log.w(
                            "DriverStats",
                            "Team car not found: " + imagePath,
                            error
                    );

                    teamCar_image.setImageResource(
                            R.drawable.placeholder_car
                    );
                });
    }

    private void applyTeamColor(String colorValue) {
        if (colorValue == null || colorValue.trim().isEmpty()) {
            applyDefaultTeamCard();
            return;
        }

        String preparedColor = colorValue.trim();

        if (!preparedColor.startsWith("#")) {
            preparedColor = "#" + preparedColor;
        }

        try {
            int parsedColor =
                    Color.parseColor(preparedColor);

            team_layout.setBackground(
                    createTeamCardBackground(parsedColor, 12)
            );

            driverInfo_layout.setBackground(
                    createTeamCardBackground(parsedColor, 15)
            );

            arrow.setColorFilter(parsedColor);
        } catch (IllegalArgumentException error) {
            Log.w(
                    "DriverStats",
                    "Invalid team color: " + colorValue,
                    error
            );

            applyDefaultTeamCard();
        }
    }

    private GradientDrawable createTeamCardBackground(
            int strokeColor,
            int strokeWidthPx) {

        GradientDrawable background = new GradientDrawable();

        background.setColor(
                ContextCompat.getColor(
                        requireContext(),
                        R.color.white
                )
        );

        background.setCornerRadii(
                new float[]{
                        0, 0,
                        30, 30,
                        0, 0,
                        0, 0
                }
        );

        background.setStroke(strokeWidthPx, strokeColor);

        return background;
    }

    private void openTeamPage(
            String teamId,
            String driverTeam,
            String currentSeason,
            int lastSeason) {

        FirebaseDatabase.getInstance()
                .getReference()
                .child("driverLineUp")
                .child("season")
                .child(String.valueOf(lastSeason))
                .child(teamId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(
                            @NonNull DataSnapshot snapshot) {

                        if (!isAdded()) {
                            return;
                        }

                        ArrayList<String> teamDrivers =
                                new ArrayList<>();

                        DataSnapshot driversSnapshot =
                                snapshot.child("drivers");

                        for (DataSnapshot driverSnapshot :
                                driversSnapshot.getChildren()) {

                            String teamDriverName =
                                    driverSnapshot.getKey();

                            if (teamDriverName != null) {
                                teamDrivers.add(teamDriverName);
                            }
                        }

                        Intent intent = new Intent(
                                requireContext(),
                                teamPageActivity.class
                        );

                        Bundle bundle = new Bundle();

                        bundle.putString("teamName", driverTeam);
                        bundle.putString("teamId", teamId);
                        bundle.putStringArrayList(
                                "teamDrivers",
                                teamDrivers
                        );

                        bundle.putString(
                                "currentSeason",
                                currentSeason
                        );

                        intent.putExtras(bundle);

                        startActivity(intent);
                    }

                    @Override
                    public void onCancelled(
                            @NonNull DatabaseError error) {

                        if (!isAdded()) {
                            return;
                        }

                        Log.e(
                                "DriverStats",
                                "Failed to load team lineup",
                                error.toException()
                        );
                    }
                });
    }

    private void applyDefaultTeamCard() {
        int defaultColor = ContextCompat.getColor(
                requireContext(),
                R.color.grey
        );

        team_layout.setBackground(
                createTeamCardBackground(defaultColor, 12)
        );

        driverInfo_layout.setBackground(
                createTeamCardBackground(defaultColor, 15)
        );

        arrow.setColorFilter(defaultColor);
        teamCar_image.setImageResource(
                R.drawable.placeholder_car
        );
    }


    public static int calculateAge(LocalDate birthdate, LocalDate currentDate) {
        Period period = Period.between(birthdate, currentDate);

        return period.getYears();
    }

    private Integer extractYear(String raceEntry) {
        if (raceEntry == null || raceEntry.trim().isEmpty()) {
            return null;
        }

        java.util.regex.Matcher matcher = java.util.regex.Pattern
                .compile("\\b(\\d{4})\\b")
                .matcher(raceEntry);

        if (!matcher.find()) {
            return null;
        }

        try {
            return Integer.parseInt(matcher.group(1));
        } catch (NumberFormatException error) {
            return null;
        }
    }

    private String formatRaceEntry(String raceEntry) {
        if (raceEntry == null) {
            return "";
        }

        java.util.regex.Matcher matcher = java.util.regex.Pattern
                .compile("^(\\d{4})\\s+(.+)$")
                .matcher(raceEntry.trim());

        if (!matcher.matches()) {
            return raceEntry;
        }

        String year = matcher.group(1);
        String raceName = matcher.group(2);

        String localizationKey = raceName
                .toLowerCase()
                .replaceAll("\\s+", "_")
                + "_text";

        int stringResId = getStringByName(localizationKey);

        String localizedRaceName = stringResId != 0
                ? getString(stringResId)
                : raceName;

        return localizedRaceName + " " + year;
    }

    private void setupCountry(String countryName) {
        String countryCode = getCountryCode(countryName);

        if (countryCode == null || countryCode.trim().isEmpty()) {
            return;
        }

        Locale countryLocale = new Locale(
                Locale.getDefault().getLanguage(),
                countryCode
        );

        driverCountry.setText(
                countryLocale.getDisplayCountry()
        );

        World.init(requireContext());
        flag.setImageResource(
                World.getFlagOf(countryCode)
        );
    }

    private void setupBirthdate(
            String birthdateValue,
            String deathDateValueFromDb) {

        deathDateLayout.setVisibility(View.GONE);
        deathdate.setText("");
        driverAge.setText("");

        birthdate.setText(
                birthdateValue != null ? birthdateValue : "—"
        );

        if (birthdateValue == null
                || birthdateValue.trim().isEmpty()) {
            return;
        }

        try {
            LocalDate born = LocalDate.parse(
                    birthdateValue.trim(),
                    DateTimeFormatter.ISO_LOCAL_DATE
            );

            LocalDate today = LocalDate.now();
            LocalDate ageEndDate = today;

            if (born.isAfter(today)) {
                throw new IllegalArgumentException(
                        "Birthdate is in the future"
                );
            }

            boolean hasDeathDate =
                    deathDateValueFromDb != null
                            && !deathDateValueFromDb.trim().isEmpty();

            if (hasDeathDate) {
                LocalDate died = LocalDate.parse(
                        deathDateValueFromDb.trim(),
                        DateTimeFormatter.ISO_LOCAL_DATE
                );

                if (died.isBefore(born) || died.isAfter(today)) {
                    throw new IllegalArgumentException(
                            "Invalid death date"
                    );
                }

                ageEndDate = died;

                deathdate.setText(
                        died.format(DateTimeFormatter.ISO_LOCAL_DATE)
                );

                deathDateLayout.setVisibility(View.VISIBLE);
            }

            int age = Period.between(born, ageEndDate).getYears();

            driverAge.setText(formatAge(age));

        } catch (java.time.format.DateTimeParseException
                 | IllegalArgumentException error) {

            Log.e(
                    "DriverStats",
                    "Invalid life dates: birth="
                            + birthdateValue
                            + ", death="
                            + deathDateValueFromDb,
                    error
            );

            // При ошибочной дате не показываем выдуманный возраст.
            driverAge.setText("");
            deathDateLayout.setVisibility(View.GONE);
        }
    }

    private String formatAge(int age) {
        if (!Locale.getDefault().getLanguage().equals("ru")) {
            return " (" + age + " "
                    + getString(R.string.age_text) + ")";
        }

        int lastDigit = age % 10;

        if (lastDigit == 1 && age % 100 != 11) {
            return " (" + age + " "
                    + getString(R.string.age_text) + ")";
        }

        if (lastDigit >= 2
                && lastDigit <= 4
                && (age % 100 < 12 || age % 100 > 14)) {

            return " (" + age + " "
                    + getString(R.string.age_text) + "a)";
        }

        return " (" + age + " "
                + getString(R.string.age_alt_text) + ")";
    }

    private void setupChampionships(String championshipsValue) {
        championships.setText(championshipsValue);

        if (!Locale.getDefault().getLanguage().equals("ru")) {
            return;
        }

        try {
            int championshipsCount = Integer.parseInt(championshipsValue);
            int lastDigit = championshipsCount % 10;

            String suffix;

            if (lastDigit == 1 && championshipsCount % 100 != 11) {
                suffix = getString(R.string.times);
            } else if (lastDigit >= 2
                    && lastDigit <= 4
                    && (championshipsCount % 100 < 12
                    || championshipsCount % 100 > 14)) {

                suffix = getString(R.string.times) + "a";
            } else {
                suffix = getString(R.string.times);
            }

            championshipsText.setText(suffix);
        } catch (NumberFormatException error) {
            Log.e(
                    "DriverStats",
                    "Invalid championships count: "
                            + championshipsValue,
                    error
            );
        }
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