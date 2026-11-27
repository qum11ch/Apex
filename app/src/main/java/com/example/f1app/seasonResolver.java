package com.example.f1app;

import androidx.annotation.NonNull;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The season the app shows. It is the season of status/season, unless that season is over by the calendar
 * and the new one is ready in the database (its calendar and its line-up): then the new one counts.
 */
final class seasonResolver {

    interface Callback {
        void onResolved(String season);
    }

    private seasonResolver() {
    }

    static void resolve(String statusSeason, Callback callback) {
        final String status = statusSeason == null ? "" : statusSeason.trim();
        final int calendar = LocalDate.now().getYear();
        int statusYear;

        try {
            statusYear = Integer.parseInt(status);
        } catch (NumberFormatException error) {
            callback.onResolved(status);
            return;
        }

        if (statusYear >= calendar) {
            callback.onResolved(status);
            return;
        }

        final String next = String.valueOf(calendar);
        final AtomicInteger ready = new AtomicInteger(0);
        final AtomicInteger answered = new AtomicInteger(0);

        ValueEventListener listener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.hasChildren()) {
                    ready.incrementAndGet();
                }

                finish();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                finish();
            }

            private void finish() {
                if (answered.incrementAndGet() == 2) {
                    callback.onResolved(ready.get() == 2 ? next : status);
                }
            }
        };

        FirebaseDatabase db = FirebaseDatabase.getInstance();
        db.getReference().child("schedule").child("season").child(next).addListenerForSingleValueEvent(listener);
        db.getReference().child("driverLineUp").child("season").child(next).addListenerForSingleValueEvent(listener);
    }
}