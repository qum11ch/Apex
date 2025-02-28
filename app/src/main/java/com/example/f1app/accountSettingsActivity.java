package com.example.f1app;

import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.preference.PreferenceManager;

import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.EmailAuthProvider;
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

public class accountSettingsActivity extends AppCompatActivity {
    ImageButton backButton;
    EditText loginUsername, loginPassword;
    Button saveButton, deleteButton, changePasswordButton, loginButton;
    FirebaseAuth auth;
    TextView username;
    TextInputLayout til_password;
    NumberPicker driverPicker, teamPicker;
    Dialog deleteDialog, loginDialog;
    ProgressBar loginProgress, saveProgress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.account_settings_page);

        WindowInsetsControllerCompat windowInsetsController =
                WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        windowInsetsController.setAppearanceLightStatusBars(false);

        username = findViewById(R.id.username);
        driverPicker = findViewById(R.id.driver_picker);
        teamPicker = findViewById(R.id.team_picker);
        saveButton = findViewById(R.id.saveButton);
        deleteButton = findViewById(R.id.deleteAccountButton);
        changePasswordButton = findViewById(R.id.changePasswordButton);
        saveProgress = findViewById(R.id.saveProgress);


        LocalDate currentDate = LocalDate.now();
        String currentYear = Integer.toString(currentDate.getYear());

        auth = FirebaseAuth.getInstance();
        FirebaseUser user = auth.getCurrentUser();
        String userId = user.getUid();


        ArrayList<String> driversList = new ArrayList<>();
        ArrayList<String> teamList = new ArrayList<>();

        DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference();
        rootRef.child("users").orderByChild("userId").equalTo(userId)
                .addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for(DataSnapshot userSnap: snapshot.getChildren()){
                    String choiceTeam = userSnap.child("choiceTeam").getValue(String.class);
                    String choiceDriver = userSnap.child("choiceDriver").getValue(String.class);
                    String mUsername = userSnap.getKey();

                    username.setText(mUsername);

                    DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference();
                    rootRef.child("driverLineUp/season").child(currentYear).addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            for(DataSnapshot teamSnapshot: snapshot.getChildren()){
                                for(DataSnapshot driverSnaphot: teamSnapshot.child("drivers").getChildren()){
                                    if (!driverSnaphot.getKey().equals(choiceDriver)){
                                        driversList.add(driverSnaphot.getKey());
                                    }
                                }
                            }
                            driverPicker.setMinValue(1);
                            driverPicker.setMaxValue(driversList.size());
                            Collections.sort(driversList, String.CASE_INSENSITIVE_ORDER);
                            Collections.reverse(driversList);
                            driversList.add("Nobody");
                            driversList.add(choiceDriver);
                            Collections.reverse(driversList);
                            driverPicker.setDisplayedValues(driversList.toArray(new String[driversList.size()]));
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            Log.e("accountSettingPageActivity", "Error while getting drivers. Error:" + error.getMessage());
                        }
                    });

                    rootRef.child("constructors").addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            for(DataSnapshot teamSnapshot: snapshot.getChildren()){
                                if (!teamSnapshot.child("name").getValue(String.class).equals(choiceTeam)){
                                    teamList.add(teamSnapshot.child("name").getValue(String.class));
                                }
                            }
                            teamPicker.setMinValue(1);
                            teamPicker.setMaxValue(teamList.size());
                            Collections.reverse(teamList);
                            teamList.add("Nobody");
                            teamList.add(choiceTeam);
                            Collections.reverse(teamList);
                            teamPicker.setDisplayedValues(teamList.toArray(new String[teamList.size()]));
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            Log.e("accountSettingPageActivity", "Error while getting constructors. Error:" + error.getMessage());
                        }
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("accountSettingPageActivity", "Error while getting user. Error:" + error.getMessage());
            }
        });

        deleteDialog = new Dialog(accountSettingsActivity.this);
        deleteDialog.setContentView(R.layout.delete_dialog_box);
        deleteDialog.getWindow().setLayout(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        deleteDialog.getWindow().setBackgroundDrawable(getDrawable(R.drawable.custom_dialog_bg));
        deleteDialog.setCancelable(false);

        loginDialog = new Dialog(accountSettingsActivity.this);
        loginDialog.setContentView(R.layout.login_dialog_box);
        loginDialog.getWindow().setLayout(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        loginDialog.getWindow().setBackgroundDrawable(getDrawable(R.drawable.custom_dialog_bg));
        loginDialog.setCancelable(true);

        loginUsername = loginDialog.findViewById(R.id.loginUsername);
        loginPassword = loginDialog.findViewById(R.id.loginPassword);
        til_password = loginDialog.findViewById(R.id.password_layout);
        TextView resetPassword = loginDialog.findViewById(R.id.resetPassword);
        loginButton = loginDialog.findViewById(R.id.loginButton);
        loginProgress = loginDialog.findViewById(R.id.loginProgress);

        loginUsername.addTextChangedListener(textWatcher);
        loginPassword.addTextChangedListener(textWatcher);

        Button cancelButton = deleteDialog.findViewById(R.id.cancel_button);
        Button confirmButton = deleteDialog.findViewById(R.id.confirm_button);

        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                deleteDialog.dismiss();
            }
        });

        confirmButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                deleteDialog.dismiss();
                loginDialog.show();
            }
        });

        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                deleteUserAccount();
                loginDialog.dismiss();
                startActivity(new Intent(accountSettingsActivity.this,
                        MainActivity.class));
                finish();
            }
        });

        resetPassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(accountSettingsActivity.this,
                        resetPageActivity.class);
                startActivity(i);
            }
        });

        backButton = (ImageButton) findViewById(R.id.backButton);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                updateChoices(driversList, teamList);
                Intent i = new Intent(accountSettingsActivity.this,
                        accountPageActivity.class);
                startActivity(i);
            }
        });

        deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                deleteDialog.show();
            }
        });

        changePasswordButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(accountSettingsActivity.this,
                        changeUserPrivateDataActivity.class));
            }
        });

    }

    private TextWatcher textWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            String mLoginUsername = loginUsername.getText().toString().trim();
            String mLoginPassword = loginPassword.getText().toString().trim();

            if(mLoginPassword.length()<6 && !mLoginPassword.isEmpty()){
                til_password.setError("Password must has at least 6 symbols");
            }else{
                til_password.setError(null);
            }

            if(!mLoginUsername.isEmpty()&&mLoginPassword.length()>5){
                loginButton.setEnabled(true);
            }
        }

        @Override
        public void afterTextChanged(Editable editable) {}
    };

    private void deleteUserAccount() {
        loginProgress.setVisibility(View.VISIBLE);
        loginButton.setVisibility(View.INVISIBLE);

        String username = loginUsername.getText().toString().trim();
        String password = loginPassword.getText().toString().trim();

        if (username.isEmpty() || password.isEmpty()) {
            Toast.makeText(accountSettingsActivity.this, "Please enter credentials",
                    Toast.LENGTH_LONG).show();
            return;
        }

        if(til_password.getError() != null) {
            Toast.makeText(accountSettingsActivity.this, "All field must be filled",
                    Toast.LENGTH_LONG).show();
        }else{
            DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference();
            final FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            AuthCredential credential = EmailAuthProvider
                    .getCredential(user.getEmail(), password);
            user.reauthenticate(credential)
                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if(task.isSuccessful()){
                                user.delete()
                                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                                            @Override
                                            public void onComplete(@NonNull Task<Void> task) {
                                                if (task.isSuccessful()) {
                                                    rootRef.child("users").child(username)
                                                            .removeValue();
                                                    Toast.makeText(accountSettingsActivity.this,
                                                            "Your account has been deleted.",
                                                            Toast.LENGTH_LONG).show();
                                                }else{
                                                    Toast.makeText(accountSettingsActivity.this,
                                                            "Something went wrong. Please try again later.",
                                                            Toast.LENGTH_LONG).show();
                                                }
                                            }
                                        });

                            }else{
                                loginProgress.setVisibility(View.INVISIBLE);
                                loginButton.setVisibility(View.VISIBLE);
                                Toast.makeText(accountSettingsActivity.this,
                                        "Authentication Failed. Wrong username or password. Please try again later.",
                                        Toast.LENGTH_LONG).show();
                            }
                        }
                    });
        }
    }

    private void updateChoices(ArrayList<String> driversList, ArrayList<String> teamList){
        saveProgress.setVisibility(View.VISIBLE);
        saveButton.setVisibility(View.INVISIBLE);
        String choiceDriver = driversList.get(driverPicker.getValue() - 1);
        String choiceTeam = teamList.get(teamPicker.getValue() - 1);

        String userId = auth.getUid();

        DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference();
        rootRef.child("users").orderByChild("userId").equalTo(userId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for(DataSnapshot userSnap: snapshot.getChildren()){
                    String username = userSnap.getKey();
                    rootRef.child("users").child(username)
                            .child("choiceTeam").setValue(choiceTeam);
                    rootRef.child("users").child(username)
                            .child("choiceDriver").setValue(choiceDriver);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                saveProgress.setVisibility(View.INVISIBLE);
                saveButton.setVisibility(View.VISIBLE);
                Log.e("accountSettingPageActivity", "Error while getting user. Error:" + error.getMessage());
            }
        });
    }
}
