package com.example.f1app;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.view.ViewCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.RequestBuilder;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.List;

public class teamsStandingsAdapter extends RecyclerView.Adapter<teamsStandingsAdapter.DataHolder>{
    Context context;
    List<teamsList> dataList;

    private final java.util.Map<String, Long> stableIds = new java.util.HashMap<>();

    private long nextStableId = 1L;

    private long stableIdFor(String key) {
        Long known = stableIds.get(key);

        if (known != null) {
            return known;
        }

        long created = nextStableId++;
        stableIds.put(key, created);
        return created;
    }

    public teamsStandingsAdapter(Context context , List<teamsList> datum){
        this.context = context;
        dataList = datum;
        setHasStableIds(true);
    }

    @NonNull
    @Override
    public DataHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view;
        if (viewType == 1) {
            view = LayoutInflater.from(context).inflate(R.layout.item_team_first, parent, false);
        } else {
            view = LayoutInflater.from(context).inflate(R.layout.item_team, parent, false);
        }
        return new DataHolder(view);
    }

    @Override
    public long getItemId(int position) {
        teamsList item = dataList.get(position);

        String id = item.getTeamId();
        String identity = id == null || id.trim().isEmpty()
                ? "empty-header"
                : id.trim();

        return stableIdFor(item.getSeason() + "|" + identity);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void onBindViewHolder(@NonNull DataHolder holder, int position) {
        teamsList datum = dataList.get(position);
        ArrayList<String> teamDrivers = datum.getDrivers();
        holder.teamName.setText(datum.getTeam());

        String firstDriver = teamDrivers != null && !teamDrivers.isEmpty()
                ? teamDrivers.get(0)
                : null;

        String secondDriver = teamDrivers != null && teamDrivers.size() > 1
                ? teamDrivers.get(1)
                : null;

        holder.teamDriverFirst.setText(getFamilyName(firstDriver));
        holder.teamDriverSecond.setText(getFamilyName(secondDriver));

        String darkTeamColor = datum.getTeamColor();

        //String season = datum.getSeason();

        StorageReference mTeamCar = datum.getImageUrl();

        String carKey = datum.getTeamId()
                + "|" + datum.getSeason()
                + "|" + (mTeamCar == null ? "" : mTeamCar.toString());

        if (!carKey.equals(holder.carRequestKey)) {
            holder.carRequestKey = carKey;

            if (holder.carTarget != null) {
                GlideApp.with(context).clear(holder.carTarget);
                holder.carTarget = null;
            }

            holder.team_car.setScaleX(1f);
            holder.team_car.setImageDrawable(null);

            final Object carToken = new Object();
            holder.carToken = carToken;

            RequestBuilder<Bitmap> carRequest = GlideApp.with(context)
                    .asBitmap()
                    .load(mTeamCar)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .error(R.drawable.placeholder_car);

            if ("2027".equals(datum.getSeason()) && mTeamCar != null) {
                String currentPath = mTeamCar.getPath();
                String previousPath = currentPath.replace("2027", "2026");

                // Используем тот же bucket и структуру пути.
                // Если года в пути нет, не повторяем тот же запрос.
                if (!previousPath.equals(currentPath)) {
                    StorageReference previousCar = mTeamCar.getRoot()
                            .child(previousPath.startsWith("/")
                                    ? previousPath.substring(1)
                                    : previousPath);

                    carRequest = carRequest.error(
                            GlideApp.with(context)
                                    .asBitmap()
                                    .load(previousCar)
                                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                                    .error(R.drawable.placeholder_car)
                    );
                }
            }

            holder.carTarget = carRequest.into(new CustomTarget<Bitmap>() {
                @Override
                public void onResourceReady(@NonNull Bitmap resource,
                                            @Nullable Transition<? super Bitmap> transition) {
                    if (holder.carToken != carToken) {
                        return; // the row now shows another team
                    }

                    int width = resource.getWidth();
                    int height = resource.getHeight();

                    Bitmap displayBitmap = Bitmap.createBitmap(resource, 0, 0,
                            width / 2, height);

                    holder.team_car.setImageBitmap(displayBitmap);
                    holder.team_car.setScaleType(ImageView.ScaleType.CENTER_CROP);
                    holder.team_car.setScaleX(1f);
                }

                @Override
                public void onLoadCleared(@Nullable Drawable placeholder) {

                }
            });

        }

        //String mTeamId = datum.getTeamId();

        int colorRgb = teamColors.parse(darkTeamColor);
        int alpha = 0x66;
        int colorWithAlpha = (alpha << 24) | (colorRgb & 0x00FFFFFF);

        int alphaLine = 0xCC;
        int colorWithAlphaLine = (alphaLine << 24) | (colorRgb & 0x00FFFFFF);

        ViewCompat.setBackgroundTintList(
                holder.itemTeam,
                ColorStateList.valueOf(colorWithAlpha)
        );

        holder.itemTeamLine.setBackground(createFrameWithLeftOffset(colorWithAlphaLine, context));

        if (datum.getStartSeasonInfo()) {
            holder.teamPointsText.setVisibility(View.GONE);
            holder.teamPoints.setVisibility(View.GONE);
        }else{
            holder.teamPointsText.setVisibility(View.VISIBLE);
            holder.teamPoints.setVisibility(View.VISIBLE);
        }

        if (holder.getItemViewType() == 1){
            if (datum.getStartSeasonInfo()) {
                int width = 0;
                int height = 0;
                holder.cardView.getLayoutParams().width = width;
                holder.cardView.getLayoutParams().height = height;
            }else{
                holder.teamPosition.setText(datum.getPosition());
                String teamPoints = datum.getPoints();
                holder.teamPoints.setText(teamPoints);
            }
        }else{
            holder.teamPosition.setText(datum.getPosition());
            String teamPoints = datum.getPoints();
            holder.teamPoints.setText(teamPoints);

        }

        holder.constraintLayout.setOnClickListener(v -> {
            Intent intent = new Intent(context , teamPageActivity.class);
            Bundle bundle = new Bundle();
            bundle.putString("teamName", datum.getTeam());
            bundle.putString("teamId", datum.getTeamId());
            bundle.putStringArrayList("teamDrivers", teamDrivers);
            bundle.putString("currentSeason", datum.getSeason());
            intent.putExtras(bundle);
            context.startActivity(intent);
        });


    }

    private LayerDrawable createFrameWithLeftOffset(int colorWithAlphaLine, Context context) {
        int strokeWidth = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 5, context.getResources().getDisplayMetrics()
        );
        LayerDrawable layerDrawable = new LayerDrawable(new Drawable[]{
                createStrokeShape(strokeWidth, colorWithAlphaLine, context),
                createInnerTransparentShape(strokeWidth, context)
        });

        int leftOffset = -strokeWidth;
        layerDrawable.setLayerInset(0, leftOffset, 0, 0, 0);
        layerDrawable.setLayerInset(1, strokeWidth, strokeWidth, strokeWidth, strokeWidth);

        return layerDrawable;
    }

    private GradientDrawable createStrokeShape(int strokeWidth, int strokeColor, Context context) {
        GradientDrawable stroke = new GradientDrawable();
        stroke.setShape(GradientDrawable.RECTANGLE);
        stroke.setStroke(strokeWidth, strokeColor);
        stroke.setColor(Color.TRANSPARENT);

        int radius20 = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 23, context.getResources().getDisplayMetrics()
        );
        stroke.setCornerRadii(new float[]{
                0, 0,
                radius20, radius20,
                radius20, radius20,
                0, 0
        });
        return stroke;
    }

    private GradientDrawable createInnerTransparentShape(int strokeWidth, Context context) {
        GradientDrawable inner = new GradientDrawable();
        inner.setShape(GradientDrawable.RECTANGLE);
        inner.setColor(Color.TRANSPARENT);

        int innerRadius = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 15, context.getResources().getDisplayMetrics());
        inner.setCornerRadii(new float[]{
                0, 0,
                innerRadius, innerRadius,
                innerRadius, innerRadius,
                0, 0
        });
        return inner;
    }

    @Override
    public int getItemViewType(int position) {
        if (position == 0)
            return 1;
        else
            return 2;
    }


    @Override
    public void onViewRecycled(@NonNull DataHolder holder) {
        super.onViewRecycled(holder);

        if (holder.carTarget != null) {
            GlideApp.with(context).clear(holder.carTarget);
            holder.carTarget = null;
        }

        holder.carToken = null;
        holder.carRequestKey = null;
    }

    @Override
    public int getItemCount() {
        return dataList.size();
    }

    public static String getFamilyName(String driverFullname) {
        if (driverFullname == null) {
            return "—";
        }

        String name = driverFullname.trim().replaceAll("\\s+", " ");

        if (name.isEmpty()
                || "N/A".equalsIgnoreCase(name)
                || "null".equalsIgnoreCase(name)
                || "TBA".equalsIgnoreCase(name)
                || "TBC".equalsIgnoreCase(name)) {
            return "—";
        }

        // Эти фамилии состоят из нескольких слов.
        String[] compoundSurnames = {
                "de la Rosa",
                "van der Garde",
                "van der Merwe",
                "de Vries",
                "di Resta"
        };

        for (String surname : compoundSurnames) {
            if (name.equalsIgnoreCase(surname)
                    || name.toLowerCase(java.util.Locale.ROOT)
                    .endsWith(" " + surname.toLowerCase(java.util.Locale.ROOT))) {
                return surname;
            }
        }

        // Одна фамилия или код остаются без изменений.
        // Для полного имени берём последнее слово, в том числе у Antonelli.
        int lastSpace = name.lastIndexOf(' ');
        return lastSpace >= 0 ? name.substring(lastSpace + 1) : name;
    }

    public static class DataHolder extends RecyclerView.ViewHolder{
        com.bumptech.glide.request.target.CustomTarget<android.graphics.Bitmap> carTarget;
        Object carToken;
        TextView teamName, teamPoints, teamPosition,
                teamDriverFirst, teamDriverSecond, teamPointsText;
        ShapeableImageView team_car;
        ConstraintLayout constraintLayout;
        RelativeLayout team_layout;
        CardView cardView;
        LinearLayout itemTeam, itemTeamLine;
        String carRequestKey;
        public DataHolder(@NonNull View itemView) {
            super(itemView);
            itemTeamLine = itemView.findViewById(R.id.item_team_line);
            teamPointsText = itemView.findViewById(R.id.team_points_text);
            itemTeam = itemView.findViewById(R.id.item_team);
            team_layout = itemView.findViewById(R.id.team_layout);
            cardView = itemView.findViewById(R.id.cardView);
            teamName = itemView.findViewById(R.id.teamName);
            teamPoints = itemView.findViewById(R.id.team_pts);
            teamPosition = itemView.findViewById(R.id.team_placement);
            teamDriverFirst = itemView.findViewById(R.id.driverFirst);
            teamDriverSecond = itemView.findViewById(R.id.driverSecond);
            constraintLayout = itemView.findViewById(R.id.main_layout);
            team_car = itemView.findViewById(R.id.team_car);
        }
    }
}
