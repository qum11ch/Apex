package com.example.f1app;

import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

/** The card of the home screen that leads to the predictions; it does not depend on season time */
final class predictionPromoBlock {
    private final Context context;
    private final View card;
    private final TextView caption, title, subtitle, cta;
    private final Supplier<String> season;
    private int token = 0;

    predictionPromoBlock(View card, Supplier<String> season) {
        this.context = card.getContext();
        this.card = card;
        this.season = season;
        caption = card.findViewById(R.id.promoCaption);
        title = card.findViewById(R.id.promoTitle);
        subtitle = card.findViewById(R.id.promoSubtitle);
        cta = card.findViewById(R.id.promoCta);
    }

    void stop() {
        token++;
    }

    void refresh() {
        final int mine = ++token;

        FirebaseDatabase.getInstance().getReference("userPredictions")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (mine != token) {
                            return;
                        }

                        long now = System.currentTimeMillis();
                        List<predictionListItem> open = new ArrayList<>();

                        for (DataSnapshot child : snapshot.getChildren()) {
                            predictionListItem item = child.getValue(predictionListItem.class);

                            if (item == null) {
                                continue;
                            }

                            if (item.getStatus() != null && !"open".equals(item.getStatus())) {
                                continue;
                            }

                            Long deadline = item.getDeadline();

                            if (deadline == null || deadline <= now) {
                                continue;
                            }

                            item.setPredictionId(child.getKey());
                            open.add(item);
                        }

                        open.sort((a, b) -> Long.compare(a.getDeadline(), b.getDeadline()));

                        if (open.isEmpty()) {
                            showResults();
                            return;
                        }

                        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

                        if (user == null) {
                            showOpen(R.string.promo_open_title, open.size(), open.get(0), now);
                            return;
                        }

                        loadAnswers(mine, user.getUid(), open);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        // the card keeps what it showed before; nothing is shown the first time
                    }
                });
    }

    private void loadAnswers(int mine, String uid, List<predictionListItem> open) {
        FirebaseDatabase.getInstance().getReference("userPredictionAnswers").child(uid)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (mine != token) {
                            return;
                        }

                        Set<String> answered = new HashSet<>();

                        for (DataSnapshot answer : snapshot.getChildren()) {
                            if (answer.getKey() != null) {
                                answered.add(answer.getKey());
                            }
                        }

                        List<predictionListItem> left = new ArrayList<>();

                        for (predictionListItem item : open) {
                            if (!answered.contains(item.getPredictionId())) {
                                left.add(item);
                            }
                        }

                        long now = System.currentTimeMillis();

                        if (left.isEmpty()) {
                            showAllDone(open.get(0), now);
                        } else {
                            showOpen(R.string.promo_left_title, left.size(), left.get(0), now);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        if (mine == token) {
                            showOpen(R.string.promo_open_title, open.size(), open.get(0),
                                    System.currentTimeMillis());
                        }
                    }
                });
    }

    private void showOpen(int titleRes, int count, predictionListItem nearest, long now) {
        caption.setText(R.string.promo_caption);
        title.setText(context.getString(titleRes, count));
        subtitle.setText(closing(nearest, now));
        cta.setText(R.string.promo_cta);
        card.setOnClickListener(v ->
                context.startActivity(new Intent(context, predictionActivity.class)));
        card.setVisibility(View.VISIBLE);
    }

    private void showAllDone(predictionListItem nearest, long now) {
        caption.setText(R.string.promo_caption);
        title.setText(R.string.promo_done_title);
        subtitle.setText(closing(nearest, now));
        cta.setText(R.string.promo_done_cta);
        card.setOnClickListener(v ->
                context.startActivity(new Intent(context, userPredictionsActivity.class)));
        card.setVisibility(View.VISIBLE);
    }

    private void showResults() {
        caption.setText(R.string.promo_caption);
        title.setText(R.string.promo_results_title);
        subtitle.setText(R.string.promo_results_body);
        cta.setText(R.string.promo_results_cta);
        card.setOnClickListener(v -> {
            String value = season.get();

            if (value == null || value.trim().isEmpty()) {
                value = String.valueOf(LocalDate.now().getYear());
            }

            Intent intent = new Intent(context, leaderboardActivity.class);
            intent.putExtra("currentSeason", value);
            context.startActivity(intent);
        });
        card.setVisibility(View.VISIBLE);
    }

    private String closing(predictionListItem item, long now) {
        return item.getLocalizedTitle() + "\n"
                + context.getString(R.string.promo_closes, inText(item.getDeadline() - now));
    }

    private String inText(long millis) {
        long minutes = Math.max(1, millis / 60_000L);

        if (minutes >= 24 * 60) {
            return context.getString(R.string.in_days, minutes / (24 * 60));
        }

        if (minutes >= 60) {
            return context.getString(R.string.in_hours, minutes / 60);
        }

        return context.getString(R.string.in_minutes, minutes);
    }
}