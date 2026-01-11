package org.freakz.engine.dto.weather;

import org.freakz.engine.services.api.ServiceResponse;
import org.freakz.engine.services.weather.weatherapi.model.AstronomyResponse;
import org.freakz.engine.services.weather.weatherapi.model.ErrorResponse;
import org.freakz.engine.services.weather.weatherapi.model.ForecastResponse;

public class WeatherAPIResponse extends ServiceResponse {

  private ForecastResponse forecastResponseModel;
  private AstronomyResponse astronomyResponse;
  private ErrorResponse errorResponse;

  public WeatherAPIResponse() {
  }

  public WeatherAPIResponse(ForecastResponse forecastResponseModel, AstronomyResponse astronomyResponse, ErrorResponse errorResponse) {
    this.forecastResponseModel = forecastResponseModel;
    this.astronomyResponse = astronomyResponse;
    this.errorResponse = errorResponse;
  }

  public ForecastResponse getForecastResponseModel() {
    return forecastResponseModel;
  }

  public void setForecastResponseModel(ForecastResponse forecastResponseModel) {
    this.forecastResponseModel = forecastResponseModel;
  }

  public AstronomyResponse getAstronomyResponse() {
    return astronomyResponse;
  }

  public void setAstronomyResponse(AstronomyResponse astronomyResponse) {
    this.astronomyResponse = astronomyResponse;
  }

  public ErrorResponse getErrorResponse() {
    return errorResponse;
  }

  public void setErrorResponse(ErrorResponse errorResponse) {
    this.errorResponse = errorResponse;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    WeatherAPIResponse that = (WeatherAPIResponse) o;

    if (forecastResponseModel != null ? !forecastResponseModel.equals(that.forecastResponseModel) : that.forecastResponseModel != null)
      return false;
    if (astronomyResponse != null ? !astronomyResponse.equals(that.astronomyResponse) : that.astronomyResponse != null)
      return false;
    return errorResponse != null ? errorResponse.equals(that.errorResponse) : that.errorResponse == null;
  }

  @Override
  public int hashCode() {
    int result = forecastResponseModel != null ? forecastResponseModel.hashCode() : 0;
    result = 31 * result + (astronomyResponse != null ? astronomyResponse.hashCode() : 0);
    result = 31 * result + (errorResponse != null ? errorResponse.hashCode() : 0);
    return result;
  }

  @Override
  public String toString() {
    return "WeatherAPIResponse{" +
        "forecastResponseModel=" + forecastResponseModel +
        ", astronomyResponse=" + astronomyResponse +
        ", errorResponse=" + errorResponse +
        '}';
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private ForecastResponse forecastResponseModel;
    private AstronomyResponse astronomyResponse;
    private ErrorResponse errorResponse;

    Builder() {
    }

    public Builder forecastResponseModel(ForecastResponse forecastResponseModel) {
      this.forecastResponseModel = forecastResponseModel;
      return this;
    }

    public Builder astronomyResponse(AstronomyResponse astronomyResponse) {
      this.astronomyResponse = astronomyResponse;
      return this;
    }

    public Builder errorResponse(ErrorResponse errorResponse) {
      this.errorResponse = errorResponse;
      return this;
    }

    public WeatherAPIResponse build() {
      return new WeatherAPIResponse(forecastResponseModel, astronomyResponse, errorResponse);
    }
  }
}

