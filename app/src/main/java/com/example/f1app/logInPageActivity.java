package com.example.f1app;

import static com.example.f1app.MainActivity.checkConnection;
import static com.example.f1app.driversStandingsActivity.startActivity_seasonData;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;

import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class logInPageActivity extends AppCompatActivity {
    FirebaseAuth auth;
    ImageButton showDrivers, showSchedule, showTeams, showHomePage;
    EditText editTextUsername, editTextPassword;
    Button loginButton;
    ProgressBar loginProgress;
    LinearLayout signUpLayout;
    TextInputLayout til_username, til_password;
    String password, username;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login_page);

        if (!checkConnection(getApplicationContext())){
            startActivity(connectionLostScreen.createShowSplashOnNetworkFailure(logInPageActivity.this));
        }else{
            startActivity(connectionLostScreen.createIntentHideSplashOnNetworkRecovery(logInPageActivity.this));
        }

        Bundle intentBundle = getIntent().getExtras();
        String mCurrentSeason = intentBundle.getString("currentSeason");

        WindowInsetsControllerCompat windowInsetsController =
                WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        windowInsetsController.setAppearanceLightStatusBars(false);

        editTextUsername = findViewById(R.id.loginUsername);
        editTextPassword = findViewById(R.id.loginPassword);
        loginButton = findViewById(R.id.loginButton);

        signUpLayout = findViewById(R.id.signUpLayout);
        signUpLayout.setOnClickListener(v -> startActivity_seasonData(logInPageActivity.this, registerPageActivity.class, mCurrentSeason));

        loginProgress = findViewById(R.id.loginProgress);
        til_username = findViewById(R.id.username_layout);
        til_password = findViewById(R.id.password_layout);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if(user!=null){
            startActivity_seasonData(logInPageActivity.this, accountPageActivity.class, mCurrentSeason);
            finish();
        }

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
        editTextUsername.addTextChangedListener(textWatcher);
    }

    private TextWatcher textWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            password = editTextPassword.getText().toString().trim();
            username = editTextUsername.getText().toString().trim();

            if(password.length()<6 && !password.isEmpty()){
                til_password.setError(getString(R.string.invalid_password_text));
            }else{
                til_password.setError(null);
            }

            loginButton.setEnabled(!(password.length() < 6) && !username.isEmpty());
        }

        @Override
        public void afterTextChanged(Editable editable) {}
    };

    private void loginUserAccount(String currentSeason) {
        loginProgress.setVisibility(View.VISIBLE);
        loginButton.setVisibility(View.INVISIBLE);

        String username = editTextUsername.getText().toString().trim();
        String password = editTextPassword.getText().toString().trim();

        if (username.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, getString(R.string.empty_credentials_text), Toast.LENGTH_LONG).show();
            return;
        }

        if(til_password.getError() != null || til_username.getError() != null) {
            Toast.makeText(logInPageActivity.this, getString(R.string.all_fields_text), Toast.LENGTH_LONG).show();
        }else{
            DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference();
            rootRef.child("users").child(username).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (!snapshot.hasChild("userEmail")){
                        loginProgress.setVisibility(View.INVISIBLE);
                        loginButton.setVisibility(View.VISIBLE);
                        Toast.makeText(logInPageActivity.this, getString(R.string.auth_fail_text), Toast.LENGTH_LONG).show();
                    }else{
                        String email = snapshot.child("userEmail").getValue(String.class);
                        auth = FirebaseAuth.getInstance();
                        auth.signInWithEmailAndPassword(email, password)
                                .addOnCompleteListener(logInPageActivity.this, task -> {
                                    if (task.isSuccessful()) {
                                        Toast.makeText(logInPageActivity.this, getString(R.string.login_succ_text), Toast.LENGTH_LONG).show();
                                        startActivity_seasonData(logInPageActivity.this, accountPageActivity.class, currentSeason);
                                        finish();
                                    } else {
                                        loginProgress.setVisibility(View.INVISIBLE);
                                        loginButton.setVisibility(View.VISIBLE);
                                        Toast.makeText(logInPageActivity.this, getString(R.string.auth_fail_text), Toast.LENGTH_LONG).show();
                                    }
                                });
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e("loginPageActivity", "Error while getting user`s email. Error:" + error.getMessage());
                }
            });
        }
    }
}
