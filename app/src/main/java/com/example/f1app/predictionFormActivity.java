package com.example.f1app;

import static com.example.f1app.MainActivity.checkConnection;
import static com.example.f1app.MainActivity.checkLightTheme;
import static com.example.f1app.userPredictionAdapter.formatPoints;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
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

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
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
    private LinearLayout selectedOptionCard;
    private ImageView selectedOptionImage;
    private TextView selectedOptionName;
    private TextView selectedOptionAction;
    private predictionParticipantPickerDialog participantDialog;
    private boolean savingPrediction;
    private final Map<String, favoriteItem> participantItems = new HashMap<>();
    private Button saveButton;
    private ProgressBar saveProgress;
    private TextView points;

    private DatabaseReference rootRef;
    private FirebaseFunctions functions;

    private LinearLayout twoOptionsContainer;

    private LinearLayout predictionStatsCard;
    private TextView predictionStatsTitle;
    private TextView predictionStatsBody;
    private ProgressBar predictionStatsProgress;

    private String headToHeadChoiceAId;
    private String headToHeadChoiceATitle;
    private String headToHeadChoiceBId;
    private String headToHeadChoiceBTitle;

    private String predictionCategory;

    private FrameLayout predictionStatsCustomContent;

    private final predictionSingleCardLoader singleCardLoader = new predictionSingleCardLoader();
    private int singleCardRequest = 0;



    private LinearLayout predictionWeekendCard;
    private FrameLayout predictionWeekendContent;
    private ProgressBar predictionWeekendProgress;
    private TextView predictionWeekendError;
    private Button predictionWeekendOpenButton;

    private final predictionCircuitCardLoader weekendCardLoader =
            new predictionCircuitCardLoader();

    private predictionCircuitCardLoader.Data loadedWeekendData;

    private int weekendCardRequest = 0;

    private final predictionTeamQualifyingLoader teamQualifyingLoader =
            new predictionTeamQualifyingLoader();
    private int teamQualifyingRequest = 0;

    private final predictionTeamRaceLoader teamRaceLoader = new predictionTeamRaceLoader();
    private int teamRaceCardRequest = 0;

    private final predictionDriverHistoryLoader driverHistoryLoader =
            new predictionDriverHistoryLoader();

    private final jolpicaPointsLoader standingsPointsLoader = new jolpicaPointsLoader();
    private int standingsPointsRequest = 0;

    private final predictionWeekendPointsLoader weekendPointsLoader =
            new predictionWeekendPointsLoader();

    private String predictionDescription;
    private int booleanHistoryRequest = 0;
    private final predictionBooleanHistoryLoader booleanHistoryLoader =
            new predictionBooleanHistoryLoader();

    private final List<predictionBooleanHistoryLoader.Row> booleanHistoryRows =
            new ArrayList<>();

    private ImageButton refreshButton;

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

        findViewById(R.id.aboutPrediction)
                .setOnClickListener(v ->
                        userPredictionRules.show(this)
                );

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
        selectedOptionCard = findViewById(R.id.selectedOptionCard);
        selectedOptionImage = findViewById(R.id.selectedOptionImage);
        selectedOptionName = findViewById(R.id.selectedOptionName);
        selectedOptionAction = findViewById(R.id.selectedOptionAction);
        selectedOptionCard.setOnClickListener(view -> showParticipantPicker());
        saveButton = findViewById(R.id.saveButton);
        saveProgress = findViewById(R.id.saveProgress);
        points = findViewById(R.id.points);

        refreshButton = findViewById(R.id.refreshButton);
        refreshButton.setOnClickListener(v -> refreshPrediction());

        rootRef = FirebaseDatabase.getInstance().getReference();
        functions = FirebaseFunctions.getInstance("us-central1");

        ImageButton backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> finish());
        saveButton.setOnClickListener(v -> savePrediction(predictionFormActivity.this));

        twoOptionsContainer = findViewById(R.id.twoOptionsContainer);

        predictionStatsCard = findViewById(R.id.predictionStatsCard);
        predictionStatsTitle = findViewById(R.id.predictionStatsTitle);
        predictionStatsBody = findViewById(R.id.predictionStatsBody);
        predictionStatsProgress = findViewById(R.id.predictionStatsProgress);
        predictionStatsCustomContent = findViewById(R.id.predictionStatsCustomContent);

        predictionWeekendCard = findViewById(R.id.predictionWeekendCard);
        predictionWeekendContent = findViewById(R.id.predictionWeekendContent);
        predictionWeekendProgress = findViewById(R.id.predictionWeekendProgress);
        predictionWeekendError = findViewById(R.id.predictionWeekendError);
        predictionWeekendOpenButton = findViewById(R.id.predictionWeekendOpenButton);

        predictionWeekendOpenButton.setOnClickListener(
                v -> openLoadedWeekendRace()
        );

        storage = FirebaseStorage.getInstance();

        selectedOptionCard.setVisibility(View.GONE);
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

    private void refreshPrediction() {
        if (savingPrediction) {
            return;
        }

        predictionBooleanHistoryLoader.clearResponseCache();
        predictionPointsHttp.clearCache();
        jolpicaSessions.clearCache();

        booleanHistoryRequest++;
        singleCardRequest++;
        teamQualifyingRequest++;
        teamRaceCardRequest++;
        standingsPointsRequest++;
        weekendCardRequest++;

        booleanHistoryRows.clear();

        selectedOptionCard.setVisibility(View.GONE);
        twoOptionsContainer.setVisibility(View.GONE);
        predictionStatsCard.setVisibility(View.GONE);
        predictionStatsCustomContent.removeAllViews();
        predictionStatsProgress.setVisibility(View.GONE);

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

    @Override
    protected void onDestroy() {
        singleCardRequest++;

        if (participantDialog != null) {
            participantDialog.dismiss();
            participantDialog = null;
        }

        super.onDestroy();
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
                        predictionCategory = snapshot.child("category").getValue(String.class);

                        predictionDescription = getLocalizedText(
                                snapshot.child("description").getValue(LocalizedText.class));

                        setupWeekendCard();

                        if (isHeadToHead(predictionType)) {
                            // Статистика загрузится после чтения участников дуэли.
                            predictionStatsCard.setVisibility(View.GONE);

                        } else if (isBooleanPrediction(predictionType, optionType)) {
                            predictionStatsCard.setVisibility(View.GONE);

                        } else {
                            predictionStatsCard.setVisibility(View.GONE);
                        }

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
                            setupBooleanHistoryCard();
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

    private void showTextStatsContent() {
        predictionStatsCustomContent.setVisibility(View.GONE);
        predictionStatsCustomContent.removeAllViews();
        predictionStatsBody.setVisibility(View.VISIBLE);
    }

    private void setupHeadToHeadStats() {
        showTextStatsContent();

        if (!isHeadToHead(predictionType)) {
            predictionStatsCard.setVisibility(View.GONE);
            return;
        }

        if (headToHeadChoiceAId == null || headToHeadChoiceBId == null) {
            showStatsUnavailable();
            return;
        }

        final Integer beforeRound = "weekend".equals(predictionCategory)
                ? predictionRaceRound : null;

        if ("weekend".equals(predictionCategory) && beforeRound == null) {
            showStatsUnavailable();
            return;
        }

        predictionStatsCard.setVisibility(View.VISIBLE);
        predictionStatsBody.setText(R.string.prediction_stats_loading);
        predictionStatsProgress.setVisibility(View.VISIBLE);

        if ("race_head_to_head".equals(predictionType)) {
            if ("constructor".equals(optionType)) {
                loadTeamRaceCard();
                return;
            }
            if (!"driver".equals(optionType)) {
                showStatsUnavailable();
                return;
            }

            driverHistoryLoader.loadRace(
                    predictionSeason, beforeRound,
                    headToHeadChoiceATitle, headToHeadChoiceBTitle,
                    new predictionStatsLoader.raceStatsCallback() {
                        @Override
                        public void onLoaded(List<raceDuelEvent> events) {
                            if (isFinishing() || isDestroyed()) {
                                return;
                            }

                            predictionStatsProgress.setVisibility(View.GONE);

                            if (events.isEmpty()) {
                                showStatsUnavailable();
                                return;
                            }

                            predictionStatsTitle.setText(
                                    R.string.prediction_stats_duel_race_title
                            );

                            predictionRaceDuelRenderer.render(
                                    predictionStatsCustomContent,
                                    headToHeadChoiceAId,
                                    headToHeadChoiceATitle,
                                    headToHeadChoiceBId,
                                    headToHeadChoiceBTitle,
                                    events
                            );

                            predictionStatsBody.setVisibility(View.GONE);
                            predictionStatsCustomContent.setVisibility(View.VISIBLE);
                        }

                        @Override
                        public void onError() {
                            showStatsError();
                        }
                    }
            );
        } else if ("qualifying_head_to_head".equals(predictionType)) {
            if ("constructor".equals(optionType)) {
                loadTeamQualifyingCard();
                return;
            }
            if (!"driver".equals(optionType)) {
                showStatsUnavailable();
                return;
            }

            driverHistoryLoader.loadQualifying(
                    predictionSeason,
                    beforeRound,
                    headToHeadChoiceAId,
                    headToHeadChoiceBId,
                    new predictionStatsLoader.qualifyingStatsCallback() {
                        @Override
                        public void onLoaded(List<qualifyingDuelEvent> events) {
                            if (isFinishing() || isDestroyed()) {
                                return;
                            }

                            predictionStatsProgress.setVisibility(View.GONE);

                            boolean hasJointResults = false;

                            for (qualifyingDuelEvent event : events) {
                                if (event != null
                                        && event.getPositionA() != null
                                        && event.getPositionA() > 0
                                        && event.getPositionB() != null
                                        && event.getPositionB() > 0) {
                                    hasJointResults = true;
                                    break;
                                }
                            }

                            if (!hasJointResults) {
                                showStatsUnavailable();
                                return;
                            }

                            predictionStatsTitle.setText(
                                    R.string.prediction_stats_duel_quali_title
                            );

                            predictionRaceDuelRenderer.renderQualifying(
                                    predictionStatsCustomContent,
                                    headToHeadChoiceAId,
                                    headToHeadChoiceATitle,
                                    headToHeadChoiceBId,
                                    headToHeadChoiceBTitle,
                                    events
                            );

                            predictionStatsBody.setVisibility(View.GONE);
                            predictionStatsCustomContent.setVisibility(View.VISIBLE);
                        }

                        @Override
                        public void onError() {
                            showStatsError();
                        }
                    }
            );
        } else if ("team_points_head_to_head".equals(predictionType)) {
            loadStandingsPointsCard();

        } else {
            predictionStatsCard.setVisibility(View.GONE);
        }
    }

    private void showStatsUnavailable() {
        showTextStatsContent();

        predictionStatsProgress.setVisibility(View.GONE);
        predictionStatsBody.setText(R.string.prediction_stats_unavailable);
    }

    private void showStatsError() {
        showTextStatsContent();

        predictionStatsProgress.setVisibility(View.GONE);
        predictionStatsBody.setText(R.string.prediction_stats_error);
    }

    private void updateSingleOptionStats(predictionOption option) {
        updateSingleCard(option, false);
    }

    private void updateConstructorStats(predictionOption option) {
        updateSingleCard(option, true);
    }

    private void updateSingleCard(predictionOption option, boolean team) {
        final int request = ++singleCardRequest;
        if (option == null
                || !(team ? "constructor" : "driver").equals(optionType)
                || (!"winner".equals(predictionType) && !"champion".equals(predictionType))) {
            predictionStatsCard.setVisibility(View.GONE);
            return;
        }
        final boolean weekend = "weekend".equals(predictionCategory);
        if (weekend && predictionRaceRound == null) {
            predictionStatsCard.setVisibility(View.VISIBLE);
            showStatsUnavailable();
            return;
        }
        showTextStatsContent();
        predictionStatsCard.setVisibility(View.VISIBLE);
        predictionStatsTitle.setText(R.string.prediction_stats_title);
        predictionStatsBody.setText(R.string.prediction_stats_loading);
        predictionStatsProgress.setVisibility(View.VISIBLE);
        singleCardLoader.load(
                predictionSeason, weekend ? predictionRaceRound : null,
                team, option.getId(), option.getTitle(),
                new predictionSingleCardLoader.Callback() {
                    private boolean outdated() {
                        return request != singleCardRequest || isFinishing() || isDestroyed();
                    }
                    @Override public void onLoaded(predictionSingleCardLoader.Data data) {
                        if (outdated()) return;
                        predictionStatsProgress.setVisibility(View.GONE);
                        predictionSingleCardRenderer.render(predictionStatsCustomContent, data);
                        predictionStatsBody.setVisibility(View.GONE);
                        predictionStatsCustomContent.setVisibility(View.VISIBLE);
                    }
                    @Override public void onError() {
                        if (!outdated()) showStatsError();
                    }
                });
    }

    private void loadTeamQualifyingCard() {
        final int request = ++teamQualifyingRequest;
        Integer beforeRound = "weekend".equals(predictionCategory)
                ? predictionRaceRound : null;
        if ("weekend".equals(predictionCategory) && beforeRound == null) {
            showStatsUnavailable();
            return;
        }
        showTextStatsContent();
        predictionStatsCard.setVisibility(View.VISIBLE);
        predictionStatsTitle.setText(R.string.tq_title);
        predictionStatsBody.setText(R.string.prediction_stats_loading);
        predictionStatsProgress.setVisibility(View.VISIBLE);

        teamQualifyingLoader.load(predictionSeason, beforeRound,
                headToHeadChoiceAId, headToHeadChoiceBId,
                new predictionTeamQualifyingLoader.Callback() {
                    private boolean outdated() {
                        return request != teamQualifyingRequest || isFinishing() || isDestroyed();
                    }
                    @Override public void onLoaded(
                            List<predictionTeamQualifyingLoader.Event> events) {
                        if (outdated()) return;
                        predictionStatsProgress.setVisibility(View.GONE);
                        predictionTeamQualifyingRenderer.render(
                                predictionStatsCustomContent,
                                headToHeadChoiceATitle, headToHeadChoiceBTitle, events);
                        predictionStatsBody.setVisibility(View.GONE);
                        predictionStatsCustomContent.setVisibility(View.VISIBLE);
                    }
                    @Override public void onError() {
                        if (!outdated()) showStatsError();
                    }
                });
    }

    private void loadTeamRaceCard() {
        final int request = ++teamRaceCardRequest;
        Integer beforeRound = "weekend".equals(predictionCategory) ? predictionRaceRound : null;
        if ("weekend".equals(predictionCategory) && beforeRound == null) {
            showStatsUnavailable();
            return;
        }
        showTextStatsContent();
        predictionStatsCard.setVisibility(View.VISIBLE);
        predictionStatsTitle.setText(R.string.tr_title);
        predictionStatsBody.setText(R.string.prediction_stats_loading);
        predictionStatsProgress.setVisibility(View.VISIBLE);

        teamRaceLoader.load(predictionSeason, beforeRound,
                headToHeadChoiceAId, headToHeadChoiceBId,
                new predictionTeamRaceLoader.Callback() {
                    private boolean outdated() {
                        return request != teamRaceCardRequest || isFinishing() || isDestroyed();
                    }
                    @Override public void onLoaded(List<predictionTeamRaceLoader.Event> events) {
                        if (outdated()) return;
                        predictionStatsProgress.setVisibility(View.GONE);
                        predictionTeamRaceRenderer.render(predictionStatsCustomContent,
                                headToHeadChoiceATitle, headToHeadChoiceBTitle, events);
                        predictionStatsBody.setVisibility(View.GONE);
                        predictionStatsCustomContent.setVisibility(View.VISIBLE);
                    }
                    @Override public void onError() {
                        if (!outdated()) showStatsError();
                    }
                });
    }

    private void booleanHistoryMessage(int messageId) {
        predictionStatsCard.setVisibility(View.VISIBLE);
        predictionStatsTitle.setText(R.string.bh_title);
        predictionStatsProgress.setVisibility(View.GONE);
        predictionBooleanHistoryRenderer.message(
                predictionStatsCustomContent, predictionDescription, messageId);
        predictionStatsBody.setVisibility(View.GONE);
        predictionStatsCustomContent.setVisibility(View.VISIBLE);
    }

    private void setupBooleanHistoryCard() {
        if (!isBooleanPrediction(predictionType, optionType)) return;
        final int request = ++booleanHistoryRequest;
        if ("boolean_prediction".equals(predictionType)) {
            booleanHistoryMessage(R.string.bh_generic);
            return;
        }
        if (!"weekend".equals(predictionCategory)) {
            booleanHistoryMessage(R.string.bh_season);
            return;
        }
        final predictionCircuitCardLoader.Data track = loadedWeekendData;
        if (track == null) {
            booleanHistoryMessage(predictionWeekendError.getVisibility() == View.VISIBLE
                    ? R.string.bh_error : R.string.bh_waiting_track);
            return;
        }
        if (track.firstPracticeDate == null) {
            booleanHistoryMessage(R.string.bh_error);
            return;
        }
        final String type = predictionType;
        final String description = predictionDescription;
        booleanHistoryMessage(R.string.bh_loading);
        predictionStatsProgress.setVisibility(View.VISIBLE);
        booleanHistoryLoader.load(
                track.location,
                track.circuitName,
                track.circuitId,
                track.firstPracticeDate,
                new predictionBooleanHistoryLoader.Callback() {
                    private boolean outdated() {
                        return request != booleanHistoryRequest || isFinishing() || isDestroyed();
                    }

                    @Override
                    public void onRowLoaded(predictionBooleanHistoryLoader.Row row) {
                        if (outdated()) {
                            return;
                        }

                        predictionStatsProgress.setVisibility(View.GONE);
                        predictionStatsTitle.setText(
                                "rain".equals(type)
                                        ? R.string.bh_rain_title
                                        : R.string.bh_sc_title
                        );

                        predictionBooleanHistoryRenderer.render(
                                predictionStatsCustomContent,
                                type,
                                description,
                                buildDataWithRow(row)
                        );

                        predictionStatsBody.setVisibility(View.GONE);
                        predictionStatsCustomContent.setVisibility(View.VISIBLE);
                    }

                    @Override public void onLoaded(predictionBooleanHistoryLoader.Data data) {
                        if (outdated()) return;
                        predictionStatsProgress.setVisibility(View.GONE);
                        predictionStatsTitle.setText("rain".equals(type)
                                ? R.string.bh_rain_title : R.string.bh_sc_title);

                        booleanHistoryRows.clear();
                        booleanHistoryRows.addAll(data.rows);

                        predictionBooleanHistoryRenderer.render(
                                predictionStatsCustomContent, type, description, data);
                        predictionStatsBody.setVisibility(View.GONE);
                        predictionStatsCustomContent.setVisibility(View.VISIBLE);
                    }

                    private predictionBooleanHistoryLoader.Data buildDataWithRow(
                            predictionBooleanHistoryLoader.Row updatedRow
                    ) {
                        for (int i = 0; i < booleanHistoryRows.size(); i++) {
                            if (booleanHistoryRows.get(i).sessionKey == updatedRow.sessionKey) {
                                booleanHistoryRows.set(i, updatedRow);
                                break;
                            }
                        }

                        return new predictionBooleanHistoryLoader.Data(
                                booleanHistoryRows,
                                0,
                                new ArrayList<>()
                        );
                    }

                    @Override
                    public void onError(String message) {
                        if (outdated()) {
                            return;
                        }

                        android.util.Log.e(
                                "BooleanHistory",
                                "History load failed: " + message
                        );

                        booleanHistoryMessage(R.string.bh_error);
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
                        participantItems.clear();

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

                            predictionOption option = new predictionOption(id, title);
                            options.add(option);
                            participantItems.put(id, createParticipantItem(option, child, false));
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
                        participantItems.clear();

                        for (DataSnapshot child : snapshot.getChildren()) {
                            String id = child.child("constructorId")
                                    .getValue(String.class);

                            String title = child.child("name")
                                    .getValue(String.class);

                            if (id == null || title == null) {
                                continue;
                            }

                            predictionOption option = new predictionOption(id, title);
                            options.add(option);
                            participantItems.put(id, createParticipantItem(option, child, true));
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

        if (options.isEmpty()) {
            selectedOptionCard.setVisibility(View.GONE);
            Toast.makeText(this, R.string.prediction_unknown_option_type, Toast.LENGTH_LONG).show();
            saveButton.setEnabled(false);

            return;
        }

        selectedOptionCard.setVisibility(View.VISIBLE);
        selectedOptionCard.setEnabled(!savingPrediction);
        selectedOptionAction.setText(R.string.pp_change);
        ((TextView) findViewById(R.id.choicePrediction_hint)).setText(R.string.pp_your_answer);

        updateSelectedOption(0);
    }

    private favoriteItem createParticipantItem(predictionOption option, DataSnapshot snapshot, boolean team) {
        String title = option.getTitle();

        if (team) {
            return new favoriteItem(
                    option.getId(), "", "", title,
                    getString(R.string.team_description),
                    "teams/" + option.getId() + "_" + predictionSeason + ".png",
                    false, true, null
            );
        }

        int split = title.lastIndexOf(' ');
        String name = split > 0 ? title.substring(0, split) : title;
        String family = split > 0 ? title.substring(split + 1) : "";
        String driverId = snapshot.child("driverId").getValue(String.class);
        String lastEntry = snapshot.child("lastEntry").getValue(String.class);
        String imageId = driverId == null || driverId.trim().isEmpty()
                ? option.getId().toLowerCase(Locale.ROOT)
                : driverId.trim();

        return new favoriteItem(
                title, name, family, "",
                getString(R.string.driver_active_text) + " " + getString(R.string.driver_description),
                "drivers/" + imageId + "_" + predictionSeason + ".png",
                false, false, lastEntry
        );
    }

    private void showParticipantPicker() {
        if (savingPrediction || options.isEmpty() || isFinishing() || isDestroyed()) {
            return;
        }

        if (participantDialog != null && participantDialog.isShowing()) {
            return;
        }

        boolean team = "constructor".equals(optionType);
        List<favoriteItem> items = new ArrayList<>();
        String currentValue = null;

        for (predictionOption option : options) {
            favoriteItem item = participantItems.get(option.getId());

            if (item == null) {
                continue;
            }

            items.add(item);

            if (java.util.Objects.equals(option.getId(), selectedOptionId)) {
                currentValue = item.value;
            }
        }

        if (items.isEmpty()) {
            return;
        }

        predictionParticipantPickerDialog dialog = new predictionParticipantPickerDialog(
                this,
                getString(team
                        ? R.string.pp_choose_team
                        : R.string.pp_choose_driver),
                items,
                currentValue,
                item -> {
                    if (savingPrediction || isFinishing() || isDestroyed()) {
                        return;
                    }

                    for (int i = 0; i < options.size(); i++) {
                        predictionOption option = options.get(i);
                        String value = team ? option.getId() : option.getTitle();

                        if (java.util.Objects.equals(value, item.value)) {
                            updateSelectedOption(i);

                            return;
                        }
                    }
                }
        );

        participantDialog = dialog;
        dialog.setOnDismissListener(ignored -> {
            if (participantDialog == dialog) {
                participantDialog = null;
            }
        });
        dialog.show();
    }

    private void renderSelectedParticipant(predictionOption option) {
        selectedOptionName.setText(option.getTitle());
        selectedOptionCard.setContentDescription(option.getTitle() + ". " + selectedOptionAction.getText());

        ViewGroup.LayoutParams imageParams = selectedOptionImage.getLayoutParams();
        imageParams.width = dpToPx("constructor".equals(optionType) ? 100 : 52);
        imageParams.height = dpToPx(52);
        selectedOptionImage.setLayoutParams(imageParams);

        selectedOptionImage.setScaleType("constructor".equals(optionType)
                ? ImageView.ScaleType.FIT_CENTER
                : ImageView.ScaleType.CENTER_CROP);

        Glide.with(this).clear(selectedOptionImage);
        selectedOptionImage.setImageResource(R.drawable.placeholder_driver);

        favoriteItem item = participantItems.get(option.getId());

        if (item == null || item.imagePath == null) {
            return;
        }

        String requestedId = option.getId();

        storage.getReference().child(item.imagePath).getDownloadUrl()
                .addOnSuccessListener(uri -> {
                    if (isFinishing() || isDestroyed()
                            || !java.util.Objects.equals(requestedId, selectedOptionId)) {
                        return;
                    }

                    Glide.with(this)
                            .load(uri)
                            .placeholder(R.drawable.placeholder_driver)
                            .error(R.drawable.placeholder_driver)
                            .into(selectedOptionImage);
                });
    }

    private void updateSelectedOption(int index) {
        if (index < 0 || index >= options.size()) {
            return;
        }

        predictionOption option = options.get(index);
        boolean changed = !java.util.Objects.equals(selectedOptionId, option.getId())
                || !java.util.Objects.equals(selectedOptionTitle, option.getTitle());

        selectedOptionId = option.getId();
        selectedOptionTitle = option.getTitle();

        if ("boolean".equals(optionType)) {
            return;
        }

        if (!changed) {
            return;
        }

        renderSelectedParticipant(option);

        android.util.Log.d("SingleCardSelection", "SELECTED; id=" + selectedOptionId);

        if ("constructor".equals(optionType)) {
            updateConstructorStats(option);
        } else if ("driver".equals(optionType)) {
            updateSingleOptionStats(option);
        }
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

            if (selectedOptionCard.getVisibility() == View.VISIBLE) {
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
            findViewById(R.id.submittedAtRow).setVisibility(View.VISIBLE);
        } else {
            findViewById(R.id.submittedAtRow).setVisibility(View.GONE);
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
            pointsEarned.setText(formatPoints(answer.getPoints()));
            findViewById(R.id.pointsEarnedRow).setVisibility(View.VISIBLE);
        } else {
            findViewById(R.id.pointsEarnedRow).setVisibility(View.GONE);
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

        publicLeaderboardConsent.submit(predictionFormActivity.this, functions, data)
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

                    if (publicLeaderboardConsent.isCancelled(error)) {
                        return;
                    }
                    hasSavedAnswer = true;
                    Toast.makeText(
                            predictionFormActivity.this,
                            error.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }

    private void setSavingState(boolean saving) {
        savingPrediction = saving;
        saveButton.setEnabled(!saving);
        selectedOptionCard.setEnabled(!saving);
        selectedOptionCard.setAlpha(saving ? 0.6f : 1f);
        twoOptionsContainer.setEnabled(!saving);

        if (saving && participantDialog != null) {
            participantDialog.dismiss();
            participantDialog = null;
        }

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

        headToHeadChoiceAId = sideAId;
        headToHeadChoiceATitle = sideATitle;
        headToHeadChoiceBId = sideBId;
        headToHeadChoiceBTitle = sideBTitle;

        setupHeadToHeadStats();

        headToHeadOptions.clear();
        headToHeadOptions.add(new predictionOption(sideAId, sideATitle));
        headToHeadOptions.add(new predictionOption(sideBId, sideBTitle));

        setupHeadToHeadCards(context);
        loadSavedAnswer(context);
    }

    private void setupHeadToHeadCards(Context context) {
        selectedOptionCard.setVisibility(View.GONE);
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

    private void setupWeekendCard() {
        final int request = ++weekendCardRequest;

        loadedWeekendData = null;

        predictionWeekendContent.removeAllViews();
        predictionWeekendContent.setVisibility(View.GONE);
        predictionWeekendError.setVisibility(View.GONE);
        predictionWeekendProgress.setVisibility(View.GONE);
        predictionWeekendOpenButton.setVisibility(View.GONE);
        predictionWeekendOpenButton.setEnabled(false);

        if (!"weekend".equals(predictionCategory)) {
            predictionWeekendCard.setVisibility(View.GONE);
            return;
        }

        predictionWeekendCard.setVisibility(View.VISIBLE);

        if (predictionSeason == null || predictionRaceRound == null) {
            showWeekendCardError();
            return;
        }

        predictionWeekendProgress.setVisibility(View.VISIBLE);

        weekendCardLoader.load(
                predictionSeason,
                predictionRaceRound,
                new predictionCircuitCardLoader.Callback() {
                    @Override
                    public void onLoaded(
                            predictionCircuitCardLoader.Data data
                    ) {
                        if (request != weekendCardRequest
                                || isFinishing()
                                || isDestroyed()) {
                            return;
                        }

                        loadedWeekendData = data;

                        predictionCircuitCardRenderer.render(
                                predictionWeekendContent,
                                data
                        );

                        predictionWeekendProgress.setVisibility(View.GONE);
                        predictionWeekendError.setVisibility(View.GONE);
                        predictionWeekendContent.setVisibility(View.VISIBLE);

                        boolean canOpen =
                                data.firstPracticeDate != null
                                        && data.raceDate != null
                                        && data.circuitId != null
                                        && data.raceName != null;

                        predictionWeekendOpenButton.setEnabled(canOpen);

                        predictionWeekendOpenButton.setVisibility(
                                canOpen ? View.VISIBLE : View.GONE
                        );

                        if (isBooleanPrediction(predictionType, optionType)) {
                            setupBooleanHistoryCard();
                        }
                    }

                    @Override
                    public void onError() {
                        if (request != weekendCardRequest
                                || isFinishing()
                                || isDestroyed()) {
                            return;
                        }

                        showWeekendCardError();
                    }
                }
        );
    }

    private void showWeekendCardError() {
        loadedWeekendData = null;

        predictionWeekendProgress.setVisibility(View.GONE);
        predictionWeekendContent.setVisibility(View.GONE);
        predictionWeekendOpenButton.setVisibility(View.GONE);
        predictionWeekendOpenButton.setEnabled(false);

        predictionWeekendError.setText(
                R.string.prediction_stats_unavailable
        );

        predictionWeekendError.setVisibility(View.VISIBLE);

        if ("rain".equals(predictionType) || "safety_car".equals(predictionType)) {
            ++booleanHistoryRequest;
            booleanHistoryMessage(R.string.bh_error);
        }
    }

    private void openLoadedWeekendRace() {
        predictionCircuitCardLoader.Data data = loadedWeekendData;

        if (data == null
                || data.firstPracticeDate == null
                || data.raceDate == null
                || data.circuitId == null
                || data.raceName == null) {
            return;
        }

        final LocalDate start;
        final LocalDate end;

        try {
            start = LocalDate.parse(data.firstPracticeDate);
            end = LocalDate.parse(data.raceDate);
        } catch (DateTimeParseException error) {
            Toast.makeText(
                    this,
                    R.string.prediction_stats_unavailable,
                    Toast.LENGTH_LONG
            ).show();

            return;
        }

        Intent intent = new Intent(
                predictionFormActivity.this,
                futureRaceActivity.class
        );

        intent.putExtra("circuitId", data.circuitId);
        intent.putExtra("raceName", data.raceName);

        intent.putExtra(
                "futureRaceStartDay",
                String.valueOf(start.getDayOfMonth())
        );

        intent.putExtra(
                "futureRaceEndDay",
                String.valueOf(end.getDayOfMonth())
        );

        intent.putExtra(
                "futureRaceStartMonth",
                String.valueOf(start.getMonthValue())
        );

        intent.putExtra(
                "futureRaceEndMonth",
                String.valueOf(end.getMonthValue())
        );

        intent.putExtra("roundCount", String.valueOf(data.round));

        intent.putExtra(
                "raceCountry",
                data.country == null ? "" : data.country
        );

        intent.putExtra("dateStart", data.firstPracticeDate);
        intent.putExtra("dateEnd", data.raceDate);

        intent.putExtra("isCanceled", false);

        startActivity(intent);
    }

    private void loadStandingsPointsCard() {
        final int request = ++standingsPointsRequest;
        final boolean teams = "constructor".equals(optionType);
        if (!teams && !"driver".equals(optionType)) {
            showStatsUnavailable();
            return;
        }

        final Integer afterRound;
        if ("weekend".equals(predictionCategory)) {
            if (predictionRaceRound == null || predictionRaceRound < 1) {
                showStatsUnavailable();
                return;
            }
            afterRound = predictionRaceRound - 1;
        } else {
            afterRound = null;
        }

        final String season = predictionSeason;
        final String idA = headToHeadChoiceAId;
        final String nameA = headToHeadChoiceATitle;
        final String idB = headToHeadChoiceBId;
        final String nameB = headToHeadChoiceBTitle;

        showTextStatsContent();
        predictionStatsCard.setVisibility(View.VISIBLE);
        predictionStatsTitle.setText(R.string.sp_title);
        predictionStatsBody.setText(R.string.prediction_stats_loading);
        predictionStatsProgress.setVisibility(View.VISIBLE);

        standingsPointsLoader.load(teams, season, afterRound,
                idA, nameA, idB, nameB,
                new jolpicaPointsLoader.Callback() {
                    private boolean outdated() {
                        return request != standingsPointsRequest || isFinishing() || isDestroyed();
                    }
                    @Override public void onLoaded(jolpicaPointsLoader.Result result) {
                        if (outdated()) return;
                        predictionStatsProgress.setVisibility(View.GONE);
                        predictionStandingsPointsRenderer.render(
                                predictionStatsCustomContent, teams, season,
                                idA, nameA, idB, nameB, result);
                        predictionStatsBody.setVisibility(View.GONE);
                        predictionStatsCustomContent.setVisibility(View.VISIBLE);

                        if (result.round == 0) {
                            predictionStandingsPointsRenderer.render(
                                    predictionStatsCustomContent,
                                    teams,
                                    season,
                                    idA,
                                    nameA,
                                    idB,
                                    nameB,
                                    result,
                                    new predictionWeekendPointsLoader.Data(),
                                    false
                            );
                            return;
                        }

                        if (result.idA == null || result.idA.trim().isEmpty()
                                || result.idB == null || result.idB.trim().isEmpty()) {

                            android.util.Log.e(
                                    "WeekendPoints",
                                    "Standings returned no participant IDs"
                                            + "; round=" + result.round
                                            + "; idA=" + result.idA
                                            + "; idB=" + result.idB
                            );

                            predictionStandingsPointsRenderer.render(
                                    predictionStatsCustomContent,
                                    teams,
                                    season,
                                    idA,
                                    nameA,
                                    idB,
                                    nameB,
                                    result,
                                    null,
                                    true
                            );
                            return;
                        }

                        weekendPointsLoader.load(season, result.round, teams,
                                result.idA, result.idB,
                                new predictionWeekendPointsLoader.Callback() {
                                    @Override public void onLoaded(predictionWeekendPointsLoader.Data history) {
                                        if (request != standingsPointsRequest || isFinishing() || isDestroyed()) return;
                                        predictionStandingsPointsRenderer.render(
                                                predictionStatsCustomContent, teams, season,
                                                idA, nameA, idB, nameB, result, history, false);
                                    }
                                    @Override
                                    public void onError(String message) {
                                        if (request != standingsPointsRequest
                                                || isFinishing()
                                                || isDestroyed()) {
                                            return;
                                        }

                                        android.util.Log.e(
                                                "WeekendPoints",
                                                "History load failed: " + message
                                        );

                                        predictionStandingsPointsRenderer.render(
                                                predictionStatsCustomContent,
                                                teams,
                                                season,
                                                idA,
                                                nameA,
                                                idB,
                                                nameB,
                                                result,
                                                null,
                                                true
                                        );
                                    }
                                });
                    }
                    @Override public void onError(String message) {
                        if (outdated()) return;
                        showStatsError();
                    }
                });
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
        selectedOptionCard.setVisibility(View.GONE);
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
