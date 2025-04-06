package com.example.f1app;

import static com.example.f1app.MainActivity.checkConnection;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;

public class resetPageActivity extends AppCompatActivity {
    FirebaseAuth auth;
    EditText userEmail;
    Button resetButton, backButton;
    ProgressBar resetProgress;
    TextInputLayout til_email;
    String email;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.password_reset_page);

        if (!checkConnection(getApplicationContext())){
            startActivity(connectionLostScreen.createShowSplashOnNetworkFailure(resetPageActivity.this));
        }else{
            startActivity(connectionLostScreen.createIntentHideSplashOnNetworkRecovery(resetPageActivity.this));
        }
        userEmail = findViewById(R.id.userEmail);
        resetButton = findViewById(R.id.resetPassword);

        til_email = findViewById(R.id.email_layout);

        resetProgress = findViewById(R.id.resetProgress);

        resetButton.setOnClickListener(v -> loginUserAccount());

        userEmail.addTextChangedListener(textWatcher);

        backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> finish());

        WindowInsetsControllerCompat windowInsetsController =
                WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        windowInsetsController.setAppearanceLightStatusBars(true);
    }

    private final TextWatcher textWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            email = userEmail.getText().toString().trim();

            if(!Patterns.EMAIL_ADDRESS.matcher(email).matches() && !email.isEmpty()){
                til_email.setError(getString(R.string.invalid_email_text));
            }else{
                til_email.setError(null);
            }
        }

        @Override
        public void afterTextChanged(Editable editable) {}
    };

    private void loginUserAccount() {
        resetButton.setVisibility(View.INVISIBLE);
        resetProgress.setVisibility(View.VISIBLE);

        String emailAddress = userEmail.getText().toString().trim();

        if (emailAddress.isEmpty()) {
            resetProgress.setVisibility(View.INVISIBLE);
            resetButton.setVisibility(View.VISIBLE);
            Toast.makeText(this, getString(R.string.empty_credentials_text), Toast.LENGTH_LONG).show();
            return;
        }

        if(til_email.getError()==null){
            auth = FirebaseAuth.getInstance();

            auth.sendPasswordResetEmail(emailAddress)
                    .addOnSuccessListener(unesed -> {
                        Toast.makeText(resetPageActivity.this, getString(R.string.reset_password_text), Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(resetPageActivity.this, logInPageActivity.class);
                        resetPageActivity.this.startActivity(intent);
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(resetPageActivity.this, getString(R.string.wrong_email), Toast.LENGTH_LONG).show();
                        resetButton.setVisibility(View.VISIBLE);
                        resetProgress.setVisibility(View.INVISIBLE);
                    });
        }else{
            resetProgress.setVisibility(View.INVISIBLE);
            resetButton.setVisibility(View.VISIBLE);
        }
    }
}
