package com.example.f1app;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.media.Image;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
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
    private teamStatsViewPagerAdapter teamAdapter;
    private TextView teamNameFull, teamName;
    private ImageView teamLogo, team_car;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.team_page);
        EdgeToEdge.enable(this);

        Bundle bundle = getIntent().getExtras();
        String mTeamId = bundle.getString("teamId");
        String mTeamName = bundle.getString("teamName");
        ArrayList<String> mDriversList = bundle.getStringArrayList("teamDrivers");

        Log.i("teamName", "" + mTeamName);
        Bundle teamPageBundle = new Bundle();
        teamPageBundle.putString("teamId", mTeamId);
        teamPageBundle.putString("teamName", mTeamName);
        teamPageBundle.putStringArrayList("teamDrivers", mDriversList);

        backButton = (ImageButton) findViewById(R.id.backButton);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        init(teamPageBundle);

        WindowInsetsControllerCompat windowInsetsController =
                WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        windowInsetsController.setAppearanceLightStatusBars(false);

        teamNameFull = (TextView) findViewById(R.id.teamNameFull);
        teamLogo = (ImageView) findViewById(R.id.teamLogo);
        team_car = (ImageView) findViewById(R.id.team_car);
        teamName = (TextView) findViewById(R.id.teamName);

        teamNameFull.setText(mTeamName);

        int resourceId_carImage = getApplicationContext().getResources().getIdentifier(mTeamId, "drawable",
                getApplicationContext().getPackageName());

        Glide.with(getApplicationContext())
                .load(resourceId_carImage)
                .transition(DrawableTransitionOptions.withCrossFade())
                .error(R.drawable.f1)
                .into(team_car);

        int resourceId_teamLogo;

        if (mTeamId.equals("alpine")){
            resourceId_teamLogo = getApplicationContext().getResources().getIdentifier(mTeamId + "_logo_alt", "drawable",
                    getApplicationContext().getPackageName());

        }else{
            resourceId_teamLogo = getApplicationContext().getResources().getIdentifier(mTeamId + "_logo", "drawable",
                    getApplicationContext().getPackageName());
        }
        Glide.with(getApplicationContext())
                .load(resourceId_teamLogo)
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
    }

    private void init(Bundle teamPageBundle) {
        myViewPager2 = findViewById(R.id.viewPager2);
        teamAdapter = new teamStatsViewPagerAdapter(this);
        teamStatsFragment teamStatsFragment = new teamStatsFragment();
        teamStatsFragment.setArguments(teamPageBundle);
        teamAdapter.addFragment(teamStatsFragment);
        teamResultsFragment teamResultsFragment = new teamResultsFragment();
        teamResultsFragment.setArguments(teamPageBundle);
        teamAdapter.addFragment(teamResultsFragment);
        myViewPager2.setAdapter(teamAdapter);
        TabLayout tabLayout = findViewById(R.id.tab_layout);
        tabLayout.setTag("sticky");
        TabLayoutMediator tabLayoutMediator= new TabLayoutMediator(tabLayout, myViewPager2, new TabLayoutMediator.TabConfigurationStrategy(){
            @Override
            public void onConfigureTab(TabLayout.Tab tab, int position) {
                if (position == 0){
                    tab.setText("Stats");
                }
                else{
                    tab.setText("Results");
                }
            }
        });
        tabLayoutMediator.attach();
    }
}