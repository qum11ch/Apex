package com.example.f1app;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class jolpicaPointsLoader {
    public interface Callback {
        void onLoaded(Result result);
        void onError(String message);
    }

    public static final class Result {
        public final double pointsA,pointsB;
        public final int round;
        public final String idA,idB;

        Result(double a, double b, int round, String idA, String idB) {
            pointsA=a;
            pointsB=b;
            this.round=round;
            this.idA=idA;
            this.idB=idB;
        }
    }

    private static final class Table {
        int round=-1;
        final JSONArray entries=new JSONArray();
    }

    private static final ExecutorService IO=Executors.newFixedThreadPool(2);
    private static final Handler MAIN=new Handler(Looper.getMainLooper());

    public void load(boolean teams, String season, Integer afterRound, String idA, String titleA,
                     String idB, String titleB, Callback callback) {
        if (idA == null || idB == null || idA.equals(idB)) {
            MAIN.post(()->callback.onError("Invalid participants"));
            return;
        }
        request(teams, season, afterRound, idA, titleA, idB, titleB, callback);
    }

    public void loadSingle(boolean team, String season, Integer afterRound,
                           String id, String title, Callback callback) {
        if (id == null){
            MAIN.post(()->callback.onError("Invalid participant"));
            return;
        }
        request(team, season, afterRound, id, title, id, title, callback);
    }

    private void request(boolean teams, String season, Integer requested, String a, String an,
                         String b, String bn, Callback callback) {
        if (season == null || !season.matches("\\d{4}") || (requested != null && requested<0)) {
            MAIN.post(()->callback.onError("Invalid standings period"));
            return;
        }

        if (requested != null && requested == 0) {
            MAIN.post(()->callback.onLoaded(new Result(0,0,0,null,null)));
            return;
        }

        IO.execute(()->{
            try {
                Table latest = read(teams, season, null);
                int effective = predictionStandingsPeriod.resolve(requested, latest.round);
                Table table = effective == latest.round ? latest : read(teams,season,effective);
                if (table.round > effective) throw new Exception("Unsafe future standings response");
                Double pa = null,pb = null;String ia = null,ib = null;
                for (int i = 0; i < table.entries.length(); i++) {
                    JSONObject entry = table.entries.getJSONObject(i);
                    JSONObject entity = entry.getJSONObject(teams ? "Constructor":"Driver");
                    double points = entry.getDouble("points");

                    if (!Double.isFinite(points)) throw new Exception("Invalid points");

                    String id = entity.getString(teams?"constructorId":"driverId");

                    if(match(entity, teams, a, an)){
                        pa=points;ia=id;
                    }

                    if(match(entity, teams, b, bn)){
                        pb=points;
                        ib=id;
                    }
                }
                if (pa == null || pb == null)
                    throw new Exception("Participant not found: " + a + " / " + b
                        + "; season=" + season + "; round=" + table.round);

                Result result = new Result(pa, pb, table.round, ia, ib);
                Log.d("StandingsPoints","season=" + season + " requested=" + requested
                        + " latest=" + latest.round + " used=" + table.round + " teams=" + teams);
                MAIN.post(()->callback.onLoaded(result));
            }catch(Exception e) {
                Log.e("StandingsPoints","Failed: season=" + season + " requested=" + requested
                        + " participantA=" + a + " participantB=" + b, e);
                MAIN.post(()->callback.onError(e.toString()));
            }
        });
    }
    private static Table read(boolean teams, String season, Integer round)throws Exception {
        String base="https://api.jolpi.ca/ergast/f1/" + season + "/"
                + (round == null ? "" : round + "/") + (teams ? "constructorStandings/" : "driverStandings/");
        Table result = new Table();
        int offset = 0;
        while (true) {
            JSONObject mr = predictionPointsJson.object(base + "?format=json&limit=100&offset=" + offset).getJSONObject("MRData");
            JSONArray lists = mr.getJSONObject("StandingsTable").getJSONArray("StandingsLists");
            if (lists.length()!=1) throw new Exception("Standings unavailable: " + base);
            JSONObject table = lists.getJSONObject(0);
            if(!season.equals(table.getString("season")))throw new Exception("Season mismatch");
            int represented=table.getInt("round");
            if(represented<0||(round!=null&&represented>round))throw new Exception("Round mismatch");
            if(result.round!=-1&&result.round!=represented)throw new Exception("Standings changed between pages");
            result.round=represented;
            JSONArray entries=table.getJSONArray(teams?"ConstructorStandings":"DriverStandings");
            for(int i=0;i<entries.length();i++)result.entries.put(entries.getJSONObject(i));
            int limit=mr.getInt("limit"),total=mr.getInt("total");
            if(limit<=0||(entries.length()==0&&offset<total))throw new Exception("Incomplete standings");
            if(offset+limit>=total)break;
            offset+=limit;
        }
        return result;
    }
    private static boolean match(JSONObject e,boolean team,String id,String title) {
        if(team)return id.equals(e.optString("constructorId"));
        return id.equals(e.optString("driverId"))||id.equals(e.optString("code"))
                ||(title!=null&&title.equals((e.optString("givenName")+" "+e.optString("familyName")).trim()));
    }
}
