package org.freakz.engine.services.weather.weatherapi.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record Current(
        Long last_updated_epoch,
        String last_updated,
        String temp_c,
        String temp_f,
        int is_day,
        Condition condition,
        Double wind_mph,
        Double wind_kph,
        Double wind_degree,
        String wind_dir,
        Double pressure_mb,
        Double pressure_in,
        Double precip_mm,
        Double precip_in,
        int humidity,
        int cloud,
        Double feelslike_c,
        Double feelslike_f,
        Double windchill_c,
        Double windchill_f,
        Double heatindex_c,
        Double heatindex_f,
        Double dewpoint_c,
        Double dewpoint_f,
        Double vis_km,
        Double vis_miles,
        Double uv,
        Double gust_mph,
        Double gust_kph,
        @JsonProperty("air_quality")
        AirQuality air_quality

) {
}
