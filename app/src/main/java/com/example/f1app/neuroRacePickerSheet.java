package com.example.f1app;

import static android.view.View.OVER_SCROLL_NEVER;

import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatEditText;
import androidx.core.view.ViewCompat;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.util.ArrayList;
import java.util.Locale;

public class neuroRacePickerSheet extends neuroBaseSheet {

    public static final String RESULT_KEY = "neuro_race_selected";
    public static final String INDEX = "index";

    private static final String ARG_LABELS = "labels";
    private static final String STATE_QUERY = "search_query";

    private final ArrayList<String> labels = new ArrayList<>();

    private final ArrayList<Integer> visibleIndices = new ArrayList<>();

    private String query = "";
    private int selectedIndex = -1;

    public static neuroRacePickerSheet create(
            ArrayList<String> labels,
            int selected
    ) {
        neuroRacePickerSheet sheet = new neuroRacePickerSheet();

        Bundle args = new Bundle();
        args.putStringArrayList(
                ARG_LABELS,
                new ArrayList<>(labels)
        );
        args.putInt(INDEX, selected);

        sheet.setArguments(args);
        return sheet;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ArrayList<String> incoming =
                requireArguments().getStringArrayList(ARG_LABELS);

        labels.clear();

        if (incoming != null) {
            labels.addAll(incoming);
        }

        selectedIndex = requireArguments().getInt(INDEX, -1);

        if (savedInstanceState != null) {
            query = savedInstanceState.getString(STATE_QUERY, "");
        }
    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        LinearLayout root = root();

        root.setFocusableInTouchMode(true);

        TextView title = text(
                getString(R.string.neuro_design_choose),
                22,
                R.color.text_color_main
        );

        title.setTypeface(
                title.getTypeface(),
                Typeface.BOLD
        );

        root.addView(title);

        // Поле поиска.
        AppCompatEditText search =
                new AppCompatEditText(requireContext());

        search.setSingleLine(true);
        search.setTextSize(16);
        search.setTextColor(color(R.color.text_color_main));
        search.setHintTextColor(color(R.color.text_color_secondary));
        search.setHint(R.string.neuro_search_hint);

        search.setInputType(
                InputType.TYPE_CLASS_TEXT
                        | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
        );

        search.setImeOptions(
                EditorInfo.IME_ACTION_DONE
                        | EditorInfo.IME_FLAG_NO_EXTRACT_UI
        );

        // Общий фон поля и кнопки.
        LinearLayout searchContainer =
                new LinearLayout(requireContext());

        searchContainer.setOrientation(LinearLayout.HORIZONTAL);
        searchContainer.setGravity(Gravity.CENTER_VERTICAL);

        searchContainer.setBackground(
                surface(
                        color(R.color.account_settings_input_color),
                        12
                )
        );

// Сам EditText без отдельной заливки.
        search.setMinHeight(dp(52));

        ViewCompat.setBackgroundTintList(search, null);
        search.setBackground(null);

        search.setPadding(
                dp(16),
                dp(12),
                dp(4),
                dp(12)
        );

        searchContainer.addView(
                search,
                new LinearLayout.LayoutParams(
                        0,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        1f
                )
        );

        androidx.appcompat.widget.AppCompatImageButton clearSearch =
                new androidx.appcompat.widget.AppCompatImageButton(
                        requireContext()
                );

        clearSearch.setImageResource(R.drawable.search_cleaner);

        clearSearch.setContentDescription(
                getString(R.string.neuro_search_clear)
        );

        clearSearch.setPadding(
                dp(12),
                dp(12),
                dp(12),
                dp(12)
        );

        android.util.TypedValue selectable =
                new android.util.TypedValue();

        if (requireContext().getTheme().resolveAttribute(
                android.R.attr.selectableItemBackgroundBorderless,
                selectable,
                true
        ) && selectable.resourceId != 0) {
            clearSearch.setBackgroundResource(selectable.resourceId);
        } else {
            clearSearch.setBackgroundColor(
                    android.graphics.Color.TRANSPARENT
            );
        }

        clearSearch.setVisibility(View.INVISIBLE);

        LinearLayout.LayoutParams clearParams =
                new LinearLayout.LayoutParams(
                        dp(48),
                        dp(48)
                );

        clearParams.setMarginEnd(dp(4));

        searchContainer.addView(clearSearch, clearParams);

        clearSearch.setOnClickListener(v -> {
            search.setText("");
            search.requestFocus();
        });

        LinearLayout.LayoutParams searchParams =
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );

