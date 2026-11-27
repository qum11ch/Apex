package com.example.f1app;

import static com.example.f1app.MainActivity.APP_PREFERENCES;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.text.format.DateUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.app.TaskStackBuilder;
import androidx.core.content.ContextCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.google.android.gms.tasks.Tasks;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.FirebaseDatabase;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import android.view.View;
import android.widget.RemoteViews;

public class predictionWatchWorker extends Worker {
    static final String KEY_SEEN = "seen_open_predictions";
    private static final String CHANNEL_ID = "prediction_open";
    private static final int NOTIFICATION_ID = 3000;
    private static final int QUIET_FROM = 22;
    private static final int QUIET_TO = 9;

    public predictionWatchWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context context = getApplicationContext();
        boolean force = getInputData().getBoolean("force", false);

        if (!reminderSettings.predictionAlertsEnabled(context)) {
            return Result.success();
        }

        int hour = LocalTime.now().getHour();

        if (!force && (hour >= QUIET_FROM || hour < QUIET_TO)) {
            return Result.success(); // not marked as seen: the next check reports them
        }

        DataSnapshot snapshot;

        try {
            snapshot = Tasks.await(
                    FirebaseDatabase.getInstance().getReference("userPredictions").get(),
                    30, TimeUnit.SECONDS);
        } catch (TimeoutException error) {
            return Result.retry();
        } catch (ExecutionException | InterruptedException error) {
            Log.w("PredictionWatch", "Cannot read the predictions", error);
            return Result.success();
        }

        long now = System.currentTimeMillis();
        Map<String, predictionListItem> open = new LinkedHashMap<>();

        for (DataSnapshot child : snapshot.getChildren()) {
            predictionListItem item = child.getValue(predictionListItem.class);

            if (item == null || child.getKey() == null) {
                continue;
            }

            if (item.getStatus() != null && !"open".equals(item.getStatus())) {
                continue;
            }

            Long deadline = item.getDeadline();

            if (deadline == null || deadline <= now) {
                continue;
            }

            item.setPredictionId(child.getKey());
            open.put(child.getKey(), item);
        }

        SharedPreferences prefs = context.getSharedPreferences(APP_PREFERENCES, Context.MODE_PRIVATE);
        Set<String> storedSeen = prefs.getStringSet(KEY_SEEN, null);

        if (storedSeen != null) {
            Set<String> seen = new HashSet<>(storedSeen);
            List<predictionListItem> fresh = new ArrayList<>();

            for (Map.Entry<String, predictionListItem> entry : open.entrySet()) {
                if (!seen.contains(entry.getKey())) {
                    fresh.add(entry.getValue());
                }
            }

            if (!fresh.isEmpty()
                    && NotificationManagerCompat.from(context).areNotificationsEnabled()) {
                fresh.sort((a, b) -> Long.compare(a.getDeadline(), b.getDeadline()));
                show(context, fresh);
            }
        }

        // first run: only remember what is open; later: forget the closed ones
        prefs.edit().putStringSet(KEY_SEEN, new HashSet<>(open.keySet())).apply();

        return Result.success();
    }

    private void show(Context context, List<predictionListItem> fresh) {
        NotificationManager manager = context.getSystemService(NotificationManager.class);

        NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                context.getString(R.string.notify_channel_predictions),
                NotificationManager.IMPORTANCE_DEFAULT);
        channel.setDescription(context.getString(R.string.notify_channel_predictions_desc));
        manager.createNotificationChannel(channel);

        predictionListItem first = fresh.get(0);
        String title = context.getString(fresh.size() == 1
                ? R.string.notify_pred_title_one : R.string.notify_pred_title_many);
        String count = String.valueOf(fresh.size());

        RemoteViews collapsed = new RemoteViews(context.getPackageName(),
                R.layout.notification_predictions_collapsed);
        collapsed.setTextViewText(R.id.notifBadge, count);
        collapsed.setTextViewText(R.id.notifTitle, title);
        collapsed.setTextViewText(R.id.notifLine, first.getLocalizedTitle());

        RemoteViews expanded = new RemoteViews(context.getPackageName(),
                R.layout.notification_predictions_expanded);
        expanded.setTextViewText(R.id.notifBadge, count);
        expanded.setTextViewText(R.id.notifTitle, title);
        expanded.removeAllViews(R.id.notifRows);

        int shown = Math.min(fresh.size(), 3);

        for (int i = 0; i < shown; i++) {
            predictionListItem item = fresh.get(i);
            RemoteViews row = new RemoteViews(context.getPackageName(),
                    R.layout.notification_prediction_row);

            row.setTextViewText(R.id.rowTitle, item.getLocalizedTitle());
            row.setTextViewText(R.id.rowDeadline, context.getString(R.string.notify_pred_row_deadline,
                    DateUtils.formatDateTime(context, item.getDeadline(),
                            DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_TIME
                                    | DateUtils.FORMAT_ABBREV_MONTH)));

            String points = points(item.getPoints());
            row.setTextViewText(R.id.rowPoints, points);
            row.setViewVisibility(R.id.rowPoints, points.isEmpty() ? View.GONE : View.VISIBLE);

            expanded.addView(R.id.notifRows, row);
        }

        if (fresh.size() > shown) {
            expanded.setTextViewText(R.id.notifMore,
                    context.getString(R.string.notify_pred_more, fresh.size() - shown));
            expanded.setViewVisibility(R.id.notifMore, View.VISIBLE);
        } else {
            expanded.setViewVisibility(R.id.notifMore, View.GONE);
        }

        PendingIntent open = TaskStackBuilder.create(context)
                .addNextIntent(new Intent(context, MainActivity.class))
                .addNextIntent(new Intent(context, predictionActivity.class))
                .getPendingIntent(NOTIFICATION_ID,
                        PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        manager.notify(NOTIFICATION_ID, new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_stat_session)
                .setColor(ContextCompat.getColor(context, R.color.hero_accent))
                .setContentTitle(title)
                .setContentText(first.getLocalizedTitle())
                .setStyle(new NotificationCompat.DecoratedCustomViewStyle())
                .setCustomContentView(collapsed)
                .setCustomBigContentView(expanded)
                .setContentIntent(open)
                .setAutoCancel(true)
                .setCategory(NotificationCompat.CATEGORY_RECOMMENDATION)
                .build());
    }

    private static String points(Double value) {
        if (value == null || value <= 0) {
            return "";
        }

        return value == Math.rint(value) ? "+" + value.longValue() : "+" + value;
    }
}