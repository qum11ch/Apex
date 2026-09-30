package com.example.f1app;

import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.StyleSpan;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

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

import android.graphics.Color;
import android.widget.Toast;

import com.google.firebase.functions.FirebaseFunctions;

import java.util.HashMap;
import java.util.Map;

import androidx.recyclerview.widget.ItemTouchHelper;

public class userPredictionsActivity extends AppCompatActivity {
    private final List<userPrediction> userPredictions = new ArrayList<>();

    private userPredictionAdapter adapter;
    private TextView emptyText;
    private boolean isLoading = false;

    private DatabaseReference rootRef;
    private FirebaseUser currentUser;

    private TextView totalPointsText;
    private TextView predictionsCountText;
    private TextView correctPredictionsText;
    private TextView rankText;
    private PieChart predictionResultsChart;
    private LinearLayout chartLegend;
    private FirebaseFunctions functions;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.user_predictions_activity);

        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        functions = FirebaseFunctions.getInstance();
        rootRef = FirebaseDatabase.getInstance().getReference();

        RecyclerView recyclerView = findViewById(R.id.userPredictionsRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new userPredictionAdapter(userPredictions, this::openPrediction);
        recyclerView.setAdapter(adapter);
        attachSwipeActions(userPredictionsActivity.this, recyclerView);

        emptyText = findViewById(R.id.userPredictionsEmptyText);
        totalPointsText = findViewById(R.id.totalPointsText);
        predictionsCountText = findViewById(R.id.predictionsCountText);
        correctPredictionsText = findViewById(R.id.correctPredictionsText);
        rankText = findViewById(R.id.rankText);
        predictionResultsChart = findViewById(R.id.predictionResultsChart);

        chartLegend = findViewById(R.id.chartLegend);

        if (currentUser == null) {
            showEmpty("Войдите в аккаунт, чтобы увидеть прогнозы");
            return;
        }

        loadUserStats(userPredictionsActivity.this);
        loadUserAnswers();
    }

    private void loadUserAnswers() {
        if (currentUser == null || isLoading) {
            return;
        }

        isLoading = true;
        userPredictions.clear();
        adapter.notifyDataSetChanged();

        rootRef.child("userPredictionAnswers").child(currentUser.getUid())
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        userPredictions.clear();

                        if (!snapshot.exists()) {
                            adapter.notifyDataSetChanged();
                            showEmpty("Вы ещё не сделали прогнозов");

                            return;
                        }

                        List<DataSnapshot> answerSnapshots = new ArrayList<>();

                        for (DataSnapshot answerSnapshot : snapshot.getChildren()) {
                            answerSnapshots.add(answerSnapshot);
                        }

                        if (answerSnapshots.isEmpty()) {
                            showEmpty("Вы ещё не сделали прогнозов");
                            return;
                        }

                        loadPredictionDetails(answerSnapshots, 0);

                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        showEmpty("Ошибка загрузки прогнозов");
                    }
                });
    }

    private void loadUserStats(Context context) {
        if (currentUser == null) {
            return;
        }

        rootRef.child("userPredictionStats").child(currentUser.getUid())
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (!snapshot.exists()) {
                            showEmptyStats();
                            return;
                        }

                        userPredictionStats stats = snapshot.getValue(userPredictionStats.class);

                        if (stats == null) {
                            showEmptyStats();
                            return;
                        }

                        Double totalPoints = valueOrZeroDouble(stats.getTotalPoints());
                        long predictionsCount = valueOrZero(stats.getPredictionsCount());
                        long correctPredictions = valueOrZero(stats.getCorrectPredictions());
                        long incorrectPredictions = valueOrZero(stats.getIncorrectPredictions());
                        long pendingPredictions = valueOrZero(stats.getPendingPredictions());
                        long tiePredictions = valueOrZero(stats.getTiePredictions());
                        // Long rank = stats.getRank();

                        totalPointsText.setText("Всего очков: " + totalPoints);
                        predictionsCountText.setText("Прогнозов: " + predictionsCount);
                        correctPredictionsText.setText("Угадано: " + correctPredictions);

                        // if (rank == null || rank <= 0) {
                        //     rankText.setText("Место: пока нет");
                        // } else {
                        //     rankText.setText("Место: " + rank);
                        // }

                        renderPredictionResultsChart(context, correctPredictions,
                                incorrectPredictions, pendingPredictions, tiePredictions);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        showEmptyStats();
                    }
                });
    }

    private long valueOrZero(Long value) {
        return value == null ? 0 : value;
    }

    private double valueOrZeroDouble(Double value) {
        return value == null ? 0.0 : value;
    }

    private void showEmptyStats() {
        totalPointsText.setText("Всего очков: 0");
        predictionsCountText.setText("Прогнозов: 0");
        correctPredictionsText.setText("Угадано: 0");
        rankText.setText("Место: пока нет");

        predictionResultsChart.setVisibility(View.GONE);
        chartLegend.removeAllViews();
    }

    private void renderPredictionResultsChart(Context context, long correct,
                                              long incorrect, long pending, long tie) {
        ArrayList<PieEntry> entries = new ArrayList<>();
        ArrayList<Integer> colors = new ArrayList<>();

        if (correct > 0) {
            entries.add(new PieEntry(correct, "Угадано"));
            colors.add(ContextCompat.getColor(context, R.color.correct_predict_color));
        }

        if (incorrect > 0) {
            entries.add(new PieEntry(incorrect, "Не угадано"));
            colors.add(ContextCompat.getColor(context, R.color.incorrect_predict_color));
        }

        if (pending > 0) {
            entries.add(new PieEntry(pending, "Ожидают"));
            colors.add(ContextCompat.getColor(context, R.color.pending_predict_color));
        }

        if (tie > 0) {
            entries.add(new PieEntry(tie, "Ничья"));
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
            return "Нет\nрезультатов";
        }

        int accuracy = (int) Math.round(correct * 100.0 / completed);
        return accuracy + "%\nточность";
    }

    private void renderChartLegend(Context context,
                                   long correct,
                                   long incorrect,
                                   long pending,
                                   long tie) {

        chartLegend.removeAllViews();

        chartLegend.post(() -> {
            int availableWidth =
                    chartLegend.getWidth()
                            - chartLegend.getPaddingLeft()
                            - chartLegend.getPaddingRight();

            if (availableWidth <= 0) {
                return;
            }

            ArrayList<LegendData> items = new ArrayList<>();

            if (correct > 0) {
                items.add(new LegendData(
                        "Угадано",
                        correct,
                        ContextCompat.getColor(
                                context,
                                R.color.correct_predict_color
                        )
                ));
            }

            if (incorrect > 0) {
                items.add(new LegendData(
                        "Не угадано",
                        incorrect,
                        ContextCompat.getColor(
                                context,
                                R.color.incorrect_predict_color
                        )
                ));
            }

            if (pending > 0) {
                items.add(new LegendData(
                        "Ожидают",
                        pending,
                        ContextCompat.getColor(
                                context,
                                R.color.pending_predict_color
                        )
                ));
            }

            if (tie > 0) {
                items.add(new LegendData(
                        "Ничья",
                        tie,
                        ContextCompat.getColor(
                                context,
                                R.color.tie_predict_color
                        )
                ));
            }

            buildLegendRows(context, items, availableWidth);
        });
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

        titleView.setText(title + ": ");
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
        userPredictions.sort(
                Comparator.comparing(
                        userPrediction::getDeadline,
                        Comparator.nullsLast(Long::compareTo)
                )
        );
        adapter.notifyDataSetChanged();

        isLoading = false;

        if (userPredictions.isEmpty()) {
            showEmpty("Вы ещё не сделали прогнозов");
        } else {
            emptyText.setVisibility(View.GONE);
        }
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
        ItemTouchHelper.SimpleCallback callback = new ItemTouchHelper.SimpleCallback(0,
                ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView,
                                  @NonNull RecyclerView.ViewHolder viewHolder,
                                  @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getBindingAdapterPosition();

                if (position == RecyclerView.NO_POSITION) {return;}

                userPrediction prediction = adapter.getItem(position);

                if (prediction == null) {
                    adapter.notifyItemChanged(position);
                    return;
                }

                if (!prediction.canBeChanged()) {
                    adapter.notifyItemChanged(position);
                    Toast.makeText(userPredictionsActivity.this,
                            "Этот прогноз уже нельзя изменить", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (direction == ItemTouchHelper.LEFT) {
                    openPredictionForEdit(prediction);
                } else {
                    showDeleteConfirmation(context, prediction, position);
                }
                adapter.notifyItemChanged(position);
            }
        };
        new ItemTouchHelper(callback).attachToRecyclerView(recyclerView);
    }

    private void openPredictionForEdit(userPrediction prediction) {
        Intent intent = new Intent(this, predictionFormActivity.class);
        intent.putExtra("predictionId", prediction.getPredictionId());
        intent.putExtra("editMode", true);
        startActivity(intent);
    }

    private void showDeleteConfirmation(Context context, userPrediction prediction, int position) {
        new AlertDialog.Builder(this).setTitle("Удалить прогноз?")
                .setMessage("Ваш ответ будет удалён. " +
                        "Вы сможете отправить его заново, " +
                        "пока прогноз открыт.")
                .setNegativeButton(
                        "Отмена",
                        (dialog, which) ->
                                adapter.notifyItemChanged(position))
                .setPositiveButton(
                        "Удалить",
                        (dialog, which) ->
                                deletePredictionAnswer(context, prediction, position)
                )
                .setOnCancelListener(dialog ->
                        adapter.notifyItemChanged(position))
                .show();
    }

    private void deletePredictionAnswer(Context context, userPrediction prediction, int position) {
        if (currentUser == null) {
            adapter.notifyItemChanged(position);
            return;
        }

        String predictionId = prediction.getPredictionId();

        if (predictionId == null || predictionId.trim().isEmpty()) {
            adapter.notifyItemChanged(position);

            Toast.makeText(this, "Не удалось определить прогноз", Toast.LENGTH_SHORT)
                    .show();

            return;
        }

        Map<String, Object> data = new HashMap<>();
        data.put("predictionId", predictionId);

        functions
                .getHttpsCallable("deleteUserPrediction")
                .call(data)
                .addOnSuccessListener(result -> {
                    int currentPosition = findPredictionPosition(predictionId);

                    if (currentPosition != -1) {
                        userPredictions.remove(currentPosition);
                        adapter.notifyItemRemoved(currentPosition);
                    }

                    loadUserStats(context);

                    Toast.makeText(
                            this,
                            "Ответ удалён",
                            Toast.LENGTH_SHORT
                    ).show();
                })
                .addOnFailureListener(error -> {
                    adapter.notifyItemChanged(position);

                    Toast.makeText(
                            this,
                            "Не удалось удалить ответ",
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

        if (currentUser != null && adapter != null && !isLoading) {
            loadUserAnswers();
            loadUserStats(userPredictionsActivity.this);
        }
    }
}