package org.freakz.engine.services.ai.hermes;

import java.util.List;

import org.freakz.common.model.engine.system.HermesBackend;
import org.freakz.common.model.engine.system.HermesProfileOption;
import org.freakz.common.model.engine.system.HermesBackendConfigResponse;
import org.freakz.common.model.engine.system.HermesRoute;
import org.freakz.common.model.engine.system.HermesSettingsRequest;
import org.freakz.common.model.engine.system.HermesSettingsResponse;
import org.freakz.common.model.users.User;
import org.freakz.common.spring.rest.RestHermesManagerClient;
import org.freakz.engine.config.ConfigService;
import org.freakz.engine.data.service.EnvValuesService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class HermesSettingsService {

  private static final Logger log = LoggerFactory.getLogger(HermesSettingsService.class);
  private static final String DEFAULT_BASE_URL = "http://ubuntu-server.local:8643";
  private static final String DEFAULT_MODEL = "hermes-chat";
  private static final String DEFAULT_API_MODE = "responses";
  private static final String AI_COMMAND_DEFAULT_API_MODE = "chat-completions";
  private static final int DEFAULT_TIMEOUT_SECONDS = 120;
  private static final String BASE_URL_KEY = "hermes.chat.base-url";
  private static final String API_KEY_KEY = "hermes.chat.api-key";
  private static final String MODEL_KEY = "hermes.chat.model";
  private static final String API_MODE_KEY = "hermes.chat.api-mode";
  private static final String TIMEOUT_SECONDS_KEY = "hermes.chat.timeout-seconds";
  private static final String CHAT_PROFILE_BASE_URL_KEY = "hermes.chat.profile-base-url";
  private static final String AI_COMMAND_PROFILE_ID = "ai-command";
  private static final String AI_COMMAND_DEFAULT_BASE_URL = "http://ubuntu-server.local:8645";
  private static final String AI_COMMAND_DEFAULT_MODEL = "hermes-ai-command";
  private static final String AI_COMMAND_PROFILE_ID_KEY = "hermes.ai-command.profile-id";
  private static final String AI_COMMAND_BASE_URL_KEY = "hermes.ai-command.base-url";
  private static final String AI_COMMAND_API_KEY_KEY = "hermes.ai-command.api-key";
  private static final String AI_COMMAND_MODEL_KEY = "hermes.ai-command.model";
  private static final String AI_COMMAND_API_MODE_KEY = "hermes.ai-command.api-mode";
  private static final String AI_COMMAND_TIMEOUT_SECONDS_KEY = "hermes.ai-command.timeout-seconds";

  private final ConfigService configService;
  private final EnvValuesService envValuesService;
  private final RestHermesManagerClient hermesManagerClient;

  public HermesSettingsService(ConfigService configService, EnvValuesService envValuesService) {
    this(configService, envValuesService, null);
  }

  @Autowired
  public HermesSettingsService(
      ConfigService configService,
      EnvValuesService envValuesService,
      RestHermesManagerClient hermesManagerClient) {
    this.configService = configService;
    this.envValuesService = envValuesService;
    this.hermesManagerClient = hermesManagerClient;
  }

  public HermesSettings resolveSettings() {
    HermesSettings local = resolveLocalSettings();
    HermesSettings managed = resolveManagedProfile("chat", local);
    return managed == null ? local : managed;
  }

  public HermesSettings resolveAiCommandSettings() {
    HermesSettings local = resolveLocalAiCommandSettings();
    HermesSettings managed = resolveManagedProfile(AI_COMMAND_PROFILE_ID, local);
    return managed == null ? local : managed;
  }

  public boolean aiEnabled() {
    HermesBackendConfigResponse config = managerConfig();
    return config == null || !"off".equalsIgnoreCase(config.systemMode());
  }

  public HermesSettingsResponse getSettings() {
    HermesSettings settings = resolveSettings();
    String currentProfileId = profileIdForSettings(settings);
    return new HermesSettingsResponse(
        currentProfileId,
        settings.baseUrl(),
        settings.model(),
        settings.apiMode(),
        settings.timeoutSeconds(),
        settings.configured(),
        healthUrl(settings.baseUrl()),
        options(currentProfileId));
  }

  public HermesSettingsResponse selectProfile(HermesSettingsRequest request) {
    String selectedProfileId = request == null ? null : request.selectedProfileId();
    HermesProfileConfig selected = profileConfigs(null).stream()
        .filter(profile -> profile.option().id().equals(selectedProfileId))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("Unsupported Hermes profile: " + selectedProfileId));
    if (selected.apiKey() == null || selected.apiKey().isBlank()) {
      throw new IllegalStateException("Hermes profile " + selectedProfileId + " API key is not configured");
    }

    User user = User.builder().username("bot-web").name("bot-web").build();
    envValuesService.setEnvValue(BASE_URL_KEY, selected.option().baseUrl(), user);
    envValuesService.setEnvValue(API_KEY_KEY, selected.apiKey(), user);
    envValuesService.setEnvValue(MODEL_KEY, selected.option().model(), user);
    envValuesService.setEnvValue(API_MODE_KEY, selected.option().apiMode(), user);
    envValuesService.setEnvValue(TIMEOUT_SECONDS_KEY, Integer.toString(selected.option().timeoutSeconds()), user);

    return new HermesSettingsResponse(
        selected.option().id(),
        selected.option().baseUrl(),
        selected.option().model(),
        selected.option().apiMode(),
        selected.option().timeoutSeconds(),
        true,
        selected.option().healthUrl(),
        options(selected.option().id()));
  }

  public String getBotInstanceId() {
    return configService.getConfigValue("hokan.bot.instance-id", "HOKAN_BOT_INSTANCE_ID", "dev");
  }

  private HermesSettings resolveLocalSettings() {
    String baseUrl = normalizeApiRoot(firstNonBlank(
        configService.getConfigValue(BASE_URL_KEY, "HERMES_CHAT_BASE_URL", ""),
        configService.getConfigValue("hermes.base-url", "HERMES_BASE_URL", ""),
        DEFAULT_BASE_URL
    ));
    String profileId = profileIdForBaseUrl(baseUrl);
    String apiKey = firstNonBlank(
        apiKeyForProfile(profileId),
        configService.getConfigValue(API_KEY_KEY, "HERMES_CHAT_API_KEY", ""),
        configService.getConfigValue("hermes.api-key", "HERMES_API_KEY", "")
    );
    String model = firstNonBlank(
        configService.getConfigValue(MODEL_KEY, "HERMES_CHAT_MODEL", ""),
        configService.getConfigValue("hermes.model", "HERMES_MODEL", ""),
        DEFAULT_MODEL
    );
    int timeoutSeconds = parseInt(
        firstNonBlank(
            configService.getConfigValue(TIMEOUT_SECONDS_KEY, "HERMES_CHAT_TIMEOUT_SECONDS", ""),
            configService.getConfigValue("hermes.timeout-seconds", "HERMES_TIMEOUT_SECONDS", ""),
            Integer.toString(DEFAULT_TIMEOUT_SECONDS)
        ),
        DEFAULT_TIMEOUT_SECONDS
    );
    String apiMode = firstNonBlank(
        configService.getConfigValue(API_MODE_KEY, "HERMES_CHAT_API_MODE", ""),
        configService.getConfigValue("hermes.api-mode", "HERMES_API_MODE", ""),
        DEFAULT_API_MODE
    );
    return new HermesSettings(baseUrl, apiKey == null ? "" : apiKey.trim(), model, timeoutSeconds, apiMode);
  }

  private HermesSettings resolveLocalAiCommandSettings() {
    String configuredProfileId = firstNonBlank(
        configService.getConfigValue(AI_COMMAND_PROFILE_ID_KEY, "HERMES_AI_COMMAND_PROFILE_ID", ""),
        AI_COMMAND_PROFILE_ID
    );
    HermesProfileConfig profileConfig = profileConfigById(configuredProfileId, true)
        .orElseThrow(() -> new IllegalArgumentException("Unsupported Hermes AI command profile: " + configuredProfileId));

    String baseUrl = normalizeApiRoot(firstNonBlank(
        configService.getConfigValue(AI_COMMAND_BASE_URL_KEY, "HERMES_AI_COMMAND_BASE_URL", ""),
        profileConfig.option().baseUrl()
    ));
    String profileId = profileIdForBaseUrl(baseUrl, true);
    if (profileId == null || profileId.isBlank()) {
      profileId = profileConfig.option().id();
    }

    String apiKey = firstNonBlank(
        apiKeyForProfile(profileId),
        configService.getConfigValue(AI_COMMAND_API_KEY_KEY, "HERMES_AI_COMMAND_API_KEY", ""),
        profileConfig.apiKey()
    );
    String model = firstNonBlank(
        configService.getConfigValue(AI_COMMAND_MODEL_KEY, "HERMES_AI_COMMAND_MODEL", ""),
        profileConfig.option().model()
    );
    int timeoutSeconds = parseInt(
        firstNonBlank(
            configService.getConfigValue(AI_COMMAND_TIMEOUT_SECONDS_KEY, "HERMES_AI_COMMAND_TIMEOUT_SECONDS", ""),
            Integer.toString(profileConfig.option().timeoutSeconds())
        ),
        profileConfig.option().timeoutSeconds()
    );
    String apiMode = firstNonBlank(
        configService.getConfigValue(AI_COMMAND_API_MODE_KEY, "HERMES_AI_COMMAND_API_MODE", ""),
        profileConfig.option().apiMode()
    );

    return new HermesSettings(baseUrl, apiKey == null ? "" : apiKey.trim(), model, timeoutSeconds, apiMode);
  }

  private String profileIdForSettings(HermesSettings settings) {
    if (settings == null || settings.baseUrl() == null) {
      return null;
    }
    return profileIdForBaseUrl(settings.baseUrl());
  }

  private String profileIdForBaseUrl(String baseUrl) {
    return profileIdForBaseUrl(baseUrl, false);
  }

  private String profileIdForBaseUrl(String baseUrl, boolean includeInternalProfiles) {
    if (baseUrl == null || baseUrl.isBlank()) {
      return null;
    }
    return profileConfigs(null, includeInternalProfiles).stream()
        .filter(profile -> profile.option().baseUrl().equalsIgnoreCase(baseUrl.trim()))
        .map(profile -> profile.option().id())
        .findFirst()
        .orElse(null);
  }

  private List<HermesProfileOption> options(String selectedProfileId) {
    return profileConfigs(selectedProfileId).stream()
        .map(HermesProfileConfig::option)
        .toList();
  }

  private List<HermesProfileConfig> profileConfigs(String selectedProfileId) {
    return profileConfigs(selectedProfileId, false);
  }

  private List<HermesProfileConfig> profileConfigs(String selectedProfileId, boolean includeInternalProfiles) {
    List<HermesProfileConfig> publicProfiles = List.of(
        profile("chat", "Chat route", chatProfileBaseUrl(), "hermes-chat", selectedProfileId));
    if (!includeInternalProfiles) {
      return publicProfiles;
    }
    return List.of(
        publicProfiles.get(0),
        profile(
            AI_COMMAND_PROFILE_ID,
            "AI command route",
            AI_COMMAND_DEFAULT_BASE_URL,
            AI_COMMAND_DEFAULT_MODEL,
            AI_COMMAND_DEFAULT_API_MODE,
            selectedProfileId));
  }

  private java.util.Optional<HermesProfileConfig> profileConfigById(String profileId, boolean includeInternalProfiles) {
    if (profileId == null || profileId.isBlank()) {
      return java.util.Optional.empty();
    }
    return profileConfigs(null, includeInternalProfiles).stream()
        .filter(profile -> profile.option().id().equals(profileId))
        .findFirst();
  }

  private HermesProfileConfig profile(String id, String label, String baseUrl, String model, String selectedProfileId) {
    return profile(id, label, baseUrl, model, DEFAULT_API_MODE, selectedProfileId);
  }

  private HermesProfileConfig profile(
      String id,
      String label,
      String baseUrl,
      String model,
      String apiMode,
      String selectedProfileId) {
    HermesProfileOption option = new HermesProfileOption(
        id,
        label,
        baseUrl,
        model,
        apiMode,
        DEFAULT_TIMEOUT_SECONDS,
        healthUrl(baseUrl),
        id.equals(selectedProfileId));
    return new HermesProfileConfig(option, apiKeyForProfile(id));
  }

  private String apiKeyForProfile(String id) {
    if (id == null || id.isBlank()) {
      return "";
    }
    return switch (id) {
      case "chat" -> firstNonBlank(
          configService.getConfigValue("hermes.profiles.chat.api-key", "HERMES_CHAT_API_KEY", ""),
          configService.getConfigValue("hermes.api-key", "HERMES_API_KEY", ""));
      case AI_COMMAND_PROFILE_ID -> configService.getConfigValue("hermes.profiles.ai-command.api-key", "HERMES_AI_COMMAND_API_KEY", "");
      default -> "";
    };
  }

  private String chatProfileBaseUrl() {
    return normalizeApiRoot(firstNonBlank(
        configService.getConfigValueWithoutOverride(CHAT_PROFILE_BASE_URL_KEY, "HERMES_CHAT_PROFILE_BASE_URL", ""),
        configService.getConfigValueWithoutOverride(BASE_URL_KEY, "HERMES_CHAT_BASE_URL", ""),
        configService.getConfigValueWithoutOverride("hermes.base-url", "HERMES_BASE_URL", ""),
        DEFAULT_BASE_URL
    ));
  }

  private String healthUrl(String baseUrl) {
    return baseUrl == null || baseUrl.isBlank() ? null : trimTrailingSlash(baseUrl) + "/health";
  }

  private String firstNonBlank(String... values) {
    if (values == null) {
      return null;
    }
    for (String value : values) {
      if (value != null && !value.isBlank()) {
        return value.trim();
      }
    }
    return null;
  }

  private String trimTrailingSlash(String value) {
    if (value == null) {
      return null;
    }
    return value.trim().replaceFirst("/+$", "");
  }

  private String normalizeApiRoot(String value) {
    String trimmed = trimTrailingSlash(value);
    if (trimmed == null) {
      return null;
    }
    return trimmed.replaceFirst("(?i)/v1$", "");
  }

  private HermesSettings resolveManagedProfile(String profileId, HermesSettings local) {
    try {
      HermesBackendConfigResponse config = managerConfig();
      if (config == null || "off".equalsIgnoreCase(config.systemMode()) || config.routes() == null || config.backends() == null) {
        return null;
      }
      HermesRoute route = config.routes().stream()
          .filter(candidate -> profileId.equals(candidate.id()))
          .findFirst()
          .orElse(null);
      if (route == null || route.backendId() == null) {
        return null;
      }
      HermesBackend backend = config.backends().stream()
          .filter(candidate -> route.backendId().equals(candidate.id()))
          .findFirst()
          .orElse(null);
      if (backend == null || backend.model() == null || backend.model().isBlank()) {
        return null;
      }
      int timeoutSeconds = backend.timeoutSeconds() == null ? local.timeoutSeconds() : backend.timeoutSeconds();
      String baseUrl = normalizeApiRoot(firstNonBlank(local.baseUrl(), route.baseUrl()));
      if (baseUrl == null || baseUrl.isBlank()) {
        return null;
      }
      String apiKey = apiKeyForProfile(profileId);
      return new HermesSettings(
          baseUrl,
          apiKey == null ? "" : apiKey.trim(),
          routeGatewayModelAlias(profileId),
          timeoutSeconds,
          firstNonBlank(backend.apiMode(), local.apiMode()));
    } catch (Exception e) {
      log.debug("Could not load Hermes profile {} from manager: {}", profileId, e.getMessage());
      return null;
    }
  }

  private HermesBackendConfigResponse managerConfig() {
    if (hermesManagerClient == null) {
      return null;
    }
    try {
      return hermesManagerClient.getBackendConfig().getBody();
    } catch (Exception e) {
      log.debug("Could not load Hermes manager config: {}", e.getMessage());
      return null;
    }
  }

  private String routeGatewayModelAlias(String routeId) {
    return switch (routeId) {
      case "chat" -> DEFAULT_MODEL;
      case AI_COMMAND_PROFILE_ID -> AI_COMMAND_DEFAULT_MODEL;
      default -> "hermes-" + routeId;
    };
  }

  private int parseInt(String value, int defaultValue) {
    if (value == null || value.isBlank()) {
      return defaultValue;
    }
    try {
      return Integer.parseInt(value.trim());
    } catch (NumberFormatException e) {
      return defaultValue;
    }
  }

  private record HermesProfileConfig(HermesProfileOption option, String apiKey) {
  }
}
