package com.example.f1app;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;

import androidx.core.content.ContextCompat;


public class splashActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        applyTheme();
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onStart() {
        super.onStart();
        setContentView(R.layout.splash_screen);
        new Handler().postDelayed(() -> {
            startActivity(new Intent(splashActivity.this, MainActivity.class));
            finish();
            overridePendingTransition(0, 0);
        }, 2500);
    }

    private void applyTheme() {
        if (isSystemDarkTheme()) {
            setTheme(R.style.RemoveAppSplashTheme_Dark);
        } else {
            setTheme(R.style.RemoveAppSplashTheme_Light);
        }
    }

    private boolean isSystemDarkTheme() {
        int currentNightMode = getResources().getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_MASK;
        return currentNightMode == Configuration.UI_MODE_NIGHT_YES;
    }
}