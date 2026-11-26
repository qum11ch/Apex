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

public class mainFutureRaceAdapter extends RecyclerView.Adapter<mainFutureRaceAdapter.DataHolder> {
    private final Activity context;
    private final List<futureRaceData> dataList;

    public mainFutureRaceAdapter(Activity context, List<futureRaceData> datum) {
        this.context = context;
        this.dataList = datum;
    }

    /** 27-29 nov / 30 oct - 1 nov */
    static String dateRange(LocalDate start, LocalDate end) {
        DateTimeFormatter month = DateTimeFormatter.ofPattern("MMM");
        String m1 = start.format(month);
        String m2 = end.format(month);
        int d1 = start.getDayOfMonth();
        int d2 = end.getDayOfMonth();

        return m1.equals(m2)
                ? d1 + "\u2013" + d2 + " " + m1
                : d1 + " " + m1 + " \u2013 " + d2 + " " + m2;
    }

    @NonNull
    @Override
    public DataHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_main_future_race, parent, false);
        return new DataHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DataHolder holder, int position) {
        futureRaceData datum = dataList.get(position);
        String year = Integer.toString(LocalDate.now().getYear());

        holder.round.setText(context.getString(R.string.round_pill, datum.getFutureRaceRound()));
        holder.cnd.setVisibility(datum.isCanceled() ? View.VISIBLE : View.GONE);

        DateTimeFormatter parser = DateTimeFormatter.ofPattern("yyyy-M-d");
        LocalDate dateStart = LocalDate.parse(datum.getFutureRaceStartDate(), parser);
        LocalDate dateEnd = LocalDate.parse(datum.getFutureRaceEndDate(), parser);
        String dayStart = dateStart.format(DateTimeFormatter.ofPattern("dd"));
        String dayEnd = dateEnd.format(DateTimeFormatter.ofPattern("dd"));
        String monthStart = dateStart.format(DateTimeFormatter.ofPattern("MMM"));
        String monthEnd = dateEnd.format(DateTimeFormatter.ofPattern("MMM"));

        holder.dates.setText(dateRange(dateStart, dateEnd));

        String raceKey = datum.getFutureRaceName().toLowerCase().replaceAll("\\s+", "_");
        holder.raceName.setText(context.getString(getStringByName(raceKey + "_text")) + " " + year);
        holder.circuitName.setText(context.getString(getStringByName(datum.getCircuitId() + "_text")));

        String locale;

        if (Locale.getDefault().getLanguage().equals("ru")) {
            ArrayList<String> localized = localizeLocality(datum.getLocale(), datum.getFutureRaceCountry(), context);
            locale = localized.get(2);
        } else {
            String city = datum.getLocale();

            if ("Monaco".equals(city) || "Singapore".equals(city)) {
                city = null;
            }

            locale = city != null ? city + ", " + datum.getFutureRaceCountry() : datum.getFutureRaceCountry();
        }

        holder.raceCountry.setText(locale);

        if (datum.isCanceled()) {
            holder.weekend.hide();
        } else {
            holder.weekend.bind(year, datum.getFutureRaceName());
        }

        holder.constraintLayout.setOnClickListener(v -> {
            Intent intent = new Intent(context, futureRaceActivity.class);

            Bundle bundle = new Bundle();
            bundle.putString("raceName", datum.getFutureRaceName());
            bundle.putString("futureRaceStartDay", dayStart);
            bundle.putString("futureRaceEndDay", dayEnd);
            bundle.putString("futureRaceStartMonth", monthStart);
            bundle.putString("futureRaceEndMonth", monthEnd);
            bundle.putString("circuitName", datum.getFutureCircuitName());
            bundle.putString("roundCount", datum.getFutureRaceRound());
            bundle.putString("raceCountry", datum.getFutureRaceCountry());
            bundle.putString("circuitId", datum.getCircuitId());
            bundle.putString("dateEnd", datum.getFutureRaceEndDate());
            bundle.putString("dateStart", datum.getFutureRaceStartDate());
            bundle.putBoolean("isCanceled", datum.isCanceled());
            intent.putExtras(bundle);

            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return dataList.size();
    }

    public static class DataHolder extends RecyclerView.ViewHolder {
        final TextView round, dates, raceCountry, raceName, circuitName, cnd;
        final ConstraintLayout constraintLayout;
        final mainWeekendBlock weekend;

        public DataHolder(@NonNull View itemView) {
            super(itemView);
            round = itemView.findViewById(R.id.round);
            dates = itemView.findViewById(R.id.raceDates);
            cnd = itemView.findViewById(R.id.cnd_layout);
            raceCountry = itemView.findViewById(R.id.raceCountry);
            raceName = itemView.findViewById(R.id.raceName);
            circuitName = itemView.findViewById(R.id.circuitName);
            constraintLayout = itemView.findViewById(R.id.main_layout);
            weekend = new mainWeekendBlock(itemView);
        }
    }
}