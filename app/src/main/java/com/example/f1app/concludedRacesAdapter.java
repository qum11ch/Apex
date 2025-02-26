package com.example.f1app;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
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
        switch (viewType){
            case 1:
                view = LayoutInflater.from(context).inflate(R.layout.item_race_first, parent , false);
                break;
            default:
                view = LayoutInflater.from(context).inflate(R.layout.item_race, parent , false);
                break;
        }
        return new DataHolder(view, viewType);
    }

    @Override
    public void onBindViewHolder(@NonNull DataHolder holder, int position) {
        LocalDate currentDate = LocalDate.now();
        String currentYear = Integer.toString(currentDate.getYear());

        concludedRacesData datum = dataList.get(position);

        String round = datum.getRoundNumber();
        holder.round.setText(round);

        String dateStart_string = datum.getDateStart();
        String dateEnd_string = datum.getDateEnd();

        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-M-d");
        LocalDate dateStart = LocalDate.parse(dateStart_string, dateFormatter);
        String dayStart = dateStart.format(DateTimeFormatter.ofPattern("dd")).toString();

        LocalDate dateEnd = LocalDate.parse(dateEnd_string, dateFormatter);
        String dayEnd = dateEnd.format(DateTimeFormatter.ofPattern("dd")).toString();

        String monthStart = dateStart.format(DateTimeFormatter.ofPattern("MMM")).toString();
        String monthEnd = dateEnd.format(DateTimeFormatter.ofPattern("MMM")).toString();

        if(monthStart.equals(monthEnd)){
            holder.raceMonth.setText(monthStart);
        }
        else{
            String month = monthStart + "-" + monthEnd;
            holder.raceMonth.setText(month);
        }

        holder.raceName.setText(datum.getRaceName() + " " + currentYear);
        holder.circuitName.setText(datum.getCircuitName());

        String locale = datum.getLocality() + ", " + datum.getCountry();
        holder.raceCountry.setText(locale);
        holder.day_start.setText(dayStart);
        holder.day_end.setText(dayEnd);


        String firstPlace_code = datum.getWinnerDriverCode();
        String secondPlace_code = datum.getSecondPlaceCode();
        String thirdPlace_code = datum.getThirdPlaceCode();
        holder.firstPlace_code.setText(firstPlace_code);
        holder.secondPlace_code.setText(secondPlace_code);
        holder.thirdPlace_code.setText(thirdPlace_code);

        if (holder.getItemViewType() == 1){
            int firstPlace_imageId = context.getResources().getIdentifier(firstPlace_code.toLowerCase(), "drawable",
                    context.getPackageName());

            Glide.with(context)
                    .load(firstPlace_imageId)
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .error(R.drawable.f1)
                    .into(holder.firstPlace_image);

            int secondPlace_imageId = context.getResources().getIdentifier(secondPlace_code.toLowerCase(), "drawable",
                    context.getPackageName());

            Glide.with(context)
                    .load(secondPlace_imageId)
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .error(R.drawable.f1)
                    .into(holder.secondPlace_image);

            int thirdPlace_imageId = context.getResources().getIdentifier(thirdPlace_code.toLowerCase(), "drawable",
                    context.getPackageName());

            Glide.with(context)
                    .load(thirdPlace_imageId)
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .error(R.drawable.f1)
                    .into(holder.thirdPlace_image);
        }

        holder.constraintLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
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
                bundle.putString("firstPlaceCode", firstPlace_code);
                bundle.putString("secondPlaceCode", secondPlace_code);
                bundle.putString("thirdPlaceCode", thirdPlace_code);
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

        TextView round, day_start, day_end, raceMonth, raceCountry, raceName, circuitName,
                secondPlace_code, firstPlace_code, thirdPlace_code;
        ImageView countryImage, secondPlace_image, firstPlace_image, thirdPlace_image;
        ConstraintLayout constraintLayout;
        public DataHolder(@NonNull View itemView, int viewType) {
            super(itemView);
            if (viewType == 1){
                round = itemView.findViewById(R.id.round);
                day_start = itemView.findViewById(R.id.day_start);
                day_end = itemView.findViewById(R.id.day_end);
                raceMonth = itemView.findViewById(R.id.raceMonth);
                raceCountry = itemView.findViewById(R.id.raceCountry);
                raceName = itemView.findViewById(R.id.raceName);
                circuitName = itemView.findViewById(R.id.circuitName);
                secondPlace_code = itemView.findViewById(R.id.secondPlace_code);
                firstPlace_code = itemView.findViewById(R.id.firstPlace_code);
                thirdPlace_code = itemView.findViewById(R.id.thirdPlace_code);
                constraintLayout = itemView.findViewById(R.id.main_layout);
                secondPlace_image = itemView.findViewById(R.id.secondPlace_image);
                firstPlace_image = itemView.findViewById(R.id.firstPlace_image);
                thirdPlace_image = itemView.findViewById(R.id.thirdPlace_image);
            } else{
                round = itemView.findViewById(R.id.round);
                day_start = itemView.findViewById(R.id.day_start);
                day_end = itemView.findViewById(R.id.day_end);
                raceMonth = itemView.findViewById(R.id.raceMonth);
                raceCountry = itemView.findViewById(R.id.raceCountry);
                raceName = itemView.findViewById(R.id.raceName);
                circuitName = itemView.findViewById(R.id.circuitName);
                secondPlace_code = itemView.findViewById(R.id.secondPlace_code);
                firstPlace_code = itemView.findViewById(R.id.firstPlace_code);
                thirdPlace_code = itemView.findViewById(R.id.thirdPlace_code);
                //countryImage = itemView.findViewById(R.id.countryImage);
                constraintLayout = itemView.findViewById(R.id.main_layout);
            }
        }
    }

}
