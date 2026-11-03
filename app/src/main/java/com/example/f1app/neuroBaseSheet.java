package com.example.f1app;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import android.os.Build;
import android.view.Window;

import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;

public abstract class neuroBaseSheet extends BottomSheetDialogFragment {
    protected int dp(float value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
    protected int color(int id) {
        return ContextCompat.getColor(requireContext(), id);
    }
    protected GradientDrawable surface(int color, int radius) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(dp(radius));
        return drawable;
    }
    protected TextView text(String value, int size, int colorId) {
        TextView view = new TextView(requireContext());
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color(colorId));
        return view;
    }
    protected LinearLayout root() {
        LinearLayout root = new LinearLayout(requireContext());
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(24), dp(12), dp(24), dp(20));
        GradientDrawable background = surface(color(R.color.colorPrimary), 0);

        float radius = dp(24);

        background.setCornerRadii(new float[]{
                radius, radius,   // верхний левый
                radius, radius,   // верхний правый
                0, 0,             // нижний правый
                0, 0              // нижний левый
        });

        root.setBackground(background);

        View handle = new View(requireContext());
        handle.setBackground(surface(color(R.color.neuro_divider), 2));
        LinearLayout.LayoutParams hp = new LinearLayout.LayoutParams(dp(36), dp(4));
        hp.gravity = android.view.Gravity.CENTER_HORIZONTAL;
        hp.bottomMargin = dp(24);
        root.addView(handle, hp);
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets safe = insets.getInsets(WindowInsetsCompat.Type.systemBars()
                    | WindowInsetsCompat.Type.displayCutout());
            v.setPadding(dp(24) + safe.left, dp(12), dp(24) + safe.right, dp(20) + safe.bottom);
            return insets;
        });
        root.post(() -> ViewCompat.requestApplyInsets(root));
        return root;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle state) {
        return new BottomSheetDialog(
                requireContext(),
                R.style.ThemeOverlay_Apex_NeuroSheet
        );
    }

    @Override
    public void onStart() {
        super.onStart();

        BottomSheetDialog dialog =
                (BottomSheetDialog) requireDialog();

        Window window = dialog.getWindow();

        if (window != null) {
            // Убираем дополнительное затемнение области навигации.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                window.setNavigationBarContrastEnforced(false);
            }

            boolean night =
                    (getResources().getConfiguration().uiMode
                            & android.content.res.Configuration.UI_MODE_NIGHT_MASK)
                            == android.content.res.Configuration.UI_MODE_NIGHT_YES;

            WindowInsetsControllerCompat controller =
                    WindowCompat.getInsetsController(
                            window,
                            window.getDecorView()
                    );

            controller.setAppearanceLightNavigationBars(!night);
        }

        View sheet = dialog.findViewById(
                com.google.android.material.R.id.design_bottom_sheet
        );

        if (sheet == null) return;

        sheet.setBackground(
                new ColorDrawable(Color.TRANSPARENT)
        );

        BottomSheetBehavior<View> behavior =
                BottomSheetBehavior.from(sheet);

        behavior.setSkipCollapsed(true);
        behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
    }
}
