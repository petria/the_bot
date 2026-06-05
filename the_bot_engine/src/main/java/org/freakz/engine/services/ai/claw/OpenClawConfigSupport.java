package org.freakz.engine.services.ai.claw;

import org.freakz.engine.config.ConfigService;

final class OpenClawConfigSupport {

  private OpenClawConfigSupport() {
  }

  static String getConfigValue(
      ConfigService configService,
      String key,
      String envKey,
      String defaultValue
  ) {
    return configService.getConfigValue(toBootstrapPropertyKey(key), envKey, defaultValue);
  }

  static int parseIntConfig(
      ConfigService configService,
      String key,
      String envKey,
      int defaultValue
  ) {
    String value = getConfigValue(configService, key, envKey, Integer.toString(defaultValue));
    try {
      return Integer.parseInt(value);
    } catch (Exception e) {
      return defaultValue;
    }
  }

  static long parseLongConfig(
      ConfigService configService,
      String key,
      String envKey,
      long defaultValue
  ) {
    String value = getConfigValue(configService, key, envKey, null);
    if (value == null || value.isBlank()) {
      return defaultValue;
    }
    try {
      return Long.parseLong(value);
    } catch (Exception e) {
      return defaultValue;
    }
  }

  static String toBootstrapPropertyKey(String key) {
    String normalized = key.startsWith("openclaw") ? key.substring("openclaw".length()) : key;
    if (normalized.isBlank()) {
      return "openclaw";
    }
    normalized = Character.toLowerCase(normalized.charAt(0)) + normalized.substring(1);
    return "openclaw." + normalized.replaceAll("([a-z0-9])([A-Z])", "$1-$2").toLowerCase();
  }
}
