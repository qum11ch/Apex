package com.example.f1app;

import static com.example.f1app.MainActivity.getStringByName;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class driverResultsAdapter extends RecyclerView.Adapter<driverResultsAdapter.DataHolder> {

    private final Context context;
    private final List<driverResultsData> dataList;

    public driverResultsAdapter(Context context, List<driverResultsData> datum) {
        this.context = context;
        this.dataList = datum;
    }

    @NonNull
    @Override
    public DataHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_driver_result, parent, false);
        return new DataHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DataHolder holder, int position) {
        driverResultsData datum = dataList.get(position);

        holder.bottomLine.setVisibility(position == dataList.size() - 1 ? View.GONE : View.VISIBLE);

        holder.raceName.setText(localizedRaceName(datum.getRaceName()));

        if (datum.getRound() > 0) {
            holder.roundText.setText(context.getString(R.string.round) + " " + datum.getRound());
            holder.roundText.setVisibility(View.VISIBLE);
        } else {
            holder.roundText.setVisibility(View.GONE);
        }

        String code = datum.getDriverResult();

        holder.gridText.setText(gridPosition(code));
        resultChipBinder.bind(context, holder.driverResults, code);
    }

    @Override
    public int getItemCount() {
        return dataList.size();
    }

    private String localizedRaceName(String raceName) {
        String key = raceName.toLowerCase().replaceAll("\\s+", "_") + "_locality";
        int id = getStringByName(key);
        return id != 0 ? context.getString(id) : raceName;
    }

    /** Starting position from "start-finish"; a dash when it is not known (0 means the pit lane). */
    private String gridPosition(String code) {
        if (code == null) {
            return "\u2013";
        }

        String[] parts = code.trim().split("-");

        if (parts.length != 2) {
            return "\u2013";
        }

        try {
            int start = Integer.parseInt(parts[0]);
            return start > 0 ? String.valueOf(start) : "\u2013";
        } catch (NumberFormatException error) {
            return "\u2013";
        }
    }

    public static class DataHolder extends RecyclerView.ViewHolder {
        final TextView roundText;
        final TextView raceName;
        final TextView gridText;
        final TextView driverResults;
        final View bottomLine;

        public DataHolder(@NonNull View itemView) {
            super(itemView);
            roundText = itemView.findViewById(R.id.round_text);
            raceName = itemView.findViewById(R.id.raceName);
            gridText = itemView.findViewById(R.id.grid_text);
            driverResults = itemView.findViewById(R.id.driverResults);
            bottomLine = itemView.findViewById(R.id.bottomLine);
        }
    }
}
