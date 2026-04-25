package org.freakz.common.config;

import org.springframework.core.env.Environment;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BotRuntimeBootstrapLoader {

  public static final String DEFAULT_PROFILE = "DEV";
  private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\$\\{([A-Za-z0-9_.-]+)}");

  public BotRuntimeBootstrapConfig load(
      Environment environment,
      String configuredConfigFile,
      String defaultRuntimeDir,
      String defaultDataDir,
      String defaultLogDir
  ) throws IOException {
    String configFileName =
        firstNonBlank(
            environment.getProperty("BOT_CONFIG_FILE"),
            configuredConfigFile
        );

    String runtimeDir = defaultRuntimeDir;
    String dataDir = defaultDataDir;
    String logDir = defaultLogDir;
    String profile =
        firstNonBlank(
            environment.getProperty("BOT_ENV"),
            environment.getProperty("hokan.runtime.profile"),
            DEFAULT_PROFILE
        );
    String runtimeConfigFile = null;
    Path configFile = null;
    Properties properties = new Properties();

    if (configFileName != null && !configFileName.isBlank()) {
      configFile = Path.of(configFileName);
      if (!Files.exists(configFile)) {
        throw new IOException("Missing bot config bootstrap file: " + configFile.toAbsolutePath());
      }

      try (InputStream input = Files.newInputStream(configFile)) {
        properties.load(input);
      }

      runtimeDir = propertyOrDefault(properties, "the.bot.runtimeDir", runtimeDir);
      dataDir = propertyOrDefault(properties, "the.bot.dataDir", dataDir);
      logDir = propertyOrDefault(properties, "the.bot.logDir", logDir);
      profile = propertyOrDefault(properties, "hokan.runtime.profile", profile);
      runtimeConfigFile =
          firstNonBlank(
              propertyOrDefault(properties, "the.bot.runtimeConfigFile", null),
              propertyOrDefault(properties, "the.bot.runtime-config-file", null)
          );
    }

    runtimeDir = ensureTrailingSlash(runtimeDir);
    dataDir = ensureTrailingSlash(dataDir);
    logDir = ensureTrailingSlash(logDir);

    return new BotRuntimeBootstrapConfig(
        configFile,
        runtimeDir,
        dataDir,
        logDir,
        profile,
        runtimeConfigFile,
        properties
    );
  }

  private String propertyOrDefault(Properties properties, String key, String defaultValue) {
    String value = properties.getProperty(key);
    if (value == null || value.isBlank()) {
      return defaultValue;
    }
    return resolvePlaceholders(value.trim());
  }

  private String ensureTrailingSlash(String value) {
    if (value == null || value.isBlank()) {
      return value;
    }
    return value.endsWith("/") ? value : value + "/";
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
    Matcher matcher = PLACEHOLDER_PATTERN.matcher(value);
    StringBuffer result = new StringBuffer();

    while (matcher.find()) {
      String key = matcher.group(1);
      String replacement = firstNonBlank(System.getenv(key), System.getProperty(key));
      if (replacement == null) {
        throw new IllegalStateException("Missing environment variable for bootstrap property placeholder: " + key);
      }
      matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
    }
    matcher.appendTail(result);
    return result.toString();
  }
}
