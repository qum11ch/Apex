package com.example.f1app;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.FrameLayout;
import android.widget.ImageView;
import androidx.annotation.DrawableRes;
import androidx.core.content.res.ResourcesCompat;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import java.util.Locale;

public final class AwardIconView extends FrameLayout {
    private static final String TAG = "AwardIconView";
    private final ImageView base;
    private final ImageView logo;
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG);
    private final Rect textBounds = new Rect();
    private String mainText = "", yearText = "";
    private float viewportW = 128f, viewportH = 128f;
    private float centerX, centerY, maxTextW, maxTextH;
    private float yearX = 125, yearY = 94, yearW = 40, yearH = 10;
    private int textColor;
    private int yearColor = Color.parseColor("#CFD4D6");
    private int binding;
    private boolean logoReady, mastery;
    private float masterySkew = -0.08f;
    private float otherSkew = -0.18f;
    private int contentX, contentY, contentW, contentH;
    private int logoX, logoY, logoW, logoH;

    public AwardIconView(Context context) { this(context, null); }

    public AwardIconView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);
        int fontId = getResources().getIdentifier("audiowide", "font", context.getPackageName());
        if (fontId == 0) fontId = getResources().getIdentifier(
                "audiowide_regular", "font", context.getPackageName());
        if (fontId == 0) throw new IllegalStateException(
                "Add your Audiowide font as res/font/audiowide.ttf or audiowide_regular.ttf");
        Typeface font = ResourcesCompat.getFont(context, fontId);
        if (font == null) throw new IllegalStateException("Cannot load Audiowide");
        textPaint.setTypeface(font);
        textPaint.setTextAlign(Paint.Align.LEFT);
        textPaint.setFakeBoldText(false);
        textPaint.setTextScaleX(1f);
        textPaint.setFontFeatureSettings("tnum");
        base = new ImageView(context);
        base.setScaleType(ImageView.ScaleType.FIT_CENTER);
        base.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
        addView(base, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        logo = new ImageView(context);
        logo.setScaleType(ImageView.ScaleType.FIT_CENTER);
        logo.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
        addView(logo, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        logo.setVisibility(GONE);
    }

    // Compare 0f, -0.08f and -0.18f without changing the other medal categories.
    public void setMasterySkew(float skew) {
        masterySkew = skew;
        invalidate();
    }

    public void setOtherTextSkew(float skew) {
        otherSkew = skew;
        invalidate();
    }

    public void clear() {
        binding++;
        GlideApp.with(logo).clear(logo);
        logo.setImageDrawable(null);
        logo.clearColorFilter();
        logo.setImageTintList(null);
        logo.setVisibility(GONE);
        logoReady = false;
        mastery = false;
        base.setImageDrawable(null);
        mainText = yearText = "";
        viewportW = viewportH = 128f;
        yearX = 125; yearY = 94; yearW = 40; yearH = 10;
        yearColor = Color.parseColor("#CFD4D6");
        setContentDescription(null);
        setOnClickListener(null);
        invalidate();
    }

    private void label(String text, float x, float y, float w, float h, String color) {
        mainText = text;
        centerX = x; centerY = y; maxTextW = w; maxTextH = h;
        textColor = Color.parseColor(color);
    }

    public void showMedal(@DrawableRes int drawable, CharSequence description) {
        clear();
        base.setImageResource(drawable);
        setContentDescription(description);
        if (drawable == R.drawable.pit_pass_correct_10_v2
                || drawable == R.drawable.pit_pass_correct_50
                || drawable == R.drawable.pit_pass_correct_100) {
            mastery = true;
            String number = drawable == R.drawable.pit_pass_correct_10_v2 ? "10"
                    : drawable == R.drawable.pit_pass_correct_50 ? "50" : "100";
            // The SAME reference string fixes the font size for all three medals.
            label(number, 64, 64, 76, 33, "#E7ECEB");
        } else if (drawable == R.drawable.pit_pass_one_year) {
            viewportW = viewportH = 96;
            label("1", 48, 54.5f, 29, 33, "#E4E7E5");
        } else if (drawable == R.drawable.pit_pass_three_years) {
            viewportW = viewportH = 96;
            label("3", 48, 54.5f, 29, 33, "#E1F3FF");
        } else if (drawable == R.drawable.pit_pass_five_years) {
            viewportW = viewportH = 96;
            label("5", 48, 54.5f, 29, 33, "#FFE9AD");
        } else if (drawable == R.drawable.pit_pass_season_place_1) {
            label("1", 64, 55, 37, 43, "#F6E8C1");
            newSeasonYear();
        } else if (drawable == R.drawable.pit_pass_season_participant_2026) {
            label("2026", 64, 56, 61, 21, "#E1EAF0");
            newSeasonYear();
        } else if (drawable == R.drawable.pit_pass_season_place_2
                || drawable == R.drawable.pit_pass_season_place_3) {
            // These two old plates intentionally stay unchanged in this test.
            viewportW = 192; viewportH = 128;
            label(drawable == R.drawable.pit_pass_season_place_2 ? "2" : "3",
                    94, 51.5f, 44, 47, "#222222");
            yearText = "2026";
        }
        requestLayout();
        invalidate();
    }

    private void newSeasonYear() {
        yearText = "2026";
        yearX = 65; yearY = 103.5f; yearW = 39; yearH = 9;
        yearColor = Color.parseColor("#DBE2E5");
    }

    public void showDriver(String driverCode, CharSequence description) {
        clear();
        base.setImageResource(R.drawable.pit_pass_fanclub_driver);
        label(normalize(driverCode), 64, 52, 59, 25, "#20272E");
        setContentDescription(description);
        requestLayout();
        invalidate();
    }

    public void showTeam(Uri imageUri, String fallbackCode, CharSequence description) {
        clear();
        base.setImageResource(R.drawable.pit_pass_fanclub_team);
        label(normalize(fallbackCode), 64, 52, 59, 25, "#DFE7EB");
        setContentDescription(description);
        requestLayout();
        invalidate();
        if (imageUri == null) return;
        final int request = binding;
        logo.setVisibility(INVISIBLE);
        GlideApp.with(logo).load(imageUri).override(384, 384).fitCenter().dontAnimate()
                .listener(new RequestListener<Drawable>() {
                    @Override public boolean onLoadFailed(GlideException error, Object model,
                            Target<Drawable> target, boolean first) {
                        if (request != binding) return true;
                        logoReady = false;
                        logo.setVisibility(GONE);
                        Log.w(TAG, "Team logo load failed", error);
                        if (error != null) error.logRootCauses(TAG);
                        invalidate();
                        return false;
                    }
                    @Override public boolean onResourceReady(Drawable resource, Object model,
                            Target<Drawable> target, DataSource source, boolean first) {
                        if (request != binding) return true;
                        logoReady = true;
                        logo.setVisibility(VISIBLE);
                        invalidate();
                        return false;
                    }
                }).into(logo);
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    @Override protected void onMeasure(int widthSpec, int heightSpec) {
        int preferred = Math.round(64 * getResources().getDisplayMetrics().density);
        setMeasuredDimension(resolveSize(Math.max(getSuggestedMinimumWidth(),
                        preferred + getPaddingLeft() + getPaddingRight()), widthSpec),
                resolveSize(Math.max(getSuggestedMinimumHeight(),
                        preferred + getPaddingTop() + getPaddingBottom()), heightSpec));
        contentX = getPaddingLeft(); contentY = getPaddingTop();
        contentW = Math.max(0, getMeasuredWidth() - contentX - getPaddingRight());
        contentH = Math.max(0, getMeasuredHeight() - contentY - getPaddingBottom());
        base.measure(MeasureSpec.makeMeasureSpec(contentW, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(contentH, MeasureSpec.EXACTLY));
        float s = Math.min(contentW / viewportW, contentH / viewportH);
        float x = contentX + (contentW - viewportW * s) / 2f;
        float y = contentY + (contentH - viewportH * s) / 2f;
        logoX = Math.round(x + 34 * s); logoY = Math.round(y + 31 * s);
        logoW = Math.max(0, Math.round(60 * s)); logoH = Math.max(0, Math.round(48 * s));
        logo.measure(MeasureSpec.makeMeasureSpec(logoW, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(logoH, MeasureSpec.EXACTLY));
    }

    @Override protected void onLayout(boolean changed, int l, int t, int r, int b) {
        base.layout(contentX, contentY, contentX + contentW, contentY + contentH);
        logo.layout(logoX, logoY, logoX + logoW, logoY + logoH);
    }

    @Override protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);
        if (contentW <= 0 || contentH <= 0 || base.getDrawable() == null) return;
        float scale = Math.min(contentW / viewportW, contentH / viewportH);
        if (scale <= 0) return;
        int state = canvas.save();
        canvas.translate(contentX + (contentW - viewportW * scale) / 2f,
                contentY + (contentH - viewportH * scale) / 2f);
        canvas.scale(scale, scale);
        if (!logoReady) drawLabel(canvas, mainText, centerX, centerY, maxTextW, maxTextH,
                textColor, mastery ? masterySkew : otherSkew, mastery ? "100" : mainText);
        drawLabel(canvas, yearText, yearX, yearY, yearW, yearH, yearColor, 0f, yearText);
        canvas.restoreToCount(state);
    }

    private void drawLabel(Canvas canvas, String text, float cx, float cy,
            float maxWidth, float maxHeight, int color, float skew, String sizingText) {
        if (text == null || text.isEmpty() || maxWidth <= 0 || maxHeight <= 0) return;
        textPaint.setColor(color);
        textPaint.setTextSkewX(skew);
        textPaint.setTextSize(100f);
        textPaint.getTextBounds(sizingText, 0, sizingText.length(), textBounds);
        if (textBounds.width() <= 0 || textBounds.height() <= 0) return;
        float fit = Math.min(maxWidth / textBounds.width(), maxHeight / textBounds.height());
        textPaint.setTextSize(100f * fit);
        textPaint.getTextBounds(text, 0, text.length(), textBounds);
        canvas.drawText(text, cx - textBounds.exactCenterX(),
                cy - textBounds.exactCenterY(), textPaint);
    }
}
