package com.example.f1app;

import android.content.Context;

import java.util.ArrayList;
import java.util.List;

/** The small grey line under a prediction title: where it belongs and what it asks. */
final class predictionMeta {

    private predictionMeta() {
    }

    /**
     * Two short lines instead of one chain of bullets:
     * "Round 5, 2026" / "Winner, Driver".
     */
    static String build(Context context, String categoryTitle, boolean weekend,
                        Object season, Object round, String typeTitle, String optionTitle) {

        String where;

        if (weekend && season != null && round != null) {
            where = context.getString(R.string.prediction_meta_weekend,
                    String.valueOf(round), String.valueOf(season));
        } else if (season != null) {
            where = context.getString(R.string.prediction_season, String.valueOf(season));
        } else {
            where = categoryTitle == null ? "" : categoryTitle;
        }

        List<String> what = new ArrayList<>();

        if (typeTitle != null && !typeTitle.isEmpty()) {
            what.add(typeTitle);
        }

        if (optionTitle != null && !optionTitle.isEmpty()) {
            what.add(optionTitle);
        }

        String second = android.text.TextUtils.join(", ", what);

        if (where.isEmpty()) {
            return second;
        }

        return second.isEmpty() ? where : where + "\n" + second;
    }
}
