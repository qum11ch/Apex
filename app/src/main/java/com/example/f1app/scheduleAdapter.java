package com.example.f1app;

import android.app.Activity;
import android.content.res.ColorStateList;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class scheduleAdapter extends RecyclerView.Adapter<scheduleAdapter.DataHolder>{
    Activity context;
    List<scheduleData> dataList;
    private static final long HOUR = 3600*1000;
    private static final long SPRINT_QUALI_DIFF = 44*60*1000;

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

        String[] eventDateData = getDate(eventDate, eventName);

        holder.eventDay.setText(eventDateData[0]);
        holder.eventTime.setText(eventDateData[1]);
        holder.eventMonth.setText(eventDateData[2]);

        if (eventDateData[3].equals("yes")){
            holder.content_layout.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(context,R.color.light_silver)));
            holder.eventMonth.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(context,R.color.white)));
        }
        holder.eventName.setText(context.getString(getStringByName(eventName + "_text")));
    }

    @Override
    public int getItemCount() {
        return dataList.size();
    }

    public class DataHolder extends RecyclerView.ViewHolder{
        TextView eventDay, eventMonth, eventName, eventTime;
        ConstraintLayout constraintLayout;
        LinearLayout content_layout;
        public DataHolder(@NonNull View itemView) {
            super(itemView);
            eventDay = itemView.findViewById(R.id.eventDay);
            eventMonth = itemView.findViewById(R.id.eventMonth);
            eventName = itemView.findViewById(R.id.eventName);
            eventTime = itemView.findViewById(R.id.eventTime);
            constraintLayout = itemView.findViewById(R.id.main_layout);
            content_layout = itemView.findViewById(R.id.content_layout);
        }
    }

    private String[] getDate(String ourDate, String eventName)
    {
        String date = ourDate.replaceAll("\\s+", "T");
        String dayEvent = " ";
        String timeEvent = " ";
        String monthEvent = " ";
        String isFinished = "";
        Log.i("fatalErrorSchedule", " " + date);
        Instant dateInst = Instant.parse(date);
        ZonedDateTime dateTime = dateInst.atZone(ZoneId.systemDefault());
        LocalDate currentDate = LocalDate.now();
        String currentDateString = currentDate.toString();
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
        SimpleDateFormat newFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        DateTimeFormatter  fullDateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z", Locale.ENGLISH);
        String newDate = dateTime.format(fullDateFormatter);
        try
        {
            //fullDateFormatter.setTimeZone(TimeZone.getDefault());
            Date value = newFormatter.parse(newDate);
            Date current = formatter.parse(currentDateString);

            if (current.after(value)){
                isFinished = "yes";
            }else{
                isFinished = "no";
            }

            SimpleDateFormat dayFormatter = new SimpleDateFormat("dd");
            dayFormatter.setTimeZone(TimeZone.getDefault());
            dayEvent = dayFormatter.format(value);

            SimpleDateFormat timeFormatter = new SimpleDateFormat("HH:mm");
            timeFormatter.setTimeZone(TimeZone.getDefault());
            String timeEvent1 = timeFormatter.format(value);


            Date timeEnd = new Date(value.getTime() + HOUR);
            timeFormatter.setTimeZone(TimeZone.getDefault());
            String timeEvent2 = timeFormatter.format(timeEnd);

            Date timeEnd_sprintQ = new Date(value.getTime() + SPRINT_QUALI_DIFF);
            timeFormatter.setTimeZone(TimeZone.getDefault());
            String timeEvent3 = timeFormatter.format(timeEnd_sprintQ);

            timeEvent = timeEvent1 + "-" + timeEvent2;

            SimpleDateFormat monthFormatter = new SimpleDateFormat("MMM");
            monthFormatter.setTimeZone(TimeZone.getDefault());
            monthEvent = monthFormatter.format(value);

            if (eventName.equals("race_event")){
                timeEvent = timeEvent1;
            }

            if (eventName.equals("sprint_quali_event")){
                timeEvent = timeEvent1 + "-" + timeEvent3;
            }

        }
        catch (Exception e)
        {
            dayEvent = "00";
            timeEvent = "00:00";
            monthEvent = "JAN";
        }
        return new String[] {dayEvent, timeEvent, monthEvent, isFinished};
    }

    private int getStringByName(String name) {
        int stringId = 0;

        try {
            Class res = R.string.class;
            Field field = res.getField(name);
            stringId = field.getInt(null);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return stringId;
    }
}
