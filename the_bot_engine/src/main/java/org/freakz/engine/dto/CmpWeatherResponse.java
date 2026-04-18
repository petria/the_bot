package org.freakz.engine.dto;

import org.freakz.engine.services.api.ServiceResponse;
import org.freakz.engine.services.weather.weatherapi.model.ForecastResponse;

import java.util.List;

public class CmpWeatherResponse extends ServiceResponse {

  private List<ForecastResponse> forecastResponses;

  public CmpWeatherResponse() {
  }

  public CmpWeatherResponse(List<ForecastResponse> forecastResponses) {
    this.forecastResponses = forecastResponses;
  }

  public static Builder builder() {
    return new Builder();
  }

  public List<ForecastResponse> getForecastResponses() {
    return forecastResponses;
  }

  public void setForecastResponses(List<ForecastResponse> forecastResponses) {
    this.forecastResponses = forecastResponses;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    CmpWeatherResponse that = (CmpWeatherResponse) o;

    return forecastResponses != null ? forecastResponses.equals(that.forecastResponses) : that.forecastResponses == null;
  }

  @Override
  public int hashCode() {
    return forecastResponses != null ? forecastResponses.hashCode() : 0;
  }

  @Override
  public String toString() {
    return "CmpWeatherResponse{" +
        "forecastResponses=" + forecastResponses +
        '}';
  }

  public static class Builder {
    private List<ForecastResponse> forecastResponses;

    Builder() {
    }

    public Builder forecastResponses(List<ForecastResponse> forecastResponses) {
      this.forecastResponses = forecastResponses;
      return this;
    }

    public CmpWeatherResponse build() {
      return new CmpWeatherResponse(forecastResponses);
    }
  }
}
