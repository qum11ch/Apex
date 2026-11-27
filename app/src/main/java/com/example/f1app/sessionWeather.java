package com.example.f1app;

/** The weather of one session, ready to show. */
final class sessionWeather {
    final int icon;
    final boolean iconRain;
    final String temp;      // "27\u00B0"
    final String rain;      // "0%", "6.1 mm", "dry", "rain"
    final String wind;      // "4 m/s"
    final String detail;    // rain + ", " + wind
    final String text;      // temp + ", " + detail
    final boolean wet;
    final boolean measured; // F1 trackside measurements instead of a model

    // for the details sheet; NaN when unknown
    final double air, track, humidity;
    final String windFull;  // "5 m/s, SW"
    final String trackState;

    sessionWeather(int icon, boolean iconRain, String temp, String rain, String wind, boolean wet) {
        this(icon, iconRain, temp, rain, wind, wet, false,
                Double.NaN, Double.NaN, Double.NaN, wind, "");
    }

    sessionWeather(int icon, boolean iconRain, String temp, String rain, String wind, boolean wet,
                   boolean measured, double air, double track, double humidity,
                   String windFull, String trackState) {
        this.icon = icon;
        this.iconRain = iconRain;
        this.temp = temp;
        this.rain = rain;
        this.wind = wind;
        this.detail = rain + ", " + wind;
        this.text = temp + ", " + detail;
        this.wet = wet;
        this.measured = measured;
        this.air = air;
        this.track = track;
        this.humidity = humidity;
        this.windFull = windFull;
        this.trackState = trackState;
    }
}