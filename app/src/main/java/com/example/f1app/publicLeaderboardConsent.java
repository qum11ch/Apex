package com.example.f1app;

import android.app.Dialog;
import android.os.Looper;
import android.view.ViewGroup;
import android.view.Window;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.functions.FirebaseFunctions;
import com.google.firebase.functions.HttpsCallableResult;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/** Consent gate placed after form validation, immediately before submission. */
public final class publicLeaderboardConsent {

    private static final int NOTICE_VERSION = 1;

    private publicLeaderboardConsent() {
    }

    public static Task<HttpsCallableResult> submit(
            AppCompatActivity activity,
            FirebaseFunctions functions,
            Map<String, Object> prediction
    ) {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            return Tasks.forException(new IllegalStateException("Main thread required"));
        }

        Gate gate = new Gate(activity, functions, prediction);
        gate.start();
        return gate.completion.getTask();
    }

    public static boolean isCancelled(Exception error) {
        return error instanceof ConsentCancelledException;
    }

    private static final class ConsentCancelledException extends Exception {

        private ConsentCancelledException() {
            super("Prediction submission cancelled before completion");
        }
    }

    private static final class Gate implements DefaultLifecycleObserver {

        private final AppCompatActivity activity;
        private final FirebaseFunctions functions;
        private final Map<String, Object> prediction;
        private final String ownerUid;
        private final TaskCompletionSource<HttpsCallableResult> completion =
                new TaskCompletionSource<>();

        private Dialog dialog;
        private FirebaseAuth.AuthStateListener authListener;
        private boolean sending;

        private Gate(
                AppCompatActivity activity,
                FirebaseFunctions functions,
                Map<String, Object> prediction
        ) {
            this.activity = activity;
            this.functions = functions;
            this.prediction = new HashMap<>(prediction);
            ownerUid = FirebaseAuth.getInstance().getUid();
            this.prediction.remove("publicLeaderboardConsent");
        }

        private void start() {
            if (!active()) {
                cancel();
                return;
            }

            activity.getLifecycle().addObserver(this);

            authListener = auth -> {
                if (!Objects.equals(ownerUid, auth.getUid())) {
                    cancel();
                }
            };
            FirebaseAuth.getInstance().addAuthStateListener(authListener);

            completion.getTask().addOnCompleteListener(task -> cleanup());

            functions.getHttpsCallable("getPublicLeaderboardConsent")
                    .call(Collections.emptyMap())
                    .addOnSuccessListener(result -> {
                        if (!active()) {
                            cancel();
                            return;
                        }

                        Object raw = result.getData();

                        if (!(raw instanceof Map)) {
                            fail(new IllegalStateException("Invalid consent response"));
                            return;
                        }

                        Map<?, ?> value = (Map<?, ?>) raw;
                        Object version = value.get("noticeVersion");
                        Object accepted = value.get("accepted");

                        if (!(version instanceof Number)
                                || ((Number) version).doubleValue() != NOTICE_VERSION) {
                            fail(new IllegalStateException(
                                    activity.getString(R.string.leaderboard_consent_update_required)
                            ));
                            return;
                        }

                        if (!(accepted instanceof Boolean)) {
                            fail(new IllegalStateException("Missing consent status"));
                            return;
                        }

                        if (Boolean.TRUE.equals(accepted)) {
                            send(false);
                        } else {
                            showNotice();
                        }
                    })
                    .addOnFailureListener(this::fail);
        }

        private boolean active() {
            return !completion.getTask().isComplete()
                    && ownerUid != null
                    && Objects.equals(ownerUid, FirebaseAuth.getInstance().getUid())
                    && !activity.isFinishing()
                    && !activity.isDestroyed()
                    && activity.getLifecycle().getCurrentState()
                    .isAtLeast(Lifecycle.State.STARTED);
        }

        private void showNotice() {
            if (!active()) {
                cancel();
                return;
            }

            dialog = new Dialog(activity);
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            dialog.setContentView(R.layout.dialog_public_leaderboard_consent);
            dialog.setCanceledOnTouchOutside(true);
            dialog.setOnCancelListener(ignored -> cancel());

            dialog.findViewById(R.id.leaderboardConsentCancel)
                    .setOnClickListener(view -> cancel());

            dialog.findViewById(R.id.leaderboardConsentAccept)
                    .setOnClickListener(view -> {
                        if (!active() || sending) {
                            return;
                        }

                        dialog.findViewById(R.id.leaderboardConsentAccept)
                                .setEnabled(false);
                        dialog.findViewById(R.id.leaderboardConsentCancel)
                                .setEnabled(false);
                        dialog.dismiss();
                        send(true);
                    });

            dialog.show();
            Window window = dialog.getWindow();

            if (window != null) {
                window.setBackgroundDrawableResource(android.R.color.transparent);
                int margin = Math.round(32 * activity.getResources().getDisplayMetrics().density);
                int width = Math.max(
                        1,
                        activity.getResources().getDisplayMetrics().widthPixels - margin
                );
                window.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT);
            }
        }

        private void send(boolean acceptedNow) {
            if (!active()) {
                cancel();
                return;
            }

            if (sending) {
                return;
            }

            sending = true;
            Map<String, Object> payload = new HashMap<>(prediction);

            if (acceptedNow) {
                Map<String, Object> confirmation = new HashMap<>();
                confirmation.put("accepted", true);
                confirmation.put("noticeVersion", NOTICE_VERSION);
                payload.put("publicLeaderboardConsent", confirmation);
            }

            functions.getHttpsCallable("submitUserPrediction")
                    .call(payload)
                    .addOnSuccessListener(result -> {
                        if (active()) {
                            completion.trySetResult(result);
                        } else {
                            cancel();
                        }
                    })
                    .addOnFailureListener(this::fail);
        }

        private void fail(Exception error) {
            if (active()) {
                completion.trySetException(error);
            } else {
                cancel();
            }
        }

        private void cancel() {
            completion.trySetException(new ConsentCancelledException());
        }

        private void cleanup() {
            activity.getLifecycle().removeObserver(this);

            if (authListener != null) {
                FirebaseAuth.getInstance().removeAuthStateListener(authListener);
                authListener = null;
            }

            if (dialog != null && dialog.isShowing()) {
                dialog.dismiss();
            }

            dialog = null;
        }

        @Override
        public void onStop(@NonNull LifecycleOwner owner) {
            cancel();
        }

        @Override
        public void onDestroy(@NonNull LifecycleOwner owner) {
            cancel();
        }
    }
}
