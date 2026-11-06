package com.example.f1app;

import static com.example.f1app.MainActivity.checkConnection;
import static com.example.f1app.MainActivity.checkLightTheme;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class leaderboardActivity extends AppCompatActivity {

    private static final String SCOPE_SEASON = "season";
    private static final String SCOPE_GLOBAL = "global";

    private String currentSeason;
    private String currentScope = SCOPE_SEASON;

    private RecyclerView list;
    private ProgressBar progress;
    private TextView errorText;
    private Button seasonButton;
    private Button globalButton;

    private final leaderboardAdapter adapter = new leaderboardAdapter();
    private final leaderboardLoader loader = new leaderboardLoader();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_leaderboard);

        setupLeaderboardInsets();

        if (!checkConnection(getApplicationContext())) {
            startActivity(connectionLostScreen
                    .createShowSplashOnNetworkFailure(leaderboardActivity.this));
        } else {
            startActivity(connectionLostScreen
                    .createIntentHideSplashOnNetworkRecovery(leaderboardActivity.this));
        }

        WindowInsetsControllerCompat controller =
                WindowCompat.getInsetsController(
                        getWindow(),
                        getWindow().getDecorView()
                );

        controller.setAppearanceLightStatusBars(false);

        CoordinatorLayout root = findViewById(R.id.leaderboardRoot);

        if (!checkLightTheme(leaderboardActivity.this)) {
            root.setBackground(
                    ContextCompat.getDrawable(
                            leaderboardActivity.this,
                            R.drawable.black_gradient
                    )
            );
        }

        currentSeason = getIntent().getStringExtra("currentSeason");

        list = findViewById(R.id.leaderboardList);
        progress = findViewById(R.id.leaderboardProgress);
        errorText = findViewById(R.id.leaderboardError);
        seasonButton = findViewById(R.id.seasonScopeButton);
        globalButton = findViewById(R.id.globalScopeButton);

        list.setLayoutManager(new LinearLayoutManager(this));
        list.setAdapter(adapter);

        findViewById(R.id.backButton).setOnClickListener(v -> finish());

        seasonButton.setOnClickListener(v -> setScope(SCOPE_SEASON));
        globalButton.setOnClickListener(v -> setScope(SCOPE_GLOBAL));

        setScope(SCOPE_SEASON);
    }

    private void setupLeaderboardInsets() {
        View root = findViewById(R.id.leaderboardRoot);
        View header = findViewById(R.id.header);
        View scrollContent = findViewById(R.id.scrollContent);

        final int rootLeft = root.getPaddingLeft();
        final int rootTop = root.getPaddingTop();
        final int rootRight = root.getPaddingRight();
        final int rootBottom = root.getPaddingBottom();

        final int headerLeft = header.getPaddingLeft();
        final int headerTop = header.getPaddingTop();
        final int headerRight = header.getPaddingRight();
        final int headerBottom = header.getPaddingBottom();

        final int contentLeft = scrollContent.getPaddingLeft();
        final int contentTop = scrollContent.getPaddingTop();
        final int contentRight = scrollContent.getPaddingRight();
        final int contentBottom = scrollContent.getPaddingBottom();

        ViewCompat.setOnApplyWindowInsetsListener(root, (view, insets) -> {
            Insets safe = insets.getInsets(
                    WindowInsetsCompat.Type.systemBars()
                            | WindowInsetsCompat.Type.displayCutout()
            );

            view.setPadding(
                    rootLeft + safe.left,
                    rootTop,
                    rootRight + safe.right,
                    rootBottom
            );

            header.setPadding(
                    headerLeft,
                    headerTop + safe.top,
                    headerRight,
                    headerBottom
            );

            scrollContent.setPadding(
                    contentLeft,
                    contentTop,
                    contentRight,
                    contentBottom + safe.bottom
            );

            return WindowInsetsCompat.CONSUMED;
        });

        ViewCompat.requestApplyInsets(root);
    }

    private void setScope(String scope) {
        currentScope = scope;

        seasonButton.setSelected(SCOPE_SEASON.equals(scope));
        globalButton.setSelected(SCOPE_GLOBAL.equals(scope));

        load();
    }

    private void load() {
        progress.setVisibility(View.VISIBLE);
        errorText.setVisibility(View.GONE);
        adapter.submit(new ArrayList<>());

        String season = currentSeason != null ? currentSeason : "2026";
        String scope = SCOPE_SEASON.equals(currentScope) ? season : SCOPE_GLOBAL;

        String uid = FirebaseAuth.getInstance().getUid();

        loader.load(scope, uid, new leaderboardLoader.Callback() {
            @Override
            public void onLoaded(List<leaderboardEntry> top, leaderboardEntry me) {
                if (isFinishing() || isDestroyed()) return;

                progress.setVisibility(View.GONE);

                List<leaderboardEntry> visible = new ArrayList<>(top);

                if (me != null && !me.isMe) {
                    visible.add(me);
                }

                adapter.submit(visible);

                if (visible.isEmpty()) {
                    errorText.setText(R.string.leaderboard_empty);
                    errorText.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onError() {
                if (isFinishing() || isDestroyed()) return;

                progress.setVisibility(View.GONE);
                errorText.setText(R.string.leaderboard_error);
                errorText.setVisibility(View.VISIBLE);
            }
        });
    }
}