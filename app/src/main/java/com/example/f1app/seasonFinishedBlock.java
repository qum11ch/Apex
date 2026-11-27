package com.example.f1app;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.ColorUtils;

import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.List;

/**
 * "Season is over": when every race of the season is concluded or canceled, one card "Season results"
 * replaces the "upcoming race" block: the wide button of the prediction results, the podium of the drivers
 * (photos on the colour of the team) and the three teams with their cars. The champions have a gold frame
 * and a badge. The numbers come from the lists the home screen already holds.
 */
final class seasonFinishedBlock {

    interface Actions {
        void openDrivers();

        void openTeams();

        void openLeaderboard();

        void onFinishedChanged(boolean finished);
    }

    /** Only honoured in a debuggable build: long press on the title of the home screen. */
    static boolean debugForce = false;

    private final Context context;
    private final Actions actions;
    private final View card, futureHeader, futureLayout, predictButton;
    private final View[] hidden;
    private final TextView caption;
    private final View driversLabel, teamsLabel;
    private final LinearLayout driversRow, teamsList, teamsContainer;
    private final int[] metals;

    private List<driversList> driverSource;
    private List<teamsList> teamSource;
    private String season = "";
    private boolean futureHidden;
    private int token = 0;

    seasonFinishedBlock(View card, View futureHeader, View futureLayout, View predictButton,
                        Actions actions, View... hidden) {
        this.context = card.getContext();
        this.actions = actions;
        this.card = card;
        this.futureHeader = futureHeader;
        this.futureLayout = futureLayout;
        this.predictButton = predictButton;
        this.hidden = hidden;

        metals = new int[]{
                ContextCompat.getColor(context, R.color.podium_gold),
                ContextCompat.getColor(context, R.color.podium_silver),
                ContextCompat.getColor(context, R.color.podium_bronze)};

        caption = card.findViewById(R.id.finishedCaption);
        driversLabel = card.findViewById(R.id.finishedDriversLabel);
        teamsLabel = card.findViewById(R.id.finishedTeamsLabel);
        driversRow = card.findViewById(R.id.finishedDriversRow);
        teamsList = card.findViewById(R.id.finishedTeamsList);
        teamsContainer = card.findViewById(R.id.finishedTeamsList);

        card.findViewById(R.id.finishedLeaderboardButton).setOnClickListener(v -> actions.openLeaderboard());
        driversLabel.setOnClickListener(v -> actions.openDrivers());
        teamsLabel.setOnClickListener(v -> actions.openTeams());
    }

    /** The lists of the home screen: the numbers, the photos and the colours are already in them. */
    void bindSources(List<driversList> driverList, List<teamsList> teamList) {
        this.driverSource = driverList;
        this.teamSource = teamList;
    }

    /** The lists of the home screen changed: rebuild the cards when the block is on screen. */
    void update() {
        if (card.getVisibility() != View.VISIBLE) {
            return;
        }

        buildDrivers();
        buildTeams();
    }

    void refresh(String season) {
        refresh(season, true);
    }

