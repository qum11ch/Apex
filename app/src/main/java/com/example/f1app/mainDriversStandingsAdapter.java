package com.example.f1app;

import static com.example.f1app.driversStandingsAdapter.getColorByName;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class mainDriversStandingsAdapter extends RecyclerView.Adapter<mainDriversStandingsAdapter.DataHolder> {
    Context context;
    List<driversList> dataList;


    @Override
    public int getItemViewType(int position) {
        if (position == 0)
            return 1;
        else
            return 2;
    }


    public mainDriversStandingsAdapter(Context context, List<driversList> datum) {
        this.context = context;
        dataList = datum;
    }

    @NonNull
    @Override
    public mainDriversStandingsAdapter.DataHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view;
        view = LayoutInflater.from(context).inflate(R.layout.item_main_driver, parent, false);
        return new DataHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull mainDriversStandingsAdapter.DataHolder holder, int position) {
        driversList datum = dataList.get(position);
        holder.driverName.setText(datum.getDriverName());
        //holder.driverTeam.setText(datum.getDriverTeam());
        holder.driverFamilyName.setText(datum.getDriverFamilyName());

        if (position == dataList.size() - 1){
            holder.bottomLine.getLayoutParams().height = 0;
        }

        if (datum.getDriverName().equals("Andrea Kimi")){
            RelativeLayout.LayoutParams params= new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,ViewGroup.LayoutParams.WRAP_CONTENT);
            params.addRule(RelativeLayout.BELOW, R.id.driverName);
            holder.driverFamilyName.setLayoutParams(params);
        }else {
            RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,ViewGroup.LayoutParams.WRAP_CONTENT);
            params.addRule(RelativeLayout.END_OF, R.id.driverName);
            Resources r = context.getResources();
            int px = (int) TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP,
                    5,
                    r.getDisplayMetrics()
            );
            params.setMargins(px, 0, 0, 0);
            holder.driverFamilyName.setLayoutParams(params);
        }
        if (datum.isStartSeason()) {
            holder.leftLayout.setLayoutParams(new LinearLayout.LayoutParams(0, RelativeLayout.LayoutParams.MATCH_PARENT, 0.2f));
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(0, RelativeLayout.LayoutParams.WRAP_CONTENT, 3.3f);
            layoutParams.setMargins(0, 10,0,80);
            holder.driver_layout.setLayoutParams(layoutParams);
            int width = 0;
            holder.driver_placement.getLayoutParams().width = width;
            holder.driver_points.getLayoutParams().width = width;
        } else {
            holder.driver_placement.setText(datum.getDriverPlacement());
            String driver_points = datum.getDriverPoints()  + " " + context.getString(R.string.pts_header);
            holder.driver_points.setText(driver_points);
        }

        int resourceId_teamColor = getColorByName(datum.getConstructorId());
        holder.line.setBackgroundResource(resourceId_teamColor);

        holder.constraintLayout.setOnClickListener(v -> {
            Intent intent = new Intent(context , driverPageActivity.class);
            Bundle bundle = new Bundle();
            bundle.putString("driverName", datum.getDriverName());
            bundle.putString("driverCode", datum.getDriverCode());
            bundle.putString("driverTeam", datum.getDriverTeam());
            bundle.putString("driverFamilyName", datum.getDriverFamilyName());
            bundle.putString("driverTeamId", datum.getConstructorId());
            intent.putExtras(bundle);
            context.startActivity(intent);
        });
    }


    @Override
    public int getItemCount() {
        return dataList.size();
    }

    public static class DataHolder extends RecyclerView.ViewHolder {
        TextView driverName, driver_placement, driver_points,
                driverFamilyName;
        ConstraintLayout constraintLayout;
        RelativeLayout leftLayout, driver_layout;
        View line, bottomLine;
        CardView cardView;

        public DataHolder(@NonNull View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.cardView);
            leftLayout = itemView.findViewById(R.id.leftLayout);
            driver_layout = itemView.findViewById(R.id.driver_layout);
            driverName = itemView.findViewById(R.id.driverName);
            driverFamilyName = itemView.findViewById(R.id.driverFamilyName);
            driver_placement = itemView.findViewById(R.id.driver_placement);
            driver_points = itemView.findViewById(R.id.driver_points);
            constraintLayout = itemView.findViewById(R.id.main_layout);
            line = itemView.findViewById(R.id.line);
            bottomLine = itemView.findViewById(R.id.bottomLine);
        }
    }
}
