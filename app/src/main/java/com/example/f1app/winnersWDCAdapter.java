package com.example.f1app;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.List;

public class winnersWDCAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>{
    Context context;
    List<winnersWdcList> dataList;


    private static final int TYPE_DRIVER = 0;
    private static final int TYPE_SEPARATOR = 1;

    @Override
    public int getItemViewType(int position) {
        return dataList.get(position).getType() == winnersWdcList.SEPARATOR
                ? TYPE_SEPARATOR
                : TYPE_DRIVER;
    }


    public winnersWDCAdapter(Context context, List<winnersWdcList> dataList) {
        this.context = context;
        this.dataList = dataList;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());

        if (viewType == TYPE_SEPARATOR) {
            View view = inflater.inflate(
                    R.layout.item_wdc_separator,
                    parent,
                    false
            );

            return new SeparatorHolder(view);
        }

        View view = inflater.inflate(
                R.layout.item_winners_wdc,
                parent,
                false
        );

        return new DataHolder(view);
    }

    private static class SeparatorHolder extends RecyclerView.ViewHolder {
        public SeparatorHolder(@NonNull View itemView) {
            super(itemView);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        winnersWdcList item = dataList.get(position);

        if (item.getType() == winnersWdcList.SEPARATOR) {
            return;
        }

        DataHolder dataHolder = (DataHolder) holder;
        driversWDCPointsList datum = item.getDriver();

        String driverName = datum.getDriverName();

        if (driverName.equals("Andrea Kimi")) {
            String[] parts = driverName.split(" ");
            driverName = parts[1];
        }

        dataHolder.driverName.setText(driverName);
        dataHolder.driverFamilyName.setText(
                datum.getDriverFamilyName()
        );
        dataHolder.placement.setText(datum.getPlacement());

        String driverCurrentPoints =
                datum.getCurrentPoints()
                        + " "
                        + context.getString(R.string.pts_header);

        String driverMaxPoints =
                datum.getMaxPoints()
                        + " "
                        + context.getString(R.string.pts_header);

        dataHolder.currentPoints.setText(driverCurrentPoints);
        dataHolder.maxPoints.setText(driverMaxPoints);

        boolean canWin = datum.isCanWin();

        if (canWin) {
            dataHolder.canWin.setText(R.string.yes_text);
        } else {
            dataHolder.canWin.setText(R.string.no_text);
        }

        String driverTeam = datum.getConstructorId();

        DatabaseReference rootRef =
                FirebaseDatabase.getInstance().getReference();

        rootRef.child("constructors")
                .child(driverTeam)
                .addListenerForSingleValueEvent(
                        new ValueEventListener() {
                            @Override
                            public void onDataChange(
                                    @NonNull DataSnapshot snapshot
                            ) {
                                String color =
                                        snapshot.child("color")
                                                .getValue(String.class);

                                if (color != null) {
                                    dataHolder.line.setBackgroundColor(
                                            Color.parseColor("#" + color)
                                    );
                                }
                            }

                            @Override
                            public void onCancelled(
                                    @NonNull DatabaseError error
                            ) {
                                Log.e(
                                        "WinnersWDCAdapter",
                                        "Ошибка получения цвета команды",
                                        error.toException()
                                );
                            }
                        }
                );
    }


    @Override
    public int getItemCount() {
        return dataList.size();
    }

    public static class DataHolder extends RecyclerView.ViewHolder {
        TextView driverName, driverFamilyName, placement, currentPoints, maxPoints, canWin;
        ConstraintLayout constraintLayout;
        View line, bottomLine;
        CardView cardView;
        LinearLayout mainLayout;

        public DataHolder(@NonNull View itemView) {
            super(itemView);
            mainLayout = itemView.findViewById(R.id.mainLayout);
            cardView = itemView.findViewById(R.id.cardView);
            driverName = itemView.findViewById(R.id.driverName);
            driverFamilyName = itemView.findViewById(R.id.driverFamilyName);
            placement = itemView.findViewById(R.id.placement);
            currentPoints = itemView.findViewById(R.id.currentPoints);
            maxPoints = itemView.findViewById(R.id.maxPoints);
            canWin = itemView.findViewById(R.id.canWin);
            constraintLayout = itemView.findViewById(R.id.main_layout);
            line = itemView.findViewById(R.id.line);
            bottomLine = itemView.findViewById(R.id.bottomLine);
        }
    }
}
