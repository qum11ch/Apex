package com.example.f1app;

import static com.example.f1app.MainActivity.checkConnection;
import static com.example.f1app.MainActivity.checkLightTheme;
import static com.example.f1app.userPredictionAdapter.formatPoints;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.ContextThemeWrapper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.functions.FirebaseFunctions;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.shawnlin.numberpicker.NumberPicker;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class predictionFormActivity extends AppCompatActivity {
    private final List<predictionOption> headToHeadOptions = new ArrayList<>();
    private final List<View> headToHeadCardViews = new ArrayList<>();
    private final List<predictionOption> options = new ArrayList<>();

    private FirebaseStorage storage;

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
    private TextView savedAnswer;
    private TextView submittedAt;
    private TextView eventDeadline;
    private TextView answerResult;
    private TextView submittedAtInfo;
    private TextView pointsEarnedInfo;
    private TextView pointsEarned;
    private LinearLayout statusLayout;
    private NumberPicker answerPicker;
    private Button saveButton;
    private ProgressBar saveProgress;
    private TextView points;

    private DatabaseReference rootRef;
    private FirebaseFunctions functions;

    private LinearLayout twoOptionsContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.prediction_form_activity);

        setupPredictionFormInsets();

        if (!checkConnection(getApplicationContext())){
            startActivity(connectionLostScreen.createShowSplashOnNetworkFailure(predictionFormActivity.this));
        }else{
            startActivity(connectionLostScreen.createIntentHideSplashOnNetworkRecovery(predictionFormActivity.this));
        }

        WindowInsetsControllerCompat windowInsetsController =
                WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        windowInsetsController.setAppearanceLightStatusBars(false);

        CoordinatorLayout page = findViewById(R.id.mainLayout);
        if (!checkLightTheme(predictionFormActivity.this)){
            page.setBackground(ContextCompat.getDrawable(predictionFormActivity.this, R.drawable.black_gradient));
        }

        predictionId = getIntent().getStringExtra("predictionId");

        eventName = findViewById(R.id.eventName);
        predictionTitle = findViewById(R.id.prediction_title);
        savedAnswer = findViewById(R.id.savedAnswer);
        submittedAt = findViewById(R.id.submittedAt);
        eventDeadline = findViewById(R.id.eventDeadline);
        answerResult = findViewById(R.id.answerResult);
        statusLayout = findViewById(R.id.statusLayout);
        pointsEarned = findViewById(R.id.pointsEarned);
        pointsEarnedInfo = findViewById(R.id.pointsEarnedInfo);
        submittedAtInfo = findViewById(R.id.submittedAtInfo);
        answerPicker = findViewById(R.id.answer_picker);
        saveButton = findViewById(R.id.saveButton);
        saveProgress = findViewById(R.id.saveProgress);
        points = findViewById(R.id.points);

        rootRef = FirebaseDatabase.getInstance().getReference();
        functions = FirebaseFunctions.getInstance("us-central1");

        ImageButton backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> finish());
        saveButton.setOnClickListener(v -> savePrediction(predictionFormActivity.this));

        twoOptionsContainer = findViewById(R.id.twoOptionsContainer);

        storage = FirebaseStorage.getInstance();

        answerPicker.setVisibility(View.GONE);
        twoOptionsContainer.setVisibility(View.GONE);

        if (predictionId == null || predictionId.trim().isEmpty()) {
            Toast.makeText(this, getString(R.string.prediction_null_id), Toast.LENGTH_LONG).show();
            saveButton.setEnabled(false);
            return;
        }

        int backgroundColor = Color.TRANSPARENT;
        int strokeColor = ContextCompat.getColor(this, R.color.white);

        GradientDrawable drawable = (GradientDrawable) points.getBackground().mutate();
        drawable.setColor(backgroundColor);
        drawable.setStroke(dpToPx(1), strokeColor);
        points.setBackground(drawable);

        loadPrediction(predictionFormActivity.this);
    }

    private void setupPredictionFormInsets() {
        View root = findViewById(R.id.mainLayout);
        View header = findViewById(R.id.header);
        View savePanel = findViewById(R.id.saveLayout);
        View scrollContent = findViewById(R.id.scrollContent);

        final int rootLeft = root.getPaddingLeft();
        final int rootTop = root.getPaddingTop();
        final int rootRight = root.getPaddingRight();

        final int headerLeft = header.getPaddingLeft();
        final int headerTop = header.getPaddingTop();
        final int headerRight = header.getPaddingRight();
        final int headerBottom = header.getPaddingBottom();

        final int contentLeft = scrollContent.getPaddingLeft();
        final int contentTop = scrollContent.getPaddingTop();
        final int contentRight = scrollContent.getPaddingRight();
        final int contentBottom = scrollContent.getPaddingBottom();

        final int panelGap = Math.round(
                8 * getResources().getDisplayMetrics().density
        );

        Runnable updateScrollReserve = () -> {
            int reserve = contentBottom;

            if (savePanel.getVisibility() != View.GONE) {
                ViewGroup.MarginLayoutParams params =
                        (ViewGroup.MarginLayoutParams) savePanel.getLayoutParams();

                reserve += savePanel.getHeight() + params.bottomMargin;
            }

            if (scrollContent.getPaddingBottom() != reserve) {
                scrollContent.setPadding(
                        contentLeft,
                        contentTop,
                        contentRight,
                        reserve
                );
            }
        };

        savePanel.addOnLayoutChangeListener(
                (view, left, top, right, bottom,
                 oldLeft, oldTop, oldRight, oldBottom) ->
                        updateScrollReserve.run()
        );

        ViewCompat.setOnApplyWindowInsetsListener(root, (view, windowInsets) -> {
            Insets safeInsets = windowInsets.getInsets(
                    WindowInsetsCompat.Type.systemBars()
                            | WindowInsetsCompat.Type.displayCutout()
            );

            view.setPadding(
                    rootLeft + safeInsets.left,
                    rootTop,
                    rootRight + safeInsets.right,
                    0
            );

            header.setPadding(
                    headerLeft,
                    headerTop + safeInsets.top,
                    headerRight,
                    headerBottom
            );

            ViewGroup.MarginLayoutParams params =
                    (ViewGroup.MarginLayoutParams) savePanel.getLayoutParams();

            int bottomMargin = safeInsets.bottom + panelGap;

            if (params.bottomMargin != bottomMargin) {
                params.bottomMargin = bottomMargin;
                savePanel.setLayoutParams(params);
            }

            updateScrollReserve.run();

            return WindowInsetsCompat.CONSUMED;
        });

        ViewCompat.requestApplyInsets(root);
        savePanel.post(updateScrollReserve);
    }

    private void loadPrediction(Context context) {
        rootRef.child("userPredictions").child(predictionId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (!snapshot.exists()) {
                            Toast.makeText(
                                    context,
                                    getString(R.string.prediction_not_found),
                                    Toast.LENGTH_LONG).show();
                            saveButton.setEnabled(false);
                            return;
                        }

                        predictionType = snapshot.child("predictionType")
                                .getValue(String.class);

                        optionType = snapshot.child("optionType").getValue(String.class);
                        predictionSeason = snapshot.child("season").getValue(String.class);
                        predictionRaceRound = snapshot.child("raceRound").getValue(Integer.class);

                        LocalizedText title =
                                snapshot.child("title").getValue(LocalizedText.class);

                        predictionTitle.setText(getLocalizedText(title));

                        Long deadline = snapshot.child("deadline").getValue(Long.class);

                        SimpleDateFormat formatter =
                                new SimpleDateFormat(
                                        "dd.MM.yyyy HH:mm",
                                        Locale.getDefault()
                                );

                        String deadlineText = getString(R.string.prediction_deadline_text) +
                                " " + formatter.format(new java.util.Date(deadline));

                        eventDeadline.setText(deadlineText);

                        eventName.setText(getMetaText(predictionFormActivity.this, snapshot));

                        Double pointsText = snapshot.child("points").getValue(Double.class);

                        String pointsString = formatPoints(pointsText)
                                + " " + context.getString(R.string.pts_header);
                        points.setText(pointsString);

                        if (optionType == null || predictionType == null) {
                            Toast.makeText(
                                    context,
                                    getString(R.string.predictions_broken_structure),
                                    Toast.LENGTH_LONG).show();
                            saveButton.setEnabled(false);
                            return;
                        }

                        if (isBooleanPrediction(predictionType, optionType)) {
                            loadBooleanOptions(context);
                        } else if (isHeadToHead(predictionType)) {
                            loadHeadToHeadOptions(context, snapshot);
                        } else if ("constructor".equals(optionType)
                                && ("champion".equals(predictionType)
                                || "winner".equals(predictionType))) {
                            loadConstructors(context);
                        } else if ("driver".equals(optionType)
                                && ("champion".equals(predictionType)
                                || "winner".equals(predictionType))) {
                            loadDrivers(context);
                        } else {
                            Toast.makeText(
                                    context,
                                    getString(R.string.prediction_unknown_option_type),
                                    Toast.LENGTH_LONG).show();
                            saveButton.setEnabled(false);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(
                                context,
                                getString(R.string.prediction_load_error),
                                Toast.LENGTH_LONG).show();
                        saveButton.setEnabled(false);
                    }
                });
    }

    private boolean isBooleanPrediction(String predictionType, String optionType) {
        return "boolean".equals(optionType)
                && ("boolean_prediction".equals(predictionType)
                        || "safety_car".equals(predictionType)
                        || "rain".equals(predictionType)
        );
    }

    private boolean isHeadToHead(String type) {
        return "race_head_to_head".equals(type)
                || "qualifying_head_to_head".equals(type)
                || "team_points_head_to_head".equals(type);
    }

    private void loadDrivers(Context context) {
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
                        loadSavedAnswer(context);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(
                                context,
                                getString(R.string.prediction_drivers_error),
                                Toast.LENGTH_LONG).show();
                        saveButton.setEnabled(false);
                    }
                });
    }

    private void loadConstructors(Context context) {
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
                        loadSavedAnswer(context);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(
                                context,
                                getString(R.string.prediction_constructors_error),
                                Toast.LENGTH_LONG).show();
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
        twoOptionsContainer.setVisibility(View.GONE);
        answerPicker.setVisibility(View.VISIBLE);

        String[] titles = new String[options.size()];

        for (int i = 0; i < options.size(); i++) {
            titles[i] = options.get(i).getTitle();
        }

        if (options.isEmpty()) {
            Toast.makeText(
                    predictionFormActivity.this,
                    getString(R.string.prediction_unknown_option_type),
                    Toast.LENGTH_LONG).show();
            saveButton.setEnabled(false);
            return;
        }

        answerPicker.setDisplayedValues(null);
        answerPicker.setMinValue(0);
        answerPicker.setMaxValue(options.size() - 1);
        answerPicker.setDisplayedValues(titles);
        answerPicker.setWrapSelectorWheel(true);
        answerPicker.setValue(0);

        answerPicker.setOnValueChangedListener(
                (picker, oldValue, newValue) ->
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
    }

    private void loadSavedAnswer(Context context) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user == null) {
            Toast.makeText(
                    predictionFormActivity.this,
                    getString(R.string.dialog_login),
                    Toast.LENGTH_LONG).show();
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
                            statusLayout.setVisibility(View.GONE);
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

                        renderSavedAnswer(context, answer);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(
                                predictionFormActivity.this,
                                getString(R.string.prediction_load_error),
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void selectSavedOption(String savedOptionId) {
        if (isHeadToHead(predictionType)) {
            for (int i = 0; i < headToHeadOptions.size(); i++) {
                if (savedOptionId.equals(headToHeadOptions.get(i).getId())) {
                    selectHeadToHeadOption(i);
                    return;
                }
            }

            return;
        }

        for (int i = 0; i < options.size(); i++) {
            if (!savedOptionId.equals(options.get(i).getId())) {
                continue;
            }

            if (answerPicker.getVisibility() == View.VISIBLE) {
                answerPicker.setValue(i);
                updateSelectedOption(i);
            } else {
                selectedOptionId = options.get(i).getId();
                selectedOptionTitle = options.get(i).getTitle();
                selectTwoOptionButton(i);
            }

            return;
        }
    }

    private void renderSavedAnswer(Context context, userPredictionAnswer answer) {
        statusLayout.setVisibility(View.VISIBLE);

        String predictionChoiceText = answer.getOptionTitle() == null
                ? answer.getOptionId()
                : answer.getOptionTitle();

        savedAnswer.setText(predictionChoiceText);

        if (answer.getSubmittedAt() != null) {
            SimpleDateFormat formatter =
                    new SimpleDateFormat(
                            "dd.MM.yyyy HH:mm",
                            Locale.getDefault()
                    );

            String submittedText = formatter.format(
                    new java.util.Date(
                            answer.getSubmittedAt()
                    ));

            submittedAt.setText(submittedText);
        }else{
            submittedAtInfo.setVisibility(View.GONE);
            submittedAt.setVisibility(View.GONE);
        }

        String result = answer.getResult();
        String answerResultText;

        if ("correct".equals(result)) {
            answerResultText = getString(R.string.prediction_result_correct);
        } else if ("incorrect".equals(result)) {
            answerResultText = getString(R.string.prediction_result_incorrect);
        } else if ("tie".equals(result)) {
            answerResultText = getString(R.string.prediction_result_tie);
        } else {
            answerResultText = getString(R.string.prediction_result_pending);
        }

        answerResult.setText(answerResultText);

        if (answer.getPoints() != null) {
            String pointsText = formatPoints(answer.getPoints());
            pointsEarned.setText(pointsText);
        }else{
            pointsEarned.setVisibility(View.GONE);
            pointsEarnedInfo.setVisibility(View.GONE);
        }

    }

    private void savePrediction(Context context) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user == null) {
            Toast.makeText(
                    predictionFormActivity.this,
                    getString(R.string.dialog_login),
                    Toast.LENGTH_LONG).show();
            return;
        }

        if (selectedOptionId == null || selectedOptionTitle == null) {
            if ("boolean".equals(optionType)) {
                Toast.makeText(
                        predictionFormActivity.this,
                        getString(R.string.prediction_option_text_boolean),
                        Toast.LENGTH_LONG).show();
            } else if (isHeadToHead(predictionType)) {
                if ("constructor".equals(optionType)){
                    Toast.makeText(
                            predictionFormActivity.this,
                            getString(R.string.prediction_option_text_head_constructors),
                                    Toast.LENGTH_LONG).show();
                }else{
                    Toast.makeText(
                            predictionFormActivity.this,
                            getString(R.string.prediction_option_text_head_drivers),
                            Toast.LENGTH_LONG).show();
                }
            } else if ("constructor".equals(optionType)) {
                Toast.makeText(
                        predictionFormActivity.this,
                        getString(R.string.prediction_option_text_head_constructors),
                        Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(
                        predictionFormActivity.this,
                        getString(R.string.prediction_option_text_head_drivers),
                        Toast.LENGTH_LONG).show();
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
                    Toast.makeText(
                            predictionFormActivity.this,
                            getString(R.string.prediction_save_text),
                            Toast.LENGTH_LONG).show();
                    loadSavedAnswer(context);
                })
                .addOnFailureListener(error -> {
                    setSavingState(false);
                    hasSavedAnswer = true;
                    Toast.makeText(
                            predictionFormActivity.this,
                            error.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }

    private void setSavingState(boolean saving) {
        saveButton.setEnabled(!saving);

        answerPicker.setEnabled(!saving);
        twoOptionsContainer.setEnabled(!saving);

        saveProgress.setVisibility(saving ? View.VISIBLE : View.INVISIBLE);
    }


    private void loadHeadToHeadOptions(Context context, DataSnapshot snapshot) {
        options.clear();

        String sideAId = snapshot.child("sideAId").getValue(String.class);
        String sideATitle = snapshot.child("sideATitle").getValue(String.class);
        String sideBId = snapshot.child("sideBId").getValue(String.class);
        String sideBTitle = snapshot.child("sideBTitle").getValue(String.class);

        if (sideAId == null || sideATitle == null || sideBId == null || sideBTitle == null) {
            Toast.makeText(
                    predictionFormActivity.this,
                    getString(R.string.prediction_head_error),
                    Toast.LENGTH_LONG).show();
            saveButton.setEnabled(false);
            return;
        }

        headToHeadOptions.clear();
        headToHeadOptions.add(new predictionOption(sideAId, sideATitle));
        headToHeadOptions.add(new predictionOption(sideBId, sideBTitle));

        setupHeadToHeadCards(context);
        loadSavedAnswer(context);
    }

    private void setupHeadToHeadCards(Context context) {
        answerPicker.setVisibility(View.GONE);
        twoOptionsContainer.setVisibility(View.VISIBLE);

        twoOptionsContainer.removeAllViews();
        headToHeadCardViews.clear();

        boolean isConstructor = "constructor".equals(optionType);

        twoOptionsContainer.setOrientation(isConstructor
                ? LinearLayout.VERTICAL : LinearLayout.HORIZONTAL);

        for (int i = 0; i < headToHeadOptions.size(); i++) {
            predictionOption option = headToHeadOptions.get(i);
            View card = createHeadToHeadCard(
                    context,
                    option,
                    i
            );

            headToHeadCardViews.add(card);
            twoOptionsContainer.addView(card);

            if (!isConstructor && i == 0 && headToHeadOptions.size() == 2) {
                twoOptionsContainer.addView(createVsView(context));
            }

            if (isConstructor && i == 0 && headToHeadOptions.size() == 2) {
                twoOptionsContainer.addView(createVerticalVsView(context));
            }
        }

        if (!headToHeadOptions.isEmpty()) {
            selectHeadToHeadOption(0);
        }
    }

    private TextView createVerticalVsView(Context context) {
        TextView vs = new TextView(context);

        vs.setText("VS");
        vs.setTextSize(16);
        vs.setTypeface(null, Typeface.BOLD);
        vs.setGravity(Gravity.CENTER);
        vs.setTextColor(ContextCompat.getColor(context, R.color.red));
        vs.setPadding(0, dpToPx(2), 0, dpToPx(2));

        return vs;
    }

    private void selectHeadToHeadOption(int index) {
        if (index < 0 || index >= headToHeadOptions.size()) {
            return;
        }

        predictionOption option = headToHeadOptions.get(index);
        selectedOptionId = option.getId();
        selectedOptionTitle = option.getTitle();

        selectHeadToHeadCard(index);
    }

    private TextView createVsView(Context context) {
        TextView vs = new TextView(context);

        vs.setText("VS");
        vs.setTextSize(16);
        vs.setTypeface(null, Typeface.BOLD);
        vs.setTextColor(ContextCompat.getColor(context, R.color.red));
        vs.setGravity(Gravity.CENTER);

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        dpToPx(25),
                        LinearLayout.LayoutParams.MATCH_PARENT
                );

        vs.setLayoutParams(params);

        return vs;
    }

    private String getMetaText(Context context, DataSnapshot snapshot) {
        StringBuilder text = new StringBuilder();

        String category = snapshot.child("category").getValue(String.class);

        text.append(getCategoryTitle(context, category));

        String season = snapshot.child("season").getValue(String.class);
        Integer raceRound = snapshot.child("raceRound").getValue(Integer.class);
        String predictionType = snapshot.child("predictionType").getValue(String.class);
        String optionType = snapshot.child("optionType").getValue(String.class);

        boolean isWeekend = "weekend".equals(category);

        if (isWeekend && season != null && raceRound != null) {
            String weekendRoundText = " • " + context.getText(R.string.round) + " ";

            text.append(" • ")
                    .append(season)
                    .append(weekendRoundText)
                    .append(raceRound);
        } else if (season != null) {
            text.append(" • ").append(season);
        }

        String typeTitle = getPredictionTypeTitle(
                context,
                predictionType
        );

        if (!typeTitle.isEmpty()) {
            text.append(" • ").append(typeTitle);
        }

        String optionTypeTitle = getOptionTypeTitle(context, optionType);

        if (!optionTypeTitle.isEmpty()) {
            text.append(" • ").append(optionTypeTitle);
        }

        return text.toString();
    }

    private String getOptionTypeTitle(Context context, String optionType) {
        if ("driver".equals(optionType)) {
            return context.getString(R.string.prediction_option_type_driver);
        }

        if ("constructor".equals(optionType)) {
            return context.getString(R.string.prediction_option_type_constructor);
        }

        if ("boolean".equals(optionType)) {
            return context.getString(R.string.prediction_option_type_boolean);
        }

        return "";
    }


    private String getPredictionTypeTitle(Context context, String predictionType) {
        if ("champion".equals(predictionType)) {
            return context.getString(R.string.prediction_type_champion);
        }

        if ("winner".equals(predictionType)) {
            return context.getString(R.string.prediction_type_winner);
        }

        if ("race_head_to_head".equals(predictionType)) {
            return context.getString(R.string.prediction_type_race_head);
        }

        if ("qualifying_head_to_head".equals(predictionType)) {
            return context.getString(R.string.prediction_type_quali_head);
        }

        if ("team_points_head_to_head".equals(predictionType)) {
            return context.getString(R.string.prediction_type_team_points_head);
        }

        if ("safety_car".equals(predictionType)) {
            return context.getString(R.string.prediction_type_safety_car);
        }

        if ("rain".equals(predictionType)) {
            return context.getString(R.string.prediction_type_rain);
        }

        if ("boolean_prediction".equals(predictionType)) {
            return context.getString(R.string.prediction_option_text_boolean);
        }

        return "";
    }

    private String getCategoryTitle(Context context, String category) {
        if ("season".equals(category)) {
            return context.getString(
                    R.string.prediction_category_season
            );
        }

        if ("weekend".equals(category)) {
            return context.getString(
                    R.string.prediction_category_weekend
            );
        }

        return category == null ? "" : category;
    }

    private View createHeadToHeadCard(Context context, predictionOption option, int index) {
        boolean isConstructor = "constructor".equals(optionType);
        LinearLayout card = new LinearLayout(context);
        card.setOrientation(LinearLayout.VERTICAL);

        card.setGravity(Gravity.CENTER);
        card.setMinimumHeight(dpToPx(190));
        card.setPadding(
                dpToPx(8),
                dpToPx(8),
                dpToPx(8),
                dpToPx(8)
        );

        LinearLayout.LayoutParams cardParams;

        if (isConstructor) {
            cardParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );

            cardParams.setMargins(
                    dpToPx(8),
                    dpToPx(8),
                    dpToPx(8),
                    dpToPx(8)
            );
        } else {
            cardParams = new LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f
            );

            cardParams.setMargins(
                    dpToPx(6),
                    dpToPx(6),
                    dpToPx(6),
                    dpToPx(6)
            );
        }

        card.setLayoutParams(cardParams);
        card.setMinimumHeight(
                isConstructor
                        ? dpToPx(100)
                        : dpToPx(190)
        );

        GradientDrawable background = new GradientDrawable();

        background.setColor(getColor(R.color.colorPrimary));
        background.setCornerRadius(dpToPx(16));
        card.setBackground(background);

        ImageView imageView =
                new ImageView(context);

        LinearLayout.LayoutParams imageParams;

        if ("constructor".equals(optionType)) {
            imageParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    dpToPx(120)
            );

            imageParams.setMargins(
                    dpToPx(8),
                    dpToPx(4),
                    dpToPx(8),
                    dpToPx(8)
            );

            imageView.setScaleType(
                    ImageView.ScaleType.FIT_CENTER
            );

            imageView.setAdjustViewBounds(true);
        } else {
            imageParams = new LinearLayout.LayoutParams(
                    dpToPx(110),
                    dpToPx(110)
            );

            imageView.setScaleType(
                    ImageView.ScaleType.CENTER_CROP
            );
        }

        imageView.setLayoutParams(imageParams);
        card.addView(imageView);

        TextView titleView = new TextView(context);

        titleView.setText(option.getTitle());
        titleView.setGravity(Gravity.CENTER);
        titleView.setTextSize(
                "constructor".equals(optionType)
                        ? 18
                        : 14
        );

        titleView.setTypeface(
                null,
                Typeface.BOLD
        );

        titleView.setTextColor(
                ContextCompat.getColor(
                        context,
                        R.color.text_color_main
                )
        );

        card.addView(titleView);

        loadHeadToHeadImage(imageView, option);
        card.setOnClickListener(v -> selectHeadToHeadOption(index));

        return card;
    }

    private void loadHeadToHeadImage(ImageView imageView, predictionOption option) {
        String folder;
        String fileId;

        if ("constructor".equals(optionType)) {
            folder = "teams";
            fileId = option.getId().toLowerCase(Locale.ROOT);
        } else {
            folder = "drivers";
            fileId = option.getId().toLowerCase(Locale.ROOT);
        }

        String fileName = fileId + "_" + predictionSeason + ".png";

        StorageReference imageRef = storage.getReference().child(folder).child(fileName);

        imageRef.getDownloadUrl()
                .addOnSuccessListener(uri ->
                        Glide.with(predictionFormActivity.this)
                                .load(uri)
                                .placeholder(R.drawable.placeholder_driver)
                                .error(R.drawable.placeholder_driver)
                                .into(imageView)
                )
                .addOnFailureListener(error ->
                        imageView.setImageResource(R.drawable.placeholder_driver));
    }

    private void selectHeadToHeadCard(int selectedIndex) {
        for (int i = 0; i < headToHeadCardViews.size(); i++) {
            View card = headToHeadCardViews.get(i);
            GradientDrawable background = new GradientDrawable();

            background.setCornerRadius(dpToPx(16));

            if (i == selectedIndex) {
                background.setColor(ContextCompat.getColor(this,
                        R.color.colorPredicts));

                background.setStroke(dpToPx(2), ContextCompat.getColor(
                        this, R.color.grey));
            } else {
                background.setColor(getColor(R.color.colorPrimary));

                background.setStroke(dpToPx(2), ContextCompat.getColor(
                        this, R.color.stroke_option_color));
            }

            card.setBackground(background);
        }
    }


    private void loadBooleanOptions(Context context) {
        options.clear();
        options.add(new predictionOption("yes", getString(R.string.yes_text)));
        options.add(new predictionOption("no", getString(R.string.no_text)));

        setupTwoOptionButtons(context);
        loadSavedAnswer(context);
    }

    private void setupTwoOptionButtons(Context context) {
        answerPicker.setVisibility(View.GONE);
        twoOptionsContainer.setVisibility(View.VISIBLE);

        twoOptionsContainer.removeAllViews();

        for (predictionOption option : options) {
            Button button = createOptionButton(
                    context,
                    option
            );

            twoOptionsContainer.addView(button);
        }

        if (!options.isEmpty()) {
            updateSelectedOption(0);
            selectTwoOptionButton(0);
        }
    }

    private Button createOptionButton(Context context, predictionOption option) {
        Button button = new Button(context);

        button.setText(option.getTitle());
        button.setTextSize(16);
        button.setAllCaps(false);

        button.setGravity(Gravity.CENTER);

        button.setBackgroundResource(
                R.drawable.bg_prediction_option
        );

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1f
                );

        params.setMargins(
                dpToPx(6),
                dpToPx(6),
                dpToPx(6),
                dpToPx(6)
        );

        button.setLayoutParams(params);

        button.setOnClickListener(v -> {
            int selectedIndex = -1;

            for (int i = 0; i < options.size(); i++) {

                if (options.get(i).getId().equals(option.getId())) {
                    selectedIndex = i;
                    break;
                }
            }

            if (selectedIndex == -1) {
                return;
            }

            updateSelectedOption(selectedIndex);
            selectTwoOptionButton(selectedIndex);
        });

        return button;
    }

    private void selectTwoOptionButton(int selectedIndex) {
        for (int i = 0; i < twoOptionsContainer.getChildCount(); i++) {
            View child = twoOptionsContainer.getChildAt(i);

            if (!(child instanceof Button)) {
                continue;
            }

            Button button = (Button) child;
            boolean selected =  i == selectedIndex;

            button.setSelected(selected);

            if (selected) {
                button.setTextColor(
                        ContextCompat.getColor(
                                this,
                                R.color.text_color_main
                        )
                );
            } else {
                button.setTextColor(getColor(R.color.text_color_thirdly));
            }
        }
    }

    private int dpToPx(int dp) {
        return Math.round(
                dp * getResources().getDisplayMetrics().density
        );
    }
}
