package com.example.f1app;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
public class signUpActivity extends AppCompatActivity {
    Button showDriverButton, showDriverStanding, showTeams, showHomePage, showAccount;
    EditText editTextUsername, editTextFullname, editTextEmail, editTextPassword;
    Button signUpButton;
    TextView logInTextView;
    ProgressBar progressBar;
    private ImageButton backButton;
    ImageView imageView;
    private static final int RQS_OPEN_IMAGE = 1;
    Uri selectedImage;
    LinearLayout footer;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);
        editTextUsername = findViewById(R.id.signUpUsername);
        editTextFullname = findViewById(R.id.signUpFullname);
        editTextEmail = findViewById(R.id.signUpEmail);
        editTextPassword = findViewById(R.id.signUpPassword);
        signUpButton = findViewById(R.id.signUpButton);
        logInTextView = findViewById(R.id.logInText);
        progressBar = findViewById(R.id.progress);

        selectedImage = Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE +
                "://" + getResources().getResourcePackageName(R.drawable.image_placeholder)
                + '/' + getResources().getResourceTypeName(R.drawable.image_placeholder) + '/' + getResources().getResourceEntryName(R.drawable.image_placeholder));

        footer = findViewById(R.id.footer);

        imageView = findViewById(R.id.imageView);
        imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent photoPickerIntent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                photoPickerIntent.addCategory(Intent.CATEGORY_OPENABLE);
                photoPickerIntent.setType("image/*");
                startActivityForResult(photoPickerIntent, RQS_OPEN_IMAGE);
            }
        });



        logInTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), LogInActivity.class);
                startActivity(intent);
                finish();
            }
        });


        signUpButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String username, fullname, email, password;
                username = String.valueOf(editTextUsername.getText());
                fullname = String.valueOf(editTextFullname.getText());
                email = String.valueOf(editTextEmail.getText());
                if(isValidEmail(email)){
                    password = String.valueOf(editTextPassword.getText());
                    if(!username.equals("")&&!fullname.equals("")
                            &&!email.equals("")&&!password.equals(""))
                    {
                        progressBar.setVisibility(View.VISIBLE);
                        Handler handler = new Handler(Looper.getMainLooper());
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                String[] field = new String[5];
                                field[0] = "username";
                                field[1] = "fullname";
                                field[2] = "email";
                                field[3] = "password";
                                field[4] = "image";
                                String[] data = new String[5];
                                data[0] = username;
                                data[1] = fullname;
                                data[2] = email;
                                data[3] = password;
                                data[4] = selectedImage.toString();
                                PutData putData = new PutData(
                                        "http://192.168.56.1/login/signup.php",
                                        "POST", field, data);
                                if (putData.startPut()) {
                                    if (putData.onComplete()) {
                                        progressBar.setVisibility(View.GONE);
                                        String result = putData.getResult();
                                        if (result.equals("Sign Up Success")) {
                                            Intent intent = new
                                                    Intent(getApplicationContext(), LogInActivity.class);
                                            startActivity(intent);
                                            finish();
                                        } else {
                                            Toast.makeText(getApplicationContext(), result,
                                                    Toast.LENGTH_SHORT).show();
                                        }
                                    }
                                }
                            }
                        });
                    }
                    else{
                        Toast.makeText(getApplicationContext(),"All fields required!",
                                Toast.LENGTH_SHORT).show();
                    }
                }else{
                    Toast.makeText(getApplicationContext(),"email is not validate!",
                            Toast.LENGTH_SHORT).show();
                }

            }
        });

        showDriverButton = (Button) findViewById(R.id.showDriver);
        showDriverButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(signUpActivity.this, driversStandingsActivity.class);
                signUpActivity.this.startActivity(intent);
            }
        });

        showDriverStanding = (Button) findViewById(R.id.showStandingsDriver);
        showDriverStanding.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(signUpActivity.this, schuduleActivity.class);
                signUpActivity.this.startActivity(intent);
            }
        });

        showHomePage = (Button) findViewById(R.id.showHomePage);
        showHomePage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(signUpActivity.this, MainActivity.class);
                signUpActivity.this.startActivity(intent);
            }
        });

        showTeams = (Button) findViewById(R.id.showTeams);
        showTeams.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(signUpActivity.this, teamsStandingsActivity.class);
                signUpActivity.this.startActivity(intent);
            }
        });

        backButton = (ImageButton) findViewById(R.id.backButton);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == RQS_OPEN_IMAGE) {
                selectedImage = data.getData();
                //String[] filePathColumn = {MediaStore.Images.Media.DATA};
                try {
                    getContentResolver().takePersistableUriPermission(selectedImage, (Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION));
                }
                catch (SecurityException e){
                    e.printStackTrace();
                }
                GlideApp.with(signUpActivity.this)
                        .load(selectedImage)
                        .placeholder(R.drawable.image_placeholder)
                        .into(imageView);
            }
        }
    }
    public final static boolean isValidEmail(CharSequence target) {
        return !TextUtils.isEmpty(target) && android.util.Patterns.EMAIL_ADDRESS.matcher(target).matches();
    }
}