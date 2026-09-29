package com.example.f1app;

import android.content.Intent;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
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

import java.util.ArrayList;
import android.graphics.Color;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.google.firebase.functions.FirebaseFunctions;
import com.google.firebase.functions.HttpsCallableResult;

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
        attachSwipeActions(recyclerView);

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

        loadUserStats();
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

    private void loadUserStats() {
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

                        long totalPoints = valueOrZero(stats.getTotalPoints());
                        long predictionsCount = valueOrZero(stats.getPredictionsCount());
                        long correctPredictions = valueOrZero(stats.getCorrectPredictions());
                        long incorrectPredictions = valueOrZero(stats.getIncorrectPredictions());
                        long pendingPredictions = valueOrZero(stats.getPendingPredictions());
                        Long rank = stats.getRank();

                        totalPointsText.setText("Всего очков: " + totalPoints);
                        predictionsCountText.setText("Прогнозов: " + predictionsCount);
                        correctPredictionsText.setText("Угадано: " + correctPredictions);

                        if (rank == null || rank <= 0) {
                            rankText.setText("Место: пока нет");
                        } else {
                            rankText.setText("Место: " + rank);
                        }

                        renderPredictionResultsChart(correctPredictions,
                                incorrectPredictions, pendingPredictions);
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

    private void showEmptyStats() {
        totalPointsText.setText("Всего очков: 0");
        predictionsCountText.setText("Прогнозов: 0");
        correctPredictionsText.setText("Угадано: 0");
        rankText.setText("Место: пока нет");

        predictionResultsChart.setVisibility(View.GONE);
        chartLegend.removeAllViews();
    }

    private void renderPredictionResultsChart(long correct, long incorrect, long pending) {
        ArrayList<PieEntry> entries = new ArrayList<>();
        ArrayList<Integer> colors = new ArrayList<>();

        if (correct > 0) {
            entries.add(new PieEntry(correct, "Угадано"));
            colors.add(Color.rgb(46, 125, 80)
            );
        }

        if (incorrect > 0) {
            entries.add(new PieEntry(incorrect, "Не угадано"));
            colors.add(Color.rgb(211, 47, 47));
        }

        if (pending > 0) {
            entries.add(new PieEntry(pending, "Ожидают"));
            colors.add(Color.rgb(245, 166, 35));
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
        predictionResultsChart.setHoleRadius(62f);
        predictionResultsChart.setTransparentCircleRadius(64f);
        predictionResultsChart.setCenterText(getAccuracyText(correct, incorrect));
        predictionResultsChart.setCenterTextSize(16f);
        predictionResultsChart.setCenterTextColor(Color.DKGRAY);
        predictionResultsChart.setDrawEntryLabels(false);

        predictionResultsChart.getLegend().setEnabled(false);

        // predictionResultsChart.getLegend().setTextSize(12f);
        // predictionResultsChart.getLegend().setWordWrapEnabled(true);
        // predictionResultsChart.getLegend().setOrientation(Legend.LegendOrientation.HORIZONTAL);
        //
        // predictionResultsChart.getLegend()
                //         .setHorizontalAlignment(Legend.LegendHorizontalAlignment.CENTER);
        //
        // predictionResultsChart.getLegend()
                //         .setVerticalAlignment(Legend.LegendVerticalAlignment.BOTTOM);
        //
        // predictionResultsChart.getLegend().setForm(Legend.LegendForm.CIRCLE);
        //
        // predictionResultsChart.getLegend().setFormSize(10f);

        predictionResultsChart.animateY(500);
        predictionResultsChart.invalidate();

        renderChartLegend(correct, incorrect, pending);
    }

    private String getAccuracyText(long correct, long incorrect) {
        long completed = correct + incorrect;

        if (completed == 0) {
            return "Нет\nрезультатов";
        }

        int accuracy = (int) Math.round(correct * 100.0 / completed);
        return accuracy + "%\nточность";
    }

    private void renderChartLegend(long correct, long incorrect, long pending) {
        chartLegend.removeAllViews();

        addLegendItem("Угадано", correct, Color.rgb(46, 125, 80));

        addLegendItem("Не угадано", incorrect, Color.rgb(211, 47, 47));

        addLegendItem("Ожидают", pending, Color.rgb(245, 166, 35));
    }

    private void addLegendItem(String title, long value, int color) {
        LinearLayout item = new LinearLayout(this);

        item.setOrientation(LinearLayout.HORIZONTAL);
        item.setGravity(Gravity.CENTER_VERTICAL);
        item.setPadding(8, 8, 20, 4);

        View marker = new View(this);

        GradientDrawable markerBackground = new GradientDrawable();

        markerBackground.setColor(color);
        markerBackground.setShape(GradientDrawable.OVAL);
        marker.setBackground(markerBackground);

        LinearLayout.LayoutParams markerParams = new LinearLayout.LayoutParams(20, 20);
        markerParams.setMargins(0, 0, 30, 0);

        item.addView(marker, markerParams);

        TextView valueView = new TextView(this);

        valueView.setText(String.valueOf(value));

        TextView titleView = new TextView(this);
        titleView.setText(title + ": ");

        titleView.setTextSize(14);
        titleView.setTextColor(Color.rgb(80, 80, 80));
        item.addView(titleView);

        valueView.setTextSize(14);
        valueView.setTextColor(Color.rgb(40, 40, 40));
        valueView.setTypeface(null, Typeface.BOLD);
        item.addView(valueView);

        chartLegend.addView(item);
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

    private userPrediction buildUserPrediction(String predictionId, DataSnapshot predictionSnapshot,
                                               userPredictionAnswer answer) {
        if (!predictionSnapshot.exists()) {return null;}

        userPrediction item = new userPrediction();

        item.setPredictionId(predictionId);

        item.setTitle(getStringValue(predictionSnapshot, "title"));
        item.setDescription(getStringValue(predictionSnapshot, "description"));
        item.setCategory(getStringValue(predictionSnapshot, "category"));
        item.setType(getStringValue(predictionSnapshot, "type"));
        item.setSeason(getStringValue(predictionSnapshot, "season"));
        item.setRaceId(getStringValue(predictionSnapshot, "raceId"));
        item.setOptionType(getStringValue(predictionSnapshot, "optionType"));

        item.setRaceName(getStringValue(predictionSnapshot, "raceName"));

        item.setPredictionStatus(getStringValue(predictionSnapshot, "status"));

        Long deadline = predictionSnapshot.child("deadline").getValue(Long.class);

        if (deadline != null) {
            item.setDeadline(deadline);
        }

        Long points = predictionSnapshot.child("points").getValue(Long.class);

        if (points != null) {
            item.setPossiblePoints(points.intValue());
        }

        item.setOptionId(answer.getOptionId());
        item.setOptionTitle(answer.getOptionTitle());
        item.setSubmittedAt(answer.getSubmittedAt());
        item.setUpdatedAt(answer.getUpdatedAt());
        item.setPoints(answer.getPoints());
        item.setResult(answer.getResult());

        return item;
    }

    private String getStringValue(DataSnapshot snapshot, String child) {
        return snapshot.child(child).getValue(String.class);
    }

    private void finishLoading() {
        userPredictions.sort(Comparator.comparingLong(userPrediction::getDeadline));
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

    private void attachSwipeActions(RecyclerView recyclerView) {
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
                    showDeleteConfirmation(prediction, position);
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

    private void showDeleteConfirmation(userPrediction prediction, int position) {
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
                                deletePredictionAnswer(prediction, position)
                )
                .setOnCancelListener(dialog ->
                        adapter.notifyItemChanged(position))
                .show();
    }

    private void deletePredictionAnswer(userPrediction prediction, int position) {
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

                    loadUserStats();

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
            loadUserStats();
        }
    }
}