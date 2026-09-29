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
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.functions.FirebaseFunctions;
import com.shawnlin.numberpicker.NumberPicker;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class predictionFormActivity extends AppCompatActivity {

    private final List<PredictionOptionItem> options = new ArrayList<>();

    private String predictionId;
    private String optionType = "driver";
    private String selectedOptionId;
    private String selectedOptionTitle;

    private boolean hasSavedAnswer = false;

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
        functions = FirebaseFunctions.getInstance("us-central1");

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
        rootRef.child("userPredictions").child(predictionId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (!snapshot.exists()) {
                            statusText.setText("Прогноз не найден");
                            saveButton.setEnabled(false);
                            return;
                        }

                        String title = snapshot.child("title").getValue(String.class);
                        String description = snapshot.child("description").getValue(String.class);
                        String raceName = snapshot.child("raceName").getValue(String.class);
                        String storedOptionType = snapshot.child("optionType")
                                .getValue(String.class);

                        optionType = storedOptionType == null
                                || storedOptionType.trim().isEmpty()
                                ? "driver"
                                : storedOptionType.trim().toLowerCase(Locale.ROOT);

                        predictionTitle.setText(title == null ? "" : title);

                        if (raceName != null && !raceName.isEmpty()) {
                            eventName.setText(raceName);
                        } else if (description != null) {
                            eventName.setText(description);
                        }

                        if (!optionType.equals("driver")
                                && !optionType.equals("constructor")) {
                            statusText.setText("Неизвестный тип варианта");
                            saveButton.setEnabled(false);
                            return;
                        }

                        loadOptions();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        statusText.setText("Ошибка загрузки прогноза");
                        saveButton.setEnabled(false);
                    }
                });
    }

    private void loadOptions() {
        String root = optionType.equals("constructor")
                ? "constructors"
                : "drivers";

        rootRef.child(root).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                options.clear();

                for (DataSnapshot child : snapshot.getChildren()) {
                    PredictionOptionItem item = new PredictionOptionItem();
                    item.setId(child.getKey());

                    if (optionType.equals("constructor")) {
                        item.setId(readString(child, "constructorId", child.getKey()));
                        item.setTitle(readString(child, "name", child.getKey()));
                        options.add(item);
                    } else {
                        String status = readString(child, "status", "");
                        String code = readString(child, "driversCode", child.getKey());
                        String name = readString(child, "driverName", child.getKey());

                        if (!"active".equals(status)) {
                            continue;
                        }

                        item.setId(code);
                        item.setTitle(name);
                        options.add(item);
                    }
                }

                options.sort(Comparator.comparing(
                        PredictionOptionItem::getTitle,
                        String.CASE_INSENSITIVE_ORDER
                ));

                if (options.isEmpty()) {
                    answerPicker.setVisibility(View.GONE);
                    saveButton.setEnabled(false);
                    statusText.setText(optionType.equals("constructor")
                            ? "Нет доступных команд"
                            : "Нет доступных пилотов");
                    return;
                }

                setupOptionPicker();
                loadSavedAnswer();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                saveButton.setEnabled(false);
                statusText.setText(optionType.equals("constructor")
                        ? "Не удалось загрузить команды"
                        : "Не удалось загрузить пилотов");
            }
        });
    }

    private String readString(DataSnapshot snapshot, String child, String fallback) {
        String value = snapshot.child(child).getValue(String.class);
        return value == null || value.trim().isEmpty() ? fallback : value;
    }

    private void setupOptionPicker() {
        String[] titles = new String[options.size()];

        for (int i = 0; i < options.size(); i++) {
            titles[i] = options.get(i).getTitle();
        }

        answerPicker.setDisplayedValues(null);
        answerPicker.setMinValue(0);
        answerPicker.setMaxValue(options.size() - 1);
        answerPicker.setDisplayedValues(titles);
        answerPicker.setWrapSelectorWheel(true);
        answerPicker.setValue(0);
        answerPicker.setOnValueChangedListener((picker, oldValue, newValue) ->
                updateSelectedOption(newValue)
        );

        updateSelectedOption(0);
    }

    private void updateSelectedOption(int index) {
        if (index < 0 || index >= options.size()) {
            return;
        }

        PredictionOptionItem item = options.get(index);
        selectedOptionId = item.getId();
        selectedOptionTitle = item.getTitle();
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

                        userPredictionAnswer answer = snapshot.getValue(
                                userPredictionAnswer.class
                        );

                        if (answer == null) {
                            return;
                        }

                        hasSavedAnswer = true;

                        if (answer.getOptionId() != null) {
                            selectSavedOption(answer.getOptionId());
                        }

                        renderSavedAnswer(answer);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        statusText.setText("Не удалось загрузить ваш прогноз");
                    }
                });
    }

    private void selectSavedOption(String savedOptionId) {
        for (int i = 0; i < options.size(); i++) {
            if (savedOptionId.equals(options.get(i).getId())) {
                answerPicker.setValue(i);
                updateSelectedOption(i);
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
            SimpleDateFormat formatter = new SimpleDateFormat(
                    "dd.MM.yyyy HH:mm",
                    Locale.getDefault()
            );
            text.append("\nВыбран: ")
                    .append(formatter.format(new java.util.Date(
                            answer.getSubmittedAt()
                    )));
        }

        if (answer.getPoints() != null) {
            text.append("\nОчки: ").append(answer.getPoints());
        } else {
            text.append("\nРезультат ещё не определён");
        }

        savedAnswerText.setText(text.toString());
    }

    private void savePrediction() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user == null) {
            statusText.setText("Войдите в аккаунт");
            return;
        }

        if (selectedOptionId == null || selectedOptionTitle == null) {
            statusText.setText(optionType.equals("constructor")
                    ? "Выберите команду"
                    : "Выберите пилота");
            return;
        }

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

    private static class PredictionOptionItem {
        private String id;
        private String title;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }
    }
}
