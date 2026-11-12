package com.example.f1app;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

/**
 * One place that turns a result code ("CND", "NP", "N/C", "null" or "start-finish")
 * into a result chip. Used by the driver results table; teamResultsAdapter can call it too.
 */
final class resultChipBinder {

    private resultChipBinder() {
    }

    static void bind(Context context, TextView chip, String rawCode) {
        String code = rawCode == null ? "null" : rawCode.trim();

        int fill = color(context, R.color.light_grey);
        int textColor = color(context, R.color.dark_grey);
        String text = "-";
        boolean pole = false;
        boolean outlined = false;
        boolean empty = false;

        switch (code) {
            case "CND":
                fill = color(context, R.color.orange);
                textColor = color(context, R.color.white);
                text = context.getString(R.string.cnd_text);
                break;
            case "NP":
                fill = color(context, R.color.white);
                outlined = true;
                text = context.getString(R.string.dns_text);
                break;
            case "N/C":
                text = context.getString(R.string.n_c_text);
                break;
            case "null":
                empty = true;
                break;
            default:
                String[] parts = code.split("-");

                if (parts.length != 2) {
                    empty = true;
                    break;
                }

                String start = parts[0];
                String finish = parts[1];

                switch (finish) {
                    case "R":
                        fill = color(context, R.color.pink);
                        text = context.getString(R.string.ret_text);
                        pole = "1".equals(start);
                        break;
                    case "W":
                        text = context.getString(R.string.wd_text);
                        break;
                    case "D":
                        fill = color(context, R.color.black);
                        textColor = color(context, R.color.white);
                        text = context.getString(R.string.dsq_text);
                        break;
                    default:
                        try {
                            int position = Integer.parseInt(finish);

                            if (position == 1) fill = color(context, R.color.light_gold);
                            else if (position == 2) fill = color(context, R.color.light_silver);
                            else if (position == 3) fill = color(context, R.color.light_bronze);
                            else if (position <= 10) fill = color(context, R.color.light_green);
                            else fill = color(context, R.color.light_purple);

                            text = finish;
                            pole = "1".equals(start);
                        } catch (NumberFormatException ignored) {
                            empty = true;
                        }
                        break;
                }
                break;
        }

        GradientDrawable background = new GradientDrawable();
        background.setCornerRadius(dp(context, 9));

        if (empty) {
            background.setColor(Color.TRANSPARENT);
            chip.setTextColor(color(context, R.color.grey));
            chip.setText("\u2013");
        } else {
            background.setColor(fill);

            if (pole) {
                background.setStroke(dp(context, 2), color(context, R.color.purple));
            } else if (outlined) {
                background.setStroke(dp(context, 1), color(context, R.color.grey));
            }

            chip.setTextColor(textColor);
            chip.setText(text);
        }

        chip.setBackground(background);
    }

    private static int color(Context context, int res) {
        return ContextCompat.getColor(context, res);
    }

    private static int dp(Context context, int value) {
        return Math.round(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, value, context.getResources().getDisplayMetrics()));
    }
}
