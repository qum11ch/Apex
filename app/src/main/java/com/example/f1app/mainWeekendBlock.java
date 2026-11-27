package com.example.f1app;

import static com.example.f1app.MainActivity.getStringByName;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Countdown, the strip of sessions and the weather of the chosen session inside the home card. */
final class mainWeekendBlock {

    private static final long HOUR = 3_600_000L;
    private static final int RAIN_WARNING = 50;

    private final Context context;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final weatherCard loader = new weatherCard();
    private final Runnable tick = this::onTick;

    private final View block, countdownGroup, weatherBody, liveBadge, liveDot;
    private final TextView liveText, countdownLabel, weatherCaption, weatherTemp,
            weatherCondition, weatherRain, weatherWind, weatherHumidity, weatherMessage, weatherNote;
    private final ImageView weatherIcon;
    private final LinearLayout chipsRow;
    private final List<View> chips = new ArrayList<>();

    private weatherCard.Result result;
    private List<raceWeekendStatus.SessionInfo> sessions;
    private String boundKey = "";
    private String shownState = "";
    private String selectedKey;
    private boolean userPicked;
    private ObjectAnimator pulse;

    private final TextView cdA, cdB, cdC, capA, capB, capC;
    private boolean daysMode;

    private java.util.Map<String, sessionWeather> byKey = new java.util.HashMap<>();
    private String sourceNote = "";

