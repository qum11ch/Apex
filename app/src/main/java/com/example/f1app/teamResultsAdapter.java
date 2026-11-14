package com.example.f1app;

import static com.example.f1app.MainActivity.getStringByName;

import android.content.Context;
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

        if (row.round > 0) {
            holder.roundText.setText(context.getString(R.string.round) + " " + row.round);
            holder.roundText.setVisibility(View.VISIBLE);
        } else {
            holder.roundText.setVisibility(View.GONE);
        }

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
        resultChipBinder.bind(context, chip, rawCode);
    }

    private int color(int res) {
        return ContextCompat.getColor(context, res);
    }

    private int dp(int value) {
        return Math.round(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, value, context.getResources().getDisplayMetrics()));
    }

    static final class Holder extends RecyclerView.ViewHolder {
        final TextView roundText;
        final TextView raceName;
        final LinearLayout cells;
        final View bottomLine;
        final List<TextView> chips = new ArrayList<>();

        Holder(@NonNull View itemView) {
            super(itemView);
            roundText = itemView.findViewById(R.id.round_text);
            raceName = itemView.findViewById(R.id.raceName);
            cells = itemView.findViewById(R.id.team_row_cells);
            bottomLine = itemView.findViewById(R.id.bottomLine);
        }
    }
}
