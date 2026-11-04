package com.example.f1app;

import com.google.firebase.database.*;
import java.util.ArrayList;
import java.util.List;

public final class predictionSingleCardLoader {
    public interface Callback {
        void onLoaded(Data data);
        void onError();
    }

    public static final class Row {
        public final String raceName, result;
        public final int points;
        Row(String raceName, String result, int points) {
            this.raceName = raceName;
            this.result = result;
            this.points = points;
        }
    }

    public static final class Data {
        public String title, season, targetRace, targetDate;
        public boolean team;
        public int points, wins, podiums, retirements;
        public final List<Row> rows = new ArrayList<>();
    }

    private final DatabaseReference root = FirebaseDatabase.getInstance().getReference();

    public void load(String season, Integer beforeRound, boolean team,
                     String optionId, String optionTitle, Callback callback) {
        if (season == null || optionId == null || optionTitle == null
                || (beforeRound != null && beforeRound < 1)) {
            callback.onError();
            return;
        }
        root.child("schedule").child("season").child(season)
                .orderByChild("round")
                .addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(DataSnapshot calendar) {
                List<String> races = new ArrayList<>();
                Data data = new Data();
                data.title = optionTitle;
                data.season = season;
                data.team = team;
                for (DataSnapshot race : calendar.getChildren()) {
                    Long round = race.child("round").getValue(Long.class);
                    String name = race.getKey();
                    if (round == null || name == null) continue;
                    if (beforeRound != null && round == beforeRound.longValue()) {
                        data.targetRace = name;
                        data.targetDate = race.child("raceDate").getValue(String.class);
                    }
                    if (beforeRound == null || round < beforeRound) races.add(name);
                }
                if (!calendar.exists() || (beforeRound != null && data.targetRace == null)) {
                    callback.onError();
                    return;
                }
                root.child("results").child("season").child(season)
                        .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override public void onDataChange(DataSnapshot results) {
                        for (String race : races) {
                            int racePoints = 0;
                            boolean found = false;
                            String driverResult = null;
                            for (DataSnapshot driver : results.getChildren()) {
                                if (!team && !optionTitle.equals(driver.getKey())) continue;
                                DataSnapshot record = driver.child(race);
                                if (team && !optionId.equals(record.child("TeamId")
                                        .getValue(String.class))) continue;
                                String raw = record.child("Result").getValue(String.class);
                                String finish = completedFinish(raw);
                                if (finish == null) continue;
                                found = true;
                                driverResult = raw;
                                Integer position = numericPosition(finish);
                                int points = position == null ? 0 : points(position);
                                racePoints += points;
                                if (position != null && position == 1) data.wins++;
                                if (position != null && position <= 3) data.podiums++;
                                if ("R".equals(finish)) data.retirements++;
                            }
                            if (found) {
                                data.points += racePoints;
                                data.rows.add(new Row(race, team ? null : driverResult, racePoints));
                            }
                        }
                        callback.onLoaded(data);
                    }
                    @Override public void onCancelled(DatabaseError error) { callback.onError(); }
                });
            }
            @Override public void onCancelled(DatabaseError error) { callback.onError(); }
        });
    }

    private static String completedFinish(String raw) {
        if (raw == null) return null;
        String[] parts = raw.trim().split("-", 2);
        if (parts.length != 2) return null;
        String finish = parts[1].trim();
        if (numericPosition(finish) != null || "R".equals(finish)
                || "W".equals(finish) || "D".equals(finish) || "NC".equals(finish)) return finish;
        return null;
    }

    private static Integer numericPosition(String value) {
        try {
            int position = Integer.parseInt(value);
            return position > 0 ? position : null;
        } catch (NumberFormatException error) { return null; }
    }

    private static int points(int position) {
        int[] scale = {25, 18, 15, 12, 10, 8, 6, 4, 2, 1};
        return position >= 1 && position <= scale.length ? scale[position - 1] : 0;
    }
}
