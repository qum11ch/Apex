package com.example.f1app;

import android.os.Handler;
import android.os.Looper;
import org.json.JSONArray;
import org.json.JSONObject;
import java.net.HttpURLConnection;
import java.net.URL;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.*;

public final class predictionBooleanHistoryLoader {
    public enum State { YES, NO, UNKNOWN }
    public interface Callback { void onLoaded(Data data); void onError(String message); }
    public static final class Row {
        public String date, circuit;
        public State rain = State.UNKNOWN, sc = State.UNKNOWN, vsc = State.UNKNOWN;
    }
    public static final class Data {
        public final List<Row> rows = new ArrayList<>();
    }
    private static final ExecutorService IO = Executors.newSingleThreadExecutor();
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static final String BASE = "https://api.openf1.org/v1/";
    private static final long GAP_MS = 5 * 60 * 1000L;

    public void load(String location, String circuitName, String circuitId,
                     String beforeDate, Callback callback) {
        IO.execute(() -> {
            try {
                Instant cutoff = java.time.LocalDate.parse(beforeDate)
                        .atStartOfDay(java.time.ZoneOffset.UTC).toInstant();
                Instant now = Instant.now();
                Instant end = cutoff.isBefore(now) ? cutoff : now;
                int lastYear = Math.min(end.atOffset(java.time.ZoneOffset.UTC).getYear(),
                        now.atOffset(java.time.ZoneOffset.UTC).getYear());
                List<JSONObject> candidates = new ArrayList<>();
                Set<Integer> keys = new HashSet<>();
                for (int year = 2023; year <= lastYear; year++) {
                    JSONArray sessions = get("sessions?year=" + year + "&session_name=Race");
                    for (int i = 0; i < sessions.length(); i++) {
                        JSONObject s = sessions.getJSONObject(i);
                        if (!"Race".equals(s.optString("session_name")) || s.optBoolean("is_cancelled", false)) continue;
                        if (!matches(s, location, circuitName, circuitId)) continue;
                        int key = s.getInt("circuit_key");
                        keys.add(key);
                        Instant finish = instant(s.getString("date_end"));
                        if (finish.isBefore(end)) candidates.add(s);
                    }
                }
                if (keys.size() != 1) throw new Exception("Circuit match is missing or ambiguous");
                candidates.sort((a,b) -> a.optString("date_start").compareTo(b.optString("date_start")));
                Data data = new Data();
                Set<Integer> seen = new HashSet<>();
                for (int i = candidates.size()-1; i >= 0 && data.rows.size() < 5; i--) {
                    JSONObject s = candidates.get(i);
                    int session = s.getInt("session_key");
                    if (!seen.add(session)) continue;
                    Instant start = instant(s.getString("date_start"));
                    Instant finish = instant(s.getString("date_end"));
                    if (!finish.isAfter(start)) continue;
                    Row row = new Row();
                    row.date = start.atOffset(java.time.ZoneOffset.UTC).toLocalDate().toString();
                    row.circuit = s.optString("circuit_short_name", location);
                    try { row.rain = rain(get("weather?session_key="+session), start, finish); }
                    catch (Exception error) { row.rain = State.UNKNOWN; }
                    try { safety(get("race_control?session_key="+session), start, finish, row); }
                    catch (Exception error) { row.sc = State.UNKNOWN; row.vsc = State.UNKNOWN; }
                    data.rows.add(row);
                }
                MAIN.post(() -> callback.onLoaded(data));
            } catch (Exception error) {
                MAIN.post(() -> callback.onError(error.getMessage()));
            }
        });
    }

    private static boolean matches(JSONObject session, String... localNames) {
        String location = normal(session.optString("location"));
        String circuit = normal(session.optString("circuit_short_name"));
        for (String name : localNames) {
            String n = normal(name);
            if (!n.isEmpty() && (n.equals(location) || n.equals(circuit))) return true;
        }
        return false;
    }
    private static String normal(String value) {
        if (value == null) return "";
        return Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }
    private static Instant instant(String value) { return OffsetDateTime.parse(value).toInstant(); }

