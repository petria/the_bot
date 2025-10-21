package org.freakz.engine.services.weather.weatherapi.model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.freakz.engine.services.weather.weatherapi.CustomLocalDateTimeDeserializer;

import java.time.LocalDateTime;

public record Hour(
    Long time_epoch,
    @JsonDeserialize(using = CustomLocalDateTimeDeserializer.class) LocalDateTime time,
    String temp_c,
    String temp_f,
    int is_day,
    Condition condition,
    String wind_mph,
    String wind_kph,
    String wind_degree,
    String wind_dir,
    String pressure_mb,
    String pressure_in,
    String precip_mm,
    String precip_in,
    String snow_cm,
    String humidity,
    String cloud,
    Double feelslike_c,
    Double feelslike_f,
    Double windchill_c,
    Double windchill_f,
    Double heatindex_c,
    Double heatindex_f,
    Double dewpoint_c,
    Double dewpoint_f,
    Double will_it_rain,
    Double chance_of_rain,
    Double will_it_snow,
    Double chance_of_snow,
    Double vis_km,
    Double vis_miles,
    Double gust_mph,
    Double gust_kph,
    Double uv,
    Double short_rad,
    Double diff_rad) {
}
