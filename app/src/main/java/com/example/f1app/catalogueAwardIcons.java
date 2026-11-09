package com.example.f1app;

import android.net.Uri;
import com.google.firebase.storage.FirebaseStorage;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BooleanSupplier;

final class catalogueAwardIcons {
    private static final Map<String,Uri> LOGOS=new ConcurrentHashMap<>();
    private catalogueAwardIcons() {}
    static void bind(awardIconView icon, awardCatalogue.Entry entry, Map<String,Object> badge, String description, BooleanSupplier active) {
        if("fan_driver".equals(entry.id)) {
            String code=badge==null?"": profileAwardPresentation.string(badge,"code");
            int titles=badge==null?0: profileAwardPresentation.titles(badge.get("championshipsCount"));
            icon.showDriver(code,titles,description);
        } else if("fan_team".equals(entry.id)) {
            String path=badge==null?"": profileAwardPresentation.string(badge,"storagePath");
            String fallback=badge==null?"": profileAwardPresentation.string(badge,"code");
            Uri cached=LOGOS.get(path);icon.showTeam(cached,fallback,description);
            if(cached==null&&path.matches("teams/[a-z0-9_-]+_logo(_alt)?\\.png")) {
                FirebaseStorage.getInstance().getReference().child(path).getDownloadUrl().addOnSuccessListener(uri->{
                    LOGOS.put(path,uri);if(active.getAsBoolean())icon.showTeam(uri,fallback,description);
                }).addOnFailureListener(error->android.util.Log.w("AwardCatalogue","Logo unavailable",error));
            }
        } else icon.showMedal(profileAwardPresentation.drawable(entry.id),description);
    }
}
