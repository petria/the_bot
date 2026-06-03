package org.freakz.engine.services.ai.commands;

import com.martiansoftware.jsap.JSAPResult;
import org.freakz.common.model.dto.DataNodeBase;
import org.freakz.common.model.dto.DataValueStatsModel;
import org.freakz.common.model.dto.DataValues;
import org.freakz.common.model.dto.DataValuesModel;
import org.freakz.common.model.users.User;
import org.freakz.common.model.users.UserChatIdentity;
import org.freakz.engine.commands.util.WeatherUtils;
import org.freakz.engine.data.service.DataValuesService;
import org.freakz.engine.data.service.UsersService;
import org.freakz.engine.dto.weather.WeatherAPIResponse;
import org.freakz.engine.services.api.ServiceRequest;
import org.freakz.engine.services.weather.weatherapi.WeatherAPIService;
import org.freakz.engine.services.weather.weatherapi.model.ForecastResponse;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import static org.freakz.engine.commands.util.StaticArgumentStrings.ARG_PLACE;

@Service
public class AiCommandToolRegistry {

  private static final int DEFAULT_LIMIT = 20;
  private static final int MAX_LIMIT = 100;

  private final WeatherAPIService weatherAPIService;
  private final UsersService usersService;
  private final DataValuesService dataValuesService;
  private final JsonMapper jsonMapper;

  public AiCommandToolRegistry(
      WeatherAPIService weatherAPIService,
      UsersService usersService,
      DataValuesService dataValuesService,
      JsonMapper jsonMapper) {
    this.weatherAPIService = weatherAPIService;
    this.usersService = usersService;
    this.dataValuesService = dataValuesService;
    this.jsonMapper = jsonMapper;
  }

  public List<String> availableToolNames() {
    return List.of(
        "weather.current",
        "users.search",
        "users.get",
        "dataValues.query",
        "dataValues.aggregate",
        "dataValues.stats");
  }

  public String execute(String toolName, JsonNode arguments) {
    return switch (toolName == null ? "" : toolName.trim()) {
      case "weather.current" -> weatherCurrent(arguments);
      case "users.search" -> usersSearch(arguments);
      case "users.get" -> usersGet(arguments);
      case "dataValues.query" -> dataValuesQuery(arguments);
      case "dataValues.aggregate" -> dataValuesAggregate(arguments);
      case "dataValues.stats" -> dataValuesStats(arguments);
      default -> error("Unknown AI command tool: " + toolName);
    };
  }

  private String weatherCurrent(JsonNode args) {
    String location = requiredText(args, "location", "city", ARG_PLACE);
    ServiceRequest request = ServiceRequest.builder().build();
    request.setResults(new FakeJSAPResults(location, true));
    WeatherAPIResponse response = weatherAPIService.handleWeatherCmdServiceRequest(request);
    ObjectNode out = jsonMapper.createObjectNode();
    out.put("tool", "weather.current");
    out.put("location", location);
    out.put("status", response.getStatus());
    if (response.getStatus() != null && response.getStatus().startsWith("OK")) {
      ForecastResponse r = response.getForecastResponseModel();
      out.put("name", WeatherUtils.formatName(r, true));
      out.put("measureTime", WeatherUtils.formatTime(r));
      out.put("temperatureC", r.current().temp_c());
      out.put("feelsLike", WeatherUtils.formatFeelsLike(r, true));
      out.put("astronomy", WeatherUtils.formatAstronomy(response.getAstronomyResponse(), true));
    } else if (response.getErrorResponse() != null && response.getErrorResponse().error() != null) {
      out.put("error", response.getErrorResponse().error().message());
    }
    return out.toString();
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

  private int limit(JsonNode args) {
    int value = args == null ? DEFAULT_LIMIT : args.path("limit").asInt(DEFAULT_LIMIT);
    return Math.max(1, Math.min(value, MAX_LIMIT));
  }

  private boolean contains(String value, String query) {
    return value != null && value.toLowerCase(Locale.ROOT).contains(query);
  }

  private String nullSafe(String value) {
    return value == null ? "" : value;
  }

  private static class FakeJSAPResults extends JSAPResult {
    private final String result;
    private final boolean boolResult;

    FakeJSAPResults(String result, boolean boolResult) {
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
}
