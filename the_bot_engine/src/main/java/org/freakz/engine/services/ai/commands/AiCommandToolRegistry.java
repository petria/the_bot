package org.freakz.engine.services.ai.commands;

import com.martiansoftware.jsap.JSAPResult;
import org.freakz.common.model.dto.DataNodeBase;
import org.freakz.common.model.dto.DataValueStatsModel;
import org.freakz.common.model.dto.DataValues;
import org.freakz.common.model.dto.DataValuesModel;
import org.freakz.common.model.engine.EngineRequest;
import org.freakz.common.model.users.User;
import org.freakz.common.model.users.UserChatIdentity;
import org.freakz.engine.commands.util.WeatherUtils;
import org.freakz.engine.data.service.DataValuesService;
import org.freakz.engine.data.service.UsersService;
import org.freakz.engine.dto.weather.WeatherAPIResponse;
import org.freakz.engine.dto.CmpWeatherResponse;
import org.freakz.engine.services.ai.claw.HokanNodeContextTokenService;
import org.freakz.engine.services.api.ServiceRequest;
import org.freakz.engine.services.logs.ChatLogAccessService;
import org.freakz.engine.services.weather.weatherapi.WeatherAPIService;
import org.freakz.engine.services.weather.weatherapi.model.ForecastResponse;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import static org.freakz.engine.commands.util.StaticArgumentStrings.ARG_PLACE;

@Service
public class AiCommandToolRegistry {

  private static final int DEFAULT_LIMIT = 20;
  private static final int MAX_LIMIT = 100;
  private static final int MAX_WEATHER_LOCATIONS = 8;

  private final ObjectProvider<WeatherAPIService> weatherAPIServiceProvider;
  private final UsersService usersService;
  private final DataValuesService dataValuesService;
  private final ChatLogAccessService chatLogAccessService;
  private final HokanNodeContextTokenService tokenService;
  private final JsonMapper jsonMapper;
  private final ImageAnalysisToolService imageAnalysisToolService;

  public AiCommandToolRegistry(
      ObjectProvider<WeatherAPIService> weatherAPIServiceProvider,
      UsersService usersService,
      DataValuesService dataValuesService,
      ChatLogAccessService chatLogAccessService,
      HokanNodeContextTokenService tokenService,
      JsonMapper jsonMapper,
      ImageAnalysisToolService imageAnalysisToolService) {
    this.weatherAPIServiceProvider = weatherAPIServiceProvider;
    this.usersService = usersService;
    this.dataValuesService = dataValuesService;
    this.chatLogAccessService = chatLogAccessService;
    this.tokenService = tokenService;
    this.jsonMapper = jsonMapper;
    this.imageAnalysisToolService = imageAnalysisToolService;
  }

  public List<String> availableToolNames() {
    return List.of(
        "weather.current",
        "weather.compare",
        "users.search",
        "users.get",
        "dataValues.query",
        "dataValues.aggregate",
        "dataValues.stats",
        "logs.read",
        "logs.search",
        "image.analyze");
  }

  public String execute(String toolName, JsonNode arguments) {
    return execute(toolName, arguments, null);
  }

  public String execute(String toolName, JsonNode arguments, EngineRequest request) {
    return switch (toolName == null ? "" : toolName.trim()) {
      case "weather.current" -> weatherCurrent(arguments);
      case "weather.compare" -> weatherCompare(arguments);
      case "users.search" -> usersSearch(arguments);
      case "users.get" -> usersGet(arguments);
      case "dataValues.query" -> dataValuesQuery(arguments);
      case "dataValues.aggregate" -> dataValuesAggregate(arguments);
      case "dataValues.stats" -> dataValuesStats(arguments);
      case "logs.read" -> logsRead(arguments, request);
      case "logs.search" -> logsSearch(arguments, request);
      case "image.analyze" -> imageAnalysisToolService.analyze(new ImageAnalysisToolService.JsonNodeArguments(arguments));
      default -> error("Unknown AI command tool: " + toolName);
    };
  }

