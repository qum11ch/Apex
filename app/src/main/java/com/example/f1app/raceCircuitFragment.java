package com.example.f1app;

import static com.example.f1app.MainActivity.getStringByName;
import static com.example.f1app.driverStatsFragment.getCountryCode;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.blongho.country_data.World;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.facebook.shimmer.ShimmerFrameLayout;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

public class raceCircuitFragment extends Fragment {

    private static final String DASH = "\u2013";

    private TextView circuitName;
    private TextView lengthValue;
    private TextView lapsValue;
    private TextView turnsValue;
    private TextView distanceValue;
    private TextView firstGPValue;
    private TextView lapRecord_time;
    private TextView lapRecord_driver;
    private View contentLayout;
    private ShimmerFrameLayout shimmerFrameLayout;

    public raceCircuitFragment() {
        // required empty public constructor.
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.race_circuit_fragment, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        EdgeInsets.applyBottom(view.findViewById(R.id.scrollView));

        contentLayout = view.findViewById(R.id.content_layout);
        shimmerFrameLayout = view.findViewById(R.id.shimmer_layout);

        TextView raceName = contentLayout.findViewById(R.id.raceName);
        circuitName = contentLayout.findViewById(R.id.circuitName);
        lapRecord_time = contentLayout.findViewById(R.id.lapRecord_time);
        lapRecord_driver = contentLayout.findViewById(R.id.lapRecord_driver);
        ImageView circuitImage = contentLayout.findViewById(R.id.circuitImage);
        ImageView flag = contentLayout.findViewById(R.id.flag);
        Button raceResults = contentLayout.findViewById(R.id.raceResults_history);

        buildStatsTiles(
                contentLayout.findViewById(R.id.stats_top),
                contentLayout.findViewById(R.id.stats_bottom));

        shimmerFrameLayout.startShimmer();

        Bundle arguments = getArguments();

        if (arguments == null || arguments.isEmpty()) {
            return;
        }

        String mCircuitId = arguments.getString("circuitId");
        String mRaceName = arguments.getString("raceName");
        String mCountry = arguments.getString("raceCountry");
        String mYear = arguments.getString("gpYear");

        if (mCircuitId == null || mRaceName == null) {
            return;
        }

        String localeRaceName = mRaceName.toLowerCase().replaceAll("\\s+", "_");
        int raceNameId = getStringByName(localeRaceName + "_text");
        String title = raceNameId != 0 ? getString(raceNameId) : mRaceName;
        raceName.setText(mYear != null ? title + " " + mYear : title);

        setupResultsHistory(raceResults, mRaceName, mCircuitId, mYear);

        StorageReference mCircuitImage = FirebaseStorage.getInstance()
                .getReference()
                .child("circuits/" + mCircuitId + ".png");

        GlideApp.with(requireContext())
                .load(mCircuitImage)
                .transition(DrawableTransitionOptions.withCrossFade())
                .error(R.drawable.placeholder)
                .into(circuitImage);

        if (mCountry != null) {
            World.init(requireContext());
            flag.setImageResource(World.getFlagOf(getCountryCode(mCountry.toLowerCase())));
        }

        final java.util.List<DataSnapshot> seen = cachedList.recall("circuit|" + mCircuitId);

        if (seen != null && !seen.isEmpty()) {
            showCircuit(seen.get(0), mCircuitId, true); // seen before: no shimmer, refresh quietly
        }

        DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference();

        rootRef.child("circuits").child(mCircuitId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (!isAdded()) {
                            return;
                        }

                        showCircuit(snapshot, mCircuitId, false);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e("raceCircuitFragment", error.getMessage());
                    }
                });
    }

    // ---------- statistics tiles ----------

    private void buildStatsTiles(LinearLayout top, LinearLayout bottom) {
        LayoutInflater inflater = LayoutInflater.from(requireContext());

        lengthValue = addTile(inflater, top, R.string.circuit_length);
        lapsValue = addTile(inflater, top, R.string.no_of_laps);
        turnsValue = addTile(inflater, top, R.string.circuit_turns);

        distanceValue = addTile(inflater, bottom, R.string.race_distance);
        firstGPValue = addTile(inflater, bottom, R.string.first_grand_prix);
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

    private String withUnit(String value) {
        return value.equals(DASH) ? value : value + " " + getString(R.string.kilometres);
    }

    private void showCircuit(DataSnapshot snapshot, String circuitId, boolean instant) {
        cachedList.remember("circuit|" + circuitId, java.util.Collections.singletonList(snapshot));

        int nameId = getStringByName(circuitId + "_text");
        String fallbackName = snapshot.child("circuitName").getValue(String.class);

        circuitName.setText(nameId != 0
                ? getString(nameId)
                : (fallbackName != null ? fallbackName : ""));

        lengthValue.setText(withUnit(text(snapshot.child("length"))));
        lapsValue.setText(text(snapshot.child("lapsCount")));
        turnsValue.setText(text(snapshot.child("turnsCount")));
        distanceValue.setText(withUnit(text(snapshot.child("raceDistance"))));
        firstGPValue.setText(text(snapshot.child("firstGPyear")));

        lapRecord_time.setText(text(snapshot.child("lapRecordTime")));

        String recordDriver = snapshot.child("lapRecordDriver").getValue(String.class);
        Object recordYear = snapshot.child("lapRecordYear").getValue();

        boolean hasYear = recordYear != null
                && !"0".equals(String.valueOf(recordYear).trim());

        if (recordDriver == null || recordDriver.trim().isEmpty()) {
            lapRecord_driver.setText("");
        } else {
            lapRecord_driver.setText(hasYear
                    ? recordDriver + " (" + String.valueOf(recordYear).trim() + ")"
                    : recordDriver);
        }

        if (instant || contentLayout.getVisibility() == View.VISIBLE) {
            contentLayout.setVisibility(View.VISIBLE);
            shimmerFrameLayout.setVisibility(View.GONE);
            shimmerFrameLayout.stopShimmer();
            return;
        }

        shimmerFrameLayout.animate()
                .setDuration(500)
                .withEndAction(() -> {
                    if (!isAdded()) {
                        return;
                    }

                    contentLayout.setVisibility(View.VISIBLE);
                    shimmerFrameLayout.setVisibility(View.GONE);
                    shimmerFrameLayout.stopShimmer();
                })
                .start();
    }

    // ---------- previous races on this circuit ----------

    private void setupResultsHistory(Button button, String raceName, String circuitId, String gpYear) {
        button.setVisibility(View.GONE);

        raceHistory.load(raceName, gpYear, entries -> {
            if (!isAdded() || entries.isEmpty()) {
                return;
            }

            button.setText(getString(R.string.ps_history_button));
            button.setVisibility(View.VISIBLE);
            button.setOnClickListener(v ->
                    raceResultsSheet.show(getParentFragmentManager(), raceName, circuitId, entries));
        });
    }
}
