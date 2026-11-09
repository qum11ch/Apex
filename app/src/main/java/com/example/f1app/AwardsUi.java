package com.example.f1app;

import android.view.View;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.functions.FirebaseFunctions;
import java.util.Collections;

public final class AwardsUi {
    private static final String ROW_TAG="apex.awards.row";
    private AwardsUi() {}
    public static void attachOwn(AppCompatActivity activity) {ProfileAwardsController.attach(activity);}
    public static void attachPublic(AppCompatActivity activity) {PublicProfileAwardsController.attach(activity);}
    private static int dp(View v,int n) {return Math.round(n*v.getResources().getDisplayMetrics().density);}
    private static void attachProfile(AppCompatActivity activity,boolean own) {
        View anchor=activity.findViewById(R.id.createdAt);
        if(anchor==null || !(anchor.getParent() instanceof LinearLayout)) throw new IllegalStateException("Awards require the profile createdAt LinearLayout anchor");
        LinearLayout parent=(LinearLayout)anchor.getParent();
        LinearLayout panel=new LinearLayout(activity);panel.setOrientation(LinearLayout.VERTICAL);panel.setPadding(dp(panel,10),dp(panel,12),dp(panel,10),dp(panel,6));
        TextView heading=new TextView(activity);heading.setText(R.string.badges_section_title);heading.setTextSize(16);heading.setTextColor(ContextCompat.getColor(activity,R.color.text_color_main));panel.addView(heading);
        HorizontalScrollView scroll=new HorizontalScrollView(activity);scroll.setHorizontalScrollBarEnabled(false);scroll.setFillViewport(false);
        AwardStrip strip=new AwardStrip(activity,false);scroll.addView(strip,new ViewGroup.LayoutParams(-2,-2));panel.addView(scroll,new LinearLayout.LayoutParams(-1,-2));
        TextView state=new TextView(activity);state.setTextSize(13);state.setTextColor(ContextCompat.getColor(activity,R.color.grey));state.setPadding(0,dp(panel,6),0,dp(panel,6));panel.addView(state);
        parent.addView(panel,parent.indexOfChild(anchor)+1,new LinearLayout.LayoutParams(-1,-2));
        activity.getLifecycle().addObserver(new DefaultLifecycleObserver() {
            private boolean started;
            private String uid;
            private int generation;
            private DatabaseReference favorites;
            private ValueEventListener listener;
            private String favoriteKey;
            private void load() {
                if(!started || uid==null || uid.isEmpty()) return;
                final String requested=uid;final int request=++generation;final int stripRequest=strip.begin(uid);
                state.setText(R.string.badges_loading);state.setVisibility(View.VISIBLE);
                AwardsRepository.load(uid,(badges,error)->{
                    if(!started || request!=generation || !requested.equals(uid)) return;
                    if(error!=null) {state.setText(R.string.badges_load_error);state.setVisibility(View.VISIBLE);return;}
                    strip.render(requested,stripRequest,badges);
                    state.setText(R.string.badges_empty);state.setVisibility(badges.isEmpty()?View.VISIBLE:View.GONE);
                });
            }
            @Override public void onStart(@NonNull LifecycleOwner owner) {
                started=true;
                uid=own?FirebaseAuth.getInstance().getUid():activity.getIntent().getStringExtra("profileUid");
                if(uid==null || uid.isEmpty()) {panel.setVisibility(View.GONE);return;}
                panel.setVisibility(View.VISIBLE);
                state.setOnClickListener(v->{AwardsRepository.invalidate(uid);load();});
                load();
                if(own) {
                    final String requested=uid;
                    FirebaseFunctions.getInstance("us-central1").getHttpsCallable("syncMyAwards").call(Collections.emptyMap()).addOnSuccessListener(result->{
                        AwardsRepository.invalidate(requested);if(started && requested.equals(uid)) load();
                    }).addOnFailureListener(error->{
                        if(started && requested.equals(uid)) {state.setText(R.string.badges_sync_error);state.setVisibility(View.VISIBLE);}
                    });
                    favoriteKey=null;
                    favorites=FirebaseDatabase.getInstance().getReference("users").child(uid);
                    listener=new ValueEventListener() {
                        @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                            String key=String.valueOf(snapshot.child("choiceDriver").getValue())+"\u0000"+String.valueOf(snapshot.child("choiceTeam").getValue());
                            if(favoriteKey!=null && !favoriteKey.equals(key)) {AwardsRepository.invalidate(requested);load();}
                            favoriteKey=key;
                        }
                        @Override public void onCancelled(@NonNull DatabaseError error) {android.util.Log.w("Awards","Favorite updates unavailable",error.toException());}
                    };
                    favorites.addValueEventListener(listener);
                }
            }
            @Override public void onStop(@NonNull LifecycleOwner owner) {
                started=false;generation++;strip.clear();
                if(favorites!=null && listener!=null) favorites.removeEventListener(listener);
                favorites=null;listener=null;
            }
            @Override public void onDestroy(@NonNull LifecycleOwner owner) {strip.clear();}
        });
    }
    public static void bindRow(View root,String uid) {
        TextView name=root.findViewById(R.id.lbName);
        if(name==null || uid==null || uid.isEmpty()) return;
        LinearLayout wrapper=root.findViewWithTag(ROW_TAG);
        AwardStrip strip;
        if(wrapper==null) {
            if(!(name.getParent() instanceof LinearLayout)) return;
            LinearLayout parent=(LinearLayout)name.getParent();int index=parent.indexOfChild(name);ViewGroup.LayoutParams old=name.getLayoutParams();parent.removeView(name);
            wrapper=new LinearLayout(root.getContext());wrapper.setOrientation(LinearLayout.VERTICAL);wrapper.setTag(ROW_TAG);parent.addView(wrapper,index,old);
            wrapper.addView(name,new LinearLayout.LayoutParams(-1,-2));
            strip=new AwardStrip(root.getContext(),true);wrapper.addView(strip,new LinearLayout.LayoutParams(-2,-2));
        } else strip=(AwardStrip)wrapper.getChildAt(1);
        final int request=strip.begin(uid);final AwardStrip target=strip;
        AwardsRepository.load(uid,(badges,error)->{
            if(!target.current(uid,request)) return;
            if(error==null) target.render(uid,request,badges);else target.setVisibility(View.GONE);
        });
    }
    public static void clearRow(View root) {
        LinearLayout wrapper=root.findViewWithTag(ROW_TAG);
        if(wrapper!=null && wrapper.getChildCount()>1) ((AwardStrip)wrapper.getChildAt(1)).clear();
    }
}
