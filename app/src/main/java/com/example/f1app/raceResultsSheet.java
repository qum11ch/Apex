package com.example.f1app;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.FragmentManager;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.ArrayList;
import java.util.List;

/** Bottom sheet with the earlier runnings of a Grand Prix; a tap opens the results of that year. */
public class raceResultsSheet extends BottomSheetDialogFragment {

    private static final String ARG_RACE = "raceName";
    private static final String ARG_CIRCUIT = "circuitId";
    private static final String ARG_ROWS = "rows";
    private static final String NAV_STRIP_TAG = "raceResultsSheetNavStrip";

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
    public void onStart() {
        super.onStart();

        if (getDialog() == null) {
            return;
        }

        // The rounded background comes from the layout; the sheet container itself stays transparent.
        View sheet = getDialog().findViewById(com.google.android.material.R.id.design_bottom_sheet);

        if (sheet != null) {
            sheet.setBackgroundColor(Color.TRANSPARENT);

            // Show every row at once instead of a half-open sheet.
            BottomSheetBehavior<View> behavior = BottomSheetBehavior.from(sheet);
            behavior.setSkipCollapsed(true);
            behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        }

        Window window = getDialog().getWindow();

        if (window != null) {
            View decor = window.getDecorView();

            // Dark gesture line on a light sheet, light one on a dark sheet.
            WindowCompat.getInsetsController(window, decor)
                    .setAppearanceLightNavigationBars(MainActivity.checkLightTheme(requireContext()));

            decor.post(() -> fillNavigationBar(decor));
        }
    }

    /**
     * The dialog stops above the navigation bar, and since Android 15 the bar's own colour is ignored
     * with gesture navigation, so the dimmed page shows through under the sheet. A strip in the sheet
     * colour over that area makes the two read as one surface.
     */
    private void fillNavigationBar(View decor) {
        if (!isAdded() || !(decor instanceof ViewGroup) || decor.findViewWithTag(NAV_STRIP_TAG) != null) {
            return;
        }

        WindowInsetsCompat insets = ViewCompat.getRootWindowInsets(decor);
        int height = insets == null ? 0 : insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom;

        if (height <= 0) {
            return;
        }

        View strip = new View(requireContext());
        strip.setTag(NAV_STRIP_TAG);
        strip.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.colorPrimary));

        ((ViewGroup) decor).addView(strip, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, height, Gravity.BOTTOM));
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

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
}
