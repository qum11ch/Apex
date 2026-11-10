package com.example.f1app;

import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;

import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Binds the displayed award next to leaderboard names.
 *
 * Stored selections come from displayedAwardRepository and appear at once.
 * Server reads are coalesced by UID and limited to three at a time.
 */
public final class leaderboardAwardBinder implements DefaultLifecycleObserver {

    private static final int MAX_RUNNING = 3;

    private final AppCompatActivity activity;
    private final Map<View, Slot> slots = new HashMap<>();
    private final Map<String, Pending> pending = new HashMap<>();
    private final ArrayDeque<Pending> queue = new ArrayDeque<>();

    private boolean active;
    private boolean disposed;
    private boolean everStarted;
    private int epoch;
    private int running;
    private String actorUid;
    private View personalKey;

    private interface Callback {
        void accept(displayedAwardApi.Selection value, Exception error);
    }

    private static final class Pending {

        final String uid;
        final int epoch;
        final String actor;
        final List<Callback> callbacks = new ArrayList<>();

        Pending(String uid, int epoch, String actor) {
            this.uid = uid;
            this.epoch = epoch;
            this.actor = actor;
        }
    }

    private static final class Slot {

        final leaderboardAwardBinder owner;
        final awardIconView icon;
        final leaderboardAwardToken token = new leaderboardAwardToken();

        String uid;
        displayedAwardApi.Selection shown;

        Slot(leaderboardAwardBinder owner, awardIconView icon) {
            this.owner = owner;
            this.icon = icon;
        }
    }

    public leaderboardAwardBinder(AppCompatActivity activity) {
        this.activity = activity;
        actorUid = FirebaseAuth.getInstance().getUid();
        activity.getLifecycle().addObserver(this);
    }

    private int dp(float value) {
        return Math.round(value * activity.getResources().getDisplayMetrics().density);
    }

    private boolean alive() {
        return active
                && !disposed
                && !activity.isFinishing()
                && !activity.isDestroyed();
    }

    // ------------------------------------------------------------ public API

    public void bindRow(View root, String uid) {
        TextView name = root.findViewById(R.id.lbName);

        if (name == null) {
            return;
        }

        bind(root, name, uid);
    }

    public void bindPersonal(TextView summary, String uid) {
        personalKey = summary;
        bind(summary, summary, uid);
    }

    public void clearPersonal() {
        if (personalKey != null) {
            clear(personalKey);
        }
    }

    public void clear(View root) {
        Slot slot = slots.remove(root);

        if (slot == null) {
            return;
        }

        slot.token.clear();
        slot.uid = null;
        slot.shown = null;
        slot.icon.clear();
        slot.icon.setVisibility(View.GONE);
    }

    /** Called when the leaderboard explicitly reloads; ranking requests remain untouched. */
    public void refresh() {
        epoch++;
        pending.clear();
        queue.clear();
        displayedAwardRepository.markAllStale();

        for (Slot slot : slots.values()) {
            slot.token.clear();
            slot.shown = null;
            slot.icon.clear();
            slot.icon.setVisibility(View.GONE);
        }
    }

    public void destroy() {
        if (disposed) {
            return;
        }

        disposed = true;
        active = false;
        epoch++;
        pending.clear();
        queue.clear();

        for (View key : new ArrayList<>(slots.keySet())) {
            clear(key);
        }

        personalKey = null;
    }

    // ----------------------------------------------------------------- slots

    private Slot slotFor(final View key, TextView text) {
        Slot cached = slots.get(key);

        if (cached != null) {
            return cached;
        }

        if (text.getParent() instanceof ConstraintLayout
                && ((View) text.getParent()).getTag() instanceof Slot) {
            Slot existing = (Slot) ((View) text.getParent()).getTag();

            if (existing.owner == this) {
                slots.put(key, existing);
                return existing;
            }
        }

        if (!(text.getParent() instanceof ViewGroup)) {
            return null;
        }

        ViewGroup parent = (ViewGroup) text.getParent();
        int index = parent.indexOfChild(text);
        ViewGroup.LayoutParams old = text.getLayoutParams();

        parent.removeView(text);

        ConstraintLayout wrapper = new ConstraintLayout(activity);
        parent.addView(wrapper, index, old);

        if (text.getId() == View.NO_ID) {
            text.setId(View.generateViewId());
        }

        awardIconView icon = new awardIconView(activity);
        icon.setId(View.generateViewId());
        icon.setVisibility(View.GONE);

        ConstraintLayout.LayoutParams textParams = new ConstraintLayout.LayoutParams(
                ConstraintLayout.LayoutParams.WRAP_CONTENT,
                ConstraintLayout.LayoutParams.WRAP_CONTENT
        );
        textParams.startToStart = ConstraintLayout.LayoutParams.PARENT_ID;
        textParams.endToStart = icon.getId();
        textParams.topToTop = ConstraintLayout.LayoutParams.PARENT_ID;
        textParams.bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID;
        textParams.constrainedWidth = true;
        textParams.horizontalChainStyle = ConstraintLayout.LayoutParams.CHAIN_PACKED;
        textParams.horizontalBias = 0f;
        wrapper.addView(text, textParams);

        ConstraintLayout.LayoutParams iconParams =
                new ConstraintLayout.LayoutParams(dp(28), dp(28));
        iconParams.startToEnd = text.getId();
        iconParams.endToEnd = ConstraintLayout.LayoutParams.PARENT_ID;
        iconParams.topToTop = ConstraintLayout.LayoutParams.PARENT_ID;
        iconParams.bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID;
        iconParams.setMarginStart(dp(6));
        wrapper.addView(icon, iconParams);

        // awardIconView.clear() may reset its click listener during image rebinding.
        // A touch bridge keeps tapping the badge routed to the existing row action.
        icon.setOnTouchListener((view, event) -> {
            if (event.getActionMasked() == MotionEvent.ACTION_UP) {
                view.performClick();
                key.performClick();
            }

            return true;
        });

        Slot slot = new Slot(this, icon);
        wrapper.setTag(slot);
        slots.put(key, slot);

        return slot;
    }

