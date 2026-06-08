package org.freakz.hermesmanager.service;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

import org.freakz.common.model.engine.system.HermesFallbackModelsResponse;
import org.freakz.common.model.engine.system.HermesFallbackProfileStatus;
import org.freakz.common.model.engine.system.HermesFallbackSettingsResponse;
import org.freakz.common.model.engine.system.HermesFallbackUpdateRequest;
import org.freakz.hermesmanager.config.HermesManagerProperties;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestTemplate;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.dataformat.yaml.YAMLMapper;

@Service
public class HermesFallbackService implements ApplicationRunner {

  private static final String SETTINGS_FILE = "the_bot_fallback.json";
  private static final Logger log = LoggerFactory.getLogger(HermesFallbackService.class);
  private final ReentrantLock updateLock = new ReentrantLock();
  private final HermesManagerProperties properties;
  private final HermesGatewayService gatewayService;
  private final RestTemplate restTemplate;
  private final JsonMapper jsonMapper;
  private final YAMLMapper yamlMapper;

  public HermesFallbackService(
      HermesManagerProperties properties,
      HermesGatewayService gatewayService,
      RestTemplate restTemplate) {
    this.properties = properties;
    this.gatewayService = gatewayService;
    this.restTemplate = restTemplate;
    this.jsonMapper = JsonMapper.builder().build();
    this.yamlMapper = YAMLMapper.builder().build();
  }

  public HermesFallbackSettingsResponse getSettings() {
    HermesFallbackUpdateRequest settings = readSettings();
    return response(settings);
  }

  @Override
  public void run(ApplicationArguments args) {
    if (Files.exists(settingsPath())) {
      return;
    }
    try {
      update(new HermesFallbackUpdateRequest(properties.defaultBaseUrl(), properties.defaultModel(), false));
      log.info("Applied initial Hermes fallback configuration");
    } catch (Exception e) {
      log.error("Could not apply initial Hermes fallback configuration: {}", e.getMessage());
    }
  }

  public HermesFallbackModelsResponse getModels(String baseUrl) {
    URI uri = validatedBaseUrl(baseUrl);
    Map<String, Object> response = restTemplate.getForObject(uri.resolve(path(uri, "models")), Map.class);
    List<String> models = new ArrayList<>();
    Object data = response == null ? null : response.get("data");
    if (data instanceof List<?> values) {
      for (Object value : values) {
        if (value instanceof Map<?, ?> item && item.get("id") != null) {
          models.add(item.get("id").toString());
        }
      }
    }
    models.sort(String::compareToIgnoreCase);
    return new HermesFallbackModelsResponse(models);
  }

  public HermesFallbackSettingsResponse update(HermesFallbackUpdateRequest request) {
    URI baseUrl = validatedBaseUrl(request == null ? null : request.baseUrl());
    String model = requireValue(request == null ? null : request.model(), "model");
    boolean enabled = request != null && request.enabled();
    validateToolCall(baseUrl, model);

    updateLock.lock();
    Map<Path, byte[]> backups = new LinkedHashMap<>();
    try {
      for (String profile : properties.profiles()) {
        Path config = findProfileConfig(profile);
        backups.put(config, Files.readAllBytes(config));
        updateYaml(config, baseUrl.toString().replaceFirst("/+$", ""), model);
      }
      restartAndVerify();
      HermesFallbackUpdateRequest saved = new HermesFallbackUpdateRequest(
          baseUrl.toString().replaceFirst("/+$", ""), model, enabled);
      writeAtomically(settingsPath(), jsonMapper.writeValueAsBytes(saved));
      return response(saved);
    } catch (Exception e) {
      rollback(backups);
      throw new IllegalStateException("Could not apply Hermes fallback configuration: " + e.getMessage(), e);
    } finally {
      updateLock.unlock();
    }
  }

  private void validateToolCall(URI baseUrl, String model) {
    Map<String, Object> request = Map.of(
        "model", model,
        "messages", List.of(Map.of("role", "user", "content", "Call the ping tool now.")),
        "tools", List.of(Map.of("type", "function", "function", Map.of(
            "name", "ping",
            "description", "Connectivity test",
            "parameters", Map.of("type", "object", "properties", Map.of())))),
        "tool_choice", "required",
        "stream", false);
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    Map<?, ?> response = restTemplate.postForObject(
        baseUrl.resolve(path(baseUrl, "chat/completions")), new HttpEntity<>(request, headers), Map.class);
    if (response == null || !response.toString().contains("tool_calls")) {
      throw new IllegalArgumentException("Selected Ollama model did not produce a tool call");
    }
  }

  private void updateYaml(Path config, String baseUrl, String model) throws IOException {
    Map<String, Object> yaml = yamlMapper.readValue(Files.readAllBytes(config), new TypeReference<>() {});
    if (yaml == null) {
      yaml = new LinkedHashMap<>();
    }
    yaml.put("fallback_providers", List.of(Map.of(
        "provider", "custom",
        "model", model,
        "base_url", baseUrl)));
    writeAtomically(config, yamlMapper.writeValueAsBytes(yaml));
  }

  private void restartAndVerify() {
    for (String profile : properties.profiles()) {
      gatewayService.restart(profile);
    }
    for (String profile : properties.profiles()) {
      int port = properties.ports().getOrDefault(profile, 0);
      boolean healthy = false;
      for (int attempt = 0; attempt < 30 && !healthy; attempt++) {
        try {
          restTemplate.getForEntity("http://127.0.0.1:" + port + "/health", String.class);
          healthy = true;
        } catch (Exception e) {
          sleep();
        }
      }
      if (!healthy) {
        throw new IllegalStateException("Hermes profile " + profile + " did not become healthy");
      }
    }
  }

