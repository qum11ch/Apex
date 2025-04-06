package com.example.f1app;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.List;

public class pastSeasonDriversStandingsAdapter extends RecyclerView.Adapter<pastSeasonDriversStandingsAdapter.DataHolder> {
    Context context;
    List<driversList> dataList;


    @Override
    public int getItemViewType(int position) {
        if (position == 0) {return 1;
        }else if (position == 1) {return 2;
        }else if (position == 2){return 3;
        }else{return 4;}
    }


    public pastSeasonDriversStandingsAdapter(Context context, List<driversList> datum) {
        this.context = context;
        dataList = datum;
    }

    @NonNull
    @Override
    public pastSeasonDriversStandingsAdapter.DataHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        View view;
        switch (viewType) {
            case 1:
                view = LayoutInflater.from(context).inflate(R.layout.item_driver_winner, parent, false);
                break;
            case 2:
                view = LayoutInflater.from(context).inflate(R.layout.item_driver_second, parent, false);
                break;
            case 3:
                view = LayoutInflater.from(context).inflate(R.layout.item_driver_third, parent, false);
                break;
            default:
                view = LayoutInflater.from(context).inflate(R.layout.item_driver, parent, false);
                break;
        }
        return new DataHolder(view, viewType);
    }

    @Override
    public void onBindViewHolder(@NonNull pastSeasonDriversStandingsAdapter.DataHolder holder, int position) {
        driversList datum = dataList.get(position);
        holder.driverName.setText(datum.getDriverName());
        holder.driverTeam.setText(datum.getDriverTeam());
        holder.driverFamilyName.setText(datum.getDriverFamilyName());

        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference storageRef = storage.getReference();
        String season = datum.getSeason();

        StorageReference mDriverImage;
        if (season.equals("2024")){
            mDriverImage = storageRef.child("drivers/" + datum.getDriverCode().toLowerCase() + "_2024.png");
        }else{
            mDriverImage = storageRef.child("drivers/" + datum.getDriverCode().toLowerCase() + ".png");
        }

        StorageReference mDriverTeamLogo = storageRef.child("teams/" + datum.getConstructorId().toLowerCase() + "_logo.png");
        mDriverTeamLogo.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
            @Override
            public void onSuccess(Uri uri) {
                GlideApp.with(context)
                        .load(uri)
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .skipMemoryCache(true)
                        .error(R.drawable.f1)
                        .into(holder.driverTeam_logo);
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                GlideApp.with(context)
                        .load(R.drawable.f1)
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .skipMemoryCache(true)
                        .into(holder.driverTeam_logo);
            }
        });

        mDriverImage.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
            @Override
            public void onSuccess(Uri uri) {
                GlideApp.with(context)
                        .load(uri)
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .skipMemoryCache(true)
                        .error(R.drawable.f1)
                        .into(holder.driverImage);
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                GlideApp.with(context)
                        .load(R.drawable.f1)
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .skipMemoryCache(true)
                        .into(holder.driverImage);
            }
        });

        holder.driver_placement.setText(datum.getDriverPlacement());
        String driver_points = datum.getDriverPoints()  + " " + context.getString(R.string.pts_header);
        holder.driver_points.setText(driver_points);
        RelativeLayout.LayoutParams params= new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,ViewGroup.LayoutParams.WRAP_CONTENT);
        if (datum.getDriverName().equals("Andrea Kimi")){
            params.addRule(RelativeLayout.BELOW, R.id.driverName);
        }else {
            params.addRule(RelativeLayout.END_OF, R.id.driverName);
            Resources r = context.getResources();
            int px = (int) TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP,
                    5,
                    r.getDisplayMetrics()
            );
            params.setMargins(px, 0, 0, 0);
        }
        holder.driverFamilyName.setLayoutParams(params);


        holder.constraintLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(context , driverPageActivity.class);
                Bundle bundle = new Bundle();
                bundle.putString("driverName", datum.getDriverName());
                bundle.putString("driverCode", datum.getDriverCode());
                bundle.putString("driverTeam", datum.getDriverTeam());
                bundle.putString("driverFamilyName", datum.getDriverFamilyName());
                bundle.putString("driverTeamId", datum.getConstructorId());
                intent.putExtras(bundle);
                context.startActivity(intent);
            }
        });
    }


    @Override
    public int getItemCount() {
        return dataList.size();
    }

    public static class DataHolder extends RecyclerView.ViewHolder {
        TextView driverName, driverTeam, driver_placement, driver_points,
                driverFamilyName;
        ImageView driverTeam_logo, driverImage, driverNumber;
        ConstraintLayout constraintLayout;
        RelativeLayout leftLayout, driver_layout;
        View line;
        CardView cardView;

        public DataHolder(@NonNull View itemView, int viewType) {
            super(itemView);
            cardView = itemView.findViewById(R.id.cardView);
            leftLayout = itemView.findViewById(R.id.leftLayout);
            driver_layout = itemView.findViewById(R.id.driver_layout);
            driverName = itemView.findViewById(R.id.driverName);
            driverFamilyName = itemView.findViewById(R.id.driverFamilyName);
            driverTeam = itemView.findViewById(R.id.driverTeam);
            driver_placement = itemView.findViewById(R.id.driver_placement);
            driver_points = itemView.findViewById(R.id.driver_points);
            constraintLayout = itemView.findViewById(R.id.main_layout);
            driverTeam_logo = itemView.findViewById(R.id.driverTeam_logo);
            driverImage = itemView.findViewById(R.id.driverImage);
            line = itemView.findViewById(R.id.line);
        }
    }
}
