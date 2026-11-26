package com.example.f1app;

import static com.example.f1app.MainActivity.checkLightTheme;
import static com.example.f1app.MainActivity.getStringByName;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.StyleSpan;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.graphics.Path;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.ColorUtils;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Locale;

public class compareActivity extends AppCompatActivity {

    static final String EXTRA_TYPE = "type";
    static final String EXTRA_FIRST = "first";
    static final String EXTRA_SECOND = "second";
    static final String EXTRA_SEASON = "season";

    private static final String PICK_A = "compare_pick_a";
    private static final String PICK_B = "compare_pick_b";
    private static final String PICK_YEAR = "compare_pick_year";
    private static final int FIRST_YEAR = 1950;
    private static final String DASH = "\u2013";

    private int type;
    private String keyA;
    private String keyB;
    private int currentSeason;

    private compareParticipant a;
    private compareParticipant b;
    private compareLoader.Season seasonA;
    private compareLoader.Season seasonB;

    private static final int TAB_CAREER = 0;
    private static final int TAB_SEASON = 1;
    private static final int TAB_DUEL = 2;

    private int tab = TAB_CAREER;
    private compareDuel.Result duel;
    private int duelToken = 0;

    private int year = -1;
    private int seasonToken = 0;
    private int loadToken = 0;

    private LinearLayout content;

