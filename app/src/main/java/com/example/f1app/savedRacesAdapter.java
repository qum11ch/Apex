package com.example.f1app;

import static com.example.f1app.MainActivity.checkLightTheme;
import static com.example.f1app.MainActivity.getStringByName;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Locale;

/**
 * One adapter, two layouts (rows and tiles) and five race states. The data is already complete here,
 * so binding never touches the network.
 */
public class savedRacesAdapter extends ListAdapter<savedRaceCard, savedRacesAdapter.DataHolder> {

    public enum ViewMode {
        TILES,
        LIST
    }

    public interface Listener {
        void onOpen(savedRaceCard race);

        void onDelete(savedRaceCard race);
    }

    private static final String DASH = "\u2014";

    private final Listener listener;
    private ViewMode viewMode = ViewMode.LIST;

    public savedRacesAdapter(@NonNull Listener listener) {
        super(DIFF);
        this.listener = listener;
    }

    public void setViewMode(@NonNull ViewMode mode) {
        if (mode == viewMode) {
            return;
        }

        viewMode = mode;
        notifyDataSetChanged(); // every holder has to be recreated with the other layout
    }

    private static final DiffUtil.ItemCallback<savedRaceCard> DIFF =
            new DiffUtil.ItemCallback<savedRaceCard>() {
                @Override
                public boolean areItemsTheSame(@NonNull savedRaceCard a, @NonNull savedRaceCard b) {
                    return a.key.equals(b.key);
                }

                @Override
                public boolean areContentsTheSame(@NonNull savedRaceCard a, @NonNull savedRaceCard b) {
                    return a.fingerprint().equals(b.fingerprint());
                }
            };

    @Override
    public int getItemViewType(int position) {
        return viewMode.ordinal();
    }

    @NonNull
    @Override
    public DataHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int layout = viewType == ViewMode.TILES.ordinal()
                ? R.layout.item_saved_race_tile
                : R.layout.item_saved_race;

