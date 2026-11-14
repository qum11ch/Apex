package com.example.f1app;

import static com.example.f1app.MainActivity.checkConnection;
import static com.example.f1app.MainActivity.getStringByName;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.pm.PackageInfoCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class neuroPredictionActivity extends AppCompatActivity {
    private RadioGroup radioGroup;
    private Button predictButton;

    private boolean checkingResultAccess = false;

    private TextView selectedRaceText;
    private View racePickerField;

    private final ArrayList<String> raceNames = new ArrayList<>();
    private final ArrayList<String> raceLabels = new ArrayList<>();

    private int selectedRaceIndex = -1;

    private static final String STATE_SELECTED_GP = "selected_gp";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.neuro_prediction_activity);

        setupNeuroPredictInsets();
        EdgeInsets.applyBottom(findViewById(R.id.scrollView));

        if (!checkConnection(getApplicationContext())){
            startActivity(connectionLostScreen.createShowSplashOnNetworkFailure(neuroPredictionActivity.this));
        }else{
            startActivity(connectionLostScreen.createIntentHideSplashOnNetworkRecovery(neuroPredictionActivity.this));
        }

        predictButton = findViewById(R.id.predict_button);
        radioGroup = findViewById(R.id.radioGroup);
        predictButton = findViewById(R.id.predict_button);

        String currentSeason = getIntent().getStringExtra("currentSeason");

        WindowInsetsControllerCompat windowInsetsController =
                WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        windowInsetsController.setAppearanceLightStatusBars(false);

        ImageButton backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> finish());

        selectedRaceText = findViewById(R.id.selectedRaceText);
        racePickerField = findViewById(R.id.racePickerField);

        TextView seasonText = findViewById(R.id.seasonText);
        seasonText.setText(getString(
                R.string.neuro_ui_season,
                currentSeason == null ? "—" : currentSeason
        ));

        findViewById(R.id.aboutPrediction).setOnClickListener(v ->
                new neuroPredictionInfoSheet().show(
                        getSupportFragmentManager(),
                        "neuro_prediction_info"
                )
        );

        racePickerField.setEnabled(false);
        predictButton.setEnabled(false);

        getSupportFragmentManager().setFragmentResultListener(
                neuroRacePickerSheet.RESULT_KEY,
                this,
                (key, result) -> {
                    int index = result.getInt(
                            neuroRacePickerSheet.INDEX,
                            -1
                    );

                    if (index < 0 || index >= raceNames.size()) {
                        return;
                    }

                    selectedRaceIndex = index;
                    selectedRaceText.setText(raceLabels.get(index));
                }
        );

        racePickerField.setOnClickListener(v -> {
            if (raceLabels.isEmpty()) return;

            if (getSupportFragmentManager()
                    .findFragmentByTag("neuro_races") != null) {
                return;
            }

            neuroRacePickerSheet
                    .create(raceLabels, selectedRaceIndex)
                    .show(getSupportFragmentManager(), "neuro_races");
        });

        findViewById(R.id.aboutPrediction).setOnClickListener(v -> {
            if (getSupportFragmentManager()
                    .findFragmentByTag("neuro_info") != null) {
                return;
            }

            new neuroPredictionInfoSheet().show(
                    getSupportFragmentManager(),
                    "neuro_info"
            );
        });

        predictButton.setOnClickListener(v -> {
            if (checkingResultAccess) return;

            if (selectedRaceIndex < 0
                    || selectedRaceIndex >= raceNames.size()) {
                return;
            }

            int selectedId = radioGroup.getCheckedRadioButtonId();

            if (selectedId == -1) {
                Toast.makeText(
                        this,
                        R.string.neuro_prediction_missing_option,
                        Toast.LENGTH_SHORT
                ).show();
                return;
            }

            RadioButton radioButton = findViewById(selectedId);
            String event = String.valueOf(radioButton.getTag());
            String gpName = raceNames.get(selectedRaceIndex);

            checkVersionAndOpenResult(event, gpName, currentSeason);
        });

        if (currentSeason == null || currentSeason.trim().isEmpty()) {
            selectedRaceText.setText(R.string.neuro_ui_gp_error);
            return;
        }

        final String restoredGp = savedInstanceState == null
                ? null
                : savedInstanceState.getString(STATE_SELECTED_GP);

        DatabaseReference rootRef =
                FirebaseDatabase.getInstance().getReference();

        rootRef.child("schedule")
                .child("season")
                .child(currentSeason)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (isFinishing() || isDestroyed()) return;

                        raceNames.clear();
                        raceLabels.clear();

                        for (DataSnapshot raceSnapshot : snapshot.getChildren()) {
                            String raceName = raceSnapshot.getKey();

                            if (raceName == null) continue;

                            if ("Australian Grand Prix".equals(raceName)) {
                                continue;
                            }

                            String resourceName = raceName
                                    .toLowerCase(java.util.Locale.ROOT)
                                    .replace(" ", "_") + "_text";

                            int titleId = getStringByName(resourceName);

                            String label = titleId == 0
                                    ? raceName
                                    : getString(titleId);

                            raceNames.add(raceName);
                            raceLabels.add(label);
                        }

                        if (raceNames.isEmpty()) {
                            selectedRaceText.setText(R.string.neuro_ui_gp_error);
                            return;
                        }

                        int restoredIndex = raceNames.indexOf(restoredGp);
                        selectedRaceIndex = restoredIndex >= 0
                                ? restoredIndex
                                : 0;

                        selectedRaceText.setText(
                                raceLabels.get(selectedRaceIndex)
                        );

                        racePickerField.setEnabled(true);
                        predictButton.setEnabled(true);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        if (isFinishing() || isDestroyed()) return;

                        Log.e("NeuroPrediction", error.getMessage());

                        selectedRaceText.setText(R.string.neuro_ui_gp_error);
                        racePickerField.setEnabled(false);
                        predictButton.setEnabled(false);
                    }
                });
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        if (selectedRaceIndex >= 0
                && selectedRaceIndex < raceNames.size()) {
            outState.putString(
                    STATE_SELECTED_GP,
                    raceNames.get(selectedRaceIndex)
            );
        }

        super.onSaveInstanceState(outState);
    }

    private void setupNeuroPredictInsets() {
        View root = findViewById(R.id.neuro_predict_root);
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

    private void checkVersionAndOpenResult(
            String event,
            String gpName,
            String currentSeason) {

        if (checkingResultAccess) return;

        checkingResultAccess = true;
        predictButton.setEnabled(false);

        FirebaseDatabase.getInstance()
                .getReference("appConfig/android/neuroPredictionMinVersionCode")
                .get()
                .addOnCompleteListener(task -> {
                    checkingResultAccess = false;

                    if (isFinishing() || isDestroyed()) return;

                    predictButton.setEnabled(true);

                    if (!task.isSuccessful()) {
                        Log.e(
                                "NeuroVersion",
                                "Не удалось проверить минимальную версию",
                                task.getException()
                        );

                        Toast.makeText(
                                this,
                                getString(R.string.update_reqired_version_task_error),
                                Toast.LENGTH_LONG
                        ).show();
                        return;
                    }

                    Object rawValue = task.getResult().getValue();

                    if (!(rawValue instanceof Number)) {
                        Toast.makeText(
                                this,
                                getString(R.string.neuro_prediction_access_blocked),
                                Toast.LENGTH_LONG
                        ).show();
                        return;
                    }

                    long minVersionCode = ((Number) rawValue).longValue();

                    if (minVersionCode < 1) {
                        Toast.makeText(
                                this,
                                getString(R.string.neuro_prediction_access_blocked),
                                Toast.LENGTH_LONG
                        ).show();
                        return;
                    }

                    try {
                        PackageInfo packageInfo =
                                getPackageManager().getPackageInfo(
                                        getPackageName(),
                                        0
                                );

                        long installedVersionCode =
                                PackageInfoCompat.getLongVersionCode(packageInfo);

                        if (installedVersionCode < minVersionCode) {
                            showUpdateRequiredDialog();
                            return;
                        }

                        openPredictionResult(event, gpName, currentSeason);

                    } catch (PackageManager.NameNotFoundException e) {
                        Log.e(
                                "NeuroVersion",
                                "Не удалось прочитать версию",
                                e
                        );

                        Toast.makeText(
                                this,
                                getString(R.string.update_required_version_error),
                                Toast.LENGTH_LONG
                        ).show();
                    }
                });
    }

    private void showUpdateRequiredDialog() {
        if (isFinishing() || isDestroyed()) return;

        View dialogView = getLayoutInflater().inflate(
                R.layout.dialog_update_required,
                null
        );

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();

        Button updateButton = dialogView.findViewById(R.id.updateButton);
        Button stayButton = dialogView.findViewById(R.id.stayButton);

        updateButton.setOnClickListener(v -> {
            Intent intent = new Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse(
                            "https://www.rustore.ru/catalog/app/" + getPackageName()
                    )
            );

            try {
                startActivity(intent);
                dialog.dismiss();
            } catch (ActivityNotFoundException e) {
                Toast.makeText(
                        this,
                        getString(R.string.update_required_url_error),
                        Toast.LENGTH_LONG
                ).show();
            }
        });

        stayButton.setOnClickListener(v -> dialog.dismiss());

        // Закрытие диалога оставляет пользователя на текущем экране.
        dialog.setCancelable(true);
        dialog.setCanceledOnTouchOutside(false);

        dialog.show();

        Window window = dialog.getWindow();

        if (window != null) {
            window.setBackgroundDrawable(
                    new ColorDrawable(Color.TRANSPARENT)
            );

            int horizontalMargin = Math.round(
                    24 * getResources().getDisplayMetrics().density
            );

            int screenWidth = getResources()
                    .getDisplayMetrics()
                    .widthPixels;

            window.setLayout(
                    screenWidth - horizontalMargin * 2,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
        }
    }

    private void openPredictionResult(
            String event,
            String gpName,
            String currentSeason) {

        Intent intent = new Intent(
                neuroPredictionActivity.this,
                neuroPredictionResultActivity.class
        );

        intent.putExtra("event", event);
        intent.putExtra("gp", gpName);
        intent.putExtra("currentSeason", currentSeason);

        startActivity(intent);
    }
}
