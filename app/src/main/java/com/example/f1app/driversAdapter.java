package com.example.f1app;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;

import java.lang.reflect.Field;
import java.util.List;

public class driversAdapter extends RecyclerView.Adapter<driversAdapter.DataHolder>{
    Context context;
    List<driversList> dataList;


    @Override
    public int getItemViewType(int position) {
        if (position == 0)
            return 1;
        else
            return 2;
    }


    public driversAdapter(Context context, List<driversList> datum){
        this.context = context;
        dataList = datum;
    }

    @NonNull
    @Override
    public driversAdapter.DataHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view;
        switch (viewType){
            case 1:
                view = LayoutInflater.from(context).inflate(R.layout.item_driver_first, parent , false);
                break;
            default:
                view = LayoutInflater.from(context).inflate(R.layout.item_driver, parent , false);
                break;
        }
        return new driversAdapter.DataHolder(view, viewType);
    }

    @Override
    public void onBindViewHolder(@NonNull driversAdapter.DataHolder holder, int position) {
        driversList datum = dataList.get(position);
        if (holder.getItemViewType() == 1){
            holder.driverName.setText(datum.getDriverName());
            holder.driverFamilyName.setText(datum.getDriverFamilyName());
            holder.driverTeam.setText(datum.getDriverTeam());
            holder.driver_placement.setText(datum.getDriverPlacement());
            String driver_points = datum.getDriverPoints() + " PTS";
            holder.driver_points.setText(driver_points);

            int resourceId_driverTeam = context.getResources().getIdentifier(datum.getConstructorId() + "_logo", "drawable",
                    context.getPackageName());

            Glide.with(context)
                    .load(resourceId_driverTeam)
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .error(R.drawable.f1)
                    .into(holder.driverTeam_logo);

            int resourceId_driverImage = context.getResources().getIdentifier(datum.getDriverCode().toLowerCase(), "drawable",
                    context.getPackageName());

            Glide.with(context)
                    .load(resourceId_driverImage)
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .error(R.drawable.f1)
                    .into(holder.driverImage);
        }else{
            holder.driverName.setText(datum.getDriverName());
            holder.driverFamilyName.setText(datum.getDriverFamilyName());
            holder.driverTeam.setText(datum.getDriverTeam());
            holder.driver_placement.setText(datum.getDriverPlacement());
            String driver_points = datum.getDriverPoints() + " PTS";
            holder.driver_points.setText(driver_points);

            int resourceId_driverTeam = context.getResources().getIdentifier(datum.getConstructorId() + "_logo", "drawable",
                    context.getPackageName());

            Glide.with(context)
                    .load(resourceId_driverTeam)
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .error(R.drawable.f1)
                    .into(holder.driverTeam_logo);

            int resourceId_driverImage = context.getResources().getIdentifier(datum.getDriverCode().toLowerCase(), "drawable",
                    context.getPackageName());

            Glide.with(context)
                    .load(resourceId_driverImage)
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .error(R.drawable.f1)
                    .into(holder.driverImage);

            int resourceId_teamColor = getColorByName(datum.getConstructorId());
            holder.line.setBackgroundResource(resourceId_teamColor);
        }
    }



    @Override
    public int getItemCount() {
        return dataList.size();
    }

    public class DataHolder extends RecyclerView.ViewHolder {
        TextView driverName, driverTeam, driver_placement, driver_points,
                driverFamilyName;
        ImageView driverTeam_logo, driverImage, driverNumber;
        ConstraintLayout constraintLayout;
        View line;

        public DataHolder(@NonNull View itemView, int viewType) {
            super(itemView);
            if (viewType == 1) {
                driverName = itemView.findViewById(R.id.driverName);
                driverFamilyName = itemView.findViewById(R.id.driverFamilyName);
                driverTeam = itemView.findViewById(R.id.driverTeam);
                driver_placement = itemView.findViewById(R.id.driver_placement);
                driver_points = itemView.findViewById(R.id.driver_points);
                constraintLayout = itemView.findViewById(R.id.main_layout);
                driverTeam_logo = itemView.findViewById(R.id.driverTeam_logo);
                driverImage = itemView.findViewById(R.id.driverImage);
            } else {
                line = itemView.findViewById(R.id.line);
                driverName = itemView.findViewById(R.id.driverName);
                driverFamilyName = itemView.findViewById(R.id.driverFamilyName);
                driverTeam = itemView.findViewById(R.id.driverTeam);
                driver_placement = itemView.findViewById(R.id.driver_placement);
                driver_points = itemView.findViewById(R.id.driver_points);
                constraintLayout = itemView.findViewById(R.id.main_layout);
                driverTeam_logo = itemView.findViewById(R.id.driverTeam_logo);
                driverImage = itemView.findViewById(R.id.driverImage);
            }
        }
    }
    public int getColorByName(String name) {
        int colorId = 0;

        try {
            Class res = R.color.class;
            Field field = res.getField(name);
            colorId = field.getInt(null);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return colorId;
    }
}
