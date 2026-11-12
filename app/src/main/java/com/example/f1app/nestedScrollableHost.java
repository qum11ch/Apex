package com.example.f1app;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.FrameLayout;

/**
 * Wraps a horizontally scrollable view that lives inside the horizontal ViewPager2 of the team page:
 * the inner list scrolls while it can, and hands the swipe to the tabs at its first and last page.
 * (The standard NestedScrollableHost helper from the ViewPager2 samples.)
 */
public class nestedScrollableHost extends FrameLayout {

    private final int touchSlop;
    private float initialX = 0f;
    private float initialY = 0f;

    public nestedScrollableHost(Context context) {
        this(context, null);
    }

    public nestedScrollableHost(Context context, AttributeSet attrs) {
        super(context, attrs);
        touchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
    }

    private View child() {
        return getChildCount() > 0 ? getChildAt(0) : null;
    }

    private boolean canChildScroll(float delta) {
        int direction = (int) -Math.signum(delta);
        View child = child();

        return child != null && child.canScrollHorizontally(direction);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent e) {
        handleInterceptTouchEvent(e);
        return super.onInterceptTouchEvent(e);
    }

    private void handleInterceptTouchEvent(MotionEvent e) {
        if (!canChildScroll(-1f) && !canChildScroll(1f)) {
            return;
        }

        ViewGroup parent = getParent() instanceof ViewGroup ? (ViewGroup) getParent() : null;

        if (parent == null) {
            return;
        }

        if (e.getAction() == MotionEvent.ACTION_DOWN) {
            initialX = e.getX();
            initialY = e.getY();
            parent.requestDisallowInterceptTouchEvent(true);
        } else if (e.getAction() == MotionEvent.ACTION_MOVE) {
            float dx = e.getX() - initialX;
            float dy = e.getY() - initialY;

            float scaledDx = Math.abs(dx) * 0.5f;
            float scaledDy = Math.abs(dy);

            if (scaledDx > touchSlop || scaledDy > touchSlop) {
                if (scaledDy > scaledDx) {
                    // vertical gesture: the page scrolls, let everyone intercept
                    parent.requestDisallowInterceptTouchEvent(false);
                } else {
                    // horizontal gesture: the list keeps it while it can scroll that way
                    parent.requestDisallowInterceptTouchEvent(canChildScroll(dx));
                }
            }
        }
    }
}
