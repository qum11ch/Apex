package com.example.f1app;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.List;

public class favoritePagerAdapter extends RecyclerView.Adapter<favoritePagerAdapter.Holder> {

    public interface OnSelectListener {
        void onSelect(favoriteItem item);
    }

    private final List<favoriteItem> items;
    private final OnSelectListener listener;

    public favoritePagerAdapter(List<favoriteItem> items, OnSelectListener listener) {
        this.items = items;
        this.listener = listener;
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_favorite_picker, parent, false);

        return new Holder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        favoriteItem item = items.get(position);

        holder.driverName.setVisibility(View.GONE);
        holder.familyName.setVisibility(View.GONE);
        holder.teamName.setVisibility(View.GONE);

        holder.subtitle.setText(item.subtitle);

        if (item.empty) {
            holder.image.setVisibility(View.GONE);
            holder.imageFrame.setBackgroundTintList(ColorStateList.valueOf(Color.TRANSPARENT));
            holder.plus.setVisibility(View.VISIBLE);
        } else {
            Context context = holder.itemView.getContext();
            int color = ContextCompat.getColor(context, R.color.colorPrimary);

            holder.imageFrame.setBackgroundTintList(ColorStateList.valueOf(color));

            holder.image.setVisibility(View.VISIBLE);
            holder.plus.setVisibility(View.GONE);

            StorageReference imageRef =
                    FirebaseStorage.getInstance()
                            .getReference()
                            .child(item.imagePath);

            GlideApp.with(holder.itemView.getContext())
                    .load(imageRef)
                    .placeholder(R.drawable.placeholder_driver)
                    .error(R.drawable.placeholder_driver)
                    .into(holder.image);
        }

        if (item.team) {
            holder.imageFrame.setBackgroundTintList(ColorStateList.valueOf(Color.TRANSPARENT));
            holder.teamName.setVisibility(View.VISIBLE);
            holder.teamName.setText(item.teamName);
        } else {
            Context context = holder.itemView.getContext();
            int color = ContextCompat.getColor(context, R.color.colorPrimary);

            holder.imageFrame.setBackgroundTintList(ColorStateList.valueOf(color));

            holder.driverName.setVisibility(View.VISIBLE);
            holder.familyName.setVisibility(View.VISIBLE);

            holder.driverName.setText(item.driverName);
            holder.familyName.setText(item.familyName);
        }

        holder.itemView.setOnClickListener(v -> listener.onSelect(item));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class Holder extends RecyclerView.ViewHolder {
        ImageView image;
        TextView plus;
        TextView driverName;
        TextView familyName;
        TextView teamName;
        TextView subtitle;
        FrameLayout imageFrame;

        Holder(@NonNull View itemView) {
            super(itemView);

            imageFrame = itemView.findViewById(R.id.image_frame);
            image = itemView.findViewById(R.id.itemImage);
            plus = itemView.findViewById(R.id.itemPlus);
            driverName = itemView.findViewById(R.id.itemDriverName);
            familyName = itemView.findViewById(R.id.itemFamilyName);
            teamName = itemView.findViewById(R.id.itemTeamName);
            subtitle = itemView.findViewById(R.id.itemSubtitle);
        }
    }
}