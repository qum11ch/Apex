package com.example.f1app;

import android.os.Handler;
import android.os.Looper;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.functions.FirebaseFunctions;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Short-lived, batched reads; no per-row database listeners. */
final class awardsRepository {
    interface Callback { void accept(List<Map<String,Object>> badges, Exception error); }
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static final Map<String,Entry> CACHE = new HashMap<>();
    private static final Map<String,List<Callback>> WAITING = new LinkedHashMap<>();
    private static final Map<String,Integer> VERSIONS = new HashMap<>();
    private static boolean scheduled;
    private static String authScope;
    private static final class Entry {
        long expires;
        int version;
        List<Map<String,Object>> badges;
    }
    static void invalidate(String uid) {
        CACHE.remove(uid);
        VERSIONS.put(uid, version(uid)+1);
    }
    private static int version(String uid) { return VERSIONS.containsKey(uid) ? VERSIONS.get(uid) : 0; }
    static void load(String uid, Callback callback) {
        String current = FirebaseAuth.getInstance().getUid();
        if (authScope == null ? current != null : !authScope.equals(current)) {
            CACHE.clear();
            for (String key : new ArrayList<>(VERSIONS.keySet())) VERSIONS.put(key,version(key)+1);
            authScope=current;
        }
        if (!VERSIONS.containsKey(uid)) VERSIONS.put(uid,0);
        Entry entry=CACHE.get(uid);
        if (entry!=null && entry.expires>android.os.SystemClock.elapsedRealtime() && entry.version==version(uid)) {
            callback.accept(entry.badges,null); return;
        }
        List<Callback> callbacks=WAITING.get(uid);
        if(callbacks==null) {callbacks=new ArrayList<>();WAITING.put(uid,callbacks);}
        callbacks.add(callback);
        if(!scheduled) {scheduled=true;MAIN.postDelayed(awardsRepository::flush,35);}
    }
    private static void flush() {
        scheduled=false;
        List<String> all=new ArrayList<>(WAITING.keySet());
        for(int offset=0;offset<all.size();offset+=20) {
            List<String> uids=new ArrayList<>(all.subList(offset,Math.min(all.size(),offset+20)));
            Map<String,List<Callback>> calls=new HashMap<>();
            Map<String,Integer> versions=new HashMap<>();
            for(String uid:uids) {calls.put(uid,WAITING.remove(uid));versions.put(uid,version(uid));}
            Map<String,Object> request=new HashMap<>();request.put("uids",uids);
            FirebaseFunctions.getInstance("us-central1").getHttpsCallable("getPublicAwards").call(request)
                .addOnSuccessListener(result->{
                    try {
                        Object raw=result.getData();
                        if(!(raw instanceof Map)) throw new IllegalStateException("Invalid awards response");
                        Object profiles=((Map<?,?>)raw).get("profiles");
                        if(!(profiles instanceof List)) throw new IllegalStateException("Missing profiles");
                        Map<String,List<Map<String,Object>>> parsed=new HashMap<>();
                        for(Object item:(List<?>)profiles) {
                            if(!(item instanceof Map)) throw new IllegalStateException("Invalid profile");
                            Map<?,?> p=(Map<?,?>)item;
                            if(!(p.get("uid") instanceof String)) throw new IllegalStateException("Missing UID");
                            if(!Boolean.TRUE.equals(p.get("available"))) continue;
                            Object values=p.get("badges");
                            if(!(values instanceof List)) throw new IllegalStateException("Missing badge list");
                            List<Map<String,Object>> list=new ArrayList<>();
                            for(Object value:(List<?>)values) {
                                if(!(value instanceof Map)) throw new IllegalStateException("Invalid badge");
                                Map<String,Object> badge=new HashMap<>();
                                for(Map.Entry<?,?> field:((Map<?,?>)value).entrySet()) if(field.getKey() instanceof String) badge.put((String)field.getKey(),field.getValue());
                                list.add(badge);
                            }
                            parsed.put((String)p.get("uid"),list);
                        }
                        for(String uid:uids) {
                            if(versions.get(uid)!=version(uid)) {for(Callback cb:calls.remove(uid)) load(uid,cb);continue;}
                            List<Map<String,Object>> badges=parsed.get(uid);
                            if(badges==null) {for(Callback cb:calls.remove(uid)) cb.accept(null,new IllegalStateException("Awards unavailable"));continue;}
                            Entry entry=new Entry();entry.badges=badges;entry.version=version(uid);entry.expires=android.os.SystemClock.elapsedRealtime()+45000;
                            if(CACHE.size()>=200) CACHE.clear();
                            CACHE.put(uid,entry);
                            for(Callback cb:calls.remove(uid)) cb.accept(badges,null);
                        }
                    } catch(RuntimeException error) {fail(calls,error);}
                }).addOnFailureListener(error->fail(calls,error));
        }
    }
    private static void fail(Map<String,List<Callback>> calls,Exception error) {
        for(List<Callback> callbacks:calls.values()) for(Callback callback:callbacks) callback.accept(null,error);
    }
}
