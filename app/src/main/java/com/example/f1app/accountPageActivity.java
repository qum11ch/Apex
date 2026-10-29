package com.example.f1app;

import static com.example.f1app.MainActivity.checkConnection;
import static com.example.f1app.MainActivity.checkLightTheme;
import static com.example.f1app.driversStandingsActivity.startActivity_seasonData;
import static com.example.f1app.userPredictionAdapter.formatPoints;

import android.app.Dialog;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
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
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.ColorUtils;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import android.view.View;
import android.view.ViewGroup;

import androidx.activity.EdgeToEdge;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class accountPageActivity extends AppCompatActivity {

    private TextView username, createdAt, teamName, driverName, driverFamilyName,
            tabUserName, noDriver, noTeam, avatarInitial, pointsEarned,
            predictionCount, racesSaved;

    private CoordinatorLayout main_content;
    private ProgressBar loadingProgress;
    private Dialog logoutDialog;

    private FirebaseAuth auth;

    private RelativeLayout driver_layout;
    private RelativeLayout team_layout;

    private ImageView teamCar, driverImage, avatarImage;

    private RelativeLayout teamPhoto, driverPhoto;

    private String mCurrentSeason;
    private String currentDriverChoice, currentTeamChoice;

    private RelativeLayout header;

    private String currentSeason;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.account_page);

        setupAccountInsets();

        if (!checkConnection(this)) {
            startActivity(connectionLostScreen
                    .createShowSplashOnNetworkFailure(accountPageActivity.this));
        } else {
            startActivity(connectionLostScreen
                    .createIntentHideSplashOnNetworkRecovery(accountPageActivity.this));
        }

        Bundle intentBundle = getIntent().getExtras();

        mCurrentSeason = intentBundle != null
                ? intentBundle.getString("currentSeason")
                : null;

        currentSeason = mCurrentSeason;

        Button logout = findViewById(R.id.logout);

        avatarInitial = findViewById(R.id.avatarInitial);
        avatarImage = findViewById(R.id.avatarImage);

        username = findViewById(R.id.username);
        createdAt = findViewById(R.id.createdAt);

        driverName = findViewById(R.id.driverName);
        driverFamilyName = findViewById(R.id.driverFamilyName);
        teamName = findViewById(R.id.teamName);

        noDriver = findViewById(R.id.noDriver);
        noTeam = findViewById(R.id.noTeam);

        driver_layout = findViewById(R.id.driver_layout);
        team_layout = findViewById(R.id.team_layout);

        teamCar = findViewById(R.id.teamCar);
        driverImage = findViewById(R.id.driver_image);

        main_content = findViewById(R.id.main_content);
        loadingProgress = findViewById(R.id.loadingBar);

        loadingProgress.setVisibility(View.VISIBLE);

        pointsEarned = findViewById(R.id.points_earned);
        predictionCount = findViewById(R.id.prediction_count);
        racesSaved = findViewById(R.id.races_saved);

        teamPhoto = findViewById(R.id.teamPhoto);
        driverPhoto = findViewById(R.id.driverPhoto);

        header = findViewById(R.id.header);

        Button openUsersPredictionsButton = findViewById(R.id.userPredictions);

        openUsersPredictionsButton.setOnClickListener(v -> {
            Intent intent = new Intent(
                    accountPageActivity.this,
                    userPredictionsActivity.class
            );

            startActivity(intent);
        });

        ImageButton editTeam = findViewById(R.id.editTeam);
        ImageButton editDriver = findViewById(R.id.editDriver);

        editTeam.setOnClickListener(v -> pickTeam());
        editDriver.setOnClickListener(v -> pickDriver());

        auth = FirebaseAuth.getInstance();

        WindowInsetsControllerCompat windowInsetsController =
                WindowCompat.getInsetsController(
                        getWindow(),
                        getWindow().getDecorView()
                );

        windowInsetsController.setAppearanceLightStatusBars(false);

        View scroll = findViewById(R.id.nestedScrollView);

        final int basePad =
                (int) (16 * getResources().getDisplayMetrics().density);

        ViewCompat.setOnApplyWindowInsetsListener(scroll, (v, insets) -> {
            Insets nav = insets.getInsets(
                    WindowInsetsCompat.Type.navigationBars()
            );

            v.setPadding(
                    v.getPaddingLeft(),
                    v.getPaddingTop(),
                    v.getPaddingRight(),
                    basePad + nav.bottom
            );

            return insets;
        });

        logoutDialog = new Dialog(accountPageActivity.this);
        logoutDialog.setContentView(R.layout.dialog_logout);

        Objects.requireNonNull(logoutDialog.getWindow()).setLayout(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );

        logoutDialog.getWindow().setBackgroundDrawable(
                AppCompatResources.getDrawable(
                        accountPageActivity.this,
                        R.drawable.custom_dialog_bg
                )
        );

        logoutDialog.setCancelable(false);

        Button cancelButton =
                logoutDialog.findViewById(R.id.cancel_button);

        Button confirmButton =
                logoutDialog.findViewById(R.id.confirm_button);

        cancelButton.setOnClickListener(view -> logoutDialog.dismiss());

        confirmButton.setOnClickListener(view -> {
            logoutDialog.dismiss();

            FirebaseUser currentUser = auth.getCurrentUser();

            if (currentUser != null) {
                Toast.makeText(
                        accountPageActivity.this,
                        currentUser.getEmail()
                                + " "
                                + getString(R.string.logout_text),
                        Toast.LENGTH_SHORT
                ).show();
            }

            auth.signOut();

            startActivity_getSeason(logInPageActivity.class);

            finish();
        });

        logout.setOnClickListener(v -> logoutDialog.show());

        ImageButton backButton = findViewById(R.id.backButton);

        backButton.setOnClickListener(v -> closeAccountPage());

        getOnBackPressedDispatcher().addCallback(
                this,
                new OnBackPressedCallback(true) {
                    @Override
                    public void handleOnBackPressed() {
                        closeAccountPage();
                    }
                }
        );

        Button savedRace = findViewById(R.id.savedRace);

        savedRace.setOnClickListener(v -> {
            Intent intent = new Intent(
                    accountPageActivity.this,
                    savedRacesActivity.class
            );

            intent.putExtra("currentSeason", mCurrentSeason);

            startActivity(intent);
        });

        ImageButton settings = findViewById(R.id.settings_button);

        settings.setOnClickListener(view -> {
            Intent intent = new Intent(
                    accountPageActivity.this,
                    accountSettingsActivity.class
            );

            intent.putExtra("currentSeason", mCurrentSeason);

            startActivity(intent);
        });
    }

    private void setupAccountInsets() {
        View root = findViewById(R.id.account_root);
        View headerContainer = findViewById(R.id.account_header_container);
        View header = findViewById(R.id.header);

        final int rootLeft = root.getPaddingLeft();
        final int rootTop = root.getPaddingTop();
        final int rootRight = root.getPaddingRight();
        final int rootBottom = root.getPaddingBottom();

        final int headerLeft = header.getPaddingLeft();
        final int headerTop = header.getPaddingTop();
        final int headerRight = header.getPaddingRight();
        final int headerBottom = header.getPaddingBottom();

        final int baseContainerHeight =
                headerContainer.getLayoutParams().height;

        final int baseHeaderHeight =
                header.getLayoutParams().height;

        ViewCompat.setOnApplyWindowInsetsListener(root, (view, windowInsets) -> {
            Insets safeInsets = windowInsets.getInsets(
                    WindowInsetsCompat.Type.systemBars()
                            | WindowInsetsCompat.Type.displayCutout()
            );

            view.setPadding(
                    rootLeft + safeInsets.left,
                    rootTop,
                    rootRight + safeInsets.right,
                    rootBottom + safeInsets.bottom
            );

            header.setPadding(
                    headerLeft,
                    headerTop + safeInsets.top,
                    headerRight,
                    headerBottom
            );

            ViewGroup.LayoutParams containerParams =
                    headerContainer.getLayoutParams();

            int containerHeight = baseContainerHeight + safeInsets.top;

            if (containerParams.height != containerHeight) {
                containerParams.height = containerHeight;
                headerContainer.setLayoutParams(containerParams);
            }

            ViewGroup.LayoutParams headerParams = header.getLayoutParams();

            int headerHeight = baseHeaderHeight + safeInsets.top;

            if (headerParams.height != headerHeight) {
                headerParams.height = headerHeight;
                header.setLayoutParams(headerParams);
            }

            return WindowInsetsCompat.CONSUMED;
        });

        ViewCompat.requestApplyInsets(root);
    }

    private void startActivity_getSeason(Class<?> target) {
        String season = currentSeason;

        if (season == null || season.trim().isEmpty()) {
            season = "2026";
        }

        Intent intent = new Intent(this, target);
        intent.putExtra("currentSeason", season);
        startActivity(intent);
        overridePendingTransition(0, 0);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (auth == null) {
            return;
        }

        main_content.setVisibility(View.GONE);
        loadingProgress.setVisibility(View.VISIBLE);

        int grey = ContextCompat.getColor(this, R.color.grey);

        driver_layout.setBackground(cardBg(grey, true));
        team_layout.setBackground(cardBg(grey, true));

        getUserInfo();
    }

    private GradientDrawable cardBg(int strokeColor, boolean dashed) {
        float d = getResources().getDisplayMetrics().density;

        GradientDrawable gd = new GradientDrawable();
        gd.setColor(ContextCompat.getColor(this, R.color.colorPrimary));
        gd.setCornerRadius(16 * d);

        if (dashed) {
            gd.setStroke((int) (2 * d), strokeColor, 8 * d, 4 * d);
        } else {
            gd.setStroke((int) (2 * d), strokeColor);
        }

        return gd;
    }

    private void getUserInfo() {
        FirebaseUser user = auth.getCurrentUser();

        if (user == null) {
            startActivity_getSeason(logInPageActivity.class);
            finish();
            return;
        }

        String uid = user.getUid();

        loadAvatarIfExists(user);

        if (user.getMetadata() != null) {
            long createdAtLong =
                    user.getMetadata().getCreationTimestamp();

            SimpleDateFormat formatter = new SimpleDateFormat(
                    "dd.MM.yyyy",
                    Locale.getDefault()
            );

            String createdAtText = getString(R.string.created_at_text)
                    + " "
                    + formatter.format(
                    new java.util.Date(createdAtLong)
            );
            createdAt.setText(createdAtText);
        }

        FirebaseDatabase.getInstance()
                .getReference()
                .child("users")
                .child(uid)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (isFinishing() || isDestroyed()) {
                            return;
                        }

                        FirebaseUser currentUser = auth.getCurrentUser();

                        if (currentUser == null
                                || !uid.equals(currentUser.getUid())) {
                            return;
                        }

                        if (!snapshot.exists()) {
                            loadingProgress.setVisibility(View.GONE);
                            openUsernameMigration();
                            return;
                        }

                        boolean migrationComplete =
                                Boolean.TRUE.equals(
                                        snapshot.child(
                                                "usernameMigrationComplete"
                                        ).getValue(Boolean.class)
                                );

                        String publicUsername =
                                snapshot.child("username")
                                        .getValue(String.class);

                        boolean hasUsername =
                                publicUsername != null
                                        && !publicUsername.trim().isEmpty();

                        if (!migrationComplete || !hasUsername) {
                            loadingProgress.setVisibility(View.GONE);
                            openUsernameMigration();
                            return;
                        }

                        String backgroundColor = snapshot.child("background_color")
                                .getValue(String.class);

                        if (backgroundColor != null
                                && backgroundColor.matches("^#[0-9A-Fa-f]{6}$")) {

                            header.setBackgroundColor(Color.parseColor(backgroundColor));
                        }else{
                            header.setBackgroundColor(getColor(R.color.account_settings_header_color));
                        }

                        String choiceDriver =
                                snapshot.child("choiceDriver")
                                        .getValue(String.class);

                        String choiceTeam =
                                snapshot.child("choiceTeam")
                                        .getValue(String.class);

                        currentDriverChoice =
                                isNullChoice(choiceDriver)
                                        ? null
                                        : choiceDriver;

                        currentTeamChoice =
                                isNullChoice(choiceTeam)
                                        ? null
                                        : choiceTeam;

                        showMainContent(publicUsername);
                        // setupToolbar(publicUsername);

                        setUserStats(uid);

                        if (currentDriverChoice == null) {
                            showNoDriver();
                        } else {
                            resetDriverCard();
                            loadDriverInfo(currentDriverChoice);
                        }

                        if (currentTeamChoice == null) {
                            showNoTeam();
                        } else {
                            resetTeamCard();
                            loadTeamInfo(currentTeamChoice);
                        }
                    }

                    @Override
                    public void onCancelled(
                            @NonNull DatabaseError error) {

                        if (isFinishing() || isDestroyed()) {
                            return;
                        }

                        loadingProgress.setVisibility(View.GONE);

                        handleFirebaseError(
                                error,
                                getString(R.string.user_account_load)
                        );
                    }
                });
    }

    private void openUsernameMigration() {
        if (isFinishing() || isDestroyed()) return;
        startActivity_getSeason(logInPageActivity.class);

        finish();
    }

    private void setUserStats(String uid) {
        DatabaseReference rootRef =
                FirebaseDatabase.getInstance().getReference();

        rootRef.child("userPredictionStats")
                .child(uid)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(
                            @NonNull DataSnapshot snapshot) {

                        if (isFinishing() || isDestroyed()) {
                            return;
                        }

                        if (!snapshot.exists()) {
                            setEmptyStats();
                            return;
                        }

                        Long predictionsCount =
                                snapshot.child("predictionsCount")
                                        .getValue(Long.class);

                        Double totalPoints =
                                snapshot.child("totalPoints")
                                        .getValue(Double.class);

                        predictionCount.setText(
                                String.valueOf(
                                        predictionsCount == null
                                                ? 0L
                                                : predictionsCount
                                )
                        );

                        pointsEarned.setText(
                                formatPoints(
                                        totalPoints == null
                                                ? 0.0
                                                : totalPoints
                                )
                        );
                    }

                    @Override
                    public void onCancelled(
                            @NonNull DatabaseError error) {

                        if (isFinishing() || isDestroyed()) {
                            return;
                        }

                        setEmptyStats();

                        handleFirebaseError(
                                error,
                                getString(R.string.user_account_load)
                        );
                    }
                });

        rootRef.child("savedRaces")
                .child(uid)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(
                            @NonNull DataSnapshot snapshot) {

                        if (isFinishing() || isDestroyed()) {
                            return;
                        }

                        racesSaved.setText(
                                String.valueOf(snapshot.getChildrenCount())
                        );
                    }

                    @Override
                    public void onCancelled(
                            @NonNull DatabaseError error) {

                        if (isFinishing() || isDestroyed()) {
                            return;
                        }

                        racesSaved.setText("0");

                        handleFirebaseError(
                                error,
                                getString(R.string.user_account_load)
                        );
                    }
                });
    }

    private void setEmptyStats() {
        pointsEarned.setText("0");
        predictionCount.setText("0");
    }

    private boolean isNullChoice(String value) {
        return value == null || value.equals("null");
    }

    private void showMainContent(String usernameStr) {
        username.setText(usernameStr);

        if (usernameStr != null && !usernameStr.isEmpty()) {
            avatarInitial.setText(
                    usernameStr.substring(0, 1).toUpperCase()
            );
        }

        main_content.setVisibility(View.VISIBLE);
        loadingProgress.setVisibility(View.GONE);
    }

    // private void setupToolbar(String username) {
    //     CollapsingToolbarLayout collapsingToolbarLayout =
    //             findViewById(R.id.collapsing_toolbar);
