package com.example.f1app;

import static com.example.f1app.MainActivity.checkLightTheme;
import static com.example.f1app.MainActivity.getStringByName;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import androidx.annotation.NonNull;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

/** The "Seasons | Circuits" switch of a results tab and the list of circuits. */
final class tracksSection {

    private static final String DASH = "\u2013";

    private final Fragment host;
    private final LinearLayout modeBar;
    private final View seasonsGroup;
    private final LinearLayout tracksGroup;
    private final TextView seasonsButton;
    private final TextView tracksButton;
    private final Set<String> expanded = new HashSet<>();

    private boolean team;
    private String key = "";
    private List<Integer> seasons;
    private boolean tracksMode;
    private boolean loading;
    private List<trackResults.Stat> stats;
    private int token = 0;

    private static final Map<String, String[]> CIRCUITS = new HashMap<>();
    private static boolean circuitsLoaded = false;

    private static final Map<String, String> DRIVER_CODES = new HashMap<>();
    private static boolean driversLoaded = false;

    private final LinearLayout tracksList;
    private final View legendTitle;
    private final View legendCard;

    tracksSection(Fragment host, View root) {
        this.host = host;
        modeBar = root.findViewById(R.id.results_mode);
        seasonsGroup = root.findViewById(R.id.seasons_group);
        tracksGroup = root.findViewById(R.id.tracks_group);

        tracksList = root.findViewById(R.id.tracks_list);
        legendTitle = root.findViewById(R.id.tracks_legend_title);
        legendCard = root.findViewById(R.id.tracks_legend_card);

        resultsLegend.build(
                host.requireContext(),
                root.findViewById(R.id.tracks_legend_left),
                root.findViewById(R.id.tracks_legend_right));

        modeBar.setBackground(rounded(color(R.color.account_settings_input_color), 14));
        modeBar.setPadding(dp(4), dp(4), dp(4), dp(4));

        seasonsButton = button(R.string.tracks_mode_seasons);
        tracksButton = button(R.string.tracks_mode_tracks);

        modeBar.addView(seasonsButton, new LinearLayout.LayoutParams(0, dp(40), 1f));
        modeBar.addView(tracksButton, new LinearLayout.LayoutParams(0, dp(40), 1f));

        seasonsButton.setOnClickListener(v -> select(false));
        tracksButton.setOnClickListener(v -> select(true));

        style();
    }

    private void setLegendVisible(boolean visible) {
        int visibility = visible ? View.VISIBLE : View.GONE;
        legendTitle.setVisibility(visibility);
        legendCard.setVisibility(visibility);
    }

    void configure(boolean team, String key) {
        this.team = team;
        this.key = key == null ? "" : key;
        reset();
    }

    void setSeasons(List<Integer> list) {
        List<Integer> copy = new ArrayList<>(list);

        if (copy.equals(seasons)) {
            return;
        }

        seasons = copy;
        reset();
    }

    private void reset() {
        token++;
        loading = false;
        stats = null;
        expanded.clear();

        if (tracksMode) {
            ensureLoaded();
        }
    }

    private void select(boolean tracks) {
        tracksMode = tracks;
        seasonsGroup.setVisibility(tracks ? View.GONE : View.VISIBLE);
        tracksGroup.setVisibility(tracks ? View.VISIBLE : View.GONE);
        style();

        if (tracks) {
            ensureLoaded();
        }
    }

    private void ensureLoaded() {
        if (stats != null || loading || seasons == null || seasons.isEmpty() || key.isEmpty()) {
            render();
            return;
        }

        loading = true;
        final int mine = ++token;
        render();

        prepare(() -> {
            if (mine != token || !host.isAdded()) {
                return;
            }

            trackResults.load(team, key, seasons, result -> {
                if (mine != token || !host.isAdded()) {
                    return;
                }

                loading = false;
                stats = result;
                render();
            });
        });
    }