  private String weatherCurrent(JsonNode args) {
    List<String> locations = weatherLocations(args);
    if (locations.size() > MAX_WEATHER_LOCATIONS) {
      return error("Too many weather locations, maximum is " + MAX_WEATHER_LOCATIONS);
    }
    boolean verbose = bool(args, "verbose");
    boolean feelsLike = bool(args, "feelsLike", "feels_like");
    boolean astronomy = bool(args, "astronomy");
    WeatherAPIService weatherAPIService = weatherAPIServiceProvider.getIfAvailable();
    if (weatherAPIService == null) {
      return error("Weather API service is not configured");
    }

    if (locations.size() == 1) {
      String location = locations.getFirst();
      WeatherAPIResponse response = queryWeather(weatherAPIService, location, astronomy, feelsLike, verbose);
      ObjectNode out = weatherResult(location, response, verbose, feelsLike, astronomy);
      out.put("tool", "weather.current");
      return out.toString();
    }

    ArrayNode locationValues = jsonMapper.createArrayNode();
    ArrayNode results = jsonMapper.createArrayNode();
    List<String> formattedLines = new ArrayList<>();
    for (String location : locations) {
      locationValues.add(location);
      WeatherAPIResponse response = queryWeather(weatherAPIService, location, astronomy, feelsLike, verbose);
      ObjectNode result = weatherResult(location, response, verbose, feelsLike, astronomy);
      results.add(result);
      formattedLines.add(result.path("formattedText").asString());
    }

    ObjectNode out = jsonMapper.createObjectNode();
    out.put("tool", "weather.current");
    out.set("locations", locationValues);
    out.set("results", results);
    out.put("formattedText", String.join("\n", formattedLines));
    return out.toString();
  }

  private String weatherCompare(JsonNode args) {
    List<String> locations = weatherLocations(args);
    if (locations.size() < 2) {
      return error("Need at least two weather locations to compare");
    }
    if (locations.size() > MAX_WEATHER_LOCATIONS) {
      return error("Too many weather locations, maximum is " + MAX_WEATHER_LOCATIONS);
    }
    WeatherAPIService weatherAPIService = weatherAPIServiceProvider.getIfAvailable();
    if (weatherAPIService == null) {
      return error("Weather API service is not configured");
    }

    CmpWeatherResponse response = queryCompareWeather(weatherAPIService, locations);
    ObjectNode out = jsonMapper.createObjectNode();
    out.put("tool", "weather.compare");
    out.set("locations", jsonMapper.valueToTree(locations));
    out.set("results", jsonMapper.valueToTree(response.getForecastResponses()));
    out.put("formattedText", formatCompareWeatherResponse(response, locations, sortAscending(args)));
    if (response.getStatus() != null) {
      out.put("status", response.getStatus());
    }
    return out.toString();
  }

  private WeatherAPIResponse queryWeather(
      WeatherAPIService weatherAPIService,
      String location,
      boolean astronomy,
      boolean feelsLike,
      boolean verbose) {
    ServiceRequest request = ServiceRequest.builder().build();
    request.setResults(new FakeJSAPResults(location, astronomy, feelsLike, verbose));
    return weatherAPIService.handleWeatherCmdServiceRequest(request);
  }

  private CmpWeatherResponse queryCompareWeather(
      WeatherAPIService weatherAPIService,
      List<String> locations) {
    ServiceRequest request = ServiceRequest.builder().build();
    request.setResults(new FakeJSAPResults(locations.toArray(String[]::new)));
    return weatherAPIService.handleCmpWeatherServiceRequest(request);
  }

  private ObjectNode weatherResult(
      String location,
      WeatherAPIResponse response,
      boolean verbose,
      boolean feelsLike,
      boolean astronomy) {
    ObjectNode out = jsonMapper.createObjectNode();
    out.put("location", location);
    out.put("status", response.getStatus());
    out.put("formattedText", WeatherUtils.formatWeatherResponse(response, location, verbose, feelsLike, astronomy));
    if (response.getStatus() != null && response.getStatus().startsWith("OK")) {
      ForecastResponse r = response.getForecastResponseModel();
      out.put("name", WeatherUtils.formatName(r, verbose));
      out.put("measureTime", WeatherUtils.formatTime(r));
      out.put("temperatureC", r.current().temp_c());
      out.put("feelsLike", WeatherUtils.formatFeelsLike(r, feelsLike));
      out.put("astronomy", WeatherUtils.formatAstronomy(response.getAstronomyResponse(), astronomy));
    } else if (response.getErrorResponse() != null && response.getErrorResponse().error() != null) {
      out.put("error", response.getErrorResponse().error().message());
    }
    return out;
  }

