package org.freakz.engine.functions;

import org.springframework.ai.model.function.FunctionCallback;
import org.springframework.ai.model.function.FunctionCallbackWrapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FunctionConfiguration {

//    private final WeatherConfigProperties props;

    public FunctionConfiguration() {
//        this.props = props;
    }

/*    @Bean
    @Description("Get the current weather conditions for the given city.")
    public Function<WeatherService.Request,WeatherService.Response> currentWeatherFunction() {
        return new WeatherService();
    }*/

    @Bean
    public FunctionCallback weatherFunctionInfo() {

        return FunctionCallbackWrapper.builder(new WeatherService())
                .withName("WeatherInfo")
                .withDescription("Get the weather in location")
                .withResponseConverter((response) -> "" + response.current().temp_f())
                .build();
    }


}
