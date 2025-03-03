package com.example.f1app;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;

import java.lang.reflect.Field;
import java.util.List;

public class driversStandingsAdapter extends RecyclerView.Adapter<driversStandingsAdapter.DataHolder> {
    Context context;
    List<driversList> dataList;


    @Override
    public int getItemViewType(int position) {
        if (position == 0)
            return 1;
        else
            return 2;
    }


    public driversStandingsAdapter(Context context, List<driversList> datum) {
        this.context = context;
        dataList = datum;
    }

    @NonNull
    @Override
    public driversStandingsAdapter.DataHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        View view;
        switch (viewType) {
            case 1:
                view = LayoutInflater.from(context).inflate(R.layout.item_driver_first, parent, false);
                break;
            default:
                view = LayoutInflater.from(context).inflate(R.layout.item_driver, parent, false);
                break;
        }
        return new driversStandingsAdapter.DataHolder(view, viewType);
    }

    @Override
    public void onBindViewHolder(@NonNull driversStandingsAdapter.DataHolder holder, int position) {
        driversList datum = dataList.get(position);
        holder.driverName.setText(datum.getDriverName());
        holder.driverFamilyName.setText(datum.getDriverFamilyName());
        holder.driverTeam.setText(datum.getDriverTeam());

        int resourceId_driverTeam = context.getResources().getIdentifier(datum.getConstructorId() + "_logo", "drawable",
                context.getPackageName());

        Glide.with(context)
                .load(resourceId_driverTeam)
                .transition(DrawableTransitionOptions.withCrossFade())
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .skipMemoryCache(true)
                .error(R.drawable.f1)
                .into(holder.driverTeam_logo);

        int resourceId_driverImage = context.getResources().getIdentifier(datum.getDriverCode().toLowerCase(), "drawable",
                context.getPackageName());

        Glide.with(context)
                .load(resourceId_driverImage)
                .transition(DrawableTransitionOptions.withCrossFade())
                .error(R.drawable.f1)
                .into(holder.driverImage);
        if (holder.getItemViewType() == 1) {
            if (datum.getStartSeasonInfo()) {
                int width = 0;
                int height = 0;
                holder.cardView.getLayoutParams().width = width;
                holder.cardView.getLayoutParams().height = height;
            }else{
                holder.driver_placement.setText(datum.getDriverPlacement());
                String driver_points = datum.getDriverPoints() + " PTS";
                holder.driver_points.setText(driver_points);
            }
        } else {
            if (datum.getStartSeasonInfo()) {
                holder.leftLayout.setLayoutParams(new LinearLayout.LayoutParams(0, RelativeLayout.LayoutParams.MATCH_PARENT, 0.2f));
                LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(0, RelativeLayout.LayoutParams.WRAP_CONTENT, 3.3f);
                layoutParams.setMargins(0, 10,0,80);
                holder.driver_layout.setLayoutParams(layoutParams);
                int width = 0;
                holder.driver_placement.getLayoutParams().width = width;
                holder.driver_points.getLayoutParams().width = width;
            } else {
                holder.driver_placement.setText(datum.getDriverPlacement());
                String driver_points = datum.getDriverPoints() + " PTS";
                holder.driver_points.setText(driver_points);
            }

            int resourceId_teamColor = getColorByName(datum.getConstructorId());
            holder.line.setBackgroundResource(resourceId_teamColor);
        }

        holder.constraintLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(context , driverPageActivity.class);
                Bundle bundle = new Bundle();
                bundle.putString("driverName", datum.getDriverName());
                bundle.putString("driverCode", datum.getDriverCode());
                bundle.putString("driverTeam", datum.getDriverTeam());
                bundle.putString("driverFamilyName", datum.getDriverFamilyName());
                intent.putExtras(bundle);
                context.startActivity(intent);
            }
        });
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
        RelativeLayout leftLayout, driver_layout;
        View line;
        CardView cardView;

        public DataHolder(@NonNull View itemView, int viewType) {
            super(itemView);
            cardView = itemView.findViewById(R.id.cardView);
            leftLayout = itemView.findViewById(R.id.leftLayout);
            driver_layout = itemView.findViewById(R.id.driver_layout);
            driverName = itemView.findViewById(R.id.driverName);
            driverFamilyName = itemView.findViewById(R.id.driverFamilyName);
            driverTeam = itemView.findViewById(R.id.driverTeam);
            driver_placement = itemView.findViewById(R.id.driver_placement);
            driver_points = itemView.findViewById(R.id.driver_points);
            constraintLayout = itemView.findViewById(R.id.main_layout);
            driverTeam_logo = itemView.findViewById(R.id.driverTeam_logo);
            driverImage = itemView.findViewById(R.id.driverImage);
            line = itemView.findViewById(R.id.line);
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
