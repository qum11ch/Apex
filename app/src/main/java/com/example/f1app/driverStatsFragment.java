package com.example.f1app;

import static com.example.f1app.MainActivity.getStringByName;

import android.content.Intent;
import android.content.res.ColorStateList;
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
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.ColorUtils;
import androidx.core.view.ViewCompat;
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

    private static final String DASH = "\u2013";

    private TextView firstGP, lastEntry, teamName, driverCountry, birthdate, deathdate, lineupHeader;
    private TextView championshipsValue, pointsValue, winsValue, podiumsValue, polesValue,
            racesValue, fastestValue;
    private ImageView teamCar_image, flag, arrow;
    private View deathDateLayout, driversTeam_layout, teamPhotoBlock;
    private LinearLayout statsTop, statsMid, statsBottom;
    private NestedScrollView scrollView;
    private View rootView;

    private driverSeasonsSection seasonsSection;

    public driverStatsFragment() {
        // required empty public constructor.
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onResume() {
        super.onResume();

        if (scrollView != null) {
            scrollView.getViewTreeObserver().addOnGlobalLayoutListener(
                    new ViewTreeObserver.OnGlobalLayoutListener() {
                        @Override
                        public void onGlobalLayout() {
                            scrollView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                            scrollView.smoothScrollTo(0, 0);
                        }
                    });
        }

        if (getView() != null) {
            getView().requestLayout();
        }
    }

    @Override
    public void onDestroy() {
        if (seasonsSection != null) {
            seasonsSection.stop();
        }

        super.onDestroy();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.driver_page_stats_fragment, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        EdgeInsets.applyBottom(view.findViewById(R.id.scrollView));

        rootView = view;
        scrollView = view.findViewById(R.id.scrollView);

        firstGP = view.findViewById(R.id.firstGP);
        lastEntry = view.findViewById(R.id.lastEntry);
        driverCountry = view.findViewById(R.id.country);
        birthdate = view.findViewById(R.id.birthdate);
        deathdate = view.findViewById(R.id.deathdate);
        deathDateLayout = view.findViewById(R.id.deathDateLayout);
        flag = view.findViewById(R.id.flag);

        teamName = view.findViewById(R.id.teamName);
        teamCar_image = view.findViewById(R.id.teamCar);
        arrow = view.findViewById(R.id.arrow);
        driversTeam_layout = view.findViewById(R.id.driversTeam_layout);
        teamPhotoBlock = view.findViewById(R.id.team_photo_block);
        lineupHeader = view.findViewById(R.id.lineup_header);

        statsTop = view.findViewById(R.id.stats_top);
        statsMid = view.findViewById(R.id.stats_mid);
        statsBottom = view.findViewById(R.id.stats_bottom);

        buildStatsTiles();
        applyDefaultTeamCard();

        if (getArguments() == null) {
            Log.e("DriverStats", "Missing fragment arguments");
            return;
        }

        String mDriverName = getArguments().getString("driverName");
        String mDriverFamilyName = getArguments().getString("driverFamilyName");
        String mDriverCode = getArguments().getString("driverCode");
        String mDriverTeam = getArguments().getString("driverTeam");
        String mTeamId = getArguments().getString("driverTeamId");
        String mCurrentSeason = getArguments().getString("currentSeason");
        int latestSeasonArg = getArguments().getInt("latestSeason", 0);

        if (mDriverName == null
                || mDriverFamilyName == null
                || mDriverTeam == null
                || mCurrentSeason == null) {

            Log.e("DriverStats", "Missing required driver arguments");
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
                            Log.e("DriverStats", "Driver not found: " + driverFullName);
                            return;
                        }

                        String mFirstGP = snapshot.child("firstEntry").getValue(String.class);
                        String mLastEntry = snapshot.child("lastEntry").getValue(String.class);

                        Integer lastSeason = extractYear(mLastEntry);

                        if (lastSeason == null) {
                            lastSeason = latestSeasonArg > 0
                                    ? latestSeasonArg
                                    : parseSeason(mCurrentSeason);
                        }

                        firstGP.setText(mFirstGP != null ? formatRaceEntry(mFirstGP) : DASH);
                        lastEntry.setText(mLastEntry != null ? formatRaceEntry(mLastEntry) : DASH);

                        championshipsValue.setText(text(snapshot.child("championshipsCount")));
                        pointsValue.setText(text(snapshot.child("totalPoints")));
                        winsValue.setText(text(snapshot.child("totalWins")));
                        podiumsValue.setText(text(snapshot.child("totalPodiums")));
                        polesValue.setText(text(snapshot.child("polesCount")));
                        racesValue.setText(text(snapshot.child("gpEntered")));
                        fastestValue.setText(text(snapshot.child("fastestLapCount")));

                        if (lastSeason != LocalDate.now().getYear()) {
                            lineupHeader.setText(getString(R.string.last_text)
                                    + " " + getString(R.string.team));
                        }

                        String country = snapshot.child("driverCountry").getValue(String.class);

                        if (country != null) {
                            setupCountry(country);
                        } else {
                            driverCountry.setText(DASH);
                            flag.setVisibility(View.GONE);
                        }

                        setupBirthdate(
                                snapshot.child("birthdayDate").getValue(String.class),
                                snapshot.child("deathDate").getValue(String.class));

                        teamName.setText(mDriverTeam);

                        if (mTeamId != null && !mTeamId.trim().isEmpty()) {
                            final int finalLastSeason = lastSeason;

                            setupTeamCard(mTeamId, mDriverTeam, mCurrentSeason, finalLastSeason);

                            driversTeam_layout.setOnClickListener(
                                    v -> openTeamPage(mTeamId, mDriverTeam,
                                            mCurrentSeason, finalLastSeason));
                        } else {
                            applyDefaultTeamCard();
                        }

                        seasonsSection = new driverSeasonsSection(
                                driverStatsFragment.this,
                                rootView,
                                driverFullName,
                                mDriverCode,
                                snapshot.child("driverId").getValue(String.class),
                                mDriverFamilyName,
                                lastSeason);

                        seasonsSection.start();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        if (!isAdded()) {
                            return;
                        }

                        Log.e("DriverStats", "Failed to load driver", error.toException());
                    }
                });
    }

    // ---------- statistics tiles ----------

    private void buildStatsTiles() {
        LayoutInflater inflater = LayoutInflater.from(requireContext());

        championshipsValue = addTile(inflater, statsTop, R.string.drivers_championships);
        pointsValue = addTile(inflater, statsTop, R.string.points_earned);

        winsValue = addTile(inflater, statsMid, R.string.wins);
        podiumsValue = addTile(inflater, statsMid, R.string.podiums);
        polesValue = addTile(inflater, statsMid, R.string.poles);

        racesValue = addTile(inflater, statsBottom, R.string.gps_entered);
        fastestValue = addTile(inflater, statsBottom, R.string.fastest_laps);
    }

    private TextView addTile(LayoutInflater inflater, LinearLayout parent, int label) {
        View tile = inflater.inflate(R.layout.item_team_results_summary, parent, false);

        ((TextView) tile.findViewById(R.id.summary_label)).setText(label);

        TextView value = tile.findViewById(R.id.summary_value);
        value.setText(DASH);

        parent.addView(tile, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        return value;
    }

    private String text(DataSnapshot snapshot) {
        Object value = snapshot.getValue();

        if (value == null) {
            return DASH;
        }

        String result = String.valueOf(value).trim();

        return result.isEmpty() ? DASH : result;
    }

    private int parseSeason(String season) {
        try {
            return Integer.parseInt(season.trim());
        } catch (RuntimeException error) {
            return LocalDate.now().getYear();
        }
    }

    // ---------- team card (the look of the account page) ----------

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
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (!isAdded()) {
                            return;
                        }

                        if (!snapshot.exists()) {
                            // A past team without a record in constructors: palette colour, car by id and year.
                            applyTeamColor(pastTeamPalette.resolve(teamId, null));
                            loadTeamCar(teamId, lastSeason);
                            driversTeam_layout.setOnClickListener(null);
                            driversTeam_layout.setClickable(false);
                            arrow.setVisibility(View.GONE);
                            return;
                        }

                        String actualTeamId = snapshot.child("constructorId").getValue(String.class);
                        String teamColor = snapshot.child("color").getValue(String.class);

                        if (actualTeamId == null) {
                            applyDefaultTeamCard();
                            return;
                        }

                        applyTeamColor(teamColor);

                        int carSeason = lastSeason;

                        try {
                            Object enter = snapshot.child("enterYear").getValue();
                            int enterYear = Integer.parseInt(String.valueOf(enter).trim());
                            int current = Integer.parseInt(currentSeason);

                            if (enterYear > lastSeason) {
                                carSeason = current;
                            }
                        } catch (RuntimeException ignored) {
                            // no enterYear or currentSeason: lastSeason stays
                        }

                        loadTeamCar(actualTeamId, carSeason);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        if (!isAdded()) {
                            return;
                        }

                        Log.e("DriverStats", "Failed to load team", error.toException());

                        applyDefaultTeamCard();
                    }
                });
    }

    private void loadTeamCar(String teamId, int season) {
        String imagePath = "teams/" + teamId.toLowerCase() + "_" + season + ".png";

        StorageReference teamCarReference =
                FirebaseStorage.getInstance().getReference().child(imagePath);

        teamCarReference.getDownloadUrl()
                .addOnSuccessListener(uri -> {
                    if (!isAdded()) {
                        return;
                    }

                    GlideApp.with(requireContext())
                            .load(uri)
                            .transition(DrawableTransitionOptions.withCrossFade())
                            .diskCacheStrategy(DiskCacheStrategy.ALL)
                            .error(R.drawable.placeholder_car)
                            .into(teamCar_image);
                })
                .addOnFailureListener(error -> {
                    if (!isAdded()) {
                        return;
                    }

                    Log.w("DriverStats", "Team car not found: " + imagePath, error);

                    teamCar_image.setImageResource(R.drawable.placeholder_car);
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
            paintTeamCard(Color.parseColor(preparedColor));
        } catch (IllegalArgumentException error) {
            Log.w("DriverStats", "Invalid team color: " + colorValue, error);

            applyDefaultTeamCard();
        }
    }

    /** Outline in the team colour and a softly tinted photo block, as on the account page. */
    private void paintTeamCard(int color) {
        float density = getResources().getDisplayMetrics().density;

        GradientDrawable background = new GradientDrawable();
        background.setColor(ContextCompat.getColor(requireContext(), R.color.colorPrimary));
        background.setCornerRadius(16 * density);
        background.setStroke(Math.round(2 * density), color);

        driversTeam_layout.setBackground(background);

        int soft = ColorUtils.compositeColors(
                ColorUtils.setAlphaComponent(color, 31), Color.WHITE);

        ViewCompat.setBackgroundTintList(teamPhotoBlock, ColorStateList.valueOf(soft));

        arrow.setColorFilter(color);

        if (seasonsSection != null) {
            seasonsSection.setDefaultColor(color);
        }
    }

    private void applyDefaultTeamCard() {
        paintTeamCard(ContextCompat.getColor(requireContext(), R.color.grey));
        teamCar_image.setImageResource(R.drawable.placeholder_car);
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
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (!isAdded()) {
                            return;
                        }

                        ArrayList<String> teamDrivers = new ArrayList<>();

                        for (DataSnapshot driverSnapshot : snapshot.child("drivers").getChildren()) {
                            String teamDriverName = driverSnapshot.getKey();

                            if (teamDriverName != null) {
                                teamDrivers.add(teamDriverName);
                            }
                        }

                        Intent intent = new Intent(requireContext(), teamPageActivity.class);

                        Bundle bundle = new Bundle();

                        bundle.putString("teamName", driverTeam);
                        bundle.putString("teamId", teamId);
                        bundle.putStringArrayList("teamDrivers", teamDrivers);
                        bundle.putString("currentSeason", currentSeason);

                        intent.putExtras(bundle);

                        startActivity(intent);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        if (!isAdded()) {
                            return;
                        }

                        Log.e("DriverStats", "Failed to load team lineup", error.toException());
                    }
                });
    }

    // ---------- helpers ----------

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
            driverCountry.setText(countryName);
            flag.setVisibility(View.GONE);
            return;
        }

        Locale countryLocale = new Locale(Locale.getDefault().getLanguage(), countryCode);

        driverCountry.setText(countryLocale.getDisplayCountry());

        World.init(requireContext());
        flag.setImageResource(World.getFlagOf(countryCode));
        flag.setVisibility(View.VISIBLE);
    }

    private void setupBirthdate(String birthdateValue, String deathDateValueFromDb) {
        deathDateLayout.setVisibility(View.GONE);
        deathdate.setText("");

        if (birthdateValue == null || birthdateValue.trim().isEmpty()) {
            birthdate.setText(DASH);
            return;
        }

        String birthText = birthdateValue.trim();

        try {
            LocalDate born = LocalDate.parse(birthText, DateTimeFormatter.ISO_LOCAL_DATE);

            LocalDate today = LocalDate.now();
            LocalDate ageEndDate = today;

            if (born.isAfter(today)) {
                throw new IllegalArgumentException("Birthdate is in the future");
            }

            boolean hasDeathDate = deathDateValueFromDb != null
                    && !deathDateValueFromDb.trim().isEmpty();

            if (hasDeathDate) {
                LocalDate died = LocalDate.parse(
                        deathDateValueFromDb.trim(), DateTimeFormatter.ISO_LOCAL_DATE);

                if (died.isBefore(born) || died.isAfter(today)) {
                    throw new IllegalArgumentException("Invalid death date");
                }

                ageEndDate = died;

                deathdate.setText(died.format(DateTimeFormatter.ISO_LOCAL_DATE));
                deathDateLayout.setVisibility(View.VISIBLE);
            }

            int age = Period.between(born, ageEndDate).getYears();

            birthdate.setText(birthText + formatAge(age));

        } catch (java.time.format.DateTimeParseException | IllegalArgumentException error) {
            Log.e("DriverStats", "Invalid life dates: birth=" + birthdateValue
                    + ", death=" + deathDateValueFromDb, error);

            // With a wrong date no invented age is shown.
            birthdate.setText(birthText);
            deathDateLayout.setVisibility(View.GONE);
        }
    }

    private String formatAge(int age) {
        if (!Locale.getDefault().getLanguage().equals("ru")) {
            return " (" + age + " " + getString(R.string.age_text) + ")";
        }

        int lastDigit = age % 10;

        if (lastDigit == 1 && age % 100 != 11) {
            return " (" + age + " " + getString(R.string.age_text) + ")";
        }

        if (lastDigit >= 2 && lastDigit <= 4 && (age % 100 < 12 || age % 100 > 14)) {
            return " (" + age + " " + getString(R.string.age_text) + "a)";
        }

        return " (" + age + " " + getString(R.string.age_alt_text) + ")";
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
