package com.example.f1app;

import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.FirebaseDatabase;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import android.graphics.Color;

import androidx.annotation.NonNull;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

/** The classification of a session from the data that is already loaded: no waiting, no extra request. */
public class sessionResultsSheet extends neuroBaseSheet {

    private static final String TAG = "sessionResultsSheet";
    private static final String ARG_TITLE = "title";
    private static final String ARG_KEY = "key";
    private static final String ARG_JSON = "json";
    private static final int NEUTRAL = 0xFF8E8E93;
    private static final Map<String, Integer> BRIGHT = new HashMap<>();

    private static final class Entry {
        String position = "";
        String name = "";
        String team = "";
        String teamId = "";
        String right = "";
        String sub = "";
        int color = NEUTRAL;
    }

    private final List<Entry> entries = new ArrayList<>();
    private RecyclerView.Adapter<?> adapter;

    static void show(FragmentManager manager, String title, String key, JSONArray results) {
        if (results == null || manager.findFragmentByTag(TAG) != null || manager.isStateSaved()) {
            return;
        }

        Bundle args = new Bundle();
        args.putString(ARG_TITLE, title);
        args.putString(ARG_KEY, key);
        args.putString(ARG_JSON, results.toString());

        sessionResultsSheet sheet = new sessionResultsSheet();
        sheet.setArguments(args);
        sheet.show(manager, TAG);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle state) {
        Bundle a = requireArguments();
        parse(a.getString(ARG_JSON, "[]"), "quali_event".equals(a.getString(ARG_KEY, "")));

        LinearLayout root = root();

        TextView title = text(a.getString(ARG_TITLE, ""), 22, R.color.text_color_main);
        title.setTypeface(title.getTypeface(), Typeface.BOLD);
        root.addView(title);

        RecyclerView list = new RecyclerView(requireContext());
        list.setLayoutManager(new LinearLayoutManager(requireContext()));
        list.setOverScrollMode(View.OVER_SCROLL_NEVER);
        list.setClipToPadding(false);

        final int listHeight = Math.min(dp(480),
                Math.round(getResources().getDisplayMetrics().heightPixels * 0.6f));
        LinearLayout.LayoutParams listParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, listHeight);
        listParams.topMargin = dp(12);
        root.addView(list, listParams);

        adapter = new Adapter();
        list.setAdapter(adapter);
        loadColors();

        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets safe = insets.getInsets(WindowInsetsCompat.Type.systemBars()
                    | WindowInsetsCompat.Type.displayCutout());

            v.setPadding(dp(24) + safe.left, dp(12), dp(24) + safe.right, 0);

            int bottom = dp(20) + safe.bottom;
            list.setPadding(0, 0, 0, bottom);

            ViewGroup.LayoutParams params = list.getLayoutParams();
            params.height = listHeight + bottom;
            list.setLayoutParams(params);

