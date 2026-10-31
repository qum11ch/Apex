package com.example.f1app;

import android.os.Build;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public final class EdgeInsets {

    private EdgeInsets() {}

    private static Insets safe(WindowInsetsCompat w) {
        return w.getInsets(
                WindowInsetsCompat.Type.systemBars()
                        | WindowInsetsCompat.Type.displayCutout()
        );
    }

    public static void transparentNavigation(Window window) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.setNavigationBarContrastEnforced(false);
        }
    }

    public static void applySides(View view) {
        if (view == null) return;
        final int l = view.getPaddingLeft();
        final int t = view.getPaddingTop();
        final int r = view.getPaddingRight();
        final int b = view.getPaddingBottom();

        ViewCompat.setOnApplyWindowInsetsListener(view, (v, w) -> {
            Insets s = safe(w);
            v.setPadding(l + s.left, t, r + s.right, b);
            return w; // не потребляем: дети тоже получат отступы
        });
        ViewCompat.requestApplyInsets(view);
    }

    public static void applyTop(View view) {
        if (view == null) return;
        final int l = view.getPaddingLeft();
        final int t = view.getPaddingTop();
        final int r = view.getPaddingRight();
        final int b = view.getPaddingBottom();

        ViewCompat.setOnApplyWindowInsetsListener(view, (v, w) -> {
            v.setPadding(l, t + safe(w).top, r, b);
            return w;
        });
        ViewCompat.requestApplyInsets(view);
    }

    public static void applyBottom(View view) {
        if (view == null) return;
        final int l = view.getPaddingLeft();
        final int t = view.getPaddingTop();
        final int r = view.getPaddingRight();
        final int b = view.getPaddingBottom();

        if (view instanceof ViewGroup) {
            ((ViewGroup) view).setClipToPadding(false);
        }

        ViewCompat.setOnApplyWindowInsetsListener(view, (v, w) -> {
            v.setPadding(l, t, r, b + safe(w).bottom);
            return w;
        });

        if (view.isAttachedToWindow()) {
            ViewCompat.requestApplyInsets(view);
        } else {
            view.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
                @Override public void onViewAttachedToWindow(View v) {
                    v.removeOnAttachStateChangeListener(this);
                    ViewCompat.requestApplyInsets(v);
                }
                @Override public void onViewDetachedFromWindow(View v) {}
            });
        }
    }
}