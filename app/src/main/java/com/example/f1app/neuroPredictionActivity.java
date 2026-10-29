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
import com.shawnlin.numberpicker.NumberPicker;
import androidx.activity.EdgeToEdge;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.pm.PackageInfoCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;


import java.util.ArrayList;

public class neuroPredictionActivity extends AppCompatActivity {
    private NumberPicker numberPicker;
    private RadioGroup radioGroup;
    private Button predictButton;

    private boolean checkingResultAccess = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.neuro_predict_activity);

        setupNeuroPredictInsets();

        if (!checkConnection(getApplicationContext())){
            startActivity(connectionLostScreen.createShowSplashOnNetworkFailure(neuroPredictionActivity.this));
        }else{
            startActivity(connectionLostScreen.createIntentHideSplashOnNetworkRecovery(neuroPredictionActivity.this));
        }

        numberPicker = findViewById(R.id.numberpicker);
        predictButton = findViewById(R.id.predict_button);
        radioGroup = findViewById(R.id.radioGroup);
        predictButton = findViewById(R.id.predict_button);

        String currentSeason = getIntent().getStringExtra("currentSeason");

        WindowInsetsControllerCompat windowInsetsController =
                WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        windowInsetsController.setAppearanceLightStatusBars(false);

        ImageButton backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> finish());

        DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference();
        rootRef.child("/schedule/season/" + currentSeason)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        ArrayList<String> raceListLocalized = new ArrayList<>();
                        ArrayList<String> raceList = new ArrayList<>();
                        long raceCount = snapshot.getChildrenCount();

                        numberPicker.setMinValue(0);
                        numberPicker.setMaxValue((int) raceCount);
                        numberPicker.setWrapSelectorWheel(true);

                        for (DataSnapshot raceSnaps: snapshot.getChildren()){
                            String raceName = raceSnaps.getKey();

                            if (!raceName.equals("Australian Grand Prix")){
                                String preprocessedGPName = raceName.toLowerCase();
                                preprocessedGPName = preprocessedGPName.replace(" ", "_");
                                String localizedGpName = getString(getStringByName(preprocessedGPName + "_text"));

                                raceListLocalized.add(localizedGpName);
                                raceList.add(raceName);
                            }
                        }

                        String[] values = raceListLocalized.toArray(new String[0]);
                        numberPicker.setMinValue(0);
                        numberPicker.setMaxValue(values.length - 1);
                        numberPicker.setDisplayedValues(values);

                        predictButton.setOnClickListener(v -> {
                            if (checkingResultAccess) return;

                            int selectedId = radioGroup.getCheckedRadioButtonId();

                            if (selectedId == -1) {
                                Toast.makeText(
                                        neuroPredictionActivity.this,
                                        getString(R.string.neuro_prediction_missing_option),
                                        Toast.LENGTH_SHORT
                                ).show();
                                return;
                            }

                            RadioButton radioButton = findViewById(selectedId);

                            String event = radioButton.getTag().toString();
                            String gpName = raceList.get(numberPicker.getValue());

                            checkVersionAndOpenResult(event, gpName, currentSeason);
                        });
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e("predictPageActivity", error.getMessage());
                    }
                });
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
                    rootBottom + safeInsets.bottom
            );

            header.setPadding(
                    headerLeft,
                    headerTop + safeInsets.top,
                    headerRight,
                    headerBottom
            );

            return WindowInsetsCompat.CONSUMED;
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
