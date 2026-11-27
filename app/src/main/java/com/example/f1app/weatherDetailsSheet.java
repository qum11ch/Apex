package com.example.f1app;

import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.google.android.material.bottomsheet.BottomSheetDialog;

/** Air and track temperature, humidity, wind, precipitation and the state of the track of one session. */
final class weatherDetailsSheet {

    private weatherDetailsSheet() {
    }

    static void show(Context context, String title, sessionWeather w) {
        BottomSheetDialog dialog = new BottomSheetDialog(context);

        LinearLayout root = new LinearLayout(context);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(ContextCompat.getColor(context, R.color.hero_card_bg));
        int pad = dp(context, 20);
        root.setPadding(pad, pad, pad, dp(context, 28));

        TextView heading = text(context, title, 18, R.color.hero_text, true);
        root.addView(heading, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        row(context, root, context.getString(R.string.sheet_air), w.measured || !w.temp.isEmpty() ? w.temp : "");

        if (!Double.isNaN(w.track)) {
            row(context, root, context.getString(R.string.sheet_track), weatherCard.degrees(w.track));
        }

        if (!Double.isNaN(w.humidity)) {
            row(context, root, context.getString(R.string.sheet_humidity), Math.round(w.humidity) + "%");
        }

        row(context, root, context.getString(R.string.sheet_wind), w.windFull);
        row(context, root, context.getString(R.string.sheet_rain), w.rain);

        if (!w.trackState.isEmpty()) {
            row(context, root, context.getString(R.string.sheet_state), w.trackState);
        }

        TextView source = text(context, context.getString(w.measured
                        ? R.string.sheet_source_measured : R.string.sheet_source_model),
                12, R.color.hero_text_secondary, false);
        LinearLayout.LayoutParams sourceParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        sourceParams.topMargin = dp(context, 16);
        root.addView(source, sourceParams);

        dialog.setContentView(root);
        dialog.show();
    }

    private static void row(Context context, LinearLayout parent, String label, String value) {
        if (value == null || value.isEmpty()) {
            return;
        }

        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);

        TextView name = text(context, label, 14, R.color.hero_text_secondary, false);
        row.addView(name, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView data = text(context, value, 15, R.color.hero_text, true);
        data.setGravity(Gravity.END);
        row.addView(data, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.topMargin = dp(context, 14);
        parent.addView(row, params);
    }

    private static TextView text(Context context, String value, int size, int colorRes, boolean bold) {
        TextView view = new TextView(context);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(ContextCompat.getColor(context, colorRes));

        if (bold) {
            view.setTypeface(view.getTypeface(), android.graphics.Typeface.BOLD);
        }

        return view;
    }

    private static int dp(Context context, float value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }
}