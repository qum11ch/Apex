package com.example.f1app;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

public class settingsActivity extends AppCompatActivity {
    private static final int RQS_OPEN_IMAGE = 1;
    Button showDriverButton, showDriverStanding, showTeams, showHomePage, showAccount;
    private ImageButton backButton;
    private EditText editTextFullname, editTextEmail;
    private Button saveButton;
    Uri selectedImage;
    ImageView imageView;
    String username;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_page);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);


        String username = prefs.getString("username","");

        editTextFullname = findViewById(R.id.fullname);
        editTextEmail = findViewById(R.id.email);
        saveButton = findViewById(R.id.saveButton);

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


        Bundle bundle = getIntent().getExtras();
        String fullName = bundle.getString("fullName");
        String email = bundle.getString("email");
        String image = bundle.getString("image");

        editTextFullname.setText(fullName, TextView.BufferType.EDITABLE);
        editTextEmail.setText(email, TextView.BufferType.EDITABLE);

        selectedImage = Uri.parse(image);
        GlideApp.with(settingsActivity.this)
                .load(selectedImage)
                .into(imageView);


        showDriverButton = (Button) findViewById(R.id.showDriver);
        showDriverButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(settingsActivity.this, driversStandingsActivity.class);
                settingsActivity.this.startActivity(intent);
            }
        });

        showDriverStanding = (Button) findViewById(R.id.showStandingsDriver);
        showDriverStanding.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(settingsActivity.this, schuduleActivity.class);
                settingsActivity.this.startActivity(intent);
            }
        });

        showHomePage = (Button) findViewById(R.id.showHomePage);
        showHomePage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(settingsActivity.this, MainActivity.class);
                settingsActivity.this.startActivity(intent);
            }
        });

        showTeams = (Button) findViewById(R.id.showTeams);
        showTeams.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(settingsActivity.this, teamsStandingsActivity.class);
                settingsActivity.this.startActivity(intent);
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
            public void onClick(View v) {
                String fullname, email;
                fullname = String.valueOf(editTextFullname.getText());
                email = String.valueOf(editTextEmail.getText());
                if(isValidEmail(email)){
                    if(!email.equals("")&&!fullname.equals(""))
                    {
                        Handler handler = new Handler(Looper.getMainLooper());
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                String[] field = new String[4];
                                field[0] = "username";
                                field[1] = "email";
                                field[2] = "fullname";
                                field[3] = "image";
                                String[] data = new String[4];
                                data[0] = username;
                                data[1] = email;
                                data[2] = fullname;
                                data[3] = selectedImage.toString();
                                PutData putData = new PutData(
                                        "http://192.168.56.1/login/update.php",
                                        "POST", field, data);
                                if (putData.startPut()) {
                                    if (putData.onComplete()) {
                                        String result = putData.getResult();
                                        if (result.equals("1")) {
                                            Toast.makeText(getApplicationContext(), "Update success",
                                                    Toast.LENGTH_LONG).show();
                                            Intent intent = new
                                                    Intent(getApplicationContext(), LogInActivity.class);
                                            startActivity(intent);
                                            finish();
                                        } else {
                                            Toast.makeText(getApplicationContext(), result,
                                                    Toast.LENGTH_SHORT).show();
                                            Log.i("errorUpdateAccount", "" + result);
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
                GlideApp.with(settingsActivity.this)
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