    private void bind(final View key, TextView text, final String uid) {
        final Slot slot = slotFor(key, text);

        if (slot == null) {
            return;
        }

        slot.uid = uid;
        slot.shown = null;

        final int token = slot.token.bind(uid);

        slot.icon.clear();
        slot.icon.setVisibility(View.GONE);

        if (uid == null || uid.isEmpty() || !alive()) {
            return;
        }

        read(uid, (value, error) -> {
            if (!alive() || slots.get(key) != slot || !slot.token.matches(uid, token)) {
                return;
            }

            if (error != null) {
                Log.w("LeaderboardAwards", "Selected award unavailable; uid=" + uid, error);
                return;
            }

            if (slot.shown != null && displayedAwardRepository.sameSelection(slot.shown, value)) {
                return;
            }

            slot.shown = value;
            show(key, slot, uid, token, value);
        });
    }

    private void show(
            final View key,
            final Slot slot,
            final String uid,
            final int token,
            displayedAwardApi.Selection value
    ) {
        slot.icon.clear();

        awardCatalogue.Entry entry = value.id == null ? null : awardCatalogue.find(value.id);

        if (entry == null) {
            slot.icon.setVisibility(View.GONE);
            return;
        }

        String caption = activity.getString(profileAwardPresentation.title(value.id));
        String name = profileAwardPresentation.string(value.award, "name");

        slot.icon.setVisibility(View.VISIBLE);

        catalogueAwardIcons.bind(
                slot.icon,
                entry,
                value.award,
                caption + (name.isEmpty() ? "" : ": " + name),
                () -> alive() && slots.get(key) == slot && slot.token.matches(uid, token)
        );
    }

    // ------------------------------------------------------------------ reads

    /** Delivers the stored selection first, then the server one if the stored one is not fresh. */
    private void read(final String uid, final Callback callback) {
        displayedAwardRepository.peek(uid, cached -> {
            if (!alive()) {
                return;
            }

            if (cached != null) {
                callback.accept(cached.selection, null);

                if (cached.fresh) {
                    return;
                }
            }

            enqueue(uid, callback);
        });
    }

    private void enqueue(String uid, Callback callback) {
        Pending request = pending.get(uid);

        if (request == null) {
            request = new Pending(uid, epoch, actorUid);
            pending.put(uid, request);
            queue.add(request);
        }

        request.callbacks.add(callback);
        pump();
    }

    private void pump() {
        while (alive() && running < MAX_RUNNING && !queue.isEmpty()) {
            final Pending request = queue.remove();

            if (request.epoch != epoch || pending.get(request.uid) != request) {
                continue;
            }

            running++;

            // A successful read is stored by displayedAwardApi itself.
            displayedAwardApi.get(request.uid, (value, error) -> {
                running--;

                boolean current = alive()
                        && request.epoch == epoch
                        && Objects.equals(request.actor, FirebaseAuth.getInstance().getUid())
                        && pending.get(request.uid) == request;

                if (pending.get(request.uid) == request) {
                    pending.remove(request.uid);
                }

                if (current) {
                    for (Callback callback : new ArrayList<>(request.callbacks)) {
                        callback.accept(value, error);
                    }
                }

                request.callbacks.clear();
                pump();
            });
        }
    }

    // -------------------------------------------------------------- lifecycle

    @Override
    public void onStart(@NonNull LifecycleOwner owner) {
        if (disposed) {
            return;
        }

        active = true;

        String current = FirebaseAuth.getInstance().getUid();

        if (!Objects.equals(actorUid, current)) {
            actorUid = current;
        }

        // The owner's own choice may have changed while settings were open.
        if (everStarted && current != null) {
            displayedAwardRepository.markStale(current);
        }

        everStarted = true;

        for (Map.Entry<View, Slot> entry : new ArrayList<>(slots.entrySet())) {
            View key = entry.getKey();
            String uid = entry.getValue().uid;
            TextView text = key instanceof TextView
                    ? (TextView) key
                    : key.findViewById(R.id.lbName);

            if (text != null && uid != null) {
                bind(key, text, uid);
            }
        }

        pump();
    }

    @Override
    public void onStop(@NonNull LifecycleOwner owner) {
        active = false;
        epoch++;
        pending.clear();
        queue.clear();
    }

    @Override
    public void onDestroy(@NonNull LifecycleOwner owner) {
        destroy();
    }
}
