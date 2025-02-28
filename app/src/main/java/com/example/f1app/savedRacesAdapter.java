package com.example.f1app;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

public class savedRacesAdapter extends RecyclerView.Adapter<savedRacesAdapter.DataHolder>{
    Activity context;
    List<savedRacesData> dataList;

    public savedRacesAdapter(Activity context , List<savedRacesData> datum){
        this.context = context;
        dataList = datum;
    }

    @NonNull
    @Override
    public savedRacesAdapter.DataHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_saved_race, parent , false);
        return new savedRacesAdapter.DataHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull savedRacesAdapter.DataHolder holder, int position) {
        savedRacesData datum = dataList.get(position);
        String fullRaceName = datum.getRaceSeason() + " " + datum.getRaceName();
        holder.raceName.setText(fullRaceName);
        String saveDate = "Saved: " + datum.getSaveDate();
        holder.save_date.setText(saveDate);
        holder.number.setText(String.valueOf(position + 1));

        holder.constraintLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openRace(datum.getRaceName(), datum.getRaceSeason());
            }
        });
    }

    @Override
    public int getItemCount() {
        return dataList.size();
    }

    public class DataHolder extends RecyclerView.ViewHolder{
        TextView raceName, number, save_date;
        ConstraintLayout constraintLayout;
        public DataHolder(@NonNull View itemView) {
            super(itemView);
            raceName = itemView.findViewById(R.id.raceName);
            number = itemView.findViewById(R.id.number);
            save_date = itemView.findViewById(R.id.save_date);
            constraintLayout = itemView.findViewById(R.id.main_layout);
        }
    }

    public void openRace(String raceName, String raceSeason){

        DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference();
        rootRef.child("schedule/season").child(raceSeason).child(raceName)
                .addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String mFirstPlaceCode = snapshot.child("RaceResults")
                        .child("raceWinnerCode").getValue(String.class);
                String mSecondPlaceCode = snapshot.child("RaceResults")
                        .child("raceSecondCode").getValue(String.class);
                String mThirdPlaceCode = snapshot.child("RaceResults")
                        .child("raceThirdCode").getValue(String.class);

                String mCircuitId = snapshot.child("Circuit/circuitId").getValue(String.class);
                String mRaceDate = snapshot.child("raceDate").getValue(String.class);
                Integer mRound = snapshot.child("round").getValue(Integer.class);
                String dateStart = snapshot.child("FirstPractice/firstPracticeDate").getValue(String.class);
                String dateEnd = snapshot.child("raceDate").getValue(String.class);

                DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-M-d");

                LocalDate startDateOngoing = LocalDate.parse(dateStart, dateFormatter);
                String dayStart = startDateOngoing.format(DateTimeFormatter.ofPattern("dd")).toString();

                LocalDate endDateOngoing = LocalDate.parse(dateEnd, dateFormatter);
                String dayEnd = endDateOngoing.format(DateTimeFormatter.ofPattern("dd")).toString();

                String monthStart = startDateOngoing.format(DateTimeFormatter.ofPattern("MMM")).toString();
                String monthEnd = endDateOngoing.format(DateTimeFormatter.ofPattern("MMM")).toString();

                Bundle bundle = new Bundle();
                rootRef.child("circuits").child(mCircuitId)
                        .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        String mCountry = snapshot.child("country").getValue(String.class);
                        String mCircuitName = snapshot.child("circuitName").getValue(String.class);

                        if(mFirstPlaceCode.equals("N/C")){
                            Intent intent = new Intent(context , futureRaceActivity.class);
                            bundle.putString("raceName" , raceName);
                            bundle.putString("futureRaceStartDay" , dayStart);
                            bundle.putString("futureRaceEndDay" , dayEnd);
                            bundle.putString("futureRaceStartMonth" , monthStart);
                            bundle.putString("futureRaceEndMonth" , monthEnd);
                            bundle.putString("roundCount" , String.valueOf(mRound));
                            bundle.putString("raceCountry" , mCountry);
                            bundle.putString("circuitId", mCircuitId);
                            bundle.putString("dateStart", dateStart);
                            bundle.putString("circuitName", mCircuitName);
                            intent.putExtras(bundle);
                            context.startActivity(intent);
                        }else{
                            Intent intent = new Intent(context , concludedRaceActivity.class);
                            Bundle bundle = new Bundle();
                            bundle.putString("raceName" , raceName);
                            bundle.putString("raceStartDay" , dayStart);
                            bundle.putString("raceEndDay" , dayEnd);
                            bundle.putString("raceStartMonth" , monthStart);
                            bundle.putString("raceEndMonth" , monthEnd);
                            bundle.putString("roundCount" , String.valueOf(mRound));
                            bundle.putString("raceCountry" , mCountry);
                            bundle.putString("dateStart", dateStart);
                            bundle.putString("firstPlaceCode", mFirstPlaceCode);
                            bundle.putString("secondPlaceCode", mSecondPlaceCode);
                            bundle.putString("thirdPlaceCode", mThirdPlaceCode);
                            bundle.putString("circuitName", mCircuitName);

                            intent.putExtras(bundle);
                            context.startActivity(intent);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

}
