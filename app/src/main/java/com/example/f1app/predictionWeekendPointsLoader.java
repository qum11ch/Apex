package com.example.f1app;

import android.os.*;
import com.google.firebase.database.*;
import org.json.*;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.*;

public final class predictionWeekendPointsLoader {
    public interface Callback {void onLoaded(Data data);void onError(String message);}
    public static final class Row {
        public final String raceName;
        public final int round;
        public final boolean hasSprint;
        public final double raceA,sprintA,raceB,sprintB;
        Row(String name,int round,boolean sprint,double ra,double sa,double rb,double sb) {
            raceName=name;this.round=round;hasSprint=sprint;raceA=ra;sprintA=sa;raceB=rb;sprintB=sb;
            predictionWeekendRules.weekend(ra,sa,sprint);
            predictionWeekendRules.weekend(rb,sb,sprint);
        }
        public double totalA(){return predictionWeekendRules.weekend(raceA,sprintA,hasSprint);}
        public double totalB(){return predictionWeekendRules.weekend(raceB,sprintB,hasSprint);}
    }
    public static final class Data {
        public final List<Row> rows;
        public final int skipped,calendarMismatches;
        public Data(){this(Collections.emptyList(),0,0);}
        Data(List<Row> rows,int skipped,int mismatches) {
            this.rows=Collections.unmodifiableList(new ArrayList<>(rows));
            this.skipped=skipped;calendarMismatches=mismatches;
        }
        public double sumA(){double s=0;for(Row r:rows)s+=r.totalA();return s;}
        public double sumB(){double s=0;for(Row r:rows)s+=r.totalB();return s;}
    }
    private static final class Race {
        final int round;final String name;final Boolean sprint;
        Race(int r,String n,Boolean s){round=r;name=n;sprint=s;}
    }
    private static final class Session {
        final Map<String,JSONObject> entries=new LinkedHashMap<>();
    }
    private static final ExecutorService IO=Executors.newFixedThreadPool(2);
    private static final Handler MAIN=new Handler(Looper.getMainLooper());

