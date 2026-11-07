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

/** Recessed metal numerals for the three current mastery backgrounds only. */
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

    /** Canvas must already be in the 128 x 128 mastery viewport. */
    public void draw(Canvas canvas, String text) {
        if (!"10".equals(text) && !"50".equals(text) && !"100".equals(text)) return;
        if (!text.equals(cachedText)) {
            if (!prepare(text)) return;
            cachedText = text;
        }
        // A tiny lighter lower/right lip and darker upper/left lip imply an inset.
        // They are sharp edge offsets, NOT a blurred shadow or extruded thickness.
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

    private boolean prepare(String text) {
        source.reset();
        fill.getTextPath(text, 0, text.length(), 0f, 0f, source);
        source.computeBounds(bounds, true);
        if (bounds.isEmpty()) return false;
        float maxW = "100".equals(text) ? 80f : 76f;
        float maxH = "100".equals(text) ? 35f : 40f;
        float factor = Math.min(maxW / bounds.width(), maxH / bounds.height());
        matrix.reset();
        matrix.setScale(factor, factor);
        matrix.postTranslate(64f - bounds.centerX() * factor,
                64f - bounds.centerY() * factor);
        fitted.reset();
        source.transform(matrix, fitted);
        fitted.computeBounds(bounds, true);
        int[] palette = "50".equals(text) ? blueSteel : "100".equals(text) ? redMetal : steel;
        // The SAME three colors as the corresponding XML frame.
        gradientColors[0] = palette[0];
        gradientColors[1] = palette[1];
        gradientColors[2] = palette[0];
        gradientColors[3] = palette[1];
        gradientColors[4] = palette[2];
        fill.setShader(new LinearGradient(bounds.left, bounds.top, bounds.right, bounds.bottom,
                gradientColors, stops, Shader.TileMode.CLAMP));
        return true;
    }

    private static int withAlpha(int color, int alpha) {
        return (color & 0x00ffffff) | (alpha << 24);
    }
}
