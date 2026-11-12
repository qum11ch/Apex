package com.example.f1app;

import static com.example.f1app.MainActivity.checkConnection;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
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
import androidx.fragment.app.Fragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import java.util.List;
import java.util.Locale;

/**
 * Race results of a past season. The newest past season opens first; the season buttons switch
 * seasons, the search field filters the races, the header button switches cards and tiles.
 */
public class pastSeasonScheduleActivity extends AppCompatActivity implements pastRacesFragment.Host {

    private static final String STATE_SEASON = "selectedSeason";
    private static final String PREFS = "past_races_view";
    private static final String KEY_MODE = "mode";

    private SwipeRefreshLayout swipeLayout;
    private TextView pageHeader;
    private EditText searchInput;
    private View searchClear;
    private ImageButton viewModeButton;

    private String mCurrentSeason;
    private int selectedSeason;
    private int viewMode = pastRacesAdapter.MODE_LIST;

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
        swipeLayout = findViewById(R.id.swipe_layout);
        searchInput = findViewById(R.id.search_input);
        searchClear = findViewById(R.id.search_clear);
        viewModeButton = findViewById(R.id.view_mode_button);

        SharedPreferences prefs = getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        viewMode = prefs.getInt(KEY_MODE, pastRacesAdapter.MODE_LIST);
        updateModeButton();

        viewModeButton.setOnClickListener(v -> {
            viewMode = viewMode == pastRacesAdapter.MODE_LIST
                    ? pastRacesAdapter.MODE_TILES
                    : pastRacesAdapter.MODE_LIST;
            prefs.edit().putInt(KEY_MODE, viewMode).apply();
            updateModeButton();
            notifyFragment();
        });

        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                searchClear.setVisibility(s.length() > 0 ? View.VISIBLE : View.GONE);
                notifyFragment();
            }
        });

        searchInput.setOnEditorActionListener((view, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                hideKeyboard();
                return true;
            }

            return false;
        });

        searchClear.setOnClickListener(v -> searchInput.setText(""));

        swipeLayout.setOnChildScrollUpCallback((parent, child) -> {
            Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.races_container);
            return fragment instanceof pastRacesFragment && ((pastRacesFragment) fragment).canScrollUp();
        });

        swipeLayout.setOnRefreshListener(() -> {
            swipeLayout.setRefreshing(false); // the list has its own loading skeleton
            showSeason();
        });

        ImageButton backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> finish());

        WindowInsetsControllerCompat windowInsetsController =
                WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        windowInsetsController.setAppearanceLightStatusBars(false);

        HorizontalScrollView seasonScroll = findViewById(R.id.seasonScroll);
        List<Integer> seasons = pastSeasons.seasons(first, last);

        seasonChipsBar chips = new seasonChipsBar(
                seasonScroll,
                findViewById(R.id.seasonButtonsLayout),
                season -> {
                    selectedSeason = season;
                    showSeason();
                }
        );

        if (seasons.isEmpty()) {
            seasonScroll.setVisibility(View.GONE);
            updateHeader();
            return;
        }

        chips.setSeasons(seasons, selectedSeason);
        showSeason();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(STATE_SEASON, selectedSeason);
    }

    // ---------- pastRacesFragment.Host ----------

    @Override
    public String currentQuery() {
        return searchInput == null ? "" : searchInput.getText().toString();
    }

    @Override
    public int currentViewMode() {
        return viewMode;
    }

    // ---------- screen ----------

    private void showSeason() {
        updateHeader();

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.races_container, pastRacesFragment.newInstance(String.valueOf(selectedSeason)))
                .commitAllowingStateLoss();
    }

    private void notifyFragment() {
        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.races_container);

        if (fragment instanceof pastRacesFragment) {
            ((pastRacesFragment) fragment).applyFilters();
        }
    }

    private void updateModeButton() {
        boolean tiles = viewMode == pastRacesAdapter.MODE_TILES;
        // The button shows what a tap switches to.
        viewModeButton.setImageResource(tiles ? R.drawable.ic_view_mode_list : R.drawable.ic_view_mode_tiles);
        viewModeButton.setContentDescription(getString(tiles ? R.string.ps_view_as_list : R.string.ps_view_as_tiles));
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

    private void hideKeyboard() {
        InputMethodManager manager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);

        if (manager != null) {
            manager.hideSoftInputFromWindow(searchInput.getWindowToken(), 0);
        }

        searchInput.clearFocus();
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
}
