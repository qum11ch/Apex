package com.example.f1app;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import com.facebook.shimmer.ShimmerFrameLayout;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Chooses which earned award is shown next to the user's name.
 *
 * What the screen shows depends on what is known:
 * - stored data available: the list appears at once and only a thin indicator
 *   runs while the server is asked again;
 * - nothing stored yet: a skeleton of the list is shown (after a short delay,
 *   so a fast load does not flash it);
 * - nothing stored and the request failed: a centered message with a retry button;
 * - short messages (save error, refresh error, award no longer available)
 *   use a banner above the save button and never replace the list.
 */
public final class displayedAwardPickerActivity extends AppCompatActivity {

    private static final String TAG = "DisplayedAwardPicker";
    private static final String STATE_CANDIDATE = "candidate";
    private static final String STATE_OWNER_UID = "ownerUid";
    private static final String STATE_HAS_CANDIDATE = "hasCandidate";
    private static final String STATE_PRESELECTION = "preselectionApplied";
    private static final long SKELETON_DELAY_MS = 120L;

    private final Map<String, Map<String, Object>> available = new LinkedHashMap<>();
    private final Map<String, RowViews> rowViews = new LinkedHashMap<>();
    private final Handler main = new Handler(Looper.getMainLooper());
    private final Runnable revealSkeleton = this::showSkeletonNow;

    private LinearLayout rows;
    private View scroll;
    private View statePanel;
    private View retry;
    private View banner;
    private TextView bannerText;
    private TextView bannerAction;
    private TextView save;
    private ShimmerFrameLayout skeleton;
    private ShimmerFrameLayout updating;

    private String uid;
    private String candidate;
    private String renderedFingerprint;
    private Runnable bannerRunnable;

    private int generation;
    private int renderGeneration;

    private boolean started;
    private boolean awardsLoaded;
    private boolean selectionLoaded;
    private boolean awardsPending;
    private boolean selectionPending;
    private boolean loadFailed;
    private boolean saving;
    private boolean preselectionApplied;
    private boolean hasCandidate;
    private boolean skeletonScheduled;

