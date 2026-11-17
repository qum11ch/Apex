package com.example.f1app;

import static com.example.f1app.MainActivity.getStringByName;

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
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;

import com.blongho.country_data.World;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class driverStatsFragment extends Fragment {

    private static final String DASH = "\u2013";
    private driverTeamSeasonsSection teamSeasonsSection;

    private TextView firstGP, lastEntry, driverCountry, birthdate, deathdate;
    private TextView championshipsValue, pointsValue, winsValue, podiumsValue, polesValue,
            racesValue, fastestValue;
    private ImageView flag;
    private View deathDateLayout;
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

        if (teamSeasonsSection != null) {
            teamSeasonsSection.stop();
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

        statsTop = view.findViewById(R.id.stats_top);
        statsMid = view.findViewById(R.id.stats_mid);
        statsBottom = view.findViewById(R.id.stats_bottom);

        buildStatsTiles();

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

                        Integer debutSeason = extractYear(mFirstGP);
                        int firstSeasonOfDriver = debutSeason != null ? debutSeason : lastSeason;

                        seasonsSection = new driverSeasonsSection(
                                driverStatsFragment.this,
                                rootView,
                                driverFullName,
                                mDriverCode,
                                snapshot.child("driverId").getValue(String.class),
                                mDriverName,
                                mDriverFamilyName,
                                firstSeasonOfDriver,
                                lastSeason);

                        seasonsSection.start();

                        teamSeasonsSection = new driverTeamSeasonsSection(
                                driverStatsFragment.this,
                                rootView,
                                mDriverCode,
                                mDriverName,
                                mDriverFamilyName,
                                firstSeasonOfDriver,
                                lastSeason,
                                mCurrentSeason);

                        teamSeasonsSection.start();
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
