package org.freakz.hermesmanager.config;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "hermes.manager")
public record HermesManagerProperties(
    Path dataDir,
    String containerName,
    List<String> profiles,
    String profilePorts,
    String defaultBaseUrl,
    String defaultModel,
    String token,
    String localCredentialKey,
    Integer localValidationTimeoutSeconds) {

  public HermesManagerProperties(
      Path dataDir,
      String containerName,
      List<String> profiles,
      String profilePorts,
      String defaultBaseUrl,
      String defaultModel,
      String token,
      String localCredentialKey) {
    this(
        dataDir,
        containerName,
        profiles,
        profilePorts,
        defaultBaseUrl,
        defaultModel,
        token,
        localCredentialKey,
        null);
  }

  public int resolvedLocalValidationTimeoutSeconds() {
    return localValidationTimeoutSeconds == null || localValidationTimeoutSeconds <= 0
        ? 180
        : localValidationTimeoutSeconds;
  }

  public Map<String, Integer> ports() {
    return List.of(profilePorts.split(",")).stream()
        .map(value -> value.split(":", 2))
        .filter(parts -> parts.length == 2)
        .collect(Collectors.toMap(parts -> parts[0], parts -> Integer.parseInt(parts[1])));
  }
}
