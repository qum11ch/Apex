package com.example.f1app;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.Year;

/** Pure calculations; missing data is not interpreted as zero. */
final class awardProgress {
    private awardProgress() {}
    static Integer completedYears(Long createdAt,long now) {
        if(createdAt==null||createdAt<=0||createdAt>now)return null;
        ZonedDateTime start=Instant.ofEpochMilli(createdAt).atZone(ZoneOffset.UTC);
        ZonedDateTime end=Instant.ofEpochMilli(now).atZone(ZoneOffset.UTC);
        int years=end.getYear()-start.getYear();
        ZonedDateTime anniversary=start.plusYears(years);
        // Match the supplied JavaScript backend: February 29 rolls to March 1.
        if(start.getMonthValue()==2&&start.getDayOfMonth()==29&&!Year.isLeap(end.getYear()))anniversary=anniversary.plusDays(1);
        if(end.isBefore(anniversary))years--;
        return Math.max(0,years);
    }
    static Long count(Object raw) {
        if(!(raw instanceof Number))return null;
        double value=((Number)raw).doubleValue();
        if(!Double.isFinite(value)||value<0||value!=Math.rint(value)||value>=Long.MAX_VALUE)return null;
        return ((Number)raw).longValue();
    }
    static Long current(awardCatalogue.Entry entry, Long correct, Integer years) {
        if(entry.category== awardCatalogue.PREDICTIONS)return correct;
        if(entry.category== awardCatalogue.TENURE)return years==null?null:years.longValue();
        return null;
    }
    static boolean conditionMet(awardCatalogue.Entry entry, Long correct, Integer years) {
        Long current=current(entry,correct,years);return current!=null&&entry.target>0&&current>=entry.target;
    }
}
