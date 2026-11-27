package com.example.f1app;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.time.LocalDate;

public class weatherForecastTest {
    @Test
    public void mapsWmoCodesToCategories() {
        assertEquals(weatherForecast.CLEAR, weatherForecast.category(0));
        assertEquals(weatherForecast.PARTLY, weatherForecast.category(2));
        assertEquals(weatherForecast.RAIN, weatherForecast.category(61));
        assertEquals(weatherForecast.RAIN, weatherForecast.category(81));
        assertEquals(weatherForecast.SNOW, weatherForecast.category(73));
        assertEquals(weatherForecast.STORM, weatherForecast.category(95));
    }

    @Test
    public void roundsTimeToTheNearestHour() {
        long hour = 3_600_000L;
        assertEquals(10 * hour, weatherForecast.hourKey(10 * hour + 20 * 60_000L));
        assertEquals(11 * hour, weatherForecast.hourKey(10 * hour + 40 * 60_000L));
    }

    @Test
    public void horizonIsSixteenDaysIncludingToday() {
        assertEquals(LocalDate.of(2026, 12, 11),
                weatherForecast.horizonEnd(LocalDate.of(2026, 11, 26)));
    }
}