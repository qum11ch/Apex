package com.example.f1app;

/** The weather of one session, ready to show. */
final class sessionWeather {
    final int icon;
    final String temp;   // "27\u00B0"
    final String detail; // "0%, 4 m/s"
    final String text;   // "27\u00B0, 0%, 4 m/s"
    final boolean wet;

    sessionWeather(int icon, String temp, String detail, boolean wet) {
        this.icon = icon;
        this.temp = temp;
        this.detail = detail;
        this.text = temp + ", " + detail;
        this.wet = wet;
    }
}