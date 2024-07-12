package org.freakz.engine.dto.weather;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.freakz.engine.services.api.ServiceResponse;
import org.freakz.engine.services.weather.weatherapi.model.AstronomyResponse;
import org.freakz.engine.services.weather.weatherapi.model.ErrorResponse;
import org.freakz.engine.services.weather.weatherapi.model.ForecastResponse;

@Builder
@Data
@EqualsAndHashCode(callSuper = false)
public class WeatherAPIResponse extends ServiceResponse {

    private ForecastResponse forecastResponseModel;
    private AstronomyResponse astronomyResponse;
    private ErrorResponse errorResponse;

}
