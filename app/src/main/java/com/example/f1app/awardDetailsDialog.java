package com.example.f1app;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.text.DateFormat;
import java.util.Date;
import java.util.Map;

final class awardDetailsDialog extends Dialog {

    private final Context ownerContext;
    private final awardCatalogue.Entry entry;
    private final Map<String, Object> badge;
    private final String caption;
    private final awardIconView icon;

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
        bindIconState(data.known);
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

    private void bindIconState(boolean known) {
        if (!known) {
            icon.setAlpha(0.7f);
            return;
        }

        icon.setAlpha(badge == null ? 0.45f : 1f);
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
        View use = findViewById(R.id.awardDetailsUse);
        View done = findViewById(R.id.awardDetailsDone);

        boolean canChoose = badge != null && ownerContext instanceof awardsCollectionActivity;
        use.setVisibility(canChoose ? View.VISIBLE : View.GONE);

        if (canChoose) {
            use.setOnClickListener(view -> {
                if (!isOwnerAvailable()) {
                    return;
                }

                dismiss();

                Intent intent = new Intent(
                        ownerContext,
                        displayedAwardPickerActivity.class
                );
                intent.putExtra("preselectAwardId", entry.id);
                ownerContext.startActivity(intent);
            });
        }

        done.setOnClickListener(view -> dismiss());
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
        icon.clear();
        super.onStop();
    }

    private int dp(float value) {
        return Math.round(value * getContext().getResources().getDisplayMetrics().density);
    }
}
