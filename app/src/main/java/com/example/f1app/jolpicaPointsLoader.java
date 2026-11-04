package com.example.f1app;

import android.os.Handler;
import android.os.Looper;
import org.json.JSONArray;
import org.json.JSONObject;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class jolpicaPointsLoader {
    public interface Callback {
        void onLoaded(Result result);
        void onError(String message);
    }

    public static final class Result {
        public final double pointsA, pointsB;
        public final int round;
        public final String idA, idB; // driverId или constructorId из Jolpica

        Result(double a, double b, int round, String idA, String idB) {
            this.pointsA = a;
            this.pointsB = b;
            this.round = round;
            this.idA = idA;
            this.idB = idB;
        }
    }

    private static final ExecutorService IO = Executors.newSingleThreadExecutor();
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    public void load(boolean teams, String season, Integer afterRound,
                     String idA, String titleA, String idB, String titleB, Callback callback) {
        if (season == null || !season.matches("\\d{4}") || idA == null || idB == null
                || idA.equals(idB) || (afterRound != null && afterRound < 0)) {
            MAIN.post(() -> callback.onError("Invalid standings request"));
            return;
        }
        if (afterRound != null && afterRound == 0) {
            MAIN.post(() -> callback.onLoaded(new Result(0, 0, 0, null, null)));
            return;
        }
        IO.execute(() -> {
            try {
                String endpoint = teams ? "constructorStandings" : "driverStandings";
                String address = "https://api.jolpi.ca/ergast/f1/" + season + "/"
                        + (afterRound == null ? "" : afterRound + "/") + endpoint + ".json";
                int offset = 0;
                Double a = null, b = null;
                String foundA = null, foundB = null;
                Integer representedRound = null;
                while (true) {
                    JSONObject mr = new JSONObject(get(address + "?limit=100&offset=" + offset))
                            .getJSONObject("MRData");
                    JSONArray lists = mr.getJSONObject("StandingsTable").getJSONArray("StandingsLists");
                    if (lists.length() != 1) throw new Exception("Standings are not available");
                    JSONObject standings = lists.getJSONObject(0);
                    if (!season.equals(standings.getString("season"))) throw new Exception("Season mismatch");
                    int round = Integer.parseInt(standings.getString("round"));
                    if (representedRound != null && representedRound != round)
                        throw new Exception("Standings changed between pages; retry");
                    representedRound = round;
                    if (afterRound != null && round != afterRound)
                        throw new Exception("Requested round is not available");
                    JSONArray entries = standings.getJSONArray(teams ? "ConstructorStandings" : "DriverStandings");
                    for (int i = 0; i < entries.length(); i++) {
                        JSONObject entry = entries.getJSONObject(i);
                        JSONObject entity = entry.getJSONObject(teams ? "Constructor" : "Driver");
                        double points = Double.parseDouble(entry.getString("points"));
                        if (Double.isNaN(points) || Double.isInfinite(points)) throw new Exception("Invalid points");

                        String jolpicaId = entity.optString(teams ? "constructorId" : "driverId");

                        if (matches(entity, teams, idA, titleA)) {
                            a = points;
                            foundA = jolpicaId;
                        }
                        if (matches(entity, teams, idB, titleB)) {
                            b = points;
                            foundB = jolpicaId;
                        }

                    }
                    int total = Integer.parseInt(mr.getString("total"));
                    int limit = Integer.parseInt(mr.getString("limit"));
                    if (offset + limit >= total || entries.length() == 0) break;
                    if (limit <= 0) throw new Exception("Invalid pagination");
                    offset += limit;
                }
                if (a == null || b == null) throw new Exception("One or both participants not found in Jolpica");
                Result result = new Result(a, b, representedRound, foundA, foundB);
                MAIN.post(() -> callback.onLoaded(result));
            } catch (Exception error) {
                MAIN.post(() -> callback.onError(error.getMessage()));
            }
        });
    }
    private static boolean matches(JSONObject entity, boolean team, String id, String title) {
        if (team) return id.equals(entity.optString("constructorId"));
        if (id.equals(entity.optString("code")) || id.equals(entity.optString("driverId"))) return true;
        String name = (entity.optString("givenName") + " " + entity.optString("familyName")).trim();
        return title != null && title.equals(name);
    }
    private static String get(String address) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(address).openConnection();
        connection.setConnectTimeout(15000);
        connection.setReadTimeout(15000);
        connection.setRequestProperty("Accept", "application/json");
        try {
            int status = connection.getResponseCode();
            if (status != 200) throw new Exception("Jolpica HTTP " + status);
            try (InputStream input = connection.getInputStream();
                 ByteArrayOutputStream output = new ByteArrayOutputStream()) {
                byte[] buffer = new byte[8192]; int count;
                while ((count = input.read(buffer)) != -1) output.write(buffer, 0, count);
                return new String(output.toByteArray(), StandardCharsets.UTF_8);
            }
        } finally { connection.disconnect(); }
    }
}
