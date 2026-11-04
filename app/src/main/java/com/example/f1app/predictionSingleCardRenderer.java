package com.example.f1app;

import android.content.Context;
import android.view.*;
import android.widget.*;
import java.text.NumberFormat;
import java.util.*;

public final class predictionSingleCardRenderer {
    private predictionSingleCardRenderer(){}
    public static void render(ViewGroup container,predictionSingleCardLoader.Data d) {
        Context c=container.getContext();container.removeAllViews();
        View root=LayoutInflater.from(c).inflate(R.layout.view_prediction_single_stats,container,false);container.addView(root);
        set(root,R.id.singleName,d.title);
        String period=c.getString(R.string.pf_period,d.season,d.throughRound);
        if(d.targetRace!=null)period=predictionRaceNameFormatter.getNameWithSeason(c,d.targetRace,d.season)
                +"\n"+c.getString(R.string.pf_before)+" · "+period;
        set(root,R.id.singleContext,period);
        set(root,R.id.singlePoints,num(d.points));
        set(root,R.id.singleWins,String.valueOf(d.wins));
        set(root,R.id.singlePodiums,String.valueOf(d.podiums));
        set(root,R.id.singleRetirements,String.valueOf(d.retirements));
        set(root,R.id.singleCount,c.getString(R.string.pf_races,d.rows.size()));
        LinearLayout weekendRows=root.findViewById(R.id.singleWeekendRows);
        if(d.history==null) {
            set(root,R.id.singleAverage,c.getString(d.historyError?R.string.pf_history_error:R.string.pf_loading));
            root.findViewById(R.id.singleWeekendCaption).setVisibility(View.GONE);
        } else {
            int n=d.history.rows.size();Double avg=predictionWeekendRules.average(d.history.sumA(),n);
            set(root,R.id.singleAverage,c.getString(R.string.pf_average,avg==null?"—":String.format(Locale.getDefault(),"%.1f",avg),n));
            for(int i=n-1;i>=Math.max(0,n-5);i--) {
                predictionWeekendPointsLoader.Row row=d.history.rows.get(i);
                String value=num(row.totalA());
                if(row.hasSprint)value=c.getString(R.string.pf_parts,value,num(row.raceA),num(row.sprintA));
                add(weekendRows,predictionRaceNameFormatter.getName(c,row.raceName),value);
            }
            if(n==0)add(weekendRows,c.getString(R.string.pf_empty),"—");
            if(d.history.skipped>0)set(root,R.id.singleCoverage,c.getString(R.string.pf_skipped,d.history.skipped));
            else root.findViewById(R.id.singleCoverage).setVisibility(View.GONE);
            if(d.history.calendarMismatches>0)set(root,R.id.singleCalendarWarning,c.getString(R.string.pf_calendar_mismatch,d.history.calendarMismatches));
            else root.findViewById(R.id.singleCalendarWarning).setVisibility(View.GONE);
            root.findViewById(R.id.singleTotalWarning).setVisibility(Math.abs(d.history.sumA()-d.points)>1e-6?View.VISIBLE:View.GONE);
        }
        if(d.history==null) {
            root.findViewById(R.id.singleCoverage).setVisibility(View.GONE);
            root.findViewById(R.id.singleCalendarWarning).setVisibility(View.GONE);
            root.findViewById(R.id.singleTotalWarning).setVisibility(View.GONE);
        }
        LinearLayout raceRows=root.findViewById(R.id.singleRecentRows);
        for(int i=d.rows.size()-1;i>=Math.max(0,d.rows.size()-5);i--) {
            predictionSingleCardLoader.Row row=d.rows.get(i);List<String> values=new ArrayList<>();
            for(String finish:row.results)values.add(finish(c,finish));
            add(raceRows,predictionRaceNameFormatter.getName(c,row.raceName),android.text.TextUtils.join(" / ",values));
        }
        if(d.rows.isEmpty())add(raceRows,c.getString(R.string.pf_empty),"—");
    }
    private static String finish(Context c,String s) {
        switch(s){case "R":return c.getString(R.string.ret_text);case "W":return c.getString(R.string.wd_text);
            case "D":return c.getString(R.string.dsq_text);case "NC":return c.getString(R.string.pf_nc);default:return s;}
    }
    private static String num(double v){NumberFormat f=NumberFormat.getNumberInstance(Locale.getDefault());f.setGroupingUsed(false);f.setMaximumFractionDigits(3);return f.format(v);}
    private static void set(View root,int id,String s){((TextView)root.findViewById(id)).setText(s==null?"—":s);}
    private static void add(LinearLayout parent,String label,String value) {
        View row=LayoutInflater.from(parent.getContext()).inflate(R.layout.item_prediction_single_result,parent,false);
        set(row,R.id.singleRaceName,label);set(row,R.id.singleRaceValue,value);parent.addView(row);
    }
}
