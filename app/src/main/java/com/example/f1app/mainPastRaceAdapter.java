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
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import android.widget.ImageView;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

public class mainPastRaceAdapter extends RecyclerView.Adapter<mainPastRaceAdapter.DataHolder>{
    Activity context;
    List<concludedRacesData> dataList;

    @Override
    public int getItemViewType(int position) {
        if (position == getItemCount() - 1)
            return 1;
        else
            return 2;
    }

    public mainPastRaceAdapter(Activity context , List<concludedRacesData> datum){
        this.context = context;
        dataList = datum;
    }

    @NonNull
    @Override
    public DataHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view;
        view = LayoutInflater.from(context).inflate(R.layout.item_main_concluded_race, parent , false);
        return new DataHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DataHolder holder, int position) {
        LocalDate currentDate = LocalDate.now();
        String currentYear = Integer.toString(currentDate.getYear());

        concludedRacesData datum = dataList.get(position);

        String round = datum.getRoundNumber();
        holder.round.setText(context.getString(R.string.round_pill, round));

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

        holder.dates.setText(mainFutureRaceAdapter.dateRange(dateStart, dateEnd));

        String localeRaceName = datum.getRaceName().toLowerCase().replaceAll("\\s+", "_");
        String pastRaceName = context.getString(getStringByName(localeRaceName + "_text")) + " " + currentYear;
        holder.raceName.setText(pastRaceName);
        DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference();
        rootRef.child("circuits").orderByChild("circuitName").equalTo(datum.getCircuitName()).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for(DataSnapshot circtuitSnap: snapshot.getChildren()){
                    String circuitId = circtuitSnap.child("circuitId").getValue(String.class);
                    holder.circuitName.setText(context.getString(getStringByName(circuitId + "_text")));
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(context, "Fail while getting data from Firebase " + error.getMessage(), Toast.LENGTH_LONG).show();
            }
        });

        String locale;
        if (Locale.getDefault().getLanguage().equals("ru")){
            ArrayList<String> localizedData = localizeLocality(datum.getLocality(), datum.getCountry(), context);
            //String country = localizedData.get(0);
            //String cityName = localizedData.get(1);
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

        String firstPlace_code = datum.getWinnerDriverCode();
        String secondPlace_code = datum.getSecondPlaceCode();
        String thirdPlace_code = datum.getThirdPlaceCode();

        String season = datum.getSeason() != null && !datum.getSeason().isEmpty()
                ? datum.getSeason() : currentYear;
        holder.podium.bind(datum, season);

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
            bundle.putString("firstPlaceCode", firstPlace_code);
            bundle.putString("secondPlaceCode", secondPlace_code);
            bundle.putString("thirdPlaceCode", thirdPlace_code);
            bundle.putBoolean("isCanceled", datum.isCanceled());
            bundle.putString("dateEnd", datum.getDateEnd());
            intent.putExtras(bundle);
            context.startActivity(intent);
        });

    }

    @Override
    public int getItemCount() {
        return dataList.size();
    }

    public static class DataHolder extends RecyclerView.ViewHolder{

        TextView round, raceCountry, raceName, circuitName, cnd, dates;
        ConstraintLayout constraintLayout;
        LinearLayout podiumNamesLayout;
        mainPodiumBlock podium;
        public DataHolder(@NonNull View itemView) {
            super(itemView);
            cnd = itemView.findViewById(R.id.cnd_layout);
            podiumNamesLayout = itemView.findViewById(R.id.podiumNamesLayout);
            round = itemView.findViewById(R.id.round);
            dates = itemView.findViewById(R.id.raceDates);
            podium = new mainPodiumBlock(itemView);
            raceCountry = itemView.findViewById(R.id.raceCountry);
            raceName = itemView.findViewById(R.id.raceName);
            circuitName = itemView.findViewById(R.id.circuitName);
            constraintLayout = itemView.findViewById(R.id.main_layout);
        }
    }

}
