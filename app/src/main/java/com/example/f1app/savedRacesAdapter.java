package com.example.f1app;

import static com.example.f1app.MainActivity.checkLightTheme;
import static com.example.f1app.MainActivity.getStringByName;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

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
        return new DataHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull savedRacesAdapter.DataHolder holder, int position) {
        savedRacesData datum = dataList.get(position);

        String localeRaceName = datum.getRaceName().toLowerCase().replaceAll("\\s+", "_");
        String fullRaceName = context.getString(getStringByName(localeRaceName + "_text")) + " " + datum.getRaceSeason();

        holder.raceName.setText(fullRaceName);
        String saveDate = context.getString(R.string.saved_race_text) + " " +  datum.getSaveDate();
        holder.save_date.setText(saveDate);

        String mRaceName = datum.getRaceName();
        String mSeason = datum.getRaceSeason();

        if (!checkLightTheme(context)){
            holder.stripedImgFirst.setBackground(ContextCompat.getDrawable(context, R.drawable.background_striped_lines_item_night));
            holder.stripedImgSecond.setBackground(ContextCompat.getDrawable(context, R.drawable.background_striped_lines_item_night));
            holder.stripedImgThird.setBackground(ContextCompat.getDrawable(context, R.drawable.background_striped_lines_item_night));
        }

        DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference();
        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference storageRef = storage.getReference();
        rootRef.child("schedule/season").child(mSeason).child(mRaceName)
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
                        //String mRaceDate = snapshot.child("raceDate").getValue(String.class);
                        Integer mRound = snapshot.child("round").getValue(Integer.class);
                        String dateStart = snapshot.child("FirstPractice/firstPracticeDate").getValue(String.class);
                        String dateEnd = snapshot.child("raceDate").getValue(String.class);
                        Boolean hasCanceled = snapshot.child("Canceled").getValue(Boolean.class);

                        if ("n/a".equals(mFirstPlaceCode.toLowerCase())
                                || "n/a".equals(mSecondPlaceCode.toLowerCase())
                                || "n/a".equals(mThirdPlaceCode.toLowerCase())){
                            holder.podiumNamesLayout.setVisibility(View.GONE);
                            holder.podiumImageLayout.setVisibility(View.GONE);
                        }

                        boolean isCanceled;
                        isCanceled = hasCanceled != null;

                        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-M-d");

                        LocalDate startDateOngoing = LocalDate.parse(dateStart, dateFormatter);
                        String dayStart = startDateOngoing.format(DateTimeFormatter.ofPattern("dd"));

                        LocalDate endDateOngoing = LocalDate.parse(dateEnd, dateFormatter);
                        String dayEnd = endDateOngoing.format(DateTimeFormatter.ofPattern("dd"));

                        String monthStart = startDateOngoing.format(DateTimeFormatter.ofPattern("MMM"));
                        String monthEnd = endDateOngoing.format(DateTimeFormatter.ofPattern("MMM"));

                        holder.firstPlace_code.setText(mFirstPlaceCode);
                        holder.secondPlace_code.setText(mSecondPlaceCode);
                        holder.thirdPlace_code.setText(mThirdPlaceCode);

                        if (isCanceled){
                            holder.podiumNamesLayout.setVisibility(View.GONE);
                            holder.podiumImageLayout.setVisibility(View.GONE);
                            holder.cnd.setVisibility(View.VISIBLE);
                        }else{
                            holder.podiumNamesLayout.setVisibility(View.VISIBLE);
                            holder.podiumImageLayout.setVisibility(View.VISIBLE);
                            holder.cnd.setVisibility(View.GONE);

                            StorageReference mWinnerImage = storageRef.child("drivers/" + mFirstPlaceCode.toLowerCase() + "_" + mSeason + ".png");
                            StorageReference mSecondImage = storageRef.child("drivers/" + mSecondPlaceCode.toLowerCase()  + "_" + mSeason + ".png");
                            StorageReference mThirdImage = storageRef.child("drivers/" + mThirdPlaceCode.toLowerCase() + "_" + mSeason + ".png");

                            GlideApp.with(context)
                                    .load(mWinnerImage)
                                    .transition(DrawableTransitionOptions.withCrossFade())
                                    .error(R.drawable.placeholder_driver)
                                    .into(holder.firstPlace_image);

                            GlideApp.with(context)
                                    .load(mSecondImage)
                                    .transition(DrawableTransitionOptions.withCrossFade())
                                    .error(R.drawable.placeholder_driver)
                                    .into(holder.secondPlace_image);

                            GlideApp.with(context)
                                    .load(mThirdImage)
                                    .transition(DrawableTransitionOptions.withCrossFade())
                                    .error(R.drawable.placeholder_driver)
                                    .into(holder.thirdPlace_image);
                        }

                        Bundle bundle = new Bundle();
                        rootRef.child("circuits").child(mCircuitId)
                                .addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                                        String mCountry = snapshot.child("country").getValue(String.class);
                                        String mCircuitName = snapshot.child("circuitName").getValue(String.class);

                                        if(mFirstPlaceCode.equals("N/C")){
                                            holder.constraintLayout.setOnClickListener(view ->{
                                                Intent intent = new Intent(context , futureRaceActivity.class);
                                                bundle.putString("raceName" , mRaceName);
                                                bundle.putString("futureRaceStartDay" , dayStart);
                                                bundle.putString("futureRaceEndDay" , dayEnd);
                                                bundle.putString("futureRaceStartMonth" , monthStart);
                                                bundle.putString("futureRaceEndMonth" , monthEnd);
                                                bundle.putString("roundCount" , String.valueOf(mRound));
                                                bundle.putString("raceCountry" , mCountry);
                                                bundle.putString("circuitId", mCircuitId);
                                                bundle.putString("dateStart", dateStart);
                                                bundle.putString("circuitName", mCircuitName);
                                                bundle.putBoolean("isCanceled", isCanceled);
                                                bundle.putString("dateEnd", dateEnd);
                                                intent.putExtras(bundle);
                                                context.startActivity(intent);
                                            });
                                        }else{
                                            holder.constraintLayout.setOnClickListener(view ->{
                                                Intent intent = new Intent(context , concludedRaceActivity.class);
                                                Bundle bundle = new Bundle();
                                                bundle.putString("raceName" , mRaceName);
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
                                                bundle.putBoolean("isCanceled", isCanceled);
                                                bundle.putString("dateEnd", dateEnd);
                                                intent.putExtras(bundle);
                                                context.startActivity(intent);
                                            });
                                        }
                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError error) {
                                        Toast.makeText(context, error.getMessage(), Toast.LENGTH_SHORT).show();
                                    }
                                });
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(context, error.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    public int getItemCount() {
        return dataList.size();
    }

    public static class DataHolder extends RecyclerView.ViewHolder{
        TextView raceName, save_date, secondPlace_code, firstPlace_code, thirdPlace_code, cnd;
        ImageView secondPlace_image, firstPlace_image, thirdPlace_image;
        ConstraintLayout constraintLayout;
        ShapeableImageView stripedImgFirst, stripedImgSecond, stripedImgThird;
        LinearLayout podiumImageLayout, podiumNamesLayout;
        public DataHolder(@NonNull View itemView) {
            super(itemView);
            cnd = itemView.findViewById(R.id.cnd_layout);
            podiumImageLayout = itemView.findViewById(R.id.podiumImage_layout);
            podiumNamesLayout = itemView.findViewById(R.id.podiumNamesLayout);
            stripedImgFirst = itemView.findViewById(R.id.stripedImg_first);
            stripedImgSecond = itemView.findViewById(R.id.stripedImg_second);
            stripedImgThird = itemView.findViewById(R.id.stripedImg_third);
            raceName = itemView.findViewById(R.id.raceName);
            save_date = itemView.findViewById(R.id.save_date);
            secondPlace_code = itemView.findViewById(R.id.secondPlace_code);
            firstPlace_code = itemView.findViewById(R.id.firstPlace_code);
            thirdPlace_code = itemView.findViewById(R.id.thirdPlace_code);
            constraintLayout = itemView.findViewById(R.id.main_layout);
            secondPlace_image = itemView.findViewById(R.id.secondPlace_image);
            firstPlace_image = itemView.findViewById(R.id.firstPlace_image);
            thirdPlace_image = itemView.findViewById(R.id.thirdPlace_image);
        }
    }
}
