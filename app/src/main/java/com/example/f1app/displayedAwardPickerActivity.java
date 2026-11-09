package com.example.f1app;

import android.os.Bundle;
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

import com.google.firebase.auth.FirebaseAuth;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public final class displayedAwardPickerActivity extends AppCompatActivity {

    private static final String TAG = "DisplayedAwardPicker";
    private static final String STATE_CANDIDATE = "candidate";
    private static final String STATE_OWNER_UID = "ownerUid";
    private static final String STATE_HAS_CANDIDATE = "hasCandidate";
    private static final String STATE_PRESELECTION = "preselectionApplied";

    private final Map<String, Map<String, Object>> available = new LinkedHashMap<>();
    private final Map<String, RowViews> rowViews = new LinkedHashMap<>();

    private LinearLayout rows;
    private TextView state;
    private TextView save;
    private View retry;

    private String uid;
    private String candidate;

    private int generation;
    private int renderGeneration;

    private boolean started;
    private boolean awardsLoaded;
    private boolean selectionLoaded;
    private boolean loadFailed;
    private boolean saving;
    private boolean preselectionApplied;
    private boolean hasCandidate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_displayed_award_picker);

        rows = findViewById(R.id.displayedAwardPickerRows);
        state = findViewById(R.id.displayedAwardPickerState);
        save = findViewById(R.id.displayedAwardPickerSave);
        retry = findViewById(R.id.displayedAwardPickerRetry);

        findViewById(R.id.displayedAwardPickerBack).setOnClickListener(view -> finish());
        retry.setOnClickListener(view -> load());
        save.setOnClickListener(view -> saveChoice());

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
            available.clear();
            clearRows();
        }

        uid = current;

        if (uid == null) {
            finish();
            return;
        }

        load();
    }

    @Override
    protected void onStop() {
        started = false;
        generation++;
        renderGeneration++;
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

    private boolean canSave() {
        return awardsLoaded && selectionLoaded && !loadFailed && !saving;
    }

    private void load() {
        if (!started || saving || uid == null) {
            return;
        }

        int request = ++generation;
        awardsLoaded = false;
        selectionLoaded = false;
        loadFailed = false;

        state.setText(R.string.displayed_award_loading);
        retry.setVisibility(View.GONE);
        clearRows();
        updateControls();

        awardsRepository.invalidate(uid);
        awardsRepository.load(uid, (badges, error) -> {
            if (!active(request)) {
                return;
            }

            if (error != null || badges == null) {
                failedLoad(error);
                return;
            }

            available.clear();

            for (Map<String, Object> badge : profileAwardPresentation.normalize(badges)) {
                String id = profileAwardPresentation.string(badge, "id");
                available.put(id, badge);
            }

            awardsLoaded = true;
            ready();
        });

        displayedAwardApi.get(uid, (value, error) -> {
            if (!active(request)) {
                return;
            }

            if (error != null || value == null) {
                failedLoad(error);
                return;
            }

            if (!hasCandidate) {
                candidate = value.id;
                hasCandidate = true;
            }

            selectionLoaded = true;
            ready();
        });
    }

    private void failedLoad(Exception error) {
        loadFailed = true;
        state.setText(R.string.displayed_award_list_error);
        retry.setVisibility(View.VISIBLE);
        updateControls();
        Log.w(TAG, "Picker loading failed", error);
    }

    private void ready() {
        if (!awardsLoaded || !selectionLoaded || loadFailed) {
            return;
        }

        if (!preselectionApplied) {
            String requested = getIntent().getStringExtra("preselectAwardId");

            if (requested != null && available.containsKey(requested)) {
                candidate = requested;
            }

            preselectionApplied = true;
        }

        boolean unavailable = candidate != null && !available.containsKey(candidate);

        if (unavailable) {
            candidate = null;
        }

        if (unavailable) {
            state.setText(R.string.displayed_award_unavailable);
        } else {
            state.setText("");
        }

        retry.setVisibility(View.GONE);
        renderRows();
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
            state.setText("");
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

    private void saveChoice() {
        if (!canSave()) {
            return;
        }

        saving = true;
        int request = generation;
        String chosen = candidate;
        String requestedUid = uid;

        updateControls();
        state.setText(R.string.displayed_award_saving);

        displayedAwardApi.set(chosen, (value, error) -> {
            if (!Objects.equals(requestedUid, uid)
                    || !Objects.equals(requestedUid, FirebaseAuth.getInstance().getUid())) {
                return;
            }

            saving = false;

            if (!active(request)) {
                if (started && !isFinishing() && !isDestroyed()) {
                    load();
                }
                return;
            }

            if (error != null || value == null) {
                state.setText(R.string.displayed_award_save_error);
                updateControls();
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

    private void clearRows() {
        renderGeneration++;

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