        searchParams.topMargin = dp(18);
        searchParams.bottomMargin = dp(12);

        root.addView(searchContainer, searchParams);

        FrameLayout resultsContainer =
                new FrameLayout(requireContext());

        int screenHeight =
                getResources().getDisplayMetrics().heightPixels;

        int resultsHeight = Math.min(
                dp(360),
                Math.round(screenHeight * 0.40f)
        );

        root.addView(
                resultsContainer,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        resultsHeight
                )
        );

        ListView list = new ListView(requireContext());
        ViewCompat.setNestedScrollingEnabled(list, true);

        list.setDivider(null);
        list.setDividerHeight(0);
        list.setOverScrollMode(OVER_SCROLL_NEVER);

        list.setOnScrollListener(new android.widget.AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(
                    android.widget.AbsListView view,
                    int scrollState
            ) {
                if (scrollState
                        == android.widget.AbsListView.OnScrollListener
                        .SCROLL_STATE_TOUCH_SCROLL) {

                    android.view.inputmethod.InputMethodManager keyboard =
                            (android.view.inputmethod.InputMethodManager)
                                    view.getContext().getSystemService(
                                            android.content.Context.INPUT_METHOD_SERVICE
                                    );

                    if (keyboard != null) {
                        keyboard.hideSoftInputFromWindow(
                                search.getWindowToken(),
                                0
                        );
                    }

                    search.clearFocus();
                }
            }

            @Override
            public void onScroll(
                    android.widget.AbsListView view,
                    int firstVisibleItem,
                    int visibleItemCount,
                    int totalItemCount
            ) {
            }
        });

        list.setVerticalScrollBarEnabled(false);

        resultsContainer.addView(
                list,
                new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                )
        );

        TextView empty = text(
                getString(R.string.neuro_search_empty),
                16,
                R.color.text_color_secondary
        );

        empty.setGravity(Gravity.CENTER);

        empty.setPadding(
                dp(16),
                dp(24),
                dp(16),
                dp(24)
        );

        resultsContainer.addView(
                empty,
                new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                )
        );

        list.setEmptyView(empty);

        RaceAdapter adapter = new RaceAdapter();

        filter(query);
        list.setAdapter(adapter);

        list.setOnItemClickListener(
                (parent, view, position, id) -> {
                    if (position < 0
                            || position >= visibleIndices.size()) {
                        return;
                    }

                    // Возвращаем индекс из полного списка,
                    // а не позицию в отфильтрованном.
                    int originalIndex =
                            visibleIndices.get(position);

                    Bundle result = new Bundle();
                    result.putInt(INDEX, originalIndex);

                    getParentFragmentManager()
                            .setFragmentResult(
                                    RESULT_KEY,
                                    result
                            );

                    dismiss();
                }
        );

        search.setText(query);
        search.setSelection(search.length());

        clearSearch.setVisibility(
                search.length() > 0
                        ? View.VISIBLE
                        : View.INVISIBLE
        );

        search.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(
                    CharSequence s,
                    int start,
                    int count,
                    int after
            ) {
            }

            @Override
            public void onTextChanged(
                    CharSequence s,
                    int start,
                    int before,
                    int count
            ) {
                query = s.toString();

                clearSearch.setVisibility(
                        s.length() > 0
                                ? View.VISIBLE
                                : View.INVISIBLE
                );

                filter(query);
                adapter.notifyDataSetChanged();

                list.setSelection(0);
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        // При открытии показываем выбранный этап.
        if (query.isEmpty()) {
            int selectedPosition =
                    visibleIndices.indexOf(selectedIndex);

            if (selectedPosition >= 0) {
                list.setSelection(selectedPosition);
            }
        }

        root.requestFocus();

        return root;
    }

    private void filter(String value) {
        visibleIndices.clear();

        String normalizedQuery = normalize(value);

        String[] words = normalizedQuery.isEmpty()
                ? new String[0]
                : normalizedQuery.split("\\s+");

        for (int i = 0; i < labels.size(); i++) {
            String normalizedLabel =
                    normalize(labels.get(i));

            boolean matches = true;

            for (String word : words) {
                if (!normalizedLabel.contains(word)) {
                    matches = false;
                    break;
                }
            }

            if (matches) {
                visibleIndices.add(i);
            }
        }
    }

    private String normalize(String value) {
        return value
                .toLowerCase(Locale.ROOT)
                .replace('ё', 'е')
                .replace('-', ' ')
                .replace('–', ' ')
                .replace('—', ' ')
                .trim();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putString(STATE_QUERY, query);
        super.onSaveInstanceState(outState);
    }

    private class RaceAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return visibleIndices.size();
        }

        @Override
        public Object getItem(int position) {
            return labels.get(
                    visibleIndices.get(position)
            );
        }

        @Override
        public long getItemId(int position) {
            return visibleIndices.get(position);
        }

        @Override
        public View getView(
                int position,
                View convertView,
                ViewGroup parent
        ) {
            LinearLayout container;
            TextView row;
            View divider;

            if (convertView instanceof LinearLayout) {
                container = (LinearLayout) convertView;
                row = (TextView) container.getChildAt(0);
                divider = container.getChildAt(1);
            } else {
                container = new LinearLayout(requireContext());
                container.setOrientation(LinearLayout.VERTICAL);

                row = text(
                        "",
                        16,
                        R.color.text_color_main
                );

                row.setMinHeight(dp(60));
                row.setGravity(Gravity.CENTER_VERTICAL);

                row.setPadding(
                        dp(12),
                        dp(14),
                        dp(12),
                        dp(14)
                );

                row.setCompoundDrawablePadding(dp(12));

                container.addView(
                        row,
                        new LinearLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.WRAP_CONTENT
                        )
                );

                divider = new View(requireContext());

                divider.setBackgroundColor(
                        color(R.color.account_settings_input_color)
                );

                container.addView(
                        divider,
                        new LinearLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                dp(1)
                        )
                );
            }

            int originalIndex = visibleIndices.get(position);
            boolean selected = originalIndex == selectedIndex;

            row.setText(labels.get(originalIndex));

            row.setCompoundDrawablesRelativeWithIntrinsicBounds(
                    0,
                    0,
                    selected ? R.drawable.neuro_check : 0,
                    0
            );

            row.setBackground(
                    surface(
                            color(
                                    selected
                                            ? R.color.neuro_selected
                                            : R.color.colorPrimary
                            ),
                            8
                    )
            );

            boolean lastRow =
                    position == visibleIndices.size() - 1;

            boolean nextRowSelected =
                    !lastRow
                            && visibleIndices.get(position + 1)
                            == selectedIndex;

            // Линия не нужна:
            // — под выбранным пунктом;
            // — перед выбранным пунктом;
            // — в самом конце списка.
            boolean showDivider =
                    !selected
                            && !nextRowSelected
                            && !lastRow;

            divider.setVisibility(
                    showDivider ? View.VISIBLE : View.INVISIBLE
            );

            return container;
        }
    }

    @Override
    public void onStart() {
        super.onStart();

        if (!(getDialog() instanceof BottomSheetDialog)) {
            return;
        }

        BottomSheetDialog dialog =
                (BottomSheetDialog) requireDialog();

        View sheet = dialog.findViewById(
                com.google.android.material.R.id.design_bottom_sheet
        );

        if (sheet == null) return;

        BottomSheetBehavior<View> behavior =
                BottomSheetBehavior.from(sheet);

        behavior.setDraggable(true);
        behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
    }
}