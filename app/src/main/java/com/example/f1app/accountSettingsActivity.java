package com.example.f1app;

import static com.example.f1app.MainActivity.checkConnection;

import android.app.Dialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.List;

public class accountSettingsActivity extends AppCompatActivity {
    private Button changeAvatarButton;
    private Button loginButton;

    private FirebaseAuth auth;

    private TextView username;
    private TextView currentEmail;
    private TextView avatarInitial;

    private ImageView avatarImage;

    private Dialog deleteDialog;
    private Dialog loginDialog;

    private EditText loginPassword;

    private TextInputLayout tilPassword;

    private ProgressBar loginProgress;

    private String mCurrentSeason;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.account_settings_page);

        WindowInsetsControllerCompat windowInsetsController =
                WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        windowInsetsController.setAppearanceLightStatusBars(false);

        if (!checkConnection(getApplicationContext())){
            startActivity(connectionLostScreen.createShowSplashOnNetworkFailure(accountSettingsActivity.this));
        }else{
            startActivity(connectionLostScreen.createIntentHideSplashOnNetworkRecovery(accountSettingsActivity.this));
        }

        Bundle intentBundle = getIntent().getExtras();

        mCurrentSeason = intentBundle != null
                ? intentBundle.getString("currentSeason")
                : null;

        username = findViewById(R.id.username);
        currentEmail = findViewById(R.id.currentEmail);

        avatarImage = findViewById(R.id.avatarImage);
        avatarInitial = findViewById(R.id.avatarInitial);

        changeAvatarButton = findViewById(R.id.changeAvatarButton);
        Button changeEmailButton = findViewById(R.id.changeEmailButton);
        Button changePasswordButton = findViewById(R.id.changePasswordButton);

        Button deleteButton = findViewById(R.id.deleteAccountButton);
        Button logoutButton = findViewById(R.id.logoutButton);
        ImageButton backButton = findViewById(R.id.backButton);

        auth = FirebaseAuth.getInstance();

        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            finish();
            return;
        }

        username.setText(getString(R.string.usernameText));
        currentEmail.setText(user.getEmail());

        if (user.getEmail() != null && !user.getEmail().isEmpty()) {
            avatarInitial.setText(
                    user.getEmail()
                            .substring(0, 1)
                            .toUpperCase()
            );
        }

        loadUserProfile();
        loadAvatarIfExists(user);

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

        loginPassword = loginDialog.findViewById(R.id.loginPassword);
        tilPassword = loginDialog.findViewById(R.id.password_layout);
        loginButton = loginDialog.findViewById(R.id.loginButton);
        loginProgress = loginDialog.findViewById(R.id.loginProgress);

        loginButton.setOnClickListener(v ->
                deleteAccountAfterReauthentication()
        );

        Button cancelButton = deleteDialog.findViewById(R.id.cancel_button);

        Button confirmButton = deleteDialog.findViewById(R.id.confirm_button);

        confirmButton.setOnClickListener(view -> {
            deleteDialog.dismiss();
            loginPassword.setText("");
            tilPassword.setError(null);
            loginProgress.setVisibility(View.GONE);
            loginButton.setEnabled(true);
            loginDialog.show();
        });

        cancelButton.setOnClickListener(view -> deleteDialog.dismiss());

        backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(view -> {
            Intent i = new Intent(
                    accountSettingsActivity.this,
                    accountPageActivity.class
            );

            i.putExtra("currentSeason", mCurrentSeason);
            i.setFlags(
                    i.getFlags()
                            | Intent.FLAG_ACTIVITY_NEW_TASK
                            | Intent.FLAG_ACTIVITY_CLEAR_TASK
            );

            startActivity(i);
            finish();
            overridePendingTransition(0, 0);
        });

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

        deleteButton.setOnClickListener(view -> deleteDialog.show());

        changeAvatarButton.setOnClickListener(v -> {
            PickVisualMediaRequest request =
                    new PickVisualMediaRequest.Builder()
                            .setMediaType(
                                    ActivityResultContracts.PickVisualMedia
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

        deleteButton.setOnClickListener(v ->
                deleteDialog.show()
        );

        logoutButton.setOnClickListener(v ->
                logout()
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
                                            getString(R.string.email_confirming_text),
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
                                            getString(R.string.confirmation_email_error)
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

        if (user == null) return;

        FirebaseDatabase.getInstance()
                .getReference()
                .child("users")
                .orderByChild("userId")
                .equalTo(user.getUid())
                .addListenerForSingleValueEvent(
                        new ValueEventListener() {
                            @Override
                            public void onDataChange(
                                    @NonNull DataSnapshot snapshot) {

                                for (DataSnapshot userSnapshot :
                                        snapshot.getChildren()) {

                                    userSnapshot.getRef()
                                            .child("userEmail")
                                            .setValue(newEmail);
                                }
                            }

                            @Override
                            public void onCancelled(
                                    @NonNull DatabaseError error) {
                            }
                        });
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
                                            getString(R.string.password_changed),
                                            Toast.LENGTH_SHORT
                                    ).show();
                                })
                                .addOnFailureListener(error -> {
                                    progress.setVisibility(View.GONE);
                                    saveButton.setEnabled(true);
                                    showError(
                                            getString(R.string.password_change_error)
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
        if (user == null) return;

        StorageReference avatarRef =
                FirebaseStorage.getInstance()
                        .getReference()
                        .child("profileImages")
                        .child(user.getUid())
                        .child("avatar.jpg");

        avatarRef.getDownloadUrl()
                .addOnSuccessListener(uri -> {
                    avatarImage.setVisibility(View.VISIBLE);
                    avatarInitial.setVisibility(View.GONE);

                    GlideApp.with(this)
                            .load(uri)
                            .circleCrop()
                            .into(avatarImage);
                })
                .addOnFailureListener(error -> {
                    avatarImage.setVisibility(View.GONE);
                    avatarInitial.setVisibility(View.VISIBLE);
                });
    }

    private void deleteUserData(FirebaseUser user) {
        StorageReference avatarRef =
                FirebaseStorage.getInstance()
                        .getReference()
                        .child("profileImages")
                        .child(user.getUid())
                        .child("avatar.jpg");

        avatarRef.delete()
                .addOnCompleteListener(task ->
                        deleteRealtimeProfile(user)
                );
    }

    private void deleteRealtimeProfile(FirebaseUser user) {
        DatabaseReference usersRef =
                FirebaseDatabase.getInstance()
                        .getReference()
                        .child("users");

        usersRef.orderByChild("userId")
                .equalTo(user.getUid())
                .addListenerForSingleValueEvent(
                        new ValueEventListener() {
                            @Override
                            public void onDataChange(
                                    @NonNull DataSnapshot snapshot) {

                                List<Task<Void>> deleteTasks =
                                        new ArrayList<>();

                                for (DataSnapshot userSnapshot :
                                        snapshot.getChildren()) {

                                    deleteTasks.add(
                                            userSnapshot.getRef()
                                                    .removeValue()
                                    );
                                }

                                Tasks.whenAllComplete(deleteTasks)
                                        .addOnSuccessListener(tasks ->
                                                deleteFirebaseUser(user)
                                        );
                            }

                            @Override
                            public void onCancelled(
                                    @NonNull DatabaseError error) {
                                showError(getString(R.string.delete_user_error));
                            }
                        }
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
                        showError(getString(R.string.delete_user_error)));
    }

    private void showChangeEmailDialog() {
        View view = getLayoutInflater().inflate(R.layout.dialog_change_email, null);

        EditText newEmailInput = view.findViewById(R.id.newEmailInput);
        EditText currentPasswordInput = view.findViewById(R.id.currentPasswordEmailInput);
        TextInputLayout newEmailLayout = view.findViewById(R.id.newEmailLayout);
        TextInputLayout passwordLayout = view.findViewById(R.id.currentPasswordEmailLayout);
        ProgressBar progress = view.findViewById(R.id.emailProgress);
        Button cancel = view.findViewById(R.id.cancelEmailButton);
        Button save = view.findViewById(R.id.saveEmailButton);

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
                newEmailLayout.setError(getString(R.string.email_enter));
                return;
            }

            if (!android.util.Patterns.EMAIL_ADDRESS
                    .matcher(email)
                    .matches()) {
                newEmailLayout.setError(getString(R.string.email_error));
                return;
            }

            if (password.isEmpty()) {
                passwordLayout.setError(getString(R.string.password_enter));
                return;
            }

            getSharedPreferences(
                    "account_settings",
                    MODE_PRIVATE
            )
                    .edit()
                    .putString("pending_email", email)
                    .apply();

            newEmailLayout.setError(null);
            passwordLayout.setError(null);

            progress.setVisibility(View.VISIBLE);
            save.setEnabled(false);

            changeEmail(email, password, dialog, progress, save);
        });

        dialog.setOnShowListener(d -> {
            if (dialog.getWindow() != null) {
                dialog.getWindow().setBackgroundDrawableResource(
                        android.R.color.transparent
                );
            }
        });

        dialog.show();
    }

    private void showChangePasswordDialog() {
        View view = getLayoutInflater().inflate(R.layout.dialog_change_password, null);

        EditText currentPasswordInput = view.findViewById(R.id.currentPasswordInput);
        EditText newPasswordInput = view.findViewById(R.id.newPasswordInput);
        EditText repeatPasswordInput = view.findViewById(R.id.repeatPasswordInput);
        TextInputLayout currentPasswordLayout = view.findViewById(R.id.currentPasswordLayout);

        TextInputLayout newPasswordLayout = view.findViewById(R.id.newPasswordLayout);
        TextInputLayout repeatPasswordLayout = view.findViewById(R.id.repeatPasswordLayout);

        ProgressBar progress = view.findViewById(R.id.passwordProgress);
        Button cancel = view.findViewById(R.id.cancelPasswordButton);
        Button save = view.findViewById(R.id.savePasswordButton);

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
                currentPasswordLayout.setError(
                        getString(R.string.password_enter)
                );
                valid = false;
            } else {
                currentPasswordLayout.setError(null);
            }

            if (newPassword.length() < 6) {
                newPasswordLayout.setError(
                        getString(R.string.six_simbols_minimum)
                );
                valid = false;
            } else {
                newPasswordLayout.setError(null);
            }

            if (!newPassword.equals(repeatPassword)) {
                repeatPasswordLayout.setError(
                        getString(R.string.passwords_not_equals)
                );
                valid = false;
            } else {
                repeatPasswordLayout.setError(null);
            }

            if (!valid) return;

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
            tilPassword.setError(
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
                    showError(getString(R.string.auth_fail_text));
                });
    }

    private void loadUserProfile() {
        FirebaseUser user = auth.getCurrentUser();

        if (user == null) {
            finish();
            return;
        }

        currentEmail.setText(user.getEmail());

        FirebaseDatabase.getInstance()
                .getReference()
                .child("users")
                .orderByChild("userId")
                .equalTo(user.getUid())
                .addListenerForSingleValueEvent(
                        new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                for (DataSnapshot userSnapshot : snapshot.getChildren()) {

                                    String name = userSnapshot.getKey();

                                    username.setText(name);

                                    if (name != null &&
                                            !name.isEmpty()) {
                                        avatarInitial.setText(
                                                name.substring(0, 1)
                                                        .toUpperCase()
                                        );
                                    }

                                    break;
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {
                                showError(
                                        getString(R.string.account_load_error)
                                );
                            }
                        }
                );
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

        changeAvatarButton.setEnabled(false);

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
                                    avatarImage.setVisibility(View.VISIBLE);
                                    avatarInitial.setVisibility(View.GONE);

                                    GlideApp.with(this)
                                            .load(downloadUri)
                                            .circleCrop()
                                            .into(avatarImage);

                                    changeAvatarButton.setEnabled(true);
                                    Toast.makeText(
                                            this,
                                            R.string.saved_text,
                                            Toast.LENGTH_SHORT
                                    ).show();
                                })
                )
                .addOnFailureListener(error -> {
                    changeAvatarButton.setEnabled(true);
                    showError(getString(R.string.photo_load_error));
                });
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (auth == null) return;

        FirebaseUser user = auth.getCurrentUser();

        if (user == null) return;

        user.reload()
                .addOnSuccessListener(unused -> {
                    FirebaseUser refreshedUser =
                            auth.getCurrentUser();

                    if (refreshedUser == null) return;

                    String actualEmail =
                            refreshedUser.getEmail();

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

                    if (pendingEmail != null &&
                            pendingEmail.equalsIgnoreCase(actualEmail)) {

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
                                getString(R.string.email_confirming_success),
                                Toast.LENGTH_SHORT
                        ).show();
                    }
                });
    }
}
