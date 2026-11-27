package com.example.f1app;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;

final class themeMenu {
    private themeMenu() {}

    static int iconFor(String mode) {
        if (themeStore.MODE_LIGHT.equals(mode)) return R.drawable.ic_theme_light;
        if (themeStore.MODE_DARK.equals(mode)) return R.drawable.ic_theme_dark;
        return R.drawable.ic_theme_system;
    }

    static void show(Activity activity, View anchor) {
        final String[] modes = {themeStore.MODE_SYSTEM, themeStore.MODE_LIGHT, themeStore.MODE_DARK};
        final int[] labels = {R.string.theme_system, R.string.theme_light, R.string.theme_dark};
        final String current = themeStore.get(activity);
        final float density = activity.getResources().getDisplayMetrics().density;

        LinearLayout card = new LinearLayout(activity);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundResource(R.drawable.bg_theme_popup);
        int pad = Math.round(6 * density);
        card.setPadding(pad, pad, pad, pad);

        PopupWindow popup = new PopupWindow(card,
                Math.round(220 * density), ViewGroup.LayoutParams.WRAP_CONTENT, true);
        popup.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        popup.setElevation(8 * density);

        LayoutInflater inflater = LayoutInflater.from(activity);
        for (int i = 0; i < modes.length; i++) {
            final String mode = modes[i];
            View row = inflater.inflate(R.layout.item_theme_option, card, false);
            ImageView icon = row.findViewById(R.id.optionIcon);
            TextView label = row.findViewById(R.id.optionLabel);
            ImageView check = row.findViewById(R.id.optionCheck);

            icon.setImageResource(iconFor(mode));
            label.setText(labels[i]);

            boolean selected = mode.equals(current);
            check.setVisibility(selected ? View.VISIBLE : View.INVISIBLE);
            if (selected) row.setBackgroundResource(R.drawable.bg_theme_option_selected);
            row.setSelected(selected);

            row.setOnClickListener(v -> {
                popup.dismiss();
                if (!mode.equals(current)) {
                    apexAnalytics.themeChanged(activity, mode);
                    themeStore.set(activity, mode);
                }
            });
            card.addView(row);
        }

        popup.showAsDropDown(anchor, 0, 0, Gravity.END);
    }
}