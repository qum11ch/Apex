package com.example.f1app;

import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
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
import java.util.Objects;

/** Stage 1: replaces only the personal-profile strip, using the existing repository/API. */
public final class profileAwardsController implements DefaultLifecycleObserver {
    private static final String TAG="apex.profile.awards.stage1";
    private final AppCompatActivity activity;
    private final profileAwardsCard card;
    private final Handler main=new Handler(Looper.getMainLooper());
    private boolean started;
    private boolean syncing;
    private String uid;
    private int session,readGeneration;
    private DatabaseReference favorites;
    private ValueEventListener listener;
    private String lastDriver,lastTeam;
    private boolean favoritesSeen;

    public static void attach(AppCompatActivity activity) {
        View anchor=activity.findViewById(R.id.createdAt);
        if(anchor==null||!(anchor.getParent() instanceof LinearLayout))throw new IllegalStateException("Profile awards: expected account_page createdAt anchor");
        LinearLayout parent=(LinearLayout)anchor.getParent();
        if(parent.findViewWithTag(TAG)!=null)return;
        View insertion=anchor;
        View stats=activity.findViewById(R.id.prediction_count);
        if(stats!=null) {
            View candidate=stats;
            while(candidate.getParent() instanceof View&&candidate.getParent()!=parent)candidate=(View)candidate.getParent();
            if(candidate.getParent()==parent)insertion=candidate;
        }
        profileAwardsCard card=new profileAwardsCard(activity);card.setTag(TAG);
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin=Math.round(16*activity.getResources().getDisplayMetrics().density);
        lp.bottomMargin=Math.round(4*activity.getResources().getDisplayMetrics().density);
        parent.addView(card,parent.indexOfChild(insertion)+1,lp);
        card.setCollectionAction(() -> activity.startActivity(
                new android.content.Intent(activity, awardsCollectionActivity.class)));
        activity.getLifecycle().addObserver(new profileAwardsController(activity,card));
    }
    private profileAwardsController(AppCompatActivity activity, profileAwardsCard card) {
        this.activity=activity;this.card=card;card.setRefreshAction(()->sync(true));
    }
    private boolean active(int expectedSession,String expectedUid) {
        return started&&session==expectedSession&&!activity.isFinishing()&&!activity.isDestroyed()
                &&expectedUid!=null&&expectedUid.equals(uid)&&expectedUid.equals(FirebaseAuth.getInstance().getUid());
    }
    private void scheduleLoading(int expectedSession,String expectedUid,int request) {
        main.postDelayed(()->{
            if(active(expectedSession,expectedUid)&&request==readGeneration)card.showLoading();
        },120);
    }
    private void read(boolean force,boolean syncWarning) {
        if(!started||uid==null)return;
        int expectedSession=session,request=++readGeneration;String requested=uid;
        if(force) awardsRepository.invalidate(uid);
        card.setBusy(true);
        scheduleLoading(expectedSession,requested,request);
        awardsRepository.load(uid,(badges, error)->{
            if(!active(expectedSession,requested)||request!=readGeneration)return;
            // Cancel the pending loading transition before showing the result.
            readGeneration++;
            if(error!=null) {
                android.util.Log.w("ProfileAwards","Awards read failed",error);card.showError();return;
            }
            card.showData(badges);
            if(syncWarning)card.showSyncWarning();
        });
    }
    private void sync(boolean manual) {
        if(!started||uid==null)return;
        if(syncing) {if(manual)card.showLoading();return;}
        syncing=true;
        int expectedSession=session;String requested=uid;
        if(manual) {
            int request=++readGeneration;card.setBusy(true);scheduleLoading(expectedSession,requested,request);
        }
        FirebaseFunctions.getInstance("us-central1").getHttpsCallable("syncMyAwards").call(Collections.emptyMap())
                .addOnCompleteListener(task->{
                    if(!active(expectedSession,requested))return;
                    syncing=false;
                    if(!task.isSuccessful())android.util.Log.w("ProfileAwards","Achievement check failed",task.getException());
                    read(true,!task.isSuccessful());
                });
    }
    @Override public void onStart(@NonNull LifecycleOwner owner) {
        started=true;session++;
        String current=FirebaseAuth.getInstance().getUid();
        if(!Objects.equals(uid,current)) {card.resetOwner();uid=current;}
        if(uid==null) {card.setVisibility(View.GONE);return;}
        card.setVisibility(View.VISIBLE);read(false,false);sync(false);
        favoritesSeen=false;favorites=FirebaseDatabase.getInstance().getReference("users").child(uid);
        final int expectedSession=session;final String requested=uid;
        listener=new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                if(!active(expectedSession,requested))return;
                String driver=String.valueOf(snapshot.child("choiceDriver").getValue());
                String team=String.valueOf(snapshot.child("choiceTeam").getValue());
                boolean changed=favoritesSeen&&(!Objects.equals(lastDriver,driver)||!Objects.equals(lastTeam,team));
                lastDriver=driver;lastTeam=team;favoritesSeen=true;
                if(changed)read(true,false);
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {android.util.Log.w("ProfileAwards","Favorite listener cancelled",error.toException());}
        };
        favorites.addValueEventListener(listener);
    }
    @Override public void onStop(@NonNull LifecycleOwner owner) {
        started=false;syncing=false;session++;readGeneration++;main.removeCallbacksAndMessages(null);
        if(favorites!=null&&listener!=null)favorites.removeEventListener(listener);
        favorites=null;listener=null;card.finishLoading();
    }
    @Override public void onDestroy(@NonNull LifecycleOwner owner) {
        started=false;syncing=false;session++;readGeneration++;main.removeCallbacksAndMessages(null);card.release();
    }
}
