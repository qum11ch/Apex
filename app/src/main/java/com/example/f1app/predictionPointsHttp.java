package com.example.f1app;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;

public final class predictionPointsHttp {
    private predictionPointsHttp() {}
    private static final long TTL_MS=5*60*1000L;
    private static final int MAX_ENTRIES=96;
    private static final Map<String,Entry> CACHE=new LinkedHashMap<String,Entry>(16,0.75f,true);
    private static final ConcurrentHashMap<String,CompletableFuture<String>> FLIGHT=new ConcurrentHashMap<>();
    private static final Object PACE=new Object();
    private static long lastStartNanos;
    private static final class Entry {
        final String body; final long at;
        Entry(String body) {this.body=body;this.at=System.nanoTime();}
    }
    public static void invalidate(String url) {synchronized(CACHE){CACHE.remove(url);}}
    public static void clearCache() {synchronized(CACHE){CACHE.clear();}}
    public static String get(String address) throws Exception {
        synchronized(CACHE) {
            Entry e=CACHE.get(address);
            if(e!=null&&(System.nanoTime()-e.at)/1000000L<TTL_MS)return e.body;
            CACHE.remove(address);
        }
        CompletableFuture<String> own=new CompletableFuture<>();
        CompletableFuture<String> other=FLIGHT.putIfAbsent(address,own);
        if(other!=null) {
            try{return other.get();}catch(ExecutionException e){throw new IOException(e.getCause());}
        }
        try {
            String body=download(address);
            synchronized(CACHE) {
                CACHE.put(address,new Entry(body));
                while(CACHE.size()>MAX_ENTRIES)CACHE.remove(CACHE.keySet().iterator().next());
            }
            own.complete(body);return body;
        } catch(Exception e) {own.completeExceptionally(e);throw e;}
        finally {FLIGHT.remove(address,own);}
    }
    private static void pace() throws InterruptedException {
        synchronized(PACE) {
            long wait=400-(System.nanoTime()-lastStartNanos)/1000000L;
            if(wait>0)Thread.sleep(wait);
            lastStartNanos=System.nanoTime();
        }
    }
    private static String download(String address) throws Exception {
        IOException last=null;
        for(int attempt=0;attempt<2;attempt++) {
            pace();
            HttpURLConnection c=(HttpURLConnection)new URL(address).openConnection();
            c.setConnectTimeout(10000);c.setReadTimeout(10000);
            c.setRequestProperty("Accept","application/json");
            c.setRequestProperty("Connection","close");
            long delay=1500;
            try {
                int status=c.getResponseCode();
                if(status!=200) {
                    boolean retry=status==429||status==502||status==503||status==504;
                    if(!retry||attempt==1)throw new IOException("HTTP "+status+"; "+address);
                    String raw=c.getHeaderField("Retry-After");
                    if(raw!=null) {
                        try {delay=Math.max(delay,Long.parseLong(raw.trim())*1000L);}
                        catch(NumberFormatException ignored){throw new IOException("HTTP "+status+"; Retry-After="+raw+"; "+address);}
                    }
                    if(delay>30000)throw new IOException("Retry later; HTTP "+status+"; "+address);
                    last=new IOException("HTTP "+status+"; "+address);
                } else {
                    try(InputStream in=c.getInputStream();ByteArrayOutputStream out=new ByteArrayOutputStream()) {
                        byte[] buffer=new byte[8192];int n;
                        while((n=in.read(buffer))!=-1) {
                            if(out.size()+n>8*1024*1024)throw new IOException("Response too large; "+address);
                            out.write(buffer,0,n);
                        }
                        return new String(out.toByteArray(),StandardCharsets.UTF_8);
                    }
                }
            } catch(SocketTimeoutException e) {
                last=new IOException("Timeout; "+address,e);
                if(attempt==1)throw last;
            } finally {c.disconnect();}
            Thread.sleep(delay);
        }
        throw last==null?new IOException("Request failed; "+address):last;
    }
}
