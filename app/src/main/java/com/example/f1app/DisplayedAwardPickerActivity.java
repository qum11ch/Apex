package com.example.f1app;

import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import com.google.firebase.auth.FirebaseAuth;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public final class DisplayedAwardPickerActivity extends AppCompatActivity {
    private LinearLayout rows;
    private TextView state,save,retry;
    private final Map<String,Map<String,Object>> available=new LinkedHashMap<>();
    private String uid,candidate,saved;
    private int generation,renderGeneration;
    private boolean started,awardsLoaded,selectionLoaded,saving,preselectionApplied,restoredCandidate;
    @Override protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);EdgeToEdge.enable(this);
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setBackgroundColor(color(R.color.colorPrimary));
        LinearLayout header=new LinearLayout(this);header.setOrientation(LinearLayout.HORIZONTAL);header.setGravity(Gravity.CENTER_VERTICAL);header.setPadding(dp(12),dp(12),dp(12),dp(12));header.setBackgroundColor(color(R.color.account_settings_header_color));
        android.widget.ImageButton back=new android.widget.ImageButton(this);back.setImageResource(R.drawable.back_arrow);back.setBackgroundColor(android.graphics.Color.TRANSPARENT);back.setPadding(dp(12),dp(12),dp(12),dp(12));back.setContentDescription(getString(R.string.back_arrow));back.setOnClickListener(v->finish());header.addView(back,new LinearLayout.LayoutParams(dp(48),dp(48)));
        TextView heading=text(getString(R.string.displayed_award_choose),18,true);heading.setTextColor(color(R.color.white));header.addView(heading,new LinearLayout.LayoutParams(0,-2,1));root.addView(header);
        TextView hint=text(getString(R.string.displayed_award_hint),13,false);hint.setPadding(dp(20),dp(16),dp(20),dp(8));hint.setTextColor(color(R.color.grey));root.addView(hint);
        state=text("",13,false);state.setPadding(dp(20),dp(8),dp(20),dp(8));root.addView(state);
        retry=text(getString(R.string.displayed_award_retry),14,false);retry.setGravity(Gravity.CENTER);retry.setMinHeight(dp(48));retry.setOnClickListener(v->load());root.addView(retry);
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(false);rows=new LinearLayout(this);rows.setOrientation(LinearLayout.VERTICAL);rows.setPadding(dp(16),0,dp(16),dp(12));scroll.addView(rows);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        save=text(getString(R.string.displayed_award_save),15,true);save.setGravity(Gravity.CENTER);save.setMinHeight(dp(52));save.setPadding(dp(16),dp(12),dp(16),dp(12));save.setBackground(surface(R.color.account_settings_input_color,16));save.setOnClickListener(v->saveChoice());LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(-1,-2);sp.setMargins(dp(16),dp(8),dp(16),dp(12));root.addView(save,sp);
        setContentView(root);WindowCompat.getInsetsController(getWindow(),getWindow().getDecorView()).setAppearanceLightStatusBars(false);
        ViewCompat.setOnApplyWindowInsetsListener(root,(v,insets)->{Insets safe=insets.getInsets(WindowInsetsCompat.Type.systemBars()|WindowInsetsCompat.Type.displayCutout());v.setPadding(safe.left,0,safe.right,safe.bottom);header.setPadding(dp(12),dp(12)+safe.top,dp(12),dp(12));return insets;});ViewCompat.requestApplyInsets(root);
        if(bundle!=null){candidate=bundle.getString("candidate");uid=bundle.getString("ownerUid");preselectionApplied=bundle.getBoolean("preselectionApplied");restoredCandidate=true;}
    }
    @Override protected void onStart(){super.onStart();started=true;String current=FirebaseAuth.getInstance().getUid();if(!Objects.equals(current,uid)){candidate=null;restoredCandidate=false;preselectionApplied=false;}uid=current;if(uid==null){finish();return;}load();}
    @Override protected void onStop(){started=false;generation++;saving=false;super.onStop();}
    @Override protected void onDestroy(){clearRows();super.onDestroy();}
    @Override protected void onSaveInstanceState(Bundle b){b.putString("candidate",candidate);b.putString("ownerUid",uid);b.putBoolean("preselectionApplied",preselectionApplied);super.onSaveInstanceState(b);}
    private boolean active(int request){return started&&request==generation&&!isFinishing()&&!isDestroyed()&&Objects.equals(uid,FirebaseAuth.getInstance().getUid());}
    private void load(){
        if(saving||uid==null)return;int request=++generation;awardsLoaded=false;selectionLoaded=false;state.setText(R.string.displayed_award_loading);retry.setVisibility(View.GONE);clearRows();save.setEnabled(false);save.setAlpha(.4f);
        AwardsRepository.invalidate(uid);
        AwardsRepository.load(uid,(badges,error)->{
            if(!active(request))return;
            if(error!=null||badges==null){failedLoad(error);return;}
            available.clear();for(Map<String,Object> b:ProfileAwardPresentation.normalize(badges))available.put(ProfileAwardPresentation.string(b,"id"),b);
            awardsLoaded=true;ready();
        });
        DisplayedAwardApi.get(uid,(value,error)->{
            if(!active(request))return;
            if(error!=null){failedLoad(error);return;}
            saved=value.id;if(!restoredCandidate)candidate=saved;selectionLoaded=true;ready();
        });
    }
    private void failedLoad(Exception error){state.setText(R.string.displayed_award_list_error);retry.setVisibility(View.VISIBLE);save.setEnabled(false);android.util.Log.w("DisplayedAward","Picker loading failed",error);}
    private void ready(){
        if(!awardsLoaded||!selectionLoaded)return;
        if(!preselectionApplied){
            String requested=getIntent().getStringExtra("preselectAwardId");
            if(requested!=null&&available.containsKey(requested))candidate=requested;
            preselectionApplied=true;
        }
        boolean unavailable=candidate!=null&&!available.containsKey(candidate);
        if(unavailable)candidate=null;
        restoredCandidate=false;
        state.setText(unavailable?getString(R.string.displayed_award_unavailable):"");retry.setVisibility(View.GONE);render();
    }
    private void clearRows(){renderGeneration++;clearIcons(rows);rows.removeAllViews();}
    private void clearIcons(View view){if(view instanceof AwardIconView)((AwardIconView)view).clear();else if(view instanceof ViewGroup){ViewGroup group=(ViewGroup)view;for(int i=0;i<group.getChildCount();i++)clearIcons(group.getChildAt(i));}}
    private void render(){
        clearRows();int request=renderGeneration;addRow(null,null,request);
        for(Map.Entry<String,Map<String,Object>> e:available.entrySet())addRow(e.getKey(),e.getValue(),request);
        save.setText(saving?R.string.displayed_award_saving:R.string.displayed_award_save);save.setEnabled(!saving&&awardsLoaded&&selectionLoaded);save.setAlpha(save.isEnabled()?1f:.4f);
    }
    private void addRow(String id,Map<String,Object> badge,int request){
        boolean selected=Objects.equals(id,candidate);LinearLayout row=new LinearLayout(this);row.setOrientation(LinearLayout.HORIZONTAL);row.setGravity(Gravity.CENTER_VERTICAL);row.setPadding(dp(12),dp(10),dp(12),dp(10));row.setBackground(surface(selected?R.color.account_settings_input_color:R.color.colorPrimary,16));row.setMinimumHeight(dp(84));
        if(id!=null){AwardIconView icon=new AwardIconView(this);row.addView(icon,new LinearLayout.LayoutParams(dp(64),dp(64)));CatalogueAwardIcons.bind(icon,AwardCatalogue.find(id),badge,getString(ProfileAwardPresentation.title(id)),()->request==renderGeneration);}
        else {TextView dash=text("—",24,false);dash.setGravity(Gravity.CENTER);row.addView(dash,new LinearLayout.LayoutParams(dp(64),dp(64)));}
        String caption=id==null?getString(R.string.displayed_award_none):getString(ProfileAwardPresentation.title(id));TextView title=text(caption,14,false);LinearLayout.LayoutParams tp=new LinearLayout.LayoutParams(0,-2,1);tp.setMarginStart(dp(12));row.addView(title,tp);
        TextView mark=text(selected?getString(R.string.displayed_award_selected):"",11,false);mark.setTextColor(color(R.color.neuro_accent));row.addView(mark,new LinearLayout.LayoutParams(-2,-2));
        row.setSelected(selected);row.setContentDescription(caption+(selected?", "+getString(R.string.displayed_award_selected):""));row.setOnClickListener(v->{if(!saving){candidate=id;render();}});LinearLayout.LayoutParams rp=new LinearLayout.LayoutParams(-1,-2);rp.bottomMargin=dp(6);rows.addView(row,rp);
    }
    private void saveChoice(){
        if(saving||!awardsLoaded||!selectionLoaded)return;saving=true;int request=generation;String chosen=candidate;render();state.setText(R.string.displayed_award_saving);
        DisplayedAwardApi.set(chosen,(value,error)->{
            if(!active(request)){if(started&&!isDestroyed()&&Objects.equals(uid,FirebaseAuth.getInstance().getUid()))load();return;}saving=false;
            if(error!=null){state.setText(R.string.displayed_award_save_error);render();return;}
            saved=value.id;Toast.makeText(this,R.string.displayed_award_saved,Toast.LENGTH_SHORT).show();setResult(RESULT_OK);finish();
        });
    }
    private int color(int id){return ContextCompat.getColor(this,id);}
    private int dp(float value){return Math.round(value*getResources().getDisplayMetrics().density);}
    private GradientDrawable surface(int id,float radius){GradientDrawable d=new GradientDrawable();d.setColor(color(id));d.setCornerRadius(dp(radius));return d;}
    private TextView text(String value,int size,boolean heading){TextView t=new TextView(this);t.setTextAppearance(this,heading?R.style.TextStyle_GoodTiming_Regular_ForHeader:R.style.TextStyle_Gram_Regular_Simple);t.setText(value);t.setTextSize(size);t.setTextColor(color(R.color.text_color_main));return t;}
}
