package com.example.f1app;

import static com.example.f1app.MainActivity.getStringByName;
import static com.example.f1app.driverStatsFragment.getCountryCode;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
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

import java.util.Locale;

public class raceCircuitFragment extends Fragment {
    private TextView circuitName;
    private TextView length;
    private TextView lapsNum;
    private TextView firstGP;
    private TextView raceDist;
    private TextView lapRecord_time;
    private TextView lapRecord_driver;
    private RelativeLayout contentLayout;
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
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.race_circuit_fragment, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        TextView raceName = view.findViewById(R.id.raceName);
        circuitName = view.findViewById(R.id.circuitName);
        length = view.findViewById(R.id.length);
        lapsNum = view.findViewById(R.id.lapsNum);
        firstGP = view.findViewById(R.id.firstGP);
        raceDist = view.findViewById(R.id.raceDist);
        lapRecord_time = view.findViewById(R.id.lapRecord_time);
        lapRecord_driver = view.findViewById(R.id.lapRecord_driver);
        ImageView circuitImage = view.findViewById(R.id.circuitImage);
        RelativeLayout circuitInfoLayout = view.findViewById(R.id.circuitInfo_layout);
        ImageView flag = view.findViewById(R.id.flag);
        View resultsLine = view.findViewById(R.id.resultsLine);
        Button raceResults_2025 = view.findViewById(R.id.raceResults_2025);
        Button raceResults_2024 = view.findViewById(R.id.raceResults_2024);
        contentLayout = view.findViewById(R.id.content_layout);
        shimmerFrameLayout = view.findViewById(R.id.shimmer_layout);

        shimmerFrameLayout.startShimmer();

        if (!getArguments().isEmpty()){
            String mCircuitId = getArguments().getString("circuitId");
            String mRaceName = getArguments().getString("raceName");
            String mCountry = getArguments().getString("raceCountry");
            String mYear = getArguments().getString("gpYear");

            String localeRaceName = mRaceName.toLowerCase().replaceAll("\\s+", "_");
            String futureRaceName = requireContext().getString(getStringByName(localeRaceName + "_text")) + " " + mYear;
            raceName.setText(futureRaceName);

            ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) circuitInfoLayout.getLayoutParams();
            switch (mYear){
                case "2024":
                    raceResults_2024.setVisibility(View.GONE);
                    raceResults_2025.setVisibility(View.GONE);
                    resultsLine.setVisibility(View.GONE);
                    params.bottomMargin = (int) (20 * getResources().getDisplayMetrics().density);
                    circuitInfoLayout.setLayoutParams(params);
                    break;
                case "2025":
                    checkPrevSeason("2024", mRaceName, mCircuitId, mYear, raceResults_2024);
                    raceResults_2025.setVisibility(View.GONE);
                    resultsLine.setVisibility(View.GONE);
                    params.bottomMargin = (int) (40 * getResources().getDisplayMetrics().density);
                    circuitInfoLayout.setLayoutParams(params);
                    break;
                default:
                    checkPrevSeason("2024", mRaceName, mCircuitId, mYear, raceResults_2024);
                    checkPrevSeason("2025", mRaceName, mCircuitId, mYear, raceResults_2025);
                    break;
            }

            FirebaseStorage storage = FirebaseStorage.getInstance();
            StorageReference storageRef = storage.getReference();

            StorageReference mCircuitImage = storageRef.child("circuits/" + mCircuitId + ".png");

            GlideApp.with(requireContext())
                    .load(mCircuitImage)
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .error(R.drawable.placeholder)
                    .into(circuitImage);

            World.init(requireContext());
            flag.setImageResource(World.getFlagOf(getCountryCode(mCountry.toLowerCase())));

            DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference();
            rootRef.child("circuits/" + mCircuitId).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    String mCircuitId = snapshot.child("circuitId").getValue(String.class);
                    String mLength = snapshot.child("length").getValue(String.class);
                    String mLapsNum = snapshot.child("lapsCount").getValue(String.class);
                    String mFirstGP = snapshot.child("firstGPyear").getValue(String.class);
                    String mRaceDist = snapshot.child("raceDistance").getValue(String.class);
                    String mLapRecordTime = snapshot.child("lapRecordTime").getValue(String.class);
                    String mLapRecordDriver = snapshot.child("lapRecordDriver").getValue(String.class);
                    Integer mLapRecordYear = snapshot.child("lapRecordYear").getValue(Integer.class);

                    if (mLapRecordYear == 0){
                        lapRecord_driver.setVisibility(View.GONE);
                    }else{
                        String lapRecordDriverSummary = mLapRecordDriver + " (" + mLapRecordYear + ")";
                        lapRecord_driver.setText(lapRecordDriverSummary);
                    }

                    circuitName.setText(requireContext().getString(getStringByName(mCircuitId + "_text")));
                    length.setText(mLength);
                    lapsNum.setText(mLapsNum);
                    firstGP.setText(mFirstGP);
                    raceDist.setText(mRaceDist);
                    lapRecord_time.setText(mLapRecordTime);

                    shimmerFrameLayout.animate()
                            .setDuration(500)
                            .withEndAction(() -> {
                                contentLayout.setVisibility(View.VISIBLE);
                                shimmerFrameLayout.setVisibility(View.GONE);
                                shimmerFrameLayout.stopShimmer();
                            })
                            .start();
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e("futureActivityFirebaseError (Circuit Fragment)", error.getMessage());
                }
            });
        }

    }

    private void checkPrevSeason(String season, String mRaceName, String circuitId,
                                 String currentSeason, Button prevSeason){
        String raceName;
        switch (mRaceName){
            case "Brazilian Grand Prix":
                raceName = "São Paulo Grand Prix";
                break;
            case "Spanish Grand Prix":
                if (currentSeason.equals("2026")){
                    raceName = "Madrid Grand Prix";
                }else{
                    raceName = mRaceName;
                }
                break;
            case "Barcelona Grand Prix":
                if (currentSeason.equals("2026")){
                    raceName = "Spanish Grand Prix";
                }else{
                    raceName = mRaceName;
                }
                break;
            default:
                raceName = mRaceName;
                break;
        }

        DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference();
        rootRef.child("schedule/season/" + season).child(raceName).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.hasChildren()){
                    //Log.e("raceCircuit", " " + snapshot + " " + snapshot.exists());
                    //Log.e("raceCircuit", "Has Previous Year Race" + " " + snapshot.hasChildren());
                    String mPrevGPtext;
                    if(Locale.getDefault().getLanguage().equals("ru")){
                        mPrevGPtext = getString(R.string.prev_race_results_text) + " " + season;
                    }else{
                        mPrevGPtext = season + " " + getString(R.string.prev_race_results_text);
                    }
                    prevSeason.setText(mPrevGPtext);

                    prevSeason.setOnClickListener(view1 -> {
                        Intent intent = new Intent(requireContext() , raceResultsActivity.class);
                        Bundle bundle = new Bundle();
                        bundle.putString("raceName", raceName);
                        bundle.putString("circuitId", circuitId);
                        bundle.putString("season", season);
                        intent.putExtras(bundle);
                        requireContext().startActivity(intent);
                    });
                }else{
                    prevSeason.setVisibility(View.GONE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("raceResultsQualiAdapter: Fatal error in Firebase getting team color", " " + error.getMessage());
            }
        });
    }

}
