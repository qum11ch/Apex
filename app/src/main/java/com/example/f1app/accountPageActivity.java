package com.example.f1app;

import static com.example.f1app.MainActivity.checkConnection;
import static com.example.f1app.MainActivity.checkLightTheme;
import static com.example.f1app.driversStandingsActivity.startActivity_seasonData;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;

import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.appcompat.widget.Toolbar;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;

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
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;

public class accountPageActivity extends AppCompatActivity {
    private TextView username, userFavDriverNumber, userFavDriver, userFavTeam, fanText,
            teamName, driverName, driverFamilyName, tabUserName, noDriver, noTeam;
    private CoordinatorLayout main_content;
    private ProgressBar loadingProgress;
    private Dialog logoutDialog;
    private FirebaseAuth auth;
    private View line2;
    private RelativeLayout teamName_layout, driver_layout, team_layout, userFavTeam_layout,
            driverName_layout;
    private ImageView teamLogo, teamCar, driverImage, arrow;
    private String mCurrentSeason;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.account_page);

        if (!checkConnection(getApplicationContext())){
            startActivity(connectionLostScreen.createShowSplashOnNetworkFailure(accountPageActivity.this));
        }else{
            startActivity(connectionLostScreen.createIntentHideSplashOnNetworkRecovery(accountPageActivity.this));
        }

        Bundle intentBundle = getIntent().getExtras();
        mCurrentSeason = intentBundle.getString("currentSeason");

        Button logout = findViewById(R.id.logout);

        username = findViewById(R.id.username);
        userFavDriverNumber = findViewById(R.id.driverNumber);
        userFavDriver = findViewById(R.id.userFavDriver);
        userFavTeam = findViewById(R.id.userFavTeam);
        fanText = findViewById(R.id.fanText);
        tabUserName = findViewById(R.id.tabUserName);
        driverName = findViewById(R.id.driverName);
        driverFamilyName = findViewById(R.id.driverFamilyName);
        teamName = findViewById(R.id.teamName);
        userFavTeam_layout = findViewById(R.id.userFavTeam_layout);
        teamName_layout = findViewById(R.id.teamName_layout);
        driverName_layout = findViewById(R.id.driverName_layout);
        noDriver = findViewById(R.id.noDriver);
        noTeam = findViewById(R.id.noTeam);

        teamLogo = findViewById(R.id.team_logo);
        teamCar = findViewById(R.id.teamCar);
        driverImage = findViewById(R.id.driver_image);

        line2 = findViewById(R.id.line2);

        teamName_layout = findViewById(R.id.teamName_layout);
        team_layout = findViewById(R.id.team_layout);
        driver_layout = findViewById(R.id.driver_layout);

        arrow = findViewById(R.id.arrow);

        main_content = findViewById(R.id.main_content);
        loadingProgress = findViewById(R.id.loadingBar);

        auth = FirebaseAuth.getInstance();
        FirebaseUser user = auth.getCurrentUser();

        getUserInfo();

        WindowInsetsControllerCompat windowInsetsController =
                WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        windowInsetsController.setAppearanceLightStatusBars(false);

        logoutDialog = new Dialog(accountPageActivity.this);
        logoutDialog.setContentView(R.layout.logout_dialog_box);
        logoutDialog.getWindow().setLayout(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        logoutDialog.getWindow().setBackgroundDrawable(AppCompatResources.getDrawable(accountPageActivity.this, R.drawable.custom_dialog_bg));
        logoutDialog.setCancelable(false);

        Button cancelButton = logoutDialog.findViewById(R.id.cancel_button);
        Button confirmButton = logoutDialog.findViewById(R.id.confirm_button);

        cancelButton.setOnClickListener(view -> logoutDialog.dismiss());

        confirmButton.setOnClickListener(view -> {
            logoutDialog.dismiss();
            Toast.makeText(accountPageActivity.this, user.getEmail() + " " + getString(R.string.logout_text), Toast.LENGTH_SHORT).show();
            startActivity_seasonData(accountPageActivity.this, logInPageActivity.class, mCurrentSeason);
            //Intent i = new Intent(accountPageActivity.this, logInPageActivity.class);
            //startActivity(i);
            auth.signOut();
            finish();
        });

        logout.setOnClickListener(v -> logoutDialog.show());

        ImageButton backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> {
            Intent i = new Intent(accountPageActivity.this, MainActivity.class);
            i.setFlags(i.getFlags() | Intent.FLAG_ACTIVITY_NO_HISTORY);
            startActivity(i);
            finish();
        });

        getOnBackPressedDispatcher().addCallback(new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                Intent i = new Intent(accountPageActivity.this, MainActivity.class);
                i.setFlags(i.getFlags() | Intent.FLAG_ACTIVITY_NO_HISTORY);
                startActivity(i);
                finish();
            }
        });

        Button savedRace = findViewById(R.id.savedRace);
        savedRace.setOnClickListener(v -> {
            Intent intent = new Intent(accountPageActivity.this, savedRacesActivity.class);
            accountPageActivity.this.startActivity(intent);
        });

        Button settings = findViewById(R.id.profileSettings);
        settings.setOnClickListener(view -> {
            Intent intent = new Intent(accountPageActivity.this, accountSettingsActivity.class);
            accountPageActivity.this.startActivity(intent);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        main_content.setVisibility(View.GONE);
        loadingProgress.setVisibility(View.VISIBLE);

        GradientDrawable gd = new GradientDrawable();
        gd.setColor(ContextCompat.getColor(accountPageActivity.this, R.color.white));
        gd.setCornerRadii(new float[]{0, 0, 30, 30, 0, 0, 0, 0});
        gd.setStroke(12, ContextCompat.getColor(accountPageActivity.this, R.color.grey));
        driver_layout.setBackground(gd);

        GradientDrawable gd1 = new GradientDrawable();
        gd1.setColor(ContextCompat.getColor(accountPageActivity.this, R.color.white));
        gd1.setCornerRadii(new float[]{0, 0, 30, 30, 0, 0, 0, 0});
        gd1.setStroke(12, ContextCompat.getColor(accountPageActivity.this, R.color.grey));
        team_layout.setBackground(gd1);
        getUserInfo();
    }

    private void getUserInfo() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            showError(getString(R.string.dialog_login));
            return;
        }

        String userId = user.getUid();
        DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference();

        rootRef.child("users")
                .orderByChild("userId")
                .equalTo(userId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        for (DataSnapshot userSnapshot : snapshot.getChildren()) {
                            String username = userSnapshot.getKey();
                            String choiceDriver = userSnapshot.child("choiceDriver").getValue(String.class);
                            String choiceTeam = userSnapshot.child("choiceTeam").getValue(String.class);

                            showMainContent(username);
                            setupToolbar(username);

                            if (!isNullChoice(choiceDriver)) {
                                loadDriverInfo(choiceDriver);
                            } else {
                                showNoDriver();
                            }

                            if (!isNullChoice(choiceTeam)) {
                                loadTeamInfo(choiceTeam);
                            } else {
                                showNoTeam();
                            }

                            if (isNullChoice(choiceDriver) && isNullChoice(choiceTeam)) {
                                showNoFavorites();
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        handleFirebaseError(error, "Не удалось загрузить профиль");
                    }
                });
    }

    private boolean isNullChoice(String value) {
        return value == null || value.equals("null");
    }

    private void showMainContent(String usernameStr) {
        username.setText(usernameStr);
        main_content.setVisibility(View.VISIBLE);
        loadingProgress.setVisibility(View.GONE);
    }

    private void setupToolbar(String username) {
        CollapsingToolbarLayout collapsingToolbarLayout = findViewById(R.id.collapsing_toolbar);
        if (!checkLightTheme(accountPageActivity.this)){
            collapsingToolbarLayout.setBackground(ContextCompat.getDrawable(accountPageActivity.this, R.drawable.black_gradient));
        }
        AppBarLayout appBarLayout = findViewById(R.id.appbar);
        Toolbar toolbar = findViewById(R.id.toolbar);

        appBarLayout.addOnOffsetChangedListener(new AppBarLayout.OnOffsetChangedListener() {
            boolean isShow = true;
            int scrollRange = -1;

            @Override
            public void onOffsetChanged(AppBarLayout appBarLayout, int verticalOffset) {
                if (scrollRange == -1) {
                    scrollRange = appBarLayout.getTotalScrollRange();
                }
                if (scrollRange + verticalOffset == 0) {
                    tabUserName.setText(username);
                    if (!checkLightTheme(accountPageActivity.this)){
                        toolbar.setBackgroundColor(ContextCompat.getColor(getApplicationContext(),R.color.darkest_grey));
                    }else{
                        toolbar.setBackgroundColor(ContextCompat.getColor(getApplicationContext(),R.color.dark_blue));
                    }
                    isShow = true;
                } else if (isShow) {
                    tabUserName.setText(" ");
                    toolbar.setBackgroundColor(ContextCompat.getColor(
                            getApplicationContext(), android.R.color.transparent));
                    isShow = false;
                }
            }
        });
    }

    private void loadDriverInfo(String choiceDriver) {
        DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference();
        String[] driverFullname = choiceDriver.split(" ");
        String mDriverName, mDriverFamilyName;

        if (choiceDriver.equals("Andrea Kimi Antonelli")) {
            mDriverName = driverFullname[0] + " " + driverFullname[1];
            mDriverFamilyName = driverFullname[2];
        } else {
            mDriverName = driverFullname[0];
            mDriverFamilyName = driverFullname[1];
        }

        rootRef.child("drivers")
                .child(choiceDriver)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        try {
                            String driverNumber = snapshot.child("permanentNumber").getValue(String.class);
                            String driversCode = snapshot.child("driversCode").getValue(String.class);
                            String team = snapshot.child("driversTeam").getValue(String.class);

                            if (driverNumber == null || driversCode == null || team == null) {
                                showError("Неполные данные пилота");
                                return;
                            }

                            displayDriverImage(driversCode);
                            userFavDriverNumber.setText(driverNumber);
                            userFavDriver.setText(choiceDriver);
                            driverName.setText(mDriverName);
                            driverFamilyName.setText(mDriverFamilyName);

                            String finalTeam = team.equals("Sauber") ? "Kick Sauber" : team;
                            loadDriverTeam(finalTeam, mDriverName, mDriverFamilyName, driversCode);

                        } catch (Exception e) {
                            Log.e("DriverInfo", "Error parsing driver data", e);
                            showError("Ошибка загрузки данных пилота");
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        handleFirebaseError(error, "Не удалось загрузить данные пилота");
                    }
                });
    }

    private void displayDriverImage(String driversCode) {
        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference storageRef = storage.getReference();

        storageRef.child("drivers/" + driversCode.toLowerCase() + "_" + mCurrentSeason + ".png")
                .getDownloadUrl()
                .addOnSuccessListener(uri -> loadImageWithGlide(uri,driverImage, "driverImage"))
                .addOnFailureListener(exception ->
                        try2025DriverImage(storageRef, driversCode));
    }

    private void try2025DriverImage(StorageReference storageRef, String driversCode) {
        storageRef.child("drivers/" + driversCode.toLowerCase() + "_2025.png")
                .getDownloadUrl()
                .addOnSuccessListener(uri -> loadImageWithGlide(uri, driverImage, "driverImage"))
                .addOnFailureListener(exception ->
                        try2024DriverImage(storageRef, driversCode));
    }

    private void try2024DriverImage(StorageReference storageRef, String driversCode) {
        storageRef.child("drivers/" + driversCode.toLowerCase() + "_2024.png")
                .getDownloadUrl()
                .addOnSuccessListener(uri -> loadImageWithGlide(uri, driverImage, "driverImage"))
                .addOnFailureListener(exception ->
                        showError(getString(R.string.smth_wrong_text)));
    }

    private void loadImageWithGlide(Uri uri, ImageView imageView, String imageType) {
        String drawableName = "";
        switch(imageType){
            case "driverImage":
                drawableName = "placeholder_driver";
                break;
            case "carImage":
                drawableName = "placeholder_car";
                break;
            case "teamLogo":
                drawableName = "placeholder";
                break;
        }

        int resId = getResources().getIdentifier(
                drawableName,
                "drawable",
                getPackageName()
        );

        GlideApp.with(getApplicationContext())
                .load(uri)
                .transition(DrawableTransitionOptions.withCrossFade())
                .error(resId)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .into(imageView);

        imageView.setVisibility(View.VISIBLE);
    }

    private void loadDriverTeam(String team, String driverName, String driverFamilyName, String driversCode) {
        DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference();

        rootRef.child("constructors")
                .orderByChild("name")
                .equalTo(team)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (!snapshot.exists()) {
                            showError("Команда не найдена");
                            return;
                        }

                        for (DataSnapshot teamSnap : snapshot.getChildren()) {
                            setupDriverClickListener(teamSnap, team, driverName, driverFamilyName, driversCode);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        handleFirebaseError(error, "Ошибка загрузки команды");
                    }
                });
    }

    private void setupDriverClickListener(DataSnapshot teamSnap, String team,
                                 String driverName, String driverFamilyName, String driversCode) {
        try {
            String teamId = teamSnap.child("constructorId").getValue(String.class);
            String colorValue = teamSnap.child("color").getValue(String.class);

            if (teamId == null || colorValue == null) {
                Log.w("DriverData", "Missing team data");
                return;
            }

            String teamColor = "#" + colorValue;

            GradientDrawable gd = new GradientDrawable();
            gd.setColor(ContextCompat.getColor(accountPageActivity.this, R.color.white));
            gd.setCornerRadii(new float[]{0, 0, 30, 30, 0, 0, 0, 0});
            gd.setStroke(12, Color.parseColor(teamColor));
            driver_layout.setBackground(gd);

            driver_layout.setOnClickListener(view -> {
                Intent intent = new Intent(accountPageActivity.this, driverPageActivity.class);
                Bundle bundle = new Bundle();
                bundle.putString("driverName", driverName);
                bundle.putString("driverFamilyName", driverFamilyName);
                bundle.putString("driverTeam", team);
                bundle.putString("driverCode", driversCode);
                bundle.putString("driverTeamId", teamId);
                bundle.putString("currentSeason", mCurrentSeason);
                intent.putExtras(bundle);
                startActivity(intent);
            });

        } catch (IllegalArgumentException e) {
            Log.e("DriverLayout", "Invalid color format", e);
            showError("Ошибка отображения");
        }
    }

    private void loadTeamInfo(String choiceTeam) {
        DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference();
        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference storageRef = storage.getReference();

        rootRef.child("constructors")
                .orderByChild("name")
                .equalTo(choiceTeam)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (!snapshot.exists()) {
                            Log.w("TeamInfo", "Team not found: " + choiceTeam);
                            showError("Команда не найдена в БД");
                            return;
                        }

                        for (DataSnapshot teamSnap : snapshot.getChildren()) {
                            try {
                                String teamId = teamSnap.child("constructorId").getValue(String.class);
                                String colorValue = teamSnap.child("color").getValue(String.class);

                                if (teamId == null || colorValue == null) {
                                    Log.w("TeamInfo", "Missing data - teamId: " + teamId + ", color: " + colorValue);
                                    showError("Неполные данные команды");
                                    continue;
                                }

                                String teamColor = "#" + colorValue;
                                Log.d("TeamInfo", "Loading team: " + teamId + ", color: " + teamColor);

                                displayTeamLayout(teamColor);
                                loadTeamLogo(storageRef, teamId);
                                loadTeamCar(storageRef, teamId);
                                userFavTeam.setText(choiceTeam);
                                teamName.setText(choiceTeam);

                                setupTeamClickListener(choiceTeam, teamId);

                            } catch (Exception e) {
                                Log.e("TeamInfo", "Error parsing team data", e);
                                showError("Ошибка загрузки команды");
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        handleFirebaseError(error, "Не удалось загрузить команду");
                    }
                });
    }


    private void displayTeamLayout(String teamColor) {
        GradientDrawable gd = new GradientDrawable();
        gd.setColor(ContextCompat.getColor(this, R.color.white));
        gd.setCornerRadii(new float[]{0, 0, 30, 30, 0, 0, 0, 0});
        gd.setStroke(12, Color.parseColor(teamColor));
        team_layout.setBackground(gd);

        arrow.setColorFilter(Color.parseColor(teamColor));
    }

    private void loadTeamCar(StorageReference storageRef, String teamId) {
        Log.d("TeamCar", "Loading team car for: " + teamId);

        storageRef.child("teams/" + teamId + "_" + mCurrentSeason + ".png")
                .getDownloadUrl()
                .addOnSuccessListener(uri -> {
                    Log.d("TeamCar", "Success loading: " + uri.toString());
                    loadImageWithGlide(uri, teamCar, "carImage");
                })
                .addOnFailureListener(exception -> {
                    Log.e("TeamCar", "Failed to load car. Path: teams/" + teamId + ".png", exception);
                    showError("Не удалось загрузить машину");
                });
    }

    private void loadTeamLogo(StorageReference storageRef, String teamId) {
        Log.d("TeamLogo", "Loading logo for: " + teamId);

        StorageReference logoRef;

        if (teamId.equals("alpine") || teamId.equals("williams")) {
            logoRef = storageRef.child("teams/" + teamId + "_logo_alt.png");
        } else {
            logoRef = storageRef.child("teams/" + teamId + "_logo.png");
        }

        logoRef.getDownloadUrl()
                .addOnSuccessListener(uri -> {
                    Log.d("TeamLogo", "Success loading: " + uri.toString());
                    loadImageWithGlide(uri, teamLogo, "logoImage");
                })
                .addOnFailureListener(exception -> {
                    Log.e("TeamLogo", "Failed to load logo. StorageRef: " + logoRef.getPath(), exception);
                    showError(getString(R.string.smth_wrong_text));
                });
    }


    private void setupTeamClickListener(String choiceTeam, String teamId) {
        userFavTeam_layout.setOnClickListener(view -> {
            DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference();

            rootRef.child("driverLineUp/season/2025/" + teamId)
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            ArrayList<String> teamDrivers = new ArrayList<>();

                            for (DataSnapshot driverSnap : snapshot.child("drivers").getChildren()) {
                                String driverName = driverSnap.getKey();
                                if (driverName != null) {
                                    teamDrivers.add(driverName);
                                }
                            }

                            Intent intent = new Intent(accountPageActivity.this, teamPageActivity.class);
                            Bundle bundle = new Bundle();
                            bundle.putString("teamName", choiceTeam);
                            bundle.putString("teamId", teamId);
                            bundle.putStringArrayList("teamDrivers", teamDrivers);
                            bundle.putString("currentSeason", mCurrentSeason);
                            intent.putExtras(bundle);
                            startActivity(intent);
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            handleFirebaseError(error, "Не удалось загрузить состав команды");
                        }
                    });
        });
    }

    private void showNoDriver() {
        driverImage.setVisibility(View.INVISIBLE);
        noDriver.setVisibility(View.VISIBLE);
        collapseDriverLayout();
    }

    private void showNoTeam() {
        teamCar.setVisibility(View.INVISIBLE);
        noTeam.setVisibility(View.VISIBLE);
        collapseTeamLayout();
    }

    private void showNoFavorites() {
        fanText.setText(getString(R.string.no_fan_of_text));
        collapseDriverLayout();
        collapseTeamLayout();
    }

    private void collapseDriverLayout() {
        userFavDriverNumber.getLayoutParams().height = 0;
        userFavDriver.getLayoutParams().height = 0;
        line2.getLayoutParams().height = 0;

        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, 0);
        driverName_layout.setLayoutParams(layoutParams);
    }

    private void collapseTeamLayout() {
        userFavTeam.getLayoutParams().height = 0;
        teamLogo.getLayoutParams().height = 0;

        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, 0);
        teamName_layout.setLayoutParams(layoutParams);
    }

    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void handleFirebaseError(DatabaseError error, String defaultMessage) {
        String userMessage = defaultMessage;

        if (error.getCode() == DatabaseError.NETWORK_ERROR) {
            userMessage = getString(R.string.lost_connection_info);
        } else if (error.getCode() == DatabaseError.PERMISSION_DENIED) {
            userMessage = "Нет доступа к этим данным";
        }

        Toast.makeText(this, userMessage, Toast.LENGTH_LONG).show();
        Log.e("FirebaseDB", "Error code: " + error.getCode() + ", " + error.getMessage());
    }

}
