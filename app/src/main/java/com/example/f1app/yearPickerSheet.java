package com.example.f1app;

import android.content.Context;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatEditText;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

/** A grid of seasons with a search by year: a tap returns the year under the request key. */
public class yearPickerSheet extends neuroBaseSheet {

    static final String YEAR = "year";

    private static final String TAG = "year_picker";
    private static final String ARG_YEARS = "years";
    private static final String ARG_SELECTED = "selected";
    private static final String ARG_KEY = "key";
    private static final String STATE_QUERY = "query";

    private final ArrayList<Integer> years = new ArrayList<>();
    private final ArrayList<Integer> visible = new ArrayList<>();

    private YearAdapter adapter;
    private AppCompatEditText search;
    private String key = "";
    private int selected = -1;
    private String query = "";

    static void show(FragmentManager manager, ArrayList<Integer> years, int selected, String requestKey) {
        if (manager.findFragmentByTag(TAG) != null || manager.isStateSaved()) {
            return;
        }

        yearPickerSheet sheet = new yearPickerSheet();

        Bundle args = new Bundle();
        args.putIntegerArrayList(ARG_YEARS, years);
        args.putInt(ARG_SELECTED, selected);
        args.putString(ARG_KEY, requestKey);

        sheet.setArguments(args);
        sheet.show(manager, TAG);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ArrayList<Integer> incoming = requireArguments().getIntegerArrayList(ARG_YEARS);

        years.clear();

        if (incoming != null) {
            years.addAll(incoming);
        }

        selected = requireArguments().getInt(ARG_SELECTED, -1);
        key = requireArguments().getString(ARG_KEY, "");

        if (savedInstanceState != null) {
            query = savedInstanceState.getString(STATE_QUERY, "");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle state) {
        LinearLayout root = root();

        TextView title = text(getString(R.string.year_picker_title), 22, R.color.text_color_main);
        title.setTypeface(title.getTypeface(), Typeface.BOLD);
        root.addView(title);

        // ---- the search field, as in the race picker ----
        search = new AppCompatEditText(requireContext());
        search.setSingleLine(true);
        search.setTextSize(16);
        search.setTextColor(color(R.color.text_color_main));
        search.setHintTextColor(color(R.color.text_color_secondary));
        search.setHint(R.string.year_search_hint);
        search.setInputType(InputType.TYPE_CLASS_NUMBER);
        search.setFilters(new InputFilter[]{new InputFilter.LengthFilter(4)});
        search.setImeOptions(EditorInfo.IME_ACTION_DONE | EditorInfo.IME_FLAG_NO_EXTRACT_UI);
        search.setMinHeight(dp(52));
        ViewCompat.setBackgroundTintList(search, null);
        search.setBackground(null);
        search.setPadding(dp(16), dp(12), dp(4), dp(12));

        LinearLayout searchContainer = new LinearLayout(requireContext());
        searchContainer.setOrientation(LinearLayout.HORIZONTAL);
        searchContainer.setGravity(Gravity.CENTER_VERTICAL);
        searchContainer.setBackground(surface(color(R.color.account_settings_input_color), 12));

        searchContainer.addView(search, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        AppCompatImageButton clear = new AppCompatImageButton(requireContext());
        clear.setImageResource(R.drawable.search_cleaner);
        clear.setContentDescription(getString(R.string.neuro_search_clear));
        clear.setPadding(dp(12), dp(12), dp(12), dp(12));

        TypedValue selectable = new TypedValue();

        if (requireContext().getTheme().resolveAttribute(
                android.R.attr.selectableItemBackgroundBorderless, selectable, true)
                && selectable.resourceId != 0) {
            clear.setBackgroundResource(selectable.resourceId);
        } else {
            clear.setBackgroundColor(android.graphics.Color.TRANSPARENT);
        }

        clear.setVisibility(View.INVISIBLE);

        LinearLayout.LayoutParams clearParams = new LinearLayout.LayoutParams(dp(48), dp(48));
        clearParams.setMarginEnd(dp(4));
        searchContainer.addView(clear, clearParams);

        LinearLayout.LayoutParams searchParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        searchParams.topMargin = dp(16);
        searchParams.bottomMargin = dp(8);

        root.addView(searchContainer, searchParams);

        // ---- the grid of years ----
        FrameLayout results = new FrameLayout(requireContext());

        int height = Math.min(dp(380), Math.round(getResources().getDisplayMetrics().heightPixels * 0.45f));

        root.addView(results, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, height));

        RecyclerView grid = new RecyclerView(requireContext());
        grid.setLayoutManager(new GridLayoutManager(requireContext(), 4));
        grid.setOverScrollMode(View.OVER_SCROLL_NEVER);

        results.addView(grid, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        TextView empty = text(getString(R.string.year_search_empty), 16, R.color.text_color_secondary);
        empty.setGravity(Gravity.CENTER);
        empty.setVisibility(View.GONE);

        results.addView(empty, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        adapter = new YearAdapter();
        grid.setAdapter(adapter);

        filter(query);
        empty.setVisibility(visible.isEmpty() ? View.VISIBLE : View.GONE);

        grid.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                if (newState == RecyclerView.SCROLL_STATE_DRAGGING) {
                    hideKeyboard();
                    search.clearFocus();
                }
            }
        });

        search.setText(query);
        search.setSelection(search.length());
        clear.setVisibility(search.length() > 0 ? View.VISIBLE : View.INVISIBLE);

        search.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                query = s.toString();

                clear.setVisibility(s.length() > 0 ? View.VISIBLE : View.INVISIBLE);

                filter(query);
                adapter.notifyDataSetChanged();
                grid.scrollToPosition(0);

                empty.setVisibility(visible.isEmpty() ? View.VISIBLE : View.GONE);
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        clear.setOnClickListener(v -> {
            search.setText("");
            search.requestFocus();
        });

        // "Done": the exact year, or the only one left
        search.setOnEditorActionListener((view, actionId, event) -> {
            if (actionId != EditorInfo.IME_ACTION_DONE) {
                return false;
            }

            Integer match = bestMatch();

            if (match != null) {
                pick(match);
            } else {
                hideKeyboard();
                search.clearFocus();
            }

            return true;
        });

        // the selected season is in sight when nothing is typed
        if (query.isEmpty()) {
            int position = visible.indexOf(selected);

            if (position > 4) {
                grid.scrollToPosition(position);
            }
        }

        root.setFocusableInTouchMode(true);
        root.requestFocus();

        return root;
    }

