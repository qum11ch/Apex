package com.example.f1app;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.List;

public class raceResultsQualiAdapter extends RecyclerView.Adapter<raceResultsQualiAdapter.DataHolder>{
    Activity context;
    List<raceResultsQualiData> dataList;

    public raceResultsQualiAdapter(Activity context , List<raceResultsQualiData> datum){
        this.context = context;
        this.dataList = datum;
    }

    @NonNull
    @Override
    public raceResultsQualiAdapter.DataHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_quali_result, parent , false);
        return new raceResultsQualiAdapter.DataHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull raceResultsQualiAdapter.DataHolder holder, int position) {
        raceResultsQualiData datum = dataList.get(position);

        String mDriverCode = datum.getDriverCode();
        String mDriverTeam = datum.getDriverTeam();
        String mDriverPosition = datum.getDriverPosition();
        String mQ1 = datum.getQ1time();
        String mQ2 = datum.getQ2time();
        String mQ3 = datum.getQ3time();
        String mSeason = datum.getSeason();

        holder.driverCode.setText(mDriverCode);
        holder.driver_placement.setText(mDriverPosition);

        if(mQ1.isEmpty()){
            holder.Q1_time.setBackgroundColor(Color.TRANSPARENT);
            holder.Q1_time.setText("--");
        }else{
            holder.Q1_time.setText(mQ1);
        }

        if(mQ2.isEmpty()){
            holder.Q2_time.setBackgroundColor(Color.TRANSPARENT);
            holder.Q2_time.setText("--");
        } else if (mQ2.equals("--")) {
            holder.Q2_time.setBackgroundColor(Color.TRANSPARENT);
            holder.Q2_time.setText(mQ2);
        }else{
            holder.Q2_time.setText(mQ2);
        }

        if(mQ3.isEmpty()){
            holder.Q3_time.setBackgroundColor(Color.TRANSPARENT);
            holder.Q3_time.setText("--");
        } else if (mQ3.equals("--")) {
            holder.Q3_time.setBackgroundColor(Color.TRANSPARENT);
            holder.Q3_time.setText(mQ3);
        }else{
            holder.Q3_time.setText(mQ3);
        }

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
        TextView driver_placement, driverCode, Q1_time, Q2_time, Q3_time;
        View line;
        ConstraintLayout constraintLayout;
        public DataHolder(@NonNull View itemView) {
            super(itemView);
            driver_placement = itemView.findViewById(R.id.driver_placement);
            driverCode = itemView.findViewById(R.id.driverCode);
            Q1_time = itemView.findViewById(R.id.Q1_time);
            Q2_time = itemView.findViewById(R.id.Q2_time);
            Q3_time = itemView.findViewById(R.id.Q3_time);
            line = itemView.findViewById(R.id.line);
        }
    }

}
