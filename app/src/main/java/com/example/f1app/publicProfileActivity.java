package com.example.f1app;

import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;

import java.text.NumberFormat;
import java.util.Locale;

import com.google.firebase.functions.FirebaseFunctions;
import com.google.firebase.storage.StorageReference;

import java.util.HashMap;
import java.util.Map;

import android.widget.LinearLayout;

import java.util.List;

public class publicProfileActivity extends AppCompatActivity {

    public static final String EXTRA_UID = "profileUid";
    public static final String EXTRA_SEASON = "currentSeason";
    public static final String EXTRA_SCOPE = "ratingScope";

    private String profileUid;
    private String ratingScope;

    private View content;
    private View cover;
    private View stats;
    private View progress;
    private View errorBox;
    private View retry;

    private ImageView avatar;

    private TextView initial;
    private TextView name;
    private TextView scopeText;
    private TextView rank;
    private TextView points;
    private TextView correct;
    private TextView ratingMessage;
    private TextView errorText;

    private int generation;

    private String assetSeason;

    private ImageView favoriteTeamImage;
    private ImageView favoriteDriverImage;

    private TextView favoriteTeamName;
    private TextView favoriteDriverName;
    private TextView favoriteDriverFamilyName;

    private TextView favoriteTeamState;
    private TextView favoriteDriverState;

