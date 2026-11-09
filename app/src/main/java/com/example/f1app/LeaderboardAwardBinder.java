package com.example.f1app;

import android.os.SystemClock;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Per-screen selection reads: coalesced by UID, up to three simultaneous requests. */
public final class LeaderboardAwardBinder implements DefaultLifecycleObserver {
    private static final int MAX_RUNNING=3;
    private static final long TTL_MS=45000L;
    private final AppCompatActivity activity;
    private final Map<View,Slot> slots=new HashMap<>();
    private final LinkedHashMap<String,Cached> cache=new LinkedHashMap<>();
    private final Map<String,Pending> pending=new HashMap<>();
    private final ArrayDeque<Pending> queue=new ArrayDeque<>();
    private boolean active,disposed,everStarted;
    private int epoch,running;
    private String actorUid;
    private View personalKey;
    private static final class Cached {
        final DisplayedAwardApi.Selection selection;final long expires;
        Cached(DisplayedAwardApi.Selection value){selection=value;expires=SystemClock.elapsedRealtime()+TTL_MS;}
    }
    private interface Callback{void accept(DisplayedAwardApi.Selection value,Exception error);}
    private static final class Pending {
        final String uid;final int epoch;final String actor;
        final List<Callback> callbacks=new ArrayList<>();
        Pending(String uid,int epoch,String actor){this.uid=uid;this.epoch=epoch;this.actor=actor;}
    }
    private static final class Slot {
        final LeaderboardAwardBinder owner;
        final AwardIconView icon;
        final LeaderboardAwardToken token=new LeaderboardAwardToken();
        String uid;
        Slot(LeaderboardAwardBinder owner,AwardIconView icon){this.owner=owner;this.icon=icon;}
    }
    public LeaderboardAwardBinder(AppCompatActivity activity){
        this.activity=activity;actorUid=FirebaseAuth.getInstance().getUid();activity.getLifecycle().addObserver(this);
    }
    private int dp(float value){return Math.round(value*activity.getResources().getDisplayMetrics().density);}
    private boolean alive(){return active&&!disposed&&!activity.isFinishing()&&!activity.isDestroyed();}
    public void bindRow(View root,String uid){
        TextView name=root.findViewById(R.id.lbName);if(name==null)return;bind(root,name,uid);
    }
    public void bindPersonal(TextView summary,String uid){personalKey=summary;bind(summary,summary,uid);}
    public void clearPersonal(){if(personalKey!=null)clear(personalKey);}
    private Slot slotFor(View key,TextView text){
        Slot cached=slots.get(key);if(cached!=null)return cached;
        if(text.getParent() instanceof ConstraintLayout&&((View)text.getParent()).getTag() instanceof Slot){
            Slot slot=(Slot)((View)text.getParent()).getTag();
            if(slot.owner==this){slots.put(key,slot);return slot;}
        }
        if(!(text.getParent() instanceof ViewGroup))return null;
        ViewGroup parent=(ViewGroup)text.getParent();int index=parent.indexOfChild(text);ViewGroup.LayoutParams old=text.getLayoutParams();parent.removeView(text);
        ConstraintLayout wrapper=new ConstraintLayout(activity);parent.addView(wrapper,index,old);
        if(text.getId()==View.NO_ID)text.setId(View.generateViewId());
        AwardIconView icon=new AwardIconView(activity);icon.setId(View.generateViewId());icon.setVisibility(View.GONE);
        ConstraintLayout.LayoutParams tp=new ConstraintLayout.LayoutParams(-2,-2);
        tp.startToStart=0;tp.endToStart=icon.getId();tp.topToTop=0;tp.bottomToBottom=0;tp.constrainedWidth=true;tp.horizontalChainStyle=ConstraintLayout.LayoutParams.CHAIN_PACKED;tp.horizontalBias=0f;
        wrapper.addView(text,tp);
        ConstraintLayout.LayoutParams ip=new ConstraintLayout.LayoutParams(dp(28),dp(28));ip.startToEnd=text.getId();ip.endToEnd=0;ip.topToTop=0;ip.bottomToBottom=0;ip.setMarginStart(dp(6));wrapper.addView(icon,ip);
        // AwardIconView.clear() may reset its click listener during image rebinding.
        // A touch bridge keeps tapping the badge routed to the existing row action.
        icon.setOnTouchListener((v,event)->{
            if(event.getActionMasked()==android.view.MotionEvent.ACTION_UP){v.performClick();key.performClick();}
            return true;
        });
        Slot slot=new Slot(this,icon);wrapper.setTag(slot);slots.put(key,slot);return slot;
    }
    private void bind(View key,TextView text,String uid){
        Slot slot=slotFor(key,text);if(slot==null)return;
        slot.uid=uid;int token=slot.token.bind(uid);slot.icon.clear();slot.icon.setVisibility(View.GONE);
        if(uid==null||uid.isEmpty()||!alive())return;
        read(uid,(value,error)->{
            if(!alive()||slots.get(key)!=slot||!slot.token.matches(uid,token))return;
            if(error!=null){android.util.Log.w("LeaderboardAwards","Selected award unavailable; uid="+uid,error);return;}
            if(value.id==null){slot.icon.clear();slot.icon.setVisibility(View.GONE);return;}
            AwardCatalogue.Entry entry=AwardCatalogue.find(value.id);if(entry==null)return;
            String caption=activity.getString(ProfileAwardPresentation.title(value.id));String name=ProfileAwardPresentation.string(value.award,"name");
            slot.icon.setVisibility(View.VISIBLE);
            CatalogueAwardIcons.bind(slot.icon,entry,value.award,caption+(name.isEmpty()?"":": "+name),()->alive()&&slots.get(key)==slot&&slot.token.matches(uid,token));
        });
    }
    private void read(String uid,Callback callback){
        Cached c=cache.get(uid);
        if(c!=null&&c.expires>SystemClock.elapsedRealtime()){callback.accept(c.selection,null);return;}
        Pending request=pending.get(uid);
        if(request==null){request=new Pending(uid,epoch,actorUid);pending.put(uid,request);queue.add(request);}
        request.callbacks.add(callback);pump();
    }
    private void pump(){
        while(alive()&&running<MAX_RUNNING&&!queue.isEmpty()){
            Pending request=queue.remove();
            if(request.epoch!=epoch||pending.get(request.uid)!=request)continue;
            running++;
            DisplayedAwardApi.get(request.uid,(value,error)->{
                running--;
                boolean current=alive()&&request.epoch==epoch&&Objects.equals(request.actor,FirebaseAuth.getInstance().getUid())&&pending.get(request.uid)==request;
                if(pending.get(request.uid)==request)pending.remove(request.uid);
                if(current){
                    if(error==null){
                        if(cache.size()>=200)cache.remove(cache.keySet().iterator().next());
                        cache.put(request.uid,new Cached(value));
                    }
                    for(Callback callback:new ArrayList<>(request.callbacks))callback.accept(value,error);
                }
                request.callbacks.clear();pump();
            });
        }
    }
    public void clear(View root){
        Slot slot=slots.remove(root);if(slot==null)return;
        slot.token.clear();slot.uid=null;slot.icon.clear();slot.icon.setVisibility(View.GONE);
    }
    /** Called when the leaderboard explicitly reloads; ranking requests remain untouched. */
    public void refresh(){
        epoch++;cache.clear();pending.clear();queue.clear();
        for(Slot slot:slots.values()){slot.token.clear();slot.icon.clear();slot.icon.setVisibility(View.GONE);}
    }
    @Override public void onStart(@NonNull LifecycleOwner owner){
        if(disposed)return;active=true;
        String current=FirebaseAuth.getInstance().getUid();
        if(!Objects.equals(actorUid,current)){actorUid=current;cache.clear();}
        // The owner's choice may have changed while settings were open.
        if(everStarted&&current!=null)cache.remove(current);
        everStarted=true;
        for(Map.Entry<View,Slot> e:new ArrayList<>(slots.entrySet())){
            View key=e.getKey();Slot slot=e.getValue();String uid=slot.uid;
            TextView text=key instanceof TextView?(TextView)key:key.findViewById(R.id.lbName);
            if(text!=null&&uid!=null)bind(key,text,uid);
        }
        pump();
    }
    @Override public void onStop(@NonNull LifecycleOwner owner){active=false;epoch++;pending.clear();queue.clear();}
    @Override public void onDestroy(@NonNull LifecycleOwner owner){destroy();}
    public void destroy(){
        if(disposed)return;disposed=true;active=false;epoch++;pending.clear();queue.clear();cache.clear();
        for(View key:new ArrayList<>(slots.keySet()))clear(key);
        personalKey=null;
    }
}
