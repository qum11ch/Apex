package com.example.f1app;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import java.util.List;

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

        // the track under the pills and the page margins are set here, so the three layouts stay as they are
        row.setBackgroundResource(R.drawable.bg_public_segment_track);
        row.setPadding(dp(4), dp(4), dp(4), dp(4));

        scroll.setClipToPadding(false);
        scroll.setPadding(dp(12), dp(10), dp(12), dp(8));
    }

    int getSelected() {
        return selected;
    }

    /** Builds the pills and marks one of them; does not call the listener. */
    void setSeasons(List<Integer> seasons, int selectedSeason) {
        Context context = row.getContext();
        row.removeAllViews();

        for (int season : seasons) {
            TextView pill = (TextView) LayoutInflater.from(context)
                    .inflate(R.layout.item_public_profile_segment, row, false);

            pill.setText(String.valueOf(season));
            pill.setTag(season);
            pill.setOnClickListener(view -> onClick((TextView) view));
            row.addView(pill);
        }

        select(selectedSeason);
    }

    /** Marks a season and scrolls it into view; does not call the listener. */
    void select(int season) {
        selected = season;

        Context context = row.getContext();
        int selectedColor = ContextCompat.getColor(context, R.color.text_color_main);
        int idleColor = ContextCompat.getColor(context, R.color.grey);

        for (int index = 0; index < row.getChildCount(); index++) {
            View child = row.getChildAt(index);

            if (!(child instanceof TextView) || child.getTag() == null) {
                continue;
            }

            TextView pill = (TextView) child;
            boolean isSelected = ((Integer) pill.getTag()) == season;

            pill.setSelected(isSelected);
            pill.setTextColor(isSelected ? selectedColor : idleColor);

            if (isSelected) {
                scroll.post(() -> scroll.smoothScrollTo(Math.max(0, pill.getLeft() - dp(16)), 0));
            }
        }
    }

    private void onClick(TextView pill) {
        int clicked = (Integer) pill.getTag();

        if (clicked == selected) {
            return;
        }

        select(clicked);
        listener.onSeasonSelected(clicked);
    }

    private int dp(int value) {
        return Math.round(value * row.getResources().getDisplayMetrics().density);
    }
}
