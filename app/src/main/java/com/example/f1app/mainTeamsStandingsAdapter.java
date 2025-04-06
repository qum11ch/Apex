package com.example.f1app;


import static com.example.f1app.driversStandingsAdapter.getColorByName;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
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
        return new mainTeamsStandingsAdapter.DataHolder(view, viewType);
    }

    @Override
    public void onBindViewHolder(@NonNull DataHolder holder, int position) {
        teamsList datum = dataList.get(position);
        ArrayList<String> teamDrivers = datum.getDrivers();
        holder.teamName.setText(datum.getTeam());

        if (position == dataList.size() - 1){
            holder.bottomLine.getLayoutParams().height = 0;
        }

        if (datum.getStartSeasonInfo()) {
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

        int resourceId_teamColor = getColorByName(datum.getTeamId());
        holder.line.setBackgroundResource(resourceId_teamColor);

        holder.constraintLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(context , teamPageActivity.class);
                Bundle bundle = new Bundle();
                bundle.putString("teamName", datum.getTeam());
                bundle.putString("teamId", datum.getTeamId());
                bundle.putStringArrayList("teamDrivers", teamDrivers);
                intent.putExtras(bundle);
                context.startActivity(intent);
            }
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

    public class DataHolder extends RecyclerView.ViewHolder{
        TextView teamName, teamPoints, teamPosition;
        ConstraintLayout constraintLayout;
        RelativeLayout leftLayout, team_layout;
        View line, bottomLine;
        CardView cardView;
        public DataHolder(@NonNull View itemView, int viewType) {
            super(itemView);
            leftLayout = itemView.findViewById(R.id.left_layout);
            team_layout = itemView.findViewById(R.id.team_layout);
            cardView = itemView.findViewById(R.id.cardView);
            teamName = itemView.findViewById(R.id.teamName);
            teamPoints = itemView.findViewById(R.id.team_pts);
            teamPosition = itemView.findViewById(R.id.team_placement);
            constraintLayout = itemView.findViewById(R.id.main_layout);
            line = itemView.findViewById(R.id.line);
            bottomLine = itemView.findViewById(R.id.bottomLine);
        }
    }
}