    private compareSeries.Data chart;
    private int chartToken = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);
        setContentView(R.layout.compare_activity);
        setupInsets();

        WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView())
                .setAppearanceLightStatusBars(false);

        content = findViewById(R.id.compareContent);
        findViewById(R.id.backButton).setOnClickListener(v -> finish());

        Bundle extras = getIntent().getExtras();

        if (extras == null || extras.getString(EXTRA_FIRST) == null
                || extras.getString(EXTRA_SECOND) == null) {
            finish();
            return;
        }

        type = extras.getInt(EXTRA_TYPE, compareParticipant.DRIVER);
        keyA = extras.getString(EXTRA_FIRST);
        keyB = extras.getString(EXTRA_SECOND);

        try {
            currentSeason = Integer.parseInt(String.valueOf(extras.getString(EXTRA_SEASON)).trim());
        } catch (NumberFormatException error) {
            currentSeason = LocalDate.now().getYear();
        }

        getSupportFragmentManager().setFragmentResultListener(PICK_A, this,
                (key, result) -> replace(true, result.getString(comparePickerSheet.KEY)));
        getSupportFragmentManager().setFragmentResultListener(PICK_B, this,
                (key, result) -> replace(false, result.getString(comparePickerSheet.KEY)));
        getSupportFragmentManager().setFragmentResultListener(PICK_YEAR, this, (key, result) -> {
            year = result.getInt(yearPickerSheet.YEAR);
            seasonA = null;
            seasonB = null;
            duel = null;
            chart = null;

            if (tab == TAB_SEASON) {
                loadSeason();
            } else if (tab == TAB_DUEL) {
                loadDuel();
            }

            render();
        });

        loadParticipants();
    }

    private void setupInsets() {
        View root = findViewById(R.id.compareRoot);
        View header = findViewById(R.id.compareHeader);

        final int left = root.getPaddingLeft();
        final int top = root.getPaddingTop();
        final int right = root.getPaddingRight();
        final int bottom = root.getPaddingBottom();

        final int hl = header.getPaddingLeft();
        final int ht = header.getPaddingTop();
        final int hr = header.getPaddingRight();
        final int hb = header.getPaddingBottom();

        ViewCompat.setOnApplyWindowInsetsListener(root, (view, windowInsets) -> {
            Insets safe = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars()
                    | WindowInsetsCompat.Type.displayCutout());

            view.setPadding(left + safe.left, top, right + safe.right, bottom);
            header.setPadding(hl, ht + safe.top, hr, hb);

            return windowInsets;
        });

        ViewCompat.requestApplyInsets(root);
        EdgeInsets.applyBottom(findViewById(R.id.compareScroll));
    }

    // ---------- loading ----------

    private void loadParticipants() {
        a = null;
        b = null;
        seasonA = null;
        seasonB = null;
        duel = null;
        chart = null;
        year = -1;

        final int mine = ++loadToken;
        final compareParticipant[] loaded = new compareParticipant[2];
        final int[] pending = {2};

        render();

        compareLoader.load(type, keyA, p -> {
            loaded[0] = p;
            finishLoad(mine, loaded, pending);
        });
        compareLoader.load(type, keyB, p -> {
            loaded[1] = p;
            finishLoad(mine, loaded, pending);
        });
    }

    private void finishLoad(int mine, compareParticipant[] loaded, int[] pending) {
        if (--pending[0] > 0 || mine != loadToken || isFinishing() || isDestroyed()) {
            return;
        }

        if (loaded[0] == null || loaded[1] == null) {
            Toast.makeText(this, R.string.compare_load_error, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        a = loaded[0];
        b = loaded[1];
        year = defaultYear(years());

        if (tab == TAB_SEASON) {
            loadSeason();
        } else if (tab == TAB_DUEL) {
            loadDuel();
        }

        render();
    }

    private void replace(boolean first, String key) {
        if (key == null) {
            return;
        }

        if (first) {
            keyA = key;
        } else {
            keyB = key;
        }

        loadParticipants();
    }

    private void loadSeason() {
        if (a == null || b == null) {
            return;
        }

        if (year < 0) {
            year = defaultYear(years());
        }

        final int mine = ++seasonToken;
        final compareLoader.Season[] loaded = new compareLoader.Season[2];
        final int[] pending = {2};

        seasonA = null;
        seasonB = null;
        chart = null;

        compareLoader.season(a, year, s -> {
            loaded[0] = s;
            finishSeason(mine, loaded, pending);
        });
        compareLoader.season(b, year, s -> {
            loaded[1] = s;
            finishSeason(mine, loaded, pending);
        });

        loadChart();
    }

    private void loadChart() {
        if (a == null || b == null) {
            return;
        }

        final int mine = ++chartToken;

        compareSeries.Callback callback = data -> {
            if (mine != chartToken || isFinishing() || isDestroyed()) {
                return;
            }

            chart = data == null ? compareSeries.EMPTY : data;

            if (tab == TAB_SEASON) {
                render();
            }
        };

        compareSeries.load(a, b, year, callback);
    }

    private void finishSeason(int mine, compareLoader.Season[] loaded, int[] pending) {
        if (--pending[0] > 0 || mine != seasonToken || isFinishing() || isDestroyed()) {
            return;
        }

        seasonA = loaded[0];
        seasonB = loaded[1];

        if (tab == TAB_SEASON){
            render();
        }
    }

    private ArrayList<Integer> years() {
        ArrayList<Integer> list = new ArrayList<>();

        if (a == null || b == null) {
            return list;
        }

        int top = Math.min(Math.max(a.lastYear, b.lastYear), currentSeason);
        int bottom = Math.max(Math.min(smallest(a.firstYear), smallest(b.firstYear)), FIRST_YEAR);

        for (int y = top; y >= bottom; y--) {
            if (a.racedIn(y) || b.racedIn(y)) {
                list.add(y);
            }
        }

        return list;
    }

    private int smallest(int firstYear) {
        return firstYear > 0 ? firstYear : Integer.MAX_VALUE;
    }

    private int defaultYear(ArrayList<Integer> years) {
        for (int y : years) {
            if (a.racedIn(y) && b.racedIn(y)) {
                return y;
            }
        }

        return years.isEmpty() ? currentSeason : years.get(0);
    }

    // ---------- screen ----------

    private void render() {
        content.removeAllViews();

        if (a == null || b == null) {
            addProgress();
            return;
        }

        int colorA = a.color;
        int colorB = distance(a.color, b.color) < 60
                ? (distance(a.color, b.altColor) < 60 ? 0xFF8E8E93 : b.altColor)
                : b.color;

        content.addView(buildCards(colorA, colorB));
        content.addView(buildTabs());

        if (tab == TAB_SEASON) {
            renderSeason(colorA, colorB);
        } else if (tab == TAB_DUEL) {
            renderDuel(colorA, colorB);
        } else {
            renderCareer(colorA, colorB);
        }
    }

    private View buildCards(int colorA, int colorB) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);

        row.addView(card(a, true, colorA), new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView vs = text(getString(R.string.compare_vs), 14, R.color.text_color_thirdly);
        vs.setTypeface(vs.getTypeface(), Typeface.BOLD);
        vs.setPadding(dp(8), 0, dp(8), 0);
        row.addView(vs);

        row.addView(card(b, false, colorB), new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        return row;
    }

    private View card(compareParticipant p, boolean first, int stroke) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(14), dp(14), dp(14), dp(14));

        GradientDrawable background = new GradientDrawable();
        background.setColor(color(R.color.colorPrimary));
        background.setCornerRadius(dp(16));
        background.setStroke(dp(2), stroke);
        card.setBackground(background);

        if (!p.given.isEmpty()) {
            TextView given = text(p.given, 14, R.color.text_color_secondary);
            given.setSingleLine(true);
            given.setEllipsize(android.text.TextUtils.TruncateAt.END);
            card.addView(given);
        }

        TextView name = text(p.type == compareParticipant.DRIVER ? p.family : p.title,
                20, R.color.text_color_main);
        name.setTypeface(name.getTypeface(), Typeface.BOLD);
        name.setMaxLines(2);
        name.setEllipsize(android.text.TextUtils.TruncateAt.END);
        card.addView(name);

        if (!p.caption.isEmpty()) {
            TextView caption = text(p.caption, 13, R.color.text_color_thirdly);
            caption.setMaxLines(2);
            caption.setEllipsize(android.text.TextUtils.TruncateAt.END);
            card.addView(caption);
        }

        card.setClickable(true);
        card.setFocusable(true);
        card.setOnClickListener(v -> comparePickerSheet.show(getSupportFragmentManager(), type,
                first ? keyB : keyA, first ? PICK_A : PICK_B));

        return card;
    }

    private View buildTabs() {
        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setPadding(dp(4), dp(4), dp(4), dp(4));
        bar.setBackground(surface(color(R.color.account_settings_input_color), 14));

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.topMargin = dp(16);
        bar.setLayoutParams(params);

        addTab(bar, R.string.compare_tab_career, TAB_CAREER);
        addTab(bar, R.string.compare_tab_season, TAB_SEASON);
        addTab(bar, R.string.compare_tab_duel, TAB_DUEL);

        return bar;
    }

    private void addTab(LinearLayout bar, int label, int target) {
        bar.addView(tabView(label, tab == target, () -> {
            tab = target;

            if (target == TAB_SEASON && (seasonA == null || seasonB == null)) {
                loadSeason();
            }

            if (target == TAB_DUEL && duel == null) {
                loadDuel();
            }

            render();
        }), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
    }

    private TextView tabView(int label, boolean selected, Runnable action) {
        boolean light = checkLightTheme(this);

        TextView view = text(getString(label), 14,
                selected ? R.color.text_color_main : R.color.grey);
        view.setGravity(Gravity.CENTER);
        view.setMinHeight(dp(40));
        view.setTypeface(null, selected ? Typeface.BOLD : Typeface.NORMAL);
        view.setBackground(new RippleDrawable(
                ColorStateList.valueOf(light ? 0x16000000 : 0x22FFFFFF),
                surface(selected ? color(R.color.colorPrimary) : Color.TRANSPARENT, 10),
                surface(Color.WHITE, 10)));
        view.setElevation(selected ? dp(2) : 0);
        view.setOnClickListener(v -> action.run());
        return view;
    }

    private void renderCareer(int colorA, int colorB) {
        boolean driver = type == compareParticipant.DRIVER;

        addRow(R.string.compare_metric_titles, a.stat("titles"), b.stat("titles"),
                compareMath.Mode.HIGHER, false, colorA, colorB);
        addRow(R.string.compare_metric_wins, a.stat("wins"), b.stat("wins"),
                compareMath.Mode.HIGHER, false, colorA, colorB);
        addRow(R.string.compare_metric_podiums, a.stat("podiums"), b.stat("podiums"),
                compareMath.Mode.HIGHER, false, colorA, colorB);
        addRow(R.string.compare_metric_poles, a.stat("poles"), b.stat("poles"),
                compareMath.Mode.HIGHER, false, colorA, colorB);
        addRow(R.string.compare_metric_fastest, a.stat("fastest"), b.stat("fastest"),
                compareMath.Mode.HIGHER, false, colorA, colorB);

        if (driver) {
            addRow(R.string.compare_metric_gp, a.stat("gp"), b.stat("gp"),
                    compareMath.Mode.NEUTRAL, false, colorA, colorB);
        } else {
            addRow(R.string.compare_metric_entry, a.stat("enter"), b.stat("enter"),
                    compareMath.Mode.NEUTRAL, false, colorA, colorB);
        }

        addRow(R.string.compare_metric_points, a.stat("points"), b.stat("points"),
                compareMath.Mode.HIGHER, false, colorA, colorB);

        if (driver) {
            addRow(R.string.compare_metric_win_rate,
                    compareMath.rate(a.stat("wins"), a.stat("gp")),
                    compareMath.rate(b.stat("wins"), b.stat("gp")),
                    compareMath.Mode.HIGHER, true, colorA, colorB);
            addRow(R.string.compare_metric_podium_rate,
                    compareMath.rate(a.stat("podiums"), a.stat("gp")),
                    compareMath.rate(b.stat("podiums"), b.stat("gp")),
                    compareMath.Mode.HIGHER, true, colorA, colorB);
            addRow(R.string.compare_metric_pole_rate,
                    compareMath.rate(a.stat("poles"), a.stat("gp")),
                    compareMath.rate(b.stat("poles"), b.stat("gp")),
                    compareMath.Mode.HIGHER, true, colorA, colorB);
            addNote(R.string.compare_rates_note);
        }

        addNote(R.string.compare_points_note);
    }

    private void renderSeason(int colorA, int colorB) {
        addYearChip();

        if (seasonA == null || seasonB == null) {
            addProgress();
            return;
        }

        if (type == compareParticipant.DRIVER
                && (!seasonA.team.isEmpty() || !seasonB.team.isEmpty())) {
            TextView teams = text(getString(R.string.compare_season_teams,
                            seasonA.team.isEmpty() ? DASH : seasonA.team,
                            seasonB.team.isEmpty() ? DASH : seasonB.team),
                    13, R.color.text_color_thirdly);
            teams.setGravity(Gravity.CENTER);

            LinearLayout.LayoutParams teamParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            teamParams.topMargin = dp(10);
            content.addView(teams, teamParams);
        }

        addRow(R.string.compare_metric_place, seasonA.place, seasonB.place,
                compareMath.Mode.LOWER, false, colorA, colorB);
        addRow(R.string.compare_metric_points, seasonA.points, seasonB.points,
                compareMath.Mode.HIGHER, false, colorA, colorB);
        addRow(R.string.compare_metric_wins, seasonA.wins, seasonB.wins,
                compareMath.Mode.HIGHER, false, colorA, colorB);

        addChart(colorA, colorB);
        addNote(R.string.compare_no_data_note);
    }

    private void addChart(int colorA, int colorB) {
        TextView title = text(getString(R.string.compare_chart_title), 12, R.color.text_color_thirdly);
        title.setAllCaps(true);
        title.setGravity(Gravity.CENTER);

        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        titleParams.topMargin = dp(28);
        content.addView(title, titleParams);

        if (chart == null) {
            addProgress();
            return;
        }

        if (chart.a.length == 0) {
            addNote(R.string.compare_chart_empty);
            return;
        }

        TextView info = text("", 14, R.color.text_color_main);
        info.setGravity(Gravity.CENTER);

        LinearLayout.LayoutParams infoParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        infoParams.topMargin = dp(8);
        content.addView(info, infoParams);

        ChartView view = new ChartView(this);
        view.setContentDescription(getString(R.string.compare_chart_title) + ": "
                + format(chart.a[chart.a.length - 1], false) + " / "
                + format(chart.b[chart.b.length - 1], false));
        view.set(chart.a, chart.b, colorA, colorB, color(R.color.neuro_divider),
                color(R.color.text_color_thirdly), index -> info.setText(getString(
                        R.string.compare_chart_info, index + 1, raceTitle(chart.names[index]),
                        format(chart.a[index], false), format(chart.b[index], false))));

        LinearLayout.LayoutParams chartParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(220));
        chartParams.topMargin = dp(8);
        content.addView(view, chartParams);
    }

    private void addYearChip() {
        ArrayList<Integer> list = years();

        TextView chip = text(year + " \u25BE", 16, R.color.text_color_main);
        chip.setTypeface(chip.getTypeface(), Typeface.BOLD);
        chip.setGravity(Gravity.CENTER);
        chip.setMinHeight(dp(44));
        chip.setPadding(dp(20), dp(8), dp(20), dp(8));
        chip.setBackground(surface(color(R.color.account_settings_input_color), 12));
        chip.setOnClickListener(v -> {
            if (!list.isEmpty()) {
                yearPickerSheet.show(getSupportFragmentManager(), list, year, PICK_YEAR);
            }
        });

        LinearLayout.LayoutParams chipParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        chipParams.gravity = Gravity.CENTER_HORIZONTAL;
        chipParams.topMargin = dp(16);
        content.addView(chip, chipParams);
    }

    private void addRow(int label, double valueA, double valueB, compareMath.Mode mode,
                        boolean percent, int colorA, int colorB) {
        int leader = compareMath.leader(valueA, valueB, mode);
        double[] shares = compareMath.shares(valueA, valueB, mode);

        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);

        LinearLayout line = new LinearLayout(this);
        line.setOrientation(LinearLayout.HORIZONTAL);
        line.setGravity(Gravity.CENTER_VERTICAL);

        TextView left = text(format(valueA, percent), 18, R.color.text_color_main);
        left.setGravity(Gravity.START);
        left.setTypeface(left.getTypeface(), leader == compareMath.FIRST ? Typeface.BOLD : Typeface.NORMAL);

        TextView center = text(getString(label), 12, R.color.text_color_thirdly);
        center.setAllCaps(true);
        center.setGravity(Gravity.CENTER);

        TextView right = text(format(valueB, percent), 18, R.color.text_color_main);
        right.setGravity(Gravity.END);
        right.setTypeface(right.getTypeface(), leader == compareMath.SECOND ? Typeface.BOLD : Typeface.NORMAL);

        line.addView(left, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        line.addView(center, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.6f));
        line.addView(right, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        box.addView(line);

        DualBar bar = new DualBar(this);
        bar.set((float) shares[0], (float) shares[1], colorA, colorB,
                color(R.color.neuro_divider),
                leader == compareMath.SECOND, leader == compareMath.FIRST);

        LinearLayout.LayoutParams barParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(10));
        barParams.topMargin = dp(8);
        box.addView(bar, barParams);

        box.setContentDescription(getString(label) + ": " + format(valueA, percent)
                + " / " + format(valueB, percent));

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.topMargin = dp(18);
        content.addView(box, params);
    }

    private void addNote(int text) {
        TextView note = text(getString(text), 13, R.color.text_color_thirdly);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.topMargin = dp(16);
        content.addView(note, params);
    }

    private void addProgress() {
        ProgressBar bar = new ProgressBar(this);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.gravity = Gravity.CENTER_HORIZONTAL;
        params.topMargin = dp(32);
        content.addView(bar, params);
    }

    // ---------- helpers ----------

    private String format(double value, boolean percent) {
        if (Double.isNaN(value)) {
            return DASH;
        }

        if (percent) {
            return String.format(Locale.getDefault(), "%.1f%%", value);
        }

        if (value == Math.rint(value)) {
            return String.format(Locale.getDefault(), "%d", (long) value);
        }

        return String.format(Locale.getDefault(), "%.1f", value);
    }

    private static int distance(int x, int y) {
        int dr = Color.red(x) - Color.red(y);
        int dg = Color.green(x) - Color.green(y);
        int db = Color.blue(x) - Color.blue(y);
        return (int) Math.sqrt(dr * dr + dg * dg + db * db);
    }

    private TextView text(String value, int size, int colorRes) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color(colorRes));
        return view;
    }

    private GradientDrawable surface(int color, int radius) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(dp(radius));
        return drawable;
    }

    private int color(int id) {
        return ContextCompat.getColor(this, id);
    }

    private int dp(float value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    /** Two bars growing from the centre: the first to the left, the second to the right. */
    private static final class DualBar extends View {
        private final Paint track = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint barA = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint barB = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF rect = new RectF();
        private float shareA;
        private float shareB;

        DualBar(Context context) {
            super(context);
        }

        void set(float shareA, float shareB, int colorA, int colorB, int trackColor,
                 boolean dimA, boolean dimB) {
            this.shareA = shareA;
            this.shareB = shareB;

            track.setColor(trackColor);
            barA.setColor(colorA);
            barA.setAlpha(dimA ? 110 : 255);
            barB.setColor(colorB);
            barB.setAlpha(dimB ? 110 : 255);

            invalidate();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            float w = getWidth();
            float h = getHeight();
            float half = w / 2f - 3f;
            float r = h / 2f;

            rect.set(0, 0, half, h);
            canvas.drawRoundRect(rect, r, r, track);
            rect.set(w - half, 0, w, h);
            canvas.drawRoundRect(rect, r, r, track);

            float la = shareA * half;
            if (la > 0f) {
                rect.set(half - la, 0, half, h);
                canvas.drawRoundRect(rect, r, r, barA);
            }

            float lb = shareB * half;
            if (lb > 0f) {
                rect.set(w - half, 0, w - half + lb, h);
                canvas.drawRoundRect(rect, r, r, barB);
            }
        }
    }

    /** One bar split by the shares of the two scores. */
    private static final class ScoreBar extends View {
        private final Paint track = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint barA = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint barB = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF rect = new RectF();
        private int winsA;
        private int winsB;

        ScoreBar(Context context) {
            super(context);
        }

        void set(int winsA, int winsB, int colorA, int colorB, int trackColor) {
            this.winsA = winsA;
            this.winsB = winsB;
            track.setColor(trackColor);
            barA.setColor(colorA);
            barB.setColor(colorB);
            invalidate();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            float w = getWidth();
            float h = getHeight();
            float r = h / 2f;
            int total = winsA + winsB;

            if (total == 0) {
                rect.set(0, 0, w, h);
                canvas.drawRoundRect(rect, r, r, track);
                return;
            }

            float widthA = w * winsA / total;

            if (winsA > 0) {
                rect.set(0, 0, winsB > 0 ? Math.max(0f, widthA - 1.5f) : w, h);
                canvas.drawRoundRect(rect, r, r, barA);
            }

            if (winsB > 0) {
                rect.set(winsA > 0 ? widthA + 1.5f : 0, 0, w, h);
                canvas.drawRoundRect(rect, r, r, barB);
            }
        }
    }

    /** Two cumulative lines; a touch picks the round. */
    private static final class ChartView extends View {

        interface Listener {
            void onSelect(int index);
        }

        private final Paint lineA = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint lineB = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint grid = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint label = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint marker = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path path = new Path();
        private final float density;

        private double[] a = new double[0];
        private double[] b = new double[0];
        private int colorA;
        private int colorB;
        private int selected = -1;
        private float downX;
        private float downY;
        private Listener listener;

        ChartView(Context context) {
            super(context);
            density = context.getResources().getDisplayMetrics().density;

            for (Paint line : new Paint[]{lineA, lineB}) {
                line.setStyle(Paint.Style.STROKE);
                line.setStrokeWidth(3 * density);
                line.setStrokeJoin(Paint.Join.ROUND);
                line.setStrokeCap(Paint.Cap.ROUND);
            }

            grid.setStrokeWidth(density);
            label.setTextSize(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 10,
                    context.getResources().getDisplayMetrics()));
            marker.setStyle(Paint.Style.FILL);
        }

        void set(double[] a, double[] b, int colorA, int colorB, int gridColor, int textColor,
                 Listener listener) {
            this.a = a;
            this.b = b;
            this.colorA = colorA;
            this.colorB = colorB;
            this.listener = listener;
            lineA.setColor(colorA);
            lineB.setColor(colorB);
            grid.setColor(gridColor);
            label.setColor(textColor);
            selected = a.length - 1;

            if (listener != null && selected >= 0) {
                listener.onSelect(selected);
            }

            invalidate();
        }

        private static double niceMax(double max) {
            if (max <= 0) {
                return 10;
            }

            double step = Math.pow(10, Math.floor(Math.log10(max)));
            double m = max / step;
            double nice = m <= 1 ? 1 : m <= 2 ? 2 : m <= 5 ? 5 : 10;

            return nice * step;
        }

        private float left() {
            return 34 * density;
        }

        private float plotWidth() {
            return getWidth() - left() - 10 * density;
        }

        private float x(int index) {
            int n = a.length;
            return n == 1 ? left() + plotWidth() / 2f : left() + plotWidth() * index / (n - 1);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            int n = a.length;

            if (n == 0) {
                return;
            }

            float padTop = 8 * density;
            float plotHeight = getHeight() - padTop - 8 * density;

            double max = 0;
            for (int i = 0; i < n; i++) {
                max = Math.max(max, Math.max(a[i], b[i]));
            }
            double scale = niceMax(max);

            for (int i = 0; i <= 4; i++) {
                float y = padTop + plotHeight - plotHeight * i / 4f;
                canvas.drawLine(left(), y, left() + plotWidth(), y, grid);
                canvas.drawText(String.valueOf((long) Math.round(scale * i / 4)), 0,
                        y - 3 * density, label);
            }

            draw(canvas, a, lineA, padTop, plotHeight, scale);
            draw(canvas, b, lineB, padTop, plotHeight, scale);

            if (selected >= 0 && selected < n) {
                float sx = x(selected);
                canvas.drawLine(sx, padTop, sx, padTop + plotHeight, grid);

                marker.setColor(colorA);
                canvas.drawCircle(sx, y(a[selected], padTop, plotHeight, scale), 5 * density, marker);
                marker.setColor(colorB);
                canvas.drawCircle(sx, y(b[selected], padTop, plotHeight, scale), 5 * density, marker);
            }
        }

        private float y(double value, float padTop, float plotHeight, double scale) {
            return padTop + plotHeight - (float) (value / scale) * plotHeight;
        }

        private void draw(Canvas canvas, double[] values, Paint line, float padTop,
                          float plotHeight, double scale) {
            path.reset();

            for (int i = 0; i < values.length; i++) {
                float px = x(i);
                float py = y(values[i], padTop, plotHeight, scale);

                if (i == 0) {
                    path.moveTo(px, py);
                } else {
                    path.lineTo(px, py);
                }
            }

            canvas.drawPath(path, line);
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            if (a.length == 0) {
                return false;
            }

            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    downX = event.getX();
                    downY = event.getY();
                    select(event.getX());
                    return true;

                case MotionEvent.ACTION_MOVE:
                    if (Math.abs(event.getX() - downX) > Math.abs(event.getY() - downY)) {
                        getParent().requestDisallowInterceptTouchEvent(true);
                        select(event.getX());
                    }
                    return true;

                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    getParent().requestDisallowInterceptTouchEvent(false);
                    return true;

                default:
                    return super.onTouchEvent(event);
            }
        }

        private void select(float touchX) {
            int n = a.length;
            float width = plotWidth();
            int index = n == 1 || width <= 0 ? 0
                    : Math.round((touchX - left()) / width * (n - 1));
            index = Math.max(0, Math.min(n - 1, index));

            if (index != selected) {
                selected = index;
                invalidate();

                if (listener != null) {
                    listener.onSelect(index);
                }
            }
        }
    }

    private void loadDuel() {
        if (a == null || b == null) {
            return;
        }

        if (year < 0) {
            year = defaultYear(years());
        }

        final int mine = ++duelToken;
        duel = null;

        final compareDuel.Callback callback = result -> {
            if (mine != duelToken || isFinishing() || isDestroyed()) {
                return;
            }

            duel = result == null ? compareDuel.EMPTY : result;

            if (tab == TAB_DUEL) {
                render();
            }
        };

        if (type == compareParticipant.TEAM) {
            compareDuel.loadTeams(a, b, year, callback);
        } else {
            compareDuel.load(a, b, year, callback);
        }
    }

    private void renderDuel(int colorA, int colorB) {
        boolean teams = type == compareParticipant.TEAM;

        addYearChip();
        addLegend(colorA, colorB);

        if (duel == null) {
            addProgress();
            return;
        }

        if (duel.rounds.isEmpty()) {
            addNote(R.string.compare_duel_empty);
            return;
        }

        addScore(R.string.compare_duel_race, duel.raceA, duel.raceB, colorA, colorB);
        addScore(R.string.compare_duel_quali, duel.qualiA, duel.qualiB, colorA, colorB);

        addRow(teams ? R.string.compare_duel_avg_quali_team : R.string.compare_duel_avg_quali,
                duel.avgQualiA, duel.avgQualiB, compareMath.Mode.LOWER, false, colorA, colorB);
        addRow(teams ? R.string.compare_duel_avg_race_team : R.string.compare_duel_avg_race,
                duel.avgRaceA, duel.avgRaceB, compareMath.Mode.LOWER, false, colorA, colorB);

        if (teams) {
            addRow(R.string.compare_metric_points, duel.pointsA, duel.pointsB,
                    compareMath.Mode.HIGHER, false, colorA, colorB);
        }

        addRow(R.string.compare_duel_retired, duel.retiredA, duel.retiredB,
                compareMath.Mode.LOWER, false, colorA, colorB);

        addRoundRow(getString(R.string.compare_duel_round),
                headerCell(getString(R.string.compare_duel_quali_short)),
                headerCell(getString(R.string.compare_duel_race_short)),
                teams ? headerCell(getString(R.string.compare_duel_points_short)) : null,
                true);

        for (compareDuel.Round round : duel.rounds) {
            addRoundRow(raceTitle(round.name),
                    duelCell(round.qualiA, round.qualiB, colorA, colorB),
                    duelCell(round.raceA, round.raceB, colorA, colorB),
                    teams ? pointsCell(round.pointsA, round.pointsB, colorA, colorB) : null,
                    false);
        }

        addNote(teams ? R.string.compare_duel_note_team : R.string.compare_duel_note);
    }

    private void addScore(int title, int winsA, int winsB, int colorA, int colorB) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);

        TextView label = text(getString(title), 12, R.color.text_color_thirdly);
        label.setAllCaps(true);
        label.setGravity(Gravity.CENTER);
        box.addView(label);

        LinearLayout line = new LinearLayout(this);
        line.setOrientation(LinearLayout.HORIZONTAL);
        line.setGravity(Gravity.CENTER);

        TextView left = badge(String.valueOf(winsA), 30, colorA, winsA > winsB);
        left.setTypeface(left.getTypeface(), winsA > winsB ? Typeface.BOLD : Typeface.NORMAL);
        TextView colon = text(" : ", 24, R.color.text_color_thirdly);
        TextView right = badge(String.valueOf(winsB), 30, colorB, winsB > winsA);
        right.setTypeface(right.getTypeface(), winsB > winsA ? Typeface.BOLD : Typeface.NORMAL);

        line.addView(left);
        line.addView(colon);
        line.addView(right);
        box.addView(line);

        ScoreBar bar = new ScoreBar(this);
        bar.set(winsA, winsB, colorA, colorB, color(R.color.neuro_divider));

        LinearLayout.LayoutParams barParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(10));
        barParams.topMargin = dp(6);
        box.addView(bar, barParams);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.topMargin = dp(20);
        content.addView(box, params);
    }

    private void addRoundRow(String name, View quali, View race, boolean header) {
        addRoundRow(name, quali, race, null, header);
    }

    private void addRoundRow(String name, View quali, View race, View points, boolean header) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(header ? 20 : 6), 0, dp(6));

        TextView nameView = text(name, header ? 12 : 14,
                header ? R.color.text_color_thirdly : R.color.text_color_main);
        nameView.setAllCaps(header);
        nameView.setMaxLines(1);
        nameView.setEllipsize(android.text.TextUtils.TruncateAt.END);

        row.addView(nameView, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, points != null ? 1.2f : 1.5f));
        row.addView(quali, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 0.8f));
        row.addView(race, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 0.8f));

        if (points != null) {
            row.addView(points, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 0.8f));
        }

        content.addView(row, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
    }

    private CharSequence pair(int x, int y) {
        if (x <= 0 || y <= 0) {
            return DASH;
        }

        SpannableStringBuilder text = new SpannableStringBuilder(x + " \u2013 " + y);

        if (x != y) {
            int start = x < y ? 0 : text.length() - String.valueOf(y).length();
            int end = x < y ? String.valueOf(x).length() : text.length();
            text.setSpan(new StyleSpan(Typeface.BOLD), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        return text;
    }

    private String raceTitle(String raceName) {
        if (raceName == null || raceName.isEmpty()) {
            return DASH;
        }

        int id = getStringByName(raceName.toLowerCase(Locale.ROOT).replaceAll("\\s+", "_") + "_text");
        return id != 0 ? getString(id) : raceName;
    }

    private TextView badge(String value, int size, int fill, boolean winner) {
        TextView view = text(value, size, R.color.text_color_main);
        view.setGravity(Gravity.CENTER);
        view.setMinWidth(dp(size > 20 ? 44 : 28));
        view.setPadding(dp(6), dp(2), dp(6), dp(2));

        if (winner) {
            view.setTypeface(view.getTypeface(), Typeface.BOLD);
            view.setTextColor(ColorUtils.calculateLuminance(fill) > 0.5 ? Color.BLACK : Color.WHITE);
            view.setBackground(surface(fill, 8));
        }

        return view;
    }

    private View pairCell(String x, String y, int winner, int colorA, int colorB) {
        LinearLayout cell = new LinearLayout(this);
        cell.setOrientation(LinearLayout.HORIZONTAL);
        cell.setGravity(Gravity.CENTER);

        TextView dash = text("\u2013", 14, R.color.text_color_thirdly);
        dash.setPadding(dp(4), 0, dp(4), 0);

        cell.addView(badge(x, 14, colorA, winner == 1));
        cell.addView(dash);
        cell.addView(badge(y, 14, colorB, winner == 2));

        return cell;
    }

    private View duelCell(int x, int y, int colorA, int colorB) {
        if (x <= 0 || y <= 0) {
            return dashCell();
        }

        return pairCell(String.valueOf(x), String.valueOf(y),
                x < y ? 1 : (y < x ? 2 : 0), colorA, colorB);
    }

    private View pointsCell(double x, double y, int colorA, int colorB) {
        if (Double.isNaN(x) || Double.isNaN(y)) {
            return dashCell();
        }

        return pairCell(format(x, false), format(y, false),
                x > y ? 1 : (y > x ? 2 : 0), colorA, colorB);
    }

    private View dashCell() {
        LinearLayout cell = new LinearLayout(this);
        cell.setGravity(Gravity.CENTER);
        cell.addView(text(DASH, 14, R.color.text_color_thirdly));
        return cell;
    }

    private TextView headerCell(String value) {
        TextView view = text(value, 12, R.color.text_color_thirdly);
        view.setAllCaps(true);
        view.setGravity(Gravity.CENTER);
        return view;
    }

    private void addLegend(int colorA, int colorB) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER);

        row.addView(legendItem(shortName(a), colorA));

        View gap = new View(this);
        row.addView(gap, new LinearLayout.LayoutParams(dp(20), 1));

        row.addView(legendItem(shortName(b), colorB));

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.topMargin = dp(14);
        content.addView(row, params);
    }

    private View legendItem(String name, int fill) {
        LinearLayout item = new LinearLayout(this);
        item.setOrientation(LinearLayout.HORIZONTAL);
        item.setGravity(Gravity.CENTER_VERTICAL);

        GradientDrawable dot = new GradientDrawable();
        dot.setShape(GradientDrawable.OVAL);
        dot.setColor(fill);

        View marker = new View(this);
        marker.setBackground(dot);
        LinearLayout.LayoutParams markerParams = new LinearLayout.LayoutParams(dp(10), dp(10));
        markerParams.setMarginEnd(dp(6));
        item.addView(marker, markerParams);

        item.addView(text(name, 13, R.color.text_color_secondary));
        return item;
    }

    private String shortName(compareParticipant p) {
        return p.type == compareParticipant.DRIVER ? p.family : p.title;
    }
}