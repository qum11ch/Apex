package com.example.f1app;

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

        holder.title.setText(item.getTitle());
        holder.description.setText(item.getDescription());

        holder.category.setText("Категория: " + getCategoryTitle(item.getCategory()));

        holder.deadline.setText("Дедлайн: " + formatDate(item.getDeadline()));

        holder.points.setText("Очки: " + item.getPoints());

        holder.openButton.setOnClickListener(v -> listener.onPredictionClick(item));

        holder.itemView.setOnClickListener(v -> listener.onPredictionClick(item));
    }

    @Override
    public int getItemCount() {
        return predictions.size();
    }

    private String getCategoryTitle(String category) {
        if ("season".equals(category)) {
            return "Сезонный";
        }

        if ("weekend".equals(category)) {
            return "Гоночный уикенд";
        }

        return category == null ? "" : category;
    }

    private String formatDate(long timestamp) {
        if (timestamp <= 0) {
            return "не установлен";
        }

        SimpleDateFormat formatter = new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault());

        return formatter.format(new Date(timestamp));
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