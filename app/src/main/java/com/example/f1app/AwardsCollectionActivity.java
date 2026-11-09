package com.example.f1app;

import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.functions.FirebaseFunctions;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class AwardsCollectionActivity extends AppCompatActivity {
    static final class Snapshot {
        final Map<String,Map<String,Object>> owned=new LinkedHashMap<>();
        boolean known,loadingAwards,awardsError,progressError;
        Long correct;
        Integer years;
    }
    private Snapshot data=new Snapshot();
    private String ownerUid;
    private int generation,filter;
    private boolean started;
    private TextView count,state;
    private ImageButton refresh;
    private TextView[] filters;
    private GridLayoutManager manager;
    private final CatalogueAdapter adapter=new CatalogueAdapter();
    private AwardDetailsDialog details;
    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);EdgeToEdge.enable(this);setContentView(R.layout.activity_awards_collection);
        count=findViewById(R.id.awardsCollectionCount);
        state=findViewById(R.id.awardsCollectionState);
        refresh=findViewById(R.id.awardsCollectionRefresh);
        findViewById(R.id.awardsCollectionBack).setOnClickListener(v->finish());
        filters=new TextView[]{findViewById(R.id.awardsFilterAll),findViewById(R.id.awardsFilterEarned),findViewById(R.id.awardsFilterLocked)};
        for(int i=0;i<filters.length;i++) {final int index=i;filters[i].setOnClickListener(v->{filter=index;render();});}
        if(savedInstanceState!=null)filter=Math.max(0,Math.min(2,savedInstanceState.getInt("filter",0)));
        RecyclerView list=findViewById(R.id.awardsCollectionList);manager=new GridLayoutManager(this,2);
        manager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override public int getSpanSize(int position) {return adapter.rows.get(position).entry==null?manager.getSpanCount():1;}
        });
        list.setLayoutManager(manager);list.setAdapter(adapter);
        list.addOnLayoutChangeListener((v,l,t,r,b,ol,ot,or,ob)->{
            int available=Math.max(1,v.getWidth()-v.getPaddingLeft()-v.getPaddingRight());
            int columns=Math.max(1,Math.min(3,available/Math.max(1,dp(148*Math.max(1f,getResources().getConfiguration().fontScale)))));
            if(manager.getSpanCount()!=columns)list.post(()->{if(!isDestroyed()&&manager.getSpanCount()!=columns){manager.setSpanCount(columns);adapter.notifyDataSetChanged();}});
        });
        refresh.setOnClickListener(v->load(true));
        WindowCompat.getInsetsController(getWindow(),getWindow().getDecorView()).setAppearanceLightStatusBars(false);
        View root=findViewById(R.id.awardsCollectionRoot),header=findViewById(R.id.awardsCollectionHeader);
        final int headerTop=header.getPaddingTop(),headerBottom=header.getPaddingBottom();
        ViewCompat.setOnApplyWindowInsetsListener(root,(v,insets)->{
            Insets safe=insets.getInsets(WindowInsetsCompat.Type.systemBars()|WindowInsetsCompat.Type.displayCutout());
            v.setPadding(safe.left,0,safe.right,safe.bottom);header.setPadding(0,headerTop+safe.top,0,headerBottom);return insets;
        });
        ViewCompat.requestApplyInsets(root);render();
    }
    @Override protected void onStart() {
        super.onStart();started=true;
        FirebaseUser user=FirebaseAuth.getInstance().getCurrentUser();
        if(user==null) {finish();return;}
        if(!user.getUid().equals(ownerUid)) {ownerUid=user.getUid();data=new Snapshot();}
        Long created=user.getMetadata()==null?null:user.getMetadata().getCreationTimestamp();
        data.years=AwardProgress.completedYears(created,System.currentTimeMillis());load(false);
    }
    @Override protected void onStop() {started=false;generation++;if(details!=null)details.dismiss();super.onStop();}
    @Override protected void onSaveInstanceState(@NonNull Bundle out) {out.putInt("filter",filter);super.onSaveInstanceState(out);}
    private boolean active(int request) {return started&&request==generation&&!isFinishing()&&!isDestroyed()&&ownerUid!=null&&ownerUid.equals(FirebaseAuth.getInstance().getUid());}
    private void load(boolean force) {
        if(ownerUid==null)return;final int request=++generation;
        data.loadingAwards=true;data.awardsError=false;data.progressError=false;refresh.setEnabled(false);render();
        if(force)AwardsRepository.invalidate(ownerUid);
        AwardsRepository.load(ownerUid,(badges,error)->{
            if(!active(request))return;
            data.loadingAwards=false;refresh.setEnabled(true);
            if(error!=null||badges==null) {data.awardsError=true;android.util.Log.w("AwardCatalogue","Awards unavailable",error);}
            else {
                data.owned.clear();
                for(Map<String,Object> badge:ProfileAwardPresentation.normalize(badges))data.owned.put(ProfileAwardPresentation.string(badge,"id"),badge);
                data.known=true;
            }
            render();
        });
        Map<String,Object> payload=new HashMap<>();payload.put("profileUid",ownerUid);
        FirebaseFunctions.getInstance("us-central1").getHttpsCallable("getPublicProfileStatistics").call(payload)
            .addOnSuccessListener(result->{
                if(!active(request))return;
                Object raw=result.getData(),totals=raw instanceof Map?((Map<?,?>)raw).get("totals"):null;
                Long value=totals instanceof Map?AwardProgress.count(((Map<?,?>)totals).get("correctPredictions")):null;
                if(value==null)data.progressError=true;else data.correct=value;
                render();
            }).addOnFailureListener(error->{
                if(!active(request))return;
                data.progressError=true;android.util.Log.w("AwardCatalogue","Prediction progress unavailable",error);render();
            });
    }
    private void render() {
        if(filters==null)return;
        for(int i=0;i<filters.length;i++) {
            boolean selected=i==filter;filters[i].setSelected(selected);filters[i].setTypeface(null,selected?Typeface.BOLD:Typeface.NORMAL);
            filters[i].setTextColor(color(selected?R.color.text_color_main:R.color.grey));
            GradientDrawable bg=new GradientDrawable();bg.setCornerRadius(dp(12));bg.setColor(selected?color(R.color.account_settings_input_color):android.graphics.Color.TRANSPARENT);filters[i].setBackground(bg);
            filters[i].setContentDescription(selected?getString(R.string.awards_filter_accessibility,filters[i].getText()):filters[i].getText());
        }
        count.setText(data.known?getString(R.string.awards_catalogue_count,data.owned.size(),AwardCatalogue.ALL.size()):"—");
        if(data.loadingAwards)state.setText(R.string.awards_catalogue_loading);
        else if(data.awardsError)state.setText(R.string.awards_catalogue_error);
        else if(data.progressError)state.setText(R.string.awards_catalogue_progress_error);
        else state.setText("");
        state.setVisibility(state.length()==0?View.GONE:View.VISIBLE);
        adapter.rows.clear();
        if(!data.known&&filter!=0)adapter.rows.add(new Row(null,data.loadingAwards?R.string.awards_status_loading:R.string.awards_status_unknown));
        else {
            for(int category=0;category<4;category++) {
                List<AwardCatalogue.Entry> entries=new ArrayList<>();
                for(AwardCatalogue.Entry entry:AwardCatalogue.ALL)if(entry.category==category) {
                    boolean earned=data.owned.containsKey(entry.id);
                    if(filter==0||(filter==1&&earned)||(filter==2&&!earned))entries.add(entry);
                }
                if(!entries.isEmpty()) {adapter.rows.add(new Row(null,AwardCatalogue.categoryTitle(category)));for(AwardCatalogue.Entry entry:entries)adapter.rows.add(new Row(entry,0));}
            }
            if(adapter.rows.isEmpty())adapter.rows.add(new Row(null,R.string.awards_catalogue_empty_filter));
        }
        adapter.notifyDataSetChanged();
    }
    String status(AwardCatalogue.Entry entry,boolean verbose) {
        if(!data.known)return getString(data.loadingAwards?R.string.awards_status_loading:R.string.awards_status_unknown);
        boolean earned=data.owned.containsKey(entry.id);
        if(entry.category==AwardCatalogue.FANCLUB)return getString(earned?R.string.awards_status_active:R.string.awards_status_inactive);
        if(earned)return getString(R.string.awards_status_earned);
        if(AwardProgress.conditionMet(entry,data.correct,data.years))return getString(verbose?R.string.awards_waiting_issue:R.string.awards_status_pending);
        return getString(R.string.awards_status_locked);
    }
    String condition(AwardCatalogue.Entry entry) {
        if(entry.category==AwardCatalogue.TENURE)return getResources().getQuantityString(R.plurals.award_condition_tenure_years,entry.target,entry.target);
        return entry.target>0?getString(entry.condition,entry.target):getString(entry.condition);
    }
    String caption(AwardCatalogue.Entry entry) {
        String title=getString(ProfileAwardPresentation.title(entry.id));
        return entry.category==AwardCatalogue.SEASON&&!title.contains("2026")?title+" · 2026":title;
    }
    private int color(int resource) {return ContextCompat.getColor(this,resource);}
    private int dp(float n) {return Math.round(n*getResources().getDisplayMetrics().density);}
    private TextView label(int size,boolean heading) {
        TextView text=new TextView(this);text.setTextAppearance(this,heading?R.style.TextStyle_GoodTiming_Regular_ForHeader:R.style.TextStyle_Gram_Regular_Simple);text.setTextSize(size);text.setTextColor(color(R.color.text_color_main));return text;
    }
    private static final class Row {final AwardCatalogue.Entry entry;final int label;Row(AwardCatalogue.Entry e,int label){entry=e;this.label=label;}}
    private static final class Holder extends RecyclerView.ViewHolder {
        AwardIconView icon;TextView title,status,progress;ProgressBar bar;int token;String id;
        Holder(View root){super(root);}
    }
    private final class CatalogueAdapter extends RecyclerView.Adapter<Holder> {
        final List<Row> rows=new ArrayList<>();
        @Override public int getItemCount(){return rows.size();}
        @Override public int getItemViewType(int position){return rows.get(position).entry==null?0:1;}
        @Override public Holder onCreateViewHolder(ViewGroup parent,int type) {
            if(type==0) {TextView heading=label(16,true);heading.setPadding(dp(4),dp(20),dp(4),dp(10));heading.setLayoutParams(new RecyclerView.LayoutParams(-1,-2));return new Holder(heading);}
            CardView root=new CardView(AwardsCollectionActivity.this);root.setRadius(dp(16));root.setCardElevation(0);root.setCardBackgroundColor(color(R.color.account_settings_input_color));
            RecyclerView.LayoutParams rp=new RecyclerView.LayoutParams(-1,-2);rp.setMargins(dp(4),dp(4),dp(4),dp(4));root.setLayoutParams(rp);
            LinearLayout body=new LinearLayout(AwardsCollectionActivity.this);body.setOrientation(LinearLayout.VERTICAL);body.setGravity(Gravity.CENTER_HORIZONTAL);body.setPadding(dp(12),dp(14),dp(12),dp(14));root.addView(body,new ViewGroup.LayoutParams(-1,-2));
            Holder h=new Holder(root);h.icon=new AwardIconView(AwardsCollectionActivity.this);body.addView(h.icon,new LinearLayout.LayoutParams(dp(76),dp(76)));
            h.title=label(13,false);h.title.setGravity(Gravity.CENTER);h.title.setMinLines(2);body.addView(h.title,new LinearLayout.LayoutParams(-1,-2));
            h.status=label(11,false);h.status.setGravity(Gravity.CENTER);h.status.setPadding(0,dp(8),0,dp(4));body.addView(h.status,new LinearLayout.LayoutParams(-1,-2));
            h.bar=new ProgressBar(AwardsCollectionActivity.this,null,android.R.attr.progressBarStyleHorizontal);h.bar.setProgressTintList(ColorStateList.valueOf(color(R.color.neuro_accent)));h.bar.setProgressBackgroundTintList(ColorStateList.valueOf(color(R.color.stroke_option_color)));body.addView(h.bar,new LinearLayout.LayoutParams(-1,dp(5)));
            h.progress=label(11,false);h.progress.setTextColor(color(R.color.grey));h.progress.setGravity(Gravity.CENTER);h.progress.setPadding(0,dp(6),0,0);body.addView(h.progress,new LinearLayout.LayoutParams(-1,-2));
            return h;
        }
        @Override public void onBindViewHolder(Holder h,int position) {
            Row row=rows.get(position);
            if(row.entry==null){((TextView)h.itemView).setText(row.label);return;}
            AwardCatalogue.Entry entry=row.entry;h.token++;int token=h.token;h.id=entry.id;
            h.icon.clear();String title=caption(entry);h.title.setText(title);h.status.setText(status(entry,false));
            boolean earned=data.owned.containsKey(entry.id);h.icon.setAlpha(!data.known?.7f:earned?1f:.38f);
            Map<String,Object> record=data.owned.get(entry.id);
            String driverOrTeam=record==null?"":ProfileAwardPresentation.string(record,"name");
            CatalogueAwardIcons.bind(h.icon,entry,record,title+(driverOrTeam.isEmpty()?"":": "+driverOrTeam),()->token==h.token&&entry.id.equals(h.id));
            Long current=AwardProgress.current(entry,data.correct,data.years);
            boolean measurable=entry.target>0&&(entry.category==AwardCatalogue.PREDICTIONS||entry.category==AwardCatalogue.TENURE);
            h.bar.setVisibility(measurable&&current!=null?View.VISIBLE:View.GONE);h.progress.setVisibility(measurable?View.VISIBLE:View.GONE);
            if(measurable) {
                if(current==null)h.progress.setText(R.string.awards_progress_unavailable);
                else {h.progress.setText(getString(entry.category==AwardCatalogue.PREDICTIONS?R.string.awards_progress_correct:R.string.awards_progress_years,current,(long)entry.target));h.bar.setMax(entry.target);h.bar.setProgress((int)Math.min(current,(long)entry.target));}
            }
            h.itemView.setOnClickListener(v->{
                if(isFinishing()||isDestroyed())return;
                if(details!=null)details.dismiss();details=new AwardDetailsDialog(AwardsCollectionActivity.this,entry,data,caption(entry),condition(entry),status(entry,true));details.show();
            });
        }
        @Override public void onViewRecycled(Holder holder) {holder.token++;holder.id=null;if(holder.icon!=null)holder.icon.clear();holder.itemView.setOnClickListener(null);super.onViewRecycled(holder);}
    }
}
