package com.example.f1app;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

public final class predictionBooleanHistoryRenderer {
    private predictionBooleanHistoryRenderer() {}

    public static void message(ViewGroup container, String description, int messageId) {
        Context c=container.getContext();
        View root=create(container,description);
        root.findViewById(R.id.bhStats).setVisibility(View.GONE);
        set(root,R.id.bhMessage,c.getString(messageId));
    }

    public static void render(ViewGroup container, String type, String description,
                              predictionBooleanHistoryLoader.Data data) {
        Context c=container.getContext();
        View root=create(container,description);
        root.findViewById(R.id.bhMessage).setVisibility(View.GONE);
        boolean rain="rain".equals(type);
        set(root,R.id.bhHeading,c.getString(rain?R.string.bh_rain_title:R.string.bh_sc_title));
        int yes=0,known=0,unknown=0,virtualYes=0,virtualKnown=0;
        for(predictionBooleanHistoryLoader.Row row:data.rows) {
            predictionBooleanHistoryLoader.State value=rain?row.rain:row.sc;
            if(value==predictionBooleanHistoryLoader.State.UNKNOWN)unknown++;
            else {known++;if(value==predictionBooleanHistoryLoader.State.YES)yes++;}
            if(row.vsc!=predictionBooleanHistoryLoader.State.UNKNOWN) {
                virtualKnown++;if(row.vsc==predictionBooleanHistoryLoader.State.YES)virtualYes++;
            }
        }
        set(root,R.id.bhRatio,known==0?"—":c.getString(R.string.bh_ratio,yes,known));
        set(root,R.id.bhCaption,c.getString(rain?R.string.bh_rain_caption:R.string.bh_sc_caption));
        set(root,R.id.bhUnknown,c.getString(R.string.bh_unknown,unknown,data.rows.size()));
        TextView virtual=root.findViewById(R.id.bhVirtual);
        virtual.setVisibility(rain?View.GONE:View.VISIBLE);
        virtual.setText(c.getString(R.string.bh_vsc_count,virtualYes,virtualKnown));
        set(root,R.id.bhRule,c.getString(rain?R.string.bh_rain_rule:R.string.bh_sc_rule));
        LinearLayout rows=root.findViewById(R.id.bhRows);
        for(predictionBooleanHistoryLoader.Row item:data.rows) {
            View view=LayoutInflater.from(c).inflate(R.layout.item_prediction_boolean_history,rows,false);
            set(view,R.id.bhRaceDate,item.circuit+" · "+item.date);
            set(view,R.id.bhOutcome,rain?state(c,item.rain)
                    :c.getString(R.string.bh_sc_vsc_row,state(c,item.sc),state(c,item.vsc)));
            rows.addView(view);
        }
        root.findViewById(R.id.bhEmpty).setVisibility(data.rows.isEmpty()?View.VISIBLE:View.GONE);
    }
    private static View create(ViewGroup container,String description) {
        container.removeAllViews();
        View root=LayoutInflater.from(container.getContext()).inflate(
                R.layout.view_prediction_boolean_history,container,false);
        container.addView(root);
        TextView d=root.findViewById(R.id.bhDescription);
        if(description==null||description.trim().isEmpty())d.setVisibility(View.GONE);
        else d.setText(description.trim());
        return root;
    }
    private static String state(Context c,predictionBooleanHistoryLoader.State s) {
        return c.getString(s==predictionBooleanHistoryLoader.State.YES?R.string.bh_yes:
                s==predictionBooleanHistoryLoader.State.NO?R.string.bh_no:R.string.bh_no_data);
    }
    private static void set(View root,int id,String text) {((TextView)root.findViewById(id)).setText(text);}
}
