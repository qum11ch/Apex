package com.example.f1app;

import static com.example.f1app.MainActivity.checkConnection;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.viewpager2.widget.ViewPager2;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Race results of a past season. The newest past season opens first;
 * the season buttons under the header switch seasons without leaving the screen.
 */
public class pastSeasonScheduleActivity extends AppCompatActivity {

    private static final String TAG = "pastSchedule";
    private static final String STATE_SEASON = "selectedSeason";

    private SwipeRefreshLayout swipeLayout;
    private ViewPager2 viewPager;
    private TextView pageHeader;
    private TextView emptyView;
    private seasonChipsBar chips;

    private String mCurrentSeason;
    private int selectedSeason;
    private int requestToken = 0;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        setContentView(R.layout.past_season_schedule_page);
        setupPagerScreenInsets();

        if (!checkConnection(getApplicationContext())) {
            startActivity(connectionLostScreen.createShowSplashOnNetworkFailure(pastSeasonScheduleActivity.this));
        } else {
            startActivity(connectionLostScreen.createIntentHideSplashOnNetworkRecovery(pastSeasonScheduleActivity.this));
        }

        Bundle extras = getIntent().getExtras();
        mCurrentSeason = extras != null ? extras.getString("currentSeason") : null;

        int first = pastSeasons.FIRST_SCHEDULE_SEASON;
        int last = pastSeasons.lastPastSeason(mCurrentSeason);
        String requested = savedInstanceState != null
                ? String.valueOf(savedInstanceState.getInt(STATE_SEASON, last))
                : (extras != null ? extras.getString("season") : null);
        selectedSeason = pastSeasons.initial(requested, first, last);

        pageHeader = findViewById(R.id.pageHeader);
        viewPager = findViewById(R.id.viewPager2);
        emptyView = findViewById(R.id.schedule_empty);
        swipeLayout = findViewById(R.id.swipe_layout);

        swipeLayout.setOnRefreshListener(this::loadSchedule);

        ImageButton backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> finish());

        WindowInsetsControllerCompat windowInsetsController =
                WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        windowInsetsController.setAppearanceLightStatusBars(false);

        HorizontalScrollView seasonScroll = findViewById(R.id.seasonScroll);
        List<Integer> seasons = pastSeasons.seasons(first, last);

        chips = new seasonChipsBar(
                seasonScroll,
                findViewById(R.id.seasonButtonsLayout),
                season -> {
                    selectedSeason = season;
                    loadSchedule();
                }
        );

        if (seasons.isEmpty()) {
            seasonScroll.setVisibility(View.GONE);
            updateHeader();
            showEmpty();
            return;
        }

        chips.setSeasons(seasons, selectedSeason);
        loadSchedule();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(STATE_SEASON, selectedSeason);
    }

    @Override
    protected void onDestroy() {
        requestToken++;
        super.onDestroy();
    }

    private void setupPagerScreenInsets() {
        View root = findViewById(R.id.main_layout);
        View header = findViewById(R.id.header);

        final int rootLeft = root.getPaddingLeft();
        final int rootTop = root.getPaddingTop();
        final int rootRight = root.getPaddingRight();
        final int rootBottom = root.getPaddingBottom();

        final int headerLeft = header.getPaddingLeft();
        final int headerTop = header.getPaddingTop();
        final int headerRight = header.getPaddingRight();
        final int headerBottom = header.getPaddingBottom();

        ViewCompat.setOnApplyWindowInsetsListener(root, (view, windowInsets) -> {
            Insets safeInsets = windowInsets.getInsets(
                    WindowInsetsCompat.Type.systemBars()
                            | WindowInsetsCompat.Type.displayCutout()
            );

            view.setPadding(
                    rootLeft + safeInsets.left,
                    rootTop,
                    rootRight + safeInsets.right,
                    rootBottom
            );

            header.setPadding(
                    headerLeft,
                    headerTop + safeInsets.top,
                    headerRight,
                    headerBottom
            );

            return windowInsets;
        });

        ViewCompat.requestApplyInsets(root);
    }

    private void updateHeader() {
        String headerText;

        if (Locale.getDefault().getLanguage().equals("ru")) {
            headerText = getString(R.string.past_season_result) + " " + selectedSeason;
        } else {
            headerText = selectedSeason + " " + getString(R.string.past_season_result);
        }

        pageHeader.setText(headerText);
    }

    private void showEmpty() {
        swipeLayout.setRefreshing(false);
        viewPager.setAdapter(null);
        viewPager.setVisibility(View.GONE);
        emptyView.setText(R.string.ps_no_data);
        emptyView.setVisibility(View.VISIBLE);
    }

    private void showError() {
        swipeLayout.setRefreshing(false);
        viewPager.setAdapter(null);
        viewPager.setVisibility(View.GONE);
        emptyView.setText(R.string.ps_load_error);
        emptyView.setVisibility(View.VISIBLE);
    }

    private boolean isUnavailable() {
        return isFinishing() || isDestroyed();
    }

    private void loadSchedule() {
        final int token = ++requestToken;
        final int season = selectedSeason;

        updateHeader();
        swipeLayout.setRefreshing(true);

        DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference();

        // One read instead of a listener that rebuilt the screen on every database change.
        rootRef.child("schedule").child("season").child(String.valueOf(season))
                .orderByChild("round")
                .get()
                .addOnCompleteListener(task -> {
                    if (isUnavailable() || token != requestToken) {
                        return;
                    }

                    if (!task.isSuccessful()) {
                        Log.e(TAG, "Schedule request failed", task.getException());
                        showError();
                        return;
                    }

                    ArrayList<String> rounds = new ArrayList<>();

                    for (DataSnapshot race : task.getResult().getChildren()) {
                        Integer round = race.child("round").getValue(Integer.class);

                        if (round != null) {
                            rounds.add(String.valueOf(round));
                        }
                    }

                    if (rounds.isEmpty()) {
                        showEmpty();
                        return;
                    }

                    Bundle arguments = new Bundle();
                    arguments.putStringArrayList("raceRound", rounds);
                    arguments.putString("season", String.valueOf(season));
                    arguments.putString("parent", "pastSeasonSchedule");
                    showRaces(arguments);
                });
    }

    private void showRaces(Bundle arguments) {
        swipeLayout.setRefreshing(false);
        emptyView.setVisibility(View.GONE);
        viewPager.setVisibility(View.VISIBLE);

        viewPagerAdapter raceAdapter = new viewPagerAdapter(this);
        concludedRaceFragment fragment = new concludedRaceFragment();
        fragment.setArguments(arguments);
        raceAdapter.addFragment(fragment);

        viewPager.setAdapter(null); // drop the previous season's fragment before a new one is attached
        viewPager.setAdapter(raceAdapter);

        View child = viewPager.getChildAt(0);

        if (child instanceof RecyclerView) {
            child.setOverScrollMode(View.OVER_SCROLL_NEVER);
        }
    }
}
