package com.example.f1app;

import android.content.Context;
import android.content.ContextWrapper;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import java.util.ArrayList;
import java.util.List;

/** The weather of one session in the style of the other sheets (neuroBaseSheet). */
public class weatherSheet extends neuroBaseSheet {

    private static final String TAG = "weatherSheet";
    private static final String ARG_TITLE = "title";
    private static final String ARG_ICON = "icon";
    private static final String ARG_TEMP = "temp";
    private static final String ARG_CONDITION = "condition";
    private static final String ARG_RAIN = "rain";
    private static final String ARG_WET = "wet";
    private static final String ARG_WIND = "wind";
    private static final String ARG_TRACK = "track";
    private static final String ARG_HUMIDITY = "humidity";
    private static final String ARG_STATE = "state";
    private static final String ARG_MEASURED = "measured";

    /** Works from an adapter or a view: the activity is found through the context. */
    static void show(Context context, String title, sessionWeather weather) {
        while (context instanceof ContextWrapper && !(context instanceof FragmentActivity)) {
            context = ((ContextWrapper) context).getBaseContext();
        }

        if (context instanceof FragmentActivity) {
            show(((FragmentActivity) context).getSupportFragmentManager(), title, weather);
        }
    }

    static void show(FragmentManager manager, String title, sessionWeather w) {
        if (manager.findFragmentByTag(TAG) != null || manager.isStateSaved()) {
            return;
        }

        Bundle args = new Bundle();
        args.putString(ARG_TITLE, title);
        args.putInt(ARG_ICON, w.icon);
        args.putString(ARG_TEMP, w.temp);
        args.putString(ARG_CONDITION, w.condition);
        args.putString(ARG_RAIN, w.rain);
        args.putBoolean(ARG_WET, w.wet);
        args.putString(ARG_WIND, w.windFull);
        args.putDouble(ARG_TRACK, w.track);
        args.putDouble(ARG_HUMIDITY, w.humidity);
        args.putString(ARG_STATE, w.trackState);
        args.putBoolean(ARG_MEASURED, w.measured);

        weatherSheet sheet = new weatherSheet();
        sheet.setArguments(args);
        sheet.show(manager, TAG);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle state) {
        Bundle a = requireArguments();
        LinearLayout root = root();

        NestedScrollView scroll = new NestedScrollView(requireContext());
        LinearLayout content = new LinearLayout(requireContext());
        content.setOrientation(LinearLayout.VERTICAL);

        TextView title = text(a.getString(ARG_TITLE, ""), 22, R.color.text_color_main);
        title.setTypeface(title.getTypeface(), Typeface.BOLD);
        content.addView(title);

        // the summary: icon, temperature, condition
        LinearLayout summary = new LinearLayout(requireContext());
        summary.setOrientation(LinearLayout.HORIZONTAL);
        summary.setGravity(android.view.Gravity.CENTER_VERTICAL);

        ImageView icon = new ImageView(requireContext());
        icon.setImageResource(a.getInt(ARG_ICON));
        icon.setColorFilter(color(R.color.text_color_main));
        summary.addView(icon, new LinearLayout.LayoutParams(dp(48), dp(48)));

        LinearLayout box = new LinearLayout(requireContext());
        box.setOrientation(LinearLayout.VERTICAL);

        TextView temp = text(a.getString(ARG_TEMP, ""), 34, R.color.text_color_main);
        temp.setTextAppearance(R.style.TextStyle_Audiowide_ForDays);
        temp.setTextSize(34);
        temp.setTextColor(color(R.color.text_color_main));
        box.addView(temp);

        String condition = a.getString(ARG_CONDITION, "");

        if (!condition.isEmpty()) {
            box.addView(text(condition, 15, R.color.text_color_secondary));
        }

        LinearLayout.LayoutParams boxParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        boxParams.setMarginStart(dp(14));
        summary.addView(box, boxParams);

        LinearLayout.LayoutParams summaryParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        summaryParams.topMargin = dp(16);
        content.addView(summary, summaryParams);

        // the tiles
        List<LinearLayout> tiles = new ArrayList<>();
        double track = a.getDouble(ARG_TRACK, Double.NaN);
        double humidity = a.getDouble(ARG_HUMIDITY, Double.NaN);

        if (!Double.isNaN(track)) {
            tiles.add(tile(getString(R.string.sheet_track), weatherCard.degrees(track), false));
        }

        if (!Double.isNaN(humidity)) {
            tiles.add(tile(getString(R.string.sheet_humidity), Math.round(humidity) + "%", false));
        }

        tiles.add(tile(getString(R.string.sheet_wind), a.getString(ARG_WIND, ""), false));
        tiles.add(tile(getString(R.string.sheet_rain), a.getString(ARG_RAIN, ""), a.getBoolean(ARG_WET)));
        addTiles(content, tiles);

        String trackState = a.getString(ARG_STATE, "");

        if (!trackState.isEmpty()) {
            LinearLayout wide = tile(getString(R.string.sheet_state), trackState, false);
            LinearLayout.LayoutParams wideParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            wideParams.topMargin = dp(10);
            content.addView(wide, wideParams);
        }

        TextView source = text(getString(a.getBoolean(ARG_MEASURED)
                        ? R.string.sheet_source_measured : R.string.sheet_source_model),
                12, R.color.text_color_thirdly);
        LinearLayout.LayoutParams sourceParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        sourceParams.topMargin = dp(18);
        content.addView(source, sourceParams);

        scroll.addView(content);
        root.addView(scroll, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return root;
    }

    private LinearLayout tile(String label, String value, boolean accent) {
        LinearLayout tile = new LinearLayout(requireContext());
        tile.setOrientation(LinearLayout.VERTICAL);
        tile.setPadding(dp(14), dp(12), dp(14), dp(12));
        tile.setBackground(surface(color(R.color.account_settings_input_color), 12));

        TextView caption = text(label, 12, R.color.text_color_thirdly);
        caption.setAllCaps(true);
        caption.setLetterSpacing(0.06f);
        caption.setTypeface(caption.getTypeface(), Typeface.BOLD);
        tile.addView(caption);

        TextView data = text(value, 18, accent ? R.color.red : R.color.text_color_main);
        data.setTypeface(data.getTypeface(), Typeface.BOLD);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.topMargin = dp(4);
        tile.addView(data, params);

        return tile;
    }

    /** Two tiles in a row, both as tall as the taller one. */
    private void addTiles(LinearLayout content, List<LinearLayout> tiles) {
        for (int i = 0; i < tiles.size(); i += 2) {
            LinearLayout row = new LinearLayout(requireContext());
            row.setOrientation(LinearLayout.HORIZONTAL);

            LinearLayout.LayoutParams first = new LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.MATCH_PARENT, 1f);
            first.setMarginEnd(dp(5));
            row.addView(tiles.get(i), first);

            LinearLayout.LayoutParams second = new LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.MATCH_PARENT, 1f);
            second.setMarginStart(dp(5));
            row.addView(i + 1 < tiles.size() ? tiles.get(i + 1) : new View(requireContext()), second);

            LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            rowParams.topMargin = dp(10);
            content.addView(row, rowParams);
        }
    }
}