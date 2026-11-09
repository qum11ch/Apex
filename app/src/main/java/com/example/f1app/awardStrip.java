package com.example.f1app;

import android.content.Context;
import android.net.Uri;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;

import com.google.firebase.storage.FirebaseStorage;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

final class awardStrip extends LinearLayout {

    private static final Map<String, Uri> LOGOS = new ConcurrentHashMap<>();

    private final boolean compact;

    private int generation;
    private String boundUid;

    awardStrip(Context context, boolean compact) {
        super(context);

        this.compact = compact;

        setOrientation(HORIZONTAL);
        setGravity(Gravity.CENTER_VERTICAL);
        setBaselineAligned(false);
    }

    int begin(String uid) {
        clear();
        boundUid = uid;

        return generation;
    }

    boolean current(String uid, int request) {
        return request == generation && uid.equals(boundUid);
    }

    void clear() {
        generation++;
        boundUid = null;

        for (int index = 0; index < getChildCount(); index++) {
            clearIcons(getChildAt(index));
        }

        removeAllViews();
    }

    private void clearIcons(View view) {
        if (view instanceof awardIconView) {
            ((awardIconView) view).clear();
        } else if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;

            for (int index = 0; index < group.getChildCount(); index++) {
                clearIcons(group.getChildAt(index));
            }
        }
    }

    private int dp(int value) {
        return Math.round(
                value * getResources().getDisplayMetrics().density
        );
    }

    private String text(Map<String, Object> badge, String key) {
        Object value = badge.get(key);

        return value instanceof String ? (String) value : "";
    }

    private static int priority(String id) {
        if (id.matches("season_2026_place_[123]")) {
            return 1000 - (id.charAt(id.length() - 1) - '0');
        }

        if ("correct_100".equals(id)) {
            return 900;
        }

        if ("correct_50".equals(id)) {
            return 890;
        }

        if ("correct_10".equals(id)) {
            return 880;
        }

        if ("tenure_5".equals(id)) {
            return 800;
        }

        if ("tenure_3".equals(id)) {
            return 790;
        }

        if ("tenure_1".equals(id)) {
            return 780;
        }

        if ("first_correct".equals(id)) {
            return 700;
        }

        if ("season_2026_participant".equals(id)) {
            return 600;
        }

        return 500;
    }

    private int championshipCount(Object value) {
        if (value instanceof Number) {
            double number = ((Number) value).doubleValue();

            return !Double.isNaN(number)
                    && !Double.isInfinite(number)
                    && number >= 0
                    && number <= Integer.MAX_VALUE
                    && number == Math.rint(number)
                    ? (int) number
                    : 0;
        }

        if (value instanceof String) {
            try {
                return Math.max(
                        0,
                        Integer.parseInt(((String) value).trim())
                );
            } catch (NumberFormatException ignored) {
                return 0;
            }
        }

        return 0;
    }

    private int drawable(String id) {
        switch (id) {
            case "first_correct":
                return R.drawable.pit_pass_first_correct;

            case "correct_10":
                return R.drawable.pit_pass_correct_10_v2;

            case "correct_50":
                return R.drawable.pit_pass_correct_50;

            case "correct_100":
                return R.drawable.pit_pass_correct_100;

            case "tenure_1":
                return R.drawable.pit_pass_one_year;

            case "tenure_3":
                return R.drawable.pit_pass_three_years;

            case "tenure_5":
                return R.drawable.pit_pass_five_years;

            case "season_2026_participant":
                return R.drawable.pit_pass_season_participant_2026;

            case "season_2026_place_1":
                return R.drawable.pit_pass_season_place_1;

            case "season_2026_place_2":
                return R.drawable.pit_pass_season_place_2;

            case "season_2026_place_3":
                return R.drawable.pit_pass_season_place_3;

            default:
                return 0;
        }
    }

    private int title(String id) {
        switch (id) {
            case "first_correct":
                return R.string.awards_first_correct;

            case "correct_10":
                return R.string.awards_correct_10;

            case "correct_50":
                return R.string.awards_correct_50;

            case "correct_100":
                return R.string.awards_correct_100;

            case "tenure_1":
                return R.string.awards_member_1_year;

            case "tenure_3":
                return R.string.awards_member_3_year;

            case "tenure_5":
                return R.string.awards_member_5_year;

            case "season_2026_participant":
                return R.string.awards_preview_member;

            case "season_2026_place_1":
                return R.string.awards_season_place_1;

            case "season_2026_place_2":
                return R.string.awards_season_place_2;

            case "season_2026_place_3":
                return R.string.awards_season_place_3;

            case "fan_driver":
                return R.string.awards_fan_driver;

            case "fan_team":
                return R.string.awards_fan_team;

            default:
                return R.string.badges_section_title;
        }
    }

    void render(
            String uid,
            int request,
            List<Map<String, Object>> values
    ) {
        if (!current(uid, request)) {
            return;
        }

        List<Map<String, Object>> badges = new ArrayList<>();

        for (Map<String, Object> badge : values) {
            String id = text(badge, "id");

            if (drawable(id) != 0
                    || "fan_driver".equals(id)
                    || "fan_team".equals(id)) {
                badges.add(badge);
            }
        }

        badges.sort(
                Comparator.comparingInt(
                        (Map<String, Object> badge) ->
                                priority(text(badge, "id"))
                ).reversed()
        );

        int limit = compact
                ? Math.min(3, badges.size())
                : badges.size();

        for (int index = 0; index < limit; index++) {
            Map<String, Object> badge = badges.get(index);

            String id = text(badge, "id");
            String caption = getContext().getString(title(id));
            String name = text(badge, "name");

            final String description = caption
                    + (name.isEmpty() ? "" : ": " + name)
                    + (id.startsWith("season_") ? " · 2026" : "");

            LinearLayout cell = new LinearLayout(getContext());
            cell.setOrientation(VERTICAL);
            cell.setGravity(Gravity.CENTER_HORIZONTAL);

            LayoutParams cellParams = new LayoutParams(
                    compact ? dp(29) : dp(80),
                    LayoutParams.WRAP_CONTENT
            );
            cellParams.setMarginEnd(dp(compact ? 1 : 6));

            addView(cell, cellParams);

            awardIconView icon = new awardIconView(getContext());

            cell.addView(
                    icon,
                    new LayoutParams(
                            dp(compact ? 26 : 56),
                            dp(compact ? 26 : 56)
                    )
            );

            if ("fan_driver".equals(id)) {
                icon.showDriver(
                        text(badge, "code"),
                        championshipCount(badge.get("championshipsCount")),
                        description
                );
            } else if ("fan_team".equals(id)) {
                String path = text(badge, "storagePath");
                String fallback = text(badge, "code");

                Uri cached = LOGOS.get(path);
                icon.showTeam(cached, fallback, description);

                if (cached == null
                        && path.matches("teams/[a-z0-9_-]+_logo(_alt)?\\.png")) {
                    FirebaseStorage.getInstance()
                            .getReference()
                            .child(path)
                            .getDownloadUrl()
                            .addOnSuccessListener(uri -> {
                                LOGOS.put(path, uri);

                                if (current(uid, request)) {
                                    icon.showTeam(
                                            uri,
                                            fallback,
                                            description
                                    );
                                }
                            })
                            .addOnFailureListener(error -> {
                                android.util.Log.w(
                                        "Awards",
                                        "Team badge logo unavailable",
                                        error
                                );
                            });
                }
            } else {
                icon.showMedal(drawable(id), description);
            }

            if (!compact) {
                TextView label = new TextView(getContext());
                label.setText(caption);
                label.setTextSize(11);
                label.setMaxLines(2);
                label.setGravity(Gravity.CENTER);
                label.setTextColor(
                        ContextCompat.getColor(
                                getContext(),
                                R.color.text_color_main
                        )
                );

                cell.addView(
                        label,
                        new LayoutParams(
                                LayoutParams.MATCH_PARENT,
                                LayoutParams.WRAP_CONTENT
                        )
                );

                icon.setOnClickListener(view -> {
                    Object stamp = badge.get("awardedAt");
                    String message = description;

                    if (stamp instanceof Number) {
                        String recordedDate = DateFormat.getDateInstance()
                                .format(
                                        new Date(
                                                ((Number) stamp).longValue()
                                        )
                                );

                        message += "\n" + getContext().getString(
                                R.string.badges_recorded_on,
                                recordedDate
                        );
                    }

                    new AlertDialog.Builder(getContext())
                            .setTitle(caption)
                            .setMessage(message)
                            .setPositiveButton(android.R.string.ok, null)
                            .show();
                });
            }
        }

        if (compact && badges.size() > limit) {
            TextView more = new TextView(getContext());
            more.setText("+" + (badges.size() - limit));
            more.setTextSize(11);
            more.setTextColor(
                    ContextCompat.getColor(
                            getContext(),
                            R.color.grey
                    )
            );

            addView(more);
        }

        setVisibility(badges.isEmpty() ? GONE : VISIBLE);
    }
}