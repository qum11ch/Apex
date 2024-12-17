package com.example.f1app;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

public class scheduleAdapter extends RecyclerView.Adapter<scheduleAdapter.DataHolder>{
    Activity context;
    List<scheduleData> dataList;
    private static final long HOUR = 3600*1000;

    public scheduleAdapter(Activity context , List<scheduleData> datum){
        this.context = context;
        dataList = datum;
    }

    @NonNull
    @Override
    public scheduleAdapter.DataHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_schedule, parent , false);
        return new scheduleAdapter.DataHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull scheduleAdapter.DataHolder holder, int position) {
        scheduleData datum = dataList.get(position);

        String eventDate = datum.getEventDate();
        String eventName = datum.getEventName();

        String eventDateData[] = getDate(eventDate, eventName);

        holder.eventDay.setText(eventDateData[0]);
        holder.eventTime.setText(eventDateData[1]);
        holder.eventMonth.setText(eventDateData[2]);
        holder.eventName.setText(eventName);
    }

    @Override
    public int getItemCount() {
        return dataList.size();
    }

    public class DataHolder extends RecyclerView.ViewHolder{
        TextView eventDay, eventMonth, eventName, eventTime;
        ConstraintLayout constraintLayout;
        public DataHolder(@NonNull View itemView) {
            super(itemView);
            eventDay = itemView.findViewById(R.id.eventDay);
            eventMonth = itemView.findViewById(R.id.eventMonth);
            eventName = itemView.findViewById(R.id.eventName);
            eventTime = itemView.findViewById(R.id.eventTime);
            constraintLayout = itemView.findViewById(R.id.main_layout);
        }
    }

    private String[] getDate(String ourDate, String eventName)
    {
        String date = ourDate;
        String dayEvent = " ";
        String timeEvent = " ";
        String monthEvent = " ";
        try
        {
            SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            formatter.setTimeZone(TimeZone.getTimeZone("UTC"));
            Date value = formatter.parse(date);

            SimpleDateFormat dayFormatter = new SimpleDateFormat("dd");
            dayFormatter.setTimeZone(TimeZone.getDefault());
            dayEvent = dayFormatter.format(value);

            SimpleDateFormat timeFormatter = new SimpleDateFormat("HH:mm");
            timeFormatter.setTimeZone(TimeZone.getDefault());
            String timeEvent1 = timeFormatter.format(value);

            Date newValue = new Date(value.getTime() + HOUR);
            timeFormatter.setTimeZone(TimeZone.getDefault());
            String timeEvent2 = timeFormatter.format(newValue);

            timeEvent = timeEvent1 + "-" + timeEvent2;

            SimpleDateFormat monthFormatter = new SimpleDateFormat("MMM");
            monthFormatter.setTimeZone(TimeZone.getDefault());
            monthEvent = monthFormatter.format(value);

            if (eventName.equals("Race")){
                timeEvent = timeEvent1;
            }

            if (eventName.equals("Sprint")){
                timeEvent = timeEvent1;
            }

        }
        catch (Exception e)
        {
            dayEvent = "00";
            timeEvent = "00:00";
            monthEvent = "JAN";
        }
        return new String[] {dayEvent, timeEvent, monthEvent};
    }
}
