package com.example.f1app;

import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
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
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.List;

public class mainTeamsStandingsAdapter extends RecyclerView.Adapter<mainTeamsStandingsAdapter.DataHolder>{
    Context context;
    List<teamsList> dataList;

    public mainTeamsStandingsAdapter(Context context , List<teamsList> datum){
        this.context = context;
        dataList = datum;
    }

    @NonNull
    @Override
    public DataHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view;
        view = LayoutInflater.from(context).inflate(R.layout.item_main_team, parent , false);
        return new DataHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DataHolder holder, int position) {
        teamsList datum = dataList.get(position);
        ArrayList<String> teamDrivers = datum.getDrivers();
        holder.teamName.setText(datum.getTeam());
        holder.teamPosition.setText(datum.getPosition());

        String darkTeamColor = datum.getTeamColor();
        StorageReference mTeamCar = datum.getImageUrl();

        if (datum.getStartSeasonInfo()) {
            holder.teamPointsText.setVisibility(View.GONE);
            holder.teamPoints.setVisibility(View.GONE);
        } else {
            holder.teamPointsText.setVisibility(View.VISIBLE);
            holder.teamPoints.setVisibility(View.VISIBLE);
            holder.teamPoints.setText(datum.getPoints());
        }

        //String mTeamId = datum.getTeamId();
        //String mSeason = datum.getSeason();

        //FirebaseStorage storage = FirebaseStorage.getInstance();
        //StorageReference storageRef = storage.getReference();

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

                        Bitmap leftHalf = Bitmap.createBitmap(
                                resource,
                                0,
                                0,
                                width /2,
                                height
                        );

                        Bitmap rightHalf = Bitmap.createBitmap(
                                resource,
                                width /2,
                                0,
                                width /2,
                                height
                        );

                        if (datum.getTeamId().equals("audi")){
                            holder.teamCar.setImageBitmap(rightHalf);
                            holder.teamCar.setScaleX(-1f);
                        }else{
                            holder.teamCar.setImageBitmap(leftHalf);
                        }

                        Bitmap displayBitmap = datum.getTeamId().equals("audi") ? rightHalf : leftHalf;

                        holder.teamCar.post(() -> {
                            Matrix matrix = new Matrix();
                            matrix.setRectToRect(
                                    new RectF(0, 0, displayBitmap.getWidth(), displayBitmap.getHeight()),
                                    new RectF(0, 0, holder.teamCar.getWidth(), holder.teamCar.getHeight()),
                                    Matrix.ScaleToFit.FILL
                            );

                            holder.teamCar.setImageMatrix(matrix);
                            holder.teamCar.setScaleType(ImageView.ScaleType.MATRIX);
                        });
                    }

                    @Override
                    public void onLoadCleared(@Nullable Drawable placeholder) {
                    }
                });

        int colorRgb = Color.parseColor("#303030");

        if (darkTeamColor != null){
            colorRgb = Color.parseColor(darkTeamColor);
        }
        int alpha = 0xCC;
        int colorWithAlpha = (alpha << 24) | (colorRgb & 0x00FFFFFF);

        ViewCompat.setBackgroundTintList(
                holder.itemTeam,
                ColorStateList.valueOf(colorWithAlpha)
        );

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

    public static class DataHolder extends RecyclerView.ViewHolder{
        TextView teamName, teamPoints, teamPosition, teamPointsText;
        ConstraintLayout constraintLayout;
        RelativeLayout team_layout;
        CardView cardView;
        ImageView teamCar;
        LinearLayout itemTeam;
        public DataHolder(@NonNull View itemView) {
            super(itemView);
            itemTeam = itemView.findViewById(R.id.item_team);
            teamPointsText = itemView.findViewById(R.id.team_points_text);
            team_layout = itemView.findViewById(R.id.team_layout);
            cardView = itemView.findViewById(R.id.cardView);
            teamName = itemView.findViewById(R.id.teamName);
            teamPoints = itemView.findViewById(R.id.team_pts);
            teamPosition = itemView.findViewById(R.id.team_placement);
            constraintLayout = itemView.findViewById(R.id.main_layout);
            teamCar = itemView.findViewById(R.id.team_car);
        }
    }
}
