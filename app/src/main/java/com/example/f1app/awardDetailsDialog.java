package com.example.f1app;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.ContextWrapper;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;

import java.text.DateFormat;
import java.util.Date;
import java.util.Map;
import java.util.Objects;

final class awardDetailsDialog extends Dialog {

    private final Context ownerContext;
    private final awardCatalogue.Entry entry;
    private final Map<String, Object> badge;
    private final String caption;
    private final awardIconView icon;
    private final String selectionOwnerUid;

    private boolean saving;
    private int selectionRequest;
    private boolean closed;

    awardDetailsDialog(
            Context context,
            awardCatalogue.Entry entry,
            awardsCollectionActivity.Snapshot data,
            String caption,
            String requirement,
            String status
    ) {
        super(context);

        selectionOwnerUid = FirebaseAuth.getInstance().getUid();

        ownerContext = context;
        this.entry = entry;
        this.caption = caption;
        badge = data.owned.get(entry.id);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.dialog_award_details);
        setCanceledOnTouchOutside(true);

        icon = findViewById(R.id.awardDetailsIcon);

        TextView titleView = findViewById(R.id.awardDetailsTitle);
        TextView statusView = findViewById(R.id.awardDetailsStatus);
        TextView conditionView = findViewById(R.id.awardDetailsCondition);

        titleView.setText(caption);
        statusView.setText(
                context.getString(R.string.awards_details_status) + ": " + status
        );
        conditionView.setText(requirement);

        bindIdentity();
        icon.setAlpha(1f);
        bindProgress(data);
        bindRecordedDate();
        bindActions();
    }

    private void bindIdentity() {
        TextView identity = findViewById(R.id.awardDetailsIdentity);

        if (badge == null) {
            identity.setVisibility(View.GONE);
            return;
        }

        String name = profileAwardPresentation.string(badge, "name");
        identity.setText(name);
        identity.setVisibility(name.isEmpty() ? View.GONE : View.VISIBLE);
    }

    private void bindProgress(awardsCollectionActivity.Snapshot data) {
        View section = findViewById(R.id.awardDetailsProgressSection);
        ProgressBar progress = findViewById(R.id.awardDetailsProgress);
        TextView progressText = findViewById(R.id.awardDetailsProgressText);

        boolean measurable = entry.target > 0
                && (entry.category == awardCatalogue.PREDICTIONS
                || entry.category == awardCatalogue.TENURE);

        if (!measurable) {
            section.setVisibility(View.GONE);
            return;
        }

        section.setVisibility(View.VISIBLE);
        Long current = awardProgress.current(entry, data.correct, data.years);

        if (current == null) {
            progress.setVisibility(View.GONE);
            progressText.setText(R.string.awards_progress_unavailable);
            return;
        }

        progress.setVisibility(View.VISIBLE);
        progress.setMax(entry.target);
        progress.setProgress((int) Math.min(current, (long) entry.target));

        int textResource = entry.category == awardCatalogue.PREDICTIONS
                ? R.string.awards_progress_correct
                : R.string.awards_progress_years;

        progressText.setText(
                getContext().getString(textResource, current, (long) entry.target)
        );
    }

    private void bindRecordedDate() {
        TextView recorded = findViewById(R.id.awardDetailsRecorded);

        if (badge == null || !(badge.get("awardedAt") instanceof Number)) {
            recorded.setVisibility(View.GONE);
            return;
        }

        long timestamp = ((Number) badge.get("awardedAt")).longValue();

        if (timestamp < 0) {
            recorded.setVisibility(View.GONE);
            return;
        }

        String date = DateFormat.getDateInstance().format(new Date(timestamp));

        recorded.setText(
                getContext().getString(R.string.awards_details_recorded, date)
        );
        recorded.setVisibility(View.VISIBLE);
    }

    private void bindActions() {
        TextView use = findViewById(R.id.awardDetailsUse);

        boolean canChoose = badge != null
                && ownerContext instanceof awardsCollectionActivity
                && selectionOwnerUid != null;

        use.setVisibility(canChoose ? View.VISIBLE : View.GONE);

        if (canChoose) {
            use.setOnClickListener(view -> saveDisplayedAward());
        }
    }

    private void saveDisplayedAward() {
        if (saving
                || badge == null
                || closed
                || !isShowing()
                || !isOwnerAvailable()
                || selectionOwnerUid == null
                || !Objects.equals(
                selectionOwnerUid,
                FirebaseAuth.getInstance().getUid()
        )) {
            return;
        }

        TextView use = findViewById(R.id.awardDetailsUse);

        saving = true;
        final int request = ++selectionRequest;

        use.setEnabled(false);
        use.setText(R.string.displayed_award_saving);

        displayedAwardApi.set(entry.id, (selection, error) -> {
            if (request != selectionRequest
                    || closed
                    || !isShowing()
                    || !isOwnerAvailable()
                    || !Objects.equals(
                    selectionOwnerUid,
                    FirebaseAuth.getInstance().getUid()
            )) {
                return;
            }

            saving = false;

            if (error != null || selection == null) {
                use.setEnabled(true);
                use.setText(R.string.displayed_award_use);

                Log.w(
                        "AwardDetails",
                        "Failed to set displayed award",
                        error
                );

                Toast.makeText(
                        getContext(),
                        R.string.displayed_award_save_error,
                        Toast.LENGTH_SHORT
                ).show();

                return;
            }

            Toast.makeText(
                    getContext(),
                    R.string.displayed_award_saved,
                    Toast.LENGTH_SHORT
            ).show();

            dismiss();
        });
    }

    @Override
    public void show() {
        if (!isOwnerAvailable()) {
            return;
        }

        closed = false;
        super.show();
        configureWindow();

        // Start binding after show(), when the asynchronous renderer may use isShowing().
        catalogueAwardIcons.bind(
                icon,
                entry,
                badge,
                caption,
                () -> !closed && isShowing() && isOwnerAvailable()
        );
    }

    private void configureWindow() {
        Window window = getWindow();

        if (window == null) {
            return;
        }

        window.setBackgroundDrawableResource(android.R.color.transparent);

        int screenWidth = getContext().getResources().getDisplayMetrics().widthPixels;
        int width = Math.max(1, screenWidth - dp(32));

        window.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT);
        window.getDecorView().post(() -> {
            if (!isShowing() || getWindow() != window || !isOwnerAvailable()) {
                return;
            }

            int screenHeight = getContext().getResources().getDisplayMetrics().heightPixels;
            int maxHeight = Math.max(1, (int) (screenHeight * 0.82f));

            if (window.getDecorView().getHeight() > maxHeight) {
                window.setLayout(width, maxHeight);
            }
        });
    }

    private boolean isOwnerAvailable() {
        Context context = ownerContext;

        while (context != null) {
            if (context instanceof Activity) {
                Activity activity = (Activity) context;
                return !activity.isFinishing() && !activity.isDestroyed();
            }

            if (!(context instanceof ContextWrapper)) {
                break;
            }

            Context next = ((ContextWrapper) context).getBaseContext();

            if (next == context) {
                break;
            }

            context = next;
        }

        return true;
    }

    @Override
    protected void onStop() {
        closed = true;
        selectionRequest++;
        saving = false;

        TextView use = findViewById(R.id.awardDetailsUse);
        use.setEnabled(true);
        use.setText(R.string.displayed_award_use);

        icon.clear();

        super.onStop();
    }

    private int dp(float value) {
        return Math.round(value * getContext().getResources().getDisplayMetrics().density);
    }
}
