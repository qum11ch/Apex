package com.example.f1app;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class weekendPredictionsSheet extends neuroBaseSheet {
    private static final String ARG_SEASON = "season";
    private static final String ARG_ROUND = "round";

    private final List<predictionListItem> items = new ArrayList<>();
    private predictionListAdapter adapter;
    private ProgressBar progress;
    private TextView empty;

    public static weekendPredictionsSheet newInstance(String season, int round) {
        weekendPredictionsSheet sheet = new weekendPredictionsSheet();
        Bundle args = new Bundle();
        args.putString(ARG_SEASON, season);
        args.putInt(ARG_ROUND, round);
        sheet.setArguments(args);
        return sheet;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle state) {
        LinearLayout root = root();

        TextView title = text(getString(R.string.weekend_predictions_button), 22, R.color.text_color_main);
        title.setTypeface(title.getTypeface(), Typeface.BOLD);
        root.addView(title);

        progress = new ProgressBar(requireContext());
        LinearLayout.LayoutParams pp = new LinearLayout.LayoutParams(dp(40), dp(40));
        pp.gravity = android.view.Gravity.CENTER_HORIZONTAL;
        pp.topMargin = dp(20);
        root.addView(progress, pp);

        empty = text(getString(R.string.user_predictions_empty), 16, R.color.text_color_thirdly);
        empty.setVisibility(View.GONE);
        LinearLayout.LayoutParams ep = new LinearLayout.LayoutParams(-1, -2);
        ep.topMargin = dp(20);
        root.addView(empty, ep);

        RecyclerView list = new RecyclerView(requireContext());
        list.setLayoutManager(new LinearLayoutManager(requireContext()));
        list.setOverScrollMode(View.OVER_SCROLL_NEVER);
        adapter = new predictionListAdapter(items, this::openPrediction, true);
        list.setAdapter(adapter);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.topMargin = dp(8);
        root.addView(list, lp);

        loadPredictions();
        return root;
    }

    private void loadPredictions() {
        Bundle args = getArguments();
        final String season = args == null ? null : args.getString(ARG_SEASON);
        final int round = args == null ? -1 : args.getInt(ARG_ROUND, -1);

        if (season == null || round < 1) {
            showResult();
            return;
        }

        FirebaseDatabase.getInstance().getReference("userPredictions")
                .orderByChild("raceRound").equalTo(round)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (!isAdded()) return;

                        long now = System.currentTimeMillis();
                        items.clear();

                        for (DataSnapshot child : snapshot.getChildren()) {
                            predictionListItem item = child.getValue(predictionListItem.class);
                            if (item == null) continue;

                            item.setPredictionId(child.getKey());

                            if (!item.isWeekend()) continue;
                            if (!season.equals(item.getSeason())) continue;
                            if (item.getRaceRound() == null || item.getRaceRound() != round) continue;
                            if (item.getStatus() != null && !"open".equals(item.getStatus())) continue;
                            if (item.getDeadline() == null || item.getDeadline() <= now) continue;

                            items.add(item);
                        }

                        // Collections.sort(items, Comparator.comparing(
                        //         predictionListItem::getDeadline,
                        //         Comparator.nullsLast(Long::compareTo)));

                        Collections.sort(items, (a, b) -> {
                            int byDeadline = Long.compare(a.getDeadline(), b.getDeadline());
                            if (byDeadline != 0) return byDeadline;
                            double pointsA = a.getPoints() == null ? 0 : a.getPoints();
                            double pointsB = b.getPoints() == null ? 0 : b.getPoints();
                            return Double.compare(pointsB, pointsA);
                        });

                        showResult();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        if (!isAdded()) return;
                        items.clear();
                        showResult();
                    }
                });
    }

    private void showResult() {
        progress.setVisibility(View.GONE);
        empty.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
        adapter.notifyDataSetChanged();
    }

    private void openPrediction(predictionListItem item) {
        Intent intent = new Intent(requireContext(), predictionFormActivity.class);
        intent.putExtra("predictionId", item.getPredictionId());
        startActivity(intent);
        dismiss();
    }
}