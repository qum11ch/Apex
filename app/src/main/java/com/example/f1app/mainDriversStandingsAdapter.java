package com.example.f1app;

import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
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
import androidx.core.view.ViewCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.firebase.storage.StorageReference;

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
        holder.driverFamilyName.setText(datum.getDriverFamilyName());
        holder.driver_placement.setText(datum.getDriverPlacement());

        //String currentSeason = datum.getSeason();
        String darkTeamColor = datum.getTeamColor();

        String driverName = datum.getDriverName();
        if (driverName.equals("Andrea Kimi")){
            String[] parts = driverName.split(" ");
            driverName = parts[1];
        }

        holder.driverName.setText(driverName);

        if (datum.isStartSeason()) {
            holder.driverPointsText.setVisibility(View.GONE);
            holder.driver_points.setVisibility(View.GONE);
        } else {
            holder.driver_points.setText(datum.getDriverPoints());
            holder.driverPointsText.setVisibility(View.VISIBLE);
            holder.driver_points.setVisibility(View.VISIBLE);
        }

        holder.driverImage.setImageDrawable(null);
        StorageReference mDriverImage = datum.getImageUrl();

        mDriverImage.getDownloadUrl().addOnSuccessListener(uri -> GlideApp.with(context)
                .load(uri)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .skipMemoryCache(true)
                .error(R.drawable.placeholder_driver)
                .into(holder.driverImage)).addOnFailureListener(e -> GlideApp.with(context)
                    .load(R.drawable.placeholder_driver)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .skipMemoryCache(true)
                    .into(holder.driverImage));


        //String mTeamId = datum.getConstructorId();
        int colorRgb = Color.parseColor("#303030");

        if (darkTeamColor != null){
            colorRgb = Color.parseColor(darkTeamColor);
        }

        int alpha = 0xCC;
        int colorWithAlpha = (alpha << 24) | (colorRgb & 0x00FFFFFF);

        ViewCompat.setBackgroundTintList(
                holder.itemDriver,
                ColorStateList.valueOf(colorWithAlpha)
        );

        holder.constraintLayout.setOnClickListener(v -> {
            Intent intent = new Intent(context , driverPageActivity.class);
            Bundle bundle = new Bundle();
            bundle.putString("driverName", datum.getDriverName());
            bundle.putString("driverCode", datum.getDriverCode());
            bundle.putString("driverTeam", datum.getDriverTeam());
            bundle.putString("driverFamilyName", datum.getDriverFamilyName());
            bundle.putString("driverTeamId", datum.getConstructorId());
            bundle.putString("currentSeason", datum.getSeason());
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
                driverFamilyName, driverPointsText;
        ConstraintLayout constraintLayout;
        RelativeLayout driver_layout;
        CardView cardView;
        LinearLayout itemDriver;
        ImageView driverImage;

        public DataHolder(@NonNull View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.cardView);
            driver_layout = itemView.findViewById(R.id.driver_layout);
            driverName = itemView.findViewById(R.id.driverName);
            driverFamilyName = itemView.findViewById(R.id.driverFamilyName);
            driver_placement = itemView.findViewById(R.id.driver_placement);
            driver_points = itemView.findViewById(R.id.driver_points);
            constraintLayout = itemView.findViewById(R.id.main_layout);
            itemDriver = itemView.findViewById(R.id.item_driver);
            driverImage = itemView.findViewById(R.id.driverImage);
            driverPointsText = itemView.findViewById(R.id.driver_points_text);
        }
    }
}
