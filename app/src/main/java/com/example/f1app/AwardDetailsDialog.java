package com.example.f1app;

import android.app.Dialog;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.core.content.ContextCompat;
import java.text.DateFormat;
import java.util.Date;
import java.util.Map;

/** App-coloured detail card; does not require a Material bottom-sheet theme. */
final class AwardDetailsDialog extends Dialog {
    private final AwardIconView icon;
    private boolean closed;
    AwardDetailsDialog(Context context,AwardCatalogue.Entry entry,AwardsCollectionActivity.Snapshot data,String caption,String requirement,String status) {
        super(context);
        ScrollView scroll=new ScrollView(context);
        LinearLayout body=new LinearLayout(context);body.setOrientation(LinearLayout.VERTICAL);body.setPadding(dp(20),dp(20),dp(20),dp(12));
        GradientDrawable shape=new GradientDrawable();shape.setColor(color(R.color.account_settings_input_color));shape.setCornerRadius(dp(18));body.setBackground(shape);scroll.addView(body);
        icon=new AwardIconView(context);LinearLayout.LayoutParams iconParams=new LinearLayout.LayoutParams(dp(96),dp(96));iconParams.gravity=Gravity.CENTER_HORIZONTAL;body.addView(icon,iconParams);
        Map<String,Object> badge=data.owned.get(entry.id);
        CatalogueAwardIcons.bind(icon,entry,badge,caption,()->!closed&&isShowing());
        icon.setAlpha(data.known?(badge==null?.45f:1f):.7f);
        TextView title=label(caption,18,true);title.setGravity(Gravity.CENTER);title.setPadding(0,dp(8),0,dp(12));body.addView(title,new LinearLayout.LayoutParams(-1,-2));
        if(badge!=null) {
            String name=ProfileAwardPresentation.string(badge,"name");
            if(!name.isEmpty()) {TextView identity=label(name,14,false);identity.setGravity(Gravity.CENTER);identity.setPadding(0,0,0,dp(12));body.addView(identity);}
        }
        TextView statusLabel=label(context.getString(R.string.awards_details_status)+": "+status,13,false);statusLabel.setPadding(0,0,0,dp(16));body.addView(statusLabel);
        body.addView(label(context.getString(R.string.awards_details_condition),15,true));
        TextView condition=label(requirement,14,false);condition.setPadding(0,dp(8),0,dp(12));body.addView(condition,new LinearLayout.LayoutParams(-1,-2));
        Long current=AwardProgress.current(entry,data.correct,data.years);
        boolean measurable=entry.target>0&&(entry.category==AwardCatalogue.PREDICTIONS||entry.category==AwardCatalogue.TENURE);
        if(measurable) {
            if(current==null)body.addView(label(context.getString(R.string.awards_progress_unavailable),13,false));
            else {
                ProgressBar progress=new ProgressBar(context,null,android.R.attr.progressBarStyleHorizontal);progress.setMax(entry.target);progress.setProgress((int)Math.min(current,(long)entry.target));
                progress.setProgressTintList(ColorStateList.valueOf(color(R.color.neuro_accent)));progress.setProgressBackgroundTintList(ColorStateList.valueOf(color(R.color.stroke_option_color)));body.addView(progress,new LinearLayout.LayoutParams(-1,dp(6)));
                TextView progressText=label(context.getString(entry.category==AwardCatalogue.PREDICTIONS?R.string.awards_progress_correct:R.string.awards_progress_years,current,(long)entry.target),13,false);progressText.setPadding(0,dp(8),0,dp(12));body.addView(progressText);
            }
        }
        if(badge!=null&&badge.get("awardedAt") instanceof Number) {
            long stamp=((Number)badge.get("awardedAt")).longValue();
            if(stamp>=0) {TextView added=label(context.getString(R.string.awards_details_recorded,DateFormat.getDateInstance().format(new Date(stamp))),12,false);added.setTextColor(color(R.color.grey));added.setPadding(0,dp(10),0,dp(8));body.addView(added);}
        }
        if("fan_driver".equals(entry.id)&&badge!=null) {
            int titles=ProfileAwardPresentation.titles(badge.get("championshipsCount"));
            if(titles>0)body.addView(label(context.getString(R.string.drivers_championships)+": "+titles,13,false));
        }
        if (badge != null && context instanceof AwardsCollectionActivity) {
            TextView choose = label(context.getString(R.string.displayed_award_use), 14, false);
            choose.setGravity(Gravity.CENTER);
            choose.setMinHeight(dp(48));
            choose.setTextColor(color(R.color.neuro_accent));
            choose.setOnClickListener(v -> {
                dismiss();
                context.startActivity(new android.content.Intent(context, DisplayedAwardPickerActivity.class)
                        .putExtra("preselectAwardId", entry.id));
            });
            body.addView(choose, new LinearLayout.LayoutParams(-1, -2));
        }
        TextView done=label(context.getString(R.string.awards_details_done),14,false);done.setGravity(Gravity.CENTER);done.setMinHeight(dp(48));done.setPadding(0,dp(12),0,dp(8));done.setOnClickListener(v->dismiss());done.setFocusable(true);body.addView(done,new LinearLayout.LayoutParams(-1,-2));
        setContentView(scroll);setCanceledOnTouchOutside(true);setOnDismissListener(d->{closed=true;icon.clear();});
    }
    private int color(int id){return ContextCompat.getColor(getContext(),id);}
    private int dp(float n){return Math.round(n*getContext().getResources().getDisplayMetrics().density);}
    private TextView label(String value,int size,boolean heading) {
        TextView text=new TextView(getContext());text.setTextAppearance(getContext(),heading?R.style.TextStyle_GoodTiming_Regular_ForHeader:R.style.TextStyle_Gram_Regular_Simple);text.setText(value);text.setTextSize(size);text.setTextColor(color(R.color.text_color_main));return text;
    }
    @Override public void show() {
        super.show();
        if(getWindow()!=null) {
            getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            int width=getContext().getResources().getDisplayMetrics().widthPixels-dp(32);
            getWindow().setLayout(width,ViewGroup.LayoutParams.WRAP_CONTENT);
            getWindow().getDecorView().post(()->{
                if(!isShowing()||getWindow()==null)return;
                int maxHeight=(int)(getContext().getResources().getDisplayMetrics().heightPixels*.82f);
                if(getWindow().getDecorView().getHeight()>maxHeight)getWindow().setLayout(width,maxHeight);
            });
        }
    }
}
