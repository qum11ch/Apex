package com.example.f1app;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONException;
import org.json.JSONObject;

public class accountPageActivity extends AppCompatActivity {
    private TextView userName, fullName, Email;
    private Button logout, settings, savedRace;
    Button showDriverButton, showDriverStanding, showTeams, showHomePage, showAccount;
    private ImageButton backButton;
    private ImageView imageView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.account_page);

        userName = (TextView) findViewById(R.id.userName);
        fullName = (TextView) findViewById(R.id.fullname);
        Email = (TextView) findViewById(R.id.email);
        logout = (Button) findViewById(R.id.logout);
        imageView = (ImageView) findViewById(R.id.poster_image);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        boolean isloged = prefs.getBoolean("Islogin", false);

        if(!isloged){
            Intent i = new Intent(accountPageActivity.this, LogInActivity.class);
            startActivity(i);
            finish();
        }

        logout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                prefs.edit().putBoolean("Islogin",false).commit();
                Intent i = new Intent(accountPageActivity.this, LogInActivity.class);
                startActivity(i);
                finish();
            }
        });

        String username = prefs.getString("username","");
        getText(username);

        showDriverButton = (Button) findViewById(R.id.showDriver);
        showDriverButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(accountPageActivity.this, driversStandingsActivity.class);
                accountPageActivity.this.startActivity(intent);
            }
        });

        showDriverStanding = (Button) findViewById(R.id.showStandingsDriver);
        showDriverStanding.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(accountPageActivity.this, schuduleActivity.class);
                accountPageActivity.this.startActivity(intent);
            }
        });


        showHomePage = (Button) findViewById(R.id.showHomePage);
        showHomePage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(accountPageActivity.this, MainActivity.class);
                accountPageActivity.this.startActivity(intent);
            }
        });

        showTeams = (Button) findViewById(R.id.showTeams);
        showTeams.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(accountPageActivity.this, teamsStandingsActivity.class);
                accountPageActivity.this.startActivity(intent);
            }
        });

        showAccount = (Button) findViewById(R.id.showAccount);
        showAccount.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(accountPageActivity.this, LogInActivity.class);
                accountPageActivity.this.startActivity(intent);
            }
        });

        backButton = (ImageButton) findViewById(R.id.backButton);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        savedRace = (Button) findViewById(R.id.savedRace);
        savedRace.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //getText2(username);
                Intent intent = new Intent(accountPageActivity.this, savedRacesActivity.class);
                accountPageActivity.this.startActivity(intent);
            }
        });

        settings = (Button) findViewById(R.id.profileSettings);
        settings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Handler handler = new Handler(Looper.getMainLooper());
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        GetDataAccount putData = new GetDataAccount(
                                "http://192.168.56.1/login/getAccount.php",
                                "GET", username);
                        if (putData.startPut()) {
                            if (putData.onComplete()) {
                                JSONObject resultGet = putData.getResult();
                                try {
                                    String email = resultGet.getString("email");
                                    String fullName = resultGet.getString("fullname");
                                    String image = resultGet.getString("image");
                                    Intent intent = new Intent(accountPageActivity.this, settingsActivity.class);
                                    intent.putExtra("email", email);
                                    intent.putExtra("fullName", fullName);
                                    intent.putExtra("image", image);
                                    accountPageActivity.this.startActivity(intent);
                                } catch (JSONException e) {
                                    throw new RuntimeException(e);
                                }
                            }
                        }
                    }
                });
            }
        });
    }

    public void getText(String username) {
        if (!username.equals("")) {
            Handler handler = new Handler(Looper.getMainLooper());
            handler.post(new Runnable() {
                @Override
                public void run() {
                    GetDataAccount putData = new GetDataAccount(
                            "http://192.168.56.1/login/getAccount.php",
                            "GET", username);
                    if (putData.startPut()) {
                        if (putData.onComplete()) {
                            JSONObject resultGet = putData.getResult();
                            try {
                                userName.setText(resultGet.getString("username"));
                                Email.setText(resultGet.getString("email"));
                                fullName.setText(resultGet.getString("fullname"));
                                Uri selectedImage = Uri.parse(resultGet.getString("image"));
                                GlideApp.with(accountPageActivity.this)
                                        .load(selectedImage)
                                        .placeholder(R.drawable.image_placeholder)
                                        .into(imageView);
                            } catch (JSONException e) {
                                throw new RuntimeException(e);
                            }
                        }
                    }
                }
            });
        }else{
            Toast.makeText(getApplicationContext(),"All fields required!",Toast.LENGTH_SHORT).show();
        }
    }
}
