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
import java.util.Objects;
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
      root.set("botConfig", botConfigToNode(asObject(root.get("botConfig")), normalized.botConfig()));
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
    AdminConnectionConfigPayload current = readConfig().config();
    AdminConnectionConfigResponse saved = saveConfig(payload);
    ApplyTargetResult botIoResult = canApplyBotIoChannelsOnly(current, saved.config())
        ? applyBotIoChannels()
        : applyBotIo();
    ApplyTargetResult botEngineResult = reloadBotEngine();
    String status = botIoResult.ok() && botEngineResult.ok() ? "OK" : "PARTIAL";
    return new AdminConnectionConfigApplyResponse(
        status,
        saved,
        List.of(botIoResult, botEngineResult));
  }

  public LiveChannelSettingsDto readChannelSettings(String connectionType, String network, String echoToAlias) {
    ChannelDto channel = findConfiguredChannel(readConfig().config(), connectionType, network, echoToAlias);
    if (channel == null) {
      throw new IllegalArgumentException("Configured channel was not found");
    }
    return settingsFrom(channel);
  }

  public synchronized LiveChannelSettingsApplyResponse saveAndApplyChannelSettings(
      String connectionType,
      String network,
      String echoToAlias,
      LiveChannelSettingsDto settings) {
    if (settings == null) {
      throw new IllegalArgumentException("Channel settings are required");
    }
    AdminConnectionConfigPayload current = readConfig().config();
    ChannelSettingsUpdate update = updateChannelSettings(current, connectionType, network, echoToAlias, settings);
    if (!update.updated()) {
      throw new IllegalArgumentException("Configured channel was not found");
    }
    saveConfig(update.payload());
    ApplyTargetResult botIoResult = applyBotIoChannels();
    ApplyTargetResult botEngineResult = reloadBotEngine();
    String status = botIoResult.ok() && botEngineResult.ok() ? "OK" : "PARTIAL";
    return new LiveChannelSettingsApplyResponse(
        status,
        settingsFrom(update.channel()),
        List.of(botIoResult, botEngineResult));
  }

  public synchronized AdminConnectionConfigResponse promoteChannel(PromoteChannelRequest request) {
    if (request == null || request.channel() == null) {
      throw new IllegalArgumentException("Promoted channel is required");
    }
    AdminConnectionConfigPayload current = readConfig().config();
    return saveConfig(addPromotedChannel(current, request));
  }

  public boolean hasConfiguredChannel(String connectionType, String network, String echoToAlias) {
    String alias = clean(echoToAlias);
    if (alias == null) {
      return false;
    }
    try {
      return hasConfiguredChannel(readConfig().config(), connectionType, network, alias);
    } catch (RuntimeException e) {
      return false;
    }
  }

  private ApplyTargetResult applyBotIo() {
    ResponseEntity<String> response = serverConfigClient.applyConfig();
    return new ApplyTargetResult(
        "bot-io",
        response.getStatusCode().is2xxSuccessful() ? "OK" : "NOK",
        response.getBody());
  }

  private ApplyTargetResult applyBotIoChannels() {
    ResponseEntity<String> response = serverConfigClient.applyChannelConfig();
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

  private boolean canApplyBotIoChannelsOnly(
      AdminConnectionConfigPayload before,
      AdminConnectionConfigPayload after) {
    AdminConnectionConfigPayload normalizedBefore = normalizeAndValidate(before);
    AdminConnectionConfigPayload normalizedAfter = normalizeAndValidate(after);
    return Objects.equals(structuralPayload(normalizedBefore), structuralPayload(normalizedAfter));
  }

  private AdminConnectionConfigPayload structuralPayload(AdminConnectionConfigPayload payload) {
    return new AdminConnectionConfigPayload(
        payload.botConfig(),
        payload.ircServerConfigs().stream()
            .map(config -> new IrcServerConfigDto(
                config.name(),
                config.connectStartup(),
                config.networkName(),
                config.host(),
                config.port(),
                structuralChannels(config.channelList())))
            .toList(),
        new DiscordConfigDto(
            payload.discordConfig().connectStartup(),
            payload.discordConfig().theBotUserId(),
            structuralChannels(payload.discordConfig().channelList())),
        new TelegramConfigDto(
            payload.telegramConfig().telegramName(),
            payload.telegramConfig().connectStartup(),
            structuralChannels(payload.telegramConfig().channelList())),
        new WhatsAppConfigDto(
            payload.whatsappConfig().network(),
            payload.whatsappConfig().sendBaseUrl(),
            payload.whatsappConfig().connectStartup(),
            structuralChannels(payload.whatsappConfig().channelList())));
  }

  private List<ChannelDto> structuralChannels(List<ChannelDto> channels) {
    return channels.stream()
        .map(channel -> new ChannelDto(
            channel.id(),
            channel.description(),
            channel.name(),
            channel.type(),
            channel.echoToAlias(),
            channel.echoToAliases(),
            false,
            false,
            false,
            false,
            false,
            false,
            false,
            List.of()))
        .toList();
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
            botConfigFrom(root.get("botConfig")),
            ircConfigsFrom(root.get("ircServerConfigs")),
            discordConfigFrom(root.get("discordConfig")),
            telegramConfigFrom(root.get("telegramConfig")),
            whatsappConfigFrom(root.get("whatsappConfig"))));
  }

  private BotConfigDto botConfigFrom(JsonNode node) {
    return new BotConfigDto(
        text(node, "botName"),
        text(node, "ircRealName"));
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
        text(node, "theBotUserId"),
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
          item.path("joinOnStart").asBoolean(false),
          item.path("publicAiEnabled").asBoolean(false),
          item.path("allowAnonymousAiCommands").asBoolean(false),
          item.path("resolveUrls").asBoolean(false),
          item.path("alertMessages").asBoolean(false),
          item.path("captureResolvedUrls").asBoolean(false),
          item.path("captureImages").asBoolean(false),
          aliasesFrom(item.get("captureImageToAliases"))));
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

    BotConfigDto botConfig = normalizeBotConfig(payload.botConfig());
    List<IrcServerConfigDto> ircConfigs = payload.ircServerConfigs() == null ? List.of() : payload.ircServerConfigs().stream()
        .map(this::normalizeIrc)
        .toList();
    DiscordConfigDto discordConfig = normalizeDiscord(payload.discordConfig());
    TelegramConfigDto telegramConfig = normalizeTelegram(payload.telegramConfig());
    WhatsAppConfigDto whatsappConfig = normalizeWhatsApp(payload.whatsappConfig());

    validateUniqueChannelAliases(ircConfigs, discordConfig, telegramConfig, whatsappConfig);
    return new AdminConnectionConfigPayload(botConfig, ircConfigs, discordConfig, telegramConfig, whatsappConfig);
  }

  private AdminConnectionConfigPayload addPromotedChannel(
      AdminConnectionConfigPayload payload,
      PromoteChannelRequest request) {
    AdminConnectionConfigPayload normalized = normalizeAndValidate(payload);
    ChannelDto channel = normalizedPromotedChannel(request);
    if (hasConfiguredChannel(normalized, request.connectionType(), request.network(), channel.echoToAlias())) {
      return normalized;
    }

    String type = clean(request.connectionType());
    if (isConnectionType(type, "IRC_CONNECTION")) {
      return promoteIrcChannel(normalized, request.network(), channel);
    }
    if (isConnectionType(type, "DISCORD_CONNECTION")) {
      DiscordConfigDto discord = normalized.discordConfig();
      return new AdminConnectionConfigPayload(
          normalized.botConfig(),
          normalized.ircServerConfigs(),
          new DiscordConfigDto(discord.connectStartup(), discord.theBotUserId(), appendChannel(discord.channelList(), channel)),
          normalized.telegramConfig(),
          normalized.whatsappConfig());
    }
    if (isConnectionType(type, "TELEGRAM_CONNECTION")) {
      TelegramConfigDto telegram = normalized.telegramConfig();
      return new AdminConnectionConfigPayload(
          normalized.botConfig(),
          normalized.ircServerConfigs(),
          normalized.discordConfig(),
          new TelegramConfigDto(telegram.telegramName(), telegram.connectStartup(), appendChannel(telegram.channelList(), channel)),
          normalized.whatsappConfig());
    }
    if (isConnectionType(type, "WHATSAPP_CONNECTION")) {
      WhatsAppConfigDto whatsapp = normalized.whatsappConfig();
      return new AdminConnectionConfigPayload(
          normalized.botConfig(),
          normalized.ircServerConfigs(),
          normalized.discordConfig(),
          normalized.telegramConfig(),
          new WhatsAppConfigDto(whatsapp.network(), whatsapp.sendBaseUrl(), whatsapp.connectStartup(), appendChannel(whatsapp.channelList(), channel)));
    }
    throw new IllegalArgumentException("Unsupported connection type for channel promotion: " + type);
  }

  private AdminConnectionConfigPayload promoteIrcChannel(
      AdminConnectionConfigPayload payload,
      String network,
      ChannelDto channel) {
    List<IrcServerConfigDto> configs = payload.ircServerConfigs();
    if (configs.isEmpty()) {
      throw new IllegalArgumentException("No IRC server config exists for promoted channel");
    }

    int targetIndex = findIrcConfigIndex(configs, network);
    if (targetIndex < 0) {
      throw new IllegalArgumentException("No IRC server config found for network: " + network);
    }

    List<IrcServerConfigDto> updated = new ArrayList<>(configs);
    IrcServerConfigDto target = updated.get(targetIndex);
    updated.set(targetIndex, new IrcServerConfigDto(
        target.name(),
        target.connectStartup(),
        target.networkName(),
        target.host(),
        target.port(),
        appendChannel(target.channelList(), channel)));

    return new AdminConnectionConfigPayload(
        payload.botConfig(),
        updated,
        payload.discordConfig(),
        payload.telegramConfig(),
        payload.whatsappConfig());
  }

  private int findIrcConfigIndex(List<IrcServerConfigDto> configs, String network) {
    String normalizedNetwork = clean(network);
    if (normalizedNetwork != null) {
      for (int i = 0; i < configs.size(); i++) {
        String configNetwork = clean(configs.get(i).networkName());
        if (configNetwork != null && configNetwork.equalsIgnoreCase(normalizedNetwork)) {
          return i;
        }
      }
    }
    return configs.size() == 1 ? 0 : -1;
  }

  private ChannelDto normalizedPromotedChannel(PromoteChannelRequest request) {
    ChannelDto source = request.channel();
    String echoToAlias = required(source.echoToAlias(), "Promoted channel alias is required");
    String name = clean(source.name());
    String id = clean(source.id());
    return new ChannelDto(
        id == null ? echoToAlias : id,
        clean(source.description()),
        name == null ? echoToAlias : name,
        firstNonBlank(source.type(), defaultChannelType(request.connectionType())),
        echoToAlias,
        normalizeAliases(source.echoToAliases()),
        source.joinOnStart(),
        source.publicAiEnabled(),
        source.allowAnonymousAiCommands(),
        source.resolveUrls(),
        source.alertMessages(),
        source.captureResolvedUrls(),
        source.captureImages(),
        normalizeAliases(source.captureImageToAliases()));
  }

  private List<ChannelDto> appendChannel(List<ChannelDto> channels, ChannelDto channel) {
    List<ChannelDto> updated = new ArrayList<>(channels == null ? List.of() : channels);
    updated.add(channel);
    return updated;
  }

  private boolean hasConfiguredChannel(
      AdminConnectionConfigPayload payload,
      String connectionType,
      String network,
      String echoToAlias) {
    String type = clean(connectionType);
    if (isConnectionType(type, "IRC_CONNECTION")) {
      return payload.ircServerConfigs().stream()
          .filter(config -> network == null || network.isBlank() || equalsIgnoreCase(config.networkName(), network))
          .flatMap(config -> config.channelList().stream())
          .anyMatch(channel -> equalsIgnoreCase(channel.echoToAlias(), echoToAlias));
    }
    if (isConnectionType(type, "DISCORD_CONNECTION")) {
      return hasChannelAlias(payload.discordConfig().channelList(), echoToAlias);
    }
    if (isConnectionType(type, "TELEGRAM_CONNECTION")) {
      return hasChannelAlias(payload.telegramConfig().channelList(), echoToAlias);
    }
    if (isConnectionType(type, "WHATSAPP_CONNECTION")) {
      return hasChannelAlias(payload.whatsappConfig().channelList(), echoToAlias);
    }
    return false;
  }

  private ChannelDto findConfiguredChannel(
      AdminConnectionConfigPayload payload,
      String connectionType,
      String network,
      String echoToAlias) {
    String type = clean(connectionType);
    if (isConnectionType(type, "IRC_CONNECTION")) {
      return payload.ircServerConfigs().stream()
          .filter(config -> network == null || network.isBlank() || equalsIgnoreCase(config.networkName(), network))
          .flatMap(config -> config.channelList().stream())
          .filter(channel -> equalsIgnoreCase(channel.echoToAlias(), echoToAlias))
          .findFirst()
          .orElse(null);
    }
    if (isConnectionType(type, "DISCORD_CONNECTION")) {
      return findChannelAlias(payload.discordConfig().channelList(), echoToAlias);
    }
    if (isConnectionType(type, "TELEGRAM_CONNECTION")) {
      return findChannelAlias(payload.telegramConfig().channelList(), echoToAlias);
    }
    if (isConnectionType(type, "WHATSAPP_CONNECTION")) {
      return findChannelAlias(payload.whatsappConfig().channelList(), echoToAlias);
    }
    return null;
  }

  private boolean hasChannelAlias(List<ChannelDto> channels, String echoToAlias) {
    return channels != null && channels.stream()
        .anyMatch(channel -> equalsIgnoreCase(channel.echoToAlias(), echoToAlias));
  }

  private ChannelDto findChannelAlias(List<ChannelDto> channels, String echoToAlias) {
    if (channels == null) {
      return null;
    }
    return channels.stream()
        .filter(channel -> equalsIgnoreCase(channel.echoToAlias(), echoToAlias))
        .findFirst()
        .orElse(null);
  }

  private ChannelSettingsUpdate updateChannelSettings(
      AdminConnectionConfigPayload payload,
      String connectionType,
      String network,
      String echoToAlias,
      LiveChannelSettingsDto settings) {
    String type = clean(connectionType);
    if (isConnectionType(type, "IRC_CONNECTION")) {
      ChannelDto updatedChannel = null;
      List<IrcServerConfigDto> ircConfigs = new ArrayList<>();
      for (IrcServerConfigDto config : payload.ircServerConfigs()) {
        if (network != null && !network.isBlank() && !equalsIgnoreCase(config.networkName(), network)) {
          ircConfigs.add(config);
          continue;
        }
        ChannelListUpdate update = updateChannelListSettings(config.channelList(), echoToAlias, settings);
        if (update.updatedChannel() != null) {
          updatedChannel = update.updatedChannel();
          ircConfigs.add(new IrcServerConfigDto(
              config.name(),
              config.connectStartup(),
              config.networkName(),
              config.host(),
              config.port(),
              update.channels()));
        } else {
          ircConfigs.add(config);
        }
      }
      return new ChannelSettingsUpdate(
          new AdminConnectionConfigPayload(
              payload.botConfig(),
              ircConfigs,
              payload.discordConfig(),
              payload.telegramConfig(),
              payload.whatsappConfig()),
          updatedChannel != null,
          updatedChannel);
    }
    if (isConnectionType(type, "DISCORD_CONNECTION")) {
      ChannelListUpdate update = updateChannelListSettings(payload.discordConfig().channelList(), echoToAlias, settings);
      return new ChannelSettingsUpdate(
          new AdminConnectionConfigPayload(
              payload.botConfig(),
              payload.ircServerConfigs(),
              new DiscordConfigDto(
                  payload.discordConfig().connectStartup(),
                  payload.discordConfig().theBotUserId(),
                  update.channels()),
              payload.telegramConfig(),
              payload.whatsappConfig()),
          update.updatedChannel() != null,
          update.updatedChannel());
    }
    if (isConnectionType(type, "TELEGRAM_CONNECTION")) {
      ChannelListUpdate update = updateChannelListSettings(payload.telegramConfig().channelList(), echoToAlias, settings);
      return new ChannelSettingsUpdate(
          new AdminConnectionConfigPayload(
              payload.botConfig(),
              payload.ircServerConfigs(),
              payload.discordConfig(),
              new TelegramConfigDto(
                  payload.telegramConfig().telegramName(),
                  payload.telegramConfig().connectStartup(),
                  update.channels()),
              payload.whatsappConfig()),
          update.updatedChannel() != null,
          update.updatedChannel());
    }
    if (isConnectionType(type, "WHATSAPP_CONNECTION")) {
      ChannelListUpdate update = updateChannelListSettings(payload.whatsappConfig().channelList(), echoToAlias, settings);
      return new ChannelSettingsUpdate(
          new AdminConnectionConfigPayload(
              payload.botConfig(),
              payload.ircServerConfigs(),
              payload.discordConfig(),
              payload.telegramConfig(),
              new WhatsAppConfigDto(
                  payload.whatsappConfig().network(),
                  payload.whatsappConfig().sendBaseUrl(),
                  payload.whatsappConfig().connectStartup(),
                  update.channels())),
          update.updatedChannel() != null,
          update.updatedChannel());
    }
    return new ChannelSettingsUpdate(payload, false, null);
  }

  private ChannelListUpdate updateChannelListSettings(
      List<ChannelDto> channels,
      String echoToAlias,
      LiveChannelSettingsDto settings) {
    List<ChannelDto> updated = new ArrayList<>();
    ChannelDto updatedChannel = null;
    for (ChannelDto channel : channels == null ? List.<ChannelDto>of() : channels) {
      if (equalsIgnoreCase(channel.echoToAlias(), echoToAlias)) {
        ChannelDto patched = copyWithSettings(channel, settings);
        updated.add(patched);
        updatedChannel = patched;
      } else {
        updated.add(channel);
      }
    }
    return new ChannelListUpdate(updated, updatedChannel);
  }

  private ChannelDto copyWithSettings(ChannelDto channel, LiveChannelSettingsDto settings) {
    return new ChannelDto(
        channel.id(),
        channel.description(),
        channel.name(),
        channel.type(),
        channel.echoToAlias(),
        channel.echoToAliases(),
        channel.joinOnStart(),
        settings.publicAiEnabled(),
        settings.allowAnonymousAiCommands(),
        settings.resolveUrls(),
        channel.alertMessages(),
        settings.captureResolvedUrls(),
        settings.captureImages(),
        channel.captureImageToAliases());
  }

  private LiveChannelSettingsDto settingsFrom(ChannelDto channel) {
    return new LiveChannelSettingsDto(
        channel.publicAiEnabled(),
        channel.allowAnonymousAiCommands(),
        channel.resolveUrls(),
        channel.captureResolvedUrls(),
        channel.captureImages());
  }

  private boolean isConnectionType(String actual, String expected) {
    return actual != null && actual.equalsIgnoreCase(expected);
  }

  private boolean equalsIgnoreCase(String left, String right) {
    String cleanLeft = clean(left);
    String cleanRight = clean(right);
    return cleanLeft != null && cleanRight != null && cleanLeft.equalsIgnoreCase(cleanRight);
  }

  private String defaultChannelType(String connectionType) {
    String type = clean(connectionType);
    if (isConnectionType(type, "IRC_CONNECTION")) {
      return "IrcPublic";
    }
    if (isConnectionType(type, "DISCORD_CONNECTION")) {
      return "SERVER_TEXT_CHANNEL";
    }
    if (isConnectionType(type, "TELEGRAM_CONNECTION") || isConnectionType(type, "WHATSAPP_CONNECTION")) {
      return "group";
    }
    return null;
  }

  private String firstNonBlank(String first, String second) {
    String cleanedFirst = clean(first);
    return cleanedFirst != null ? cleanedFirst : clean(second);
  }

  private BotConfigDto normalizeBotConfig(BotConfigDto config) {
    if (config == null) {
      return new BotConfigDto(null, null);
    }
    return new BotConfigDto(clean(config.botName()), clean(config.ircRealName()));
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
    return new DiscordConfigDto(config.connectStartup(), clean(config.theBotUserId()), normalizeChannels(config.channelList()));
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
            channel.joinOnStart(),
            channel.publicAiEnabled(),
            channel.allowAnonymousAiCommands(),
            channel.resolveUrls(),
            channel.alertMessages(),
            channel.captureResolvedUrls(),
            channel.captureImages(),
            normalizeAliases(channel.captureImageToAliases())))
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
        && normalizeAliases(channel.echoToAliases()).isEmpty()
        && normalizeAliases(channel.captureImageToAliases()).isEmpty();
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

  private ObjectNode botConfigToNode(ObjectNode existing, BotConfigDto config) {
    ObjectNode node = existing == null ? jsonMapper.createObjectNode() : existing.deepCopy();
    node.remove("openAiApiKey");
    putNullable(node, "botName", config.botName());
    putNullable(node, "ircRealName", config.ircRealName());
    return node;
  }

  private ObjectNode discordConfigToNode(ObjectNode existing, DiscordConfigDto config) {
    ObjectNode node = existing == null ? jsonMapper.createObjectNode() : existing.deepCopy();
    node.set("channelList", channelsToNode(config.channelList()));
    node.put("connectStartup", config.connectStartup());
    putNullable(node, "theBotUserId", config.theBotUserId());
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
      item.put("publicAiEnabled", channel.publicAiEnabled());
      item.put("allowAnonymousAiCommands", channel.allowAnonymousAiCommands());
      item.put("resolveUrls", channel.resolveUrls());
      item.put("alertMessages", channel.alertMessages());
      item.put("captureResolvedUrls", channel.captureResolvedUrls());
      item.put("captureImages", channel.captureImages());
      ArrayNode captureAliases = jsonMapper.createArrayNode();
      channel.captureImageToAliases().forEach(captureAliases::add);
      item.set("captureImageToAliases", captureAliases);
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

  private record ChannelListUpdate(List<ChannelDto> channels, ChannelDto updatedChannel) {
  }

  private record ChannelSettingsUpdate(AdminConnectionConfigPayload payload, boolean updated, ChannelDto channel) {
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
      BotConfigDto botConfig,
      List<IrcServerConfigDto> ircServerConfigs,
      DiscordConfigDto discordConfig,
      TelegramConfigDto telegramConfig,
      WhatsAppConfigDto whatsappConfig) {
  }

  public record PromoteChannelRequest(
      String connectionType,
      String network,
      ChannelDto channel) {
  }

  public record LiveChannelSettingsDto(
      boolean publicAiEnabled,
      boolean allowAnonymousAiCommands,
      boolean resolveUrls,
      boolean captureResolvedUrls,
      boolean captureImages) {
  }

  public record LiveChannelSettingsApplyResponse(
      String status,
      LiveChannelSettingsDto settings,
      List<ApplyTargetResult> targets) {
  }

  public record BotConfigDto(
      String botName,
      String ircRealName) {
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
      String theBotUserId,
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
      boolean joinOnStart,
      boolean publicAiEnabled,
      boolean allowAnonymousAiCommands,
      boolean resolveUrls,
      boolean alertMessages,
      boolean captureResolvedUrls,
      boolean captureImages,
      List<String> captureImageToAliases) {

    public ChannelDto(
        String id,
        String description,
        String name,
        String type,
        String echoToAlias,
        List<String> echoToAliases,
        boolean joinOnStart,
        boolean publicAiEnabled,
        boolean allowAnonymousAiCommands,
        boolean resolveUrls,
        boolean alertMessages) {
      this(
          id,
          description,
          name,
          type,
          echoToAlias,
          echoToAliases,
          joinOnStart,
          publicAiEnabled,
          allowAnonymousAiCommands,
          resolveUrls,
          alertMessages,
          false,
          false,
          List.of());
    }
  }
}
