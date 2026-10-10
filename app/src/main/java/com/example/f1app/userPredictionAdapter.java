package com.example.f1app;

import static androidx.core.util.TypedValueCompat.dpToPx;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class userPredictionAdapter
        extends RecyclerView.Adapter<userPredictionAdapter.ViewHolder> {

    public interface OnPredictionClickListener {
        void onPredictionClick(userPrediction prediction);
    }

    private final List<userPrediction> predictions;
    private final OnPredictionClickListener listener;

    public userPredictionAdapter(List<userPrediction> predictions, OnPredictionClickListener listener) {
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
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(
                R.layout.item_user_prediction,
                parent,
                false
        );
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        userPrediction prediction = predictions.get(position);
        Context context = holder.itemView.getContext();

        holder.title.setText(safe(prediction.getLocalizedTitle()));

        holder.event.setText(getMetaText(context, holder, prediction));

        holder.answer.setText(getAnswerText(context, prediction));

        holder.status.setText(getStatusText(context, prediction.getAnswerResult()));
        holder.status.setTextColor(getStatusColor(context, prediction.getAnswerResult()));

        Double answerPoints = prediction.getAnswerPoints();
        Double maxPoints = prediction.getMaxPoints();

        String pointsText;

        if (answerPoints != null) {
            pointsText = formatPoints(answerPoints)
                    + " " + context.getString(R.string.pts_header);
        }else{
            pointsText = formatPoints(maxPoints)
                    + " " + context.getString(R.string.pts_header);
        }

        holder.points.setText(pointsText);

        holder.title.setMaxLines(2);
        holder.title.setEllipsize(TextUtils.TruncateAt.END);

        String deadline = getDeadlineText(context, prediction);

        if (!"null".equals(deadline)){
            holder.deadline.setText(deadline);
            holder.deadline.setVisibility(View.VISIBLE);
        }else{
            holder.deadline.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(view -> {
            if (listener != null) {
                listener.onPredictionClick(prediction);
            }
        });

        setStatusColor(context, holder, prediction);
    }

    @Override
    public int getItemCount() {
        return predictions.size();
    }

    private String getMetaText(Context context,
                               userPredictionAdapter.ViewHolder holder, userPrediction item) {
        StringBuilder text = new StringBuilder();

        text.append(getCategoryTitle(holder.itemView.getContext(), item.getCategory()));

        if (item.isWeekend() && item.getSeason() != null && item.getRaceRound() != null) {
            String weekendRoundText = " • " + context.getText(R.string.round) + " ";

            text.append(" • ")
                    .append(item.getSeason())
                    .append(weekendRoundText)
                    .append(item.getRaceRound());
        } else if (item.getSeason() != null) {
            text.append(" • ")
                    .append(item.getSeason());
        }

        String typeTitle = getPredictionTypeTitle(
                context,
                item.getPredictionType()
        );

        if (!typeTitle.isEmpty()) {
            text.append(" • ").append(typeTitle);
        }

        String optionTypeTitle = getOptionTypeTitle(context, item.getOptionType());

        if (!optionTypeTitle.isEmpty()) {
            text.append(" • ").append(optionTypeTitle);
        }

        return text.toString();
    }

    private String getOptionTypeTitle(Context context, String optionType) {
        if ("driver".equals(optionType)) {
            return context.getString(R.string.prediction_option_type_driver);
        }

        if ("constructor".equals(optionType)) {
            return context.getString(R.string.prediction_option_type_constructor);
        }

        if ("boolean".equals(optionType)) {
            return context.getString(R.string.prediction_option_type_boolean);
        }

        return "";
    }

    private String getCategoryTitle(Context context, String category) {
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


    private String getPredictionTypeTitle(Context context, String predictionType) {
        if ("champion".equals(predictionType)) {
            return context.getString(R.string.prediction_type_champion);
        }

        if ("winner".equals(predictionType)) {
            return context.getString(R.string.prediction_type_winner);
        }

        if ("race_head_to_head".equals(predictionType)) {
            return context.getString(R.string.prediction_type_race_head);
        }

        if ("qualifying_head_to_head".equals(predictionType)) {
            return context.getString(R.string.prediction_type_quali_head);
        }

        if ("team_points_head_to_head".equals(predictionType)) {
            return context.getString(R.string.prediction_type_team_points_head);
        }

        if ("safety_car".equals(predictionType)) {
            return context.getString(R.string.prediction_type_safety_car);
        }

        if ("rain".equals(predictionType)) {
            return context.getString(R.string.prediction_type_rain);
        }

        if ("boolean_prediction".equals(predictionType)) {
            return context.getString(R.string.prediction_option_text_boolean);
        }

        return "";
    }

    private String getAnswerText(Context context, userPrediction prediction) {
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

        return answer;
    }

    private String getDeadlineText(Context context, userPrediction prediction){
        String result = prediction.getAnswerResult();

        if ("pending".equals(result)){
            return context.getString(R.string.user_prediction_deadline_text)
                    + " " + formatDate(context, prediction.getDeadline());
        }

        return "null";
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

    private void setStatusColor(Context context, userPredictionAdapter.ViewHolder holder,
                                userPrediction item) {
        String result = item.getAnswerResult();

        int backgroundColor;
        int strokeColor;

        if ("correct".equals(result)) {
            backgroundColor = ContextCompat.getColor(context, R.color.correct_predict_color);

            strokeColor = ContextCompat.getColor(
                    context,
                    R.color.correct_predict_color
            );

        } else if ("incorrect".equals(result)) {
            backgroundColor = ContextCompat.getColor(context, R.color.incorrect_predict_color);

            strokeColor = ContextCompat.getColor(
                    context,
                    R.color.incorrect_predict_color
            );

        } else if ("tie".equals(result)) {
            backgroundColor = ContextCompat.getColor(context, R.color.tie_predict_color);

            strokeColor = ContextCompat.getColor(
                    context,
                    R.color.tie_predict_color
            );

        } else {
            backgroundColor = ContextCompat.getColor(context, R.color.pending_status_text);

            strokeColor = ContextCompat.getColor(
                    context,
                    R.color.pending_status_text
            );
        }

        GradientDrawable drawable = (GradientDrawable) holder.points.getBackground().mutate();

        drawable.setColor(backgroundColor);
        drawable.setStroke(dpToPx(1, context), strokeColor);

        holder.points.setBackground(drawable);
    }

    private int getStatusColor(Context context, String result) {
        if ("correct".equals(result)) {
            return context.getColor(R.color.correct_predict_color);
        }

        if ("incorrect".equals(result)) {
            return context.getColor(R.color.incorrect_predict_color);
        }

        if ("tie".equals(result)) {
            return context.getColor(R.color.tie_predict_color);
        }

        return context.getColor(R.color.pending_status_text);
    }

    public static String formatPoints(Double value) {
        if (value == null) {
            return "0";
        }

        if (value.doubleValue() == Math.rint(value.doubleValue())) {
            return String.valueOf(value.longValue());
        }

        return String.valueOf(value);
    }

    private String formatDate(Context context, Long timestamp) {
        if (timestamp == null || timestamp <= 0) {
            return context.getString(R.string.deadline_null);
        }

        SimpleDateFormat formatter =
                new SimpleDateFormat(
                        "dd.MM.yyyy HH:mm",
                        Locale.getDefault()
                );

        return formatter.format(new Date(timestamp));
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
        TextView deadline;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.userPredictionTitle);
            event = itemView.findViewById(R.id.userPredictionEvent);
            answer = itemView.findViewById(R.id.userPredictionAnswer);
            status = itemView.findViewById(R.id.userPredictionStatus);
            points = itemView.findViewById(R.id.userPredictionPoints);
            deadline = itemView.findViewById(R.id.userPredictionDeadline);
        }
    }

    private int dpToPx(int dp, Context context) {
        return Math.round(
                dp * context.getResources().getDisplayMetrics().density
        );
    }
}