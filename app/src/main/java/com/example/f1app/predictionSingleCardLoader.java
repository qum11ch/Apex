package com.example.f1app;

import com.google.firebase.database.*;
import java.util.*;

public final class predictionSingleCardLoader {
    public interface Callback {void onLoaded(Data data);void onError();}
    public static final class Row {
        public final String raceName;
        public final List<String> results;
        Row(String name,List<String> raw){raceName=name;results=Collections.unmodifiableList(new ArrayList<>(raw));}
    }
    public static final class Data {
        public String title,season,targetRace,targetDate;
        public boolean team,historyLoading,historyError;
        public double points;
        public int wins,podiums,retirements,throughRound;
        public final List<Row> rows=new ArrayList<>();
        public predictionWeekendPointsLoader.Data history;
    }
    private final DatabaseReference root=FirebaseDatabase.getInstance().getReference();
    private final jolpicaPointsLoader standings=new jolpicaPointsLoader();
    private final predictionWeekendPointsLoader weekends=new predictionWeekendPointsLoader();

    public void load(String season,Integer beforeRound,boolean team,String id,String title,Callback callback) {
        if(beforeRound!=null&&beforeRound<1){callback.onError();return;}
        standings.loadSingle(team,season,beforeRound==null?null:beforeRound-1,id,title,
                new jolpicaPointsLoader.Callback() {
            @Override public void onError(String message) {callback.onError();}
            @Override public void onLoaded(jolpicaPointsLoader.Result total) {
                Data data=new Data();data.title=title;data.season=season;data.team=team;
                data.points=total.pointsA;data.throughRound=total.round;
                root.child("schedule").child("season").child(season)
                        .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override public void onDataChange(DataSnapshot calendar) {
                        List<DataSnapshot> races=new ArrayList<>();
                        for(DataSnapshot race:calendar.getChildren()) {
                            Long round=race.child("round").getValue(Long.class);
                            if(round==null||race.getKey()==null)continue;
                            if(beforeRound!=null&&round.longValue()==beforeRound.longValue()) {
                                data.targetRace=race.getKey();data.targetDate=race.child("raceDate").getValue(String.class);
                            }
                            if(round>0&&round<=total.round)races.add(race);
                        }
                        races.sort((a,b)->Long.compare(a.child("round").getValue(Long.class),b.child("round").getValue(Long.class)));
                        if(beforeRound!=null&&data.targetRace==null){callback.onError();return;}
                        root.child("results").child("season").child(season)
                                .addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override public void onDataChange(DataSnapshot results) {
                                for(DataSnapshot race:races) {
                                    String name=race.getKey();List<String> finishes=new ArrayList<>();
                                    for(DataSnapshot driver:results.getChildren()) {
                                        if(!team&&!title.equals(driver.getKey()))continue;
                                        DataSnapshot record=driver.child(name);
                                        if(team&&!id.equals(record.child("TeamId").getValue(String.class)))continue;
                                        String raw=record.child("Result").getValue(String.class);
                                        if(raw==null)continue;String[] p=raw.trim().split("-",2);if(p.length!=2)continue;
                                        String finish=p[1].trim();Integer position=position(finish);
                                        if(position==null&&!Arrays.asList("R","W","D","NC").contains(finish))continue;
                                        finishes.add(finish);
                                        if(position!=null&&position==1)data.wins++;
                                        if(position!=null&&position<=3)data.podiums++;
                                        if("R".equals(finish))data.retirements++;
                                    }
                                    if(!finishes.isEmpty())data.rows.add(new Row(name,finishes));
                                }
                                data.historyLoading=total.round>0;
                                if(total.round==0)data.history=new predictionWeekendPointsLoader.Data();
                                callback.onLoaded(data);
                                if(total.round==0)return;
                                weekends.loadSingle(season,total.round,team,total.idA,new predictionWeekendPointsLoader.Callback() {
                                    @Override public void onLoaded(predictionWeekendPointsLoader.Data h) {
                                        data.historyLoading=false;data.history=h;callback.onLoaded(data);
                                    }
                                    @Override public void onError(String message) {
                                        data.historyLoading=false;data.historyError=true;callback.onLoaded(data);
                                    }
                                });
                            }
                            @Override public void onCancelled(DatabaseError e){callback.onError();}
                        });
                    }
                    @Override public void onCancelled(DatabaseError e){callback.onError();}
                });
            }
        });
    }
    private static Integer position(String s){try{int p=Integer.parseInt(s);return p>0?p:null;}catch(Exception e){return null;}}
}
