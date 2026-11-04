package com.example.f1app;

import android.content.Context;
import android.view.*;
import android.widget.*;

public final class predictionBooleanHistoryRenderer {
    private predictionBooleanHistoryRenderer(){}

    public static void message(ViewGroup container,String description,int messageId) {
        View root=create(container,description);
        root.findViewById(R.id.bhStats).setVisibility(View.GONE);
        set(root,R.id.bhMessage,container.getContext().getString(messageId));
    }

    public static void render(ViewGroup container,String type,String description,predictionBooleanHistoryLoader.Data data) {
        Context c=container.getContext();
        View root=create(container,description);
        root.findViewById(R.id.bhMessage).setVisibility(View.GONE);

        boolean rain="rain".equals(type);
        int yes=0, no=0, unknown=0, vy=0, vno=0, vu=0;

        for (predictionBooleanHistoryLoader.Row row:data.rows) {
            predictionBooleanHistoryLoader.State s=rain?row.rain:row.sc;
            if (s == predictionBooleanHistoryLoader.State.YES) {
                yes++;
            }
            else if (s == predictionBooleanHistoryLoader.State.NO) {
                no++;
            }
            else unknown++;

            if (row.vsc==predictionBooleanHistoryLoader.State.YES)
            {
                vy++;
            }
            else if (row.vsc==predictionBooleanHistoryLoader.State.NO){
                vno++;
            }
            else vu++;
        }

        set(root,R.id.bhRatio,String.valueOf(yes));
        set(root,R.id.bhCaption,c.getString(rain?R.string.bh3_rain_caption:R.string.bh3_sc_caption));

        String coverage = c.getString(
                R.string.bh3_counts,
                no,
                unknown,
                data.rows.size(),
                data.failedRequests
        );

        if (!data.unavailableYears.isEmpty()) {
            coverage += "\n" + c.getString(
                    R.string.bh4_unavailable_years,
                    android.text.TextUtils.join(
                            ", ",
                            data.unavailableYears
                    )
            );
        }

        set(root, R.id.bhUnknown, coverage);

        root.findViewById(R.id.bhVirtual).setVisibility(rain?View.GONE:View.VISIBLE);

        set(root,R.id.bhVirtual,c.getString(R.string.bh3_vsc_counts,vy,vno,vu));
        set(root,R.id.bhRule,c.getString(rain?R.string.bh3_rain_rule:R.string.bh2_sc_rule));

        LinearLayout rows=root.findViewById(R.id.bhRows);

        for (predictionBooleanHistoryLoader.Row item:data.rows) {
            View row=LayoutInflater.from(c).inflate(R.layout.item_prediction_boolean_history,rows,false);
            set(row,R.id.bhRaceDate,item.circuit+" · "+item.date);
            String value;
            if (rain) {
                if (item.rainError!=null)
                {
                    value=c.getString(R.string.bh3_network_error);
                }
                else if (item.rain==predictionBooleanHistoryLoader.State.UNKNOWN){
                    value=rainUnknown(c,item.rainReason);
                }
                else {
                    value=state(c,item.rain);
                }
            } else {
                value=item.safetyError!=null?c.getString(R.string.bh3_network_error)
                        :c.getString(R.string.bh_sc_vsc_row,state(c,item.sc),state(c,item.vsc));
            }
            set(row,R.id.bhOutcome,value);rows.addView(row);
        }
        root.findViewById(R.id.bhEmpty).setVisibility(data.rows.isEmpty()?View.VISIBLE:View.GONE);
    }

    private static String rainUnknown(Context c,String reason) {
        if ("EDGE_GAP".equals(reason)||"MEASUREMENT_GAP".equals(reason)){
            return c.getString(R.string.bh3_coverage_gap);
        }
        if (reason!=null&&reason.startsWith("RACE_")) {
            return c.getString(R.string.bh3_window_unknown);
        }
        return c.getString(R.string.bh_no_data);
    }

    private static View create(ViewGroup container,String description) {
        container.removeAllViews();
        View root=LayoutInflater.from(container.getContext()).inflate(R.layout.view_prediction_boolean_history,container,false);
        container.addView(root);
        root.findViewById(R.id.bhHeading).setVisibility(View.GONE);
        //TextView d=root.findViewById(R.id.bhDescription);
        //if(description==null||description.trim().isEmpty())d.setVisibility(View.GONE);else d.setText(description.trim());
        return root;
    }

    private static String state(Context c,predictionBooleanHistoryLoader.State s){
        return c.getString(s==predictionBooleanHistoryLoader.State.YES?R.string.bh_yes:s==predictionBooleanHistoryLoader.State.NO?R.string.bh_no:R.string.bh_no_data);
    }

    private static void set(View root,int id,String value){
        ((TextView)root.findViewById(id)).setText(value);
    }
}
