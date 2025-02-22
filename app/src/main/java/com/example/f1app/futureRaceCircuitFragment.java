package com.example.f1app;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.blongho.country_data.World;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Locale;

public class futureRaceCircuitFragment extends Fragment {
    private TextView raceName, circuitName, length, lapsNum, firstGP, raceDist, lapRecord_time,
            lapRecord_driver;
    private ImageView circuitImage, flag;


    public futureRaceCircuitFragment() {
        // required empty public constructor.
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.future_race_circuit_fragment, container, false);
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

        String mCircuitId = getArguments().getString("circuitId");
        String mRaceName = getArguments().getString("raceName");
        String mCountry = getArguments().getString("raceCountry");
        String mYear = getArguments().getString("gpYear");

        String fullRaceName = mRaceName + " " + mYear;
        raceName.setText(fullRaceName);


        int resourceId_driverTeam = this.getActivity().getResources().getIdentifier(mCircuitId, "drawable",
                this.getActivity().getPackageName());

        Glide.with(this.getActivity())
                .load(resourceId_driverTeam)
                .transition(DrawableTransitionOptions.withCrossFade())
                .error(R.drawable.f1)
                .into(circuitImage);

        World.init(this.getActivity());
        flag.setImageResource(World.getFlagOf(getCountryCode(mCountry.toLowerCase())));

        DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference();
        rootRef.child("circuits/" + mCircuitId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String mCircuitName = snapshot.child("circuitName").getValue(String.class);
                String mLength = snapshot.child("length").getValue(String.class);
                String mLapsNum = snapshot.child("lapsCount").getValue(String.class);
                String mFirstGP = snapshot.child("firstGPyear").getValue(String.class);
                String mRaceDist = snapshot.child("raceDistance").getValue(String.class);
                String mLapRecordTime = snapshot.child("lapRecordTime").getValue(String.class);
                String mLapRecordDriver = snapshot.child("lapRecordDriver").getValue(String.class);
                String mLapRecordYear = snapshot.child("lapRecordYear").getValue(String.class);

                circuitName.setText(mCircuitName);
                length.setText(mLength);
                lapsNum.setText(mLapsNum);
                firstGP.setText(mFirstGP);
                raceDist.setText(mRaceDist);
                lapRecord_time.setText(mLapRecordTime);
                String lapRecordDriverSummary = mLapRecordDriver + " (" + mLapRecordYear + ")";
                lapRecord_driver.setText(lapRecordDriverSummary);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("futureActivityFirebaseError (Circuit Fragment)", error.getMessage());
            }
        });

    }
    public String getCountryCode(String countryName) {
        String[] isoCountryCodes = Locale.getISOCountries();
        if(countryName.equals("usa")){
            return "us";
        } else if (countryName.equals("uk")) {
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