  private HermesFallbackSettingsResponse response(HermesFallbackUpdateRequest settings) {
    List<HermesFallbackProfileStatus> statuses = properties.profiles().stream()
        .map(profile -> {
          int port = properties.ports().getOrDefault(profile, 0);
          CredentialStatus credential = credentialStatus(profile);
          try {
            restTemplate.getForEntity("http://127.0.0.1:" + port + "/health", String.class);
            String route = settings.enabled() ? "OLLAMA_FORCED"
                : credential.openAiAvailable() ? "OPENAI_PRIMARY" : "OLLAMA_FALLBACK";
            return new HermesFallbackProfileStatus(
                profile,
                route,
                true,
                credential.openAiAvailable(),
                credential.cooldownUntil(),
                settings.enabled()
                    ? credential.detail() + " / Shared Ollama override enabled"
                    : credential.detail());
          } catch (Exception e) {
            return new HermesFallbackProfileStatus(
                profile, "UNAVAILABLE", false, credential.openAiAvailable(), credential.cooldownUntil(),
                "Gateway health check failed");
          }
        })
        .toList();
    return new HermesFallbackSettingsResponse(settings.enabled(), settings.baseUrl(), settings.model(), statuses);
  }

  private CredentialStatus credentialStatus(String profile) {
    Path authPath = properties.dataDir().resolve("profiles").resolve(profile).resolve("auth.json");
    if (!Files.exists(authPath)) {
      return new CredentialStatus(false, null, "OpenAI credentials are missing");
    }
    try {
      Map<String, Object> auth = jsonMapper.readValue(Files.readAllBytes(authPath), new TypeReference<>() {});
      Object poolValue = auth.get("credential_pool");
      if (!(poolValue instanceof Map<?, ?> pool)
          || !(pool.get("openai-codex") instanceof List<?> credentials)
          || credentials.isEmpty()) {
        return new CredentialStatus(false, null, "OpenAI credential pool is empty");
      }
      Instant now = Instant.now();
      Instant latestCooldown = null;
      for (Object value : credentials) {
        if (!(value instanceof Map<?, ?> credential)) {
          continue;
        }
        Object resetValue = credential.get("last_error_reset_at");
        if (resetValue == null || resetValue.toString().isBlank()) {
          return new CredentialStatus(true, null, "OpenAI credential available");
        }
        try {
          Instant resetAt = Instant.parse(resetValue.toString());
          if (!resetAt.isAfter(now)) {
            return new CredentialStatus(true, null, "OpenAI credential available");
          }
          if (latestCooldown == null || resetAt.isAfter(latestCooldown)) {
            latestCooldown = resetAt;
          }
        } catch (Exception ignored) {
          return new CredentialStatus(true, null, "OpenAI credential available");
        }
      }
      String cooldown = latestCooldown == null ? null : latestCooldown.toString();
      return new CredentialStatus(false, cooldown, "OpenAI quota cooldown active");
    } catch (Exception e) {
      return new CredentialStatus(false, null, "Could not inspect OpenAI credential state");
    }
  }

  private HermesFallbackUpdateRequest readSettings() {
    Path path = settingsPath();
    if (Files.exists(path)) {
      try {
        return jsonMapper.readValue(Files.readAllBytes(path), HermesFallbackUpdateRequest.class);
      } catch (IOException e) {
        throw new IllegalStateException("Could not read " + path, e);
      }
    }
    return new HermesFallbackUpdateRequest(properties.defaultBaseUrl(), properties.defaultModel(), false);
  }

  private Path findProfileConfig(String profile) throws IOException {
    try (var paths = Files.walk(properties.dataDir())) {
      return paths.filter(Files::isRegularFile)
          .filter(path -> path.getFileName().toString().matches("config\\.ya?ml"))
          .filter(path -> path.toString().contains("/" + profile + "/"))
          .min(Comparator.comparingInt(path -> path.getNameCount()))
          .orElseThrow(() -> new IllegalStateException("Could not find config YAML for Hermes profile " + profile));
    }
  }

  private void rollback(Map<Path, byte[]> backups) {
    backups.forEach((path, bytes) -> {
      try {
        writeAtomically(path, bytes);
      } catch (IOException ignored) {
      }
    });
    for (String profile : properties.profiles()) {
      try {
        gatewayService.restart(profile);
      } catch (Exception ignored) {
      }
    }
  }

  private void writeAtomically(Path path, byte[] bytes) throws IOException {
    Files.createDirectories(path.getParent());
    Path temporary = Files.createTempFile(path.getParent(), path.getFileName().toString(), ".tmp");
    Files.write(temporary, bytes);
    try {
      Files.move(temporary, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
    } catch (java.nio.file.AtomicMoveNotSupportedException e) {
      Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING);
    }
  }

  private URI validatedBaseUrl(String value) {
    String cleaned = requireValue(value, "baseUrl").replaceFirst("/+$", "") + "/";
    URI uri = URI.create(cleaned);
    if (!"http".equals(uri.getScheme()) && !"https".equals(uri.getScheme())) {
      throw new IllegalArgumentException("baseUrl must use http or https");
    }
    return uri;
  }

  private String requireValue(String value, String field) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(field + " is required");
    }
    return value.trim();
  }

  private String path(URI baseUrl, String suffix) {
    String path = baseUrl.getPath();
    return (path == null || path.isBlank() ? "/" : path) + suffix;
  }

  private Path settingsPath() {
    return properties.dataDir().resolve(SETTINGS_FILE);
  }

  private void sleep() {
    try {
      Thread.sleep(Duration.ofSeconds(1));
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  private record CredentialStatus(boolean openAiAvailable, String cooldownUntil, String detail) {
  }
}
