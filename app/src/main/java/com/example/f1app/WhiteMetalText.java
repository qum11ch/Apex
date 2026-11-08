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

/** Bright white metal with a stronger graphite outline; existing API is unchanged. */
public final class WhiteMetalText {
    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint outline = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint lip = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path source = new Path();
    private final Path fitted = new Path();
    private final Path shifted = new Path();
    private final RectF bounds = new RectF();
    private final Matrix fitMatrix = new Matrix();
    private final Matrix shiftMatrix = new Matrix();
    private final int[] colors = {
            Color.rgb(255, 255, 255), Color.rgb(248, 250, 252),
            Color.rgb(255, 255, 255), Color.rgb(248, 250, 252),
            Color.rgb(226, 230, 235)
    };
    private final float[] stops = {0f, .30f, .46f, .56f, 1f};
    private String cachedText;
    private float cachedX, cachedY, cachedW, cachedH;
    private float reliefScale;

    public WhiteMetalText(Context context) {
        Typeface font = ResourcesCompat.getFont(context, R.font.oxanium_bold);
        if (font == null) throw new IllegalStateException("Cannot load oxanium_bold.ttf");
        fill.setTypeface(font);
        fill.setTextSize(100f);
        fill.setFakeBoldText(false);
        fill.setTextScaleX(1f);
        fill.setTextSkewX(0f);
        fill.setFontFeatureSettings("tnum");
        fill.setStyle(Paint.Style.FILL);
        outline.setStyle(Paint.Style.STROKE);
        outline.setStrokeJoin(Paint.Join.ROUND);
        outline.setColor(Color.rgb(20, 26, 31));
        lip.setStyle(Paint.Style.FILL);
    }

    /** Keep a separate instance for each simultaneously visible label. */
    public void draw(Canvas canvas, String text, float cx, float cy,
            float maxWidth, float maxHeight) {
        if (text == null || text.isEmpty() || maxWidth <= 0f || maxHeight <= 0f) return;
        if (!text.equals(cachedText) || cx != cachedX || cy != cachedY
                || maxWidth != cachedW || maxHeight != cachedH) {
            if (!prepare(text, cx, cy, maxWidth, maxHeight)) return;
            cachedText = text;
            cachedX = cx; cachedY = cy; cachedW = maxWidth; cachedH = maxHeight;
        }
        lip.setColor(Color.argb(180, 255, 255, 255));
        shiftMatrix.reset();
        shiftMatrix.setTranslate(.28f * reliefScale, .34f * reliefScale);
        fitted.transform(shiftMatrix, shifted);
        canvas.drawPath(shifted, lip);
        lip.setColor(Color.argb(235, 16, 23, 27));
        shiftMatrix.reset();
        shiftMatrix.setTranslate(-.38f * reliefScale, -.44f * reliefScale);
        fitted.transform(shiftMatrix, shifted);
        canvas.drawPath(shifted, lip);
        canvas.drawPath(fitted, outline);
        canvas.drawPath(fitted, fill);
    }

    private boolean prepare(String text, float cx, float cy, float maxWidth, float maxHeight) {
        source.reset();
        fill.getTextPath(text, 0, text.length(), 0f, 0f, source);
        source.computeBounds(bounds, true);
        if (bounds.isEmpty()) return false;
        float scale = Math.min(maxWidth / bounds.width(), maxHeight / bounds.height());
        fitMatrix.reset();
        fitMatrix.setScale(scale, scale);
        fitMatrix.postTranslate(cx - bounds.centerX() * scale,
                cy - bounds.centerY() * scale);
        fitted.reset();
        source.transform(fitMatrix, fitted);
        fitted.computeBounds(bounds, true);
        reliefScale = Math.max(.35f, Math.min(1f, bounds.height() / 28f));
        outline.setStrokeWidth(2.2f * reliefScale);
        fill.setShader(new LinearGradient(bounds.left, bounds.top, bounds.right, bounds.bottom,
                colors, stops, Shader.TileMode.CLAMP));
        return true;
    }
}
