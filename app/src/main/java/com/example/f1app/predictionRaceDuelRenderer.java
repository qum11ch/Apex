package com.example.f1app;

import android.content.Context;
import android.view.*;
import android.widget.*;
import java.util.*;

public final class predictionRaceDuelRenderer {
    private predictionRaceDuelRenderer(){}

    public static void render(ViewGroup container,String codeA,String nameA,String codeB,String nameB,List<raceDuelEvent> events) {
        Context c=container.getContext();View root=inflate(container);
        predictionRaceDuelSummary s=predictionRaceDuelSummary.calculate(events);
        names(root,codeA,nameA,codeB,nameB);
        text(root,R.id.duelScoreA,String.valueOf(s.winsA));text(root,R.id.duelScoreB,String.valueOf(s.winsB));
        text(root,R.id.duelScoreCaption,c.getString(R.string.pred_duel_wins));
        text(root,R.id.duelRecentTitle,c.getString(R.string.pred_duel_recent));
        text(root,R.id.duelResultsNote,c.getString(R.string.rd_race_note));
        LinearLayout metrics=root.findViewById(R.id.duelMetrics);
        row(metrics,"",codeA,codeB,true);
        row(metrics,c.getString(R.string.rd_average_finish),decimal(s.averageA()),decimal(s.averageB()),false);
        row(metrics,c.getString(R.string.rd_numeric_finishes),String.valueOf(s.numericA),String.valueOf(s.numericB),false);
        row(metrics,c.getString(R.string.pred_duel_retirements),String.valueOf(s.retirementsA),String.valueOf(s.retirementsB),false);
        LinearLayout recent=root.findViewById(R.id.duelRecentRows);
        row(recent,c.getString(R.string.pred_duel_race),codeA,codeB,true);
        int shown=0;
        for(int i=events.size()-1;i>=0&&shown<5;i--) {
            raceDuelEvent event=events.get(i);if(event==null)continue;
            row(recent,predictionRaceNameFormatter.getName(c,event.getRaceName()),
                    finish(c,event.getResultA()),finish(c,event.getResultB()),false);shown++;
        }
    }

    public static void renderQualifying(ViewGroup container,String codeA,String nameA,String codeB,String nameB,List<qualifyingDuelEvent> events) {
        Context c=container.getContext();View root=inflate(container);
        int winsA=0,winsB=0,n=0;long sumA=0,sumB=0;
        for(qualifyingDuelEvent event:events) {
            if(!both(event))continue;
            int a=event.getPositionA(),b=event.getPositionB();
            if(a<b)winsA++;else if(b<a)winsB++;sumA+=a;sumB+=b;n++;
        }
        names(root,codeA,nameA,codeB,nameB);
        text(root,R.id.duelScoreA,String.valueOf(winsA));text(root,R.id.duelScoreB,String.valueOf(winsB));
        text(root,R.id.duelScoreCaption,c.getString(R.string.pred_quali_duel_wins));
        text(root,R.id.duelRecentTitle,c.getString(R.string.pred_quali_duel_recent));
        text(root,R.id.duelResultsNote,c.getString(R.string.pred_quali_duel_note));
        LinearLayout metrics=root.findViewById(R.id.duelMetrics);
        row(metrics,"",codeA,codeB,true);
        row(metrics,c.getString(R.string.pred_quali_duel_average),n==0?"—":decimal((double)sumA/n),n==0?"—":decimal((double)sumB/n),false);
        LinearLayout recent=root.findViewById(R.id.duelRecentRows);
        row(recent,c.getString(R.string.pred_duel_race),codeA,codeB,true);
        int shown=0;
        for(int i=events.size()-1;i>=0&&shown<5;i--) {
            qualifyingDuelEvent event=events.get(i);if(!both(event))continue;
            row(recent,predictionRaceNameFormatter.getName(c,event.getRaceName()),
                    String.valueOf(event.getPositionA()),String.valueOf(event.getPositionB()),false);shown++;
        }
    }
    private static boolean both(qualifyingDuelEvent e){return e!=null&&e.getPositionA()!=null&&e.getPositionA()>0&&e.getPositionB()!=null&&e.getPositionB()>0;}
    private static View inflate(ViewGroup container) {
        container.removeAllViews();View root=LayoutInflater.from(container.getContext()).inflate(R.layout.view_prediction_duel_stats,container,false);container.addView(root);return root;
    }
    private static void names(View root,String a,String an,String b,String bn){text(root,R.id.duelCodeA,a);text(root,R.id.duelNameA,an);text(root,R.id.duelCodeB,b);text(root,R.id.duelNameB,bn);}
    private static String decimal(Double value){return value==null?"—":String.format(Locale.getDefault(),"%.1f",value);}
    private static String finish(Context c,String raw) {
        if(raw==null)return "—";String s=raw.trim();
        if("N/C".equals(s))return c.getString(R.string.n_c_text);
        if("CND".equals(s))return c.getString(R.string.cnd_text);
        if("NP".equals(s))return c.getString(R.string.dns_text);
        String[] p=s.split("-",2);if(p.length!=2)return "—";s=p[1].trim();
        switch(s) {
            case "R":return c.getString(R.string.ret_text);
            case "W":return c.getString(R.string.wd_text);
            case "D":return c.getString(R.string.dsq_text);
            case "NC":return c.getString(R.string.rd_not_classified);
            default:try{int value=Integer.parseInt(s);return value>0?String.valueOf(value):"—";}catch(Exception e){return "—";}
        }
    }
    private static void text(View root,int id,String value){((TextView)root.findViewById(id)).setText(value==null?"—":value);}
    private static void row(LinearLayout parent,String label,String a,String b,boolean header) {
        View view=LayoutInflater.from(parent.getContext()).inflate(R.layout.item_prediction_comparison_row,parent,false);
        text(view,R.id.comparisonLabel,label);text(view,R.id.comparisonValueA,a);text(view,R.id.comparisonValueB,b);
        if(header)for(int id:new int[]{R.id.comparisonLabel,R.id.comparisonValueA,R.id.comparisonValueB}) {
            TextView t=view.findViewById(id);t.setTextSize(12);t.setAlpha(0.65f);
        }
        parent.addView(view);
    }
}
