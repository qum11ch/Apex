package com.example.f1app;

import static com.example.f1app.MainActivity.checkConnection;
import static com.example.f1app.driversStandingsActivity.startActivity_seasonData;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.ColorUtils;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Transaction;
import com.google.firebase.database.MutableData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import android.net.Uri;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class registerPageActivity extends AppCompatActivity {
    private FirebaseAuth auth;

    private EditText editTextLogin;
    private EditText editTextUsername;
    private EditText editTextEmail;
    private EditText editTextPassword;

    private Button registerButton;
    private ProgressBar registerProgress;

    private boolean driversLoaded = false;
    private boolean teamsLoaded = false;
    private boolean registrationInProgress = false;

    private String mCurrentSeason;

    private ImageView avatarImage;
    private TextView avatarInitial;
    private Button changeAvatarButton;

    private RelativeLayout teamLayout;
    private RelativeLayout driverLayout;
    private ImageView teamCar;
    private ImageView driverImage;
    private TextView teamName;
    private TextView driverName;
    private TextView driverFamilyName;
    private TextView noTeam;
    private TextView noDriver;

    private RelativeLayout teamPhoto, driverPhoto;

    private String userKey;
    private String currentDriverChoice;
    private String currentTeamChoice;

    private String selectedDriver = "null";
    private String selectedTeam = "null";

    private ActivityResultLauncher<PickVisualMediaRequest> avatarPicker;
    private Uri selectedAvatarUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.register_page);
        setupRegisterInsets();

        if (!checkConnection(getApplicationContext())) {
            startActivity(
                    connectionLostScreen.createShowSplashOnNetworkFailure(this)
            );
        } else {
            startActivity(
                    connectionLostScreen.createIntentHideSplashOnNetworkRecovery(this)
            );
        }

        Bundle extras = getIntent().getExtras();
        mCurrentSeason = extras == null
                ? null
                : extras.getString("currentSeason");

        WindowInsetsControllerCompat insetsController =
                WindowCompat.getInsetsController(
                        getWindow(),
                        getWindow().getDecorView()
                );
        insetsController.setAppearanceLightStatusBars(false);

        editTextLogin = findViewById(R.id.signUpLogin);
        editTextUsername = findViewById(R.id.signUpUsername);
        editTextEmail = findViewById(R.id.signUpEmail);
        editTextPassword = findViewById(R.id.signUpPassword);

        editTextUsername.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String value = s.toString().trim();

                if (value.isEmpty()) {
                    avatarInitial.setText("");
                } else {
                    avatarInitial.setText(
                            value.substring(0, 1).toUpperCase(Locale.ROOT)
                    );
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        teamName = findViewById(R.id.teamName);

        registerButton = findViewById(R.id.registerButton);
        registerProgress = findViewById(R.id.registerProgress);

        avatarImage = findViewById(R.id.avatarImage);
        avatarInitial = findViewById(R.id.avatarInitial);
        changeAvatarButton = findViewById(R.id.changeAvatarButton);

        teamLayout = findViewById(R.id.team_layout);
        driverLayout = findViewById(R.id.driver_layout);
        teamCar = findViewById(R.id.teamCar);
        driverImage = findViewById(R.id.driver_image);

        teamName = findViewById(R.id.teamName);
        driverName = findViewById(R.id.driverName);
        driverFamilyName = findViewById(R.id.driverFamilyName);
        noTeam = findViewById(R.id.noTeam);
        noDriver = findViewById(R.id.noDriver);

        teamPhoto = findViewById(R.id.teamPhoto);
        driverPhoto = findViewById(R.id.driverPhoto);

        showNoTeam();
        showNoDriver();

        teamLayout.setOnClickListener(v -> pickTeam());
        driverLayout.setOnClickListener(v -> pickDriver());

        updateRegisterButtonState();

        auth = FirebaseAuth.getInstance();

        registerButton.setEnabled(false);

        ImageButton backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> finish());

        TextWatcher watcher = createTextWatcher();
        editTextLogin.addTextChangedListener(watcher);
        editTextUsername.addTextChangedListener(watcher);
        editTextEmail.addTextChangedListener(watcher);
        editTextPassword.addTextChangedListener(watcher);

        registerButton.setOnClickListener(v -> registerNewUser());

        changeAvatarButton.setOnClickListener(v -> {
            PickVisualMediaRequest request =
                    new PickVisualMediaRequest.Builder()
                            .setMediaType(
                                    ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE
                            )
                            .build();

            avatarPicker.launch(request);
        });

        avatarPicker = registerForActivityResult(
                new ActivityResultContracts.PickVisualMedia(),
                uri -> {
                    if (uri == null) return;

                    selectedAvatarUri = uri;

                    GlideApp.with(avatarImage)
                            .load(uri)
                            .circleCrop()
                            .into(avatarImage);

                    avatarImage.setVisibility(View.VISIBLE);
                    avatarInitial.setVisibility(View.GONE);
                }
        );
    }

    private void setupRegisterInsets() {
        View root = findViewById(R.id.register_root);
        View header = findViewById(R.id.header);

        final int rootLeft = root.getPaddingLeft();
        final int rootTop = root.getPaddingTop();
        final int rootRight = root.getPaddingRight();
        final int rootBottom = root.getPaddingBottom();

        final int headerLeft = header.getPaddingLeft();
        final int headerTop = header.getPaddingTop();
        final int headerRight = header.getPaddingRight();
        final int headerBottom = header.getPaddingBottom();

        ViewCompat.setOnApplyWindowInsetsListener(root, (view, windowInsets) -> {
            Insets safeInsets = windowInsets.getInsets(
                    WindowInsetsCompat.Type.systemBars()
                            | WindowInsetsCompat.Type.displayCutout()
            );

            Insets imeInsets = windowInsets.getInsets(
                    WindowInsetsCompat.Type.ime()
            );

            int bottomInset = Math.max(
                    safeInsets.bottom,
                    imeInsets.bottom
            );

            view.setPadding(
                    rootLeft + safeInsets.left,
                    rootTop,
                    rootRight + safeInsets.right,
                    rootBottom
            );

            header.setPadding(
                    headerLeft,
                    headerTop + safeInsets.top,
                    headerRight,
                    headerBottom
            );

            return windowInsets;
        });

        ViewCompat.requestApplyInsets(root);
    }

    private GradientDrawable cardBg(int strokeColor, boolean dashed) {
        float d = getResources().getDisplayMetrics().density;
        GradientDrawable gd = new GradientDrawable();
        gd.setColor(ContextCompat.getColor(this, R.color.colorPrimary));
        gd.setCornerRadius(16 * d);
        if (dashed) gd.setStroke((int) (2 * d), strokeColor, 8 * d, 4 * d);
        else        gd.setStroke((int) (2 * d), strokeColor);
        return gd;
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
                            String driversCode = snapshot.child("driversCode").getValue(String.class);
                            String team = snapshot.child("driversTeam").getValue(String.class);
                            String driverId = snapshot.child("driverId").getValue(String.class);

                            if (driversCode == null || team == null) {
                                showError("Неполные данные пилота");
                                return;
                            }

                            displayDriverImage(driverId, choiceDriver, driversCode);
                            driverName.setText(mDriverName);
                            driverFamilyName.setText(mDriverFamilyName);
                            driverFamilyName.setVisibility(View.VISIBLE);

                            String finalTeam = team.equals("Sauber") ? "Kick Sauber" : team;
                            loadDriverTeam(finalTeam, mDriverName, mDriverFamilyName, driversCode);
                        } catch (Exception e) {
                            Log.e("DriverInfo", "Error parsing driver data", e);
                            showError(getString(R.string.user_driver_error));
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        handleFirebaseError(error, getString(R.string.user_driver_error));
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
                    public void onDataChange(
                            @NonNull DataSnapshot snapshot) {

                        if (isFinishing() || isDestroyed()) {
                            return;
                        }

                        String lastEntry =
                                snapshot.getValue(String.class);

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

    private void loadDriverTeam(String team, String driverName, String driverFamilyName, String driversCode) {
        DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference();
        rootRef.child("constructors")
                .orderByChild("name")
                .equalTo(team)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (!snapshot.exists()) {
                            showError(getString(R.string.team_not_found));
                            return;
                        }
                        for (DataSnapshot teamSnap : snapshot.getChildren()) {
                            setupDriverClickListener(teamSnap);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        handleFirebaseError(error, getString(R.string.user_team_error));
                    }
                });
    }

    private void setupDriverClickListener(DataSnapshot teamSnap) {
        try {
            String teamId = teamSnap.child("constructorId").getValue(String.class);
            String colorValue = teamSnap.child("color").getValue(String.class);

            if (teamId == null || colorValue == null) {
                Log.w("DriverData", "Missing team data");
                return;
            }

            String teamColor = "#" + colorValue;
            int parsedTeamColor = Color.parseColor(teamColor);

            driverLayout.setBackground(
                    cardBg(parsedTeamColor, false)
            );

            applySoftTeamTint(driverPhoto, teamColor);

            driverLayout.setOnClickListener(view -> {
                pickDriver();
            });
        } catch (IllegalArgumentException e) {
            Log.e("DriverLayout", "Invalid color format", e);
            showError(getString(R.string.render_error));
        }
    }

    private void loadTeamInfo(String choiceTeam) {
        DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference();
        StorageReference storageRef = FirebaseStorage.getInstance().getReference();

        rootRef.child("constructors")
                .orderByChild("name")
                .equalTo(choiceTeam)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (!snapshot.exists()) {
                            Log.w("TeamInfo", "Team not found: " + choiceTeam);
                            showError(getString(R.string.team_not_found));
                            return;
                        }

                        for (DataSnapshot teamSnap : snapshot.getChildren()) {
                            try {
                                String teamId = teamSnap.child("constructorId").getValue(String.class);
                                String colorValue = teamSnap.child("color").getValue(String.class);

                                if (teamId == null || colorValue == null) {
                                    showError(getString(R.string.empty_team_data));
                                    continue;
                                }

                                String teamColor = "#" + colorValue;
                                int parsedTeamColor = Color.parseColor(teamColor);

                                teamLayout.setBackground(
                                        cardBg(parsedTeamColor, false)
                                );

                                applySoftTeamTint(teamPhoto, teamColor);

                                loadTeamCar(storageRef, teamId);
                                teamName.setText(choiceTeam);
                            } catch (Exception e) {
                                Log.e("TeamInfo", "Error parsing team data", e);
                                showError(getString(R.string.user_team_error));
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        handleFirebaseError(error, getString(R.string.user_team_error));
                    }
                });
    }

    private void loadTeamCar(StorageReference storageRef, String teamId) {
        storageRef.child("teams/" + teamId + "_" + mCurrentSeason + ".png")
                .getDownloadUrl()
                .addOnSuccessListener(uri -> loadImageWithGlide(uri, teamCar, "carImage"))
                .addOnFailureListener(exception -> {
                    Log.e("TeamCar", "Failed to load car for " + teamId, exception);
                    showError(getString(R.string.car_load_error));
                });
    }

    private void showNoDriver() {
        driverImage.setVisibility(View.INVISIBLE);
        noDriver.setVisibility(View.VISIBLE);
        noDriver.setText("+");

        driverName.setText("Выбрать пилота");
        driverFamilyName.setText("");

        driverLayout.setBackground(
                cardBg(ContextCompat.getColor(this, R.color.grey), true));

        driverPhoto.setBackgroundTintList(ColorStateList.valueOf(
                ContextCompat.getColor(this, R.color.colorPrimary)
        ));

        driverLayout.setOnClickListener(v -> pickDriver());
    }

    private void showNoTeam() {
        teamCar.setVisibility(View.INVISIBLE);
        noTeam.setVisibility(View.VISIBLE);
        noTeam.setText("+");

        teamName.setText(getString(R.string.choose_team));

        teamLayout.setBackground(
                cardBg(ContextCompat.getColor(this, R.color.grey), true)
        );

        teamPhoto.setBackgroundTintList(ColorStateList.valueOf(
                ContextCompat.getColor(this, R.color.colorPrimary)
        ));

        teamLayout.setOnClickListener(v -> pickTeam());
    }

    private void pickDriver() {
        FirebaseDatabase.getInstance()
                .getReference()
                .child("drivers")
                .addListenerForSingleValueEvent(
                        new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot) {

                                List<registerPageActivity.DriverRecord> records = new ArrayList<>();

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

                                    records.add(new registerPageActivity.DriverRecord(
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

                                for (registerPageActivity.DriverRecord record : records) {
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

                                    items.add(
                                            new favoriteItem(
                                                    record.fullName,
                                                    driverName,
                                                    familyName,
                                                    "",
                                                    description,
                                                    imagePath,
                                                    false,
                                                    false,
                                                    record.lastEntry
                                            )
                                    );
                                }

                                showDriverPicker(items);
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {

                                handleFirebaseError(
                                        error,
                                        getString(
                                                R.string.smth_wrong_text
                                        )
                                );
                            }
                        }
                );
    }

    private void showDriverPicker(List<favoriteItem> items) {
        favoritePickerDialog dialog = new favoritePickerDialog(
                this,
                getString(R.string.driver_choice_text),
                items,
                selectedDriver,
                item -> {
                    selectedDriver = item.value == null ? "null" : item.value;

                    if ("null".equals(selectedDriver)) {
                        showNoDriver();
                    } else {
                        resetDriverCard();
                        loadDriverInfo(selectedDriver);
                        noDriver.setVisibility(View.INVISIBLE);

                        loadSelectedImage(
                                item.imagePath,
                                driverImage,
                                R.drawable.placeholder_driver
                        );

                        String[] parts = selectedDriver.split("\\s+");
                        if ("Andrea Kimi Antonelli".equals(selectedDriver)) {
                            driverName.setText("Andrea Kimi");
                            driverFamilyName.setText("Antonelli");
                        } else {
                            driverName.setText(parts[0]);
                            driverFamilyName.setText(
                                    parts.length > 1 ? parts[parts.length - 1] : ""
                            );
                        }

                        driverImage.setVisibility(View.VISIBLE);
                    }

                    updateRegisterButtonState();
                }
        );

        dialog.show();
    }

    private void loadSelectedImage(
            String imagePath,
            ImageView target,
            int placeholderRes) {

        if (imagePath == null || imagePath.trim().isEmpty()) {
            target.setImageResource(placeholderRes);
            return;
        }

        StorageReference ref =
                FirebaseStorage.getInstance()
                        .getReference()
                        .child(imagePath);

        GlideApp.with(target)
                .load(ref)
                .placeholder(placeholderRes)
                .error(placeholderRes)
                .into(target);
    }

    private void pickTeam() {
        FirebaseDatabase.getInstance()
                .getReference()
                .child("constructors")
                .addListenerForSingleValueEvent(
                        new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot) {

                                List<favoriteItem> items = new ArrayList<>();

                                items.add(favoriteItem.emptyTeam());

                                for (DataSnapshot teamSnapshot : snapshot.getChildren()) {

                                    String teamName = teamSnapshot
                                            .child("name")
                                            .getValue(String.class);

                                    String teamId = teamSnapshot
                                            .child("constructorId")
                                            .getValue(String.class);

                                    if (teamName == null || teamId == null) {
                                        continue;
                                    }

                                    String imagePath = "teams/" + teamId + "_" + mCurrentSeason + ".png";

                                    items.add(
                                            new favoriteItem(
                                                    teamName,
                                                    "",
                                                    "",
                                                    teamName,
                                                    getString(R.string.team_description),
                                                    imagePath,
                                                    false,
                                                    true,
                                                    null
                                            )
                                    );
                                }

                                showTeamPicker(items);
                            }

                            @Override
                            public void onCancelled(
                                    @NonNull DatabaseError error) {

                                handleFirebaseError(
                                        error,
                                        getString(
                                                R.string.smth_wrong_text
                                        )
                                );
                            }
                        }
                );
    }

    private void showTeamPicker(List<favoriteItem> items) {
        favoritePickerDialog dialog = new favoritePickerDialog(
                this,
                getString(R.string.choiceTeam_text),
                items,
                selectedTeam,
                item -> {
                    selectedTeam =
                            item.value == null ? "null" : item.value;

                    if ("null".equals(selectedTeam)) {
                        showNoTeam();
                    } else {
                        resetTeamCard();
                        loadTeamInfo(selectedTeam);
                        noTeam.setVisibility(View.INVISIBLE);
                        teamName.setText(selectedTeam);

                        loadSelectedImage(
                                item.imagePath,
                                teamCar,
                                R.drawable.placeholder_car
                        );

                        teamCar.setVisibility(View.VISIBLE);
                    }

                    updateRegisterButtonState();
                }
        );

        dialog.show();
    }

    private void resetTeamCard() {
        teamCar.setVisibility(View.INVISIBLE);
        noTeam.setVisibility(View.INVISIBLE);
        teamName.setText("");

        teamLayout.setBackground(
                cardBg(
                        ContextCompat.getColor(this, R.color.grey),
                        true
                )
        );
    }

    private void resetDriverCard() {
        driverImage.setVisibility(View.INVISIBLE);
        noDriver.setVisibility(View.INVISIBLE);
        driverName.setText("");
        driverFamilyName.setText("");

        driverLayout.setBackground(
                cardBg(
                        ContextCompat.getColor(this, R.color.grey),
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

    public TextView getTeamName() {
        return teamName;
    }

    public void setTeamName(TextView teamName) {
        this.teamName = teamName;
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

    private void handleFirebaseError(DatabaseError error, String defaultMessage) {
        String userMessage = defaultMessage;
        if (error.getCode() == DatabaseError.NETWORK_ERROR) {
            userMessage = getString(R.string.lost_connection_info);
        } else if (error.getCode() == DatabaseError.PERMISSION_DENIED) {
            userMessage = getString(R.string.data_rules_error);
        }
        Toast.makeText(this, userMessage, Toast.LENGTH_LONG).show();
        Log.e("FirebaseDB", "Error code: " + error.getCode() + ", " + error.getMessage());
    }

    private TextWatcher createTextWatcher() {
        return new TextWatcher() {
            @Override
            public void beforeTextChanged(
                    CharSequence s,
                    int start,
                    int count,
                    int after) {
            }

            @Override
            public void onTextChanged(
                    CharSequence s,
                    int start,
                    int before,
                    int count) {
                validateFields();
                updateRegisterButtonState();
            }

            @Override
            public void afterTextChanged(Editable editable) {
            }
        };
    }

    private void validateFields() {
        String login = getLoginInput();
        String username = getUsernameInput();
        String email = getEmailInput();
        String password = getPasswordInput();

        setLoginError(
                login.isEmpty() || login.matches("[A-Za-z0-9_]{3,24}")
                        ? null
                        : "Логин: 3–24 символа, латиница, цифры или _"
        );

        setUsernameError(
                username.isEmpty()
                        ? "Введите публичный username"
                        : username.length() > 30
                        ? "Максимум 30 символов"
                        : null
        );

        setEmailError(
                email.isEmpty() || Patterns.EMAIL_ADDRESS.matcher(email).matches()
                        ? null
                        : getString(R.string.invalid_email_text)
        );

        setPasswordError(
                password.isEmpty() || password.length() >= 6
                        ? null
                        : getString(R.string.invalid_password_text)
        );
    }

    private void setEmailError(String message) {
        LinearLayout box = findViewById(R.id.emailBox);
        TextView error = findViewById(R.id.emailError);

        boolean hasError = message != null && !message.isEmpty();

        box.setBackgroundResource(
                hasError
                        ? R.drawable.bg_input_box_error
                        : R.drawable.bg_input_box
        );

        error.setText(hasError ? message : "");
        error.setVisibility(hasError ? View.VISIBLE : View.GONE);
    }

    private void setUsernameError(String message) {
        LinearLayout box = findViewById(R.id.usernameBox);
        TextView error = findViewById(R.id.usernameError);

        boolean hasError = message != null && !message.isEmpty();

        box.setBackgroundResource(
                hasError
                        ? R.drawable.bg_input_box_error
                        : R.drawable.bg_input_box
        );

        error.setText(hasError ? message : "");
        error.setVisibility(hasError ? View.VISIBLE : View.GONE);
    }

    private void setPasswordError(String message) {
        LinearLayout box = findViewById(R.id.passwordBox);
        TextView error = findViewById(R.id.passwordError);

        boolean hasError = message != null && !message.isEmpty();

        box.setBackgroundResource(
                hasError
                        ? R.drawable.bg_input_box_error
                        : R.drawable.bg_input_box
        );

        error.setText(hasError ? message : "");
        error.setVisibility(hasError ? View.VISIBLE : View.GONE);
    }

    private void setLoginError(String message) {
        LinearLayout box = findViewById(R.id.loginBox);
        TextView error = findViewById(R.id.loginError);

        boolean hasError = message != null && !message.isEmpty();

        box.setBackgroundResource(
                hasError
                        ? R.drawable.bg_input_box_error
                        : R.drawable.bg_input_box
        );

        error.setText(hasError ? message : "");
        error.setVisibility(hasError ? View.VISIBLE : View.GONE);
    }

    private void updateRegisterButtonState() {
        String login = getLoginInput();
        String username = getUsernameInput();
        String email = getEmailInput();
        String password = getPasswordInput();

        boolean fieldsValid =
                login.matches("[A-Za-z0-9_]{3,24}")
                        && !username.isEmpty()
                        && username.length() <= 30
                        && Patterns.EMAIL_ADDRESS.matcher(email).matches()
                        && password.length() >= 6;

        registerButton.setEnabled(
                fieldsValid && !registrationInProgress
        );
    }

    private void registerNewUser() {
        if (registrationInProgress) return;

        validateFields();
        updateRegisterButtonState();

        if (!registerButton.isEnabled()) {
            showError(getString(R.string.all_fields_text));
            return;
        }

        String login = getLoginInput();
        String publicUsername = getUsernameInput();
        String email = getEmailInput();
        String password = getPasswordInput();

        String choiceDriver = selectedDriver;
        String choiceTeam = selectedTeam;

        registrationInProgress = true;
        setLoading(true);

        reserveLoginAndCreateAccount(
                login,
                publicUsername,
                email,
                password,
                choiceDriver,
                choiceTeam
        );
    }

    private void reserveLoginAndCreateAccount(
            String login,
            String publicUsername,
            String email,
            String password,
            String choiceDriver,
            String choiceTeam) {

        String normalizedLogin = login.toLowerCase(Locale.ROOT);

        DatabaseReference loginRef =
                FirebaseDatabase.getInstance()
                        .getReference()
                        .child("loginIndex")
                        .child(normalizedLogin);

        loginRef.runTransaction(new Transaction.Handler() {
            @NonNull
            @Override
            public Transaction.Result doTransaction(@NonNull MutableData currentData) {
                if (currentData.getValue() != null) {
                    return Transaction.abort();
                }

                currentData.setValue("RESERVED");
                return Transaction.success(currentData);
            }

            @Override
            public void onComplete(
                    DatabaseError error,
                    boolean committed,
                    DataSnapshot currentData) {

                if (isFinishing() || isDestroyed()) return;

                if (error != null) {
                    Log.e("Register", "Login reservation failed", error.toException());
                    failRegistration(
                            getString(R.string.smth_wrong_text)
                    );
                    return;
                }

                if (!committed) {
                    failRegistration("Выберите другой логин");
                    setLoginError("Этот логин уже занят");
                    return;
                }

                createFirebaseAccount(
                        loginRef,
                        login,
                        publicUsername,
                        email,
                        password,
                        choiceDriver,
                        choiceTeam
                );
            }
        });
    }

    private void createFirebaseAccount(
            DatabaseReference loginRef,
            String login,
            String publicUsername,
            String email,
            String password,
            String choiceDriver,
            String choiceTeam) {

        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (isFinishing() || isDestroyed()) return;

                    if (!task.isSuccessful()) {
                        // Освобождаем занятый login, если аккаунт не создан.
                        loginRef.removeValue()
                                .addOnCompleteListener(unused ->
                                        failRegistration(
                                                getString(
                                                        R.string.reqistration_fail_text
                                                )
                                        )
                                );
                        return;
                    }

                    FirebaseUser firebaseUser =
                            task.getResult().getUser();

                    if (firebaseUser == null) {
                        failRegistration(
                                getString(R.string.smth_wrong_text)
                        );
                        return;
                    }

                    createProfileAfterAuth(
                            firebaseUser,
                            loginRef,
                            login,
                            publicUsername,
                            choiceDriver,
                            choiceTeam
                    );
                });
    }

    private void createProfileAfterAuth(
            FirebaseUser user,
            DatabaseReference loginRef,
            String login,
            String publicUsername,
            String choiceDriver,
            String choiceTeam) {

        if (selectedAvatarUri == null) {
            saveProfile(
                    user,
                    loginRef,
                    login,
                    publicUsername,
                    choiceDriver,
                    choiceTeam,
                    null
            );
            return;
        }

        StorageReference avatarRef =
                FirebaseStorage.getInstance()
                        .getReference()
                        .child("profileImages")
                        .child(user.getUid())
                        .child("avatar.jpg");

        avatarRef.putFile(selectedAvatarUri)
                .addOnSuccessListener(taskSnapshot ->
                        avatarRef.getDownloadUrl()
                                .addOnSuccessListener(uri ->{
                                    saveProfile(
                                            user,
                                            loginRef,
                                            login,
                                            publicUsername,
                                            choiceDriver,
                                            choiceTeam,
                                            uri.toString()
                                    );

                                    GlideApp.with(this)
                                            .load(uri)
                                            .circleCrop()
                                            .into(avatarImage);
                                })
                                .addOnFailureListener(this::registrationFailed)
                )
                .addOnFailureListener(this::registrationFailed);
    }

    private void saveProfile(
            FirebaseUser user,
            DatabaseReference loginRef,
            String login,
            String publicUsername,
            String choiceDriver,
            String choiceTeam,
            String avatarUrl) {

        String uid = user.getUid();

        Map<String, Object> profile = new HashMap<>();
        profile.put("userId", uid);
        profile.put("login", login);
        profile.put("username", publicUsername);
        profile.put("userEmail", user.getEmail());
        profile.put("choiceDriver", choiceDriver);
        profile.put("choiceTeam", choiceTeam);
        profile.put("usernameMigrationComplete", true);

        // if (avatarUrl != null) {
        //     profile.put("avatarUrl", avatarUrl);
        // }

        Map<String, Object> updates = new HashMap<>();
        updates.put("/users/" + uid, profile);
        updates.put("/loginIndex/" + login.toLowerCase(Locale.ROOT), uid);
        updates.put("/publicProfiles/" + uid + "/username", publicUsername);

        FirebaseDatabase.getInstance()
                .getReference()
                .updateChildren(updates)
                .addOnSuccessListener(unused -> {
                    registrationInProgress = false;
                    setLoading(false);

                    Toast.makeText(
                            this,
                            getString(R.string.reqistration_succ_text),
                            Toast.LENGTH_LONG
                    ).show();

                    startActivity_seasonData(
                            this,
                            logInPageActivity.class,
                            mCurrentSeason
                    );
                    finish();
                })
                .addOnFailureListener(error -> {
                    Log.e("Register", "Profile save failed", error);
                    registrationFailed(error);
                });
    }

    private void setLoading(boolean loading) {
        registerProgress.setVisibility(
                loading ? View.VISIBLE : View.GONE
        );
        registerButton.setVisibility(
                loading ? View.INVISIBLE : View.VISIBLE
        );
        registerButton.setEnabled(!loading);
    }

    private void failRegistration(String message) {
        registrationInProgress = false;
        setLoading(false);
        validateFields();
        updateRegisterButtonState();

        Toast.makeText(
                this,
                message,
                Toast.LENGTH_LONG
        ).show();
    }

    private String getLoginInput() {
        return editTextLogin.getText().toString().trim();
    }

    private String getUsernameInput() {
        return editTextUsername.getText().toString().trim();
    }

    private String getEmailInput() {
        return editTextEmail.getText().toString().trim();
    }

    private String getPasswordInput() {
        return editTextPassword.getText().toString();
    }
    private void registrationFailed(@NonNull Exception error) {
        Log.e("Register", "Registration step failed", error);

        if (isFinishing() || isDestroyed()) {
            return;
        }

        registrationInProgress = false;
        setLoading(false);
        updateRegisterButtonState();

        Toast.makeText(
                this,
                getString(R.string.smth_wrong_text),
                Toast.LENGTH_LONG
        ).show();
    }

}