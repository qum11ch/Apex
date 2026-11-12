package com.example.f1app;

import static com.example.f1app.teamsStandingsActivity.localizeLocality;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.Fragment;

import com.blongho.country_data.World;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Locale;

public class teamStatsFragment extends Fragment {

    private TextView fullTeamName, teamBase, teamChief, techChief, chassis, powerUnit;
    private TextView enterYearValue, championshipsValue, winsValue, podiumsValue, polesValue;
    private ImageView flag;
    private LinearLayout statsTop, statsRow, lineupRow;
    private View lineupHeader, lineupCard;

    private final ArrayList<View> stripedViews = new ArrayList<>();
    private Integer teamColor;

    private String teamId;
    private String teamName;
    private String currentSeason;

    public teamStatsFragment() {
        // required empty public constructor.
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.team_page_stats_fragment, container, false);
    }

    @Override
    public void onResume() {
        super.onResume();

        if (getView() != null) {
            getView().requestLayout();
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        EdgeInsets.applyBottom(view.findViewById(R.id.scrollView));

        fullTeamName = view.findViewById(R.id.fullTeamName);
        teamBase = view.findViewById(R.id.teamBase);
        teamChief = view.findViewById(R.id.teamChief);
        techChief = view.findViewById(R.id.techChief);
        chassis = view.findViewById(R.id.chassis);
        powerUnit = view.findViewById(R.id.powerUnit);
        flag = view.findViewById(R.id.flag);

        statsTop = view.findViewById(R.id.stats_top);
        statsRow = view.findViewById(R.id.stats_row);
        lineupRow = view.findViewById(R.id.lineup_row);
        lineupHeader = view.findViewById(R.id.lineup_header);
        lineupCard = view.findViewById(R.id.lineup_card);

        buildStatsTiles();

        Bundle arguments = getArguments();

        if (arguments == null || arguments.isEmpty()) {
            Log.e("teamStats", "Error: Bundle from teamsAdapter is empty!");
            return;
        }

        teamId = arguments.getString("teamId");
        teamName = arguments.getString("teamName");
        currentSeason = arguments.getString("currentSeason");

        if (currentSeason == null || currentSeason.trim().isEmpty()) {
            currentSeason = String.valueOf(LocalDate.now().getYear());
        }

        if (teamId == null || teamId.trim().isEmpty()) {
            return;
        }

        DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference();

        loadTeam(rootRef);
        loadLineup(rootRef);
    }

    // ---------- statistics tiles ----------

    private void buildStatsTiles() {
        LayoutInflater inflater = LayoutInflater.from(requireContext());

        enterYearValue = addTile(inflater, statsTop, R.string.enter_year);
        championshipsValue = addTile(inflater, statsTop, R.string.constructors_championships);

        winsValue = addTile(inflater, statsRow, R.string.wins);
        podiumsValue = addTile(inflater, statsRow, R.string.podiums);
        polesValue = addTile(inflater, statsRow, R.string.poles);
    }

    private TextView addTile(LayoutInflater inflater, LinearLayout parent, int label) {
        View tile = inflater.inflate(R.layout.item_team_results_summary, parent, false);

        ((TextView) tile.findViewById(R.id.summary_label)).setText(label);

        TextView value = tile.findViewById(R.id.summary_value);
        value.setText("\u2013");

        parent.addView(tile, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        return value;
    }

    // ---------- team data ----------

    private void loadTeam(DatabaseReference rootRef) {
        rootRef.child("constructors").child(teamId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (!isAdded()) {
                            return;
                        }

                        enterYearValue.setText(text(snapshot.child("enterYear")));
                        winsValue.setText(text(snapshot.child("totalWins")));
                        podiumsValue.setText(text(snapshot.child("totalPodiums")));
                        polesValue.setText(text(snapshot.child("totalPoles")));
                        championshipsValue.setText(text(snapshot.child("totalChampionships")));

                        fullTeamName.setText(text(snapshot.child("fullTeamName")));
                        teamChief.setText(text(snapshot.child("teamChief")));
                        techChief.setText(text(snapshot.child("thechnicalChief")));
                        chassis.setText(text(snapshot.child("chassis")));
                        powerUnit.setText(text(snapshot.child("powerUnit")));

                        showBase(text(snapshot.child("base")));

                        String color = pastTeamPalette.resolve(
                                teamId, snapshot.child("color").getValue(String.class));

                        try {
                            teamColor = Color.parseColor(color);
                            applyTeamColor();
                        } catch (IllegalArgumentException ignored) {
                            // the stripes keep their default colour
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e("teamStats", "Constructor information getting error:"
                                + error.getMessage());
                    }
                });
    }

    private void showBase(String base) {
        if (base.equals("\u2013")) {
            teamBase.setText(base);
            flag.setVisibility(View.GONE);
            return;
        }

        String[] baseLocation = base.split(", ");

        if (baseLocation.length < 2) {
            teamBase.setText(base);
            flag.setVisibility(View.GONE);
            return;
        }

        String baseCountry = baseLocation[1];

        ArrayList<String> localizedData =
                localizeLocality(baseLocation[0], baseCountry, requireContext());

        teamBase.setText(localizedData.get(2));

        World.init(requireContext());
        flag.setImageResource(World.getFlagOf(getTeamCountryCode(baseCountry)));
        flag.setVisibility(View.VISIBLE);
    }

    private String text(DataSnapshot snapshot) {
        Object value = snapshot.getValue();

        if (value == null) {
            return "\u2013";
        }

        String result = String.valueOf(value).trim();

        return result.isEmpty() ? "\u2013" : result;
    }

    private void applyTeamColor() {
        if (teamColor == null) {
            return;
        }

        for (View striped : stripedViews) {
            ViewCompat.setBackgroundTintList(striped, ColorStateList.valueOf(teamColor));
        }
    }

    // ---------- line-up (any number of drivers) ----------

    private void loadLineup(DatabaseReference rootRef) {
        rootRef.child("driverLineUp").child("season").child(currentSeason).child(teamId)
                .child("drivers")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (!isAdded()) {
                            return;
                        }

                        ArrayList<String> drivers = new ArrayList<>();

                        for (DataSnapshot driver : snapshot.getChildren()) {
                            Object value = driver.getValue();

                            // a driver marked false has left the team
                            if (driver.getKey() != null && !Boolean.FALSE.equals(value)) {
                                drivers.add(driver.getKey());
                            }
                        }

                        if (drivers.isEmpty()) {
                            lineupHeader.setVisibility(View.GONE);
                            lineupCard.setVisibility(View.GONE);
                            return;
                        }

                        showLineup(rootRef, drivers);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e("teamStats", "Lineup error:" + error.getMessage());
                    }
                });
    }

    private void showLineup(DatabaseReference rootRef, ArrayList<String> drivers) {
        lineupRow.removeAllViews();
        stripedViews.clear();

        LayoutInflater inflater = LayoutInflater.from(requireContext());
        int gap = Math.round(6 * getResources().getDisplayMetrics().density);

        for (int index = 0; index < drivers.size(); index++) {
            String driverFullName = drivers.get(index);
            String[] names = driverNameFormatter.split(driverFullName);

            View column = inflater.inflate(R.layout.item_team_lineup_driver, lineupRow, false);

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            params.setMarginStart(index == 0 ? 0 : gap);
            params.setMarginEnd(index == drivers.size() - 1 ? 0 : gap);

            lineupRow.addView(column, params);

            View striped = column.findViewById(R.id.lineup_striped);
            ImageView photo = column.findViewById(R.id.lineup_photo);

            ((TextView) column.findViewById(R.id.lineup_given_name)).setText(names[0]);
            ((TextView) column.findViewById(R.id.lineup_family_name)).setText(names[1]);

            stripedViews.add(striped);

            column.setOnClickListener(v ->
                    openDriver(rootRef, driverFullName, names[0], names[1]));

            rootRef.child("drivers").child(driverFullName).child("driversCode")
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            String code = snapshot.getValue(String.class);

                            if (!isAdded() || code == null) {
                                return;
                            }

                            GlideApp.with(requireContext())
                                    .load(FirebaseStorage.getInstance().getReference().child(
                                            "drivers/" + code.toLowerCase() + "_"
                                                    + currentSeason + ".png"))
                                    .transition(DrawableTransitionOptions.withCrossFade())
                                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                                    .error(R.drawable.placeholder_driver)
                                    .into(photo);
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            Log.e("teamStats", "Drivers error:" + error.getMessage());
                        }
                    });
        }

        applyTeamColor();
    }

    private void openDriver(DatabaseReference rootRef, String driverFullName,
                            String driverName, String driverFamilyName) {

        rootRef.child("drivers").child(driverFullName)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (!isAdded()) {
                            return;
                        }

                        Intent intent = new Intent(requireContext(), driverPageActivity.class);

                        Bundle bundle = new Bundle();
                        bundle.putString("driverName", driverName);
                        bundle.putString("driverFamilyName", driverFamilyName);
                        bundle.putString("driverTeam", teamName);
                        bundle.putString("driverCode",
                                snapshot.child("driversCode").getValue(String.class));
                        bundle.putString("driverTeamId", teamId);
                        bundle.putString("currentSeason", currentSeason);

                        intent.putExtras(bundle);
                        startActivity(intent);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e("teamStats", "Drivers error:" + error.getMessage());
                    }
                });
    }

    private String getTeamCountryCode(String countryName) {
        String[] isoCountryCodes = Locale.getISOCountries();
        switch (countryName.toLowerCase(Locale.ROOT)) {
            case "united states":
                return "us";
            case "united kingdom":
                return "gb";
            case "uae":
                return "ae";
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
