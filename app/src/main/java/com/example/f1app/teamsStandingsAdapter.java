package com.example.f1app;

import static com.example.f1app.driversStandingsAdapter.setTeamColor;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.google.firebase.storage.FirebaseStorage;
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
        holder.teamDriverFirst.setText(teamDrivers.get(0));
        holder.teamDriverSecond.setText(teamDrivers.get(1));

        String season = datum.getSeason();

        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference storageRef = storage.getReference();

        StorageReference mTeamCar = storageRef.child("teams/" + datum.getTeamId().toLowerCase() + "_"  + season + ".png");

        GlideApp.with(context)
                .load(mTeamCar)
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .skipMemoryCache(true)
                .transition(DrawableTransitionOptions.withCrossFade())
                .error(R.drawable.placeholder_car)
                .into(holder.team_car);

        holder.scrollView.setOnTouchListener(new OnTouch());

        if (holder.getItemViewType() == 1){
            if (datum.getTeamId().equals("audi")) {
                holder.team_car.setScaleX(-1);
                holder.team_car.setScrollX(250);
            } else {
                holder.team_car.setScrollX(-250);
            }
            if (datum.getStartSeasonInfo()) {
                int width = 0;
                int height = 0;
                holder.cardView.getLayoutParams().width = width;
                holder.cardView.getLayoutParams().height = height;
            }else{
                holder.teamPosition.setText(datum.getPosition());
                String teamPoints = datum.getPoints() + " " + context.getString(R.string.pts_header);
                holder.teamPoints.setText(teamPoints);
            }
        }else{
            if (datum.getTeamId().equals("audi")) {
                holder.team_car.setScaleX(-1);
                holder.team_car.setScrollX(185);
            } else {
                holder.team_car.setScrollX(-185);
            }
            if (datum.getStartSeasonInfo()) {
                if (datum.getTeamId().equals("audi")) {
                    holder.team_car.setScaleX(-1);
                    holder.team_car.setScrollX(135);
                } else {
                    holder.team_car.setScrollX(-135);
                }

                holder.leftLayout.setLayoutParams(new LinearLayout.LayoutParams(0, RelativeLayout.LayoutParams.MATCH_PARENT, 0.2f));
                LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(0, RelativeLayout.LayoutParams.WRAP_CONTENT, 3f);
                layoutParams.setMargins(0, 10,0,20);
                holder.team_layout.setLayoutParams(layoutParams);
                int width = 0;
                holder.teamPosition.getLayoutParams().width = width;
                holder.teamPoints.getLayoutParams().width = width;
            } else {
                holder.teamPosition.setText(datum.getPosition());
                String teamPoints = datum.getPoints() + " " + context.getString(R.string.pts_header);
                holder.teamPoints.setText(teamPoints);
            }

            String mTeamId = datum.getTeamId();
            setTeamColor(mTeamId, holder.line, context);
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
        TextView teamName, teamPoints, teamPosition,
                teamDriverFirst, teamDriverSecond;
        ImageView team_car;
        ConstraintLayout constraintLayout;
        RelativeLayout leftLayout, team_layout;
        View line;
        CardView cardView;
        HorizontalScrollView scrollView;
        public DataHolder(@NonNull View itemView) {
            super(itemView);
            leftLayout = itemView.findViewById(R.id.left_layout);
            team_layout = itemView.findViewById(R.id.team_layout);
            cardView = itemView.findViewById(R.id.cardView);
            teamName = itemView.findViewById(R.id.teamName);
            teamPoints = itemView.findViewById(R.id.team_pts);
            teamPosition = itemView.findViewById(R.id.team_placement);
            teamDriverFirst = itemView.findViewById(R.id.driverFirst);
            teamDriverSecond = itemView.findViewById(R.id.driverSecond);
            constraintLayout = itemView.findViewById(R.id.main_layout);
            team_car = itemView.findViewById(R.id.team_car);
            line = itemView.findViewById(R.id.line);
            scrollView= itemView.findViewById(R.id.horizontal_scroll);
        }
    }

    private static class OnTouch implements View.OnTouchListener
    {
        @SuppressLint("ClickableViewAccessibility")
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            return true;
        }
    }
}
