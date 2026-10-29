package com.example.f1app;

import static com.example.f1app.MainActivity.checkConnection;

import android.app.Dialog;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class accountSettingsActivity extends AppCompatActivity {

    private RelativeLayout changeAvatarLayout;
    private RelativeLayout changeBackgroundLayout;
    private LinearLayout header;

    private Button loginButton;

    private FirebaseAuth auth;

    private TextView username;
    private TextView currentEmail;
    private TextView currentLogin;
    private TextView avatarInitial;

    private ImageView avatarImage;

    private Dialog deleteDialog;
    private Dialog loginDialog;
    private Dialog changeUsernameDialog;

    private EditText loginPassword;

    private LinearLayout passwordBox;
    private TextView loginPasswordError;

    private TextView usernameMain;

    private ProgressBar loginProgress;

    private String mCurrentSeason;

    private int selectedBackgroundColor;

    private backgroundColorPickerDialog backgroundColorDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.account_settings_page);

        setupAccountSettingsInsets();

        WindowInsetsControllerCompat windowInsetsController =
                WindowCompat.getInsetsController(
                        getWindow(),
                        getWindow().getDecorView()
                );

        windowInsetsController.setAppearanceLightStatusBars(false);

        if (!checkConnection(getApplicationContext())) {
            startActivity(connectionLostScreen
                    .createShowSplashOnNetworkFailure(
                            accountSettingsActivity.this
                    ));
        } else {
            startActivity(connectionLostScreen
                    .createIntentHideSplashOnNetworkRecovery(
                            accountSettingsActivity.this
                    ));
        }

        Bundle intentBundle = getIntent().getExtras();

        mCurrentSeason = intentBundle != null
                ? intentBundle.getString("currentSeason")
                : null;

        username = findViewById(R.id.username);
        usernameMain = findViewById(R.id.usernameMain);
        currentEmail = findViewById(R.id.currentEmail);
        currentLogin = findViewById(R.id.currentLogin);

        avatarImage = findViewById(R.id.avatarImage);
        avatarInitial = findViewById(R.id.avatarInitial);

        changeAvatarLayout = findViewById(R.id.changeAvatarLayout);
        changeBackgroundLayout = findViewById(R.id.changeBackgroundLayout);
        header = findViewById(R.id.header);

        selectedBackgroundColor = readBackgroundColor(
                changeBackgroundLayout,
                ContextCompat.getColor(this, R.color.account_settings_header_color)
        );

        applyBackgroundColor(selectedBackgroundColor);

        changeBackgroundLayout.setOnClickListener(view ->
                showBackgroundColorDialog()
        );

        LinearLayout changeUsernameLayout = findViewById(R.id.changeUsernameButton);

        LinearLayout changeEmailButton =
                findViewById(R.id.changeEmailButton);

        LinearLayout changePasswordButton =
                findViewById(R.id.changePasswordButton);

        Button deleteButton =
                findViewById(R.id.deleteAccountButton);

        Button logoutButton = findViewById(R.id.logoutButton);

        auth = FirebaseAuth.getInstance();

        FirebaseUser user = auth.getCurrentUser();

        if (user == null) {
            finish();
            return;
        }

        if (user.getEmail() != null
                && !user.getEmail().isEmpty()) {

            avatarInitial.setText(
                    user.getEmail()
                            .substring(0, 1)
                            .toUpperCase()
            );
        }

        loadUserProfile();
        loadAvatarIfExists(user);

        changeUsernameLayout.setOnClickListener(v ->
                showChangeUsernameDialog()
        );

        deleteDialog = new Dialog(accountSettingsActivity.this);
        deleteDialog.setContentView(R.layout.delete_dialog_box);

        if (deleteDialog.getWindow() != null) {
            deleteDialog.getWindow().setLayout(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );

            deleteDialog.getWindow().setBackgroundDrawable(
                    getDrawable(R.drawable.custom_dialog_bg)
            );
        }

        deleteDialog.setCancelable(false);

        loginDialog = new Dialog(accountSettingsActivity.this);
        loginDialog.setContentView(R.layout.dialog_login);

        if (loginDialog.getWindow() != null) {
            loginDialog.getWindow().setLayout(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );

            loginDialog.getWindow().setBackgroundDrawable(
                    getDrawable(R.drawable.custom_dialog_bg)
            );
        }

        loginDialog.setCancelable(true);

        loginPassword =
                loginDialog.findViewById(R.id.loginPassword);

        passwordBox =
                loginDialog.findViewById(R.id.passwordBox);

        loginPasswordError =
                loginDialog.findViewById(R.id.passwordError);

        loginButton =
                loginDialog.findViewById(R.id.loginButton);

        loginProgress =
                loginDialog.findViewById(R.id.loginProgress);

        loginButton.setOnClickListener(v ->
                deleteAccountAfterReauthentication()
        );

        Button cancelButton =
                deleteDialog.findViewById(R.id.cancel_button);

        Button confirmButton =
                deleteDialog.findViewById(R.id.confirm_button);

        confirmButton.setOnClickListener(view -> {
            deleteDialog.dismiss();

            loginPassword.setText("");
            loginPasswordError.setVisibility(View.GONE);
            loginProgress.setVisibility(View.GONE);
            loginButton.setEnabled(true);

            loginDialog.show();
        });

        cancelButton.setOnClickListener(view ->
                deleteDialog.dismiss()
        );

        ImageButton backButton = findViewById(R.id.backButton);

        backButton.setOnClickListener(view -> {
            finish();
            overridePendingTransition(0, 0);
        });

        getOnBackPressedDispatcher().addCallback(
                this,
                new OnBackPressedCallback(true) {
                    @Override
                    public void handleOnBackPressed() {
                        finish();
                        overridePendingTransition(0, 0);
                    }
                }
        );

        deleteButton.setOnClickListener(view ->
                deleteDialog.show()
        );

        changeAvatarLayout.setOnClickListener(v -> {
            PickVisualMediaRequest request =
                    new PickVisualMediaRequest.Builder()
                            .setMediaType(
                                    ActivityResultContracts
                                            .PickVisualMedia
                                            .ImageOnly.INSTANCE
                            )
                            .build();

            avatarPicker.launch(request);
        });



        changeEmailButton.setOnClickListener(v ->
                showChangeEmailDialog()
        );

        changePasswordButton.setOnClickListener(v ->
                showChangePasswordDialog()
        );

        logoutButton.setOnClickListener(v -> logout());
    }

    private void setupAccountSettingsInsets() {
        View root = findViewById(R.id.account_settings_root);
        View header = findViewById(R.id.header);

        final int rootLeft = root.getPaddingLeft();
        final int rootTop = root.getPaddingTop();
        final int rootRight = root.getPaddingRight();
        final int rootBottom = root.getPaddingBottom();

        final int headerLeft = header.getPaddingLeft();
        final int headerTop = header.getPaddingTop();
        final int headerRight = header.getPaddingRight();
        final int headerBottom = header.getPaddingBottom();

        ViewCompat.setOnApplyWindowInsetsListener(root, (view, windowInsets) -> {
            Insets safeInsets = windowInsets.getInsets(
                    WindowInsetsCompat.Type.systemBars()
                            | WindowInsetsCompat.Type.displayCutout()
            );

            view.setPadding(
                    rootLeft + safeInsets.left,
                    rootTop,
                    rootRight + safeInsets.right,
                    rootBottom + safeInsets.bottom
            );

            header.setPadding(
                    headerLeft,
                    headerTop + safeInsets.top,
                    headerRight,
                    headerBottom
            );

            return WindowInsetsCompat.CONSUMED;
        });

        ViewCompat.requestApplyInsets(root);
    }

    private int readBackgroundColor(View view, int fallbackColor) {
        ColorStateList tint = view.getBackgroundTintList();

        if (tint != null) {
            return tint.getColorForState(
                    view.getDrawableState(),
                    tint.getDefaultColor()
            );
        }

        Drawable background = view.getBackground();

        if (background instanceof ColorDrawable) {
            return ((ColorDrawable) background).getColor();
        }

        if (android.os.Build.VERSION.SDK_INT >= 24
                && background instanceof GradientDrawable) {

            ColorStateList fill = ((GradientDrawable) background).getColor();

            if (fill != null) {
                return fill.getColorForState(
                        view.getDrawableState(),
                        fill.getDefaultColor()
                );
            }
        }

        return fallbackColor;
    }

    private void applyBackgroundColor(int color) {
        selectedBackgroundColor = color;

        tintBackground(changeBackgroundLayout, color);
        tintBackground(header, color);
    }

    private void tintBackground(View view, int color) {
        if (view.getBackground() == null) {
            view.setBackgroundColor(getColor(R.color.account_settings_header_color));
        }

        view.setBackgroundTintList(
                ColorStateList.valueOf(color)
        );
    }

    private void showBackgroundColorDialog() {
        if (isFinishing() || isDestroyed()) {
            return;
        }

        if (backgroundColorDialog != null
                && backgroundColorDialog.isShowing()) {
            return;
        }

        FirebaseUser user =
                FirebaseAuth.getInstance().getCurrentUser();

        if (user == null) {
            Toast.makeText(
                    this,
                    R.string.background_color_login_required,
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        // Запоминаем владельца профиля при открытии окна.
        final String ownerUid = user.getUid();

        backgroundColorDialog = new backgroundColorPickerDialog(
                this,
                selectedBackgroundColor,
                color -> {
                    backgroundColorPickerDialog dialog =
                            backgroundColorDialog;

                    if (dialog != null && dialog.isShowing()) {
                        saveBackgroundColor(ownerUid, color, dialog);
                    }
                }
        );

        backgroundColorDialog.setOnDismissListener(dialog ->
                backgroundColorDialog = null
        );

        backgroundColorDialog.show();
    }

    private void saveBackgroundColor(
            String ownerUid,
            int color,
            backgroundColorPickerDialog dialog) {

        FirebaseUser user =
                FirebaseAuth.getInstance().getCurrentUser();

        if (user == null || !ownerUid.equals(user.getUid())) {
            dialog.dismiss();

            Toast.makeText(
                    this,
                    R.string.background_color_login_required,
                    Toast.LENGTH_LONG
            ).show();

            return;
        }

        // Непрозрачный цвет в формате #RRGGBB.
        final int opaqueColor = Color.rgb(
                Color.red(color),
                Color.green(color),
                Color.blue(color)
        );

        final String hexColor = String.format(
                Locale.ROOT,
                "#%06X",
                opaqueColor & 0xFFFFFF
        );

        Map<String, Object> updates = new HashMap<>();

        updates.put(
                "users/" + ownerUid + "/background_color",
                hexColor
        );

        updates.put(
                "publicProfiles/" + ownerUid + "/background_color",
                hexColor
        );

        dialog.setSaving(true);

        FirebaseDatabase.getInstance()
                .getReference()
                .updateChildren(updates)
                .addOnSuccessListener(unused -> {
                    if (isFinishing() || isDestroyed()) {
                        return;
                    }

                    FirebaseUser currentUser =
                            FirebaseAuth.getInstance().getCurrentUser();

                    if (currentUser == null
                            || !ownerUid.equals(currentUser.getUid())) {

                        dialog.dismiss();
                        return;
                    }

                    // Меняем экран только после успешной записи.
                    applyBackgroundColor(opaqueColor);

                    dialog.dismiss();

                    Toast.makeText(
                            this,
                            R.string.background_color_saved,
                            Toast.LENGTH_SHORT
                    ).show();
                })
                .addOnFailureListener(error -> {
                    Log.e(
                            "BackgroundColor",
                            "Failed to save background color",
                            error
                    );

                    if (isFinishing() || isDestroyed()) {
                        return;
                    }

                    if (dialog.isShowing()) {
                        dialog.setSaving(false);
                    }

                    Toast.makeText(
                            this,
                            R.string.background_color_save_error,
                            Toast.LENGTH_LONG
                    ).show();
                });
    }

    private int dp(float value) {
        return Math.round(
                value * getResources().getDisplayMetrics().density
        );
    }

    private void logout() {
        auth.signOut();

        Intent intent = new Intent(
                accountSettingsActivity.this,
                logInPageActivity.class
        );

        intent.putExtra("currentSeason", mCurrentSeason);

        intent.setFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TASK
        );

        startActivity(intent);
        finish();
    }

    private void changeEmail(
            String newEmail,
            String currentPassword,
            AlertDialog dialog,
            ProgressBar progress,
            Button saveButton) {

        FirebaseUser user = auth.getCurrentUser();

        if (user == null || user.getEmail() == null) {
            progress.setVisibility(View.GONE);
            saveButton.setEnabled(true);

            showError(getString(R.string.users_not_found));
            return;
        }

        if (newEmail.equalsIgnoreCase(user.getEmail())) {
            progress.setVisibility(View.GONE);
            saveButton.setEnabled(true);

            showError(getString(R.string.new_email_error));
            return;
        }

        AuthCredential credential =
                EmailAuthProvider.getCredential(
                        user.getEmail(),
                        currentPassword
                );

        user.reauthenticate(credential)
                .addOnSuccessListener(unused ->
                        user.verifyBeforeUpdateEmail(newEmail)
                                .addOnSuccessListener(unused2 -> {
                                    progress.setVisibility(View.GONE);
                                    saveButton.setEnabled(true);
                                    dialog.dismiss();

                                    Toast.makeText(
                                            this,
                                            getString(
                                                    R.string.email_confirming_text
                                            ),
                                            Toast.LENGTH_LONG
                                    ).show();
                                })
                                .addOnFailureListener(error -> {
                                    progress.setVisibility(View.GONE);
                                    saveButton.setEnabled(true);

                                    Log.e(
                                            "ChangeEmail",
                                            "Email verification failed",
                                            error
                                    );

                                    showError(
                                            getString(
                                                    R.string.confirmation_email_error
                                            )
                                    );
                                })
                )
                .addOnFailureListener(error -> {
                    progress.setVisibility(View.GONE);
                    saveButton.setEnabled(true);

                    showError(getString(R.string.password_error));
                });
    }

    private void updateEmailInDatabase(String newEmail) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user == null) {
            return;
        }

        String uid = user.getUid();

        Map<String, Object> updates = new HashMap<>();
        updates.put("userEmail", newEmail);

        FirebaseDatabase.getInstance()
                .getReference()
                .child("users")
                .child(uid)
                .updateChildren(updates)
                .addOnFailureListener(error ->
                        Log.e(
                                "UpdateEmail",
                                "Failed to update userEmail",
                                error
                        )
                );
    }

    private void changePassword(
            String currentPassword,
            String newPassword,
            AlertDialog dialog,
            ProgressBar progress,
            Button saveButton) {

        FirebaseUser user = auth.getCurrentUser();

        if (user == null || user.getEmail() == null) {
            progress.setVisibility(View.GONE);
            saveButton.setEnabled(true);

            showError(getString(R.string.users_not_found));
            return;
        }

        AuthCredential credential =
                EmailAuthProvider.getCredential(
                        user.getEmail(),
                        currentPassword
                );

        user.reauthenticate(credential)
                .addOnSuccessListener(unused ->
                        user.updatePassword(newPassword)
                                .addOnSuccessListener(unused2 -> {
                                    progress.setVisibility(View.GONE);
                                    saveButton.setEnabled(true);
                                    dialog.dismiss();

                                    Toast.makeText(
                                            this,
                                            getString(
                                                    R.string.password_changed
                                            ),
                                            Toast.LENGTH_SHORT
                                    ).show();
                                })
                                .addOnFailureListener(error -> {
                                    progress.setVisibility(View.GONE);
                                    saveButton.setEnabled(true);

                                    showError(
                                            getString(
                                                    R.string.password_change_error
                                            )
                                    );
                                })
                )
                .addOnFailureListener(error -> {
                    progress.setVisibility(View.GONE);
                    saveButton.setEnabled(true);

                    showError(getString(R.string.password_error));
                });
    }

    private final ActivityResultLauncher<PickVisualMediaRequest> avatarPicker =
            registerForActivityResult(
                    new ActivityResultContracts.PickVisualMedia(),
                    uri -> {
                        if (uri != null) {
                            uploadAvatar(uri);
                        }
                    }
            );

    private void loadAvatarIfExists(FirebaseUser user) {
        if (user == null) {
            return;
        }

        StorageReference avatarRef =
                FirebaseStorage.getInstance()
                        .getReference()
                        .child("profileImages")
                        .child(user.getUid())
                        .child("avatar.jpg");

        avatarRef.getDownloadUrl()
                .addOnSuccessListener(uri -> {
                    if (isFinishing() || isDestroyed()) {
                        return;
                    }

                    avatarImage.setVisibility(View.VISIBLE);
                    avatarInitial.setVisibility(View.GONE);

                    GlideApp.with(this)
                            .load(uri)
                            .into(avatarImage);
                })
                .addOnFailureListener(error -> {
                    if (isFinishing() || isDestroyed()) {
                        return;
                    }

                    avatarImage.setVisibility(View.GONE);
                    avatarInitial.setVisibility(View.VISIBLE);
                });
    }

    private void deleteUserData(FirebaseUser user) {
        String uid = user.getUid();

        DatabaseReference rootRef =
                FirebaseDatabase.getInstance().getReference();

        rootRef.child("leaderboards")
                .get()
                .addOnSuccessListener(snapshot -> {
                    Map<String, Object> updates = new HashMap<>();

                    for (DataSnapshot seasonSnapshot :
                            snapshot.getChildren()) {

                        String season = seasonSnapshot.getKey();

                        if (season != null
                                && seasonSnapshot.child(uid).exists()) {

                            updates.put(
                                    "/leaderboards/"
                                            + season
                                            + "/"
                                            + uid,
                                    null
                            );
                        }
                    }

                    updates.put("/users/" + uid, null);
                    updates.put("/publicProfiles/" + uid, null);
                    updates.put("/userPredictionStats/" + uid, null);
                    updates.put("/savedRaces/" + uid, null);

                    rootRef.updateChildren(updates)
                            .addOnSuccessListener(unused ->
                                    deleteAvatarAndFirebaseUser(user)
                            )
                            .addOnFailureListener(error -> {
                                Log.e(
                                        "DeleteAccount",
                                        "Failed to delete user data",
                                        error
                                );

                                showError(
                                        getString(
                                                R.string.delete_user_error
                                        )
                                );
                            });
                })
                .addOnFailureListener(error -> {
                    Log.e(
                            "DeleteAccount",
                            "Failed to read leaderboards",
                            error
                    );

                    showError(
                            getString(R.string.delete_user_error)
                    );
                });
    }

    private void deleteAvatarAndFirebaseUser(FirebaseUser user) {
        StorageReference avatarRef =
                FirebaseStorage.getInstance()
                        .getReference()
                        .child("profileImages")
                        .child(user.getUid())
                        .child("avatar.jpg");

        avatarRef.delete()
                .addOnCompleteListener(task ->
                        deleteFirebaseUser(user)
                );
    }

    private void deleteFirebaseUser(FirebaseUser user) {
        user.delete()
                .addOnSuccessListener(unused -> {
                    auth.signOut();

                    Toast.makeText(
                            this,
                            R.string.delete_account_text,
                            Toast.LENGTH_LONG
                    ).show();

                    Intent intent = new Intent(
                            accountSettingsActivity.this,
                            logInPageActivity.class
                    );

                    intent.setFlags(
                            Intent.FLAG_ACTIVITY_NEW_TASK
                                    | Intent.FLAG_ACTIVITY_CLEAR_TASK
                    );

                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(error ->
                        showError(
                                getString(R.string.delete_user_error)
                        )
                );
    }

    private void showChangeEmailDialog() {
        View view = getLayoutInflater().inflate(
                R.layout.dialog_change_email,
                null
        );

        EditText newEmailInput =
                view.findViewById(R.id.newEmailInput);

        EditText currentPasswordInput =
                view.findViewById(R.id.currentPasswordEmailInput);

        LinearLayout emailBox =
                view.findViewById(R.id.emailBox);

        LinearLayout passwordBox =
                view.findViewById(R.id.passwordBox);

        TextView emailError =
                view.findViewById(R.id.emailError);

        TextView passwordError =
                view.findViewById(R.id.passwordError);

        if (emailBox == null || emailError == null
                || passwordBox == null || passwordError == null) {

            throw new IllegalStateException(
                    "Не найдены emailBox/emailError/"
                            + "passwordBox/passwordError "
                            + "в dialog_change_email.xml"
            );
        }

        ProgressBar progress =
                view.findViewById(R.id.emailProgress);

        Button cancel =
                view.findViewById(R.id.cancelEmailButton);

        Button save =
                view.findViewById(R.id.saveEmailButton);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(view)
                .create();

        cancel.setOnClickListener(v -> dialog.dismiss());

        save.setOnClickListener(v -> {
            String email = newEmailInput.getText()
                    .toString()
                    .trim();

            String password = currentPasswordInput.getText()
                    .toString()
                    .trim();

            if (email.isEmpty()) {
                showFieldError(
                        emailBox,
                        emailError,
                        getString(R.string.email_enter)
                );

                return;
            }

            if (!android.util.Patterns.EMAIL_ADDRESS
                    .matcher(email)
                    .matches()) {

                showFieldError(
                        emailBox,
                        emailError,
                        getString(R.string.email_error)
                );

                return;
            }

            if (password.isEmpty()) {
                showFieldError(
                        passwordBox,
                        passwordError,
                        getString(R.string.password_enter)
                );

                return;
            }

            if (password.length() < 6) {
                showFieldError(
                        passwordBox,
                        passwordError,
                        getString(R.string.six_simbols_minimum)
                );

                return;
            }

            getSharedPreferences(
                    "account_settings",
                    MODE_PRIVATE
            )
                    .edit()
                    .putString("pending_email", email)
                    .apply();

            showFieldError(emailBox, emailError, null);
            showFieldError(passwordBox, passwordError, null);

            progress.setVisibility(View.VISIBLE);
            save.setEnabled(false);

            changeEmail(email, password, dialog, progress, save);
        });

        dialog.show();

        Window window = dialog.getWindow();

        if (window != null) {
            int width = (int) (getResources()
                    .getDisplayMetrics().widthPixels * 0.92f);

            window.setBackgroundDrawable(
                    new android.graphics.drawable.ColorDrawable(
                            android.graphics.Color.TRANSPARENT
                    )
            );

            window.getDecorView().setPadding(0, 0, 0, 0);
            window.setLayout(
                    width,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
        }
    }

    private void showFieldError(
            LinearLayout box,
            TextView errorView,
            String message) {

        if (box == null || errorView == null) {
            Log.e(
                    "DialogError",
                    "box или errorView = null"
            );

            return;
        }

        boolean hasError =
                message != null && !message.isEmpty();

        box.setBackgroundResource(
                hasError
                        ? R.drawable.bg_input_box_error
                        : R.drawable.bg_input_box
        );

        errorView.setText(hasError ? message : "");
        errorView.setVisibility(
                hasError ? View.VISIBLE : View.GONE
        );
    }

    private void showChangePasswordDialog() {
        View view = getLayoutInflater().inflate(
                R.layout.dialog_change_password,
                null
        );

        EditText currentPasswordInput =
                view.findViewById(R.id.currentPasswordInput);

        EditText newPasswordInput =
                view.findViewById(R.id.newPasswordInput);

        EditText repeatPasswordInput =
                view.findViewById(R.id.repeatPasswordInput);

        LinearLayout newPasswordBox =
                view.findViewById(R.id.newPasswordBox);

        TextView newPasswordError =
                view.findViewById(R.id.newPasswordError);

        LinearLayout currentPasswordBox =
                view.findViewById(R.id.passwordBox);

        TextView currentPasswordError =
                view.findViewById(R.id.passwordError);

        LinearLayout repeatPasswordBox =
                view.findViewById(R.id.repeatPasswordBox);

        TextView repeatPasswordError =
                view.findViewById(R.id.repeatPasswordError);

        ProgressBar progress =
                view.findViewById(R.id.passwordProgress);

        Button cancel =
                view.findViewById(R.id.cancelPasswordButton);

        Button save =
                view.findViewById(R.id.savePasswordButton);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(view)
                .create();

        cancel.setOnClickListener(v -> dialog.dismiss());

        save.setOnClickListener(v -> {
            String currentPassword = currentPasswordInput
                    .getText()
                    .toString();

            String newPassword = newPasswordInput
                    .getText()
                    .toString();

            String repeatPassword = repeatPasswordInput
                    .getText()
                    .toString();

            boolean valid = true;

            if (currentPassword.length() < 6) {
                showFieldError(
                        currentPasswordBox,
                        currentPasswordError,
                        getString(R.string.six_simbols_minimum)
                );

                valid = false;
            } else {
                showFieldError(
                        currentPasswordBox,
                        currentPasswordError,
                        null
                );
            }

            if (newPassword.length() < 6) {
                showFieldError(
                        newPasswordBox,
                        newPasswordError,
                        getString(R.string.six_simbols_minimum)
                );

                valid = false;
            } else {
                showFieldError(
                        newPasswordBox,
                        newPasswordError,
                        null
                );
            }

            if (!newPassword.equals(repeatPassword)) {
                showFieldError(
                        repeatPasswordBox,
                        repeatPasswordError,
                        getString(R.string.passwords_not_equals)
                );

                valid = false;
            } else {
                showFieldError(
                        repeatPasswordBox,
                        repeatPasswordError,
                        null
                );
            }

            if (!valid) {
                return;
            }

            progress.setVisibility(View.VISIBLE);
            save.setEnabled(false);

            changePassword(
                    currentPassword,
                    newPassword,
                    dialog,
                    progress,
                    save
            );
        });

        dialog.show();

        Window window = dialog.getWindow();

        if (window != null) {
            int width = (int) (getResources()
                    .getDisplayMetrics().widthPixels * 0.92f);

            window.setBackgroundDrawable(
                    new android.graphics.drawable.ColorDrawable(
                            android.graphics.Color.TRANSPARENT
                    )
            );

            window.getDecorView().setPadding(0, 0, 0, 0);
            window.setLayout(
                    width,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
        }
    }

    private void deleteAccountAfterReauthentication() {
        FirebaseUser user = auth.getCurrentUser();

        if (user == null || user.getEmail() == null) {
            showError(getString(R.string.users_not_found));
            return;
        }

        String password = loginPassword.getText()
                .toString()
                .trim();

        if (password.length() < 6) {
            setLoginPasswordError(
                    getString(R.string.invalid_password_text)
            );

            return;
        }

        loginProgress.setVisibility(View.VISIBLE);
        loginButton.setEnabled(false);

        AuthCredential credential =
                EmailAuthProvider.getCredential(
                        user.getEmail(),
                        password
                );

        user.reauthenticate(credential)
                .addOnSuccessListener(unused ->
                        deleteUserData(user)
                )
                .addOnFailureListener(error -> {
                    loginProgress.setVisibility(View.GONE);
                    loginButton.setEnabled(true);

                    showError(
                            getString(R.string.auth_fail_text)
                    );
                });
    }

    private void setLoginPasswordError(String message) {
        boolean hasError =
                message != null && !message.isEmpty();

        passwordBox.setBackgroundResource(
                hasError
                        ? R.drawable.bg_input_box_error
                        : R.drawable.bg_input_box
        );

        loginPasswordError.setText(hasError ? message : "");
        loginPasswordError.setVisibility(
                hasError ? View.VISIBLE : View.GONE
        );
    }

    private void loadUserProfile() {
        FirebaseUser user = auth.getCurrentUser();

        if (user == null) {
            finish();
            return;
        }

        String uid = user.getUid();

        currentEmail.setText(user.getEmail());

        FirebaseDatabase.getInstance()
                .getReference()
                .child("users")
                .child(uid)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (isFinishing() || isDestroyed()) {
                        return;
                    }

                    if (!snapshot.exists()) {
                        showError(
                                getString(R.string.account_load_error)
                        );
                        return;
                    }

                    String publicUsername =
                            snapshot.child("username")
                                    .getValue(String.class);

                    if (publicUsername == null
                            || publicUsername.trim().isEmpty()) {

                        publicUsername = "Пользователь";
                    }

                    String backgroundColor = snapshot.child("background_color")
                            .getValue(String.class);

                    if (backgroundColor != null
                            && backgroundColor.matches("^#[0-9A-Fa-f]{6}$")) {

                        applyBackgroundColor(
                                Color.parseColor(backgroundColor)
                        );

                    }else{
                        applyBackgroundColor(
                                ContextCompat.getColor(this, R.color.account_settings_header_color));
                    }

                    String currentLoginText =
                            snapshot.child("login")
                                    .getValue(String.class);

                    username.setText(publicUsername);

                    usernameMain.setText(publicUsername);

                    currentLogin.setText(currentLoginText);

                    avatarInitial.setText(
                            publicUsername
                                    .substring(0, 1)
                                    .toUpperCase()
                    );
                })
                .addOnFailureListener(error -> {
                    if (isFinishing() || isDestroyed()) {
                        return;
                    }

                    Log.e(
                            "AccountSettings",
                            "Failed to load profile",
                            error
                    );

                    showError(
                            getString(R.string.account_load_error)
                    );
                });
    }

    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void uploadAvatar(Uri uri) {
        FirebaseUser user = auth.getCurrentUser();

        if (user == null) {
            showError(getString(R.string.users_not_found));
            return;
        }

        changeAvatarLayout.setEnabled(false);

        StorageReference avatarRef =
                FirebaseStorage.getInstance()
                        .getReference()
                        .child("profileImages")
                        .child(user.getUid())
                        .child("avatar.jpg");

        avatarRef.putFile(uri)
                .addOnSuccessListener(task ->
                        avatarRef.getDownloadUrl()
                                .addOnSuccessListener(downloadUri -> {
                                    if (isFinishing()
                                            || isDestroyed()) {
                                        return;
                                    }

                                    avatarImage.setVisibility(
                                            View.VISIBLE
                                    );

                                    avatarInitial.setVisibility(
                                            View.GONE
                                    );

                                    GlideApp.with(this)
                                            .load(downloadUri)
                                            .into(avatarImage);

                                    changeAvatarLayout.setEnabled(true);

                                    Toast.makeText(
                                            this,
                                            R.string.saved_text,
                                            Toast.LENGTH_SHORT
                                    ).show();
                                })
                )
                .addOnFailureListener(error -> {
                    if (isFinishing() || isDestroyed()) {
                        return;
                    }

                    changeAvatarLayout.setEnabled(true);

                    showError(
                            getString(R.string.photo_load_error)
                    );
                });
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (auth == null) {
            return;
        }

        FirebaseUser user = auth.getCurrentUser();

        if (user == null) {
            return;
        }

        user.reload()
                .addOnSuccessListener(unused -> {
                    FirebaseUser refreshedUser =
                            auth.getCurrentUser();

                    if (refreshedUser == null) {
                        return;
                    }

                    String actualEmail = refreshedUser.getEmail();

                    currentEmail.setText(actualEmail);

                    String pendingEmail =
                            getSharedPreferences(
                                    "account_settings",
                                    MODE_PRIVATE
                            )
                                    .getString(
                                            "pending_email",
                                            null
                                    );

                    if (pendingEmail != null
                            && pendingEmail.equalsIgnoreCase(
                            actualEmail
                    )) {

                        updateEmailInDatabase(actualEmail);

                        getSharedPreferences(
                                "account_settings",
                                MODE_PRIVATE
                        )
                                .edit()
                                .remove("pending_email")
                                .apply();

                        Toast.makeText(
                                this,
                                getString(
                                        R.string.email_confirming_success
                                ),
                                Toast.LENGTH_SHORT
                        ).show();
                    }
                });
    }

    private void showChangeUsernameDialog() {
        FirebaseUser user = auth.getCurrentUser();

        if (user == null) {
            showError(getString(R.string.users_not_found));
            return;
        }

        if (changeUsernameDialog != null
                && changeUsernameDialog.isShowing()) {
            return;
        }

        changeUsernameDialog = new Dialog(this);
        changeUsernameDialog.setContentView(
                R.layout.dialog_change_username
        );

        Window window = changeUsernameDialog.getWindow();

        if (window != null) {
            window.setBackgroundDrawable(
                    new android.graphics.drawable.ColorDrawable(
                            android.graphics.Color.TRANSPARENT
                    )
            );

            int width = (int) (getResources()
                    .getDisplayMetrics().widthPixels * 0.92f);

            window.setLayout(
                    width,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
        }

        EditText usernameInput =
                changeUsernameDialog.findViewById(
                        R.id.newUsernameInput
                );

        LinearLayout usernameBox =
                changeUsernameDialog.findViewById(
                        R.id.usernameBox
                );

        TextView usernameError =
                changeUsernameDialog.findViewById(
                        R.id.usernameError
                );

        ProgressBar progress =
                changeUsernameDialog.findViewById(
                        R.id.usernameProgress
                );

        Button cancelButton =
                changeUsernameDialog.findViewById(
                        R.id.cancelUsernameButton
                );

        Button saveButton =
                changeUsernameDialog.findViewById(
                        R.id.saveUsernameButton
                );

        usernameInput.setText(username.getText().toString());

        cancelButton.setOnClickListener(v ->
                changeUsernameDialog.dismiss()
        );

        saveButton.setOnClickListener(v -> {
            String newUsername = usernameInput.getText()
                    .toString()
                    .trim();

            String validationError =
                    validateUsername(newUsername);

            if (validationError != null) {
                showUsernameError(
                        usernameBox,
                        usernameError,
                        validationError
                );

                return;
            }

            showUsernameError(usernameBox, usernameError, null);

            progress.setVisibility(View.VISIBLE);
            saveButton.setEnabled(false);
            cancelButton.setEnabled(false);

            updateUsername(
                    user,
                    newUsername,
                    progress,
                    saveButton,
                    cancelButton
            );
        });

        changeUsernameDialog.show();
    }

    private String validateUsername(String value) {
        if (value.isEmpty()) {
            return "Введите публичный никнейм";
        }

        if (value.length() > 30) {
            return "Максимум 30 символов";
        }

        return null;
    }

    private void showUsernameError(
            LinearLayout box,
            TextView errorView,
            String message) {

        boolean hasError =
                message != null && !message.isEmpty();

        box.setBackgroundResource(
                hasError
                        ? R.drawable.bg_input_box_error
                        : R.drawable.bg_input_box
        );

        errorView.setText(hasError ? message : "");
        errorView.setVisibility(
                hasError ? View.VISIBLE : View.GONE
        );
    }

    private void updateUsername(
            FirebaseUser user,
            String newUsername,
            ProgressBar progress,
            Button saveButton,
            Button cancelButton) {

        String uid = user.getUid();

        Map<String, Object> updates = new HashMap<>();

        updates.put(
                "/users/" + uid + "/username",
                newUsername
        );

        updates.put(
                "/publicProfiles/" + uid + "/username",
                newUsername
        );

        FirebaseDatabase.getInstance()
                .getReference()
                .updateChildren(updates)
                .addOnSuccessListener(unused -> {
                    if (isFinishing() || isDestroyed()) {
                        return;
                    }

                    progress.setVisibility(View.GONE);
                    saveButton.setEnabled(true);
                    cancelButton.setEnabled(true);

                    username.setText(newUsername);
                    avatarInitial.setText(
                            newUsername
                                    .substring(0, 1)
                                    .toUpperCase()
                    );

                    if (changeUsernameDialog != null) {
                        changeUsernameDialog.dismiss();
                    }

                    Toast.makeText(
                            this,
                            getString(R.string.saved_text),
                            Toast.LENGTH_SHORT
                    ).show();
                })
                .addOnFailureListener(error -> {
                    if (isFinishing() || isDestroyed()) {
                        return;
                    }

                    Log.e(
                            "ChangeUsername",
                            "Failed to update username",
                            error
                    );

                    progress.setVisibility(View.GONE);
                    saveButton.setEnabled(true);
                    cancelButton.setEnabled(true);

                    showError(
                            getString(R.string.smth_wrong_text)
                    );
                });
    }

    @Override
    protected void onDestroy() {
        if (backgroundColorDialog != null) {
            backgroundColorDialog.dismiss();
            backgroundColorDialog = null;
        }

        super.onDestroy();
    }
}