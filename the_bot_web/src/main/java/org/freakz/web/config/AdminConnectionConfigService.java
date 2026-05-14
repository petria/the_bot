package org.freakz.web.config;

import org.freakz.common.config.BotRuntimeBootstrapConfig;
import org.freakz.common.config.BotRuntimeBootstrapLoader;
import org.freakz.common.config.ConfigConstants;
import org.freakz.common.config.TheBotProperties;
import org.freakz.common.spring.rest.RestEngineClient;
import org.freakz.common.spring.rest.RestServerConfigClient;
import org.springframework.http.ResponseEntity;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class AdminConnectionConfigService {

  private static final BotRuntimeBootstrapLoader BOOTSTRAP_LOADER = new BotRuntimeBootstrapLoader();

  private final Environment environment;
  private final TheBotProperties botProperties;
  private final JsonMapper jsonMapper;
  private final RestServerConfigClient serverConfigClient;
  private final RestEngineClient engineClient;

  public AdminConnectionConfigService(
      Environment environment,
      TheBotProperties botProperties,
      JsonMapper jsonMapper,
      RestServerConfigClient serverConfigClient,
      RestEngineClient engineClient) {
    this.environment = environment;
    this.botProperties = botProperties;
    this.jsonMapper = jsonMapper;
    this.serverConfigClient = serverConfigClient;
    this.engineClient = engineClient;
  }

  public AdminConnectionConfigResponse readConfig() {
    try {
      ConfigFile configFile = resolveConfigFile();
      ObjectNode root = readRoot(configFile.path());
      return responseFrom(configFile, root);
    } catch (IOException | RuntimeException e) {
      throw new IllegalStateException("Could not read connection config", e);
    }
  }

  public synchronized AdminConnectionConfigResponse saveConfig(AdminConnectionConfigPayload payload) {
    try {
      AdminConnectionConfigPayload normalized = normalizeAndValidate(payload);
      ConfigFile configFile = resolveConfigFile();
      ObjectNode root = readRoot(configFile.path());

      root.set("ircServerConfigs", ircConfigsToNode(normalized.ircServerConfigs()));
      root.set("discordConfig", discordConfigToNode(asObject(root.get("discordConfig")), normalized.discordConfig()));
      root.set("telegramConfig", telegramConfigToNode(asObject(root.get("telegramConfig")), normalized.telegramConfig()));
      root.set("whatsappConfig", whatsappConfigToNode(asObject(root.get("whatsappConfig")), normalized.whatsappConfig()));

      writeWithBackup(configFile.path(), root);
      return responseFrom(configFile, readRoot(configFile.path()));
    } catch (IOException | RuntimeException e) {
      throw new IllegalStateException("Could not save connection config", e);
    }
  }

  public synchronized AdminConnectionConfigApplyResponse saveAndApplyConfig(AdminConnectionConfigPayload payload) {
    AdminConnectionConfigResponse saved = saveConfig(payload);
    ApplyTargetResult botIoResult = applyBotIo();
    ApplyTargetResult botEngineResult = reloadBotEngine();
    String status = botIoResult.ok() && botEngineResult.ok() ? "OK" : "PARTIAL";
    return new AdminConnectionConfigApplyResponse(
        status,
        saved,
        List.of(botIoResult, botEngineResult));
  }

  private ApplyTargetResult applyBotIo() {
    ResponseEntity<String> response = serverConfigClient.applyConfig();
    return new ApplyTargetResult(
        "bot-io",
        response.getStatusCode().is2xxSuccessful() ? "OK" : "NOK",
        response.getBody());
  }

  private ApplyTargetResult reloadBotEngine() {
    ResponseEntity<String> response = engineClient.reloadConfig();
    return new ApplyTargetResult(
        "bot-engine",
        response.getStatusCode().is2xxSuccessful() ? "OK" : "NOK",
        response.getBody());
  }

  private ConfigFile resolveConfigFile() throws IOException {
    BotRuntimeBootstrapConfig bootstrapConfig = BOOTSTRAP_LOADER.load(
        environment,
        botProperties.getConfigFile(),
        botProperties.getRuntimeDir(),
        botProperties.getDataDir(),
        botProperties.getLogDir());

    String configFileName = bootstrapConfig.runtimeConfigFile();
    if (configFileName == null || configFileName.isBlank()) {
      String profile = bootstrapConfig.profile();
      configFileName = (profile == null || profile.isBlank())
          ? bootstrapConfig.runtimeDir() + ConfigConstants.RUNTIME_CONFIG_FILE_NAME
          : bootstrapConfig.runtimeDir() + profile + "." + ConfigConstants.RUNTIME_CONFIG_FILE_NAME;
    }

    return new ConfigFile(Path.of(configFileName).toAbsolutePath().normalize(), bootstrapConfig.profile());
  }

  private ObjectNode readRoot(Path path) throws IOException {
    JsonNode node = jsonMapper.readTree(path);
    if (node == null || !node.isObject()) {
      throw new IllegalStateException("Runtime config root must be a JSON object: " + path);
    }
    return (ObjectNode) node;
  }

  private AdminConnectionConfigResponse responseFrom(ConfigFile configFile, ObjectNode root) throws IOException {
    return new AdminConnectionConfigResponse(
        configFile.profile(),
        configFile.path().toString(),
        Files.getLastModifiedTime(configFile.path()).toInstant(),
        new AdminConnectionConfigPayload(
            ircConfigsFrom(root.get("ircServerConfigs")),
            discordConfigFrom(root.get("discordConfig")),
            telegramConfigFrom(root.get("telegramConfig")),
            whatsappConfigFrom(root.get("whatsappConfig"))));
  }

  private List<IrcServerConfigDto> ircConfigsFrom(JsonNode node) {
    if (node == null || !node.isArray()) {
      return List.of();
    }
    List<IrcServerConfigDto> configs = new ArrayList<>();
    for (JsonNode item : node.values()) {
      JsonNode network = item.path("ircNetwork");
      JsonNode server = network.path("ircServer");
      configs.add(new IrcServerConfigDto(
          text(item, "name"),
          item.path("connectStartup").asBoolean(false),
          text(network, "name"),
          text(server, "host"),
          server.path("port").asInt(0),
          channelsFrom(item.get("channelList"))));
    }
    return configs;
  }

  private DiscordConfigDto discordConfigFrom(JsonNode node) {
    return new DiscordConfigDto(
        node != null && node.path("connectStartup").asBoolean(false),
        node == null || node.get("theBotUserId") == null || node.get("theBotUserId").isNull()
            ? null
            : node.get("theBotUserId").asLong(),
        channelsFrom(node == null ? null : node.get("channelList")));
  }

  private TelegramConfigDto telegramConfigFrom(JsonNode node) {
    return new TelegramConfigDto(
        text(node, "telegramName"),
        node != null && node.path("connectStartup").asBoolean(false),
        channelsFrom(node == null ? null : node.get("channelList")));
  }

  private WhatsAppConfigDto whatsappConfigFrom(JsonNode node) {
    return new WhatsAppConfigDto(
        text(node, "network"),
        text(node, "sendBaseUrl"),
        node != null && node.path("connectStartup").asBoolean(false),
        channelsFrom(node == null ? null : node.get("channelList")));
  }

  private List<ChannelDto> channelsFrom(JsonNode node) {
    if (node == null || !node.isArray()) {
      return List.of();
    }
    List<ChannelDto> channels = new ArrayList<>();
    for (JsonNode item : node.values()) {
      channels.add(new ChannelDto(
          text(item, "id"),
          text(item, "description"),
          text(item, "name"),
          text(item, "type"),
          text(item, "echoToAlias"),
          aliasesFrom(item.get("echoToAliases")),
          item.path("joinOnStart").asBoolean(false)));
    }
    return channels;
  }

  private List<String> aliasesFrom(JsonNode node) {
    if (node == null || !node.isArray()) {
      return List.of();
    }
    List<String> aliases = new ArrayList<>();
    for (JsonNode item : node.values()) {
      String value = clean(item.asText(null));
      if (value != null) {
        aliases.add(value);
      }
    }
    return aliases;
  }

  private AdminConnectionConfigPayload normalizeAndValidate(AdminConnectionConfigPayload payload) {
    if (payload == null) {
      throw new IllegalArgumentException("Config payload is required");
    }

    List<IrcServerConfigDto> ircConfigs = payload.ircServerConfigs() == null ? List.of() : payload.ircServerConfigs().stream()
        .map(this::normalizeIrc)
        .toList();
    DiscordConfigDto discordConfig = normalizeDiscord(payload.discordConfig());
    TelegramConfigDto telegramConfig = normalizeTelegram(payload.telegramConfig());
    WhatsAppConfigDto whatsappConfig = normalizeWhatsApp(payload.whatsappConfig());

    validateUniqueChannelAliases(ircConfigs, discordConfig, telegramConfig, whatsappConfig);
    return new AdminConnectionConfigPayload(ircConfigs, discordConfig, telegramConfig, whatsappConfig);
  }

  private IrcServerConfigDto normalizeIrc(IrcServerConfigDto config) {
    if (config == null) {
      throw new IllegalArgumentException("IRC config cannot be null");
    }
    String name = required(config.name(), "IRC config name is required");
    String networkName = required(config.networkName(), "IRC network name is required");
    String host = required(config.host(), "IRC server host is required");
    int port = config.port();
    if (port < 1 || port > 65535) {
      throw new IllegalArgumentException("IRC server port must be between 1 and 65535");
    }
    return new IrcServerConfigDto(
        name,
        config.connectStartup(),
        networkName,
        host,
        port,
        normalizeChannels(config.channelList()));
  }

  private DiscordConfigDto normalizeDiscord(DiscordConfigDto config) {
    if (config == null) {
      return new DiscordConfigDto(false, null, List.of());
    }
    return new DiscordConfigDto(config.connectStartup(), config.theBotUserId(), normalizeChannels(config.channelList()));
  }

  private TelegramConfigDto normalizeTelegram(TelegramConfigDto config) {
    if (config == null) {
      return new TelegramConfigDto(null, false, List.of());
    }
    return new TelegramConfigDto(clean(config.telegramName()), config.connectStartup(), normalizeChannels(config.channelList()));
  }

  private WhatsAppConfigDto normalizeWhatsApp(WhatsAppConfigDto config) {
    if (config == null) {
      return new WhatsAppConfigDto(null, null, false, List.of());
    }
    return new WhatsAppConfigDto(
        clean(config.network()),
        clean(config.sendBaseUrl()),
        config.connectStartup(),
        normalizeChannels(config.channelList()));
  }

  private List<ChannelDto> normalizeChannels(List<ChannelDto> channels) {
    if (channels == null) {
      return List.of();
    }
    return channels.stream()
        .filter(channel -> channel != null && !isBlankChannel(channel))
        .map(channel -> new ChannelDto(
            clean(channel.id()),
            clean(channel.description()),
            clean(channel.name()),
            clean(channel.type()),
            clean(channel.echoToAlias()),
            normalizeAliases(channel.echoToAliases()),
            channel.joinOnStart()))
        .toList();
  }

  private List<String> normalizeAliases(List<String> aliases) {
    if (aliases == null) {
      return List.of();
    }
    return aliases.stream()
        .map(this::clean)
        .filter(value -> value != null && !value.isBlank())
        .distinct()
        .toList();
  }

  private boolean isBlankChannel(ChannelDto channel) {
    return clean(channel.id()) == null
        && clean(channel.name()) == null
        && clean(channel.echoToAlias()) == null
        && clean(channel.description()) == null
        && clean(channel.type()) == null
        && normalizeAliases(channel.echoToAliases()).isEmpty();
  }

  private void validateUniqueChannelAliases(
      List<IrcServerConfigDto> ircConfigs,
      DiscordConfigDto discordConfig,
      TelegramConfigDto telegramConfig,
      WhatsAppConfigDto whatsappConfig) {
    Set<String> seen = new HashSet<>();
    List<ChannelDto> channels = new ArrayList<>();
    ircConfigs.forEach(config -> channels.addAll(config.channelList()));
    channels.addAll(discordConfig.channelList());
    channels.addAll(telegramConfig.channelList());
    channels.addAll(whatsappConfig.channelList());

    for (ChannelDto channel : channels) {
      String alias = channel.echoToAlias();
      if (alias == null || alias.isBlank()) {
        continue;
      }
      String key = alias.toLowerCase(Locale.ROOT);
      if (!seen.add(key)) {
        throw new IllegalArgumentException("Duplicate channel alias: " + alias);
      }
    }
  }

  private ArrayNode ircConfigsToNode(List<IrcServerConfigDto> configs) {
    ArrayNode array = jsonMapper.createArrayNode();
    for (IrcServerConfigDto config : configs) {
      ObjectNode item = jsonMapper.createObjectNode();
      item.put("name", config.name());
      ObjectNode network = jsonMapper.createObjectNode();
      network.put("name", config.networkName());
      ObjectNode server = jsonMapper.createObjectNode();
      server.put("host", config.host());
      server.put("port", config.port());
      network.set("ircServer", server);
      item.set("ircNetwork", network);
      item.set("channelList", channelsToNode(config.channelList()));
      item.put("connectStartup", config.connectStartup());
      array.add(item);
    }
    return array;
  }

  private ObjectNode discordConfigToNode(ObjectNode existing, DiscordConfigDto config) {
    ObjectNode node = existing == null ? jsonMapper.createObjectNode() : existing.deepCopy();
    node.set("channelList", channelsToNode(config.channelList()));
    node.put("connectStartup", config.connectStartup());
    if (config.theBotUserId() == null) {
      node.putNull("theBotUserId");
    } else {
      node.put("theBotUserId", config.theBotUserId());
    }
    return node;
  }

  private ObjectNode telegramConfigToNode(ObjectNode existing, TelegramConfigDto config) {
    ObjectNode node = existing == null ? jsonMapper.createObjectNode() : existing.deepCopy();
    putNullable(node, "telegramName", config.telegramName());
    node.set("channelList", channelsToNode(config.channelList()));
    node.put("connectStartup", config.connectStartup());
    return node;
  }

  private ObjectNode whatsappConfigToNode(ObjectNode existing, WhatsAppConfigDto config) {
    ObjectNode node = existing == null ? jsonMapper.createObjectNode() : existing.deepCopy();
    putNullable(node, "network", config.network());
    putNullable(node, "sendBaseUrl", config.sendBaseUrl());
    node.set("channelList", channelsToNode(config.channelList()));
    node.put("connectStartup", config.connectStartup());
    return node;
  }

  private ArrayNode channelsToNode(List<ChannelDto> channels) {
    ArrayNode array = jsonMapper.createArrayNode();
    for (ChannelDto channel : channels) {
      ObjectNode item = jsonMapper.createObjectNode();
      putNullable(item, "id", channel.id());
      putNullable(item, "description", channel.description());
      putNullable(item, "name", channel.name());
      putNullable(item, "type", channel.type());
      putNullable(item, "echoToAlias", channel.echoToAlias());
      ArrayNode aliases = jsonMapper.createArrayNode();
      channel.echoToAliases().forEach(aliases::add);
      item.set("echoToAliases", aliases);
      item.put("joinOnStart", channel.joinOnStart());
      array.add(item);
    }
    return array;
  }

  private void writeWithBackup(Path configPath, ObjectNode root) throws IOException {
    Path parent = configPath.getParent();
    if (parent != null) {
      Files.createDirectories(parent);
    }
    if (Files.exists(configPath)) {
      String suffix = DateTimeFormatter.ISO_INSTANT.format(Instant.now()).replace(':', '-');
      Files.copy(configPath, configPath.resolveSibling(configPath.getFileName() + ".bak." + suffix),
          StandardCopyOption.COPY_ATTRIBUTES);
    }

    String json = jsonMapper.writerWithDefaultPrettyPrinter().writeValueAsString(root);
    Path tempFile = Files.createTempFile(parent, configPath.getFileName().toString(), ".tmp");
    Files.writeString(tempFile, json, Charset.defaultCharset());
    try {
      Files.move(tempFile, configPath, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
    } catch (AtomicMoveNotSupportedException e) {
      Files.move(tempFile, configPath, StandardCopyOption.REPLACE_EXISTING);
    }
  }

  private ObjectNode asObject(JsonNode node) {
    return node != null && node.isObject() ? (ObjectNode) node : null;
  }

  private void putNullable(ObjectNode node, String field, String value) {
    if (value == null) {
      node.putNull(field);
    } else {
      node.put(field, value);
    }
  }

  private String text(JsonNode node, String field) {
    if (node == null || node.get(field) == null || node.get(field).isNull()) {
      return null;
    }
    return node.get(field).asText(null);
  }

  private String clean(String value) {
    if (value == null) {
      return null;
    }
    String trimmed = value.trim();
    return trimmed.isEmpty() ? null : trimmed;
  }

  private String required(String value, String message) {
    String cleaned = clean(value);
    if (cleaned == null) {
      throw new IllegalArgumentException(message);
    }
    return cleaned;
  }

  private record ConfigFile(Path path, String profile) {
  }

  public record AdminConnectionConfigResponse(
      String profile,
      String configFile,
      Instant lastModifiedAt,
      AdminConnectionConfigPayload config) {
  }

  public record AdminConnectionConfigApplyResponse(
      String status,
      AdminConnectionConfigResponse savedConfig,
      List<ApplyTargetResult> targets) {
  }

  public record ApplyTargetResult(
      String target,
      String status,
      String message) {

    boolean ok() {
      return "OK".equals(status);
    }
  }

  public record AdminConnectionConfigPayload(
      List<IrcServerConfigDto> ircServerConfigs,
      DiscordConfigDto discordConfig,
      TelegramConfigDto telegramConfig,
      WhatsAppConfigDto whatsappConfig) {
  }

  public record IrcServerConfigDto(
      String name,
      boolean connectStartup,
      String networkName,
      String host,
      int port,
      List<ChannelDto> channelList) {
  }

  public record DiscordConfigDto(
      boolean connectStartup,
      Long theBotUserId,
      List<ChannelDto> channelList) {
  }

  public record TelegramConfigDto(
      String telegramName,
      boolean connectStartup,
      List<ChannelDto> channelList) {
  }

  public record WhatsAppConfigDto(
      String network,
      String sendBaseUrl,
      boolean connectStartup,
      List<ChannelDto> channelList) {
  }

  public record ChannelDto(
      String id,
      String description,
      String name,
      String type,
      String echoToAlias,
      List<String> echoToAliases,
      boolean joinOnStart) {
  }
}