    // ---------------------------------------------------------------- lifecycle

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_displayed_award_picker);

        rows = findViewById(R.id.displayedAwardPickerRows);
        scroll = findViewById(R.id.displayedAwardPickerScroll);
        statePanel = findViewById(R.id.displayedAwardPickerStatePanel);
        retry = findViewById(R.id.displayedAwardPickerRetry);
        banner = findViewById(R.id.displayedAwardPickerBanner);
        bannerText = findViewById(R.id.displayedAwardPickerBannerText);
        bannerAction = findViewById(R.id.displayedAwardPickerBannerAction);
        save = findViewById(R.id.displayedAwardPickerSave);
        skeleton = findViewById(R.id.displayedAwardPickerSkeleton);
        updating = findViewById(R.id.displayedAwardPickerUpdating);

        findViewById(R.id.displayedAwardPickerBack).setOnClickListener(view -> finish());
        retry.setOnClickListener(view -> load(true));
        save.setOnClickListener(view -> saveChoice());

        bannerAction.setOnClickListener(view -> {
            Runnable action = bannerRunnable;

            if (action != null) {
                action.run();
            }
        });

        setupInsets();

        if (savedInstanceState != null) {
            candidate = savedInstanceState.getString(STATE_CANDIDATE);
            uid = savedInstanceState.getString(STATE_OWNER_UID);
            hasCandidate = savedInstanceState.getBoolean(STATE_HAS_CANDIDATE, false);
            preselectionApplied = savedInstanceState.getBoolean(STATE_PRESELECTION, false);
        }
    }

    private void setupInsets() {
        View root = findViewById(R.id.displayedAwardPickerRoot);
        View header = findViewById(R.id.displayedAwardPickerHeader);

        int rootLeft = root.getPaddingLeft();
        int rootTop = root.getPaddingTop();
        int rootRight = root.getPaddingRight();
        int rootBottom = root.getPaddingBottom();

        int headerLeft = header.getPaddingLeft();
        int headerTop = header.getPaddingTop();
        int headerRight = header.getPaddingRight();
        int headerBottom = header.getPaddingBottom();

        WindowCompat.getInsetsController(
                getWindow(),
                getWindow().getDecorView()
        ).setAppearanceLightStatusBars(false);

        ViewCompat.setOnApplyWindowInsetsListener(root, (view, insets) -> {
            Insets safe = insets.getInsets(
                    WindowInsetsCompat.Type.systemBars()
                            | WindowInsetsCompat.Type.displayCutout()
            );

            view.setPadding(
                    rootLeft + safe.left,
                    rootTop,
                    rootRight + safe.right,
                    rootBottom + safe.bottom
            );

            header.setPadding(
                    headerLeft,
                    headerTop + safe.top,
                    headerRight,
                    headerBottom
            );

            return insets;
        });

        ViewCompat.requestApplyInsets(root);
    }

    @Override
    protected void onStart() {
        super.onStart();
        started = true;

        String current = FirebaseAuth.getInstance().getUid();

        if (!Objects.equals(current, uid)) {
            candidate = null;
            hasCandidate = false;
            preselectionApplied = false;
            saving = false;
            awardsLoaded = false;
            selectionLoaded = false;
            available.clear();
            clearRows();
        }

        uid = current;

        if (uid == null) {
            finish();
            return;
        }

        load(false);
    }

    @Override
    protected void onStop() {
        started = false;
        generation++;
        renderGeneration++;
        awardsPending = false;
        selectionPending = false;

        main.removeCallbacks(revealSkeleton);
        skeletonScheduled = false;
        skeleton.stopShimmer();
        updating.stopShimmer();

        super.onStop();
    }

    @Override
    protected void onDestroy() {
        clearRows();
        super.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putString(STATE_CANDIDATE, candidate);
        outState.putString(STATE_OWNER_UID, uid);
        outState.putBoolean(STATE_HAS_CANDIDATE, hasCandidate);
        outState.putBoolean(STATE_PRESELECTION, preselectionApplied);
        super.onSaveInstanceState(outState);
    }

    private boolean active(int request) {
        return started
                && request == generation
                && !isFinishing()
                && !isDestroyed()
                && Objects.equals(uid, FirebaseAuth.getInstance().getUid());
    }

    private boolean haveData() {
        return awardsLoaded && selectionLoaded;
    }

    private boolean canSave() {
        return haveData() && !saving;
    }

    // ------------------------------------------------------------------ loading

    /**
     * Shows stored data first, then asks the repositories for fresh data.
     * {@code force} is used by the retry actions only: stored data stays on
     * screen until the answer arrives.
     */
    private void load(boolean force) {
        if (!started || saving || uid == null) {
            return;
        }

        final int request = ++generation;
        final String requested = uid;

        loadFailed = false;
        awardsPending = true;
        selectionPending = true;
        hideBanner();

        if (force) {
            awardsRepository.invalidate(requested);
            displayedAwardRepository.markStale(requested);
        }

        updateUi();

        awardsRepository.peek(requested, snapshot -> {
            if (!active(request) || snapshot == null || awardsLoaded) {
                return;
            }

            applyAwards(snapshot.badges);
            render();
        });

        displayedAwardRepository.peek(requested, cached -> {
            if (!active(request) || cached == null || selectionLoaded) {
                return;
            }

            applySelection(cached.selection);
            render();
        });

        awardsRepository.load(requested, (badges, error) -> {
            if (!active(request)) {
                return;
            }

            awardsPending = false;

            if (error != null || badges == null) {
                refreshFailed(error);
                return;
            }

            applyAwards(badges);
            render();
        });

        displayedAwardRepository.load(requested, false, (value, error) -> {
            if (!active(request)) {
                return;
            }

            selectionPending = false;

            if (error != null || value == null) {
                refreshFailed(error);
                return;
            }

            applySelection(value);
            render();
        });
    }

    private void applyAwards(List<Map<String, Object>> badges) {
        available.clear();

        for (Map<String, Object> badge : profileAwardPresentation.normalize(badges)) {
            String id = profileAwardPresentation.string(badge, "id");
            available.put(id, badge);
        }

        awardsLoaded = true;
    }

    private void applySelection(displayedAwardApi.Selection value) {
        // Follow the server's choice until the user picks something themselves.
        if (!hasCandidate) {
            candidate = value.id;
        }

        selectionLoaded = true;
    }

    private void refreshFailed(Exception error) {
        Log.w(TAG, "Picker loading failed", error);

        if (haveData()) {
            showBanner(R.string.displayed_award_refresh_error, () -> load(true));
        } else {
            loadFailed = true;
        }

        updateUi();
    }

    // ---------------------------------------------------------------- rendering

    private void render() {
        if (haveData()) {
            applyPreselection();

            if (!awardsPending) {
                dropUnavailableCandidate();
            }

            String fingerprint = awardsCacheStore.fingerprint(new ArrayList<>(available.values()));

            if (!fingerprint.equals(renderedFingerprint)) {
                renderRows();
                renderedFingerprint = fingerprint;
            } else {
                updateSelection();
            }
        }

        updateUi();
    }

    private void applyPreselection() {
        if (preselectionApplied) {
            return;
        }

        String requested = getIntent().getStringExtra("preselectAwardId");

        if (requested == null) {
            preselectionApplied = true;
        } else if (available.containsKey(requested)) {
            candidate = requested;
            hasCandidate = true;
            preselectionApplied = true;
        } else if (!awardsPending) {
            preselectionApplied = true;
        }
    }

    /** Checked only against a server-confirmed list, never against stored data. */
    private void dropUnavailableCandidate() {
        if (candidate != null && !available.containsKey(candidate)) {
            candidate = null;
            showBanner(R.string.displayed_award_unavailable, null);
        }
    }

    private void renderRows() {
        clearRows();
        int request = renderGeneration;
        addRow(null, null, request);

        for (Map.Entry<String, Map<String, Object>> entry : available.entrySet()) {
            addRow(entry.getKey(), entry.getValue(), request);
        }

        updateSelection();
    }

    private void addRow(String id, Map<String, Object> badge, int request) {
        View item = LayoutInflater.from(this).inflate(
                R.layout.item_displayed_award_picker,
                rows,
                false
        );

        RowViews holder = new RowViews(item);
        String caption = getString(
                id == null
                        ? R.string.displayed_award_none
                        : profileAwardPresentation.title(id)
        );

        holder.title.setText(caption);
        holder.caption = caption;
        holder.icon.setVisibility(id == null ? View.GONE : View.VISIBLE);
        holder.none.setVisibility(id == null ? View.VISIBLE : View.GONE);

        if (id != null) {
            catalogueAwardIcons.bind(
                    holder.icon,
                    awardCatalogue.find(id),
                    badge,
                    caption,
                    () -> started
                            && !isFinishing()
                            && !isDestroyed()
                            && request == renderGeneration
                            && Objects.equals(uid, FirebaseAuth.getInstance().getUid())
            );
        }

        holder.clickTarget.setOnClickListener(view -> {
            if (!canSave() || Objects.equals(candidate, id)) {
                return;
            }

            candidate = id;
            hasCandidate = true;
            hideBanner();
            updateSelection();
        });

        rowViews.put(id, holder);
        rows.addView(item);
    }

    private void updateSelection() {
        for (Map.Entry<String, RowViews> entry : rowViews.entrySet()) {
            RowViews holder = entry.getValue();
            boolean selected = Objects.equals(entry.getKey(), candidate);

            holder.root.setSelected(selected);
            holder.clickTarget.setSelected(selected);
            holder.mark.setVisibility(selected ? View.VISIBLE : View.INVISIBLE);
            holder.clickTarget.setContentDescription(
                    selected
                            ? holder.caption + ", " + getString(R.string.displayed_award_selected)
                            : holder.caption
            );
        }

        updateControls();
    }

    // -------------------------------------------------------------------- state

    /** Chooses the single content of the main area and the indicators around it. */
    private void updateUi() {
        boolean busy = awardsPending || selectionPending;
        boolean showList = haveData();
        boolean showPanel = !showList && !busy && loadFailed;
        boolean wantSkeleton = !showList && busy;

        scroll.setVisibility(showList ? View.VISIBLE : View.GONE);
        statePanel.setVisibility(showPanel ? View.VISIBLE : View.GONE);

        setSkeletonWanted(wantSkeleton);
        setUpdating(showList && (busy || saving));
        updateControls();
    }

    private void setSkeletonWanted(boolean wanted) {
        if (!wanted) {
            main.removeCallbacks(revealSkeleton);
            skeletonScheduled = false;
            skeleton.stopShimmer();
            skeleton.setVisibility(View.GONE);
            return;
        }

        if (skeleton.getVisibility() == View.VISIBLE || skeletonScheduled) {
            return;
        }

        skeletonScheduled = true;
        main.postDelayed(revealSkeleton, SKELETON_DELAY_MS);
    }

    private void showSkeletonNow() {
        skeletonScheduled = false;

        if (!started) {
            return;
        }

        skeleton.setVisibility(View.VISIBLE);
        skeleton.startShimmer();
    }

    private void setUpdating(boolean on) {
        if (on) {
            updating.setVisibility(View.VISIBLE);
            updating.startShimmer();
        } else {
            updating.stopShimmer();
            updating.setVisibility(View.INVISIBLE);
        }
    }

    private void updateControls() {
        boolean enabled = canSave();

        save.setText(
                saving
                        ? R.string.displayed_award_saving
                        : R.string.displayed_award_save
        );
        save.setEnabled(enabled);
        save.setAlpha(enabled ? 1f : 0.4f);

        for (RowViews holder : rowViews.values()) {
            holder.clickTarget.setEnabled(enabled);
        }
    }

    private void showBanner(int messageRes, Runnable action) {
        bannerRunnable = action;
        bannerText.setText(messageRes);
        bannerAction.setVisibility(action == null ? View.GONE : View.VISIBLE);
        banner.setVisibility(View.VISIBLE);
    }

    private void hideBanner() {
        bannerRunnable = null;
        banner.setVisibility(View.GONE);
    }

    // ------------------------------------------------------------------- saving

    private void saveChoice() {
        if (!canSave()) {
            return;
        }

        saving = true;

        final int request = generation;
        final String chosen = candidate;
        final String requestedUid = uid;

        hideBanner();
        updateUi();

        displayedAwardApi.set(chosen, (value, error) -> {
            if (!Objects.equals(requestedUid, uid)
                    || !Objects.equals(requestedUid, FirebaseAuth.getInstance().getUid())) {
                return;
            }

            saving = false;

            if (!active(request)) {
                if (started && !isFinishing() && !isDestroyed()) {
                    load(false);
                }

                return;
            }

            if (error != null || value == null) {
                showBanner(R.string.displayed_award_save_error, this::saveChoice);
                updateUi();
                return;
            }

            Toast.makeText(
                    this,
                    R.string.displayed_award_saved,
                    Toast.LENGTH_SHORT
            ).show();

            setResult(RESULT_OK);
            finish();
        });
    }

    // ------------------------------------------------------------------ cleanup

    private void clearRows() {
        renderGeneration++;
        renderedFingerprint = null;

        if (rows != null) {
            clearIcons(rows);
            rows.removeAllViews();
        }

        rowViews.clear();
    }

    private void clearIcons(View view) {
        if (view instanceof awardIconView) {
            ((awardIconView) view).clear();
            return;
        }

        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;

            for (int index = 0; index < group.getChildCount(); index++) {
                clearIcons(group.getChildAt(index));
            }
        }
    }

    private static final class RowViews {

        private final View root;
        private final View clickTarget;
        private final awardIconView icon;
        private final TextView none;
        private final TextView title;
        private final TextView mark;

        private String caption;

        private RowViews(View root) {
            this.root = root;
            clickTarget = root.findViewById(R.id.pickerAwardClickTarget);
            icon = root.findViewById(R.id.pickerAwardIcon);
            none = root.findViewById(R.id.pickerAwardNone);
            title = root.findViewById(R.id.pickerAwardTitle);
            mark = root.findViewById(R.id.pickerAwardSelected);
        }
    }
}
