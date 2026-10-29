package com.example.f1app;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.skydoves.colorpickerview.ColorPickerView;
import com.skydoves.colorpickerview.listeners.ColorEnvelopeListener;
import com.skydoves.colorpickerview.sliders.BrightnessSlideBar;

import java.util.Locale;

public class backgroundColorPickerDialog extends Dialog {

    public interface OnColorAppliedListener {
        void onColorApplied(int color);
    }

    private final int initialColor;
    private final OnColorAppliedListener listener;

    private int pendingColor;

    private View preview;
    private TextView hexText;
    private GradientDrawable previewBackground;

    private Button cancelButton;
    private Button applyButton;
    private boolean saving = false;

    public backgroundColorPickerDialog(
            @NonNull Context context,
            int initialColor,
            OnColorAppliedListener listener) {

        super(context);

        // Для фона используем только непрозрачные цвета.
        this.initialColor = makeOpaque(initialColor);
        this.pendingColor = this.initialColor;
        this.listener = listener;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.dialog_background_color_picker);

        ColorPickerView colorPicker =
                findViewById(R.id.backgroundColorPicker);

        BrightnessSlideBar brightnessSlider =
                findViewById(R.id.backgroundBrightnessSlider);

        preview = findViewById(R.id.backgroundColorPreview);
        hexText = findViewById(R.id.backgroundColorHex);

        cancelButton = findViewById(R.id.cancelBackgroundColor);
        applyButton = findViewById(R.id.applyBackgroundColor);

        previewBackground = new GradientDrawable();
        previewBackground.setCornerRadius(dp(12));
        previewBackground.setStroke(dp(1), Color.GRAY);

        preview.setBackground(previewBackground);

        updatePreview(initialColor);

        colorPicker.attachBrightnessSlider(brightnessSlider);

        colorPicker.setColorListener(
                (ColorEnvelopeListener) (envelope, fromUser) -> {
                    pendingColor = makeOpaque(envelope.getColor());
                    updatePreview(pendingColor);
                }
        );

        colorPicker.setInitialColor(initialColor);

        cancelButton.setOnClickListener(view -> {
            if (!saving) {
                dismiss();
            }
        });

        applyButton.setOnClickListener(view -> {
            if (saving || listener == null) {
                return;
            }

            listener.onColorApplied(pendingColor);
        });

        Window window = getWindow();

        if (window != null) {
            window.setBackgroundDrawable(
                    new ColorDrawable(Color.TRANSPARENT)
            );

            window.getDecorView().setPadding(0, 0, 0, 0);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        Window window = getWindow();

        if (window != null) {
            int width = (int) (
                    getContext().getResources()
                            .getDisplayMetrics().widthPixels * 0.92f
            );

            window.setLayout(
                    width,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
        }
    }

    private void updatePreview(int color) {
        previewBackground.setColor(color);

        hexText.setText(
                String.format(
                        Locale.ROOT,
                        "#%06X",
                        color & 0xFFFFFF
                )
        );
    }

    private static int makeOpaque(int color) {
        return Color.rgb(
                Color.red(color),
                Color.green(color),
                Color.blue(color)
        );
    }

    private int dp(float value) {
        return Math.round(
                value * getContext().getResources()
                        .getDisplayMetrics().density
        );
    }

    public void setSaving(boolean saving) {
        this.saving = saving;

        applyButton.setEnabled(!saving);
        cancelButton.setEnabled(!saving);

        setCancelable(!saving);
        setCanceledOnTouchOutside(!saving);

        applyButton.setText(
                saving
                        ? R.string.saving_color
                        : R.string.apply_color
        );
    }
}