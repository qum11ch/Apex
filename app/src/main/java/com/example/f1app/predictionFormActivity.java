package com.example.f1app;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import com.shawnlin.numberpicker.NumberPicker;
import com.google.firebase.functions.FirebaseFunctions;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class predictionFormActivity extends AppCompatActivity {

    private List<driverPredictionOption> activeDrivers = new ArrayList<>();

    private String predictionId;
    private String selectedOptionId;
    private String selectedOptionTitle;

    private boolean hasSavedAnswer = false;
    private boolean isLoadingSavedAnswer = false;

    private TextView eventName;
    private TextView predictionTitle;
    private TextView savedAnswerText;
    private TextView statusText;
    private NumberPicker answerPicker;
    private Button saveButton;
    private ProgressBar saveProgress;

    private DatabaseReference rootRef;
    private FirebaseFunctions functions;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_prediction_form);

        predictionId = getIntent().getStringExtra("predictionId");
        eventName = findViewById(R.id.eventName);
        predictionTitle = findViewById(R.id.prediction_title);
        savedAnswerText = findViewById(R.id.savedAnswerText);
        statusText = findViewById(R.id.statusText);
        answerPicker = findViewById(R.id.answer_picker);
        saveButton = findViewById(R.id.saveButton);
        saveProgress = findViewById(R.id.saveProgress);

        rootRef = FirebaseDatabase.getInstance().getReference();
        functions = FirebaseFunctions.getInstance();

        ImageButton backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> finish());
        saveButton.setOnClickListener(v -> savePrediction());

        if (predictionId == null || predictionId.trim().isEmpty()) {
            statusText.setText("Идентификатор прогноза отсутствует");

            saveButton.setEnabled(false);
            return;
        }

        loadPrediction();
    }

    private void loadPrediction() {
        rootRef.child("userPredictions").child(predictionId).addListenerForSingleValueEvent(
                new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (!snapshot.exists()) {
                            statusText.setText("Прогноз не найден");

                            saveButton.setEnabled(false);
                            return;
                        }
                        String title = snapshot.child("title").getValue(String.class);

                        String description = snapshot.child("description").getValue(String.class);

                        String category = snapshot.child("category").getValue(String.class);

                        String raceName = snapshot.child("raceName").getValue(String.class);

                        predictionTitle.setText(title == null ? "" : title);

                        if (raceName != null && !raceName.isEmpty()) {
                            eventName.setText(raceName);
                        } else if (description != null) {eventName.setText(description);}

                        loadActiveDrivers();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        statusText.setText("Ошибка загрузки прогноза");
                    }
                });
    }

    private void loadActiveDrivers() {
        rootRef.child("drivers").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                activeDrivers.clear();

                for (DataSnapshot child : snapshot.getChildren()) {
                    driverPredictionOption driver = child.getValue(driverPredictionOption.class);
                    if (driver == null) {
                        continue;
                    }
                    if (driver.getDriverName() == null) {
                        driver.setDriverName(child.getKey());
                    }
                    if (driver.isAvailableForPrediction()) {
                        activeDrivers.add(driver);
                    }
                }

                Collections.sort(activeDrivers,
                        Comparator.comparing(
                                driverPredictionOption
                                        ::getDriverName,
                                String.CASE_INSENSITIVE_ORDER
                        )
                );

                setupDriverPicker();
                loadSavedAnswer();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                statusText.setText("Не удалось загрузить пилотов");
                saveButton.setEnabled(false);

            }
        });
    }

    private void setupDriverPicker() {
        if (activeDrivers.isEmpty()) {
            answerPicker.setVisibility(View.GONE);
            statusText.setText("Нет доступных пилотов");
            saveButton.setEnabled(false);
            return;
        }

        String[] driverNames = new String[activeDrivers.size()];

        for (int i = 0; i < activeDrivers.size(); i++) {
            driverNames[i] = activeDrivers.get(i).getDriverName();
        }

        answerPicker.setDisplayedValues(null);
        answerPicker.setMinValue(0);

        answerPicker.setMaxValue(activeDrivers.size() - 1);

        answerPicker.setDisplayedValues(driverNames);

        answerPicker.setWrapSelectorWheel(true);
        answerPicker.setValue(0);

        updateSelectedDriver(0);

        answerPicker.setOnValueChangedListener((picker, oldValue, newValue)
                -> updateSelectedDriver(newValue)
        );
    }

    private void updateSelectedDriver(int index) {
        if (index < 0 || index >= activeDrivers.size()) {
            return;
        }

        driverPredictionOption driver = activeDrivers.get(index);
        selectedOptionId = driver.getDriversCode();
        selectedOptionTitle = driver.getDriverName();
        statusText.setText(selectedOptionTitle);
    }

    private void loadSavedAnswer() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user == null) {
            statusText.setText("Войдите в аккаунт");
            saveButton.setEnabled(false);
            return;
        }

        rootRef.child("userPredictionAnswers")
                .child(user.getUid())
                .child(predictionId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (!snapshot.exists()) {
                            hasSavedAnswer = false;
                            savedAnswerText.setVisibility(View.GONE);
                            return;
                        }
                        userPredictionAnswer answer = snapshot.getValue(userPredictionAnswer.class);
                        if (answer == null) {
                            return;
                        }
                        hasSavedAnswer = true;
                        if (answer.getOptionId() != null) {
                            selectSavedDriver(answer.getOptionId());
                        }
                        renderSavedAnswer(answer);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        statusText.setText("Не удалось загрузить ваш прогноз");
                    }
                });
    }

    private void selectSavedDriver(String savedOptionId) {
        for (int i = 0; i < activeDrivers.size(); i++) {
            driverPredictionOption driver = activeDrivers.get(i);

            if (savedOptionId.equals(driver.getDriversCode())) {
                answerPicker.setValue(i);
                updateSelectedDriver(i);
                return;
            }
        }
    }

    private void renderSavedAnswer(userPredictionAnswer answer) {
        savedAnswerText.setVisibility(View.VISIBLE);

        StringBuilder text = new StringBuilder();

        text.append("Ваш текущий прогноз: ")
                .append(answer.getOptionTitle() == null
                        ? answer.getOptionId()
                        : answer.getOptionTitle());

        if (answer.getSubmittedAt() != null) {
            SimpleDateFormat formatter = new SimpleDateFormat("dd.MM.yyyy HH:mm",
                    Locale.getDefault());

            text.append("\nВыбран: ").append(formatter.format(new Date(answer.getSubmittedAt())));
        }

        if (answer.getPoints() != null) {
            text.append("\nОчки: ").append(answer.getPoints());
        } else {text.append("\nРезультат ещё не определён");}

        savedAnswerText.setText(text.toString());
    }

    private void savePrediction() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user == null) {
            statusText.setText("Войдите в аккаунт");
            return;
        }

        if (selectedOptionId == null) {
            statusText.setText("Выберите пилота");
            return;
        }

        setSavingState(true);

        // Map<String, Object> updates = new HashMap<>();

        // updates.put("optionId", selectedOptionId);
        // updates.put("optionTitle", selectedOptionTitle);
        // updates.put("updatedAt", ServerValue.TIMESTAMP);

        // if (!hasSavedAnswer) {
        //     updates.put("submittedAt", ServerValue.TIMESTAMP);
        //     updates.put("points", null);
        //     updates.put("result", "pending");
        // }

        // rootRef.child("userPredictionAnswers")
        //                 .child(user.getUid())
        //                 .child(predictionId)
        //                 .updateChildren(updates)
        //                 .addOnSuccessListener(unused -> {
        //                 hasSavedAnswer = true;
        //
        //                 setSavingState(false);
        //                 statusText.setText("Прогноз сохранён: " + selectedOptionTitle);
        //
        //                 loadSavedAnswer();
        //             })
        //                 .addOnFailureListener(error -> {
        //                 setSavingState(false);
        //
        //                 statusText.setText("Ошибка сохранения: " + error.getMessage());
        //             });

        Map<String, Object> data = new HashMap<>();

        data.put("predictionId", predictionId);
        data.put("optionId", selectedOptionId);
        data.put("optionTitle", selectedOptionTitle);
        setSavingState(true);

        functions.getHttpsCallable("submitUserPrediction")
                .call(data)
                .addOnSuccessListener(result -> {
                    setSavingState(false);

                    hasSavedAnswer = true;
                    statusText.setText("Прогноз сохранён");
                    loadSavedAnswer();
                })
                .addOnFailureListener(error -> {
                    setSavingState(false);

                    statusText.setText(error.getMessage());
                });
    }

    private void setSavingState(boolean saving) {
        saveButton.setEnabled(!saving);
        answerPicker.setEnabled(!saving);

        saveProgress.setVisibility(saving ? View.VISIBLE : View.INVISIBLE);
    }
}