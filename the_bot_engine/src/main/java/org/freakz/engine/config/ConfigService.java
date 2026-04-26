package org.freakz.engine.config;

import org.freakz.common.config.BotConfigDefaults;
import org.freakz.common.config.BotConfigService;
import org.freakz.common.config.TheBotProperties;
import org.freakz.common.model.botconfig.TheBotConfig;
import org.freakz.common.model.env.SysEnvValue;
import org.freakz.engine.data.service.EnvValuesService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import tools.jackson.databind.json.JsonMapper;

import java.io.File;
import java.io.IOException;

@Service
public class ConfigService {

  @Autowired
  private TheBotProperties botProperties;
  @Autowired
  private Environment environment;
  @Autowired
  private JsonMapper objectMapper;
  @Autowired
  private ObjectProvider<EnvValuesService> envValuesServiceProvider;

  private BotConfigService delegate;

  public TheBotConfig readBotConfig() {
    try {
      return delegate().readBotConfig();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public String getActiveProfile() {
    return delegate().getActiveProfile();
  }

  public void reloadConfig() throws IOException {
    delegate().reloadConfig();
  }

  public File getRuntimeDirFile(String fileName) {
    return delegate().getRuntimeDirFile(fileName);
  }

  public File getRuntimeDataFile(String fileName) {
    return delegate().getRuntimeDataFile(fileName);
  }

  public String getRuntimeDirFileName(String fileName) {
    return delegate().getRuntimeDirFileName(fileName);
  }

  public String getRuntimeDataFileName(String fileName) {
    return delegate().getRuntimeDataFileName(fileName);
  }

  public String getBotLogDir() {
    return delegate().getBotLogDir();
  }

  public String getConfigValue(String propertyKey, String envKey, String defaultValue) {
    return delegate().getConfigValue(propertyKey, envKey, defaultValue);
  }

  public String getRequiredConfigValue(String propertyKey, String envKey) {
    return delegate().getRequiredConfigValue(propertyKey, envKey);
  }

  public int getConfigIntValue(String propertyKey, String envKey, int defaultValue) {
    return delegate().getConfigIntValue(propertyKey, envKey, defaultValue);
  }

  public long getConfigLongValue(String propertyKey, String envKey, long defaultValue) {
    return delegate().getConfigLongValue(propertyKey, envKey, defaultValue);
  }

  public boolean getConfigBooleanValue(String propertyKey, String envKey, boolean defaultValue) {
    return delegate().getConfigBooleanValue(propertyKey, envKey, defaultValue);
  }

  private BotConfigService delegate() {
    if (delegate == null) {
      delegate =
          new BotConfigService(
              environment,
              objectMapper,
              new BotConfigDefaults(
                  botProperties.getConfigFile(),
                  botProperties.getRuntimeDir(),
                  botProperties.getDataDir(),
                  botProperties.getLogDir()),
              this::findRuntimeOverride);
    }
    return delegate;
  }

  private java.util.Optional<String> findRuntimeOverride(String propertyKey) {
    if (!isRuntimeOverridable(propertyKey)) {
      return java.util.Optional.empty();
    }
    try {
      EnvValuesService envValuesService = envValuesServiceProvider.getIfAvailable();
      if (envValuesService == null) {
        return java.util.Optional.empty();
      }
      SysEnvValue value = envValuesService.findFirstByKey(propertyKey);
      if (value == null) {
        return java.util.Optional.empty();
      }
      return java.util.Optional.of(value.getValue() == null ? "" : value.getValue().trim());
    } catch (RuntimeException e) {
      return java.util.Optional.empty();
    }
  }

  private boolean isRuntimeOverridable(String propertyKey) {
    return propertyKey != null && propertyKey.startsWith("channel.");
  }
}