//
    //     if (!checkLightTheme(accountPageActivity.this)) {
    //         collapsingToolbarLayout.setBackground(
    //                 ContextCompat.getDrawable(
    //                         accountPageActivity.this,
    //                         R.drawable.black_gradient
    //                 )
    //         );
    //     }
//
    //     AppBarLayout appBarLayout = findViewById(R.id.appbar);
    //     Toolbar toolbar = findViewById(R.id.toolbar);
//
    //     appBarLayout.addOnOffsetChangedListener(
    //             new AppBarLayout.OnOffsetChangedListener() {
//
    //                 boolean isShow = true;
    //                 int scrollRange = -1;
//
    //                 @Override
    //                 public void onOffsetChanged(
    //                         AppBarLayout appBarLayout,
    //                         int verticalOffset) {
//
    //                     if (scrollRange == -1) {
    //                         scrollRange =
    //                                 appBarLayout.getTotalScrollRange();
    //                     }
//
    //                     if (scrollRange + verticalOffset == 0) {
    //                         tabUserName.setText(username);
//
    //                         if (!checkLightTheme(
    //                                 accountPageActivity.this)) {
//
    //                             toolbar.setBackgroundColor(
    //                                     ContextCompat.getColor(
    //                                             getApplicationContext(),
    //                                             R.color.darkest_grey
    //                                     )
    //                             );
    //                         } else {
    //                             toolbar.setBackgroundColor(
    //                                     ContextCompat.getColor(
    //                                             getApplicationContext(),
    //                                             R.color.dark_blue
    //                                     )
    //                             );
    //                         }