            return insets;
        });
        root.post(() -> ViewCompat.requestApplyInsets(root));

        return root;
    }

    // ---------- data ----------

    private void parse(String json, boolean qualifying) {
        entries.clear();

        try {
            JSONArray array = new JSONArray(json);

            for (int i = 0; i < array.length(); i++) {
                JSONObject o = array.optJSONObject(i);

                if (o == null) {
                    continue;
                }

                JSONObject driver = o.optJSONObject("Driver");
                JSONObject constructor = o.optJSONObject("Constructor");

                Entry e = new Entry();
                e.position = o.optString("position", String.valueOf(i + 1));
                e.name = driver == null ? "" : driver.optString("familyName");
                e.team = constructor == null ? "" : constructor.optString("name");
                e.teamId = constructor == null ? "" : constructor.optString("constructorId");

                if (qualifying) {
                    String q1 = o.optString("Q1");
                    String q2 = o.optString("Q2");
                    String q3 = o.optString("Q3");

                    if (!q3.isEmpty()) {
                        e.right = q3;
                        e.sub = "Q3";
                    } else if (!q2.isEmpty()) {
                        e.right = q2;
                        e.sub = "Q2";
                    } else {
                        e.right = q1;
                        e.sub = q1.isEmpty() ? "" : "Q1";
                    }
                } else {
                    JSONObject time = o.optJSONObject("Time");
                    e.right = time != null ? time.optString("time") : o.optString("status");

                    String points = o.optString("points");
                    e.sub = points.isEmpty() || "0".equals(points) ? "" : "+" + points;
                }

                Integer known = BRIGHT.get(e.teamId);
                e.color = known != null ? known : colorOf(e.teamId, null);

                entries.add(e);
            }
        } catch (org.json.JSONException ignored) {
            // an empty list is shown
        }
    }

    // ---------- list ----------

    private final class Adapter extends RecyclerView.Adapter<Holder> {

        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LinearLayout row = new LinearLayout(parent.getContext());
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(dp(4), dp(10), dp(4), dp(10));
            row.setLayoutParams(new RecyclerView.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

            TextView position = text("", 15, R.color.text_color_secondary);
            position.setTypeface(position.getTypeface(), Typeface.BOLD);
            position.setGravity(Gravity.CENTER);
            row.addView(position, new LinearLayout.LayoutParams(dp(32), ViewGroup.LayoutParams.WRAP_CONTENT));

            View bar = new View(parent.getContext());
            LinearLayout.LayoutParams barParams = new LinearLayout.LayoutParams(dp(4), dp(36));
            barParams.setMarginStart(dp(4));
            barParams.setMarginEnd(dp(12));
            row.addView(bar, barParams);

            LinearLayout texts = new LinearLayout(parent.getContext());
            texts.setOrientation(LinearLayout.VERTICAL);
            TextView name = text("", 16, R.color.text_color_main);
            name.setTypeface(name.getTypeface(), Typeface.BOLD);
            TextView team = text("", 13, R.color.text_color_secondary);
            texts.addView(name);
            texts.addView(team);
            row.addView(texts, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

            LinearLayout end = new LinearLayout(parent.getContext());
            end.setOrientation(LinearLayout.VERTICAL);
            end.setGravity(Gravity.END);
            TextView right = text("", 15, R.color.text_color_main);
            right.setTypeface(right.getTypeface(), Typeface.BOLD);
            TextView sub = text("", 12, R.color.text_color_secondary);
            end.addView(right);
            end.addView(sub);
            row.addView(end, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

            return new Holder(row, position, bar, name, team, right, sub);
        }

        @Override
        public void onBindViewHolder(@NonNull Holder h, int index) {
            Entry e = entries.get(index);

            h.position.setText(e.position);
            h.bar.setBackground(surface(e.color, 2));
            h.name.setText(e.name);
            h.team.setText(e.team);
            h.right.setText(e.right);
            h.sub.setText(e.sub);
            h.sub.setVisibility(e.sub.isEmpty() ? View.GONE : View.VISIBLE);
        }

        @Override
        public int getItemCount() {
            return entries.size();
        }
    }

    private static final class Holder extends RecyclerView.ViewHolder {
        final TextView position, name, team, right, sub;
        final View bar;

        Holder(@NonNull View itemView, TextView position, View bar, TextView name, TextView team,
               TextView right, TextView sub) {
            super(itemView);
            this.position = position;
            this.bar = bar;
            this.name = name;
            this.team = team;
            this.right = right;
            this.sub = sub;
        }
    }

    /** The bright colour of the team, like in the compare picker; old teams have their own fallbacks. */
    private static int colorOf(String teamId, String hex) {
        try {
            return Color.parseColor(pastTeamPalette.resolve(teamId, hex));
        } catch (RuntimeException error) {
            return NEUTRAL;
        }
    }

    private void loadColors() {
        FirebaseDatabase.getInstance().getReference().child("constructors")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (!isAdded()) {
                            return;
                        }

                        for (Entry e : entries) {
                            if (e.teamId.isEmpty()) {
                                continue;
                            }

                            String hex = snapshot.child(e.teamId).child("color").getValue(String.class);
                            e.color = colorOf(e.teamId, hex);
                            BRIGHT.put(e.teamId, e.color);
                        }

                        if (adapter != null) {
                            adapter.notifyDataSetChanged();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        // the colours from the palette stay
                    }
                });
    }
}