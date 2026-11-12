package com.example.f1app;

import java.util.ArrayList;
import java.util.List;

/** One race row of the team results table: results are aligned with the drivers list. */
final class teamRaceRow {
    final String raceName;
    /** Codes in the old format: "CND", "NP", "N/C", "null" or "start-finish" ("1-3", "5-R", "0-W", "7-D"). */
    final List<String> results;

    teamRaceRow(String raceName, List<String> results) {
        this.raceName = raceName;
        this.results = new ArrayList<>(results);
    }
}
