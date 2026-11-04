package com.example.f1app;

import android.os.Handler;
import android.os.Looper;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.LinkedHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.os.SystemClock;
import android.util.Log;

import java.io.IOException;
import java.net.SocketTimeoutException;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.HashSet;
import java.util.Set;

public final class predictionWeekendPointsLoader {
    public interface Callback {
        void onLoaded(Data data);
        void onError(String message);
    }

    private static final class ScheduledRace {
        final int round;
        final String name;
        final boolean sprint;

        ScheduledRace(int round, String name, boolean sprint) {
            this.round = round;
            this.name = name;
            this.sprint = sprint;
        }
    }

    private static final Map<String, Data> CACHE = new java.util.concurrent.ConcurrentHashMap<>();

    public static final class Row {
        public final String raceName;
        public final int round;
        public final boolean hasSprint;
        public final double raceA, sprintA, raceB, sprintB;
        Row(String name, int round, boolean sprint, double ra, double sa, double rb, double sb) {
            this.raceName = name; this.round = round; this.hasSprint = sprint;
            this.raceA = ra; this.sprintA = sa; this.raceB = rb; this.sprintB = sb;
        }
        public double totalA() { return raceA + sprintA; }
        public double totalB() { return raceB + sprintB; }
    }

    public static final class Data {
        public final List<Row> rows = new ArrayList<>();
        public int skipped;
        public double sumA() { double n = 0; for (Row r : rows) n += r.totalA(); return n; }
        public double sumB() { double n = 0; for (Row r : rows) n += r.totalB(); return n; }
    }

    private static final class Session {
        String name;
        final Map<String, JSONObject> entries = new LinkedHashMap<>();
    }

