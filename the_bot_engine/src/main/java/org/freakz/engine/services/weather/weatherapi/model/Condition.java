package org.freakz.engine.services.weather.weatherapi.model;

public record Condition(
        String text,
        String icon,
        int code
) {
}
