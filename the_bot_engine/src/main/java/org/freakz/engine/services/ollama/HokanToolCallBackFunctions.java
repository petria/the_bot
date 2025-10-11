package org.freakz.engine.services.ollama;

import com.martiansoftware.jsap.JSAPResult;
import org.freakz.engine.dto.weather.WeatherAPIResponse;
import org.freakz.engine.services.api.ServiceRequest;
import org.freakz.engine.services.weather.weatherapi.WeatherAPIService;
import org.slf4j.Logger;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.time.LocalDateTime;

public class HokanToolCallBackFunctions {

  private static final Logger log = org.slf4j.LoggerFactory.getLogger(HokanToolCallBackFunctions.class);

  private final WeatherAPIService weatherAPIService;

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

  public HokanToolCallBackFunctions(WeatherAPIService weatherAPIService) {
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
    JSAPResult results = new FakeJSAPResults(city);
    request.setResults(results);

    WeatherAPIResponse response = weatherAPIService.handleWeatherCmdServiceRequest(request);

    return response.toString();
  }

  /*
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

 */
}
