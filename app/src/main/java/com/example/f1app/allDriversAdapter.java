package com.example.f1app;

import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.view.ViewCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.storage.StorageReference;

import java.util.List;

public class allDriversAdapter extends ListAdapter<driversList, allDriversAdapter.DataHolder> {

    private final Context context;

    public allDriversAdapter(Context context) {
        super(DIFF);
        this.context = context;
    }

    private static final DiffUtil.ItemCallback<driversList> DIFF =
            new DiffUtil.ItemCallback<driversList>() {
                @Override
                public boolean areItemsTheSame(@NonNull driversList a, @NonNull driversList b) {
                    return a.getDriverCode().equals(b.getDriverCode());
                }
                @Override
                public boolean areContentsTheSame(@NonNull driversList a, @NonNull driversList b) {
                    return a.getDriverName().equals(b.getDriverName())
                            && a.getDriverFamilyName().equals(b.getDriverFamilyName())
                            && a.getDriverTeam().equals(b.getDriverTeam());
                }
            };

    @NonNull
    @Override
    public DataHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_all_drivers, parent, false);
        return new DataHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DataHolder holder, int position) {
        driversList datum = getItem(position);

        String name = datum.getDriverName();
        if (name.equals("Andrea Kimi")) {
            name = name.split(" ")[1];
        }
        holder.name.setText(name);
        holder.familyName.setText(datum.getDriverFamilyName());

        holder.image.setImageResource(R.drawable.placeholder_driver);

        StorageReference imageRef = datum.getDriverImageRef();

        if (imageRef == null) {
            holder.image.setImageResource(R.drawable.placeholder_driver);
        } else {
            GlideApp.with(holder.image)
                    .load(imageRef)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .placeholder(R.drawable.placeholder_driver)
                    .error(R.drawable.placeholder_driver)
                    .into(holder.image);
        }

        holder.card.setOnClickListener(v -> {
            Intent intent = new Intent(v.getContext(), driverPageActivity.class);
            Bundle b = new Bundle();
            b.putString("driverName", datum.getDriverName());
            b.putString("driverCode", datum.getDriverCode());
            b.putString("driverTeam", datum.getDriverTeam());
            b.putString("driverFamilyName", datum.getDriverFamilyName());
            b.putString("driverTeamId", datum.getConstructorId());
            b.putString("currentSeason", datum.getSeason());
            intent.putExtras(b);
            v.getContext().startActivity(intent);
        });
    }

    static class DataHolder extends RecyclerView.ViewHolder {
        final TextView name, familyName;
        final ImageView image;
        final CardView card;

        DataHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.driver_name);
            familyName = itemView.findViewById(R.id.driver_family_name);
            image = itemView.findViewById(R.id.driver_image);
            card = itemView.findViewById(R.id.card);
        }
    }
}
