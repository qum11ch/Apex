package com.example.f1app;

import static com.example.f1app.MainActivity.checkLightTheme;
import static com.example.f1app.MainActivity.getStringByName;
import static com.example.f1app.teamsStandingsActivity.localizeLocality;

import android.content.Context;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Rows and tiles share the look of the schedule cards: the round in the accent colour, the date pill,
 * ShareTech info lines and the same weekend states. The data is already complete here,
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

    private static final String SEPARATOR = " \u00B7 ";

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
        long now = System.currentTimeMillis();
        raceWeekendStatus.State state = race.stateAt(now);

        holder.raceName.setText(displayName(context, race));
        holder.saveDate.setText(context.getString(R.string.saved_race_text) + " " + race.saveDate);

        bindRound(holder, race);
        holder.canceled.setVisibility(race.canceled ? View.VISIBLE : View.GONE);

        if (!race.scheduleKnown) {
            // The schedule entry is missing: show only what the user saved.
            hide(holder.dates);
            hide(holder.locale);
            hide(holder.circuit);
            hide(holder.podiumGroup);
            hide(holder.statusRow);
            hide(holder.infoLine);
        } else {
            bindDates(holder, race);
            bindPlace(holder, context, race);
            bindBody(holder, context, race, state, now);
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

    private static void bindRound(DataHolder holder, savedRaceCard race) {
        if (race.round > 0) {
            holder.round.setText(String.valueOf(race.round));
            holder.roundGroup.setVisibility(View.VISIBLE);
        } else {
            holder.roundGroup.setVisibility(View.GONE);
        }
    }

    private static void bindDates(DataHolder holder, savedRaceCard race) {
        String text = datesText(race);
        holder.dates.setText(text);
        holder.dates.setVisibility(text.isEmpty() ? View.GONE : View.VISIBLE);
    }

    /** "04-06 NOV" like the schedule's date block; both months when the weekend crosses a month. */
    private static String datesText(savedRaceCard race) {
        LocalDate end = race.raceLocalDate();

        if (end == null) {
            return "";
        }

        LocalDate start = race.weekendStartDate();

        if (start == null) {
            start = end;
        }

        DateTimeFormatter day = DateTimeFormatter.ofPattern("dd");
        DateTimeFormatter month = DateTimeFormatter.ofPattern("MMM");
        String monthStart = start.format(month);
        String monthEnd = end.format(month);

        String days = start.equals(end)
                ? end.format(day)
                : start.format(day) + "-" + end.format(day);

        return days + " " + (monthStart.equals(monthEnd) ? monthStart : monthStart + "-" + monthEnd);
    }

    private static void bindPlace(DataHolder holder, Context context, savedRaceCard race) {
        String locale = localeLine(context, race);
        holder.locale.setText(locale);
        holder.locale.setVisibility(locale.isEmpty() ? View.GONE : View.VISIBLE);

        String circuit = race.circuitName;

        if (race.circuitId != null) {
            try {
                int id = getStringByName(race.circuitId + "_text");

                if (id != 0) {
                    circuit = context.getString(id);
                }
            } catch (RuntimeException ignored) {
                // keep the stored circuit name
            }
        }

        holder.circuit.setText(circuit);
        holder.circuit.setVisibility(savedRaceCard.isMissing(circuit) ? View.GONE : View.VISIBLE);
    }

    /** Same rule as the schedule lists: city and country, without a city for Monaco and Singapore. */
    private static String localeLine(Context context, savedRaceCard race) {
        String country = savedRaceCard.isMissing(race.country) ? null : race.country;
        String city = savedRaceCard.isMissing(race.locality) ? null : race.locality;

        if (country == null && city == null) {
            return "";
        }

        if (Locale.getDefault().getLanguage().equals("ru") && city != null && country != null) {
            try {
                ArrayList<String> localized = localizeLocality(city, country, context);

                if (localized != null && localized.size() > 2 && localized.get(2) != null) {
                    return localized.get(2);
                }
            } catch (RuntimeException ignored) {
                // fall back to the English names below
            }
        }

        if ("Monaco".equals(city) || "Singapore".equals(city)) {
            city = null;
        }

        if (city != null && country != null) {
            return city + ", " + country;
        }

        return country != null ? country : city;
    }

    // ---------- state dependent body ----------

    private void bindBody(
            DataHolder holder,
            Context context,
            savedRaceCard race,
            raceWeekendStatus.State state,
            long now
    ) {
        boolean showPodium = state == raceWeekendStatus.State.CONCLUDED && race.hasPodium();

        holder.podiumGroup.setVisibility(showPodium ? View.VISIBLE : View.GONE);

        if (showPodium) {
            bindPodium(holder, context, race);
        } else {
            clearPodiumImages(holder);
        }

        bindStatus(holder, context, race, state, now);
        bindInfoLine(holder, context, race, state);
    }

    private void bindPodium(DataHolder holder, Context context, savedRaceCard race) {
        holder.firstCode.setText(race.winnerCode);
        holder.secondCode.setText(race.secondCode);
        holder.thirdCode.setText(race.thirdCode);

        if (holder.firstImage == null) {
            return; // tile layout: codes only
        }

        if (!checkLightTheme(context)) {
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

    private static void clearPodiumImages(DataHolder holder) {
        // A recycled holder must not show the previous card's drivers when its request finishes late.
        if (holder.firstImage != null) {
            GlideApp.with(holder.firstImage).clear(holder.firstImage);
            GlideApp.with(holder.secondImage).clear(holder.secondImage);
            GlideApp.with(holder.thirdImage).clear(holder.thirdImage);
        }
    }

    /** The line under the circuit: what is happening with the weekend. */
    private void bindStatus(
            DataHolder holder,
            Context context,
            savedRaceCard race,
            raceWeekendStatus.State state,
            long now
    ) {
        String text = null;
        boolean live = false;

        switch (state) {
            case ONGOING: {
                raceWeekendStatus.SessionInfo current = raceWeekendStatus.liveSession(race.sessions, now);
                raceWeekendStatus.SessionInfo next = raceWeekendStatus.nextSession(race.sessions, now);

                if (current != null) {
                    live = true;
                    text = sessionName(context, current.key);
                } else if (next != null) {
                    text = context.getString(
                            R.string.next_session_text,
                            sessionName(context, next.key),
                            localTime(next.start, "EEE HH:mm")
                    );
                } else {
                    text = context.getString(R.string.awaiting_results_text);
                }
                break;
            }
            case AWAITING_RESULTS:
                text = context.getString(R.string.awaiting_results_text);
                break;
            case FUTURE:
                text = futureLine(context, race);
                break;
            default:
                break; // finished and canceled races have nothing to announce
        }

        if (text == null || text.isEmpty()) {
            holder.statusRow.setVisibility(View.GONE);
            return;
        }

        holder.statusText.setText(text);
        holder.liveDot.setVisibility(live ? View.VISIBLE : View.GONE);
        holder.statusRow.setVisibility(View.VISIBLE);
    }

    private static String futureLine(Context context, savedRaceCard race) {
        List<String> parts = new ArrayList<>();
        LocalDate raceDay = race.raceLocalDate();

        if (raceDay != null) {
            long days = ChronoUnit.DAYS.between(LocalDate.now(), raceDay);

            parts.add(days <= 0
                    ? context.getString(R.string.sr_race_day)
                    : context.getResources().getQuantityString(R.plurals.sr_in_days, (int) days, (int) days));
        }

        Long raceStart = race.raceStartMs();

        if (raceStart != null) {
            parts.add(context.getString(R.string.sr_race_at, localTime(raceStart, "EEE HH:mm")));
        }

        return join(parts);
    }

    private void bindInfoLine(
            DataHolder holder,
            Context context,
            savedRaceCard race,
            raceWeekendStatus.State state
    ) {
        List<String> parts = new ArrayList<>();

        if (race.isSprintWeekend()) {
            parts.add(context.getString(R.string.sr_sprint));
        }

        if (state == raceWeekendStatus.State.CONCLUDED) {
            if (!savedRaceCard.isMissing(race.fastestLapTime)) {
                String lap = race.fastestLapTime.trim();

                if (!savedRaceCard.isMissing(race.fastestLapDriver) && holder.firstImage != null) {
                    lap += SEPARATOR + race.fastestLapDriver.trim();
                }

                parts.add(context.getString(R.string.sr_fastest_lap, lap));
            }
        } else {
            addIfPresent(parts, context, R.string.sr_unit_km, race.length);
            addIfPresent(parts, context, R.string.sr_unit_laps, race.lapsCount);
            addIfPresent(parts, context, R.string.sr_unit_turns, race.turnsCount);
        }

        String text = join(parts);
        holder.infoLine.setText(text);
        holder.infoLine.setVisibility(text.isEmpty() ? View.GONE : View.VISIBLE);
    }

    // ---------- helpers ----------

    private static void addIfPresent(List<String> parts, Context context, int format, String value) {
        if (!savedRaceCard.isMissing(value)) {
            parts.add(context.getString(format, value.trim()));
        }
    }

    private static String join(List<String> parts) {
        StringBuilder result = new StringBuilder();

        for (String part : parts) {
            if (result.length() > 0) {
                result.append(SEPARATOR);
            }
            result.append(part);
        }

        return result.toString();
    }

    private static String sessionName(Context context, String key) {
        try {
            int id = getStringByName(key + "_text");

            if (id != 0) {
                return context.getString(id);
            }
        } catch (RuntimeException ignored) {
            // fall through
        }

        return key;
    }

    private static String localTime(long millis, String pattern) {
        return DateTimeFormatter.ofPattern(pattern, Locale.getDefault())
                .format(Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()));
    }

    private static void hide(@Nullable View view) {
        if (view != null) {
            view.setVisibility(View.GONE);
        }
    }

    // ---------- holder ----------

    public static class DataHolder extends RecyclerView.ViewHolder {
        final View roundGroup, podiumGroup, statusRow, liveDot;
        final TextView round, dates, canceled, raceName, locale, circuit, statusText, infoLine, saveDate;
        final TextView firstCode, secondCode, thirdCode;

        // photos exist only in the row layout
        final ImageView firstImage, secondImage, thirdImage;
        final View stripedFirst, stripedSecond, stripedThird;

        public DataHolder(@NonNull View itemView) {
            super(itemView);
            roundGroup = itemView.findViewById(R.id.round_group);
            round = itemView.findViewById(R.id.round);
            dates = itemView.findViewById(R.id.race_dates);
            canceled = itemView.findViewById(R.id.cnd_layout);
            raceName = itemView.findViewById(R.id.raceName);
            locale = itemView.findViewById(R.id.race_locale);
            circuit = itemView.findViewById(R.id.circuitName);

            podiumGroup = itemView.findViewById(R.id.podium_group);
            firstCode = itemView.findViewById(R.id.firstPlace_code);
            secondCode = itemView.findViewById(R.id.secondPlace_code);
            thirdCode = itemView.findViewById(R.id.thirdPlace_code);
            firstImage = itemView.findViewById(R.id.firstPlace_image);
            secondImage = itemView.findViewById(R.id.secondPlace_image);
            thirdImage = itemView.findViewById(R.id.thirdPlace_image);
            stripedFirst = itemView.findViewById(R.id.stripedImg_first);
            stripedSecond = itemView.findViewById(R.id.stripedImg_second);
            stripedThird = itemView.findViewById(R.id.stripedImg_third);

            statusRow = itemView.findViewById(R.id.status_row);
            liveDot = itemView.findViewById(R.id.live_dot);
            statusText = itemView.findViewById(R.id.status_text);
            infoLine = itemView.findViewById(R.id.info_line);
            saveDate = itemView.findViewById(R.id.save_date);
        }
    }
}
