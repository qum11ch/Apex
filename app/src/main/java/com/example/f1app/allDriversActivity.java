package com.example.f1app;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.TooltipCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.facebook.shimmer.ShimmerFrameLayout;
import com.google.android.material.appbar.AppBarLayout;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.text.Normalizer;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class allDriversActivity extends AppCompatActivity {

    private static final String TAG = "allDriversActivity";
    private static final String STATE_CAREER = "all_drivers_career";
    private static final String STATE_CHAMPIONS = "all_drivers_champions";
    private static final String STATE_LEGENDS = "all_drivers_legends";
    private static final String PREFS = "all_drivers_prefs";
    private static final String KEY_VIEW_MODE = "view_mode";

    private static final Pattern MARKS = Pattern.compile("\\p{M}+");
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");
    private static final Pattern YEAR = Pattern.compile("\\b(\\d{4})\\b");

    private static final class Entry {
        final driversList driver;
        final String searchable;

        Entry(driversList driver, String searchable) {
            this.driver = driver;
            this.searchable = searchable;
        }
    }

    private final List<Entry> entries = new ArrayList<>();

    private RecyclerView recyclerView;
    private GridLayoutManager layoutManager;
    private allDriversAdapter adapter;
    private ShimmerFrameLayout shimmerFrameLayout;
    private View skeletonTiles;
    private View skeletonRows;
    private SwipeRefreshLayout swipeLayout;
    private View stateContainer;
    private TextView stateMessage;
    private Button stateRetry;
    private Button stateReset;
    private EditText searchInput;
    private ImageButton viewModeButton;

    private String currentSeason;
    private String searchQuery = "";
    private String selectedCareer = "ALL"; // ALL, ACTIVE, RESERVE, RETIRED
    private boolean championsOnly = false;
    private boolean legendsOnly = false;
    private allDriversAdapter.ViewMode viewMode = allDriversAdapter.ViewMode.TILES;

    private ToggleButton allButton;
    private ToggleButton activeButton;
    private ToggleButton reserveButton;
    private ToggleButton retiredButton;
    private ToggleButton championsButton;
    private ToggleButton legendsButton;

    private int cacheToken = 0;
    private int requestToken = 0;
    private boolean hasData = false;
    private boolean loading = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.all_drivers_page);
        setupDriversInsets();

        networkNotice.showIfOffline(allDriversActivity.this);

        currentSeason = resolveSeason();
        restoreFilters(savedInstanceState);
        viewMode = loadViewMode();

        shimmerFrameLayout = findViewById(R.id.shimmer_layout);
        skeletonTiles = findViewById(R.id.skeleton_tiles);
        skeletonRows = findViewById(R.id.skeleton_rows);
        stateContainer = findViewById(R.id.state_container);
        stateMessage = findViewById(R.id.state_message);
        stateRetry = findViewById(R.id.state_retry);
        stateReset = findViewById(R.id.state_reset);
        viewModeButton = findViewById(R.id.view_mode_button);

        recyclerView = findViewById(R.id.recyclerview_all_drivers);
        recyclerView.setOverScrollMode(View.OVER_SCROLL_NEVER);

        layoutManager = new GridLayoutManager(this, spanCountFor(viewMode));
        recyclerView.setLayoutManager(layoutManager);

        adapter = new allDriversAdapter();
        recyclerView.setAdapter(adapter);

        setupSearch();
        setupFilters();
        setupRefresh();
        setupViewModeToggle();

        ImageButton backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> finish());

        WindowInsetsControllerCompat windowInsetsController =
                WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        windowInsetsController.setAppearanceLightStatusBars(false);

        updateFilterButtons();
        applyViewMode(false);
        loadScreen();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(STATE_CAREER, selectedCareer);
        outState.putBoolean(STATE_CHAMPIONS, championsOnly);
        outState.putBoolean(STATE_LEGENDS, legendsOnly);
    }

    @Override
    protected void onDestroy() {
        cacheToken++;
        requestToken++;

        if (shimmerFrameLayout != null) {
            shimmerFrameLayout.stopShimmer();
        }

        super.onDestroy();
    }

    // ---------- window insets ----------

    private void setupDriversInsets() {
        final View root = findViewById(R.id.drivers_root);
        final View header = findViewById(R.id.header);
        final View searchBar = findViewById(R.id.search_bar);
        final View content = findViewById(R.id.content_coordinator);
        final RecyclerView list = findViewById(R.id.recyclerview_all_drivers);

        final int headerLeft = header.getPaddingLeft();
        final int headerTop = header.getPaddingTop();
        final int headerRight = header.getPaddingRight();
        final int headerBottom = header.getPaddingBottom();

        final int searchLeft = searchBar.getPaddingLeft();
        final int searchTop = searchBar.getPaddingTop();
        final int searchRight = searchBar.getPaddingRight();
        final int searchBottom = searchBar.getPaddingBottom();

        final int listLeft = list.getPaddingLeft();
        final int listTop = list.getPaddingTop();
        final int listRight = list.getPaddingRight();
        final int listBottom = list.getPaddingBottom();

        ViewCompat.setOnApplyWindowInsetsListener(root, (view, windowInsets) -> {
            Insets safeInsets = windowInsets.getInsets(
                    WindowInsetsCompat.Type.systemBars()
                            | WindowInsetsCompat.Type.displayCutout()
            );
            Insets imeInsets = windowInsets.getInsets(WindowInsetsCompat.Type.ime());

            // The header background fills the whole width; only its content is inset.
            header.setPadding(
                    headerLeft + safeInsets.left,
                    headerTop + safeInsets.top,
                    headerRight + safeInsets.right,
                    headerBottom
            );

            searchBar.setPadding(
                    searchLeft + safeInsets.left,
                    searchTop,
                    searchRight + safeInsets.right,
                    searchBottom
            );

            // The gradient stays edge-to-edge; the interactive content avoids side bars and cutouts.
            content.setPadding(safeInsets.left, 0, safeInsets.right, 0);

            // The list scrolls under the navigation bar and can be scrolled clear of it or the keyboard.
            list.setPadding(
                    listLeft,
                    listTop,
                    listRight,
                    listBottom + Math.max(safeInsets.bottom, imeInsets.bottom)
            );

            return windowInsets;
        });

        ViewCompat.requestApplyInsets(root);
    }

    // ---------- setup ----------

    private String resolveSeason() {
        String season = getIntent() != null
                ? getIntent().getStringExtra("currentSeason")
                : null;

        if (season == null || season.trim().isEmpty()) {
            season = String.valueOf(LocalDate.now().getYear());
        }

        return season.trim();
    }

    private void restoreFilters(Bundle state) {
        if (state == null) {
            return;
        }

        String career = state.getString(STATE_CAREER, "ALL");

        if ("ACTIVE".equals(career) || "RESERVE".equals(career) || "RETIRED".equals(career)) {
            selectedCareer = career;
        }

        championsOnly = state.getBoolean(STATE_CHAMPIONS, false);
        legendsOnly = state.getBoolean(STATE_LEGENDS, false);
    }

    private void setupSearch() {
        searchInput = findViewById(R.id.search);

        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchQuery = normalizeSearchText(s != null ? s.toString() : "");
                applyFilters(true);
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    private void setupFilters() {
        activeButton = findViewById(R.id.toggle_button_active);
        reserveButton = findViewById(R.id.toggle_button_reserve);
        retiredButton = findViewById(R.id.toggle_button_retired);
        championsButton = findViewById(R.id.toggle_button_champions);
        legendsButton = findViewById(R.id.toggle_button_legends);
        allButton = findViewById(R.id.toggle_button_all);

        activeButton.setOnClickListener(v -> {
            selectedCareer = selectedCareer.equals("ACTIVE") ? "ALL" : "ACTIVE";
            filtersChanged();
        });

        reserveButton.setOnClickListener(v -> {
            selectedCareer = selectedCareer.equals("RESERVE") ? "ALL" : "RESERVE";
            filtersChanged();
        });

        retiredButton.setOnClickListener(v -> {
            selectedCareer = selectedCareer.equals("RETIRED") ? "ALL" : "RETIRED";
            filtersChanged();
        });

        championsButton.setOnClickListener(v -> {
            championsOnly = !championsOnly;
            filtersChanged();
        });

        legendsButton.setOnClickListener(v -> {
            legendsOnly = !legendsOnly;
            filtersChanged();
        });

        allButton.setOnClickListener(v -> {
            selectedCareer = "ALL";
            championsOnly = false;
            legendsOnly = false;
            filtersChanged();
        });
    }

    private void setupRefresh() {
        swipeLayout = findViewById(R.id.swipe_layout);
        AppBarLayout appBar = findViewById(R.id.app_bar);

        swipeLayout.setOnRefreshListener(() -> {
            showSkeleton(); // pull-to-refresh: the shimmer comes back
            refresh(true);
        });

        // The only child is a FrameLayout, so ask the list whether it can still scroll up.
        swipeLayout.setOnChildScrollUpCallback((parent, child) ->
                recyclerView.getVisibility() == View.VISIBLE
                        && recyclerView.canScrollVertically(-1));

        // Pull-to-refresh only when the filter block is fully visible.
        appBar.addOnOffsetChangedListener((layout, offset) -> swipeLayout.setEnabled(offset == 0));

        stateRetry.setOnClickListener(v -> refresh(true));
        stateReset.setOnClickListener(v -> clearFilters());
    }

    private void filtersChanged() {
        updateFilterButtons();
        applyFilters(true);
    }

    private void clearFilters() {
        selectedCareer = "ALL";
        championsOnly = false;
        legendsOnly = false;
        updateFilterButtons();
        searchInput.setText(""); // the watcher resets searchQuery and re-applies filters
        applyFilters(true);
    }

    private boolean hasActiveFilters() {
        return !searchQuery.isEmpty()
                || !selectedCareer.equals("ALL")
                || championsOnly
                || legendsOnly;
    }

    private void updateFilterButtons() {
        boolean allSelected =
                selectedCareer.equals("ALL")
                        && !championsOnly
                        && !legendsOnly;

        allButton.setChecked(allSelected);
        activeButton.setChecked(selectedCareer.equals("ACTIVE"));
        reserveButton.setChecked(selectedCareer.equals("RESERVE"));
        retiredButton.setChecked(selectedCareer.equals("RETIRED"));
        championsButton.setChecked(championsOnly);
        legendsButton.setChecked(legendsOnly);
    }

    // ---------- tiles / list ----------

    private allDriversAdapter.ViewMode loadViewMode() {
        String stored = viewModeStore.getName(
                this,
                viewModeStore.SCREEN_ALL_DRIVERS,
                allDriversAdapter.ViewMode.TILES.name()
        );

        try {
            return allDriversAdapter.ViewMode.valueOf(stored);
        } catch (IllegalArgumentException error) {
            return allDriversAdapter.ViewMode.TILES;
        }
    }

    private void setupViewModeToggle() {
        viewModeButton.setOnClickListener(v -> {
            viewMode = viewMode == allDriversAdapter.ViewMode.TILES
                    ? allDriversAdapter.ViewMode.LIST
                    : allDriversAdapter.ViewMode.TILES;

            viewModeStore.setName(this, viewModeStore.SCREEN_ALL_DRIVERS, viewMode.name());

            applyViewMode(true);
        });
    }

    /** Tiles use as many columns as fit (min 2); rows stay single-column on phones and use 2-3 columns on wide screens. */
    private int spanCountFor(allDriversAdapter.ViewMode mode) {
        int widthDp = getResources().getConfiguration().screenWidthDp;

        if (mode == allDriversAdapter.ViewMode.TILES) {
            return Math.max(2, Math.min(5, widthDp / 170));
        }

        if (widthDp >= 840) return 3;
        if (widthDp >= 600) return 2;
        return 1;
    }

    private void applyViewMode(boolean keepPosition) {
        int firstVisible = keepPosition
                ? layoutManager.findFirstVisibleItemPosition()
                : RecyclerView.NO_POSITION;

        boolean tiles = viewMode == allDriversAdapter.ViewMode.TILES;

        layoutManager.setSpanCount(spanCountFor(viewMode));
        adapter.setViewMode(viewMode);

        skeletonTiles.setVisibility(tiles ? View.VISIBLE : View.GONE);
        skeletonRows.setVisibility(tiles ? View.GONE : View.VISIBLE);

        // The button shows the view that a tap will switch to.
        String description = getString(tiles
                ? R.string.all_drivers_view_as_list
                : R.string.all_drivers_view_as_tiles);

        viewModeButton.setImageResource(tiles
                ? R.drawable.ic_all_drivers_view_list
                : R.drawable.ic_all_drivers_view_grid);
        viewModeButton.setContentDescription(description);
        TooltipCompat.setTooltipText(viewModeButton, description);

        if (firstVisible > 0) {
            recyclerView.scrollToPosition(firstVisible);
        }
    }

    // ---------- loading ----------

    private boolean isUnavailable() {
        return isFinishing() || isDestroyed();
    }

    private void loadScreen() {
        final allDriversRepository.Snapshot remembered = allDriversRepository.memorySnapshot();

        if (remembered != null) {
            showDrivers(remembered); // seen before: no skeleton, refresh quietly below
        } else {
            showSkeleton();
        }

        final int token = ++cacheToken;

        allDriversRepository.readCache(getApplicationContext(), cached -> {
            if (isUnavailable() || token != cacheToken) {
                return;
            }

            if (cached != null) {
                if (!hasData) {
                    showDrivers(cached);
                }

                if (!allDriversRepository.isFresh(cached, System.currentTimeMillis())) {
                    refresh(false);
                }
            } else if (!hasData && !loading) {
                refresh(false);
            }
        });
    }

    private void refresh(boolean force) {
        if (loading && !force) {
            return;
        }

        loading = true;
        final int token = ++requestToken;

        if (hasData) {
            swipeLayout.setRefreshing(true);
        } else {
            showSkeleton();
        }

        allDriversRepository.fetch(
                snapshot -> {
                    if (isUnavailable() || token != requestToken) {
                        return;
                    }

                    loading = false;
                    swipeLayout.setRefreshing(false);
                    allDriversRepository.writeCache(getApplicationContext(), snapshot);
                    showDrivers(snapshot);
                },
                error -> {
                    if (isUnavailable() || token != requestToken) {
                        return;
                    }

                    loading = false;
                    swipeLayout.setRefreshing(false);
                    Log.e(TAG, "Drivers loading failed", error);

                    if (hasData) {
                        shimmerFrameLayout.stopShimmer();
                        shimmerFrameLayout.setVisibility(View.GONE);
                        applyFilters(false); // bring the old list back
                        Toast.makeText(
                                this,
                                R.string.all_drivers_refresh_failed,
                                Toast.LENGTH_SHORT
                        ).show();
                    } else {
                        showLoadError();
                    }
                }
        );
    }

    // ---------- screen states ----------

    private void showSkeleton() {
        recyclerView.setVisibility(View.GONE);
        stateContainer.setVisibility(View.GONE);
        shimmerFrameLayout.setVisibility(View.VISIBLE);
        shimmerFrameLayout.startShimmer();
    }

    private void showLoadError() {
        shimmerFrameLayout.stopShimmer();
        shimmerFrameLayout.setVisibility(View.GONE);
        recyclerView.setVisibility(View.GONE);
        stateMessage.setText(R.string.all_drivers_load_error);
        stateRetry.setVisibility(View.VISIBLE);
        stateReset.setVisibility(View.GONE);
        stateContainer.setVisibility(View.VISIBLE);
    }

    private void showDrivers(allDriversRepository.Snapshot snapshot) {
        entries.clear();
        FirebaseStorage storage = FirebaseStorage.getInstance();

        for (allDriversRepository.Driver driver : snapshot.drivers) {
            driversList data = new driversList(
                    driver.givenName,
                    driver.familyName,
                    driver.team,
                    driver.constructorId,
                    "",
                    "",
                    driver.code,
                    true,
                    currentSeason
            );

            data.setStatus(driver.status);
            data.setChampionships(driver.championships);
            data.setLegend(driver.legend);
            data.setDriverImageRef(imageRef(storage, driver));

            entries.add(new Entry(
                    data,
                    normalizeSearchText(
                            driver.givenName + " " + driver.familyName + " " + driver.team
                    )
            ));
        }

        hasData = true;
        shimmerFrameLayout.stopShimmer();
        shimmerFrameLayout.setVisibility(View.GONE);
        applyFilters(false);
    }

    private void applyFilters(boolean scrollToTop) {
        if (!hasData) {
            return;
        }

        List<driversList> result = new ArrayList<>();

        for (Entry entry : entries) {
            if (matchesSearch(entry) && matchesDriverFilters(entry.driver)) {
                result.add(entry.driver);
            }
        }

        if (result.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            stateMessage.setText(R.string.all_drivers_empty);
            stateRetry.setVisibility(View.GONE);
            // Filters may be scrolled out of sight, so offer a way out of an empty result.
            stateReset.setVisibility(hasActiveFilters() ? View.VISIBLE : View.GONE);
            stateContainer.setVisibility(View.VISIBLE);
            adapter.submitList(result);
            return;
        }

        stateContainer.setVisibility(View.GONE);
        recyclerView.setVisibility(View.VISIBLE);

        if (scrollToTop) {
            adapter.submitList(result, () -> recyclerView.scrollToPosition(0));
        } else {
            adapter.submitList(result);
        }
    }

    // ---------- filtering ----------

    private boolean matchesSearch(Entry entry) {
        if (searchQuery.isEmpty()) {
            return true;
        }

        for (String word : searchQuery.split(" ")) {
            if (!entry.searchable.contains(word)) {
                return false;
            }
        }

        return true;
    }

    private boolean matchesDriverFilters(driversList driver) {
        boolean careerMatches;

        switch (selectedCareer) {
            case "ACTIVE":
                careerMatches = "active".equals(driver.getStatus());
                break;
            case "RESERVE":
                careerMatches = "reserve".equals(driver.getStatus());
                break;
            case "RETIRED":
                careerMatches = "retired".equals(driver.getStatus());
                break;
            default:
                careerMatches = true;
                break;
        }

        return careerMatches
                && (!championsOnly || driver.getChampionships() > 0)
                && (!legendsOnly || driver.isLegend());
    }

    private static String normalizeSearchText(String value) {
        if (value == null) {
            return "";
        }

        String decomposed = Normalizer.normalize(value, Normalizer.Form.NFD);
        String withoutMarks = MARKS.matcher(decomposed).replaceAll("");

        return WHITESPACE.matcher(
                withoutMarks
                        .toLowerCase(Locale.ROOT)
                        .replace('ё', 'е')
                        .trim()
        ).replaceAll(" ");
    }

    // ---------- images ----------

    private static StorageReference imageRef(
            FirebaseStorage storage,
            allDriversRepository.Driver driver
    ) {
        String year = extractYear(driver.lastEntry);

        if (driver.code == null || driver.code.trim().isEmpty() || year == null) {
            return null;
        }

        String fileName = driver.driverId != null && !driver.driverId.trim().isEmpty()
                ? driver.driverId.trim() + "_" + year + ".png"
                : driver.code.toLowerCase(Locale.ROOT) + "_" + year + ".png";

        return storage.getReference().child("drivers").child(fileName);
    }

    private static String extractYear(String lastEntry) {
        if (lastEntry == null || lastEntry.trim().isEmpty()) {
            return null;
        }

        Matcher matcher = YEAR.matcher(lastEntry);
        return matcher.find() ? matcher.group(1) : null;
    }
}
