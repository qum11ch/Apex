package com.example.f1app;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.widget.NestedScrollView;

import java.util.List;
import java.util.Locale;

public class neuroFactorsSheet extends neuroBaseSheet {
    public static final class Factor {
        final String group;
        final double seconds;

        Factor(String group, double seconds) {
            this.group = group;
            this.seconds = seconds;
        }
    }

    private static final String ARG_NAME = "name";
    private static final String ARG_GROUPS = "groups";
    private static final String ARG_SECONDS = "seconds";
    private static final int FASTER = Color.parseColor("#2E9E5B");

    public static neuroFactorsSheet newInstance(String name, List<Factor> factors) {
        String[] groups = new String[factors.size()];
        double[] seconds = new double[factors.size()];
        for (int i = 0; i < factors.size(); i++) {
            groups[i] = factors.get(i).group;
            seconds[i] = factors.get(i).seconds;
        }
        Bundle args = new Bundle();
        args.putString(ARG_NAME, name);
        args.putStringArray(ARG_GROUPS, groups);
        args.putDoubleArray(ARG_SECONDS, seconds);
        neuroFactorsSheet sheet = new neuroFactorsSheet();
        sheet.setArguments(args);
        return sheet;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle state) {
        Bundle args = requireArguments();
        String name = args.getString(ARG_NAME, "");
        String[] groups = args.getStringArray(ARG_GROUPS);
        double[] seconds = args.getDoubleArray(ARG_SECONDS);

        LinearLayout root = root();
        NestedScrollView scroll = new NestedScrollView(requireContext());
        LinearLayout content = new LinearLayout(requireContext());
        content.setOrientation(LinearLayout.VERTICAL);

        TextView title = text(name, 22, R.color.text_color_main);
        title.setTypeface(title.getTypeface(), Typeface.BOLD);
        content.addView(title);

        TextView heading = text(getString(R.string.neuro_factors_title), 12, R.color.text_color_thirdly);
        heading.setAllCaps(true);
        heading.setLetterSpacing(0.06f);
        LinearLayout.LayoutParams hp = new LinearLayout.LayoutParams(-1, -2);
        hp.topMargin = dp(6);
        content.addView(heading, hp);

        if (groups != null && seconds != null) {
            double scale = 0.05;
            for (double value : seconds) scale = Math.max(scale, Math.abs(value));
            for (int i = 0; i < groups.length && i < seconds.length; i++) {
                addRow(content, groups[i], seconds[i], scale);
            }
        }

        TextView legend = text(getString(R.string.neuro_factors_legend), 13, R.color.text_color_thirdly);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.topMargin = dp(20);
        content.addView(legend, lp);

        TextView note = text(getString(R.string.neuro_factors_note), 13, R.color.text_color_thirdly);
        LinearLayout.LayoutParams np = new LinearLayout.LayoutParams(-1, -2);
        np.topMargin = dp(8);
        content.addView(note, np);

        TextView done = text(getString(R.string.neuro_design_done), 16, R.color.red);
        done.setTypeface(done.getTypeface(), Typeface.BOLD);
        done.setGravity(Gravity.CENTER);
        done.setMinHeight(dp(48));
        done.setPadding(dp(12), dp(12), dp(12), dp(12));
        done.setFocusable(true);
        done.setOnClickListener(v -> dismiss());
        LinearLayout.LayoutParams dp = new LinearLayout.LayoutParams(-1, -2);
        dp.topMargin = dp(16);
        content.addView(done, dp);

        scroll.addView(content);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, -2));
        return root;
    }

    private void addRow(LinearLayout content, String group, double delta, double scale) {
        int labelRes = labelFor(group);
        if (labelRes == 0) return;

        String label = getString(labelRes);
        String number = String.format(Locale.getDefault(), "%+.2f", delta).replace('-', '\u2212');
        String value = getString(R.string.neuro_factors_seconds, number);
        boolean faster = delta < 0;

        LinearLayout row = new LinearLayout(requireContext());
        row.setOrientation(LinearLayout.VERTICAL);
        row.setContentDescription(label + ", " + value);

        LinearLayout header = new LinearLayout(requireContext());
        header.setOrientation(LinearLayout.HORIZONTAL);
        TextView labelView = text(label, 16, R.color.text_color_main);
        TextView valueView = text(value, 16, R.color.text_color_main);
        valueView.setTypeface(valueView.getTypeface(), Typeface.BOLD);
        valueView.setTextColor(faster ? FASTER : color(R.color.red));
        header.addView(labelView, new LinearLayout.LayoutParams(0, -2, 1f));
        header.addView(valueView, new LinearLayout.LayoutParams(-2, -2));
        row.addView(header);

        BarView bar = new BarView(requireContext(), color(R.color.neuro_divider),
                color(R.color.text_color_thirdly));
        bar.set((float) (-delta / scale), faster ? FASTER : color(R.color.red));
        LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(-1, dp(10));
        bp.topMargin = dp(8);
        row.addView(bar, bp);

        LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(-1, -2);
        rp.topMargin = dp(18);
        content.addView(row, rp);
    }

    private static int labelFor(String group) {
        if (group == null) return 0;
        switch (group) {
            case "driver": return R.string.neuro_factor_driver;
            case "team": return R.string.neuro_factor_team;
            case "track": return R.string.neuro_factor_track;
            case "weather": return R.string.neuro_factor_weather;
            case "season": return R.string.neuro_factor_season;
            default: return 0;
        }
    }

    private static final class BarView extends View {
        private final Paint track = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint bar = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint center = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF rect = new RectF();
        private float fraction;

        BarView(Context context, int trackColor, int centerColor) {
            super(context);
            track.setColor(trackColor);
            center.setColor(centerColor);
        }

        void set(float fraction, int barColor) {
            this.fraction = Math.max(-1f, Math.min(1f, fraction));
            bar.setColor(barColor);
            invalidate();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            float w = getWidth(), h = getHeight(), mid = w / 2f, r = h / 2f;
            rect.set(0, 0, w, h);
            canvas.drawRoundRect(rect, r, r, track);
            float length = Math.abs(fraction) * mid;
            if (length > 0f) {
                if (fraction > 0f) rect.set(mid, 0, mid + length, h);
                else rect.set(mid - length, 0, mid, h);
                canvas.drawRoundRect(rect, r, r, bar);
            }
            canvas.drawRect(mid - 1f, 0, mid + 1f, h, center);
        }
    }
}