package org.freakz.engine.services.ai.hermes;

import java.util.List;

import org.freakz.common.model.engine.system.HermesProfileOption;
import org.freakz.common.model.engine.system.HermesSettingsRequest;
import org.freakz.common.model.engine.system.HermesSettingsResponse;
import org.freakz.common.model.users.User;
import org.freakz.engine.config.ConfigService;
import org.freakz.engine.data.service.EnvValuesService;
import org.springframework.stereotype.Service;

@Service
public class HermesSettingsService {

  private static final String DEFAULT_BASE_URL = "http://ubuntu-server.local:8643";
  private static final String DEFAULT_MODEL = "hermes-chat";
  private static final String DEFAULT_API_MODE = "responses";
  private static final int DEFAULT_TIMEOUT_SECONDS = 120;
  private static final String BASE_URL_KEY = "hermes.chat.base-url";
  private static final String API_KEY_KEY = "hermes.chat.api-key";
  private static final String MODEL_KEY = "hermes.chat.model";
  private static final String API_MODE_KEY = "hermes.chat.api-mode";
  private static final String TIMEOUT_SECONDS_KEY = "hermes.chat.timeout-seconds";

  private final ConfigService configService;
  private final EnvValuesService envValuesService;

  public HermesSettingsService(ConfigService configService, EnvValuesService envValuesService) {
    this.configService = configService;
    this.envValuesService = envValuesService;
  }

  HermesSettings resolveSettings() {
    String baseUrl = trimTrailingSlash(firstNonBlank(
        configService.getConfigValue(BASE_URL_KEY, "HERMES_CHAT_BASE_URL", ""),
        configService.getConfigValue("hermes.base-url", "HERMES_BASE_URL", ""),
        DEFAULT_BASE_URL
    ));
    String apiKey = firstNonBlank(
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

  String getBotInstanceId() {
    return configService.getConfigValue("hokan.bot.instance-id", "HOKAN_BOT_INSTANCE_ID", "dev");
  }

  private String profileIdForSettings(HermesSettings settings) {
    if (settings == null || settings.baseUrl() == null) {
      return null;
    }
    return profileConfigs(null).stream()
        .filter(profile -> profile.option().baseUrl().equalsIgnoreCase(settings.baseUrl().trim()))
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
    return List.of(
        profile("chat", "Chat profile", "http://ubuntu-server.local:8643", "hermes-chat", selectedProfileId),
        profile("coder", "Coder profile", "http://ubuntu-server.local:8644", "hermes-coder", selectedProfileId));
  }

  private HermesProfileConfig profile(String id, String label, String baseUrl, String model, String selectedProfileId) {
    HermesProfileOption option = new HermesProfileOption(
        id,
        label,
        baseUrl,
        model,
        DEFAULT_API_MODE,
        DEFAULT_TIMEOUT_SECONDS,
        healthUrl(baseUrl),
        id.equals(selectedProfileId));
    return new HermesProfileConfig(option, apiKeyForProfile(id));
  }

  private String apiKeyForProfile(String id) {
    return switch (id) {
      case "chat" -> firstNonBlank(
          configService.getConfigValue("hermes.profiles.chat.api-key", "HERMES_CHAT_API_KEY", ""),
          configService.getConfigValue("hermes.api-key", "HERMES_API_KEY", ""));
      case "coder" -> configService.getConfigValue("hermes.profiles.coder.api-key", "HERMES_CODER_API_KEY", "");
      default -> "";
    };
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
