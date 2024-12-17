package com.example.f1app;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

public class raceDataAdapter extends RecyclerView.Adapter<raceDataAdapter.DataHolder> {
    Context context;
    List<raceData> dataList;

    public raceDataAdapter(Context context , List<raceData> datum){
        this.context = context;
        dataList = datum;
    }

    @NonNull
    @Override
    public DataHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item , parent , false);
        return new DataHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DataHolder holder, int position) {
        raceData datum = dataList.get(position);
        holder.raceName.setText(datum.getRaceName());
        holder.raceWinner.setText(datum.getWinnerDriver());

        String raceDate = datum.getRaceDate();
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
        String[] monthName = {"Jan.", "Feb.", "Mar.", "Apr.", "May", "June", "July", "Aug.", "Sept.",
                "Oct.", "Nov.", "Dec."};
        try{
            Date dateRace = formatter.parse(raceDate);
            Calendar myCal = new GregorianCalendar();
            myCal.setTime(dateRace);
            String raceMonth = monthName[myCal.get(Calendar.MONTH) - 1];
            String raceDay = String.valueOf(myCal.get(Calendar.DAY_OF_MONTH));
            String raceYear = String.valueOf(myCal.get(Calendar.YEAR));
            String date;
            date = raceDay  + " " + raceMonth + " " + raceYear;
            holder.raceDate.setText(date);
        }catch(ParseException e){
            Log.d("ParseExeption", "" + e);
        }

        holder.constraintLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(context , savedRacePage.class);
                Bundle bundle = new Bundle();
                bundle.putString("id", datum.getId());
                bundle.putString("raceName" , datum.getRaceName());
                bundle.putString("raceDate" , datum.getRaceDate());
                bundle.putString("circuitName" , datum.getCircuitName());
                bundle.putString("raceRound" , datum.getRound());
                bundle.putString("raceCountry" , datum.getCountry());
                bundle.putString("raceWinner", datum.getWinnerDriver());
                bundle.putString("winnerConstructor", datum.getWinnerConstructor());
                bundle.putString("secondPlaceDriver" , datum.getSecondDriver());
                bundle.putString("secondPlaceConstructor" , datum.getSecondConstructor());
                bundle.putString("thirdPlaceDriver" , datum.getThirdDriver());
                bundle.putString("circuitId", datum.getCircuitId());
                bundle.putString("thirdPlaceConstructor" , datum.getThirdConstructor());
                bundle.putString("src" , datum.getSrc());
                intent.putExtras(bundle);

                context.startActivity(intent);
            }
        });
    }

    @Override
    public int getItemCount() {
        return dataList.size();
    }

    public class DataHolder extends RecyclerView.ViewHolder{
        TextView raceName , raceDate , raceWinner;
        ConstraintLayout constraintLayout;
        public DataHolder(@NonNull View itemView) {
            super(itemView);
            raceName = itemView.findViewById(R.id.raceName);
            raceDate = itemView.findViewById(R.id.raceDate);
            raceWinner = itemView.findViewById(R.id.raceWinner);
            constraintLayout = itemView.findViewById(R.id.main_layout);
        }
    }

}
