package com.example.f1app;

import static com.example.f1app.MainActivity.checkConnection;
import static com.example.f1app.MainActivity.checkLightTheme;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;

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

    private leaderboardScreenController screenController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_leaderboard);

        networkNotice.showIfOffline(leaderboardActivity.this);

        WindowInsetsControllerCompat controller =
                WindowCompat.getInsetsController(
                        getWindow(),
                        getWindow().getDecorView()
                );

        controller.setAppearanceLightStatusBars(false);

        currentSeason = getIntent().getStringExtra("currentSeason");

        list = findViewById(R.id.leaderboardList);
        progress = findViewById(R.id.leaderboardProgress);
        errorText = findViewById(R.id.leaderboardError);
        seasonButton = findViewById(R.id.seasonScopeButton);
        globalButton = findViewById(R.id.globalScopeButton);

        findViewById(R.id.backButton).setOnClickListener(v -> finish());

        setupScopeAppearance();

        String season = currentSeason == null ? "" : currentSeason.trim();

        if (season.isEmpty()) {
            progress.setVisibility(View.GONE);
            errorText.setText(R.string.lb_season_missing);
            errorText.setVisibility(View.VISIBLE);
            return;
        }

        screenController = new leaderboardScreenController(
                this,
                season,
                selectedScope -> {
                    currentScope = selectedScope;
                    styleScopeButton(
                            seasonButton,
                            SCOPE_SEASON.equals(selectedScope)
                    );
                    styleScopeButton(
                            globalButton,
                            SCOPE_GLOBAL.equals(selectedScope)
                    );
                }
        );
    }

    @Override
    protected void onDestroy() {
        if (screenController != null) {
            screenController.destroy();
        }
        super.onDestroy();
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

        styleScopeButton(seasonButton, SCOPE_SEASON.equals(scope));
        styleScopeButton(globalButton, SCOPE_GLOBAL.equals(scope));

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

    private int dp(float value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private GradientDrawable roundedBackground(int color, float radiusDp) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(dp(radiusDp));
        return drawable;
    }

    private void setupScopeAppearance() {
        boolean light = checkLightTheme(this);

        View container = findViewById(R.id.scopeButtons);
        container.setBackground(roundedBackground(
                getColor(R.color.account_settings_input_color),
                14
        ));

        String season = currentSeason == null ? "" : currentSeason.trim();

        seasonButton.setText(
                season.isEmpty()
                        ? getString(R.string.leaderboard_season)
                        : getString(R.string.leaderboard_season) + " " + season
        );

        globalButton.setText(R.string.leaderboard_global);
    }

    private void styleScopeButton(Button button, boolean selected) {
        boolean light = checkLightTheme(this);

        int surface = selected
                ? getColor(R.color.colorPrimary)
                : Color.TRANSPARENT;

        int text = selected
                ? getColor(R.color.text_color_main)
                : getColor(R.color.grey);

        button.setBackgroundTintList(null);
        ViewCompat.setBackgroundTintList(button, null);
        button.setBackground(new RippleDrawable(
                ColorStateList.valueOf(
                        light ? 0x16000000 : 0x22FFFFFF
                ),
                roundedBackground(surface, 10),
                roundedBackground(Color.WHITE, 10)
        ));

        button.setTextColor(text);
        button.setTypeface(null, selected ? Typeface.BOLD : Typeface.NORMAL);
        button.setSelected(selected);
        button.setElevation(selected ? dp(2) : 0);
    }
}