package org.freakz.engine.services.weather.weatherapi;

import static org.freakz.engine.commands.util.StaticArgumentStrings.ARG_ASTRONOMY;
import static org.freakz.engine.commands.util.StaticArgumentStrings.ARG_PLACE;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.freakz.engine.config.ConfigService;
import org.freakz.engine.dto.weather.WeatherAPIResponse;
import org.freakz.engine.services.api.*;
import org.freakz.engine.services.weather.weatherapi.model.AstronomyResponse;
import org.freakz.engine.services.weather.weatherapi.model.ErrorCode;
import org.freakz.engine.services.weather.weatherapi.model.ErrorResponse;
import org.freakz.engine.services.weather.weatherapi.model.ForecastResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Slf4j
@SpringServiceMethodHandler
@Service
public class WeatherAPIService {

    private final ConfigService configService;
    private final RestClient restClient;

    private final String weatherApiUrl;
    private final String weatherApiKey;


    public WeatherAPIService(ConfigService configService, WeatherConfigProperties properties) {
        this.configService = configService;
        this.weatherApiUrl = properties.apiUrl();
        this.weatherApiKey = properties.apiKey();
        this.restClient = RestClient.create(this.weatherApiUrl);
    }

    private AstronomyResponse getAstronomyData(String query) {
        AstronomyResponse r = restClient.get()
                .uri("/astronomy.json?key={key}&aqi=yes&alerts=yes&q={q}", weatherApiKey, query)
                .retrieve()
                .body(AstronomyResponse.class);

        return r;
    }

    @ServiceMessageHandlerMethod(ServiceRequestType = ServiceRequestType.WeatherAPIService)
    public <T extends ServiceResponse> WeatherAPIResponse handleForecaCmdServiceRequest(ServiceRequest request) {

        String query = request.getResults().getString(ARG_PLACE).toLowerCase();
        try {
            ForecastResponse r = restClient.get()
                    .uri("/forecast.json?key={key}&aqi=yes&days=2&alerts=yes&q={q}", weatherApiKey, query)
                    .retrieve()
                    .body(ForecastResponse.class);

            WeatherAPIResponse weatherAPIResponse = WeatherAPIResponse.builder().forecastResponseModel(r).build();
            if (request.getResults().getBoolean(ARG_ASTRONOMY)) {
                weatherAPIResponse.setAstronomyResponse(getAstronomyData(query));
            }
            weatherAPIResponse.setStatus("OK: WeatherAPI service");

            return weatherAPIResponse;

        } catch (Exception e) {
            ErrorResponse errorResponse = getErrorFromException(e);
            WeatherAPIResponse weatherAPIResponse = WeatherAPIResponse.builder().build();
            weatherAPIResponse.setStatus("NOK: fetching weather failed");
            weatherAPIResponse.setErrorResponse(errorResponse);
            return weatherAPIResponse;
        }

    }

    private ErrorResponse getErrorFromException(Exception e) {
        ErrorResponse error;
        try {
            String msg = e.getMessage();
            int idx1 = msg.indexOf("\"");
            String json = msg.substring(idx1);
            if (json.startsWith("\"") && json.endsWith("\"")) {
                json = json.substring(1, json.length() - 1);
                ObjectMapper mapper = new ObjectMapper();
                error = mapper.readValue(json, ErrorResponse.class);
                int foo = 0;
            } else {
                error = new ErrorResponse(new ErrorCode(-1, "Unknown error 1"));
            }

        } catch (Exception ex) {
            error = new ErrorResponse(new ErrorCode(-1, "Unknown error 2"));
        }
        return error;
    }


}
