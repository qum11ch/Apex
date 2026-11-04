package com.example.f1app;

import org.json.*;
import java.time.*;
import java.util.*;

public final class predictionRaceWindowResolver {
    private predictionRaceWindowResolver(){}
    public static final class Window {
        public final long startMs,endMs;
        public final boolean resolved;
        public final String source,reason;
        Window(long s,long e,boolean ok,String source,String reason) {
            startMs=s;endMs=e;resolved=ok;this.source=source;this.reason=reason;
        }
    }
    public static Window resolve(int session,Instant metaStart,Instant metaEnd)throws Exception {
        JSONArray control=get("race_control?session_key="+session);
        Long started=null;List<Long> finishFlags=new ArrayList<>();
        long earliest=metaStart.minusSeconds(300).toEpochMilli();
        for(int i=0;i<control.length();i++) {
            JSONObject item=control.optJSONObject(i);if(item==null)continue;
            try {
                long date=OffsetDateTime.parse(item.getString("date")).toInstant().toEpochMilli();
                if(date<earliest||date>Instant.now().toEpochMilli())continue;
                String m=normal(item.optString("message"));
                String flag=normal(item.optString("flag")),scope=normal(item.optString("scope"));
                if(item.optInt("lap_number",-1)==1&&(m.equals("SESSION STARTED")||m.equals("RACE STARTED")))
                    started=started==null?date:Math.min(started,date);
                if(scope.equals("TRACK")&&(flag.equals("CHEQUERED")||m.equals("CHEQUERED FLAG")))finishFlags.add(date);
            }catch(Exception ignored){}
        }
        String source="CONTROL_LAP1_AND_CHEQUERED";
        if(started==null) {
            source="LAP1_TIMING_AND_CHEQUERED";
            JSONArray laps=get("laps?session_key="+session+"&lap_number=1");
            for(int i=0;i<laps.length();i++) {
                JSONObject item=laps.optJSONObject(i);if(item==null||item.optInt("lap_number",-1)!=1)continue;
                try {
                    long date=OffsetDateTime.parse(item.getString("date_start")).toInstant().toEpochMilli();
                    if(date>=earliest&&date<=Instant.now().toEpochMilli())started=started==null?date:Math.min(started,date);
                }catch(Exception ignored){}
            }
        }
        Collections.sort(finishFlags);Long finished=null;
        if(started!=null)for(Long date:finishFlags)if(date>started){finished=date;break;}
        return choose(metaStart.toEpochMilli(),metaEnd.toEpochMilli(),started,finished,source);
    }
    public static Window choose(long metaStart,long metaEnd,Long raceStart,Long raceEnd,String source) {
        if(raceStart==null)return new Window(metaStart,metaEnd,false,"SESSION_METADATA","RACE_START_NOT_RESOLVED");
        if(raceEnd==null)return new Window(metaStart,metaEnd,false,"SESSION_METADATA","RACE_END_NOT_RESOLVED");
        if(raceEnd<=raceStart||raceStart<metaStart-300000L||raceEnd-raceStart>8*60*60*1000L)
            return new Window(metaStart,metaEnd,false,"SESSION_METADATA","RACE_WINDOW_INVALID");
        return new Window(raceStart,raceEnd,true,source,"TIMING_WINDOW_RESOLVED");
    }
    private static JSONArray get(String query)throws Exception {
        String url="https://api.openf1.org/v1/"+query;
        try{return new JSONArray(predictionOpenF1Http.get(url));}
        catch(Exception e){predictionOpenF1Http.invalidate(url);throw e;}
    }
    private static String normal(String s){return s.toUpperCase(Locale.ROOT).trim().replaceAll("\\s+"," ");}
}
