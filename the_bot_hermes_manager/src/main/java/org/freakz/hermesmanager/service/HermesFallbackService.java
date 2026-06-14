package org.freakz.hermesmanager.service;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

import org.freakz.common.model.engine.system.HermesBackendConfigResponse;
import org.freakz.common.model.engine.system.HermesBackendConfigUpdateRequest;
import org.freakz.common.model.engine.system.HermesFallbackModelsResponse;
import org.freakz.common.model.engine.system.HermesFallbackProfileStatus;
import org.freakz.common.model.engine.system.HermesFallbackSettingsResponse;
import org.freakz.common.model.engine.system.HermesFallbackUpdateRequest;
import org.freakz.common.model.engine.system.HermesProfile;
import org.freakz.hermesmanager.config.HermesManagerProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@Service
public class HermesFallbackService implements ApplicationRunner {

  private static final String SETTINGS_FILE = "the_bot_fallback.json";
  private static final String BACKEND_SETTINGS_FILE = "the_bot_hermes_backends.json";
  private static final String OPENAI_PROVIDER = "openai";
  private static final String OLLAMA_PROVIDER = "ollama";
  private static final String CHAT_COMPLETIONS_API_MODE = "chat-completions";
  private static final String RESPONSES_API_MODE = "responses";
  private static final int DEFAULT_TIMEOUT_SECONDS = 120;
  private static final Logger log = LoggerFactory.getLogger(HermesFallbackService.class);

  private final ReentrantLock updateLock = new ReentrantLock();
  private final HermesManagerProperties properties;
  private final RestTemplate restTemplate;
  private final JsonMapper jsonMapper;

  public HermesFallbackService(HermesManagerProperties properties, RestTemplate restTemplate) {
    this.properties = properties;
    this.restTemplate = restTemplate;
    this.jsonMapper = JsonMapper.builder().build();
  }

  @Override
  public void run(ApplicationArguments args) {
    if (!Files.exists(settingsPath())) {
      try {
        writeAtomically(settingsPath(), jsonMapper.writeValueAsBytes(defaultFallbackSettings()));
      } catch (IOException e) {
        log.error("Could not initialize Hermes fallback settings: {}", e.getMessage());
      }
    }
    if (!Files.exists(backendSettingsPath())) {
      try {
        writeBackendConfig(defaultBackendConfig());
      } catch (IOException e) {
        log.error("Could not initialize Hermes backend configuration: {}", e.getMessage());
      }
    }
  }

  public HermesFallbackSettingsResponse getSettings() {
    return fallbackResponse(readSettings());
  }

