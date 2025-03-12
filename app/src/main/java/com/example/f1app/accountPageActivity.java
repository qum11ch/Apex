package com.example.f1app;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;

import androidx.annotation.NonNull;

import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class accountPageActivity extends AppCompatActivity {
    private TextView username, userFavDriverNumber, userFavDriver, userFavTeam, fanText,
            teamName, driverName, driverFamilyName, tabUserName, noDriver, noTeam;
    private Button logout, settings, savedRace;
    private CoordinatorLayout main_content;
    private ProgressBar loadingProgress;
    private Dialog logoutDialog;
    private FirebaseAuth auth;
    private View line, line2;
    private Button showDriverButton, showDriverStanding, showTeams, showHomePage, showAccount;
    private LinearLayout driverName_layout;
    private RelativeLayout teamName_layout, driver_layout, team_layout, userFavTeam_layout;
    private ImageButton backButton;
    private AppBarLayout appbar;
    private ImageView teamLogo, teamCar, driverImage;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.account_page);

        logout = (Button) findViewById(R.id.logout);

        username = (TextView) findViewById(R.id.username);
        userFavDriverNumber = (TextView) findViewById(R.id.driverNumber);
        userFavDriver = (TextView) findViewById(R.id.userFavDriver);
        userFavTeam = (TextView) findViewById(R.id.userFavTeam);
        fanText = (TextView) findViewById(R.id.fanText);
        tabUserName = findViewById(R.id.tabUserName);
        driverName = findViewById(R.id.driverName);
        driverFamilyName = findViewById(R.id.driverFamilyName);
        teamName = findViewById(R.id.teamName);
        userFavTeam_layout = findViewById(R.id.userFavTeam_layout);
        teamName_layout = findViewById(R.id.teamName_layout);
        driverName_layout = findViewById(R.id.driverName_layout);
        noDriver = findViewById(R.id.noDriver);
        noTeam = findViewById(R.id.noTeam);

        teamLogo = (ImageView) findViewById(R.id.team_logo);
        teamCar = (ImageView) findViewById(R.id.teamCar);
        driverImage = (ImageView) findViewById(R.id.driver_image);

        line2 = (View) findViewById(R.id.line2);
        line = (View) findViewById(R.id.line);

        appbar = findViewById(R.id.appbar);
        teamName_layout = findViewById(R.id.teamName_layout);
        driverName_layout = findViewById(R.id.driverName_layout);
        team_layout = findViewById(R.id.team_layout);
        driver_layout = findViewById(R.id.driver_layout);

        main_content = findViewById(R.id.main_content);
        loadingProgress = findViewById(R.id.loadingBar);

        auth = FirebaseAuth.getInstance();
        FirebaseUser user = auth.getCurrentUser();
        String userId = user.getUid();

        WindowInsetsControllerCompat windowInsetsController =
                WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        windowInsetsController.setAppearanceLightStatusBars(false);


        DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference();
        rootRef.child("users").orderByChild("userId").equalTo(userId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot userSnapshot: snapshot.getChildren()){
                    String choiceDriver = userSnapshot.child("choiceDriver").getValue(String.class);
                    String choiceTeam = userSnapshot.child("choiceTeam").getValue(String.class);
                    String mUsername = userSnapshot.getKey();
                    username.setText(mUsername);

                    main_content.setVisibility(View.VISIBLE);
                    loadingProgress.setVisibility(View.GONE);

                    CollapsingToolbarLayout collapsingToolbarLayout = (CollapsingToolbarLayout) findViewById(R.id.collapsing_toolbar);
                    AppBarLayout appBarLayout = (AppBarLayout) findViewById(R.id.appbar);
                    Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
                    appBarLayout.addOnOffsetChangedListener(new AppBarLayout.OnOffsetChangedListener() {
                        boolean isShow = true;
                        int scrollRange = -1;

                        @Override
                        public void onOffsetChanged(AppBarLayout appBarLayout, int verticalOffset) {
                            if (scrollRange == -1) {
                                scrollRange = appBarLayout.getTotalScrollRange();
                            }
                            if (scrollRange + verticalOffset == 0) {
                                tabUserName.setText(mUsername);
                                toolbar.setBackgroundColor(ContextCompat.getColor(getApplicationContext(),R.color.dark_blue));
                                isShow = true;
                            } else if (isShow) {
                                tabUserName.setText(" ");
                                toolbar.setBackgroundColor(ContextCompat.getColor(getApplicationContext(),android.R.color.transparent));
                                isShow = false;
                            }
                        }
                    });

                    if(!choiceDriver.equals("Nobody")){
                        driverImage.setVisibility(View.VISIBLE);
                        noDriver.setVisibility(View.INVISIBLE);
                        fanText.setText("Fan of");
                        String[] driverFullname = choiceDriver.split(" ");
                        String mDriverName, mDriverFamilyName;
                        if(choiceDriver.equals("Andrea Kimi Antonelli")){
                            mDriverName = driverFullname[0] + " " + driverFullname[1];
                            mDriverFamilyName = driverFullname[2];
                        }else{
                            mDriverName = driverFullname[0];
                            mDriverFamilyName = driverFullname[1];
                        }
                        rootRef.child("drivers").child(choiceDriver).addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                String driverNumber = snapshot.child("permanentNumber").getValue(String.class);
                                String driversCode = snapshot.child("driversCode").getValue(String.class);
                                String team = snapshot.child("driversTeam").getValue(String.class);
                                int resourceId_driver = getApplicationContext().getResources().getIdentifier(driversCode.toLowerCase(), "drawable",
                                        getApplicationContext().getPackageName());
                                Glide.with(getApplicationContext())
                                        .load(resourceId_driver)
                                        .transition(DrawableTransitionOptions.withCrossFade())
                                        .error(R.drawable.f1)
                                        .diskCacheStrategy(DiskCacheStrategy.NONE)
                                        .skipMemoryCache(true)
                                        .into(driverImage);
                                userFavDriverNumber.setText(driverNumber);
                                userFavDriver.setText(choiceDriver);
                                driverName.setText(mDriverName);
                                driverFamilyName.setText(mDriverFamilyName);

                                rootRef.child("constructors")
                                        .orderByChild("name")
                                        .equalTo(team)
                                        .addValueEventListener(new ValueEventListener() {
                                                                   @Override
                                                                   public void onDataChange(@NonNull DataSnapshot snapshot) {
                                                                       for (DataSnapshot teamSnap: snapshot.getChildren()){
                                                                           String teamId = teamSnap.child("constructorId").getValue(String.class);
                                                                           String teamColor = "#" + teamSnap.child("color").getValue(String.class);

                                                                           GradientDrawable gd = new GradientDrawable();
                                                                           gd.setColor(ContextCompat.getColor(accountPageActivity.this,R.color.white));
                                                                           gd.setCornerRadii(new float[] {0, 0, 30, 30, 0, 0, 0, 0});
                                                                           gd.setStroke(12, Color.parseColor(teamColor));
                                                                           driver_layout.setBackground(gd);
                                                                       }
                                                                   }

                                                                   @Override
                                                                   public void onCancelled(@NonNull DatabaseError error) {
                                                                       Log.e("accountPageActivity error while opening driver`s team page. ERROR: ", error.getMessage());
                                                                   }
                                                               });
                                String finalTeam = team;
                                driver_layout.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View view) {
                                        Intent intent = new Intent(accountPageActivity.this, driverPageActivity.class);
                                        Bundle bundle = new Bundle();
                                        bundle.putString("driverName", mDriverName);
                                        bundle.putString("driverFamilyName", mDriverFamilyName);
                                        bundle.putString("driverTeam", finalTeam);
                                        bundle.putString("driverCode", driversCode);
                                        intent.putExtras(bundle);
                                        accountPageActivity.this.startActivity(intent);
                                    }
                                });
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {
                                Log.e("accountPageActivity", "Error while getting driver`s info. Error:" + error.getMessage());
                            }
                        });
                    }else{
                        driverImage.setVisibility(View.INVISIBLE);
                        noDriver.setVisibility(View.VISIBLE);
                        int height = 0;
                        userFavDriverNumber.getLayoutParams().height = height;
                        userFavDriver.getLayoutParams().height = height;
                        line2.getLayoutParams().height = height;
                        RelativeLayout.LayoutParams layoutParams2 = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, 0);
                        driverName_layout.setLayoutParams(layoutParams2);
                        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                        int marginLeft = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 15, getResources().getDisplayMetrics());
                        int marginTop = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 5, getResources().getDisplayMetrics());
                        layoutParams.setMargins(marginLeft, marginTop,0,0);
                        layoutParams.addRule(RelativeLayout.BELOW, R.id.userFavDriver);
                        layoutParams.addRule(RelativeLayout.END_OF, R.id.team_logo);
                        userFavTeam.setLayoutParams(layoutParams);
                        int lineHeight = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 130, getResources().getDisplayMetrics());
                        line.getLayoutParams().height = lineHeight;
                    }

                    if(!choiceTeam.equals("Nobody")){
                        teamCar.setVisibility(View.VISIBLE);
                        noTeam.setVisibility(View.INVISIBLE);
                        fanText.setText("Fan of");
                        rootRef.child("constructors").orderByChild("name").equalTo(choiceTeam).addValueEventListener(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                for (DataSnapshot teamSnap: snapshot.getChildren()){
                                    String teamId = teamSnap.child("constructorId").getValue(String.class);
                                    String teamColor = "#" + teamSnap.child("color").getValue(String.class);

                                    GradientDrawable gd1 = new GradientDrawable();
                                    gd1.setColor(ContextCompat.getColor(accountPageActivity.this,R.color.white));
                                    gd1.setCornerRadii(new float[] {0, 0, 30, 30, 0, 0, 0, 0});
                                    gd1.setStroke(12, Color.parseColor(teamColor));
                                    team_layout.setBackground(gd1);

                                    int resourceId_teamLogo;
                                    if (teamId.equals("alpine")) {
                                        resourceId_teamLogo = getApplicationContext().getResources().getIdentifier(teamId + "_logo_alt", "drawable",
                                                getApplicationContext().getPackageName());
                                    } else if (teamId.equals("williams")) {
                                        resourceId_teamLogo = getApplicationContext().getResources().getIdentifier(teamId + "_logo_alt", "drawable",
                                                getApplicationContext().getPackageName());
                                    } else{
                                        resourceId_teamLogo = getApplicationContext().getResources().getIdentifier(teamId + "_logo", "drawable",
                                                getApplicationContext().getPackageName());
                                    }
                                    Glide.with(getApplicationContext())
                                            .load(resourceId_teamLogo)
                                            .diskCacheStrategy(DiskCacheStrategy.NONE)
                                            .skipMemoryCache(true)
                                            .transition(DrawableTransitionOptions.withCrossFade())
                                            .error(R.drawable.f1)
                                            .into(teamLogo);

                                    int resourceId_teamCar = getApplicationContext().getResources().getIdentifier(teamId, "drawable",
                                            getApplicationContext().getPackageName());
                                    Glide.with(getApplicationContext())
                                            .load(resourceId_teamCar)
                                            .diskCacheStrategy(DiskCacheStrategy.NONE)
                                            .skipMemoryCache(true)
                                            .transition(DrawableTransitionOptions.withCrossFade())
                                            .error(R.drawable.f1)
                                            .into(teamCar);
                                    userFavTeam.setText(choiceTeam);
                                    teamName.setText(choiceTeam);

                                    userFavTeam_layout.setOnClickListener(new View.OnClickListener() {
                                        @Override
                                        public void onClick(View view) {
                                            DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference();
                                            rootRef.child("driverLineUp/season/" + "2025" + "/" + teamId).addValueEventListener(new ValueEventListener() {
                                                @Override
                                                public void onDataChange(@NonNull DataSnapshot snapshot) {
                                                    ArrayList<String> teamDrivers = new ArrayList<>();
                                                    for (DataSnapshot driverDataSnapshot : snapshot.child("drivers").getChildren()) {
                                                        String driverName = driverDataSnapshot.getKey();
                                                        teamDrivers.add(driverName);
                                                    }
                                                    Intent intent = new Intent(accountPageActivity.this, teamPageActivity.class);
                                                    Bundle bundle = new Bundle();
                                                    bundle.putString("teamName", choiceTeam);
                                                    bundle.putString("teamId", teamId);
                                                    bundle.putStringArrayList("teamDrivers", teamDrivers);
                                                    intent.putExtras(bundle);
                                                    accountPageActivity.this.startActivity(intent);
                                                }
                                                @Override
                                                public void onCancelled(@NonNull DatabaseError error) {
                                                    Log.e("accountPageActivity error while opening driver`s team page. ERROR: ", error.getMessage());
                                                }
                                            });
                                        }
                                    });
                                }
                            }
                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {
                                Log.e("accountPageActivity", "Error while getting constructors. Error:" + error.getMessage());
                            }
                        });
                    }else{
                        teamCar.setVisibility(View.INVISIBLE);
                        noTeam.setVisibility(View.VISIBLE);
                        int height = 0;
                        userFavTeam.getLayoutParams().height = height;
                        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, 0);
                        teamName_layout.setLayoutParams(layoutParams);
                        teamLogo.getLayoutParams().height = height;
                        int lineHeight = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 120, getResources().getDisplayMetrics());
                        line.getLayoutParams().height = lineHeight;
                    }
                    if(choiceTeam.equals("Nobody")&&choiceDriver.equals("Nobody")){
                        teamCar.setVisibility(View.INVISIBLE);
                        noTeam.setVisibility(View.VISIBLE);
                        driverImage.setVisibility(View.INVISIBLE);
                        noDriver.setVisibility(View.VISIBLE);
                        RelativeLayout.LayoutParams layoutParams2 = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, 0);
                        driverName_layout.setLayoutParams(layoutParams2);
                        LinearLayout.LayoutParams layoutParams3 = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, 0);
                        teamName_layout.setLayoutParams(layoutParams3);
                        fanText.setText("Has no favourite driver and team");
                        int height = 0;
                        userFavTeam.getLayoutParams().height = height;
                        int lineWidth = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 3, getResources().getDisplayMetrics());
                        int lineHeight = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 80, getResources().getDisplayMetrics());
                        int marginTop = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 40, getResources().getDisplayMetrics());
                        int marginStartLine = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 30, getResources().getDisplayMetrics());
                        RelativeLayout.LayoutParams layoutParams = new RelativeLayout
                                .LayoutParams(lineWidth, lineHeight);
                        layoutParams.setMargins(marginStartLine, marginTop,0,0);
                        line.setLayoutParams(layoutParams);
                        RelativeLayout.LayoutParams Params = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                        int marginStartDriver = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 20, getResources().getDisplayMetrics());
                        Params.setMargins(marginStartDriver, marginTop,0,0);
                        Params.addRule(RelativeLayout.END_OF, R.id.line);
                        username.setLayoutParams(Params);
                        View grid = (View) findViewById(R.id.grid);
                        int gridHeight = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 70, getResources().getDisplayMetrics());
                        grid.getLayoutParams().height = gridHeight;
                        //RelativeLayout contentHeader = (RelativeLayout) findViewById(R.id.content_header_layout);
                        //int contentHeight = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 200, getResources().getDisplayMetrics());
                        //contentHeader.getLayoutParams().height = contentHeight;
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("accountPageActivity", "Error while getting user`s info. Error:" + error.getMessage());
            }
        });

        logoutDialog = new Dialog(accountPageActivity.this);
        logoutDialog.setContentView(R.layout.logout_dialog_box);
        logoutDialog.getWindow().setLayout(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        logoutDialog.getWindow().setBackgroundDrawable(getDrawable(R.drawable.custom_dialog_bg));
        logoutDialog.setCancelable(false);

        Button cancelButton = logoutDialog.findViewById(R.id.cancel_button);
        Button confirmButton = logoutDialog.findViewById(R.id.confirm_button);

        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                logoutDialog.dismiss();
            }
        });

        confirmButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                logoutDialog.dismiss();
                Toast.makeText(accountPageActivity.this, user.getEmail() + " Sign out!", Toast.LENGTH_SHORT).show();
                Intent i = new Intent(accountPageActivity.this, logInPageActivity.class);
                startActivity(i);
                auth.signOut();
                finish();
            }
        });

        logout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                logoutDialog.show();
            }
        });

        backButton = (ImageButton) findViewById(R.id.backButton);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        savedRace = (Button) findViewById(R.id.savedRace);
        savedRace.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //getText2(username);
                Intent intent = new Intent(accountPageActivity.this, savedRacesActivity.class);
                accountPageActivity.this.startActivity(intent);
            }
        });

        settings = (Button) findViewById(R.id.profileSettings);
        settings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(accountPageActivity.this, accountSettingsActivity.class);
                accountPageActivity.this.startActivity(intent);
            }
        });
    }
}
