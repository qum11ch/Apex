package com.example.f1app;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

public class userPredictionRulesDialog extends DialogFragment {

    static final String PREFS = "user_prediction_rules";
    static final String SEEN = "rules_seen";

    public static final String TAG =
            "user_prediction_rules_dialog";

    private View dialogContent;

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        dialogContent = getLayoutInflater().inflate(
                R.layout.dialog_user_prediction_rules,
                null,
                false
        );

        dialogContent
                .findViewById(R.id.rulesUnderstoodButton)
                .setOnClickListener(v -> {
                    requireContext()
                            .getSharedPreferences(
                                    PREFS,
                                    Context.MODE_PRIVATE
                            )
                            .edit()
                            .putBoolean(SEEN, true)
                            .apply();

                    dismiss();
                });

        AlertDialog dialog =
                new AlertDialog.Builder(requireContext())
                        .setView(dialogContent)
                        .create();

        dialog.setCanceledOnTouchOutside(true);

        return dialog;
    }

    @Override
    public void onStart() {
        super.onStart();

        Window window = requireDialog().getWindow();

        if (window == null || dialogContent == null) {
            return;
        }

        window.setBackgroundDrawable(
                new ColorDrawable(Color.TRANSPARENT)
        );

        int availableWidth =
                getResources().getDisplayMetrics().widthPixels;

        int availableHeight =
                getResources().getDisplayMetrics().heightPixels;

        int dialogWidth = Math.min(
                availableWidth - dp(40),
                dp(420)
        );

        int maxDialogHeight =
                Math.round(availableHeight * 0.80f);

        // Измеряем естественную высоту XML при заданной ширине.
        dialogContent.measure(
                View.MeasureSpec.makeMeasureSpec(
                        dialogWidth,
                        View.MeasureSpec.EXACTLY
                ),
                View.MeasureSpec.makeMeasureSpec(
                        0,
                        View.MeasureSpec.UNSPECIFIED
                )
        );

        boolean needsHeightLimit =
                dialogContent.getMeasuredHeight() > maxDialogHeight;

        window.setLayout(
                dialogWidth,
                needsHeightLimit
                        ? maxDialogHeight
                        : ViewGroup.LayoutParams.WRAP_CONTENT
        );
    }

    private int dp(float value) {
        return Math.round(
                value
                        * getResources()
                        .getDisplayMetrics()
                        .density
        );
    }

    @Override
    public void onDestroyView() {
        dialogContent = null;
        super.onDestroyView();
    }
}