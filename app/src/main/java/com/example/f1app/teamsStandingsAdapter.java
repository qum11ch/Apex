package com.example.f1app;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.view.ViewCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.List;

public class teamsStandingsAdapter extends RecyclerView.Adapter<teamsStandingsAdapter.DataHolder>{
    Context context;
    List<teamsList> dataList;

    public teamsStandingsAdapter(Context context , List<teamsList> datum){
        this.context = context;
        dataList = datum;
    }

    @NonNull
    @Override
    public DataHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view;
        if (viewType == 1) {
            view = LayoutInflater.from(context).inflate(R.layout.item_team_first, parent, false);
        } else {
            view = LayoutInflater.from(context).inflate(R.layout.item_team, parent, false);
        }
        return new DataHolder(view);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void onBindViewHolder(@NonNull DataHolder holder, int position) {
        teamsList datum = dataList.get(position);
        ArrayList<String> teamDrivers = datum.getDrivers();
        holder.teamName.setText(datum.getTeam());

        holder.teamDriverFirst.setText(getFamilyName(teamDrivers.get(0)));
        holder.teamDriverSecond.setText(getFamilyName(teamDrivers.get(1)));

        String darkTeamColor = datum.getTeamColor();

        holder.team_car.setScaleX(1f);
        holder.team_car.setImageDrawable(null);
        holder.team_car.setImageBitmap(null);

        //String season = datum.getSeason();

        StorageReference mTeamCar = datum.getImageUrl();


        GlideApp.with(context)
                .asBitmap()
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .skipMemoryCache(true)
                .error(R.drawable.placeholder_car)
                .load(mTeamCar)
                .into(new CustomTarget<Bitmap>() {
                    @Override
                    public void onResourceReady(@NonNull Bitmap resource,
                                                @Nullable Transition<? super Bitmap> transition) {

                        int width = resource.getWidth();
                        int height = resource.getHeight();

                        Bitmap leftHalf = Bitmap.createBitmap(resource, 0, 0,
                                width / 2, height);

                        Bitmap displayBitmap;
                        if (datum.getTeamId().equals("audi")) {
                            Bitmap rightHalf = Bitmap.createBitmap(resource, width / 2, 0,
                                    width / 2, height);
                            Matrix mirrorMatrix = new Matrix();
                            mirrorMatrix.preScale(-1f, 1f);
                            displayBitmap = Bitmap.createBitmap(
                                    rightHalf, 0, 0,
                                    rightHalf.getWidth(), rightHalf.getHeight(),
                                    mirrorMatrix, true
                            );
                        } else {
                            displayBitmap = leftHalf;
                        }

                        holder.team_car.setImageBitmap(displayBitmap);
                        holder.team_car.setScaleType(ImageView.ScaleType.CENTER_CROP);
                        holder.team_car.setScaleX(1f);
                    }

                    @Override
                    public void onLoadCleared(@Nullable Drawable placeholder) {

                    }
                });

        //String mTeamId = datum.getTeamId();

        int colorRgb = Color.parseColor("#303030");

        if (darkTeamColor != null){
            colorRgb = Color.parseColor(darkTeamColor);
        }
        int alpha = 0x66;
        int colorWithAlpha = (alpha << 24) | (colorRgb & 0x00FFFFFF);

        int alphaLine = 0xCC;
        int colorWithAlphaLine = (alphaLine << 24) | (colorRgb & 0x00FFFFFF);

        ViewCompat.setBackgroundTintList(
                holder.itemTeam,
                ColorStateList.valueOf(colorWithAlpha)
        );

        holder.itemTeamLine.setBackground(createFrameWithLeftOffset(colorWithAlphaLine, context));

        if (datum.getStartSeasonInfo()) {
            holder.teamPointsText.setVisibility(View.GONE);
            holder.teamPoints.setVisibility(View.GONE);
        }else{
            holder.teamPointsText.setVisibility(View.VISIBLE);
            holder.teamPoints.setVisibility(View.VISIBLE);
        }

        if (holder.getItemViewType() == 1){
            if (datum.getStartSeasonInfo()) {
                int width = 0;
                int height = 0;
                holder.cardView.getLayoutParams().width = width;
                holder.cardView.getLayoutParams().height = height;
            }else{
                holder.teamPosition.setText(datum.getPosition());
                String teamPoints = datum.getPoints();
                holder.teamPoints.setText(teamPoints);
            }
        }else{
            holder.teamPosition.setText(datum.getPosition());
            String teamPoints = datum.getPoints();
            holder.teamPoints.setText(teamPoints);

        }

        holder.constraintLayout.setOnClickListener(v -> {
            Intent intent = new Intent(context , teamPageActivity.class);
            Bundle bundle = new Bundle();
            bundle.putString("teamName", datum.getTeam());
            bundle.putString("teamId", datum.getTeamId());
            bundle.putStringArrayList("teamDrivers", teamDrivers);
            bundle.putString("currentSeason", datum.getSeason());
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
    public int getItemViewType(int position) {
        if (position == 0)
            return 1;
        else
            return 2;
    }


    @Override
    public int getItemCount() {
        return dataList.size();
    }

    public static String getFamilyName (String driverFullname){
        String[] parts = driverFullname.split(" ");
        String driverFamilyName;
        if(driverFullname.equals("Andrea Kimi Antonelli")){
            driverFamilyName = parts[2];
        }else{
            driverFamilyName = parts[1];
        }
        return driverFamilyName;
    }

    public static class DataHolder extends RecyclerView.ViewHolder{
        TextView teamName, teamPoints, teamPosition,
                teamDriverFirst, teamDriverSecond, teamPointsText;
        ShapeableImageView team_car;
        ConstraintLayout constraintLayout;
        RelativeLayout team_layout;
        CardView cardView;
        LinearLayout itemTeam, itemTeamLine;
        public DataHolder(@NonNull View itemView) {
            super(itemView);
            itemTeamLine = itemView.findViewById(R.id.item_team_line);
            teamPointsText = itemView.findViewById(R.id.team_points_text);
            itemTeam = itemView.findViewById(R.id.item_team);
            team_layout = itemView.findViewById(R.id.team_layout);
            cardView = itemView.findViewById(R.id.cardView);
            teamName = itemView.findViewById(R.id.teamName);
            teamPoints = itemView.findViewById(R.id.team_pts);
            teamPosition = itemView.findViewById(R.id.team_placement);
            teamDriverFirst = itemView.findViewById(R.id.driverFirst);
            teamDriverSecond = itemView.findViewById(R.id.driverSecond);
            constraintLayout = itemView.findViewById(R.id.main_layout);
            team_car = itemView.findViewById(R.id.team_car);
        }
    }
}
