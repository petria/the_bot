package org.freakz.engine.functions;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Description;

import java.util.function.Function;

@Configuration
@Slf4j
public class FunctionConfiguration {

//    private final WeatherConfigProperties props;

    public FunctionConfiguration() {

    }

    @Bean
    @Description("Get the current weather conditions for the given city.")
    public Function<WeatherService.Request,WeatherService.Response> currentWeatherFunction() {
        return new WeatherService();
    }

    @Bean
    @Description("Get the current local time and date.")
    public Function<TimeService.Request, TimeService.Response> currentTimeFunction() {
        return new TimeService();
    }

    @Bean
    @Description("Get the current physical location of the AI bot.")
    public Function<MyLocationService.Request, MyLocationService.Response> myCurrentLocationFunction() {
        return new MyLocationService();
    }

}