    mainWeekendBlock(View root) {
        context = root.getContext();

        block = root.findViewById(R.id.weekendBlock);
        countdownGroup = root.findViewById(R.id.countdownGroup);
        countdownLabel = root.findViewById(R.id.countdownLabel);
        chipsRow = root.findViewById(R.id.sessionsRow);
        weatherCaption = root.findViewById(R.id.weatherCaption);
        weatherBody = root.findViewById(R.id.weatherBody);
        weatherIcon = root.findViewById(R.id.weatherIcon);
        weatherTemp = root.findViewById(R.id.weatherTemp);
        weatherCondition = root.findViewById(R.id.weatherCondition);
        weatherRain = root.findViewById(R.id.weatherRain);
        weatherWind = root.findViewById(R.id.weatherWind);
        weatherHumidity = root.findViewById(R.id.weatherHumidity);
        weatherMessage = root.findViewById(R.id.weatherMessage);
        weatherNote = root.findViewById(R.id.weatherNote);
        liveBadge = root.findViewById(R.id.liveBadge);
        liveDot = root.findViewById(R.id.liveDot);
        liveText = root.findViewById(R.id.liveText);
        cdA = root.findViewById(R.id.cdA);
        cdB = root.findViewById(R.id.cdB);
        cdC = root.findViewById(R.id.cdC);
        capA = root.findViewById(R.id.capA);
        capB = root.findViewById(R.id.capB);
        capC = root.findViewById(R.id.capC);

        root.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
            @Override
            public void onViewAttachedToWindow(View v) {
                if (sessions != null) {
                    start();
                }
            }

            @Override
            public void onViewDetachedFromWindow(View v) {
                stop();
            }
        });
    }

    void bind(String year, String raceName) {
        String key = year + "|" + raceName;

        if (key.equals(boundKey)) {
            if (sessions != null) {
                start();
            }
            return;
        }

        hide();
        boundKey = key;
        byKey = new java.util.HashMap<>();
        sourceNote = "";

        loader.fetchRace(year, raceName, r -> {
            if (!key.equals(boundKey)) {
                return;
            }

            result = r;
            sessions = r.race.sessions;
            selectedKey = null;
            userPicked = false;
            shownState = "";
            buildChips();
            block.setVisibility(View.VISIBLE);
            start();

            // the same weather as on the race pages: session window plus F1 measurements
            weekendWeather.load(context, year, raceName, (map, note) -> {
                if (!key.equals(boundKey) || sessions == null) {
                    return;
                }

                byKey = map;
                sourceNote = note;
                buildChips();
                shownState = "";
                refresh();
            });
        });
    }

    void hide() {
        loader.cancel();
        stop();
        boundKey = "";
        shownState = "";
        result = null;
        sessions = null;
        block.setVisibility(View.GONE);
        liveBadge.setVisibility(View.GONE);
    }

    // ---------- timer ----------

    private void start() {
        handler.removeCallbacks(tick);
        onTick();
    }

    private void stop() {
        handler.removeCallbacks(tick);
        stopPulse();
        shownState = "";
    }

    private void onTick() {
        long remaining = refresh();

        if (remaining >= 0) {
            long step = remaining >= 86_400_000L ? 60_000L : 1_000L;
            handler.postDelayed(tick, Math.max(250, remaining % step + 30));
        }
    }

    private long refresh() {
        long now = System.currentTimeMillis();
        raceWeekendStatus.SessionInfo live = raceWeekendStatus.liveSession(sessions, now);
        raceWeekendStatus.SessionInfo next = raceWeekendStatus.nextSession(sessions, now);

        String state = (live == null ? "-" : live.key) + "|" + (next == null ? "-" : next.key);

        if (!state.equals(shownState)) {
            shownState = state;
            applyState(live, next, now);
        }

        if (live != null) {
            setCountdown(live.end - now);
            return live.end - now;
        }

        if (next != null) {
            setCountdown(next.start - now);
            return next.start - now;
        }

        return -1;
    }

    private void setCountdown(long millis) {
        long s = Math.max(0, millis / 1000);
        long d = s / 86400;
        long h = s % 86400 / 3600;
        long m = s % 3600 / 60;
        boolean days = d > 0;

        cdA.setText(two(days ? d : h));
        cdB.setText(two(days ? h : m));
        cdC.setText(two(days ? m : s % 60));

        if (days != daysMode || capA.length() == 0) {
            daysMode = days;
            capA.setText(days ? R.string.unit_days : R.string.unit_hours);
            capB.setText(days ? R.string.unit_hours : R.string.unit_minutes);
            capC.setText(days ? R.string.unit_minutes : R.string.unit_seconds);
        }
    }

    private static String two(long value) {
        return String.format(Locale.getDefault(), "%02d", value);
    }

    private void setCountdownColor(boolean live) {
        int color = color(live ? R.color.hero_accent : R.color.hero_text);
        cdA.setTextColor(color);
        cdB.setTextColor(color);
        cdC.setTextColor(color);
    }

    private void applyState(raceWeekendStatus.SessionInfo live, raceWeekendStatus.SessionInfo next,
                            long now) {
        stopPulse();

        if (live != null) {
            liveBadge.setVisibility(View.VISIBLE);
            liveBadge.setBackgroundResource(R.drawable.bg_live_pill);
            liveDot.setVisibility(View.VISIBLE);
            liveText.setText(R.string.live_badge);
            liveText.setTextColor(color(R.color.hero_accent));
            pulse = ObjectAnimator.ofFloat(liveDot, "alpha", 1f, 0.45f);
            pulse.setDuration(800);
            pulse.setRepeatMode(ValueAnimator.REVERSE);
            pulse.setRepeatCount(ValueAnimator.INFINITE);
            pulse.start();
        } else if (next == null) {
            liveBadge.setVisibility(View.VISIBLE);
            liveBadge.setBackground(null);
            liveDot.setVisibility(View.GONE);
            liveText.setText(R.string.awaiting_results_text);
            liveText.setTextColor(color(R.color.hero_text_secondary));
        } else {
            liveBadge.setVisibility(View.GONE);
        }

        raceWeekendStatus.SessionInfo target = live != null ? live : next;

        if (target == null) {
            countdownGroup.setVisibility(View.GONE);
        } else {
            countdownGroup.setVisibility(View.VISIBLE);
            countdownLabel.setText(context.getString(
                    live != null ? R.string.countdown_ends : R.string.countdown_starts,
                    sessionName(target.key)));
            setCountdownColor(live != null);
        }

        if (!userPicked) {
            selectedKey = live != null ? live.key
                    : next != null ? next.key
                    : sessions.get(sessions.size() - 1).key;
        }

        styleChips(now);
        showWeather();
    }

    private void stopPulse() {
        if (pulse != null) {
            pulse.cancel();
            pulse = null;
        }

        liveDot.setAlpha(1f);
    }

    // ---------- the strip of sessions ----------

    private void buildChips() {
        chipsRow.removeAllViews();
        chips.clear();

        boolean anyWeather = !byKey.isEmpty();
        DateTimeFormatter dayFormat = DateTimeFormatter.ofPattern("EEE", Locale.getDefault());
        DateTimeFormatter timeFormat = DateTimeFormatter.ofPattern("HH:mm");

        for (raceWeekendStatus.SessionInfo s : sessions) {
            ZonedDateTime time = Instant.ofEpochMilli(s.start).atZone(ZoneId.systemDefault());

            LinearLayout chip = new LinearLayout(context);
            chip.setOrientation(LinearLayout.VERTICAL);
            chip.setGravity(Gravity.CENTER_HORIZONTAL);
            chip.setPadding(dp(2), dp(10), dp(2), dp(10));

            TextView name = text(context.getString(getStringByName("session_short_" + s.key)),
                    10, R.color.hero_text, true);
            name.setAllCaps(true);
            addCentered(chip, name, 0);

            TextView day = text(time.format(dayFormat), 11, R.color.hero_text_secondary, false);
            day.setAllCaps(true);
            addCentered(chip, day, 2);

            addCentered(chip, text(time.format(timeFormat), 14, R.color.hero_text, true), 2);

            if (anyWeather) {
                sessionWeather sw = byKey.get(s.key);

                LinearLayout row = new LinearLayout(context);
                row.setGravity(Gravity.CENTER);

                ImageView icon = new ImageView(context);
                icon.setColorFilter(color(R.color.hero_text));
                row.addView(icon, new LinearLayout.LayoutParams(dp(16), dp(16)));

                TextView temp = text("", 12, R.color.hero_text, false);
                temp.setPadding(dp(3), 0, 0, 0);
                temp.setGravity(Gravity.CENTER_VERTICAL);
                row.addView(temp, new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

                if (sw != null) {
                    icon.setImageResource(sw.icon);
                    temp.setText(sw.temp);
                } else {
                    temp.setText(" ");
                    row.setVisibility(View.INVISIBLE);
                }

                addCentered(chip, row, 6);
            }

            chip.setOnClickListener(v -> {
                userPicked = true;
                selectedKey = s.key;
                styleChips(System.currentTimeMillis());
                showWeather();
            });

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            params.setMargins(dp(3), 0, dp(3), 0);
            chipsRow.addView(chip, params);
            chips.add(chip);
        }
    }

    private void addCentered(LinearLayout parent, View child, int topDp) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.topMargin = dp(topDp);
        parent.addView(child, params);
    }

    private void styleChips(long now) {
        for (int i = 0; i < chips.size(); i++) {
            raceWeekendStatus.SessionInfo s = sessions.get(i);
            LinearLayout chip = (LinearLayout) chips.get(i);

            boolean selected = s.key.equals(selectedKey);
            boolean live = now >= s.start && now < s.end;
            boolean finished = now >= s.end;

            GradientDrawable background = new GradientDrawable();
            background.setCornerRadius(dp(12));
            background.setColor(color(R.color.hero_chip_bg));

            if (selected) {
                background.setStroke(dp(2), color(R.color.hero_accent));
            }

            chip.setBackground(background);
            chip.setAlpha(finished && !selected ? 0.45f : 1f);
            ((TextView) chip.getChildAt(0)).setTextColor(
                    color(live ? R.color.hero_accent : R.color.hero_text));
        }
    }

    // ---------- the weather of the chosen session ----------

    private void showWeather() {
        sessionWeather sw = byKey.get(selectedKey);
        weatherForecast.Hour current = null;

        if (sw == null && result.data != null && result.data.current != null) {
            current = result.data.current;
        }

        String note = !byKey.isEmpty() ? sourceNote : weatherCard.note(context, result);

        if (sw == null && current == null) {
            weatherCaption.setText(R.string.weather_title);
            weatherBody.setVisibility(View.GONE);
            weatherMessage.setText(note);
            weatherMessage.setVisibility(View.VISIBLE);
            weatherNote.setVisibility(View.GONE);
            return;
        }

        weatherNote.setText(note);
        weatherNote.setVisibility(note.isEmpty() ? View.GONE : View.VISIBLE);
        weatherMessage.setVisibility(View.GONE);
        weatherBody.setVisibility(View.VISIBLE);

        if (sw != null) {
            weatherCaption.setText(context.getString(R.string.weather_for_session, sessionName(selectedKey)));
            weatherIcon.setImageResource(sw.icon);
            weatherIcon.setColorFilter(color(R.color.hero_text));
            weatherTemp.setText(sw.temp);
            weatherCondition.setText(sw.condition);

            weatherRain.setVisibility(View.VISIBLE);
            weatherRain.setText(context.getString(R.string.sheet_rain) + " " + sw.rain);
            weatherRain.setTextColor(color(sw.wet ? R.color.hero_accent : R.color.hero_text));

            weatherWind.setText(context.getString(R.string.weather_wind_full, sw.wind));

            if (Double.isNaN(sw.humidity)) {
                weatherHumidity.setVisibility(View.GONE);
            } else {
                weatherHumidity.setVisibility(View.VISIBLE);
                weatherHumidity.setText(context.getString(R.string.weather_humidity, (int) Math.round(sw.humidity)));
            }

            return;
        }

        // the weekend is further than the forecast reaches: the weather now at the circuit
        weatherCaption.setText(R.string.weather_title_now);
        weatherIcon.setImageResource(weatherCard.iconFor(current.code));
        weatherIcon.setColorFilter(color(R.color.hero_text));
        weatherTemp.setText(weatherCard.degrees(current.temp));
        weatherCondition.setText(weatherCard.conditionText(context, current.code));

        if (current.rain >= 0) {
            weatherRain.setVisibility(View.VISIBLE);
            weatherRain.setText(context.getString(R.string.weather_rain, current.rain));
            weatherRain.setTextColor(color(current.rain >= RAIN_WARNING ? R.color.hero_accent : R.color.hero_text));
        } else {
            weatherRain.setVisibility(View.GONE);
        }

        weatherWind.setText(context.getString(R.string.weather_wind_full,
                weatherCard.windText(context, current.wind)));

        if (current.humidity >= 0) {
            weatherHumidity.setVisibility(View.VISIBLE);
            weatherHumidity.setText(context.getString(R.string.weather_humidity, current.humidity));
        } else {
            weatherHumidity.setVisibility(View.GONE);
        }
    }

    // ---------- helpers ----------

    private String sessionName(String key) {
        return context.getString(getStringByName(key + "_text"));
    }

    private TextView text(String value, int size, int colorRes, boolean bold) {
        TextView view = new TextView(context);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color(colorRes));
        view.setIncludeFontPadding(false);
        view.setMaxLines(1);
        view.setGravity(Gravity.CENTER);

        if (bold) {
            view.setTypeface(view.getTypeface(), Typeface.BOLD);
        }

        return view;
    }

    private int color(int res) {
        return ContextCompat.getColor(context, res);
    }

    private int dp(float value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }
}