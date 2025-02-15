package com.example.f1app;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.media.Image;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.widget.Toolbar;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;

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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class teamPageActivity extends AppCompatActivity {
    private TextView teamName, teamNameFull, location, wins, firstTeamEntry, worldChamps, polePositions,
            teamPrincipal, engine;
    private ImageView teamLogo, team_car;

    private ImageButton backButton;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.team_page);

        teamName = (TextView) findViewById(R.id.teamName);
        teamNameFull = (TextView) findViewById(R.id.teamNameFull);
        teamLogo = (ImageView) findViewById(R.id.teamLogo);
        team_car = (ImageView) findViewById(R.id.team_car);

        CollapsingToolbarLayout collapsingToolbarLayout = (CollapsingToolbarLayout) findViewById(R.id.collapsing_toolbar);
        AppBarLayout appBarLayout = (AppBarLayout) findViewById(R.id.appbar);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);

        Bundle bundle = getIntent().getExtras();
        if (!bundle.isEmpty()){
            String mTeamId = bundle.getString("teamId");
            String mTeamName = bundle.getString("teamName");
            ArrayList<String> driversList = bundle.getStringArrayList("teamDrivers");

            teamNameFull.setText(mTeamName);
            int resourceId_teamLogo;

            int resourceId_carImage = getApplicationContext().getResources().getIdentifier(mTeamId, "drawable",
                    getApplicationContext().getPackageName());

            Glide.with(getApplicationContext())
                    .load(resourceId_carImage)
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .error(R.drawable.f1)
                    .into(team_car);

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
        }else{
            Log.e("teamPageActivity", "Error: Bundle from teamsAdapter is empty!");
        }

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

    }
}