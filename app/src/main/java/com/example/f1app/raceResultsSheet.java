package com.example.f1app;

import android.content.Intent;
import android.os.Bundle;
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

import java.util.ArrayList;
import java.util.List;

/**
 * Bottom sheet with the earlier runnings of a Grand Prix; a tap opens the results of that year.
 * The dialog, the edge-to-edge theme and the expanded state come from neuroBaseSheet,
 * the same base the race picker sheet uses.
 */
public class raceResultsSheet extends neuroBaseSheet {

    private static final String ARG_RACE = "raceName";
    private static final String ARG_CIRCUIT = "circuitId";
    private static final String ARG_ROWS = "rows";

    static void show(FragmentManager manager, String raceName, String circuitId, List<raceHistory.Entry> entries) {
        ArrayList<String> rows = new ArrayList<>();

        for (raceHistory.Entry entry : entries) {
            rows.add(entry.year + "|" + entry.raceName + "|" + entry.first + "|" + entry.second + "|" + entry.third);
        }

        raceResultsSheet sheet = new raceResultsSheet();
        Bundle arguments = new Bundle();
        arguments.putString(ARG_RACE, raceName);
        arguments.putString(ARG_CIRCUIT, circuitId);
        arguments.putStringArrayList(ARG_ROWS, rows);
        sheet.setArguments(arguments);
        sheet.show(manager, "raceResultsSheet");
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.sheet_race_results, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        applySystemInsets(view);

        Bundle arguments = getArguments();

        if (arguments == null) {
            dismissAllowingStateLoss();
            return;
        }

        String raceName = arguments.getString(ARG_RACE, "");
        String circuitId = arguments.getString(ARG_CIRCUIT, "");
        ArrayList<String> rows = arguments.getStringArrayList(ARG_ROWS);

        ((TextView) view.findViewById(R.id.sheet_title))
                .setText(pastRaceItem.localizedGrandPrix(requireContext(), raceName));

        LinearLayout list = view.findViewById(R.id.sheet_rows);
        LayoutInflater inflater = LayoutInflater.from(requireContext());

        if (rows == null) {
            return;
        }

        for (int index = 0; index < rows.size(); index++) {
            String[] parts = rows.get(index).split("\\|", -1);

            if (parts.length < 5) {
                continue;
            }

            View row = inflater.inflate(R.layout.item_race_result_year, list, false);
            ((TextView) row.findViewById(R.id.row_year)).setText(parts[0]);
            ((TextView) row.findViewById(R.id.row_first)).setText(parts[2]);
            ((TextView) row.findViewById(R.id.row_second)).setText(parts[3]);
            ((TextView) row.findViewById(R.id.row_third)).setText(parts[4]);

            if (index == rows.size() - 1) {
                row.findViewById(R.id.row_divider).setVisibility(View.GONE);
            }

            final String season = parts[0];
            final String raceKey = parts[1];

            row.setOnClickListener(v -> {
                Intent intent = new Intent(requireContext(), raceResultsActivity.class);
                Bundle bundle = new Bundle();
                bundle.putString("raceName", raceKey);
                bundle.putString("circuitId", circuitId);
                bundle.putString("season", season);
                intent.putExtras(bundle);
                dismissAllowingStateLoss();
                startActivity(intent);
            });

            list.addView(row);
        }
    }

    /** The sheet is drawn under the navigation bar (see ThemeOverlay.Apex.NeuroSheet), so the content is lifted by its height. */
    private void applySystemInsets(View root) {
        final int left = root.getPaddingLeft();
        final int top = root.getPaddingTop();
        final int right = root.getPaddingRight();
        final int bottom = root.getPaddingBottom();

        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets safe = insets.getInsets(
                    WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
            v.setPadding(left + safe.left, top, right + safe.right, bottom + safe.bottom);
            return insets;
        });

        root.post(() -> ViewCompat.requestApplyInsets(root));
    }
}
