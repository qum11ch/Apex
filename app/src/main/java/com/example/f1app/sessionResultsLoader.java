package com.example.f1app;

import android.content.Context;
import android.util.Log;

import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONArray;

import java.time.Instant;
import java.util.List;

public final class sessionResultsLoader {

    private sessionResultsLoader() {}

    public static void load(
            Fragment fragment,
            String season,
            String round,
            List<scheduleData> datum,
            RecyclerView.Adapter<?> adapter) {

        Context context = fragment.requireContext().getApplicationContext();
        long now = System.currentTimeMillis();

        jolpicaSessions.Callback callback = new jolpicaSessions.Callback() {
            @Override
            public void onResults(jolpicaSessions.Session session, JSONArray results) {
                // Фрагмент мог быть закрыт, пока шёл запрос.
                if (!fragment.isAdded() || fragment.getView() == null) return;

                String key = eventKey(session);

                for (int i = 0; i < datum.size(); i++) {
                    if (key.equals(datum.get(i).getEventName())) {
                        int codeColor = ContextCompat.getColor(context, R.color.text_color_main);

                        datum.get(i).setResultsText(
                                jolpicaSessions.podiumText(results, codeColor)
                        );
                        adapter.notifyItemChanged(i);
                        break;
                    }
                }
            }

            @Override
            public void onNoResults(jolpicaSessions.Session session) {
                // Результатов ещё нет — строка остаётся без подиума.
            }

            @Override
            public void onError(jolpicaSessions.Session session, Exception error) {
                Log.w("SessionResults", "Не удалось загрузить " + session, error);
            }
        };

        for (jolpicaSessions.Session session : jolpicaSessions.Session.values()) {
            scheduleData event = findEvent(datum, eventKey(session));

            // Нет такой сессии в уикенде (например, спринта) — не запрашиваем.
            if (event == null) continue;

            // Сессия ещё не началась — результатов быть не может.
            Long start = startMillis(event.getEventDate());
            if (start != null && now < start) continue;

            jolpicaSessions.load(context, season, round, session, callback);
        }
    }

    private static String eventKey(jolpicaSessions.Session session) {
        switch (session) {
            case QUALIFYING: return "quali_event";
            case SPRINT:     return "sprint_event";
            default:         return "race_event";
        }
    }

    private static scheduleData findEvent(List<scheduleData> datum, String key) {
        for (scheduleData item : datum) {
            if (key.equals(item.getEventName())) return item;
        }
        return null;
    }

    private static Long startMillis(String eventDate) {
        try {
            // Тот же формат, что разбирает scheduleAdapter: "yyyy-MM-dd HH:mm:ssZ".
            return Instant.parse(eventDate.trim().replaceAll("\\s+", "T"))
                    .toEpochMilli();
        } catch (Exception e) {
            return null;
        }
    }
}