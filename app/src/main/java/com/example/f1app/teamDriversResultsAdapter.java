package com.example.f1app;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.core.widget.TextViewCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

public class teamDriversResultsAdapter extends RecyclerView.Adapter<teamDriversResultsAdapter.DataHolder>{
    Activity context;
    List<driverResultsData> dataList;

    public teamDriversResultsAdapter(Activity context , List<driverResultsData> datum){
        this.context = context;
        this.dataList = datum;
    }

    @NonNull
    @Override
    public teamDriversResultsAdapter.DataHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_result, parent , false);
        return new teamDriversResultsAdapter.DataHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull teamDriversResultsAdapter.DataHolder holder, int position) {
        driverResultsData datum = dataList.get(position);

        String raceName = datum.getRaceName();
        String firstDriverResult = datum.getFirstDriverResult();
        String secondDriverResult = datum.getSecondDriverResult();

        holder.raceName.setText(raceName);

        if (!firstDriverResult.equals("N/C")){
            if (firstDriverResult.equals("NP")){
                holder.firstDriverResult.setTextColor(ContextCompat.getColor(context,R.color.dark_grey));
                holder.firstDriverResult.setBackgroundColor(ContextCompat.getColor(context,R.color.white));
                holder.firstDriverResult.setText("DNS");
            }else{
                String[] firstDriver = firstDriverResult.split("-");
                String firstDriverFinish = firstDriver[1];
                String firstDriverStart = firstDriver[0];

                if (firstDriverFinish.equals("R")){
                    holder.firstDriverResult.setTextColor(ContextCompat.getColor(context,R.color.dark_grey));
                    holder.firstDriverResult.setBackgroundColor(ContextCompat.getColor(context,R.color.pink));
                    holder.firstDriverResult.setText("Ret");
                } else if (firstDriverFinish.equals("W")) {
                    holder.firstDriverResult.setTextColor(ContextCompat.getColor(context,R.color.dark_grey));
                    holder.firstDriverResult.setBackgroundColor(ContextCompat.getColor(context,R.color.light_grey));
                    holder.firstDriverResult.setText("WD");
                } else{
                    int finishPos = Integer.parseInt(firstDriverFinish);
                    if (finishPos <= 3){
                        switch(finishPos){
                            case (1):
                                holder.firstDriverResult.setTextColor(ContextCompat.getColor(context,R.color.dark_grey));
                                holder.firstDriverResult.setBackgroundColor(ContextCompat.getColor(context,R.color.gold));
                                break;
                            case (2):
                                holder.firstDriverResult.setTextColor(ContextCompat.getColor(context,R.color.dark_grey));
                                holder.firstDriverResult.setBackgroundColor(ContextCompat.getColor(context,R.color.silver));
                                break;
                            case (3):
                                holder.firstDriverResult.setTextColor(ContextCompat.getColor(context,R.color.dark_grey));
                                holder.firstDriverResult.setBackgroundColor(ContextCompat.getColor(context,R.color.bronze));
                                break;
                            default:
                                break;
                        }
                    } else if (finishPos <= 10) {
                        holder.firstDriverResult.setTextColor(ContextCompat.getColor(context,R.color.dark_grey));
                        holder.firstDriverResult.setBackgroundColor(ContextCompat.getColor(context,R.color.light_green));
                    } else{
                        holder.firstDriverResult.setTextColor(ContextCompat.getColor(context,R.color.dark_grey));
                        holder.firstDriverResult.setBackgroundColor(ContextCompat.getColor(context,R.color.light_purple));
                    }
                    if(firstDriverStart.equals("1")){
                        holder.firstDriverResult.setTextColor(ContextCompat.getColor(context,R.color.purple));
                        holder.firstDriverResult.setTypeface(holder.secondDriverResult.getTypeface(), Typeface.BOLD);
                    }
                    holder.firstDriverResult.setText(firstDriverFinish);
                }
            }

            if (secondDriverResult.equals("NP")) {
                holder.secondDriverResult.setTextColor(ContextCompat.getColor(context, R.color.dark_grey));
                holder.secondDriverResult.setBackgroundColor(ContextCompat.getColor(context, R.color.white));
                holder.secondDriverResult.setText("DNS");
            }else{
                String[] secondDriver = secondDriverResult.split("-");
                String secondDriverFinish = secondDriver[1];
                String secondDriverStart = secondDriver[0];
                if (secondDriverFinish.equals("R")){
                    holder.secondDriverResult.setTextColor(ContextCompat.getColor(context,R.color.dark_grey));
                    holder.secondDriverResult.setBackgroundColor(ContextCompat.getColor(context,R.color.pink));
                    holder.secondDriverResult.setText("Ret");
                } else if (secondDriverFinish.equals("W")) {
                    holder.secondDriverResult.setTextColor(ContextCompat.getColor(context,R.color.dark_grey));
                    holder.secondDriverResult.setBackgroundColor(ContextCompat.getColor(context,R.color.light_grey));
                    holder.secondDriverResult.setText("WD");
                } else{
                    int finishPos = Integer.parseInt(secondDriverFinish);
                    if (finishPos <= 3){
                        switch(finishPos){
                            case (1):
                                holder.secondDriverResult.setTextColor(ContextCompat.getColor(context,R.color.dark_grey));
                                holder.secondDriverResult.setBackgroundColor(ContextCompat.getColor(context,R.color.gold));
                                break;
                            case (2):
                                holder.secondDriverResult.setTextColor(ContextCompat.getColor(context,R.color.dark_grey));
                                holder.secondDriverResult.setBackgroundColor(ContextCompat.getColor(context,R.color.silver));
                                break;
                            case (3):
                                holder.secondDriverResult.setTextColor(ContextCompat.getColor(context,R.color.dark_grey));
                                holder.secondDriverResult.setBackgroundColor(ContextCompat.getColor(context,R.color.bronze));
                                break;
                            default:
                                break;
                        }
                    } else if (finishPos <= 10) {
                        holder.secondDriverResult.setTextColor(ContextCompat.getColor(context,R.color.dark_grey));
                        holder.secondDriverResult.setBackgroundColor(ContextCompat.getColor(context,R.color.light_green));
                    } else{
                        holder.secondDriverResult.setTextColor(ContextCompat.getColor(context,R.color.dark_grey));
                        holder.secondDriverResult.setBackgroundColor(ContextCompat.getColor(context,R.color.light_purple));
                    }
                    holder.secondDriverResult.setText(secondDriverFinish);
                }
                if(secondDriverStart.equals("1")){
                    holder.secondDriverResult.setTextColor(ContextCompat.getColor(context,R.color.purple));
                    holder.secondDriverResult.setTypeface(holder.secondDriverResult.getTypeface(), Typeface.BOLD);
                }
            }
        } else{
            holder.secondDriverResult.setText(secondDriverResult);
        }


    }

    @Override
    public int getItemCount() {
        return dataList.size();
    }

    public class DataHolder extends RecyclerView.ViewHolder{
        TextView raceName, firstDriverResult, secondDriverResult;
        ConstraintLayout constraintLayout;
        public DataHolder(@NonNull View itemView) {
            super(itemView);
            raceName = itemView.findViewById(R.id.raceName);
            firstDriverResult = itemView.findViewById(R.id.result_firstDriver);
            secondDriverResult = itemView.findViewById(R.id.result_secondDriver);
            constraintLayout = itemView.findViewById(R.id.main_layout);
        }
    }

}
