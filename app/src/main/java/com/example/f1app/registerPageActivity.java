package com.example.f1app;

import static com.example.f1app.MainActivity.checkConnection;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import android.content.Intent;
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
import android.widget.Toast;

import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.shawnlin.numberpicker.NumberPicker;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;

public class registerPageActivity extends AppCompatActivity {
    FirebaseAuth auth;
    EditText editTextUsername, editTextEmail, editTextPassword;
    Button registerButton;
    ProgressBar registerProgress;
    TextInputLayout til_username, til_email, til_password;
    String email, password, username;
    NumberPicker driverPicker, teamPicker;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.register_page);

        if (!checkConnection(getApplicationContext())){
            startActivity(connectionLostScreen.createShowSplashOnNetworkFailure(registerPageActivity.this));
        }else{
            startActivity(connectionLostScreen.createIntentHideSplashOnNetworkRecovery(registerPageActivity.this));
        }

        editTextUsername = findViewById(R.id.signUpUsername);
        editTextEmail = findViewById(R.id.signUpEmail);
        editTextPassword = findViewById(R.id.signUpPassword);
        registerButton = findViewById(R.id.registerButton);
        driverPicker = findViewById(R.id.driver_picker);
        teamPicker = findViewById(R.id.team_picker);
        registerProgress = findViewById(R.id.registerProgress);

        LocalDate currentDate = LocalDate.now();
        String currentYear = Integer.toString(currentDate.getYear());

        WindowInsetsControllerCompat windowInsetsController =
                WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        windowInsetsController.setAppearanceLightStatusBars(false);


        ArrayList<String> driversList = new ArrayList<>();
        DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference();
        rootRef.child("driverLineUp/season").child(currentYear).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for(DataSnapshot teamSnapshot: snapshot.getChildren()){
                    for(DataSnapshot driverSnaphot: teamSnapshot.child("drivers").getChildren()){
                        driversList.add(driverSnaphot.getKey());
                    }
                }
                driverPicker.setMinValue(1);
                driverPicker.setMaxValue(driversList.size());
                Collections.sort(driversList, String.CASE_INSENSITIVE_ORDER);
                Collections.reverse(driversList);
                driversList.add(getString(R.string.nobody));
                Collections.reverse(driversList);
                driverPicker.setDisplayedValues(driversList.toArray(new String[driversList.size()]));
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("registerPageActivity", "Error while getting drivers. Error:" + error.getMessage());
            }
        });

        ArrayList<String> teamList = new ArrayList<>();
        teamList.add(getString(R.string.nobody));
        rootRef.child("constructors").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for(DataSnapshot teamSnapshot: snapshot.getChildren()){
                    teamList.add(teamSnapshot.child("name").getValue(String.class));
                }
                teamPicker.setMinValue(1);
                teamPicker.setMaxValue(teamList.size());
                teamPicker.setDisplayedValues(teamList.toArray(new String[teamList.size()]));
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("registerPageActivity", "Error while getting constructors. Error:" + error.getMessage());
            }
        });

        til_username = findViewById(R.id.username_layout);
        til_email = findViewById(R.id.email_layout);
        til_password = findViewById(R.id.password_layout);

        auth = FirebaseAuth.getInstance();


        registerButton.setOnClickListener(v -> registerNewUser(driversList, teamList));

        ImageButton backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> finish());

        editTextEmail.addTextChangedListener(textWatcher);
        editTextPassword.addTextChangedListener(textWatcher);
        editTextUsername.addTextChangedListener(textWatcher);


    }

    private final TextWatcher textWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            email = editTextEmail.getText().toString().trim();
            password = editTextPassword.getText().toString().trim();
            username = editTextUsername.getText().toString().trim();

            if(!username.isEmpty()){
                DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference();
                rootRef.child("users").addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if(snapshot.hasChild(username)){
                            til_username.setError(getString(R.string.invalid_username_text));
                        }else{
                            til_username.setError(null);
                        }
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e("registerPageActivity", "Error while checking username. Error:" + error.getMessage());
                    }
                });
            }

            if(password.length()<6 && !password.isEmpty()){
                til_password.setError(getString(R.string.invalid_password_text));
            }else{
                til_password.setError(null);
            }

            if(!Patterns.EMAIL_ADDRESS.matcher(email).matches() && !email.isEmpty()){
                til_email.setError(getString(R.string.invalid_email_text));
            }else{
                til_email.setError(null);
            }

            registerButton.setEnabled(!email.isEmpty() && !(password.length() < 6) && !username.isEmpty());
        }

        @Override
        public void afterTextChanged(Editable editable) {}
    };

    private void registerNewUser(ArrayList<String> driversList, ArrayList<String> teamList) {
        registerProgress.setVisibility(View.VISIBLE);
        registerButton.setVisibility(View.INVISIBLE);

        email = editTextEmail.getText().toString().trim();
        password = editTextPassword.getText().toString().trim();
        username = editTextUsername.getText().toString().trim();


        String driverPickerValue = driversList.get(driverPicker.getValue() - 1);
        String choiceDriver;
        String choiceTeam;
        String teamPickerValue = teamList.get(teamPicker.getValue() - 1);
        if (driverPickerValue.equals(getString(R.string.nobody))){
            choiceDriver = "null";
        }else{
            choiceDriver = driverPickerValue;
        }

        if (teamPickerValue.equals(getString(R.string.nobody))){
            choiceTeam = "null";
        }else{
            choiceTeam = teamPickerValue;
        }

        if(til_email.getError() != null || til_password.getError() != null || til_username.getError() != null){
            Toast.makeText(registerPageActivity.this, getString(R.string.all_fields_text), Toast.LENGTH_LONG).show();
        }else{
            auth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(registerPageActivity.this, task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(registerPageActivity.this, getString(R.string.reqistration_succ_text), Toast.LENGTH_LONG).show();
                            createNewUser(task.getResult().getUser(), username, choiceDriver, choiceTeam);
                            startActivity(new Intent(registerPageActivity.this, logInPageActivity.class));
                            finish();
                        } else {
                            registerProgress.setVisibility(View.INVISIBLE);
                            registerButton.setVisibility(View.VISIBLE);
                            Toast.makeText(registerPageActivity.this, getString(R.string.reqistration_fail_text), Toast.LENGTH_LONG).show();
                        }
                    });
        }
    }

    private void createNewUser(FirebaseUser userFromRegistration, String username, String choiceDriver,
                               String choiceTeam) {
        String email = userFromRegistration.getEmail();
        String userId = userFromRegistration.getUid();
        userData user = new userData(userId, email, choiceDriver, choiceTeam);

        DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference();
        rootRef.child("users").child(username).setValue(user);
    }
}