package com.example.f1app;

import static com.example.f1app.MainActivity.getConnectionType;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.media.Image;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.viewpager2.widget.ViewPager2;

import com.android.volley.AuthFailureError;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

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

        if (getConnectionType(getApplicationContext())==0){
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