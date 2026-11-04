package com.example.f1app;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public final class predictionCircuitCardLoader {

    public interface Callback {
        void onLoaded(Data data);
        void onError();
    }

    public static final class Data {
        public String season, raceName, raceDate, circuitId, circuitName;
        public String location, country, laps, turns, length, raceDistance;
        public String opened, firstGp, altitude;
        public String recordTime, recordDriver;
        public Long recordYear;
        public int round;
        public String firstPracticeDate;
    }

    private final DatabaseReference root = FirebaseDatabase.getInstance().getReference();

    public void load(String season, Integer round, Callback callback) {
        if (season == null || round == null || round < 1) {
            callback.onError();
            return;
        }
        root.child("schedule").child("season").child(season)
                .addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(DataSnapshot calendar) {
                for (DataSnapshot race : calendar.getChildren()) {
                    Long value = race.child("round").getValue(Long.class);
                    if (value == null || value.longValue() != round.longValue()) continue;
                    String circuitId = race.child("Circuit").child("circuitId")
                            .getValue(String.class);
                    if (circuitId == null || circuitId.trim().isEmpty()) break;
                    Data data = new Data();
                    data.season = season;
                    data.raceName = race.getKey();
                    data.raceDate = race.child("raceDate").getValue(String.class);
                    data.circuitId = circuitId;

                    data.round = round;
                    data.firstPracticeDate = race
                            .child("FirstPractice")
                            .child("firstPracticeDate")
                            .getValue(String.class);

                    loadCircuit(data, callback);
                    return;
                }
                callback.onError();
            }
            @Override public void onCancelled(DatabaseError error) { callback.onError(); }
        });
    }

    private void loadCircuit(Data data, Callback callback) {
        root.child("circuits").child(data.circuitId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(DataSnapshot c) {
                if (!c.exists()) { callback.onError(); return; }
                data.circuitName = text(c, "circuitName");
                data.location = text(c, "location");
                data.country = text(c, "country");
                data.laps = text(c, "lapsCount");
                data.turns = text(c, "turnsCount");
                data.length = text(c, "length");
                data.raceDistance = text(c, "raceDistance");
                data.opened = text(c, "opened");
                data.firstGp = text(c, "firstGPyear");
                data.altitude = text(c, "altitude");
                data.recordTime = text(c, "lapRecordTime");
                data.recordDriver = text(c, "lapRecordDriver");
                data.recordYear = c.child("lapRecordYear").getValue(Long.class);
                if (data.circuitName == null) { callback.onError(); return; }
                callback.onLoaded(data);
            }
            @Override public void onCancelled(DatabaseError error) { callback.onError(); }
        });
    }

    private static String text(DataSnapshot parent, String key) {
        String value = parent.child(key).getValue(String.class);
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }
}
