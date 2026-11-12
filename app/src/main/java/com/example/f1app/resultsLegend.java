package com.example.f1app;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

/** Key map of the result chips (same entries as in the team results screen). */
final class resultsLegend {

    private resultsLegend() {
    }

    static void build(Context context, LinearLayout left, LinearLayout right) {
        // 0 = no fill ("did not take part")
        int[] fills = {
                R.color.light_gold, R.color.light_silver, R.color.light_bronze,
                R.color.light_green, R.color.light_purple, R.color.light_grey,
                R.color.pink, R.color.white, R.color.black,
                0, R.color.light_grey, R.color.orange
        };

        int[] labels = {
                R.string._1st_place_header, R.string._2nd_place_header, R.string._3rd_place_header,
                R.string.points_finish, R.string.out_of_points_finish, R.string.pole,
                R.string.retired, R.string.dns, R.string.dsq,
                R.string.wd, R.string.not_concluded, R.string.cnd
        };

        String[] samples = {
                "1", "2", "3", "5", "15", "1",
                "", "", "", "-", context.getString(R.string.n_c_text), ""
        };

        float density = context.getResources().getDisplayMetrics().density;
        LayoutInflater inflater = LayoutInflater.from(context);

        left.removeAllViews();
        right.removeAllViews();

        for (int i = 0; i < fills.length; i++) {
            LinearLayout column = i < 6 ? left : right;
            View entry = inflater.inflate(R.layout.item_results_legend_entry, column, false);

            TextView swatch = entry.findViewById(R.id.legend_swatch);
            TextView label = entry.findViewById(R.id.legend_label);

            boolean pole = labels[i] == R.string.pole;
            boolean empty = fills[i] == 0;

            GradientDrawable background = new GradientDrawable();
            background.setCornerRadius(6 * density);
            background.setColor(empty
                    ? Color.TRANSPARENT
                    : ContextCompat.getColor(context, fills[i]));

            if (pole) {
                background.setStroke(Math.round(2 * density),
                        ContextCompat.getColor(context, R.color.purple));
            } else if (fills[i] == R.color.white) {
                background.setStroke(Math.round(density),
                        ContextCompat.getColor(context, R.color.grey));
            }

            swatch.setBackground(background);
            swatch.setText(samples[i]);
            swatch.setTextColor(ContextCompat.getColor(context,
                    empty ? R.color.grey
                            : (fills[i] == R.color.black || fills[i] == R.color.orange
                            ? R.color.white : R.color.dark_grey)));

            label.setText(labels[i]);

            column.addView(entry);
        }
    }
}
