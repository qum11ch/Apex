package com.example.f1app;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class predictionsActivity
        extends AppCompatActivity {

    private RecyclerView recyclerView;
    private TextView emptyText;

    private final List<predictionListItem> predictions = new ArrayList<>();

    private predictionListAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.predictions_activity);

        recyclerView = findViewById(R.id.predictionsRecyclerView);

        emptyText = findViewById(R.id.emptyPredictionsText);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new predictionListAdapter(predictions, this::openPrediction);

        recyclerView.setAdapter(adapter);

        loadPredictions();
    }

    private void loadPredictions() {
        DatabaseReference predictionsRef = FirebaseDatabase.getInstance().getReference("userPredictions");

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

                        Collections.sort(
                                predictions,
                                Comparator.comparing(
                                        predictionListItem::getDeadline,
                                        Comparator.nullsLast(Long::compareTo)
                                )
                        );

                        adapter.notifyDataSetChanged();

                        boolean isEmpty = predictions.isEmpty();
                        emptyText.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
                        recyclerView.setVisibility(isEmpty ? View.GONE : View.VISIBLE);

                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        predictions.clear();
                        adapter.notifyDataSetChanged();

                        emptyText.setText("Ошибка загрузки прогнозов");

                        emptyText.setVisibility(View.VISIBLE);
                    }
                });
    }

    private void openPrediction(predictionListItem item) {
        Intent intent = new Intent(this, predictionFormActivity.class);

        intent.putExtra("predictionId", item.getPredictionId());
        startActivity(intent);
    }
}