  public HermesFallbackSettingsResponse update(HermesFallbackUpdateRequest request) {
    URI baseUrl = validatedBaseUrl(request == null ? null : request.baseUrl());
    String model = requireValue(request == null ? null : request.model(), "model");
    boolean enabled = request != null && Boolean.TRUE.equals(request.enabled());
    validateToolCall(baseUrl, model);

    HermesFallbackUpdateRequest saved = new HermesFallbackUpdateRequest(
        trimTrailingSlash(baseUrl.toString()),
        model,
        enabled);
    updateLock.lock();
    try {
      writeAtomically(settingsPath(), jsonMapper.writeValueAsBytes(saved));
      return fallbackResponse(saved);
    } catch (IOException e) {
      throw new IllegalStateException("Could not apply Hermes fallback configuration: " + e.getMessage(), e);
    } finally {
      updateLock.unlock();
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

  public HermesBackendConfigResponse getBackendConfig() {
    return backendResponse(readBackendConfig());
  }

  public HermesBackendConfigResponse updateBackendConfig(HermesBackendConfigUpdateRequest request) {
    StoredBackendConfig config = sanitizeBackendConfig(request);
    validateProfiles(config);

    updateLock.lock();
    try {
      for (StoredProfile profile : config.profiles()) {
        if (OLLAMA_PROVIDER.equals(profile.provider())) {
          URI baseUrl = validatedBaseUrl(profile.baseUrl());
          getModels(baseUrl.toString());
          if (requiresTools(profile.id())) {
            validateToolCall(baseUrl, profile.model());
          }
        }
      }
      writeBackendConfig(config);
      return backendResponse(config);
    } catch (IOException e) {
      throw new IllegalStateException("Could not apply Hermes backend configuration: " + e.getMessage(), e);
    } finally {
      updateLock.unlock();
    }
  }

  private HermesFallbackSettingsResponse fallbackResponse(HermesFallbackUpdateRequest settings) {
    List<HermesFallbackProfileStatus> statuses = properties.profiles().stream()
        .map(profileId -> {
          CredentialStatus credential = credentialStatus(profileId);
          if (Boolean.TRUE.equals(settings.enabled())) {
            try {
              validateToolCall(validatedBaseUrl(settings.baseUrl()), settings.model());
              return new HermesFallbackProfileStatus(
                  profileId,
                  "OLLAMA_FORCED",
                  true,
                  credential.openAiAvailable(),
                  credential.cooldownUntil(),
                  firstNonBlank(credential.detail(), "Shared Ollama override active"));
            } catch (Exception e) {
              return new HermesFallbackProfileStatus(
                  profileId,
                  "OLLAMA_FORCED",
                  false,
                  credential.openAiAvailable(),
                  credential.cooldownUntil(),
                  "Shared Ollama override failed health check");
            }
          }

          ProfileHealth health = openAiProfileHealth(profileId);
          return new HermesFallbackProfileStatus(
              profileId,
              credential.openAiAvailable() ? "OPENAI_PRIMARY" : "OPENAI_PROFILE_UNAVAILABLE",
              Boolean.TRUE.equals(health.healthy()),
              credential.openAiAvailable(),
              credential.cooldownUntil(),
              firstNonBlank(health.detail(), credential.detail()));
        })
        .toList();
    return new HermesFallbackSettingsResponse(
        Boolean.TRUE.equals(settings.enabled()),
        settings.baseUrl(),
        settings.model(),
        statuses);
  }

  private HermesBackendConfigResponse backendResponse(StoredBackendConfig config) {
    return new HermesBackendConfigResponse(config.profiles().stream()
        .map(this::profileResponse)
        .toList());
  }

  private HermesProfile profileResponse(StoredProfile profile) {
    ProfileHealth health = OPENAI_PROVIDER.equals(profile.provider())
        ? openAiProfileHealth(profile.id())
        : ollamaProfileHealth(profile);
    return new HermesProfile(
        profile.id(),
        profile.label(),
        profile.provider(),
        profile.baseUrl(),
        profile.model(),
        profile.apiMode(),
        profile.timeoutSeconds(),
        health.healthy(),
        health.toolCapable(),
        health.detail(),
        profile.contextWindow());
  }

  private ProfileHealth openAiProfileHealth(String profileId) {
    Integer port = properties.ports().get(profileId);
    if (port == null) {
      return new ProfileHealth(false, true, "Hermes profile port is not configured");
    }
    try {
      restTemplate.getForEntity("http://127.0.0.1:" + port + "/health", String.class);
      return new ProfileHealth(true, true, "Hermes profile healthy");
    } catch (Exception e) {
      return new ProfileHealth(false, true, "Hermes profile health check failed");
    }
  }

  private ProfileHealth ollamaProfileHealth(StoredProfile profile) {
    try {
      URI baseUrl = validatedBaseUrl(profile.baseUrl());
      getModels(baseUrl.toString());
      if (requiresTools(profile.id())) {
        validateToolCall(baseUrl, profile.model());
        return new ProfileHealth(true, true, "Ollama backend reachable and tool-capable");
      }
      return new ProfileHealth(true, null, "Ollama backend reachable");
    } catch (Exception e) {
      return new ProfileHealth(false, null, "Ollama backend check failed");
    }
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
      return new CredentialStatus(
          false,
          latestCooldown == null ? null : latestCooldown.toString(),
          "OpenAI quota cooldown active");
    } catch (Exception e) {
      return new CredentialStatus(false, null, "Could not inspect OpenAI credential state");
    }
  }

  private StoredBackendConfig readBackendConfig() {
    Path path = backendSettingsPath();
    if (!Files.exists(path)) {
      return defaultBackendConfig();
    }
    try {
      JsonNode root = jsonMapper.readTree(Files.readAllBytes(path));
      if (root != null && root.has("routes")) {
        return normalizeBackendConfig(migrateLegacyBackendConfig(root));
      }
      StoredBackendConfig config = jsonMapper.treeToValue(root, StoredBackendConfig.class);
      return normalizeBackendConfig(config);
    } catch (IOException e) {
      throw new IllegalStateException("Could not read " + path, e);
    }
  }

  private HermesFallbackUpdateRequest readSettings() {
    Path path = settingsPath();
    if (!Files.exists(path)) {
      return defaultFallbackSettings();
    }
    try {
      return jsonMapper.readValue(Files.readAllBytes(path), HermesFallbackUpdateRequest.class);
    } catch (IOException e) {
      throw new IllegalStateException("Could not read " + path, e);
    }
  }

  private StoredBackendConfig sanitizeBackendConfig(HermesBackendConfigUpdateRequest request) {
    if (request == null || request.profiles() == null) {
      throw new IllegalArgumentException("profiles are required");
    }
    return normalizeBackendConfig(new StoredBackendConfig(request.profiles().stream()
        .map(profile -> new StoredProfile(
            requireValue(profile.id(), "profile id"),
            requireValue(profile.label(), "profile label"),
            normalizeProvider(profile.provider()),
            blankToNull(profile.baseUrl()),
            requireValue(profile.model(), "profile model"),
            firstNonBlank(profile.apiMode(), RESPONSES_API_MODE),
            profile.timeoutSeconds() == null ? DEFAULT_TIMEOUT_SECONDS : profile.timeoutSeconds(),
            profile.contextWindow()))
        .toList()));
  }

  private StoredBackendConfig normalizeBackendConfig(StoredBackendConfig config) {
    Map<String, StoredProfile> merged = new LinkedHashMap<>();
    defaultBackendConfig().profiles().forEach(profile -> merged.put(profile.id(), profile));
    if (config != null && config.profiles() != null) {
      config.profiles().forEach(profile -> merged.put(profile.id(), profile));
    }
    return new StoredBackendConfig(List.copyOf(merged.values()));
  }

  private StoredBackendConfig migrateLegacyBackendConfig(JsonNode root) {
    Map<String, JsonNode> profileMap = new LinkedHashMap<>();
    JsonNode profilesNode = root.path("profiles");
    if (profilesNode.isArray()) {
      for (JsonNode profileNode : profilesNode) {
        String id = text(profileNode, "id");
        if (id != null) {
          profileMap.put(id, profileNode);
        }
      }
    }
    Map<String, String> routeTargets = new LinkedHashMap<>();
    JsonNode routesNode = root.path("routes");
    if (routesNode.isArray()) {
      for (JsonNode routeNode : routesNode) {
        String routeId = text(routeNode, "routeId");
        String backendProfileId = text(routeNode, "backendProfileId");
        if (routeId != null && backendProfileId != null) {
          routeTargets.put(routeId, backendProfileId);
        }
      }
    }

    List<StoredProfile> profiles = new ArrayList<>();
    for (StoredProfile defaults : defaultBackendConfig().profiles()) {
      String targetId = routeTargets.getOrDefault(defaults.id(), defaults.id());
      JsonNode targetNode = profileMap.get(targetId);
      if (targetNode == null) {
        profiles.add(defaults);
        continue;
      }
      String type = text(targetNode, "type");
      if ("OPENAI_COMPATIBLE".equalsIgnoreCase(type)) {
        profiles.add(new StoredProfile(
            defaults.id(),
            defaults.label(),
            OLLAMA_PROVIDER,
            text(targetNode, "baseUrl"),
            firstNonBlank(text(targetNode, "model"), properties.defaultModel()),
            firstNonBlank(text(targetNode, "apiMode"), CHAT_COMPLETIONS_API_MODE),
            intValue(targetNode, "timeoutSeconds", DEFAULT_TIMEOUT_SECONDS),
            null));
      } else {
        profiles.add(new StoredProfile(
            defaults.id(),
            defaults.label(),
            OPENAI_PROVIDER,
            null,
            firstNonBlank(text(targetNode, "model"), defaults.model()),
            firstNonBlank(text(targetNode, "apiMode"), defaults.apiMode()),
            intValue(targetNode, "timeoutSeconds", defaults.timeoutSeconds()),
            null));
      }
    }
    return new StoredBackendConfig(profiles);
  }

  private void validateProfiles(StoredBackendConfig config) {
    if (config.profiles().isEmpty()) {
      throw new IllegalArgumentException("Hermes profiles are required");
    }
    for (String profileId : List.of("chat", "coder", "ai-command")) {
      if (profileById(config, profileId) == null) {
        throw new IllegalArgumentException("Missing Hermes profile: " + profileId);
      }
    }
    for (StoredProfile profile : config.profiles()) {
      if (profile.timeoutSeconds() == null || profile.timeoutSeconds() <= 0) {
        throw new IllegalArgumentException("profile " + profile.id() + " timeoutSeconds must be positive");
      }
      if (OPENAI_PROVIDER.equals(profile.provider())) {
        requireValue(profile.model(), "profile model");
      } else if (OLLAMA_PROVIDER.equals(profile.provider())) {
        validatedBaseUrl(profile.baseUrl());
        requireValue(profile.model(), "profile model");
        if (profile.contextWindow() != null && profile.contextWindow() <= 0) {
          throw new IllegalArgumentException("profile " + profile.id() + " contextWindow must be positive");
        }
      } else {
        throw new IllegalArgumentException("Unsupported provider: " + profile.provider());
      }
    }
  }

  private StoredProfile profileById(StoredBackendConfig config, String id) {
    if (config == null || config.profiles() == null) {
      return null;
    }
    return config.profiles().stream()
        .filter(profile -> id.equals(profile.id()))
        .findFirst()
        .orElse(null);
  }

  private boolean requiresTools(String profileId) {
    return "chat".equals(profileId) || "ai-command".equals(profileId);
  }

  private StoredBackendConfig defaultBackendConfig() {
    return new StoredBackendConfig(List.of(
        new StoredProfile("chat", "Hermes chat", OPENAI_PROVIDER, null, "hermes-chat", RESPONSES_API_MODE, DEFAULT_TIMEOUT_SECONDS, null),
        new StoredProfile("coder", "Hermes coder", OPENAI_PROVIDER, null, "hermes-coder", RESPONSES_API_MODE, DEFAULT_TIMEOUT_SECONDS, null),
        new StoredProfile("ai-command", "Hermes AI command", OPENAI_PROVIDER, null, "hermes-ai-command", RESPONSES_API_MODE, DEFAULT_TIMEOUT_SECONDS, null)));
  }

  private HermesFallbackUpdateRequest defaultFallbackSettings() {
    return new HermesFallbackUpdateRequest(properties.defaultBaseUrl(), properties.defaultModel(), false);
  }

  private void writeBackendConfig(StoredBackendConfig config) throws IOException {
    writeAtomically(backendSettingsPath(), jsonMapper.writeValueAsBytes(normalizeBackendConfig(config)));
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
        baseUrl.resolve(path(baseUrl, "chat/completions")),
        new HttpEntity<>(request, headers),
        Map.class);
    if (response == null || !response.toString().contains("tool_calls")) {
      throw new IllegalArgumentException("Selected Ollama model did not produce a tool call");
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

  private String normalizeProvider(String provider) {
    String normalized = requireValue(provider, "provider").toLowerCase();
    if (!OPENAI_PROVIDER.equals(normalized) && !OLLAMA_PROVIDER.equals(normalized)) {
      throw new IllegalArgumentException("provider must be openai or ollama");
    }
    return normalized;
  }

  private String requireValue(String value, String field) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(field + " is required");
    }
    return value.trim();
  }

