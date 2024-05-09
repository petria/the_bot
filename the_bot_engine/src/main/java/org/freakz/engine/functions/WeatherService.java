package org.freakz.engine.functions;

import com.martiansoftware.jsap.JSAPResult;
import lombok.extern.slf4j.Slf4j;
import org.freakz.engine.dto.ForecaResponse;
import org.freakz.engine.services.api.ServiceRequest;
import org.freakz.engine.services.foreca.ForecaWeatherService;

import java.util.function.Function;

/*
   Weather API
   https://www.weatherapi.com/api-explorer.aspx
 */
@Slf4j
public class WeatherService implements Function<WeatherService.Request, WeatherService.Response> {

    private final ForecaWeatherService forecaWeatherService;

    public WeatherService(ForecaWeatherService forecaWeatherService) {
        log.debug("Init!");
        this.forecaWeatherService = forecaWeatherService;
    }

    static class FakeJSAPResults extends JSAPResult {
        private final String result;

        public FakeJSAPResults(String result) {
            this.result = result;
        }

        @Override
        public String getString(String s) {
            return result;
        }
    }

    @Override
    public Response apply(Request weatherRequest) {
        log.info("Weather Request: {}", weatherRequest);

        ServiceRequest request = ServiceRequest.builder().build();
        JSAPResult results = new FakeJSAPResults(weatherRequest.city());
        request.setResults(results);

        ForecaResponse forecaResponse = forecaWeatherService.handleForecaCmdServiceRequest(request);
        Response response = new Response(forecaResponse);
        log.info("Weather API Response: {}", response);
        return response;
    }

    // mapping the response of the Weather API to records. I only mapped the information I was interested in.
    public record Request(String city) {
    }

    public record Response(ForecaResponse response) {
    }
//    public record Response(Location location, Current current, Time time) {}

    public record Time(String timeNow) {
    }

    public record Location(String name, String region, String country, Long lat, Long lon) {
    }

    public record Current(String temp_f, Condition condition, String wind_mph, String humidity) {
    }

    public record Condition(String text) {
    }

}
