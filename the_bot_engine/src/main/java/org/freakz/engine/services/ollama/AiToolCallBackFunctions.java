package org.freakz.engine.services.ollama;

import com.martiansoftware.jsap.JSAPResult;
import org.freakz.engine.commands.util.WeatherUtils;
import org.freakz.engine.dto.weather.WeatherAPIResponse;
import org.freakz.engine.services.api.ServiceRequest;
import org.freakz.engine.services.weather.weatherapi.WeatherAPIService;
import org.freakz.engine.services.weather.weatherapi.model.ForecastResponse;
import org.slf4j.Logger;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.time.LocalDateTime;

import static org.freakz.engine.commands.util.StaticArgumentStrings.ARG_PLACE;

public class AiToolCallBackFunctions {

  private static final Logger log = org.slf4j.LoggerFactory.getLogger(AiToolCallBackFunctions.class);

  private final WeatherAPIService weatherAPIService;

  static class FakeJSAPResults extends JSAPResult {
    private final String result;
    private final boolean boolResult;

    public FakeJSAPResults(String result, boolean boolResult) {
      this.result = result;
      this.boolResult = boolResult;
    }

    @Override
    public String getString(String s) {
      return result;
    }

    @Override
    public boolean getBoolean(String s) {
      return boolResult;
    }
  }

  public AiToolCallBackFunctions(WeatherAPIService weatherAPIService) {
    this.weatherAPIService = weatherAPIService;
  }

  @Tool(description = "Return the current local date time in system running the bot")
  public String getCurrentLocalDateTime() {
    log.debug("tool called: getCurrentLocalDateTime()");
    String answer = LocalDateTime.now().toString();
    return answer;
  }

  @Tool(description = "Return the current live weather in city")
  public String getCurrentWeatherByCity(@ToolParam(description = "City name to query live weather. Returns matching list of city weather info") String city) {
    log.debug("tool called: getCurrentWeatherByCity({})", city);

    ServiceRequest request = ServiceRequest.builder().build();
    JSAPResult results = new FakeJSAPResults(city, true);
    request.setResults(results);

    WeatherAPIResponse response = weatherAPIService.handleWeatherCmdServiceRequest(request);
    if (response.getStatus().startsWith("OK")) {
      ForecastResponse r = response.getForecastResponseModel();
      String time = WeatherUtils.formatTime(r);
      String name = WeatherUtils.formatName(r, true);
      String feelsLike = WeatherUtils.formatFeelsLike(r, true);
      String astronomy = WeatherUtils.formatAstronomy(response.getAstronomyResponse(), true);
      return String.format(
          "Weather in city: '%s' - measure time: '%s', current temperature: '%s°C'%s%s", name, time, r.current().temp_c(), feelsLike, astronomy);

    } else {
      return String.format(
          "%s: %s", results.getString(ARG_PLACE), response.getErrorResponse().error().message());
    }
  }

}
