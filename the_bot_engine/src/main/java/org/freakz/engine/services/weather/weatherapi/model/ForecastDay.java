package org.freakz.engine.services.weather.weatherapi.model;

import java.time.LocalDate;
import java.util.List;

public record ForecastDay(LocalDate date, Long date_epoch, Astro astro, List<Hour> hour) {
}