    public void load(String season,int throughRound,boolean teams,String a,String b,Callback callback) {
        if(a!=null&&a.equals(b)){MAIN.post(()->callback.onError("Equal participants"));return;}
        start(season,throughRound,teams,a,b,callback);
    }
    public void loadSingle(String season,int throughRound,boolean team,String id,Callback callback) {
        start(season,throughRound,team,id,id,callback);
    }
    private void start(String season,int through,boolean teams,String a,String b,Callback callback) {
        if(season==null||!season.matches("\\d{4}")||through<0) {MAIN.post(()->callback.onError("Invalid period"));return;}
        if(through==0){MAIN.post(()->callback.onLoaded(new Data()));return;}
        if(a==null||a.isEmpty()||b==null||b.isEmpty()) {MAIN.post(()->callback.onError("Missing Jolpica IDs"));return;}
        FirebaseDatabase.getInstance().getReference("schedule").child("season").child(season)
                .addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(DataSnapshot snapshot) {
                try {
                    List<Race> calendar=new ArrayList<>();Set<Integer> seen=new HashSet<>();
                    for(DataSnapshot race:snapshot.getChildren()) {
                        Long r=race.child("round").getValue(Long.class);
                        if(r==null||r<1||r>through||race.getKey()==null)continue;
                        if(!seen.add(r.intValue()))throw new Exception("Duplicate calendar round "+r);
                        calendar.add(new Race(r.intValue(),race.getKey(),sprintFlag(race)));
                    }
                    // A number gap may be deliberate cancellation; retain a strict error rather than silently call it full.
                    for(int r=1;r<=through;r++)if(!seen.contains(r))throw new Exception("Calendar missing round "+r);
                    calendar.sort((x,y)->Integer.compare(x.round,y.round));
                    IO.execute(()->fetch(season,through,teams,a,b,calendar,callback));
                }catch(Exception e){callback.onError(e.toString());}
            }
            @Override public void onCancelled(DatabaseError e){callback.onError(e.getMessage());}
        });
    }
    private static Boolean sprintFlag(DataSnapshot race) {
        String raw=race.child("Sprint").child("sprintRaceDate").getValue(String.class);
        if(raw==null||raw.trim().isEmpty())return null;
        String value=raw.trim();
        if(value.equalsIgnoreCase("N/A")||value.equalsIgnoreCase("NA"))return false;
        try {LocalDate.parse(value);return true;}catch(Exception e){return null;}
    }
    private static void fetch(String season,int through,boolean teams,String a,String b,
                              List<Race> calendar,Callback callback) {
        try {
            String base="https://api.jolpi.ca/ergast/f1/"+season+"/";
            String filter=teams?"constructors/":"drivers/";
            Map<Integer,Session> ra=sessions(base+filter+a+"/results/","Results",season,through);
            Map<Integer,Session> rb=a.equals(b)?ra:sessions(base+filter+b+"/results/","Results",season,through);
            Map<Integer,Session> sa=sessions(base+filter+a+"/sprint/","SprintResults",season,through);
            Map<Integer,Session> sb=a.equals(b)?sa:sessions(base+filter+b+"/sprint/","SprintResults",season,through);
            // Small shared responses establish session existence even when both selected drivers did not compete.
            Map<Integer,Session> completedRaces=sessions(base+"results/1/","Results",season,through);
            Map<Integer,Session> completedSprints=sessions(base+"sprint/1/","SprintResults",season,through);
            List<Row> rows=new ArrayList<>();int skipped=0,mismatches=0;
            for(Race race:calendar) {
                boolean raceComplete=present(completedRaces,race.round);
                boolean sprintComplete=present(completedSprints,race.round);
                predictionWeekendRules.Decision decision=predictionWeekendRules.decide(raceComplete,race.sprint,sprintComplete);
                if(!decision.included){skipped++;continue;}
                if(decision.calendarMismatch)mismatches++;
                rows.add(new Row(race.name,race.round,decision.hasSprint,
                        points(ra.get(race.round)),decision.hasSprint?points(sa.get(race.round)):0,
                        points(rb.get(race.round)),decision.hasSprint?points(sb.get(race.round)):0));
            }
            Data data=new Data(rows,skipped,mismatches);
            MAIN.post(()->callback.onLoaded(data));
        }catch(Exception e){MAIN.post(()->callback.onError(e.toString()));}
    }
    private static boolean present(Map<Integer,Session> sessions,int round) {
        Session s=sessions.get(round);return s!=null&&!s.entries.isEmpty();
    }
    private static double points(Session session)throws Exception {
        double sum=0;
        if(session==null)return sum; // complete filtered response: no entry means no awarded points
        for(JSONObject e:session.entries.values()) {
            double p=e.getDouble("points");if(!Double.isFinite(p))throw new Exception("Invalid points");sum+=p;
        }
        return sum;
    }
    private static Map<Integer,Session> sessions(String url,String field,String season,int through)throws Exception {
        Map<Integer,Session> result=new TreeMap<>();int offset=0;
        while(true) {
            JSONObject mr=predictionPointsJson.object(url+"?format=json&limit=100&offset="+offset).getJSONObject("MRData");
            JSONArray races=mr.getJSONObject("RaceTable").getJSONArray("Races");
            for(int i=0;i<races.length();i++) {
                JSONObject race=races.getJSONObject(i);
                if(!season.equals(race.getString("season")))throw new Exception("Season mismatch");
                int r=race.getInt("round");if(r>through)continue;
                Session s=result.get(r);if(s==null){s=new Session();result.put(r,s);}
                JSONArray entries=race.getJSONArray(field);
                for(int j=0;j<entries.length();j++) {
                    JSONObject entry=entries.getJSONObject(j);
                    String id=entry.getJSONObject("Driver").getString("driverId");
                    JSONObject prior=s.entries.get(id);
                    if(prior!=null&&Double.compare(prior.getDouble("points"),entry.getDouble("points"))!=0)
                        throw new Exception("Points changed between pages; refresh");
                    s.entries.put(id,entry);
                }
            }
            int limit=mr.getInt("limit"),total=mr.getInt("total");
            if(limit<=0||(races.length()==0&&offset<total))throw new Exception("Incomplete results pagination");
            if(offset+limit>=total)break;offset+=limit;
        }
        return result;
    }
}
