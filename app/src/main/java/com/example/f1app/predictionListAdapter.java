package com.example.f1app;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class predictionListAdapter extends RecyclerView.Adapter<predictionListAdapter.ViewHolder> {

    public interface OnPredictionClickListener {
        void onPredictionClick(
                predictionListItem prediction
        );
    }

    private final List<predictionListItem> predictions;
    private final OnPredictionClickListener listener;

    public predictionListAdapter(List<predictionListItem> predictions, OnPredictionClickListener listener) {
        this.predictions = predictions;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(
                R.layout.item_prediction,
                parent,
                false
        );

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        predictionListItem item = predictions.get(position);

        holder.title.setText(item.getLocalizedTitle());
        holder.description.setText(
                getDescriptionText(item)
        );

        holder.category.setText(getMetaText(holder, item));
        holder.deadline.setText("Дедлайн: " + formatDate(item.getDeadline()));
        holder.points.setText("Очки: " + item.getPoints().intValue());
        holder.openButton.setOnClickListener(v -> listener.onPredictionClick(item));
        holder.itemView.setOnClickListener(v -> listener.onPredictionClick(item));
    }

    @Override
    public int getItemCount() {
        return predictions.size();
    }

    private String getMetaText(ViewHolder holder, predictionListItem item) {
        StringBuilder text = new StringBuilder();

        text.append(getCategoryTitle(
                holder.itemView.getContext(),
                item.getCategory()));

        if (item.isWeekend()
                && item.getSeason() != null
                && item.getRaceRound() != null) {
            text.append(" • ")
                    .append(item.getSeason())
                    .append(" • Раунд ")
                    .append(item.getRaceRound());
        } else if (item.getSeason() != null) {
            text.append(" • ")
                    .append(item.getSeason());
        }

        String typeTitle = getPredictionTypeTitle(
                item.getPredictionType()
        );

        if (!typeTitle.isEmpty()) {
            text.append(" • ").append(typeTitle);
        }

        return text.toString();
    }


    private String getPredictionTypeTitle(String predictionType) {
        if ("champion".equals(predictionType)) {
            return "Чемпион";
        }

        if ("winner".equals(predictionType)) {
            return "Победитель";
        }

        if ("race_head_to_head".equals(predictionType)) {
            return "Гоночное противостояние";
        }

        if ("qualifying_head_to_head".equals(predictionType)) {
            return "Квалификационное противостояние";
        }

        if ("team_points_head_to_head".equals(predictionType)) {
            return "Очковое противостояние";
        }

        if ("safety_car".equals(predictionType)) {
            return "Safety Car";
        }

        if ("rain".equals(predictionType)) {
            return "Дождь";
        }

        if ("boolean_prediction".equals(predictionType)) {
            return "Да / Нет";
        }

        return "";
    }

    private String getDescriptionText(predictionListItem item) {
        String description = item.getLocalizedDescription();

        if (item.isHeadToHead()) {
            String matchup = safe(item.getDriverATitle())
                    + " vs "
                    + safe(item.getDriverBTitle());

            if (description.isEmpty()) {
                return matchup;
            }

            return description + "\n" + matchup;
        }

        if (item.isBooleanPrediction()) {
            if (description.isEmpty()) {
                return "Да / Нет";
            }
        }

        return description;
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private String formatDate(Long timestamp) {
        if (timestamp == null || timestamp <= 0) {
            return "не установлен";
        }

        SimpleDateFormat formatter =
                new SimpleDateFormat(
                        "dd.MM.yyyy HH:mm",
                        Locale.getDefault()
                );

        return formatter.format(new Date(timestamp));
    }

    private String getCategoryTitle(
            Context context,
            String category
    ) {
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

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView title;
        TextView description;
        TextView category;
        TextView deadline;
        TextView points;
        Button openButton;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.predictionItemTitle);
            description = itemView.findViewById(R.id.predictionItemDescription);
            category = itemView.findViewById(R.id.predictionItemCategory);
            deadline = itemView.findViewById(R.id.predictionItemDeadline);
            points = itemView.findViewById(R.id.predictionItemPoints);
            openButton = itemView.findViewById(R.id.openPredictionButton);
        }
    }
}