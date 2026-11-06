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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);
        setContentView(R.layout.public_profile_activity);

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
        String requestedScope =
                getIntent().getStringExtra(EXTRA_SCOPE);

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

        progress.setVisibility(View.VISIBLE);
        errorBox.setVisibility(View.GONE);
        content.setVisibility(View.VISIBLE);
        name.setText("");
        initial.setText("");

        stats.setVisibility(View.GONE);
        ratingMessage.setVisibility(View.GONE);

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

        Task<DataSnapshot> ratingTask = db
                .getReference("leaderboard")
                .child(ratingScope)
                .child(profileUid)
                .get();

        Tasks.whenAllComplete(profileTask, ratingTask)
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

                    if (!ratingTask.isSuccessful()) {
                        Log.e(
                                "PublicProfile",
                                "Failed to load rating",
                                ratingTask.getException()
                        );

                        showError(
                                R.string.public_profile_rating_error,
                                true
                        );
                    } else {
                        showRating(ratingTask.getResult());
                    }

                    loadAvatar(request);
                });
    }

    private void showRating(DataSnapshot snapshot) {
        if (snapshot == null || !snapshot.exists()) {
            stats.setVisibility(View.GONE);
            ratingMessage.setText(
                    R.string.public_profile_not_ranked
            );
            ratingMessage.setVisibility(View.VISIBLE);
            return;
        }

        double rankValue = numeric(
                snapshot.child("rank").getValue()
        );

        double pointsValue = numeric(
                snapshot.child("totalPoints").getValue()
        );

        double correctValue = numeric(
                snapshot.child("correctPredictions").getValue()
        );

        if (!Double.isFinite(rankValue)
                || rankValue < 1
                || rankValue != Math.rint(rankValue)
                || !Double.isFinite(pointsValue)
                || pointsValue < 0
                || !Double.isFinite(correctValue)
                || correctValue < 0
                || correctValue != Math.rint(correctValue)) {

            showError(
                    R.string.public_profile_rating_error,
                    true
            );
            return;
        }

        NumberFormat integerFormat =
                NumberFormat.getIntegerInstance();

        NumberFormat pointsFormat =
                NumberFormat.getNumberInstance();

        pointsFormat.setMaximumFractionDigits(2);

        rank.setText(integerFormat.format(rankValue));
        points.setText(pointsFormat.format(pointsValue));
        correct.setText(integerFormat.format(correctValue));

        ratingMessage.setVisibility(View.GONE);
        stats.setVisibility(View.VISIBLE);
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

                    GlideApp.with(avatar)
                            .load(uri)
                            .centerCrop()
                            .listener(new RequestListener<Drawable>() {
                                @Override
                                public boolean onLoadFailed(
                                        GlideException exception,
                                        Object model,
                                        Target<Drawable> target,
                                        boolean isFirstResource
                                ) {
                                    if (!active(request)) return true;

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
                    if (!active(request)) return;

                    avatar.setVisibility(View.GONE);
                    initial.setVisibility(View.VISIBLE);
                });
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