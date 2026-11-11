package com.example.f1app;

import static com.example.f1app.MainActivity.checkConnection;
import static com.example.f1app.MainActivity.checkLightTheme;
import static com.example.f1app.driversStandingsActivity.startActivity_seasonData;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.appcompat.widget.TooltipCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.facebook.shimmer.ShimmerFrameLayout;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.FirebaseDatabase;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class savedRacesActivity extends AppCompatActivity {

    private static final String TAG = "savedRacesPage";
    private static final long REBIND_AFTER_MS = 60_000L;

    private RecyclerView recyclerView;
    private GridLayoutManager layoutManager;
    private savedRacesAdapter adapter;
    private ImageButton viewModeButton;
    private SwipeRefreshLayout swipeLayout;
    private ShimmerFrameLayout shimmerFrameLayout;
    private View skeletonRows;
    private View skeletonTiles;
    private View emptyScroll;
    private View stateContainer;
    private TextView stateMessage;
    private Button stateRetry;
    private Dialog deleteRaceDialog;

    private final List<savedRaceCard> races = new ArrayList<>();
    private savedRacesAdapter.ViewMode viewMode = savedRacesAdapter.ViewMode.LIST;

    private String uid;
    private int cacheToken = 0;
    private int requestToken = 0;
    private boolean hasData = false;
    private boolean loading = false;
    private long lastRenderAt = 0L;
    private long lastCacheSavedAt = 0L;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.saved_races);
        setupSavedRacesInsets();

        if (!checkConnection(getApplicationContext())) {
            startActivity(connectionLostScreen
                    .createShowSplashOnNetworkFailure(savedRacesActivity.this));
        } else {
            startActivity(connectionLostScreen
                    .createIntentHideSplashOnNetworkRecovery(savedRacesActivity.this));
        }

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user == null) {
            openLogin();
            return;
        }

        uid = user.getUid();
        viewMode = loadViewMode();

        shimmerFrameLayout = findViewById(R.id.shimmer_layout);
        skeletonRows = findViewById(R.id.skeleton_rows);
        skeletonTiles = findViewById(R.id.skeleton_tiles);
        emptyScroll = findViewById(R.id.emptySavedRaceScroll);
        stateContainer = findViewById(R.id.state_container);
        stateMessage = findViewById(R.id.state_message);
        stateRetry = findViewById(R.id.state_retry);
        viewModeButton = findViewById(R.id.view_mode_button);

        recyclerView = findViewById(R.id.recyclerview_savedRaces);
        recyclerView.setOverScrollMode(View.OVER_SCROLL_NEVER);

        layoutManager = new GridLayoutManager(this, spanCountFor(viewMode));
        recyclerView.setLayoutManager(layoutManager);

        adapter = new savedRacesAdapter(new savedRacesAdapter.Listener() {
            @Override
            public void onOpen(savedRaceCard race) {
                openRace(race);
            }

            @Override
            public void onDelete(savedRaceCard race) {
                showDeleteRaceDialog(race);
            }
        });
        recyclerView.setAdapter(adapter);

        swipeLayout = findViewById(R.id.swipe_layout);

        swipeLayout.setOnChildScrollUpCallback((parent, child) -> {
            if (emptyScroll.getVisibility() == View.VISIBLE) {
                return emptyScroll.canScrollVertically(-1);
            }

            return recyclerView.getVisibility() == View.VISIBLE
                    && recyclerView.canScrollVertically(-1);
        });

        swipeLayout.setOnRefreshListener(() -> refresh(true));
        stateRetry.setOnClickListener(v -> refresh(true));

        ShapeableImageView stripedImage = findViewById(R.id.stripedImage);

        if (!checkLightTheme(savedRacesActivity.this)) {
            stripedImage.setBackground(
                    ContextCompat.getDrawable(
                            savedRacesActivity.this,
                            R.drawable.background_striped_lines_item_night
                    )
            );
        }

        WindowInsetsControllerCompat windowInsetsController =
                WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        windowInsetsController.setAppearanceLightStatusBars(false);

        ImageButton backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> finish());

        viewModeButton.setOnClickListener(v -> {
            viewMode = viewMode == savedRacesAdapter.ViewMode.LIST
                    ? savedRacesAdapter.ViewMode.TILES
                    : savedRacesAdapter.ViewMode.LIST;

            // The choice belongs to this screen only; other screens keep their own.
            viewModeStore.setName(this, viewModeStore.SCREEN_SAVED_RACES, viewMode.name());
            applyViewMode(true);
        });

        setupDeleteSwipe();
        applyViewMode(false);
        loadScreen();
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Countdowns and race states change with time, so draw the cards again after a pause.
        if (hasData && adapter != null
                && System.currentTimeMillis() - lastRenderAt > REBIND_AFTER_MS) {
            renderList(false);
        }
    }

    @Override
    protected void onDestroy() {
        cacheToken++;
        requestToken++;

        if (deleteRaceDialog != null && deleteRaceDialog.isShowing()) {
            deleteRaceDialog.dismiss();
        }

        if (shimmerFrameLayout != null) {
            shimmerFrameLayout.stopShimmer();
        }

        super.onDestroy();
    }

    // ---------- insets ----------

    private void setupSavedRacesInsets() {
        final View root = findViewById(R.id.saved_races_root);
        final View header = findViewById(R.id.header);
        final View stage = findViewById(R.id.saved_stage);
        final RecyclerView list = findViewById(R.id.recyclerview_savedRaces);
        final View empty = findViewById(R.id.emptySavedRaceScroll);

        final int headerLeft = header.getPaddingLeft();
        final int headerTop = header.getPaddingTop();
        final int headerRight = header.getPaddingRight();
        final int headerBottom = header.getPaddingBottom();

        final int stageLeft = stage.getPaddingLeft();
        final int stageTop = stage.getPaddingTop();
        final int stageRight = stage.getPaddingRight();
        final int stageBottom = stage.getPaddingBottom();

        final int listBottom = list.getPaddingBottom();
        final int emptyBottom = empty.getPaddingBottom();

        ViewCompat.setOnApplyWindowInsetsListener(root, (view, windowInsets) -> {
            Insets safeInsets = windowInsets.getInsets(
                    WindowInsetsCompat.Type.systemBars()
                            | WindowInsetsCompat.Type.displayCutout()
            );

            // The header background stays full width; its content avoids the status bar and cutouts.
            header.setPadding(
                    headerLeft + safeInsets.left,
                    headerTop + safeInsets.top,
                    headerRight + safeInsets.right,
                    headerBottom
            );

            stage.setPadding(
                    stageLeft + safeInsets.left,
                    stageTop,
                    stageRight + safeInsets.right,
                    stageBottom
            );

            // Cards scroll under the navigation bar and can be scrolled clear of it.
            list.setPadding(
                    list.getPaddingLeft(),
                    list.getPaddingTop(),
                    list.getPaddingRight(),
                    listBottom + safeInsets.bottom
            );

            empty.setPadding(
                    empty.getPaddingLeft(),
                    empty.getPaddingTop(),
                    empty.getPaddingRight(),
                    emptyBottom + safeInsets.bottom
            );

            return windowInsets;
        });

        ViewCompat.requestApplyInsets(root);
    }

    // ---------- tiles / list ----------

    private savedRacesAdapter.ViewMode loadViewMode() {
        String stored = viewModeStore.getName(
                this,
                viewModeStore.SCREEN_SAVED_RACES,
                savedRacesAdapter.ViewMode.LIST.name()
        );

        try {
            return savedRacesAdapter.ViewMode.valueOf(stored);
        } catch (IllegalArgumentException error) {
            return savedRacesAdapter.ViewMode.LIST;
        }
    }

    private int spanCountFor(savedRacesAdapter.ViewMode mode) {
        int widthDp = getResources().getConfiguration().screenWidthDp;

        if (mode == savedRacesAdapter.ViewMode.TILES) {
            return Math.max(2, Math.min(4, widthDp / 170));
        }

        return widthDp >= 700 ? 2 : 1;
    }

    private void applyViewMode(boolean keepPosition) {
        int firstVisible = keepPosition
                ? layoutManager.findFirstVisibleItemPosition()
                : RecyclerView.NO_POSITION;

        boolean tiles = viewMode == savedRacesAdapter.ViewMode.TILES;

        layoutManager.setSpanCount(spanCountFor(viewMode));
        adapter.setViewMode(viewMode);

        skeletonRows.setVisibility(tiles ? View.GONE : View.VISIBLE);
        skeletonTiles.setVisibility(tiles ? View.VISIBLE : View.GONE);

        // The button shows the view that a tap will switch to.
        String description = getString(tiles
                ? R.string.saved_races_view_as_list
                : R.string.saved_races_view_as_tiles);

        viewModeButton.setImageResource(tiles
                ? R.drawable.ic_view_mode_list
                : R.drawable.ic_view_mode_tiles);
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

    private boolean accountChanged() {
        FirebaseUser current = FirebaseAuth.getInstance().getCurrentUser();
        return current == null || !current.getUid().equals(uid);
    }

    private void openLogin() {
        startActivity_seasonData(savedRacesActivity.this, logInPageActivity.class, null);
        finish();
    }

    private void loadScreen() {
        showSkeleton();

        final int token = ++cacheToken;

        savedRacesRepository.readCache(getApplicationContext(), uid, cached -> {
            if (isUnavailable() || token != cacheToken || accountChanged()) {
                return;
            }

            if (cached != null) {
                if (!hasData) {
                    lastCacheSavedAt = cached.savedAt;
                    showRaces(cached.races);
                }

                if (!savedRacesRepository.isFresh(cached, System.currentTimeMillis())) {
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

        if (accountChanged()) {
            swipeLayout.setRefreshing(false);
            openLogin();
            return;
        }

        loading = true;
        final int token = ++requestToken;
        final String requestedUid = uid;

        if (hasData) {
            swipeLayout.setRefreshing(true);
        } else {
            showSkeleton();
        }

        savedRacesRepository.fetch(
                requestedUid,
                result -> {
                    if (isUnavailable() || token != requestToken || accountChanged()) {
                        return;
                    }

                    loading = false;
                    swipeLayout.setRefreshing(false);

                    if (result.complete) {
                        lastCacheSavedAt = result.savedAt;
                        savedRacesRepository.writeCache(getApplicationContext(), requestedUid, result);
                        showRaces(result.races);
                    } else if (hasData) {
                        // Keep the complete data that is already on screen.
                        showMessage(getString(R.string.saved_races_refresh_failed));
                    } else {
                        // Better a card without circuit facts than an empty screen; it is not cached.
                        showRaces(result.races);
                    }
                },
                error -> {
                    if (isUnavailable() || token != requestToken || accountChanged()) {
                        return;
                    }

                    loading = false;
                    swipeLayout.setRefreshing(false);
                    Log.e(TAG, "Failed to load saved races", error);

                    if (hasData) {
                        showMessage(getString(R.string.saved_races_refresh_failed));
                    } else {
                        showLoadError();
                    }
                }
        );
    }

    // ---------- screen states ----------

    private void showSkeleton() {
        recyclerView.setVisibility(View.GONE);
        emptyScroll.setVisibility(View.GONE);
        stateContainer.setVisibility(View.GONE);
        shimmerFrameLayout.setVisibility(View.VISIBLE);
        shimmerFrameLayout.startShimmer();
    }

    private void showLoadError() {
        shimmerFrameLayout.stopShimmer();
        shimmerFrameLayout.setVisibility(View.GONE);
        recyclerView.setVisibility(View.GONE);
        emptyScroll.setVisibility(View.GONE);
        stateMessage.setText(R.string.saved_races_load_error);
        stateContainer.setVisibility(View.VISIBLE);
    }

    private void showRaces(List<savedRaceCard> list) {
        races.clear();
        races.addAll(list);
        hasData = true;

        shimmerFrameLayout.stopShimmer();
        shimmerFrameLayout.setVisibility(View.GONE);
        stateContainer.setVisibility(View.GONE);

        renderList(true);
    }

    private void renderList(boolean afterDataChange) {
        List<savedRaceCard> sorted = new ArrayList<>(races);
        savedRacesRepository.sortForDisplay(sorted, System.currentTimeMillis());
        lastRenderAt = System.currentTimeMillis();

        if (sorted.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            emptyScroll.setVisibility(View.VISIBLE);
            adapter.submitList(sorted);
            return;
        }

        emptyScroll.setVisibility(View.GONE);
        recyclerView.setVisibility(View.VISIBLE);

        if (afterDataChange) {
            adapter.submitList(sorted);
        } else {
            // Same cards, new time: countdowns and states have to be drawn again once the list is committed.
            adapter.submitList(sorted, () -> adapter.notifyItemRangeChanged(0, sorted.size()));
        }
    }

    private void showMessage(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    // ---------- opening a race ----------

    private void openRace(savedRaceCard race) {
        LocalDate start = race.weekendStartDate();
        LocalDate end = race.raceLocalDate();

        if (!race.scheduleKnown || start == null || end == null) {
            showMessage(getString(R.string.smth_wrong_text));
            return;
        }

        DateTimeFormatter dayFormat = DateTimeFormatter.ofPattern("dd");
        DateTimeFormatter monthFormat = DateTimeFormatter.ofPattern("MMM");
        String dayStart = start.format(dayFormat);
        String dayEnd = end.format(dayFormat);
        String monthStart = start.format(monthFormat);
        String monthEnd = end.format(monthFormat);
        String dateStart = race.firstPracticeDate != null ? race.firstPracticeDate : race.raceDate;

        Bundle bundle = new Bundle();
        bundle.putString("raceName", race.raceName);
        bundle.putString("roundCount", String.valueOf(race.round));
        bundle.putString("raceCountry", race.country);
        bundle.putString("circuitName", race.circuitName);
        bundle.putString("dateStart", dateStart);
        bundle.putString("dateEnd", race.raceDate);
        bundle.putBoolean("isCanceled", race.canceled);

        Intent intent;

        // The old check compared against "N/C", but the database stores "N/A" for races without results,
        // so every upcoming race opened the finished-race screen. Now the schedule's own rule decides.
        if (race.stateAt(System.currentTimeMillis()) == raceWeekendStatus.State.CONCLUDED) {
            intent = new Intent(this, concludedRaceActivity.class);
            bundle.putString("raceStartDay", dayStart);
            bundle.putString("raceEndDay", dayEnd);
            bundle.putString("raceStartMonth", monthStart);
            bundle.putString("raceEndMonth", monthEnd);
            bundle.putString("firstPlaceCode", race.winnerCode);
            bundle.putString("secondPlaceCode", race.secondCode);
            bundle.putString("thirdPlaceCode", race.thirdCode);
        } else {
            intent = new Intent(this, futureRaceActivity.class);
            bundle.putString("futureRaceStartDay", dayStart);
            bundle.putString("futureRaceEndDay", dayEnd);
            bundle.putString("futureRaceStartMonth", monthStart);
            bundle.putString("futureRaceEndMonth", monthEnd);
            bundle.putString("circuitId", race.circuitId);
        }

        intent.putExtras(bundle);
        startActivity(intent);
    }

    private void setupDeleteSwipe() {
        final int corner = Math.round(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 8, getResources().getDisplayMetrics()));

        ItemTouchHelper.SimpleCallback callback =
                new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
                    @Override
                    public boolean onMove(
                            @NonNull RecyclerView recyclerView,
                            @NonNull RecyclerView.ViewHolder viewHolder,
                            @NonNull RecyclerView.ViewHolder target) {
                        return false;
                    }

                    @Override
                    public float getSwipeThreshold(@NonNull RecyclerView.ViewHolder viewHolder) {
                        return 0.4f;
                    }

                    @Override
                    public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int swipeDir) {
                        int position = viewHolder.getBindingAdapterPosition();

                        if (position == RecyclerView.NO_POSITION
                                || position >= adapter.getCurrentList().size()) {
                            adapter.notifyDataSetChanged();
                            return;
                        }

                        showDeleteRaceDialog(adapter.getCurrentList().get(position));
                        // Put the swiped card back; it disappears only after the user confirms.
                        adapter.notifyItemChanged(position);
                    }

                    @Override
                    public void onChildDraw(
                            @NonNull Canvas c,
                            @NonNull RecyclerView recyclerView,
                            @NonNull RecyclerView.ViewHolder viewHolder,
                            float dX,
                            float dY,
                            int actionState,
                            boolean isCurrentlyActive) {

                        if (actionState != ItemTouchHelper.ACTION_STATE_SWIPE) {
                            super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
                            return;
                        }

                        View card = viewHolder.itemView.findViewById(R.id.cardView);
                        View background = viewHolder.itemView.findViewById(R.id.swipeBackground);
                        float shift = Math.min(0f, dX);

                        // Only the card moves; the item itself stays put, so nothing gets raised or shadowed.
                        card.setTranslationX(shift);

                        if (shift < 0f) {
                            int width = background.getWidth();
                            int revealed = Math.min(width, Math.round(-shift));

                            // Red is visible only in the strip the card has uncovered.
                            background.setClipBounds(new Rect(
                                    Math.max(0, width - revealed - corner), 0, width, background.getHeight()));
                            background.setVisibility(View.VISIBLE);
                        } else {
                            background.setVisibility(View.INVISIBLE);
                        }
                    }

                    @Override
                    public void clearView(
                            @NonNull RecyclerView recyclerView,
                            @NonNull RecyclerView.ViewHolder viewHolder) {
                        super.clearView(recyclerView, viewHolder);

                        View card = viewHolder.itemView.findViewById(R.id.cardView);
                        View background = viewHolder.itemView.findViewById(R.id.swipeBackground);

                        if (card != null) {
                            card.setTranslationX(0f);
                        }

                        if (background != null) {
                            background.setClipBounds(null);
                            background.setVisibility(View.INVISIBLE);
                        }
                    }
                };

        new ItemTouchHelper(callback).attachToRecyclerView(recyclerView);
    }

    private void showDeleteRaceDialog(savedRaceCard race) {
        if (isFinishing() || isDestroyed()) {
            return;
        }

        if (deleteRaceDialog != null && deleteRaceDialog.isShowing()) {
            return;
        }

        deleteRaceDialog = new Dialog(savedRacesActivity.this);
        deleteRaceDialog.setContentView(R.layout.delete_race_dialog_box);

        if (deleteRaceDialog.getWindow() != null) {
            deleteRaceDialog.getWindow().setLayout(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );

            deleteRaceDialog.getWindow().setBackgroundDrawable(
                    AppCompatResources.getDrawable(
                            savedRacesActivity.this,
                            R.drawable.custom_dialog_bg
                    )
            );
        }

        deleteRaceDialog.setCancelable(false);

        Button cancelButton = deleteRaceDialog.findViewById(R.id.cancel_button);
        Button confirmButton = deleteRaceDialog.findViewById(R.id.confirm_button);

        cancelButton.setOnClickListener(view -> deleteRaceDialog.dismiss());

        confirmButton.setOnClickListener(view -> {
            deleteRaceDialog.dismiss();
            deleteRace(race);
        });

        deleteRaceDialog.show();
    }

    private void deleteRace(savedRaceCard race) {
        if (accountChanged()) {
            openLogin();
            return;
        }

        final String requestedUid = uid;

        // The database key is the one that was read, so a differently written key can never miss.
        FirebaseDatabase.getInstance()
                .getReference()
                .child("savedRaces")
                .child(requestedUid)
                .child(race.key)
                .removeValue()
                .addOnSuccessListener(unused -> {
                    if (isUnavailable() || accountChanged()) {
                        return;
                    }

                    races.removeIf(item -> item.key.equals(race.key));
                    renderList(true);

                    // Keep the original timestamp: deleting a card does not make the other data fresher.
                    savedRacesRepository.writeCache(
                            getApplicationContext(),
                            requestedUid,
                            new savedRacesRepository.Result(
                                    new ArrayList<>(races),
                                    lastCacheSavedAt > 0 ? lastCacheSavedAt : System.currentTimeMillis(),
                                    true
                            )
                    );

                    Toast.makeText(
                            savedRacesActivity.this,
                            getString(R.string.race_delete_succ_text),
                            Toast.LENGTH_SHORT
                    ).show();
                })
                .addOnFailureListener(error -> {
                    if (isUnavailable()) {
                        return;
                    }

                    Log.e(TAG, "Failed to delete race", error);
                    showMessage(getString(R.string.smth_wrong_text));
                });
    }
}
