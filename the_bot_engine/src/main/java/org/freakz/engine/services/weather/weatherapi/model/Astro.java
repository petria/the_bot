package org.freakz.engine.services.weather.weatherapi.model;

public record Astro(
    String sunrise,
    String sunset,
    String moonrise,
    String moonset,
    String moon_phase,
    String moon_illumination,
    int is_moon_up,
    int is_sun_up) {
}
