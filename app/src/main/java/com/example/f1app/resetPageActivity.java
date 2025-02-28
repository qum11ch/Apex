package com.example.f1app;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
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

public class resetPageActivity extends AppCompatActivity {
    FirebaseAuth auth;
    EditText userEmail;
    Button resetButton, backButton;
    ProgressBar resetProgress;
    TextInputLayout til_email, til_password;
    String email;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.password_reset_page);
        userEmail = findViewById(R.id.userEmail);
        resetButton = findViewById(R.id.resetPassword);

        til_email = (TextInputLayout) findViewById(R.id.email_layout);

        resetProgress = findViewById(R.id.resetProgress);

        resetButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loginUserAccount();
            }
        });

        userEmail.addTextChangedListener(textWatcher);

        backButton = (Button) findViewById(R.id.backButton);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        WindowInsetsControllerCompat windowInsetsController =
                WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        windowInsetsController.setAppearanceLightStatusBars(true);
    }

    private TextWatcher textWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            email = userEmail.getText().toString().trim();

            if(!Patterns.EMAIL_ADDRESS.matcher(email).matches() && !email.isEmpty()){
                til_email.setError("Invalid email");
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

        if(til_email.getError()==null){
            auth = FirebaseAuth.getInstance();
            String emailAddress = userEmail.getText().toString().trim();

            auth.sendPasswordResetEmail(emailAddress)
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void unesed) {
                            Toast.makeText(resetPageActivity.this, "Reset password link has been sent to your registered Email", Toast.LENGTH_SHORT).show();
                            Intent intent = new Intent(resetPageActivity.this, logInPageActivity.class);
                            resetPageActivity.this.startActivity(intent);
                            finish();
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Toast.makeText(resetPageActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                            Log.e("resetPageActivityError", " " + e.getMessage());
                            resetButton.setVisibility(View.VISIBLE);
                            resetProgress.setVisibility(View.INVISIBLE);
                        }
                    });
        }
    }
}
