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

public class teamTripleDriversResultsAdapter extends RecyclerView.Adapter<teamTripleDriversResultsAdapter.DataHolder>{
    Activity context;
    List<teamTripleDriversResultsData> dataList;

    public teamTripleDriversResultsAdapter(Activity context , List<teamTripleDriversResultsData> datum){
        this.context = context;
        this.dataList = datum;
    }

    @NonNull
    @Override
    public teamTripleDriversResultsAdapter.DataHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_team_triple_result, parent , false);
        return new DataHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull teamTripleDriversResultsAdapter.DataHolder holder, int position) {
        teamTripleDriversResultsData datum = dataList.get(position);

        if (position==dataList.size()-1){
            holder.bottomLine.setVisibility(View.GONE);
        }

        String raceName = datum.getRaceName();
        String firstDriverResult = datum.getFirstDriverResult();
        String secondDriverResult = datum.getSecondDriverResult();
        String thirdDriverResult = datum.getThirdDriverResult();

        String localeRaceName = raceName.toLowerCase().replaceAll("\\s+", "_");
        String resultsRaceName = context.getString(getStringByName(localeRaceName + "_locality"));
        holder.raceName.setText(resultsRaceName);

        int strokeWidth = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1, context.getResources().getDisplayMetrics());

        GradientDrawable myGradFirst = (GradientDrawable)holder.firstDriverResult.getBackground();
        GradientDrawable myGradSecond = (GradientDrawable)holder.secondDriverResult.getBackground();
        GradientDrawable myGradThird = (GradientDrawable)holder.thirdDriverResult.getBackground();

        myGradFirst.setStroke(strokeWidth, ContextCompat.getColor(context, R.color.grey));
        myGradSecond.setStroke(strokeWidth, ContextCompat.getColor(context, R.color.grey));
        myGradThird.setStroke(strokeWidth, ContextCompat.getColor(context, R.color.grey));

        if (!firstDriverResult.equals("N/C")){
            if (firstDriverResult.equals("NP")) {
                myGradFirst.setColor(ContextCompat.getColor(context, R.color.white));
                holder.firstDriverResult.setBackground(myGradFirst);
                holder.firstDriverResult.setTextColor(ContextCompat.getColor(context, R.color.dark_grey));
                holder.firstDriverResult.setText(context.getResources().getString(R.string.dns_text));
            } else if (firstDriverResult.equals("null")) {
                myGradFirst.setStroke(strokeWidth, ContextCompat.getColor(context, R.color.light_grey));
                myGradFirst.setColor(ContextCompat.getColor(context, R.color.light_grey));
                holder.firstDriverResult.setBackground(myGradFirst);
                holder.firstDriverResult.setTextColor(ContextCompat.getColor(context, R.color.dark_grey));
                holder.firstDriverResult.setText("-");
            }else{
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
            }

            if (secondDriverResult.equals("NP")) {
                myGradSecond.setColor(ContextCompat.getColor(context, R.color.white));
                holder.secondDriverResult.setBackground(myGradSecond);
                holder.secondDriverResult.setTextColor(ContextCompat.getColor(context, R.color.dark_grey));
                holder.secondDriverResult.setText(context.getResources().getString(R.string.dns_text));
            } else if (secondDriverResult.equals("null")) {
                myGradSecond.setStroke(strokeWidth, ContextCompat.getColor(context, R.color.light_grey));
                myGradSecond.setColor(ContextCompat.getColor(context, R.color.light_grey));
                holder.secondDriverResult.setBackground(myGradSecond);
                holder.secondDriverResult.setTextColor(ContextCompat.getColor(context, R.color.dark_grey));
                holder.secondDriverResult.setText("-");
            }else{
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
                if(secondDriverStart.equals("1")){
                    holder.secondDriverResult.setTextColor(ContextCompat.getColor(context,R.color.purple));
                    holder.secondDriverResult.setTypeface(holder.secondDriverResult.getTypeface(), Typeface.BOLD);
                }
            }

            if (thirdDriverResult.equals("NP")) {
                myGradThird.setColor(ContextCompat.getColor(context, R.color.white));
                holder.thirdDriverResult.setBackground(myGradThird);
                holder.thirdDriverResult.setTextColor(ContextCompat.getColor(context, R.color.dark_grey));
                holder.thirdDriverResult.setText(context.getResources().getString(R.string.dns_text));
            }else if (thirdDriverResult.equals("null")) {
                myGradThird.setStroke(strokeWidth, ContextCompat.getColor(context, R.color.light_grey));
                myGradThird.setColor(ContextCompat.getColor(context, R.color.light_grey));
                holder.thirdDriverResult.setBackground(myGradThird);
                holder.thirdDriverResult.setTextColor(ContextCompat.getColor(context, R.color.dark_grey));
                holder.thirdDriverResult.setText("-");
            }else{
                String[] thirdDriver = thirdDriverResult.split("-");
                String thirdDriverFinish = thirdDriver[1];
                String thirdDriverStart = thirdDriver[0];
                switch (thirdDriverFinish) {
                    case "R":
                        myGradThird.setColor(ContextCompat.getColor(context, R.color.pink));
                        holder.thirdDriverResult.setBackground(myGradThird);
                        holder.thirdDriverResult.setTextColor(ContextCompat.getColor(context, R.color.dark_grey));
                        holder.thirdDriverResult.setText(context.getResources().getString(R.string.ret_text));
                        break;
                    case "W":
                        myGradThird.setColor(ContextCompat.getColor(context, R.color.light_grey));
                        holder.thirdDriverResult.setBackground(myGradThird);
                        holder.thirdDriverResult.setTextColor(ContextCompat.getColor(context, R.color.dark_grey));
                        holder.thirdDriverResult.setText(context.getResources().getString(R.string.wd_text));
                        break;
                    case "D":
                        myGradThird.setColor(ContextCompat.getColor(context, R.color.black));
                        holder.thirdDriverResult.setBackground(myGradThird);
                        holder.thirdDriverResult.setTextColor(ContextCompat.getColor(context, R.color.white));
                        holder.thirdDriverResult.setText(context.getResources().getString(R.string.dsq_text));
                        break;
                    default:
                        int finishPos = Integer.parseInt(thirdDriverFinish);
                        if (finishPos <= 3) {
                            switch (finishPos) {
                                case (1):
                                    myGradThird.setColor(ContextCompat.getColor(context, R.color.light_gold));
                                    holder.thirdDriverResult.setBackground(myGradThird);
                                    holder.thirdDriverResult.setTextColor(ContextCompat.getColor(context, R.color.dark_grey));
                                    break;
                                case (2):
                                    myGradThird.setColor(ContextCompat.getColor(context, R.color.light_silver));
                                    holder.thirdDriverResult.setBackground(myGradThird);
                                    holder.thirdDriverResult.setTextColor(ContextCompat.getColor(context, R.color.dark_grey));
                                    break;
                                case (3):
                                    myGradThird.setColor(ContextCompat.getColor(context, R.color.light_bronze));
                                    holder.thirdDriverResult.setBackground(myGradThird);
                                    holder.thirdDriverResult.setTextColor(ContextCompat.getColor(context, R.color.dark_grey));
                                    break;
                                default:
                                    break;
                            }
                        } else if (finishPos <= 10) {
                            myGradThird.setColor(ContextCompat.getColor(context, R.color.light_green));
                            holder.thirdDriverResult.setBackground(myGradThird);
                            holder.thirdDriverResult.setTextColor(ContextCompat.getColor(context, R.color.dark_grey));
                        } else {
                            myGradThird.setColor(ContextCompat.getColor(context, R.color.light_purple));
                            holder.thirdDriverResult.setBackground(myGradThird);
                            holder.thirdDriverResult.setTextColor(ContextCompat.getColor(context, R.color.dark_grey));
                        }
                        holder.thirdDriverResult.setText(thirdDriverFinish);
                        break;
                }
                if(thirdDriverStart.equals("1")){
                    holder.thirdDriverResult.setTextColor(ContextCompat.getColor(context,R.color.purple));
                    holder.thirdDriverResult.setTypeface(holder.thirdDriverResult.getTypeface(), Typeface.BOLD);
                }
            }
        } else{
            myGradFirst.setColor(ContextCompat.getColor(context, R.color.light_grey));
            holder.firstDriverResult.setBackground(myGradFirst);
            myGradSecond.setColor(ContextCompat.getColor(context, R.color.light_grey));
            holder.secondDriverResult.setBackground(myGradSecond);
            myGradThird.setColor(ContextCompat.getColor(context, R.color.light_grey));
            holder.thirdDriverResult.setBackground(myGradThird);
            holder.firstDriverResult.setText(context.getResources().getString(R.string.n_c_text));
            holder.secondDriverResult.setText(context.getResources().getString(R.string.n_c_text));
            holder.thirdDriverResult.setText(context.getResources().getString(R.string.n_c_text));
        }


    }


    @Override
    public int getItemCount() {
        return dataList.size();
    }

    public static class DataHolder extends RecyclerView.ViewHolder{
        TextView raceName, firstDriverResult, secondDriverResult, thirdDriverResult;
        ConstraintLayout constraintLayout;
        View bottomLine;
        public DataHolder(@NonNull View itemView) {
            super(itemView);
            raceName = itemView.findViewById(R.id.raceName);
            firstDriverResult = itemView.findViewById(R.id.result_firstDriver);
            secondDriverResult = itemView.findViewById(R.id.result_secondDriver);
            thirdDriverResult = itemView.findViewById(R.id.result_thirdDriver);
            constraintLayout = itemView.findViewById(R.id.main_layout);
            bottomLine = itemView.findViewById(R.id.bottomLine);
        }
    }

}
