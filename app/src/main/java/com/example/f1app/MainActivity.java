package com.example.f1app;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.facebook.shimmer.ShimmerFrameLayout;
import com.google.android.material.appbar.AppBarLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Field;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicInteger;


public class MainActivity extends AppCompatActivity {
    ImageButton showDriverButton, showSchedule, showTeams, showAccount;
    FirebaseDatabase database;
    private LinearLayout futureLayout, pastLayout;
    public static final String APP_PREFERENCES = "mysettings";
    private List<driversList> datumDrivers;
    private List<teamsList> datumTeams;
    private List<concludedRacesData> datumPast;
    private List<futureRaceData> datumFuture;
    private RecyclerView rvFuture, rvPast, rvDrivers, rvTeams;
    private ShimmerFrameLayout sfFuture, sfPast, sfDrivers, sfTeams, sfProgressBar;
    private ProgressBar raceProgress;
    private TextView raceProgressText, pastRaceHeader;
    SharedPreferences mPrefs;
    private static final long HOUR = 3600*1000;
    private static final long SPRINT_QUALI_DIFF = 44*60*1000;
    private static final int REQUEST_CODE_NOTIFICATIONS = 1001;
    private static final String KEY_NOTIFICATIONS_ENABLED = "notifications_enabled";
    private mainDriversStandingsAdapter driversAdapter;
    private mainTeamsStandingsAdapter teamsAdapter;
    private mainPastRaceAdapter pastRaceAdapter;
    private futureRaceAdapter futureRaceAdapter;
    private String currentSeason;
    private FirebaseStorage storage;
    private StorageReference storageRef;
    private DatabaseReference rootRef;
    private FirebaseUser currentUser;
    private SwipeRefreshLayout swipeLayout;
    private boolean accountNavigationInProgress = false;
    private Button allDriversButton;
    private int progressDelayMs = 500;

    private com.google.firebase.database.Query scheduleQuery;
    private ValueEventListener scheduleListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        View root = findViewById(R.id.main_root);
        View footerContainer = findViewById(R.id.footer_container);

        final int initialLeft = root.getPaddingLeft();
        final int initialTop = root.getPaddingTop();
        final int initialRight = root.getPaddingRight();

        final int footerGap = Math.round(
                1 * getResources().getDisplayMetrics().density
        );

        ViewCompat.setOnApplyWindowInsetsListener(root, (view, windowInsets) -> {
            Insets safeInsets = windowInsets.getInsets(
                    WindowInsetsCompat.Type.systemBars()
                            | WindowInsetsCompat.Type.displayCutout()
            );

            // Сверху и по бокам защищаем весь интерфейс.
            // Снизу контент продолжает рисоваться до края экрана.
            view.setPadding(
                    initialLeft + safeInsets.left,
                    initialTop + safeInsets.top,
                    initialRight + safeInsets.right,
                    0
            );

            // Только футер поднимаем выше системной навигации.
            ViewGroup.MarginLayoutParams params =
                    (ViewGroup.MarginLayoutParams) footerContainer.getLayoutParams();

            int bottomMargin = safeInsets.bottom + footerGap;

            if (params.bottomMargin != bottomMargin) {
                params.bottomMargin = bottomMargin;
                footerContainer.setLayoutParams(params);
            }

            return WindowInsetsCompat.CONSUMED;
        });

        ViewCompat.requestApplyInsets(root);

