package org.freakz.common.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.env.MockEnvironment;
import tools.jackson.databind.json.JsonMapper;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BotConfigServiceTest {

  @TempDir
  Path tempDir;

  @Test
  void runtimeOverrideWinsOverFileEnvAndDefault() throws Exception {
    BotConfigService service =
        service("feature.enabled=file", "FEATURE_ENABLED", "env", key -> Optional.of("override"));

    service.reloadConfig();

    assertThat(service.getConfigValue("feature.enabled", "FEATURE_ENABLED", "default"))
        .isEqualTo("override");
  }

  @Test
  void propertiesFileWinsOverEnvAndDefault() throws Exception {
    BotConfigService service = service("feature.enabled=file", "FEATURE_ENABLED", "env");

    service.reloadConfig();

    assertThat(service.getConfigValue("feature.enabled", "FEATURE_ENABLED", "default"))
        .isEqualTo("file");
  }

  @Test
  void envWinsOverDefaultWhenFileValueIsMissing() throws Exception {
    BotConfigService service = service("", "FEATURE_ENABLED", "env");

    service.reloadConfig();

    assertThat(service.getConfigValue("feature.enabled", "FEATURE_ENABLED", "default"))
        .isEqualTo("env");
  }

  @Test
  void defaultIsUsedWhenFileAndEnvAreMissing() throws Exception {
    BotConfigService service = service("", null, null);

    service.reloadConfig();

    assertThat(service.getConfigValue("feature.enabled", null, "default"))
        .isEqualTo("default");
  }

  @Test
  void unresolvedPlaceholderFailsClearly() throws Exception {
    BotConfigService service = service("feature.enabled=${MISSING_TEST_VALUE}", null, null);

    service.reloadConfig();

    assertThatThrownBy(() -> service.getConfigValue("feature.enabled", null, "default"))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("Missing environment variable for config placeholder: MISSING_TEST_VALUE");
  }

  private BotConfigService service(String extraProperties, String envKey, String envValue) throws Exception {
    return service(extraProperties, envKey, envValue, BotConfigOverrideSource.NONE);
  }

  private BotConfigService service(
      String extraProperties,
      String envKey,
      String envValue,
      BotConfigOverrideSource overrideSource) throws Exception {
    Path configFile = writeBootstrapProperties(extraProperties);
    MockEnvironment environment = new MockEnvironment().withProperty("BOT_CONFIG_FILE", configFile.toString());
    if (envKey != null && envValue != null) {
      environment.withProperty(envKey, envValue);
    }

    return new BotConfigService(
        environment,
        JsonMapper.builder().build(),
        new BotConfigDefaults(null, tempDir.toString(), tempDir.resolve("data").toString(), tempDir.resolve("logs").toString()),
        overrideSource);
  }

  private Path writeBootstrapProperties(String extraProperties) throws Exception {
    Path runtimeConfig = tempDir.resolve("TEST.the_bot_config.json");
    Files.writeString(runtimeConfig, "{\"botConfig\":{\"botName\":\"TestBot\"}}\n");

    Properties properties = new Properties();
    properties.setProperty("hokan.runtime.profile", "TEST");
    properties.setProperty("the.bot.runtimeDir", "./");
    properties.setProperty("the.bot.dataDir", "./data/");
    properties.setProperty("the.bot.logDir", "./logs/");
    properties.setProperty("the.bot.runtimeConfigFile", "./TEST.the_bot_config.json");

    Path configFile = tempDir.resolve("test.properties");
    StringBuilder content = new StringBuilder();
    for (String key : properties.stringPropertyNames()) {
      content.append(key).append("=").append(properties.getProperty(key)).append("\n");
    }
    if (extraProperties != null && !extraProperties.isBlank()) {
      content.append(extraProperties).append("\n");
    }
    Files.writeString(configFile, content.toString());
    return configFile;
  }
}
