package com.example.f1app;

import static com.example.f1app.MainActivity.getStringByName;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.TextView;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

public final class ongoingStatusView {

    private static final int LIVE_RED = Color.parseColor("#E10600");

    private final View container;
    private final View dot;
    private final TextView status;
    private final Handler handler = new Handler(Looper.getMainLooper());

    private Runnable refresh;
    private ObjectAnimator pulse;

    private List<raceWeekendStatus.SessionInfo> sessions;
    private boolean awaitingResults;
    private boolean bound;

    public ongoingStatusView(View container, View dot, TextView status) {
        this.container = container;
        this.dot = dot;
        this.status = status;

        // Уходим с экрана — останавливаемся; возвращаемся — пересчитываем.
        container.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
            @Override
            public void onViewAttachedToWindow(View v) {
                if (bound) render();
            }

            @Override
            public void onViewDetachedFromWindow(View v) {
                stopTimers();
            }
        });
    }

    public void bind(List<raceWeekendStatus.SessionInfo> sessions, boolean awaitingResults) {
        this.sessions = sessions;
        this.awaitingResults = awaitingResults;
        this.bound = true;
        container.setVisibility(View.VISIBLE);
        render();
    }

    public void clear() {
        bound = false;
        stopTimers();
        container.setVisibility(View.GONE);
    }

    private void render() {
        stopTimers();
        if (!bound || sessions == null) return;

        Context context = status.getContext();
        long now = System.currentTimeMillis();

        raceWeekendStatus.SessionInfo live =
                raceWeekendStatus.liveSession(sessions, now);
        raceWeekendStatus.SessionInfo next =
                raceWeekendStatus.nextSession(sessions, now);

        if (!awaitingResults && live != null) {
            container.setBackgroundResource(R.drawable.bg_live_pill);
            status.setText(context.getString(getStringByName(live.key + "_text")));
            status.setTextColor(LIVE_RED);
            status.setAlpha(1f);
            dot.setVisibility(View.VISIBLE);
            startPulse();
        } else {
            container.setBackground(null);
            status.setTextColor(Color.WHITE);
            status.setAlpha(0.85f);
            dot.setVisibility(View.GONE);

            if (!awaitingResults && next != null) {
                String name = context.getString(getStringByName(next.key + "_text"));
                String time = Instant.ofEpochMilli(next.start)
                        .atZone(ZoneId.systemDefault())
                        .format(DateTimeFormatter.ofPattern("EEE HH:mm", Locale.getDefault()));
                status.setText(context.getString(R.string.next_session_text, name, time));
            } else {
                status.setText(R.string.awaiting_results_text);
            }
        }

        long nextChange = Long.MAX_VALUE;
        if (live != null) nextChange = Math.min(nextChange, live.end);
        if (next != null) nextChange = Math.min(nextChange, next.start);

        if (nextChange != Long.MAX_VALUE) {
            refresh = this::render;
            handler.postDelayed(refresh, Math.max(1000, nextChange - now + 1000));
        }
    }

    private void startPulse() {
        pulse = ObjectAnimator.ofFloat(dot, "alpha", 1f, 0.45f);
        pulse.setDuration(800);
        pulse.setRepeatMode(ValueAnimator.REVERSE);
        pulse.setRepeatCount(ValueAnimator.INFINITE);
        pulse.start();
    }

    private void stopTimers() {
        if (refresh != null) {
            handler.removeCallbacks(refresh);
            refresh = null;
        }
        if (pulse != null) {
            pulse.cancel();
            pulse = null;
        }
        dot.setAlpha(1f);
    }
}