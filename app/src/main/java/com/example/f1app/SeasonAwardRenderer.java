package com.example.f1app;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;
import androidx.core.content.res.ResourcesCompat;

/** Seasonal placements share the mastery font/effect; participant rendering stays unchanged. */
public final class SeasonAwardRenderer {
    private final Typeface mainFont;
    private final Paint mainPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path source = new Path();
    private final Path mainPath = new Path();
    private final RectF bounds = new RectF();
    private final Matrix matrix = new Matrix();
    private final MedalMetalText rankMetalText;
    private final MedalMetalText yearMetalText;
    private int rank = -1;
    private int hi, mid, lo;
    private String rankText = "";

    public SeasonAwardRenderer(Context context) {
        mainFont = ResourcesCompat.getFont(context, R.font.good_timing_bd);
        if (mainFont == null) {
            throw new IllegalStateException("Existing good_timing_bd font is required");
        }
        mainPaint.setStyle(Paint.Style.FILL);
        mainPaint.setFakeBoldText(false);
        mainPaint.setTextScaleX(1f);
        mainPaint.setTextSkewX(0f);
        mainPaint.setFontFeatureSettings("tnum");
        rankMetalText = new MedalMetalText(context);
        yearMetalText = new MedalMetalText(context);
    }

    public void clear() {
        rank = -1;
        rankText = "";
        mainPath.reset();
    }

    public boolean isActive() { return rank >= 0; }

    public boolean bind(int drawable) {
        if (drawable == R.drawable.pit_pass_season_participant_2026) rank = 0;
        else if (drawable == R.drawable.pit_pass_season_place_1) rank = 1;
        else if (drawable == R.drawable.pit_pass_season_place_2) rank = 2;
        else if (drawable == R.drawable.pit_pass_season_place_3) rank = 3;
        else return false;
        mainPath.reset();
        mainPaint.setShader(null);
        if (rank == 0) {
            makePath("2026", mainFont, 64, 66, 54, 21, mainPath);
            mainPaint.setColor(Color.rgb(39, 57, 69));
        } else {
            rankText = String.valueOf(rank);
            if (rank == 1) {
                hi = Color.rgb(255, 240, 188);
                mid = Color.rgb(206, 167, 96);
                lo = Color.rgb(128, 97, 52);
            } else if (rank == 2) {
                hi = Color.rgb(237, 242, 242);
                mid = Color.rgb(183, 200, 208);
                lo = Color.rgb(108, 132, 147);
            } else {
                hi = Color.rgb(237, 204, 174);
                mid = Color.rgb(184, 131, 95);
                lo = Color.rgb(118, 80, 59);
            }
        }
        return true;
    }

    public void draw(Canvas canvas) {
        if (!isActive()) return;
        if (rank == 0) {
            canvas.drawPath(mainPath, mainPaint);
            return;
        }
        rankMetalText.draw(canvas, rankText, 64f, 54f, 37f, 43f, hi, mid, lo);
        yearMetalText.draw(canvas, "2026", 64f, 95.5f, 36f, 8f, hi, mid, lo);
    }

    private void makePath(String text, Typeface typeface, float cx, float cy,
            float maxWidth, float maxHeight, Path target) {
        mainPaint.setTypeface(typeface);
        mainPaint.setTextSize(100f);
        source.reset();
        mainPaint.getTextPath(text, 0, text.length(), 0, 0, source);
        source.computeBounds(bounds, true);
        target.reset();
        if (bounds.isEmpty()) return;
        float fit = Math.min(maxWidth / bounds.width(), maxHeight / bounds.height());
        matrix.reset();
        matrix.setScale(fit, fit);
        matrix.postTranslate(cx - bounds.centerX() * fit, cy - bounds.centerY() * fit);
        source.transform(matrix, target);
    }
}
