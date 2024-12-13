package org.freakz.engine.services.weather.weatherapi.model;

import java.util.List;

public record Forecast(List<ForecastDay> forecastday) {}
