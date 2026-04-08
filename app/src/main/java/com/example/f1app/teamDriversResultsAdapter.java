package com.example.f1app;

import static com.example.f1app.MainActivity.getStringByName;

import android.app.Activity;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class teamDriversResultsAdapter extends RecyclerView.Adapter<teamDriversResultsAdapter.DataHolder>{
    Activity context;
    List<teamDriversResultsData> dataList;

    public teamDriversResultsAdapter(Activity context , List<teamDriversResultsData> datum){
        this.context = context;
        this.dataList = datum;
    }

    @NonNull
    @Override
    public teamDriversResultsAdapter.DataHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_team_result, parent , false);
        return new DataHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull teamDriversResultsAdapter.DataHolder holder, int position) {
        teamDriversResultsData datum = dataList.get(position);

        if (position==dataList.size()-1){
            holder.bottomLine.setVisibility(View.GONE);
        }

        String raceName = datum.getRaceName();
        String firstDriverResult = datum.getFirstDriverResult();
        String secondDriverResult = datum.getSecondDriverResult();

        String localeRaceName = raceName.toLowerCase().replaceAll("\\s+", "_");
        String resultsRaceName = context.getString(getStringByName(localeRaceName + "_locality"));
        holder.raceName.setText(resultsRaceName);

        int strokeWidth = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1, context.getResources().getDisplayMetrics());

        GradientDrawable myGradFirst = (GradientDrawable)holder.firstDriverResult.getBackground();
        GradientDrawable myGradSecond = (GradientDrawable)holder.secondDriverResult.getBackground();

        myGradFirst.setStroke(strokeWidth, ContextCompat.getColor(context, R.color.grey));
        myGradSecond.setStroke(strokeWidth, ContextCompat.getColor(context, R.color.grey));

        if (!firstDriverResult.equals("N/C")){
            switch (firstDriverResult) {
                case "CND":
                    myGradFirst.setColor(ContextCompat.getColor(context, R.color.orange));
                    holder.firstDriverResult.setBackground(myGradFirst);
                    holder.firstDriverResult.setTextColor(ContextCompat.getColor(context, R.color.white));
                    holder.firstDriverResult.setText(context.getResources().getString(R.string.cnd_text));
                    break;
                case "NP":
                    myGradFirst.setColor(ContextCompat.getColor(context, R.color.white));
                    holder.firstDriverResult.setBackground(myGradFirst);
                    holder.firstDriverResult.setTextColor(ContextCompat.getColor(context, R.color.dark_grey));
                    holder.firstDriverResult.setText(context.getResources().getString(R.string.dns_text));
                    break;
                case "null":
                    myGradFirst.setColor(ContextCompat.getColor(context, R.color.light_grey));
                    holder.firstDriverResult.setBackground(myGradFirst);
                    holder.firstDriverResult.setTextColor(ContextCompat.getColor(context, R.color.dark_grey));
                    holder.firstDriverResult.setText("-");
                    break;
                default:
                    String[] firstDriver = firstDriverResult.split("-");
                    String firstDriverFinish = firstDriver[1];
                    String firstDriverStart = firstDriver[0];

                    switch (firstDriverFinish) {
                        case "R":
                            myGradFirst.setColor(ContextCompat.getColor(context, R.color.pink));
                            holder.firstDriverResult.setBackground(myGradFirst);
                            holder.firstDriverResult.setTextColor(ContextCompat.getColor(context, R.color.dark_grey));
                            holder.firstDriverResult.setText(context.getResources().getString(R.string.ret_text));
                            break;
                        case "W":
                            myGradFirst.setColor(ContextCompat.getColor(context, R.color.light_grey));
                            holder.firstDriverResult.setBackground(myGradFirst);
                            holder.firstDriverResult.setTextColor(ContextCompat.getColor(context, R.color.dark_grey));
                            holder.firstDriverResult.setText(context.getResources().getString(R.string.wd_text));
                            break;
                        case "D":
                            myGradFirst.setColor(ContextCompat.getColor(context, R.color.black));
                            holder.firstDriverResult.setBackground(myGradFirst);
                            holder.firstDriverResult.setTextColor(ContextCompat.getColor(context, R.color.white));
                            holder.firstDriverResult.setText(context.getResources().getString(R.string.dsq_text));
                            break;
                        default:
                            int finishPos = Integer.parseInt(firstDriverFinish);
                            if (finishPos <= 3) {
                                switch (finishPos) {
                                    case (1):
                                        myGradFirst.setColor(ContextCompat.getColor(context, R.color.light_gold));
                                        holder.firstDriverResult.setBackground(myGradFirst);
                                        holder.firstDriverResult.setTextColor(ContextCompat.getColor(context, R.color.dark_grey));
                                        break;
                                    case (2):
                                        myGradFirst.setColor(ContextCompat.getColor(context, R.color.light_silver));
                                        holder.firstDriverResult.setBackground(myGradFirst);
                                        holder.firstDriverResult.setTextColor(ContextCompat.getColor(context, R.color.dark_grey));
                                        break;
                                    case (3):
                                        myGradFirst.setColor(ContextCompat.getColor(context, R.color.light_bronze));
                                        holder.firstDriverResult.setBackground(myGradFirst);
                                        holder.firstDriverResult.setTextColor(ContextCompat.getColor(context, R.color.dark_grey));
                                        break;
                                    default:
                                        break;
                                }
                            } else if (finishPos <= 10) {
                                myGradFirst.setColor(ContextCompat.getColor(context, R.color.light_green));
                                holder.firstDriverResult.setBackground(myGradFirst);
                                holder.firstDriverResult.setTextColor(ContextCompat.getColor(context, R.color.dark_grey));
                            } else {
                                myGradFirst.setColor(ContextCompat.getColor(context, R.color.light_purple));
                                holder.firstDriverResult.setBackground(myGradFirst);
                                holder.firstDriverResult.setTextColor(ContextCompat.getColor(context, R.color.dark_grey));
                            }
                            if (firstDriverStart.equals("1")) {
                                holder.firstDriverResult.setTextColor(ContextCompat.getColor(context, R.color.purple));
                                holder.firstDriverResult.setTypeface(holder.secondDriverResult.getTypeface(), Typeface.BOLD);
                            }
                            holder.firstDriverResult.setText(firstDriverFinish);
                            break;
                    }
                    break;
            }

            switch (secondDriverResult) {
                case "CND":
                    myGradSecond.setColor(ContextCompat.getColor(context, R.color.orange));
                    holder.secondDriverResult.setBackground(myGradSecond);
                    holder.secondDriverResult.setTextColor(ContextCompat.getColor(context, R.color.white));
                    holder.secondDriverResult.setText(context.getResources().getString(R.string.cnd_text));
                    break;
                case "NP":
                    myGradSecond.setColor(ContextCompat.getColor(context, R.color.white));
                    holder.secondDriverResult.setBackground(myGradSecond);
                    holder.secondDriverResult.setTextColor(ContextCompat.getColor(context, R.color.dark_grey));
                    holder.secondDriverResult.setText(context.getResources().getString(R.string.dns_text));
                    break;
                case "null":
                    myGradSecond.setColor(ContextCompat.getColor(context, R.color.light_grey));
                    holder.secondDriverResult.setBackground(myGradSecond);
                    holder.secondDriverResult.setTextColor(ContextCompat.getColor(context, R.color.dark_grey));
                    holder.secondDriverResult.setText("-");
                    break;
                default:
                    String[] secondDriver = secondDriverResult.split("-");
                    String secondDriverFinish = secondDriver[1];
                    String secondDriverStart = secondDriver[0];
                    switch (secondDriverFinish) {
                        case "R":
                            myGradSecond.setColor(ContextCompat.getColor(context, R.color.pink));
                            holder.secondDriverResult.setBackground(myGradSecond);
                            holder.secondDriverResult.setTextColor(ContextCompat.getColor(context, R.color.dark_grey));
                            holder.secondDriverResult.setText(context.getResources().getString(R.string.ret_text));
                            break;
                        case "W":
                            myGradSecond.setColor(ContextCompat.getColor(context, R.color.light_grey));
                            holder.secondDriverResult.setBackground(myGradSecond);
                            holder.secondDriverResult.setTextColor(ContextCompat.getColor(context, R.color.dark_grey));
                            holder.secondDriverResult.setText(context.getResources().getString(R.string.wd_text));
                            break;
                        case "D":
                            myGradSecond.setColor(ContextCompat.getColor(context, R.color.black));
                            holder.secondDriverResult.setBackground(myGradSecond);
                            holder.secondDriverResult.setTextColor(ContextCompat.getColor(context, R.color.white));
                            holder.secondDriverResult.setText(context.getResources().getString(R.string.dsq_text));
                            break;
                        default:
                            int finishPos = Integer.parseInt(secondDriverFinish);
                            if (finishPos <= 3) {
                                switch (finishPos) {
                                    case (1):
                                        myGradSecond.setColor(ContextCompat.getColor(context, R.color.light_gold));
                                        holder.secondDriverResult.setBackground(myGradSecond);
                                        holder.secondDriverResult.setTextColor(ContextCompat.getColor(context, R.color.dark_grey));
                                        break;
                                    case (2):
                                        myGradSecond.setColor(ContextCompat.getColor(context, R.color.light_silver));
                                        holder.secondDriverResult.setBackground(myGradSecond);
                                        holder.secondDriverResult.setTextColor(ContextCompat.getColor(context, R.color.dark_grey));
                                        break;
                                    case (3):
                                        myGradSecond.setColor(ContextCompat.getColor(context, R.color.light_bronze));
                                        holder.secondDriverResult.setBackground(myGradSecond);
                                        holder.secondDriverResult.setTextColor(ContextCompat.getColor(context, R.color.dark_grey));
                                        break;
                                    default:
                                        break;
                                }
                            } else if (finishPos <= 10) {
                                myGradSecond.setColor(ContextCompat.getColor(context, R.color.light_green));
                                holder.secondDriverResult.setBackground(myGradSecond);
                                holder.secondDriverResult.setTextColor(ContextCompat.getColor(context, R.color.dark_grey));
                            } else {
                                myGradSecond.setColor(ContextCompat.getColor(context, R.color.light_purple));
                                holder.secondDriverResult.setBackground(myGradSecond);
                                holder.secondDriverResult.setTextColor(ContextCompat.getColor(context, R.color.dark_grey));
                            }
                            holder.secondDriverResult.setText(secondDriverFinish);
                            break;
                    }
                    if (secondDriverStart.equals("1")) {
                        holder.secondDriverResult.setTextColor(ContextCompat.getColor(context, R.color.purple));
                        holder.secondDriverResult.setTypeface(holder.secondDriverResult.getTypeface(), Typeface.BOLD);
                    }
                    break;
            }
        } else{
            myGradFirst.setColor(ContextCompat.getColor(context, R.color.light_grey));
            holder.firstDriverResult.setBackground(myGradFirst);
            myGradSecond.setColor(ContextCompat.getColor(context, R.color.light_grey));
            holder.secondDriverResult.setBackground(myGradSecond);
            holder.firstDriverResult.setText(context.getResources().getString(R.string.n_c_text));
            holder.secondDriverResult.setText(context.getResources().getString(R.string.n_c_text));
        }


    }


    @Override
    public int getItemCount() {
        return dataList.size();
    }

    public static class DataHolder extends RecyclerView.ViewHolder{
        TextView raceName, firstDriverResult, secondDriverResult;
        ConstraintLayout constraintLayout;
        View bottomLine;
        public DataHolder(@NonNull View itemView) {
            super(itemView);
            raceName = itemView.findViewById(R.id.raceName);
            firstDriverResult = itemView.findViewById(R.id.result_firstDriver);
            secondDriverResult = itemView.findViewById(R.id.result_secondDriver);
            constraintLayout = itemView.findViewById(R.id.main_layout);
            bottomLine = itemView.findViewById(R.id.bottomLine);
        }
    }

}
