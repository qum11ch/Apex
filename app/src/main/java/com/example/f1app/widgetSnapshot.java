package com.example.f1app;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/** What the home-screen widgets draw: a small copy of the data the app already loads. */
final class widgetSnapshot {

    static final int VERSION = 1;

    static final class Session {
        String key = "";
        long start;
        long end;
        String icon = "";   // name of the drawable: ids change with every build
        String temp = "";
        String rain = "";
        boolean wet;
        boolean measured;

        boolean hasWeather() {
            return !temp.isEmpty();
        }

        int iconRes(Context context) {
            return icon.isEmpty() ? 0 : context.getResources()
                    .getIdentifier(icon, "drawable", context.getPackageName());
        }
    }

    static final class Standing {
        int position;
        String label = "";          // driver code or team name
        String points = "";
        String constructorId = "";
        String color = "";          // "#rrggbb" or empty
    }

    String season = "";
    String raceName = "";           // key under schedule/season/<season>; empty when no race is left
    String circuitId = "";
    long updatedAt;
    long weatherAt;
    long standingsAt;
    final List<Session> sessions = new ArrayList<>();
    final List<Standing> drivers = new ArrayList<>();
    final List<Standing> teams = new ArrayList<>();

    String toJson() {
        try {
            JSONObject o = new JSONObject();
            o.put("v", VERSION);
            o.put("season", season);
            o.put("raceName", raceName);
            o.put("circuitId", circuitId);
            o.put("updatedAt", updatedAt);
            o.put("weatherAt", weatherAt);
            o.put("standingsAt", standingsAt);

            JSONArray s = new JSONArray();
            for (Session x : sessions) {
                JSONObject j = new JSONObject();
                j.put("key", x.key);
                j.put("start", x.start);
                j.put("end", x.end);
                j.put("icon", x.icon);
                j.put("temp", x.temp);
                j.put("rain", x.rain);
                j.put("wet", x.wet);
                j.put("measured", x.measured);
                s.put(j);
            }
            o.put("sessions", s);
            o.put("drivers", standings(drivers));
            o.put("teams", standings(teams));
            return o.toString();
        } catch (JSONException error) {
            return "{}";
        }
    }

    private static JSONArray standings(List<Standing> list) throws JSONException {
        JSONArray a = new JSONArray();
        for (Standing x : list) {
            JSONObject j = new JSONObject();
            j.put("position", x.position);
            j.put("label", x.label);
            j.put("points", x.points);
            j.put("constructorId", x.constructorId);
            j.put("color", x.color);
            a.put(j);
        }
        return a;
    }

    /** Null when the text is damaged or written by another version. */
    static widgetSnapshot fromJson(String raw) {
        try {
            JSONObject o = new JSONObject(raw);

            if (o.optInt("v", 0) != VERSION) {
                return null;
            }

            widgetSnapshot r = new widgetSnapshot();
            r.season = o.optString("season", "");
            r.raceName = o.optString("raceName", "");
            r.circuitId = o.optString("circuitId", "");
            r.updatedAt = o.optLong("updatedAt", 0);
            r.weatherAt = o.optLong("weatherAt", 0);
            r.standingsAt = o.optLong("standingsAt", 0);

            JSONArray s = o.optJSONArray("sessions");
            for (int i = 0; s != null && i < s.length(); i++) {
                JSONObject j = s.getJSONObject(i);
                Session x = new Session();
                x.key = j.optString("key", "");
                x.start = j.optLong("start", 0);
                x.end = j.optLong("end", 0);
                x.icon = j.optString("icon", "");
                x.temp = j.optString("temp", "");
                x.rain = j.optString("rain", "");
                x.wet = j.optBoolean("wet", false);
                x.measured = j.optBoolean("measured", false);
                r.sessions.add(x);
            }

            read(o.optJSONArray("drivers"), r.drivers);
            read(o.optJSONArray("teams"), r.teams);
            return r;
        } catch (JSONException error) {
            return null;
        }
    }

    private static void read(JSONArray a, List<Standing> out) throws JSONException {
        for (int i = 0; a != null && i < a.length(); i++) {
            JSONObject j = a.getJSONObject(i);
            Standing x = new Standing();
            x.position = j.optInt("position", i + 1);
            x.label = j.optString("label", "");
            x.points = j.optString("points", "");
            x.constructorId = j.optString("constructorId", "");
            x.color = j.optString("color", "");
            out.add(x);
        }
    }
}