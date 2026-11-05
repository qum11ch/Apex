package com.example.f1app;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.Glide;
import com.google.firebase.storage.FirebaseStorage;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public final class predictionParticipantPickerDialog extends Dialog {
    public interface OnItemSelected {
        void selected(favoriteItem item);
    }

    private final List<favoriteItem> source = new ArrayList<>();
    private final boolean team;
    private final OnItemSelected listener;
    private final List<favoriteItem> visible = new ArrayList<>();

    private ViewPager2 pager;
    private TextView counter;
    private TextView empty;
    private ParticipantAdapter adapter;
    private ViewPager2.OnPageChangeCallback pageCallback;
    private ScrollView scroll;
    private boolean selectionDelivered;

    public predictionParticipantPickerDialog(Context context, String title, List<favoriteItem> items,
                                             String currentValue, OnItemSelected listener) {
        super(context);
        this.listener = listener;

        favoriteItem current = null;

        for (favoriteItem item : items) {
            if (item == null || item.empty) {
                continue;
            }

            if (Objects.equals(item.value, currentValue)) {
                current = item;
            } else {
                source.add(item);
            }
        }

        if (current != null) {
            source.add(0, current);
        }

        team = !source.isEmpty() && source.get(0).team;
        visible.addAll(source);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        Context context = getContext();
        scroll = new ScrollView(context);
        scroll.setFillViewport(false);
        scroll.setClipToPadding(false);

        LinearLayout content = new LinearLayout(context);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(20), dp(20), dp(20), dp(16));
        content.setBackground(background(R.color.colorPrimary, 20, false));
        scroll.addView(content);
        setContentView(scroll);

        TextView heading = text(18, R.color.text_color_main);
        heading.setTypeface(null, Typeface.BOLD);
        heading.setText(team ? R.string.pp_choose_team : R.string.pp_choose_driver);
        content.addView(heading, params(-1, -2, 0));

        EditText search = new EditText(context);
        search.setSingleLine(true);
        search.setTextSize(16);
        search.setTextColor(color(R.color.text_color_main));
        search.setHintTextColor(color(R.color.text_color_secondary));
        search.setHint(team ? R.string.pp_search_team : R.string.pp_search_driver);
        search.setPadding(dp(14), dp(8), dp(14), dp(8));
        search.setBackground(background(R.color.colorPrimary, 12, true));
        search.setInputType(android.text.InputType.TYPE_CLASS_TEXT);
        search.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_DONE);
        content.addView(search, params(-1, dp(52), 16));

        empty = text(14, R.color.text_color_secondary);
        empty.setText(R.string.pp_empty);
        empty.setGravity(Gravity.CENTER);
        empty.setVisibility(View.GONE);
        content.addView(empty, params(-1, -2, 16));

        pager = new ViewPager2(context);
        int screenHeight = context.getResources().getDisplayMetrics().heightPixels;
        int pagerHeight = Math.max(dp(180), Math.min(dp(270), (int) (screenHeight * 0.32f)));
        content.addView(pager, params(-1, pagerHeight, 12));

        adapter = new ParticipantAdapter();
        pager.setAdapter(adapter);
        pager.setOffscreenPageLimit(1);

        counter = text(13, R.color.text_color_secondary);
        counter.setGravity(Gravity.CENTER);
        content.addView(counter, params(-1, -2, 10));

        pageCallback = new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                updateCounter();
            }
        };
        pager.registerOnPageChangeCallback(pageCallback);

        Button cancel = new androidx.appcompat.widget.AppCompatButton(context);
        cancel.setText(R.string.pp_cancel);
        cancel.setAllCaps(false);
        cancel.setTextSize(15);
        cancel.setTextColor(color(R.color.text_color_main));
        cancel.setBackground(background(R.color.colorPrimary, 12, true));
        cancel.setOnClickListener(view -> dismiss());
        content.addView(cancel, params(-1, dp(48), 16));

        search.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence value, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence value, int start, int before, int count) {
                filter(value == null ? "" : value.toString());
            }

            @Override
            public void afterTextChanged(Editable value) {
            }
        });

        pager.setFocusableInTouchMode(true);
        pager.requestFocus();
        updateCounter();

        Window window = getWindow();

        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN
                    | WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        Window window = getWindow();

        if (window != null) {
            int width = (int) (getContext().getResources().getDisplayMetrics().widthPixels * 0.92f);
            window.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }

    private void filter(String query) {
        String normalized = normalize(query);
        visible.clear();

        for (favoriteItem item : source) {
            String haystack = normalize(item.driverName + " " + item.familyName
                    + " " + item.teamName + " " + item.value);
            boolean matches = true;

            for (String word : normalized.split("\\s+")) {
                if (!haystack.contains(word)) {
                    matches = false;
                    break;
                }
            }

            if (matches) {
                visible.add(item);
            }
        }

        adapter.notifyDataSetChanged();
        pager.setVisibility(visible.isEmpty() ? View.GONE : View.VISIBLE);
        empty.setVisibility(visible.isEmpty() ? View.VISIBLE : View.GONE);

        if (!visible.isEmpty()) {
            pager.setCurrentItem(0, false);
        }

        updateCounter();
    }

    private static String normalize(String value) {
        return Normalizer.normalize(value == null ? "" : value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT)
                .replace('ё', 'е')
                .trim();
    }

    private void updateCounter() {
        int total = visible.size();
        int current = total == 0 ? 0 : Math.min(pager.getCurrentItem() + 1, total);
        counter.setText(getContext().getString(R.string.pp_page, current, total));
    }

    private final class ParticipantAdapter extends RecyclerView.Adapter<ParticipantHolder> {
        @NonNull
        @Override
        public ParticipantHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LinearLayout card = new LinearLayout(parent.getContext());
            card.setOrientation(LinearLayout.VERTICAL);
            card.setGravity(Gravity.CENTER);
            card.setPadding(dp(16), dp(12), dp(16), dp(12));
            card.setBackground(background(R.color.colorPredicts, 14, false));
            card.setLayoutParams(new ViewGroup.LayoutParams(-1, -1));
            card.setFocusable(true);
            card.setClickable(true);

            ImageView image = new ImageView(parent.getContext());
            image.setScaleType(ImageView.ScaleType.FIT_CENTER);
            image.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
            card.addView(image, new LinearLayout.LayoutParams(-1, 0, 1f));

            TextView name = text(21, R.color.text_color_main);
            name.setGravity(Gravity.CENTER);
            name.setTypeface(null, Typeface.BOLD);
            card.addView(name, params(-1, -2, 10));

            TextView action = text(14, R.color.text_color_secondary);
            action.setText(R.string.pp_select);
            action.setGravity(Gravity.CENTER);
            card.addView(action, params(-1, -2, 6));

            return new ParticipantHolder(card, image, name);
        }

        @Override
        public void onBindViewHolder(@NonNull ParticipantHolder holder, int position) {
            favoriteItem item = visible.get(position);
            String label = item.team ? item.teamName : item.driverName + " " + item.familyName;
            holder.name.setText(label.trim());
            holder.itemView.setContentDescription(label.trim() + ". "
                    + getContext().getString(R.string.pp_select));
            holder.bound = item;
            Glide.with(getContext()).clear(holder.image);
            holder.image.setImageResource(R.drawable.placeholder_driver);

            if (item.imagePath != null) {
                FirebaseStorage.getInstance().getReference().child(item.imagePath).getDownloadUrl()
                        .addOnSuccessListener(uri -> {
                            if (!isShowing() || holder.bound != item) {
                                return;
                            }

                            Glide.with(getContext()).load(uri)
                                    .placeholder(R.drawable.placeholder_driver)
                                    .error(R.drawable.placeholder_driver)
                                    .into(holder.image);
                        });
            }

            holder.itemView.setOnClickListener(view -> {
                if (selectionDelivered || !isShowing()) {
                    return;
                }

                selectionDelivered = true;
                dismiss();
                listener.selected(item);
            });
        }

        @Override
        public int getItemCount() {
            return visible.size();
        }

        @Override
        public void onViewRecycled(@NonNull ParticipantHolder holder) {
            holder.bound = null;
            Glide.with(getContext()).clear(holder.image);
            super.onViewRecycled(holder);
        }
    }

    private static final class ParticipantHolder extends RecyclerView.ViewHolder {
        final ImageView image;
        final TextView name;
        favoriteItem bound;

        ParticipantHolder(View view, ImageView image, TextView name) {
            super(view);
            this.image = image;
            this.name = name;
        }
    }

    private TextView text(int size, int colorId) {
        TextView view = new TextView(getContext());
        view.setTextSize(size);
        view.setTextColor(color(colorId));
        return view;
    }

    private GradientDrawable background(int fill, int radius, boolean stroke) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color(fill));
        drawable.setCornerRadius(dp(radius));

        if (stroke) {
            drawable.setStroke(dp(1), color(R.color.stroke_option_color));
        }

        return drawable;
    }

    private LinearLayout.LayoutParams params(int width, int height, int top) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(width, height);
        params.topMargin = dp(top);
        return params;
    }

    private int color(int resource) {
        return ContextCompat.getColor(getContext(), resource);
    }

    private int dp(int value) {
        return Math.round(value * getContext().getResources().getDisplayMetrics().density);
    }

    @Override
    public void dismiss() {
        if (pager != null && pageCallback != null) {
            pager.unregisterOnPageChangeCallback(pageCallback);
            pageCallback = null;
        }

        if (pager != null) {
            pager.setAdapter(null);
        }

        super.dismiss();
    }
}