    @Override
    public void onStart() {
        super.onStart();

        if (getDialog() != null && getDialog().getWindow() != null) {
            getDialog().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putString(STATE_QUERY, query);
        super.onSaveInstanceState(outState);
    }

    private void filter(String value) {
        visible.clear();

        String text = value == null ? "" : value.trim();

        for (int year : years) {
            if (text.isEmpty() || String.valueOf(year).contains(text)) {
                visible.add(year);
            }
        }
    }

    private Integer bestMatch() {
        String text = query.trim();

        if (!text.isEmpty()) {
            try {
                int typed = Integer.parseInt(text);

                if (years.contains(typed)) {
                    return typed;
                }
            } catch (NumberFormatException ignored) {
                // falls through to the "only one left" rule
            }
        }

        return visible.size() == 1 ? visible.get(0) : null;
    }

    private void pick(int year) {
        hideKeyboard();

        Bundle result = new Bundle();
        result.putInt(YEAR, year);

        getParentFragmentManager().setFragmentResult(key, result);
        dismiss();
    }

    private void hideKeyboard() {
        if (search == null) {
            return;
        }

        InputMethodManager keyboard = (InputMethodManager)
                requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);

        if (keyboard != null) {
            keyboard.hideSoftInputFromWindow(search.getWindowToken(), 0);
        }
    }

    private final class YearAdapter extends RecyclerView.Adapter<YearAdapter.Holder> {

        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            TextView view = new TextView(parent.getContext());

            view.setGravity(Gravity.CENTER);
            view.setMinHeight(dp(48));
            view.setTextSize(16);

            RecyclerView.LayoutParams params = new RecyclerView.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            params.setMargins(dp(4), dp(4), dp(4), dp(4));
            view.setLayoutParams(params);

            return new Holder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull Holder holder, int position) {
            int year = visible.get(position);
            boolean current = year == selected;

            holder.label.setText(String.valueOf(year));
            holder.label.setTextColor(color(R.color.text_color_main));
            holder.label.setTypeface(null, current ? Typeface.BOLD : Typeface.NORMAL);
            holder.label.setBackground(surface(
                    color(current ? R.color.neuro_selected : R.color.account_settings_input_color), 12));

            holder.label.setOnClickListener(v -> pick(year));
        }

        @Override
        public int getItemCount() {
            return visible.size();
        }

        final class Holder extends RecyclerView.ViewHolder {
            final TextView label;

            Holder(@NonNull TextView itemView) {
                super(itemView);
                label = itemView;
            }
        }
    }
}