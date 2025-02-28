package com.example.f1app;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;

import androidx.annotation.NonNull;

import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class accountPageActivity extends AppCompatActivity {
    private TextView username, userFavDriverNumber, userFavDriver, userFavTeam, fanText;
    private Button logout, settings, savedRace;
    Dialog logoutDialog;
    FirebaseAuth auth;
    View line, line2;
    Button showDriverButton, showDriverStanding, showTeams, showHomePage, showAccount;
    private ImageButton backButton;
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

        teamLogo = (ImageView) findViewById(R.id.team_logo);
        teamCar = (ImageView) findViewById(R.id.team_car);
        driverImage = (ImageView) findViewById(R.id.driver_image);

        line2 = (View) findViewById(R.id.line2);
        line = (View) findViewById(R.id.line);

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

                    if(!choiceDriver.equals("Nobody")){
                        fanText.setText("Fan of");
                        rootRef.child("drivers").child(choiceDriver).addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                String driverNumber = snapshot.child("permanentNumber").getValue(String.class);
                                String driversCode = snapshot.child("driversCode").getValue(String.class);
                                int resourceId_driver = getApplicationContext().getResources().getIdentifier(driversCode.toLowerCase(), "drawable",
                                        getApplicationContext().getPackageName());
                                Glide.with(getApplicationContext())
                                        .load(resourceId_driver)
                                        .transition(DrawableTransitionOptions.withCrossFade())
                                        .error(R.drawable.f1)
                                        .into(driverImage);
                                userFavDriverNumber.setText(driverNumber);
                                userFavDriver.setText(choiceDriver);
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {
                                Log.e("accountPageActivity", "Error while getting driver`s info. Error:" + error.getMessage());
                            }
                        });
                    }else{
                        int height = 0;
                        userFavDriverNumber.getLayoutParams().height = height;
                        userFavDriver.getLayoutParams().height = height;
                        line2.getLayoutParams().height = height;
                        driverImage.getLayoutParams().height = height;
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
                        fanText.setText("Fan of");
                        rootRef.child("constructors").orderByChild("name").equalTo(choiceTeam).addValueEventListener(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                for (DataSnapshot teamSnap: snapshot.getChildren()){
                                    String teamId = teamSnap.child("constructorId").getValue(String.class);
                                    String teamColor = "#" + teamSnap.child("color").getValue(String.class);

                                    username.setTextColor(Color.parseColor(teamColor));
                                    userFavTeam.setTextColor(Color.parseColor(teamColor));

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
                                            .transition(DrawableTransitionOptions.withCrossFade())
                                            .error(R.drawable.f1)
                                            .into(teamLogo);

                                    int resourceId_teamCar = getApplicationContext().getResources().getIdentifier(teamId, "drawable",
                                            getApplicationContext().getPackageName());
                                    Glide.with(getApplicationContext())
                                            .load(resourceId_teamCar)
                                            .transition(DrawableTransitionOptions.withCrossFade())
                                            .error(R.drawable.f1)
                                            .into(teamCar);
                                    userFavTeam.setText(choiceTeam);
                                }
                            }
                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {
                                Log.e("accountPageActivity", "Error while getting constructors. Error:" + error.getMessage());
                            }
                        });
                    }else{
                        int height = 0;
                        userFavTeam.getLayoutParams().height = height;
                        teamCar.getLayoutParams().height = height;
                        teamLogo.getLayoutParams().height = height;
                        int lineHeight = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 120, getResources().getDisplayMetrics());
                        line.getLayoutParams().height = lineHeight;
                    }
                    if(choiceTeam.equals("Nobody")&&choiceDriver.equals("Nobody")){
                        fanText.setText("Has no favourite driver and team");
                        int height = 0;
                        userFavTeam.getLayoutParams().height = height;
                        teamCar.getLayoutParams().height = height;
                        teamLogo.getLayoutParams().height = height;
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
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("accountPageActivity", "Error while getting user`s info. Error:" + error.getMessage());
            }
        });

        logoutDialog = new Dialog(accountPageActivity.this);
        logoutDialog.setContentView(R.layout.signout_dialog_box);
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


        showDriverButton = (Button) findViewById(R.id.showDriver);
        showDriverButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(accountPageActivity.this, driversStandingsActivity.class);
                accountPageActivity.this.startActivity(intent);
            }
        });

        showDriverStanding = (Button) findViewById(R.id.showStandingsDriver);
        showDriverStanding.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(accountPageActivity.this, schuduleActivity.class);
                accountPageActivity.this.startActivity(intent);
            }
        });


        showHomePage = (Button) findViewById(R.id.showHomePage);
        showHomePage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(accountPageActivity.this, MainActivity.class);
                accountPageActivity.this.startActivity(intent);
            }
        });

        showTeams = (Button) findViewById(R.id.showTeams);
        showTeams.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(accountPageActivity.this, teamsStandingsActivity.class);
                accountPageActivity.this.startActivity(intent);
            }
        });

        showAccount = (Button) findViewById(R.id.showAccount);
        showAccount.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(accountPageActivity.this, logInPageActivity.class);
                accountPageActivity.this.startActivity(intent);
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
