package com.example.f1app;

import android.graphics.Color;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.content.res.ColorStateList;
import android.graphics.drawable.GradientDrawable;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.ColorUtils;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.PagerSnapHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.google.firebase.storage.FirebaseStorage;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * "Cars" block of the team statistics: a swipeable list of seasons (newest first), each page has the
 * car photo, the year, the name of the team that year and the season results (place, points, wins).
 * Photos and results of every season are loaded once; a year without a photo says so and is written
 * to the log (tag TeamCars, lines "NO PHOTO ...").
 */
final class teamCarsSection {

    private static final String TAG = "TeamCars";
    private static final String DASH = "\u2013";

    private final Fragment host;
    private final View root;
    private final String teamId;
    private final String teamName;
    private final int latest;

    private final List<Integer> years = new ArrayList<>();
    private final Map<Integer, List<String>> candidates = new HashMap<>();

    private final Map<Integer, Uri> uris = new HashMap<>();
    private final Set<Integer> photoChecked = new HashSet<>();
    private final Map<Integer, String[]> standings = new HashMap<>();

    private Integer teamColor;
    private RequestQueue queue;

    private RecyclerView pager;
    private LinearLayoutManager layoutManager;
    private View prev;
    private View next;
    private CarAdapter adapter;

    teamCarsSection(Fragment host, View root, String teamId, String teamName, String currentSeason) {
        this.host = host;
        this.root = root;
        this.teamId = teamId;
        this.teamName = teamName;

        int parsed;

        try {
            parsed = Integer.parseInt(currentSeason.trim());
        } catch (NumberFormatException error) {
            parsed = LocalDate.now().getYear();
        }

        this.latest = parsed;
    }

    void setTeamColor(Integer color) {
        teamColor = color;

        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }

    void stop() {
        if (queue != null) {
            queue.cancelAll(this);
        }
    }

