package com.example.f1app;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ToggleButton;

import java.util.List;

/** A row of season buttons built from the same item_season_toggle that the driver and team pages use. */
final class seasonChipsBar {

    interface Listener {
        void onSeasonSelected(int season);
    }

    private final HorizontalScrollView scroll;
    private final LinearLayout row;
    private final Listener listener;
    private int selected = -1;

    seasonChipsBar(HorizontalScrollView scroll, LinearLayout row, Listener listener) {
        this.scroll = scroll;
        this.row = row;
        this.listener = listener;
    }

    int getSelected() {
        return selected;
    }

    /** Builds the buttons and marks one of them; does not call the listener. */
    void setSeasons(List<Integer> seasons, int selectedSeason) {
        Context context = row.getContext();
        row.removeAllViews();

        for (int season : seasons) {
            ToggleButton button = (ToggleButton) LayoutInflater.from(context)
                    .inflate(R.layout.item_season_toggle, row, false);

            String text = String.valueOf(season);
            button.setTextOn(text);
            button.setTextOff(text);
            button.setText(text);
            button.setTag(season);
            button.setOnClickListener(view -> onClick((ToggleButton) view));
            row.addView(button);
        }

        select(selectedSeason);
    }

    /** Marks a season and scrolls it into view; does not call the listener. */
    void select(int season) {
        selected = season;

        for (int index = 0; index < row.getChildCount(); index++) {
            View child = row.getChildAt(index);

            if (!(child instanceof ToggleButton)) {
                continue;
            }

            ToggleButton button = (ToggleButton) child;
            boolean isSelected = ((Integer) button.getTag()) == season;
            button.setChecked(isSelected);

            if (isSelected) {
                scroll.post(() -> scroll.smoothScrollTo(Math.max(0, button.getLeft() - dp(16)), 0));
            }
        }
    }

    private void onClick(ToggleButton button) {
        int clicked = (Integer) button.getTag();

        if (clicked == selected) {
            button.setChecked(true); // a toggle must not switch itself off
            return;
        }

        select(clicked);
        listener.onSeasonSelected(clicked);
    }

    private int dp(int value) {
        return Math.round(value * row.getResources().getDisplayMetrics().density);
    }
}
