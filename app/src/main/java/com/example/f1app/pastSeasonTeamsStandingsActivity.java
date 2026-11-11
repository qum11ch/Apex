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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Constructor standings of a past season. The newest past season opens first;
 * the season buttons under the header switch seasons without leaving the screen.
 */
public class pastSeasonTeamsStandingsActivity extends AppCompatActivity {

    private static final String TAG = "pastTeamsStandings";
    private static final String STATE_SEASON = "selectedSeason";

    private static final class DriverEntry {
        final String name;
        final double points;

        DriverEntry(String name, double points) {
            this.name = name;
            this.points = points;
        }
    }

    // One list instance for the whole life of the screen: the adapter keeps a reference to it.
    private final List<teamsList> datum = new ArrayList<>();
    private final Map<String, String> databaseTeamColors = new HashMap<>();
    private final Set<String> knownTeams = new HashSet<>();

    private RecyclerView recyclerView;
    private pastSeasonTeamsStandingsAdapter adapter;
    private ShimmerFrameLayout shimmerFrameLayout;
    private SwipeRefreshLayout swipeLayout;
    private TextView teamsHeader;
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
    private boolean teamsKnown = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.past_season_teams_standing_page);

        setupTeamsStandingsInsets();
        EdgeInsets.applyBottom(findViewById(R.id.recyclerview_currentTeams));

        if (!checkConnection(getApplicationContext())) {
            startActivity(connectionLostScreen.createShowSplashOnNetworkFailure(pastSeasonTeamsStandingsActivity.this));
        } else {
            startActivity(connectionLostScreen.createIntentHideSplashOnNetworkRecovery(pastSeasonTeamsStandingsActivity.this));
        }

        Bundle extras = getIntent().getExtras();
        mCurrentSeason = extras != null ? extras.getString("currentSeason") : null;

        int first = pastSeasons.FIRST_STANDINGS_SEASON;
        int last = pastSeasons.lastPastSeason(mCurrentSeason);
        String requested = savedInstanceState != null
                ? String.valueOf(savedInstanceState.getInt(STATE_SEASON, last))
                : (extras != null ? extras.getString("season") : null);
        selectedSeason = pastSeasons.initial(requested, first, last);

        shimmerFrameLayout = findViewById(R.id.shimmer_layout);
        recyclerView = findViewById(R.id.recyclerview_currentTeams);
        swipeLayout = findViewById(R.id.swipe_layout);
        teamsHeader = findViewById(R.id.teamsHeader);
        stateContainer = findViewById(R.id.state_container);
        stateMessage = findViewById(R.id.state_message);
        seasonScroll = findViewById(R.id.seasonScroll);

        queue = Volley.newRequestQueue(this);
        storageRef = FirebaseStorage.getInstance().getReference();
        rootRef = FirebaseDatabase.getInstance().getReference();

        adapter = new pastSeasonTeamsStandingsAdapter(this, datum);
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

    private void setupTeamsStandingsInsets() {
        View root = findViewById(R.id.teams_standings_root);
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
            headerText = getString(R.string.past_season_teams) + " " + selectedSeason;
        } else {
            headerText = selectedSeason + " " + getString(R.string.past_season_teams);
        }

        teamsHeader.setText(headerText);
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

            requestTeamStandings(season, token);
        });
    }

    private void loadReferenceData(Runnable done) {
        if (referenceLoaded) {
            done.run();
            return;
        }

        rootRef.child("constructors").get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                for (DataSnapshot team : task.getResult().getChildren()) {
                    String id = team.getKey();

                    if (id == null) {
                        continue;
                    }

                    knownTeams.add(id);
                    String color = team.child("darkColor").getValue(String.class);

                    if (color != null) {
                        databaseTeamColors.put(id, color);
                    }
                }

                teamsKnown = true;
            } else {
                Log.w(TAG, "Teams are not available", task.getException());
            }

            referenceLoaded = true;
            done.run();
        });
    }

    private void requestTeamStandings(int season, int token) {
        String url = "https://api.jolpi.ca/ergast/f1/" + season + "/constructorstandings/?format=json";

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.GET,
                url,
                null,
                response -> {
                    if (isUnavailable() || token != requestToken) {
                        return;
                    }

                    try {
                        List<JSONObject> standings = parseTeamStandings(response);

                        if (standings.isEmpty()) {
                            showTeams(new ArrayList<>());
                        } else {
                            loadLineUp(season, token, standings);
                        }
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

    private List<JSONObject> parseTeamStandings(JSONObject response) throws JSONException {
        List<JSONObject> result = new ArrayList<>();
        JSONObject data = response.getJSONObject("MRData");
        JSONArray lists = data.getJSONObject("StandingsTable").optJSONArray("StandingsLists");

        if (lists == null) {
            return result;
        }

        for (int i = 0; i < lists.length(); i++) {
            JSONArray standings = lists.getJSONObject(i).optJSONArray("ConstructorStandings");

            if (standings == null) {
                continue;
            }

            for (int j = 0; j < standings.length(); j++) {
                result.add(standings.getJSONObject(j));
            }
        }

        return result;
    }

    private void loadLineUp(int season, int token, List<JSONObject> standings) {
        rootRef.child("driverLineUp").child("season").child(String.valueOf(season)).get()
                .addOnCompleteListener(task -> {
                    if (isUnavailable() || token != requestToken) {
                        return;
                    }

                    if (task.isSuccessful() && task.getResult().exists()) {
                        showTeams(buildTeams(season, standings, task.getResult(), null));
                    } else {
                        requestDriversFromStandings(season, token, standings);
                    }
                });
    }

    private void requestDriversFromStandings(int season, int token, List<JSONObject> standings) {
        String url = "https://api.jolpi.ca/ergast/f1/" + season + "/driverstandings/?format=json";

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.GET,
                url,
                null,
                response -> {
                    if (isUnavailable() || token != requestToken) {
                        return;
                    }

                    Map<String, List<String>> drivers = new HashMap<>();

                    try {
                        drivers = parseDriversByTeam(response);
                    } catch (JSONException error) {
                        Log.w(TAG, "Could not read drivers of the season", error);
                    }

                    showTeams(buildTeams(season, standings, null, drivers));
                },
                error -> {
                    if (isUnavailable() || token != requestToken) {
                        return;
                    }

                    // The standings are still worth showing without driver names.
                    Log.w(TAG, "Drivers request failed", error);
                    showTeams(buildTeams(season, standings, null, new HashMap<>()));
                }
        );

        request.setTag(this);
        queue.add(request);
    }

    /** The two drivers with the most points for each team. */
    private Map<String, List<String>> parseDriversByTeam(JSONObject response) throws JSONException {
        Map<String, List<DriverEntry>> byTeam = new HashMap<>();
        JSONObject data = response.getJSONObject("MRData");
        JSONArray lists = data.getJSONObject("StandingsTable").optJSONArray("StandingsLists");

        if (lists != null) {
            for (int i = 0; i < lists.length(); i++) {
                JSONArray standings = lists.getJSONObject(i).optJSONArray("DriverStandings");

                if (standings == null) {
                    continue;
                }

                for (int j = 0; j < standings.length(); j++) {
                    JSONObject standing = standings.getJSONObject(j);
                    JSONObject driver = standing.getJSONObject("Driver");
                    JSONArray constructors = standing.optJSONArray("Constructors");

                    if (constructors == null || constructors.length() == 0) {
                        continue;
                    }

                    String teamId = constructors
                            .getJSONObject(constructors.length() - 1)
                            .optString("constructorId", "");
                    String name = driver.getString("givenName") + " " + driver.getString("familyName");
                    double points = standing.optDouble("points", 0);

                    List<DriverEntry> entries = byTeam.get(teamId);

                    if (entries == null) {
                        entries = new ArrayList<>();
                        byTeam.put(teamId, entries);
                    }

                    entries.add(new DriverEntry(name, points));
                }
            }
        }

        Map<String, List<String>> result = new HashMap<>();

        for (Map.Entry<String, List<DriverEntry>> entry : byTeam.entrySet()) {
            List<DriverEntry> entries = entry.getValue();
            Collections.sort(entries, (a, b) -> Double.compare(b.points, a.points));

            List<String> names = new ArrayList<>();

            for (int i = 0; i < Math.min(2, entries.size()); i++) {
                names.add(entries.get(i).name);
            }

            result.put(entry.getKey(), names);
        }

        return result;
    }

    private List<teamsList> buildTeams(
            int season,
            List<JSONObject> standings,
            DataSnapshot lineUp,
            Map<String, List<String>> derivedDrivers
    ) {
        List<teamsList> loaded = new ArrayList<>();

        for (JSONObject standing : standings) {
            JSONObject constructor = standing.optJSONObject("Constructor");

            if (constructor == null) {
                continue;
            }

            String teamId = constructor.optString("constructorId", "");
            ArrayList<String> drivers = new ArrayList<>();

            if (lineUp != null) {
                for (DataSnapshot driver : lineUp.child(teamId).child("drivers").getChildren()) {
                    if (driver.getKey() != null) {
                        drivers.add(driver.getKey());
                    }
                }
            } else if (derivedDrivers != null && derivedDrivers.get(teamId) != null) {
                drivers.addAll(derivedDrivers.get(teamId));
            }

            teamsList item = new teamsList(
                    constructor.optString("name", ""),
                    standing.optString("positionText", ""),
                    standing.optString("points", ""),
                    teamId,
                    false
            );

            item.setSeason(String.valueOf(season));
            item.setCurrentSeason(mCurrentSeason);
            item.setDrivers(drivers);
            item.setImageUrl(storageRef.child(
                    "teams/" + teamId.toLowerCase(Locale.ROOT) + "_" + season + ".png"));
            item.setTeamColor(pastTeamPalette.resolve(teamId, databaseTeamColors.get(teamId)));
            loaded.add(item);
        }

        return loaded;
    }

    private void showTeams(List<teamsList> loaded) {
        datum.clear();
        datum.addAll(loaded);

        // null means "the team list could not be read": do not block any team page then.
        adapter.setLinkableTeams(teamsKnown ? knownTeams : null);
        adapter.notifyDataSetChanged();

        if (datum.isEmpty()) {
            showMessage(getString(R.string.ps_no_data), false);
        } else {
            showList();
            recyclerView.scrollToPosition(0);
        }
    }
}
