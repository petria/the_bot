package org.freakz.engine.services.weather.weatherapi.model;

public record ForecastResponse(
    ErrorCode errorCode, Location location, Current current, Forecast forecast
    // , Forecast forecast, Alerts alerts) {
    ) {}
