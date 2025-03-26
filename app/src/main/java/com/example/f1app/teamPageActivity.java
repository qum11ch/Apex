package com.example.f1app;

import static com.example.f1app.MainActivity.checkConnection;

import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.widget.Toolbar;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
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

import java.util.ArrayList;

public class teamPageActivity extends AppCompatActivity {
    private ImageButton backButton;
    private ViewPager2 myViewPager2;
    private viewPagerAdapter adapter;
    private TextView teamNameFull, teamName;
    private ImageView teamLogo, team_car;
    private ProgressBar progressBar;
    private CoordinatorLayout contentLayout;


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

        contentLayout = (CoordinatorLayout) findViewById(R.id.content_layout);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        teamNameFull = (TextView) findViewById(R.id.teamNameFull);
        teamLogo = (ImageView) findViewById(R.id.teamLogo);
        team_car = (ImageView) findViewById(R.id.team_car);
        teamName = (TextView) findViewById(R.id.teamName);

        backButton = (ImageButton) findViewById(R.id.backButton);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        WindowInsetsControllerCompat windowInsetsController =
                WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        windowInsetsController.setAppearanceLightStatusBars(false);

        if (!getIntent().getExtras().isEmpty()){
            Bundle bundle = getIntent().getExtras();
            String mTeamId = bundle.getString("teamId");
            String mTeamName = bundle.getString("teamName");
            ArrayList<String> mDriversList = bundle.getStringArrayList("teamDrivers");

            Bundle teamPageBundle = new Bundle();
            teamPageBundle.putString("teamId", mTeamId);
            teamPageBundle.putString("teamName", mTeamName);
            teamPageBundle.putStringArrayList("teamDrivers", mDriversList);


            teamNameFull.setText(mTeamName);

            int resourceId_carImage = getResources().getIdentifier(mTeamId, "drawable",
                    getApplicationContext().getPackageName());

            Glide.with(getApplicationContext())
                    .load(resourceId_carImage)
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .skipMemoryCache(true)
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .error(R.drawable.f1)
                    .into(team_car);

            int resourceId_teamLogo;

            if (mTeamId.equals("alpine")) {
                resourceId_teamLogo = getResources().getIdentifier(mTeamId + "_logo_alt", "drawable",
                        getApplicationContext().getPackageName());
            } else if (mTeamId.equals("williams")) {
                resourceId_teamLogo = getResources().getIdentifier(mTeamId + "_logo_alt", "drawable",
                        getApplicationContext().getPackageName());
            } else{
                resourceId_teamLogo = getResources().getIdentifier(mTeamId + "_logo", "drawable",
                        getApplicationContext().getPackageName());
            }
            Glide.with(getApplicationContext())
                    .load(resourceId_teamLogo)
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .skipMemoryCache(true)
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .error(R.drawable.f1)
                    .into(teamLogo);

            CollapsingToolbarLayout collapsingToolbarLayout = (CollapsingToolbarLayout) findViewById(R.id.collapsing_toolbar);

            AppBarLayout appBarLayout = (AppBarLayout) findViewById(R.id.appbar);
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

            init(teamPageBundle);
        }
    }

    private void init(Bundle teamPageBundle) {
        myViewPager2 = findViewById(R.id.viewPager2);
        adapter = new viewPagerAdapter(this);
        teamStatsFragment teamStatsFragment = new teamStatsFragment();
        teamStatsFragment.setArguments(teamPageBundle);
        adapter.addFragment(teamStatsFragment);
        teamResultsFragment teamResultsFragment = new teamResultsFragment();
        teamResultsFragment.setArguments(teamPageBundle);
        adapter.addFragment(teamResultsFragment);
        myViewPager2.setAdapter(adapter);
        TabLayout tabLayout = findViewById(R.id.tab_layout);
        tabLayout.setTag("sticky");
        TabLayoutMediator tabLayoutMediator= new TabLayoutMediator(tabLayout, myViewPager2, new TabLayoutMediator.TabConfigurationStrategy(){
            @Override
            public void onConfigureTab(TabLayout.Tab tab, int position) {
                if (position == 0){
                    tab.setText(getResources().getString(R.string.stats_text));
                }
                else{
                    tab.setText(getResources().getString(R.string.results_text));
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