package com.example.f1app;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
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

/** Typography and season revision based on the current qum11ch/Apex master. */
public final class AwardIconView extends FrameLayout {
    private static final String TAG = "AwardIconView";
    private final ImageView base;
    private final ImageView logo;
    private final SeasonAwardRenderer seasonalAwards;
    private final MedalMetalText masteryMetalText;
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG);
    private final Rect legacyBounds = new Rect();
    private final Path glyphPath = new Path();
    private final RectF glyphBounds = new RectF();
    private final Typeface audiowide;
    private final Typeface goodTimingBold;
    private String mainText = "", yearText = "";
    private float viewportW = 128f, viewportH = 128f;
    private float centerX, centerY, maxTextW, maxTextH;
    private float yearX = 64, yearY = 106.5f, yearW = 43, yearH = 9;
    private int textColor;
    private final int yearColor = Color.parseColor("#E4E8E6");
    private int binding;
    private boolean logoReady, mastery, season;
    private boolean tenureMetal;
    private boolean useAudiowideForAchievements;
    private float masterySkew = -0.04f;
    private float otherSkew = -0.18f;
    private int contentX, contentY, contentW, contentH;
    private int logoX, logoY, logoW, logoH;
    private boolean seasonParticipant;
    private final WhiteMetalText whiteDriverText;
    private final WhiteMetalText whiteParticipantText;
    private final WhiteMetalText whiteParticipantYearText;
    private static final float FANCLUB_CENTER_X = 64f;
    private static final float FANCLUB_CENTER_Y = 62f;
    private int driverChampionshipsCount;
    private final ChampionDriverMarker championDriverMarker;
    private boolean driverFanclub;

    private final Typeface driverCodeTypeface =
            Typeface.create("sans-serif-condensed", Typeface.BOLD);

    public AwardIconView(Context context) { this(context, null); }

    public AwardIconView(Context context, AttributeSet attrs) {
        super(context, attrs);
        seasonalAwards = new SeasonAwardRenderer(context);
        masteryMetalText = new MedalMetalText(context);
        championDriverMarker = new ChampionDriverMarker(context);
        whiteDriverText = new WhiteMetalText(context);
        whiteParticipantText = new WhiteMetalText(context);
        whiteParticipantYearText = new WhiteMetalText(context);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);
        // Both fonts already exist in the checked repository. No synthetic bold.
        audiowide = ResourcesCompat.getFont(context, R.font.audiowide);
        goodTimingBold = ResourcesCompat.getFont(context, R.font.good_timing_bd);
        if (audiowide == null || goodTimingBold == null) {
            throw new IllegalStateException("audiowide.ttf and good_timing_bd.ttf are required");
        }
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

    public void setMasterySkew(float skew) {
        masterySkew = skew;
        invalidate();
    }

    public void setOtherTextSkew(float skew) {
        otherSkew = skew;
        invalidate();
    }

    /** Legacy A/B switch; metal numerals always use the shared Oxanium renderer. */
    public void setUseAudiowideForAchievements(boolean useAudiowide) {
        useAudiowideForAchievements = useAudiowide;
        invalidate();
    }

    public void clear() {
        seasonalAwards.clear();
        binding++;
        GlideApp.with(logo).clear(logo);
        logo.setImageDrawable(null);
        logo.clearColorFilter();
        logo.setImageTintList(null);
        logo.setVisibility(GONE);
        logoReady = false;
        mastery = season = false;
        tenureMetal = false;
        seasonParticipant = false;
        driverFanclub = false;
        driverChampionshipsCount = 0;
        base.setImageDrawable(null);
        mainText = yearText = "";
        viewportW = viewportH = 128f;
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

        // Старый широкий знак участника: отдельная геометрия и типографика.
        if (drawable == R.drawable.pit_pass_season_participant_2026) {
            seasonParticipant = true;

            viewportW = 192f;
            viewportH = 128f;

            // Крупный год по центру светлой металлической плоскости.
            label("2026", 99f, 52f, 104f, 28f, "#22262B");

            requestLayout();
            invalidate();
            return;
        }

        // BEGIN SEASON_HIERARCHY_BIND
        if (seasonalAwards.bind(drawable)) {
            viewportW = viewportH = 128f;
            mainText = yearText = "";
            requestLayout();
            invalidate();
            return;
        }
        // END SEASON_HIERARCHY_BIND
        if (drawable == R.drawable.pit_pass_correct_10_v2) {
            mastery = true;
            label("10", 64, 64, 78, 40, "#E9EEEB");
        } else if (drawable == R.drawable.pit_pass_correct_50) {
            mastery = true;
            label("50", 64, 64, 78, 40, "#E9EEEB");
        } else if (drawable == R.drawable.pit_pass_correct_100) {
            mastery = true;
            label("100", 64, 64, 82, 35, "#E9EEEB");
        } else if (drawable == R.drawable.pit_pass_one_year) {
            tenureMetal = true;
            viewportW = viewportH = 96;
            label("1", 48, 54.5f, 29, 33, "#E4E7E5");
        } else if (drawable == R.drawable.pit_pass_three_years) {
            tenureMetal = true;
            viewportW = viewportH = 96;
            label("3", 48, 54.5f, 29, 33, "#E1F3FF");
        } else if (drawable == R.drawable.pit_pass_five_years) {
            tenureMetal = true;
            viewportW = viewportH = 96;
            label("5", 48, 54.5f, 29, 33, "#FFE9AD");
        } else if (drawable == R.drawable.pit_pass_season_place_1
                || drawable == R.drawable.pit_pass_season_place_2
                || drawable == R.drawable.pit_pass_season_place_3) {
            season = true;
            String rank = drawable == R.drawable.pit_pass_season_place_1 ? "1"
                    : drawable == R.drawable.pit_pass_season_place_2 ? "2" : "3";
            label(rank, 64, 50.5f, 42, 43, "#242C31");
            yearText = "2026";
        }

        requestLayout();
        invalidate();
    }

    public void showDriver(String driverCode, CharSequence description) {
        clear();

        driverFanclub = true;
        base.setImageResource(R.drawable.pit_pass_fanclub_driver);

        // Спокойная, компактная надпись, без растягивания на весь щит.
        label(normalize(driverCode), FANCLUB_CENTER_X, FANCLUB_CENTER_Y, 54f, 23f, "#20272E");

        setContentDescription(description);
        requestLayout();
        invalidate();
    }

    /** championshipsCount is the driver's personal world-title count. */
    public void showDriver(String driverCode, int championshipsCount, CharSequence description) {
        showDriver(driverCode, description);
        driverChampionshipsCount = Math.max(0, championshipsCount);
        if (driverChampionshipsCount > 0) {
            setContentDescription(String.valueOf(description) + ", "
                    + getContext().getString(R.string.drivers_championships)
                    + ": " + driverChampionshipsCount);
        }
        invalidate();
    }

    public void showTeam(Uri imageUri, String fallbackCode, CharSequence description) {
        clear();
        base.setImageResource(R.drawable.pit_pass_fanclub_team);
        label(normalize(fallbackCode), FANCLUB_CENTER_X, FANCLUB_CENTER_Y, 59, 25, "#DFE7EB");
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
        logoX = Math.round(x + (FANCLUB_CENTER_X - 30f) * s);
        logoY = Math.round(y + (FANCLUB_CENTER_Y - 24f) * s);
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

        if (driverFanclub) {
            if (driverChampionshipsCount > 0) {
                float badgeSizeDp = Math.min(contentW, contentH)
                        / getResources().getDisplayMetrics().density;
                championDriverMarker.draw(canvas, driverChampionshipsCount, badgeSizeDp);
            }
            whiteDriverText.draw(
                    canvas, mainText,
                    centerX, centerY, maxTextW, maxTextH
            );
            canvas.restoreToCount(state);
            return;
        }

        if (seasonParticipant) {
            float skewX = -0.16f;

            // Запас по ширине: после скоса надпись станет немного шире.
            float availableWidth = Math.max(
                    1f,
                    maxTextW - Math.abs(skewX) * maxTextH
            );

            int textState = canvas.save();

            canvas.translate(centerX, centerY);
            canvas.skew(skewX, 0f);
            canvas.translate(-centerX, -centerY);

            whiteParticipantText.draw(
                    canvas,
                    mainText,
                    centerX,
                    centerY,
                    availableWidth,
                    maxTextH
            );

            whiteParticipantYearText.draw(
                    canvas, yearText,
                    123f, 96f, 40f, 9f
            );

            canvas.restoreToCount(textState);
            canvas.restoreToCount(state);
            return;
        }

        // BEGIN SEASON_HIERARCHY_RENDER
        if (seasonalAwards.isActive()) {
            seasonalAwards.draw(canvas);
            canvas.restoreToCount(state);
            return;
        }
        // END SEASON_HIERARCHY_RENDER
        // BEGIN MASTERY_METAL_RENDER
        if (mastery) {
            masteryMetalText.draw(canvas, mainText);
            canvas.restoreToCount(state);
            return;
        }
        // END MASTERY_METAL_RENDER
        // Tenure numerals use the same font and recessed metal finish as mastery.
        if (tenureMetal) {
            int hi, mid, lo;
            if ("3".equals(mainText)) {
                hi = Color.rgb(196, 213, 236);
                mid = Color.rgb(116, 142, 175);
                lo = Color.rgb(62, 82, 109);
            } else if ("5".equals(mainText)) {
                hi = Color.rgb(255, 240, 188);
                mid = Color.rgb(206, 167, 96);
                lo = Color.rgb(128, 97, 52);
            } else {
                hi = Color.rgb(213, 213, 200);
                mid = Color.rgb(133, 136, 126);
                lo = Color.rgb(71, 78, 73);
            }
            masteryMetalText.draw(canvas, mainText, centerX, centerY,
                    maxTextW, maxTextH, hi, mid, lo);
            canvas.restoreToCount(state);
            return;
        }
        if (!logoReady) {
            if (driverFanclub) {
                drawFitted(
                        canvas,
                        mainText,
                        centerX, centerY,
                        maxTextW, maxTextH,
                        textColor,
                        0f,
                        driverCodeTypeface
                );
            } else if (mastery || season) {
                Typeface face = useAudiowideForAchievements ? audiowide : goodTimingBold;
                drawFitted(canvas, mainText, centerX, centerY, maxTextW, maxTextH,
                        textColor, mastery ? masterySkew : 0f, face);
            } else {
                // Preserve legacy lettering for remaining non-metal labels.
                drawLegacy(canvas, mainText, centerX, centerY, maxTextW, maxTextH,
                        textColor, otherSkew);
            }
        }
        drawFitted(canvas, yearText, yearX, yearY, yearW, yearH,
                yearColor, 0f, audiowide);
        canvas.restoreToCount(state);
    }

    private void drawFitted(Canvas canvas, String text, float cx, float cy,
            float maxWidth, float maxHeight, int color, float skew, Typeface face) {
        if (text == null || text.isEmpty() || maxWidth <= 0 || maxHeight <= 0) return;
        textPaint.setTypeface(face);
        textPaint.setColor(color);
        textPaint.setTextSkewX(skew);
        textPaint.setTextSize(100f);
        glyphPath.reset();
        textPaint.getTextPath(text, 0, text.length(), 0, 0, glyphPath);
        glyphPath.computeBounds(glyphBounds, true);
        if (glyphBounds.isEmpty()) return;
        // Uniform scaling of the WHOLE word. No glyph-by-glyph deformation.
        float fit = Math.min(maxWidth / glyphBounds.width(), maxHeight / glyphBounds.height());
        int state = canvas.save();
        canvas.translate(cx, cy);
        canvas.scale(fit, fit);
        canvas.drawText(text, -glyphBounds.centerX(), -glyphBounds.centerY(), textPaint);
        canvas.restoreToCount(state);
    }

    private void drawLegacy(Canvas canvas, String text, float cx, float cy,
            float maxWidth, float maxHeight, int color, float skew) {
        if (text == null || text.isEmpty() || maxWidth <= 0 || maxHeight <= 0) return;
        textPaint.setTypeface(audiowide);
        textPaint.setColor(color);
        textPaint.setTextSkewX(skew);
        textPaint.setTextSize(100f);
        textPaint.getTextBounds(text, 0, text.length(), legacyBounds);
        if (legacyBounds.width() <= 0 || legacyBounds.height() <= 0) return;
        float fit = Math.min(maxWidth / legacyBounds.width(), maxHeight / legacyBounds.height());
        textPaint.setTextSize(100f * fit);
        textPaint.getTextBounds(text, 0, text.length(), legacyBounds);
        canvas.drawText(text, cx - legacyBounds.exactCenterX(),
                cy - legacyBounds.exactCenterY(), textPaint);
    }
}
