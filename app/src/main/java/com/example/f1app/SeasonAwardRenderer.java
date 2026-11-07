package com.example.f1app;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import androidx.core.content.res.ResourcesCompat;

/** Owns ONLY the four seasonal award labels. Other categories stay untouched. */
public final class SeasonAwardRenderer {
    private final Typeface mainFont;
    private final Typeface yearFont;
    private final Paint mainPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint yearPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint rimPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path source = new Path();
    private final Path mainPath = new Path();
    private final Path yearPath = new Path();
    private final RectF bounds = new RectF();
    private final Matrix matrix = new Matrix();
    private int rank = -1;

    public SeasonAwardRenderer(Context context) {
        mainFont = ResourcesCompat.getFont(context, R.font.good_timing_bd);
        yearFont = ResourcesCompat.getFont(context, R.font.audiowide);
        if (mainFont == null || yearFont == null) {
            throw new IllegalStateException("Existing good_timing_bd and audiowide fonts are required");
        }
        for (Paint paint : new Paint[]{mainPaint, yearPaint, rimPaint}) {
            paint.setStyle(Paint.Style.FILL);
            paint.setFakeBoldText(false);
            paint.setTextScaleX(1f);
            paint.setTextSkewX(0f);
            paint.setFontFeatureSettings("tnum");
        }
        yearPaint.setColor(Color.rgb(230, 235, 230));
        rimPaint.setColor(Color.rgb(10, 19, 26));
    }

    public void clear() {
        rank = -1;
        mainPath.reset();
        yearPath.reset();
    }

    public boolean isActive() { return rank >= 0; }

    public boolean bind(int drawable) {
        if (drawable == R.drawable.pit_pass_season_participant_2026) rank = 0;
        else if (drawable == R.drawable.pit_pass_season_place_1) rank = 1;
        else if (drawable == R.drawable.pit_pass_season_place_2) rank = 2;
        else if (drawable == R.drawable.pit_pass_season_place_3) rank = 3;
        else return false;
        mainPaint.setShader(null);
        if (rank == 0) {
            makePath("2026", mainFont, 64, 66, 54, 21, mainPath);
            mainPaint.setColor(Color.rgb(39, 57, 69));
        } else {
            makePath(String.valueOf(rank), mainFont, 64, 54, 37, 43, mainPath);
            int hi, mid, lo;
            if (rank == 1) {
                hi = Color.rgb(245,231,187); mid = Color.rgb(205,167,95); lo = Color.rgb(126,97,56);
            } else if (rank == 2) {
                hi = Color.rgb(237,242,242); mid = Color.rgb(183,200,208); lo = Color.rgb(108,132,147);
            } else {
                hi = Color.rgb(237,204,174); mid = Color.rgb(184,131,95); lo = Color.rgb(118,80,59);
            }
            mainPath.computeBounds(bounds, true);
            mainPaint.setShader(new LinearGradient(bounds.left, bounds.top, bounds.right, bounds.bottom,
                    new int[]{hi, mid, hi, lo}, new float[]{0f, .38f, .52f, 1f}, Shader.TileMode.CLAMP));
            makePath("2026", yearFont, 64, 95.5f, 36, 8, yearPath);
        }
        return true;
    }

    public void draw(Canvas canvas) {
        if (!isActive()) return;
        if (rank > 0) {
            int state = canvas.save();
            canvas.translate(0.35f, 0.5f);
            canvas.drawPath(mainPath, rimPaint);
            canvas.restoreToCount(state);
        }
        canvas.drawPath(mainPath, mainPaint);
        if (rank > 0) canvas.drawPath(yearPath, yearPaint);
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
