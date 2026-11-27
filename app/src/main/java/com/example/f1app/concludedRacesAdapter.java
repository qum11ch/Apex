package com.example.f1app;

import static com.example.f1app.MainActivity.getStringByName;
import static com.example.f1app.teamsStandingsActivity.localizeLocality;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class concludedRacesAdapter extends RecyclerView.Adapter<concludedRacesAdapter.DataHolder>{
    Activity context;
    List<concludedRacesData> dataList;

    @Override
    public int getItemViewType(int position) {
        if (position == getItemCount() - 1)
            return 1;
        else
            return 2;
    }

    public concludedRacesAdapter(Activity context , List<concludedRacesData> datum){
        this.context = context;
        dataList = datum;
    }

    @NonNull
    @Override
    public DataHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view;
        if (viewType == 1) {
            view = LayoutInflater.from(context).inflate(R.layout.item_race_first, parent, false);
        } else {
            view = LayoutInflater.from(context).inflate(R.layout.item_concluded_race, parent, false);
        }

        return new DataHolder(view, viewType);
    }

    @Override
    public void onBindViewHolder(@NonNull DataHolder holder, int position) {
        concludedRacesData datum = dataList.get(position);

        String round = datum.getRoundNumber();
        holder.round.setText(round);

        String mSeason = datum.getSeason();

        String dateStart_string = datum.getDateStart();
        String dateEnd_string = datum.getDateEnd();

        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-M-d");
        LocalDate dateStart = LocalDate.parse(dateStart_string, dateFormatter);
        String dayStart = dateStart.format(DateTimeFormatter.ofPattern("dd"));

        LocalDate dateEnd = LocalDate.parse(dateEnd_string, dateFormatter);
        String dayEnd = dateEnd.format(DateTimeFormatter.ofPattern("dd"));

        String monthStart = dateStart.format(DateTimeFormatter.ofPattern("MMM"));
        String monthEnd = dateEnd.format(DateTimeFormatter.ofPattern("MMM"));

        if (datum.isCanceled()){
            holder.podiumNamesLayout.setVisibility(View.GONE);
            holder.cnd.setVisibility(View.VISIBLE);
        }else {
            holder.podiumNamesLayout.setVisibility(View.VISIBLE);
            holder.cnd.setVisibility(View.GONE);
        }

        if(monthStart.equals(monthEnd)){
            holder.raceMonth.setText(monthStart);
        }
        else{
            String month = monthStart + "-" + monthEnd;
            holder.raceMonth.setText(month);
        }

        String localeRaceName = datum.getRaceName().toLowerCase().replaceAll("\\s+", "_");
        String pastRaceName = context.getString(getStringByName(localeRaceName + "_text")) + " " + mSeason;
        holder.raceName.setText(pastRaceName);

        circuitNames.bind(context, datum.getCircuitName(), holder.circuitName);

        String locale;
        if (Locale.getDefault().getLanguage().equals("ru")){
            ArrayList<String> localizedData = localizeLocality(datum.getLocality(), datum.getCountry(), context);
            locale = localizedData.get(2);
        }else{
            String cityName = datum.getLocality();
            switch (cityName){
                case "Monaco":
                case "Singapore":
                    cityName = null;
                    break;
                default:
                    break;
            }
            if (cityName!=null){
                locale = cityName + ", " + datum.getCountry();
            }else{
                locale = datum.getCountry();
            }

        }

        holder.raceCountry.setText(locale);
        holder.day_start.setText(dayStart);
        holder.day_end.setText(dayEnd);

        String firstPlace_code = datum.getWinnerDriverCode();
        String secondPlace_code = datum.getSecondPlaceCode();
        String thirdPlace_code = datum.getThirdPlaceCode();

        if (holder.podium != null) {
            // the first row of the list: cards with photos
            holder.podium.bind(mSeason, round, firstPlace_code, secondPlace_code, thirdPlace_code);
        } else {
            // the other rows: codes only
            holder.firstPlace_code.setText(firstPlace_code);
            holder.secondPlace_code.setText(secondPlace_code);
            holder.thirdPlace_code.setText(thirdPlace_code);
        }

        holder.constraintLayout.setOnClickListener(v -> {
            Intent intent = new Intent(context, concludedRaceActivity.class);
            Bundle bundle = new Bundle();
            bundle.putString("raceName" , datum.getRaceName());
            bundle.putString("raceStartDay" , dayStart);
            bundle.putString("raceEndDay" , dayEnd);
            bundle.putString("raceStartMonth" , monthStart);
            bundle.putString("raceEndMonth" , monthEnd);
            bundle.putString("circuitName" , datum.getCircuitName());
            bundle.putString("roundCount" , datum.getRoundNumber());
            bundle.putString("raceCountry" , datum.getCountry());
            bundle.putString("dateStart", datum.getDateStart());
            bundle.putString("dateEnd", datum.getDateEnd());
            bundle.putString("firstPlaceCode", firstPlace_code);
            bundle.putString("secondPlaceCode", secondPlace_code);
            bundle.putString("thirdPlaceCode", thirdPlace_code);
            bundle.putBoolean("isCanceled", datum.isCanceled());
            intent.putExtras(bundle);
            context.startActivity(intent);
        });

    }

    @Override
    public int getItemCount() {
        return dataList.size();
    }

    public static class DataHolder extends RecyclerView.ViewHolder{
        TextView round, day_start, day_end, raceMonth, raceCountry, raceName, circuitName,
                secondPlace_code, firstPlace_code, thirdPlace_code, cnd;
        ConstraintLayout constraintLayout;
        LinearLayout podiumNamesLayout;
        podiumCards podium;

        public DataHolder(@NonNull View itemView, int viewType) {
            super(itemView);
            round = itemView.findViewById(R.id.round);
            day_start = itemView.findViewById(R.id.day_start);
            day_end = itemView.findViewById(R.id.day_end);
            raceMonth = itemView.findViewById(R.id.raceMonth);
            raceCountry = itemView.findViewById(R.id.raceCountry);
            raceName = itemView.findViewById(R.id.raceName);
            circuitName = itemView.findViewById(R.id.circuitName);
            constraintLayout = itemView.findViewById(R.id.main_layout);
            podiumNamesLayout = itemView.findViewById(R.id.podiumNamesLayout);
            cnd = itemView.findViewById(R.id.cnd_layout);

            if (viewType == 1){
                podium = new podiumCards(itemView.findViewById(R.id.podiumRow), true);
            } else{
                secondPlace_code = itemView.findViewById(R.id.secondPlace_code);
                firstPlace_code = itemView.findViewById(R.id.firstPlace_code);
                thirdPlace_code = itemView.findViewById(R.id.thirdPlace_code);
            }
        }
    }

}