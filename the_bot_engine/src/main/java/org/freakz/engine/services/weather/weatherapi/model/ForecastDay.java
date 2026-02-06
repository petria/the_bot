package org.freakz.engine.services.weather.weatherapi.model;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDate;
import java.util.List;

public record ForecastDay(
    @JsonFormat(pattern = "yyyy-MM-dd")
    LocalDate date,
    Long date_epoch,
    Astro astro, List<Hour> hour) {
}
