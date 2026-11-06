package com.example.f1app;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.storage.FirebaseStorage;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class leaderboardAdapter extends RecyclerView.Adapter<leaderboardAdapter.Holder> {
    private static final Map<String, String> AVATAR_URLS = new ConcurrentHashMap<>();
    private static final String NO_AVATAR = "";

    private final List<leaderboardEntry> items = new ArrayList<>();

    public void submit(List<leaderboardEntry> list) {
        items.clear();
        items.addAll(list);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_leaderboard_row, parent, false);
        return new Holder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder h, int position) {
        leaderboardEntry e = items.get(position);
        String name = e.username != null && !e.username.isEmpty() ? e.username : "—";

        h.rank.setText(String.valueOf(e.rank));
        h.name.setText(name);
        h.points.setText(String.valueOf(e.points));
        h.itemView.setAlpha(e.isMe ? 1f : 0.92f);
        h.itemView.setSelected(e.isMe);

        h.initial.setText(name.substring(0, 1).toUpperCase());
        h.initial.setVisibility(View.VISIBLE);
        h.avatar.setVisibility(View.GONE);
        h.boundUid = e.uid;

        String cached = AVATAR_URLS.get(e.uid);
        if (cached != null) {
            showAvatar(h, e.uid, cached);
            return;
        }

        FirebaseStorage.getInstance().getReference()
                .child("profileImages").child(e.uid).child("avatar.jpg")
                .getDownloadUrl()
                .addOnSuccessListener(uri -> {
                    AVATAR_URLS.put(e.uid, uri.toString());
                    showAvatar(h, e.uid, uri.toString());
                })
                .addOnFailureListener(error -> AVATAR_URLS.put(e.uid, NO_AVATAR));
    }

    private void showAvatar(Holder h, String uid, String url) {
        if (NO_AVATAR.equals(url) || !uid.equals(h.boundUid)) return;
        h.avatar.setVisibility(View.VISIBLE);
        h.initial.setVisibility(View.GONE);
        GlideApp.with(h.avatar).load(url).circleCrop().into(h.avatar);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class Holder extends RecyclerView.ViewHolder {
        final TextView rank, name, points, initial;
        final ImageView avatar;
        String boundUid;

        Holder(@NonNull View v) {
            super(v);
            rank = v.findViewById(R.id.lbRank);
            name = v.findViewById(R.id.lbName);
            points = v.findViewById(R.id.lbPoints);
            initial = v.findViewById(R.id.lbAvatarInitial);
            avatar = v.findViewById(R.id.lbAvatar);
        }
    }
}