package com.example.f1app;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Shader;

/** Optional world-champion contour, in the driver's existing 128 x 128 viewport. */
public final class ChampionDriverOutline {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    public ChampionDriverOutline() {
        path.moveTo(36f, 16f); path.lineTo(92f, 16f);
        path.lineTo(108f, 32f); path.lineTo(108f, 65f);
        path.cubicTo(108f, 87f, 87f, 104f, 64f, 115f);
        path.cubicTo(41f, 104f, 20f, 87f, 20f, 65f);
        path.lineTo(20f, 32f); path.close();
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(2.2f);
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setShader(new LinearGradient(24f, 16f, 105f, 115f,
                new int[]{Color.rgb(255,240,188), Color.rgb(206,167,96),
                        Color.rgb(255,240,188), Color.rgb(128,97,52)},
                new float[]{0f,.40f,.58f,1f}, Shader.TileMode.CLAMP));
    }
    public void draw(Canvas canvas) { canvas.drawPath(path, paint); }
}
