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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.facebook.shimmer.ShimmerFrameLayout;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Driver standings of a past season. The newest past season opens first;
 * the season buttons under the header switch seasons without leaving the screen.
 */
public class pastSeasonDriversStandingsActivity extends AppCompatActivity {

    private static final String TAG = "pastDriversStandings";
    private static final String STATE_SEASON = "selectedSeason";

    // One list instance for the whole life of the screen: the adapter keeps a reference to it.
    private final List<driversList> datum = new ArrayList<>();
    private final Map<String, String> databaseTeamColors = new HashMap<>();
    private final Set<String> knownDrivers = new HashSet<>();

    private RecyclerView recyclerView;
    private pastSeasonDriversStandingsAdapter adapter;
    private ShimmerFrameLayout shimmerFrameLayout;
    private SwipeRefreshLayout swipeLayout;
    private TextView driversHeader;
    private View stateContainer;
    private TextView stateMessage;
    private seasonChipsBar chips;
    private HorizontalScrollView seasonScroll;

    private RequestQueue queue;
    private StorageReference storageRef;
    private DatabaseReference rootRef;

    private String mCurrentSeason;
    private int selectedSeason;
    private int requestToken = 0;
    private boolean referenceLoaded = false;
    private boolean driversKnown = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.past_season_drivers_standing_page);

        setupStandingsInsets();
        EdgeInsets.applyBottom(findViewById(R.id.recyclerview_currentDrivers));

        networkNotice.showIfOffline(pastSeasonDriversStandingsActivity.this);

        Bundle extras = getIntent().getExtras();
        mCurrentSeason = extras != null ? extras.getString("currentSeason") : null;

        int first = pastSeasons.FIRST_STANDINGS_SEASON;
        int last = pastSeasons.lastPastSeason(mCurrentSeason);
        String requested = savedInstanceState != null
                ? String.valueOf(savedInstanceState.getInt(STATE_SEASON, last))
                : (extras != null ? extras.getString("season") : null);
        selectedSeason = pastSeasons.initial(requested, first, last);

        shimmerFrameLayout = findViewById(R.id.shimmer_layout);
        recyclerView = findViewById(R.id.recyclerview_currentDrivers);
        swipeLayout = findViewById(R.id.swipe_layout);
        driversHeader = findViewById(R.id.driversHeader);
        stateContainer = findViewById(R.id.state_container);
        stateMessage = findViewById(R.id.state_message);
        seasonScroll = findViewById(R.id.seasonScroll);

        queue = Volley.newRequestQueue(this);
        storageRef = FirebaseStorage.getInstance().getReference();
        rootRef = FirebaseDatabase.getInstance().getReference();

        adapter = new pastSeasonDriversStandingsAdapter(this, datum);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        swipeLayout.setOnChildScrollUpCallback(
                (parent, child) -> recyclerView.canScrollVertically(-1)
        );

        swipeLayout.setOnRefreshListener(() -> {
            swipeLayout.setRefreshing(false); // the screen has its own loading skeleton
            loadStandings();
        });

        findViewById(R.id.state_retry).setOnClickListener(v -> loadStandings());

        ImageButton backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> finish());

        WindowInsetsControllerCompat windowInsetsController =
                WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        windowInsetsController.setAppearanceLightStatusBars(false);

        List<Integer> seasons = pastSeasons.seasons(first, last);

        chips = new seasonChipsBar(
                seasonScroll,
                findViewById(R.id.seasonButtonsLayout),
                season -> {
                    selectedSeason = season;
                    loadStandings();
                }
        );

        if (seasons.isEmpty()) {
            seasonScroll.setVisibility(View.GONE);
            showMessage(getString(R.string.ps_no_data), false);
            return;
        }

        chips.setSeasons(seasons, selectedSeason);
        loadStandings();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(STATE_SEASON, selectedSeason);
    }

    @Override
    protected void onDestroy() {
        requestToken++;

        if (queue != null) {
            queue.cancelAll(this);
        }

        if (shimmerFrameLayout != null) {
            shimmerFrameLayout.stopShimmer();
        }

        super.onDestroy();
    }

    private void setupStandingsInsets() {
        View root = findViewById(R.id.standings_root);
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

    // ---------- screen states ----------

    private void updateHeader() {
        String headerText;

        if (Locale.getDefault().getLanguage().equals("ru")) {
            headerText = getString(R.string.past_season_drivers) + " " + selectedSeason;
        } else {
            headerText = selectedSeason + " " + getString(R.string.past_season_drivers);
        }

        driversHeader.setText(headerText);
    }

    private void showLoading() {
        stateContainer.setVisibility(View.GONE);
        recyclerView.setVisibility(View.GONE);
        shimmerFrameLayout.setVisibility(View.VISIBLE);
        shimmerFrameLayout.startShimmer();
    }

    private void showList() {
        shimmerFrameLayout.stopShimmer();
        shimmerFrameLayout.setVisibility(View.GONE);
        stateContainer.setVisibility(View.GONE);
        recyclerView.setVisibility(View.VISIBLE);
    }

    private void showMessage(String message, boolean canRetry) {
        shimmerFrameLayout.stopShimmer();
        shimmerFrameLayout.setVisibility(View.GONE);
        recyclerView.setVisibility(View.GONE);
        stateMessage.setText(message);
        findViewById(R.id.state_retry).setVisibility(canRetry ? View.VISIBLE : View.GONE);
        stateContainer.setVisibility(View.VISIBLE);
    }

    // ---------- loading ----------

    private boolean isUnavailable() {
        return isFinishing() || isDestroyed();
    }

    private void loadStandings() {
        final int token = ++requestToken;
        final int season = selectedSeason;

        queue.cancelAll(this);
        updateHeader();
        showLoading();

        loadReferenceData(() -> {
            if (isUnavailable() || token != requestToken) {
                return;
            }

            requestStandings(season, token);
        });
    }

    /** Team colours and the list of drivers that have a profile page; read once per screen. */
    private void loadReferenceData(Runnable done) {
        if (referenceLoaded) {
            done.run();
            return;
        }

        final int[] pending = {2};
        Runnable oneDone = () -> {
            pending[0]--;

            if (pending[0] == 0) {
                referenceLoaded = true;
                done.run();
            }
        };

        rootRef.child("constructors").get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                for (DataSnapshot team : task.getResult().getChildren()) {
                    String color = team.child("darkColor").getValue(String.class);

                    if (team.getKey() != null && color != null) {
                        databaseTeamColors.put(team.getKey(), color);
                    }
                }
            } else {
                Log.w(TAG, "Team colours are not available", task.getException());
            }

            oneDone.run();
        });

        rootRef.child("drivers").get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                for (DataSnapshot driver : task.getResult().getChildren()) {
                    if (driver.getKey() != null) {
                        knownDrivers.add(driver.getKey());
                    }
                }

                driversKnown = true;
            } else {
                Log.w(TAG, "Driver list is not available", task.getException());
            }

            oneDone.run();
        });
    }

    private void requestStandings(int season, int token) {
        String url = "https://api.jolpi.ca/ergast/f1/" + season + "/driverstandings/?format=json";

        JsonObjectRequest request = new cachedJsonRequest(
                Request.Method.GET,
                url,
                null,
                response -> {
                    if (isUnavailable() || token != requestToken) {
                        return;
                    }

                    try {
                        showStandings(parseStandings(response, season));
                    } catch (JSONException error) {
                        Log.e(TAG, "Unexpected standings response", error);
                        showMessage(getString(R.string.ps_load_error), true);
                    }
                },
                error -> {
                    if (isUnavailable() || token != requestToken) {
                        return;
                    }

                    Log.e(TAG, "Standings request failed", error);
                    showMessage(getString(R.string.ps_load_error), true);
                }
        );

        request.setTag(this);
        queue.add(request);
    }

    private List<driversList> parseStandings(JSONObject response, int season) throws JSONException {
        List<driversList> loaded = new ArrayList<>();
        JSONObject data = response.getJSONObject("MRData");
        JSONArray lists = data.getJSONObject("StandingsTable").optJSONArray("StandingsLists");

        if (lists == null) {
            return loaded;
        }

        for (int i = 0; i < lists.length(); i++) {
            JSONArray standings = lists.getJSONObject(i).optJSONArray("DriverStandings");

            if (standings == null) {
                continue;
            }

            for (int j = 0; j < standings.length(); j++) {
                JSONObject standing = standings.getJSONObject(j);
                JSONObject driver = standing.getJSONObject("Driver");

                String givenName = driver.getString("givenName");
                String familyName = driver.getString("familyName");
                String code = driver.optString("code", "");

                if (code.isEmpty()) {
                    code = familyName.substring(0, Math.min(3, familyName.length())).toUpperCase(Locale.ROOT);
                }

                String teamName = "";
                String teamId = "";
                JSONArray constructors = standing.optJSONArray("Constructors");

                if (constructors != null && constructors.length() > 0) {
                    // The last team of the season, as before.
                    JSONObject team = constructors.getJSONObject(constructors.length() - 1);
                    teamName = team.optString("name", "");
                    teamId = team.optString("constructorId", "");
                }

                driversList item = new driversList(
                        givenName,
                        familyName,
                        teamName,
                        teamId,
                        standing.getString("points"),
                        standing.getString("positionText"),
                        code,
                        false,
                        String.valueOf(season)
                );

                item.setImageUrl(storageRef.child(
                        "drivers/" + code.toLowerCase(Locale.ROOT) + "_" + season + ".png"));
                item.setCurrentSeason(mCurrentSeason);
                item.setTeamColor(pastTeamPalette.resolve(teamId, databaseTeamColors.get(teamId)));
                loaded.add(item);
            }
        }

        return loaded;
    }

    private void showStandings(List<driversList> loaded) {
        datum.clear();
        datum.addAll(loaded);

        // null means "the driver list could not be read": do not block any profile then.
        adapter.setLinkableDrivers(driversKnown ? knownDrivers : null);
        adapter.notifyDataSetChanged();

        if (datum.isEmpty()) {
            showMessage(getString(R.string.ps_no_data), false);
        } else {
            showList();
            recyclerView.scrollToPosition(0);
        }
    }
}
