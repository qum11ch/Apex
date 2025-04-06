package com.example.f1app;

import static com.example.f1app.MainActivity.checkConnection;

import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
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
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.time.LocalDate;
import java.util.ArrayList;

public class teamPageActivity extends AppCompatActivity {
    private TextView teamName;
    private ProgressBar progressBar;
    private CoordinatorLayout contentLayout;
    private ToggleButton likeButton;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.team_page);
        EdgeToEdge.enable(this);

        if (!checkConnection(getApplicationContext())){
            startActivity(connectionLostScreen.createShowSplashOnNetworkFailure(teamPageActivity.this));
        }else{
            startActivity(connectionLostScreen.createIntentHideSplashOnNetworkRecovery(teamPageActivity.this));
        }

        contentLayout = findViewById(R.id.content_layout);
        progressBar = findViewById(R.id.progressBar);
        TextView teamNameFull = findViewById(R.id.teamNameFull);
        ImageView teamLogo = findViewById(R.id.teamLogo);
        ImageView team_car = findViewById(R.id.team_car);
        teamName = findViewById(R.id.teamName);
        likeButton = findViewById(R.id.like_button);

        ImageButton backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> finish());

        WindowInsetsControllerCompat windowInsetsController =
                WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        windowInsetsController.setAppearanceLightStatusBars(false);

        if (!getIntent().getExtras().isEmpty()){
            Bundle bundle = getIntent().getExtras();
            String mTeamId = bundle.getString("teamId");
            String mTeamName = bundle.getString("teamName");

            FirebaseStorage storage = FirebaseStorage.getInstance();
            StorageReference storageRef = storage.getReference();

            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user!=null){
                isFavourite(mTeamName);
                likeButton.setOnClickListener(view -> {
                    if(likeButton.isChecked()){
                        saveTeam(mTeamName);
                    }else{
                        deleteTeam(mTeamName);
                    }
                });
            }else{
                likeButton.setOnClickListener(view -> {
                    likeButton.setChecked(false);
                    Toast.makeText(teamPageActivity.this, getString(R.string.team_save_error_login_text), Toast.LENGTH_LONG).show();
                });
            }

            LocalDate currentDate = LocalDate.now();
            String currentYear = Integer.toString(currentDate.getYear());

            DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference();
            rootRef.child("driverLineUp/season/" + currentYear + "/" + mTeamId).addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    ArrayList<String> teamDrivers = new ArrayList<>();
                    for (DataSnapshot driverDataSnapshot : snapshot.child("drivers").getChildren()) {
                        String driverFullname = driverDataSnapshot.getKey();
                        teamDrivers.add(driverFullname);
                    }

                    Bundle teamPageBundle = new Bundle();
                    teamPageBundle.putString("teamId", mTeamId);
                    teamPageBundle.putString("teamName", mTeamName);
                    teamPageBundle.putStringArrayList("teamDrivers", teamDrivers);

                    init(teamPageBundle);
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {

                }
            });


            teamNameFull.setText(mTeamName);
            StorageReference mTeamCar = storageRef.child("teams/" + mTeamId.toLowerCase() + ".png");

            GlideApp.with(getApplicationContext())
                    .load(mTeamCar)
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .skipMemoryCache(true)
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .error(R.drawable.f1)
                    .into(team_car);

            StorageReference mTeamLogo;

            if (mTeamId.equals("alpine")) {
                mTeamLogo = storageRef.child("teams/" + mTeamId.toLowerCase() + "_logo_alt.png");
            } else if (mTeamId.equals("williams")) {
                mTeamLogo = storageRef.child("teams/" + mTeamId.toLowerCase() + "_logo_alt.png");
            } else{
                mTeamLogo = storageRef.child("teams/" + mTeamId.toLowerCase() + "_logo.png");
            }
            GlideApp.with(getApplicationContext())
                    .load(mTeamLogo)
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .skipMemoryCache(true)
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .error(R.drawable.f1)
                    .into(teamLogo);

            CollapsingToolbarLayout collapsingToolbarLayout = findViewById(R.id.collapsing_toolbar);

            AppBarLayout appBarLayout = findViewById(R.id.appbar);
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
                        teamName.setText(mTeamName);
                        toolbar.setBackgroundColor(ContextCompat.getColor(getApplicationContext(),R.color.dark_blue));
                        isShow = true;
                    } else if (isShow) {
                        teamName.setText(" ");
                        toolbar.setBackgroundColor(ContextCompat.getColor(getApplicationContext(),android.R.color.transparent));
                        isShow = false;
                    }
                }
            });

        }
    }

    private void saveTeam(String teamName){
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference();
        rootRef.child("users").orderByChild("userId")
                .equalTo(user.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        for(DataSnapshot userSnap: snapshot.getChildren()){
                            String username = userSnap.getKey();

                            rootRef.child("users").child(username).child("choiceTeam").setValue(teamName);
                            Toast.makeText(teamPageActivity.this, teamName + " " + getString(R.string.team_save_succ_text), Toast.LENGTH_LONG).show();
                        }
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e("concludedRacePage", "Drivers error:" + error.getMessage());
                    }
                });
    }

    private void deleteTeam(String teamName){
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference();
        rootRef.child("users").orderByChild("userId").equalTo(user.getUid())
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        for(DataSnapshot userSnap: snapshot.getChildren()){
                            String username = userSnap.getKey();
                            rootRef.child("users").child(username).child("choiceTeam").setValue("null");
                            Toast.makeText(teamPageActivity.this, teamName + " " + getString(R.string.team_delete_succ_text), Toast.LENGTH_LONG).show();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e("concludedRacePage", "Drivers error:" + error.getMessage());
                    }
                });
    }

    private void isFavourite(String teamName){
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference();
        rootRef.child("users").orderByChild("userId")
                .equalTo(user.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        for(DataSnapshot userSnap: snapshot.getChildren()){
                            String choiceTeam = userSnap.child("choiceTeam").getValue(String.class);
                            likeButton.setChecked(choiceTeam.equals(teamName));
                        }
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e("concludedRacePage", "Drivers error:" + error.getMessage());
                    }
                });
    }

    private void init(Bundle teamPageBundle) {
        ViewPager2 myViewPager2 = findViewById(R.id.viewPager2);
        viewPagerAdapter adapter = new viewPagerAdapter(this);
        teamStatsFragment teamStatsFragment = new teamStatsFragment();
        teamStatsFragment.setArguments(teamPageBundle);
        adapter.addFragment(teamStatsFragment);
        teamResultsFragment teamResultsFragment = new teamResultsFragment();
        teamResultsFragment.setArguments(teamPageBundle);
        adapter.addFragment(teamResultsFragment);
        myViewPager2.setAdapter(adapter);

        View child = myViewPager2.getChildAt(0);
        if (child instanceof RecyclerView) {
            child.setOverScrollMode(View.OVER_SCROLL_NEVER);
        }

        TabLayout tabLayout = findViewById(R.id.tab_layout);
        tabLayout.setTag("sticky");
        TabLayoutMediator tabLayoutMediator= new TabLayoutMediator(tabLayout, myViewPager2, (tab, position) -> {
            if (position == 0){
                tab.setText(getResources().getString(R.string.stats_text));
            }
            else{
                tab.setText(getResources().getString(R.string.results_text));
            }
        });
        tabLayoutMediator.attach();

        Handler handler = new Handler();
        handler.postDelayed(()->{
            contentLayout.setVisibility(View.VISIBLE);
            progressBar.setVisibility(View.GONE);
        },500);
    }
}