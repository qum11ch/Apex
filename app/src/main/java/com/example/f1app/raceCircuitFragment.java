package com.example.f1app;

import static com.example.f1app.MainActivity.getStringByName;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.blongho.country_data.World;
import com.bumptech.glide.Glide;
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
    private TextView raceName, circuitName, length, lapsNum, firstGP, raceDist, lapRecord_time,
            lapRecord_driver, prevGPtext;
    private ImageView circuitImage, flag;
    private RelativeLayout previousGP, contentLayout;
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
        raceName = view.findViewById(R.id.raceName);
        circuitName = view.findViewById(R.id.circuitName);
        length = view.findViewById(R.id.length);
        lapsNum = view.findViewById(R.id.lapsNum);
        firstGP = view.findViewById(R.id.firstGP);
        raceDist = view.findViewById(R.id.raceDist);
        lapRecord_time = view.findViewById(R.id.lapRecord_time);
        lapRecord_driver = view.findViewById(R.id.lapRecord_driver);
        circuitImage = view.findViewById(R.id.circuitImage);
        flag = view.findViewById(R.id.flag);
        previousGP = view.findViewById(R.id.previousGP);
        prevGPtext = view.findViewById(R.id.prevGPtext);
        contentLayout = view.findViewById(R.id.content_layout);
        shimmerFrameLayout = view.findViewById(R.id.shimmer_layout);

        shimmerFrameLayout.startShimmer();

        if (!getArguments().isEmpty()){
            String mCircuitId = getArguments().getString("circuitId");
            String mRaceName = getArguments().getString("raceName");
            String mCountry = getArguments().getString("raceCountry");
            String mYear = getArguments().getString("gpYear");

            String mPrevGPtext = " ";
            if(Locale.getDefault().getLanguage().equals("ru")){
                mPrevGPtext = getString(R.string.prev_race_results_text) + " " + (Integer.parseInt(mYear) - 1);
            }else{
                mPrevGPtext = (Integer.parseInt(mYear) - 1) + " " + getString(R.string.prev_race_results_text);
            }

            prevGPtext.setText(mPrevGPtext);

            String localeRaceName = mRaceName.toLowerCase().replaceAll("\\s+", "_");
            String futureRaceName = requireContext().getString(getStringByName(localeRaceName + "_text")) + " " + mYear;
            raceName.setText(futureRaceName);

            previousGP.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intent = new Intent(requireContext() , raceResultsActivity.class);
                    Bundle bundle = new Bundle();
                    bundle.putString("raceName", mRaceName);
                    bundle.putString("circuitId", mCircuitId);
                    bundle.putString("season", String.valueOf(Integer.parseInt(mYear) - 1));
                    intent.putExtras(bundle);
                    requireContext().startActivity(intent);
                }
            });

            FirebaseStorage storage = FirebaseStorage.getInstance();
            StorageReference storageRef = storage.getReference();

            StorageReference mCircuitImage = storageRef.child("circuits/" + mCircuitId + ".png");

            GlideApp.with(requireContext())
                    .load(mCircuitImage)
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .error(R.drawable.f1)
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
                    String mLapRecordYear = snapshot.child("lapRecordYear").getValue(String.class);

                    circuitName.setText(requireContext().getString(getStringByName(mCircuitId + "_text")));
                    length.setText(mLength);
                    lapsNum.setText(mLapsNum);
                    firstGP.setText(mFirstGP);
                    raceDist.setText(mRaceDist);
                    lapRecord_time.setText(mLapRecordTime);
                    String lapRecordDriverSummary = mLapRecordDriver + " (" + mLapRecordYear + ")";
                    lapRecord_driver.setText(lapRecordDriverSummary);

                    Handler handler = new Handler();
                    handler.postDelayed(()->{
                        contentLayout.setVisibility(View.VISIBLE);
                        shimmerFrameLayout.setVisibility(View.GONE);
                        shimmerFrameLayout.stopShimmer();
                    },500);
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e("futureActivityFirebaseError (Circuit Fragment)", error.getMessage());
                }
            });
        }

    }

    public String getCountryCode(String countryName) {
        String[] isoCountryCodes = Locale.getISOCountries();
        switch (countryName) {
            case "usa":
                return "us";
            case "uk":
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
