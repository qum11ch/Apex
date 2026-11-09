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

public final class championDriverMarker {
    private final Path stars = new Path();
    private final Path number = new Path();
    private final Path source = new Path();
    private final Paint gold = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint edge = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint faceRepair = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint type = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF bounds = new RectF();
    private final Matrix matrix = new Matrix();
    private int cachedTitles = -1;
    private boolean cachedCompact;

    public championDriverMarker(Context context) {
        gold.setStyle(Paint.Style.FILL);
        gold.setShader(new LinearGradient(43f,30f,85f,44f,
                new int[]{Color.rgb(255,240,188),Color.rgb(206,167,96),
                        Color.rgb(255,240,188),Color.rgb(128,97,52)},
                new float[]{0f,.40f,.58f,1f},Shader.TileMode.CLAMP));
        edge.setStyle(Paint.Style.STROKE);
        edge.setStrokeWidth(.35f);
        edge.setStrokeJoin(Paint.Join.ROUND);
        edge.setColor(Color.rgb(10,16,21));
        // Restore only the old red bar, using the exact existing face gradient.
        // This is not a new panel: its shader shares the XML's viewport coordinates.
        faceRepair.setStyle(Paint.Style.FILL);
        faceRepair.setShader(new LinearGradient(28f,12f,103f,117f,
                new int[]{Color.rgb(30,39,44),Color.rgb(17,25,30)},
                new float[]{0f,1f},Shader.TileMode.CLAMP));
        Typeface font=ResourcesCompat.getFont(context,R.font.oxanium_bold);
        if(font==null) throw new IllegalStateException("Cannot load oxanium_bold.ttf");
        type.setTypeface(font);
        type.setTextSize(100f);
        type.setFakeBoldText(false);
        type.setTextScaleX(1f);
        type.setTextSkewX(0f);
        type.setFontFeatureSettings("tnum");
    }

    /** Canvas uses the 128 x 128 viewport; badgeSizeDp is the displayed size. */
    public void draw(Canvas canvas,int titles,float badgeSizeDp) {
        if(titles<=0) return;
        boolean compact=badgeSizeDp<40f;
        if(titles!=cachedTitles || compact!=cachedCompact) {
            rebuild(titles,compact);
            cachedTitles=titles;cachedCompact=compact;
        }
        canvas.drawRect(44f,30f,84f,34f,faceRepair);
        canvas.drawPath(stars,edge);
        canvas.drawPath(stars,gold);
        canvas.drawPath(number,edge);
        canvas.drawPath(number,gold);
    }

    private void rebuild(int titles,boolean compact) {
        stars.reset();number.reset();
        if(compact || titles>21) {
            addStar(stars,53f,37f,4.8f);
            fitNumber(String.valueOf(titles),70f,37f,22f,10f);
            return;
        }
        if(titles<=3) {
            float step=12f;
            float left=64f-(titles-1)*step/2f;
            for(int i=0;i<titles;i++) addStar(stars,left+i*step,37f,4.5f);
            return;
        }
        int rows=Math.max(2,(titles+6)/7);
        int columns=(titles+rows-1)/rows;
        float gapX=2f,gapY=1.5f;
        float diameter=Math.min(7f,Math.min((42f-(columns-1)*gapX)/columns,
                (14f-(rows-1)*gapY)/rows));
        if(diameter<3.2f) {
            addStar(stars,53f,37f,4.8f);
            fitNumber(String.valueOf(titles),70f,37f,22f,10f);
            return;
        }
        int base=titles/rows,extra=titles%rows;
        float top=37f-(rows-1)*(diameter+gapY)/2f;
        for(int row=0;row<rows;row++) {
            int count=base+(row<extra?1:0);
            float left=64f-(count-1)*(diameter+gapX)/2f;
            for(int column=0;column<count;column++) {
                addStar(stars,left+column*(diameter+gapX),top+row*(diameter+gapY),diameter/2f);
            }
        }
    }

    private void addStar(Path target,float cx,float cy,float radius) {
        for(int i=0;i<10;i++) {
            double a=-Math.PI/2d+i*Math.PI/5d;
            float r=(i%2==0)?radius:radius*.45f;
            float x=cx+(float)Math.cos(a)*r,y=cy+(float)Math.sin(a)*r;
            if(i==0) target.moveTo(x,y);else target.lineTo(x,y);
        }
        target.close();
    }

    private void fitNumber(String value,float cx,float cy,float maxW,float maxH) {
        source.reset();
        type.getTextPath(value,0,value.length(),0f,0f,source);
        source.computeBounds(bounds,true);
        if(bounds.isEmpty()) return;
        float scale=Math.min(maxW/bounds.width(),maxH/bounds.height());
        matrix.reset();matrix.setScale(scale,scale);
        matrix.postTranslate(cx-bounds.centerX()*scale,cy-bounds.centerY()*scale);
        source.transform(matrix,number);
    }
}
