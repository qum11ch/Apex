package com.example.f1app;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.storage.FirebaseStorage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class leaderboardScreenController {

    public interface ScopeAppearance {
        void apply(String scope);
    }

    private static final String PREFS = "leaderboard_screen";
    private static final String PREF_SCOPE = "scope";
    private static final String PREF_NEAR = "near";
    private static final int TOP_LIMIT = 50;
    private static final int NEIGHBOR_LIMIT = 3;

    private final AppCompatActivity activity;
    private final ScopeAppearance appearance;
    private final FirebaseDatabase db = FirebaseDatabase.getInstance();

    private final RecyclerView list;
    private final Rows adapter;
    private final SwipeRefreshLayout swipe;
    private final ProgressBar progress;
    private final TextView empty;
    private final TextView error;
    private final Button retry;
    private final Button topButton;
    private final Button nearButton;

    private final LinearLayout personal;
    private final TextView personalTitle;
    private final TextView personalMessage;
    private final Button personalRetry;
    private final RecyclerView personalList;
    private final Rows personalAdapter;

    private String scope;
    private final String season;
    private boolean near;
    private boolean destroyed;
    private int generation;
    private leaderboardEntry me;

    private TextView personalSummary;
    private View personalDivider;

    public leaderboardScreenController(
            AppCompatActivity activity,
            String season,
            ScopeAppearance appearance
    ) {
        this.activity = activity;
        this.appearance = appearance;
        this.season = season;

        scope = activity.getPreferences(Context.MODE_PRIVATE)
                .getString(PREF_SCOPE, "season");
        near = activity.getPreferences(Context.MODE_PRIVATE)
                .getBoolean(PREF_NEAR, false);

        if (!"global".equals(scope)) scope = "season";

        list = activity.findViewById(R.id.leaderboardList);
        progress = activity.findViewById(R.id.leaderboardProgress);
        empty = activity.findViewById(R.id.leaderboardEmpty);
        error = activity.findViewById(R.id.leaderboardError);

        View scroll = activity.findViewById(R.id.contentScroll);
        LinearLayout parent = (LinearLayout) scroll.getParent();
        int scrollIndex = parent.indexOfChild(scroll);

        // Список становится единственным прокручиваемым контейнером.
        ((ViewGroup) list.getParent()).removeView(list);
        ((ViewGroup) empty.getParent()).removeView(empty);
        ((ViewGroup) error.getParent()).removeView(error);
        parent.removeView(scroll);

        LinearLayout modes = new LinearLayout(activity);
        modes.setOrientation(LinearLayout.HORIZONTAL);
        modes.setPadding(dp(16), 0, dp(16), 0);

        topButton = button(R.string.lb_top);
        nearButton = button(R.string.lb_near);

        modes.addView(topButton, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1
        ));
        modes.addView(nearButton, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1
        ));
        parent.addView(modes, scrollIndex++);

        retry = button(R.string.lb_retry);
        retry.setVisibility(View.GONE);

        parent.addView(empty, scrollIndex++);
        parent.addView(error, scrollIndex++);
        parent.addView(retry, scrollIndex++);

        swipe = new SwipeRefreshLayout(activity);
        swipe.setColorSchemeColors(color(R.color.text_color_main));
        swipe.setProgressBackgroundColorSchemeColor(color(R.color.colorPrimary));

        list.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));
        list.setNestedScrollingEnabled(true);
        list.setClipToPadding(false);
        list.setPadding(0, dp(8), 0, dp(16));
        list.setLayoutManager(new LinearLayoutManager(activity));

        list.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(
                    RecyclerView recyclerView,
                    int dx,
                    int dy
            ) {
                schedulePersonalVisibility();
            }
        });

        list.addOnLayoutChangeListener(
                (view, left, top, right, bottom,
                 oldLeft, oldTop, oldRight, oldBottom) ->
                        schedulePersonalVisibility()
        );

        adapter = new Rows();
        list.setAdapter(adapter);
        swipe.addView(list);
        parent.addView(swipe, scrollIndex, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1
        ));

        personal = new LinearLayout(activity);
        personal.setOrientation(LinearLayout.VERTICAL);
        personal.setPadding(dp(16), dp(10), dp(16), dp(8));
        personal.setBackgroundColor(color(R.color.colorPrimary));

        personalTitle = text(12);
        personalTitle.setTypeface(null, Typeface.BOLD);
        personalTitle.setPadding(0, 0, 0, dp(6));
        personalTitle.setTextColor(color(R.color.neuro_secondary));
        personal.addView(personalTitle);

        personalMessage = text(14);
        personalMessage.setPadding(dp(16), dp(6), dp(16), dp(6));
        personal.addView(personalMessage);

        personalRetry = button(R.string.lb_retry);
        personal.addView(personalRetry);

        personalList = new RecyclerView(activity);
        personalList.setLayoutManager(new LinearLayoutManager(activity));
        personalList.setNestedScrollingEnabled(false);
        personalAdapter = new Rows();
        personalList.setAdapter(personalAdapter);
        personal.addView(personalList, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        personal.setBackgroundColor(color(R.color.colorPrimary));

        personalSummary = text(14);
        personalSummary.setTypeface(null, Typeface.BOLD);
        personalSummary.setPadding(0, dp(8), 0, dp(8));
        personalSummary.setVisibility(View.GONE);

        personal.addView(personalSummary, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        personalDivider  = new View(activity);
        personalDivider.setBackgroundColor(color(R.color.stroke_option_color));

        parent.addView(personalDivider, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(1)
        ));

        parent.addView(personal, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));


        activity.findViewById(R.id.seasonScopeButton)
                .setOnClickListener(v -> selectScope("season"));
        activity.findViewById(R.id.globalScopeButton)
                .setOnClickListener(v -> selectScope("global"));

        topButton.setOnClickListener(v -> selectMode(false));
        nearButton.setOnClickListener(v -> selectMode(true));
        retry.setOnClickListener(v -> reload());
        personalRetry.setOnClickListener(v -> reload());
        swipe.setOnRefreshListener(this::reload);

        setupInsets();
        reload();
    }

    private void selectScope(String value) {
        if (value.equals(scope)) return;
        scope = value;
        save();
        reload();
    }

    private void selectMode(boolean value) {
        if (near == value) return;
        near = value;
        save();
        reload();
    }

    private void save() {
        activity.getPreferences(Context.MODE_PRIVATE).edit()
                .putString(PREF_SCOPE, scope)
                .putBoolean(PREF_NEAR, near)
                .apply();
    }

    private boolean valid(int request) {
        return !destroyed
                && !activity.isFinishing()
                && !activity.isDestroyed()
                && request == generation;
    }

    private String databaseScope() {
        return "global".equals(scope) ? "global" : season;
    }

    public void reload() {
        final int request = ++generation;
        final String uid = FirebaseAuth.getInstance().getUid();

        appearance.apply(scope);
        styleMode(topButton, !near);
        styleMode(nearButton, near);

        personalTitle.setText(activity.getString(
                R.string.lb_personal_title,
                "global".equals(scope)
                        ? activity.getString(R.string.leaderboard_global)
                        : activity.getString(R.string.leaderboard_season)
                        + " " + season
        ));

        me = null;
        personal.setVisibility(View.VISIBLE);
        personalDivider.setVisibility(View.VISIBLE);
        personalSummary.setVisibility(View.GONE);
        personalTitle.setVisibility(View.VISIBLE);

        adapter.submit(Collections.emptyList());
        personalAdapter.submit(Collections.emptyList());
        personalList.setVisibility(View.GONE);
        personalRetry.setVisibility(View.GONE);
        personalMessage.setVisibility(View.VISIBLE);
        personalMessage.setText(R.string.lb_loading);

        empty.setVisibility(View.GONE);
        error.setVisibility(View.GONE);
        retry.setVisibility(View.GONE);
        progress.setVisibility(View.VISIBLE);

        Query base = db.getReference("leaderboard").child(databaseScope());
        Task<DataSnapshot> topTask = base.orderByChild("rank")
                .limitToFirst(TOP_LIMIT).get();

        if (uid == null) {
            personalMessage.setText(R.string.lb_sign_in);
            finishTopOnly(topTask, request, uid);
            return;
        }

        Task<DataSnapshot> meTask = db.getReference("leaderboard")
                .child(databaseScope()).child(uid).get();

        Tasks.whenAllComplete(topTask, meTask).addOnCompleteListener(done -> {
            if (!valid(request)) return;

            if (!meTask.isSuccessful()) {
                personalMessage.setText(R.string.lb_personal_error);
                personalRetry.setVisibility(View.VISIBLE);
            } else if (!meTask.getResult().exists()) {
                personalMessage.setText(R.string.lb_not_ranked);
            } else {
                me = parse(meTask.getResult(), uid);

                if (me.rank <= 0) {
                    me = null;
                    personalMessage.setText(R.string.lb_personal_error);
                    personalRetry.setVisibility(View.VISIBLE);
                } else {
                    personalMessage.setVisibility(View.GONE);
                    personalRetry.setVisibility(View.GONE);

                    personalTitle.setVisibility(View.GONE);
                    personalList.setVisibility(View.GONE);

                    personalSummary.setText(activity.getString(
                            R.string.lb_personal_summary,
                            me.rank,
                            me.points
                    ));
                    personalSummary.setVisibility(View.VISIBLE);

                    schedulePersonalVisibility();
                }
            }

            if (near) {
                if (me != null) {
                    loadNeighbors(request, uid);
                } else {
                    finishLoading();
                    empty.setText(meTask.isSuccessful()
                            ? R.string.lb_near_unavailable
                            : R.string.lb_personal_error);
                    empty.setVisibility(View.VISIBLE);
                    if (!meTask.isSuccessful()) retry.setVisibility(View.VISIBLE);
                }
            } else {
                showTop(topTask, request, uid);
            }
        });
    }

    private void finishTopOnly(
            Task<DataSnapshot> task, int request, String uid
    ) {
        task.addOnCompleteListener(done -> {
            if (!valid(request)) return;
            if (near) {
                finishLoading();
                empty.setText(R.string.lb_sign_in);
                empty.setVisibility(View.VISIBLE);
            } else {
                showTop(task, request, uid);
            }
        });
    }

    private void schedulePersonalVisibility() {
        list.post(() -> {
            if (destroyed
                    || activity.isFinishing()
                    || activity.isDestroyed()) {
                return;
            }

            updatePersonalVisibility();
        });
    }

    private void updatePersonalVisibility() {
        boolean ownRowVisible = false;

        if (me != null) {
            android.graphics.Rect viewport = new android.graphics.Rect();

            if (list.getGlobalVisibleRect(viewport)) {
                for (int i = 0; i < list.getChildCount(); i++) {
                    View child = list.getChildAt(i);
                    int position = list.getChildAdapterPosition(child);

                    if (!adapter.isOwnPosition(position)) continue;

                    android.graphics.Rect row = new android.graphics.Rect();

                    if (child.getGlobalVisibleRect(row)) {
                        int visibleHeight =
                                Math.min(row.bottom, viewport.bottom)
                                        - Math.max(row.top, viewport.top);

                        // Считаем строку видимой, когда читается
                        // хотя бы половина её высоты.
                        if (visibleHeight >= child.getHeight() / 2) {
                            ownRowVisible = true;
                            break;
                        }
                    }
                }
            }
        }

        int visibility = ownRowVisible ? View.GONE : View.VISIBLE;

        if (personal.getVisibility() != visibility) {
            personal.setVisibility(visibility);
        }

        if (personalDivider.getVisibility() != visibility) {
            personalDivider.setVisibility(visibility);
        }
    }

    private void showTop(
            Task<DataSnapshot> task, int request, String uid
    ) {
        if (!valid(request)) return;
        if (!task.isSuccessful()) {
            android.util.Log.e(
                    "Leaderboard",
                    "Top query failed; scope=" + databaseScope()
                            + ", limit=" + TOP_LIMIT,
                    task.getException()
            );
            showError();
            return;
        }

        List<leaderboardEntry> entries = parseList(task.getResult(), uid);
        showRows(entries, request);
    }

    private void loadNeighbors(int request, String uid) {
        final leaderboardEntry own = me;

        if (own == null) {
            if (valid(request)) {
                finishLoading();
                empty.setText(R.string.lb_near_unavailable);
                empty.setVisibility(View.VISIBLE);
            }
            return;
        }

        Query base = db.getReference("leaderboard")
                .child(databaseScope());

        Task<DataSnapshot> above = base.orderByChild("rank")
                .endBefore((double) own.rank)
                .limitToLast(NEIGHBOR_LIMIT)
                .get();

        Task<DataSnapshot> below = base.orderByChild("rank")
                .startAfter((double) own.rank)
                .limitToFirst(NEIGHBOR_LIMIT)
                .get();

        Task<DataSnapshot> tied = base.orderByChild("rank")
                .equalTo((double) own.rank)
                .limitToFirst(7)
                .get();

        Tasks.whenAllComplete(above, below, tied)
                .addOnCompleteListener(done -> {
                    if (!valid(request)) return;

                    boolean failed = false;

                    if (!above.isSuccessful()) {
                        failed = true;
                        android.util.Log.e(
                                "Leaderboard",
                                "Neighbors above failed; scope="
                                        + databaseScope(),
                                above.getException()
                        );
                    }

                    if (!below.isSuccessful()) {
                        failed = true;
                        android.util.Log.e(
                                "Leaderboard",
                                "Neighbors below failed; scope="
                                        + databaseScope(),
                                below.getException()
                        );
                    }

                    if (!tied.isSuccessful()) {
                        failed = true;
                        android.util.Log.e(
                                "Leaderboard",
                                "Equal-rank query failed; scope="
                                        + databaseScope(),
                                tied.getException()
                        );
                    }

                    if (failed) {
                        showError();
                        return;
                    }

                    List<leaderboardEntry> entries = new ArrayList<>();
                    Set<String> used = new HashSet<>();

                    for (Task<DataSnapshot> task :
                            java.util.Arrays.asList(above, tied, below)) {

                        DataSnapshot snapshot = task.getResult();

                        if (snapshot == null) {
                            android.util.Log.e(
                                    "Leaderboard",
                                    "Neighbors query returned null snapshot"
                            );
                            showError();
                            return;
                        }

                        for (leaderboardEntry entry :
                                parseList(snapshot, uid)) {
                            if (used.add(entry.uid)) {
                                entries.add(entry);
                            }
                        }
                    }

                    if (used.add(own.uid)) {
                        entries.add(own);
                    }

                    showRows(entries, request);
                });
    }

    private void showRows(List<leaderboardEntry> entries, int request) {
        entries.sort((a, b) -> {
            int result = Integer.compare(a.rank, b.rank);
            if (result != 0) return result;
            return a.uid.compareTo(b.uid);
        });

        finishLoading();
        adapter.submit(entries);
        schedulePersonalVisibility();

        if (entries.isEmpty()) {
            empty.setText(R.string.leaderboard_empty);
            empty.setVisibility(View.VISIBLE);
        }

        list.scrollToPosition(0);
        loadNames(entries, request, adapter::notifyDataSetChanged);
    }

    private void loadNames(
            List<leaderboardEntry> entries, int request, Runnable finished
    ) {
        if (entries.isEmpty()) return;

        List<Task<DataSnapshot>> tasks = new ArrayList<>();
        for (leaderboardEntry entry : entries) {
            tasks.add(db.getReference("publicProfiles")
                    .child(entry.uid).child("username").get());
        }

        Tasks.whenAllComplete(tasks).addOnCompleteListener(done -> {
            if (!valid(request)) return;

            for (int i = 0; i < entries.size(); i++) {
                Task<DataSnapshot> task = tasks.get(i);
                if (task.isSuccessful()) {
                    Object value = task.getResult().getValue();
                    if (value != null) {
                        entries.get(i).username = value.toString();
                    }
                }
            }

            finished.run();
        });
    }

    private leaderboardEntry parse(DataSnapshot snapshot, String uid) {
        leaderboardEntry entry = new leaderboardEntry();
        entry.uid = snapshot.getKey();
        entry.rank = number(snapshot.child("rank").getValue());
        entry.points = number(snapshot.child("totalPoints").getValue());
        entry.correct = number(
                snapshot.child("correctPredictions").getValue()
        );
        entry.isMe = entry.uid != null && entry.uid.equals(uid);
        return entry;
    }

    private List<leaderboardEntry> parseList(
            DataSnapshot snapshot, String uid
    ) {
        List<leaderboardEntry> result = new ArrayList<>();
        for (DataSnapshot child : snapshot.getChildren()) {
            leaderboardEntry entry = parse(child, uid);
            if (entry.uid != null && entry.rank > 0) result.add(entry);
        }
        return result;
    }

    private int number(Object value) {
        if (value instanceof Number) return ((Number) value).intValue();
        if (value != null) {
            try {
                return (int) Math.round(Double.parseDouble(value.toString()));
            } catch (NumberFormatException ignored) {
            }
        }
        return 0;
    }

    private void finishLoading() {
        progress.setVisibility(View.GONE);
        swipe.setRefreshing(false);
    }

    private void showError() {
        finishLoading();
        error.setText(R.string.leaderboard_error);
        error.setVisibility(View.VISIBLE);
        retry.setVisibility(View.VISIBLE);
    }

    private void setupInsets() {
        View root = activity.findViewById(R.id.leaderboardRoot);
        View header = activity.findViewById(R.id.header);

        final int left = root.getPaddingLeft();
        final int top = root.getPaddingTop();
        final int right = root.getPaddingRight();
        final int bottom = root.getPaddingBottom();

        final int headerLeft = header.getPaddingLeft();
        final int headerTop = header.getPaddingTop();
        final int headerRight = header.getPaddingRight();
        final int headerBottom = header.getPaddingBottom();

        final int panelBottom = personal.getPaddingBottom();

        ViewCompat.setOnApplyWindowInsetsListener(root, (view, insets) -> {
            Insets safe = insets.getInsets(
                    WindowInsetsCompat.Type.systemBars()
                            | WindowInsetsCompat.Type.displayCutout()
            );

            view.setPadding(
                    left + safe.left,
                    top,
                    right + safe.right,
                    bottom + safe.bottom
            );

            header.setPadding(
                    headerLeft, headerTop + safe.top, headerRight, headerBottom
            );

            personal.setPadding(
                    personal.getPaddingLeft(),
                    personal.getPaddingTop(),
                    personal.getPaddingRight(),
                    panelBottom
            );

            return insets;
        });

        ViewCompat.requestApplyInsets(root);
    }

    private Button button(int stringId) {
        androidx.appcompat.widget.AppCompatButton button =
                new androidx.appcompat.widget.AppCompatButton(activity);
        button.setText(stringId);
        button.setTextSize(13);
        button.setAllCaps(false);
        button.setMinHeight(dp(48));
        button.setTextColor(color(R.color.text_color_main));
        button.setBackgroundTintList(null);
        button.setStateListAnimator(null);
        ViewCompat.setBackgroundTintList(button, null);
        button.setBackgroundColor(Color.TRANSPARENT);
        return button;
    }

    private TextView text(int size) {
        TextView text = new TextView(activity);
        text.setTextSize(size);
        text.setTextColor(color(R.color.text_color_main));
        return text;
    }

    private void styleMode(Button button, boolean selected) {
        button.setSelected(selected);
        button.setAlpha(1f);
        button.setElevation(0f);
        button.setStateListAnimator(null);

        button.setTextColor(color(
                selected
                        ? R.color.text_color_main
                        : R.color.neuro_secondary
        ));

        button.setTypeface(
                null,
                selected ? Typeface.BOLD : Typeface.NORMAL
        );

        button.setBackgroundTintList(null);
        ViewCompat.setBackgroundTintList(button, null);

        if (!selected) {
            button.setBackgroundResource(android.R.color.transparent);
            return;
        }

        android.graphics.drawable.GradientDrawable indicator =
                new android.graphics.drawable.GradientDrawable();

        indicator.setColor(color(R.color.text_color_main));

        android.graphics.drawable.LayerDrawable background =
                new android.graphics.drawable.LayerDrawable(
                        new android.graphics.drawable.Drawable[]{indicator}
                );

        background.setLayerHeight(0, dp(2));
        background.setLayerGravity(
                0,
                android.view.Gravity.BOTTOM
                        | android.view.Gravity.FILL_HORIZONTAL
        );

        button.setBackground(background);
    }

    private int color(int id) {
        return ContextCompat.getColor(activity, id);
    }

    private int dp(float value) {
        return Math.round(
                value * activity.getResources().getDisplayMetrics().density
        );
    }

    public void destroy() {
        destroyed = true;
        generation++;
    }

    private final class Rows extends RecyclerView.Adapter<RowHolder> {
        private final List<leaderboardEntry> entries = new ArrayList<>();

        void submit(List<leaderboardEntry> values) {
            entries.clear();
            entries.addAll(values);
            notifyDataSetChanged();
        }

        boolean isOwnPosition(int position) {
            return position >= 0
                    && position < entries.size()
                    && entries.get(position).isMe;
        }

        @Override
        public RowHolder onCreateViewHolder(ViewGroup parent, int type) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_leaderboard_row, parent, false);
            return new RowHolder(view);
        }

        @Override
        public void onBindViewHolder(RowHolder holder, int position) {
            leaderboardEntry entry = entries.get(position);

            holder.itemView.setOnClickListener(v ->
                    openPublicProfile(entry.uid)
            );

            String username = entry.username == null
                    || entry.username.trim().isEmpty()
                    ? "—" : entry.username.trim();

            holder.rank.setText(String.valueOf(entry.rank));
            holder.name.setText(entry.isMe
                    ? activity.getString(R.string.lb_name_you, username)
                    : username);
            holder.points.setText(String.valueOf(entry.points));
            holder.correct.setText(activity.getString(
                    R.string.lb_correct, entry.correct
            ));

            holder.itemView.setAlpha(1f);
            holder.itemView.setSelected(entry.isMe);

            View row = holder.itemView.findViewById(R.id.lbRow);

            row.setBackgroundColor(color(
                    entry.isMe
                            ? R.color.neuro_selected
                            : R.color.colorPrimary
            ));

            holder.boundUid = entry.uid;
            GlideApp.with(holder.avatar).clear(holder.avatar);
            holder.avatar.setVisibility(View.GONE);
            holder.initial.setVisibility(View.VISIBLE);
            holder.initial.setText(
                    username.substring(
                            0, username.offsetByCodePoints(0, 1)
                    ).toUpperCase(Locale.getDefault())
            );

            final String uid = entry.uid;
            FirebaseStorage.getInstance().getReference()
                    .child("profileImages").child(uid).child("avatar.jpg")
                    .getDownloadUrl()
                    .addOnSuccessListener(uri -> {
                        if (destroyed || activity.isDestroyed()
                                || activity.isFinishing()
                                || !uid.equals(holder.boundUid)) return;

                        holder.avatar.setVisibility(View.VISIBLE);

                        GlideApp.with(holder.avatar)
                                .load(uri)
                                .listener(new com.bumptech.glide.request.RequestListener<
                                        android.graphics.drawable.Drawable>() {

                                    @Override
                                    public boolean onLoadFailed(
                                            com.bumptech.glide.load.engine.GlideException error,
                                            Object model,
                                            com.bumptech.glide.request.target.Target<
                                                    android.graphics.drawable.Drawable> target,
                                            boolean isFirstResource
                                    ) {
                                        if (uid.equals(holder.boundUid)) {
                                            holder.avatar.setVisibility(View.GONE);
                                            holder.initial.setVisibility(View.VISIBLE);
                                        }
                                        return false;
                                    }

                                    @Override
                                    public boolean onResourceReady(
                                            android.graphics.drawable.Drawable resource,
                                            Object model,
                                            com.bumptech.glide.request.target.Target<
                                                    android.graphics.drawable.Drawable> target,
                                            com.bumptech.glide.load.DataSource dataSource,
                                            boolean isFirstResource
                                    ) {
                                        if (uid.equals(holder.boundUid)) {
                                            holder.initial.setVisibility(View.GONE);
                                        }
                                        return false;
                                    }
                                })
                                .into(holder.avatar);
                    });
        }

        @Override
        public void onViewRecycled(RowHolder holder) {
            holder.boundUid = null;
            GlideApp.with(holder.avatar).clear(holder.avatar);
            holder.itemView.setOnClickListener(null);
            super.onViewRecycled(holder);
        }

        @Override
        public int getItemCount() {
            return entries.size();
        }
    }

    private final class RowHolder extends RecyclerView.ViewHolder {
        final TextView rank;
        final TextView name;
        final TextView points;
        final TextView initial;
        final TextView correct;
        final ImageView avatar;
        String boundUid;

        RowHolder(View view) {
            super(view);
            rank = view.findViewById(R.id.lbRank);
            name = view.findViewById(R.id.lbName);
            points = view.findViewById(R.id.lbPoints);
            initial = view.findViewById(R.id.lbAvatarInitial);
            avatar = view.findViewById(R.id.lbAvatar);

            // Добавляем второй показатель под именем,
            // не меняя принятую разметку строки.
            LinearLayout parent = (LinearLayout) name.getParent();
            int index = parent.indexOfChild(name);
            ViewGroup.LayoutParams original = name.getLayoutParams();
            parent.removeView(name);

            GradientDrawable initialBackground = new GradientDrawable();
            initialBackground.setShape(GradientDrawable.OVAL);
            initialBackground.setColor(
                    color(R.color.account_settings_input_color)
            );

            initial.setBackground(initialBackground);
            initial.setTextColor(color(R.color.text_color_main));

            LinearLayout names = new LinearLayout(activity);
            names.setOrientation(LinearLayout.VERTICAL);
            names.setPadding(0, 0, 0, 0);
            parent.addView(names, index, original);

            names.addView(name, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            ));

            correct = text(12);
            correct.setTextSize(12);
            correct.setTextColor(color(R.color.neuro_secondary));
            correct.setAlpha(1f);

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            params.topMargin = dp(3);
            names.addView(correct, params);
        }
    }

    private void openPublicProfile(String uid) {
        if (uid == null
                || uid.isEmpty()
                || destroyed
                || activity.isFinishing()
                || activity.isDestroyed()) {
            return;
        }

        android.content.Intent intent = new android.content.Intent(
                activity,
                publicProfileActivity.class
        );

        intent.putExtra(publicProfileActivity.EXTRA_UID, uid);
        intent.putExtra(publicProfileActivity.EXTRA_SEASON, season);
        intent.putExtra(
                publicProfileActivity.EXTRA_SCOPE,
                databaseScope()
        );

        activity.startActivity(intent);
    }
}