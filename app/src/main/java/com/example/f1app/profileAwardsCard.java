package com.example.f1app;

import android.content.Context;
import android.net.Uri;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.ImageButton;
import android.widget.TextView;
import com.facebook.shimmer.ShimmerFrameLayout;
import com.google.firebase.storage.FirebaseStorage;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class profileAwardsCard extends FrameLayout {
    private static final Map<String,Uri> LOGOS = new ConcurrentHashMap<>();
    private final TextView empty, message, expand;
    private final ImageButton refresh;
    private final GridLayout grid;
    private final ShimmerFrameLayout shimmer,updating;
    private List<Map<String,Object>> data = Collections.emptyList();
    private boolean hasData,expanded,loading;
    private Runnable collectionAction;
    private int columns=3,renderGeneration;

    public profileAwardsCard(Context context) {
        this(context,null);
    }

    public profileAwardsCard(Context context, AttributeSet attrs) {
        super(context,attrs);
        LayoutInflater.from(context).inflate(R.layout.view_profile_awards_card,this,true);
        refresh=findViewById(R.id.profileAwardsRefresh);
        empty=findViewById(R.id.profileAwardsEmpty);
        message=findViewById(R.id.profileAwardsMessage);
        expand=findViewById(R.id.profileAwardsExpand);
        grid=findViewById(R.id.profileAwardsGrid);
        shimmer=findViewById(R.id.profileAwardsShimmer);
        updating=findViewById(R.id.profileAwardsUpdating);
        expand.setOnClickListener(v->{
            if (collectionAction != null)
                collectionAction.run();
            else {
                expanded =! expanded;
                rebuild();
            }
        });
    }

    private int dp(float n) {
        return Math.round(n*getResources().getDisplayMetrics().density);
    }

    public void setCollectionAction(Runnable action) {
        collectionAction = action;
        updateFooter();
    }

    public void setRefreshAction(Runnable action) {
        refresh.setOnClickListener(v->action.run());
    }

    public boolean hasData() {
        return hasData;
    }

    public void setBusy(boolean busy) {
        refresh.setEnabled(!busy);
        refresh.setAlpha(busy?.45f:1f);
    }

    public void showLoading() {
        loading = true;
        setBusy(true);
        message.setVisibility(GONE);

        if (hasData) {
            shimmer.stopShimmer();
            shimmer.setVisibility(GONE);

            updating.setContentDescription(
                    getContext().getString(R.string.profile_awards_updating)
            );
            updating.setVisibility(VISIBLE);
            updating.startShimmer();

            return;
        }

        updating.stopShimmer();
        updating.setVisibility(GONE);

        empty.setVisibility(GONE);
        grid.setVisibility(GONE);
        expand.setVisibility(
                collectionAction == null ? GONE : VISIBLE
        );

        shimmer.setVisibility(VISIBLE);
        shimmer.startShimmer();
    }

    public void showData(List<Map<String,Object>> values) {
        List<Map<String,Object>> normalized = profileAwardPresentation.normalize(values);
        boolean changed =! hasData || !data.equals(normalized);
        data = normalized;
        hasData = true;
        finishLoading();
        message.setVisibility(GONE);
        empty.setVisibility(data.isEmpty() ? VISIBLE : GONE);
        if(changed) rebuild();
        else updateFooter();
        grid.setVisibility(data.isEmpty() ? GONE : VISIBLE);
    }

    public void showError() {
        finishLoading();
        message.setText(hasData?R.string.profile_awards_refresh_error:R.string.profile_awards_error);
        message.setVisibility(VISIBLE);
        if(!hasData) {
            empty.setVisibility(GONE);
            grid.setVisibility(GONE);
            expand.setVisibility(collectionAction == null ? GONE : VISIBLE);
        }
    }

    public void showSyncWarning() {
        message.setText(R.string.profile_awards_sync_error);
        message.setVisibility(VISIBLE);
    }

    public void finishLoading() {
        loading = false;
        setBusy(false);

        stopAnimations();

        shimmer.setVisibility(GONE);
        updating.setVisibility(GONE);
    }

    public void stopAnimations() {
        shimmer.stopShimmer();
        updating.stopShimmer();
    }

    public void resetOwner() {
        finishLoading();
        renderGeneration++;
        clearIcons(grid);
        grid.removeAllViews();
        data = Collections.emptyList();
        hasData = false;
        expanded = false;
        grid.setVisibility(GONE);
        empty.setVisibility(GONE);
        message.setVisibility(GONE);
        expand.setVisibility(GONE);
    }

    public void release() {
        resetOwner();
        setRefreshAction(()->{});
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldW, int oldH) {
        super.onSizeChanged(w, h, oldW, oldH);

        float fontScale = Math.max(
                1f,
                getResources().getConfiguration().fontScale
        );

        int available = Math.max(0, w - dp(32));

        int newColumns = Math.max(
                1,
                Math.min(
                        4,
                        available / Math.max(1, dp(90 * fontScale))
                )
        );

        if (newColumns != columns || w != oldW) {
            columns = newColumns;

            post(() -> {
                if (getWidth() != w) {
                    return;
                }

                if (hasData) {
                    rebuild();
                }
            });
        }
    }

    private GridLayout.LayoutParams position(int index) {
        int margin = dp(3);
        int available = Math.max(0, getWidth() - dp(32));
        GridLayout.LayoutParams params = new GridLayout.LayoutParams(
                GridLayout.spec(index / columns),
                GridLayout.spec(index % columns)
        );
        params.width = available > 0
                ? Math.max(1, available / columns - 2 * margin)
                : ViewGroup.LayoutParams.WRAP_CONTENT;
        params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
        params.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL);
        params.setMargins(margin, margin, margin, margin);
        return params;
    }

    private void updateFooter() {
        if (collectionAction != null) {
            expand.setVisibility(VISIBLE);
            expand.setText(R.string.awards_catalogue_link);
            return;
        }
        int preview=columns*2;
        expand.setVisibility(data.size() > preview ? VISIBLE : GONE);
        expand.setText(expanded?getContext().getString(R.string.profile_awards_collapse):getContext().getString(R.string.profile_awards_show_all,data.size()));
    }

    private void rebuild() {
        renderGeneration++;
        int request = renderGeneration;

        clearIcons(grid);
        grid.removeAllViews();
        grid.setColumnCount(columns);

        LayoutInflater inflater = LayoutInflater.from(getContext());

        int itemCount = expanded
                ? data.size()
                : Math.min(columns * 2, data.size());

        for (int index = 0; index < itemCount; index++) {
            Map<String, Object> badge = data.get(index);

            String id = profileAwardPresentation.string(badge, "id");
            String caption = getContext().getString(
                    profileAwardPresentation.title(id)
            );
            String name = profileAwardPresentation.string(badge, "name");

            String description = caption;

            if (!name.isEmpty()) {
                description += ": " + name;
            }

            if (id.startsWith("season_")) {
                description += " · 2026";
            }

            View item = inflater.inflate(
                    R.layout.item_medal,
                    grid,
                    false
            );

            awardIconView icon = item.findViewById(R.id.medalIcon);
            TextView title = item.findViewById(R.id.medalTitle);

            title.setText(caption);

            bindIcon(
                    icon,
                    badge,
                    id,
                    description,
                    request
            );

            grid.addView(item, position(index));
        }

        updateFooter();
        grid.setVisibility(data.isEmpty() ? GONE : VISIBLE);
    }

    private void bindIcon(awardIconView icon,
                          Map<String,Object> badge,
                          String id,
                          String description,
                          int request) {
        if("fan_driver".equals(id)) {
            icon.showDriver(profileAwardPresentation.string(badge,"code"), profileAwardPresentation.titles(badge.get("championshipsCount")),description);
        } else if("fan_team".equals(id)) {
            String path = profileAwardPresentation.string(badge,"storagePath"), fallback = profileAwardPresentation.string(badge,"code");
            Uri cached = LOGOS.get(path);
            icon.showTeam(cached, fallback, description);
            if(cached == null && path.matches("teams/[a-z0-9_-]+_logo(_alt)?\\.png")) {
                FirebaseStorage.getInstance().getReference().child(path).getDownloadUrl().addOnSuccessListener(uri->{
                    LOGOS.put(path,uri);
                    if(request == renderGeneration)
                        icon.showTeam(uri,fallback,description);
                }).addOnFailureListener(error->android.util.Log.w("ProfileAwards","Team logo unavailable",error));
            }
        } else icon.showMedal(profileAwardPresentation.drawable(id),description);
    }

    private void clearIcons(View view) {
        if (view instanceof awardIconView) ((awardIconView)view).clear();
        else if(view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup)view;
            for(int i = 0; i < group.getChildCount(); i++)
                clearIcons(group.getChildAt(i));
        }
    }
}
