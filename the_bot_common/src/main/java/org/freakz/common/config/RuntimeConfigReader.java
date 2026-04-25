package org.freakz.common.config;


import org.freakz.common.model.botconfig.TheBotConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.freakz.common.config.ConfigConstants.RUNTIME_CONFIG_FILE_NAME;

public class RuntimeConfigReader {

  private static final Logger log = LoggerFactory.getLogger(RuntimeConfigReader.class);
  private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\$\\{([A-Za-z0-9_.-]+)}");

  public TheBotConfig readBotConfig(
      JsonMapper mapper, BotRuntimeBootstrapConfig bootstrapConfig)
      throws IOException {
    String profile = bootstrapConfig.profile();
    log.debug("readBotConfig --->>> PROFILE: {}", profile);
    String cfgFile;
    if (bootstrapConfig.runtimeConfigFile() != null && !bootstrapConfig.runtimeConfigFile().isBlank()) {
      cfgFile = bootstrapConfig.runtimeConfigFile();
    } else if (profile != null && profile.length() > 0) {
      log.debug("Prefixing profile to runtime config file: {}", profile);
      cfgFile = bootstrapConfig.runtimeDir() + profile + "." + RUNTIME_CONFIG_FILE_NAME;
    } else {
      cfgFile = bootstrapConfig.runtimeDir() + RUNTIME_CONFIG_FILE_NAME;
    }

    log.debug("Reading runtime config from: {}", cfgFile);

    Path path = Path.of(cfgFile);
    String json = Files.readString(path);
    String replacedJson = replaceEnvPlaceholders(json);

    TheBotConfig theConfig = mapper.readValue(replacedJson, TheBotConfig.class);
    return theConfig;
  }

  private String replaceEnvPlaceholders(String text) {
    Matcher matcher = PLACEHOLDER_PATTERN.matcher(text);
    StringBuffer result = new StringBuffer();
    List<String> missingKeys = new ArrayList<>();

    while (matcher.find()) {
      String key = matcher.group(1);
      String value = firstNonBlank(System.getenv(key), System.getProperty(key));
      if (value == null) {
        missingKeys.add(key);
        continue;
      }
      matcher.appendReplacement(result, Matcher.quoteReplacement(value));
    }
    matcher.appendTail(result);

    if (!missingKeys.isEmpty()) {
      throw new IllegalStateException(
          "Missing environment variables for runtime config placeholders: " + String.join(", ", missingKeys));
    }

    return result.toString();
  }

  private String firstNonBlank(String... values) {
    for (String value : values) {
      if (value != null && !value.isBlank()) {
        return value;
      }
    }
    return null;
  }
}
