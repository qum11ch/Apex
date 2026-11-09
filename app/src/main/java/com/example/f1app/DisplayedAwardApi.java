package com.example.f1app;

import com.google.firebase.functions.FirebaseFunctions;
import java.util.HashMap;
import java.util.Map;

final class DisplayedAwardApi {
    static final class Selection {
        final String id;
        final Map<String,Object> award;
        Selection(String id,Map<String,Object> award){this.id=id;this.award=award;}
    }
    interface Callback {void accept(Selection value,Exception error);}
    private DisplayedAwardApi() {}
    private static Selection parse(Object raw) {
        if(!(raw instanceof Map))throw new IllegalStateException("Invalid displayed award response");
        Map<?,?> root=(Map<?,?>)raw;
        if(!root.containsKey("selectedAwardId")||!root.containsKey("award"))throw new IllegalStateException("Missing selection fields");
        Object id=root.get("selectedAwardId"),award=root.get("award");
        if(id==null&&award==null)return new Selection(null,null);
        if(!(id instanceof String)||!(award instanceof Map)||AwardCatalogue.find((String)id)==null)throw new IllegalStateException("Invalid selected award");
        Map<String,Object> result=new HashMap<>();
        for(Map.Entry<?,?> e:((Map<?,?>)award).entrySet())if(e.getKey() instanceof String)result.put((String)e.getKey(),e.getValue());
        if(!id.equals(result.get("id")))throw new IllegalStateException("Selection ID mismatch");
        return new Selection((String)id,result);
    }
    static void get(String uid,Callback callback) {
        Map<String,Object> payload=new HashMap<>();payload.put("profileUid",uid);
        FirebaseFunctions.getInstance("us-central1").getHttpsCallable("getDisplayedAward").call(payload)
            .addOnSuccessListener(result->{
                Selection value;
                try{value=parse(result.getData());}catch(RuntimeException error){callback.accept(null,error);return;}
                callback.accept(value,null);
            }).addOnFailureListener(error->callback.accept(null,error));
    }
    static void set(String id,Callback callback) {
        Map<String,Object> payload=new HashMap<>();payload.put("awardId",id);
        FirebaseFunctions.getInstance("us-central1").getHttpsCallable("setDisplayedAward").call(payload)
            .addOnSuccessListener(result->{
                Selection value;
                try {
                    Object raw=result.getData();
                    if(!(raw instanceof Map)||!Boolean.TRUE.equals(((Map<?,?>)raw).get("success")))throw new IllegalStateException("Save not confirmed");
                    value=parse(raw);
                    if(!java.util.Objects.equals(id,value.id))throw new IllegalStateException("Saved ID mismatch");
                }catch(RuntimeException error){callback.accept(null,error);return;}
                callback.accept(value,null);
            }).addOnFailureListener(error->callback.accept(null,error));
    }
}
