package com.example.f1app;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

final class widgetFavoritesLoader {

    enum State {
        SIGNED_OUT,
        LOADED,
        PROFILE_MISSING,
        ERROR,
        ACCOUNT_CHANGED
    }

    static final class Result {
        final State state;
        final String uid;
        final String driver;
        final String team;

        Result(State state, String uid, String driver, String team) {
            this.state = state;
            this.uid = uid;
            this.driver = driver;
            this.team = team;
        }

        boolean hasDriver() {
            return !driver.isEmpty();
        }

        boolean hasTeam() {
            return !team.isEmpty();
        }
    }

    interface Callback {
        void onLoaded(Result result);
    }

    private static final long TIMEOUT_MS = 15_000L;

    private widgetFavoritesLoader() {
    }

    /** Call on the main thread. The callback is also delivered there. */
    static void load(Callback callback) {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            new Handler(Looper.getMainLooper()).post(() -> load(callback));
            return;
        }

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user == null) {
            callback.onLoaded(new Result(State.SIGNED_OUT, "", "", ""));
            return;
        }

        new Read(user.getUid(), callback).start();
    }

    private static final class Read implements ValueEventListener {
        final String uid;
        final Callback callback;
        final DatabaseReference reference;
        final Handler main = new Handler(Looper.getMainLooper());
        final Runnable timeout;
        boolean finished;

        Read(String uid, Callback callback) {
            this.uid = uid;
            this.callback = callback;
            reference = FirebaseDatabase.getInstance()
                    .getReference("users").child(uid);
            timeout = () -> finish(State.ERROR, "", "");
        }

        void start() {
            main.postDelayed(timeout, TIMEOUT_MS);
            reference.addListenerForSingleValueEvent(this);
        }

        @Override
        public void onDataChange(@NonNull DataSnapshot snapshot) {
            if (!snapshot.exists()) {
                finish(State.PROFILE_MISSING, "", "");
                return;
            }

            finish(
                    State.LOADED,
                    choice(snapshot.child("choiceDriver").getValue()),
                    choice(snapshot.child("choiceTeam").getValue())
            );
        }

        @Override
        public void onCancelled(@NonNull DatabaseError error) {
            android.util.Log.w(
                    "WidgetFavorites",
                    "Profile read failed: " + error.getCode(),
                    error.toException()
            );
            finish(State.ERROR, "", "");
        }

        void finish(State state, String driver, String team) {
            if (finished) {
                return;
            }

            finished = true;
            main.removeCallbacks(timeout);
            reference.removeEventListener(this);

            FirebaseUser current = FirebaseAuth.getInstance().getCurrentUser();

            if (current == null) {
                callback.onLoaded(new Result(State.SIGNED_OUT, "", "", ""));
                return;
            }

            if (!uid.equals(current.getUid())) {
                callback.onLoaded(new Result(State.ACCOUNT_CHANGED, "", "", ""));
                return;
            }

            callback.onLoaded(new Result(state, uid, driver, team));
        }
    }

    private static String choice(Object raw) {
        if (!(raw instanceof String)) {
            return "";
        }

        String value = ((String) raw).trim();

        return value.isEmpty() || "null".equalsIgnoreCase(value)
                ? "" : value;
    }
}