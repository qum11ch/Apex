package com.example.f1app;

import static com.example.f1app.MainActivity.getStringByName;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

/**
 * Results table for any number of drivers (replaces teamDriversResultsAdapter and
 * teamTripleDriversResultsAdapter). Every cell gets its own background, so colours never leak between cells.
 */
public class teamResultsAdapter extends RecyclerView.Adapter<teamResultsAdapter.Holder> {

    private final Context context;
    private final List<teamRaceRow> rows;
    private final int driverCount;

    public teamResultsAdapter(Context context, List<teamRaceRow> rows, int driverCount) {
        this.context = context;
        this.rows = rows;
        this.driverCount = Math.max(1, driverCount);
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.item_team_result_row, parent, false);
        Holder holder = new Holder(view);

        LinearLayout.LayoutParams cellsParams =
                (LinearLayout.LayoutParams) holder.cells.getLayoutParams();
        cellsParams.weight = driverCount;
        holder.cells.setLayoutParams(cellsParams);

        for (int i = 0; i < driverCount; i++) {
            View cell = inflater.inflate(R.layout.item_team_result_cell, holder.cells, false);
            holder.cells.addView(cell, new LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            holder.chips.add((TextView) cell.findViewById(R.id.result_chip));
        }

        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        teamRaceRow row = rows.get(position);

        holder.raceName.setText(localizedRaceName(row.raceName));
        holder.bottomLine.setVisibility(position == rows.size() - 1 ? View.GONE : View.VISIBLE);

        for (int i = 0; i < driverCount; i++) {
            String code = i < row.results.size() ? row.results.get(i) : "null";
            bindChip(holder.chips.get(i), code);
        }
    }

    @Override
    public int getItemCount() {
        return rows.size();
    }

    private String localizedRaceName(String raceName) {
        String key = raceName.toLowerCase().replaceAll("\\s+", "_") + "_locality";
        int id = getStringByName(key);
        return id != 0 ? context.getString(id) : raceName;
    }

    private void bindChip(TextView chip, String rawCode) {
        String code = rawCode == null ? "null" : rawCode.trim();

        int fill = color(R.color.light_grey);
        int stroke = color(R.color.grey);
        int textColor = color(R.color.dark_grey);
        String text = "-";
        boolean pole = false;

        switch (code) {
            case "CND":
                fill = color(R.color.orange);
                stroke = fill;
                textColor = color(R.color.white);
                text = context.getString(R.string.cnd_text);
                break;
            case "NP":
                fill = color(R.color.white);
                text = context.getString(R.string.dns_text);
                break;
            case "N/C":
                text = context.getString(R.string.n_c_text);
                break;
            case "null":
                stroke = fill;
                break;
            default:
                String[] parts = code.split("-");

                if (parts.length != 2) {
                    stroke = fill;
                    break;
                }

                String start = parts[0];
                String finish = parts[1];

                switch (finish) {
                    case "R":
                        fill = color(R.color.pink);
                        text = context.getString(R.string.ret_text);
                        pole = "1".equals(start);
                        break;
                    case "W":
                        text = context.getString(R.string.wd_text);
                        break;
                    case "D":
                        fill = color(R.color.black);
                        textColor = color(R.color.white);
                        text = context.getString(R.string.dsq_text);
                        break;
                    default:
                        try {
                            int position = Integer.parseInt(finish);

                            if (position == 1) fill = color(R.color.light_gold);
                            else if (position == 2) fill = color(R.color.light_silver);
                            else if (position == 3) fill = color(R.color.light_bronze);
                            else if (position <= 10) fill = color(R.color.light_green);
                            else fill = color(R.color.light_purple);

                            text = finish;
                            pole = "1".equals(start);
                        } catch (NumberFormatException error) {
                            stroke = fill;
                        }
                        break;
                }
                break;
        }

        GradientDrawable background = new GradientDrawable();
        background.setCornerRadius(dp(7));
        background.setColor(fill);
        background.setStroke(dp(1), stroke);

        chip.setBackground(background);
        chip.setText(text);
        chip.setTextColor(pole ? color(R.color.purple) : textColor);
        chip.getPaint().setFakeBoldText(pole);
    }

    private int color(int res) {
        return ContextCompat.getColor(context, res);
    }

    private int dp(int value) {
        return Math.round(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, value, context.getResources().getDisplayMetrics()));
    }

    static final class Holder extends RecyclerView.ViewHolder {
        final TextView raceName;
        final LinearLayout cells;
        final View bottomLine;
        final List<TextView> chips = new ArrayList<>();

        Holder(@NonNull View itemView) {
            super(itemView);
            raceName = itemView.findViewById(R.id.raceName);
            cells = itemView.findViewById(R.id.team_row_cells);
            bottomLine = itemView.findViewById(R.id.bottomLine);
        }
    }
}
