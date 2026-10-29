package com.example.f1app;

import static com.example.f1app.MainActivity.checkConnection;
import static com.example.f1app.driversStandingsActivity.startActivity_seasonData;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;

public class resetPageActivity extends AppCompatActivity {
    private FirebaseAuth auth;
    private EditText userEmail;
    private Button resetButton, backButton;
    private ProgressBar resetProgress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        setContentView(R.layout.password_reset_page);
        setupResetPasswordInsets();

        if (!checkConnection(getApplicationContext())){
            startActivity(connectionLostScreen.createShowSplashOnNetworkFailure(resetPageActivity.this));
        }else{
            startActivity(connectionLostScreen.createIntentHideSplashOnNetworkRecovery(resetPageActivity.this));
        }

        Bundle intentBundle = getIntent().getExtras();
        String mCurrentSeason = intentBundle.getString("currentSeason");

        userEmail = findViewById(R.id.userEmail);
        resetButton = findViewById(R.id.resetPassword);

        resetProgress = findViewById(R.id.resetProgress);

        resetButton.setOnClickListener(v -> loginUserAccount(mCurrentSeason));

        userEmail.addTextChangedListener(textWatcher);

        backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> finish());
    }

    private void setupResetPasswordInsets() {
        View root = findViewById(R.id.reset_root);

        final int initialLeft = root.getPaddingLeft();
        final int initialTop = root.getPaddingTop();
        final int initialRight = root.getPaddingRight();
        final int initialBottom = root.getPaddingBottom();

        ViewCompat.setOnApplyWindowInsetsListener(root, (view, windowInsets) -> {
            Insets safeInsets = windowInsets.getInsets(
                    WindowInsetsCompat.Type.systemBars()
                            | WindowInsetsCompat.Type.displayCutout()
            );

            Insets imeInsets = windowInsets.getInsets(
                    WindowInsetsCompat.Type.ime()
            );

            view.setPadding(
                    initialLeft + safeInsets.left,
                    initialTop + safeInsets.top,
                    initialRight + safeInsets.right,
                    initialBottom + Math.max(
                            safeInsets.bottom,
                            imeInsets.bottom
                    )
            );

            return WindowInsetsCompat.CONSUMED;
        });

        ViewCompat.requestApplyInsets(root);
    }

    private final TextWatcher textWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            String value = s.toString().trim();

            if (value.isEmpty()) {
                setEmailError(null);
            } else if (!Patterns.EMAIL_ADDRESS.matcher(value).matches()) {
                setEmailError(getString(R.string.invalid_email_text));
            } else {
                setEmailError(null);
            }
        }

        @Override
        public void afterTextChanged(Editable editable) {}
    };

    private void loginUserAccount(String currentSeason) {
        String emailAddress = userEmail.getText().toString().trim();

        if (emailAddress.isEmpty()) {
            setEmailError(getString(R.string.empty_credentials_text));
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(emailAddress).matches()) {
            setEmailError(getString(R.string.invalid_email_text));
            return;
        }

        resetButton.setVisibility(View.INVISIBLE);
        resetProgress.setVisibility(View.VISIBLE);

        auth = FirebaseAuth.getInstance();
        auth.sendPasswordResetEmail(emailAddress)
                .addOnSuccessListener(unused -> {
                    Toast.makeText(
                            resetPageActivity.this,
                            getString(R.string.reset_password_text),
                            Toast.LENGTH_SHORT
                    ).show();

                    startActivity_seasonData(
                            resetPageActivity.this,
                            logInPageActivity.class,
                            currentSeason
                    );
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(
                            resetPageActivity.this,
                            getString(R.string.wrong_email),
                            Toast.LENGTH_LONG
                    ).show();

                    resetButton.setVisibility(View.VISIBLE);
                    resetProgress.setVisibility(View.INVISIBLE);
                });
    }

    private void setEmailError(String message) {
        LinearLayout box = findViewById(R.id.emailBox);
        TextView error = findViewById(R.id.emailError);

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
