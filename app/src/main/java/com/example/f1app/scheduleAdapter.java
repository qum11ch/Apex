package com.example.f1app;

import static com.example.f1app.MainActivity.getStringByName;

import android.app.Activity;
import android.graphics.drawable.GradientDrawable;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** The weekend as a timeline grouped by day: time, state, results and weather of every session. */
public class scheduleAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public interface SessionClick {
        void onClick(String eventKey);
    }

    public interface WeatherClick {
        void onClick(String eventKey, sessionWeather weather);
    }

    private static final int TYPE_DAY = 0;
    private static final int TYPE_SESSION = 1;
    private static final long HOUR = 3_600_000L;
    private static final long SPRINT_QUALI = 44 * 60_000L;

    private static final class Row {
        final String day;
        final scheduleData data;
        final long start, end;

        Row(String day) {
            this.day = day;
            this.data = null;
            this.start = 0;
            this.end = 0;
        }

        Row(scheduleData data, long start, long end) {
            this.day = null;
            this.data = data;
            this.start = start;
            this.end = end;
        }

        boolean header() {
            return day != null;
        }
    }

    private final Activity context;
    private final List<scheduleData> dataList;
    private final boolean isPast;
    private final List<Row> rows = new ArrayList<>();
    private Map<String, sessionWeather> weather = new HashMap<>();
    private SessionClick click;
    private WeatherClick weatherClick;

    public scheduleAdapter(Activity context, List<scheduleData> datum, boolean isPast) {
        this.context = context;
        this.dataList = datum;
        this.isPast = isPast;

        registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onChanged() {
                rebuild();
            }
        });

        rebuild();
    }

    public void setWeather(Map<String, sessionWeather> byKey) {
        weather = byKey == null ? new HashMap<>() : byKey;
        notifyDataSetChanged();
    }

    public void setOnSessionClick(SessionClick listener) {
        click = listener;
        notifyDataSetChanged();
    }

    public void setOnWeatherClick(WeatherClick listener) {
        weatherClick = listener;
        notifyDataSetChanged();
    }

    // ---------- rows ----------

    private void rebuild() {
        rows.clear();

        List<Row> sessions = new ArrayList<>();

        for (scheduleData item : dataList) {
            Long start = startMillis(item.getEventDate());

            if (start != null) {
                sessions.add(new Row(item, start, start + durationOf(item.getEventName())));
            }
        }

        sessions.sort((a, b) -> Long.compare(a.start, b.start));

        DateTimeFormatter format = DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.getDefault());
        LocalDate lastDay = null;

        for (Row row : sessions) {
            ZonedDateTime time = Instant.ofEpochMilli(row.start).atZone(ZoneId.systemDefault());
            LocalDate day = time.toLocalDate();

            if (!day.equals(lastDay)) {
                String label = time.format(format);
                rows.add(new Row(label.substring(0, 1).toUpperCase(Locale.getDefault()) + label.substring(1)));
                lastDay = day;
            }

            rows.add(row);
        }
    }

    private static Long startMillis(String eventDate) {
        try {
            return Instant.parse(eventDate.trim().replaceAll("\\s+", "T")).toEpochMilli();
        } catch (Exception error) {
            return null;
        }
    }

    private static long durationOf(String key) {
        if ("race_event".equals(key)) {
            return 2 * HOUR;
        }

        return "sprint_quali_event".equals(key) ? SPRINT_QUALI : HOUR;
    }

    private long nextStart(long now) {
        long best = Long.MAX_VALUE;

        for (Row row : rows) {
            if (!row.header() && row.start > now && row.start < best) {
                best = row.start;
            }
        }

        return best;
    }

    // ---------- adapter ----------

    @Override
    public int getItemViewType(int position) {
        return rows.get(position).header() ? TYPE_DAY : TYPE_SESSION;
    }

    @Override
    public int getItemCount() {
        return rows.size();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(context);

        if (viewType == TYPE_DAY) {
            return new DayHolder(inflater.inflate(R.layout.item_schedule_day, parent, false));
        }

        return new SessionHolder(inflater.inflate(R.layout.item_schedule, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Row row = rows.get(position);
        int line = color(R.color.hero_card_stroke);

        if (holder instanceof DayHolder) {
            DayHolder day = (DayHolder) holder;
            day.dayText.setText(row.day);
            day.railTop.setBackgroundColor(line);
            day.railBottom.setBackgroundColor(line);
            day.railTop.setVisibility(position == 0 ? View.INVISIBLE : View.VISIBLE);
            return;
        }

        SessionHolder h = (SessionHolder) holder;
        String key = row.data.getEventName();
        long now = System.currentTimeMillis();
        boolean live = now >= row.start && now < row.end;
        boolean finished = now >= row.end;
        boolean next = !isPast && !live && !finished && row.start == nextStart(now);
        boolean race = "race_event".equals(key);
        // only the finished sessions of a weekend that goes on are dimmed
        boolean dim = finished && !isPast;

        ZonedDateTime start = Instant.ofEpochMilli(row.start).atZone(ZoneId.systemDefault());
        DateTimeFormatter time = DateTimeFormatter.ofPattern("HH:mm");

        h.time.setText(start.format(time));
        h.time.setTextColor(color(dim ? R.color.hero_text_secondary : R.color.hero_text));

        if (race) {
            h.duration.setVisibility(View.GONE);
        } else {
            h.duration.setVisibility(View.VISIBLE);
            h.duration.setText(context.getString(R.string.session_until,
                    Instant.ofEpochMilli(row.end).atZone(ZoneId.systemDefault()).format(time)));
        }

        h.name.setText(context.getString(getStringByName(key + "_text")));
        h.name.setTextSize(race ? 18 : 16);
        h.name.setTextColor(color(dim ? R.color.hero_text_secondary : R.color.hero_text));

        if (live) {
            h.status.setText(R.string.live_badge);
            h.status.setVisibility(View.VISIBLE);
        } else if (next) {
            h.status.setText(inText(row.start - now));
            h.status.setVisibility(View.VISIBLE);
        } else {
            h.status.setVisibility(View.GONE);
        }

        CharSequence results = row.data.getResultsText();
        boolean hasResults = results != null && results.length() > 0;

        if (hasResults) {
            h.results.setText(results);
            h.results.setVisibility(View.VISIBLE);
        } else {
            h.results.setVisibility(View.GONE);
        }

        sessionWeather w = weather.get(key);

        if (w != null) {
            h.weatherIcon.setImageResource(w.icon);
            h.weatherIcon.setColorFilter(color(dim ? R.color.hero_text_secondary : R.color.hero_text));
            h.temp.setText(w.temp);
            h.temp.setTextColor(color(dim ? R.color.hero_text_secondary : R.color.hero_text));

            SpannableStringBuilder detail = new SpannableStringBuilder(w.rain);

            if (w.wet) {
                detail.setSpan(new ForegroundColorSpan(color(R.color.hero_accent)),
                        0, w.rain.length(), 0);
            }

            detail.append(", ").append(w.wind);
            h.detail.setText(detail);
            h.detail.setTextColor(color(R.color.hero_text_secondary));
            h.weatherBox.setVisibility(View.VISIBLE);

            boolean tappable = weatherClick != null;
            h.weatherBox.setOnClickListener(tappable ? v -> weatherClick.onClick(key, w) : null);
            h.weatherBox.setClickable(tappable);
        } else {
            h.weatherBox.setVisibility(View.GONE);
        }

        h.railTop.setBackgroundColor(line);
        h.railBottom.setBackgroundColor(line);
        h.railTop.setVisibility(View.VISIBLE);
        h.railBottom.setVisibility(position == rows.size() - 1 ? View.INVISIBLE : View.VISIBLE);
        styleDot(h.dot, race, live, next, finished);

        boolean clickable = click != null && hasResults;
        h.itemView.setOnClickListener(clickable ? v -> click.onClick(key) : null);
        h.itemView.setClickable(clickable);
    }

    private void styleDot(View dot, boolean race, boolean live, boolean next, boolean finished) {
        float density = context.getResources().getDisplayMetrics().density;
        int size = Math.round((race ? 14 : 10) * density);
        ViewGroup.LayoutParams params = dot.getLayoutParams();
        params.width = size;
        params.height = size;
        dot.setLayoutParams(params);

        GradientDrawable shape = new GradientDrawable();
        shape.setShape(GradientDrawable.OVAL);
        int stroke = Math.round(2 * density);

        if (live || (finished && race)) {
            shape.setColor(color(R.color.hero_accent));
        } else if (finished) {
            shape.setColor(color(R.color.hero_text_secondary));
        } else {
            shape.setColor(color(R.color.hero_card_bg));
            shape.setStroke(stroke, color(next || race ? R.color.hero_accent : R.color.hero_text_secondary));
        }

        dot.setBackground(shape);
    }

    private String inText(long millis) {
        long minutes = Math.max(1, millis / 60_000L);

        if (minutes >= 24 * 60) {
            return context.getString(R.string.in_days, minutes / (24 * 60));
        }

        if (minutes >= 60) {
            return context.getString(R.string.in_hours, minutes / 60);
        }

        return context.getString(R.string.in_minutes, minutes);
    }

    private int color(int res) {
        return ContextCompat.getColor(context, res);
    }

    // ---------- holders ----------

    static class DayHolder extends RecyclerView.ViewHolder {
        final TextView dayText;
        final View railTop, railBottom;

        DayHolder(@NonNull View itemView) {
            super(itemView);
            dayText = itemView.findViewById(R.id.dayText);
            railTop = itemView.findViewById(R.id.railTop);
            railBottom = itemView.findViewById(R.id.railBottom);
        }
    }

    static class SessionHolder extends RecyclerView.ViewHolder {
        final TextView time, duration, name, status, results, temp, detail;
        final ImageView weatherIcon;
        final View weatherBox, railTop, railBottom, dot;

        SessionHolder(@NonNull View itemView) {
            super(itemView);
            time = itemView.findViewById(R.id.eventTime);
            duration = itemView.findViewById(R.id.eventDuration);
            name = itemView.findViewById(R.id.eventName);
            status = itemView.findViewById(R.id.eventStatus);
            results = itemView.findViewById(R.id.eventResults);
            temp = itemView.findViewById(R.id.eventTemp);
            detail = itemView.findViewById(R.id.eventWeatherDetail);
            weatherIcon = itemView.findViewById(R.id.eventWeatherIcon);
            weatherBox = itemView.findViewById(R.id.weatherBox);
            railTop = itemView.findViewById(R.id.railTop);
            railBottom = itemView.findViewById(R.id.railBottom);
            dot = itemView.findViewById(R.id.eventDot);
        }
    }
}