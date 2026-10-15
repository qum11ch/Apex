package com.example.f1app;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ToggleButton;

import androidx.annotation.NonNull;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class favoritePickerDialog extends Dialog {

    public interface OnItemSelected {
        void selected(favoriteItem item);
    }

    private enum DriverFilter {
        ALL,
        ACTIVE,
        RESERVE,
        RETIRED,
        CHAMPIONS,
        LEGENDS
    }

    private enum CareerFilter {
        ALL,
        ACTIVE,
        RESERVE,
        RETIRED
    }

    private static class DriverFilterData {
        final String status;
        final int championships;
        final boolean legend;

        DriverFilterData(
                String status,
                int championships,
                boolean legend) {

            this.status = status;
            this.championships = championships;
            this.legend = legend;
        }
    }

    private final String title;
    private final List<favoriteItem> allItems;
    private final String currentValue;
    private final OnItemSelected listener;

    private final Map<String, DriverFilterData> driverData =
            new HashMap<>();

    private final Map<DriverFilter, ToggleButton> filterButtons =
            new HashMap<>();

    private ViewPager2 pager;
    private TextView pageIndicator;
    private TextView filterMessage;
    private LinearLayout filtersLayout;
    private View filtersScroll;

    private favoritePagerAdapter adapter;

    private DatabaseReference driversRef;
    private ValueEventListener driversListener;

    private ViewPager2.OnPageChangeCallback pageCallback;

    public favoritePickerDialog(
            @NonNull Context context,
            String title,
            List<favoriteItem> items,
            String currentValue,
            OnItemSelected listener) {

        super(context);

        this.title = title;
        this.allItems = buildOrderedItems(items, currentValue);
        this.currentValue = currentValue;
        this.listener = listener;
    }

    private CareerFilter selectedCareer = CareerFilter.ALL;
    private boolean championsOnly = false;
    private boolean legendsOnly = false;

    private EditText pickerSearch;
    private String searchQuery = "";
    private boolean driverPicker;

    private Button toggleFiltersButton;
    private boolean filtersExpanded = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.dialog_favorite_picker);

        TextView pickerTitle = findViewById(R.id.pickerTitle);
        Button cancel = findViewById(R.id.cancelPicker);

        pager = findViewById(R.id.favoritePager);
        pageIndicator = findViewById(R.id.pageIndicator);
        filterMessage = findViewById(R.id.filterMessage);

        filtersLayout = findViewById(R.id.driverFiltersLayout);
        filtersScroll = findViewById(R.id.driverFiltersScroll);

        toggleFiltersButton = findViewById(R.id.toggleFiltersButton);

        pickerTitle.setText(title);

        adapter = new favoritePagerAdapter(
                allItems,
                item -> {
                    listener.selected(item);
                    dismiss();
                }
        );

        pager.setAdapter(adapter);

        pageCallback = new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                updatePageIndicator();
            }
        };

        pager.registerOnPageChangeCallback(pageCallback);

        int initialPosition = findFirstRealItemPosition(allItems);

        pager.setCurrentItem(initialPosition, false);
        updatePageIndicator();

        cancel.setOnClickListener(view -> dismiss());

        driverPicker = isDriverPicker();

        pickerSearch = findViewById(R.id.pickerSearch);

        pickerSearch.setHint(
                driverPicker
                        ? R.string.favorite_search_driver_hint
                        : R.string.favorite_search_team_hint
        );


        pager.setFocusableInTouchMode(true);
        pager.requestFocus();

        setupSearch();

        if (driverPicker) {
            toggleFiltersButton.setVisibility(View.VISIBLE);

            filtersExpanded = false;

            setupFilterButtons();
            updateFiltersPanel();

            toggleFiltersButton.setOnClickListener(view -> {
                filtersExpanded = !filtersExpanded;
                updateFiltersPanel();
            });

            loadDriverFilterData();

        } else {
            toggleFiltersButton.setVisibility(View.GONE);
            filtersScroll.setVisibility(View.GONE);
        }

        Window window = getWindow();

        if (window != null) {
            window.setBackgroundDrawable(
                    new ColorDrawable(Color.TRANSPARENT)
            );

            window.setSoftInputMode(
                    WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN
                            | WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
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

    private void setupSearch() {
        pickerSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(
                    CharSequence text,
                    int start,
                    int count,
                    int after) {
            }

            @Override
            public void onTextChanged(
                    CharSequence text,
                    int start,
                    int before,
                    int count) {

                String newQuery = normalizeSearchText(
                        text != null ? text.toString() : ""
                );

                if (newQuery.equals(searchQuery)) {
                    return;
                }

                searchQuery = newQuery;
                applyFilter();
            }

            @Override
            public void afterTextChanged(Editable text) {
            }
        });
    }

    private String normalizeSearchText(String value) {
        if (value == null) {
            return "";
        }

        return Normalizer.normalize(
                        value,
                        Normalizer.Form.NFD
                )
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT)
                .replace('ё', 'е')
                .trim()
                .replaceAll("\\s+", " ");
    }

    private boolean matchesSearch(favoriteItem item) {
        if (searchQuery.isEmpty()) {
            return true;
        }

        String searchableText;

        if (item.team) {
            searchableText =
                    safeText(item.teamName)
                            + " "
                            + safeText(item.value);
        } else {
            searchableText =
                    safeText(item.driverName)
                            + " "
                            + safeText(item.familyName)
                            + " "
                            + safeText(item.value);
        }

        searchableText = normalizeSearchText(searchableText);

        for (String word : searchQuery.split(" ")) {
            if (!searchableText.contains(word)) {
                return false;
            }
        }

        return true;
    }

    private String safeText(String value) {
        return value != null ? value : "";
    }

    private boolean isDriverPicker() {
        for (favoriteItem item : allItems) {
            if (item.team) {
                return false;
            }
        }

        return !allItems.isEmpty();
    }

    private int findCurrentPosition() {
        for (int index = 0; index < allItems.size(); index++) {
            if (Objects.equals(
                    allItems.get(index).value,
                    currentValue)) {

                return index;
            }
        }

        return 0;
    }

    private void setupFilterButtons() {
        filtersLayout.removeAllViews();
        filterButtons.clear();

        addFilterButton(DriverFilter.ALL, "Все");
        addFilterButton(DriverFilter.ACTIVE, "Действующие");
        addFilterButton(DriverFilter.RESERVE, "Резервные");
        addFilterButton(DriverFilter.RETIRED, "Завершили карьеру");
        addFilterButton(DriverFilter.CHAMPIONS, "Чемпионы");
        addFilterButton(DriverFilter.LEGENDS, "Легенды");

        updateFilterButtons();
    }

    private void updateFiltersPanel() {
        if (toggleFiltersButton == null || filtersScroll == null) {
            return;
        }

        int activeCount = 0;

        if (selectedCareer != CareerFilter.ALL) {
            activeCount++;
        }

        if (championsOnly) {
            activeCount++;
        }

        if (legendsOnly) {
            activeCount++;
        }

        filtersScroll.setVisibility(
                filtersExpanded ? View.VISIBLE : View.GONE
        );

        String label;

        if (activeCount == 0) {
            label = getContext().getString(
                    filtersExpanded
                            ? R.string.picker_filters_expanded
                            : R.string.picker_filters_collapsed
            );
        } else {
            label = getContext().getString(
                    filtersExpanded
                            ? R.string.picker_filters_count_expanded
                            : R.string.picker_filters_count_collapsed,
                    activeCount
            );
        }

        toggleFiltersButton.setText(label);
    }

    private void addFilterButton(
            DriverFilter filter,
            String label) {

        ToggleButton button = (ToggleButton)
                LayoutInflater.from(getContext()).inflate(
                        R.layout.item_driver_filter_toggle,
                        filtersLayout,
                        false
                );

        button.setTextOn(label);
        button.setTextOff(label);
        button.setText(label);

        // Остальные фильтры включатся после загрузки данных.
        button.setEnabled(filter == DriverFilter.ALL);

        button.setOnClickListener(view -> {
            switch (filter) {
                case ALL:
                    selectedCareer = CareerFilter.ALL;
                    championsOnly = false;
                    legendsOnly = false;
                    break;

                case ACTIVE:
                    selectedCareer =
                            selectedCareer == CareerFilter.ACTIVE
                                    ? CareerFilter.ALL
                                    : CareerFilter.ACTIVE;
                    break;

                case RESERVE:
                    selectedCareer =
                            selectedCareer == CareerFilter.RESERVE
                                    ? CareerFilter.ALL
                                    : CareerFilter.RESERVE;
                    break;

                case RETIRED:
                    selectedCareer =
                            selectedCareer == CareerFilter.RETIRED
                                    ? CareerFilter.ALL
                                    : CareerFilter.RETIRED;
                    break;

                case CHAMPIONS:
                    championsOnly = !championsOnly;
                    break;

                case LEGENDS:
                    legendsOnly = !legendsOnly;
                    break;
            }

            updateFilterButtons();
            applyFilter();
        });

        filterButtons.put(filter, button);
        addFilterButtonToLayout(button);
    }

    private void updateFilterButtons() {
        for (Map.Entry<DriverFilter, ToggleButton> entry :
                filterButtons.entrySet()) {

            boolean checked;

            switch (entry.getKey()) {
                case ALL:
                    checked = selectedCareer == CareerFilter.ALL
                            && !championsOnly
                            && !legendsOnly;
                    break;

                case ACTIVE:
                    checked = selectedCareer == CareerFilter.ACTIVE;
                    break;

                case RESERVE:
                    checked = selectedCareer == CareerFilter.RESERVE;
                    break;

                case RETIRED:
                    checked = selectedCareer == CareerFilter.RETIRED;
                    break;

                case CHAMPIONS:
                    checked = championsOnly;
                    break;

                case LEGENDS:
                    checked = legendsOnly;
                    break;

                default:
                    checked = false;
                    break;
            }

            entry.getValue().setChecked(checked);

            updateFiltersPanel();
        }
    }

    private int findFirstRealItemPosition(List<favoriteItem> items) {
        for (int index = 0; index < items.size(); index++) {
            if (!items.get(index).empty) {
                return index;
            }
        }

        return 0;
    }

    private void loadDriverFilterData() {
        filterMessage.setText("Загружаем фильтры…");
        filterMessage.setVisibility(View.VISIBLE);

        driversRef = FirebaseDatabase.getInstance()
                .getReference()
                .child("drivers");

        driversListener = new ValueEventListener() {
            @Override
            public void onDataChange(
                    @NonNull DataSnapshot snapshot) {

                if (!isShowing()) {
                    return;
                }

                driverData.clear();

                for (DataSnapshot driverSnapshot :
                        snapshot.getChildren()) {

                    String fullName = driverSnapshot.getKey();

                    if (fullName == null) {
                        continue;
                    }

                    String status = driverSnapshot
                            .child("status")
                            .getValue(String.class);

                    int championships = parseChampionships(
                            driverSnapshot
                                    .child("championshipsCount")
                                    .getValue()
                    );

                    boolean legend = Boolean.TRUE.equals(
                            driverSnapshot
                                    .child("legendStatus")
                                    .getValue()
                    );

                    driverData.put(
                            fullName,
                            new DriverFilterData(
                                    status,
                                    championships,
                                    legend
                            )
                    );
                }

                for (ToggleButton button : filterButtons.values()) {
                    button.setEnabled(true);
                }

                if (!searchQuery.isEmpty()
                        || selectedCareer != CareerFilter.ALL
                        || championsOnly
                        || legendsOnly) {

                    applyFilter();
                } else {
                    filterMessage.setVisibility(View.GONE);
                }
            }

            @Override
            public void onCancelled(
                    @NonNull DatabaseError error) {

                if (!isShowing()) {
                    return;
                }

                filterMessage.setText(
                        "Не удалось загрузить фильтры. "
                                + "Доступен полный список."
                );

                filterMessage.setVisibility(View.VISIBLE);

                android.util.Log.e(
                        "FavoritePicker",
                        "Failed to load driver filters",
                        error.toException()
                );
            }
        };

        driversRef.addListenerForSingleValueEvent(driversListener);
    }

    private int parseChampionships(Object value) {
        if (value == null) {
            return 0;
        }

        if (value instanceof Number) {
            return ((Number) value).intValue();
        }

        try {
            return Integer.parseInt(value.toString().trim());
        } catch (NumberFormatException error) {
            return 0;
        }
    }

    private void applyFilter() {
        List<favoriteItem> filteredItems = new ArrayList<>();
        favoriteItem emptyItem = null;

        int matchingItems = 0;

        for (favoriteItem item : allItems) {
            if (item.empty) {
                emptyItem = item;
                continue;
            }

            boolean categoryMatches =
                    !driverPicker || matchesFilter(item);

            if (categoryMatches && matchesSearch(item)) {
                filteredItems.add(item);
                matchingItems++;
            }
        }

        if (searchQuery.isEmpty() && emptyItem != null) {
            filteredItems.add(0, emptyItem);
        }

        adapter.submitItems(filteredItems);

        if (!filteredItems.isEmpty()) {
            pager.setCurrentItem(
                    findFirstRealItemPosition(filteredItems),
                    false
            );
        }

        updatePageIndicator();

        if (matchingItems == 0) {
            filterMessage.setText(
                    driverPicker
                            ? R.string.favorite_no_drivers_found
                            : R.string.favorite_no_teams_found
            );

            filterMessage.setVisibility(View.VISIBLE);
        } else {
            filterMessage.setVisibility(View.GONE);
        }
    }

    private List<favoriteItem> buildOrderedItems(
            List<favoriteItem> source,
            String currentValue) {

        favoriteItem emptyItem = null;
        favoriteItem selectedItem = null;

        List<favoriteItem> remainingItems = new ArrayList<>();

        boolean hasCurrentValue =
                currentValue != null
                        && !currentValue.trim().isEmpty()
                        && !"null".equalsIgnoreCase(currentValue.trim());

        for (favoriteItem item : source) {
            if (item.empty) {
                if (emptyItem == null) {
                    emptyItem = item;
                }

                continue;
            }

            if (hasCurrentValue
                    && Objects.equals(item.value, currentValue)) {

                if (selectedItem == null) {
                    selectedItem = item;
                }

                continue;
            }

            remainingItems.add(item);
        }

        List<favoriteItem> orderedItems = new ArrayList<>();

        if (emptyItem != null) {
            orderedItems.add(emptyItem);
        }

        if (selectedItem != null) {
            orderedItems.add(selectedItem);
        }

        orderedItems.addAll(remainingItems);

        return orderedItems;
    }

    private boolean matchesFilter(favoriteItem item) {
        DriverFilterData data = driverData.get(item.value);

        if (data == null) {
            return selectedCareer == CareerFilter.ALL
                    && !championsOnly
                    && !legendsOnly;
        }

        boolean careerMatches;

        switch (selectedCareer) {
            case ACTIVE:
                careerMatches = "active".equals(data.status);
                break;

            case RESERVE:
                careerMatches = "reserve".equals(data.status);
                break;

            case RETIRED:
                careerMatches = "retired".equals(data.status);
                break;

            default:
                careerMatches = true;
                break;
        }

        return careerMatches
                && (!championsOnly || data.championships > 0)
                && (!legendsOnly || data.legend);
    }

    private void updatePageIndicator() {
        int total = adapter.getItemCount();

        int current = total == 0
                ? 0
                : Math.min(pager.getCurrentItem() + 1, total);

        pageIndicator.setText(current + " / " + total);
    }

    private void addFilterButtonToLayout(ToggleButton button) {
        LinearLayout row = null;

        int rowCount = filtersLayout.getChildCount();

        if (rowCount > 0) {
            LinearLayout lastRow = (LinearLayout)
                    filtersLayout.getChildAt(rowCount - 1);

            if (lastRow.getChildCount() < 2) {
                row = lastRow;
            }
        }

        if (row == null) {
            row = new LinearLayout(getContext());
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(android.view.Gravity.CENTER_VERTICAL);
            row.setBaselineAligned(false);

            row.setLayoutParams(
                    new LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                    )
            );

            filtersLayout.addView(row);
        }

        int gap = Math.round(
                4 * getContext().getResources()
                        .getDisplayMetrics().density
        );

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        0,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        1f
                );

        params.setMargins(gap, gap, gap, gap);
        button.setLayoutParams(params);

        row.addView(button);
    }

    @Override
    public void dismiss() {
        if (driversRef != null && driversListener != null) {
            driversRef.removeEventListener(driversListener);
            driversListener = null;
        }

        if (pager != null && pageCallback != null) {
            pager.unregisterOnPageChangeCallback(pageCallback);
            pageCallback = null;
        }

        super.dismiss();
    }
}