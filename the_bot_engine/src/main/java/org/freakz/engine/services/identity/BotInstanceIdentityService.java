package org.freakz.engine.services.identity;

import org.freakz.engine.config.ConfigService;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.regex.Pattern;

@Service
public class BotInstanceIdentityService {

  private static final Pattern VALID_INSTANCE_ID = Pattern.compile("[a-z0-9_-]+");

  private final ConfigService configService;

  public BotInstanceIdentityService(ConfigService configService) {
    this.configService = configService;
  }

  public String getInstanceId() {
    String configured =
        configService.getConfigValue("hokan.bot.instance-id", "HOKAN_BOT_INSTANCE_ID", null);
    if (configured == null || configured.isBlank()) {
      configured = configService.getActiveProfile();
    }
    return normalizeInstanceId(configured);
  }

  public String normalizeInstanceId(String value) {
    String normalized = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    if (normalized.isBlank()) {
      throw new IllegalStateException("Missing bot instance identity");
    }
    if (!VALID_INSTANCE_ID.matcher(normalized).matches()) {
      throw new IllegalStateException(
          "Invalid bot instance identity '" + value + "'. Allowed characters: lowercase letters, numbers, dash and underscore.");
    }
    return normalized;
  }

}
