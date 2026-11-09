package com.example.f1app;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.core.content.ContextCompat;
import com.facebook.shimmer.ShimmerFrameLayout;
import com.google.firebase.storage.FirebaseStorage;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Stage 1: read-only earned-awards card. Rendering of medal artwork stays in AwardIconView. */
public final class ProfileAwardsCard extends FrameLayout {
    private static final Map<String,Uri> LOGOS=new ConcurrentHashMap<>();
    private final TextView count,refresh,empty,message,expand;
    private final GridLayout grid,skeleton;
    private final ShimmerFrameLayout shimmer,updating;
    private List<Map<String,Object>> data=Collections.emptyList();
    private boolean hasData,expanded,loading;
    private int columns=3,renderGeneration;

    public ProfileAwardsCard(Context context) {this(context,null);}
    public ProfileAwardsCard(Context context,AttributeSet attrs) {
        super(context,attrs);
        LayoutInflater.from(context).inflate(R.layout.view_profile_awards_card,this,true);
        count=findViewById(R.id.profileAwardsCount);
        refresh=findViewById(R.id.profileAwardsRefresh);
        empty=findViewById(R.id.profileAwardsEmpty);
        message=findViewById(R.id.profileAwardsMessage);
        expand=findViewById(R.id.profileAwardsExpand);
        grid=findViewById(R.id.profileAwardsGrid);
        skeleton=findViewById(R.id.profileAwardsSkeleton);
        shimmer=findViewById(R.id.profileAwardsShimmer);
        updating=findViewById(R.id.profileAwardsUpdating);
        expand.setOnClickListener(v->{expanded=!expanded;rebuild();});
        buildSkeleton();
    }
    private int dp(float n) {return Math.round(n*getResources().getDisplayMetrics().density);}
    private GradientDrawable surface(int color,float radius) {
        GradientDrawable shape=new GradientDrawable();shape.setColor(ContextCompat.getColor(getContext(),color));shape.setCornerRadius(dp(radius));return shape;
    }
    public void setRefreshAction(Runnable action) {refresh.setOnClickListener(v->action.run());}
    public boolean hasData() {return hasData;}
    public void setBusy(boolean busy) {refresh.setEnabled(!busy);refresh.setAlpha(busy?.45f:1f);}
    public void showLoading() {
        loading=true;setBusy(true);message.setVisibility(GONE);
        if(hasData) {
            updating.setContentDescription(getContext().getString(R.string.profile_awards_updating));
            updating.setVisibility(VISIBLE);updating.startShimmer();
        } else {
            count.setText("—");count.setContentDescription(null);
            empty.setVisibility(GONE);grid.setVisibility(GONE);expand.setVisibility(GONE);
            shimmer.setVisibility(VISIBLE);shimmer.startShimmer();
        }
    }
    public void showData(List<Map<String,Object>> values) {
        List<Map<String,Object>> normalized=ProfileAwardPresentation.normalize(values);
        boolean changed=!hasData||!data.equals(normalized);
        data=normalized;hasData=true;finishLoading();message.setVisibility(GONE);
        count.setText(java.text.NumberFormat.getIntegerInstance().format(data.size()));
        count.setContentDescription(getContext().getString(R.string.profile_awards_count_accessibility,data.size()));
        empty.setVisibility(data.isEmpty()?VISIBLE:GONE);
        if(changed)rebuild();else updateFooter();
        grid.setVisibility(data.isEmpty()?GONE:VISIBLE);
    }
    public void showError() {
        finishLoading();
        message.setText(hasData?R.string.profile_awards_refresh_error:R.string.profile_awards_error);
        message.setVisibility(VISIBLE);
        if(!hasData) {count.setText("—");empty.setVisibility(GONE);grid.setVisibility(GONE);expand.setVisibility(GONE);}
    }
    public void showSyncWarning() {
        message.setText(R.string.profile_awards_sync_error);message.setVisibility(VISIBLE);
    }
    public void finishLoading() {
        loading=false;setBusy(false);stopAnimations();
        shimmer.setVisibility(GONE);updating.setVisibility(GONE);
    }
    public void stopAnimations() {shimmer.stopShimmer();updating.stopShimmer();}
    public void resetOwner() {
        finishLoading();renderGeneration++;clearIcons(grid);grid.removeAllViews();
        data=Collections.emptyList();hasData=false;expanded=false;
        grid.setVisibility(GONE);empty.setVisibility(GONE);message.setVisibility(GONE);expand.setVisibility(GONE);
        count.setText("—");count.setContentDescription(null);
    }
    public void release() {resetOwner();setRefreshAction(()->{});}
    @Override protected void onSizeChanged(int w, int h, int oldW, int oldH) {
        super.onSizeChanged(w, h, oldW, oldH);
        float fontScale = Math.max(1f, getResources().getConfiguration().fontScale);
        int available = Math.max(0, w - dp(32));
        int newColumns = Math.max(1, Math.min(4,
                available / Math.max(1, dp(90 * fontScale))));
        if (newColumns != columns || w != oldW) {
            columns = newColumns;
            post(() -> {
                if (getWidth() != w) return;
                buildSkeleton();
                if (hasData) rebuild();
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
    private void buildSkeleton() {
        skeleton.removeAllViews();skeleton.setColumnCount(columns);
        for(int i=0;i<columns*2;i++) {
            LinearLayout cell=new LinearLayout(getContext());cell.setOrientation(LinearLayout.VERTICAL);cell.setGravity(Gravity.CENTER_HORIZONTAL);
            cell.setPadding(dp(6),dp(12),dp(6),dp(12));cell.setMinimumHeight(dp(118));
            View icon=new View(getContext());icon.setBackground(surface(R.color.stroke_option_color,10));
            cell.addView(icon,new LinearLayout.LayoutParams(dp(56),dp(56)));
            View line=new View(getContext());line.setBackground(surface(R.color.stroke_option_color,3));
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(dp(48),dp(8));lp.topMargin=dp(12);cell.addView(line,lp);
            View line2=new View(getContext());line2.setBackground(surface(R.color.stroke_option_color,3));
            LinearLayout.LayoutParams lp2=new LinearLayout.LayoutParams(dp(34),dp(7));lp2.topMargin=dp(5);cell.addView(line2,lp2);
            cell.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
            skeleton.addView(cell,position(i));
        }
    }
    private void updateFooter() {
        int preview=columns*2;
        expand.setVisibility(data.size()>preview?VISIBLE:GONE);
        expand.setText(expanded?getContext().getString(R.string.profile_awards_collapse):getContext().getString(R.string.profile_awards_show_all,data.size()));
    }
    private void rebuild() {
        renderGeneration++;int request=renderGeneration;
        clearIcons(grid);grid.removeAllViews();grid.setColumnCount(columns);
        int limit=expanded?data.size():Math.min(columns*2,data.size());
        for(int i=0;i<limit;i++) {
            Map<String,Object> badge=data.get(i);String id=ProfileAwardPresentation.string(badge,"id");
            String caption=getContext().getString(ProfileAwardPresentation.title(id));
            String name=ProfileAwardPresentation.string(badge,"name");
            String description=caption+(name.isEmpty()?"":": "+name);
            if(id.startsWith("season_"))description+=" · 2026";
            LinearLayout cell=new LinearLayout(getContext());cell.setOrientation(LinearLayout.VERTICAL);cell.setGravity(Gravity.CENTER_HORIZONTAL);
            cell.setPadding(dp(4),dp(10),dp(4),dp(10));cell.setMinimumHeight(dp(118));cell.setBackground(surface(R.color.colorPrimary,12));
            AwardIconView icon=new AwardIconView(getContext());cell.addView(icon,new LinearLayout.LayoutParams(dp(64),dp(64)));
            bindIcon(icon,badge,id,description,request);
            TextView label=new TextView(getContext());
            label.setTextAppearance(getContext(),R.style.TextStyle_Gram_Regular_Simple);
            label.setText(caption);label.setTextSize(12);label.setGravity(Gravity.CENTER);label.setMaxLines(3);
            label.setTextColor(ContextCompat.getColor(getContext(),R.color.text_color_main));
            label.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.topMargin=dp(6);cell.addView(label,lp);
            grid.addView(cell,position(i));
        }
        updateFooter();grid.setVisibility(data.isEmpty()?GONE:VISIBLE);
    }
    private void bindIcon(AwardIconView icon,Map<String,Object> badge,String id,String description,int request) {
        if("fan_driver".equals(id)) {
            icon.showDriver(ProfileAwardPresentation.string(badge,"code"),ProfileAwardPresentation.titles(badge.get("championshipsCount")),description);
        } else if("fan_team".equals(id)) {
            String path=ProfileAwardPresentation.string(badge,"storagePath"),fallback=ProfileAwardPresentation.string(badge,"code");
            Uri cached=LOGOS.get(path);icon.showTeam(cached,fallback,description);
            if(cached==null&&path.matches("teams/[a-z0-9_-]+_logo(_alt)?\\.png")) {
                FirebaseStorage.getInstance().getReference().child(path).getDownloadUrl().addOnSuccessListener(uri->{
                    LOGOS.put(path,uri);
                    if(request==renderGeneration)icon.showTeam(uri,fallback,description);
                }).addOnFailureListener(error->android.util.Log.w("ProfileAwards","Team logo unavailable",error));
            }
        } else icon.showMedal(ProfileAwardPresentation.drawable(id),description);
    }
    private void clearIcons(View view) {
        if(view instanceof AwardIconView)((AwardIconView)view).clear();
        else if(view instanceof ViewGroup) {
            ViewGroup group=(ViewGroup)view;
            for(int i=0;i<group.getChildCount();i++)clearIcons(group.getChildAt(i));
        }
    }
}
