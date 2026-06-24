package org.freakz.engine.services.ai.commands;

import org.freakz.common.model.engine.EngineRequest;
import org.freakz.engine.data.service.DataValuesService;
import org.freakz.engine.data.service.UsersService;
import org.freakz.engine.dto.CmpWeatherResponse;
import org.freakz.engine.dto.weather.WeatherAPIResponse;
import org.freakz.engine.services.ai.claw.HokanNodeContextTokenService;
import org.freakz.engine.services.api.ServiceRequest;
import org.freakz.engine.services.logs.ChatLogAccessService;
import org.freakz.engine.services.weather.weatherapi.WeatherAPIService;
import org.freakz.engine.services.weather.weatherapi.model.Condition;
import org.freakz.engine.services.weather.weatherapi.model.Current;
import org.freakz.engine.services.weather.weatherapi.model.ForecastResponse;
import org.freakz.engine.services.weather.weatherapi.model.Location;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.freakz.engine.commands.util.StaticArgumentStrings.ARG_PLACE;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AiCommandToolRegistryTest {

  private final JsonMapper jsonMapper = new JsonMapper();
  private static final LocalDateTime FIXED_TIME = LocalDateTime.of(2026, 6, 3, 21, 40);

  @Test
  void weatherCurrentFormatsMultipleLocations() throws Exception {
    WeatherAPIService weatherAPIService = mock(WeatherAPIService.class);
    when(weatherAPIService.handleWeatherCmdServiceRequest(any(ServiceRequest.class)))
        .thenAnswer(invocation -> {
          ServiceRequest request = invocation.getArgument(0);
          String place = request.getResults().getString(ARG_PLACE);
          return weatherResponse(forecast(place, place.equals("Turku") ? "12.4" : "8.1"));
        });

    AiCommandToolRegistry registry = newRegistry(weatherAPIService);
    JsonNode args = jsonMapper.readTree("""
        {"locations":["Turku","Oulu"]}
        """);

    JsonNode result = jsonMapper.readTree(registry.execute("weather.current", args));

    assertThat(result.path("formattedText").asString())
        .isEqualTo("""
            Turku: 21:40, 12.4°C
            Oulu: 21:40, 8.1°C""");
    assertThat(result.path("locations")).hasSize(2);
    assertThat(result.path("results")).hasSize(2);
    assertThat(result.path("results").get(0).path("formattedText").asString())
        .isEqualTo("Turku: 21:40, 12.4°C");
  }

  @Test
  void weatherCompareFormatsMultipleLocationsLikeCmpWeatherCmd() throws Exception {
    WeatherAPIService weatherAPIService = mock(WeatherAPIService.class);
    when(weatherAPIService.handleCmpWeatherServiceRequest(any(ServiceRequest.class)))
        .thenAnswer(invocation -> {
          ServiceRequest request = invocation.getArgument(0);
          String[] places = request.getResults().getStringArray(ARG_PLACE);
          assertThat(places).containsExactly("Turku", "Oulu");
          return compareResponse(List.of(
              forecast("Turku", "12.4"),
              forecast("Oulu", "8.1")));
        });

    AiCommandToolRegistry registry = newRegistry(weatherAPIService);
    JsonNode args = jsonMapper.readTree("""
        {"locations":["Turku","Oulu"]}
        """);

    JsonNode result = jsonMapper.readTree(registry.execute("weather.compare", args));

    assertThat(result.path("tool").asString()).isEqualTo("weather.compare");
    assertThat(result.path("formattedText").asString())
        .isEqualTo("""
            Turku 2026-06-03 21:40  12.4°C - difference
            Oulu  2026-06-03 21:40   8.1°C - 4.30°C""");
    assertThat(result.path("locations")).hasSize(2);
    assertThat(result.path("results")).hasSize(2);
  }

  @Test
  void logsReadUsesCurrentRequestContextToken() throws Exception {
    ObjectProvider<WeatherAPIService> weatherProvider = mock(ObjectProvider.class);
    ChatLogAccessService chatLogAccessService = mock(ChatLogAccessService.class);
    HokanNodeContextTokenService tokenService = mock(HokanNodeContextTokenService.class);
    EngineRequest request = EngineRequest.builder()
        .chatProtocol("irc")
        .network("IRCNet")
        .chatType("channel")
        .replyTo("#HokanDEV")
        .build();
    when(tokenService.createToken(eq(request), eq("ai-command-tool:logs.read"))).thenReturn("ctx-token");
    when(chatLogAccessService.readLogs(any(ChatLogAccessService.LogReadRequest.class)))
        .thenReturn(new ChatLogAccessService.LogReadResponse(
            "current-chat", "irc", "ircnet", "channel", "hokandev",
            "2026-06-09", true, 2, List.of(), "one\ntwo"));

    AiCommandToolRegistry registry = new AiCommandToolRegistry(
        weatherProvider,
        mock(UsersService.class),
        mock(DataValuesService.class),
        chatLogAccessService,
        tokenService,
        jsonMapper);
    JsonNode args = jsonMapper.readTree("""
        {"lines":2,"date":"2026-06-09"}
        """);

    JsonNode result = jsonMapper.readTree(registry.execute("logs.read", args, request));

    var captor = forClass(ChatLogAccessService.LogReadRequest.class);
    verify(chatLogAccessService).readLogs(captor.capture());
    assertThat(captor.getValue().hokanContextToken()).isEqualTo("ctx-token");
    assertThat(captor.getValue().lines()).isEqualTo(2);
    assertThat(captor.getValue().date()).isEqualTo("2026-06-09");
    assertThat(result.path("tool").asString()).isEqualTo("logs.read");
    assertThat(result.path("result").path("content").asString()).isEqualTo("one\ntwo");
  }

  @Test
  void logsSearchWithoutTermsFallsBackToCurrentLogRead() throws Exception {
    ObjectProvider<WeatherAPIService> weatherProvider = mock(ObjectProvider.class);
    ChatLogAccessService chatLogAccessService = mock(ChatLogAccessService.class);
    HokanNodeContextTokenService tokenService = mock(HokanNodeContextTokenService.class);
    EngineRequest request = EngineRequest.builder()
        .chatProtocol("irc")
        .network("IRCNet")
        .chatType("channel")
        .replyTo("#lowlife")
        .build();
    when(tokenService.createToken(eq(request), eq("ai-command-tool:logs.read"))).thenReturn("ctx-token");
    when(chatLogAccessService.readLogs(any(ChatLogAccessService.LogReadRequest.class)))
        .thenReturn(new ChatLogAccessService.LogReadResponse(
            "current-chat", "irc", "ircnet", "channel", "lowlife",
            "2026-06-09", true, 2, List.of(), "one\ntwo"));

    AiCommandToolRegistry registry = new AiCommandToolRegistry(
        weatherProvider,
        mock(UsersService.class),
        mock(DataValuesService.class),
        chatLogAccessService,
        tokenService,
        jsonMapper);
    JsonNode args = jsonMapper.readTree("""
        {"scope":"current-chat","query":"","maxMatches":20}
        """);

    JsonNode result = jsonMapper.readTree(registry.execute("logs.search", args, request));

    var captor = forClass(ChatLogAccessService.LogReadRequest.class);
    verify(chatLogAccessService).readLogs(captor.capture());
    assertThat(captor.getValue().hokanContextToken()).isEqualTo("ctx-token");
    assertThat(captor.getValue().scope()).isEqualTo("current-chat");
    assertThat(captor.getValue().lines()).isEqualTo(120);
    assertThat(result.path("tool").asString()).isEqualTo("logs.read");
    assertThat(result.path("fallbackFrom").asString()).isEqualTo("logs.search");
    assertThat(result.path("fallbackReason").asString()).isEqualTo("missing search terms");
    assertThat(result.path("result").path("content").asString()).isEqualTo("one\ntwo");
  }

  @SuppressWarnings("unchecked")
  private AiCommandToolRegistry newRegistry(WeatherAPIService weatherAPIService) {
    ObjectProvider<WeatherAPIService> weatherProvider = mock(ObjectProvider.class);
    when(weatherProvider.getIfAvailable()).thenReturn(weatherAPIService);
    return new AiCommandToolRegistry(
        weatherProvider,
        mock(UsersService.class),
        mock(DataValuesService.class),
        mock(ChatLogAccessService.class),
        mock(HokanNodeContextTokenService.class),
        jsonMapper);
  }

  private ForecastResponse forecast(String name, String tempC) {
    return new ForecastResponse(
        null,
        new Location(name, "", "", "60.45", "22.27", "Europe/Helsinki", 0L, FIXED_TIME),
        new Current(
            0L,
            "2026-06-03 21:40",
            tempC,
            "54.3",
            1,
            new Condition("Clear", "", 1000),
            0.0,
            0.0,
            0.0,
            "N",
            0.0,
            0.0,
            0.0,
            0.0,
            0,
            0,
            10.2,
            50.0,
            0.0,
            0.0,
            0.0,
            0.0,
            0.0,
            0.0,
            0.0,
            0.0,
            0.0,
            0.0,
            0.0,
            null),
        null);
  }

  private WeatherAPIResponse weatherResponse(ForecastResponse forecast) {
    WeatherAPIResponse response = WeatherAPIResponse.builder()
        .forecastResponseModel(forecast)
        .build();
    response.setStatus("OK: WeatherAPI service");
    return response;
  }

  private CmpWeatherResponse compareResponse(List<ForecastResponse> forecasts) {
    CmpWeatherResponse response = CmpWeatherResponse.builder()
        .forecastResponses(forecasts)
        .build();
    response.setStatus("OK: data size " + forecasts.size());
    return response;
  }
}
