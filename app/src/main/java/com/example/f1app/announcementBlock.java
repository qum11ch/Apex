package com.example.f1app;

import static com.example.f1app.MainActivity.APP_PREFERENCES;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.function.Supplier;

/** A message from the administrator: the card on top of the home screen. */
final class announcementBlock {
    private static final String KEY_DISMISSED = "dismissed_announcements";

    private final Context context;
    private final View card, close;
    private final TextView title, body, cta;
    private final Supplier<String> season;
    private final SharedPreferences prefs;
    private int token = 0;

    announcementBlock(View card, Supplier<String> season) {
        this.context = card.getContext();
        this.card = card;
        this.season = season;
        this.prefs = context.getSharedPreferences(APP_PREFERENCES, Context.MODE_PRIVATE);
        title = card.findViewById(R.id.announcementTitle);
        body = card.findViewById(R.id.announcementBody);
        cta = card.findViewById(R.id.announcementCta);
        close = card.findViewById(R.id.announcementClose);
    }

    void stop() {
        token++;
    }

    void refresh() {
        final int mine = ++token;

        FirebaseDatabase.getInstance().getReference("announcements")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (mine != token) {
                            return;
                        }

                        DataSnapshot best = pick(snapshot);

                        if (best == null) {
                            card.setVisibility(View.GONE);
                        } else {
                            show(best);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        // the card keeps what it showed before
                    }
                });
    }

    private DataSnapshot pick(DataSnapshot all) {
        long now = System.currentTimeMillis();
        Set<String> dismissed = new HashSet<>(
                prefs.getStringSet(KEY_DISMISSED, new HashSet<>()));

        DataSnapshot best = null;
        long bestPriority = Long.MIN_VALUE;
        long bestStart = Long.MIN_VALUE;

        for (DataSnapshot item : all.getChildren()) {
            if (!Boolean.TRUE.equals(item.child("enabled").getValue(Boolean.class))) {
                continue;
            }

            if (text(item, "title").isEmpty()) {
                continue;
            }

            Long startsAt = number(item, "startsAt");
            Long expiresAt = number(item, "expiresAt");
            Long minVersion = number(item, "minVersionCode");
            Long maxVersion = number(item, "maxVersionCode");

            if ((startsAt != null && startsAt > now)
                    || (expiresAt != null && expiresAt <= now)
                    || (minVersion != null && BuildConfig.VERSION_CODE < minVersion)
                    || (maxVersion != null && BuildConfig.VERSION_CODE > maxVersion)) {
                continue;
            }

            if (dismissible(item) && dismissed.contains(item.getKey())) {
                continue;
            }

            Long priority = number(item, "priority");
            long p = priority == null ? 0 : priority;
            long s = startsAt == null ? 0 : startsAt;

            if (best == null || p > bestPriority || (p == bestPriority && s > bestStart)) {
                best = item;
                bestPriority = p;
                bestStart = s;
            }
        }

        return best;
    }

    private void show(DataSnapshot item) {
        String action = item.child("action").getValue(String.class);
        String bodyText = text(item, "body");

        title.setText(text(item, "title"));
        body.setText(bodyText);
        body.setVisibility(bodyText.isEmpty() ? View.GONE : View.VISIBLE);

        boolean clickable = action != null && !"none".equals(action);
        String label = text(item, "ctaText");

        if (label.isEmpty()) {
            label = defaultLabel(action);
        }

        cta.setText(label);
        cta.setVisibility(clickable && !label.isEmpty() ? View.VISIBLE : View.GONE);

        card.setOnClickListener(clickable ? v -> run(item, action) : null);
        card.setClickable(clickable);

        close.setVisibility(dismissible(item) ? View.VISIBLE : View.GONE);
        close.setOnClickListener(v -> {
            Set<String> dismissed = new HashSet<>(
                    prefs.getStringSet(KEY_DISMISSED, new HashSet<>()));
            dismissed.add(item.getKey());
            prefs.edit().putStringSet(KEY_DISMISSED, dismissed).apply();
            card.setVisibility(View.GONE);
        });

        card.setVisibility(View.VISIBLE);
    }

    private String defaultLabel(String action) {
        if ("predictions".equals(action)) {
            return context.getString(R.string.announcement_cta_predictions);
        }

        if ("leaderboard".equals(action)) {
            return context.getString(R.string.announcement_cta_leaderboard);
        }

        if ("update".equals(action)) {
            return context.getString(R.string.announcement_cta_update);
        }

        if ("link".equals(action)) {
            return context.getString(R.string.announcement_cta_link);
        }

        return "";
    }

    private void run(DataSnapshot item, String action) {
        try {
            if ("predictions".equals(action)) {
                context.startActivity(new Intent(context, predictionActivity.class));
            } else if ("leaderboard".equals(action)) {
                String value = season.get();

                if (value == null || value.trim().isEmpty()) {
                    value = String.valueOf(LocalDate.now().getYear());
                }

                Intent intent = new Intent(context, leaderboardActivity.class);
                intent.putExtra("currentSeason", value);
                context.startActivity(intent);
            } else if ("update".equals(action)) {
                open("https://www.rustore.ru/catalog/app/" + context.getPackageName());
            } else if ("link".equals(action)) {
                String url = item.child("url").getValue(String.class);

                if (url != null && url.startsWith("https://")) {
                    open(url);
                }
            } else {
                Log.w("Announcement", "unknown action " + action);
            }
        } catch (ActivityNotFoundException error) {
            Log.w("Announcement", "Nothing can open the action", error);
            Toast.makeText(context, R.string.announcement_open_error, Toast.LENGTH_LONG).show();
        } catch (RuntimeException error) {
            Log.e("Announcement", "Action failed", error);
        }
    }

    private void open(String url) {
        context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
    }

    private boolean dismissible(DataSnapshot item) {
        return !Boolean.FALSE.equals(item.child("dismissible").getValue(Boolean.class));
    }

    private String text(DataSnapshot item, String key) {
        DataSnapshot node = item.child(key);

        if (node.getValue() instanceof String) {
            return ((String) node.getValue()).trim();
        }

        boolean russian = "ru".equals(Locale.getDefault().getLanguage());
        String first = node.child(russian ? "ru" : "en").getValue(String.class);
        String second = node.child(russian ? "en" : "ru").getValue(String.class);

        if (first != null && !first.trim().isEmpty()) {
            return first.trim();
        }

        return second == null ? "" : second.trim();
    }

    private Long number(DataSnapshot item, String key) {
        Object value = item.child(key).getValue();
        return value instanceof Number ? ((Number) value).longValue() : null;
    }
}