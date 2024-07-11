package org.freakz.engine.services.weather.weatherapi;

import lombok.extern.slf4j.Slf4j;
import org.freakz.engine.config.ConfigService;
import org.freakz.engine.dto.weather.WeatherAPIResponse;
import org.freakz.engine.services.api.*;
import org.freakz.engine.services.weather.weatherapi.model.AstronomyResponse;
import org.freakz.engine.services.weather.weatherapi.model.ForecastResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import static org.freakz.engine.commands.util.StaticArgumentStrings.ARG_ASTRONOMY;
import static org.freakz.engine.commands.util.StaticArgumentStrings.ARG_PLACE;

@Slf4j
@SpringServiceMethodHandler
@Service
public class WeatherAPIService {

    private final ConfigService configService;
    private final RestClient restClient;
    private final String weatherApiUrl = "http://api.weatherapi.com/v1";
    private final String weatherApiKey = "10dd82570c684d7b8b1114704240707";
//    private final WeatherConfigProperties weatherProps;

    public WeatherAPIService(ConfigService configService) {
        this.configService = configService;

//        log.debug("Weather API URL: {}", apiUrl);
//        log.debug("Weather API Key: {}", apiKey);
        this.restClient = RestClient.create(weatherApiUrl);

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

        ForecastResponse r = restClient.get()
                .uri("/forecast.json?key={key}&aqi=yes&alerts=yes&q={q}", weatherApiKey, query)
                .retrieve()
                .body(ForecastResponse.class);

        WeatherAPIResponse weatherAPIResponse = WeatherAPIResponse.builder().forecastResponseModel(r).build();
        if (request.getResults().getBoolean(ARG_ASTRONOMY)) {
            weatherAPIResponse.setAstronomyResponse(getAstronomyData(query));
        }
        weatherAPIResponse.setStatus("OK: WeatherAPI service");

        return weatherAPIResponse;
    }


}
