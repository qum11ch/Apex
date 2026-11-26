package com.example.f1app;

import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
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

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** A searchable list of drivers or teams: a tap returns the key under the request key. */
public class comparePickerSheet extends neuroBaseSheet {

    static final String KEY = "key";

    private static final String TAG = "compare_picker";
    private static final String ARG_TYPE = "type";
    private static final String ARG_EXCLUDE = "exclude";
    private static final String ARG_REQUEST = "request";
    private static final String ARG_SEASON = "season";

    private static final int KIND_HEADER = 0;
    private static final int KIND_ITEM = 1;
    private static final int NEUTRAL = 0xFF8E8E93;

    private static final class Item {
        final String key;
        final String label;
        final String extra;
        final String sort;
        final boolean mate;
        final boolean active;
        final int lastYear;
        final int color;

        Item(String key, String label, String extra, String sort, boolean mate,
             boolean active, int lastYear, int color) {
            this.key = key;
            this.label = label;
            this.extra = extra;
            this.sort = sort;
            this.mate = mate;
            this.active = active;
            this.lastYear = lastYear;
            this.color = color;
        }
    }

    private static final class Row {
        final int kind;
        final String title;
        final Item item;

        Row(int kind, String title, Item item) {
            this.kind = kind;
            this.title = title;
            this.item = item;
        }
    }

    private final ArrayList<Item> all = new ArrayList<>();
    private final ArrayList<Row> rows = new ArrayList<>();

    private Adapter adapter;
    private TextView empty;
    private String request = "";
    private String query = "";
    private String seasonLabel = "";

    static void show(FragmentManager manager, int type, String exclude, String requestKey) {
        show(manager, type, exclude, requestKey, "", "");
    }

