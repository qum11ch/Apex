package com.example.f1app;

import android.app.Application;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class teamStatsFragment extends Fragment {
    private TextView enterYear, wins, podiums, poles, championships,
            firstDriverName, firstDriverFamilyName, secondDriverName, secondDriverFamilyName,
            teamBase, powerUnit, teamChief, techChief, chassis;
    private ImageView firstDriver_image, secondDriver_image;
    private RelativeLayout secondDriver_layout, firstDriver_layout, tech_layout;


    public teamStatsFragment() {
        // required empty public constructor.
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.team_page_stats_fragment, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        enterYear = (TextView) view.findViewById(R.id.enterYear);
        wins = (TextView) view.findViewById(R.id.wins);
        podiums = (TextView) view.findViewById(R.id.podiums);
        poles = (TextView) view.findViewById(R.id.poles);
        championships = (TextView) view.findViewById(R.id.championships);
        firstDriverName = (TextView) view.findViewById(R.id.firstDriverName);
        firstDriverFamilyName = (TextView) view.findViewById(R.id.firstDriverFamilyName);
        secondDriverName = (TextView) view.findViewById(R.id.secondDriverName);
        secondDriverFamilyName = (TextView) view.findViewById(R.id.secondDriverFamilyName);
        teamBase = (TextView) view.findViewById(R.id.teamBase);
        teamChief = (TextView) view.findViewById(R.id.teamChief);
        techChief = (TextView) view.findViewById(R.id.techChief);
        chassis = (TextView) view.findViewById(R.id.chassis);
        powerUnit = (TextView) view.findViewById(R.id.powerUnit);
        firstDriver_image = (ImageView) view.findViewById(R.id.firstDriver_image);
        secondDriver_image = (ImageView) view.findViewById(R.id.secondDriver_image);
        firstDriver_layout = (RelativeLayout) view.findViewById(R.id.firstDriver_layout);
        secondDriver_layout = (RelativeLayout) view.findViewById(R.id.secondDriver_layout);
        tech_layout = (RelativeLayout) view.findViewById(R.id.tech_layout);



        DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference();
        if (!getArguments().isEmpty()){
            String mTeamId = getArguments().getString("teamId");
            String mTeamName = getArguments().getString("teamName");
            ArrayList<String> driversList = getArguments().getStringArrayList("teamDrivers");

            firstDriver_layout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    String[] driverFullname = driversList.get(0).split(" ");
                    String mDriverName, mDriverFamilyName;
                    if(driversList.get(0).equals("Andrea Kimi Antonelli")){
                        mDriverName = driverFullname[0] + " " + driverFullname[1];
                        mDriverFamilyName = driverFullname[2];
                    }else{
                        mDriverName = driverFullname[0];
                        mDriverFamilyName = driverFullname[1];
                    }
                    rootRef.child("drivers").child(driversList.get(0)).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            String mDriverCode = snapshot.child("driversCode").getValue(String.class);
                            Intent intent = new Intent(requireContext(), driverPageActivity.class);
                            Bundle bundle = new Bundle();
                            bundle.putString("driverName", mDriverName);
                            bundle.putString("driverFamilyName", mDriverFamilyName);
                            bundle.putString("driverTeam", mTeamName);
                            bundle.putString("driverCode", mDriverCode);
                            intent.putExtras(bundle);
                            requireContext().startActivity(intent);
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            Log.e("teamPageActivity", "Drivers error:" + error.getMessage());
                        }
                    });
                }
            });

            secondDriver_layout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    String[] driverFullname = driversList.get(1).split(" ");
                    String mDriverName, mDriverFamilyName;
                    if(driversList.get(1).equals("Andrea Kimi Antonelli")){
                        mDriverName = driverFullname[0] + " " + driverFullname[1];
                        mDriverFamilyName = driverFullname[2];
                    }else{
                        mDriverName = driverFullname[0];
                        mDriverFamilyName = driverFullname[1];
                    }
                    rootRef.child("drivers").child(driversList.get(1)).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            String mDriverCode = snapshot.child("driversCode").getValue(String.class);
                            Intent intent = new Intent(requireContext(), driverPageActivity.class);
                            Bundle bundle = new Bundle();
                            bundle.putString("driverName", mDriverName);
                            bundle.putString("driverFamilyName", mDriverFamilyName);
                            bundle.putString("driverTeam", mTeamName);
                            bundle.putString("driverCode", mDriverCode);
                            intent.putExtras(bundle);
                            requireContext().startActivity(intent);
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            Log.e("teamPageActivity", "Drivers error:" + error.getMessage());
                        }
                    });
                }
            });

            int resourceId_teamLogo;
            rootRef.child("constructors").child(mTeamId).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    Integer mEnterYear = snapshot.child("enterYear").getValue(Integer.class);
                    String mWins = snapshot.child("totalWins").getValue(String.class);
                    String mPodiums = snapshot.child("totalPodiums").getValue(String.class);
                    String mPoles = snapshot.child("totalPoles").getValue(String.class);
                    String mChampionships = snapshot.child("totalChampionships").getValue(String.class);
                    String mColor = "#" + snapshot.child("color").getValue(String.class);
                    String mTeamBase = snapshot.child("base").getValue(String.class);
                    String mTeamChief = snapshot.child("teamChief").getValue(String.class);
                    String mTechChief = snapshot.child("thechnicalChief").getValue(String.class);
                    String mPowerUnit = snapshot.child("powerUnit").getValue(String.class);
                    String mChassis = snapshot.child("chassis").getValue(String.class);


                    enterYear.setText(String.valueOf(mEnterYear));
                    wins.setText(mWins);
                    podiums.setText(mPodiums);
                    poles.setText(mPoles);
                    championships.setText(mChampionships);
                    teamBase.setText(mTeamBase);
                    teamChief.setText(mTeamChief);
                    techChief.setText(mTechChief);
                    powerUnit.setText(mPowerUnit);
                    chassis.setText(mChassis);

                    GradientDrawable gd = new GradientDrawable();
                    gd.setColor(ContextCompat.getColor(requireContext(),R.color.white));
                    gd.setCornerRadii(new float[] {0, 0, 30, 30, 0, 0, 0, 0});
                    gd.setStroke(12, Color.parseColor(mColor));
                    secondDriver_layout.setBackground(gd);
                    firstDriver_layout.setBackground(gd);

                    GradientDrawable gd1 = new GradientDrawable();
                    gd1.setColor(ContextCompat.getColor(requireContext(),R.color.white));
                    gd1.setCornerRadii(new float[] {0, 0, 30, 30, 0, 0, 0, 0});
                    gd1.setStroke(12, Color.parseColor(mColor));
                    tech_layout.setBackground(gd1);
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e("teamPageActivity", "Constructor information getting error:" + error.getMessage());
                }
            });

            for (int i = 0; i < driversList.size(); i++){
                String[] driverFullname = driversList.get(i).split(" ");
                String mDriverName, mDriverFamilyName;
                if(driversList.get(i).equals("Andrea Kimi Antonelli")){
                    mDriverName = driverFullname[0] + " " + driverFullname[1];
                    mDriverFamilyName = driverFullname[2];
                }else{
                    mDriverName = driverFullname[0];
                    mDriverFamilyName = driverFullname[1];
                }
                int finalI = i;
                rootRef.child("drivers").child(driversList.get(i)).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        String driversCode = snapshot.child("driversCode").getValue(String.class);
                        int resourceId_driverImage = requireContext().getResources().getIdentifier(driversCode.toLowerCase(), "drawable",
                                requireContext().getPackageName());
                        if (finalI == 0){
                            firstDriverName.setText(mDriverName);
                            firstDriverFamilyName.setText(mDriverFamilyName);
                            Glide.with(requireContext())
                                    .load(resourceId_driverImage)
                                    .transition(DrawableTransitionOptions.withCrossFade())
                                    .error(R.drawable.f1)
                                    .into(firstDriver_image);
                        }else{
                            secondDriverName.setText(mDriverName);
                            secondDriverFamilyName.setText(mDriverFamilyName);
                            Glide.with(requireContext())
                                    .load(resourceId_driverImage)
                                    .transition(DrawableTransitionOptions.withCrossFade())
                                    .error(R.drawable.f1)
                                    .into(secondDriver_image);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e("teamPageActivity", "Drivers error:" + error.getMessage());
                    }
                });
            }


        }else{
            Log.e("teamPageActivity", "Error: Bundle from teamsAdapter is empty!");
        }

    }
}