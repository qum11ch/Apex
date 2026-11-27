package com.example.f1app;

import android.widget.ImageView;

import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.Locale;

final class driverPhotos {

    private driverPhotos() {
    }

    static void load(ImageView target, String code, String season) {
        String driverCode = code == null
                ? "" : code.trim().toLowerCase(Locale.ROOT);
        String year = season == null ? "" : season.trim();
        String key = driverCode + "|" + year;

        if (key.equals(target.getTag(R.id.driver_photo_request_key))) {
            return;
        }

        target.setTag(R.id.driver_photo_request_key, key);

        if (driverCode.isEmpty()
                || year.isEmpty()
                || "n/a".equals(driverCode)) {
            GlideApp.with(target).clear(target);
            target.setImageResource(R.drawable.placeholder_driver);
            return;
        }

        StorageReference root = FirebaseStorage.getInstance().getReference();
        StorageReference primary = root.child(
                "drivers/" + driverCode + "_" + year + ".png");

        if ("2027".equals(year)) {
            StorageReference previous = root.child(
                    "drivers/" + driverCode + "_2026.png");

            GlideApp.with(target)
                    .load(primary)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .placeholder(R.drawable.placeholder_driver)
                    .error(
                            GlideApp.with(target)
                                    .load(previous)
                                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                                    .error(R.drawable.placeholder_driver)
                    )
                    .dontAnimate()
                    .into(target);
        } else {
            GlideApp.with(target)
                    .load(primary)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .placeholder(R.drawable.placeholder_driver)
                    .error(R.drawable.placeholder_driver)
                    .dontAnimate()
                    .into(target);
        }
    }

    static void clear(ImageView target) {
        target.setTag(R.id.driver_photo_request_key, null);
        GlideApp.with(target).clear(target);
        target.setImageDrawable(null);
    }
}