    private void withDrivers(Runnable next) {
        if (driversLoaded) {
            next.run();
            return;
        }

        FirebaseDatabase.getInstance().getReference().child("drivers")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        for (DataSnapshot driver : snapshot.getChildren()) {
                            String code = driver.child("driversCode").getValue(String.class);

                            if (driver.getKey() != null && code != null) {
                                DRIVER_CODES.put(driver.getKey(), code);
                            }
                        }

                        driversLoaded = true;
                        next.run();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        next.run();
                    }
                });
    }

    private String driverCode(String fullName) {
        if (fullName == null || fullName.isEmpty()) {
            return "";
        }

        String code = DRIVER_CODES.get(fullName);

        if (code != null && !code.isEmpty()) {
            return code;
        }

        String family = driverNameFormatter.split(fullName)[1];
        return family.length() > 3 ? family.substring(0, 3).toUpperCase(Locale.ROOT)
                : family.toUpperCase(Locale.ROOT);
    }

    private void prepare(Runnable next) {
        withCircuits(() -> {
            if (team) {
                withDrivers(next);
            } else {
                next.run();
            }
        });
    }

    private void withCircuits(Runnable next) {
        if (circuitsLoaded) {
            next.run();
            return;
        }

        FirebaseDatabase.getInstance().getReference().child("circuits")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        for (DataSnapshot circuit : snapshot.getChildren()) {
                            String id = circuit.getKey();
                            String name = circuit.child("circuitName").getValue(String.class);
                            String country = circuit.child("country").getValue(String.class);

                            if (id != null) {
                                CIRCUITS.put(id, new String[]{
                                        name == null ? "" : name,
                                        country == null ? "" : country});
                            }
                        }

                        circuitsLoaded = true;
                        next.run();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        next.run();
                    }
                });
    }

    private String circuitTitle(trackResults.Stat stat) {
        int id = getStringByName(stat.circuitId + "_text");

        if (id != 0) {
            return host.getString(id);
        }

        String[] info = CIRCUITS.get(stat.circuitId);

        if (info != null && !info[0].isEmpty()) {
            return info[0];
        }

        return localized(stat.raceName);
    }

    private String countryOf(String circuitId) {
        String[] info = CIRCUITS.get(circuitId);

        if (info == null || info[1].isEmpty()) {
            return "";
        }

        String code = driverStatsFragment.getCountryCode(info[1]).trim();

        if (code.isEmpty()) {
            return info[1];
        }

        String display = new Locale(Locale.getDefault().getLanguage(), code).getDisplayCountry();
        return display.isEmpty() ? info[1] : display;
    }

    // ---------- switch ----------

    private TextView button(int label) {
        TextView view = new TextView(host.requireContext());
        view.setText(label);
        view.setTextSize(14);
        view.setGravity(Gravity.CENTER);
        return view;
    }

    private void style() {
        styleButton(seasonsButton, !tracksMode);
        styleButton(tracksButton, tracksMode);
    }

    private void styleButton(TextView view, boolean selected) {
        boolean light = checkLightTheme(host.requireContext());

        view.setBackground(new RippleDrawable(
                ColorStateList.valueOf(light ? 0x16000000 : 0x22FFFFFF),
                rounded(selected ? color(R.color.colorPrimary) : Color.TRANSPARENT, 10),
                rounded(Color.WHITE, 10)));
        view.setTextColor(color(selected ? R.color.text_color_main : R.color.grey));
        view.setTypeface(null, selected ? Typeface.BOLD : Typeface.NORMAL);
        view.setElevation(selected ? dp(2) : 0);
    }

    // ---------- list ----------

    private void render() {
        tracksList.removeAllViews();
        setLegendVisible(false);

        if (!tracksMode) {
            return;
        }

        if (stats == null) {
            boolean nothing = seasons != null && seasons.isEmpty() && !loading;

            if (nothing) {
                addMessage(R.string.tracks_empty);
            } else {
                addProgress();
            }

            return;
        }

        if (stats.isEmpty()) {
            addMessage(R.string.tracks_empty);
            return;
        }

        LinearLayout card = new LinearLayout(host.requireContext());
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundResource(R.drawable.bg_results_card);

        for (int i = 0; i < stats.size(); i++) {
            card.addView(circuitRow(stats.get(i)));

            if (i < stats.size() - 1) {
                View line = new View(host.requireContext());
                line.setBackgroundColor(color(R.color.colorPrimary));
                card.addView(line, new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, dp(1)));
            }
        }

        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cardParams.topMargin = dp(14);
        tracksList.addView(card, cardParams);

        int first = Integer.MAX_VALUE;
        int last = 0;

        for (int season : seasons) {
            first = Math.min(first, season);
            last = Math.max(last, season);
        }

        TextView note = new TextView(host.requireContext());
        note.setText(host.getString(R.string.tracks_note, first, last));
        note.setTextSize(13);
        note.setTextColor(color(R.color.text_color_secondary));

        setLegendVisible(true);

        LinearLayout.LayoutParams noteParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        noteParams.topMargin = dp(12);
        tracksList.addView(note, noteParams);
    }

    private View circuitRow(trackResults.Stat stat) {
        Context context = host.requireContext();

        LinearLayout box = new LinearLayout(context);
        box.setOrientation(LinearLayout.VERTICAL);

        LinearLayout head = new LinearLayout(context);
        head.setOrientation(LinearLayout.HORIZONTAL);
        head.setGravity(Gravity.CENTER_VERTICAL);
        head.setPadding(dp(14), dp(12), dp(14), dp(12));
        head.setClickable(true);
        head.setFocusable(true);

        LinearLayout texts = new LinearLayout(context);
        texts.setOrientation(LinearLayout.VERTICAL);

        TextView name = new TextView(context);
        name.setText(circuitTitle(stat));
        name.setTextSize(16);
        name.setTypeface(null, Typeface.BOLD);
        name.setTextColor(color(R.color.text_color_main));
        texts.addView(name);

        String country = countryOf(stat.circuitId);

        if (!country.isEmpty()) {
            TextView place = new TextView(context);
            place.setText(country);
            place.setTextSize(13);
            place.setTextColor(color(R.color.text_color_secondary));
            texts.addView(place);
        }

        TextView summary = new TextView(context);
        summary.setText(host.getString(R.string.tracks_summary, stat.starts, stat.wins, stat.podiums));
        summary.setTextSize(13);
        summary.setTextColor(color(R.color.text_color_secondary));
        texts.addView(summary);

        head.addView(texts, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView best = chip(stat.best > 0 ? "0-" + stat.best : "null");
        LinearLayout.LayoutParams bestParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        bestParams.setMarginStart(dp(8));
        head.addView(best, bestParams);

        TextView arrow = new TextView(context);
        arrow.setTextSize(14);
        arrow.setTextColor(color(R.color.grey));
        arrow.setPadding(dp(10), 0, 0, 0);
        head.addView(arrow);

        LinearLayout details = new LinearLayout(context);
        details.setOrientation(LinearLayout.VERTICAL);
        details.setPadding(dp(12), 0, dp(12), dp(8));
        fillDetails(details, stat);

        boolean open = expanded.contains(stat.circuitId);
        details.setVisibility(open ? View.VISIBLE : View.GONE);
        arrow.setText(open ? "\u25B4" : "\u25BE");

        head.setOnClickListener(v -> {
            boolean show = details.getVisibility() != View.VISIBLE;
            details.setVisibility(show ? View.VISIBLE : View.GONE);
            arrow.setText(show ? "\u25B4" : "\u25BE");

            if (show) {
                expanded.add(stat.circuitId);
            } else {
                expanded.remove(stat.circuitId);
            }
        });

        box.addView(head);
        box.addView(details);
        return box;
    }

    private void fillDetails(LinearLayout details, trackResults.Stat stat) {
        Context context = host.requireContext();
        LayoutInflater inflater = LayoutInflater.from(context);

        details.addView(team ? teamHeader(context) : driverHeader(context));

        for (int i = 0; i < stat.rows.size(); i++) {
            trackResults.Row row = stat.rows.get(i);
            boolean last = i == stat.rows.size() - 1;

            details.addView(team
                    ? teamRow(inflater, details, row, last)
                    : driverRow(inflater, details, row, last));
        }
    }

    private View driverHeader(Context context) {
        LinearLayout header = new LinearLayout(context);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setPadding(0, dp(2), 0, dp(6));

        header.addView(headerText(context, R.string.tracks_year),
                new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        header.addView(headerText(context, R.string.driver_results_grid_header),
                new LinearLayout.LayoutParams(dp(56), ViewGroup.LayoutParams.WRAP_CONTENT));
        header.addView(headerText(context, R.string.result_header),
                new LinearLayout.LayoutParams(dp(72), ViewGroup.LayoutParams.WRAP_CONTENT));

        return header;
    }

    private View teamHeader(Context context) {
        LinearLayout header = new LinearLayout(context);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setPadding(0, dp(2), 0, dp(6));

        header.addView(headerText(context, R.string.tracks_year),
                new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.6f));

        LinearLayout cars = new LinearLayout(context);
        cars.setOrientation(LinearLayout.HORIZONTAL);
        cars.addView(headerText(context, R.string.tracks_car_best),
                new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        cars.addView(headerText(context, R.string.tracks_car_second),
                new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        header.addView(cars, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 2f));
        return header;
    }

    private TextView headerText(Context context, int label) {
        TextView view = new TextView(context);
        view.setText(label);
        view.setTextSize(13);
        view.setGravity(Gravity.CENTER);
        view.setMaxLines(2);
        view.setTextColor(color(R.color.text_color_secondary));
        return view;
    }

    private View driverRow(LayoutInflater inflater, ViewGroup parent, trackResults.Row row, boolean last) {
        View view = inflater.inflate(R.layout.item_driver_result, parent, false);
        String code = row.codes.get(0);

        ((TextView) view.findViewById(R.id.raceName)).setText(String.valueOf(row.year));
        view.findViewById(R.id.round_text).setVisibility(View.GONE);
        ((TextView) view.findViewById(R.id.grid_text)).setText(grid(code));
        resultChipBinder.bind(host.requireContext(), view.findViewById(R.id.driverResults), code);
        view.findViewById(R.id.bottomLine).setVisibility(last ? View.GONE : View.VISIBLE);

        return view;
    }

    private View teamRow(LayoutInflater inflater, ViewGroup parent, trackResults.Row row, boolean last) {
        View view = inflater.inflate(R.layout.item_team_result_row, parent, false);

        ((TextView) view.findViewById(R.id.raceName)).setText(String.valueOf(row.year));
        view.findViewById(R.id.round_text).setVisibility(View.GONE);
        view.findViewById(R.id.bottomLine).setVisibility(last ? View.GONE : View.VISIBLE);

        LinearLayout cells = view.findViewById(R.id.team_row_cells);
        LinearLayout.LayoutParams cellsParams = (LinearLayout.LayoutParams) cells.getLayoutParams();
        cellsParams.weight = 2;
        cells.setLayoutParams(cellsParams);

        for (int i = 0; i < 2; i++) {
            View cell = inflater.inflate(R.layout.item_team_result_cell, cells, false);

            String code = i < row.codes.size() ? row.codes.get(i) : "null";
            resultChipBinder.bind(host.requireContext(), cell.findViewById(R.id.result_chip), code);

            LinearLayout box = new LinearLayout(host.requireContext());
            box.setOrientation(LinearLayout.VERTICAL);
            box.setGravity(Gravity.CENTER_HORIZONTAL);
            box.addView(cell, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

            TextView caption = new TextView(host.requireContext());
            caption.setText(i < row.drivers.size() ? driverCode(row.drivers.get(i)) : "");
            caption.setTextSize(11);
            caption.setGravity(Gravity.CENTER);
            caption.setTextColor(color(R.color.text_color_secondary));
            box.addView(caption, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

            cells.addView(box, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        }

        return view;
    }

    private String grid(String code) {
        String[] parts = code.trim().split("-");

        if (parts.length != 2) {
            return DASH;
        }

        try {
            int start = Integer.parseInt(parts[0]);
            return start > 0 ? String.valueOf(start) : DASH;
        } catch (NumberFormatException error) {
            return DASH;
        }
    }

    private TextView chip(String code) {
        Context context = host.requireContext();

        TextView chip = new TextView(context);
        chip.setGravity(Gravity.CENTER);
        chip.setMinWidth(dp(40));
        chip.setMinHeight(dp(30));
        chip.setTextSize(14);
        chip.setTypeface(null, Typeface.BOLD);
        chip.setPadding(dp(8), dp(2), dp(8), dp(2));
        resultChipBinder.bind(context, chip, code);

        return chip;
    }

    private void addProgress() {
        ProgressBar bar = new ProgressBar(host.requireContext());

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.gravity = Gravity.CENTER_HORIZONTAL;
        params.topMargin = dp(32);
        tracksList.addView(bar, params);
    }

    private void addMessage(int text) {
        TextView view = new TextView(host.requireContext());
        view.setText(text);
        view.setTextSize(16);
        view.setGravity(Gravity.CENTER);
        view.setPadding(dp(24), dp(32), dp(24), dp(32));
        view.setTextColor(color(R.color.text_color_main));
        tracksList.addView(view);
    }

    private String localized(String raceName) {
        String id = raceName.toLowerCase(Locale.ROOT).replaceAll("\\s+", "_") + "_locality";
        int resource = getStringByName(id);

        return resource != 0 ? host.getString(resource) : raceName;
    }

    private GradientDrawable rounded(int fill, int radius) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(fill);
        drawable.setCornerRadius(dp(radius));
        return drawable;
    }

    private int color(int res) {
        return ContextCompat.getColor(host.requireContext(), res);
    }

    private int dp(float value) {
        return Math.round(value * host.getResources().getDisplayMetrics().density);
    }
}