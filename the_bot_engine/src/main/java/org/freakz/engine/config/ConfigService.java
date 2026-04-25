package org.freakz.engine.config;

import org.freakz.common.config.BotRuntimeBootstrapConfig;
import org.freakz.common.config.BotRuntimeBootstrapLoader;
import org.freakz.common.config.RuntimeConfigReader;
import org.freakz.common.model.botconfig.TheBotConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import tools.jackson.databind.json.JsonMapper;

import java.io.File;
import java.io.IOException;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ConfigService {

  private static final Logger log = LoggerFactory.getLogger(ConfigService.class);
  private static RuntimeConfigReader configReader = new RuntimeConfigReader();
  private static final BotRuntimeBootstrapLoader bootstrapLoader = new BotRuntimeBootstrapLoader();
  private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\$\\{([A-Za-z0-9_.-]+)}");
  @Autowired
  private TheBotProperties botProperties;
  @Autowired
  private Environment environment;
  @Autowired
  private JsonMapper objectMapper;
  private TheBotConfig theBotConfig = null;
  private BotRuntimeBootstrapConfig bootstrapConfig;

  public TheBotConfig readBotConfig() {
    if (theBotConfig == null) {
      try {
        reloadConfig();
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
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
            botProperties.getConfigFile(),
            botProperties.getRuntimeDir(),
            botProperties.getDataDir(),
            botProperties.getLogDir());
    theBotConfig = configReader.readBotConfig(objectMapper, bootstrapConfig);
  }

  public File getRuntimeDirFile(String fileName) {
    File file = new File(getRuntimeDir() + fileName);
    return file;
  }

  public File getRuntimeDataFile(String fileName) {
    File file = new File(getDataDir() + fileName);
    return file;
  }

  public String getRuntimeDirFileName(String fileName) {
    return getRuntimeDir() + fileName;
  }

  public String getRuntimeDataFileName(String fileName) {
    return getDataDir() + fileName;
  }

  public String getBotLogDir() {
    return bootstrapConfig == null ? botProperties.getLogDir() : bootstrapConfig.logDir();
  }

  public String getConfigValue(String propertyKey, String envKey, String defaultValue) {
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
    return bootstrapConfig == null ? botProperties.getRuntimeDir() : bootstrapConfig.runtimeDir();
  }

  private String getDataDir() {
    return bootstrapConfig == null ? botProperties.getDataDir() : bootstrapConfig.dataDir();
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
      String replacement = firstNonBlank(
          environment.getProperty(key),
          System.getenv(key),
          System.getProperty(key));
      if (replacement == null) {
        throw new IllegalStateException("Missing environment variable for config placeholder: " + key);
      }
      matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
    }
    matcher.appendTail(result);
    return result.toString();
  }
}
