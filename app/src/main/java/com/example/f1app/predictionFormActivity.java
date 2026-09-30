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

    private final List<predictionOption> options = new ArrayList<>();

    private String predictionType;
    private String optionType;
    private String predictionSeason;
    private Integer predictionRaceRound;

    private String predictionId;
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

                        predictionType = snapshot.child("predictionType")
                                .getValue(String.class);

                        optionType = snapshot.child("optionType").getValue(String.class);

                        String category = snapshot.child("category").getValue(String.class);

                        predictionSeason = snapshot.child("season").getValue(String.class);

                        predictionRaceRound = snapshot.child("raceRound")
                                .getValue(Integer.class);

                        LocalizedText title =
                                snapshot.child("title").getValue(LocalizedText.class);
                        LocalizedText description =
                                snapshot.child("description").getValue(LocalizedText.class);

                        predictionTitle.setText(getLocalizedText(title));

                        if ("weekend".equals(category)
                                && predictionSeason != null
                                && predictionRaceRound != null) {
                            eventName.setText(predictionSeason + " • Раунд " + predictionRaceRound);
                        } else {
                            eventName.setText(getLocalizedText(description));
                        }

                        if (optionType == null || predictionType == null) {
                            statusText.setText(
                                    "Неполная структура прогноза"
                            );
                            saveButton.setEnabled(false);
                            return;
                        }

                        if ("boolean".equals(optionType)) {
                            loadBooleanOptions();
                        } else if (isHeadToHead(predictionType)) {
                            loadHeadToHeadOptions(snapshot);
                        } else if ("constructor".equals(optionType)) {
                            loadConstructors();
                        } else if ("driver".equals(optionType)){
                            loadDrivers();
                        } else{
                            statusText.setText("Неизвестный тип варианта");
                            saveButton.setEnabled(false);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        statusText.setText("Ошибка загрузки прогноза");
                        saveButton.setEnabled(false);
                    }
                });
    }

    private boolean isHeadToHead(String type) {
        return "race_head_to_head".equals(type)
                || "qualifying_head_to_head".equals(type)
                || "team_points_head_to_head".equals(type);
    }

    private void loadDrivers() {
        rootRef.child("drivers")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        options.clear();

                        for (DataSnapshot child : snapshot.getChildren()) {
                            String status = child.child("status")
                                    .getValue(String.class);

                            String id = child.child("driversCode")
                                    .getValue(String.class);

                            String title = child.child("driverName")
                                    .getValue(String.class);

                            if (!"active".equals(status)
                                    || id == null
                                    || title == null) {
                                continue;
                            }

                            options.add(new predictionOption(id, title));
                        }

                        options.sort(Comparator.comparing(
                                predictionOption::getTitle,
                                String.CASE_INSENSITIVE_ORDER
                        ));

                        setupOptionPicker();
                        loadSavedAnswer();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        statusText.setText("Не удалось загрузить пилотов");
                        saveButton.setEnabled(false);
                    }
                });
    }

    private void loadConstructors() {
        rootRef.child("constructors")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        options.clear();

                        for (DataSnapshot child : snapshot.getChildren()) {
                            String id = child.child("constructorId")
                                    .getValue(String.class);

                            String title = child.child("name")
                                    .getValue(String.class);

                            if (id == null || title == null) {
                                continue;
                            }

                            options.add(new predictionOption(id, title));
                        }

                        options.sort(Comparator.comparing(
                                predictionOption::getTitle,
                                String.CASE_INSENSITIVE_ORDER
                        ));

                        setupOptionPicker();
                        loadSavedAnswer();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        statusText.setText("Не удалось загрузить команды");
                        saveButton.setEnabled(false);
                    }
                });
    }


    private String getLocalizedText(LocalizedText text) {
        if (text == null) {
            return "";
        }

        boolean isRussian = "ru".equals(Locale.getDefault().getLanguage());

        if (isRussian) {
            if (text.getRu() != null && !text.getRu().trim().isEmpty()) {
                return text.getRu();
            }

            return text.getEn() == null ? "" : text.getEn();
        }

        if (text.getEn() != null && !text.getEn().trim().isEmpty()) {
            return text.getEn();
        }

        return text.getRu() == null ? "" : text.getRu();
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

        predictionOption option = options.get(index);
        selectedOptionId = option.getId();
        selectedOptionTitle = option.getTitle();
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
            SimpleDateFormat formatter =
                    new SimpleDateFormat(
                            "dd.MM.yyyy HH:mm",
                            Locale.getDefault()
                    );

            text.append("\nВыбран: ")
                    .append(formatter.format(
                            new java.util.Date(
                                    answer.getSubmittedAt()
                            )
                    ));
        }

        String result = answer.getResult();

        if ("correct".equals(result)) {
            text.append("\nПравильно");
        } else if ("incorrect".equals(result)) {
            text.append("\nНеправильно");
        } else if ("tie".equals(result)) {
            text.append("\nНичья — начислена половина очков");
        } else {
            text.append("\nРезультат ещё не определён");
        }

        if (answer.getPoints() != null) {
            text.append("\nОчки: ")
                    .append(answer.getPoints());
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
            if ("boolean".equals(optionType)) {
                statusText.setText("Выберите Да или Нет");
            } else if (isHeadToHead(predictionType)) {
                statusText.setText("Выберите одного из двух пилотов");
            } else if ("constructor".equals(optionType)) {
                statusText.setText("Выберите команду");
            } else {
                statusText.setText("Выберите пилота");
            }
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

    private void loadHeadToHeadOptions(DataSnapshot snapshot) {
        options.clear();

        String driverAId = snapshot.child("driverAId")
                .getValue(String.class);

        String driverATitle = snapshot.child("driverATitle")
                .getValue(String.class);

        String driverBId = snapshot.child("driverBId")
                .getValue(String.class);

        String driverBTitle = snapshot.child("driverBTitle")
                .getValue(String.class);

        if (driverAId == null || driverATitle == null
                || driverBId == null || driverBTitle == null) {
            statusText.setText("Некорректное head-to-head событие");
            saveButton.setEnabled(false);
            return;
        }

        options.add(new predictionOption(driverAId, driverATitle));
        options.add(new predictionOption(driverBId, driverBTitle));

        setupOptionPicker();
        loadSavedAnswer();
    }

    private void loadBooleanOptions() {
        options.clear();

        options.add(new predictionOption("yes", getString(R.string.yes_text)));
        options.add(new predictionOption("no", getString(R.string.no_text)));

        setupOptionPicker();
        loadSavedAnswer();
    }
}