    void start() {
        int first = teamCarCatalog.firstYear(teamId);

        if (latest < first) {
            root.findViewById(R.id.cars_header).setVisibility(View.GONE);
            root.findViewById(R.id.car_pager_host).setVisibility(View.GONE);
            return;
        }

        for (int year = latest; year >= first; year--) {
            years.add(year);
            candidates.put(year, teamCarCatalog.candidates(teamId, year));
        }

        pager = root.findViewById(R.id.car_pager);
        prev = root.findViewById(R.id.car_prev);
        next = root.findViewById(R.id.car_next);

        layoutManager = new LinearLayoutManager(
                host.requireContext(), LinearLayoutManager.HORIZONTAL, false);

        pager.setLayoutManager(layoutManager);
        new PagerSnapHelper().attachToRecyclerView(pager);

        adapter = new CarAdapter();
        pager.setAdapter(adapter);

        pager.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                updateArrows();
            }

            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                updateArrows();
            }
        });

        // left = the newer season, right = the earlier one
        prev.setOnClickListener(v -> scrollBy(-1));
        next.setOnClickListener(v -> scrollBy(1));

        updateArrows();

        for (int year : years) {
            probePhoto(year, 0, new ArrayList<>());
            loadStanding(year);
        }
    }

    // ---------- arrows ----------

    private int currentPosition() {
        int position = layoutManager.findFirstCompletelyVisibleItemPosition();

        if (position == RecyclerView.NO_POSITION) {
            position = layoutManager.findFirstVisibleItemPosition();
        }

        return Math.max(position, 0);
    }

    private void scrollBy(int step) {
        int target = currentPosition() + step;

        if (target >= 0 && target < years.size()) {
            pager.smoothScrollToPosition(target);
        }
    }

    private void updateArrows() {
        int position = currentPosition();

        setArrow(prev, position > 0);
        setArrow(next, position < years.size() - 1);
    }

    private void setArrow(View arrow, boolean enabled) {
        arrow.setEnabled(enabled);
        arrow.setAlpha(enabled ? 1f : 0.3f);
    }

    // ---------- photos ----------

    private void probePhoto(int year, int index, List<String> tried) {
        List<String> ids = candidates.get(year);

        if (ids == null || index >= ids.size()) {
            photoChecked.add(year);
            Log.w(TAG, "NO PHOTO " + teamId + " " + year + ", tried " + tried);
            refresh(year);
            return;
        }

        String path = "teams/" + ids.get(index) + "_" + year + ".png";
        tried.add(path);

        FirebaseStorage.getInstance().getReference().child(path).getDownloadUrl()
                .addOnSuccessListener(uri -> {
                    if (!host.isAdded()) {
                        return;
                    }

                    Log.d(TAG, "found " + path);

                    uris.put(year, uri);
                    photoChecked.add(year);
                    refresh(year);
                })
                .addOnFailureListener(error -> {
                    if (!host.isAdded()) {
                        return;
                    }

                    probePhoto(year, index + 1, tried);
                });
    }

    // ---------- season results (the same source as the standings tables) ----------

    private void loadStanding(int year) {
        if (queue == null) {
            queue = Volley.newRequestQueue(host.requireContext().getApplicationContext());
        }

        String url = "https://api.jolpi.ca/ergast/f1/" + year + "/constructorstandings/?format=json";

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.GET,
                url,
                null,
                response -> {
                    if (!host.isAdded()) {
                        return;
                    }

                    try {
                        JSONArray lists = response
                                .getJSONObject("MRData")
                                .getJSONObject("StandingsTable")
                                .optJSONArray("StandingsLists");

                        if (lists == null) {
                            return;
                        }

                        List<String> wanted = new ArrayList<>();

                        for (String id : candidates.get(year)) {
                            wanted.add(teamCarCatalog.normalize(id));
                        }

                        for (int i = 0; i < lists.length(); i++) {
                            JSONArray entries = lists.getJSONObject(i)
                                    .optJSONArray("ConstructorStandings");

                            if (entries == null) {
                                continue;
                            }

                            for (int j = 0; j < entries.length(); j++) {
                                JSONObject entry = entries.getJSONObject(j);
                                JSONObject constructor = entry.optJSONObject("Constructor");

                                if (constructor == null) {
                                    continue;
                                }

                                String id = teamCarCatalog.normalize(
                                        constructor.optString("constructorId"));

                                if (wanted.contains(id)) {
                                    standings.put(year, new String[]{
                                            entry.optString("positionText", DASH),
                                            entry.optString("points", DASH),
                                            entry.optString("wins", DASH)
                                    });

                                    refresh(year);
                                    return;
                                }
                            }
                        }
                    } catch (JSONException error) {
                        Log.w(TAG, "Unexpected standings response for " + year, error);
                    }
                },
                error -> Log.w(TAG, "Standings are not available for " + year, error)
        );

        request.setTag(this);
        queue.add(request);
    }

    private void refresh(int year) {
        if (adapter == null) {
            return;
        }

        int position = years.indexOf(year);

        if (position >= 0) {
            adapter.notifyItemChanged(position);
        }
    }

    // ---------- pages ----------

    private final class CarAdapter extends RecyclerView.Adapter<CarAdapter.Holder> {

        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            View view = inflater.inflate(R.layout.item_team_car_page, parent, false);
            Holder holder = new Holder(view);

            int[] labels = {R.string.team_cars_position, R.string.team_results_points, R.string.wins};

            for (int i = 0; i < labels.length; i++) {
                View tile = inflater.inflate(
                        R.layout.item_team_results_summary, holder.results, false);

                ((TextView) tile.findViewById(R.id.summary_label)).setText(labels[i]);
                holder.values[i] = tile.findViewById(R.id.summary_value);

                holder.results.addView(tile, new LinearLayout.LayoutParams(
                        0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            }

            return holder;
        }

        @Override
        public void onBindViewHolder(@NonNull Holder holder, int position) {
            int year = years.get(position);

            holder.year.setText(String.valueOf(year));

            List<String> ids = candidates.get(year);
            String former = ids == null || ids.isEmpty() ? null : teamCarCatalog.formerName(ids.get(0));
            holder.caption.setText(former != null ? former : (teamName != null ? teamName : ""));

            paint(holder);

            Uri uri = uris.get(year);

            if (uri != null) {
                holder.missing.setVisibility(View.GONE);

                GlideApp.with(holder.photo)
                        .load(uri)
                        .transition(DrawableTransitionOptions.withCrossFade())
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .into(holder.photo);
            } else {
                GlideApp.with(holder.photo).clear(holder.photo);
                holder.photo.setImageDrawable(null);

                // still checking: nothing to say yet; checked and not found: say it
                holder.missing.setVisibility(
                        photoChecked.contains(year) ? View.VISIBLE : View.GONE);
            }

            String[] values = standings.get(year);

            for (int i = 0; i < holder.values.length; i++) {
                holder.values[i].setText(values != null ? values[i] : DASH);
            }
        }

        @Override
        public int getItemCount() {
            return years.size();
        }

        private void paint(Holder holder) {
            float density = holder.itemView.getResources().getDisplayMetrics().density;

            int color = teamColor != null
                    ? teamColor
                    : ContextCompat.getColor(holder.itemView.getContext(), R.color.grey);

            GradientDrawable background = new GradientDrawable();
            background.setColor(ContextCompat.getColor(
                    holder.itemView.getContext(), R.color.colorPrimary));
            background.setCornerRadius(16 * density);
            background.setStroke(Math.round(2 * density), color);

            holder.card.setBackground(background);

            int soft = ColorUtils.compositeColors(
                    ColorUtils.setAlphaComponent(color, 31), Color.WHITE);

            ViewCompat.setBackgroundTintList(holder.photoBlock, ColorStateList.valueOf(soft));
        }

        final class Holder extends RecyclerView.ViewHolder {
            final View card;
            final View photoBlock;
            final ImageView photo;
            final View missing;
            final TextView year;
            final TextView caption;
            final LinearLayout results;
            final TextView[] values = new TextView[3];

            Holder(@NonNull View itemView) {
                super(itemView);
                card = itemView.findViewById(R.id.car_card);
                photoBlock = itemView.findViewById(R.id.car_photo_block);
                photo = itemView.findViewById(R.id.car_photo);
                missing = itemView.findViewById(R.id.car_missing);
                year = itemView.findViewById(R.id.car_year);
                caption = itemView.findViewById(R.id.car_caption);
                results = itemView.findViewById(R.id.car_results);
            }
        }
    }
}
