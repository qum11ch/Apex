package com.example.f1app;

import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.Objects;

/** Only profile headers and profile settings; leaderboard is a separate stage. */
public final class DisplayedAwardUi {
    private static final String PROFILE_TAG="apex.displayed.award.header";
    private static final String SETTINGS_TAG="apex.displayed.award.settings";
    private DisplayedAwardUi() {}
    private static int dp(View view,float value){return Math.round(value*view.getResources().getDisplayMetrics().density);}
    public static void attachProfile(AppCompatActivity activity,boolean own) {
        TextView name=activity.findViewById(R.id.username);
        if(name==null||!(name.getParent() instanceof ViewGroup))return;
        ViewGroup parent=(ViewGroup)name.getParent();
        if(activity.findViewById(android.R.id.content).findViewWithTag(PROFILE_TAG)!=null)return;
        int index=parent.indexOfChild(name);ViewGroup.LayoutParams old=name.getLayoutParams();parent.removeView(name);
        ConstraintLayout row=new ConstraintLayout(activity);row.setTag(PROFILE_TAG);parent.addView(row,index,old);
        AwardIconView icon=new AwardIconView(activity);icon.setId(View.generateViewId());icon.setVisibility(View.GONE);
        ConstraintLayout.LayoutParams textParams=new ConstraintLayout.LayoutParams(-2,-2);
        textParams.startToStart=ConstraintLayout.LayoutParams.PARENT_ID;textParams.endToStart=icon.getId();textParams.topToTop=0;textParams.bottomToBottom=0;textParams.constrainedWidth=true;textParams.horizontalChainStyle=ConstraintLayout.LayoutParams.CHAIN_PACKED;textParams.horizontalBias=0f;
        row.addView(name,textParams);
        ConstraintLayout.LayoutParams iconParams=new ConstraintLayout.LayoutParams(dp(row,32),dp(row,32));iconParams.startToEnd=name.getId();iconParams.endToEnd=0;iconParams.topToTop=0;iconParams.bottomToBottom=0;iconParams.setMarginStart(dp(row,8));row.addView(icon,iconParams);
        observe(activity,own,icon,null,null);
    }
    public static void attachSettings(AppCompatActivity activity) {
        View anchor=activity.findViewById(R.id.usernameMain);
        if(anchor==null||!(anchor.getParent() instanceof LinearLayout))return;
        LinearLayout parent=(LinearLayout)anchor.getParent();if(parent.findViewWithTag(SETTINGS_TAG)!=null)return;
        CardView card=new CardView(activity);card.setTag(SETTINGS_TAG);card.setRadius(dp(card,18));card.setCardElevation(0);card.setCardBackgroundColor(ContextCompat.getColor(activity,R.color.account_settings_input_color));
        LinearLayout body=new LinearLayout(activity);body.setOrientation(LinearLayout.VERTICAL);body.setPadding(dp(card,16),dp(card,12),dp(card,16),dp(card,12));card.addView(body,new ViewGroup.LayoutParams(-1,-2));
        TextView heading=new TextView(activity);heading.setTextAppearance(activity,R.style.TextStyle_GoodTiming_Regular_ForHeader);heading.setTextSize(14);heading.setTextColor(ContextCompat.getColor(activity,R.color.text_color_main));heading.setText(R.string.displayed_award_title);body.addView(heading);
        LinearLayout line=new LinearLayout(activity);line.setGravity(Gravity.CENTER_VERTICAL);line.setOrientation(LinearLayout.HORIZONTAL);body.addView(line,new LinearLayout.LayoutParams(-1,-2));
        AwardIconView icon=new AwardIconView(activity);line.addView(icon,new LinearLayout.LayoutParams(dp(card,56),dp(card,56)));
        LinearLayout texts=new LinearLayout(activity);texts.setOrientation(LinearLayout.VERTICAL);LinearLayout.LayoutParams tp=new LinearLayout.LayoutParams(0,-2,1);tp.setMarginStart(dp(card,12));line.addView(texts,tp);
        TextView selected=new TextView(activity);selected.setTextAppearance(activity,R.style.TextStyle_Gram_Regular_Simple);selected.setTextColor(ContextCompat.getColor(activity,R.color.text_color_main));selected.setTextSize(15);selected.setText(R.string.displayed_award_loading);texts.addView(selected);
        TextView state=new TextView(activity);state.setTextAppearance(activity,R.style.TextStyle_Gram_Regular_Simple);state.setTextColor(ContextCompat.getColor(activity,R.color.grey));state.setTextSize(12);state.setText(R.string.displayed_award_subtitle);texts.addView(state);
        android.widget.ImageView arrow=new android.widget.ImageView(activity);arrow.setImageResource(R.drawable.arrow);arrow.setScaleX(-1);line.addView(arrow,new LinearLayout.LayoutParams(dp(card,24),dp(card,24)));
        card.setOnClickListener(v->activity.startActivity(new Intent(activity,DisplayedAwardPickerActivity.class)));card.setFocusable(true);
        LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(-1,-2);cp.setMargins(dp(card,20),dp(card,16),dp(card,20),0);parent.addView(card,parent.indexOfChild(anchor)+1,cp);
        observe(activity,true,icon,selected,state);
    }
    private static void observe(AppCompatActivity activity,boolean own,AwardIconView icon,TextView title,TextView state) {
        activity.getLifecycle().addObserver(new DefaultLifecycleObserver() {
            private boolean started,hasValue;
            private int generation,session;
            private String uid,key;
            private DatabaseReference watched;
            private ValueEventListener listener;
            private void load() {
                if(!started||uid==null)return;
                final int request=++generation,expectedSession=session;final String requested=uid;
                DisplayedAwardApi.get(uid,(selection,error)->{
                    if(!started||generation!=request||session!=expectedSession||activity.isFinishing()||activity.isDestroyed()||!requested.equals(uid))return;
                    if(own&&!requested.equals(FirebaseAuth.getInstance().getUid()))return;
                    if(error!=null) {
                        android.util.Log.w("DisplayedAward","Selection unavailable",error);
                        if(title!=null){if(!hasValue)title.setText(R.string.displayed_award_load_error);state.setText(R.string.displayed_award_load_error);}
                        return;
                    }
                    hasValue=true;icon.clear();
                    if(selection.id==null) {
                        icon.setVisibility(View.GONE);if(title!=null)title.setText(R.string.displayed_award_none);
                    } else {
                        icon.setVisibility(View.VISIBLE);AwardCatalogue.Entry entry=AwardCatalogue.find(selection.id);
                        String caption=activity.getString(ProfileAwardPresentation.title(selection.id));
                        String name=ProfileAwardPresentation.string(selection.award,"name");
                        CatalogueAwardIcons.bind(icon,entry,selection.award,caption+(name.isEmpty()?"":": "+name),()->started&&generation==request&&session==expectedSession&&requested.equals(uid));
                        if(title!=null)title.setText(caption);
                    }
                    if(state!=null)state.setText(R.string.displayed_award_subtitle);
                });
            }
            @Override public void onStart(@NonNull LifecycleOwner owner) {
                started=true;session++;
                String current=own?FirebaseAuth.getInstance().getUid():activity.getIntent().getStringExtra("profileUid");
                if(!Objects.equals(current,uid)){uid=current;hasValue=false;icon.clear();icon.setVisibility(View.GONE);}
                if(uid==null||uid.isEmpty()||uid.matches(".*[.#$\\[\\]/\\p{Cntrl}].*"))return;
                key=null;
                watched=FirebaseDatabase.getInstance().getReference(own?"users":"publicProfiles").child(uid);
                listener=new ValueEventListener() {
                    @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                        String next=String.valueOf(snapshot.child("displayedAwardId").getValue());
                        if(own)next+="|"+String.valueOf(snapshot.child("choiceDriver").getValue())+"|"+String.valueOf(snapshot.child("choiceTeam").getValue());
                        if(!Objects.equals(key,next)){key=next;load();}
                    }
                    @Override public void onCancelled(@NonNull DatabaseError error){load();}
                };
                watched.addValueEventListener(listener);
            }
            @Override public void onStop(@NonNull LifecycleOwner owner) {
                started=false;session++;generation++;
                if(watched!=null&&listener!=null)watched.removeEventListener(listener);watched=null;listener=null;
            }
            @Override public void onDestroy(@NonNull LifecycleOwner owner){icon.clear();}
        });
    }
}
