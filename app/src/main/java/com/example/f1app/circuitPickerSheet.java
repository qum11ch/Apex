package com.example.f1app;

import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatEditText;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Locale;

/** A searchable list of circuits: a tap returns the circuit id under the request key. */
public class circuitPickerSheet extends neuroBaseSheet {

    static final String KEY = "circuit";

    private static final String TAG = "circuit_picker";
    private static final String ARG_IDS = "ids";
    private static final String ARG_NAMES = "names";
    private static final String ARG_PLACES = "places";
    private static final String ARG_SELECTED = "selected";
    private static final String ARG_REQUEST = "request";

    private final ArrayList<String> ids = new ArrayList<>();
    private final ArrayList<String> names = new ArrayList<>();
    private final ArrayList<String> places = new ArrayList<>();
    private final ArrayList<Integer> visible = new ArrayList<>();

    private Adapter adapter;
    private TextView empty;
    private String selected = "";
    private String request = "";
    private String query = "";

    static void show(FragmentManager manager, ArrayList<String> ids, ArrayList<String> names,
                     ArrayList<String> places, String selected, String requestKey) {
        if (manager.findFragmentByTag(TAG) != null || manager.isStateSaved()) {
            return;
        }

        circuitPickerSheet sheet = new circuitPickerSheet();

        Bundle args = new Bundle();
        args.putStringArrayList(ARG_IDS, ids);
        args.putStringArrayList(ARG_NAMES, names);
        args.putStringArrayList(ARG_PLACES, places);
        args.putString(ARG_SELECTED, selected);
        args.putString(ARG_REQUEST, requestKey);

        sheet.setArguments(args);
        sheet.show(manager, TAG);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle args = requireArguments();
        ids.addAll(args.getStringArrayList(ARG_IDS));
        names.addAll(args.getStringArrayList(ARG_NAMES));
        places.addAll(args.getStringArrayList(ARG_PLACES));
        selected = args.getString(ARG_SELECTED, "");
        request = args.getString(ARG_REQUEST, "");
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle state) {
        LinearLayout root = root();

        TextView title = text(getString(R.string.compare_circuit_pick_title), 22, R.color.text_color_main);
        title.setTypeface(title.getTypeface(), Typeface.BOLD);
        root.addView(title);

        AppCompatEditText search = new AppCompatEditText(requireContext());
        search.setSingleLine(true);
        search.setTextSize(16);
        search.setTextColor(color(R.color.text_color_main));
        search.setHintTextColor(color(R.color.text_color_secondary));
        search.setHint(R.string.compare_pick_search_hint);
        search.setMinHeight(dp(52));
        search.setBackground(surface(color(R.color.account_settings_input_color), 12));
        search.setPadding(dp(16), dp(12), dp(16), dp(12));

        LinearLayout.LayoutParams searchParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        searchParams.topMargin = dp(16);
        searchParams.bottomMargin = dp(8);
        root.addView(search, searchParams);

        FrameLayout results = new FrameLayout(requireContext());
        final int resultsHeight = Math.min(dp(420),
                Math.round(getResources().getDisplayMetrics().heightPixels * 0.5f));
        root.addView(results, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, resultsHeight));

        RecyclerView list = new RecyclerView(requireContext());
        list.setLayoutManager(new LinearLayoutManager(requireContext()));
        list.setOverScrollMode(View.OVER_SCROLL_NEVER);
        list.setClipToPadding(false);
        results.addView(list, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        empty = text(getString(R.string.compare_pick_empty), 16, R.color.text_color_secondary);
        empty.setGravity(Gravity.CENTER);
        empty.setVisibility(View.GONE);
        results.addView(empty, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        adapter = new Adapter();
        list.setAdapter(adapter);
        filter();

        search.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                query = s.toString();
                filter();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets safe = insets.getInsets(WindowInsetsCompat.Type.systemBars()
                    | WindowInsetsCompat.Type.displayCutout());

            v.setPadding(dp(24) + safe.left, dp(12), dp(24) + safe.right, 0);

            int bottom = dp(20) + safe.bottom;
            list.setPadding(0, 0, 0, bottom);

            ViewGroup.LayoutParams params = results.getLayoutParams();
            params.height = resultsHeight + bottom;
            results.setLayoutParams(params);

            return insets;
        });
        root.post(() -> ViewCompat.requestApplyInsets(root));

        return root;
    }

    @Override
    public void onStart() {
        super.onStart();

        if (getDialog() != null && getDialog().getWindow() != null) {
            getDialog().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        }
    }

    private void filter() {
        visible.clear();

        String text = query.trim().toLowerCase(Locale.ROOT);

        for (int i = 0; i < ids.size(); i++) {
            if (text.isEmpty() || names.get(i).toLowerCase(Locale.ROOT).contains(text)
                    || places.get(i).toLowerCase(Locale.ROOT).contains(text)) {
                visible.add(i);
            }
        }

        adapter.notifyDataSetChanged();
        empty.setVisibility(visible.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void pick(String id) {
        Bundle result = new Bundle();
        result.putString(KEY, id);

        getParentFragmentManager().setFragmentResult(request, result);
        dismiss();
    }

    private final class Adapter extends RecyclerView.Adapter<Adapter.Holder> {

        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LinearLayout row = new LinearLayout(parent.getContext());
            row.setOrientation(LinearLayout.VERTICAL);
            row.setPadding(dp(12), dp(10), dp(12), dp(10));
            row.setLayoutParams(new RecyclerView.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

            TextView label = text("", 16, R.color.text_color_main);
            TextView extra = text("", 13, R.color.text_color_secondary);
            row.addView(label);
            row.addView(extra);

            return new Holder(row, label, extra);
        }

        @Override
        public void onBindViewHolder(@NonNull Holder holder, int position) {
            int index = visible.get(position);
            boolean current = ids.get(index).equals(selected);

            holder.label.setText(names.get(index));
            holder.label.setTypeface(null, current ? Typeface.BOLD : Typeface.NORMAL);
            holder.extra.setText(places.get(index));
            holder.extra.setVisibility(places.get(index).isEmpty() ? View.GONE : View.VISIBLE);
            holder.itemView.setBackground(current
                    ? surface(color(R.color.account_settings_input_color), 12) : null);
            holder.itemView.setOnClickListener(v -> pick(ids.get(index)));
        }

        @Override
        public int getItemCount() {
            return visible.size();
        }

        final class Holder extends RecyclerView.ViewHolder {
            final TextView label;
            final TextView extra;

            Holder(@NonNull View itemView, TextView label, TextView extra) {
                super(itemView);
                this.label = label;
                this.extra = extra;
            }
        }
    }
}