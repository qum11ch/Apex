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
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Locale;

/** A searchable list of drivers or teams: a tap returns the key under the request key. */
public class comparePickerSheet extends neuroBaseSheet {

    static final String KEY = "key";

    private static final String TAG = "compare_picker";
    private static final String ARG_TYPE = "type";
    private static final String ARG_EXCLUDE = "exclude";
    private static final String ARG_REQUEST = "request";

    private static final class Item {
        final String key;
        final String label;
        final String extra;
        final String sort;

        Item(String key, String label, String extra, String sort) {
            this.key = key;
            this.label = label;
            this.extra = extra;
            this.sort = sort;
        }
    }

    private final ArrayList<Item> all = new ArrayList<>();
    private final ArrayList<Item> visible = new ArrayList<>();

    private Adapter adapter;
    private TextView empty;
    private String request = "";
    private String query = "";

    static void show(FragmentManager manager, int type, String exclude, String requestKey) {
        if (manager.findFragmentByTag(TAG) != null || manager.isStateSaved()) {
            return;
        }

        comparePickerSheet sheet = new comparePickerSheet();

        Bundle args = new Bundle();
        args.putInt(ARG_TYPE, type);
        args.putString(ARG_EXCLUDE, exclude);
        args.putString(ARG_REQUEST, requestKey);

        sheet.setArguments(args);
        sheet.show(manager, TAG);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle state) {
        int type = requireArguments().getInt(ARG_TYPE);
        request = requireArguments().getString(ARG_REQUEST, "");

        LinearLayout root = root();

        TextView title = text(getString(type == compareParticipant.TEAM
                        ? R.string.compare_pick_team_title : R.string.compare_pick_driver_title),
                22, R.color.text_color_main);
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
        int height = Math.min(dp(420), Math.round(getResources().getDisplayMetrics().heightPixels * 0.5f));
        root.addView(results, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, height));

        RecyclerView list = new RecyclerView(requireContext());
        list.setLayoutManager(new LinearLayoutManager(requireContext()));
        list.setOverScrollMode(View.OVER_SCROLL_NEVER);
        results.addView(list, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        empty = text(getString(R.string.compare_pick_empty), 16, R.color.text_color_secondary);
        empty.setGravity(Gravity.CENTER);
        empty.setVisibility(View.GONE);
        results.addView(empty, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        adapter = new Adapter();
        list.setAdapter(adapter);

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

        load(type, requireArguments().getString(ARG_EXCLUDE, ""));
        return root;
    }

    @Override
    public void onStart() {
        super.onStart();

        if (getDialog() != null && getDialog().getWindow() != null) {
            getDialog().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        }
    }

    private void load(int type, String exclude) {
        String node = type == compareParticipant.TEAM ? "constructors" : "drivers";

        FirebaseDatabase.getInstance().getReference().child(node)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (!isAdded()) {
                            return;
                        }

                        for (DataSnapshot child : snapshot.getChildren()) {
                            String key = child.getKey();

                            if (key == null || key.equals(exclude)) {
                                continue;
                            }

                            if (type == compareParticipant.TEAM) {
                                String name = child.child("name").getValue(String.class);
                                String full = child.child("fullTeamName").getValue(String.class);
                                String label = name == null ? key : name;
                                all.add(new Item(key, label, full == null ? "" : full,
                                        label.toLowerCase(Locale.ROOT)));
                            } else {
                                String team = child.child("driversTeam").getValue(String.class);
                                String[] names = driverNameFormatter.split(key);
                                all.add(new Item(key, key, team == null ? "" : team,
                                        names[1].toLowerCase(Locale.ROOT) + " " + names[0].toLowerCase(Locale.ROOT)));
                            }
                        }

                        Collections.sort(all, (x, y) -> x.sort.compareTo(y.sort));
                        filter();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        if (isAdded()) {
                            filter();
                        }
                    }
                });
    }

    private void filter() {
        visible.clear();

        String text = query.trim().toLowerCase(Locale.ROOT);

        for (Item item : all) {
            if (text.isEmpty() || item.label.toLowerCase(Locale.ROOT).contains(text)
                    || item.extra.toLowerCase(Locale.ROOT).contains(text)) {
                visible.add(item);
            }
        }

        adapter.notifyDataSetChanged();
        empty.setVisibility(visible.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void pick(String key) {
        Bundle result = new Bundle();
        result.putString(KEY, key);

        getParentFragmentManager().setFragmentResult(request, result);
        dismiss();
    }

    private final class Adapter extends RecyclerView.Adapter<Adapter.Holder> {

        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LinearLayout row = new LinearLayout(parent.getContext());
            row.setOrientation(LinearLayout.VERTICAL);
            row.setPadding(dp(8), dp(10), dp(8), dp(10));
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
            Item item = visible.get(position);

            holder.label.setText(item.label);
            holder.extra.setText(item.extra);
            holder.extra.setVisibility(item.extra.isEmpty() ? View.GONE : View.VISIBLE);
            holder.itemView.setOnClickListener(v -> pick(item.key));
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