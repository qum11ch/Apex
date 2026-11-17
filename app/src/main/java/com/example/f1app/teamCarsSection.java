package com.example.f1app;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.ColorUtils;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.PagerSnapHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.google.firebase.storage.FirebaseStorage;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Every season of the team: the car, the name of the team that year and the constructors' results. */
final class teamCarsSection {

    private static final String TAG = "TeamCars";
    private static final String DASH = "\u2013";
    private static final int FIRST_CHAMPIONSHIP = 1958;

    private final Fragment host;
    private final View root;
    private final String teamId;
    private final String teamName;
    private final int latest;

    private final List<Integer> years = new ArrayList<>();
    /** year -> Jolpica id of the team that raced that season (the team itself or a predecessor). */
    private final Map<Integer, String> yearTeam = new HashMap<>();

    private final Map<Integer, Uri> uris = new HashMap<>();
    private final Set<Integer> photoRequested = new HashSet<>();
    private final Set<Integer> photoChecked = new HashSet<>();
    private final Set<Integer> standingsRequested = new HashSet<>();
    private final Map<Integer, String[]> standings = new HashMap<>();

    private static final String PICKER_KEY = "team_cars_year";

    private Integer teamColor;

    private RecyclerView pager;
    private LinearLayoutManager layoutManager;
    private View prev;
    private View next;
    private CarAdapter adapter;

