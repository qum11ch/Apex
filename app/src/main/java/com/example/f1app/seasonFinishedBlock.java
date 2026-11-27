package com.example.f1app;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.List;

/**
 * "Season is over": when every race of the season is concluded or canceled, the summary replaces the
 * "upcoming race" block: the champions, the wide button of the prediction results and the top 3 of the
 * drivers and of the teams (the cards of the home screen leaders in the colours of the podium).
 */
final class seasonFinishedBlock {

    interface Actions {
        void openDrivers();

        void openTeams();

        void openLeaderboard();
    }

    /** Only honoured in a debuggable build: long press on the title of the home screen. */
    static boolean debugForce = false;

    private final Context context;
    private final View card, futureHeader, futureLayout, predictButton;
    private final View[] hidden;
    private final TextView caption, champion, constructors;
    private final RecyclerView driversView, teamsView;
    private final int[] tints;

    private List<driversList> driverSource;
    private List<teamsList> teamSource;
    private mainDriversStandingsAdapter driversAdapter;
    private mainTeamsStandingsAdapter teamsAdapter;
    private boolean futureHidden;
    private int token = 0;

    seasonFinishedBlock(View card, View futureHeader, View futureLayout, View predictButton,
                        Actions actions, View... hidden) {
        this.context = card.getContext();
        this.card = card;
        this.futureHeader = futureHeader;
        this.futureLayout = futureLayout;
        this.predictButton = predictButton;
        this.hidden = hidden;

        tints = new int[]{
                ContextCompat.getColor(context, R.color.podium_gold),
                ContextCompat.getColor(context, R.color.podium_silver),
                ContextCompat.getColor(context, R.color.podium_bronze)};

        caption = card.findViewById(R.id.finishedCaption);
        champion = card.findViewById(R.id.finishedChampion);
        constructors = card.findViewById(R.id.finishedConstructor);
        driversView = card.findViewById(R.id.finishedDriversList);
        teamsView = card.findViewById(R.id.finishedTeamsList);

        card.findViewById(R.id.finishedLeaderboardButton).setOnClickListener(v -> actions.openLeaderboard());
        card.findViewById(R.id.finishedDriversLabel).setOnClickListener(v -> actions.openDrivers());
        card.findViewById(R.id.finishedTeamsLabel).setOnClickListener(v -> actions.openTeams());
    }

    /** The lists of the home screen: the same objects the leaders use, so one load feeds both. */
    void bindSources(List<driversList> driverList, List<teamsList> teamList) {
        this.driverSource = driverList;
        this.teamSource = teamList;

        driversAdapter = new mainDriversStandingsAdapter(context, driverList);
        driversAdapter.setPodiumTints(tints);
        driversView.setLayoutManager(new LinearLayoutManager(context));
        driversView.setNestedScrollingEnabled(false);
        driversView.setOverScrollMode(View.OVER_SCROLL_NEVER);
        driversView.setAdapter(driversAdapter);

        teamsAdapter = new mainTeamsStandingsAdapter(context, teamList);
        teamsAdapter.setPodiumTints(tints);
        teamsView.setLayoutManager(new LinearLayoutManager(context));
        teamsView.setNestedScrollingEnabled(false);
        teamsView.setOverScrollMode(View.OVER_SCROLL_NEVER);
        teamsView.setAdapter(teamsAdapter);
    }

    /** The lists of the home screen changed: redraw the cards and the champions when the block is on screen. */
    void update() {
        if (card.getVisibility() != View.VISIBLE) {
            return;
        }

        if (driversAdapter != null) {
            driversAdapter.notifyDataSetChanged();
        }

        if (teamsAdapter != null) {
            teamsAdapter.notifyDataSetChanged();
        }

        if (driverSource != null && !driverSource.isEmpty() && !driverSource.get(0).isStartSeason()) {
            champion.setText(context.getString(R.string.finished_champion,
                    driverSource.get(0).getDriverFamilyName()));
            champion.setVisibility(View.VISIBLE);
        } else {
            champion.setVisibility(View.GONE);
        }

        if (teamSource != null && !teamSource.isEmpty() && !teamSource.get(0).getStartSeasonInfo()) {
            constructors.setText(context.getString(R.string.finished_constructors,
                    teamSource.get(0).getTeam()));
            constructors.setVisibility(View.VISIBLE);
        } else {
            constructors.setVisibility(View.GONE);
        }
    }

    void refresh(String season) {
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
    }

    private void showFinished(String season) {
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
    }
}