//
    //                         isShow = true;
    //                     } else if (isShow) {
    //                         tabUserName.setText(" ");
//
    //                         toolbar.setBackgroundColor(
    //                                 ContextCompat.getColor(
    //                                         getApplicationContext(),
    //                                         android.R.color.transparent
    //                                 )
    //                         );
//
    //                         isShow = false;
    //                     }
    //                 }
    //             });
    // }

    private void loadDriverInfo(String choiceDriver) {
        DatabaseReference rootRef =
                FirebaseDatabase.getInstance().getReference();

        String[] driverFullname = choiceDriver.split(" ");

        String mDriverName;
        String mDriverFamilyName;

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
                    public void onDataChange(
                            @NonNull DataSnapshot snapshot) {

                        if (isFinishing() || isDestroyed()) {
                            return;
                        }

                        try {
                            String driversCode =
                                    snapshot.child("driversCode")
                                            .getValue(String.class);

                            String team =
                                    snapshot.child("driversTeam")
                                            .getValue(String.class);

                            String driverId = snapshot.child("driverId")
                                    .getValue(String.class);

                            if (driversCode == null
                                    || team == null) {

                                showError("Неполные данные пилота");
                                return;
                            }

                            displayDriverImage(
                                    driversCode,
                                    choiceDriver,
                                    driverId
                            );

                            driverName.setText(mDriverName);
                            driverFamilyName.setText(mDriverFamilyName);
                            driverFamilyName.setVisibility(View.VISIBLE);

                            String finalTeam =
                                    team.equals("Sauber")
                                            ? "Kick Sauber"
                                            : team;

                            loadDriverTeam(
                                    finalTeam,
                                    mDriverName,
                                    mDriverFamilyName,
                                    driversCode
                            );
                        } catch (Exception e) {
                            Log.e(
                                    "DriverInfo",
                                    "Error parsing driver data",
                                    e
                            );

                            showError(
                                    getString(R.string.user_driver_error)
                            );
                        }
                    }

                    @Override
                    public void onCancelled(
                            @NonNull DatabaseError error) {

                        if (isFinishing() || isDestroyed()) {
                            return;
                        }

                        handleFirebaseError(
                                error,
                                getString(R.string.user_driver_error)
                        );
                    }
                });
    }

    private void displayDriverImage(
            String driversCode,
            String driverFullName,
            String driverId) {

        if (driversCode == null
                || driversCode.trim().isEmpty()
                || driverFullName == null
                || driverFullName.trim().isEmpty()) {

            showDriverImageError();
            return;
        }

        FirebaseDatabase.getInstance()
                .getReference()
                .child("drivers")
                .child(driverFullName)
                .child("lastEntry")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (isFinishing() || isDestroyed()) {
                            return;
                        }

                        String lastEntry = snapshot.getValue(String.class);
                        String year = extractYear(lastEntry);

                        if (year == null) {
                            Log.w(
                                    "DriverImage",
                                    "Invalid lastEntry: " + lastEntry
                            );

                            showDriverImageError();
                            return;
                        }

                        loadDriverImage(
                                driverId,
                                driversCode,
                                year
                        );
                    }

                    @Override
                    public void onCancelled(
                            @NonNull DatabaseError error) {

                        if (isFinishing() || isDestroyed()) {
                            return;
                        }

                        Log.e(
                                "DriverImage",
                                "Failed to read lastEntry",
                                error.toException()
                        );

                        showDriverImageError();
                    }
                });
    }

    private String extractYear(String lastEntry) {
        if (lastEntry == null || lastEntry.trim().isEmpty()) {
            return null;
        }

        java.util.regex.Matcher matcher = java.util.regex.Pattern
                .compile("\\b(\\d{4})\\b")
                .matcher(lastEntry);

        if (!matcher.find()) {
            return null;
        }

        return matcher.group(1);
    }

    private void loadDriverImage(
            String driversCode,
            String year) {

        FirebaseStorage storage = FirebaseStorage.getInstance();

        StorageReference imageRef = storage.getReference()
                .child("drivers")
                .child(driversCode.toLowerCase() + "_" + year + ".png");

        imageRef.getDownloadUrl()
                .addOnSuccessListener(uri -> {
                    if (isFinishing() || isDestroyed()) {
                        return;
                    }

                    loadImageWithGlide(
                            uri,
                            driverImage,
                            "driverImage"
                    );
                })
                .addOnFailureListener(error -> {
                    if (isFinishing() || isDestroyed()) {
                        return;
                    }

                    Log.w(
                            "DriverImage",
                            "Image not found for "
                                    + driversCode
                                    + " in "
                                    + year,
                            error
                    );

                    showDriverImageError();
                });
    }

    private void loadDriverImage(
            String driverId,
            String driverCode,
            String year) {

        if (driverId != null && !driverId.trim().isEmpty()) {
            String preferredPath =
                    "drivers/" + driverId.trim() + "_" + year + ".png";

            FirebaseStorage.getInstance()
                    .getReference()
                    .child(preferredPath)
                    .getDownloadUrl()
                    .addOnSuccessListener(uri -> {
                        if (isFinishing() || isDestroyed()) {
                            return;
                        }

                        loadImageWithGlide(
                                uri,
                                driverImage,
                                "driverImage"
                        );
                    })
                    .addOnFailureListener(error -> {
                        if (isFinishing() || isDestroyed()) {
                            return;
                        }

                        Log.w(
                                "DriverImage",
                                "Image not found for "
                                        + driverCode
                                        + " in "
                                        + year,
                                error
                        );

                        showDriverImageError();

                        loadDriverImage(driverCode, year);
                    });

        } else {
            loadDriverImage(driverCode, year);
        }
    }

    private void showDriverImageError() {
        if (isFinishing() || isDestroyed()) {
            return;
        }

        int placeholderResId = getResources().getIdentifier(
                "placeholder_driver",
                "drawable",
                getPackageName()
        );

        if (placeholderResId != 0) {
            driverImage.setImageResource(placeholderResId);
        }

        driverImage.setVisibility(View.VISIBLE);
    }

    private void loadImageWithGlide(
            Uri uri,
            ImageView imageView,
            String imageType) {

        if (isFinishing() || isDestroyed()) {
            return;
        }

        String drawableName;

        switch (imageType) {
            case "driverImage":
                drawableName = "placeholderdriver";
                break;

            case "carImage":
                drawableName = "placeholdercar";
                break;

            default:
                return;
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


    private void loadDriverTeam(
            String team,
            String driverName,
            String driverFamilyName,
            String driversCode) {

        if (isFinishing() || isDestroyed()) {
            return;
        }

        applyDefaultDriverCard();

        setDriverCardClick(
                team,
                driverName,
                driverFamilyName,
                driversCode,
                null
        );

        if (team == null || team.trim().isEmpty()) {
            return;
        }

        FirebaseDatabase.getInstance()
                .getReference()
                .child("constructors")
                .orderByChild("name")
                .equalTo(team)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(
                            @NonNull DataSnapshot snapshot) {

                        if (isFinishing() || isDestroyed()) {
                            return;
                        }

                        // Игнорируем ответ для уже сменённого пилота.
                        if (!Objects.equals(
                                currentDriverChoice,
                                "Andrea Kimi".equals(driverName)
                                        ? driverName + " " + driverFamilyName
                                        : driverName + (
                                        driverFamilyName.isEmpty()
                                                ? ""
                                                : " " + driverFamilyName
                                )
                        )) {
                            return;
                        }

                        if (!snapshot.exists()) {
                            return;
                        }

                        for (DataSnapshot teamSnap :
                                snapshot.getChildren()) {

                            setupDriverClickListener(
                                    teamSnap,
                                    team,
                                    driverName,
                                    driverFamilyName,
                                    driversCode
                            );

                            break;
                        }
                    }

                    @Override
                    public void onCancelled(
                            @NonNull DatabaseError error) {

                        if (isFinishing() || isDestroyed()) {
                            return;
                        }

                        Log.e(
                                "DriverTeam",
                                "Failed to load team: " + team,
                                error.toException()
                        );
                    }
                });
    }

    private void setupDriverClickListener(
            DataSnapshot teamSnap,
            String team,
            String driverName,
            String driverFamilyName,
            String driversCode) {

        String teamId = teamSnap.child("constructorId")
                .getValue(String.class);

        String colorValue = teamSnap.child("color")
                .getValue(String.class);

        applyDefaultDriverCard();

        if (colorValue != null && !colorValue.trim().isEmpty()) {
            String teamColor = colorValue.trim();

            if (!teamColor.startsWith("#")) {
                teamColor = "#" + teamColor;
            }

            try {
                int parsedColor = Color.parseColor(teamColor);

                driver_layout.setBackground(
                        cardBg(parsedColor, false)
                );

                applySoftTeamTint(driverPhoto, teamColor);
            } catch (IllegalArgumentException error) {
                Log.w(
                        "DriverTeam",
                        "Invalid team color: " + colorValue,
                        error
                );
            }
        }

        setDriverCardClick(
                team,
                driverName,
                driverFamilyName,
                driversCode,
                teamId
        );
    }

    private void applyDefaultDriverCard() {
        int defaultColor =
                ContextCompat.getColor(this, R.color.grey);

        driver_layout.setBackground(
                cardBg(defaultColor, false)
        );

        driverPhoto.setBackgroundTintList(
                ColorStateList.valueOf(
                        ContextCompat.getColor(
                                this,
                                R.color.colorPrimary
                        )
                )
        );
    }

    private void setDriverCardClick(
            String team,
            String driverName,
            String driverFamilyName,
            String driversCode,
            String teamId) {

        driver_layout.setOnClickListener(view -> {
            if (isFinishing() || isDestroyed()) {
                return;
            }

            Intent intent = new Intent(
                    accountPageActivity.this,
                    driverPageActivity.class
            );

            intent.putExtra("driverName", driverName);
            intent.putExtra("driverFamilyName", driverFamilyName);
            intent.putExtra("driverTeam", team);
            intent.putExtra("driverCode", driversCode);
            intent.putExtra("currentSeason", mCurrentSeason);

            if (teamId != null && !teamId.trim().isEmpty()) {
                intent.putExtra("driverTeamId", teamId);
            }

            startActivity(intent);
        });
    }

    private void loadTeamInfo(String choiceTeam) {
        DatabaseReference rootRef =
                FirebaseDatabase.getInstance().getReference();

        StorageReference storageRef =
                FirebaseStorage.getInstance().getReference();

        rootRef.child("constructors")
                .orderByChild("name")
                .equalTo(choiceTeam)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(
                            @NonNull DataSnapshot snapshot) {

                        if (isFinishing() || isDestroyed()) {
                            return;
                        }

                        if (!snapshot.exists()) {
                            Log.w(
                                    "TeamInfo",
                                    "Team not found: " + choiceTeam
                            );

                            showError(
                                    getString(R.string.team_not_found)
                            );
                            return;
                        }

                        for (DataSnapshot teamSnap :
                                snapshot.getChildren()) {

                            try {
                                String teamId =
                                        teamSnap.child("constructorId")
                                                .getValue(String.class);

                                String colorValue =
                                        teamSnap.child("color")
                                                .getValue(String.class);

                                if (teamId == null
                                        || colorValue == null) {

                                    showError(
                                            getString(
                                                    R.string.empty_team_data
                                            )
                                    );

                                    continue;
                                }

                                String teamColor = "#" + colorValue;
                                int parsedTeamColor =
                                        Color.parseColor(teamColor);

                                team_layout.setBackground(
                                        cardBg(parsedTeamColor, false)
                                );

                                applySoftTeamTint(teamPhoto, teamColor);

                                loadTeamCar(storageRef, teamId);
                                teamName.setText(choiceTeam);
                                setupTeamClickListener(choiceTeam, teamId);
                            } catch (Exception e) {
                                Log.e(
                                        "TeamInfo",
                                        "Error parsing team data",
                                        e
                                );

                                showError(
                                        getString(R.string.user_team_error)
                                );
                            }
                        }
                    }

                    @Override
                    public void onCancelled(
                            @NonNull DatabaseError error) {

                        if (isFinishing() || isDestroyed()) {
                            return;
                        }

                        handleFirebaseError(
                                error,
                                getString(R.string.user_team_error)
                        );
                    }
                });
    }

    private void loadTeamCar(
            StorageReference storageRef,
            String teamId) {

        storageRef.child(
                        "teams/"
                                + teamId
                                + "_"
                                + mCurrentSeason
                                + ".png"
                )
                .getDownloadUrl()
                .addOnSuccessListener(uri ->
                        loadImageWithGlide(
                                uri,
                                teamCar,
                                "carImage"
                        )
                )
                .addOnFailureListener(exception -> {
                    Log.e(
                            "TeamCar",
                            "Failed to load car for " + teamId,
                            exception
                    );

                    showError(
                            getString(R.string.car_load_error)
                    );
                });
    }

    private void setupTeamClickListener(
            String choiceTeam,
            String teamId) {

        team_layout.setOnClickListener(view -> {
            DatabaseReference rootRef =
                    FirebaseDatabase.getInstance().getReference();

            rootRef.child(
                            "driverLineUp/season/"
                                    + mCurrentSeason
                                    + "/"
                                    + teamId
                    )
                    .addListenerForSingleValueEvent(
                            new ValueEventListener() {
                                @Override
                                public void onDataChange(
                                        @NonNull DataSnapshot snapshot) {

                                    if (isFinishing() || isDestroyed()) {
                                        return;
                                    }

                                    ArrayList<String> teamDrivers =
                                            new ArrayList<>();

                                    for (DataSnapshot driverSnap :
                                            snapshot.child("drivers")
                                                    .getChildren()) {

                                        String name =
                                                driverSnap.getKey();

                                        if (name != null) {
                                            teamDrivers.add(name);
                                        }
                                    }

                                    Intent intent = new Intent(
                                            accountPageActivity.this,
                                            teamPageActivity.class
                                    );

                                    Bundle bundle = new Bundle();

                                    bundle.putString(
                                            "teamName",
                                            choiceTeam
                                    );
                                    bundle.putString(
                                            "teamId",
                                            teamId
                                    );
                                    bundle.putStringArrayList(
                                            "teamDrivers",
                                            teamDrivers
                                    );
                                    bundle.putString(
                                            "currentSeason",
                                            mCurrentSeason
                                    );

                                    intent.putExtras(bundle);

                                    startActivity(intent);
                                }

                                @Override
                                public void onCancelled(
                                        @NonNull DatabaseError error) {

                                    if (isFinishing() || isDestroyed()) {
                                        return;
                                    }

                                    handleFirebaseError(
                                            error,
                                            getString(
                                                    R.string.team_roster_error
                                            )
                                    );
                                }
                            });
        });
    }

    private void showNoDriver() {
        driverImage.setVisibility(View.INVISIBLE);
        noDriver.setVisibility(View.VISIBLE);
        noDriver.setText("+");

        driverName.setText("Выбрать пилота");
        driverFamilyName.setText("");

        driver_layout.setBackground(
                cardBg(
                        ContextCompat.getColor(
                                this,
                                R.color.grey
                        ),
                        true
                )
        );

        driverPhoto.setBackgroundTintList(
                ColorStateList.valueOf(
                        ContextCompat.getColor(
                                this,
                                R.color.colorPrimary
                        )
                )
        );

        driver_layout.setOnClickListener(v -> pickDriver());
    }

    private void showNoTeam() {
        teamCar.setVisibility(View.INVISIBLE);
        noTeam.setVisibility(View.VISIBLE);
        noTeam.setText("+");

        teamName.setText(getString(R.string.choose_team));

        team_layout.setBackground(
                cardBg(
                        ContextCompat.getColor(
                                this,
                                R.color.grey
                        ),
                        true
                )
        );

        teamPhoto.setBackgroundTintList(
                ColorStateList.valueOf(
                        ContextCompat.getColor(
                                this,
                                R.color.colorPrimary
                        )
                )
        );

        team_layout.setOnClickListener(v -> pickTeam());
    }

    private void pickDriver() {
        FirebaseDatabase.getInstance()
                .getReference()
                .child("drivers")
                .addListenerForSingleValueEvent(
                        new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot) {

                                if (isFinishing() || isDestroyed()) {
                                    return;
                                }

                                List<DriverRecord> records = new ArrayList<>();

                                for (DataSnapshot driverSnapshot : snapshot.getChildren()) {
                                    String fullName = driverSnapshot.getKey();

                                    String code = driverSnapshot.child("driversCode")
                                            .getValue(String.class);

                                    if (fullName == null || code == null) {
                                        continue;
                                    }

                                    String lastEntry = driverSnapshot.child("lastEntry")
                                            .getValue(String.class);

                                    String driverId = driverSnapshot.child("driverId")
                                            .getValue(String.class);

                                    String status = driverSnapshot.child("status")
                                            .getValue(String.class);

                                    if (driverId != null) {
                                        driverId = driverId.trim();

                                        if (driverId.isEmpty()) {
                                            driverId = null;
                                        }
                                    }

                                    if (status != null) {
                                        status = status.trim().toLowerCase(Locale.ROOT);
                                    }

                                    String yearText = extractYear(lastEntry);
                                    int lastEntryYear = Integer.MIN_VALUE;

                                    if (yearText != null) {
                                        try {
                                            lastEntryYear = Integer.parseInt(yearText);
                                        } catch (NumberFormatException error) {
                                            Log.w(
                                                    "DriverPicker",
                                                    "Invalid lastEntry for " + fullName,
                                                    error
                                            );
                                        }
                                    }

                                    records.add(new DriverRecord(
                                            fullName,
                                            code,
                                            lastEntry,
                                            lastEntryYear,
                                            driverId,
                                            status
                                    ));

                                    Log.d(
                                            "DriverPicker",
                                            "driver=" + fullName
                                                    + ", id=" + driverId
                                                    + ", status=" + status
                                                    + ", year=" + yearText
                                    );
                                }

                                records.sort((first, second) -> {
                                    int yearCompare = Integer.compare(
                                            second.lastEntryYear,
                                            first.lastEntryYear
                                    );

                                    if (yearCompare != 0) {
                                        return yearCompare;
                                    }

                                    return first.fullName.compareToIgnoreCase(
                                            second.fullName
                                    );
                                });

                                List<favoriteItem> items = new ArrayList<>();

                                items.add(favoriteItem.emptyDriver());

                                for (DriverRecord record : records) {
                                    String[] parts = record.fullName.split(" ");

                                    String driverName;
                                    String familyName;

                                    if (record.fullName.equals("Andrea Kimi Antonelli")) {
                                        driverName = "Andrea Kimi";
                                        familyName = "Antonelli";
                                    } else {
                                        driverName = parts[0];
                                        familyName = parts.length > 1
                                                ? parts[parts.length - 1]
                                                : "";
                                    }

                                    String imageYear;

                                    if (record.lastEntryYear == Integer.MIN_VALUE) {
                                        imageYear = mCurrentSeason;
                                    } else {
                                        imageYear = String.valueOf(
                                                record.lastEntryYear
                                        );
                                    }

                                    String imagePath;

                                    if (record.id != null && !record.id.trim().isEmpty()) {
                                        imagePath = "drivers/"
                                                + record.id.trim()
                                                + "_"
                                                + imageYear
                                                + ".png";
                                    } else {
                                        imagePath = "drivers/"
                                                + record.code.trim().toLowerCase(Locale.ROOT)
                                                + "_"
                                                + imageYear
                                                + ".png";
                                    }

                                    String description = getString(R.string.driver_description);

                                    if (record.status != null) {
                                        switch (record.status) {
                                            case "active":
                                                description =
                                                        getString(R.string.driver_active_text)
                                                                + " "
                                                                + getString(R.string.driver_description);
                                                break;

                                            case "reserve":
                                                description =
                                                        getString(R.string.driver_reserve_text)
                                                                + " "
                                                                + getString(R.string.driver_description);
                                                break;

                                            case "retired":
                                                description = getString(R.string.retired_text);
                                                break;

                                            default:
                                                break;
                                        }
                                    }

                                    items.add(new favoriteItem(
                                            record.fullName,
                                            driverName,
                                            familyName,
                                            "",
                                            description,
                                            imagePath,
                                            false,
                                            false,
                                            record.lastEntry
                                    ));
                                }

                                showDriverPicker(items);
                            }

                            @Override
                            public void onCancelled(
                                    @NonNull DatabaseError error) {

                                if (isFinishing() || isDestroyed()) {
                                    return;
                                }

                                handleFirebaseError(
                                        error,
                                        getString(
                                                R.string.smth_wrong_text
                                        )
                                );
                            }
                        });
    }

    private void showDriverPicker(List<favoriteItem> items) {
        favoritePickerDialog dialog =
                new favoritePickerDialog(
                        this,
                        getString(R.string.driver_choice_text),
                        items,
                        currentDriverChoice,
                        item -> saveChoice(
                                "choiceDriver",
                                item.value
                        )
                );

        dialog.show();
    }

    private void pickTeam() {
        FirebaseDatabase.getInstance()
                .getReference()
                .child("constructors")
                .addListenerForSingleValueEvent(
                        new ValueEventListener() {
                            @Override
                            public void onDataChange(
                                    @NonNull DataSnapshot snapshot) {

                                if (isFinishing() || isDestroyed()) {
                                    return;
                                }

                                List<favoriteItem> items =
                                        new ArrayList<>();

                                items.add(favoriteItem.emptyTeam());

                                for (DataSnapshot teamSnapshot :
                                        snapshot.getChildren()) {

                                    String teamName =
                                            teamSnapshot.child("name")
                                                    .getValue(String.class);

                                    String teamId =
                                            teamSnapshot.child(
                                                    "constructorId"
                                            ).getValue(String.class);

                                    if (teamName == null
                                            || teamId == null) {
                                        continue;
                                    }

                                    String imagePath =
                                            "teams/"
                                                    + teamId
                                                    + "_"
                                                    + mCurrentSeason
                                                    + ".png";

                                    items.add(new favoriteItem(
                                            teamName,
                                            "",
                                            "",
                                            teamName,
                                            getString(
                                                    R.string.team_description
                                            ),
                                            imagePath,
                                            false,
                                            true,
                                            null
                                    ));
                                }

                                showTeamPicker(items);
                            }

                            @Override
                            public void onCancelled(
                                    @NonNull DatabaseError error) {

                                if (isFinishing() || isDestroyed()) {
                                    return;
                                }

                                handleFirebaseError(
                                        error,
                                        getString(
                                                R.string.smth_wrong_text
                                        )
                                );
                            }
                        });
    }

    private void showTeamPicker(List<favoriteItem> items) {
        favoritePickerDialog dialog =
                new favoritePickerDialog(
                        this,
                        getString(R.string.choiceTeam_text),
                        items,
                        currentTeamChoice,
                        item -> saveChoice(
                                "choiceTeam",
                                item.value
                        )
                );

        dialog.show();
    }

    private void saveChoice(String field, String valueOrNull) {
        FirebaseUser user = auth.getCurrentUser();

        if (user == null) {
            startActivity_seasonData(
                    accountPageActivity.this,
                    logInPageActivity.class,
                    mCurrentSeason
            );

            finish();
            return;
        }

        String uid = user.getUid();

        DatabaseReference userRef =
                FirebaseDatabase.getInstance()
                        .getReference()
                        .child("users")
                        .child(uid);

        userRef.child(field)
                .setValue(valueOrNull)
                .addOnSuccessListener(unused -> {
                    if (isFinishing() || isDestroyed()) {
                        return;
                    }

                    if ("choiceTeam".equals(field)) {
                        currentTeamChoice = valueOrNull;

                        if (isNullChoice(valueOrNull)) {
                            showNoTeam();
                        } else {
                            resetTeamCard();
                            loadTeamInfo(valueOrNull);
                        }
                    }

                    if ("choiceDriver".equals(field)) {
                        currentDriverChoice = valueOrNull;

                        if (isNullChoice(valueOrNull)) {
                            showNoDriver();
                        } else {
                            resetDriverCard();
                            loadDriverInfo(valueOrNull);
                        }
                    }

                    Toast.makeText(
                            this,
                            getString(R.string.saved_text),
                            Toast.LENGTH_SHORT
                    ).show();
                })
                .addOnFailureListener(error -> {
                    if (isFinishing() || isDestroyed()) {
                        return;
                    }

                    showError(
                            getString(R.string.smth_wrong_text)
                    );
                });
    }

    private void resetTeamCard() {
        teamCar.setVisibility(View.INVISIBLE);
        noTeam.setVisibility(View.INVISIBLE);
        teamName.setText("");

        team_layout.setBackground(
                cardBg(
                        ContextCompat.getColor(
                                this,
                                R.color.grey
                        ),
                        true
                )
        );
    }

    private void resetDriverCard() {
        driverImage.setVisibility(View.INVISIBLE);
        noDriver.setVisibility(View.INVISIBLE);
        driverName.setText("");
        driverFamilyName.setText("");

        driver_layout.setBackground(
                cardBg(
                        ContextCompat.getColor(
                                this,
                                R.color.grey
                        ),
                        true
                )
        );
    }

    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private int getSoftTeamColor(String teamColor) {
        int originalColor = Color.parseColor(teamColor);

        return ColorUtils.compositeColors(
                ColorUtils.setAlphaComponent(originalColor, 31),
                Color.WHITE
        );
    }

    private void applySoftTeamTint(View view, String teamColor) {
        int softColor = getSoftTeamColor(teamColor);

        view.setBackgroundTintList(
                ColorStateList.valueOf(softColor)
        );
    }

    private static class DriverRecord {
        String fullName;
        String code;
        String lastEntry;
        int lastEntryYear;
        String id;
        String status;

        DriverRecord(
                String fullName,
                String code,
                String lastEntry,
                int lastEntryYear,
                String id,
                String status) {

            this.fullName = fullName;
            this.code = code;
            this.lastEntry = lastEntry;
            this.lastEntryYear = lastEntryYear;
            this.id = id;
            this.status = status;
        }
    }

    private void handleFirebaseError(
            DatabaseError error,
            String defaultMessage) {

        String userMessage = defaultMessage;

        if (error.getCode() == DatabaseError.NETWORK_ERROR) {
            userMessage = getString(R.string.lost_connection_info);
        } else if (error.getCode()
                == DatabaseError.PERMISSION_DENIED) {

            userMessage = getString(R.string.data_rules_error);
        }

        Toast.makeText(
                this,
                userMessage,
                Toast.LENGTH_LONG
        ).show();

        Log.e(
                "FirebaseDB",
                "Error code: "
                        + error.getCode()
                        + ", "
                        + error.getMessage()
        );
    }

    private void loadAvatarIfExists(FirebaseUser user) {
        if (user == null || avatarImage == null) {
            return;
        }

        StorageReference avatarRef =
                FirebaseStorage.getInstance()
                        .getReference()
                        .child("profileImages")
                        .child(user.getUid())
                        .child("avatar.jpg");

        avatarRef.getDownloadUrl()
                .addOnSuccessListener(uri -> {
                    if (isFinishing() || isDestroyed()) {
                        return;
                    }

                    avatarImage.setVisibility(View.VISIBLE);
                    avatarInitial.setVisibility(View.GONE);

                    GlideApp.with(this)
                            .load(uri)
                            .skipMemoryCache(true)
                            .diskCacheStrategy(DiskCacheStrategy.NONE)
                            .into(avatarImage);
                })
                .addOnFailureListener(error -> {
                    if (isFinishing() || isDestroyed()) {
                        return;
                    }

                    avatarImage.setVisibility(View.GONE);
                    avatarInitial.setVisibility(View.VISIBLE);
                });
    }

    private void closeAccountPage() {
        finish();

        if (Build.VERSION.SDK_INT
                >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {

            overrideActivityTransition(
                    OVERRIDE_TRANSITION_CLOSE,
                    0,
                    0
            );
        } else {
            overridePendingTransition(0, 0);
        }
    }
}