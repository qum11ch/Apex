package com.example.f1app;

import static com.example.f1app.MainActivity.checkConnection;
import static com.example.f1app.driversStandingsActivity.startActivity_seasonData;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;

import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.functions.FirebaseFunctions;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.ViewGroup;

import android.view.View;
import android.view.ViewGroup;

import androidx.activity.EdgeToEdge;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class logInPageActivity extends AppCompatActivity {
    private FirebaseAuth auth;
    private ImageButton showDrivers, showSchedule, showTeams, showHomePage;
    private EditText editTextLogin, editTextPassword;
    private Button loginButton;
    private ProgressBar loginProgress;
    private LinearLayout signUpLayout;
    private boolean loginInProgress = false;
    private Dialog usernameDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);
        setContentView(R.layout.login_page);
        setupLoginInsets();

        auth = FirebaseAuth.getInstance();

        TextView reloginNotice = findViewById(R.id.reloginNotice);

        reloginNotice.setVisibility(
                AuthSessionGate.shouldShowReloginNotice(this)
                        ? View.VISIBLE
                        : View.GONE
        );

        if (!checkConnection(getApplicationContext())){
            startActivity(connectionLostScreen.createShowSplashOnNetworkFailure(logInPageActivity.this));
        }else{
            startActivity(connectionLostScreen.createIntentHideSplashOnNetworkRecovery(logInPageActivity.this));
        }

        Bundle intentBundle = getIntent().getExtras();

        String mCurrentSeason = intentBundle != null
                ? intentBundle.getString("currentSeason")
                : null;

        WindowInsetsControllerCompat windowInsetsController =
                WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        windowInsetsController.setAppearanceLightStatusBars(false);

        editTextLogin = findViewById(R.id.loginUsername);
        editTextPassword = findViewById(R.id.loginPassword);
        loginButton = findViewById(R.id.loginButton);

        signUpLayout = findViewById(R.id.signUpLayout);
        signUpLayout.setOnClickListener(v -> startActivity_seasonData(logInPageActivity.this, registerPageActivity.class, mCurrentSeason));

        loginProgress = findViewById(R.id.loginProgress);

        TextView resetPassword = findViewById(R.id.resetPassword);
        resetPassword.setOnClickListener(view -> startActivity_seasonData(logInPageActivity.this, resetPageActivity.class, mCurrentSeason));

        loginButton.setOnClickListener(v -> loginUserAccount(mCurrentSeason));

        showDrivers = findViewById(R.id.showDriver);
        showDrivers.setOnClickListener(v -> startActivity_seasonData(logInPageActivity.this, driversStandingsActivity.class, mCurrentSeason));

        showSchedule = findViewById(R.id.showSchedule);
        showSchedule.setOnClickListener(v -> startActivity_seasonData(logInPageActivity.this, scheduleActivity.class, mCurrentSeason));

        showHomePage = findViewById(R.id.showHomePage);
        showHomePage.setOnClickListener(v -> {
            Intent intent = new Intent(logInPageActivity.this, MainActivity.class);
            logInPageActivity.this.startActivity(intent);
            overridePendingTransition(0, 0);
        });

        showTeams = findViewById(R.id.showTeams);
        showTeams.setOnClickListener(v -> startActivity_seasonData(logInPageActivity.this, teamsStandingsActivity.class, mCurrentSeason));

        ImageButton backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> finish());

        editTextPassword.addTextChangedListener(textWatcher);
        editTextLogin.addTextChangedListener(textWatcher);

        FirebaseUser existingUser = auth.getCurrentUser();


        Log.d("AUTH_CHECK", "currentUser = " +
                (existingUser == null ? "null" : existingUser.getUid()));

        if (existingUser != null) {
            checkOrMigrateProfile(
                    existingUser,
                    null,
                    mCurrentSeason
            );
        } else {
            updateRegisterButtonState();
        }
    }

    private void setupLoginInsets() {
        View root = findViewById(R.id.login_root);
        View header = findViewById(R.id.header);
        View footer = findViewById(R.id.footer_container);
        View content = findViewById(R.id.login_scroll_content);

        final int rootLeft = root.getPaddingLeft();
        final int rootTop = root.getPaddingTop();
        final int rootRight = root.getPaddingRight();

        final int headerLeft = header.getPaddingLeft();
        final int headerTop = header.getPaddingTop();
        final int headerRight = header.getPaddingRight();
        final int headerBottom = header.getPaddingBottom();

        final int contentLeft = content.getPaddingLeft();
        final int contentTop = content.getPaddingTop();
        final int contentRight = content.getPaddingRight();

        final float density = getResources().getDisplayMetrics().density;
        final int footerGap = Math.round(1 * density);
        final int contentGap = Math.round(16 * density);

        Runnable updateContentPadding = () -> {
            int bottomPadding = contentGap;

            if (footer.getVisibility() == View.VISIBLE) {
                ViewGroup.MarginLayoutParams params =
                        (ViewGroup.MarginLayoutParams) footer.getLayoutParams();

                bottomPadding += footer.getHeight() + params.bottomMargin;
            }

            if (content.getPaddingBottom() != bottomPadding) {
                content.setPadding(
                        contentLeft,
                        contentTop,
                        contentRight,
                        bottomPadding
                );
            }
        };

        footer.addOnLayoutChangeListener(
                (view, left, top, right, bottom,
                 oldLeft, oldTop, oldRight, oldBottom) ->
                        updateContentPadding.run()
        );

        ViewCompat.setOnApplyWindowInsetsListener(root, (view, windowInsets) -> {
            Insets safeInsets = windowInsets.getInsets(
                    WindowInsetsCompat.Type.systemBars()
                            | WindowInsetsCompat.Type.displayCutout()
            );

            Insets imeInsets = windowInsets.getInsets(
                    WindowInsetsCompat.Type.ime()
            );

            boolean keyboardVisible = windowInsets.isVisible(
                    WindowInsetsCompat.Type.ime()
            );

            view.setPadding(
                    rootLeft + safeInsets.left,
                    rootTop,
                    rootRight + safeInsets.right,
                    keyboardVisible
                            ? Math.max(imeInsets.bottom, safeInsets.bottom)
                            : 0
            );

            header.setPadding(
                    headerLeft,
                    headerTop + safeInsets.top,
                    headerRight,
                    headerBottom
            );

            int visibility = keyboardVisible ? View.GONE : View.VISIBLE;

            if (footer.getVisibility() != visibility) {
                footer.setVisibility(visibility);
            }

            ViewGroup.MarginLayoutParams params =
                    (ViewGroup.MarginLayoutParams) footer.getLayoutParams();

            int bottomMargin = safeInsets.bottom + footerGap;

            if (params.bottomMargin != bottomMargin) {
                params.bottomMargin = bottomMargin;
                footer.setLayoutParams(params);
            }

            updateContentPadding.run();

            return WindowInsetsCompat.CONSUMED;
        });

        ViewCompat.requestApplyInsets(root);
    }

    private void showFieldError(
            LinearLayout box,
            TextView errorView,
            String message
    ) {
        if (box == null || errorView == null) {
            Log.e("DialogError", "box или errorView = null");
            return;
        }

        boolean hasError = message != null && !message.isEmpty();

        box.setBackgroundResource(
                hasError
                        ? R.drawable.bg_input_box_error
                        : R.drawable.bg_input_box
        );

        errorView.setText(hasError ? message : "");
        errorView.setVisibility(hasError ? View.VISIBLE : View.GONE);

        Log.d("DialogError", "message=" + message +
                ", visibility=" + errorView.getVisibility());
    }

    private TextWatcher textWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

        @Override
        public void onTextChanged(
                CharSequence s,
                int start,
                int before,
                int count) {
            validateFields();
            updateRegisterButtonState();
        }

        @Override
        public void afterTextChanged(Editable editable) {}
    };

    private void validateFields() {
        String login = getLoginInput();
        String password = getPasswordInput();

        setLoginError(
                null
        );

        setPasswordError(
                password.isEmpty() || password.length() >= 6
                        ? null
                        : getString(R.string.invalid_password_text)
        );
    }

    private void updateRegisterButtonState() {
        String login = getLoginInput();
        String password = getPasswordInput();

        loginButton.setEnabled(
                !loginInProgress
                        && !login.isEmpty()
                        && password.length() >= 6
        );
    }

    private String getLoginInput() {
        return editTextLogin.getText().toString().trim();
    }

    private String getPasswordInput() {
        return editTextPassword.getText().toString();
    }

    private void loginUserAccount(String currentSeason) {
        if (loginInProgress) return;

        String login = getLoginInput();
        String password = getPasswordInput();

        if (login.isEmpty() || password.isEmpty()) {
            Toast.makeText(
                    this,
                    getString(R.string.empty_credentials_text),
                    Toast.LENGTH_LONG
            ).show();
            return;
        }

        setLoginLoading(true);

        Map<String, Object> data = new HashMap<>();
        data.put("login", login);
        data.put("password", password);

        FirebaseFunctions.getInstance("us-central1")
                .getHttpsCallable("loginWithLogin")
                .call(data)
                .addOnSuccessListener(result -> {
                    Log.d("LoginFlow", "LOGIN_CALLABLE_OK");

                    if (isFinishing() || isDestroyed()) return;

                    Object raw = result.getData();

                    if (!(raw instanceof Map)) {
                        finishLoginWithError();
                        return;
                    }

                    Object tokenValue =
                            ((Map<?, ?>) raw).get("customToken");

                    if (!(tokenValue instanceof String)) {
                        finishLoginWithError();
                        return;
                    }

                    auth.signInWithCustomToken((String) tokenValue)
                            .addOnCompleteListener(this, task -> {
                                if (isFinishing() || isDestroyed()) return;

                                if (!task.isSuccessful()) {
                                    Log.e(
                                            "LoginFlow",
                                            "SIGN_IN_CUSTOM_TOKEN_FAILED",
                                            task.getException()
                                    );

                                    finishLoginWithError();
                                    return;
                                }

                                Log.d("LoginFlow", "SIGN_IN_CUSTOM_TOKEN_OK");

                                FirebaseUser user = auth.getCurrentUser();

                                if (user == null) {
                                    finishLoginWithError();
                                    return;
                                }

                                checkOrMigrateProfile(
                                        user,
                                        login,
                                        currentSeason
                                );
                            });
                })
                .addOnFailureListener(error -> {
                    if (isFinishing() || isDestroyed()) return;

                    Log.e("Login", "Server login failed", error);

                    String message = error.getLocalizedMessage();

                    if (message == null || message.trim().isEmpty()) {
                        message = getString(R.string.auth_fail_text);
                    }

                    setLoginLoading(false);

                    Toast.makeText(
                            this,
                            message,
                            Toast.LENGTH_LONG
                    ).show();
                });
    }

    private void signInLegacyUser(
            String email,
            String enteredPassword,
            String legacyLogin,
            String storedUid,
            String currentSeason) {

        auth = FirebaseAuth.getInstance();

        auth.signInWithEmailAndPassword(email, enteredPassword)
                .addOnCompleteListener(this, task -> {
                    if (isFinishing() || isDestroyed()) return;

                    if (!task.isSuccessful()) {
                        finishLoginWithError();
                        return;
                    }

                    FirebaseUser firebaseUser = auth.getCurrentUser();

                    if (firebaseUser == null) {
                        finishLoginWithError();
                        return;
                    }

                    String uid = firebaseUser.getUid();

                    if (storedUid != null && !uid.equals(storedUid)) {
                        auth.signOut();
                        finishLoginWithError();
                        return;
                    }

                    checkOrMigrateProfile(
                            firebaseUser,
                            legacyLogin,
                            currentSeason
                    );
                });
    }

    private void checkOrMigrateProfile(
            FirebaseUser firebaseUser,
            String legacyLogin,
            String currentSeason) {

        setLoginLoading(true);

        FirebaseFunctions.getInstance("us-central1")
                .getHttpsCallable("prepareAccountProfile")
                .call()
                .addOnSuccessListener(result -> {
                    Log.d("LoginFlow", "PREPARE_PROFILE_OK");
                    if (isFinishing() || isDestroyed()) return;

                    FirebaseUser current = auth.getCurrentUser();

                    if (current == null ||
                            !current.getUid().equals(firebaseUser.getUid())) {
                        finishLoginWithError();
                        return;
                    }


                    Object raw = result.getData();

                    if (!(raw instanceof Map)) {
                        finishLoginWithError();
                        return;
                    }

                    Map<?, ?> data = (Map<?, ?>) raw;

                    if (Boolean.TRUE.equals(data.get("migrationComplete"))) {
                        openAccount(currentSeason);
                        return;
                    }

                    Object loginValue = data.get("login");

                    if (!(loginValue instanceof String)) {
                        finishLoginWithError();
                        return;
                    }

                    showPublicUsernameDialog(
                            current,
                            (String) loginValue,
                            currentSeason
                    );
                })
                .addOnFailureListener(error -> {
                    if (isFinishing() || isDestroyed()) return;

                    Log.e(
                            "Migration",
                            "Server profile preparation failed",
                            error
                    );

                    // Не оставляем доступ к аккаунту через неполный сценарий.
                    auth.signOut();
                    finishLoginWithError();
                });
    }

    private void migrateLegacyProfile(
            FirebaseUser firebaseUser,
            String legacyLogin,
            String currentSeason) {

        String uid = firebaseUser.getUid();

        DatabaseReference root =
                FirebaseDatabase.getInstance().getReference();

        DatabaseReference oldProfile =
                root.child("users").child(legacyLogin);

        oldProfile.get()
                .addOnSuccessListener(oldSnapshot -> {
                    if (isFinishing() || isDestroyed()) return;

                    String oldUid =
                            oldSnapshot.child("userId")
                                    .getValue(String.class);

                    if (!oldSnapshot.exists() || !uid.equals(oldUid)) {
                        Log.e("Migration",
                                "Legacy profile missing or UID mismatch");
                        finishLoginWithError();
                        return;
                    }

                    DatabaseReference savedRacesRef =
                            root.child("savedRaces").child(legacyLogin);

                    savedRacesRef.get()
                            .addOnSuccessListener(racesSnapshot -> {
                                if (isFinishing() || isDestroyed()) return;

                                // Копируем старые поля в новый UID-путь.
                                // usernameMigrationComplete пока false:
                                // пользователь ещё не выбрал публичный ник.
                                Map<String, Object> profile = new HashMap<>();

                                profile.put("userId", uid);
                                profile.put("login", legacyLogin.toLowerCase(Locale.ROOT));
                                profile.put(
                                        "userEmail",
                                        oldSnapshot.child("userEmail").getValue(String.class)
                                );
                                profile.put(
                                        "choiceDriver",
                                        oldSnapshot.child("choiceDriver").getValue(String.class)
                                );
                                profile.put(
                                        "choiceTeam",
                                        oldSnapshot.child("choiceTeam").getValue(String.class)
                                );
                                profile.put(
                                        "usernameMigrationComplete",
                                        false
                                );

                                Map<String, Object> updates =
                                        new HashMap<>();

                                updates.put("/users/" + uid, profile);
                                updates.put("/loginIndex/" + legacyLogin.toLowerCase(Locale.ROOT), uid);

                                if (racesSnapshot.exists()) {
                                    updates.put(
                                            "/savedRaces/" + uid,
                                            racesSnapshot.getValue()
                                    );
                                }

                                root.updateChildren(updates)
                                        .addOnSuccessListener(unused -> {
                                            if (isFinishing() || isDestroyed()) {
                                                return;
                                            }

                                            showPublicUsernameDialog(
                                                    firebaseUser,
                                                    legacyLogin,
                                                    currentSeason
                                            );
                                        })
                                        .addOnFailureListener(error -> {
                                            Log.e(
                                                    "Migration",
                                                    "Failed to copy legacy data",
                                                    error
                                            );
                                            finishLoginWithError();
                                        });
                            })
                            .addOnFailureListener(error -> {
                                Log.e(
                                        "Migration",
                                        "Failed to read saved races",
                                        error
                                );
                                finishLoginWithError();
                            });
                })
                .addOnFailureListener(error -> {
                    Log.e("Migration", "Failed to read old profile", error);
                    finishLoginWithError();
                });
    }

    private void showPublicUsernameDialog(
            FirebaseUser firebaseUser,
            String legacyLogin,
            String currentSeason) {

        if (isFinishing() || isDestroyed()) return;

        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_choose_public_username);
        dialog.setCancelable(false);

        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(
                    new ColorDrawable(Color.TRANSPARENT)
            );
        }

        TextInputEditText input =
                dialog.findViewById(R.id.publicUsernameInput);

        TextView errorText =
                dialog.findViewById(R.id.publicUsernameError);

        LinearLayout box = dialog.findViewById(R.id.usernameBox);

        Button saveButton =
                dialog.findViewById(R.id.saveUsernameButton);

        Button cancelButton =
                dialog.findViewById(R.id.cancelUsernameButton);

        // Для первого входа можно предложить старый логин как исходный текст,
        // но пользователь вправе его заменить любым username, включая русский.
        input.setText(legacyLogin);
        input.setSelection(input.length());

        cancelButton.setOnClickListener(v -> {
            dialog.dismiss();
            FirebaseAuth.getInstance().signOut();
            setLoginLoading(false);
        });

        saveButton.setOnClickListener(v -> {
            String publicUsername =
                    input.getText() == null
                            ? ""
                            : input.getText().toString().trim();

            String validationError =
                    validatePublicUsername(publicUsername);

            if (validationError != null) {
                showFieldError(box, errorText, validationError);
                return;
            }

            showFieldError(box, errorText, null);
            saveButton.setEnabled(false);
            cancelButton.setEnabled(false);

            savePublicUsername(
                    firebaseUser,
                    legacyLogin,
                    publicUsername,
                    currentSeason,
                    dialog,
                    saveButton,
                    cancelButton
            );
        });

        dialog.setOnDismissListener(v -> {
            if (usernameDialog == dialog) {
                usernameDialog = null;
            }
        });

        usernameDialog = dialog;
        dialog.show();

        if (window != null) {
            int width = (int) (getResources()
                    .getDisplayMetrics().widthPixels * 0.92f);

            window.setLayout(
                    width,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
        }
    }

    private String validatePublicUsername(String value) {
        String trimmed = value.trim();

        if (trimmed.isEmpty()) {
            return "Введите публичный никнейм";
        }

        if (trimmed.length() > 30) {
            return "Максимум 30 символов";
        }

        return null;
    }

    private void savePublicUsername(
            FirebaseUser firebaseUser,
            String legacyLogin,
            String publicUsername,
            String currentSeason,
            Dialog dialog,
            Button saveButton,
            Button cancelButton) {

        FirebaseUser current = auth.getCurrentUser();

        if (current == null ||
                !current.getUid().equals(firebaseUser.getUid())) {
            dialog.dismiss();
            finishLoginWithError();
            return;
        }

        setLoginLoading(true);
        saveButton.setEnabled(false);
        cancelButton.setEnabled(false);

        Map<String, Object> data = new HashMap<>();
        data.put("username", publicUsername);

        FirebaseFunctions.getInstance("us-central1")
                .getHttpsCallable("completeAccountMigration")
                .call(data)
                .addOnSuccessListener(result -> {
                    if (isFinishing() || isDestroyed()) return;

                    dialog.dismiss();
                    openAccount(currentSeason);
                })
                .addOnFailureListener(error -> {
                    if (isFinishing() || isDestroyed()) return;

                    Log.e(
                            "Migration",
                            "Server migration completion failed",
                            error
                    );

                    setLoginLoading(false);
                    saveButton.setEnabled(true);
                    cancelButton.setEnabled(true);
                    showMigrationError();
                });
    }

    private void openAccount(String currentSeason) {
        if (isFinishing() || isDestroyed()) return;

        AuthSessionGate.clearReloginNotice(this);
        setLoginLoading(false);

        Toast.makeText(
                this,
                getString(R.string.login_succ_text),
                Toast.LENGTH_SHORT
        ).show();

        startActivity_seasonData(
                logInPageActivity.this,
                accountPageActivity.class,
                currentSeason
        );
        finish();
    }

    private void showMigrationError() {
        if (isFinishing() || isDestroyed()) return;

        Toast.makeText(
                this,
                getString(R.string.smth_wrong_text),
                Toast.LENGTH_LONG
        ).show();
    }

    private void setLoginLoading(boolean loading) {
        loginInProgress = loading;
        loginProgress.setVisibility(
                loading ? View.VISIBLE : View.GONE
        );
        loginButton.setVisibility(
                loading ? View.INVISIBLE : View.VISIBLE
        );
        updateRegisterButtonState();
    }

    private void finishLoginWithError() {
        if (isFinishing() || isDestroyed()) return;

        setLoginLoading(false);
        showLoginError();
    }

    private void showLoginError() {
        if (isFinishing() || isDestroyed()) return;

        Toast.makeText(
                this,
                getString(R.string.auth_fail_text),
                Toast.LENGTH_LONG
        ).show();
    }

    private void setLoginError(String message) {
        LinearLayout box = findViewById(R.id.loginBox);
        TextView error = findViewById(R.id.loginError);

        boolean hasError = message != null && !message.isEmpty();

        box.setBackgroundResource(
                hasError
                        ? R.drawable.bg_input_box_error
                        : R.drawable.bg_input_box
        );

        error.setText(hasError ? message : "");
        error.setVisibility(hasError ? View.VISIBLE : View.GONE);
    }

    private void setPasswordError(String message) {
        LinearLayout box = findViewById(R.id.passwordBox);
        TextView error = findViewById(R.id.passwordError);

        boolean hasError = message != null && !message.isEmpty();

        box.setBackgroundResource(
                hasError
                        ? R.drawable.bg_input_box_error
                        : R.drawable.bg_input_box
        );

        error.setText(hasError ? message : "");
        error.setVisibility(hasError ? View.VISIBLE : View.GONE);
    }
}
