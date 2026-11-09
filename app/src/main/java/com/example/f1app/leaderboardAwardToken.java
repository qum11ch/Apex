package com.example.f1app;

/** Invalidates late results when a recycled row changes owner. */
final class leaderboardAwardToken {
    private String uid;
    private int generation;
    int bind(String uid){this.uid=uid;return ++generation;}
    void clear(){uid=null;generation++;}
    boolean matches(String expected,int request){return uid!=null&&uid.equals(expected)&&generation==request;}
}
