package com.example.f1app;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.Build;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.TextUtils;
import android.util.TypedValue;
import android.widget.RemoteViews;

import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;

import java.util.Locale;

/**
 * The launcher inflates the widgets and does not apply the fonts of the app, so the text is drawn
 * here in white with the font of the app; the colour is set on the picture by tint(), separately for
 * the light and the dark theme.
 */
final class widgetGlyphs {

    private widgetGlyphs() {
    }

    static Bitmap text(Context context, String text, int fontRes, float sp, int maxDp,
                       int maxLines, boolean caps) {
        String value = caps ? text.toUpperCase(Locale.getDefault()) : text;
        return render(value, font(context, fontRes, sp), px(context, maxDp), maxLines);
    }

    /** One line that gets smaller instead of being cut: for the numbers. */
    static Bitmap fit(Context context, String text, int fontRes, float sp, int maxDp) {
        float size = sp;
        TextPaint paint = font(context, fontRes, size);
        int maxPx = px(context, maxDp);

        while (Layout.getDesiredWidth(text, paint) > maxPx && size > sp * 0.6f) {
            size -= 1f;
            paint.setTextSize(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, size,
                    context.getResources().getDisplayMetrics()));
        }

        return render(text, paint, maxPx, 1);
    }

    /** Paints a white picture; on Android 12 and newer the theme is followed without a redraw. */
    static void tint(Context context, RemoteViews v, int viewId, int colorRes) {
        int light = ContextCompat.getColor(themed(context, false), colorRes);
        int dark = ContextCompat.getColor(themed(context, true), colorRes);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            v.setColorInt(viewId, "setColorFilter", light, dark);
        } else {
            boolean night = (context.getResources().getConfiguration().uiMode
                    & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
            v.setInt(viewId, "setColorFilter", night ? dark : light);
        }
    }

    /** "4 ч 42 мин": the numbers big, the units smaller and paler, one baseline; shrinks to fit. */
    static Bitmap time(Context context, String[] numbers, String[] units, int fontRes,
                       float numberSp, float unitSp, int maxDp) {
        float density = context.getResources().getDisplayMetrics().density;
        int maxPx = px(context, maxDp);
        float k = 1f;

        while (true) {
            TextPaint number = font(context, fontRes, numberSp * k);
            TextPaint unit = font(context, fontRes, unitSp * k);
            unit.setAlpha(170);

            float inner = 3 * density * k;
            float between = 8 * density * k;
            float total = 0;

            for (int i = 0; i < numbers.length; i++) {
                total += number.measureText(numbers[i]) + inner + unit.measureText(units[i]);

                if (i < numbers.length - 1) {
                    total += between;
                }
            }

            if (total <= maxPx || k <= 0.55f) {
                Paint.FontMetrics m = number.getFontMetrics();
                Bitmap bitmap = Bitmap.createBitmap((int) Math.ceil(total) + 2,
                        (int) Math.ceil(m.descent - m.ascent) + 2, Bitmap.Config.ARGB_8888);
                Canvas canvas = new Canvas(bitmap);
                float baseline = -m.ascent + 1;
                float x = 1;

                for (int i = 0; i < numbers.length; i++) {
                    canvas.drawText(numbers[i], x, baseline, number);
                    x += number.measureText(numbers[i]) + inner;
                    canvas.drawText(units[i], x, baseline, unit);
                    x += unit.measureText(units[i]) + between;
                }

                return bitmap;
            }

            k -= 0.05f;
        }
    }

    private static Context themed(Context context, boolean night) {
        Configuration configuration = new Configuration(context.getResources().getConfiguration());
        configuration.uiMode = (configuration.uiMode & ~Configuration.UI_MODE_NIGHT_MASK)
                | (night ? Configuration.UI_MODE_NIGHT_YES : Configuration.UI_MODE_NIGHT_NO);
        return context.createConfigurationContext(configuration);
    }

    private static TextPaint font(Context context, int fontRes, float sp) {
        TextPaint paint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(Color.WHITE);

        try {
            Typeface typeface = ResourcesCompat.getFont(context, fontRes);

            if (typeface != null) {
                paint.setTypeface(typeface);
            }
        } catch (RuntimeException ignored) {
            // the system font stays
        }

        paint.setTextSize(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp,
                context.getResources().getDisplayMetrics()));
        return paint;
    }

    private static int px(Context context, int dp) {
        return Math.max(1, Math.round(dp * context.getResources().getDisplayMetrics().density));
    }

    private static Bitmap render(String value, TextPaint paint, int maxPx, int maxLines) {
        int natural = (int) Math.ceil(Layout.getDesiredWidth(value, paint)) + 2;
        int width = Math.max(1, Math.min(maxPx, natural));

        StaticLayout layout = StaticLayout.Builder.obtain(value, 0, value.length(), paint, width)
                .setMaxLines(Math.max(1, maxLines))
                .setEllipsize(TextUtils.TruncateAt.END)
                .build();

        Bitmap bitmap = Bitmap.createBitmap(width, Math.max(1, layout.getHeight()) + 2,
                Bitmap.Config.ARGB_8888);
        layout.draw(new Canvas(bitmap));
        return bitmap;
    }
}