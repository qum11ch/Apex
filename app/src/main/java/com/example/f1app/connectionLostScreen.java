package com.example.f1app;

import static com.example.f1app.MainActivity.checkConnection;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;

public class connectionLostScreen extends AppCompatActivity {
    private final static int SHOW = 1;
    private final static int HIDE = 2;
    private final static String EXTRA_NAME = "ACTION";

    public static Intent createShowSplashOnNetworkFailure(Context app) {
        Intent intent = new Intent(app, connectionLostScreen.class);
        intent.putExtra(EXTRA_NAME, SHOW);
        intent.addFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT| Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_NO_ANIMATION);
        return intent;
    }

    public static Intent createIntentHideSplashOnNetworkRecovery(Context app) {
        Intent intent = new Intent(app, connectionLostScreen.class);
        intent.putExtra(EXTRA_NAME, HIDE);
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        return intent;
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.lost_connection_screen);

        Button tryAgainButton = (Button) findViewById(R.id.tryAgainButton);

        if (getIntent() != null) handleIntent(getIntent());

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {

            }
        });

        tryAgainButton.setOnClickListener(v -> {
            if(checkConnection(getApplicationContext())){
                finish();
            }
        });

    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        if (intent != null) handleIntent(intent);
    }

    void handleIntent(Intent intent) {
        int value = intent.getIntExtra(EXTRA_NAME, 0);

        if (value == 0 || value == HIDE) {
            finish();
        }
    }

    //private static int getConnectionType(Context context) {
    //    int result = 0; // Returns connection type. 0: none; 1: mobile data; 2: wifi; 3: vpn
    //    ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
    //    if (cm != null) {
    //        NetworkCapabilities capabilities = cm.getNetworkCapabilities(cm.getActiveNetwork());
    //        if (capabilities != null) {
    //            if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
    //                result = 2;
    //            } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
    //                result = 1;
    //            } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) {
    //                result = 3;
    //            }
    //        }
    //    }
    //    return result;
    //}
}