    /** The team id is not needed any more: the line-up of the season tells who the teammates are. */
    static void show(FragmentManager manager, int type, String exclude, String requestKey,
                     String teamId, String season) {
        if (manager.findFragmentByTag(TAG) != null || manager.isStateSaved()) {
            return;
        }

        comparePickerSheet sheet = new comparePickerSheet();

        Bundle args = new Bundle();
        args.putInt(ARG_TYPE, type);
        args.putString(ARG_EXCLUDE, exclude);
        args.putString(ARG_REQUEST, requestKey);
        args.putString(ARG_SEASON, season == null ? "" : season);

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
        final int resultsHeight = Math.min(dp(420), Math.round(getResources().getDisplayMetrics().heightPixels * 0.5f));
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

    // ---------- loading ----------

    private void load(int type, String exclude) {
        if (type != compareParticipant.DRIVER) {
            loadItems(type, exclude, Collections.emptySet(), Collections.emptySet());
            return;
        }

        String requested = requireArguments().getString(ARG_SEASON, "");
        final String season = requested.isEmpty()
                ? String.valueOf(LocalDate.now().getYear()) : requested;

        FirebaseDatabase.getInstance().getReference()
                .child("driverLineUp").child("season").child(season)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (!isAdded()) {
                            return;
                        }

                        Set<String> mates = new HashSet<>();
                        Set<String> lineup = new HashSet<>();

                        for (DataSnapshot team : snapshot.getChildren()) {
                            DataSnapshot drivers = team.child("drivers");
                            boolean mine = drivers.hasChild(exclude)
                                    && !Boolean.FALSE.equals(drivers.child(exclude).getValue());

                            for (DataSnapshot driver : drivers.getChildren()) {
                                if (driver.getKey() == null
                                        || Boolean.FALSE.equals(driver.getValue())) {
                                    continue;
                                }

                                lineup.add(driver.getKey());

                                if (mine) {
                                    mates.add(driver.getKey());
                                }
                            }
                        }

                        mates.remove(exclude);
                        lineup.remove(exclude);
                        seasonLabel = season;

                        Log.d("comparePicker", "season=" + season + " driver=" + exclude
                                + " teammates=" + mates + " lineup=" + lineup.size());

                        loadItems(type, exclude, mates, lineup);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.w("comparePicker", "lineup is not available", error.toException());

                        if (isAdded()) {
                            loadItems(type, exclude, Collections.emptySet(), Collections.emptySet());
                        }
                    }
                });
    }

    private void loadItems(int type, String exclude, Set<String> mates, Set<String> lineup) {
        FirebaseDatabase.getInstance().getReference().child("constructors")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot constructors) {
                        if (!isAdded()) {
                            return;
                        }

                        Map<String, Integer> colors = new HashMap<>();

                        for (DataSnapshot team : constructors.getChildren()) {
                            String name = team.child("name").getValue(String.class);

                            if (name != null) {
                                colors.put(name.toLowerCase(Locale.ROOT), teamColor(
                                        team.getKey(), team.child("color").getValue(String.class)));
                            }
                        }

                        if (type == compareParticipant.TEAM) {
                            for (DataSnapshot team : constructors.getChildren()) {
                                String key = team.getKey();

                                if (key == null || key.equals(exclude)) {
                                    continue;
                                }

                                String name = team.child("name").getValue(String.class);
                                String full = team.child("fullTeamName").getValue(String.class);
                                String label = name == null ? key : name;

                                all.add(new Item(key, label, full == null ? "" : full,
                                        label.toLowerCase(Locale.ROOT), false, false, 0,
                                        teamColor(key, team.child("color").getValue(String.class))));
                            }

                            sortAndFilter();
                        } else {
                            loadDrivers(exclude, mates, colors, lineup);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        if (isAdded()) {
                            loadDrivers(exclude, mates, new HashMap<>(), lineup);
                        }
                    }
                });
    }

    private void loadDrivers(String exclude, Set<String> mates, Map<String, Integer> colors, Set<String> lineup) {
        FirebaseDatabase.getInstance().getReference().child("drivers")
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

                            String team = child.child("driversTeam").getValue(String.class);
                            String[] names = driverNameFormatter.split(key);

                            Integer known = team == null ? null : colors.get(team.toLowerCase(Locale.ROOT));
                            int fill = known != null ? known : teamColor(pastTeamPalette.idForName(team), null);

                            int lastYear = year(child.child("lastEntry").getValue(String.class));

                            all.add(new Item(key, key, team == null ? "" : team,
                                    names[1].toLowerCase(Locale.ROOT) + " " + names[0].toLowerCase(Locale.ROOT),
                                    mates.contains(key), lineup.contains(key), lastYear, fill));
                        }

                        sortAndFilter();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        if (isAdded()) {
                            sortAndFilter();
                        }
                    }
                });
    }

    private void sortAndFilter() {
        Collections.sort(all, (x, y) -> {
            int byRank = Integer.compare(rank(x), rank(y));

            if (byRank != 0) {
                return byRank;
            }

            if (rank(x) == 2 && x.lastYear != y.lastYear) {
                return Integer.compare(y.lastYear, x.lastYear);
            }

            return x.sort.compareTo(y.sort);
        });

        filter();
    }

    private int teamColor(String id, String hex) {
        try {
            return Color.parseColor(pastTeamPalette.resolve(id, hex));
        } catch (RuntimeException error) {
            return NEUTRAL;
        }
    }

    private static int year(String entry) {
        if (entry == null) {
            return 0;
        }

        java.util.regex.Matcher matcher = java.util.regex.Pattern
                .compile("\\b(\\d{4})\\b").matcher(entry);

        return matcher.find() ? Integer.parseInt(matcher.group(1)) : 0;
    }

    private static int rank(Item item) {
        return item.mate ? 0 : (item.active ? 1 : 2);
    }

    // ---------- list ----------

    private void filter() {
        rows.clear();

        String text = query.trim().toLowerCase(Locale.ROOT);
        ArrayList<Item> matches = new ArrayList<>();
        boolean anyTop = false;

        for (Item item : all) {
            if (text.isEmpty() || item.label.toLowerCase(Locale.ROOT).contains(text)
                    || item.extra.toLowerCase(Locale.ROOT).contains(text)) {
                matches.add(item);
                anyTop |= rank(item) < 2;
            }
        }

        boolean sections = text.isEmpty() && anyTop;
        int lastRank = -1;

        for (Item item : matches) {
            int rank = rank(item);

            if (sections && rank != lastRank) {
                rows.add(new Row(KIND_HEADER, header(rank), null));
                lastRank = rank;
            }

            rows.add(new Row(KIND_ITEM, null, item));
        }

        adapter.notifyDataSetChanged();
        empty.setVisibility(matches.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private String header(int rank) {
        if (rank == 0) {
            return getString(R.string.compare_pick_teammate);
        }

        if (rank == 1) {
            return getString(R.string.compare_pick_season_format, seasonLabel);
        }

        return getString(R.string.compare_pick_others);
    }

    private void pick(String key) {
        Bundle result = new Bundle();
        result.putString(KEY, key);

        getParentFragmentManager().setFragmentResult(request, result);
        dismiss();
    }

    private final class Adapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        @Override
        public int getItemViewType(int position) {
            return rows.get(position).kind;
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            if (viewType == KIND_HEADER) {
                TextView header = text("", 12, R.color.text_color_thirdly);
                header.setAllCaps(true);
                header.setLetterSpacing(0.06f);
                header.setTypeface(header.getTypeface(), Typeface.BOLD);
                header.setPadding(dp(8), dp(14), dp(8), dp(6));
                header.setLayoutParams(new RecyclerView.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

                return new HeaderHolder(header);
            }

            LinearLayout row = new LinearLayout(parent.getContext());
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(dp(12), dp(10), dp(12), dp(10));

            RecyclerView.LayoutParams params = new RecyclerView.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            params.topMargin = dp(2);
            params.bottomMargin = dp(2);
            row.setLayoutParams(params);

            View bar = new View(parent.getContext());
            LinearLayout.LayoutParams barParams = new LinearLayout.LayoutParams(dp(4), dp(36));
            barParams.setMarginEnd(dp(12));
            row.addView(bar, barParams);

            LinearLayout texts = new LinearLayout(parent.getContext());
            texts.setOrientation(LinearLayout.VERTICAL);

            TextView label = text("", 16, R.color.text_color_main);
            TextView extra = text("", 13, R.color.text_color_secondary);
            texts.addView(label);
            texts.addView(extra);

            row.addView(texts, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

            return new ItemHolder(row, bar, label, extra);
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            Row row = rows.get(position);

            if (holder instanceof HeaderHolder) {
                ((HeaderHolder) holder).title.setText(row.title);
                return;
            }

            ItemHolder item = (ItemHolder) holder;

            item.bar.setBackground(surface(row.item.color, 2));
            item.label.setText(row.item.label);
            item.label.setTypeface(null, row.item.mate ? Typeface.BOLD : Typeface.NORMAL);
            item.extra.setText(row.item.extra);
            item.extra.setVisibility(row.item.extra.isEmpty() ? View.GONE : View.VISIBLE);
            item.itemView.setBackground(row.item.mate
                    ? surface(color(R.color.account_settings_input_color), 12) : null);
            item.itemView.setOnClickListener(v -> pick(row.item.key));
        }

        @Override
        public int getItemCount() {
            return rows.size();
        }
    }

    private static final class HeaderHolder extends RecyclerView.ViewHolder {
        final TextView title;

        HeaderHolder(@NonNull TextView itemView) {
            super(itemView);
            title = itemView;
        }
    }

    private static final class ItemHolder extends RecyclerView.ViewHolder {
        final View bar;
        final TextView label;
        final TextView extra;

        ItemHolder(@NonNull View itemView, View bar, TextView label, TextView extra) {
            super(itemView);
            this.bar = bar;
            this.label = label;
            this.extra = extra;
        }
    }
}