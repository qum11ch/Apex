package com.example.f1app;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.RelativeLayout;

public class StripedRelativeLayout extends RelativeLayout {

    public StripedRelativeLayout(Context context) {
        super(context);
    }

    public StripedRelativeLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public StripedRelativeLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected int getSuggestedMinimumHeight() {
        return getMinimumHeight();
    }
}