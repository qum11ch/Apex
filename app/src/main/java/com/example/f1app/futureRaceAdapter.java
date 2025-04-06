package com.example.f1app;

import static com.example.f1app.MainActivity.getStringByName;
import static com.example.f1app.teamsStandingsActivity.localizeLocality;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class futureRaceAdapter extends RecyclerView.Adapter<futureRaceAdapter.DataHolder>{
    Activity context;
    List<futureRaceData> dataList;

    public futureRaceAdapter(Activity context , List<futureRaceData> datum){
        this.context = context;
        dataList = datum;
    }

    @NonNull
    @Override
    public futureRaceAdapter.DataHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_future_race, parent , false);
        return new DataHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull futureRaceAdapter.DataHolder holder, int position) {
        LocalDate currentDate = LocalDate.now();
        String currentYear = Integer.toString(currentDate.getYear());


        futureRaceData datum = dataList.get(position);

        String round = datum.getFutureRaceRound();
        holder.round.setText(round);

        String dateStart_string = datum.getFutureRaceStartDate();
        String dateEnd_string = datum.getFutureRaceEndDate();

        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-M-d");
        LocalDate dateStart = LocalDate.parse(dateStart_string, dateFormatter);
        String dayStart = dateStart.format(DateTimeFormatter.ofPattern("dd"));

        LocalDate dateEnd = LocalDate.parse(dateEnd_string, dateFormatter);
        String dayEnd = dateEnd.format(DateTimeFormatter.ofPattern("dd"));

        String monthStart = dateStart.format(DateTimeFormatter.ofPattern("MMM"));
        String monthEnd = dateEnd.format(DateTimeFormatter.ofPattern("MMM"));

        if(monthStart.equals(monthEnd)){
            holder.raceMonth.setText(monthStart);
        }
        else{
            String month = monthStart + "-" + monthEnd;
            holder.raceMonth.setText(month);
        }

        String localeRaceName = datum.getFutureRaceName().toLowerCase().replaceAll("\\s+", "_");
        String futureRaceName = context.getString(getStringByName(localeRaceName + "_text")) + " " + currentYear;
        holder.raceName.setText(futureRaceName);

        //World.init(context);
        //holder.countryImage.setImageResource(World.getFlagOf(getCountryCode(datum.getCountry())));

        holder.circuitName.setText(context.getString(getStringByName(datum.getCircuitId() + "_text")));

        String locale;
        if (Locale.getDefault().getLanguage().equals("ru")){
            ArrayList<String> localizedData = localizeLocality(datum.getLocale(), datum.getFutureRaceCountry(), context);
            //String country = localizedData.get(0);
            //String cityName = localizedData.get(1);
            locale = localizedData.get(2);
        }else{
            String cityName = datum.getLocale();
            switch (cityName){
                case "Monaco":
                case "Singapore":
                    cityName = null;
                    break;
                default:
                    break;
            }
            if (cityName!=null){
                locale = cityName + ", " + datum.getFutureRaceCountry();
            }else{
                locale = datum.getFutureRaceCountry();
            }

        }

        holder.raceCountry.setText(locale);
        holder.day_start.setText(dayStart);
        holder.day_end.setText(dayEnd);


        holder.constraintLayout.setOnClickListener(v -> {
            Intent intent = new Intent(context , futureRaceActivity.class);

            Bundle bundle = new Bundle();
            bundle.putString("raceName" , datum.getFutureRaceName());
            bundle.putString("futureRaceStartDay" , dayStart);
            bundle.putString("futureRaceEndDay" , dayEnd);
            bundle.putString("futureRaceStartMonth" , monthStart);
            bundle.putString("futureRaceEndMonth" , monthEnd);
            bundle.putString("circuitName" , datum.getFutureCircuitName());
            bundle.putString("roundCount" , datum.getFutureRaceRound());
            bundle.putString("raceCountry" , datum.getFutureRaceCountry());
            bundle.putString("circuitId", datum.getCircuitId());
            bundle.putString("dateStart", datum.getFutureRaceStartDate());
            intent.putExtras(bundle);

            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return dataList.size();
    }

    public static class DataHolder extends RecyclerView.ViewHolder{
        TextView round, day_start, day_end, raceMonth, raceCountry, raceName, circuitName;
        ConstraintLayout constraintLayout;
        public DataHolder(@NonNull View itemView) {
            super(itemView);
            round = itemView.findViewById(R.id.round);
            day_start = itemView.findViewById(R.id.day_start);
            day_end = itemView.findViewById(R.id.day_end);
            raceMonth = itemView.findViewById(R.id.raceMonth);
            raceCountry = itemView.findViewById(R.id.raceCountry);
            raceName = itemView.findViewById(R.id.raceName);
            circuitName = itemView.findViewById(R.id.circuitName);
            constraintLayout = itemView.findViewById(R.id.main_layout);
        }
    }
}
