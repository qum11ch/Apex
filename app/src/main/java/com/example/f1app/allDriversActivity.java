package com.example.f1app;

import static com.example.f1app.MainActivity.checkConnection;
import static com.example.f1app.MainActivity.hideShimmer;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ToggleButton;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.facebook.shimmer.ShimmerFrameLayout;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class allDriversActivity extends AppCompatActivity {
    private List<driversList> datum;
    private RecyclerView recyclerView;
    private allDriversAdapter adapter;
    private ShimmerFrameLayout shimmerFrameLayout;
    private SwipeRefreshLayout swipeLayout;
    private String mCurrentSeason;
    private StorageReference storageRef;

    private String currentSeason;

    private List<driversList> allDrivers = new ArrayList<>();
    private List<driversList> filteredDrivers = new ArrayList<>();

    private String searchQuery = "";
    private String selectedCareer = "ALL"; // ALL, ACTIVE, RESERVE, RETIRED
    private boolean championsOnly = false;
    private boolean legendsOnly = false;

    private ToggleButton allButton;
    private ToggleButton activeButton;
    private ToggleButton reserveButton;
    private ToggleButton retiredButton;
    private ToggleButton championsButton;
    private ToggleButton legendsButton;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.all_drivers_page);
        setupDriversInsets();

        if (!checkConnection(getApplicationContext())){
            startActivity(connectionLostScreen.createShowSplashOnNetworkFailure(allDriversActivity.this));
        }else{
            startActivity(connectionLostScreen.createIntentHideSplashOnNetworkRecovery(allDriversActivity.this));
        }

        Bundle intentBundle = getIntent().getExtras();
        mCurrentSeason = intentBundle.getString("currentSeason");

        currentSeason = mCurrentSeason;

        shimmerFrameLayout = findViewById(R.id.shimmer_layout);
        shimmerFrameLayout.startShimmer();

        recyclerView = findViewById(R.id.recyclerview_all_drivers);
        // recyclerView.setHasFixedSize(true);
        recyclerView.setOverScrollMode(View.OVER_SCROLL_NEVER);
        recyclerView.setNestedScrollingEnabled(false);

        datum = new ArrayList<>();
        FirebaseStorage storage = FirebaseStorage.getInstance();
        storageRef = storage.getReference();

        recyclerView.setLayoutManager(new GridLayoutManager(this, 2));

        adapter = new allDriversAdapter(this);
        recyclerView.setAdapter(adapter);

        EditText searchInput = findViewById(R.id.search);

        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchQuery = normalizeSearchText(s != null ? s.toString() : "");
                applyFilters();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        activeButton = findViewById(R.id.toggle_button_active);

        activeButton.setOnClickListener(v -> {
            selectedCareer = selectedCareer.equals("ACTIVE") ? "ALL" : "ACTIVE";
            updateFilterButtons();
            applyFilters();
        });

        reserveButton = findViewById(R.id.toggle_button_reserve);

        reserveButton.setOnClickListener(v -> {
            selectedCareer = selectedCareer.equals("RESERVE") ? "ALL" : "RESERVE";
            updateFilterButtons();
            applyFilters();
        });

        retiredButton = findViewById(R.id.toggle_button_retired);

        retiredButton.setOnClickListener(v -> {
            selectedCareer = selectedCareer.equals("RETIRED") ? "ALL" : "RETIRED";
            updateFilterButtons();
            applyFilters();
        });

        championsButton = findViewById(R.id.toggle_button_champions);

        championsButton.setOnClickListener(v -> {
            championsOnly = !championsOnly;
            updateFilterButtons();
            applyFilters();
        });

        legendsButton = findViewById(R.id.toggle_button_legends);

        legendsButton.setOnClickListener(v -> {
            legendsOnly = !legendsOnly;
            updateFilterButtons();
            applyFilters();
        });

        allButton = findViewById(R.id.toggle_button_all);

        allButton.setOnClickListener(v -> {
            selectedCareer = "ALL";
            championsOnly = false;
            legendsOnly = false;
            updateFilterButtons();
            applyFilters();
        });

        allButton.setChecked(
                selectedCareer.equals("ALL")
                        && !championsOnly
                        && !legendsOnly
        );

        swipeLayout = findViewById(R.id.swipe_layout);

        swipeLayout.setOnRefreshListener(() -> {
            recyclerView.setVisibility(View.GONE);
            shimmerFrameLayout.setVisibility(View.VISIBLE);
            shimmerFrameLayout.startShimmer();
            getDrivers();
        });

        ImageButton backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> finish());

        getDrivers();

        WindowInsetsControllerCompat windowInsetsController =
                WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        windowInsetsController.setAppearanceLightStatusBars(false);
    }

    private void setupDriversInsets() {
        View root = findViewById(R.id.drivers_root);
        View header = findViewById(R.id.header);
        View scrollContent = findViewById(R.id.drivers_scroll_content);

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

    private String normalizeSearchText(String value) {
        if (value == null) {
            return "";
        }

        return Normalizer.normalize(
                        value,
                        Normalizer.Form.NFD
                )
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT)
                .replace('ё', 'е')
                .trim()
                .replaceAll("\\s+", " ");
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


    private void getDrivers() {
        DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference();

        rootRef.child("constructors").get().addOnSuccessListener(teamsSnap -> {
            Map<String, String> teamIds = new HashMap<>();

            for (DataSnapshot team : teamsSnap.getChildren()) {
                String name = team.child("name").getValue(String.class);
                String id = team.child("constructorId").getValue(String.class);

                if (name != null) {
                    teamIds.put(name, id);
                }
            }

            rootRef.child("drivers")
                    .orderByChild("status")
                    .get()
                    .addOnSuccessListener(snapshot -> {
                        allDrivers = new ArrayList<>();

                        for (DataSnapshot driver : snapshot.getChildren()) {
                            String team = driver.child("driversTeam").getValue(String.class);
                            String fullName = driver.child("driverName").getValue(String.class);
                            String code = driver.child("driversCode").getValue(String.class);
                            String status = driver.child("status").getValue(String.class);
                            String lastEntry = driver.child("lastEntry").getValue(String.class);
                            String driverId = driver.child("driverId").getValue(String.class);

                            int championships = parseChampionships(
                                    driver.child("championshipsCount").getValue()
                            );

                            boolean legend = Boolean.TRUE.equals(
                                    driver.child("legendStatus").getValue()
                            );

                            if (fullName == null || code == null) continue;

                            String[] parts = fullName.split(" ");
                            String name = parts[0];
                            String family = parts.length > 1 ? parts[1] : "";

                            if (parts.length >= 3) {
                                name = parts[0] + " " + parts[1];
                                family = parts[2];
                            }

                            String teamId = teamIds.get(team);

                            driversList data = new driversList(
                                    name,
                                    family,
                                    team,
                                    teamId != null ? teamId : "",
                                    "",
                                    "",
                                    code,
                                    true,
                                    currentSeason
                            );

                            data.setStatus(status);
                            data.setChampionships(championships);
                            data.setLegend(legend);

                            setDriverImage(
                                    data,
                                    driverId,
                                    code,
                                    lastEntry
                            );

                            allDrivers.add(data);
                        }

                        applyFilters();
                        hideShimmer(recyclerView, shimmerFrameLayout);
                        swipeLayout.setRefreshing(false);
                    })
                    .addOnFailureListener(e -> {
                        Log.e("driverLoading", String.valueOf(e.getMessage()));
                        swipeLayout.setRefreshing(false);
                    });
        });
    }

    private void applyFilters() {
        filteredDrivers = new ArrayList<>();

        String query = normalizeSearchText(searchQuery);

        for (driversList driver : allDrivers) {
            if (!matchesSearch(driver, query)) {
                continue;
            }

            if (!matchesDriverFilters(driver)) {
                continue;
            }

            filteredDrivers.add(driver);
        }

        adapter.submitList(new ArrayList<>(filteredDrivers));

        if (filteredDrivers.isEmpty()) {
            // покажите empty view / message
        }
    }

    private boolean matchesSearch(driversList driver, String query) {
        if (query.isEmpty()) {
            return true;
        }

        String searchable = normalizeSearchText(
                safe(driver.getDriverName()) + " "
                        + safe(driver.getDriverFamilyName()) + " "
                        + safe(driver.getDriverTeam())
        );

        for (String word : query.split(" ")) {
            if (!searchable.contains(word)) {
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

    private String safe(String value) {
        return value != null ? value : "";
    }

    private int parseChampionships(Object value) {
        if (value == null) {
            return 0;
        }

        if (value instanceof Number) {
            return ((Number) value).intValue();
        }

        try {
            return Integer.parseInt(value.toString().trim());
        } catch (NumberFormatException error) {
            return 0;
        }
    }

    private void setDriverImage(
            driversList driver,
            String driverId,
            String driverCode,
            String driverLastEntry
    ) {
        String year = extractYear(driverLastEntry);

        if (driverCode == null || driverCode.trim().isEmpty() || year == null) {
            driver.setDriverImageRef(null);
            return;
        }

        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference imageRef;

        if (driverId != null && !driverId.trim().isEmpty()) {
            imageRef = storage.getReference()
                    .child("drivers")
                    .child(driverId.trim() + "_" + year + ".png");
        } else {
            imageRef = storage.getReference()
                    .child("drivers")
                    .child(driverCode.toLowerCase(Locale.ROOT) + "_" + year + ".png");
        }

        driver.setDriverImageRef(imageRef);
    }

    private String extractYear(String lastEntry) {
        if (lastEntry == null || lastEntry.trim().isEmpty()) {
            return null;
        }

        java.util.regex.Matcher matcher = java.util.regex.Pattern
                .compile("\\b(\\d{4})\\b")
                .matcher(lastEntry);

        if (!matcher.find()) {
            return null;
        }

        return matcher.group(1);
    }
}
