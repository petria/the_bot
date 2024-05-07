package org.freakz.engine.functions;

import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.function.Function;

/*
   Weather API
   https://www.weatherapi.com/api-explorer.aspx
 */
@Slf4j
public class WeatherService implements Function<WeatherService.Request, WeatherService.Response> {

//    private final RestClient restClient;
//    private final WeatherConfigProperties weatherProps;

    public WeatherService() {
//        this.weatherProps = props;
//        log.debug("Weather API URL: {}", weatherProps.apiUrl());
//        log.debug("Weather API Key: {}", weatherProps.apiKey());
//        this.restClient = RestClient.create(weatherProps.apiUrl());
        log.debug("Init!");
    }

    @Override
    public Response apply(Request weatherRequest) {
        log.info("Weather Request: {}", weatherRequest);
/*        Response response = restClient.get()
                .uri("/current.json?key={key}&q={q}", weatherProps.apiKey(), weatherRequest.city())
                .retrieve()
                .body(Response.class);*/
        Long lat = 10L;
        Long lon = 20L;
        Location location = new Location("Oulu", "Pohjois-Pohjanmaa", "Nevada", lat, lon);
        Current current = new Current("42.3°C", new Condition("Tuulee"), "5m/s", "90%");
        Response response = new Response(location, current, new Time(LocalDateTime.now().toString()));
        log.info("Weather API Response: {}", response);
        return response;
    }

    // mapping the response of the Weather API to records. I only mapped the information I was interested in.
    public record Request(String city) {
    }

    public record Response(Location location, Current current, Time time) {
    }

    public record Time(String timeNow) {
    }

    public record Location(String name, String region, String country, Long lat, Long lon) {
    }

    public record Current(String temp_f, Condition condition, String wind_mph, String humidity) {
    }

    public record Condition(String text) {
    }

}
