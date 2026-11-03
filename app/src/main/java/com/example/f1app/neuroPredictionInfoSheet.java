package com.example.f1app;

import android.graphics.Typeface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.widget.NestedScrollView;

public class neuroPredictionInfoSheet extends neuroBaseSheet {
    @Nullable @Override public View onCreateView(@NonNull LayoutInflater inflater,
            @Nullable ViewGroup container, @Nullable Bundle state) {
        LinearLayout root = root();
        NestedScrollView scroll = new NestedScrollView(requireContext());
        LinearLayout content = new LinearLayout(requireContext());
        content.setOrientation(LinearLayout.VERTICAL);
        TextView title = text(getString(R.string.neuro_design_about), 22, R.color.text_color_main);
        title.setTypeface(title.getTypeface(), Typeface.BOLD);
        content.addView(title);
        addSection(content, R.string.neuro_design_capabilities, R.string.neuro_design_capabilities_body);
        addSection(content, R.string.neuro_design_round, R.string.neuro_design_round_body);
        addSection(content, R.string.neuro_design_major, R.string.neuro_design_major_body);
        TextView done = text(getString(R.string.neuro_design_done), 16, R.color.red);
        done.setTypeface(done.getTypeface(), Typeface.BOLD);
        done.setGravity(android.view.Gravity.CENTER);
        done.setMinHeight(dp(48));
        done.setPadding(dp(12), dp(12), dp(12), dp(12));
        // done.setBackgroundResource(android.R.drawable.list_selector_background);
        done.setFocusable(true);
        done.setOnClickListener(v -> dismiss());
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.topMargin = dp(20);
        content.addView(done, lp);
        scroll.addView(content);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, -2));
        return root;
    }
    private void addSection(LinearLayout content, int heading, int body) {
        TextView label = text(getString(heading), 12, R.color.text_color_thirdly);
        label.setAllCaps(true);
        label.setLetterSpacing(0.06f);
        label.setTypeface(label.getTypeface(), Typeface.BOLD);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.topMargin = dp(22);
        content.addView(label, lp);
        TextView description = text(getString(body), 16, R.color.text_color_main);
        description.setLineSpacing(dp(3), 1f);
        LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(-1, -2);
        bp.topMargin = dp(8);
        content.addView(description, bp);
    }
}
