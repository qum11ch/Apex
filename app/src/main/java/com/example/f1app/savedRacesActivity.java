package com.example.f1app;

import static com.example.f1app.MainActivity.checkConnection;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
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
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.facebook.shimmer.ShimmerFrameLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class savedRacesActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    ImageButton backButton;
    List<savedRacesData> datum;
    savedRacesAdapter adapter;
    private RelativeLayout emptySavedRaceLayout;
    SwipeRefreshLayout swipeLayout;
    ShimmerFrameLayout shimmerFrameLayout;
    private Dialog deleteRaceDialog;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.saved_races);

        if (!checkConnection(getApplicationContext())){
            startActivity(connectionLostScreen.createShowSplashOnNetworkFailure(savedRacesActivity.this));
        }else{
            startActivity(connectionLostScreen.createIntentHideSplashOnNetworkRecovery(savedRacesActivity.this));
        }

        shimmerFrameLayout = findViewById(R.id.shimmer_layout);
        shimmerFrameLayout.startShimmer();

        recyclerView = findViewById(R.id.recyclerview_savedRaces);
        recyclerView.setHasFixedSize(true);
        LinearLayoutManager linearLayoutManager3 = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(linearLayoutManager3);

        swipeLayout = findViewById(R.id.swipe_layout);
        swipeLayout.setOnRefreshListener(() -> {
            emptySavedRaceLayout.setVisibility(View.GONE);
            recyclerView.setVisibility(View.GONE);
            shimmerFrameLayout.setVisibility(View.VISIBLE);
            shimmerFrameLayout.startShimmer();
            putRaces();
            swipeLayout.setRefreshing(false);
        });

        putRaces();

        emptySavedRaceLayout = findViewById(R.id.emptySavedRace_layout);

        //putRaces();

        WindowInsetsControllerCompat windowInsetsController =
                WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        windowInsetsController.setAppearanceLightStatusBars(false);

        backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> finish());
    }

    private void putRaces(){
        datum = new ArrayList<>();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference();
        rootRef.child("users").orderByChild("userId")
                .equalTo(user.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        for(DataSnapshot userSnap: snapshot.getChildren()){
                            String username = userSnap.getKey();
                            rootRef.child("savedRaces").addValueEventListener(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot snapshot) {
                                    if(!snapshot.hasChild(username)) {
                                        Handler handler = new Handler();
                                        handler.postDelayed(()->{
                                            recyclerView.setVisibility(View.GONE);
                                            shimmerFrameLayout.setVisibility(View.GONE);
                                            shimmerFrameLayout.stopShimmer();
                                            emptySavedRaceLayout.setVisibility(View.VISIBLE);
                                        },500);
                                    }else {
                                        Handler handler = new Handler();
                                        handler.postDelayed(()->{
                                            shimmerFrameLayout.setVisibility(View.GONE);
                                            shimmerFrameLayout.stopShimmer();
                                            emptySavedRaceLayout.setVisibility(View.GONE);
                                            recyclerView.setVisibility(View.VISIBLE);
                                        },500);
                                    }
                                    for (DataSnapshot savedRaceSnap: snapshot.child(username).getChildren()){
                                            String raceName = savedRaceSnap.child("raceName").getValue(String.class);
                                            String raceSeason = savedRaceSnap.child("raceSeason").getValue(String.class);
                                            String saveDate = savedRaceSnap.child("saveDate").getValue(String.class);
                                            savedRacesData savedRacesData = new savedRacesData(raceName, raceSeason, saveDate);
                                            datum.add(savedRacesData);
                                    }
                                    adapter = new savedRacesAdapter(savedRacesActivity.this, datum);
                                    recyclerView.setAdapter(adapter);

                                    ItemTouchHelper.SimpleCallback simpleItemTouchCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
                                        @Override
                                        public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                                            return false;
                                        }

                                        @SuppressLint("NotifyDataSetChanged")
                                        @Override
                                        public void onSwiped(RecyclerView.ViewHolder viewHolder, int swipeDir) {
                                            final int position = viewHolder.getAdapterPosition();
                                            if (swipeDir == ItemTouchHelper.LEFT) {
                                                deleteRaceDialog = new Dialog(savedRacesActivity.this);
                                                deleteRaceDialog.setContentView(R.layout.delete_race_dialog_box);
                                                deleteRaceDialog.getWindow().setLayout(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                                                deleteRaceDialog.getWindow().setBackgroundDrawable(AppCompatResources.getDrawable(savedRacesActivity.this, R.drawable.custom_dialog_bg));
                                                deleteRaceDialog.setCancelable(false);

                                                Button cancelButton = deleteRaceDialog.findViewById(R.id.cancel_button);
                                                Button confirmButton = deleteRaceDialog.findViewById(R.id.confirm_button);

                                                cancelButton.setOnClickListener(view -> {
                                                    adapter.notifyItemRemoved(position + 1);
                                                    adapter.notifyItemRangeChanged(position, adapter.getItemCount());
                                                    putRaces();
                                                    deleteRaceDialog.dismiss();
                                                });

                                                confirmButton.setOnClickListener(view -> {
                                                    deleteRaceDialog.dismiss();
                                                    adapter.notifyItemRemoved(position);
                                                    String modifiedRaceName = datum.get(position).getRaceName().replaceAll("\\s+","");
                                                    String root = datum.get(position).getRaceSeason() + "_" + modifiedRaceName;
                                                    rootRef.child("savedRaces").child(username).child(root)
                                                            .removeValue();
                                                    putRaces();
                                                    Toast.makeText(savedRacesActivity.this, getString(R.string.race_delete_succ_text), Toast.LENGTH_SHORT).show();
                                                });

                                                deleteRaceDialog.show();
                                            }
                                        }

                                        @Override
                                        public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
                                            super.onChildDraw(c, recyclerView, viewHolder, dX / 4, dY, actionState, isCurrentlyActive);

                                            View itemView = viewHolder.itemView;
                                            Drawable icon = AppCompatResources.getDrawable(savedRacesActivity.this, R.drawable.delete_white);
                                            int iconMargin = (itemView.getHeight() - icon.getIntrinsicHeight()) / 2;
                                            int iconTop = itemView.getTop() + (itemView.getHeight() - icon.getIntrinsicHeight()) / 2;
                                            int iconBottom = iconTop + icon.getIntrinsicHeight();

                                            if (dX < 0) {
                                                int iconLeft = itemView.getRight() - iconMargin - icon.getIntrinsicWidth();
                                                int iconRight = itemView.getRight() - iconMargin;
                                                icon.setBounds(iconLeft, iconTop, iconRight, iconBottom);
                                                int backgroundCornerOffset = 100;
                                                int marginBottom = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 0, getResources().getDisplayMetrics());
                                                RectF bg = new RectF(itemView.getRight() + ((int) dX) - backgroundCornerOffset,
                                                        itemView.getTop(), itemView.getRight(), itemView.getBottom() - marginBottom);
                                                Paint p = new Paint();
                                                p.setColor(getColor(R.color.red));
                                                int rounded = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 20, getResources().getDisplayMetrics());
                                                c.drawRoundRect(bg, rounded, rounded, p);
                                            }
                                            icon.draw(c);
                                        }
                                    };

                                    ItemTouchHelper itemTouchHelper = new ItemTouchHelper(simpleItemTouchCallback);
                                    itemTouchHelper.attachToRecyclerView(recyclerView);
                                }
                                @Override
                                public void onCancelled(@NonNull DatabaseError error) {
                                    Log.e("savedRacesPage", "Drivers error:" + error.getMessage());
                                }
                            });
                        }
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e("savedRacesPage", "Drivers error:" + error.getMessage());
                    }
                });
    }
}
