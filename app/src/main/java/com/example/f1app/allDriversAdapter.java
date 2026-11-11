package com.example.f1app;

import android.content.Intent;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.firebase.storage.StorageReference;

import java.util.Objects;

import android.content.res.ColorStateList;
import androidx.core.content.ContextCompat;

public class allDriversAdapter extends ListAdapter<driversList, allDriversAdapter.DataHolder> {

    public enum ViewMode {
        TILES,
        LIST
    }

    private static final long CLICK_GUARD_MS = 600L;

    private long lastClickAt = 0L;
    private ViewMode viewMode = ViewMode.TILES;

    public allDriversAdapter() {
        super(DIFF);
    }

    public void setViewMode(@NonNull ViewMode mode) {
        if (mode == viewMode) {
            return;
        }

        viewMode = mode;
        // The view type of every item changes, so all holders must be recreated.
        notifyDataSetChanged();
    }

    private static String key(driversList driver) {
        return driverNameFormatter.fullName(
                driver.getDriverName(),
                driver.getDriverFamilyName()
        );
    }

    // Driver codes are not unique (Michael and Mick Schumacher both use MSC), so identity is the full name.
    private static final DiffUtil.ItemCallback<driversList> DIFF =
            new DiffUtil.ItemCallback<driversList>() {
                @Override
                public boolean areItemsTheSame(@NonNull driversList a, @NonNull driversList b) {
                    return key(a).equals(key(b));
                }

                @Override
                public boolean areContentsTheSame(@NonNull driversList a, @NonNull driversList b) {
                    return Objects.equals(a.getDriverName(), b.getDriverName())
                            && Objects.equals(a.getDriverFamilyName(), b.getDriverFamilyName())
                            && Objects.equals(a.getDriverTeam(), b.getDriverTeam())
                            && Objects.equals(a.getConstructorId(), b.getConstructorId())
                            && Objects.equals(a.getDriverCode(), b.getDriverCode())
                            && Objects.equals(a.getStatus(), b.getStatus())
                            && a.getChampionships() == b.getChampionships()
                            && a.isLegend() == b.isLegend()
                            && Objects.equals(a.getDriverImageRef(), b.getDriverImageRef());
                }
            };

    @Override
    public int getItemViewType(int position) {
        return viewMode.ordinal();
    }

    @NonNull
    @Override
    public DataHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int layout = viewType == ViewMode.LIST.ordinal()
                ? R.layout.item_all_drivers_row
                : R.layout.item_all_drivers;

        View view = LayoutInflater.from(parent.getContext()).inflate(layout, parent, false);
        return new DataHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DataHolder holder, int position) {
        driversList datum = getItem(position);

        holder.name.setText(driverNameFormatter.displayGivenName(datum.getDriverName()));
        holder.familyName.setText(datum.getDriverFamilyName());
        holder.image.setContentDescription(key(datum));

        if (holder.team != null) {
            String team = datum.getDriverTeam();
            boolean hasTeam = team != null && !team.trim().isEmpty();
            holder.team.setText(hasTeam ? team : "");
            holder.team.setVisibility(hasTeam ? View.VISIBLE : View.GONE);
        }

        bindStatus(holder.status, datum.getStatus());

        StorageReference imageRef = datum.getDriverImageRef();

        if (imageRef == null) {
            GlideApp.with(holder.image).clear(holder.image);
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
            long now = SystemClock.elapsedRealtime();

            if (now - lastClickAt < CLICK_GUARD_MS) {
                return;
            }

            lastClickAt = now;

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

    private static void bindStatus(TextView view, String status) {
        if (view == null) {
            return;
        }

        int label;
        int textColor;
        int backgroundColor;

        if ("active".equals(status)) {
            label = R.string.filter_active_text;
            textColor = R.color.driver_status_active;
            backgroundColor = R.color.driver_status_active_bg;
        } else if ("reserve".equals(status)) {
            label = R.string.filter_reserve_text;
            textColor = R.color.driver_status_reserve;
            backgroundColor = R.color.driver_status_reserve_bg;
        } else if ("retired".equals(status)) {
            label = R.string.filter_retired_text;
            textColor = R.color.driver_status_retired;
            backgroundColor = R.color.driver_status_retired_bg;
        } else {
            view.setVisibility(View.GONE);
            return;
        }

        view.setText(label);
        view.setTextColor(ContextCompat.getColor(view.getContext(), textColor));
        view.setBackgroundTintList(ColorStateList.valueOf(
                ContextCompat.getColor(view.getContext(), backgroundColor)
        ));
        view.setVisibility(View.VISIBLE);
    }

    static class DataHolder extends RecyclerView.ViewHolder {
        final TextView name, familyName;
        final TextView team; // only present in the list layout
        final ImageView image;
        final CardView card;
        final TextView status;

        DataHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.driver_name);
            status = itemView.findViewById(R.id.driver_status);
            familyName = itemView.findViewById(R.id.driver_family_name);
            team = itemView.findViewById(R.id.driver_team);
            image = itemView.findViewById(R.id.driver_image);
            card = itemView.findViewById(R.id.card);
        }
    }
}
