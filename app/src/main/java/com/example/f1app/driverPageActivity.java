package com.example.f1app;

import static com.example.f1app.MainActivity.checkConnection;
import static com.example.f1app.MainActivity.checkLightTheme;
import static com.example.f1app.teamPageActivity.deleteChoice;
import static com.example.f1app.teamPageActivity.isFavouriteValue;
import static com.example.f1app.teamPageActivity.saveChoice;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class driverPageActivity extends AppCompatActivity {

    private static final int EARLIEST_AVAILABLE_SEASON = 2018;

    private TextView teamName;
    private TextView driverNumber;
    private TextView driverFullName;
    private TextView driverFamilyName;
    private TextView driverName;

    private ProgressBar progressBar;
    private CoordinatorLayout contentLayout;

    private ImageView driverImage;
    private ToggleButton likeButton;

    private String currentDriverFullName;

    private TextView driverStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);
        setContentView(R.layout.driver_page);

        setupDriverInsets();

        contentLayout = findViewById(R.id.content_layout);
        progressBar = findViewById(R.id.progressBar);

        driverFullName = findViewById(R.id.driverfullName);
        driverFamilyName = findViewById(R.id.driverFamilyName);
        driverImage = findViewById(R.id.driver_image);
        driverNumber = findViewById(R.id.driverNumber);
        driverName = findViewById(R.id.driverName);
        teamName = findViewById(R.id.teamName);
        likeButton = findViewById(R.id.like_button);
        driverStatus = findViewById(R.id.driverStatus);

        ImageButton backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> finishAfterTransition());

        WindowInsetsControllerCompat windowInsetsController =
                WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        windowInsetsController.setAppearanceLightStatusBars(false);

        if (!checkConnection(getApplicationContext())){
            startActivity(connectionLostScreen.createShowSplashOnNetworkFailure(driverPageActivity.this));
        }else{
            startActivity(connectionLostScreen.createIntentHideSplashOnNetworkRecovery(driverPageActivity.this));
        }

        Bundle bundle = getIntent().getExtras();

        if (bundle == null) {
            finish();
            return;
        }

        String mDriverName = bundle.getString("driverName");
        String mDriverFamilyName = bundle.getString("driverFamilyName");
        String mDriverCode = bundle.getString("driverCode");
        String mCurrentSeason = bundle.getString("currentSeason");

        if (mDriverName == null || mDriverFamilyName == null ||
                mDriverCode == null || mCurrentSeason == null) {
            finish();
            return;
        }

        currentDriverFullName = mDriverName + " " + mDriverFamilyName;
        setupFavouriteButton(currentDriverFullName);

        loadDriverData(
                currentDriverFullName,
                mDriverName,
                mDriverFamilyName,
                mDriverCode,
                mCurrentSeason
        );

        setupToolbar(currentDriverFullName);
    }

    private void setupDriverInsets() {
        View root = findViewById(R.id.main_content);
        View appBar = findViewById(R.id.appbar);

        final int rootLeft = root.getPaddingLeft();
        final int rootTop = root.getPaddingTop();
        final int rootRight = root.getPaddingRight();
        final int rootBottom = root.getPaddingBottom();

        final int appBarLeft = appBar.getPaddingLeft();
        final int appBarTop = appBar.getPaddingTop();
        final int appBarRight = appBar.getPaddingRight();
        final int appBarBottom = appBar.getPaddingBottom();

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
            appBar.setPadding(
                    appBarLeft,
                    appBarTop + safeInsets.top,
                    appBarRight,
                    appBarBottom
            );

            return windowInsets;
        });

        ViewCompat.requestApplyInsets(root);
    }

    private void loadDriverData(
            String driverFullName,
            String mDriverName,
            String mDriverFamilyName,
            String mDriverCode,
            String mCurrentSeason) {

        FirebaseDatabase.getInstance()
                .getReference()
                .child("drivers")
                .child(driverFullName)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (isFinishing() || isDestroyed()) {
                            return;
                        }

                        if (!snapshot.exists()) {
                            showError(
                                    getString(
                                            R.string.user_driver_error
                                    )
                            );

                            finish();
                            return;
                        }

                        String driverTeam =
                                snapshot.child("driversTeam")
                                        .getValue(String.class);

                        String driverNumberText = snapshot.child("permanentNumber")
                                .getValue(String.class);

                        displayDriverNumber(driverNumberText);

                        String lastEntry =
                                snapshot.child("lastEntry")
                                        .getValue(String.class);

                        String status =
                                snapshot.child("status")
                                        .getValue(String.class);

                        String driverId = snapshot.child("driverId")
                                .getValue(String.class);

                        if (driverTeam == null
                                || lastEntry == null
                                || status == null) {
                            showError(
                                    getString(
                                            R.string.user_driver_error
                                    )
                            );

                            return;
                        }

                        if (driverTeam.equals("Sauber") || driverTeam.equals("Kick Sauber")) {
                            driverTeam = "Audi";
                        }

                        int latestSeason = extractYear(lastEntry);

                        if (latestSeason == -1) {
                            Log.e(
                                    "DriverPage",
                                    "Invalid lastEntry: " + lastEntry
                            );

                            try {
                                latestSeason = Integer.parseInt(mCurrentSeason);
                            } catch (NumberFormatException error) {
                                showError(
                                        getString(
                                                R.string.user_driver_error
                                        )
                                );

                                return;
                            }
                        }

                        driverFullNameView()
                                .setText(mDriverName);

                        driverFamilyName.setText(mDriverFamilyName);
                        // driverNumber.setText(driverNumberText);

                        applyDriverStatus(
                                status,
                                driverTeam
                        );

                        loadDriverImage(driverId, mDriverCode, latestSeason);

                        loadConstructorAndInit(
                                driverTeam,
                                mDriverName,
                                mDriverFamilyName,
                                mDriverCode,
                                mCurrentSeason,
                                latestSeason
                        );
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                        if (isFinishing() || isDestroyed()) {
                            return;
                        }

                        Log.e(
                                "DriverPage",
                                "Failed to load driver",
                                error.toException()
                        );

                        showError(
                                getString(R.string.user_driver_error)
                        );
                    }
                });
    }

    private TextView driverFullNameView() {
        return driverFullName;
    }

    private int extractYear(String lastEntry) {
        if (lastEntry == null || lastEntry.trim().isEmpty()) {
            return -1;
        }

        Matcher matcher = Pattern
                .compile("\\b(\\d{4})\\b")
                .matcher(lastEntry);

        if (!matcher.find()) {
            return -1;
        }

        try {
            return Integer.parseInt(matcher.group(1));
        } catch (NumberFormatException error) {
            return -1;
        }
    }

    private void applyDriverStatus(String status, String driverTeam) {
        teamName.setText(driverTeam != null ? driverTeam : "");

        driverStatus.setText("");
        driverStatus.setVisibility(View.GONE);

        if ("reserve".equals(status)) {
            driverStatus.setText(R.string.reserve_text);

            driverStatus.setTextColor(
                    ContextCompat.getColor(
                            this,
                            R.color.light_bronze
                    )
            );

            driverStatus.setVisibility(View.VISIBLE);

        } else if ("retired".equals(status)) {
            driverStatus.setText(R.string.retired_text);

            driverStatus.setTextColor(
                    ContextCompat.getColor(
                            this,
                            R.color.red
                    )
            );

            driverStatus.setVisibility(View.VISIBLE);
        }
    }

    private void loadDriverImage(
            String driverCode,
            int latestSeason) {

        if (driverCode == null
                || driverCode.trim().isEmpty()
                || latestSeason == -1) {

            showDriverImagePlaceholder();
            return;
        }

        String imagePath =
                "drivers/"
                        + driverCode.toLowerCase()
                        + "_"
                        + latestSeason
                        + ".png";

        StorageReference imageReference =
                FirebaseStorage.getInstance()
                        .getReference()
                        .child(imagePath);

        imageReference.getDownloadUrl()
                .addOnSuccessListener(uri -> {
                    if (isFinishing() || isDestroyed()) {
                        return;
                    }

                    GlideApp.with(getApplicationContext())
                            .load(uri)
                            .transition(
                                    DrawableTransitionOptions
                                            .withCrossFade()
                            )
                            .diskCacheStrategy(
                                    DiskCacheStrategy.ALL
                            )
                            .error(R.drawable.placeholder_driver)
                            .into(driverImage);
                })
                .addOnFailureListener(error -> {
                    if (isFinishing() || isDestroyed()) {
                        return;
                    }

                    Log.w(
                            "DriverPage",
                            "Driver image not found: "
                                    + imagePath,
                            error
                    );

                    showDriverImagePlaceholder();
                });
    }

    private void loadDriverImage(
            String driverId,
            String driverCode,
            int year) {

        if (driverId != null && !driverId.trim().isEmpty()) {
            String preferredPath =
                    "drivers/" + driverId.trim() + "_" + year + ".png";

            FirebaseStorage.getInstance()
                    .getReference()
                    .child(preferredPath)
                    .getDownloadUrl()
                    .addOnSuccessListener(uri -> {
                        if (isFinishing() || isDestroyed()) {
                            return;
                        }

                        GlideApp.with(this)
                                .load(uri)
                                .placeholder(R.drawable.placeholder_driver)
                                .error(R.drawable.placeholder_driver)
                                .into(driverImage);
                    })
                    .addOnFailureListener(error -> {
                        if (isFinishing() || isDestroyed()) {
                            return;
                        }

                        loadDriverImage(driverCode, year);
                    });

        } else {
            loadDriverImage(driverCode, year);
        }
    }

    private void showDriverImagePlaceholder() {
        driverImage.setImageResource(
                R.drawable.placeholder_driver
        );
    }

    private void loadConstructorAndInit(
            String driverTeam,
            String mDriverName,
            String mDriverFamilyName,
            String mDriverCode,
            String mCurrentSeason,
            int latestSeason) {

        FirebaseDatabase.getInstance()
                .getReference()
                .child("constructors")
                .orderByChild("name")
                .equalTo(driverTeam)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {

                        if (isFinishing() || isDestroyed()) {
                            return;
                        }

                        String driverTeamId = null;

                        for (DataSnapshot teamSnapshot :
                                snapshot.getChildren()) {

                            driverTeamId =
                                    teamSnapshot.getKey();

                            break;
                        }

                        init(
                                createDriverPageBundle(
                                        mDriverName,
                                        mDriverFamilyName,
                                        driverTeam,
                                        mDriverCode,
                                        driverTeamId,
                                        mCurrentSeason,
                                        latestSeason
                                )
                        );
                    }

                    @Override
                    public void onCancelled(
                            @NonNull DatabaseError error) {

                        if (isFinishing() || isDestroyed()) {
                            return;
                        }

                        Log.e(
                                "DriverPage",
                                "Failed to load constructor",
                                error.toException()
                        );

                        init(
                                createDriverPageBundle(
                                        mDriverName,
                                        mDriverFamilyName,
                                        driverTeam,
                                        mDriverCode,
                                        null,
                                        mCurrentSeason,
                                        latestSeason
                                )
                        );
                    }
                });
    }

    private Bundle createDriverPageBundle(
            String driverName,
            String driverFamilyName,
            String driverTeam,
            String driverCode,
            String driverTeamId,
            String currentSeason,
            int latestSeason) {

        Bundle bundle = new Bundle();

        bundle.putString("driverName", driverName);
        bundle.putString("driverFamilyName", driverFamilyName);
        bundle.putString("driverTeam", driverTeam);
        bundle.putString("driverCode", driverCode);
        bundle.putString("currentSeason", currentSeason);

        bundle.putInt("latestSeason", latestSeason);
        bundle.putInt("earliestSeason", EARLIEST_AVAILABLE_SEASON);

        if (driverTeamId != null
                && !driverTeamId.trim().isEmpty()) {

            bundle.putString("driverTeamId", driverTeamId);
        }

        return bundle;
    }

    private void setupFavouriteButton(String driverFullName) {
        FirebaseUser user =
                FirebaseAuth.getInstance().getCurrentUser();

        if (user == null) {
            likeButton.setOnClickListener(view -> {
                likeButton.setChecked(false);

                Toast.makeText(
                        driverPageActivity.this,
                        getString(
                                R.string.driver_save_error_login_text
                        ),
                        Toast.LENGTH_LONG
                ).show();
            });

            return;
        }

        isFavouriteValue("choiceDriver", driverFullName,
                likeButton, driverPageActivity.this);

        likeButton.setOnClickListener(view -> {
            if(likeButton.isChecked()){
                saveChoice("choiceDriver", driverFullName,
                        likeButton, driverPageActivity.this);
            }else{
                deleteChoice("choiceDriver", driverFullName,
                        likeButton, driverPageActivity.this);
            }
        });
    }

    private void setupToolbar(String driverFullName) {
        CollapsingToolbarLayout collapsingToolbarLayout = findViewById(R.id.collapsing_toolbar);

        if (!checkLightTheme(driverPageActivity.this)) {
            collapsingToolbarLayout.setBackground(ContextCompat.getDrawable(driverPageActivity.this, R.drawable.black_gradient));
        }

        AppBarLayout appBarLayout = findViewById(R.id.appbar);
        appBarLayout.setExpanded(true, true);
        Toolbar toolbar = findViewById(R.id.toolbar);
        appBarLayout.addOnOffsetChangedListener(new AppBarLayout.OnOffsetChangedListener() {
                    boolean isShow = true;
                    int scrollRange = -1;

                    @Override
                    public void onOffsetChanged(AppBarLayout appBarLayout, int verticalOffset) {
                        if (scrollRange == -1) {
                            scrollRange = appBarLayout.getTotalScrollRange();
                        }
                        if (scrollRange + verticalOffset == 0) {
                            driverName.setText(driverFullName);
                            if (!checkLightTheme(driverPageActivity.this)) {
                                toolbar.setBackgroundColor(
                                        ContextCompat.getColor(getApplicationContext(), R.color.darkest_grey));
                            } else {
                                toolbar.setBackgroundColor(
                                        ContextCompat.getColor(getApplicationContext(), R.color.dark_blue));
                            }
                            isShow = true;
                        } else if (isShow) {
                            driverName.setText(" ");
                            toolbar.setBackgroundColor(
                                    ContextCompat.getColor(
                                            getApplicationContext(),
                                            android.R.color.transparent
                                    )
                            );
                            isShow = false;
                        }
                    }
                }
        );
    }

    private void init(Bundle driverPageBundle) {
        ViewPager2 viewPager =
                findViewById(R.id.viewPager2);

        viewPagerAdapter adapter =
                new viewPagerAdapter(this);

        driverStatsFragment statsFragment =
                new driverStatsFragment();

        statsFragment.setArguments(driverPageBundle);
        adapter.addFragment(statsFragment);

        driverResultsFragment resultsFragment =
                new driverResultsFragment();

        resultsFragment.setArguments(driverPageBundle);
        adapter.addFragment(resultsFragment);

        viewPager.setAdapter(adapter);

        View child = viewPager.getChildAt(0);

        if (child instanceof RecyclerView) {
            child.setOverScrollMode(View.OVER_SCROLL_NEVER);
        }

        TabLayout tabLayout = findViewById(R.id.tab_layout);
        tabLayout.setTag("sticky");

        new TabLayoutMediator(
                tabLayout,
                viewPager,
                (tab, position) -> {
                    if (position == 0) {
                        tab.setText(R.string.stats_text);
                    } else {
                        tab.setText(R.string.results_text);
                    }
                }
        ).attach();

        progressBar.animate()
                .setDuration(500)
                .withEndAction(() -> {
                    if (isFinishing() || isDestroyed()) {
                        return;
                    }

                    contentLayout.setVisibility(View.VISIBLE);
                    progressBar.setVisibility(View.GONE);
                })
                .start();
    }

    private void displayDriverNumber(String number) {
        boolean hasNumber =
                number != null
                        && !number.trim().isEmpty()
                        && !"null".equalsIgnoreCase(number.trim());

        driverNumber.setText(hasNumber ? number.trim() : "");

        driverNumber.setVisibility(
                hasNumber ? View.VISIBLE : View.GONE
        );
    }

    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}