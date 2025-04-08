package com.example.f1app;

import static com.example.f1app.teamsStandingsActivity.localizeLocality;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
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
import androidx.core.content.ContextCompat;
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
import com.google.firebase.storage.StorageReference;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Locale;

public class teamStatsFragment extends Fragment {
    private TextView enterYear, wins, podiums, poles, championships,
            firstDriverName, firstDriverFamilyName, secondDriverName, secondDriverFamilyName,
            teamBase, powerUnit, teamChief, techChief, chassis, fullTeamName, championshipsText;
    private ImageView firstDriver_image, secondDriver_image, flag;
    private RelativeLayout secondDriver_layout, firstDriver_layout, tech_layout;


    public teamStatsFragment() {
        // required empty public constructor.
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    public void onResume(){
        super.onResume();
        getView().requestLayout();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.team_page_stats_fragment, container, false);
    }


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        enterYear = view.findViewById(R.id.enterYear);
        wins = view.findViewById(R.id.wins);
        podiums = view.findViewById(R.id.podiums);
        poles = view.findViewById(R.id.poles);
        championships = view.findViewById(R.id.championships);
        firstDriverName = view.findViewById(R.id.firstDriverName);
        firstDriverFamilyName = view.findViewById(R.id.firstDriverFamilyName);
        secondDriverName = view.findViewById(R.id.secondDriverName);
        championshipsText = view.findViewById(R.id.championshipsText);
        secondDriverFamilyName = view.findViewById(R.id.secondDriverFamilyName);
        teamBase = view.findViewById(R.id.teamBase);
        teamChief = view.findViewById(R.id.teamChief);
        techChief = view.findViewById(R.id.techChief);
        chassis = view.findViewById(R.id.chassis);
        powerUnit = view.findViewById(R.id.powerUnit);
        firstDriver_image = view.findViewById(R.id.firstDriver_image);
        secondDriver_image = view.findViewById(R.id.secondDriver_image);
        firstDriver_layout = view.findViewById(R.id.firstDriver_layout);
        secondDriver_layout = view.findViewById(R.id.secondDriver_layout);
        tech_layout = view.findViewById(R.id.tech_layout);
        fullTeamName = view.findViewById(R.id.fullTeamName);
        flag = view.findViewById(R.id.flag);



        DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference();
        FirebaseStorage storage = FirebaseStorage.getInstance();

        if (!getArguments().isEmpty()){

            LocalDate currentDate = LocalDate.now();
            String currentYear = String.valueOf(currentDate.getYear());
            String mTeamId = getArguments().getString("teamId");
            String mTeamName = getArguments().getString("teamName");

            rootRef.child("driverLineUp/season/" + currentYear + "/" + mTeamId).addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    ArrayList<String> driversList = new ArrayList<>();
                    for (DataSnapshot driverDataSnapshot : snapshot.child("drivers").getChildren()) {
                        String driverFullname = driverDataSnapshot.getKey();
                        driversList.add(driverFullname);
                    }

                    firstDriver_layout.setOnClickListener(view2 -> {
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
                                bundle.putString("driverTeamId", mTeamId);
                                intent.putExtras(bundle);
                                requireContext().startActivity(intent);
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {
                                Log.e("teamPageActivity", "Drivers error:" + error.getMessage());
                            }
                        });
                    });

                    secondDriver_layout.setOnClickListener(view1 -> {
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
                                bundle.putString("driverTeamId", mTeamId);
                                intent.putExtras(bundle);
                                requireContext().startActivity(intent);
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {
                                Log.e("teamPageActivity", "Drivers error:" + error.getMessage());
                            }
                        });
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
                                StorageReference storageRef = storage.getReference();
                                StorageReference mDriverImage = storageRef.child("drivers/" + driversCode.toLowerCase().toLowerCase() + ".png");
                                if (finalI == 0){
                                    firstDriverName.setText(mDriverName);
                                    firstDriverFamilyName.setText(mDriverFamilyName);
                                    GlideApp.with(requireContext())
                                            .load(mDriverImage)
                                            .transition(DrawableTransitionOptions.withCrossFade())
                                            .diskCacheStrategy(DiskCacheStrategy.NONE)
                                            .transition(DrawableTransitionOptions.withCrossFade())
                                            .error(R.drawable.f1)
                                            .into(firstDriver_image);
                                }else{
                                    secondDriverName.setText(mDriverName);
                                    secondDriverFamilyName.setText(mDriverFamilyName);
                                    GlideApp.with(requireContext())
                                            .load(mDriverImage)
                                            .transition(DrawableTransitionOptions.withCrossFade())
                                            .diskCacheStrategy(DiskCacheStrategy.NONE)
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
                }
                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e("teamStandingsError", error.getMessage());
                }
            });

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
                    String mFullTeamName = snapshot.child("fullTeamName").getValue(String.class);


                    enterYear.setText(String.valueOf(mEnterYear));
                    wins.setText(mWins);
                    podiums.setText(mPodiums);
                    poles.setText(mPoles);
                    championships.setText(mChampionships);

                    String mChampionshipsText;

                    if(Locale.getDefault().getLanguage().equals("ru")){
                        assert mChampionships != null;
                        int champCount = Integer.parseInt(mChampionships);
                        if (champCount % 10 < 5 && champCount % 10!= 0){
                            mChampionshipsText = getString(R.string.times) + "a";
                        }else{
                            mChampionshipsText = getString(R.string.times);
                        }
                        championshipsText.setText(mChampionshipsText);
                    }

                    teamChief.setText(mTeamChief);
                    techChief.setText(mTechChief);
                    powerUnit.setText(mPowerUnit);
                    chassis.setText(mChassis);
                    fullTeamName.setText(mFullTeamName);
                    assert mTeamBase != null;
                    String[] baseLocation = mTeamBase.split(", ");
                    String baseCountry = baseLocation[1];

                    ArrayList<String> localizedData = localizeLocality(baseLocation[0], baseCountry, requireContext());
                    //String country = localizedData.get(0);
                    //String cityName = localizedData.get(1);
                    String locale = localizedData.get(2);
                    teamBase.setText(locale);

                    World.init(requireContext());
                    flag.setImageResource(World.getFlagOf(getTeamCountryCode(baseCountry)));

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


        }else{
            Log.e("teamPageActivity", "Error: Bundle from teamsAdapter is empty!");
        }

    }

    private String getTeamCountryCode(String countryName) {
        String[] isoCountryCodes = Locale.getISOCountries();
        switch (countryName) {
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