    private void refresh(String season, boolean allowPrevious) {
        final int mine = ++token;

        if (debugForce && (context.getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0) {
            showFinished(season);
            return;
        }

        FirebaseDatabase.getInstance().getReference().child("schedule").child("season").child(season)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (mine != token) {
                            return;
                        }

                        long now = System.currentTimeMillis();
                        int races = 0;
                        boolean finished = true;

                        for (DataSnapshot race : snapshot.getChildren()) {
                            races++;
                            raceWeekendStatus.State state = raceWeekendStatus.resolve(race, now);

                            if (state != raceWeekendStatus.State.CONCLUDED
                                    && state != raceWeekendStatus.State.CANCELED) {
                                finished = false;
                                break;
                            }
                        }

                        if (races == 0 && allowPrevious) {
                            // the calendar of the new year is not uploaded yet: keep the results of the last season
                            try {
                                refresh(String.valueOf(Integer.parseInt(season) - 1), false);
                                return;
                            } catch (NumberFormatException ignored) {
                                // fall through to the regular screen
                            }
                        }

                        if (races > 0 && finished) {
                            showFinished(season);
                        } else {
                            showRegular();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        if (mine == token) {
                            showRegular();
                        }
                    }
                });
    }

    private void showRegular() {
        card.setVisibility(View.GONE);
        futureHeader.setVisibility(View.VISIBLE);

        if (futureHidden) { // restore only what this block has hidden itself
            futureLayout.setVisibility(View.VISIBLE);
            futureHidden = false;
        }

        for (View view : hidden) {
            view.setVisibility(View.VISIBLE);
        }

        actions.onFinishedChanged(false);
    }

    private void showFinished(String season) {
        this.season = season;

        futureHeader.setVisibility(View.GONE);
        futureLayout.setVisibility(View.GONE);
        futureHidden = true;
        predictButton.setVisibility(View.GONE);

        for (View view : hidden) {
            view.setVisibility(View.GONE);
        }

        caption.setText(context.getString(R.string.finished_caption, season));
        card.setVisibility(View.VISIBLE);

        update();
        actions.onFinishedChanged(true);
    }

    // ---------- drivers ----------

    private void buildDrivers() {
        driversRow.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(context);

        for (int i = 0; driverSource != null && i < Math.min(3, driverSource.size()); i++) {
            driversList driver = driverSource.get(i);

            if (driver.isStartSeason()) {
                break; // no points yet
            }

            View view = inflater.inflate(R.layout.view_season_driver, driversRow, false);
            bindDriver(view, driver, i);

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            params.setMargins(dp(3), 0, dp(3), 0);
            driversRow.addView(view, params);
        }

        int visible = driversRow.getChildCount() > 0 ? View.VISIBLE : View.GONE;
        driversLabel.setVisibility(visible);
        driversRow.setVisibility(visible);
    }

    private void bindDriver(View view, driversList driver, int index) {
        int team = teamColors.parse(driver.getTeamColor());
        view.findViewById(R.id.seasonTile).setBackground(new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{team, ColorUtils.blendARGB(team, Color.BLACK, 0.4f)}));
        view.findViewById(R.id.seasonMetal).setBackgroundColor(metals[index]);

        ((TextView) view.findViewById(R.id.seasonPlace)).setText(driver.getDriverPlacement());
        ((TextView) view.findViewById(R.id.seasonSurname)).setText(driver.getDriverFamilyName());

        TextView points = view.findViewById(R.id.seasonPoints);
        points.setText(points(driver.getDriverPoints()));

        if (index == 0) {
            view.setBackgroundResource(R.drawable.bg_podium_champion);
            points.setTextColor(metals[0]);

            TextView badge = view.findViewById(R.id.seasonBadge);
            badge.setText(R.string.champion_driver);
            badge.setVisibility(View.VISIBLE);
        }

        ImageView photo = view.findViewById(R.id.seasonPhoto);
        GlideApp.with(photo)
                .load(driver.getImageUrl())
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .error(R.drawable.placeholder_driver)
                .into(photo);

        view.setOnClickListener(v -> {
            Intent intent = new Intent(context, driverPageActivity.class);
            Bundle bundle = new Bundle();
            bundle.putString("driverName", driver.getDriverName());
            bundle.putString("driverCode", driver.getDriverCode());
            bundle.putString("driverTeam", driver.getDriverTeam());
            bundle.putString("driverFamilyName", driver.getDriverFamilyName());
            bundle.putString("driverTeamId", driver.getConstructorId());
            bundle.putString("currentSeason", driver.getSeason());
            intent.putExtras(bundle);
            context.startActivity(intent);
        });
    }

    // ---------- teams ----------

    private void buildTeams() {
        teamsContainer.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(context);

        for (int i = 0; teamSource != null && i < Math.min(3, teamSource.size()); i++) {
            teamsList team = teamSource.get(i);

            if (team.getStartSeasonInfo()) {
                break;
            }

            View view = inflater.inflate(R.layout.view_season_team, teamsContainer, false);
            bindTeam(view, team, i);

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            params.bottomMargin = dp(6);
            teamsContainer.addView(view, params);
        }

        int visible = teamsContainer.getChildCount() > 0 ? View.VISIBLE : View.GONE;
        teamsLabel.setVisibility(visible);
        teamsContainer.setVisibility(visible);
    }

    private void bindTeam(View view, teamsList team, int index) {
        view.findViewById(R.id.seasonBar).setBackgroundColor(teamColors.parse(team.getTeamColor()));
        ((TextView) view.findViewById(R.id.seasonPlace)).setText(team.getPosition());
        ((TextView) view.findViewById(R.id.seasonName)).setText(team.getTeam());
        ((TextView) view.findViewById(R.id.seasonPoints)).setText(
                points(team.getPoints()) + " " + context.getString(R.string.finished_points_label));

        if (index == 0) {
            view.setBackgroundResource(R.drawable.bg_podium_champion);

            TextView badge = view.findViewById(R.id.seasonBadge);
            badge.setText(R.string.champion_team);
            badge.setVisibility(View.VISIBLE);
        }

        loadCar(view.findViewById(R.id.seasonCar), team);

        view.setOnClickListener(v -> {
            Intent intent = new Intent(context, teamPageActivity.class);
            Bundle bundle = new Bundle();
            bundle.putString("teamName", team.getTeam());
            bundle.putString("teamId", team.getTeamId());
            bundle.putStringArrayList("teamDrivers", team.getDrivers());
            bundle.putString("currentSeason", team.getSeason());
            intent.putExtras(bundle);
            context.startActivity(intent);
        });
    }

    /** The cars are wide pictures: the left half (the right half for Audi, mirrored), as on the leaders. */
    private void loadCar(ImageView car, teamsList team) {
        car.setTag(team.getTeamId());

        GlideApp.with(car)
                .asBitmap()
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .error(R.drawable.placeholder_car)
                .load(team.getImageUrl())
                .into(new CustomTarget<Bitmap>() {
                    @Override
                    public void onResourceReady(@NonNull Bitmap resource,
                                                @Nullable Transition<? super Bitmap> transition) {
                        if (!team.getTeamId().equals(car.getTag())) {
                            return;
                        }

                        boolean audi = "audi".equals(team.getTeamId());
                        int half = resource.getWidth() / 2;

                        car.setScaleX(audi ? -1f : 1f);
                        car.setImageBitmap(Bitmap.createBitmap(
                                resource, audi ? half : 0, 0, half, resource.getHeight()));
                    }

                    @Override
                    public void onLoadCleared(@Nullable Drawable placeholder) {
                    }
                });
    }

    // ---------- helpers ----------

    private static String points(String value) {
        if (value == null) {
            return "";
        }

        return value.endsWith(".0") ? value.substring(0, value.length() - 2) : value;
    }

    private int dp(float value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }
}