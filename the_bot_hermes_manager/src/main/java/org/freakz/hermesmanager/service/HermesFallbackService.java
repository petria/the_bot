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
import org.freakz.common.model.engine.system.HermesAiRoute;
import org.freakz.common.model.engine.system.HermesBackendConfigResponse;
import org.freakz.common.model.engine.system.HermesBackendConfigUpdateRequest;
import org.freakz.common.model.engine.system.HermesBackendProfile;
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
  private static final String BACKEND_SETTINGS_FILE = "the_bot_hermes_backends.json";
  private static final String OLLAMA_PROFILE_ID = "ollama-default";
  private static final String CHAT_COMPLETIONS_API_MODE = "chat-completions";
  private static final String RESPONSES_API_MODE = "responses";
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
    return fallbackResponse(readBackendConfig());
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
      writeBackendConfig(backendConfigForFallback(saved));
      return fallbackResponse(readBackendConfig());
    } catch (Exception e) {
      rollback(backups);
      throw new IllegalStateException("Could not apply Hermes fallback configuration: " + e.getMessage(), e);
    } finally {
      updateLock.unlock();
    }
  }

  public HermesBackendConfigResponse getBackendConfig() {
    return backendResponse(readBackendConfig());
  }

  public HermesBackendConfigResponse updateBackendConfig(HermesBackendConfigUpdateRequest request) {
    StoredBackendConfig config = sanitizeBackendConfig(request);
    validateRoutes(config);
    updateLock.lock();
    try {
      for (StoredRoute route : config.routes()) {
        if (requiresTools(route.routeId())) {
          StoredProfile profile = profileById(config, route.backendProfileId());
          if (profile != null && "OPENAI_COMPATIBLE".equalsIgnoreCase(profile.type())) {
            validateToolCall(validatedBaseUrl(profile.baseUrl()), profile.model());
          }
        }
      }
      StoredProfile fallback = firstOpenAiCompatible(config);
      if (fallback != null) {
        applyFallbackProvider(fallback);
      }
      writeBackendConfig(config);
      writeAtomically(settingsPath(), jsonMapper.writeValueAsBytes(fallbackRequest(config)));
      return backendResponse(config);
    } catch (Exception e) {
      throw new IllegalStateException("Could not apply Hermes backend configuration: " + e.getMessage(), e);
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

  private HermesFallbackSettingsResponse fallbackResponse(StoredBackendConfig config) {
    HermesFallbackUpdateRequest settings = fallbackRequest(config);
    List<HermesFallbackProfileStatus> statuses = properties.profiles().stream()
        .map(profile -> {
          int port = properties.ports().getOrDefault(profile, 0);
          CredentialStatus credential = credentialStatus(profile);
          try {
            restTemplate.getForEntity("http://127.0.0.1:" + port + "/health", String.class);
            boolean forced = routeUsesProfile(config, profile, OLLAMA_PROFILE_ID);
            String route = forced ? "OLLAMA_FORCED"
                : credential.openAiAvailable() ? "OPENAI_PRIMARY" : "OLLAMA_FALLBACK";
            return new HermesFallbackProfileStatus(
                profile,
                route,
                true,
                credential.openAiAvailable(),
                credential.cooldownUntil(),
                forced
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

  private HermesBackendConfigResponse backendResponse(StoredBackendConfig config) {
    List<HermesBackendProfile> profiles = config.profiles().stream()
        .map(profile -> profileResponse(profile))
        .toList();
    List<HermesAiRoute> routes = config.routes().stream()
        .map(route -> routeResponse(config, route))
        .toList();
    return new HermesBackendConfigResponse(profiles, routes);
  }

  private HermesBackendProfile profileResponse(StoredProfile profile) {
    ProfileHealth health = profileHealth(profile);
    return new HermesBackendProfile(
        profile.id(),
        profile.label(),
        profile.type(),
        profile.baseUrl(),
        profile.model(),
        profile.apiMode(),
        profile.timeoutSeconds(),
        healthUrl(profile.baseUrl()),
        health.healthy(),
        health.toolCapable(),
        health.detail());
  }

  private HermesAiRoute routeResponse(StoredBackendConfig config, StoredRoute route) {
    StoredProfile profile = profileById(config, route.backendProfileId());
    if (profile == null) {
      return new HermesAiRoute(route.routeId(), routeLabel(route.routeId()), route.backendProfileId(),
          null, null, null, route.timeoutSeconds(), null, false, "Backend profile is missing");
    }
    ProfileHealth health = profileHealth(profile);
    return new HermesAiRoute(
        route.routeId(),
        routeLabel(route.routeId()),
        route.backendProfileId(),
        profile.baseUrl(),
        profile.model(),
        profile.apiMode(),
        route.timeoutSeconds() == null ? profile.timeoutSeconds() : route.timeoutSeconds(),
        healthUrl(profile.baseUrl()),
        health.healthy(),
        health.detail());
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

  private StoredBackendConfig readBackendConfig() {
    Path path = backendSettingsPath();
    if (Files.exists(path)) {
      try {
        return normalizeBackendConfig(jsonMapper.readValue(Files.readAllBytes(path), StoredBackendConfig.class));
      } catch (IOException e) {
        throw new IllegalStateException("Could not read " + path, e);
      }
    }
    return backendConfigForFallback(readSettings());
  }

  private void writeBackendConfig(StoredBackendConfig config) throws IOException {
    writeAtomically(backendSettingsPath(), jsonMapper.writeValueAsBytes(normalizeBackendConfig(config)));
  }

  private StoredBackendConfig backendConfigForFallback(HermesFallbackUpdateRequest settings) {
    String baseUrl = firstNonBlank(settings == null ? null : settings.baseUrl(), properties.defaultBaseUrl());
    String model = firstNonBlank(settings == null ? null : settings.model(), properties.defaultModel());
    boolean enabled = settings != null && Boolean.TRUE.equals(settings.enabled());
    List<StoredProfile> profiles = new ArrayList<>();
    profiles.add(profile("chat", "Hermes chat", "HERMES_PROFILE", "http://ubuntu-server.local:8643", "hermes-chat", RESPONSES_API_MODE, 120));
    profiles.add(profile("coder", "Hermes coder", "HERMES_PROFILE", "http://ubuntu-server.local:8644", "hermes-coder", RESPONSES_API_MODE, 120));
    profiles.add(profile("ai-command", "Hermes AI command", "HERMES_PROFILE", "http://ubuntu-server.local:8645", "hermes-ai-command", RESPONSES_API_MODE, 120));
    profiles.add(profile(OLLAMA_PROFILE_ID, "Ollama default", "OPENAI_COMPATIBLE", baseUrl, model, CHAT_COMPLETIONS_API_MODE, 120));
    return normalizeBackendConfig(new StoredBackendConfig(profiles, defaultRoutes(enabled)));
  }

  private StoredBackendConfig sanitizeBackendConfig(HermesBackendConfigUpdateRequest request) {
    if (request == null) {
      throw new IllegalArgumentException("request is required");
    }
    List<StoredProfile> profiles = request.profiles() == null ? List.of() : request.profiles().stream()
        .map(profile -> profile(
            requireValue(profile.id(), "profile id"),
            requireValue(profile.label(), "profile label"),
            requireValue(profile.type(), "profile type"),
            requireValue(profile.baseUrl(), "profile baseUrl"),
            requireValue(profile.model(), "profile model"),
            firstNonBlank(profile.apiMode(), RESPONSES_API_MODE),
            profile.timeoutSeconds() == null ? 120 : profile.timeoutSeconds()))
        .toList();
    List<StoredRoute> routes = request.routes() == null ? List.of() : request.routes().stream()
        .map(route -> new StoredRoute(
            requireValue(route.routeId(), "route id"),
            requireValue(route.backendProfileId(), "route backendProfileId"),
            route.timeoutSeconds()))
        .toList();
    return normalizeBackendConfig(new StoredBackendConfig(profiles, routes));
  }

  private StoredBackendConfig normalizeBackendConfig(StoredBackendConfig config) {
    StoredBackendConfig defaults = backendConfigForDefaultsOnly();
    List<StoredProfile> profiles = new ArrayList<>();
    if (config != null && config.profiles() != null) {
      profiles.addAll(config.profiles());
    }
    for (StoredProfile profile : defaults.profiles()) {
      if (profiles.stream().noneMatch(existing -> profile.id().equals(existing.id()))) {
        profiles.add(profile);
      }
    }
    List<StoredRoute> routes = new ArrayList<>();
    if (config != null && config.routes() != null) {
      routes.addAll(config.routes());
    }
    for (StoredRoute route : defaults.routes()) {
      if (routes.stream().noneMatch(existing -> route.routeId().equals(existing.routeId()))) {
        routes.add(route);
      }
    }
    return new StoredBackendConfig(profiles, routes);
  }

  private StoredBackendConfig backendConfigForDefaultsOnly() {
    List<StoredProfile> profiles = List.of(
        profile("chat", "Hermes chat", "HERMES_PROFILE", "http://ubuntu-server.local:8643", "hermes-chat", RESPONSES_API_MODE, 120),
        profile("coder", "Hermes coder", "HERMES_PROFILE", "http://ubuntu-server.local:8644", "hermes-coder", RESPONSES_API_MODE, 120),
        profile("ai-command", "Hermes AI command", "HERMES_PROFILE", "http://ubuntu-server.local:8645", "hermes-ai-command", RESPONSES_API_MODE, 120),
        profile(OLLAMA_PROFILE_ID, "Ollama default", "OPENAI_COMPATIBLE", properties.defaultBaseUrl(), properties.defaultModel(), CHAT_COMPLETIONS_API_MODE, 120));
    return new StoredBackendConfig(profiles, defaultRoutes(false));
  }

  private void validateRoutes(StoredBackendConfig config) {
    if (config.profiles().isEmpty()) {
      throw new IllegalArgumentException("At least one backend profile is required");
    }
    for (StoredRoute route : config.routes()) {
      if (profileById(config, route.backendProfileId()) == null) {
        throw new IllegalArgumentException("Route " + route.routeId() + " references missing profile " + route.backendProfileId());
      }
    }
  }

  private void applyFallbackProvider(StoredProfile profile) throws IOException {
    Map<Path, byte[]> backups = new LinkedHashMap<>();
    try {
      for (String hermesProfile : properties.profiles()) {
        Path config = findProfileConfig(hermesProfile);
        backups.put(config, Files.readAllBytes(config));
        updateYaml(config, validatedBaseUrl(profile.baseUrl()).toString().replaceFirst("/+$", ""), profile.model());
      }
      restartAndVerify();
    } catch (Exception e) {
      rollback(backups);
      throw e;
    }
  }

  private ProfileHealth profileHealth(StoredProfile profile) {
    if ("HERMES_PROFILE".equalsIgnoreCase(profile.type())) {
      Integer port = properties.ports().get(profile.id());
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
    try {
      getModels(profile.baseUrl());
      return new ProfileHealth(true, null, "OpenAI-compatible backend reachable");
    } catch (Exception e) {
      return new ProfileHealth(false, null, "OpenAI-compatible backend check failed");
    }
  }

  private HermesFallbackUpdateRequest fallbackRequest(StoredBackendConfig config) {
    StoredProfile fallback = profileById(config, OLLAMA_PROFILE_ID);
    if (fallback == null) {
      fallback = firstOpenAiCompatible(config);
    }
    StoredProfile effectiveFallback = fallback;
    boolean enabled = config.routes().stream()
        .filter(route -> properties.profiles().contains(route.routeId()))
        .allMatch(route -> effectiveFallback != null && effectiveFallback.id().equals(route.backendProfileId()));
    return new HermesFallbackUpdateRequest(
        fallback == null ? properties.defaultBaseUrl() : fallback.baseUrl(),
        fallback == null ? properties.defaultModel() : fallback.model(),
        enabled);
  }

  private List<StoredRoute> defaultRoutes(boolean ollamaForced) {
    String target = ollamaForced ? OLLAMA_PROFILE_ID : null;
    return List.of(
        new StoredRoute("chat", target == null ? "chat" : target, 120),
        new StoredRoute("coder", target == null ? "coder" : target, 120),
        new StoredRoute("ai-command", target == null ? "ai-command" : target, 120));
  }

  private StoredProfile firstOpenAiCompatible(StoredBackendConfig config) {
    return config.profiles().stream()
        .filter(profile -> "OPENAI_COMPATIBLE".equalsIgnoreCase(profile.type()))
        .findFirst()
        .orElse(null);
  }

  private StoredProfile profileById(StoredBackendConfig config, String id) {
    if (config == null || config.profiles() == null || id == null) {
      return null;
    }
    return config.profiles().stream()
        .filter(profile -> id.equals(profile.id()))
        .findFirst()
        .orElse(null);
  }

  private boolean routeUsesProfile(StoredBackendConfig config, String routeId, String profileId) {
    return config.routes().stream()
        .anyMatch(route -> routeId.equals(route.routeId()) && profileId.equals(route.backendProfileId()));
  }

  private boolean requiresTools(String routeId) {
    return "ai-command".equals(routeId) || "chat".equals(routeId);
  }

  private String routeLabel(String routeId) {
    return switch (routeId) {
      case "chat" -> "Chat";
      case "coder" -> "Coder";
      case "ai-command" -> "AI command";
      default -> routeId;
    };
  }

  private StoredProfile profile(String id, String label, String type, String baseUrl, String model, String apiMode, Integer timeoutSeconds) {
    return new StoredProfile(id, label, type, baseUrl == null ? null : baseUrl.replaceFirst("/+$", ""), model, apiMode, timeoutSeconds);
  }

  private String healthUrl(String baseUrl) {
    return baseUrl == null || baseUrl.isBlank() ? null : baseUrl.replaceFirst("/+$", "") + "/health";
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

  private Path backendSettingsPath() {
    return properties.dataDir().resolve(BACKEND_SETTINGS_FILE);
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

  private record ProfileHealth(Boolean healthy, Boolean toolCapable, String detail) {
  }

  private record StoredBackendConfig(List<StoredProfile> profiles, List<StoredRoute> routes) {
  }

  private record StoredProfile(
      String id,
      String label,
      String type,
      String baseUrl,
      String model,
      String apiMode,
      Integer timeoutSeconds) {
  }

  private record StoredRoute(String routeId, String backendProfileId, Integer timeoutSeconds) {
  }
}