    private static final ExecutorService IO = Executors.newSingleThreadExecutor();
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    public void load(String season, int throughRound, boolean teams,
                     String idA, String idB, Callback callback) {
        if (season == null || !season.matches("\\d{4}") || throughRound < 0
                || idA == null || idB == null || idA.equals(idB)) {
            MAIN.post(() -> callback.onError("Invalid weekend history request"));
            return;
        }
        if (throughRound == 0) {
            MAIN.post(() -> callback.onLoaded(new Data()));
            return;
        }

        final String cacheKey = season + "|" + throughRound + "|" + teams
                + "|" + idA + "|" + idB;
        Data cached = CACHE.get(cacheKey);
        if (cached != null) {
            MAIN.post(() -> callback.onLoaded(cached));
            return;
        }

        FirebaseDatabase.getInstance().getReference("schedule")
                .child("season").child(season)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        List<ScheduledRace> races = readCalendar(snapshot, throughRound);
                        if (races == null) {
                            callback.onError("Calendar contains duplicate rounds");
                            return;
                        }
                        if (races.isEmpty()) {
                            callback.onError("Calendar has no rounds up to " + throughRound);
                            return;
                        }
                        loadFromJolpica(season, teams, idA, idB, races, cacheKey, callback);
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        callback.onError("Calendar: " + error.getMessage());
                    }
                });
    }

    private static List<ScheduledRace> readCalendar(DataSnapshot season, int throughRound) {
        List<ScheduledRace> races = new ArrayList<>();
        Set<Integer> seen = new HashSet<>();

        for (DataSnapshot race : season.getChildren()) {
            Long round = race.child("round").getValue(Long.class);
            String name = race.getKey();

            if (round == null || name == null || round < 1 || round > throughRound) {
                continue;
            }
            if (!seen.add(round.intValue())) {
                return null;
            }

            String sprintDate = race.child("Sprint")
                    .child("sprintRaceDate").getValue(String.class);

            races.add(new ScheduledRace(round.intValue(), name, hasDate(sprintDate)));
        }

        races.sort((a, b) -> Integer.compare(a.round, b.round));
        return races;
    }

    private static boolean hasDate(String value) {
        if (value == null) return false;
        String text = value.trim();
        return !text.isEmpty()
                && !text.equalsIgnoreCase("N/A")
                && !text.equalsIgnoreCase("NA");
    }

    private void loadFromJolpica(String season, boolean teams, String idA, String idB,
                                 List<ScheduledRace> races, String cacheKey,
                                 Callback callback) {
        boolean anySprint = false;
        for (ScheduledRace race : races) {
            anySprint |= race.sprint;
        }
        final boolean needSprints = anySprint;

        IO.execute(() -> {
            try {
                String root = "https://api.jolpi.ca/ergast/f1/" + season + "/"
                        + (teams ? "constructors" : "drivers") + "/";

                Map<Integer, Session> racesA =
                        sessions(root + idA + "/results/", "Results", season);
                Map<Integer, Session> racesB =
                        sessions(root + idB + "/results/", "Results", season);

                Map<Integer, Session> sprintsA = needSprints
                        ? sessions(root + idA + "/sprint/", "SprintResults", season)
                        : new TreeMap<Integer, Session>();
                Map<Integer, Session> sprintsB = needSprints
                        ? sessions(root + idB + "/sprint/", "SprintResults", season)
                        : new TreeMap<Integer, Session>();

                Data data = new Data();

                for (ScheduledRace race : races) {
                    boolean raceSeen = racesA.containsKey(race.round)
                            || racesB.containsKey(race.round);
                    boolean sprintSeen = sprintsA.containsKey(race.round)
                            || sprintsB.containsKey(race.round);

                    if (!raceSeen || (race.sprint && !sprintSeen)) {
                        data.skipped++;
                        continue;
                    }

                    data.rows.add(new Row(
                            race.name, race.round, race.sprint,
                            pointsOf(racesA.get(race.round), teams, idA),
                            pointsOf(sprintsA.get(race.round), teams, idA),
                            pointsOf(racesB.get(race.round), teams, idB),
                            pointsOf(sprintsB.get(race.round), teams, idB)));
                }

                if (data.skipped == 0) {
                    CACHE.put(cacheKey, data);
                }

                MAIN.post(() -> callback.onLoaded(data));
            } catch (Exception error) {
                Log.e("WeekendPoints", "Weekend history failed", error);
                MAIN.post(() -> callback.onError(error.toString()));
            }
        });
    }

    private static double pointsOf(Session session, boolean team, String id)
            throws Exception {
        return session == null ? 0 : sum(session, team, id, null);
    }

    private static Map<Integer, Session> sessions(String address, String field, String season)
            throws Exception {
        Map<Integer, Session> result = new TreeMap<>();
        for (JSONObject race : pages(address)) {
            verifySeason(race, season);
            int round = Integer.parseInt(race.getString("round"));
            Session session = result.get(round);
            if (session == null) {
                session = new Session(); session.name = race.getString("raceName");
                result.put(round, session);
            }
            JSONArray entries = race.getJSONArray(field);
            for (int i = 0; i < entries.length(); i++) {
                JSONObject item = entries.getJSONObject(i);
                String driver = item.getJSONObject("Driver").getString("driverId");
                JSONObject previous = session.entries.get(driver);
                if (previous != null && !previous.toString().equals(item.toString()))
                    throw new Exception("Source changed between pages; retry");
                session.entries.put(driver, item);
            }
        }
        return result;
    }

    private static List<JSONObject> pages(String address) throws Exception {
        List<JSONObject> races = new ArrayList<>();
        int offset = 0;
        while (true) {
            String response = get(address + "?limit=100&offset=" + offset);

            Thread.sleep(600);

            JSONObject mr = new JSONObject(response).getJSONObject("MRData");

            JSONArray batch = mr.getJSONObject("RaceTable").getJSONArray("Races");

            for (int i = 0; i < batch.length(); i++)
                races.add(batch.getJSONObject(i));

            int limit = Integer.parseInt(mr.getString("limit"));
            int total = Integer.parseInt(mr.getString("total"));

            if (limit <= 0)
                throw new Exception("Invalid pagination");

            if (batch.length() == 0 && offset < total)
                throw new Exception("Incomplete pagination");

            if (offset + limit >= total)
                return races;

            offset += limit;
        }
    }

    private static void verifySeason(JSONObject race, String season) throws Exception {
        if (!season.equals(race.getString("season"))) throw new Exception("Season mismatch");
    }

    private static double sum(Session session, boolean team, String id, String name) throws Exception {
        double sum = 0;
        for (JSONObject entry : session.entries.values()) {
            boolean match;
            if (team) match = id.equals(entry.getJSONObject("Constructor").getString("constructorId"));
            else {
                JSONObject driver = entry.getJSONObject("Driver");
                String title = (driver.optString("givenName") + " " + driver.optString("familyName")).trim();
                match = id.equals(driver.optString("code")) || id.equals(driver.optString("driverId"))
                        || (name != null && name.equals(title));
            }
            if (match) {
                double points = Double.parseDouble(entry.getString("points"));
                if (Double.isNaN(points) || Double.isInfinite(points)) throw new Exception("Invalid points");
                sum += points;
            }
        }
        return sum;
    }

    private static String get(String address) throws Exception {
        final int attempts = 2;

        IOException lastFailure = null;

        for (int attempt = 1; attempt <= attempts; attempt++) {
            HttpURLConnection connection = null;

            String stage = "opening";
            long startedAt = SystemClock.elapsedRealtime();

            Log.d(
                    "WeekendPoints",
                    "GET attempt=" + attempt + "/" + attempts
                            + " url=" + address
            );

            try {
                connection = (HttpURLConnection)
                        new URL(address).openConnection();

                connection.setConnectTimeout(10_000);
                connection.setReadTimeout(10_000);
                connection.setRequestProperty("Accept", "application/json");
                connection.setRequestProperty("Connection", "close");

                stage = "response_headers";

                int status = connection.getResponseCode();

                Log.d(
                        "WeekendPoints",
                        "HTTP " + status
                                + " elapsedMs="
                                + (SystemClock.elapsedRealtime() - startedAt)
                                + " url=" + address
                );

                boolean retryableStatus =
                        status == 429
                                || status == 502
                                || status == 503
                                || status == 504;

                if (status != HttpURLConnection.HTTP_OK) {
                    IOException error = new IOException(
                            "HTTP " + status + "; url=" + address
                    );

                    if (!retryableStatus || attempt == attempts) {
                        throw error;
                    }

                    lastFailure = error;

                } else {
                    stage = "response_body";

                    try (InputStream input = connection.getInputStream();
                         ByteArrayOutputStream output =
                                 new ByteArrayOutputStream()) {

                        byte[] buffer = new byte[8192];
                        int count;

                        while ((count = input.read(buffer)) != -1) {
                            output.write(buffer, 0, count);
                        }

                        String body = new String(
                                output.toByteArray(),
                                StandardCharsets.UTF_8
                        );

                        Log.d(
                                "WeekendPoints",
                                "OK bytes=" + output.size()
                                        + " elapsedMs="
                                        + (SystemClock.elapsedRealtime()
                                        - startedAt)
                                        + " url=" + address
                        );

                        return body;
                    }
                }

            } catch (SocketTimeoutException error) {
                lastFailure = new IOException(
                        "Timeout at " + stage
                                + "; attempt=" + attempt
                                + "; elapsedMs="
                                + (SystemClock.elapsedRealtime() - startedAt)
                                + "; url=" + address,
                        error
                );

                Log.w(
                        "WeekendPoints",
                        lastFailure.getMessage(),
                        error
                );

                if (attempt == attempts) {
                    throw lastFailure;
                }

            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }

            Thread.sleep(1_500L * attempt);
        }

        throw lastFailure != null
                ? lastFailure
                : new IOException("Request failed; url=" + address);
    }
}
