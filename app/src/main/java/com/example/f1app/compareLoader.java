package com.example.f1app;

import android.graphics.Color;

import androidx.annotation.NonNull;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONArray;
import org.json.JSONObject;

import java.time.LocalDate;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class compareLoader {

    interface Callback<T> {
        void onLoaded(T result);
    }

    static final class Season {
        double place = Double.NaN;
        double points = Double.NaN;
        double wins = Double.NaN;
        String team = "";
    }

    private compareLoader() {
    }

    static void load(int type, String key, Callback<compareParticipant> callback) {
        if (type == compareParticipant.TEAM) {
            loadTeam(key, callback);
        } else {
            loadDriver(key, callback);
        }
    }

    // ---------- career ----------

    private static void loadDriver(String fullName, Callback<compareParticipant> callback) {
        DatabaseReference root = FirebaseDatabase.getInstance().getReference();

        root.child("drivers").child(fullName).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    callback.onLoaded(null);
                    return;
                }

                compareParticipant p = new compareParticipant(compareParticipant.DRIVER, fullName);
                String[] names = driverNameFormatter.split(fullName);
                p.given = names[0];
                p.family = names[1];

                String code = snapshot.child("driversCode").getValue(String.class);
                p.code = code == null ? "" : code;

                int now = LocalDate.now().getYear();
                p.firstYear = Math.max(year(snapshot.child("firstEntry").getValue(String.class)), 0);
                int last = year(snapshot.child("lastEntry").getValue(String.class));
                p.lastYear = last > 0 ? last : now;

                String team = snapshot.child("driversTeam").getValue(String.class);
                if (p.lastYear >= 2026 && ("Sauber".equals(team) || "Kick Sauber".equals(team))) {
                    team = "Audi";
                }
                p.caption = team == null ? "" : team;

                p.career.put("titles", number(snapshot.child("championshipsCount")));
                p.career.put("gp", number(snapshot.child("gpEntered")));
                p.career.put("wins", number(snapshot.child("totalWins")));
                p.career.put("podiums", number(snapshot.child("totalPodiums")));
                p.career.put("poles", number(snapshot.child("polesCount")));
                p.career.put("fastest", number(snapshot.child("fastestLapCount")));
                p.career.put("points", number(snapshot.child("totalPoints")));

                loadColor(root, team, p, callback);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                callback.onLoaded(null);
            }
        });
    }

    private static void loadColor(DatabaseReference root, String team, compareParticipant p,
                                  Callback<compareParticipant> callback) {
        if (team == null || team.isEmpty()) {
            callback.onLoaded(p);
            return;
        }

        root.child("constructors").orderByChild("name").equalTo(team)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        String id = null;
                        String color = null;
                        String dark = null;

                        for (DataSnapshot item : snapshot.getChildren()) {
                            id = item.getKey();
                            color = item.child("color").getValue(String.class);
                            dark = item.child("darkColor").getValue(String.class);
                            break;
                        }

                        if (id == null) {
                            id = pastTeamPalette.idForName(team);
                        }

                        p.teamId = id == null ? "" : id;
                        p.color = parse(pastTeamPalette.resolve(id, color), p.color);
                        p.altColor = parse(pastTeamPalette.resolve(id, dark), p.altColor);
                        callback.onLoaded(p);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        callback.onLoaded(p);
                    }
                });
    }

    private static void loadTeam(String teamId, Callback<compareParticipant> callback) {
        FirebaseDatabase.getInstance().getReference().child("constructors").child(teamId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (!snapshot.exists()) {
                            callback.onLoaded(null);
                            return;
                        }

                        compareParticipant p = new compareParticipant(compareParticipant.TEAM, teamId);
                        String name = snapshot.child("name").getValue(String.class);
                        String full = snapshot.child("fullTeamName").getValue(String.class);

                        p.title = name == null ? teamId : name;
                        p.caption = full == null ? "" : full;
                        p.teamId = teamId;

                        double enter = number(snapshot.child("enterYear"));
                        p.firstYear = Double.isNaN(enter) ? 0 : (int) enter;
                        p.lastYear = LocalDate.now().getYear();

                        p.color = parse(pastTeamPalette.resolve(teamId,
                                snapshot.child("color").getValue(String.class)), p.color);
                        p.altColor = parse(pastTeamPalette.resolve(teamId,
                                snapshot.child("darkColor").getValue(String.class)), p.altColor);

                        p.career.put("titles", number(snapshot.child("totalChampionships")));
                        p.career.put("wins", number(snapshot.child("totalWins")));
                        p.career.put("podiums", number(snapshot.child("totalPodiums")));
                        p.career.put("poles", number(snapshot.child("totalPoles")));
                        p.career.put("fastest", number(snapshot.child("totalFastestLaps")));
                        p.career.put("points", number(snapshot.child("totalPoints")));
                        p.career.put("enter", enter);

                        callback.onLoaded(p);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        callback.onLoaded(null);
                    }
                });
    }

    // ---------- one season (Jolpica standings) ----------

    static void season(compareParticipant p, int year, Callback<Season> callback) {
        Season result = new Season();

        if (!p.racedIn(year)) {
            callback.onLoaded(result);
            return;
        }

        if (p.type == compareParticipant.DRIVER) {
            jolpicaSeasons.driverStandings(year, response -> {
                if (response != null) {
                    JSONObject entry = jolpicaSeasons.driverEntry(response, p.code, p.given, p.family);

                    if (entry != null) {
                        fill(result, entry);

                        JSONArray teams = entry.optJSONArray("Constructors");
                        if (teams != null && teams.length() > 0) {
                            JSONObject last = teams.optJSONObject(teams.length() - 1);
                            if (last != null) {
                                result.team = last.optString("name", "");
                            }
                        }
                    }
                }
                callback.onLoaded(result);
            });
        } else {
            jolpicaSeasons.constructorStandings(year, response -> {
                if (response != null) {
                    JSONObject entry = constructorEntry(response, p.teamId);

                    if (entry != null) {
                        fill(result, entry);
                    }
                }
                callback.onLoaded(result);
            });
        }
    }

    private static void fill(Season result, JSONObject entry) {
        result.place = parseNumber(entry.optString("positionText", ""));
        result.points = parseNumber(entry.optString("points", ""));
        result.wins = parseNumber(entry.optString("wins", ""));
    }

    private static JSONObject constructorEntry(JSONObject response, String teamId) {
        JSONObject data = response.optJSONObject("MRData");
        JSONObject table = data == null ? null : data.optJSONObject("StandingsTable");
        JSONArray lists = table == null ? null : table.optJSONArray("StandingsLists");

        if (lists == null) {
            return null;
        }

        String wanted = normalize(teamId);

        for (int i = 0; i < lists.length(); i++) {
            JSONObject list = lists.optJSONObject(i);
            JSONArray entries = list == null ? null : list.optJSONArray("ConstructorStandings");

            if (entries == null) {
                continue;
            }

            for (int j = 0; j < entries.length(); j++) {
                JSONObject entry = entries.optJSONObject(j);
                JSONObject constructor = entry == null ? null : entry.optJSONObject("Constructor");

                if (constructor != null
                        && wanted.equals(normalize(constructor.optString("constructorId", "")))) {
                    return entry;
                }
            }
        }

        return null;
    }

    // ---------- helpers ----------

    private static String normalize(String id) {
        return id == null ? "" : id.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }

    private static int year(String entry) {
        if (entry == null) {
            return -1;
        }

        Matcher matcher = Pattern.compile("\\b(\\d{4})\\b").matcher(entry);

        if (!matcher.find()) {
            return -1;
        }

        try {
            return Integer.parseInt(matcher.group(1));
        } catch (NumberFormatException error) {
            return -1;
        }
    }

    private static double number(DataSnapshot snapshot) {
        Object value = snapshot.getValue();

        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }

        return value instanceof String ? parseNumber((String) value) : Double.NaN;
    }

    private static double parseNumber(String value) {
        try {
            return Double.parseDouble(value.trim().replace(',', '.'));
        } catch (RuntimeException error) {
            return Double.NaN;
        }
    }

    private static int parse(String hex, int fallback) {
        try {
            return Color.parseColor(hex);
        } catch (RuntimeException error) {
            return fallback;
        }
    }
}