        // main_footer_padding: the list ends above the footer, whatever its height
        final View scrollContent = findViewById(R.id.nestedScrollView);
        footerContainer.addOnLayoutChangeListener((v, l, t, r, b, oldL, oldT, oldR, oldB) -> {
            ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) v.getLayoutParams();
            final int need = (b - t) + lp.bottomMargin
                    + Math.round(8 * getResources().getDisplayMetrics().density);
            if (scrollContent.getPaddingBottom() != need) {
                scrollContent.post(() -> scrollContent.setPadding(
                        scrollContent.getPaddingLeft(), scrollContent.getPaddingTop(),
                        scrollContent.getPaddingRight(), need));
            }
        });

        if (authSessionGate.redirectIfRequired(this, null)) {
            return;
        }


        networkNotice.showIfOffline(MainActivity.this);

        storage = FirebaseStorage.getInstance();
        storageRef = storage.getReference();
        rootRef = FirebaseDatabase.getInstance().getReference();

        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        LocalDate currentDate = LocalDate.now();
        database = FirebaseDatabase.getInstance();

        futureLayout = findViewById(R.id.main_future);
        pastLayout = findViewById(R.id.main_past);

        rvFuture = findViewById(R.id.recyclerView_future);
        rvPast = findViewById(R.id.recyclerView_past);
        rvDrivers = findViewById(R.id.recyclerView_drivers);
        rvTeams = findViewById(R.id.recyclerView_teams);
        raceProgress = findViewById(R.id.race_progress);
        raceProgressText = findViewById(R.id.race_progress_text);
        pastRaceHeader = findViewById(R.id.pastRace_header);

        sfFuture = findViewById(R.id.shimmerFuture_layout);
        sfPast = findViewById(R.id.shimmerPast_layout);
        sfDrivers = findViewById(R.id.shimmerDrivers_layout);
        sfTeams = findViewById(R.id.shimmerTeams_layout);
        sfProgressBar = findViewById(R.id.shimmerProgress_layout);

        datumDrivers = new cachedList<>("main_drivers");
        datumTeams = new cachedList<>("main_teams");
        datumPast = new cachedList<>("main_past");
        datumFuture = new cachedList<>("main_future");

        driversAdapter = new mainDriversStandingsAdapter(this, datumDrivers);
        rvDrivers.setAdapter(driversAdapter);

        teamsAdapter = new mainTeamsStandingsAdapter(this, datumTeams);
        rvTeams.setAdapter(teamsAdapter);

        pastRaceAdapter = new mainPastRaceAdapter(this, datumPast);
        rvPast.setAdapter(pastRaceAdapter);

        futureRaceAdapter = new futureRaceAdapter(this, datumFuture);
        rvFuture.setAdapter(futureRaceAdapter);

        Button winnersWDC = findViewById(R.id.winnersWDS);

        sfProgressBar.startShimmer();
        sfFuture.startShimmer();
        sfPast.startShimmer();
        sfDrivers.startShimmer();
        sfTeams.startShimmer();

        // seen before: the remembered blocks are shown at once, no shimmer
        cachedList.restore(datumDrivers, driversAdapter, rvDrivers, sfDrivers);
        cachedList.restore(datumTeams, teamsAdapter, rvTeams, sfTeams);
        cachedList.restore(datumPast, pastRaceAdapter, rvPast, sfPast);
        cachedList.restore(datumFuture, futureRaceAdapter, rvFuture, sfFuture);

        if (cachedList.hasCache(datumFuture) && !datumFuture.isEmpty()) {
            futureLayout.setVisibility(View.VISIBLE);
        }

        if (cachedList.hasCache(datumPast) && !datumPast.isEmpty()) {
            pastLayout.setVisibility(View.VISIBLE);
            pastRaceHeader.setVisibility(View.VISIBLE);
        }

        final String[] seenProgress = cachedList.getExtra("main_progress");

        if (seenProgress != null) {
            progressDelayMs = 0;
            postProgress(Integer.parseInt(seenProgress[0]), Long.parseLong(seenProgress[1]));
            progressDelayMs = 500;
        }

        allDriversButton = findViewById(R.id.allDriversButton);
        allDriversButton.setOnClickListener(v -> {
            Intent intent = new Intent(
                    MainActivity.this,
                    allDriversActivity.class
            );

            intent.putExtra("currentSeason", currentSeason);
            startActivity(intent);
        });

        // rvFuture.setHasFixedSize(true);
        // rvPast.setHasFixedSize(true);
        // rvDrivers.setHasFixedSize(true);
        // rvTeams.setHasFixedSize(true);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        rvFuture.setLayoutManager(linearLayoutManager);
        LinearLayoutManager linearLayoutManager2 = new LinearLayoutManager(this);
        rvPast.setLayoutManager(linearLayoutManager2);
        LinearLayoutManager linearLayoutManager3 = new LinearLayoutManager(this);
        rvDrivers.setLayoutManager(linearLayoutManager3);
        LinearLayoutManager linearLayoutManager4 = new LinearLayoutManager(this);
        rvTeams.setLayoutManager(linearLayoutManager4);

        Button openPredictionsButton = findViewById(R.id.predictionsPageButton);

        openPredictionsButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, predictionsActivity.class);
            startActivity(intent);
        });

        Button openUsersPredictionsButton = findViewById(R.id.userPredictionsButton);

        Button leaderboardButton = findViewById(R.id.leaderboardButton);

        leaderboardButton.setOnClickListener(v -> {
            Intent intent = new Intent(
                    MainActivity.this,
                    leaderboardActivity.class
            );

            intent.putExtra("currentSeason", currentSeason);
            startActivity(intent);
        });

        if (currentUser == null) {
            openUsersPredictionsButton.setVisibility(View.GONE);
        } else {
            openUsersPredictionsButton.setVisibility(View.VISIBLE);

            openUsersPredictionsButton.setOnClickListener(v -> {
                Intent intent = new Intent(
                        MainActivity.this,
                        userPredictionsActivity.class
                );
                startActivity(intent);
            });
        }

        swipeLayout = findViewById(R.id.swipe_layout);
        swipeLayout.setOnRefreshListener(this::refreshMainScreen);

        showDriverButton = findViewById(R.id.showDriver);
        showDriverButton.setOnClickListener(v -> {
                    startActivity_getSeason(driversStandingsActivity.class);
                });

        showSchedule = findViewById(R.id.showSchedule);
        showSchedule.setOnClickListener(v ->
                startActivity_getSeason(scheduleActivity.class));

        showTeams = findViewById(R.id.showTeams);
        showTeams.setOnClickListener(v ->
                startActivity_getSeason(teamsStandingsActivity.class));

        showAccount = findViewById(R.id.showAccount);
        showAccount.setOnClickListener(v -> openAccountOrLogin());

        String currentYear = Integer.toString(currentDate.getYear());

        Button predict = findViewById(R.id.predict);

        DatabaseReference rootRef = database.getReference();

        rootRef.child("status")
                .addListenerForSingleValueEvent(
                        new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot) {

                                currentSeason = snapshot.child("season").getValue(String.class);

                                if (currentSeason == null || currentSeason.trim().isEmpty()) {
                                    showError("Сезон не найден в Firebase");
                                    return;
                                }

                                predict.setVisibility(View.VISIBLE);

                                getTeamStanding(currentSeason);
                                getDriversStanding(currentSeason);
                                getSchedule(currentYear, currentDate);
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {
                                showError(error.getMessage());
                                Log.e(
                                        "MainActivity",
                                        "status request failed: "
                                                + error.getMessage()
                                );
                            }
                        }
                );

        winnersWDC.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, winnersWDCActivity.class);
            intent.putExtra("currentSeason", currentSeason);
            MainActivity.this.startActivity(intent);
        });

        AppBarLayout appBarLayout = findViewById(R.id.appbar);
        appBarLayout.setExpanded(true,true);

        mPrefs = getSharedPreferences(APP_PREFERENCES, Activity.MODE_PRIVATE);
        checkNotificationPermission();

        predict.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, neuroPredictionActivity.class);
            intent.putExtra("currentSeason", currentSeason);
            MainActivity.this.startActivity(intent);

        });
    }

    private void openAccountOrLogin() {
        if (accountNavigationInProgress) return;

        // Сохраняем ваш механизм обязательного повторного входа.
        if (authSessionGate.redirectIfRequired(this, currentSeason)) {
            return;
        }

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user == null) {
            startActivity_getSeason(logInPageActivity.class);
            return;
        }

        if (!networkNotice.isOnline(this)) {
            // the profile cannot be checked without the network: the sign-in screen finishes the check later
            startActivity_getSeason(logInPageActivity.class);
            return;
        }

        String checkedUid = user.getUid();

        accountNavigationInProgress = true;
        showAccount.setEnabled(false);

        FirebaseDatabase.getInstance()
                .getReference("users")
                .child(checkedUid)
                .get()
                .addOnCompleteListener(task -> {
                    accountNavigationInProgress = false;

                    if (isFinishing() || isDestroyed()) return;

                    showAccount.setEnabled(true);

                    if (!task.isSuccessful()) {
                        Log.e(
                                "AccountNavigation",
                                "Не удалось проверить профиль",
                                task.getException()
                        );

                        Toast.makeText(
                                this,
                                getString(R.string.account_open_error),
                                Toast.LENGTH_SHORT
                        ).show();
                        return;
                    }

                    // Сессия могла измениться во время запроса.
                    FirebaseUser current =
                            FirebaseAuth.getInstance().getCurrentUser();

                    if (current == null ||
                            !checkedUid.equals(current.getUid())) {
                        startActivity_getSeason(logInPageActivity.class);
                        return;
                    }

                    DataSnapshot snapshot = task.getResult();

                    Boolean migrationComplete =
                            snapshot.child("usernameMigrationComplete")
                                    .getValue(Boolean.class);

                    String username =
                            snapshot.child("username")
                                    .getValue(String.class);

                    boolean profileReady =
                            snapshot.exists()
                                    && Boolean.TRUE.equals(migrationComplete)
                                    && username != null
                                    && !username.trim().isEmpty();

                    if (profileReady) {
                        startActivity_getSeason(accountPageActivity.class);
                    } else {
                        startActivity_getSeason(logInPageActivity.class);
                    }
                });
    }

    private void refreshMainScreen() {
        String currentYear = Integer.toString(LocalDate.now().getYear());

        cachedList.reset(datumDrivers);
        cachedList.reset(datumTeams);
        cachedList.reset(datumPast);
        cachedList.reset(datumFuture);

        driversAdapter.notifyDataSetChanged();
        teamsAdapter.notifyDataSetChanged();
        pastRaceAdapter.notifyDataSetChanged();
        futureRaceAdapter.notifyDataSetChanged();

        // Возвращаем shimmer
        showShimmer(rvDrivers, sfDrivers);
        showShimmer(rvTeams, sfTeams);
        showShimmer(rvPast, sfPast);
        showShimmer(rvFuture, sfFuture);

        // Скрываем блоки до новой загрузки
        futureLayout.setVisibility(View.GONE);
        pastLayout.setVisibility(View.GONE);
        pastRaceHeader.setVisibility(View.GONE);

        if (currentSeason == null || currentSeason.trim().isEmpty()) {
            rootRef.child("status")
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            currentSeason = snapshot.child("season").getValue(String.class);

                            if (currentSeason == null || currentSeason.trim().isEmpty()) {
                                swipeLayout.setRefreshing(false);
                                showError("Firebase error");
                                return;
                            }

                            loadMainData(currentYear);
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            swipeLayout.setRefreshing(false);
                            showError(error.getMessage());
                            Log.e("MainActivity", "status request failed: " + error.getMessage());
                        }
                    });
        } else {
            loadMainData(currentYear);
        }
    }

    private void loadMainData(String currentYear) {
        getTeamStanding(currentSeason);
        getDriversStanding(currentSeason);
        getSchedule(currentYear, LocalDate.now());

        swipeLayout.setRefreshing(false);
    }

    private void startActivity_getSeason(Class<?> target) {
        String season = currentSeason;

        if (season == null || season.trim().isEmpty()) {
            season = "2026";
        }

        Intent intent = new Intent(this, target);
        intent.putExtra("currentSeason", season);
        startActivity(intent);
        overridePendingTransition(0, 0);
    }

    private void checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                if (!ActivityCompat.shouldShowRequestPermissionRationale(this,
                        Manifest.permission.POST_NOTIFICATIONS)) {
                    ActivityCompat.requestPermissions(this,
                            new String[]{Manifest.permission.POST_NOTIFICATIONS},
                            REQUEST_CODE_NOTIFICATIONS);
                }
            } else {
                saveNotificationPreference(true);
            }
        } else {
            saveNotificationPreference(true);
        }
    }


    private void saveNotificationPreference(boolean isEnabled) {
        mPrefs.edit().putBoolean(KEY_NOTIFICATIONS_ENABLED, isEnabled).apply();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_CODE_NOTIFICATIONS) {
            saveNotificationPreference(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED);
        }
    }

    private void postProgress(int progress, long totalRounds) {
        cachedList.putExtra("main_progress", String.valueOf(progress), String.valueOf(totalRounds));
        String strProgress = progress + "/" + totalRounds;
        if (progress>10 && progress<=13){
            raceProgressText.setTextColor(getColor(R.color.dark_grey));
        } else if (progress>13) {
            raceProgressText.setTextColor(getColor(R.color.white));
        }

        Handler handler = new Handler();
        handler.postDelayed(()->{
            raceProgress.setVisibility(View.VISIBLE);
            sfProgressBar.setVisibility(View.GONE);
            sfProgressBar.stopShimmer();
            raceProgress.setProgress(progress);
            raceProgressText.setText(strProgress);
        }, progressDelayMs);
    }

    public void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void getDriversStanding(String season){
        RequestQueue queue = apiQueue.get();
        String url2 = "https://api.jolpi.ca/ergast/f1/" + season + "/driverstandings/?format=json";
        JsonObjectRequest jsonObjectRequest2 = new cachedJsonRequest(
                Request.Method.GET,
                url2,
                null,
                response -> {
                    try {
                        JSONObject MRData = response.getJSONObject("MRData");
                        String total = MRData.getString("total");
                        if (!total.equals("0")){
                            JSONObject StandingsTable = MRData.getJSONObject("StandingsTable");
                            JSONArray StandingsLists = StandingsTable.getJSONArray("StandingsLists");
                            for(int i = 0; i < StandingsLists.length(); i++){
                                JSONArray DriverStandings = StandingsLists.getJSONObject(i)
                                        .getJSONArray("DriverStandings");
                                for(int j = 0; j < 3; j++) {
                                    String placement = DriverStandings.getJSONObject(j).getString("positionText");
                                    String points = DriverStandings.getJSONObject(j).getString("points");
                                    String driverName = DriverStandings.getJSONObject(j)
                                            .getJSONObject("Driver").getString("givenName");
                                    String driverFamilyName = DriverStandings.getJSONObject(j)
                                            .getJSONObject("Driver").getString("familyName");
                                    String driverCode = DriverStandings.getJSONObject(j)
                                            .getJSONObject("Driver").getString("code");
                                    JSONArray Constructors = DriverStandings.getJSONObject(j).getJSONArray("Constructors");
                                    String constructorsName = Constructors.getJSONObject(Constructors.length() - 1).getString("name");
                                    String constructorId = Constructors.getJSONObject(Constructors.length() - 1).getString("constructorId");

                                    StorageReference mDriverImage = storageRef.child("drivers/" + driverCode.toLowerCase() + "_"  + currentSeason + ".png");
                                    driversList smth = new driversList(driverName, driverFamilyName, constructorsName, constructorId, points, placement, driverCode, false, season);
                                    smth.setImageUrl(mDriverImage);

                                    final String teamKey = constructorId;
                                    smth.setTeamColor(teamColors.get(teamKey)); // known colour: no grey flash

                                    if (smth.getTeamColor() == null) {
                                        teamColors.fetch(rootRef, teamKey, color -> {
                                            smth.setTeamColor(color != null ? color : "#ffffff");
                                            driversAdapter.notifyDataSetChanged();
                                        });
                                    }

                                    datumDrivers.add(smth);
                                }
                            }
                            hideShimmer(rvDrivers, sfDrivers);
                            driversAdapter.notifyDataSetChanged();

                        }else{
                            AtomicInteger driverCount = new AtomicInteger(0);
                            final int MAX_DRIVERS = 3;
                            DatabaseReference rootRef = database.getReference();
                            rootRef.child("constructors").orderByChild("lastSeasonPos").limitToFirst(3).addValueEventListener(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot snapshot) {
                                    datumDrivers.clear();
                                    for (DataSnapshot child: snapshot.getChildren()) {
                                        if (driverCount.get() >= MAX_DRIVERS) {
                                            hideShimmer(rvDrivers, sfDrivers);
                                            break;
                                        }

                                        String constructorId = child.child("constructorId").getValue(String.class);
                                        String constructorsName = child.child("name").getValue(String.class);
                                        String teamColor = teamColors.hex(child.child("darkColor").getValue(String.class));

                                        rootRef.child("driverLineUp/season/" + season + "/" + constructorId)
                                                .addListenerForSingleValueEvent(new ValueEventListener() {
                                                    @Override
                                                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                                                        if (!snapshot.hasChild("drivers")) return;

                                                        for (DataSnapshot driverDataSnapshot : snapshot.child("drivers").getChildren()) {
                                                            if (driverCount.get() >= MAX_DRIVERS) {
                                                                hideShimmer(rvDrivers, sfDrivers);
                                                                return;
                                                            }

                                                            final String currentDriverFullname = driverDataSnapshot.getKey();
                                                            final String finalConstructorsName = constructorsName;
                                                            final String finalConstructorId = constructorId;

                                                            DatabaseReference driversRef = rootRef.child("drivers");
                                                            DatabaseReference driverRef = driversRef.child(currentDriverFullname);

                                                            driverRef.addListenerForSingleValueEvent(new ValueEventListener() {
                                                                @Override
                                                                public void onDataChange(DataSnapshot dataSnapshot) {
                                                                    if (driverCount.get() >= MAX_DRIVERS) return;
                                                                    final int currentDriverPlace = driverCount.get() + 1;

                                                                    String[] parts = currentDriverFullname.split(" ");
                                                                    String driverName, driverFamilyName;
                                                                    if(currentDriverFullname.equals("Andrea Kimi Antonelli")){
                                                                        driverName = parts[0] + " " + parts[1];
                                                                        driverFamilyName = parts[2];
                                                                    } else {
                                                                        driverName = parts[0];
                                                                        driverFamilyName = parts[1];
                                                                    }
                                                                    String driverCode = dataSnapshot.child("driversCode").getValue(String.class);

                                                                    StorageReference mDriverImage = storageRef.child("drivers/" + driverCode.toLowerCase() + "_"  + currentSeason + ".png");

                                                                    driversList smth = new driversList(driverName, driverFamilyName, finalConstructorsName,
                                                                            finalConstructorId, "", String.valueOf(currentDriverPlace), driverCode, true, season);
                                                                    smth.setImageUrl(mDriverImage);
                                                                    smth.setTeamColor(teamColor);
                                                                    datumDrivers.add(smth);
                                                                    driverCount.incrementAndGet();
                                                                    driversAdapter.notifyDataSetChanged();

                                                                    if (driverCount.get() >= MAX_DRIVERS) {
                                                                        hideShimmer(rvDrivers, sfDrivers);
                                                                    }
                                                                }

                                                                @Override
                                                                public void onCancelled(@NonNull DatabaseError databaseError) {
                                                                    showError(databaseError.getMessage());
                                                                    Log.e("driverStandingsError", databaseError.getMessage());
                                                                }
                                                            });
                                                        }
                                                    }

                                                    @Override
                                                    public void onCancelled(@NonNull DatabaseError error) {
                                                        showError(error.getMessage());
                                                        Log.e("driverStandingsError", error.getMessage());
                                                    }
                                                });
                                    }
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError error) {
                                    showError(error.getMessage());
                                    Log.e("driverStandingsError", error.getMessage());
                                }
                            });

                        }
                    } catch (JSONException e) {
                        showError(e.getMessage());
                        e.printStackTrace();
                    }
                }, error -> Toast.makeText(MainActivity.this, " " + error.getMessage(), Toast.LENGTH_SHORT).show());
        queue.add(jsonObjectRequest2);
    }

    private void getTeamStanding(String season){
        RequestQueue queue = apiQueue.get();
        String url2 = "https://api.jolpi.ca/ergast/f1/" + season + "/constructorstandings/?format=json";
        JsonObjectRequest jsonObjectRequest2 = new cachedJsonRequest(
                Request.Method.GET,
                url2,
                null,
                response -> {
                    try {
                        JSONObject MRData = response.getJSONObject("MRData");
                        String total = MRData.getString("total");
                        if (!total.equals("0")){
                            JSONObject StandingsTable = MRData.getJSONObject("StandingsTable");
                            JSONArray StandingsLists = StandingsTable.getJSONArray("StandingsLists");
                            for(int i = 0; i < StandingsLists.length(); i++){
                                JSONArray ConstructorStandings = StandingsLists.getJSONObject(i)
                                        .getJSONArray("ConstructorStandings");
                                for(int j = 0; j < 3; j++){
                                    String constructorName = ConstructorStandings.getJSONObject(j)
                                            .getJSONObject("Constructor").getString("name");
                                    String position = ConstructorStandings.getJSONObject(j).getString("positionText");
                                    String points = ConstructorStandings.getJSONObject(j).getString("points");
                                    String constructorId = ConstructorStandings.getJSONObject(j)
                                            .getJSONObject("Constructor").getString("constructorId");
                                    DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference();

                                    StorageReference mTeamCar = storageRef.child("teams/" + constructorId.toLowerCase() + "_"  + season + ".png");

                                    rootRef.child("driverLineUp/season/" + season + "/" + constructorId).addValueEventListener(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                                            ArrayList<String> teamDrivers = new ArrayList<>();
                                            for (DataSnapshot driverDataSnapshot : snapshot.child("drivers").getChildren()) {
                                                String driverFullname = driverDataSnapshot.getKey();
                                                teamDrivers.add(driverFullname);
                                            }

                                            rootRef.child("constructors").child(constructorId).addListenerForSingleValueEvent(new ValueEventListener() {
                                                @Override
                                                public void onDataChange(@NonNull DataSnapshot snapshot) {
                                                    String mTeamColor = teamColors.hex(snapshot.child("darkColor").getValue(String.class));

                                                    teamsList smth = new teamsList(constructorName, position, points, constructorId, false);
                                                    smth.setDrivers(teamDrivers);
                                                    smth.setSeason(season);
                                                    smth.setImageUrl(mTeamCar);
                                                    smth.setTeamColor(mTeamColor);
                                                    datumTeams.add(smth);
                                                    hideShimmer(rvTeams, sfTeams);
                                                    teamsAdapter.notifyDataSetChanged();

                                                }

                                                @Override
                                                public void onCancelled(@NonNull DatabaseError error) {
                                                    showError("Team color information getting error:" + error.getMessage());
                                                    Log.e(String.valueOf(MainActivity.this), "Team color information getting error:" + error.getMessage());
                                                }
                                            });
                                        }

                                        @Override
                                        public void onCancelled(@NonNull DatabaseError error) {
                                            showError(error.getMessage());
                                            Log.e("MainActivityTeams", error.getMessage());
                                        }
                                    });
                                }
                            }

                        }
                        else{
                            DatabaseReference rootRef = database.getReference();
                            rootRef.child("constructors").orderByChild("lastSeasonPos").limitToFirst(3).addValueEventListener(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot snapshot) {
                                    datumDrivers.clear();
                                    int teamIndex = 0;
                                    for (DataSnapshot child: snapshot.getChildren()) {
                                        final int currentTeamPlace = teamIndex++;
                                        String constructorId = child.child("constructorId").getValue(String.class);
                                        String constructorsName = child.child("name").getValue(String.class);
                                        String teamColor = teamColors.hex(child.child("darkColor").getValue(String.class));

                                        StorageReference mTeamCar = storageRef.child("teams/" + constructorId.toLowerCase() + "_"  + season + ".png");

                                        DatabaseReference rootRef = database.getReference();
                                        rootRef.child("driverLineUp/season/" + season + "/" + constructorId).addListenerForSingleValueEvent(new ValueEventListener() {
                                            @Override
                                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                                ArrayList<String> teamDrivers = new ArrayList<>();
                                                for (DataSnapshot driverDataSnapshot : snapshot.child("drivers").getChildren()) {
                                                    String driverFullname = driverDataSnapshot.getKey();
                                                    teamDrivers.add(driverFullname);
                                                }
                                                teamsList smth = new teamsList(constructorsName, String.valueOf(currentTeamPlace + 1), "", constructorId, true);
                                                smth.setDrivers(teamDrivers);
                                                smth.setSeason(season);
                                                smth.setImageUrl(mTeamCar);
                                                smth.setTeamColor(teamColor);

                                                hideShimmer(rvTeams, sfTeams);
                                                datumTeams.add(smth);
                                                teamsAdapter.notifyDataSetChanged();
                                            }
                                            @Override
                                            public void onCancelled(@NonNull DatabaseError error) {
                                                showError(error.getMessage());
                                                Log.e("MainActivityTeams", error.getMessage());
                                            }
                                        });
                                    }

                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError error) {
                                    showError(error.getMessage());
                                    Log.e("MainActivityTeams", error.getMessage());
                                }
                            });

                        }
                    } catch (JSONException e) {
                        showError(e.getMessage());
                        Log.e("MainActivityTeams", " " + e.getMessage());
                    }
                }, error -> Toast.makeText(MainActivity.this, " " + error.getMessage(), Toast.LENGTH_SHORT).show());
        queue.add(jsonObjectRequest2);
    }

    private void removeScheduleListener() {
        if (scheduleQuery != null && scheduleListener != null) {
            scheduleQuery.removeEventListener(scheduleListener);
        }

        scheduleQuery = null;
        scheduleListener = null;
    }

    @Override
    protected void onDestroy() {
        removeScheduleListener();
        super.onDestroy();
    }

    private void getSchedule(String year, LocalDate currentDate){
        ArrayList<String> concludedRoundNumber = new ArrayList<>();
        ArrayList<String> futureRaceRoundNumber = new ArrayList<>();
        ArrayList<String> raceNames = new ArrayList<>();
        ArrayList<String> circuitsId = new ArrayList<>();
        DatabaseReference rootRef = database.getReference();
        removeScheduleListener();
        scheduleQuery = rootRef.child("schedule/season/" + year).orderByChild("round");
        scheduleListener = scheduleQuery.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                long totalRounds = snapshot.getChildrenCount();
                concludedRoundNumber.clear();
                futureRaceRoundNumber.clear();
                raceNames.clear();
                circuitsId.clear();

                for (DataSnapshot ds : snapshot.getChildren()) {
                    Integer round = ds.child("round").getValue(Integer.class);

                    raceWeekendStatus.State state =
                            raceWeekendStatus.resolve(ds, System.currentTimeMillis());

                    boolean isCanceled = raceWeekendStatus.isCanceled(ds);

                    switch (state) {
                        case CONCLUDED:
                        case CANCELED:
                            concludedRoundNumber.add(round.toString());
                            break;
                        case FUTURE:
                            if (!isCanceled) futureRaceRoundNumber.add(round.toString());
                            break;
                        case ONGOING:
                        case AWAITING_RESULTS:
                            futureRaceRoundNumber.add(round.toString());
                            break;
                    }
                }
                if(!concludedRoundNumber.isEmpty()){
                    pastLayout.setVisibility(View.VISIBLE);
                    getPastRace(year, concludedRoundNumber.get(concludedRoundNumber.size()-1));
                    postProgress(Integer.parseInt(concludedRoundNumber.get(concludedRoundNumber.size()-1)), totalRounds);
                }else{
                    postProgress(0, totalRounds);
                    pastLayout.setVisibility(View.GONE);
                    pastRaceHeader.setVisibility(View.GONE);
                }
                if(!futureRaceRoundNumber.isEmpty()){
                    futureLayout.setVisibility(View.VISIBLE);
                    getFutureRace(year, futureRaceRoundNumber.get(0));
                }else{
                    futureLayout.setVisibility(View.GONE);
                }

                boolean notificationsEnabled = mPrefs.getBoolean(KEY_NOTIFICATIONS_ENABLED, false);

                if(notificationsEnabled){
                    getRaceSchedule(raceNames, year, circuitsId);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                showError(error.getMessage());
                Log.e("scheduleActivityFirebaseError", error.getMessage());
            }
        });
    }

    private void getPastRace(String currentYear, String round){
        DatabaseReference rootRef = database.getReference();
        rootRef.child("schedule/season/" + currentYear).orderByChild("round").equalTo(Integer.parseInt(round)).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for(DataSnapshot ds : snapshot.getChildren()) {
                    String winnerCode = ds.child("RaceResults/raceWinnerCode").getValue(String.class);
                    Boolean hasCanceled = ds.child("Canceled").getValue(Boolean.class);

                    boolean isCanceled;
                    isCanceled = hasCanceled != null;

                    if (!winnerCode.equals("N/A") || isCanceled){
                        String raceName = ds.child("Circuit/raceName").getValue(String.class);
                        String dateStart = ds.child("FirstPractice/firstPracticeDate").getValue(String.class);
                        String dateEnd = ds.child("raceDate").getValue(String.class);
                        String circuitId = ds.child("Circuit/circuitId").getValue(String.class);
                        String secondCode = ds.child("RaceResults/raceSecondCode").getValue(String.class);
                        String thirdCode = ds.child("RaceResults/raceThirdCode").getValue(String.class);

                        rootRef.child("circuits/" + circuitId).addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                String circuitName = dataSnapshot.child("circuitName").getValue(String.class);
                                String raceCountry = dataSnapshot.child("country").getValue(String.class);
                                String raceLocation = dataSnapshot.child("location").getValue(String.class);

                                concludedRacesData concludedRace = new concludedRacesData(dateStart,
                                        dateEnd, raceName, round, circuitName, raceCountry, raceLocation, winnerCode, secondCode,
                                        thirdCode, currentYear);
                                concludedRace.setCanceled(isCanceled);
                                if (isDestroyed()) return;
                                datumPast.clear(); // one block = one race: replace, never append
                                datumPast.add(concludedRace);

                                hideShimmer(rvPast, sfPast);
                                pastRaceAdapter.notifyDataSetChanged();
                            }
                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {
                                showError(error.getMessage());
                                Log.e("MainActivityTeams", error.getMessage());
                            }
                        });
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                showError(error.getMessage());
                Log.e("MainActivityTeams", error.getMessage());
            }
        });
    }

    private void getFutureRace(String currentYear, String round){
        DatabaseReference rootRef = database.getReference();
        String newRound = round.trim().split("\\s+")[0];
        rootRef.child("schedule/season/" + currentYear).orderByChild("round")
                .equalTo(Integer.parseInt(newRound)).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        for(DataSnapshot ds : dataSnapshot.getChildren()) {
                            String raceName = ds.child("Circuit/raceName").getValue(String.class);
                            String dateStart = ds.child("FirstPractice/firstPracticeDate").getValue(String.class);
                            String dateEnd = ds.child("raceDate").getValue(String.class);
                            String circuitId = ds.child("Circuit/circuitId").getValue(String.class);

                            Boolean hasCanceled = ds.child("Canceled").getValue(Boolean.class);

                            boolean isCanceled;
                            isCanceled = hasCanceled != null;

                            rootRef.child("circuits/" + circuitId).addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                    String circuitName = dataSnapshot.child("circuitName").getValue(String.class);
                                    String raceCountry = dataSnapshot.child("country").getValue(String.class);
                                    String raceLocation = dataSnapshot.child("location").getValue(String.class);
                                    futureRaceData futureRaceData = new futureRaceData(raceName, dateStart, dateEnd,
                                            circuitName, round, raceCountry, circuitId);
                                    futureRaceData.setLocality(raceLocation);
                                    futureRaceData.setCanceled(isCanceled);

                                    futureRaceData.setWeekendState(
                                            raceWeekendStatus.resolve(ds, System.currentTimeMillis())
                                    );
                                    futureRaceData.setSessions(raceWeekendStatus.sessions(ds));

                                    if (isDestroyed()) return;
                                    datumFuture.clear(); // one block = one race: replace, never append
                                    datumFuture.add(futureRaceData);

                                    hideShimmer(rvFuture, sfFuture);
                                    futureRaceAdapter.notifyDataSetChanged();
                                }
                                @Override
                                public void onCancelled(@NonNull DatabaseError error) {
                                    showError("getFutureRace error" + error.getMessage());
                                    Toast.makeText(MainActivity.this, "MainActivity getFutureRace error" + error.getMessage(), Toast.LENGTH_SHORT).show();
                                }
                            });
                        }

                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        showError("getFutureRace error" + error.getMessage());
                        Toast.makeText(MainActivity.this, "MainActivity getFutureRace error" + error.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void getRaceSchedule(ArrayList<String> raceNames, String currentYear, ArrayList<String> circuitsId){
        ArrayList<LinkedHashMap<String, String>> eventsPerRace = new ArrayList<>();
        for (int i = 0; i < raceNames.size(); i++) {
            String raceName = raceNames.get(i);
            LinkedHashMap<String, String> eventsCountdown = new LinkedHashMap<>();
            DatabaseReference rootRef = database.getReference();
            int finalI = i;
            rootRef.child("/schedule/season/" + currentYear + "/" + raceName).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    String firstPractice = snapshot.child("FirstPractice/firstPracticeDate").getValue(String.class) +
                            " " + snapshot.child("FirstPractice/firstPracticeTime").getValue(String.class);
                    String race = snapshot.child("raceDate").getValue(String.class) +
                            " " + snapshot.child("raceTime").getValue(String.class);

                    String raceQuali = snapshot.child("Qualifying/raceQualiDate").getValue(String.class) +
                            " " + snapshot.child("Qualifying/raceQualiTime").getValue(String.class);

                    String sprintDate = snapshot.child("Sprint/sprintRaceDate").getValue(String.class);
                    if (sprintDate.equals("N/A")){
                        String secondPractice = snapshot.child("SecondPractice/secondPracticeDate").getValue(String.class) +
                                " " + snapshot.child("SecondPractice/secondPracticeTime").getValue(String.class);

                        String thirdPractice = snapshot.child("ThirdPractice/thirdPracticeDate").getValue(String.class) +
                                " " + snapshot.child("ThirdPractice/thirdPracticeTime").getValue(String.class);

                        eventsCountdown.put("first_practice_event", firstPractice);
                        eventsCountdown.put("second_practice_event", secondPractice);
                        eventsCountdown.put("third_practice_event", thirdPractice);
                        eventsCountdown.put("quali_event", raceQuali);
                        eventsCountdown.put("race_event", race);
                    }else{
                        String sprintQuali = snapshot.child("SprintQualifying/sprintQualiDate").getValue(String.class) +
                                " " + snapshot.child("SprintQualifying/sprintQualiTime").getValue(String.class);
                        String sprint = sprintDate +
                                " " + snapshot.child("Sprint/sprintRaceTime").getValue(String.class);

                        eventsCountdown.put("first_practice_event", firstPractice);
                        eventsCountdown.put("sprint_quali_event", sprintQuali);
                        eventsCountdown.put("sprint_event", sprint);
                        eventsCountdown.put("quali_event", raceQuali);
                        eventsCountdown.put("race_event", race);
                    }
                    eventsPerRace.add(eventsCountdown);
                    if(finalI == raceNames.size() - 1){
                        scheduleNotification(eventsPerRace, raceNames, currentYear, circuitsId);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    showError("getRaceSchedule error" + error.getMessage());
                    Log.e("futureActivityFirebaseError", error.getMessage());
                }
            });

        }
    }

    private void scheduleNotification(ArrayList<LinkedHashMap<String, String>> eventsPerRace, ArrayList<String> raceNames, String currentYear, ArrayList<String> circuitsId) {
        ArrayList<String> eventsStr = new ArrayList<>();
        SharedPreferences.Editor editor = mPrefs.edit();
        for (int i = 0; i < raceNames.size(); i++){
            LinkedHashMap<String, String> events = eventsPerRace.get(i);
            String raceName = raceNames.get(i);
            if (!events.isEmpty()) {

                String stringForSharedPrefs = currentYear + "$" + raceName;
                for(Map.Entry<String, String> entry : events.entrySet()){
                    String key = entry.getKey();
                    String value = entry.getValue();
                    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                    dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
                    try {
                        Date eventStart_date = dateFormat.parse(value);
                        Date endTime = new Date(eventStart_date.getTime() + HOUR);
                        String endTimeStr = dateFormat.format(endTime);
                        Date endTime_sprintQ = new Date(eventStart_date.getTime() + SPRINT_QUALI_DIFF);
                        String endTimeStr_sprintQ = dateFormat.format(endTime_sprintQ);
                        Date endTime_race = new Date(eventStart_date.getTime() + 2 * HOUR);
                        String endTimeStr_race = dateFormat.format(endTime_race);
                        String eventEndStr_date;
                        switch (key) {
                            case "race_event":
                                eventEndStr_date = endTimeStr_race + "Z";
                                break;
                            case "sprint_quali_event":
                                eventEndStr_date = endTimeStr_sprintQ + "Z";
                                break;
                            default:
                                eventEndStr_date = endTimeStr + "Z";
                                break;
                        }
                        stringForSharedPrefs += "%" + key + "$" + value + "$" + eventEndStr_date;

                    }catch(ParseException e){
                        showError(e.getMessage());
                        e.printStackTrace();
                    }
                }
                eventsStr.add(stringForSharedPrefs);
            }

        }
        JSONArray jsonArrayEvents = new JSONArray(eventsStr);
        JSONArray jsonArrayCircuits = new JSONArray(circuitsId);
        editor.putString("events_json", jsonArrayEvents.toString());
        editor.putString("circuits_json", jsonArrayCircuits.toString());
        editor.apply();

        Intent intentStart = new Intent(this, BootService.class);
        String channelId = "channelID2";
        intentStart.putExtra("channelId", channelId);
        startService(intentStart);
    }

    public static void hideShimmer(RecyclerView rv, ShimmerFrameLayout sf) {
        sf.animate()
                .setDuration(500)
                .withEndAction(() -> {
                    rv.setVisibility(View.VISIBLE);
                    sf.setVisibility(View.GONE);
                    sf.stopShimmer();
                })
                .start();
    }

    public static void showShimmer(RecyclerView recyclerView, ShimmerFrameLayout shimmer) {
        recyclerView.setVisibility(View.GONE);

        shimmer.setAlpha(1f);
        shimmer.setVisibility(View.VISIBLE);
        shimmer.startShimmer();
    }

    public static boolean checkLightTheme(Context context) {
        int currentNightMode = context.getResources().getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_MASK;
        switch (currentNightMode) {
            case Configuration.UI_MODE_NIGHT_YES:
                return false;
            case Configuration.UI_MODE_NIGHT_NO:
                return true;
            default:
                return true;
        }
    }

    public static boolean checkConnection(Context context) {
        ConnectivityManager manager = (ConnectivityManager)
                context.getSystemService(Context.CONNECTIVITY_SERVICE);

        if (manager == null) {
            return false;
        }

        Network network = manager.getActiveNetwork();

        if (network == null) {
            return false;
        }

        NetworkCapabilities capabilities =
                manager.getNetworkCapabilities(network);

        if (capabilities == null) {
            return false;
        }

        return capabilities.hasCapability(
                NetworkCapabilities.NET_CAPABILITY_INTERNET
        ) && capabilities.hasCapability(
                NetworkCapabilities.NET_CAPABILITY_VALIDATED
        );
    }

    public static int getStringByName(String name) {
        int stringId = 0;

        try {
            Class<R.string> res = R.string.class;
            Field field = res.getField(name);
            stringId = field.getInt(null);
        } catch (Exception e) {
            Log.e("getStringByName", " " + e.getMessage());
        }

        return stringId;
    }
}