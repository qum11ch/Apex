package com.example.f1app;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;

/** Makes "Open-Meteo.com" in a caption a link, as the licence of the data asks. */
final class sourceLink {

    private static final String BRAND = "Open-Meteo.com";
    private static final String URL = "https://open-meteo.com/";

    private sourceLink() {
    }

    static void apply(TextView view, CharSequence text) {
        String value = text == null ? "" : text.toString();

        // the bare source name becomes the sentence of the licence: "Weather data by Open-Meteo.com"
        if (BRAND.equals(value)) {
            value = view.getContext().getString(R.string.weather_credit, BRAND);
        }

        int start = value.indexOf(BRAND);

        if (start < 0) {
            view.setText(value);
            return;
        }

        SpannableString span = new SpannableString(value);
        span.setSpan(new ClickableSpan() {
            @Override
            public void onClick(@NonNull View widget) {
                widget.getContext().startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(URL)));
            }

            @Override
            public void updateDrawState(@NonNull TextPaint paint) {
                paint.setUnderlineText(true); // the colour of the caption stays
            }
        }, start, start + BRAND.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        view.setText(span);
        view.setMovementMethod(LinkMovementMethod.getInstance());
        view.setHighlightColor(Color.TRANSPARENT);
        view.setClickable(false); // a touch outside the link goes to the card behind
    }
}