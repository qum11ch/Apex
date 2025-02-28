package com.example.f1app;

import static kotlin.jvm.internal.Reflection.function;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class logInPageActivity extends AppCompatActivity {
    FirebaseAuth auth;
    Button showDriverButton, showDriverStanding, showTeams, showHomePage, showAccount;
    EditText editTextUsername, editTextPassword;
    Button loginButton;
    ProgressBar loginProgress;
    LinearLayout signUpLayout;
    TextInputLayout til_username, til_password;
    private ImageButton backButton;
    String password, username;
    SharedPreferences prefs;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login_page);

        WindowInsetsControllerCompat windowInsetsController =
                WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        windowInsetsController.setAppearanceLightStatusBars(false);

        editTextUsername = findViewById(R.id.loginUsername);
        editTextPassword = findViewById(R.id.loginPassword);
        loginButton = findViewById(R.id.loginButton);

        signUpLayout = findViewById(R.id.signUpLayout);
        signUpLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), registerPageActivity.class);
                startActivity(intent);
            }
        });

        loginProgress = findViewById(R.id.loginProgress);
        til_username = (TextInputLayout) findViewById(R.id.username_layout);
        til_password = (TextInputLayout) findViewById(R.id.password_layout);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if(user!=null){
            Log.i("checkUSer"," " + user.getEmail());
            Intent i = new Intent(logInPageActivity.this, accountPageActivity.class);
            startActivity(i);
            finish();
        }else{
            Log.i("checkUSer", "NO USER");
        }

        TextView resetPassword = (TextView) findViewById(R.id.resetPassword);
        resetPassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(logInPageActivity.this, resetPageActivity.class);
                startActivity(i);
            }
        });

        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loginUserAccount();
            }
        });

        showDriverButton = (Button) findViewById(R.id.showDriver);
        showDriverButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(logInPageActivity.this, driversStandingsActivity.class);
                logInPageActivity.this.startActivity(intent);
            }
        });

        showDriverStanding = (Button) findViewById(R.id.showStandingsDriver);
        showDriverStanding.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(logInPageActivity.this, schuduleActivity.class);
                logInPageActivity.this.startActivity(intent);
            }
        });

        showHomePage = (Button) findViewById(R.id.showHomePage);
        showHomePage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(logInPageActivity.this, MainActivity.class);
                logInPageActivity.this.startActivity(intent);
            }
        });

        showTeams = (Button) findViewById(R.id.showTeams);
        showTeams.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(logInPageActivity.this, teamsStandingsActivity.class);
                logInPageActivity.this.startActivity(intent);
            }
        });

        backButton = (ImageButton) findViewById(R.id.backButton);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

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
                til_password.setError("Password must has at least 6 symbols");
            }else{
                til_password.setError(null);
            }

            loginButton.setEnabled(!(password.length() < 6) && !username.isEmpty());
        }

        @Override
        public void afterTextChanged(Editable editable) {}
    };

    private void loginUserAccount() {
        loginProgress.setVisibility(View.VISIBLE);
        loginButton.setVisibility(View.INVISIBLE);

        String username = editTextUsername.getText().toString().trim();
        String password = editTextPassword.getText().toString().trim();

        if (username.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please enter credentials", Toast.LENGTH_LONG).show();
            return;
        }

        if(til_password.getError() != null || til_username.getError() != null) {
            Toast.makeText(logInPageActivity.this, "All field must be filled", Toast.LENGTH_LONG).show();
        }else{
            DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference();
            rootRef.child("users").child(username).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    String email = snapshot.child("userEmail").getValue(String.class);
                    auth = FirebaseAuth.getInstance();
                    auth.signInWithEmailAndPassword(email, password)
                            .addOnCompleteListener(logInPageActivity.this, new OnCompleteListener<AuthResult>() {
                                @Override
                                public void onComplete(@NonNull Task<AuthResult> task) {
                                    if (task.isSuccessful()) {
                                        Toast.makeText(logInPageActivity.this, "Login successful!!", Toast.LENGTH_LONG).show();
                                        startActivity(new Intent(logInPageActivity.this, accountPageActivity.class));
                                        finish();
                                    } else {
                                        loginProgress.setVisibility(View.INVISIBLE);
                                        loginButton.setVisibility(View.VISIBLE);
                                        Toast.makeText(logInPageActivity.this, "Login failed!!", Toast.LENGTH_LONG).show();
                                    }
                                }
                            });
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e("loginPageActivity", "Error while getting user`s email. Error:" + error.getMessage());
                }
            });
        }
    }
}
