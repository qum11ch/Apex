package com.example.f1app;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.viewpager2.widget.ViewPager2;

import java.util.List;

public class favoritePickerDialog extends Dialog {

    public interface OnItemSelected {
        void selected(favoriteItem item);
    }

    private final String title;
    private final List<favoriteItem> items;
    private final String currentValue;
    private final OnItemSelected listener;

    public favoritePickerDialog(
            @NonNull Context context,
            String title,
            List<favoriteItem> items,
            String currentValue,
            OnItemSelected listener) {

        super(context);
        this.title = title;
        this.items = items;
        this.currentValue = currentValue;
        this.listener = listener;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.dialog_favorite_picker);

        TextView pickerTitle = findViewById(R.id.pickerTitle);
        TextView pageIndicator = findViewById(R.id.pageIndicator);
        ViewPager2 pager = findViewById(R.id.favoritePager);
        Button cancel = findViewById(R.id.cancelPicker);

        pickerTitle.setText(title);

        favoritePagerAdapter adapter =
                new favoritePagerAdapter(items, item -> {
                    listener.selected(item);
                    dismiss();
                });

        pager.setAdapter(adapter);

        int totalItems = items.size();
        int initialPosition = 0;

        for (int i = 0; i < totalItems; i++) {
            String value = items.get(i).value;

            if (value != null && value.equals(currentValue)) {
                initialPosition = i;
                break;
            }
        }

        pager.setCurrentItem(initialPosition, false);

        String pageIndicatorText = (initialPosition + 1) + " / " + totalItems;

        pageIndicator.setText(
                pageIndicatorText
        );

        pager.registerOnPageChangeCallback(
                new ViewPager2.OnPageChangeCallback() {
                    @Override
                    public void onPageSelected(int position) {
                        String pageIndicatorText = (position + 1) + " / " + totalItems;

                        pageIndicator.setText(pageIndicatorText);
                    }
                }
        );

        cancel.setOnClickListener(v -> dismiss());

        Window window = getWindow();
        if (window != null) {
            int width = (int) (getContext().getResources()
                    .getDisplayMetrics().widthPixels * 0.92f);

            window.setBackgroundDrawable(
                    new android.graphics.drawable.ColorDrawable(
                            android.graphics.Color.TRANSPARENT
                    )
            );

            window.getDecorView().setPadding(0, 0, 0, 0);
            window.setLayout(
                    width,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
        }
    }
}