    private LinearLayout rankingsContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);
        setContentView(R.layout.public_profile_activity);
        AwardsUi.attachPublic(this);
        DisplayedAwardUi.attachProfile(this, false);

        content = findViewById(R.id.main_content);
        cover = findViewById(R.id.header);
        stats = findViewById(R.id.publicProfileStats);
        progress = findViewById(R.id.loadingBar);

        errorBox = findViewById(R.id.publicProfileErrorBox);
        retry = findViewById(R.id.publicProfileRetry);

        avatar = findViewById(R.id.avatarImage);
        initial = findViewById(R.id.avatarInitial);
        name = findViewById(R.id.username);
        scopeText = findViewById(R.id.createdAt);

        rank = findViewById(R.id.prediction_count);
        points = findViewById(R.id.points_earned);
        correct = findViewById(R.id.races_saved);

        ratingMessage = findViewById(R.id.publicProfileRatingMessage);
        errorText = findViewById(R.id.publicProfileError);

        favoriteTeamImage = findViewById(R.id.teamCar);
        favoriteDriverImage = findViewById(R.id.driver_image);

        favoriteTeamName = findViewById(R.id.teamName);
        favoriteDriverName = findViewById(R.id.driverName);
        favoriteDriverFamilyName = findViewById(R.id.driverFamilyName);

        favoriteTeamState = findViewById(R.id.noTeam);
        favoriteDriverState = findViewById(R.id.noDriver);

        favoriteTeamState.setTextSize(14);
        favoriteDriverState.setTextSize(14);

        rankingsContainer = findViewById(R.id.publicProfileRankings);

        findViewById(R.id.team_layout).setClickable(false);
        findViewById(R.id.driver_layout).setClickable(false);

        findViewById(R.id.backButton)
                .setOnClickListener(v -> finish());

        content.setVisibility(View.VISIBLE);
        name.setText("");
        initial.setText("");
        stats.setVisibility(View.GONE);

        retry.setOnClickListener(v -> loadProfile());

        WindowCompat.getInsetsController(
                getWindow(),
                getWindow().getDecorView()
        ).setAppearanceLightStatusBars(false);

        setupInsets();

        profileUid = getIntent().getStringExtra(EXTRA_UID);
        String season = getIntent().getStringExtra(EXTRA_SEASON);
        String requestedScope = getIntent().getStringExtra(EXTRA_SCOPE);

        assetSeason = season != null && season.matches("[0-9]{4}")
                ? season
                : null;

        if (!validUid(profileUid)) {
            showError(R.string.public_profile_invalid, false);
            return;
        }

        if ("global".equals(requestedScope)) {
            ratingScope = "global";
            scopeText.setText(R.string.leaderboard_global);
        } else {
            String normalizedScope = "season".equals(requestedScope)
                    || requestedScope == null
                    ? season : requestedScope;

            if (normalizedScope == null
                    || !normalizedScope.matches("[0-9]{4}")) {
                showError(R.string.public_profile_invalid, false);
                return;
            }

            ratingScope = normalizedScope;

            scopeText.setText(getString(
                    R.string.public_profile_season,
                    ratingScope
            ));
        }

        loadProfile();
    }

    private boolean validUid(String uid) {
        return uid != null
                && !uid.isEmpty()
                && !uid.matches(".*[.#$\\[\\]/\\p{Cntrl}].*");
    }

    private boolean active(int request) {
        return request == generation
                && !isFinishing()
                && !isDestroyed();
    }

    private void loadProfile() {
        if (ratingScope == null || !validUid(profileUid)) return;

        final int request = ++generation;

        resetFavorites();

        rankingsContainer.removeAllViews();

        scopeText.setText(R.string.public_stats_heading);

        ratingMessage.setText(R.string.public_stats_loading);
        ratingMessage.setVisibility(View.VISIBLE);

        progress.setVisibility(View.VISIBLE);
        errorBox.setVisibility(View.GONE);
        content.setVisibility(View.VISIBLE);
        name.setText("");
        initial.setText("");

        stats.setVisibility(View.GONE);

        GlideApp.with(avatar).clear(avatar);
        avatar.setVisibility(View.GONE);
        initial.setVisibility(View.VISIBLE);

        cover.setBackgroundColor(ContextCompat.getColor(
                this,
                R.color.account_settings_header_color
        ));

        FirebaseDatabase db = FirebaseDatabase.getInstance();

        Task<DataSnapshot> profileTask = db
                .getReference("publicProfiles")
                .child(profileUid)
                .get();

        Tasks.whenAllComplete(profileTask)
                .addOnCompleteListener(done -> {
                    if (!active(request)) return;

                    progress.setVisibility(View.GONE);

                    if (!profileTask.isSuccessful()) {
                        Log.e(
                                "PublicProfile",
                                "Failed to load public profile",
                                profileTask.getException()
                        );
                        showError(
                                R.string.public_profile_load_error,
                                true
                        );
                        return;
                    }

                    DataSnapshot profile = profileTask.getResult();

                    if (profile == null || !profile.exists()) {
                        showError(
                                R.string.public_profile_unavailable,
                                false
                        );
                        return;
                    }

                    Object usernameValue =
                            profile.child("username").getValue();

                    if (!(usernameValue instanceof String)
                            || ((String) usernameValue).trim().isEmpty()) {
                        showError(
                                R.string.public_profile_unavailable,
                                false
                        );
                        return;
                    }

                    String username =
                            ((String) usernameValue).trim();

                    name.setText(username);

                    initial.setText(
                            username.substring(
                                    0,
                                    username.offsetByCodePoints(0, 1)
                            ).toUpperCase(Locale.getDefault())
                    );

                    Object colorValue =
                            profile.child("background_color").getValue();

                    if (colorValue instanceof String
                            && ((String) colorValue)
                            .matches("#[0-9A-Fa-f]{6}")) {
                        cover.setBackgroundColor(
                                Color.parseColor((String) colorValue)
                        );
                    }

                    content.setVisibility(View.VISIBLE);

                    loadPublicStatistics(request);

                    loadAvatar(request);
                    loadFavorites(request);
                });
    }

    private void loadPublicStatistics(int request) {
        Map<String, Object> data = new HashMap<>();
        data.put("profileUid", profileUid);

        FirebaseFunctions.getInstance("us-central1")
                .getHttpsCallable("getPublicProfileStatistics")
                .call(data)
                .addOnSuccessListener(result -> {
                    if (!active(request)) return;

                    try {
                        renderPublicStatistics(result.getData());
                    } catch (RuntimeException exception) {
                        Log.e(
                                "PublicProfile",
                                "Invalid public statistics response",
                                exception
                        );

                        showPublicStatisticsError();
                    }
                })
                .addOnFailureListener(exception -> {
                    if (!active(request)) return;

                    Log.e(
                            "PublicProfile",
                            "Failed to load public statistics",
                            exception
                    );

                    showPublicStatisticsError();
                });
    }

    private void showPublicStatisticsError() {
        stats.setVisibility(View.GONE);
        rankingsContainer.removeAllViews();

        ratingMessage.setText(
                R.string.public_profile_rating_error
        );
        ratingMessage.setVisibility(View.VISIBLE);

        showError(
                R.string.public_profile_rating_error,
                true
        );
    }

    private Map<?, ?> requireStatsMap(Object value) {
        if (!(value instanceof Map)) {
            throw new IllegalStateException("Expected statistics object");
        }

        return (Map<?, ?>) value;
    }

    private double requireStatNumber(Map<?, ?> data, String key) {
        Object value = data.get(key);

        if (!(value instanceof Number)) {
            throw new IllegalStateException("Missing number: " + key);
        }

        double number = ((Number) value).doubleValue();

        if (!Double.isFinite(number) || number < 0) {
            throw new IllegalStateException("Invalid number: " + key);
        }

        return number;
    }

    private long requireStatCount(Map<?, ?> data, String key) {
        double number = requireStatNumber(data, key);

        if (number != Math.rint(number) || number > Long.MAX_VALUE) {
            throw new IllegalStateException("Invalid count: " + key);
        }

        return (long) number;
    }

    private String formatStatPoints(double value) {
        NumberFormat format = NumberFormat.getNumberInstance();
        format.setMaximumFractionDigits(2);
        return format.format(value);
    }

    private void renderPublicStatistics(Object response) {
        Map<?, ?> root = requireStatsMap(response);
        Map<?, ?> totals = requireStatsMap(root.get("totals"));

        Object rankingsValue = root.get("rankings");

        if (!(rankingsValue instanceof List)) {
            throw new IllegalStateException("Missing rankings array");
        }

        NumberFormat integers = NumberFormat.getIntegerInstance();

        long totalPredictions =
                requireStatCount(totals, "predictionsCount");

        long totalCorrect =
                requireStatCount(totals, "correctPredictions");

        double earnedPoints =
                requireStatNumber(totals, "earnedPoints");

        /*
         * Сначала строим всё во временном контейнере.
         * При ошибочном ответе не оставляем половину списка.
         */
        LinearLayout temporary = new LinearLayout(this);
        temporary.setOrientation(LinearLayout.VERTICAL);

        for (Object value : (List<?>) rankingsValue) {
            Map<?, ?> entry = requireStatsMap(value);

            Object scopeValue = entry.get("scope");

            if (!(scopeValue instanceof String)) {
                throw new IllegalStateException("Missing ranking scope");
            }

            String scope = (String) scopeValue;

            if (!"global".equals(scope)
                    && !scope.matches("[0-9]{4}")) {
                throw new IllegalStateException("Invalid ranking scope");
            }

            View row = getLayoutInflater().inflate(
                    R.layout.item_public_profile_rating,
                    temporary,
                    false
            );
            TextView title =
                    row.findViewById(R.id.publicRatingTitle);

            LinearLayout primaryRow =
                    row.findViewById(R.id.publicRatingPrimaryRow);

            LinearLayout resultsRow =
                    row.findViewById(R.id.publicRatingResultsRow);

            TextView pendingView =
                    row.findViewById(R.id.publicRatingPending);

            title.setText("global".equals(scope)
                    ? getString(R.string.leaderboard_global)
                    : getString(
                    R.string.public_profile_season,
                    scope
            ));

            String rankText = "—";
            String pointsText = "—";

            if (entry.get("rank") != null) {
                long rankValue = requireStatCount(entry, "rank");

                if (rankValue < 1) {
                    throw new IllegalStateException("Invalid rank");
                }

                rankText = integers.format(rankValue);

                pointsText = formatStatPoints(
                        requireStatNumber(entry, "totalPoints")
                );
            }

            addRatingStat(
                    primaryRow,
                    rankText,
                    R.string.public_profile_rank
            );

            addRatingStat(
                    primaryRow,
                    pointsText,
                    R.string.public_profile_points
            );

            addRatingStat(
                    primaryRow,
                    integers.format(
                            requireStatCount(entry, "predictionsCount")
                    ),
                    R.string.public_stats_total_predictions
            );

            addRatingStat(
                    resultsRow,
                    integers.format(
                            requireStatCount(entry, "correctPredictions")
                    ),
                    R.string.public_stats_correct_label
            );

            addRatingStat(
                    resultsRow,
                    integers.format(
                            requireStatCount(entry, "incorrectPredictions")
                    ),
                    R.string.public_stats_incorrect_label
            );

            addRatingStat(
                    resultsRow,
                    integers.format(
                            requireStatCount(entry, "tiePredictions")
                    ),
                    R.string.public_stats_ties_label
            );

            pendingView.setText(integers.format(
                    requireStatCount(entry, "pendingPredictions")
            ));

            temporary.addView(row);
        }

        // Существующие три колонки аккаунта.
        rank.setText(integers.format(totalPredictions));
        points.setText(formatStatPoints(earnedPoints));
        correct.setText(integers.format(totalCorrect));

        rankingsContainer.removeAllViews();

        while (temporary.getChildCount() > 0) {
            View row = temporary.getChildAt(0);
            temporary.removeViewAt(0);
            rankingsContainer.addView(row);
        }

        ratingMessage.setVisibility(View.GONE);
        stats.setVisibility(View.VISIBLE);
    }

    private void addRatingStat(
            LinearLayout parent,
            String value,
            int labelRes
    ) {
        View tile = getLayoutInflater().inflate(
                R.layout.item_public_profile_stat,
                parent,
                false
        );

        TextView valueView = tile.findViewById(R.id.publicStatValue);
        TextView labelView = tile.findViewById(R.id.publicStatLabel);

        valueView.setText(value);
        labelView.setText(labelRes);

        int gap = Math.round(
                5 * getResources().getDisplayMetrics().density
        );

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        0,
                        android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
                        1f
                );

        int index = parent.getChildCount();

        params.setMarginStart(index == 0 ? 0 : gap);
        params.setMarginEnd(index == 2 ? 0 : gap);

        parent.addView(tile, params);
    }

    private void resetFavorites() {
        GlideApp.with(favoriteTeamImage).clear(favoriteTeamImage);
        GlideApp.with(favoriteDriverImage).clear(favoriteDriverImage);

        favoriteTeamImage.setVisibility(View.INVISIBLE);
        favoriteDriverImage.setVisibility(View.INVISIBLE);

        favoriteTeamName.setText("");
        favoriteDriverName.setText("");
        favoriteDriverFamilyName.setText("");

        favoriteTeamState.setText(R.string.public_favorite_loading);
        favoriteDriverState.setText(R.string.public_favorite_loading);

        favoriteTeamState.setVisibility(View.VISIBLE);
        favoriteDriverState.setVisibility(View.VISIBLE);
    }

    private double numeric(Object value) {
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }

        if (value instanceof String) {
            try {
                return Double.parseDouble((String) value);
            } catch (NumberFormatException ignored) {
                return Double.NaN;
            }
        }

        return Double.NaN;
    }

    private void loadAvatar(int request) {
        FirebaseStorage.getInstance().getReference()
                .child("profileImages")
                .child(profileUid)
                .child("avatar.jpg")
                .getDownloadUrl()
                .addOnSuccessListener(uri -> {
                    if (!active(request)) return;

                    avatar.setVisibility(View.VISIBLE);
                    initial.setVisibility(View.VISIBLE);

                    GlideApp.with(avatar)
                            .load(uri)
                            .override(
                                    Math.round(100 * getResources().getDisplayMetrics().density),
                                    Math.round(100 * getResources().getDisplayMetrics().density)
                            )
                            .listener(new RequestListener<Drawable>() {
                                @Override
                                public boolean onLoadFailed(
                                        GlideException exception,
                                        Object model,
                                        Target<Drawable> target,
                                        boolean isFirstResource
                                ) {
                                    if (!active(request)) return true;

                                    Log.w(
                                            "PublicProfile",
                                            "Avatar image load failed; uid=" + profileUid,
                                            exception
                                    );

                                    avatar.setVisibility(View.GONE);
                                    initial.setVisibility(View.VISIBLE);
                                    return false;
                                }

                                @Override
                                public boolean onResourceReady(
                                        Drawable resource,
                                        Object model,
                                        Target<Drawable> target,
                                        DataSource dataSource,
                                        boolean isFirstResource
                                ) {
                                    if (!active(request)) return true;

                                    avatar.setVisibility(View.VISIBLE);
                                    initial.setVisibility(View.GONE);
                                    return false;
                                }
                            })
                            .into(avatar);
                })
                .addOnFailureListener(exception -> {
                    Log.w(
                            "PublicProfile",
                            "Avatar URL unavailable; uid=" + profileUid,
                            exception
                    );

                    if (!active(request)) return;

                    avatar.setVisibility(View.GONE);
                    initial.setVisibility(View.VISIBLE);
                });
    }

    private String favoriteValue(Object value) {
        if (!(value instanceof String)) return null;

        String text = ((String) value).trim();

        return text.isEmpty() || "null".equalsIgnoreCase(text)
                ? null
                : text;
    }

    private void loadFavorites(int request) {
        Map<String, Object> data = new HashMap<>();
        data.put("profileUid", profileUid);

        FirebaseFunctions.getInstance("us-central1")
                .getHttpsCallable("getPublicProfileFavorites")
                .call(data)
                .addOnSuccessListener(result -> {
                    if (!active(request)) return;

                    if (!(result.getData() instanceof Map)) {
                        showFavoritesError();
                        return;
                    }

                    Map<?, ?> favorites = (Map<?, ?>) result.getData();

                    if (!favorites.containsKey("choiceTeam")
                            || !favorites.containsKey("choiceDriver")) {
                        showFavoritesError();
                        return;
                    }

                    String team = favoriteValue(
                            favorites.get("choiceTeam")
                    );

                    String driver = favoriteValue(
                            favorites.get("choiceDriver")
                    );

                    if (team == null) {
                        favoriteTeamName.setText(
                                R.string.public_favorite_not_selected
                        );
                        favoriteTeamState.setText(
                                R.string.public_favorite_team_empty
                        );
                        favoriteTeamState.setVisibility(View.VISIBLE);
                    } else {
                        favoriteTeamName.setText(team);
                        loadFavoriteTeam(team, request);
                    }

                    if (driver == null) {
                        favoriteDriverName.setText(
                                R.string.public_favorite_not_selected
                        );
                        favoriteDriverFamilyName.setText("");
                        favoriteDriverState.setText(
                                R.string.public_favorite_driver_empty
                        );
                        favoriteDriverState.setVisibility(View.VISIBLE);
                    } else {
                        // Не пытаемся угадать структуру составного имени.
                        favoriteDriverName.setText(driver);
                        favoriteDriverFamilyName.setText("");
                        loadFavoriteDriver(driver, request);
                    }
                })
                .addOnFailureListener(exception -> {
                    if (!active(request)) return;

                    Log.e(
                            "PublicProfile",
                            "Failed to load public favorites",
                            exception
                    );

                    showFavoritesError();
                });
    }

    private void showFavoritesError() {
        favoriteTeamImage.setVisibility(View.INVISIBLE);
        favoriteDriverImage.setVisibility(View.INVISIBLE);

        favoriteTeamState.setText(
                R.string.public_favorite_load_error
        );
        favoriteDriverState.setText(
                R.string.public_favorite_load_error
        );

        favoriteTeamState.setVisibility(View.VISIBLE);
        favoriteDriverState.setVisibility(View.VISIBLE);

        // Используем уже существующую общую кнопку повтора.
        showError(R.string.public_favorites_error, true);
    }

    private void loadFavoriteTeam(String team, int request) {
        FirebaseDatabase.getInstance()
                .getReference("constructors")
                .orderByChild("name")
                .equalTo(team)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (!active(request)) return;

                    String teamId = null;

                    for (DataSnapshot child : snapshot.getChildren()) {
                        Object value = child.child("constructorId").getValue();

                        if (value instanceof String
                                && !((String) value).trim().isEmpty()) {
                            teamId = ((String) value).trim();
                            break;
                        }
                    }

                    if (teamId == null || assetSeason == null) {
                        showFavoriteImageUnavailable(
                                favoriteTeamImage,
                                favoriteTeamState
                        );
                        return;
                    }

                    StorageReference image = FirebaseStorage.getInstance()
                            .getReference()
                            .child("teams")
                            .child(
                                    teamId.trim().toLowerCase(Locale.ROOT)
                                            + "_" + assetSeason + ".png"
                            );

                    loadFavoriteImage(
                            image,
                            null,
                            favoriteTeamImage,
                            favoriteTeamState,
                            request
                    );
                })
                .addOnFailureListener(exception -> {
                    if (!active(request)) return;

                    Log.w(
                            "PublicProfile",
                            "Favorite team lookup failed",
                            exception
                    );

                    showFavoriteImageUnavailable(
                            favoriteTeamImage,
                            favoriteTeamState
                    );
                });
    }

    private void loadFavoriteDriver(String driver, int request) {
        FirebaseDatabase.getInstance()
                .getReference("drivers")
                .child(driver)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (!active(request)) return;

                    String driverId = favoriteValue(
                            snapshot.child("driverId").getValue()
                    );

                    String code = favoriteValue(
                            snapshot.child("driversCode").getValue()
                    );

                    String lastEntry = favoriteValue(
                            snapshot.child("lastEntry").getValue()
                    );

                    String year = null;

                    if (lastEntry != null) {
                        java.util.regex.Matcher matcher =
                                java.util.regex.Pattern
                                        .compile("[0-9]{4}")
                                        .matcher(lastEntry);

                        if (matcher.find()) {
                            year = matcher.group();
                        }
                    }

                    if (year == null || (driverId == null && code == null)) {
                        showFavoriteImageUnavailable(
                                favoriteDriverImage,
                                favoriteDriverState
                        );
                        return;
                    }

                    StorageReference drivers = FirebaseStorage.getInstance()
                            .getReference()
                            .child("drivers");

                    StorageReference fallback = code == null
                            ? null
                            : drivers.child(
                            code.trim().toLowerCase(Locale.ROOT)
                                    + "_" + year + ".png"
                    );

                    StorageReference preferred = driverId == null
                            ? fallback
                            : drivers.child(
                            driverId.trim() + "_" + year + ".png"
                    );

                    loadFavoriteImage(
                            preferred,
                            driverId == null ? null : fallback,
                            favoriteDriverImage,
                            favoriteDriverState,
                            request
                    );
                })
                .addOnFailureListener(exception -> {
                    if (!active(request)) return;

                    Log.w(
                            "PublicProfile",
                            "Favorite driver lookup failed",
                            exception
                    );

                    showFavoriteImageUnavailable(
                            favoriteDriverImage,
                            favoriteDriverState
                    );
                });
    }

    private void loadFavoriteImage(
            StorageReference source,
            StorageReference fallback,
            ImageView target,
            TextView state,
            int request
    ) {
        if (!active(request)) return;

        if (source == null) {
            showFavoriteImageUnavailable(target, state);
            return;
        }

        Log.d(
                "PublicProfile",
                "Loading favorite image: " + source.getPath()
        );

        source.getDownloadUrl()
                .addOnSuccessListener(uri -> {
                    if (!active(request)) return;

                    target.setVisibility(View.VISIBLE);

                    GlideApp.with(target)
                            .load(uri)
                            .fitCenter()
                            .listener(new RequestListener<Drawable>() {
                                @Override
                                public boolean onLoadFailed(
                                        GlideException exception,
                                        Object model,
                                        Target<Drawable> glideTarget,
                                        boolean isFirstResource
                                ) {
                                    if (!active(request)) return true;

                                    Log.w(
                                            "PublicProfile",
                                            "Favorite image decode failed",
                                            exception
                                    );

                                    showFavoriteImageUnavailable(target, state);
                                    return false;
                                }

                                @Override
                                public boolean onResourceReady(
                                        Drawable resource,
                                        Object model,
                                        Target<Drawable> glideTarget,
                                        DataSource dataSource,
                                        boolean isFirstResource
                                ) {
                                    if (!active(request)) return true;

                                    state.setVisibility(View.GONE);
                                    target.setVisibility(View.VISIBLE);
                                    return false;
                                }
                            })
                            .into(target);
                })
                .addOnFailureListener(exception -> {
                    if (!active(request)) return;

                    if (fallback != null) {
                        loadFavoriteImage(
                                fallback,
                                null,
                                target,
                                state,
                                request
                        );
                        return;
                    }

                    Log.w(
                            "PublicProfile",
                            "Favorite image URL unavailable",
                            exception
                    );

                    showFavoriteImageUnavailable(target, state);
                });
    }

    private void showFavoriteImageUnavailable(
            ImageView image,
            TextView state
    ) {
        image.setVisibility(View.INVISIBLE);
        state.setText(R.string.public_favorite_image_unavailable);
        state.setVisibility(View.VISIBLE);
    }

    private void showError(int message, boolean canRetry) {
        progress.setVisibility(View.GONE);
        errorText.setText(message);
        retry.setVisibility(canRetry ? View.VISIBLE : View.GONE);
        errorBox.setVisibility(View.VISIBLE);
    }

    private void setupInsets() {
        View root = findViewById(R.id.account_root);
        View headerContainer =
                findViewById(R.id.account_header_container);
        View header = findViewById(R.id.header);
        View scroll = findViewById(R.id.nestedScrollView);

        final int rootLeft = root.getPaddingLeft();
        final int rootTop = root.getPaddingTop();
        final int rootRight = root.getPaddingRight();
        final int rootBottom = root.getPaddingBottom();

        final int headerLeft = header.getPaddingLeft();
        final int headerTop = header.getPaddingTop();
        final int headerRight = header.getPaddingRight();
        final int headerBottom = header.getPaddingBottom();

        final int baseContainerHeight =
                headerContainer.getLayoutParams().height;
        final int baseHeaderHeight =
                header.getLayoutParams().height;

        final int scrollLeft = scroll.getPaddingLeft();
        final int scrollTop = scroll.getPaddingTop();
        final int scrollRight = scroll.getPaddingRight();
        final int scrollBottom = scroll.getPaddingBottom();

        ViewCompat.setOnApplyWindowInsetsListener(
                root,
                (view, insets) -> {
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

                    android.view.ViewGroup.LayoutParams containerParams =
                            headerContainer.getLayoutParams();

                    int containerHeight =
                            baseContainerHeight + safe.top;

                    if (containerParams.height != containerHeight) {
                        containerParams.height = containerHeight;
                        headerContainer.setLayoutParams(containerParams);
                    }

                    android.view.ViewGroup.LayoutParams headerParams =
                            header.getLayoutParams();

                    int headerHeight = baseHeaderHeight + safe.top;

                    if (headerParams.height != headerHeight) {
                        headerParams.height = headerHeight;
                        header.setLayoutParams(headerParams);
                    }

                    scroll.setPadding(
                            scrollLeft,
                            scrollTop,
                            scrollRight,
                            scrollBottom + safe.bottom
                    );

                    return insets;
                }
        );

        ViewCompat.requestApplyInsets(root);
    }

    @Override
    protected void onDestroy() {
        generation++;
        super.onDestroy();
    }
}