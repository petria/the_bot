package org.freakz.engine.services.weather.weatherapi.model;


public record ForecastResponse(Location location, Current current) { //, Forecast forecast, Alerts alerts) {
}