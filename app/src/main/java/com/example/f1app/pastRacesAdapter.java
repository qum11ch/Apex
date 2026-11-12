package com.example.f1app;

import static com.example.f1app.MainActivity.checkLightTheme;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Races of a past season as cards (the old look) or compact tiles, with a text filter. */
public class pastRacesAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    static final int MODE_LIST = 0;
    static final int MODE_TILES = 1;

    private final Activity context;
    private final List<pastRaceItem> all = new ArrayList<>();
    private final List<pastRaceItem> shown = new ArrayList<>();
    private int mode = MODE_LIST;
    private String query = "";

    pastRacesAdapter(Activity context) {
        this.context = context;
    }

    // ---------- data ----------

    void setItems(List<pastRaceItem> items) {
        all.clear();
        all.addAll(items);
        applyFilter();
        notifyDataSetChanged();
    }

    void setMode(int newMode) {
        if (mode != newMode) {
            mode = newMode;
            notifyDataSetChanged();
        }
    }

    int getMode() {
        return mode;
    }

    void setQuery(String newQuery) {
        String normalized = pastRaceItem.plain(newQuery);

        if (!normalized.equals(query)) {
            query = normalized;
            applyFilter();
            notifyDataSetChanged();
        }
    }

    int totalCount() {
        return all.size();
    }

    private void applyFilter() {
        shown.clear();

        if (query.isEmpty()) {
            shown.addAll(all);
            return;
        }

        String[] words = query.split("\\s+");

        for (pastRaceItem item : all) {
            boolean matches = true;

            for (String word : words) {
                if (!item.searchText.contains(word)) {
                    matches = false;
                    break;
                }
            }

            if (matches) {
                shown.add(item);
            }
        }
    }

    // ---------- adapter ----------

    @Override
    public int getItemViewType(int position) {
        return mode;
    }

    @Override
    public int getItemCount() {
        return shown.size();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(context);

        if (viewType == MODE_TILES) {
            return new TileHolder(inflater.inflate(R.layout.item_past_race_tile, parent, false));
        }

        return new ListHolder(inflater.inflate(R.layout.item_past_seasons_race, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        pastRaceItem item = shown.get(position);

        if (holder instanceof TileHolder) {
            bindTile((TileHolder) holder, item);
        } else {
            bindList((ListHolder) holder, item);
        }
    }

    private void bindList(ListHolder holder, pastRaceItem item) {
        concludedRacesData race = item.race;

        // The 10dp gap on the right that the old layout got from its parent.
        holder.itemView.setPaddingRelative(0, 0, Math.round(10 * context.getResources().getDisplayMetrics().density), 0);

        if (!checkLightTheme(context)) {
            holder.stripedImgFirst.setBackground(ContextCompat.getDrawable(context, R.drawable.background_striped_lines_item_night));
            holder.stripedImgSecond.setBackground(ContextCompat.getDrawable(context, R.drawable.background_striped_lines_item_night));
            holder.stripedImgThird.setBackground(ContextCompat.getDrawable(context, R.drawable.background_striped_lines_item_night));
        }

        holder.round.setText(race.getRoundNumber());
        holder.raceMonth.setText(monthText(item));
        holder.day_start.setText(item.dayStart);
        holder.day_end.setText(item.dayEnd);
        holder.raceName.setText(item.title);
        holder.circuitName.setText(item.circuitTitle);
        holder.raceCountry.setText(item.place);

        holder.firstPlace_code.setText(race.getWinnerDriverCode());
        holder.secondPlace_code.setText(race.getSecondPlaceCode());
        holder.thirdPlace_code.setText(race.getThirdPlaceCode());

        if (race.isCanceled()) {
            holder.podiumNamesLayout.setVisibility(View.GONE);
            holder.podiumImageLayout.setVisibility(View.GONE);
            holder.cnd.setVisibility(View.VISIBLE);
        } else {
            holder.podiumNamesLayout.setVisibility(View.VISIBLE);
            holder.podiumImageLayout.setVisibility(View.VISIBLE);
            holder.cnd.setVisibility(View.GONE);

            StorageReference root = FirebaseStorage.getInstance().getReference();
            loadDriver(root, race.getWinnerDriverCode(), race.getSeason(), holder.firstPlace_image);
            loadDriver(root, race.getSecondPlaceCode(), race.getSeason(), holder.secondPlace_image);
            loadDriver(root, race.getThirdPlaceCode(), race.getSeason(), holder.thirdPlace_image);
        }

        holder.constraintLayout.setOnClickListener(v -> open(item));
    }

    private void loadDriver(StorageReference root, String code, String season, ImageView target) {
        if (code == null || code.isEmpty()) {
            target.setImageResource(R.drawable.placeholder_driver);
            return;
        }

        StorageReference image = root.child("drivers/" + code.toLowerCase(Locale.ROOT) + "_" + season + ".png");

        GlideApp.with(context)
                .load(image)
                .transition(DrawableTransitionOptions.withCrossFade())
                .error(R.drawable.placeholder_driver)
                .into(target);
    }

    private void bindTile(TileHolder holder, pastRaceItem item) {
        concludedRacesData race = item.race;

        holder.round.setText(context.getString(R.string.round) + " " + race.getRoundNumber());
        holder.dates.setText(item.dayStart.isEmpty() ? "" : item.dayStart + "-" + item.dayEnd + " " + monthText(item));
        holder.name.setText(withoutSeason(item));
        holder.place.setText(item.place);

        if (race.isCanceled()) {
            holder.podium.setVisibility(View.INVISIBLE);
            holder.canceled.setVisibility(View.VISIBLE);
        } else {
            holder.podium.setVisibility(View.VISIBLE);
            holder.canceled.setVisibility(View.GONE);
            holder.first.setText(race.getWinnerDriverCode());
            holder.second.setText(race.getSecondPlaceCode());
            holder.third.setText(race.getThirdPlaceCode());
        }

        holder.root.setOnClickListener(v -> open(item));
    }

    /** The year is already on the season buttons, so the tile title drops it to fit on two lines. */
    private static String withoutSeason(pastRaceItem item) {
        String suffix = " " + item.race.getSeason();
        return item.title.endsWith(suffix)
                ? item.title.substring(0, item.title.length() - suffix.length())
                : item.title;
    }

    private static String monthText(pastRaceItem item) {
        return item.monthStart.equals(item.monthEnd)
                ? item.monthStart
                : item.monthStart + "-" + item.monthEnd;
    }

    private void open(pastRaceItem item) {
        concludedRacesData race = item.race;

        Intent intent = new Intent(context, concludedRaceActivity.class);
        Bundle bundle = new Bundle();
        bundle.putString("raceName", race.getRaceName());
        bundle.putString("raceStartDay", item.dayStart);
        bundle.putString("raceEndDay", item.dayEnd);
        bundle.putString("raceStartMonth", item.monthStart);
        bundle.putString("raceEndMonth", item.monthEnd);
        bundle.putString("circuitName", race.getCircuitName());
        bundle.putString("roundCount", race.getRoundNumber());
        bundle.putString("raceCountry", race.getCountry());
        bundle.putString("dateStart", race.getDateStart());
        bundle.putString("firstPlaceCode", race.getWinnerDriverCode());
        bundle.putString("secondPlaceCode", race.getSecondPlaceCode());
        bundle.putString("thirdPlaceCode", race.getThirdPlaceCode());
        bundle.putBoolean("isCanceled", race.isCanceled());
        bundle.putString("dateEnd", race.getDateEnd());
        bundle.putString("season", race.getSeason());
        intent.putExtras(bundle);
        context.startActivity(intent);
    }

    // ---------- holders ----------

    static class ListHolder extends RecyclerView.ViewHolder {
        final TextView round, day_start, day_end, raceMonth, raceCountry, raceName, circuitName,
                secondPlace_code, firstPlace_code, thirdPlace_code, cnd;
        final ImageView secondPlace_image, firstPlace_image, thirdPlace_image;
        final ConstraintLayout constraintLayout;
        final ShapeableImageView stripedImgFirst, stripedImgSecond, stripedImgThird;
        final LinearLayout podiumImageLayout, podiumNamesLayout;

        ListHolder(@NonNull View itemView) {
            super(itemView);
            cnd = itemView.findViewById(R.id.cnd_layout);
            podiumImageLayout = itemView.findViewById(R.id.podiumImage_layout);
            podiumNamesLayout = itemView.findViewById(R.id.podiumNamesLayout);
            stripedImgFirst = itemView.findViewById(R.id.stripedImg_first);
            stripedImgSecond = itemView.findViewById(R.id.stripedImg_second);
            stripedImgThird = itemView.findViewById(R.id.stripedImg_third);
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
        }
    }

    static class TileHolder extends RecyclerView.ViewHolder {
        final View root;
        final View podium;
        final TextView round, dates, name, place, first, second, third, canceled;

        TileHolder(@NonNull View itemView) {
            super(itemView);
            root = itemView.findViewById(R.id.tile_card);
            podium = itemView.findViewById(R.id.tile_podium);
            round = itemView.findViewById(R.id.tile_round);
            dates = itemView.findViewById(R.id.tile_dates);
            name = itemView.findViewById(R.id.tile_name);
            place = itemView.findViewById(R.id.tile_place);
            first = itemView.findViewById(R.id.tile_code_first);
            second = itemView.findViewById(R.id.tile_code_second);
            third = itemView.findViewById(R.id.tile_code_third);
            canceled = itemView.findViewById(R.id.tile_canceled);
        }
    }
}