  private String formatCompareWeatherResponse(CmpWeatherResponse response, List<String> locations, boolean ascending) {
    if (response == null || response.getStatus() == null || !response.getStatus().startsWith("OK")) {
      return "Check spelling, no weather data found with: " + String.join(" ", locations);
    }
    List<ForecastResponse> forecastResponses = response.getForecastResponses();
    if (forecastResponses == null || forecastResponses.isEmpty()) {
      return "Check spelling, no weather data found with: " + String.join(" ", locations);
    }
    int longestCityName = findLongestCityNameLength(forecastResponses);
    forecastResponses = new ArrayList<>(forecastResponses);
    Comparator<ForecastResponse> comparator =
        Comparator.comparing(forecastResponse -> Double.parseDouble(forecastResponse.current().temp_c()));
    forecastResponses.sort(ascending ? comparator : comparator.reversed());
    double baselineTemp = Double.parseDouble(forecastResponses.getFirst().current().temp_c());
    StringBuilder sb = new StringBuilder();
    int index = 0;
    for (ForecastResponse forecastResponse : forecastResponses) {
      String formatted;
      if (index != 0) {
        double temp = Double.parseDouble(forecastResponse.current().temp_c());
        double diff = ascending ? temp - baselineTemp : baselineTemp - temp;
        String differenceStr = String.format("%.2f°C", diff);
        formatted = formatCompareWeatherLine(forecastResponse, differenceStr, longestCityName);
        sb.append("\n");
      } else {
        formatted = formatCompareWeatherLine(forecastResponse, "difference", longestCityName);
      }
      sb.append(formatted);
      index++;
    }
    return sb.toString();
  }

  private boolean sortAscending(JsonNode args) {
    String sort = text(args, "sort", "order", "sortOrder", "sort_order").toLowerCase(Locale.ROOT);
    return sort.equals("asc")
        || sort.equals("ascending")
        || sort.equals("coldest")
        || sort.equals("coldest-first")
        || sort.equals("kylmin")
        || sort.equals("kylmin ensin");
  }

  private String formatCompareWeatherLine(ForecastResponse response, String diff, int longestCityName) {
    String template = "%-" + longestCityName + "s %s %s%6.1f°C - %s";
    return String.format(
        template,
        response.location().name(),
        response.location().localtime().toLocalDate(),
        response.location().localtime().toLocalTime(),
        Double.parseDouble(response.current().temp_c()),
        diff);
  }

  private int findLongestCityNameLength(List<ForecastResponse> forecastResponses) {
    int longest = Integer.MIN_VALUE;
    for (ForecastResponse response : forecastResponses) {
      if (response.location().name().length() > longest) {
        longest = response.location().name().length();
      }
    }
    return longest;
  }

  private String usersSearch(JsonNode args) {
    String query = text(args, "query").toLowerCase(Locale.ROOT);
    int limit = limit(args);
    ArrayNode users = jsonMapper.createArrayNode();
    for (User user : allUsers().stream()
        .filter(user -> matchesUser(user, query))
        .sorted(Comparator.comparing(user -> nullSafe(user.getUsername()), String.CASE_INSENSITIVE_ORDER))
        .limit(limit)
        .toList()) {
      users.add(userSummary(user));
    }
    ObjectNode out = jsonMapper.createObjectNode();
    out.put("tool", "users.search");
    out.put("query", query);
    out.put("limit", limit);
    out.set("users", users);
    return out.toString();
  }

  private String usersGet(JsonNode args) {
    String username = text(args, "username");
    long id = args == null ? -1 : args.path("id").asLong(-1);
    for (User user : allUsers()) {
      if (id >= 0 && user.getId() != null && user.getId() == id) {
        return userSummaryResponse(user).toString();
      }
      if (!username.isBlank() && username.equalsIgnoreCase(nullSafe(user.getUsername()))) {
        return userSummaryResponse(user).toString();
      }
    }
    return error("User not found");
  }

