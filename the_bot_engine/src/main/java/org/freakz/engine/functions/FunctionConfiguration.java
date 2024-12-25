package org.freakz.engine.functions;

import lombok.extern.slf4j.Slf4j;
import org.freakz.engine.commands.BotEngine;
import org.freakz.engine.services.weather.foreca.ForecaWeatherService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Description;

import java.util.function.Function;

@Configuration
@Slf4j
public class FunctionConfiguration {

  //    private final WeatherConfigProperties props;
  private final ForecaWeatherService forecaWeatherService;

  private final BotEngine botEngine;

  public FunctionConfiguration(ForecaWeatherService forecaWeatherService, BotEngine botEngine) {
    this.forecaWeatherService = forecaWeatherService;
    this.botEngine = botEngine;
  }

  @Bean
  @Description("Invokes Bot Engine to handle incoming message.")
  public Function<BotEngineService.Request, BotEngineService.Response> handeEngineCommandFunction() {
    return new BotEngineService(this.botEngine);
  }

  @Bean
  @Description("Get the current weather conditions for the given city.")
  public Function<WeatherService.Request, WeatherService.Response> currentWeatherFunction() {
    return new WeatherService(this.forecaWeatherService);
  }

  @Bean
  @Description("Get the current local time and date.")
  public Function<TimeService.Request, TimeService.Response> currentTimeFunction() {
    return new TimeService();
  }

  @Bean
  @Description("Get the current physical location of the AI bot.")
  public Function<MyLocationService.Request, MyLocationService.Response>
      myCurrentLocationFunction() {
    return new MyLocationService();
  }

  @Bean
  @Description(
      "Get information about live IRC chat network connection where AI bot is connected to.")
  public Function<IrcInfoService.Request, IrcInfoService.Response> ircChatInfoFunction() {
    return new IrcInfoService();
  }
}
