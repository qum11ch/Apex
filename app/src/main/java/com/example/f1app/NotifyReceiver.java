package com.example.f1app;

import static com.example.f1app.MainActivity.getStringByName;
import static com.example.f1app.teamsStandingsActivity.localizeLocality;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.TaskStackBuilder;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class NotifyReceiver extends BroadcastReceiver {
    static final String CHANNEL_START = "session_start";
    static final String CHANNEL_END = "session_end";
    private static final String LEGACY_CHANNEL = "channelID2";

    private static final class Job {
        final Context context;
        final PendingResult result;
        final String season, raceName, circuitId, eventKey;
        final boolean start;
        final long startMillis;
        final int leadMinutes, notificationId;
        final DatabaseReference root = FirebaseDatabase.getInstance().getReference();

        int round;
        String dateStart, dateEnd;
        String circuitName, country, location;
        int openPredictions;

        Job(Context context, PendingResult result, Bundle extras) {
            this.context = context;
            this.result = result;
            season = extras.getString("season");
            raceName = extras.getString("raceName");
            circuitId = extras.getString("circuitId");
            eventKey = extras.getString("eventKey");
            start = extras.getBoolean("start", true);
            startMillis = extras.getLong("startMillis", System.currentTimeMillis());
            leadMinutes = extras.getInt("leadMinutes", 15);
            notificationId = extras.getInt("notificationId", 1);
        }
    }

    @SuppressLint("UnsafeProtectedBroadcastReceiver")
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            try {
                reminderSettings.reschedule(context);
            } catch (RuntimeException error) {
                Log.w("NotifyReceiver", "Cannot restart reminders after boot", error);
            }
            return;
        }

        Bundle extras = intent.getExtras();

        if (extras == null
                || extras.getString("eventKey") == null
                || extras.getString("season") == null
                || extras.getString("raceName") == null
                || extras.getString("circuitId") == null) {
            return;
        }

        FirebaseApp.initializeApp(context);
        createChannels(context);

        loadRace(new Job(context, goAsync(), extras));
    }

    // ---------- data ----------

    private void loadRace(Job job) {
        job.root.child("schedule").child("season").child(job.season).child(job.raceName)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        String first = snapshot.child("FirstPractice/firstPracticeDate")
                                .getValue(String.class);
                        String raceDate = snapshot.child("raceDate").getValue(String.class);
                        Integer round = snapshot.child("round").getValue(Integer.class);

                        boolean canceled = Boolean.TRUE.equals(
                                snapshot.child("Canceled").getValue(Boolean.class));

                        if (first == null || raceDate == null || round == null || canceled) {
                            job.result.finish();
                            return;
                        }

                        job.dateStart = first;
                        job.dateEnd = raceDate;
                        job.round = round;
                        loadCircuit(job);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        job.result.finish();
                    }
                });
    }

    private void loadCircuit(Job job) {
        job.root.child("circuits").child(job.circuitId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot circuit) {
                        job.circuitName = circuit.child("circuitName").getValue(String.class);
                        job.country = circuit.child("country").getValue(String.class);
                        job.location = circuit.child("location").getValue(String.class);

                        if (job.start) {
                            countPredictions(job);
                        } else {
                            loadImage(job);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        job.result.finish();
                    }
                });
    }

    /** Open predictions of this weekend that the user has not answered yet. */
    private void countPredictions(Job job) {
        job.root.child("userPredictions").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                long now = System.currentTimeMillis();
                List<String> open = new ArrayList<>();

                for (DataSnapshot child : snapshot.getChildren()) {
                    predictionListItem item = child.getValue(predictionListItem.class);

                    if (item == null || !item.isWeekend() || child.getKey() == null) {
                        continue;
                    }

                    if (item.getStatus() != null && !"open".equals(item.getStatus())) {
                        continue;
                    }

                    Long deadline = item.getDeadline();

                    if (deadline == null || deadline <= now
                            || item.getRaceRound() == null
                            || item.getRaceRound() != job.round
                            || !job.season.equals(item.getSeason())) {
                        continue;
                    }

                    open.add(child.getKey());
                }

                subtractAnswered(job, open);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                loadImage(job);
            }
        });
    }

    private void subtractAnswered(Job job, List<String> open) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user == null || open.isEmpty()) {
            job.openPredictions = open.size();
            loadImage(job);
            return;
        }

        job.root.child("userPredictionAnswers").child(user.getUid())
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot answers) {
                        int left = 0;

                        for (String id : open) {
                            if (!answers.hasChild(id)) {
                                left++;
                            }
                        }

                        job.openPredictions = left;
                        loadImage(job);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        job.openPredictions = open.size();
                        loadImage(job);
                    }
                });
    }

    private void loadImage(Job job) {
        GlideApp.with(job.context).asBitmap()
                .load(FirebaseStorage.getInstance().getReference()
                        .child("circuits/" + job.circuitId + ".png"))
                .override(320, 320)
                .into(new CustomTarget<Bitmap>() {
                    @Override
                    public void onResourceReady(@NonNull Bitmap resource,
                                                @Nullable Transition<? super Bitmap> transition) {
                        finishWith(job, resource);
                    }

                    @Override
                    public void onLoadFailed(@Nullable Drawable errorDrawable) {
                        finishWith(job, null);
                    }

                    @Override
                    public void onLoadCleared(@Nullable Drawable placeholder) {
                    }
                });
    }

    private void finishWith(Job job, @Nullable Bitmap bitmap) {
        try {
            post(job, bitmap);
        } catch (RuntimeException error) {
            Log.w("NotifyReceiver", "Cannot show the notification", error);
        } finally {
            job.result.finish();
        }
    }

    // ---------- notification ----------

    private void post(Job job, @Nullable Bitmap bitmap) {
        Context context = job.context;

        String session = context.getString(getStringByName(job.eventKey + "_text"));
        String race = context.getString(getStringByName(
                job.raceName.toLowerCase(Locale.ROOT).replaceAll("\\s+", "_") + "_text"))
                + " " + job.season;

        String line;
        String detail;

        if (job.start) {
            line = context.getResources().getQuantityString(
                    R.plurals.notify_start_line, job.leadMinutes, session, job.leadMinutes);
            detail = line;
        } else {
            line = context.getString(R.string.notify_end_line, session);
            detail = context.getString(R.string.notify_end_detail);
        }

        String place = circuitLine(job);
        Bitmap small = bitmap == null ? null : scaled(bitmap, 96);

        RemoteViews collapsed = sessionViews(job, R.layout.notification_session_collapsed,
                false, race, session, line, place, small);
        RemoteViews expanded = sessionViews(job, R.layout.notification_session_expanded,
                true, race, session, detail, place, bitmap);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(
                context, job.start ? CHANNEL_START : CHANNEL_END)
                .setSmallIcon(R.drawable.ic_stat_session)
                .setColor(ContextCompat.getColor(context, R.color.hero_accent))
                .setContentTitle(race)
                .setContentText(line)
                .setStyle(new NotificationCompat.DecoratedCustomViewStyle())
                .setCustomContentView(collapsed)
                .setCustomBigContentView(expanded)
                .setContentIntent(contentIntent(job))
                .setAutoCancel(true)
                .setCategory(NotificationCompat.CATEGORY_EVENT)
                .setPriority(job.start
                        ? NotificationCompat.PRIORITY_HIGH
                        : NotificationCompat.PRIORITY_DEFAULT);

        if (job.start) {
            builder.setWhen(job.startMillis).setShowWhen(true);

            long timeout = job.startMillis - System.currentTimeMillis();

            if (timeout > 0) {
                builder.setTimeoutAfter(timeout);
            }
        }

        if (job.start && job.openPredictions > 0) {
            expanded.setTextViewText(R.id.notifPrimary,
                    context.getString(R.string.notify_action_predictions, job.openPredictions));
            expanded.setViewVisibility(R.id.notifPrimary, View.VISIBLE);
            expanded.setOnClickPendingIntent(R.id.notifPrimary, predictionsIntent(job));
        }

        expanded.setTextViewText(R.id.notifSecondary,
                context.getString(R.string.notify_action_disable));
        expanded.setOnClickPendingIntent(R.id.notifSecondary, disableIntent(job));

        context.getSystemService(NotificationManager.class)
                .notify(job.notificationId, builder.build());
    }

    private RemoteViews sessionViews(Job job, int layout, boolean expanded, String race,
                                     String session, String line, String place,
                                     @Nullable Bitmap image) {
        RemoteViews views = new RemoteViews(job.context.getPackageName(), layout);
        int imageId = expanded ? R.id.notifCircuitBig : R.id.notifCircuit;

        views.setTextViewText(R.id.notifRace, race);
        views.setTextViewText(R.id.notifLine, line);

        if (image != null) {
            views.setImageViewBitmap(imageId, image);
            views.setViewVisibility(imageId, View.VISIBLE);
        } else {
            views.setViewVisibility(imageId, View.GONE);
        }

        if (expanded) {
            views.setTextViewText(R.id.notifSession, session.toUpperCase(Locale.getDefault()));
            views.setTextViewText(R.id.notifPlace, place);
            views.setViewVisibility(R.id.notifPlace, place.isEmpty() ? View.GONE : View.VISIBLE);
        }

        long untilStart = job.startMillis - System.currentTimeMillis();

        if (job.start && untilStart > 0) {
            views.setChronometerCountDown(R.id.notifCountdown, true);
            views.setChronometer(R.id.notifCountdown,
                    SystemClock.elapsedRealtime() + untilStart, null, true);
            views.setViewVisibility(R.id.notifCountdown, View.VISIBLE);
        } else {
            views.setViewVisibility(R.id.notifCountdown, View.GONE);
        }

        return views;
    }

    private Bitmap scaled(Bitmap source, int box) {
        float ratio = Math.min((float) box / source.getWidth(), (float) box / source.getHeight());

        if (ratio >= 1f) {
            return source;
        }

        return Bitmap.createScaledBitmap(source,
                Math.max(1, Math.round(source.getWidth() * ratio)),
                Math.max(1, Math.round(source.getHeight() * ratio)), true);
    }

    private String circuitLine(Job job) {
        String place = null;

        if (job.country != null && job.location != null) {
            try {
                if ("ru".equals(Locale.getDefault().getLanguage())) {
                    place = localizeLocality(job.location, job.country, job.context).get(2);
                } else if ("Monaco".equals(job.location) || "Singapore".equals(job.location)) {
                    place = job.country;
                } else {
                    place = job.location + ", " + job.country;
                }
            } catch (RuntimeException error) {
                place = job.location + ", " + job.country;
            }
        }

        if (job.circuitName == null) {
            return place == null ? "" : place;
        }

        return place == null ? job.circuitName : job.circuitName + ", " + place;
    }

    private PendingIntent contentIntent(Job job) {
        DateTimeFormatter format = DateTimeFormatter.ofPattern("yyyy-M-d");
        LocalDate start = LocalDate.parse(job.dateStart, format);
        LocalDate end = LocalDate.parse(job.dateEnd, format);

        Intent intent = new Intent(job.context, splashNotificationActivity.class);
        Bundle bundle = new Bundle();
        bundle.putString("raceName", job.raceName);
        bundle.putString("futureRaceStartDay", start.format(DateTimeFormatter.ofPattern("dd")));
        bundle.putString("futureRaceEndDay", end.format(DateTimeFormatter.ofPattern("dd")));
        bundle.putString("futureRaceStartMonth", start.format(DateTimeFormatter.ofPattern("MMM")));
        bundle.putString("futureRaceEndMonth", end.format(DateTimeFormatter.ofPattern("MMM")));
        bundle.putString("roundCount", String.valueOf(job.round));
        bundle.putString("raceCountry", job.country);
        bundle.putString("circuitId", job.circuitId);
        bundle.putString("dateStart", job.dateStart);
        bundle.putString("dateEnd", job.dateEnd);
        bundle.putBoolean("isCanceled", false);
        bundle.putBoolean("fromNotify", true);
        intent.putExtras(bundle);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        return PendingIntent.getActivity(job.context, job.notificationId * 10 + 1, intent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private PendingIntent predictionsIntent(Job job) {
        Intent predictions = new Intent(job.context, predictionActivity.class);
        predictions.putExtra("season", job.season);
        predictions.putExtra("raceRound", job.round);
        predictions.putExtra("dismissNotification", job.notificationId);

        return TaskStackBuilder.create(job.context)
                .addNextIntent(new Intent(job.context, MainActivity.class))
                .addNextIntent(predictions)
                .getPendingIntent(job.notificationId * 10 + 2,
                        PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private PendingIntent disableIntent(Job job) {
        Intent intent = new Intent(job.context, BootService.class);
        intent.putExtra("disable", true);
        intent.putExtra("notificationId", job.notificationId);

        return PendingIntent.getService(job.context, job.notificationId * 10 + 3, intent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private void createChannels(Context context) {
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        manager.deleteNotificationChannel(LEGACY_CHANNEL);

        NotificationChannel start = new NotificationChannel(CHANNEL_START,
                context.getString(R.string.notify_channel_start),
                NotificationManager.IMPORTANCE_HIGH);
        start.setDescription(context.getString(R.string.notify_channel_start_desc));
        start.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        manager.createNotificationChannel(start);

        NotificationChannel end = new NotificationChannel(CHANNEL_END,
                context.getString(R.string.notify_channel_end),
                NotificationManager.IMPORTANCE_DEFAULT);
        end.setDescription(context.getString(R.string.notify_channel_end_desc));
        end.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        manager.createNotificationChannel(end);
    }
}