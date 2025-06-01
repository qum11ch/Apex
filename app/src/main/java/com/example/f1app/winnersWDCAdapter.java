package com.example.f1app;

import android.content.Context;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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

public class winnersWDCAdapter extends RecyclerView.Adapter<winnersWDCAdapter.DataHolder> {
    Context context;
    List<driversWDCPointsList> dataList;


    @Override
    public int getItemViewType(int position) {
        if (position == 0)
            return 1;
        else
            return 2;
    }


    public winnersWDCAdapter(Context context, List<driversWDCPointsList> datum) {
        this.context = context;
        dataList = datum;
    }

    @NonNull
    @Override
    public winnersWDCAdapter.DataHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_winners_wdc, parent, false);
        return new DataHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull winnersWDCAdapter.DataHolder holder, int position) {
        driversWDCPointsList datum = dataList.get(position);
        holder.driverName.setText(datum.getDriverName());
        holder.driverFamilyName.setText(datum.getDriverFamilyName());
        holder.placement.setText(datum.getPlacement());

        String mDriverCurrentPoints = datum.getCurrentPoints()  + " " + context.getString(R.string.pts_header);
        String mDriverMaxPoints = datum.getMaxPoints()  + " " + context.getString(R.string.pts_header);

        holder.currentPoints.setText(mDriverCurrentPoints);
        holder.maxPoints.setText(mDriverMaxPoints);

        boolean mCanWin = datum.isCanWin();
        if (mCanWin){
            holder.canWin.setText(R.string.yes_text);
            holder.canWin.setTextColor(ContextCompat.getColor(context, R.color.sauber));
            holder.bottomLine.setBackgroundColor(ContextCompat.getColor(context, R.color.sauber));
        }else{
            holder.canWin.setText(R.string.no_text);
            holder.canWin.setTextColor(ContextCompat.getColor(context, R.color.red));
            holder.bottomLine.setBackgroundColor(ContextCompat.getColor(context, R.color.red));
        }

        String mDriverTeam = datum.getConstructorId();

        DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference();
        rootRef.child("constructors/" + mDriverTeam).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String teamColor = "#" + snapshot.child("color").getValue(String.class);
                holder.line.setBackgroundColor(Color.parseColor(teamColor));
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("winnersWDCActivity: Fatal error in Firebase getting team color", " " + error.getMessage());
            }
        });
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

        public DataHolder(@NonNull View itemView) {
            super(itemView);
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
