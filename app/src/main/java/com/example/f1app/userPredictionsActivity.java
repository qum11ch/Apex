package com.example.f1app;

import static com.example.f1app.MainActivity.checkConnection;
import static com.example.f1app.MainActivity.hideShimmer;
import static com.example.f1app.MainActivity.showShimmer;
import static com.example.f1app.userPredictionAdapter.formatPoints;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.facebook.shimmer.ShimmerFrameLayout;
import com.github.mikephil.charting.charts.PieChart;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;

import android.widget.Toast;

import com.google.firebase.functions.FirebaseFunctions;

import java.util.HashMap;
import java.util.Map;

import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class userPredictionsActivity extends AppCompatActivity {
    private final List<userPrediction> userPredictions = new ArrayList<>();

    private userPredictionAdapter adapter;
    private TextView emptyText;
    private boolean isLoading = false;

    private DatabaseReference rootRef;
    private FirebaseUser currentUser;

    private TextView totalPointsTextView;
    private TextView predictionsCountTextView;
    private TextView correctPredictionsTextView;
    private TextView seasonRankTextView;
    private TextView seasonRankInfo;
    private TextView globalRankTextView;
    private TextView globalRankInfo;
    private PieChart predictionResultsChart;
    private LinearLayout chartLegend;
    private FirebaseFunctions functions;

    private SwipeRefreshLayout swipeRefreshLayout;
    private ShimmerFrameLayout shimmerLayout;
    private RecyclerView recyclerView;

    private String currentPredictionSeason;
    private Dialog deleteDialog;

    private Button deletePrediction, cancelDelete;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);
        setContentView(R.layout.user_predictions_activity);

        setupUserPredictionsInsets();
        EdgeInsets.applyBottom(findViewById(R.id.userPredictionsScrollView));

        if (!checkConnection(getApplicationContext())){
            startActivity(connectionLostScreen.createShowSplashOnNetworkFailure(userPredictionsActivity.this));
        }else{
            startActivity(connectionLostScreen.createIntentHideSplashOnNetworkRecovery(userPredictionsActivity.this));
        }

        WindowInsetsControllerCompat windowInsetsController =
                WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        windowInsetsController.setAppearanceLightStatusBars(false);

        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        functions = FirebaseFunctions.getInstance();
        rootRef = FirebaseDatabase.getInstance().getReference();

        recyclerView = findViewById(R.id.userPredictionsRecyclerView);
        shimmerLayout = findViewById(R.id.shimmer_layout);
        swipeRefreshLayout = findViewById(R.id.swipe_layout);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new userPredictionAdapter(userPredictions, this::openPrediction);
        recyclerView.setAdapter(adapter);
        attachSwipeActions(userPredictionsActivity.this, recyclerView);

        swipeRefreshLayout.setColorSchemeResources(
                R.color.incorrect_predict_color,
                R.color.correct_predict_color,
                R.color.tie_predict_color
        );

        ImageButton backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> finish());

        swipeRefreshLayout.setOnRefreshListener(() -> {
            if (isLoading) {
                swipeRefreshLayout.setRefreshing(false);
                return;
            }

            loadUserAnswers(this);

            if (currentPredictionSeason != null) {
                loadUserStats(
                        this,
                        currentPredictionSeason
                );
            }
        });

        emptyText = findViewById(R.id.userPredictionsEmptyText);
        totalPointsTextView = findViewById(R.id.totalPointsText);
        predictionsCountTextView = findViewById(R.id.predictionsCountText);
        correctPredictionsTextView = findViewById(R.id.correctPredictionsText);
        seasonRankTextView = findViewById(R.id.seasonRank);
        seasonRankInfo = findViewById(R.id.seasonRank_info);
        globalRankTextView = findViewById(R.id.globalRank);
        globalRankInfo = findViewById(R.id.globalRank_info);
        predictionResultsChart = findViewById(R.id.predictionResultsChart);


        chartLegend = findViewById(R.id.chartLegend);

        if (currentUser == null) {
            showEmpty(userPredictionsActivity.this.getString(R.string.user_predictions_account_error));
            return;
        }

        rootRef.child("status/predictions_season")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        String season = snapshot.getValue(String.class);

                        if (season == null || !season.matches("\\d{4}")) {

                            season = String.valueOf(java.util
                                            .Calendar.getInstance().get(java.util.Calendar.YEAR));
                        }

                        currentPredictionSeason = season;

                        loadUserStats(userPredictionsActivity.this, currentPredictionSeason);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        currentPredictionSeason = String
                                .valueOf(
                                        java.util.Calendar.getInstance()
                                                .get(java.util.Calendar.YEAR));

                        loadUserStats(userPredictionsActivity.this, currentPredictionSeason);

                        Toast.makeText(
                                userPredictionsActivity.this,
                                error.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });

        loadUserAnswers(userPredictionsActivity.this);

        deleteDialog = new Dialog(this);
        deleteDialog.setContentView(R.layout.delete_prediction_dialog_box);
        deleteDialog.setCancelable(false);
        deleteDialog.getWindow().setLayout(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        deleteDialog.getWindow().setBackgroundDrawable(getDrawable(R.drawable.custom_dialog_bg));

        cancelDelete = deleteDialog.findViewById(R.id.cancel_button);
        deletePrediction = deleteDialog.findViewById(R.id.confirm_button);
    }

    private void setupUserPredictionsInsets() {
        View root = findViewById(R.id.user_predictions_root);
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

    private void loadUserAnswers(Context context) {
        if (currentUser == null || isLoading) {
            return;
        }

        isLoading = true;

        showShimmer(recyclerView, shimmerLayout);

        userPredictions.clear();
        adapter.notifyDataSetChanged();

        rootRef.child("userPredictionAnswers").child(currentUser.getUid())
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        userPredictions.clear();

                        if (!snapshot.exists()) {
                            userPredictions.clear();
                            adapter.notifyDataSetChanged();

                            finishLoading();
                            return;
                        }

                        List<DataSnapshot> answerSnapshots = new ArrayList<>();

                        for (DataSnapshot answerSnapshot : snapshot.getChildren()) {
                            answerSnapshots.add(answerSnapshot);
                        }

                        if (answerSnapshots.isEmpty()) {
                            finishLoading();
                            return;
                        }

                        loadPredictionDetails(answerSnapshots, 0);

                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        isLoading = false;

                        if (swipeRefreshLayout != null) {
                            swipeRefreshLayout.setRefreshing(false);
                        }

                        showEmpty(context.getString(R.string.prediction_load_error));

                        recyclerView.setVisibility(View.GONE);

                        shimmerLayout.stopShimmer();
                        shimmerLayout.setVisibility(View.GONE);
                    }
                });
    }

    private void loadUserStats(Context context, String currentPredictionsSeason) {

        if (currentUser == null) {
            return;
        }

        if (currentPredictionsSeason == null || currentPredictionsSeason.trim().isEmpty()) {
            currentPredictionsSeason = String
                    .valueOf(java.util.Calendar.getInstance().get(java.util.Calendar.YEAR));

            currentPredictionSeason = currentPredictionsSeason;
        }

        final String season = currentPredictionsSeason;

        String userId = currentUser.getUid();

        rootRef.child("userPredictionStats").child(userId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (!snapshot.exists()) {
                            showEmptyStats(season);
                            return;
                        }

                        userPredictionStats stats = snapshot.getValue(userPredictionStats.class);

                        if (stats == null) {
                            showEmptyStats(season);
                            return;
                        }

                        String totalPoints = formatPoints(stats.getTotalPoints());
                        long predictionsCount = valueOrZero(stats.getPredictionsCount());
                        long correctPredictions = valueOrZero(stats.getCorrectPredictions());
                        long incorrectPredictions = valueOrZero(stats.getIncorrectPredictions());
                        long pendingPredictions = valueOrZero(stats.getPendingPredictions());
                        long tiePredictions = valueOrZero(stats.getTiePredictions());

                        totalPointsTextView.setText(totalPoints);
                        predictionsCountTextView.setText(String.valueOf(predictionsCount));
                        correctPredictionsTextView.setText(String.valueOf(correctPredictions));

                        getRanks(
                                userId,
                                season,
                                new RankCallback() {
                                    @Override
                                    public void onSuccess(HashMap<String, Integer> ranks) {
                                        int seasonRank = ranks.get("season");
                                        int globalRank = ranks.get("global");

                                        if (seasonRank <= 0) {
                                            String seasonRankText = getString(
                                                    R.string.user_predictions_stats_no_season_rank,
                                                    season);
                                            seasonRankInfo.setText(seasonRankText);
                                            seasonRankTextView.setVisibility(View.GONE);
                                        } else {
                                            String seasonRankText = getString(
                                                    R.string.user_predictions_stats_seasonRank,
                                                    season);
                                            seasonRankInfo.setText(seasonRankText);
                                            seasonRankTextView.setVisibility(View.VISIBLE);
                                            seasonRankTextView.setText(String.valueOf(seasonRank));
                                        }

                                        if (globalRank <= 0) {
                                            String globalRankText = getString(
                                                    R.string.user_predictions_stats_no_global_rank);
                                            globalRankInfo.setText(globalRankText);
                                            globalRankTextView.setVisibility(View.GONE);
                                        } else {
                                            globalRankInfo.setText(getString(
                                                    R.string.user_predictions_stats_globalRank));
                                            globalRankTextView.setVisibility(View.VISIBLE);
                                            globalRankTextView.setText(String.valueOf(globalRank));
                                        }
                                    }

                                    @Override
                                    public void onError(DatabaseError error) {
                                        Toast.makeText(
                                                userPredictionsActivity.this,
                                                error.getMessage(),
                                                Toast.LENGTH_SHORT
                                        ).show();
                                    }
                                }
                        );

                        renderPredictionResultsChart(context, correctPredictions,
                                incorrectPredictions, pendingPredictions, tiePredictions);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        showEmptyStats(season);
                    }
                });
    }

    private long valueOrZero(Long value) {
        return value == null ? 0 : value;
    }

    public interface RankCallback {
        void onSuccess(HashMap<String, Integer> ranks);
        void onError(DatabaseError error);
    }

    private void getRanks(String uId, String currentPredictionsSeason, RankCallback callback) {
        if (currentPredictionsSeason == null || currentPredictionsSeason.trim().isEmpty()) {
            HashMap<String, Integer> rank = new HashMap<>();
            rank.put("season", 0);
            rank.put("global", 0);

            callback.onSuccess(rank);
            return;
        }

        rootRef.child("leaderboard")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        HashMap<String, Integer> rank = new HashMap<>();

                        DataSnapshot seasonLeaderboard =
                                snapshot.child(currentPredictionsSeason).child(uId);

                        DataSnapshot globalLeaderboard = snapshot.child("global").child(uId);

                        int seasonRank = 0;

                        if (seasonLeaderboard.exists()) {
                            Integer value = seasonLeaderboard.child("rank").getValue(Integer.class);

                            if (value != null) {
                                seasonRank = value;
                            }
                        }

                        int globalRank = 0;

                        if (globalLeaderboard.exists()) {
                            Integer value =
                                    globalLeaderboard.child("rank").getValue(Integer.class);

                            if (value != null) {
                                globalRank = value;
                            }
                        }

                        rank.put("season", seasonRank);
                        rank.put("global", globalRank);

                        callback.onSuccess(rank);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        callback.onError(error);
                    }
                });
    }

    private void showEmptyStats(String currentPredictionsSeason) {
        totalPointsTextView.setText("0");
        predictionsCountTextView.setText("0");
        correctPredictionsTextView.setText("0");

        String seasonRankText = getString(
                R.string.user_predictions_stats_no_season_rank,
                currentPredictionsSeason);
        seasonRankInfo.setText(seasonRankText);
        seasonRankTextView.setVisibility(View.GONE);

        String globalRankText = getString(
                R.string.user_predictions_stats_no_global_rank);
        globalRankInfo.setText(globalRankText);
        globalRankTextView.setVisibility(View.GONE);

        predictionResultsChart.setVisibility(View.GONE);
        chartLegend.removeAllViews();
    }

    private void renderPredictionResultsChart(Context context, long correct,
                                              long incorrect, long pending, long tie) {
        ArrayList<PieEntry> entries = new ArrayList<>();
        ArrayList<Integer> colors = new ArrayList<>();

        if (correct > 0) {
            entries.add(new PieEntry(correct, getString(R.string.prediction_correct_text)));
            colors.add(ContextCompat.getColor(context, R.color.correct_predict_color));
        }

        if (incorrect > 0) {
            entries.add(new PieEntry(incorrect, getString(R.string.prediction_incorrect_text)));
            colors.add(ContextCompat.getColor(context, R.color.incorrect_predict_color));
        }

        if (pending > 0) {
            entries.add(new PieEntry(pending, getString(R.string.prediction_pending_text)));
            colors.add(ContextCompat.getColor(context, R.color.pending_predict_color));
        }

        if (tie > 0) {
            entries.add(new PieEntry(tie, getString(R.string.prediction_tie_text)));
            colors.add(ContextCompat.getColor(context, R.color.tie_predict_color));
        }

        if (entries.isEmpty()) {
            predictionResultsChart.setVisibility(View.GONE);
            return;
        }

        predictionResultsChart.setVisibility(View.VISIBLE);

        PieDataSet dataSet = new PieDataSet(entries, "");
        dataSet.setColors(colors);
        dataSet.setDrawValues(false);
        dataSet.setSliceSpace(2f);
        dataSet.setSelectionShift(0f);

        PieData data = new PieData(dataSet);

        predictionResultsChart.setData(data);
        predictionResultsChart.setUsePercentValues(false);
        predictionResultsChart.getDescription().setEnabled(false);
        predictionResultsChart.setDrawHoleEnabled(true);
        predictionResultsChart.setHoleColor(
                ContextCompat.getColor(context, R.color.colorPrimary)
        );
        predictionResultsChart.setHoleRadius(62f);
        predictionResultsChart.setTransparentCircleRadius(64f);
        predictionResultsChart.setCenterText(getAccuracyText(correct, incorrect, tie));

        Typeface typeface = ResourcesCompat.getFont(
                context,
                R.font.nimbus_reg
        );

        predictionResultsChart.setCenterTextTypeface(typeface);
        predictionResultsChart.setCenterTextSize(16f);
        predictionResultsChart.setCenterTextColor(ContextCompat.getColor(context, R.color.text_color_main));

        predictionResultsChart.setDrawEntryLabels(false);

        predictionResultsChart.getLegend().setEnabled(false);

        predictionResultsChart.animateY(500);
        predictionResultsChart.invalidate();

        renderChartLegend(context, correct, incorrect, pending, tie);
    }

    private String getAccuracyText(long correct, long incorrect, long tie) {
        long completed = correct + incorrect;

        if (completed == 0) {
            return getString(R.string.none_results_text);
        }

        int accuracy = (int) Math.round(correct * 100.0 / completed);
        return accuracy + getString(R.string.accuracy_text);
    }

    private void renderChartLegend(Context context, long correct,
                                   long incorrect, long pending, long tie) {
        chartLegend.removeAllViews();

        ArrayList<LegendData> items = new ArrayList<>();

        if (correct > 0) {
            items.add(new LegendData(getString(R.string.prediction_correct_text),
                    correct,
                    ContextCompat.getColor(
                            context,
                            R.color.correct_predict_color)
            ));
        }

        if (incorrect > 0) {
            items.add(new LegendData(
                    getString(R.string.prediction_incorrect_text),
                    incorrect,
                    ContextCompat.getColor(
                            context,
                            R.color.incorrect_predict_color
                    )
            ));
        }

        if (pending > 0) {
            items.add(new LegendData(
                    getString(R.string.prediction_pending_text),
                    pending,
                    ContextCompat.getColor(
                            context,
                            R.color.pending_predict_color
                    )
            ));
        }

        if (tie > 0) {
            items.add(new LegendData(
                    getString(R.string.prediction_tie_text),
                    tie,
                    ContextCompat.getColor(
                            context,
                            R.color.tie_predict_color
                    )
            ));
        }

        if (items.isEmpty()) {
            return;
        }

        int availableWidth = chartLegend.getWidth()
                - chartLegend.getPaddingLeft()
                - chartLegend.getPaddingRight();

        if (availableWidth <= 0) {
            availableWidth = getResources().getDisplayMetrics().widthPixels;
        }

        buildLegendRows(context, items, availableWidth);
    }

    private static class LegendData {

        final String title;
        final long value;
        final int color;

        LegendData(String title, long value, int color) {
            this.title = title;
            this.value = value;
            this.color = color;
        }
    }

    private void buildLegendRows(Context context,
                                 ArrayList<LegendData> items,
                                 int availableWidth) {

        LinearLayout row = createLegendRow();
        int usedWidth = 0;

        for (LegendData legendItem : items) {

            View itemView = createLegendItemView(
                    context,
                    legendItem.title,
                    legendItem.value,
                    legendItem.color
            );

            itemView.measure(
                    View.MeasureSpec.makeMeasureSpec(
                            availableWidth,
                            View.MeasureSpec.AT_MOST
                    ),
                    View.MeasureSpec.makeMeasureSpec(
                            0,
                            View.MeasureSpec.UNSPECIFIED
                    )
            );

            int itemWidth = itemView.getMeasuredWidth();

            boolean rowHasItems = row.getChildCount() > 0;
            boolean doesNotFit = usedWidth + itemWidth > availableWidth;

            if (rowHasItems && doesNotFit) {
                chartLegend.addView(row);

                row = createLegendRow();
                usedWidth = 0;
            }

            row.addView(itemView);
            usedWidth += itemWidth;
        }

        if (row.getChildCount() > 0) {
            chartLegend.addView(row);
        }
    }

    private LinearLayout createLegendRow() {
        LinearLayout row = new LinearLayout(this);

        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER);
        row.setPadding(0, dpToPx(2), 0, dpToPx(2));

        LinearLayout.LayoutParams rowParams =
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );

        row.setLayoutParams(rowParams);

        return row;
    }

    private View createLegendItemView(Context context,
                                      String title,
                                      long value,
                                      int color) {

        LinearLayout item = new LinearLayout(this);

        item.setOrientation(LinearLayout.HORIZONTAL);
        item.setGravity(Gravity.CENTER_VERTICAL);
        item.setPadding(
                dpToPx(6),
                dpToPx(4),
                dpToPx(6),
                dpToPx(4)
        );

        View marker = new View(this);

        GradientDrawable markerBackground = new GradientDrawable();
        markerBackground.setShape(GradientDrawable.OVAL);
        markerBackground.setColor(color);
        marker.setBackground(markerBackground);

        LinearLayout.LayoutParams markerParams =
                new LinearLayout.LayoutParams(
                        dpToPx(14),
                        dpToPx(14)
                );

        markerParams.setMargins(
                0,
                0,
                dpToPx(7),
                0
        );

        item.addView(marker, markerParams);

        TextView titleView = new TextView(this);

        String rawText = title + ": ";
        titleView.setText(rawText);
        titleView.setTextSize(
                TypedValue.COMPLEX_UNIT_SP,
                14
        );
        titleView.setTextColor(
                ContextCompat.getColor(
                        context,
                        R.color.text_color_main
                )
        );
        titleView.setSingleLine(true);

        item.addView(titleView);

        TextView valueView = new TextView(this);

        valueView.setText(String.valueOf(value));
        valueView.setTextSize(
                TypedValue.COMPLEX_UNIT_SP,
                14
        );
        valueView.setTextColor(
                ContextCompat.getColor(
                        context,
                        R.color.text_color_main
                )
        );
        valueView.setTypeface(null, Typeface.BOLD);
        valueView.setSingleLine(true);

        item.addView(valueView);

        return item;
    }

    private int dpToPx(int dp) {
        return Math.round(
                dp * getResources().getDisplayMetrics().density
        );
    }

    private void loadPredictionDetails(List<DataSnapshot> answerSnapshots, int index) {
        if (index >= answerSnapshots.size()) {
            finishLoading();
            return;
        }

        DataSnapshot answerSnapshot = answerSnapshots.get(index);

        String predictionId = answerSnapshot.getKey();

        userPredictionAnswer answer = answerSnapshot.getValue(userPredictionAnswer.class);

        if (predictionId == null || answer == null) {
            loadPredictionDetails(answerSnapshots, index + 1);
            return;
        }

        rootRef.child("userPredictions").child(predictionId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot predictionSnapshot) {
                        userPrediction item = buildUserPrediction(predictionId, predictionSnapshot, answer);

                        if (item != null && !containsPrediction(item.getPredictionId())) {
                            userPredictions.add(item);
                        }

                        loadPredictionDetails(answerSnapshots, index + 1);

                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        loadPredictionDetails(answerSnapshots, index + 1);
                    }
                });
    }

    private boolean containsPrediction(String predictionId) {
        for (userPrediction item : userPredictions) {
            if (predictionId.equals(item.getPredictionId())) {
                return true;
            }
        }

        return false;
    }

    private userPrediction buildUserPrediction(
            String predictionId,
            DataSnapshot predictionSnapshot,
            userPredictionAnswer answer
    ) {
        if (!predictionSnapshot.exists()) {
            return null;
        }

        userPrediction item = new userPrediction();

        item.setPredictionId(predictionId);

        LocalizedText title = predictionSnapshot.child("title").getValue(LocalizedText.class);
        LocalizedText description = predictionSnapshot.child("description").getValue(LocalizedText.class);

        item.setTitle(title);
        item.setDescription(description);

        item.setCategory(getStringValue(predictionSnapshot, "category"));
        item.setPredictionType(getStringValue(predictionSnapshot, "predictionType"));
        item.setOptionType(getStringValue(predictionSnapshot, "optionType"));
        item.setSeason(getStringValue(predictionSnapshot, "season"));
        Integer raceRound = predictionSnapshot.child("raceRound").getValue(Integer.class);

        if (raceRound != null) {
            item.setRaceRound(raceRound);
        }

        item.setStatus(getStringValue(predictionSnapshot, "status"));
        item.setConstructorId(getStringValue(predictionSnapshot, "constructorId"));
        item.setDriverAId(getStringValue(predictionSnapshot, "driverAId"));
        item.setDriverATitle(getStringValue(predictionSnapshot, "driverATitle"));
        item.setDriverBId(getStringValue(predictionSnapshot, "driverBId"));
        item.setDriverBTitle(getStringValue(predictionSnapshot, "driverBTitle"));
        Long deadline = predictionSnapshot.child("deadline").getValue(Long.class);
        item.setDeadline(deadline);

        Double possiblePoints = predictionSnapshot.child("points").getValue(Double.class);

        if (possiblePoints == null) {
            Long integerPoints = predictionSnapshot.child("points").getValue(Long.class);

            if (integerPoints != null) {
                possiblePoints = integerPoints.doubleValue();
            }
        }

        item.setCorrectOptionId(predictionSnapshot.child("correctOptionId")
                .getValue(String.class));

        item.setMaxPoints(possiblePoints);
        item.setAnswerOptionId(answer.getOptionId());
        item.setAnswerOptionTitle(answer.getOptionTitle());
        item.setSubmittedAt(answer.getSubmittedAt());
        item.setAnswerUpdatedAt(answer.getUpdatedAt());
        item.setAnswerPoints(answer.getPoints());
        item.setAnswerResult(answer.getResult());

        return item;
    }

    private String getStringValue(DataSnapshot snapshot, String child) {
        return snapshot.child(child).getValue(String.class);
    }

    private void finishLoading() {
        userPredictions.sort(Comparator
                .comparing(userPrediction::getDeadline, Comparator.nullsLast(Long::compareTo))
        );

        adapter.notifyDataSetChanged();

        isLoading = false;

        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setRefreshing(false);
        }

        if (userPredictions.isEmpty()) {
            showEmpty(getString(R.string.user_predictions_empty));

            recyclerView.setVisibility(View.GONE);

            shimmerLayout.stopShimmer();
            shimmerLayout.setVisibility(View.GONE);

            return;
        }

        emptyText.setVisibility(View.GONE);

        hideShimmer(recyclerView, shimmerLayout);
    }

    private void showEmpty(String text) {
        emptyText.setText(text);
        emptyText.setVisibility(View.VISIBLE);
    }

    private void openPrediction(userPrediction prediction) {
        Intent intent = new Intent(this, predictionFormActivity.class);
        intent.putExtra("predictionId", prediction.getPredictionId());
        startActivity(intent);
    }

    private void attachSwipeActions(Context context, RecyclerView recyclerView) {
        ItemTouchHelper.SimpleCallback callback = new ItemTouchHelper.SimpleCallback(
                0,
                ItemTouchHelper.LEFT) {

            @Override
            public boolean onMove(
                    @NonNull RecyclerView recyclerView,
                    @NonNull RecyclerView.ViewHolder viewHolder,
                    @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getBindingAdapterPosition();
                if (position == RecyclerView.NO_POSITION || position >= userPredictions.size()) {
                    return;
                }

                userPrediction prediction = adapter.getItem(position);

                if (prediction == null) {
                    adapter.notifyItemChanged(position);
                    return;
                }

                if (!prediction.canBeChanged()) {
                    adapter.notifyItemChanged(position);
                    Toast.makeText(
                            userPredictionsActivity.this,
                            getString(R.string.user_predictions_update_ban),
                            Toast.LENGTH_SHORT
                    ).show();
                    return;
                }
                showDeleteConfirmation(
                        context,
                        prediction,
                        position
                );

                adapter.notifyItemChanged(position);
            }

            @Override
            public void onChildDraw(
                    @NonNull Canvas canvas,
                    @NonNull RecyclerView recyclerView,
                    @NonNull RecyclerView.ViewHolder viewHolder,
                    float dX, float dY,
                    int actionState,
                    boolean isCurrentlyActive) {

                if (actionState != ItemTouchHelper.ACTION_STATE_SWIPE) {
                    return;
                }

                View card = viewHolder.itemView.findViewById(R.id.cardView);
                View bg = viewHolder.itemView.findViewById(R.id.swipeBackground);

                float distance = Math.min(Math.max(0f, -dX), dpToPx(110)); // длина свайпа, всегда >= 0
                card.setTranslationX(-distance);

                if (distance > 0) {
                    bg.setVisibility(View.VISIBLE);
                    int radius = dpToPx(16); // радиус карточки
                    int w = Math.round(distance) + radius;
                    ViewGroup.LayoutParams lp = bg.getLayoutParams();
                    if (lp.width != w) {
                        lp.width = w;
                        bg.setLayoutParams(lp);
                    }
                } else {
                    bg.setVisibility(View.INVISIBLE);
                }
            }

            @Override
            public float getSwipeThreshold(@NonNull RecyclerView.ViewHolder viewHolder) {
                return 0.2f;
            }

            @Override
            public void onChildDrawOver(@NonNull Canvas c, @NonNull RecyclerView rv,
                                        @NonNull RecyclerView.ViewHolder vh,
                                        float dX, float dY, int actionState, boolean isCurrentlyActive) {
            }

            @Override
            public void clearView(@NonNull RecyclerView rv, @NonNull RecyclerView.ViewHolder vh) {
                super.clearView(rv, vh);
                View card = vh.itemView.findViewById(R.id.cardView);
                card.setTranslationX(0f);
                vh.itemView.findViewById(R.id.swipeBackground).setVisibility(View.INVISIBLE);
            }
        };

        new ItemTouchHelper(callback).attachToRecyclerView(recyclerView);
    }

    private void showDeleteConfirmation(Context context, userPrediction prediction, int position) {
        deleteDialog.show();

        deletePrediction.setOnClickListener(v ->{
                    deleteDialog.dismiss();
                    deletePredictionAnswer(context, prediction, position);
                });

        cancelDelete.setOnClickListener(v -> {
            deleteDialog.dismiss();
            adapter.notifyItemChanged(position);
        });
    }

    private void deletePredictionAnswer(Context context, userPrediction prediction, int position) {
        if (currentUser == null) {
            adapter.notifyItemChanged(position);
            return;
        }

        String predictionId = prediction.getPredictionId();

        if (predictionId == null || predictionId.trim().isEmpty()) {
            adapter.notifyItemChanged(position);

            Toast.makeText(this, getString(R.string.user_predictions_recognition_error),
                            Toast.LENGTH_SHORT).show();

            return;
        }

        Map<String, Object> data = new HashMap<>();
        data.put("predictionId", predictionId);

        functions.getHttpsCallable("deleteUserPrediction")
                .call(data)
                .addOnSuccessListener(result -> {
                    int currentPosition = findPredictionPosition(predictionId);

                    if (currentPosition != -1) {
                        userPredictions.remove(currentPosition);
                        adapter.notifyItemRemoved(currentPosition);
                    }

                    loadUserStats(context, currentPredictionSeason);

                    Toast.makeText(
                            this,
                            getString(R.string.user_predictions_delete_success),
                            Toast.LENGTH_SHORT
                    ).show();
                })
                .addOnFailureListener(error -> {
                    adapter.notifyItemChanged(position);

                    Toast.makeText(
                            this,
                            error.getMessage(),
                            Toast.LENGTH_SHORT
                    ).show();
                });
    }

    private int findPredictionPosition(String predictionId) {
        for (int i = 0; i < userPredictions.size(); i++) {

            if (predictionId.equals(userPredictions.get(i).getPredictionId())) {
                return i;
            }
        }

        return -1;
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (currentUser != null && adapter != null) {
            isLoading = false;
            loadUserAnswers(this);
            loadUserStats(this, currentPredictionSeason);
        }
    }
}