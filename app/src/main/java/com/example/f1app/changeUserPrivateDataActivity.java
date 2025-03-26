package com.example.f1app;

import static com.example.f1app.MainActivity.checkConnection;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
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

public class changeUserPrivateDataActivity extends AppCompatActivity {
    Dialog loginDialog;
    EditText loginUsername, loginPassword, userEmail, userPassword;
    FirebaseAuth auth;
    FirebaseUser user;
    Button saveChanges, backButton, loginButton;
    ProgressBar loginProgress;
    TextInputLayout til_email, til_password, login_til_password;
    String mUserId, mUserEmail;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.change_private_data_page);

        WindowInsetsControllerCompat windowInsetsController =
                WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        windowInsetsController.setAppearanceLightStatusBars(true);

        if (!checkConnection(getApplicationContext())){
            startActivity(connectionLostScreen.createShowSplashOnNetworkFailure(changeUserPrivateDataActivity.this));
        }else{
            startActivity(connectionLostScreen.createIntentHideSplashOnNetworkRecovery(changeUserPrivateDataActivity.this));
        }

        auth = FirebaseAuth.getInstance();
        user = auth.getCurrentUser();
        mUserId = user.getUid();
        mUserEmail = user.getEmail();

        backButton = findViewById(R.id.backButton);
        userEmail = findViewById(R.id.userEmail);
        userPassword = findViewById(R.id.userPassword);
        saveChanges = findViewById(R.id.saveChanges);
        til_email = findViewById(R.id.email_layout);
        til_password = findViewById(R.id.password_layout);

        userEmail.setText(mUserEmail);

        loginDialog = new Dialog(changeUserPrivateDataActivity.this);
        loginDialog.setContentView(R.layout.login_dialog_box);
        loginDialog.getWindow().setLayout(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        loginDialog.getWindow().setBackgroundDrawable(getDrawable(R.drawable.custom_dialog_bg));
        loginDialog.setCancelable(true);

        loginUsername = loginDialog.findViewById(R.id.loginUsername);
        loginPassword = loginDialog.findViewById(R.id.loginPassword);
        login_til_password = loginDialog.findViewById(R.id.password_layout);
        TextView resetPassword = loginDialog.findViewById(R.id.resetPassword);
        loginButton = loginDialog.findViewById(R.id.loginButton);
        loginProgress = loginDialog.findViewById(R.id.loginProgress);

        userPassword.addTextChangedListener(textWatcher);
        userEmail.addTextChangedListener(textWatcher);
        loginUsername.addTextChangedListener(textWatcher);
        loginPassword.addTextChangedListener(textWatcher);

        saveChanges.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(til_password.getError()!=null||til_email.getError()!=null){
                    Toast.makeText(changeUserPrivateDataActivity.this, getString(R.string.empty_credentials_text), Toast.LENGTH_LONG).show();
                }else{
                    loginDialog.show();
                }

            }
        });

        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                loginDialog.dismiss();
                updateUser();
                finish();
            }
        });

        resetPassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(changeUserPrivateDataActivity.this, resetPageActivity.class);
                startActivity(i);
            }
        });

        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }


    private TextWatcher textWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            String mUserEmail = userEmail.getText().toString().trim();
            String mPassword = userPassword.getText().toString().trim();
            String mLoginUsername = loginUsername.getText().toString().trim();
            String mLoginPassword = loginPassword.getText().toString().trim();

            if(mLoginPassword.length()<6 && !mLoginPassword.isEmpty()){
                login_til_password.setError(getString(R.string.invalid_password_text));
            }else{
                login_til_password.setError(null);
            }

            if(mPassword.length()<6 && !mPassword.isEmpty()){
                til_password.setError(getString(R.string.invalid_password_text));
            }else{
                til_password.setError(null);
            }

            if(!Patterns.EMAIL_ADDRESS.matcher(mUserEmail).matches() && !mUserEmail.isEmpty()){
                til_email.setError(getString(R.string.invalid_email_text));
            }else{
                til_email.setError(null);
            }

            if(!mLoginUsername.isEmpty()&&mLoginPassword.length()>5){
                loginButton.setEnabled(true);
            }
        }

        @Override
        public void afterTextChanged(Editable editable) {}
    };

    private void updateUser(){
        String oldPass = loginPassword.getText().toString().trim();
        String newPass = userPassword.getText().toString().trim();
        String newEmail = userEmail.getText().toString().trim();
        String oldEmail = user.getEmail().toString().trim();
        if (newEmail.isEmpty()) {
            Toast.makeText(changeUserPrivateDataActivity.this, getString(R.string.empty_credentials_text), Toast.LENGTH_LONG).show();
            return;
        }

        if(til_password.getError() != null && til_email.getError() != null) {
            Toast.makeText(changeUserPrivateDataActivity.this, getString(R.string.all_fields_text), Toast.LENGTH_LONG).show();
        }else{
            if (!newPass.equals(oldPass) || !newEmail.equals(oldEmail)){
                DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference();
                AuthCredential credential = EmailAuthProvider.getCredential(oldEmail, oldPass);
                if (newPass.isEmpty()){
                    newPass = oldPass;
                }
                String finalNewPass = newPass;
                user.reauthenticate(credential).addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if(task.isSuccessful()){
                            if(!oldPass.equals(finalNewPass)){
                                user.updatePassword(finalNewPass).addOnCompleteListener(new OnCompleteListener<Void>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Void> task) {
                                        if(!task.isSuccessful()){
                                            Toast.makeText(changeUserPrivateDataActivity.this, getString(R.string.smth_wrong_text), Toast.LENGTH_LONG).show();
                                        }else {
                                            Toast.makeText(changeUserPrivateDataActivity.this, getString(R.string.change_pass_succ_text), Toast.LENGTH_LONG).show();
                                        }
                                    }
                                });
                            }
                            if(!oldEmail.equals(newEmail)){
                                Log.i("changesActivity", "email success");
                                user.updateEmail(newEmail).addOnCompleteListener(new OnCompleteListener<Void>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Void> task) {
                                        if(!task.isSuccessful()){
                                            Toast.makeText(changeUserPrivateDataActivity.this, getString(R.string.smth_wrong_text), Toast.LENGTH_LONG).show();
                                        }else {
                                            rootRef.child("users").orderByChild("userId")
                                                    .equalTo(mUserId).addValueEventListener(new ValueEventListener() {
                                                        @Override
                                                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                                                            for(DataSnapshot userSnap: snapshot.getChildren()){
                                                                String username = userSnap.getKey();
                                                                rootRef.child("users").child(username).child("userEmail").setValue(newEmail);
                                                                Toast.makeText(changeUserPrivateDataActivity.this, getString(R.string.change_email_succ_text), Toast.LENGTH_LONG).show();
                                                            }
                                                        }

                                                        @Override
                                                        public void onCancelled(@NonNull DatabaseError error) {
                                                            Toast.makeText(changeUserPrivateDataActivity.this, " " + error.getMessage(), Toast.LENGTH_LONG).show();
                                                        }
                                                    });
                                        }
                                    }
                                });
                            }
                        }else {
                            Toast.makeText(changeUserPrivateDataActivity.this, getString(R.string.auth_fail_text), Toast.LENGTH_LONG).show();
                        }
                    }
                });
            }else{
                Toast.makeText(changeUserPrivateDataActivity.this, getString(R.string.change_nothing_text), Toast.LENGTH_LONG).show();
            }
        }
    }
}
