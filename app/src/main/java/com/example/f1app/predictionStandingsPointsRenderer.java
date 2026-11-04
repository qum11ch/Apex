package com.example.f1app;

import android.content.Context;
import android.view.*;
import android.widget.*;
import java.text.NumberFormat;
import java.util.Locale;

public final class predictionStandingsPointsRenderer {
    private predictionStandingsPointsRenderer(){}
    public static void render(ViewGroup container,boolean teams,String season,String a,String an,String b,String bn,jolpicaPointsLoader.Result total) {
        render(container,teams,season,a,an,b,bn,total,null,false);
    }
    public static void render(ViewGroup container,boolean teams,String season,String a,String an,String b,String bn,
                              jolpicaPointsLoader.Result total,predictionWeekendPointsLoader.Data history,boolean error) {
        Context c=container.getContext();container.removeAllViews();
        View root=LayoutInflater.from(c).inflate(R.layout.view_prediction_standings_points,container,false);container.addView(root);
        String ca=teams?c.getString(R.string.spw_a):a,cb=teams?c.getString(R.string.spw_b):b;
        set(root,R.id.spwContext,c.getString(R.string.spw_period,season,total.round));
        set(root,R.id.spwCodeA,ca);set(root,R.id.spwCodeB,cb);
        set(root,R.id.spwNameA,an);set(root,R.id.spwNameB,bn);
        set(root,R.id.spwPointsA,num(total.pointsA));set(root,R.id.spwPointsB,num(total.pointsB));
        set(root,R.id.spwSource,c.getString(teams?R.string.spw_teams_source:R.string.spw_drivers_source));
        set(root,R.id.spwSideA,ca);set(root,R.id.spwSideB,cb);
        if(history==null) {
            root.findViewById(R.id.spwHistory).setVisibility(View.GONE);
            set(root,R.id.spwHistoryState,c.getString(error?R.string.spw_history_error:R.string.spw_loading));return;
        }
        root.findViewById(R.id.spwHistoryState).setVisibility(View.GONE);
        root.findViewById(R.id.spwHistory).setVisibility(View.VISIBLE);
        int n=history.rows.size();
        set(root,R.id.spwAverageA,avg(history.sumA(),n));set(root,R.id.spwAverageB,avg(history.sumB(),n));
        set(root,R.id.spwCount,c.getString(R.string.pf_period_weekends,n));
        root.findViewById(R.id.spwEmpty).setVisibility(n==0?View.VISIBLE:View.GONE);
        String coverage="";
        if(history.skipped>0)coverage=c.getString(R.string.pf_skipped,history.skipped);
        if(history.calendarMismatches>0)coverage+=(coverage.isEmpty()?"":"\n")+c.getString(R.string.pf_calendar_mismatch,history.calendarMismatches);
        root.findViewById(R.id.spwSkipped).setVisibility(coverage.isEmpty()?View.GONE:View.VISIBLE);
        set(root,R.id.spwSkipped,coverage);
        root.findViewById(R.id.spwMismatch).setVisibility(Math.abs(history.sumA()-total.pointsA)>1e-6
                ||Math.abs(history.sumB()-total.pointsB)>1e-6?View.VISIBLE:View.GONE);
        LinearLayout rows=root.findViewById(R.id.spwRows);
        for(int i=n-1;i>=Math.max(0,n-5);i--) {
            predictionWeekendPointsLoader.Row r=history.rows.get(i);
            View row=LayoutInflater.from(c).inflate(R.layout.item_prediction_weekend_points,rows,false);
            set(row,R.id.spwRace,predictionRaceNameFormatter.getName(c,r.raceName));
            set(row,R.id.spwValueA,value(c,r.hasSprint,r.raceA,r.sprintA));
            set(row,R.id.spwValueB,value(c,r.hasSprint,r.raceB,r.sprintB));rows.addView(row);
        }
    }
    private static String value(Context c,boolean sprint,double race,double sp){return sprint?c.getString(R.string.spw_total_parts,num(race+sp),num(race),num(sp)):num(race);}
    private static String avg(double total,int n){Double v=predictionWeekendRules.average(total,n);return v==null?"—":String.format(Locale.getDefault(),"%.1f",v);}
    private static String num(double v){NumberFormat f=NumberFormat.getNumberInstance(Locale.getDefault());f.setGroupingUsed(false);f.setMaximumFractionDigits(3);return f.format(v);}
    private static void set(View root,int id,String value){((TextView)root.findViewById(id)).setText(value==null?"—":value);}
}