  private String dataValuesQuery(JsonNode args) {
    String channel = requiredText(args, "channel");
    String network = requiredText(args, "network");
    String key = requiredText(args, "key", "keyName");
    String nick = text(args, "nick");
    int limit = limit(args);
    List<DataValues> values = dataValuesService.getRawDataValues(channel, network, key);
    ArrayNode items = jsonMapper.createArrayNode();
    values.stream()
        .filter(value -> nick.isBlank() || nick.equalsIgnoreCase(nullSafe(value.getNick())))
        .sorted(Comparator.comparing(DataValues::getKeyName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
        .limit(limit)
        .forEach(value -> items.add(dataValue(value)));
    ObjectNode out = jsonMapper.createObjectNode();
    out.put("tool", "dataValues.query");
    out.put("channel", channel);
    out.put("network", network);
    out.put("key", key);
    out.put("limit", limit);
    out.set("values", items);
    return out.toString();
  }

  private String dataValuesAggregate(JsonNode args) {
    String channel = requiredText(args, "channel");
    String network = requiredText(args, "network");
    String key = requiredText(args, "key", "keyName");
    int limit = limit(args);
    ArrayNode items = jsonMapper.createArrayNode();
    dataValuesService.getDataValuesAsc(channel, network, key).stream()
        .limit(limit)
        .forEach(value -> items.add(dataValueModel(value)));
    ObjectNode out = jsonMapper.createObjectNode();
    out.put("tool", "dataValues.aggregate");
    out.put("channel", channel);
    out.put("network", network);
    out.put("key", key);
    out.put("limit", limit);
    out.set("values", items);
    return out.toString();
  }

  private String dataValuesStats(JsonNode args) {
    String nick = requiredText(args, "nick");
    String channel = requiredText(args, "channel");
    String network = requiredText(args, "network");
    String key = requiredText(args, "key", "keyName");
    DataValueStatsModel stats = dataValuesService.getValueStats(nick, channel, network, key);
    ObjectNode out = jsonMapper.createObjectNode();
    out.put("tool", "dataValues.stats");
    out.put("nick", nick);
    out.put("channel", channel);
    out.put("network", network);
    out.put("key", key);
    out.put("output", stats == null ? "" : stats.getOutput());
    return out.toString();
  }

  private String logsRead(JsonNode args, EngineRequest request) {
    EngineRequest contextRequest = requireRequestContext(request);
    ChatLogAccessService.LogReadRequest readRequest =
        new ChatLogAccessService.LogReadRequest(
            tokenService.createToken(contextRequest, "ai-command-tool:logs.read"),
            textOrNull(args, "scope"),
            textOrNull(args, "protocol"),
            textOrNull(args, "network"),
            textOrNull(args, "chatType", "chat_type"),
            textOrNull(args, "chatTarget", "chat_target", "target", "channel"),
            textOrNull(args, "date"),
            optionalInt(args, "lines"),
            optionalBool(args, "includeAvailableFiles", "include_available_files")
        );
    ObjectNode out = jsonMapper.createObjectNode();
    out.put("tool", "logs.read");
    out.set("result", jsonMapper.valueToTree(chatLogAccessService.readLogs(readRequest)));
    return out.toString();
  }

  private String logsSearch(JsonNode args, EngineRequest request) {
    EngineRequest contextRequest = requireRequestContext(request);
    if (!hasLogSearchTerms(args)) {
      Integer lines = optionalInt(args, "lines");
      Boolean includeAvailableFiles = optionalBool(args, "includeAvailableFiles", "include_available_files");
      ChatLogAccessService.LogReadRequest readRequest =
          new ChatLogAccessService.LogReadRequest(
              tokenService.createToken(contextRequest, "ai-command-tool:logs.read"),
              textOrNull(args, "scope"),
              textOrNull(args, "protocol"),
              textOrNull(args, "network"),
              textOrNull(args, "chatType", "chat_type"),
              textOrNull(args, "chatTarget", "chat_target", "target", "channel"),
              textOrNull(args, "date"),
              lines == null ? 120 : lines,
              includeAvailableFiles != null && includeAvailableFiles
          );

      ObjectNode out = jsonMapper.createObjectNode();
      out.put("tool", "logs.read");
      out.put("fallbackFrom", "logs.search");
      out.put("fallbackReason", "missing search terms");
      out.set("result", jsonMapper.valueToTree(chatLogAccessService.readLogs(readRequest)));
      return out.toString();
    }

    ChatLogAccessService.LogSearchRequest searchRequest =
        new ChatLogAccessService.LogSearchRequest(
            tokenService.createToken(contextRequest, "ai-command-tool:logs.search"),
            textOrNull(args, "scope"),
            textOrNull(args, "protocol"),
            textOrNull(args, "network"),
            textOrNull(args, "chatType", "chat_type"),
            textOrNull(args, "chatTarget", "chat_target", "target", "channel"),
            textOrNull(args, "nick"),
            textOrNull(args, "query"),
            textList(firstNode(args, "anyTerms", "any_terms")),
            textList(firstNode(args, "allTerms", "all_terms")),
            textOrNull(args, "dateFrom", "date_from"),
            textOrNull(args, "dateTo", "date_to", "date"),
            optionalInt(args, "maxDays", "max_days"),
            optionalInt(args, "maxMatches", "max_matches", "limit"),
            optionalInt(args, "maxBytes", "max_bytes")
        );
    ObjectNode out = jsonMapper.createObjectNode();
    out.put("tool", "logs.search");
    out.set("result", jsonMapper.valueToTree(chatLogAccessService.searchLogs(searchRequest)));
    return out.toString();
  }

  private boolean hasLogSearchTerms(JsonNode args) {
    return textOrNull(args, "nick") != null
        || textOrNull(args, "query") != null
        || !safeTextList(firstNode(args, "anyTerms", "any_terms")).isEmpty()
        || !safeTextList(firstNode(args, "allTerms", "all_terms")).isEmpty();
  }

  private List<String> safeTextList(JsonNode node) {
    List<String> values = textList(node);
    return values == null ? List.of() : values;
  }

  private List<String> weatherLocations(JsonNode args) {
    Set<String> locations = new LinkedHashSet<>();
    collectTextValues(locations, args == null ? null : args.path("locations"));
    collectTextValues(locations, args == null ? null : args.path("cities"));
    if (locations.isEmpty()) {
      collectTextValues(locations, args == null ? null : args.path("location"));
    }
    if (locations.isEmpty()) {
      collectTextValues(locations, args == null ? null : args.path("city"));
    }
    if (locations.isEmpty()) {
      collectTextValues(locations, args == null ? null : args.path(ARG_PLACE));
    }
    if (locations.isEmpty()) {
      locations.add(requiredText(args, "location", "city", ARG_PLACE));
    }
    return List.copyOf(locations);
  }

  private void collectTextValues(Set<String> values, JsonNode node) {
    if (node == null || node.isMissingNode() || node.isNull()) {
      return;
    }
    if (node.isArray()) {
      for (JsonNode item : node) {
        addSplitText(values, item.asString(""));
      }
      return;
    }
    addSplitText(values, node.asString(""));
  }

  private void addSplitText(Set<String> values, String value) {
    if (value == null || value.isBlank()) {
      return;
    }
    for (String item : value.split("[,;\\n]")) {
      String cleaned = item.trim();
      if (!cleaned.isBlank()) {
        values.add(cleaned);
      }
    }
  }

  private List<User> allUsers() {
    return usersService.findAll().stream()
        .filter(User.class::isInstance)
        .map(User.class::cast)
        .toList();
  }

  private boolean matchesUser(User user, String query) {
    if (query == null || query.isBlank()) {
      return true;
    }
    if (contains(user.getUsername(), query) || contains(user.getName(), query) || contains(user.getEmail(), query)) {
      return true;
    }
    if (user.getChatIdentities() == null) {
      return false;
    }
    return user.getChatIdentities().stream().anyMatch(identity ->
        contains(identity.getUserId(), query)
            || contains(identity.getUsername(), query)
            || contains(identity.getDisplayName(), query));
  }

  private ObjectNode userSummaryResponse(User user) {
    ObjectNode out = jsonMapper.createObjectNode();
    out.put("tool", "users.get");
    out.set("user", userSummary(user));
    return out;
  }

  private ObjectNode userSummary(User user) {
    ObjectNode out = jsonMapper.createObjectNode();
    out.put("id", user.getId());
    out.put("username", user.getUsername());
    out.put("name", user.getName());
    out.put("email", user.getEmail());
    ArrayNode identities = jsonMapper.createArrayNode();
    if (user.getChatIdentities() != null) {
      for (UserChatIdentity identity : user.getChatIdentities()) {
        ObjectNode node = jsonMapper.createObjectNode();
        node.put("connectionType", identity.getConnectionType());
        node.put("network", identity.getNetwork());
        node.put("userId", identity.getUserId());
        node.put("username", identity.getUsername());
        node.put("displayName", identity.getDisplayName());
        identities.add(node);
      }
    }
    out.set("chatIdentities", identities);
    return out;
  }

  private ObjectNode dataValue(DataValues value) {
    ObjectNode node = jsonMapper.createObjectNode();
    node.put("nick", value.getNick());
    node.put("network", value.getNetwork());
    node.put("channel", value.getChannel());
    node.put("keyName", value.getKeyName());
    node.put("value", value.getValue());
    return node;
  }

  private ObjectNode dataValueModel(DataValuesModel value) {
    ObjectNode node = jsonMapper.createObjectNode();
    node.put("nick", value.getNick());
    node.put("network", value.getNetwork());
    node.put("channel", value.getChannel());
    node.put("keyName", value.getKeyName());
    node.put("value", value.getValue());
    node.put("numberValue", value.getNumberValue());
    return node;
  }

  private String error(String message) {
    ObjectNode out = jsonMapper.createObjectNode();
    out.put("error", message);
    return out.toString();
  }

  private String requiredText(JsonNode node, String... fields) {
    String value = text(node, fields);
    if (value.isBlank()) {
      throw new IllegalArgumentException("Missing required tool argument: " + String.join("/", fields));
    }
    return value;
  }

  private EngineRequest requireRequestContext(EngineRequest request) {
    if (request == null) {
      throw new IllegalArgumentException("Tool requires current chat request context");
    }
    return request;
  }

  private String textOrNull(JsonNode node, String... fields) {
    String value = text(node, fields);
    return value.isBlank() ? null : value;
  }

  private String text(JsonNode node, String... fields) {
    if (node == null) {
      return "";
    }
    for (String field : fields) {
      String value = node.path(field).asString("");
      if (value != null && !value.isBlank()) {
        return value.trim();
      }
    }
    return "";
  }

  private Integer optionalInt(JsonNode node, String... fields) {
    JsonNode value = firstNode(node, fields);
    if (value == null || value.isMissingNode() || value.isNull()) {
      return null;
    }
    return value.asInt();
  }

  private Boolean optionalBool(JsonNode node, String... fields) {
    JsonNode value = firstNode(node, fields);
    if (value == null || value.isMissingNode() || value.isNull()) {
      return null;
    }
    return value.asBoolean();
  }

  private List<String> textList(JsonNode node) {
    if (node == null || node.isMissingNode() || node.isNull()) {
      return null;
    }
    List<String> values = new ArrayList<>();
    if (node.isArray()) {
      for (JsonNode item : node) {
        String value = item.asString("").trim();
        if (!value.isBlank()) {
          values.add(value);
        }
      }
      return values;
    }
    String value = node.asString("").trim();
    return value.isBlank() ? List.of() : List.of(value);
  }

  private JsonNode firstNode(JsonNode node, String... fields) {
    if (node == null) {
      return null;
    }
    for (String field : fields) {
      JsonNode value = node.path(field);
      if (!value.isMissingNode() && !value.isNull()) {
        return value;
      }
    }
    return null;
  }

  private int limit(JsonNode args) {
    int value = args == null ? DEFAULT_LIMIT : args.path("limit").asInt(DEFAULT_LIMIT);
    return Math.max(1, Math.min(value, MAX_LIMIT));
  }

  private boolean bool(JsonNode node, String... fields) {
    if (node == null) {
      return false;
    }
    for (String field : fields) {
      JsonNode value = node.path(field);
      if (!value.isMissingNode() && !value.isNull()) {
        return value.asBoolean(false);
      }
    }
    return false;
  }

  private boolean contains(String value, String query) {
    return value != null && value.toLowerCase(Locale.ROOT).contains(query);
  }

  private String nullSafe(String value) {
    return value == null ? "" : value;
  }

  private static class FakeJSAPResults extends JSAPResult {
    private final String result;
    private final String[] results;
    private final boolean astronomy;
    private final boolean feelsLike;
    private final boolean verbose;

    FakeJSAPResults(String result, boolean astronomy, boolean feelsLike, boolean verbose) {
      this.result = result;
      this.results = null;
      this.astronomy = astronomy;
      this.feelsLike = feelsLike;
      this.verbose = verbose;
    }

    FakeJSAPResults(String[] results) {
      this.result = null;
      this.results = results == null ? new String[0] : results.clone();
      this.astronomy = false;
      this.feelsLike = false;
      this.verbose = false;
    }

    @Override
    public String getString(String s) {
      return result;
    }

    @Override
    public String[] getStringArray(String s) {
      return results == null ? new String[0] : results.clone();
    }

    @Override
    public boolean getBoolean(String s) {
      return switch (s) {
        case "astronomy" -> astronomy;
        case "feelsLike" -> feelsLike;
        case "verbose" -> verbose;
        default -> false;
      };
    }
  }
}
