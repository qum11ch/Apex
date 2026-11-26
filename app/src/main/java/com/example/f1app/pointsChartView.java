package com.example.f1app;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;

/** One or two cumulative lines; a touch picks the round. */
public class pointsChartView extends View {

    interface Listener {
        void onSelect(int index);
    }

    private final Paint lineA = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint lineB = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint grid = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint label = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint marker = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private final float density;

    private double[] a = new double[0];
    private double[] b = new double[0];
    private int colorA;
    private int colorB;
    private int selected = -1;
    private float downX;
    private float downY;
    private Listener listener;

    public pointsChartView(Context context) {
        this(context, null);
    }

    public pointsChartView(Context context, AttributeSet attrs) {
        super(context, attrs);
        density = context.getResources().getDisplayMetrics().density;

        for (Paint line : new Paint[]{lineA, lineB}) {
            line.setStyle(Paint.Style.STROKE);
            line.setStrokeWidth(3 * density);
            line.setStrokeJoin(Paint.Join.ROUND);
            line.setStrokeCap(Paint.Cap.ROUND);
        }

        grid.setStrokeWidth(density);
        label.setTextSize(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 10,
                context.getResources().getDisplayMetrics()));
        marker.setStyle(Paint.Style.FILL);
    }

    void set(double[] a, double[] b, int colorA, int colorB, int gridColor, int textColor,
             Listener listener) {
        this.a = a;
        this.b = b;
        this.colorA = colorA;
        this.colorB = colorB;
        this.listener = listener;
        lineA.setColor(colorA);
        lineB.setColor(colorB);
        grid.setColor(gridColor);
        label.setColor(textColor);
        selected = a.length - 1;

        if (listener != null && selected >= 0) {
            listener.onSelect(selected);
        }

        invalidate();
    }

    private boolean hasB() {
        return b.length == a.length && b.length > 0;
    }

    private static double niceMax(double max) {
        if (max <= 0) {
            return 10;
        }

        double step = Math.pow(10, Math.floor(Math.log10(max)));
        double m = max / step;
        double nice = m <= 1 ? 1 : m <= 2 ? 2 : m <= 5 ? 5 : 10;

        return nice * step;
    }

    private float left() {
        return 34 * density;
    }

    private float plotWidth() {
        return getWidth() - left() - 10 * density;
    }

    private float x(int index) {
        int n = a.length;
        return n == 1 ? left() + plotWidth() / 2f : left() + plotWidth() * index / (n - 1);
    }

    private float y(double value, float padTop, float plotHeight, double scale) {
        return padTop + plotHeight - (float) (value / scale) * plotHeight;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int n = a.length;

        if (n == 0) {
            return;
        }

        float padTop = 16 * density;
        float plotHeight = getHeight() - padTop - 8 * density;

        double max = 0;
        for (int i = 0; i < n; i++) {
            max = Math.max(max, a[i]);
            if (hasB()) {
                max = Math.max(max, b[i]);
            }
        }
        double scale = niceMax(max);

        for (int i = 0; i <= 4; i++) {
            float lineY = padTop + plotHeight - plotHeight * i / 4f;
            canvas.drawLine(left(), lineY, left() + plotWidth(), lineY, grid);
            canvas.drawText(String.valueOf(Math.round(scale * i / 4)), 0,
                    lineY - 3 * density, label);
        }

        draw(canvas, a, lineA, padTop, plotHeight, scale);

        if (hasB()) {
            draw(canvas, b, lineB, padTop, plotHeight, scale);
        }

        if (selected >= 0 && selected < n) {
            float sx = x(selected);
            canvas.drawLine(sx, padTop, sx, padTop + plotHeight, grid);

            marker.setColor(colorA);
            canvas.drawCircle(sx, y(a[selected], padTop, plotHeight, scale), 5 * density, marker);

            if (hasB()) {
                marker.setColor(colorB);
                canvas.drawCircle(sx, y(b[selected], padTop, plotHeight, scale), 5 * density, marker);
            }
        }
    }

    private void draw(Canvas canvas, double[] values, Paint line, float padTop,
                      float plotHeight, double scale) {
        path.reset();

        for (int i = 0; i < values.length; i++) {
            float px = x(i);
            float py = y(values[i], padTop, plotHeight, scale);

            if (i == 0) {
                path.moveTo(px, py);
            } else {
                path.lineTo(px, py);
            }
        }

        canvas.drawPath(path, line);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (a.length == 0) {
            return false;
        }

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                downX = event.getX();
                downY = event.getY();
                select(event.getX());
                return true;

            case MotionEvent.ACTION_MOVE:
                if (Math.abs(event.getX() - downX) > Math.abs(event.getY() - downY)) {
                    if (getParent() != null) {
                        getParent().requestDisallowInterceptTouchEvent(true);
                    }
                    select(event.getX());
                }
                return true;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (getParent() != null) {
                    getParent().requestDisallowInterceptTouchEvent(false);
                }
                return true;

            default:
                return super.onTouchEvent(event);
        }
    }

    private void select(float touchX) {
        int n = a.length;
        float width = plotWidth();
        int index = n == 1 || width <= 0 ? 0
                : Math.round((touchX - left()) / width * (n - 1));
        index = Math.max(0, Math.min(n - 1, index));

        if (index != selected) {
            selected = index;
            invalidate();

            if (listener != null) {
                listener.onSelect(index);
            }
        }
    }
}