        return new DataHolder(LayoutInflater.from(parent.getContext()).inflate(layout, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull DataHolder holder, int position) {
        savedRaceCard race = getItem(position);
        Context context = holder.itemView.getContext();
        Instant now = Instant.now();
        savedRaceCard.State state = race.stateAt(now);

        holder.raceName.setText(displayName(context, race));
        holder.saveDate.setText(context.getString(R.string.saved_race_text) + " " + race.saveDate);

        bindPlace(holder, race);
        bindRound(holder, race);
        bindSprint(holder, race);

        if (!race.scheduleKnown) {
            holder.state.setVisibility(View.GONE);
            holder.podiumGroup.setVisibility(View.GONE);
            holder.countdownGroup.setVisibility(View.GONE);
            setVisible(holder.statsGroup, false);
            setVisible(holder.summary, false);
            setVisible(holder.dates, false);
        } else {
            bindState(holder.state, state);
            bindDates(holder, race);
            bindBody(holder, race, state, now);
        }

        holder.itemView.setOnClickListener(v -> listener.onOpen(race));
        holder.itemView.setOnLongClickListener(v -> {
            listener.onDelete(race);
            return true;
        });
    }

    // ---------- header ----------

    private static String displayName(Context context, savedRaceCard race) {
        String name = race.raceName;

        try {
            int id = getStringByName(race.raceName.toLowerCase(Locale.ROOT).replaceAll("\\s+", "_") + "_text");

            if (id != 0) {
                name = context.getString(id);
            }
        } catch (RuntimeException ignored) {
            // no translation for this race: show the stored name
        }

        return name + " " + race.raceSeason;
    }

    private static void bindPlace(DataHolder holder, savedRaceCard race) {
        StringBuilder place = new StringBuilder();

        if (!savedRaceCard.isMissing(race.circuitName)) {
            place.append(race.circuitName);
        }

        if (!savedRaceCard.isMissing(race.country)) {
            if (place.length() > 0) {
                place.append(" \u00B7 ");
            }
            place.append(race.country);
        }

        holder.place.setText(place);
        holder.place.setVisibility(place.length() > 0 ? View.VISIBLE : View.GONE);
    }

    private static void bindRound(DataHolder holder, savedRaceCard race) {
        if (race.round > 0) {
            holder.round.setText(holder.itemView.getContext().getString(R.string.race_round_format, race.round));
            holder.round.setVisibility(View.VISIBLE);
        } else {
            holder.round.setVisibility(View.GONE);
        }
    }

    private static void bindSprint(DataHolder holder, savedRaceCard race) {
        holder.sprint.setVisibility(race.scheduleKnown && race.isSprintWeekend() ? View.VISIBLE : View.GONE);
    }

    private static void bindState(TextView view, savedRaceCard.State state) {
        int label;
        int text;
        int background;

        switch (state) {
            case IN_PROGRESS:
                label = R.string.race_state_live;
                text = R.color.race_state_live;
                background = R.color.race_state_live_bg;
                break;
            case RESULTS_PENDING:
                label = R.string.race_state_pending;
                text = R.color.race_state_pending;
                background = R.color.race_state_pending_bg;
                break;
            case CONCLUDED:
                label = R.string.race_state_finished;
                text = R.color.race_state_finished;
                background = R.color.race_state_finished_bg;
                break;
            case CANCELED:
                label = R.string.canceled_text;
                text = R.color.race_state_canceled;
                background = R.color.race_state_canceled_bg;
                break;
            default:
                label = R.string.race_state_upcoming;
                text = R.color.race_state_upcoming;
                background = R.color.race_state_upcoming_bg;
                break;
        }

        Context context = view.getContext();
        view.setText(label);
        view.setTextColor(ContextCompat.getColor(context, text));
        view.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(context, background)));
        view.setVisibility(View.VISIBLE);
    }

    private static void bindDates(DataHolder holder, savedRaceCard race) {
        String range = dateRange(race);
        holder.dates.setText(range);
        holder.dates.setVisibility(range.isEmpty() ? View.GONE : View.VISIBLE);
    }

    private static String dateRange(savedRaceCard race) {
        LocalDate end = race.raceLocalDate();

        if (end == null) {
            return "";
        }

        LocalDate start = race.weekendStartDate();
        Locale locale = Locale.getDefault();
        DateTimeFormatter full = DateTimeFormatter.ofPattern("d MMM", locale);

        if (start == null || start.equals(end)) {
            return end.format(full);
        }

        if (start.getMonth() == end.getMonth() && start.getYear() == end.getYear()) {
            return start.getDayOfMonth() + "\u2013" + end.format(full);
        }

        return start.format(full) + " \u2013 " + end.format(full);
    }

    // ---------- state dependent body ----------

    private void bindBody(DataHolder holder, savedRaceCard race, savedRaceCard.State state, Instant now) {
        Context context = holder.itemView.getContext();

        if (state == savedRaceCard.State.CONCLUDED && race.hasPodium()) {
            holder.podiumGroup.setVisibility(View.VISIBLE);
            holder.countdownGroup.setVisibility(View.GONE);
            bindPodium(holder, race);
        } else {
            holder.podiumGroup.setVisibility(View.GONE);
            holder.countdownGroup.setVisibility(
                    state == savedRaceCard.State.CONCLUDED ? View.GONE : View.VISIBLE
            );
            clearPodium(holder);
            bindCountdown(holder, race, state, now);
        }

        if (holder.statsGroup != null) {
            bindStats(holder, race, state);
        }

        if (holder.summary != null) {
            bindSummary(holder, race, state);
        }

        if (holder.raceTime != null) {
            boolean showTime = state != savedRaceCard.State.CONCLUDED
                    && state != savedRaceCard.State.CANCELED
                    && race.hasRaceTime()
                    && race.raceStart() != null;

            if (showTime) {
                String when = DateTimeFormatter.ofPattern("EEE, HH:mm", Locale.getDefault())
                        .format(race.raceStart().atZone(ZoneId.systemDefault()));
                holder.raceTime.setText(context.getString(R.string.race_start_local, when));
                holder.raceTime.setVisibility(View.VISIBLE);
            } else {
                holder.raceTime.setVisibility(View.GONE);
            }
        }
    }

    private void bindCountdown(DataHolder holder, savedRaceCard race, savedRaceCard.State state, Instant now) {
        Resources resources = holder.itemView.getResources();
        String value;
        String label;

        switch (state) {
            case IN_PROGRESS:
                value = resources.getString(R.string.race_countdown_now_value);
                label = resources.getString(R.string.race_countdown_now_label);
                break;
            case RESULTS_PENDING:
                value = resources.getString(R.string.race_countdown_pending_value);
                label = resources.getString(R.string.race_countdown_pending_label);
                break;
            case CANCELED:
                value = resources.getString(R.string.canceled_text);
                label = resources.getString(R.string.race_countdown_canceled_label);
                break;
            default:
                LocalDate raceDay = race.raceLocalDate();

                if (raceDay == null) {
                    value = DASH;
                    label = "";
                } else {
                    long days = ChronoUnit.DAYS.between(LocalDate.now(), raceDay);

                    if (days <= 0) {
                        value = resources.getString(R.string.race_countdown_today_value);
                        label = resources.getString(R.string.race_countdown_today_label);
                    } else {
                        value = String.valueOf(days);
                        label = resources.getQuantityString(R.plurals.race_days_until, (int) days);
                    }
                }
                break;
        }

        holder.countdownValue.setText(value);
        holder.countdownLabel.setText(label);
        holder.countdownLabel.setVisibility(label.isEmpty() ? View.GONE : View.VISIBLE);
    }

    private void bindPodium(DataHolder holder, savedRaceCard race) {
        Context context = holder.itemView.getContext();

        holder.firstCode.setText(race.winnerCode);
        holder.secondCode.setText(race.secondCode);
        holder.thirdCode.setText(race.thirdCode);

        if (holder.stripedFirst != null && !checkLightTheme(context)) {
            holder.stripedFirst.setBackground(ContextCompat.getDrawable(context, R.drawable.background_striped_lines_item_night));
            holder.stripedSecond.setBackground(ContextCompat.getDrawable(context, R.drawable.background_striped_lines_item_night));
            holder.stripedThird.setBackground(ContextCompat.getDrawable(context, R.drawable.background_striped_lines_item_night));
        }

        StorageReference root = FirebaseStorage.getInstance().getReference();
        loadDriver(holder.firstImage, root, race.winnerCode, race.raceSeason);
        loadDriver(holder.secondImage, root, race.secondCode, race.raceSeason);
        loadDriver(holder.thirdImage, root, race.thirdCode, race.raceSeason);
    }

    private static void loadDriver(ImageView view, StorageReference root, String code, String season) {
        GlideApp.with(view)
                .load(root.child("drivers/" + code.toLowerCase(Locale.ROOT) + "_" + season + ".png"))
                .transition(DrawableTransitionOptions.withCrossFade())
                .error(R.drawable.placeholder_driver)
                .into(view);
    }

    private static void clearPodium(DataHolder holder) {
        // A recycled holder must not show the previous card's drivers when its request finishes late.
        GlideApp.with(holder.firstImage).clear(holder.firstImage);
        GlideApp.with(holder.secondImage).clear(holder.secondImage);
        GlideApp.with(holder.thirdImage).clear(holder.thirdImage);
    }

    private void bindStats(DataHolder holder, savedRaceCard race, savedRaceCard.State state) {
        Context context = holder.itemView.getContext();
        String v1;
        String v2;
        String v3;
        int l1;
        int l2;
        int l3;

        if (state == savedRaceCard.State.CONCLUDED) {
            v1 = value(race.fastestLapTime);
            v2 = value(race.fastestLapDriver);
            l1 = R.string.race_stat_fastest_lap;
            l2 = R.string.race_stat_fastest_driver;

            if (race.isSprintWeekend()) {
                v3 = value(race.sprintWinnerCode);
                l3 = R.string.race_stat_sprint_winner;
            } else {
                v3 = value(race.lapsCount);
                l3 = R.string.race_stat_laps;
            }
        } else {
            v1 = savedRaceCard.isMissing(race.length)
                    ? DASH
                    : context.getString(R.string.race_unit_km, race.length);
            v2 = value(race.lapsCount);
            v3 = value(race.turnsCount);
            l1 = R.string.race_stat_length;
            l2 = R.string.race_stat_laps;
            l3 = R.string.race_stat_turns;
        }

        boolean empty = DASH.equals(v1) && DASH.equals(v2) && DASH.equals(v3);
        holder.statsGroup.setVisibility(empty ? View.GONE : View.VISIBLE);

        holder.stat1Value.setText(v1);
        holder.stat2Value.setText(v2);
        holder.stat3Value.setText(v3);
        holder.stat1Label.setText(l1);
        holder.stat2Label.setText(l2);
        holder.stat3Label.setText(l3);
    }

    private void bindSummary(DataHolder holder, savedRaceCard race, savedRaceCard.State state) {
        Context context = holder.itemView.getContext();
        String summary = "";

        if (state == savedRaceCard.State.CONCLUDED) {
            if (!savedRaceCard.isMissing(race.fastestLapTime)) {
                summary = context.getString(R.string.race_summary_fastest, race.fastestLapTime);
            }
        } else if (!savedRaceCard.isMissing(race.length) && !savedRaceCard.isMissing(race.lapsCount)) {
            summary = context.getString(R.string.race_summary_circuit, race.length, race.lapsCount);
        }

        holder.summary.setText(summary);
        holder.summary.setVisibility(summary.isEmpty() ? View.GONE : View.VISIBLE);
    }

    private static String value(@Nullable String text) {
        return savedRaceCard.isMissing(text) ? DASH : text.trim();
    }

    private static void setVisible(@Nullable View view, boolean visible) {
        if (view != null) {
            view.setVisibility(visible ? View.VISIBLE : View.GONE);
        }
    }

    // ---------- holder ----------

    public static class DataHolder extends RecyclerView.ViewHolder {
        final TextView raceName, place, dates, round, state, sprint, saveDate;
        final View podiumGroup, countdownGroup;
        final TextView countdownValue, countdownLabel, firstCode, secondCode, thirdCode;
        final ImageView firstImage, secondImage, thirdImage;

        // only in the row layout
        final View statsGroup;
        final TextView raceTime, stat1Value, stat1Label, stat2Value, stat2Label, stat3Value, stat3Label;
        final View stripedFirst, stripedSecond, stripedThird;

        // only in the tile layout
        final TextView summary;

        public DataHolder(@NonNull View itemView) {
            super(itemView);
            raceName = itemView.findViewById(R.id.raceName);
            place = itemView.findViewById(R.id.race_place);
            dates = itemView.findViewById(R.id.race_dates);
            round = itemView.findViewById(R.id.race_round);
            state = itemView.findViewById(R.id.race_state);
            sprint = itemView.findViewById(R.id.sprint_badge);
            saveDate = itemView.findViewById(R.id.save_date);

            podiumGroup = itemView.findViewById(R.id.podium_group);
            countdownGroup = itemView.findViewById(R.id.countdown_group);
            countdownValue = itemView.findViewById(R.id.countdown_value);
            countdownLabel = itemView.findViewById(R.id.countdown_label);

            firstCode = itemView.findViewById(R.id.firstPlace_code);
            secondCode = itemView.findViewById(R.id.secondPlace_code);
            thirdCode = itemView.findViewById(R.id.thirdPlace_code);
            firstImage = itemView.findViewById(R.id.firstPlace_image);
            secondImage = itemView.findViewById(R.id.secondPlace_image);
            thirdImage = itemView.findViewById(R.id.thirdPlace_image);

            statsGroup = itemView.findViewById(R.id.stats_group);
            raceTime = itemView.findViewById(R.id.race_time);
            stat1Value = itemView.findViewById(R.id.stat1_value);
            stat1Label = itemView.findViewById(R.id.stat1_label);
            stat2Value = itemView.findViewById(R.id.stat2_value);
            stat2Label = itemView.findViewById(R.id.stat2_label);
            stat3Value = itemView.findViewById(R.id.stat3_value);
            stat3Label = itemView.findViewById(R.id.stat3_label);
            stripedFirst = itemView.findViewById(R.id.stripedImg_first);
            stripedSecond = itemView.findViewById(R.id.stripedImg_second);
            stripedThird = itemView.findViewById(R.id.stripedImg_third);

            summary = itemView.findViewById(R.id.race_summary);
        }
    }
}
