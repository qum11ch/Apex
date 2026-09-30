package com.example.f1app;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class userPredictionAdapter
        extends RecyclerView.Adapter<userPredictionAdapter.ViewHolder> {

    public interface OnPredictionClickListener {
        void onPredictionClick(userPrediction prediction);
    }

    private final List<userPrediction> predictions;
    private final OnPredictionClickListener listener;

    public userPredictionAdapter(
            List<userPrediction> predictions,
            OnPredictionClickListener listener
    ) {
        this.predictions = predictions;
        this.listener = listener;
    }

    public userPrediction getItem(int position) {
        if (position < 0 || position >= predictions.size()) {
            return null;
        }
        return predictions.get(position);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType
    ) {
        View view = LayoutInflater.from(parent.getContext()).inflate(
                R.layout.item_user_prediction,
                parent,
                false
        );
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(
            @NonNull ViewHolder holder,
            int position
    ) {
        userPrediction prediction = predictions.get(position);
        Context context = holder.itemView.getContext();

        holder.title.setText(safe(prediction.getLocalizedTitle()));
        holder.event.setText(getEventText(context, prediction));
        holder.answer.setText(getAnswerText(context, prediction));
        holder.status.setText(getStatusText(context, prediction.getAnswerResult()));
        holder.points.setText(getPointsText(context, prediction));

        holder.itemView.setOnClickListener(view -> {
            if (listener != null) {
                listener.onPredictionClick(prediction);
            }
        });
    }

    @Override
    public int getItemCount() {
        return predictions.size();
    }

    private String getEventText(
            Context context,
            userPrediction prediction
    ) {
        if (prediction.isWeekend()
                && prediction.getSeason() != null
                && prediction.getRaceRound() != null) {
            return context.getString(
                    R.string.prediction_weekend_round,
                    prediction.getSeason(),
                    prediction.getRaceRound()
            );
        }

        return context.getString(
                R.string.prediction_season,
                safe(prediction.getSeason())
        );
    }

    private String getAnswerText(
            Context context,
            userPrediction prediction
    ) {
        String answerTitle = prediction.getAnswerOptionTitle();
        String answerId = prediction.getAnswerOptionId();

        String answer;

        if (answerTitle != null && !answerTitle.trim().isEmpty()) {
            answer = answerTitle;
        } else if (answerId != null && !answerId.trim().isEmpty()) {
            answer = answerId;
        } else {
            answer = context.getString(
                    R.string.prediction_answer_unknown
            );
        }

        return context.getString(
                R.string.prediction_answer_prefix,
                answer
        );
    }

    private String getStatusText(Context context, String result) {
        if ("correct".equals(result)) {
            return context.getString(R.string.prediction_status_correct);
        }

        if ("incorrect".equals(result)) {
            return context.getString(R.string.prediction_status_incorrect);
        }

        if ("tie".equals(result)) {
            return context.getString(R.string.prediction_status_tie);
        }

        return context.getString(R.string.prediction_status_pending);
    }

    private String formatPoints(Double value) {
        if (value == null) {
            return "0";
        }

        if (value.doubleValue() == Math.rint(value.doubleValue())) {
            return String.valueOf(value.longValue());
        }

        return String.valueOf(value);
    }

    private String getPointsText(
            Context context,
            userPrediction prediction
    ) {
        Double answerPoints = prediction.getAnswerPoints();

        if (answerPoints != null) {
            return context.getString(
                    R.string.prediction_points_awarded,
                    formatPoints(answerPoints)
            );
        }

        return context.getString(
                R.string.prediction_points_possible,
                formatPoints(prediction.getMaxPoints())
        );
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