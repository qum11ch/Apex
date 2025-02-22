package com.example.f1app;

import android.app.Activity;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class driverResultsAdapter extends RecyclerView.Adapter<driverResultsAdapter.DataHolder>{
    Activity context;
    List<driverResultsData> dataList;

    public driverResultsAdapter(Activity context , List<driverResultsData> datum){
        this.context = context;
        this.dataList = datum;
    }

    @NonNull
    @Override
    public driverResultsAdapter.DataHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_driver_result, parent , false);
        return new driverResultsAdapter.DataHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull driverResultsAdapter.DataHolder holder, int position) {
        driverResultsData datum = dataList.get(position);

        String raceName = datum.getRaceName();
        String driverResult = datum.getDriverResult();
        String driverName = datum.getDriverName();

        holder.raceName.setText(raceName);

        if (!driverResult.equals("N/C")) {
            if (driverResult.equals("NP")) {
                holder.driverResults.setTextColor(ContextCompat.getColor(context, R.color.dark_grey));
                holder.driverResults.setBackgroundColor(ContextCompat.getColor(context, R.color.white));
                holder.driverResults.setText("DNS");
            } else {
                String[] driverRes = driverResult.split("-");
                String driverFinish = driverRes[1];
                String driverStart = driverRes[0];

                if (driverFinish.equals("R")) {
                    holder.driverResults.setTextColor(ContextCompat.getColor(context, R.color.dark_grey));
                    holder.driverResults.setBackgroundColor(ContextCompat.getColor(context, R.color.pink));
                    holder.driverResults.setText("Ret");
                } else if (driverFinish.equals("W")) {
                    holder.driverResults.setTextColor(ContextCompat.getColor(context, R.color.dark_grey));
                    holder.driverResults.setBackgroundColor(ContextCompat.getColor(context, R.color.light_grey));
                    holder.driverResults.setText("WD");
                } else {
                    int finishPos = Integer.parseInt(driverFinish);
                    if (finishPos <= 3) {
                        switch (finishPos) {
                            case (1):
                                holder.driverResults.setTextColor(ContextCompat.getColor(context, R.color.dark_grey));
                                holder.driverResults.setBackgroundColor(ContextCompat.getColor(context, R.color.gold));
                                break;
                            case (2):
                                holder.driverResults.setTextColor(ContextCompat.getColor(context, R.color.dark_grey));
                                holder.driverResults.setBackgroundColor(ContextCompat.getColor(context, R.color.silver));
                                break;
                            case (3):
                                holder.driverResults.setTextColor(ContextCompat.getColor(context, R.color.dark_grey));
                                holder.driverResults.setBackgroundColor(ContextCompat.getColor(context, R.color.bronze));
                                break;
                            default:
                                break;
                        }
                    } else if (finishPos <= 10) {
                        holder.driverResults.setTextColor(ContextCompat.getColor(context, R.color.dark_grey));
                        holder.driverResults.setBackgroundColor(ContextCompat.getColor(context, R.color.light_green));
                    } else {
                        holder.driverResults.setTextColor(ContextCompat.getColor(context, R.color.dark_grey));
                        holder.driverResults.setBackgroundColor(ContextCompat.getColor(context, R.color.light_purple));
                    }
                    if (driverStart.equals("1")) {
                        holder.driverResults.setTextColor(ContextCompat.getColor(context, R.color.purple));
                        holder.driverResults.setTypeface(holder.driverResults.getTypeface(), Typeface.BOLD);
                    }
                    holder.driverResults.setText(driverFinish);
                }
            }
        }
    }

    @Override
    public int getItemCount() {
        return dataList.size();
    }

    public class DataHolder extends RecyclerView.ViewHolder{
        TextView raceName, driverResults;
        ConstraintLayout constraintLayout;
        public DataHolder(@NonNull View itemView) {
            super(itemView);
            raceName = itemView.findViewById(R.id.raceName);
            driverResults = itemView.findViewById(R.id.driverResults);
            constraintLayout = itemView.findViewById(R.id.main_layout);
        }
    }

}
