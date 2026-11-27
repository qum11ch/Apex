package com.example.f1app;

import org.json.JSONObject;

final class podiumEntry {
    final String code, surname, teamId, team, gap;

    private podiumEntry(String code, String surname, String teamId, String team, String gap) {
        this.code = code;
        this.surname = surname;
        this.teamId = teamId;
        this.team = team;
        this.gap = gap;
    }

    static podiumEntry from(JSONObject result) {
        JSONObject driver = result.optJSONObject("Driver");
        JSONObject constructor = result.optJSONObject("Constructor");
        JSONObject time = result.optJSONObject("Time");

        return new podiumEntry(
                driver == null ? "" : driver.optString("code"),
                driver == null ? "" : driver.optString("familyName"),
                constructor == null ? "" : constructor.optString("constructorId"),
                constructor == null ? "" : constructor.optString("name"),
                time != null ? time.optString("time") : result.optString("status"));
    }
}