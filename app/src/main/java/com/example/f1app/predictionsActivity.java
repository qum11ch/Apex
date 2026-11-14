package com.example.f1app;

import static com.example.f1app.MainActivity.checkConnection;
import static com.example.f1app.MainActivity.hideShimmer;
import static com.example.f1app.MainActivity.showShimmer;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.facebook.shimmer.ShimmerFrameLayout;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class predictionsActivity extends AppCompatActivity {
    private static final String CACHE_KEY = "open_predictions";
    private RecyclerView recyclerView;
    private TextView emptyText;

    private boolean isLoading = false;

    private final List<predictionListItem> predictions = new ArrayList<>();

    private predictionListAdapter adapter;

    private SwipeRefreshLayout swipeRefreshLayout;
    private ShimmerFrameLayout shimmerLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.predictions_activity);

        setupPredictionsInsets();
        EdgeInsets.applyBottom(findViewById(R.id.predictionsRecyclerView));

        networkNotice.showIfOffline(predictionsActivity.this);

        WindowInsetsControllerCompat windowInsetsController =
                WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        windowInsetsController.setAppearanceLightStatusBars(false);

        recyclerView = findViewById(R.id.predictionsRecyclerView);
        shimmerLayout = findViewById(R.id.shimmer_layout);
        swipeRefreshLayout = findViewById(R.id.swipe_layout);
        emptyText = findViewById(R.id.emptyPredictionsText);

        ImageButton backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> finish());

        swipeRefreshLayout.setOnChildScrollUpCallback(
                (parent, child) ->
                        recyclerView.getVisibility() == View.VISIBLE
                                && recyclerView.canScrollVertically(-1)
        );

        findViewById(R.id.predictionRulesButton)
                .setOnClickListener(v ->
                        userPredictionRules.show(this)
                );

        swipeRefreshLayout.setOnRefreshListener(() -> {
            if (isLoading) {
                swipeRefreshLayout.setRefreshing(false);
                return;
            }

            loadPredictions(predictionsActivity.this);
        });

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new predictionListAdapter(predictions, this::openPrediction);
        recyclerView.setAdapter(adapter);

        final List<predictionListItem> remembered = cachedList.recall(CACHE_KEY);

        if (remembered != null) {
            // seen before: show it at once, refresh quietly
            predictions.addAll(remembered);
            adapter.notifyDataSetChanged();
            shimmerLayout.stopShimmer();
            shimmerLayout.setVisibility(View.GONE);

            if (predictions.isEmpty()) {
                recyclerView.setVisibility(View.GONE);
                showEmpty(getString(R.string.user_predictions_empty));
            } else {
                recyclerView.setVisibility(View.VISIBLE);
                emptyText.setVisibility(View.GONE);
            }
        }

        loadPredictions(predictionsActivity.this);
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();

        userPredictionRules.showIfNeeded(this);
    }

    private void setupPredictionsInsets() {
        View root = findViewById(R.id.predictions_root);
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

    private void loadPredictions(Context context) {
        isLoading = true;
        DatabaseReference predictionsRef = FirebaseDatabase.getInstance().getReference("userPredictions");

        if (cachedList.recall(CACHE_KEY) == null) {
            showShimmer(recyclerView, shimmerLayout);
        }

        predictionsRef.addListenerForSingleValueEvent(
                new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        predictions.clear();

                        long now = System.currentTimeMillis();

                        for (DataSnapshot child : snapshot.getChildren()) {

                            predictionListItem item = child.getValue(predictionListItem.class);

                            if (item == null) {continue;}

                            item.setPredictionId(child.getKey());

                            if (item.getStatus() != null && !"open".equals(item.getStatus())) {
                                continue;
                            }

                            Long deadline = item.getDeadline();

                            if (deadline == null || deadline <= now) {
                                continue;
                            }

                            predictions.add(item);
                        }

                        finishLoading();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        isLoading = false;

                        if (swipeRefreshLayout != null) {
                            swipeRefreshLayout.setRefreshing(false);
                        }

                        if (cachedList.recall(CACHE_KEY) != null) {
                            return; // the remembered list stays on screen
                        }

                        recyclerView.setVisibility(View.GONE);

                        shimmerLayout.stopShimmer();
                        shimmerLayout.setVisibility(View.GONE);

                        showEmpty((String) context.getText(R.string.prediction_load_error));
                    }
                });
    }

    private void finishLoading() {
        Collections.sort(
                predictions,
                Comparator.comparing(
                        predictionListItem::getDeadline,
                        Comparator.nullsLast(Long::compareTo)
                )
        );

        adapter.notifyDataSetChanged();
        cachedList.remember(CACHE_KEY, predictions);

        isLoading = false;

        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setRefreshing(false);
        }

        if (predictions.isEmpty()) {
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

    private void openPrediction(predictionListItem item) {
        Intent intent = new Intent(this, predictionFormActivity.class);

        intent.putExtra("predictionId", item.getPredictionId());
        startActivity(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();

        isLoading = false;
        loadPredictions(this);
    }
}