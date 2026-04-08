package com.example.f1app;

import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
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
import androidx.core.view.ViewCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.storage.StorageReference;

import java.util.List;

public class pastSeasonDriversStandingsAdapter extends RecyclerView.Adapter<pastSeasonDriversStandingsAdapter.DataHolder> {
    Context context;
    List<driversList> dataList;


    @Override
    public int getItemViewType(int position) {
        if (position == 0) {return 1;
        }else if (position == 1) {return 2;
        }else if (position == 2){return 3;
        }else{return 4;}
    }


    public pastSeasonDriversStandingsAdapter(Context context, List<driversList> datum) {
        this.context = context;
        dataList = datum;
    }

    @NonNull
    @Override
    public pastSeasonDriversStandingsAdapter.DataHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        View view;
        if (viewType == 1) {
            view = LayoutInflater.from(context).inflate(R.layout.item_driver_first, parent, false);
        } else {
            view = LayoutInflater.from(context).inflate(R.layout.item_driver, parent, false);
        }
        return new DataHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull pastSeasonDriversStandingsAdapter.DataHolder holder, int position) {
        driversList datum = dataList.get(position);

        String driverName = datum.getDriverName();
        if (driverName.equals("Andrea Kimi")){
            String[] parts = driverName.split(" ");
            driverName = parts[1];
        }

        holder.driverName.setText(driverName);

        holder.driverTeam.setText(datum.getDriverTeam());
        holder.driverFamilyName.setText(datum.getDriverFamilyName());

        String currentSeason = datum.getCurrentSeason();
        String darkTeamColor = datum.getTeamColor();

        //String season = datum.getSeason();

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

        holder.driver_placement.setText(datum.getDriverPlacement());

        holder.driver_points.setText(datum.getDriverPoints());

        String mTeamId;
        if (datum.getConstructorId().equals("sauber")){
            mTeamId = "audi";
        }else{
            mTeamId = datum.getConstructorId();
        }

        if (mTeamId.equals("audi")){
            String mTeamColor = "#64A462";
            int colorRgb = Color.parseColor(mTeamColor);
            int alpha = 0x66;
            int colorWithAlpha = (alpha << 24) | (colorRgb & 0x00FFFFFF);

            int alphaLine = 0xCC;
            int colorWithAlphaLine = (alphaLine << 24) | (colorRgb & 0x00FFFFFF);

            ViewCompat.setBackgroundTintList(
                    holder.itemDriver,
                    ColorStateList.valueOf(colorWithAlpha)
            );

            holder.itemDriverLine.setBackground(createFrameWithLeftOffset(colorWithAlphaLine, context));
            ViewCompat.setBackgroundTintList(
                    holder.stripedLines,
                    ColorStateList.valueOf(colorWithAlphaLine)
            );
        }
        else{
            int colorRgb = Color.parseColor("#303030");

            if (darkTeamColor != null){
                colorRgb = Color.parseColor(darkTeamColor);
            }

            int alpha = 0x66;
            int colorWithAlpha = (alpha << 24) | (colorRgb & 0x00FFFFFF);

            int alphaLine = 0xCC;
            int colorWithAlphaLine = (alphaLine << 24) | (colorRgb & 0x00FFFFFF);

            ViewCompat.setBackgroundTintList(
                    holder.itemDriver,
                    ColorStateList.valueOf(colorWithAlpha)
            );

            holder.itemDriverLine.setBackground(createFrameWithLeftOffset(colorWithAlphaLine, context));
            ViewCompat.setBackgroundTintList(
                    holder.stripedLines,
                    ColorStateList.valueOf(colorWithAlphaLine)
            );
        }

        holder.constraintLayout.setOnClickListener(v -> {
            Intent intent = new Intent(context , driverPageActivity.class);
            Bundle bundle = new Bundle();
            bundle.putString("driverName", datum.getDriverName());
            bundle.putString("driverCode", datum.getDriverCode());
            bundle.putString("driverTeam", datum.getDriverTeam());
            bundle.putString("driverFamilyName", datum.getDriverFamilyName());
            bundle.putString("driverTeamId", mTeamId);
            bundle.putString("currentSeason", currentSeason);
            intent.putExtras(bundle);
            context.startActivity(intent);
        });
    }



    private LayerDrawable createFrameWithLeftOffset(int colorWithAlphaLine, Context context) {
        int strokeWidth = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 5, context.getResources().getDisplayMetrics()
        );
        LayerDrawable layerDrawable = new LayerDrawable(new Drawable[]{
                createStrokeShape(strokeWidth, colorWithAlphaLine, context),
                createInnerTransparentShape(strokeWidth, context)
        });

        int leftOffset = -strokeWidth;
        layerDrawable.setLayerInset(0, leftOffset, 0, 0, 0);
        layerDrawable.setLayerInset(1, strokeWidth, strokeWidth, strokeWidth, strokeWidth);

        return layerDrawable;
    }

    private GradientDrawable createStrokeShape(int strokeWidth, int strokeColor, Context context) {
        GradientDrawable stroke = new GradientDrawable();
        stroke.setShape(GradientDrawable.RECTANGLE);
        stroke.setStroke(strokeWidth, strokeColor);
        stroke.setColor(Color.TRANSPARENT);

        int radius20 = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 23, context.getResources().getDisplayMetrics()
        );
        stroke.setCornerRadii(new float[]{
                0, 0,
                radius20, radius20,
                radius20, radius20,
                0, 0
        });
        return stroke;
    }

    private GradientDrawable createInnerTransparentShape(int strokeWidth, Context context) {
        GradientDrawable inner = new GradientDrawable();
        inner.setShape(GradientDrawable.RECTANGLE);
        inner.setColor(Color.TRANSPARENT);

        int innerRadius = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 15, context.getResources().getDisplayMetrics());
        inner.setCornerRadii(new float[]{
                0, 0,
                innerRadius, innerRadius,
                innerRadius, innerRadius,
                0, 0
        });
        return inner;
    }


    @Override
    public int getItemCount() {
        return dataList.size();
    }

    public static class DataHolder extends RecyclerView.ViewHolder {
        TextView driverName, driverTeam, driver_placement, driver_points,
                driverFamilyName, driverPointsText;
        ShapeableImageView driverImage, stripedLines;
        ConstraintLayout constraintLayout;
        RelativeLayout driver_layout;
        CardView cardView;
        RelativeLayout itemDriver;
        LinearLayout itemDriverLine;
        public DataHolder(@NonNull View itemView) {
            super(itemView);
            stripedLines = itemView.findViewById(R.id.striped_lines);
            itemDriverLine = itemView.findViewById(R.id.item_driver_line);
            cardView = itemView.findViewById(R.id.cardView);
            driver_layout = itemView.findViewById(R.id.driver_layout);
            driverName = itemView.findViewById(R.id.driverName);
            driverFamilyName = itemView.findViewById(R.id.driverFamilyName);
            driverTeam = itemView.findViewById(R.id.driverTeam);
            driver_placement = itemView.findViewById(R.id.driver_placement);
            driver_points = itemView.findViewById(R.id.driver_points);
            constraintLayout = itemView.findViewById(R.id.main_layout);
            //driverTeam_logo = itemView.findViewById(R.id.driverTeam_logo);
            driverImage = itemView.findViewById(R.id.driverImage);
            driverPointsText = itemView.findViewById(R.id.driver_points_text);
            itemDriver = itemView.findViewById(R.id.item_driver);
        }
    }
}
