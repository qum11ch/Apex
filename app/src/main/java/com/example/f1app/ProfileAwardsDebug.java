package com.example.f1app;

import android.app.Dialog;
import android.content.pm.ApplicationInfo;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Optional debug-only UI preview. Does not read/write Firebase or grant awards. */
public final class ProfileAwardsDebug {
    private ProfileAwardsDebug() {}
    public static void show(AppCompatActivity activity) {
        if((activity.getApplicationInfo().flags&ApplicationInfo.FLAG_DEBUGGABLE)==0||activity.isFinishing()||activity.isDestroyed())return;
        Dialog dialog=new Dialog(activity);
        ScrollView scroll=new ScrollView(activity);
        LinearLayout content=new LinearLayout(activity);content.setOrientation(LinearLayout.VERTICAL);
        int dp=Math.round(12*activity.getResources().getDisplayMetrics().density);
        content.setPadding(dp,dp,dp,dp);content.setBackgroundColor(ContextCompat.getColor(activity,R.color.colorPrimary));scroll.addView(content);
        List<ProfileAwardsCard> cards=new ArrayList<>();
        boolean ru="ru".equals(java.util.Locale.getDefault().getLanguage());
        TextView notice=new TextView(activity);notice.setText(ru?"Предпросмотр этапа 1. Данные демонстрационные, награды не выдаются.":"Stage 1 preview. Sample data only; no awards are granted.");
        notice.setTextColor(ContextCompat.getColor(activity,R.color.text_color_main));notice.setPadding(0,dp,0,dp);content.addView(notice);
        List<Map<String,Object>> samples=new ArrayList<>();
        for(String id:new String[]{"first_correct","correct_10","correct_50","correct_100","tenure_1","tenure_3","tenure_5","season_2026_participant"}) {
            Map<String,Object> item=new HashMap<>();item.put("id",id);samples.add(item);
        }
        Map<String,Object> driver=new HashMap<>();driver.put("id","fan_driver");driver.put("code","DRV");driver.put("name",ru?"Тестовый пилот":"Sample driver");driver.put("championshipsCount",7);samples.add(driver);
        String[] titles=ru?new String[]{"Загружено","Первичная загрузка","Пустой список","Ошибка без данных","Обновление с данными","Ошибка обновления"}:new String[]{"Loaded","First load","Empty","Error without data","Refreshing existing data","Refresh error"};
        for(int i=0;i<titles.length;i++) {
            TextView label=new TextView(activity);label.setText(titles[i]);label.setPadding(0,dp*2,0,dp);label.setTextColor(ContextCompat.getColor(activity,R.color.text_color_main));content.addView(label);
            ProfileAwardsCard card=new ProfileAwardsCard(activity);cards.add(card);content.addView(card,new LinearLayout.LayoutParams(-1,-2));
            card.setRefreshAction(()->card.showData(samples));
            switch(i) {
                case 0:card.showData(samples);break;
                case 1:card.showLoading();break;
                case 2:card.showData(Collections.emptyList());break;
                case 3:card.showError();break;
                case 4:card.showData(samples);card.showLoading();break;
                default:card.showData(samples);card.showError();
            }
        }
        dialog.setContentView(scroll);dialog.setOnDismissListener(d->{for(ProfileAwardsCard card:cards)card.release();});dialog.show();
        if(dialog.getWindow()!=null) {dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.MATCH_PARENT);}
    }
}