    private int pageWidth;
    private int pendingSeasons;

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
        // requests are shared and finish on their own
    }

    void start() {
        root.findViewById(R.id.cars_header).setVisibility(View.GONE);
        root.findViewById(R.id.car_pager_host).setVisibility(View.GONE);

        List<String> ids = teamCarCatalog.lineage(teamId);

        if (ids.isEmpty()) {
            return;
        }

        pendingSeasons = ids.size();

        for (String id : ids) {
            jolpicaSeasons.constructorSeasons(id, response -> {
                if (host.isAdded() && response != null) {
                    try {
                        readSeasons(id, response);
                    } catch (JSONException error) {
                        Log.w(TAG, "Unexpected seasons response for " + id, error);
                    }
                }

                seasonsDone();
            });
        }
    }

    // ---------- the seasons of the team ----------

    private void readSeasons(String id, JSONObject response) throws JSONException {
        JSONArray seasons = response
                .getJSONObject("MRData")
                .getJSONObject("SeasonTable")
                .optJSONArray("Seasons");

        if (seasons == null) {
            return;
        }

        boolean own = teamCarCatalog.normalize(id).equals(teamCarCatalog.normalize(teamId));

        for (int i = 0; i < seasons.length(); i++) {
            int year;

            try {
                year = Integer.parseInt(seasons.getJSONObject(i).optString("season", ""));
            } catch (NumberFormatException error) {
                continue;
            }

            if (year > latest) {
                continue;
            }

            if (own) {
                yearTeam.put(year, id);
            } else if (!yearTeam.containsKey(year)) {
                yearTeam.put(year, id);
            }
        }
    }

    /** Called once for every requested team id, whatever the outcome. */
    private void seasonsDone() {
        pendingSeasons--;

        if (pendingSeasons > 0 || !host.isAdded()) {
            return;
        }

        String own = teamId.toLowerCase(Locale.ROOT);

        // the season that has just begun may not be in the list yet
        if (!yearTeam.containsKey(latest) && yearTeam.containsKey(latest - 1)) {
            yearTeam.put(latest, own);
        }

        // the list is not available: the seasons with photos, as before
        if (yearTeam.isEmpty()) {
            for (int year = latest; year >= teamCarCatalog.firstYear(teamId); year--) {
                yearTeam.put(year, own);
            }
        }

        years.addAll(yearTeam.keySet());
        Collections.sort(years, Collections.reverseOrder());

        if (years.isEmpty()) {
            return;
        }

        show();
    }

    private void show() {
        root.findViewById(R.id.cars_header).setVisibility(View.VISIBLE);
        root.findViewById(R.id.car_pager_host).setVisibility(View.VISIBLE);

        pager = root.findViewById(R.id.car_pager);
        prev = root.findViewById(R.id.car_prev);
        next = root.findViewById(R.id.car_next);

        layoutManager = new LinearLayoutManager(
                host.requireContext(), LinearLayoutManager.HORIZONTAL, false);

        pager.setLayoutManager(layoutManager);
        new PagerSnapHelper().attachToRecyclerView(pager);

        adapter = new CarAdapter();
        pager.setAdapter(adapter);

        host.getChildFragmentManager().setFragmentResultListener(
                PICKER_KEY,
                host.getViewLifecycleOwner(),
                (key, result) -> scrollToYear(result.getInt(yearPickerSheet.YEAR)));

        pager.setClipToPadding(true);
        pager.setClipChildren(true);
        pager.setPadding(0, 0, 0, 0);

        ViewGroup hostView = root.findViewById(R.id.car_pager_host);
        hostView.setClipToPadding(true);
        hostView.setClipChildren(true);

        pager.post(() -> {
            if (!host.isAdded() || pager == null || adapter == null) {
                return;
            }

            int width = pager.getWidth() - pager.getPaddingLeft() - pager.getPaddingRight();
            adapter.setPageWidth(width);
        });

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
    }

    private void openYearPicker(int selectedYear) {
        if (!host.isAdded()) {
            return;
        }

        yearPickerSheet.show(host.getChildFragmentManager(),
                new ArrayList<>(years), selectedYear, PICKER_KEY);
    }

    private void scrollToYear(int year) {
        int position = years.indexOf(year);

        if (pager != null && position >= 0) {
            pager.scrollToPosition(position);
            updateArrows();
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

    // ---------- photos and results are loaded for the pages that are about to be seen ----------

    private void ensure(int year) {
        if (photoRequested.add(year)) {
            probePhoto(year, 0, new ArrayList<>());
        }

        if (year >= FIRST_CHAMPIONSHIP && standingsRequested.add(year)) {
            loadStanding(year);
        }
    }

    private List<String> photoIds(int year) {
        String yearId = yearTeam.get(year);

        List<String> ids = new ArrayList<>(
                teamCarCatalog.candidates(yearId != null ? yearId : teamId, year));

        for (String id : teamCarCatalog.candidates(teamId, year)) {
            if (!ids.contains(id)) {
                ids.add(id);
            }
        }

        return ids;
    }

    private void probePhoto(int year, int index, List<String> tried) {
        List<String> ids = photoIds(year);

        if (index >= ids.size()) {
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

    private void loadStanding(int year) {
        jolpicaSeasons.constructorStandings(year, response -> {
            if (!host.isAdded()) {
                return;
            }

            if (response == null) {
                standingsRequested.remove(year);
                Log.w(TAG, "Constructor standings are not available for " + year);
                return;
            }

            try {
                readStanding(year, response);
            } catch (JSONException error) {
                Log.w(TAG, "Unexpected standings response for " + year, error);
            }

            refresh(year);
        });
    }

    private void readStanding(int year, JSONObject response) throws JSONException {
        JSONArray lists = response
                .getJSONObject("MRData")
                .getJSONObject("StandingsTable")
                .optJSONArray("StandingsLists");

        if (lists == null) {
            return;
        }

        String yearId = yearTeam.get(year);
        String wanted = teamCarCatalog.normalize(yearId != null ? yearId : teamId);

        for (int i = 0; i < lists.length(); i++) {
            JSONArray entries = lists.getJSONObject(i).optJSONArray("ConstructorStandings");

            if (entries == null) {
                continue;
            }

            for (int j = 0; j < entries.length(); j++) {
                JSONObject entry = entries.getJSONObject(j);
                JSONObject constructor = entry.optJSONObject("Constructor");

                if (constructor == null) {
                    continue;
                }

                if (wanted.equals(teamCarCatalog.normalize(constructor.optString("constructorId")))) {
                    standings.put(year, new String[]{
                            entry.optString("positionText", DASH),
                            entry.optString("points", DASH),
                            entry.optString("wins", DASH)
                    });

                    return;
                }
            }
        }

        Log.w(TAG, "TEAM NOT IN STANDINGS " + year + " " + wanted);
    }

    private void refresh(int year) {
        if (pager == null) {
            return;
        }

        // never inside a layout pass: the answer may come at once from memory
        pager.post(() -> {
            if (!host.isAdded() || adapter == null) {
                return;
            }

            int position = years.indexOf(year);

            if (position >= 0) {
                adapter.notifyItemChanged(position);
            }
        });
    }

    // ---------- pages ----------

    private final class CarAdapter extends RecyclerView.Adapter<CarAdapter.Holder> {

        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            View view = inflater.inflate(R.layout.item_team_car_page, parent, false);

            int width = pageWidth > 0 ? pageWidth : parent.getWidth();
            if (width > 0) {
                view.setLayoutParams(new RecyclerView.LayoutParams(
                        width, ViewGroup.LayoutParams.WRAP_CONTENT));
            }

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

            // this page and the next two: the data is ready before the user gets there
            for (int step = 0; step < 3 && position + step < years.size(); step++) {
                ensure(years.get(position + step));
            }

            if (pageWidth > 0) {
                ViewGroup.LayoutParams params = holder.itemView.getLayoutParams();
                if (params.width != pageWidth) {
                    params.width = pageWidth;
                    holder.itemView.setLayoutParams(params);
                }
            }

            holder.year.setText(String.valueOf(year));
            holder.year.setOnClickListener(v -> openYearPicker(year));

            String yearId = yearTeam.get(year);
            boolean predecessor = yearId != null
                    && !teamCarCatalog.normalize(yearId).equals(teamCarCatalog.normalize(teamId));
            String former = predecessor ? teamCarCatalog.formerName(yearId) : null;

            holder.caption.setText(former != null ? former : (teamName != null ? teamName : ""));

            paint(holder);

            Uri uri = uris.get(year);

            holder.missing.setVisibility(View.GONE);

            if (uri != null) {
                GlideApp.with(holder.photo)
                        .load(uri)
                        .transition(DrawableTransitionOptions.withCrossFade())
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .into(holder.photo);
            } else {
                GlideApp.with(holder.photo).clear(holder.photo);

                if (photoChecked.contains(year)) {
                    holder.photo.setImageResource(R.drawable.placeholder_car);
                } else {
                    holder.photo.setImageDrawable(null);
                }
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

        void setPageWidth(int width) {
            if (width > 0 && pageWidth != width) {
                pageWidth = width;
                notifyDataSetChanged();
            }
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