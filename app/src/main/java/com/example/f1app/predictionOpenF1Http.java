package com.example.f1app;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;

public final class predictionOpenF1Http {
    private predictionOpenF1Http(){}
    private static final Map<String,Entry> CACHE=new LinkedHashMap<String,Entry>(16,0.75f,true);
    private static final ConcurrentHashMap<String,CompletableFuture<String>> FLIGHT=new ConcurrentHashMap<>();
    private static long lastStart;

    private static final class Entry {
        final String body;
        final long at=System.nanoTime();
        Entry(String b){
            body=b;
        }
    }

    public static void clearCache(){
        synchronized(CACHE){
            CACHE.clear();
        }
    }

    public static void invalidate(String key){
        synchronized(CACHE){
            CACHE.remove(key);
        }
    }

    public static String get(String address) throws Exception {
        synchronized(CACHE) {
            Entry e=CACHE.get(address);
            if(e!=null&&(System.nanoTime()-e.at)/1000000<10*60*1000)return e.body;
            CACHE.remove(address);
        }
        CompletableFuture<String> own=new CompletableFuture<>(),existing=FLIGHT.putIfAbsent(address,own);

        if (existing!=null){
            try{
                return existing.get();
            }
            catch(ExecutionException e){
                throw new IOException(e.getCause());
            }
        }
        try {
            String body=download(address);
            synchronized(CACHE){
                CACHE.put(address,new Entry(body));
                while (CACHE.size()>64){
                    CACHE.remove(CACHE.keySet().iterator().next());
                }
            }
            own.complete(body);
            return body;
        }
        catch(Exception e){
            own.completeExceptionally(e);
            throw e;
        }

        finally{
            FLIGHT.remove(address,own);
        }
    }

    private static synchronized void pace() throws InterruptedException {
        long wait=1000-(System.nanoTime()-lastStart)/1000000;
        if (wait>0) {
            Thread.sleep(wait);
            lastStart = System.nanoTime();
        }
    }

    private static String download(String address)throws Exception {
        IOException failure = null;
        for (int attempt = 0; attempt < 2; attempt++) {
            pace();
            long delay = 2000;
            HttpURLConnection c=(HttpURLConnection) new URL(address).openConnection();
            c.setConnectTimeout(10000);
            c.setReadTimeout(15000);
            c.setRequestProperty("Accept","application/json");
            try {
                int status = c.getResponseCode();
                if (status != 200) {
                    boolean retry=status==429 || status==502 || status==503 || status==504;
                    if (!retry || attempt == 1){
                        throw new IOException("OpenF1 HTTP "+status+"; "+address);
                    }

                    String raw = c.getHeaderField("Retry-After");
                    if (raw != null){
                        try{
                            delay = Math.max(delay,Long.parseLong(raw)*1000);
                        } catch(Exception e){
                            throw new IOException("Retry later: "+raw);
                        }
                    }

                    if (delay > 30000){
                        throw new IOException("OpenF1 retry later; "+address);
                    }
                    failure = new IOException("OpenF1 HTTP "+status);
                } else {
                    try (InputStream in = c.getInputStream(); ByteArrayOutputStream out=new ByteArrayOutputStream()) {
                        byte[] buf=new byte[8192];
                        int n;
                        while((n = in.read(buf)) != -1){
                            if (out.size() + n > 8*1024*1024)throw new IOException("OpenF1 response too large");out.write(buf,0,n);}
                        return new String(out.toByteArray(),StandardCharsets.UTF_8);
                    }
                }
            }catch(SocketTimeoutException e){
                failure = new IOException("OpenF1 timeout; "+address,e);
                if(attempt==1) throw failure;
            }
            finally{c.disconnect();}
            Thread.sleep(delay);
        }
        throw failure==null?new IOException("OpenF1 request failed"):failure;
    }
}
