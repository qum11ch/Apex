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

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class driverPageActivity extends AppCompatActivity {
    private ImageButton backButton;
    private ViewPager2 myViewPager2;
    private viewPagerAdapter adapter;
    private TextView teamName, driverNumber, driverfullName,
            driverFamilyName, driverName;
    private ProgressBar progressBar;
    private CoordinatorLayout contentLayout;
    private ImageView driverImage;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //getWindow().requestFeature(Window.FEATURE_ACTIVITY_TRANSITIONS);
        //getWindow().setExitTransition(new Explode());
        setContentView(R.layout.driver_page);
        EdgeToEdge.enable(this);

        contentLayout = (CoordinatorLayout) findViewById(R.id.content_layout);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        driverfullName = (TextView) findViewById(R.id.driverfullName);
        driverFamilyName = (TextView) findViewById(R.id.driverFamilyName);
        driverImage = (ImageView) findViewById(R.id.driver_image);
        driverNumber = (TextView) findViewById(R.id.driverNumber);
        driverName = (TextView) findViewById(R.id.driverName);
        teamName = (TextView) findViewById(R.id.teamName);

        backButton = (ImageButton) findViewById(R.id.backButton);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finishAfterTransition();
            }
        });

        WindowInsetsControllerCompat windowInsetsController =
                WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        windowInsetsController.setAppearanceLightStatusBars(false);

        if (!checkConnection(getApplicationContext())){
            startActivity(connectionLostScreen.createShowSplashOnNetworkFailure(driverPageActivity.this));
        }else{
            startActivity(connectionLostScreen.createIntentHideSplashOnNetworkRecovery(driverPageActivity.this));
        }


        if(!getIntent().getExtras().isEmpty()){
            Bundle bundle = getIntent().getExtras();
            String mDriverName = bundle.getString("driverName");
            String mDriverFamilyName = bundle.getString("driverFamilyName");
            String mDriverTeam = bundle.getString("driverTeam");
            String mDriverCode = bundle.getString("driverCode");
            String mTeamId = bundle.getString("driverTeamId");

            String driver = mDriverName + " " + mDriverFamilyName;

            Bundle driverPageBundle = new Bundle();
            driverPageBundle.putString("driverName", mDriverName);
            driverPageBundle.putString("driverFamilyName", mDriverFamilyName);
            driverPageBundle.putString("driverTeam", mDriverTeam);
            driverPageBundle.putString("driverCode", mDriverCode);
            driverPageBundle.putString("driverTeamId", mTeamId);

            int resourceId_driverImage = getApplicationContext().getResources().getIdentifier(mDriverCode.toLowerCase(), "drawable",
                    getApplicationContext().getPackageName());

            DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference();
            rootRef.child("drivers").child(driver).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    driverfullName.setText(mDriverName);
                    teamName.setText(mDriverTeam);
                    driverFamilyName.setText(mDriverFamilyName);

                    String mDriverNumber = snapshot.child("permanentNumber").getValue(String.class);
                    driverNumber.setText(mDriverNumber);

                    Glide.with(getApplicationContext())
                            .load(resourceId_driverImage)
                            .transition(DrawableTransitionOptions.withCrossFade())
                            .diskCacheStrategy(DiskCacheStrategy.NONE)
                            .transition(DrawableTransitionOptions.withCrossFade())
                            .error(R.drawable.f1)
                            .into(driverImage);

                    init(driverPageBundle);
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e("driverPageActivity firebase error: ", error.getMessage());
                }
            });

            CollapsingToolbarLayout collapsingToolbarLayout = (CollapsingToolbarLayout) findViewById(R.id.collapsing_toolbar);
            AppBarLayout appBarLayout = (AppBarLayout) findViewById(R.id.appbar);
            appBarLayout.setExpanded(true,true);
            Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
            appBarLayout.addOnOffsetChangedListener(new AppBarLayout.OnOffsetChangedListener() {
                boolean isShow = true;
                int scrollRange = -1;

                @Override
                public void onOffsetChanged(AppBarLayout appBarLayout, int verticalOffset) {
                    if (scrollRange == -1) {
                        scrollRange = appBarLayout.getTotalScrollRange();
                    }
                    if (scrollRange + verticalOffset == 0) {
                        driverName.setText(driver);
                        toolbar.setBackgroundColor(ContextCompat.getColor(getApplicationContext(),R.color.dark_blue));
                        isShow = true;
                    } else if (isShow) {
                        driverName.setText(" ");
                        toolbar.setBackgroundColor(ContextCompat.getColor(getApplicationContext(),android.R.color.transparent));
                        isShow = false;
                    }
                }
            });
        }
    }

    private void init(Bundle driverPageBundle) {
        myViewPager2 = findViewById(R.id.viewPager2);
        adapter = new viewPagerAdapter(this);
        driverStatsFragment driverStatsFragment = new driverStatsFragment();
        driverStatsFragment.setArguments(driverPageBundle);
        adapter.addFragment(driverStatsFragment);
        driverResultsFragment driverResultsFragment = new driverResultsFragment();
        driverResultsFragment.setArguments(driverPageBundle);
        adapter.addFragment(driverResultsFragment);
        myViewPager2.setAdapter(adapter);
        TabLayout tabLayout = findViewById(R.id.tab_layout);
        tabLayout.setTag("sticky");
        TabLayoutMediator tabLayoutMediator= new TabLayoutMediator(tabLayout, myViewPager2, new TabLayoutMediator.TabConfigurationStrategy(){
            @Override
            public void onConfigureTab(TabLayout.Tab tab, int position) {
                if (position == 0){
                    tab.setText(R.string.stats_text);
                }
                else{
                    tab.setText(R.string.results_text);
                }
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