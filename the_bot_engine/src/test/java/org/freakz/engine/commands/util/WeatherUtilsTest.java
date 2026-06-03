package org.freakz.engine.commands.util;

import org.freakz.engine.dto.weather.WeatherAPIResponse;
import org.freakz.engine.services.weather.weatherapi.model.Condition;
import org.freakz.engine.services.weather.weatherapi.model.Current;
import org.freakz.engine.services.weather.weatherapi.model.ForecastResponse;
import org.freakz.engine.services.weather.weatherapi.model.Location;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class WeatherUtilsTest {

  @Test
  void formatsWeatherResponseLikeWeatherCommandDefaultOutput() {
    WeatherAPIResponse response = weatherResponse(
        forecast("Turku", "Varsinais-Suomi", "Finland", "2026-06-03 21:40", "12.4", 10.2));

    assertThat(WeatherUtils.formatWeatherResponse(response, "turku", false, false, false))
        .isEqualTo("Turku: 21:40, 12.4°C");
  }

  @Test
  void formatsWeatherResponseWithVerboseNameAndFeelsLikeWhenRequested() {
    WeatherAPIResponse response = weatherResponse(
        forecast("Turku", "Varsinais-Suomi", "Finland", "2026-06-03 21:40", "12.4", 10.2));

    assertThat(WeatherUtils.formatWeatherResponse(response, "turku", true, true, false))
        .isEqualTo("Turku/Varsinais-Suomi/Finland: 21:40, 12.4°C (feels like 10.2°C)");
  }

  private ForecastResponse forecast(
      String name,
      String region,
      String country,
      String lastUpdated,
      String tempC,
      double feelsLikeC) {
    return new ForecastResponse(
        null,
        new Location(name, region, country, "60.45", "22.27", "Europe/Helsinki", 0L, LocalDateTime.now()),
        new Current(
            0L,
            lastUpdated,
            tempC,
            "54.3",
            1,
            new Condition("Clear", "", 1000),
            0.0,
            0.0,
            0.0,
            "N",
            0.0,
            0.0,
            0.0,
            0.0,
            0,
            0,
            feelsLikeC,
            50.0,
            0.0,
            0.0,
            0.0,
            0.0,
            0.0,
            0.0,
            0.0,
            0.0,
            0.0,
            0.0,
            0.0,
            null),
        null);
  }

  private WeatherAPIResponse weatherResponse(ForecastResponse forecast) {
    WeatherAPIResponse response = WeatherAPIResponse.builder()
        .forecastResponseModel(forecast)
        .build();
    response.setStatus("OK: WeatherAPI service");
    return response;
  }
}