  private String blankToNull(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }

  private String trimTrailingSlash(String value) {
    return value == null ? null : value.replaceFirst("/+$", "");
  }

  private String firstNonBlank(String... values) {
    if (values == null) {
      return null;
    }
    for (String value : values) {
      if (value != null && !value.isBlank()) {
        return value.trim();
      }
    }
    return null;
  }

  private String text(JsonNode node, String field) {
    if (node == null || !node.hasNonNull(field)) {
      return null;
    }
    String value = node.get(field).asText();
    return value == null || value.isBlank() ? null : value.trim();
  }

  private Integer intValue(JsonNode node, String field, Integer defaultValue) {
    if (node == null || !node.hasNonNull(field)) {
      return defaultValue;
    }
    return node.get(field).asInt(defaultValue == null ? DEFAULT_TIMEOUT_SECONDS : defaultValue);
  }

  private String path(URI baseUrl, String suffix) {
    String path = baseUrl.getPath();
    return (path == null || path.isBlank() ? "/" : path) + suffix;
  }

  private Path settingsPath() {
    return properties.dataDir().resolve(SETTINGS_FILE);
  }

  private Path backendSettingsPath() {
    return properties.dataDir().resolve(BACKEND_SETTINGS_FILE);
  }

  private void writeAtomically(Path path, byte[] bytes) throws IOException {
    Files.createDirectories(path.getParent());
    Path temporary = Files.createTempFile(path.getParent(), path.getFileName().toString(), ".tmp");
    Files.write(temporary, bytes);
    try {
      Files.move(temporary, path, java.nio.file.StandardCopyOption.ATOMIC_MOVE, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
    } catch (java.nio.file.AtomicMoveNotSupportedException e) {
      Files.move(temporary, path, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
    }
  }

  private record CredentialStatus(boolean openAiAvailable, String cooldownUntil, String detail) {
  }

  private record ProfileHealth(Boolean healthy, Boolean toolCapable, String detail) {
  }

  private record StoredBackendConfig(List<StoredProfile> profiles) {
  }

  private record StoredProfile(
      String id,
      String label,
      String provider,
      String baseUrl,
      String model,
      String apiMode,
      Integer timeoutSeconds,
      Integer contextWindow) {
  }
}
