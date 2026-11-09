package com.example.f1app;

import android.net.Uri;
import com.google.firebase.storage.FirebaseStorage;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BooleanSupplier;

final class CatalogueAwardIcons {
    private static final Map<String,Uri> LOGOS=new ConcurrentHashMap<>();
    private CatalogueAwardIcons() {}
    static void bind(AwardIconView icon,AwardCatalogue.Entry entry,Map<String,Object> badge,String description,BooleanSupplier active) {
        if("fan_driver".equals(entry.id)) {
            String code=badge==null?"":ProfileAwardPresentation.string(badge,"code");
            int titles=badge==null?0:ProfileAwardPresentation.titles(badge.get("championshipsCount"));
            icon.showDriver(code,titles,description);
        } else if("fan_team".equals(entry.id)) {
            String path=badge==null?"":ProfileAwardPresentation.string(badge,"storagePath");
            String fallback=badge==null?"":ProfileAwardPresentation.string(badge,"code");
            Uri cached=LOGOS.get(path);icon.showTeam(cached,fallback,description);
            if(cached==null&&path.matches("teams/[a-z0-9_-]+_logo(_alt)?\\.png")) {
                FirebaseStorage.getInstance().getReference().child(path).getDownloadUrl().addOnSuccessListener(uri->{
                    LOGOS.put(path,uri);if(active.getAsBoolean())icon.showTeam(uri,fallback,description);
                }).addOnFailureListener(error->android.util.Log.w("AwardCatalogue","Logo unavailable",error));
            }
        } else icon.showMedal(ProfileAwardPresentation.drawable(entry.id),description);
    }
}
