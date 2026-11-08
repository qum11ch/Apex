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

/** Shared Oxanium Bold lettering with the original recessed mastery metal finish. */
public final class MedalMetalText {
    private final Typeface typeface;
    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint edge = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path source = new Path();
    private final Path fitted = new Path();
    private final RectF bounds = new RectF();
    private final Matrix matrix = new Matrix();
    private final Matrix shift = new Matrix();
    private final Path shifted = new Path();
    private final int[] steel = {
            Color.rgb(213, 213, 200), Color.rgb(133, 136, 126), Color.rgb(71, 78, 73)
    };
    private final int[] blueSteel = {
            Color.rgb(196, 213, 236), Color.rgb(116, 142, 175), Color.rgb(62, 82, 109)
    };
    private final int[] redMetal = {
            Color.rgb(240, 173, 176), Color.rgb(181, 87, 104), Color.rgb(95, 43, 59)
    };
    private final float[] stops = {0f, 0.30f, 0.46f, 0.56f, 1f};
    private final int[] gradientColors = new int[5];
    private String cachedText;
    private float cachedCx, cachedCy, cachedW, cachedH;
    private int cachedHi, cachedMid, cachedLo;

    public MedalMetalText(Context context) {
        typeface = ResourcesCompat.getFont(context, R.font.oxanium_bold);
        if (typeface == null) throw new IllegalStateException("Cannot load oxanium_bold.ttf");
        fill.setTypeface(typeface);
        fill.setTextSize(100f);
        fill.setFakeBoldText(false);
        fill.setTextSkewX(0f);
        fill.setTextScaleX(1f);
        fill.setFontFeatureSettings("tnum");
        fill.setStyle(Paint.Style.FILL);
        edge.setStyle(Paint.Style.FILL);
    }

    /** Original API and original dimensions/palettes for 10, 50 and 100. */
    public void draw(Canvas canvas, String text) {
        if (!"10".equals(text) && !"50".equals(text) && !"100".equals(text)) return;
        int[] palette = "50".equals(text) ? blueSteel : "100".equals(text) ? redMetal : steel;
        draw(canvas, text, 64f, 64f,
                "100".equals(text) ? 80f : 76f,
                "100".equals(text) ? 35f : 40f,
                palette[0], palette[1], palette[2]);
    }

    /** Caller supplies the label box in its own canvas viewport and the XML metal colors. */
    public void draw(Canvas canvas, String text, float cx, float cy,
            float maxWidth, float maxHeight, int hi, int mid, int lo) {
        if (text == null || text.isEmpty() || maxWidth <= 0f || maxHeight <= 0f) return;
        if (!text.equals(cachedText) || cx != cachedCx || cy != cachedCy
                || maxWidth != cachedW || maxHeight != cachedH
                || hi != cachedHi || mid != cachedMid || lo != cachedLo) {
            if (!prepare(text, cx, cy, maxWidth, maxHeight, hi, mid, lo)) return;
            cachedText = text;
            cachedCx = cx; cachedCy = cy; cachedW = maxWidth; cachedH = maxHeight;
            cachedHi = hi; cachedMid = mid; cachedLo = lo;
        }
        edge.setColor(withAlpha(gradientColors[0], 115));
        shift.reset();
        shift.setTranslate(0.38f, 0.48f);
        fitted.transform(shift, shifted);
        canvas.drawPath(shifted, edge);
        edge.setColor(Color.rgb(8, 13, 17));
        shift.reset();
        shift.setTranslate(-0.48f, -0.58f);
        fitted.transform(shift, shifted);
        canvas.drawPath(shifted, edge);
        canvas.drawPath(fitted, fill);
    }

    private boolean prepare(String text, float cx, float cy,
            float maxWidth, float maxHeight, int hi, int mid, int lo) {
        source.reset();
        fill.getTextPath(text, 0, text.length(), 0f, 0f, source);
        source.computeBounds(bounds, true);
        if (bounds.isEmpty()) return false;
        float factor = Math.min(maxWidth / bounds.width(), maxHeight / bounds.height());
        matrix.reset();
        matrix.setScale(factor, factor);
        matrix.postTranslate(cx - bounds.centerX() * factor, cy - bounds.centerY() * factor);
        fitted.reset();
        source.transform(matrix, fitted);
        fitted.computeBounds(bounds, true);
        gradientColors[0] = hi;
        gradientColors[1] = mid;
        gradientColors[2] = hi;
        gradientColors[3] = mid;
        gradientColors[4] = lo;
        fill.setShader(new LinearGradient(bounds.left, bounds.top, bounds.right, bounds.bottom,
                gradientColors, stops, Shader.TileMode.CLAMP));
        return true;
    }

    private static int withAlpha(int color, int alpha) {
        return (color & 0x00ffffff) | (alpha << 24);
    }
}
