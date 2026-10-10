package com.example.f1app;

import static com.example.f1app.userPredictionAdapter.formatPoints;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
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
        Context context = holder.itemView.getContext();

        predictionListItem item = predictions.get(position);

        holder.title.setText(item.getLocalizedTitle());

        String descriptionText = getDescriptionText(item);

        if (!"null".equals(descriptionText)){
            holder.description.setVisibility(View.VISIBLE);
            holder.description.setText(getDescriptionText(item));
        }else{
            holder.description.setVisibility(View.GONE);
        }

        holder.category.setText(getMetaText(context, holder, item));

        String deadlineText = context.getString(R.string.deadline_text)
                + " " + formatDate(context, item.getDeadline());
        holder.deadline.setText(deadlineText);

        String pointsText = formatPoints(item.getPoints())
                + " " + context.getString(R.string.pts_header);
        holder.points.setText(pointsText);

        holder.itemView.setOnClickListener(v -> listener.onPredictionClick(item));

        int backgroundColor = ContextCompat.getColor(context, R.color.prediction_points_color);
        int strokeColor = ContextCompat.getColor(context, R.color.grey);

        GradientDrawable drawable = (GradientDrawable) holder.points.getBackground().mutate();
        drawable.setColor(backgroundColor);
        drawable.setStroke(dpToPx(1, context), strokeColor);
        holder.points.setBackground(drawable);

    }

    @Override
    public int getItemCount() {
        return predictions.size();
    }

    private String getMetaText(Context context, ViewHolder holder, predictionListItem item) {
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

    private String getDescriptionText(predictionListItem item) {
        if (item.isHeadToHead()) {
            String sideATitle = safe(item.getSideATitle());
            String sideBTitle = safe(item.getSideBTitle());

            return sideATitle + " vs " + sideBTitle;
        }
        return "null";
    }

    private String safe(String value) {
        return value == null ? "" : value;
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

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView title;
        TextView description;
        TextView category;
        TextView deadline;
        TextView points;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.predictionItemTitle);
            description = itemView.findViewById(R.id.predictionItemDescription);
            category = itemView.findViewById(R.id.predictionItemCategory);
            deadline = itemView.findViewById(R.id.predictionItemDeadline);
            points = itemView.findViewById(R.id.predictionItemPoints);
        }
    }

    private int dpToPx(int dp, Context context) {
        return Math.round(
                dp * context.getResources().getDisplayMetrics().density
        );
    }
}