package com.example.f1app;

import java.util.HashMap;
import java.util.Map;

final class compareParticipant {
    static final int DRIVER = 0;
    static final int TEAM = 1;

    final int type;
    final String key;

    String title = "";
    String caption = "";
    String code = "";
    String given = "";
    String family = "";
    String teamId = "";
    int color = 0xFF8E8E93;
    int altColor = 0xFF5A5A5F;
    int firstYear;
    int lastYear;
    final Map<String, Double> career = new HashMap<>();

    compareParticipant(int type, String key) {
        this.type = type;
        this.key = key;
    }

    boolean racedIn(int year) {
        return firstYear > 0 && year >= firstYear && year <= lastYear;
    }

    double stat(String name) {
        Double value = career.get(name);
        return value == null ? Double.NaN : value;
    }
}