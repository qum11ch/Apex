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

        if (position==dataList.size()-1){
            holder.bottomLine.setVisibility(View.GONE);
        }

        String raceName = datum.getRaceName();
        String driverResult = datum.getDriverResult();
        String driverName = datum.getDriverName();

        String localeRaceName = raceName.toLowerCase().replaceAll("\\s+", "_");
        String resultsRaceName = context.getString(getStringByName(localeRaceName + "_locality"));
        holder.raceName.setText(resultsRaceName);

        int strokeWidth = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1, context.getResources().getDisplayMetrics());

        GradientDrawable myGrad = (GradientDrawable)holder.driverResults.getBackground();
        myGrad.setStroke(strokeWidth, ContextCompat.getColor(context, R.color.grey));

        if (!driverResult.equals("N/C")) {
            if (driverResult.equals("NP")) {
                myGrad.setColor(ContextCompat.getColor(context, R.color.white));
                holder.driverResults.setBackground(myGrad);
                holder.driverResults.setTextColor(ContextCompat.getColor(context, R.color.dark_grey));
                holder.driverResults.setText(context.getResources().getString(R.string.dns_text));
            } else {
                String[] driverRes = driverResult.split("-");
                String driverFinish = driverRes[1];
                String driverStart = driverRes[0];

                if (driverFinish.equals("R")) {
                    myGrad.setColor(ContextCompat.getColor(context, R.color.pink));
                    holder.driverResults.setBackground(myGrad);
                    holder.driverResults.setTextColor(ContextCompat.getColor(context, R.color.dark_grey));
                    holder.driverResults.setText(context.getResources().getString(R.string.ret_text));
                } else if (driverFinish.equals("W")) {
                    myGrad.setColor(ContextCompat.getColor(context, R.color.light_grey));
                    holder.driverResults.setBackground(myGrad);
                    holder.driverResults.setTextColor(ContextCompat.getColor(context, R.color.dark_grey));
                    holder.driverResults.setText(context.getResources().getString(R.string.wd_text));
                } else if (driverFinish.equals("D")) {
                    myGrad.setColor(ContextCompat.getColor(context, R.color.black));
                    holder.driverResults.setBackground(myGrad);
                    holder.driverResults.setTextColor(ContextCompat.getColor(context, R.color.white));
                    holder.driverResults.setText(context.getResources().getString(R.string.dsq_text));
                } else {
                    int finishPos = Integer.parseInt(driverFinish);
                    if (finishPos <= 3) {
                        switch (finishPos) {
                            case (1):
                                myGrad.setColor(ContextCompat.getColor(context, R.color.light_gold));
                                holder.driverResults.setBackground(myGrad);
                                holder.driverResults.setTextColor(ContextCompat.getColor(context, R.color.dark_grey));
                                break;
                            case (2):
                                myGrad.setColor(ContextCompat.getColor(context, R.color.light_silver));
                                holder.driverResults.setBackground(myGrad);
                                holder.driverResults.setTextColor(ContextCompat.getColor(context, R.color.dark_grey));
                                break;
                            case (3):
                                myGrad.setColor(ContextCompat.getColor(context, R.color.light_bronze));
                                holder.driverResults.setBackground(myGrad);
                                holder.driverResults.setTextColor(ContextCompat.getColor(context, R.color.dark_grey));
                                break;
                            default:
                                break;
                        }
                    } else if (finishPos <= 10) {
                        myGrad.setColor(ContextCompat.getColor(context, R.color.light_green));
                        holder.driverResults.setBackground(myGrad);
                        holder.driverResults.setTextColor(ContextCompat.getColor(context, R.color.dark_grey));
                    } else {
                        myGrad.setColor(ContextCompat.getColor(context, R.color.light_purple));
                        holder.driverResults.setBackground(myGrad);
                        holder.driverResults.setTextColor(ContextCompat.getColor(context, R.color.dark_grey));
                    }
                    if (driverStart.equals("1")) {
                        holder.driverResults.setTextColor(ContextCompat.getColor(context, R.color.purple));
                        holder.driverResults.setTypeface(holder.driverResults.getTypeface(), Typeface.BOLD);
                    }
                    holder.driverResults.setText(driverFinish);
                }
            }
        }else {
            myGrad.setColor(ContextCompat.getColor(context, R.color.light_grey));
            holder.driverResults.setBackground(myGrad);
            holder.driverResults.setText(context.getResources().getString(R.string.n_c_text));
        }
    }

    @Override
    public int getItemCount() {
        return dataList.size();
    }

    public class DataHolder extends RecyclerView.ViewHolder{
        TextView raceName, driverResults;
        ConstraintLayout constraintLayout;
        View bottomLine;
        public DataHolder(@NonNull View itemView) {
            super(itemView);
            raceName = itemView.findViewById(R.id.raceName);
            driverResults = itemView.findViewById(R.id.driverResults);
            constraintLayout = itemView.findViewById(R.id.main_layout);
            bottomLine = itemView.findViewById(R.id.bottomLine);
        }
    }

}
