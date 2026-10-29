package com.example.f1app;

import static com.example.f1app.MainActivity.checkConnection;
import static com.example.f1app.MainActivity.checkLightTheme;
import static com.example.f1app.driversStandingsActivity.startActivity_seasonData;

import android.app.Dialog;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.facebook.shimmer.ShimmerFrameLayout;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class savedRacesActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private ImageButton backButton;

    private List<savedRacesData> datum;
    private savedRacesAdapter adapter;

    // private RelativeLayout emptySavedRaceLayout;

    private SwipeRefreshLayout swipeLayout;
    private ShimmerFrameLayout shimmerFrameLayout;

    private Dialog deleteRaceDialog;

    private DatabaseReference savedRacesRef;

    private View emptyScroll;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        setContentView(R.layout.saved_races);

        setupSavedRacesInsets();

        if (!checkConnection(getApplicationContext())) {
            startActivity(connectionLostScreen
                    .createShowSplashOnNetworkFailure(
                            savedRacesActivity.this
                    ));
        } else {
            startActivity(connectionLostScreen
                    .createIntentHideSplashOnNetworkRecovery(
                            savedRacesActivity.this
                    ));
        }

        shimmerFrameLayout = findViewById(R.id.shimmer_layout);
        shimmerFrameLayout.startShimmer();

        recyclerView = findViewById(R.id.recyclerview_savedRaces);
        // recyclerView.setHasFixedSize(true);

        LinearLayoutManager linearLayoutManager =
                new LinearLayoutManager(this);

        recyclerView.setLayoutManager(linearLayoutManager);

        datum = new ArrayList<>();
        adapter = new savedRacesAdapter(
                savedRacesActivity.this,
                datum
        );

        recyclerView.setAdapter(adapter);

        swipeLayout = findViewById(R.id.swipe_layout);

        emptyScroll = findViewById(R.id.emptySavedRaceScroll);

        swipeLayout.setOnChildScrollUpCallback((parent, child) -> {
            if (emptyScroll.getVisibility() == View.VISIBLE) {
                return emptyScroll.canScrollVertically(-1);
            }

            return recyclerView.getVisibility() == View.VISIBLE
                    && recyclerView.canScrollVertically(-1);
        });

        swipeLayout.setOnRefreshListener(() -> {
            emptyScroll.setVisibility(View.GONE);
            recyclerView.setVisibility(View.GONE);

            shimmerFrameLayout.setVisibility(View.VISIBLE);
            shimmerFrameLayout.startShimmer();

            datum.clear();
            adapter.notifyDataSetChanged();

            loadSavedRaces();

            swipeLayout.setRefreshing(false);
        });

        ShapeableImageView stripedImage =
                findViewById(R.id.stripedImage);

        if (!checkLightTheme(savedRacesActivity.this)) {
            stripedImage.setBackground(
                    ContextCompat.getDrawable(
                            savedRacesActivity.this,
                            R.drawable.background_striped_lines_item_night
                    )
            );
        }

        WindowInsetsControllerCompat windowInsetsController =
                WindowCompat.getInsetsController(
                        getWindow(),
                        getWindow().getDecorView()
                );

        windowInsetsController.setAppearanceLightStatusBars(false);

        backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> finish());

        setupDeleteSwipe();

        loadSavedRaces();
    }

    private void setupSavedRacesInsets() {
        View root = findViewById(R.id.saved_races_root);
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
                    rootBottom + safeInsets.bottom
            );

            header.setPadding(
                    headerLeft,
                    headerTop + safeInsets.top,
                    headerRight,
                    headerBottom
            );

            return WindowInsetsCompat.CONSUMED;
        });

        ViewCompat.requestApplyInsets(root);
    }

    private void loadSavedRaces() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user == null) {
            startActivity_seasonData(
                    savedRacesActivity.this,
                    logInPageActivity.class,
                    null
            );

            finish();
            return;
        }

        String uid = user.getUid();

        savedRacesRef = FirebaseDatabase.getInstance()
                .getReference()
                .child("savedRaces")
                .child(uid);

        savedRacesRef.addListenerForSingleValueEvent(
                new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (isFinishing() || isDestroyed()) {
                            return;
                        }

                        datum.clear();

                        if (!snapshot.exists()) {
                            showEmptyState();
                            return;
                        }

                        for (DataSnapshot savedRaceSnap :
                                snapshot.getChildren()) {

                            String raceName =
                                    savedRaceSnap.child("raceName")
                                            .getValue(String.class);

                            String raceSeason =
                                    savedRaceSnap.child("raceSeason")
                                            .getValue(String.class);

                            String saveDate =
                                    savedRaceSnap.child("saveDate")
                                            .getValue(String.class);

                            if (raceName == null
                                    || raceSeason == null
                                    || saveDate == null) {
                                continue;
                            }

                            datum.add(new savedRacesData(
                                    raceName,
                                    raceSeason,
                                    saveDate
                            ));
                        }

                        if (datum.isEmpty()) {
                            showEmptyState();
                        } else {
                            showRaces();
                        }

                        adapter.notifyDataSetChanged();
                    }

                    @Override
                    public void onCancelled(
                            @NonNull DatabaseError error) {

                        if (isFinishing() || isDestroyed()) {
                            return;
                        }

                        Log.e(
                                "savedRacesPage",
                                "Failed to load saved races: "
                                        + error.getMessage()
                        );

                        showEmptyState();
                        showError(
                                getString(R.string.smth_wrong_text)
                        );
                    }
                });
    }

    private void showEmptyState() {
        shimmerFrameLayout.animate()
                .setDuration(500)
                .withEndAction(() -> {
                    if (isFinishing() || isDestroyed()) {
                        return;
                    }

                    emptyScroll.setVisibility(View.VISIBLE);
                    recyclerView.setVisibility(View.GONE);

                    shimmerFrameLayout.setVisibility(View.GONE);
                    shimmerFrameLayout.stopShimmer();
                })
                .start();
    }

    private void showRaces() {
        shimmerFrameLayout.animate()
                .setDuration(500)
                .withEndAction(() -> {
                    if (isFinishing() || isDestroyed()) {
                        return;
                    }

                    emptyScroll.setVisibility(View.GONE);
                    recyclerView.setVisibility(View.VISIBLE);

                    shimmerFrameLayout.setVisibility(View.GONE);
                    shimmerFrameLayout.stopShimmer();
                })
                .start();
    }

    private void setupDeleteSwipe() {
        ItemTouchHelper.SimpleCallback simpleItemTouchCallback =
                new ItemTouchHelper.SimpleCallback(
                        0,
                        ItemTouchHelper.LEFT
                ) {
                    @Override
                    public boolean onMove(
                            @NonNull RecyclerView recyclerView,
                            @NonNull RecyclerView.ViewHolder viewHolder,
                            @NonNull RecyclerView.ViewHolder target) {

                        return false;
                    }

                    @Override
                    public void onSwiped(
                            @NonNull RecyclerView.ViewHolder viewHolder,
                            int swipeDir) {

                        if (swipeDir != ItemTouchHelper.LEFT) {
                            return;
                        }

                        int position =
                                viewHolder.getBindingAdapterPosition();

                        if (position == RecyclerView.NO_POSITION
                                || position >= datum.size()) {

                            loadSavedRaces();
                            return;
                        }

                        showDeleteRaceDialog(position);
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

                        super.onChildDraw(
                                c,
                                recyclerView,
                                viewHolder,
                                dX / 4,
                                dY,
                                actionState,
                                isCurrentlyActive
                        );

                        View itemView = viewHolder.itemView;

                        Drawable icon = AppCompatResources.getDrawable(
                                savedRacesActivity.this,
                                R.drawable.delete_white
                        );

                        if (icon == null) {
                            return;
                        }

                        int iconMargin =
                                (itemView.getHeight()
                                        - icon.getIntrinsicHeight()) / 2;

                        int iconTop =
                                itemView.getTop()
                                + iconMargin;

                        int iconBottom =
                                iconTop
                                        + icon.getIntrinsicHeight();

                        if (dX < 0) {
                            int iconLeft =
                                    itemView.getRight()
                                            - iconMargin
                                            - icon.getIntrinsicWidth();

                            int iconRight =
                                    itemView.getRight()
                                            - iconMargin;

                            icon.setBounds(
                                    iconLeft,
                                    iconTop,
                                    iconRight,
                                    iconBottom
                            );

                            int backgroundCornerOffset = 100;

                            int marginBottom =
                                    (int) TypedValue.applyDimension(
                                            TypedValue.COMPLEX_UNIT_DIP,
                                            0,
                                            getResources()
                                                    .getDisplayMetrics()
                                    );

                            RectF bg = new RectF(
                                    itemView.getRight()
                                            + ((int) dX)
                                            - backgroundCornerOffset,
                                    itemView.getTop(),
                                    itemView.getRight(),
                                    itemView.getBottom() - marginBottom
                            );

                            Paint paint = new Paint();
                            paint.setColor(getColor(R.color.red));

                            int rounded =
                                    (int) TypedValue.applyDimension(
                                            TypedValue.COMPLEX_UNIT_DIP,
                                            20,
                                            getResources()
                                                    .getDisplayMetrics()
                                    );

                            c.drawRoundRect(
                                    bg,
                                    rounded,
                                    rounded,
                                    paint
                            );
                        }

                        icon.draw(c);
                    }
                };

        ItemTouchHelper itemTouchHelper =
                new ItemTouchHelper(simpleItemTouchCallback);

        itemTouchHelper.attachToRecyclerView(recyclerView);
    }

    private void showDeleteRaceDialog(int position) {
        if (isFinishing() || isDestroyed()) {
            return;
        }

        deleteRaceDialog = new Dialog(savedRacesActivity.this);
        deleteRaceDialog.setContentView(
                R.layout.delete_race_dialog_box
        );

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

        Button cancelButton =
                deleteRaceDialog.findViewById(R.id.cancel_button);

        Button confirmButton =
                deleteRaceDialog.findViewById(R.id.confirm_button);

        cancelButton.setOnClickListener(view -> {
            deleteRaceDialog.dismiss();
            loadSavedRaces();
        });

        confirmButton.setOnClickListener(view -> {
            deleteRaceDialog.dismiss();

            savedRacesData race = datum.get(position);

            String modifiedRaceName = race.getRaceName()
                    .replaceAll("\\s+", "");

            String raceKey =
                    race.getRaceSeason()
                            + "_"
                            + modifiedRaceName;

            deleteRace(raceKey);
        });

        deleteRaceDialog.show();
    }

    private void deleteRace(String raceKey) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user == null) {
            startActivity_seasonData(
                    savedRacesActivity.this,
                    logInPageActivity.class,
                    null
            );

            finish();
            return;
        }

        FirebaseDatabase.getInstance()
                .getReference()
                .child("savedRaces")
                .child(user.getUid())
                .child(raceKey)
                .removeValue()
                .addOnSuccessListener(unused -> {
                    if (isFinishing() || isDestroyed()) {
                        return;
                    }

                    Toast.makeText(
                            savedRacesActivity.this,
                            getString(R.string.race_delete_succ_text),
                            Toast.LENGTH_SHORT
                    ).show();

                    loadSavedRaces();
                })
                .addOnFailureListener(error -> {
                    if (isFinishing() || isDestroyed()) {
                        return;
                    }

                    Log.e(
                            "savedRacesPage",
                            "Failed to delete race",
                            error
                    );

                    showError(
                            getString(R.string.smth_wrong_text)
                    );

                    loadSavedRaces();
                });
    }

    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}