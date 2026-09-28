package com.example.f1app;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class userPredictionAdapter extends RecyclerView.Adapter<userPredictionAdapter.ViewHolder> {

    public interface OnPredictionClickListener {
        void onPredictionClick(
                userPrediction prediction
        );
    }

    public userPrediction getItem(int position) {
        if (position < 0 || position >= predictions.size()) {
            return null;
        }

        return predictions.get(position);
    }
    private final List<userPrediction> predictions;
    private final OnPredictionClickListener listener;

    public userPredictionAdapter(List<userPrediction> predictions, OnPredictionClickListener listener) {
        this.predictions = predictions;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(
                R.layout.item_user_prediction,
                parent,
                false);

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        userPrediction prediction = predictions.get(position);

        holder.title.setText(safe(prediction.getTitle()));

        String event = prediction.getRaceName();

        if (event == null || event.isEmpty()) {
            event = "Сезон " + safe(prediction.getSeason());
        }

        holder.event.setText(event);
        holder.answer.setText("Ваш ответ: " + safe(prediction.getOptionTitle()));
        holder.status.setText(getStatusText(prediction.getResult()));
        holder.points.setText(getPointsText(prediction));
        holder.itemView.setOnClickListener(v -> listener.onPredictionClick(prediction));
    }


    @Override
    public int getItemCount() {
        return predictions.size();
    }

    private String getStatusText(String result) {
        if ("correct".equals(result)) {
            return "Статус: угадано";
        }

        if ("incorrect".equals(result)) {
            return "Статус: не угадано";
        }

        return "Статус: ожидает результата";
    }

    private String getPointsText(userPrediction prediction) {
        if (prediction.getPoints() != null) {
            return "Начислено очков: " + prediction.getPoints();
        }

        return "Возможные очки: " + prediction.getPossiblePoints();
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView title;
        TextView event;
        TextView answer;
        TextView status;
        TextView points;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.userPredictionTitle);
            event = itemView.findViewById(R.id.userPredictionEvent);
            answer = itemView.findViewById(R.id.userPredictionAnswer);
            status = itemView.findViewById(R.id.userPredictionStatus);
            points = itemView.findViewById(R.id.userPredictionPoints);
        }
    }
}