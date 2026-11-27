package com.example.f1app;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.graphics.ColorUtils;

import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;

import java.util.Locale;

/**
 * Three podium cards: the photo on the colour of the team, the place, the surname.
 * Full mode adds the team and the gap; compact mode is for lists;
 * onDark is for surfaces that are dark in every theme (the header of the race page).
 */
final class podiumCards {

    private final Context context;
    private final boolean compact;
    private final View[] cards = new View[3];
    private String boundKey = "";
    private int token = 0;

    podiumCards(LinearLayout row, boolean compact) {
        this(row, compact, false);
    }

    podiumCards(LinearLayout row, boolean compact, boolean onDark) {
        this.context = row.getContext();
        this.compact = compact;

        LayoutInflater inflater = LayoutInflater.from(context);

        for (int i = 0; i < 3; i++) {
            View card = inflater.inflate(R.layout.view_podium_driver, row, false);
            ((TextView) card.findViewById(R.id.podiumPlace)).setText(String.valueOf(i + 1));

            if (onDark) {
                card.setBackgroundResource(R.drawable.bg_podium_card_dark);
                ((TextView) card.findViewById(R.id.podiumSurname)).setTextColor(Color.WHITE);
                ((TextView) card.findViewById(R.id.podiumTeam)).setTextColor(Color.parseColor("#B3FFFFFF"));
                ((TextView) card.findViewById(R.id.podiumGap)).setTextColor(Color.WHITE);
            }

            if (compact) {
                card.findViewById(R.id.podiumTile).getLayoutParams().height = dp(72);
                ((TextView) card.findViewById(R.id.podiumSurname)).setTextSize(12);
                card.findViewById(R.id.podiumTeam).setVisibility(View.GONE);
                card.findViewById(R.id.podiumGap).setVisibility(View.GONE);
            }

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            params.setMargins(dp(3), 0, dp(3), 0);
            row.addView(card, params);
            cards[i] = card;
        }
    }

    /** Photos and codes at once; surnames, colours and gaps follow when the season is known. */
    void bind(String season, String round, String first, String second, String third) {
        String key = season + "|" + round + "|" + first + "|" + second + "|" + third;

        if (key.equals(boundKey)) {
            return;
        }

        boundKey = key;
        final int mine = ++token;

        showBasics(season, first, second, third);

        podiumRepo.load(season, round, podium -> {
            if (mine == token && podium != null) {
                show(podium);
            }
        });
    }

    void showBasics(String season, String... codes) {
        for (int i = 0; i < 3; i++) {
            View card = cards[i];
            String code = i < codes.length ? codes[i] : null;
            boolean valid = raceWeekendStatus.isValidCode(code);

            ((TextView) card.findViewById(R.id.podiumSurname)).setText(valid ? code : "");
            paintTile(card, Color.parseColor("#303030"));

            ImageView photo = card.findViewById(R.id.podiumPhoto);
            photo.setImageDrawable(null);

            if (!valid) {
                photo.setImageResource(R.drawable.placeholder_driver);
                continue;
            }

            GlideApp.with(photo)
                    .load(FirebaseStorage.getInstance().getReference()
                            .child("drivers/" + code.toLowerCase(Locale.ROOT) + "_" + season + ".png"))
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .error(R.drawable.placeholder_driver)
                    .into(photo);
        }
    }

    void show(podiumEntry[] entries) {
        for (int i = 0; i < 3 && i < entries.length; i++) {
            if (entries[i] != null) {
                apply(cards[i], entries[i]);
            }
        }
    }

    private void apply(View card, podiumEntry entry) {
        if (!entry.surname.isEmpty()) {
            ((TextView) card.findViewById(R.id.podiumSurname)).setText(entry.surname);
        }

        if (!compact) {
            TextView team = card.findViewById(R.id.podiumTeam);
            team.setText(entry.team);
            team.setVisibility(entry.team.isEmpty() ? View.GONE : View.VISIBLE);

            TextView gap = card.findViewById(R.id.podiumGap);
            gap.setText(entry.gap);
            gap.setVisibility(entry.gap.isEmpty() ? View.GONE : View.VISIBLE);
        }

        String known = teamColors.get(entry.teamId);

        if (known != null) {
            paintTile(card, teamColors.parse(known));
        } else if (!entry.teamId.isEmpty()) {
            teamColors.fetch(FirebaseDatabase.getInstance().getReference(), entry.teamId, color -> {
                if (color != null) {
                    paintTile(card, teamColors.parse(color));
                }
            });
        }
    }

    private void paintTile(View card, int color) {
        card.findViewById(R.id.podiumTile).setBackground(new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{color, ColorUtils.blendARGB(color, Color.BLACK, 0.4f)}));
    }

    private int dp(float value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }
}