    private static State rain(JSONArray data, Instant start, Instant end) {
        List<Long> dry = new ArrayList<>();
        for (int i=0;i<data.length();i++) {
            JSONObject item = data.optJSONObject(i);
            if (item == null || item.isNull("date") || item.isNull("rainfall")) continue;
            try {
                Instant date = instant(item.getString("date"));
                if (date.isBefore(start) || date.isAfter(end)) continue;
                Object flag = item.get("rainfall");
                if (!(flag instanceof Number)) continue;
                double value = ((Number)flag).doubleValue();
                if (value == 1) return State.YES;
                if (value == 0) dry.add(date.toEpochMilli());
            } catch (Exception ignored) { }
        }
        if (dry.size()<2) return State.UNKNOWN;
        Collections.sort(dry);
        if (dry.get(0)-start.toEpochMilli()>GAP_MS
                || end.toEpochMilli()-dry.get(dry.size()-1)>GAP_MS) return State.UNKNOWN;
        for (int i=1;i<dry.size();i++) if (dry.get(i)-dry.get(i-1)>GAP_MS) return State.UNKNOWN;
        return State.NO;
    }

    private static void safety(JSONArray data, Instant start, Instant end, Row row) {
        boolean started=false, ended=false, ambiguous=false;
        for (int i=0;i<data.length();i++) {
            JSONObject item=data.optJSONObject(i);
            if(item==null)continue;
            try {
                Instant date=instant(item.getString("date"));
                if(date.isBefore(start)||date.isAfter(end))continue;
                String message=item.optString("message").toUpperCase(Locale.ROOT)
                        .trim().replaceAll("\\s+"," ");
                String category=item.optString("category");
                if(message.equals("SESSION STARTED"))started=true;
                if(message.equals("SESSION FINISHED")||message.equals("SESSION ENDED")
                        ||"CHEQUERED".equals(item.optString("flag")))ended=true;
                boolean virtual=message.contains("VIRTUAL SAFETY CAR");
                boolean vEvidence=virtual&&(message.contains("DEPLOYED")||message.contains("ENDING"));
                boolean scEvidence=!virtual&&(message.equals("SAFETY CAR DEPLOYED")
                        ||message.equals("SAFETY CAR IN THIS LAP"));
                if(vEvidence)row.vsc=State.YES;
                if(scEvidence)row.sc=State.YES;
                if("SafetyCar".equalsIgnoreCase(category) && !vEvidence && !scEvidence) ambiguous=true;
            }catch(Exception ignored){ }
        }
        // NO means no recognised deployment in a bounded log, not proof from an empty response.
        if(started&&ended&&!ambiguous) {
            if(row.sc!=State.YES)row.sc=State.NO;
            if(row.vsc!=State.YES)row.vsc=State.NO;
        }
    }

    private static JSONArray get(String query) throws Exception {
        for(int attempt=0;attempt<3;attempt++) {
            HttpURLConnection c=(HttpURLConnection)new URL(BASE+query).openConnection();
            c.setConnectTimeout(15000); c.setReadTimeout(15000);
            c.setRequestProperty("Accept","application/json");
            try {
                int status=c.getResponseCode();
                if(status==429&&attempt<2){Thread.sleep(2000L*(attempt+1));continue;}
                if(status!=200)throw new Exception("OpenF1 HTTP "+status);
                try(InputStream in=c.getInputStream();ByteArrayOutputStream out=new ByteArrayOutputStream()) {
                    byte[] buffer=new byte[8192];int n;
                    while((n=in.read(buffer))!=-1)out.write(buffer,0,n);
                    JSONArray data=new JSONArray(new String(out.toByteArray(),StandardCharsets.UTF_8));
                    Thread.sleep(1000);
                    return data;
                }
            }finally{c.disconnect();}
        }
        throw new Exception("OpenF1 rate limit");
    }
}
