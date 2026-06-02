package org.freakz.engine.services.ai.hermes;

import org.freakz.common.model.engine.system.HermesSettingsResponse;
import org.freakz.engine.config.ConfigService;
import org.springframework.stereotype.Service;

@Service
public class HermesSettingsService {

  private static final String DEFAULT_BASE_URL = "http://ubuntu-server.local:8643";
  private static final String DEFAULT_MODEL = "hermes-chat";
  private static final String DEFAULT_API_MODE = "responses";
  private static final int DEFAULT_TIMEOUT_SECONDS = 120;

  private final ConfigService configService;

  public HermesSettingsService(ConfigService configService) {
    this.configService = configService;
  }

  HermesSettings resolveSettings() {
    String baseUrl = trimTrailingSlash(firstNonBlank(
        configService.getConfigValue("hermes.chat.base-url", "HERMES_CHAT_BASE_URL", ""),
        configService.getConfigValue("hermes.base-url", "HERMES_BASE_URL", ""),
        DEFAULT_BASE_URL
    ));
    String apiKey = firstNonBlank(
        configService.getConfigValue("hermes.chat.api-key", "HERMES_CHAT_API_KEY", ""),
        configService.getConfigValue("hermes.api-key", "HERMES_API_KEY", "")
    );
    String model = firstNonBlank(
        configService.getConfigValue("hermes.chat.model", "HERMES_CHAT_MODEL", ""),
        configService.getConfigValue("hermes.model", "HERMES_MODEL", ""),
        DEFAULT_MODEL
    );
    int timeoutSeconds = parseInt(
        firstNonBlank(
            configService.getConfigValue("hermes.chat.timeout-seconds", "HERMES_CHAT_TIMEOUT_SECONDS", ""),
            configService.getConfigValue("hermes.timeout-seconds", "HERMES_TIMEOUT_SECONDS", ""),
            Integer.toString(DEFAULT_TIMEOUT_SECONDS)
        ),
        DEFAULT_TIMEOUT_SECONDS
    );
    String apiMode = firstNonBlank(
        configService.getConfigValue("hermes.chat.api-mode", "HERMES_CHAT_API_MODE", ""),
        configService.getConfigValue("hermes.api-mode", "HERMES_API_MODE", ""),
        DEFAULT_API_MODE
    );
    return new HermesSettings(baseUrl, apiKey == null ? "" : apiKey.trim(), model, timeoutSeconds, apiMode);
  }

  public HermesSettingsResponse getSettings() {
    HermesSettings settings = resolveSettings();
    return new HermesSettingsResponse(
        settings.baseUrl(),
        settings.model(),
        settings.apiMode(),
        settings.timeoutSeconds(),
        settings.configured());
  }

  String getBotInstanceId() {
    return configService.getConfigValue("hokan.bot.instance-id", "HOKAN_BOT_INSTANCE_ID", "dev");
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
}
