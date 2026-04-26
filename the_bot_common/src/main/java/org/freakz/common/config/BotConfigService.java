package org.freakz.common.config;

import org.freakz.common.model.botconfig.TheBotConfig;
import org.springframework.core.env.Environment;
import tools.jackson.databind.json.JsonMapper;

import java.io.File;
import java.io.IOException;
import java.util.Optional;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BotConfigService {

  private static final RuntimeConfigReader configReader = new RuntimeConfigReader();
  private static final BotRuntimeBootstrapLoader bootstrapLoader = new BotRuntimeBootstrapLoader();
  private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\$\\{([A-Za-z0-9_.-]+)(?::([^}]*))?}");

  private final Environment environment;
  private final JsonMapper objectMapper;
  private final BotConfigDefaults defaults;
  private final BotConfigOverrideSource overrideSource;

  private TheBotConfig theBotConfig;
  private BotRuntimeBootstrapConfig bootstrapConfig;

  public BotConfigService(Environment environment, JsonMapper objectMapper, BotConfigDefaults defaults) {
    this(environment, objectMapper, defaults, BotConfigOverrideSource.NONE);
  }

  public BotConfigService(
      Environment environment,
      JsonMapper objectMapper,
      BotConfigDefaults defaults,
      BotConfigOverrideSource overrideSource) {
    this.environment = environment;
    this.objectMapper = objectMapper;
    this.defaults = defaults;
    this.overrideSource = overrideSource == null ? BotConfigOverrideSource.NONE : overrideSource;
  }

  public TheBotConfig readBotConfig() throws IOException {
    if (theBotConfig == null) {
      reloadConfig();
    }
    return theBotConfig;
  }

  public String getActiveProfile() {
    if (bootstrapConfig != null) {
      return bootstrapConfig.profile();
    }
    String activeProfile = environment.getProperty("BOT_ENV");
    if (activeProfile == null) {
      activeProfile = environment.getProperty("hokan.runtime.profile");
    }
    if (activeProfile == null) {
      activeProfile = BotRuntimeBootstrapLoader.DEFAULT_PROFILE;
    }
    return activeProfile;
  }

  public void reloadConfig() throws IOException {
    bootstrapConfig =
        bootstrapLoader.load(
            environment,
            defaults.configFile(),
            defaults.runtimeDir(),
            defaults.dataDir(),
            defaults.logDir());
    theBotConfig = configReader.readBotConfig(objectMapper, bootstrapConfig);
  }

  public File getRuntimeDirFile(String fileName) {
    return new File(getRuntimeDir() + fileName);
  }

  public File getRuntimeDataFile(String fileName) {
    return new File(getDataDir() + fileName);
  }

  public String getRuntimeDirFileName(String fileName) {
    return getRuntimeDir() + fileName;
  }

  public String getRuntimeDataFileName(String fileName) {
    return getDataDir() + fileName;
  }

  public String getBotLogDir() {
    return bootstrapConfig == null ? defaults.logDir() : bootstrapConfig.logDir();
  }

  public String getConfigValue(String propertyKey, String envKey, String defaultValue) {
    Optional<String> override = overrideSource.findOverride(propertyKey);
    if (override.isPresent()) {
      return override.get();
    }

    String fromFile = getBootstrapProperty(propertyKey);
    String fromEnv = firstNonBlank(
        envKey == null ? null : environment.getProperty(envKey),
        envKey == null ? null : System.getenv(envKey),
        envKey == null ? null : System.getProperty(envKey)
    );

    if (fromFile != null && !fromFile.isBlank()) {
      return fromFile;
    }
    if (fromEnv != null && !fromEnv.isBlank()) {
      return fromEnv;
    }
    return defaultValue;
  }

  public String getRequiredConfigValue(String propertyKey, String envKey) {
    String value = getConfigValue(propertyKey, envKey, null);
    if (value == null || value.isBlank()) {
      throw new IllegalStateException(
          "Missing required config value for property '" + propertyKey + "'" +
              (envKey == null ? "" : " or env '" + envKey + "'"));
    }
    return value;
  }

  public int getConfigIntValue(String propertyKey, String envKey, int defaultValue) {
    String value = getConfigValue(propertyKey, envKey, Integer.toString(defaultValue));
    try {
      return Integer.parseInt(value);
    } catch (Exception e) {
      return defaultValue;
    }
  }

  public long getConfigLongValue(String propertyKey, String envKey, long defaultValue) {
    String value = getConfigValue(propertyKey, envKey, Long.toString(defaultValue));
    try {
      return Long.parseLong(value);
    } catch (Exception e) {
      return defaultValue;
    }
  }

  public boolean getConfigBooleanValue(String propertyKey, String envKey, boolean defaultValue) {
    String value = getConfigValue(propertyKey, envKey, Boolean.toString(defaultValue));
    if (value == null || value.isBlank()) {
      return defaultValue;
    }
    return Boolean.parseBoolean(value);
  }

  private String getRuntimeDir() {
    return bootstrapConfig == null ? defaults.runtimeDir() : bootstrapConfig.runtimeDir();
  }

  private String getDataDir() {
    return bootstrapConfig == null ? defaults.dataDir() : bootstrapConfig.dataDir();
  }

  private String getBootstrapProperty(String key) {
    Properties properties = bootstrapConfig == null ? null : bootstrapConfig.properties();
    if (properties == null) {
      return null;
    }
    String value = properties.getProperty(key);
    if (value == null || value.isBlank()) {
      return null;
    }
    return resolvePlaceholders(value.trim());
  }

  private String firstNonBlank(String... values) {
    for (String value : values) {
      if (value != null && !value.isBlank()) {
        return value.trim();
      }
    }
    return null;
  }

  private String resolvePlaceholders(String value) {
    if (value == null || value.isBlank()) {
      return value;
    }
    if (!value.contains("${")) {
      return value;
    }
    Matcher matcher = PLACEHOLDER_PATTERN.matcher(value);
    StringBuffer result = new StringBuffer();
    while (matcher.find()) {
      String key = matcher.group(1);
      String defaultValue = matcher.group(2);
      String replacement = firstNonBlank(
          environment.getProperty(key),
          System.getenv(key),
          System.getProperty(key));
      if (replacement == null) {
        if (defaultValue != null) {
          replacement = defaultValue;
        } else {
          throw new IllegalStateException("Missing environment variable for config placeholder: " + key);
        }
      }
      matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
    }
    matcher.appendTail(result);
    return result.toString();
  }
}
