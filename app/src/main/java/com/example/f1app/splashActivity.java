package com.example.f1app;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;


public class splashActivity extends Activity {
    private final Handler handler = new Handler(Looper.getMainLooper());

    private boolean navigationStarted = false;

    private final Runnable openMain = () -> {
        if (navigationStarted || isFinishing() || isDestroyed()) {
            return;
        }

        navigationStarted = true;

        startActivity(
                new Intent(
                        splashActivity.this,
                        MainActivity.class
                )
        );

        finish();
        overridePendingTransition(0, 0);
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        applyTheme();
        super.onCreate(savedInstanceState);

        setContentView(R.layout.splash_screen);
    }

    @Override
    protected void onStart() {
        super.onStart();

        if (!navigationStarted) {
            handler.removeCallbacks(openMain);
            handler.postDelayed(openMain, 2500);
        }
    }

    @Override
    protected void onStop() {
        handler.removeCallbacks(openMain);
        super.onStop();
    }

    private void applyTheme() {
        if (isSystemDarkTheme()) {
            setTheme(R.style.RemoveAppSplashTheme_Dark);
        } else {
            setTheme(R.style.RemoveAppSplashTheme_Light);
        }
    }

    private boolean isSystemDarkTheme() {
        int currentNightMode =
                getResources().getConfiguration().uiMode
                        & Configuration.UI_MODE_NIGHT_MASK;

        return currentNightMode
                == Configuration.UI_MODE_NIGHT_YES;
    }
}