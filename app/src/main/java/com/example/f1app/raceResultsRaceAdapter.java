package com.example.f1app;

import android.app.Activity;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.List;

public class raceResultsRaceAdapter extends RecyclerView.Adapter<raceResultsRaceAdapter.DataHolder>{
    Activity context;
    List<raceResultsRaceData> dataList;

    public raceResultsRaceAdapter(Activity context , List<raceResultsRaceData> datum){
        this.context = context;
        this.dataList = datum;
    }

    @NonNull
    @Override
    public raceResultsRaceAdapter.DataHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_race_result, parent , false);
        return new raceResultsRaceAdapter.DataHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull raceResultsRaceAdapter.DataHolder holder, int position) {
        raceResultsRaceData datum = dataList.get(position);

        String mDriverCode = datum.getDriverCode();
        String mDriverTeam = datum.getDriverTeam();
        String mDriverPosition = datum.getDriverPosition();
        String mTime = datum.getDriverTime();
        String mPoints = datum.getDriverPoints();
        String mSeason = datum.getSeason();

        holder.driverCode.setText(mDriverCode);
        holder.driver_placement.setText(mDriverPosition);
        holder.driver_time.setText(mTime);
        holder.driverPoints.setText(mPoints);


        Integer year = Integer.parseInt(mSeason);

        if (year >= 2024){
            DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference();
            rootRef.child("constructors/" + mDriverTeam).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    String teamColor = "#" + snapshot.child("color").getValue(String.class);
                    holder.line.setBackgroundColor(Color.parseColor(teamColor));
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e("raceResultsQualiAdapter: Fatal error in Firebase getting team color", " " + error.getMessage());
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return dataList.size();
    }

    public class DataHolder extends RecyclerView.ViewHolder{
        TextView driver_placement, driverCode, driver_time, driverPoints;
        View line;
        public DataHolder(@NonNull View itemView) {
            super(itemView);
            driver_placement = itemView.findViewById(R.id.driver_placement);
            driverCode = itemView.findViewById(R.id.driverCode);
            driver_time = itemView.findViewById(R.id.driver_time);
            driverPoints = itemView.findViewById(R.id.driverPoints);
            line = itemView.findViewById(R.id.line);
        }
    }

}
