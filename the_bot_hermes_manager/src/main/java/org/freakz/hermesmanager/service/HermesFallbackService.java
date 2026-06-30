package org.freakz.hermesmanager.service;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFilePermission;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantLock;

import org.freakz.common.model.engine.system.HermesBackend;
import org.freakz.common.model.engine.system.HermesBackendConfigResponse;
import org.freakz.common.model.engine.system.HermesBackendConfigUpdateRequest;
import org.freakz.common.model.engine.system.HermesBackendUpdate;
import org.freakz.common.model.engine.system.HermesFallbackModel;
import org.freakz.common.model.engine.system.HermesFallbackModelsResponse;
import org.freakz.common.model.engine.system.HermesFallbackProfileStatus;
import org.freakz.common.model.engine.system.HermesFallbackSettingsResponse;
import org.freakz.common.model.engine.system.HermesFallbackUpdateRequest;
import org.freakz.common.model.engine.system.HermesGlobalOverrideSettings;
import org.freakz.common.model.engine.system.HermesModelDiscoveryRequest;
import org.freakz.common.model.engine.system.HermesProfile;
import org.freakz.common.model.engine.system.HermesRoute;
import org.freakz.common.model.engine.system.HermesRouteUpdate;
import org.freakz.hermesmanager.config.HermesManagerProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.dataformat.yaml.YAMLMapper;

@Service
public class HermesFallbackService implements ApplicationRunner {

  private static final Logger log = LoggerFactory.getLogger(HermesFallbackService.class);
  private static final String CONFIG_FILE = "the_bot_hermes_backends.json";
  private static final String LEGACY_FALLBACK_FILE = "the_bot_fallback.json";
  private static final String OPENAI = "openai";
  private static final String LOCAL = "local";
  private static final String AI_COMMAND = "ai-command";
  private static final String MODE_ENABLED = "enabled";
  private static final String MODE_OFF = "off";
  private static final String RESPONSES = "responses";
  private static final int DEFAULT_TIMEOUT_SECONDS = 120;
  private static final int MIN_CONTEXT_WINDOW = 65536;

  private final ReentrantLock updateLock = new ReentrantLock();
  private final HermesManagerProperties properties;
  private final HermesGatewayService gatewayService;
  private final RestTemplate restTemplate;
  private final LocalLlmClient localLlmClient;
  private final LocalCredentialCipher credentialCipher;
  private final JsonMapper jsonMapper;
  private final YAMLMapper yamlMapper;

  public HermesFallbackService(
      HermesManagerProperties properties,
      HermesGatewayService gatewayService,
      RestTemplate restTemplate,
      LocalLlmClient localLlmClient,
      LocalCredentialCipher credentialCipher) {
    this.properties = properties;
    this.gatewayService = gatewayService;
    this.restTemplate = restTemplate;
    this.localLlmClient = localLlmClient;
    this.credentialCipher = credentialCipher;
    this.jsonMapper = JsonMapper.builder().build();
    this.yamlMapper = YAMLMapper.builder().build();
  }

  @Override
  public void run(ApplicationArguments args) {
    try {
      StoredConfig config = readConfig();
      applyHermesConfiguration(config);
      writeConfig(config);
    } catch (Exception e) {
      log.warn("Could not reconcile Hermes backend configuration during startup: {}", e.getMessage());
    }
  }

  public HermesFallbackSettingsResponse getSettings() {
    StoredConfig config = readConfig();
    StoredBackend local = backendById(config, LOCAL);
    BackendHealth health = backendHealth(local);
    return new HermesFallbackSettingsResponse(
        false,
        local.provider(),
        local.baseUrl(),
        local.model(),
        config.routes().stream()
            .map(route -> new HermesFallbackProfileStatus(
                route.id(),
                route.backendId().toUpperCase() + "_ROUTE",
                health.healthy(),
                openAiCredentialAvailable(),
                null,
                health.detail(),
                route.backendId(),
                null,
                null,
                null,
                null))
            .toList(),
        local.contextWindow(),
        health.healthy(),
        health.toolCapable(),
        "Compatibility view; fallback routing has been removed",
        local.lastValidatedAt(),
        local.validationStatus(),
        hasCredential(local.encryptedApiKey()));
  }

  public HermesFallbackSettingsResponse update(HermesFallbackUpdateRequest request) {
    StoredConfig config = readConfig();
    StoredBackend current = backendById(config, LOCAL);
    StoredBackend updated = new StoredBackend(
        LOCAL,
        "Local LLM backend",
        normalizeLocalProvider(firstNonBlank(request == null ? null : request.provider(), current.provider())),
        trimTrailingSlash(firstNonBlank(request == null ? null : request.baseUrl(), current.baseUrl())),
        firstNonBlank(request == null ? null : request.model(), current.model()),
        firstNonBlank(current.apiMode(), RESPONSES),
        current.timeoutSeconds(),
        normalizeContextWindow(request == null || request.contextWindow() == null ? current.contextWindow() : request.contextWindow()),
        current.lastValidatedAt(),
        current.validationStatus(),
        current.toolCapable(),
        current.detail(),
        updatedCredential(
            current.encryptedApiKey(),
            request == null ? null : request.apiKey(),
            request != null && Boolean.TRUE.equals(request.clearApiKey())));
    StoredConfig saved = new StoredConfig(config.systemMode(), replaceBackend(config.backends(), updated), config.routes());
    updateLock.lock();
    try {
      validateBackend(updated);
      applyHermesConfiguration(saved);
      writeConfig(saved);
      return getSettings();
    } catch (Exception e) {
      throw new IllegalStateException("Could not update local Hermes backend: " + e.getMessage(), e);
    } finally {
      updateLock.unlock();
    }
  }

  public HermesFallbackModelsResponse getModels(HermesModelDiscoveryRequest request) {
    String provider = normalizeProvider(request == null ? null : request.provider());
    if (OPENAI.equals(provider)) {
      StoredBackend openAi = backendById(readConfig(), OPENAI);
      List<String> models = List.of(openAi.model(), "gpt-5.5").stream()
          .filter(value -> value != null && !value.isBlank())
          .distinct()
          .sorted(String.CASE_INSENSITIVE_ORDER)
          .toList();
      return new HermesFallbackModelsResponse(models, models.stream()
          .map(model -> new HermesFallbackModel(
              model,
              "tool-capable",
              "OpenAI/Codex model",
              true,
              "Configured OpenAI backend model"))
          .toList());
    }
    URI uri = validatedBaseUrl(request == null ? null : request.baseUrl());
    return localLlmClient.discover(provider, uri, discoveryApiKey(request));
  }

  public HermesBackendConfigResponse getBackendConfig() {
    return response(readConfig());
  }

  public HermesBackendConfigResponse updateBackendConfig(HermesBackendConfigUpdateRequest request) {
    StoredConfig config = sanitizeConfig(request);
    updateLock.lock();
    try {
      validateConfig(config);
      applyHermesConfiguration(config);
      writeConfig(config);
      return response(config);
    } catch (HermesValidationException e) {
      throw e;
    } catch (Exception e) {
      throw new IllegalStateException("Could not apply Hermes backend configuration: " + e.getMessage(), e);
    } finally {
      updateLock.unlock();
    }
  }

  private HermesBackendConfigResponse response(StoredConfig config) {
    List<HermesBackend> backends = config.backends().stream()
        .map(backend -> {
          BackendHealth health = backendHealth(backend);
          return new HermesBackend(
              backend.id(),
              backend.label(),
              backend.provider(),
              backend.baseUrl(),
              backend.model(),
              backend.apiMode(),
              backend.timeoutSeconds(),
              backend.contextWindow(),
              health.healthy(),
              health.toolCapable(),
              firstNonBlank(health.detail(), backend.detail()),
              backend.lastValidatedAt(),
              backend.validationStatus(),
              hasCredential(backend.encryptedApiKey()));
        })
        .toList();
    List<HermesRoute> routes = config.routes().stream()
        .map(route -> routeResponse(route, backendById(config, route.backendId())))
        .toList();
    return new HermesBackendConfigResponse(
        config.systemMode(),
        backends,
        routes,
        legacyProfiles(routes),
        null,
        new HermesGlobalOverrideSettings(
            MODE_OFF.equals(config.systemMode()),
            null,
            null,
            null,
            null,
            null,
            MODE_OFF.equals(config.systemMode()) ? "AI system is off" : "AI system uses configured routes",
            null,
            null,
            MODE_OFF.equals(config.systemMode()) ? "OFF" : "ENABLED",
            null));
  }

  private HermesRoute routeResponse(StoredRoute route, StoredBackend backend) {
    BackendHealth health = backendHealth(backend);
    return new HermesRoute(
        route.id(),
        route.label(),
        route.backendId(),
        backend.provider(),
        routeGatewayBaseUrl(route.id()),
        routeGatewayModelAlias(route.id()),
        routeGatewayApiMode(route.id(), backend.apiMode()),
        backend.timeoutSeconds(),
        backend.contextWindow(),
        health.healthy(),
        health.toolCapable(),
        firstNonBlank(health.detail(), "Route uses " + backend.label()));
  }

  private List<HermesProfile> legacyProfiles(List<HermesRoute> routes) {
    return routes.stream()
        .map(route -> new HermesProfile(
            route.id(),
            route.label(),
            route.provider(),
            route.baseUrl(),
            route.model(),
            route.apiMode(),
            route.timeoutSeconds(),
            route.healthy(),
            route.toolCapable(),
            route.detail(),
            route.contextWindow(),
            false,
            route.provider(),
            route.healthy(),
            route.healthy(),
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            false))
        .toList();
  }

  private StoredConfig sanitizeConfig(HermesBackendConfigUpdateRequest request) {
    StoredConfig current = readConfig();
    if (request == null) {
      return current;
    }
    String mode = normalizeMode(firstNonBlank(request.systemMode(), current.systemMode()));
    List<StoredBackend> backends = request.backends() == null || request.backends().isEmpty()
        ? current.backends()
        : request.backends().stream()
            .map(update -> sanitizeBackend(update, backendById(current, update.id())))
            .toList();
    List<StoredRoute> routes = request.routes() == null || request.routes().isEmpty()
        ? current.routes()
        : request.routes().stream()
            .map(update -> new StoredRoute(
                requireValue(update.id(), "route id"),
                firstNonBlank(update.label(), defaultRouteLabel(update.id())),
                normalizeBackendId(update.backendId())))
            .toList();
    return normalizeConfig(new StoredConfig(mode, backends, routes));
  }

  private StoredBackend sanitizeBackend(HermesBackendUpdate update, StoredBackend existing) {
    if (update == null) {
      throw new IllegalArgumentException("backend is required");
    }
    String id = normalizeBackendId(update.id());
    StoredBackend defaults = existing == null ? defaultBackend(id) : existing;
    String provider = OPENAI.equals(id)
        ? OPENAI
        : normalizeLocalProvider(firstNonBlank(update.provider(), defaults.provider()));
    return new StoredBackend(
        id,
        firstNonBlank(update.label(), defaults.label()),
        provider,
        OPENAI.equals(id) ? null : trimTrailingSlash(firstNonBlank(update.baseUrl(), defaults.baseUrl())),
        requireValue(firstNonBlank(update.model(), defaults.model()), id + " model"),
        firstNonBlank(update.apiMode(), defaults.apiMode(), RESPONSES),
        update.timeoutSeconds() == null ? defaults.timeoutSeconds() : update.timeoutSeconds(),
        OPENAI.equals(id)
            ? null
            : normalizeContextWindow(update.contextWindow() == null ? defaults.contextWindow() : update.contextWindow()),
        defaults.lastValidatedAt(),
        defaults.validationStatus(),
        defaults.toolCapable(),
        defaults.detail(),
        updatedCredential(
            defaults.encryptedApiKey(),
            update.apiKey(),
            Boolean.TRUE.equals(update.clearApiKey())));
  }

  private void validateConfig(StoredConfig config) {
    if (!MODE_ENABLED.equals(config.systemMode()) && !MODE_OFF.equals(config.systemMode())) {
      throw new IllegalArgumentException("systemMode must be enabled or off");
    }
    validateBackend(backendById(config, OPENAI));
    validateBackend(backendById(config, LOCAL));
    for (StoredRoute route : config.routes()) {
      backendById(config, route.backendId());
    }
  }

  private void validateBackend(StoredBackend backend) {
    if (backend.timeoutSeconds() == null || backend.timeoutSeconds() <= 0) {
      throw new IllegalArgumentException("backend " + backend.id() + " timeoutSeconds must be positive");
    }
    if (OPENAI.equals(backend.id())) {
      requireValue(backend.model(), "OpenAI model");
      return;
    }
    URI baseUrl = validatedBaseUrl(backend.baseUrl());
    requireValue(backend.model(), "local model");
    validateContextWindow(backend.contextWindow(), "local backend");
    String apiKey = decryptCredential(backend.encryptedApiKey());
    try {
      localLlmClient.discover(backend.provider(), baseUrl, apiKey);
      localLlmClient.validateToolCall(baseUrl, backend.model(), apiKey);
      localLlmClient.validateChat(baseUrl, backend.model(), apiKey);
    } catch (HermesValidationException e) {
      throw new HermesValidationException(
          "Hermes local backend validation failed",
          "backend=local, provider=" + backend.provider()
              + ", baseUrl=" + trimTrailingSlash(backend.baseUrl())
              + ", model=" + backend.model()
              + ", detail=" + e.getDetail(),
          e);
    }
  }

  private BackendHealth backendHealth(StoredBackend backend) {
    if (OPENAI.equals(backend.id())) {
      CredentialStatus credential = openAiCredentialStatus();
      boolean healthy = credential.openAiAvailable();
      return new BackendHealth(
          healthy,
          true,
          healthy ? "OpenAI backend available" : firstNonBlank(credential.detail(), "OpenAI backend unavailable"));
    }
    try {
      URI baseUrl = validatedBaseUrl(backend.baseUrl());
      HermesFallbackModelsResponse models = localLlmClient.discover(
          backend.provider(),
          baseUrl,
          decryptCredential(backend.encryptedApiKey()));
      Boolean toolCapable = models.items().stream()
          .filter(model -> backend.model().equals(model.id()))
          .map(HermesFallbackModel::toolCapable)
          .findFirst()
          .orElse(backend.toolCapable());
      return new BackendHealth(true, toolCapable, LocalLlmClient.displayName(backend.provider()) + " backend reachable");
    } catch (Exception e) {
      return new BackendHealth(false, null, LocalLlmClient.displayName(backend.provider()) + " backend check failed");
    }
  }

  private boolean openAiCredentialAvailable() {
    return openAiCredentialStatus().openAiAvailable();
  }

  private CredentialStatus openAiCredentialStatus() {
    List<String> profiles = List.of("chat", AI_COMMAND);
    String detail = null;
    for (String profile : profiles) {
      CredentialStatus status = credentialStatus(profile);
      if (status.openAiAvailable()) {
        return status;
      }
      detail = firstNonBlank(detail, status.detail());
    }
    return new CredentialStatus(false, firstNonBlank(detail, "OpenAI credentials are missing"));
  }

  private CredentialStatus credentialStatus(String profile) {
    Path authPath = properties.dataDir().resolve("profiles").resolve(profile).resolve("auth.json");
    if (!Files.exists(authPath)) {
      return new CredentialStatus(false, "OpenAI credentials are missing");
    }
    try {
      Map<String, Object> auth = jsonMapper.readValue(Files.readAllBytes(authPath), new TypeReference<>() {});
      Object poolValue = auth.get("credential_pool");
      if (!(poolValue instanceof Map<?, ?> pool)
          || !(pool.get("openai-codex") instanceof List<?> credentials)
          || credentials.isEmpty()) {
        return new CredentialStatus(false, "OpenAI credential pool is empty");
      }
      for (Object value : credentials) {
        if (value instanceof Map<?, ?> credential) {
          Object accessToken = credential.get("access_token");
          Object refreshToken = credential.get("refresh_token");
          if ((accessToken != null && !accessToken.toString().isBlank())
              || (refreshToken != null && !refreshToken.toString().isBlank())) {
            return new CredentialStatus(true, "OpenAI credential available");
          }
        }
      }
      return new CredentialStatus(false, "OpenAI access token is missing");
    } catch (Exception e) {
      return new CredentialStatus(false, "Could not inspect OpenAI credential state");
    }
  }

  private StoredConfig readConfig() {
    Path path = configPath();
    if (!Files.exists(path)) {
      return defaultConfig();
    }
    try {
      JsonNode root = jsonMapper.readTree(Files.readAllBytes(path));
      if (root == null) {
        return defaultConfig();
      }
      if (root.has("backends") && root.has("routes")) {
        return normalizeConfig(jsonMapper.treeToValue(root, StoredConfig.class));
      }
      return migrateLegacyConfig(root);
    } catch (IOException e) {
      throw new IllegalStateException("Could not read " + path, e);
    }
  }

  private StoredConfig migrateLegacyConfig(JsonNode root) {
    Map<String, JsonNode> profiles = new LinkedHashMap<>();
    JsonNode profilesNode = root.path("profiles");
    if (profilesNode.isArray()) {
      for (JsonNode profile : profilesNode) {
        String id = text(profile, "id");
        if (id != null) {
          profiles.put(id, profile);
        }
      }
    }
    JsonNode legacyChat = profiles.get("chat");
    JsonNode legacyAiCommand = profiles.get("ai-command");
    JsonNode localSource = firstLocalProfile(legacyChat, legacyAiCommand);
    StoredBackend openAi = new StoredBackend(
        OPENAI,
        "OpenAI backend",
        OPENAI,
        null,
        firstNonBlank(openAiModel(legacyChat), openAiModel(legacyAiCommand), upstreamModel(OPENAI, "gpt-5.5")),
        firstNonBlank(text(legacyChat, "apiMode"), RESPONSES),
        intValue(legacyChat, "timeoutSeconds", DEFAULT_TIMEOUT_SECONDS),
        null,
        null,
        null,
        true,
        null,
        null);
    StoredBackend local = localFromLegacy(localSource);
    return normalizeConfig(new StoredConfig(
        MODE_ENABLED,
        List.of(openAi, local),
        List.of(
            new StoredRoute("chat", "Hermes chat", isLocal(legacyChat) ? LOCAL : OPENAI),
            new StoredRoute("ai-command", "Hermes AI command", isLocal(legacyAiCommand) ? LOCAL : OPENAI))));
  }

  private StoredBackend localFromLegacy(JsonNode source) {
    LegacyFallback legacyFallback = readLegacyFallback();
    return new StoredBackend(
        LOCAL,
        "Local LLM backend",
        normalizeLocalProvider(firstNonBlank(text(source, "provider"), legacyFallback.provider(), "ollama")),
        trimTrailingSlash(firstNonBlank(text(source, "baseUrl"), legacyFallback.baseUrl(), properties.defaultBaseUrl())),
        firstNonBlank(text(source, "model"), legacyFallback.model(), properties.defaultModel()),
        firstNonBlank(text(source, "apiMode"), RESPONSES),
        intValue(source, "timeoutSeconds", DEFAULT_TIMEOUT_SECONDS),
        normalizeContextWindow(firstNonNull(intValueOrNull(source, "contextWindow"), legacyFallback.contextWindow(), MIN_CONTEXT_WINDOW)),
        legacyFallback.lastValidatedAt(),
        legacyFallback.validationStatus(),
        legacyFallback.toolCapable(),
        legacyFallback.validationDetail(),
        legacyFallback.encryptedApiKey());
  }

  private LegacyFallback readLegacyFallback() {
    Path path = properties.dataDir().resolve(LEGACY_FALLBACK_FILE);
    if (!Files.exists(path)) {
      return new LegacyFallback(null, null, null, null, null, null, null, null, null, null);
    }
    try {
      return jsonMapper.readValue(Files.readAllBytes(path), LegacyFallback.class);
    } catch (Exception e) {
      return new LegacyFallback(null, null, null, null, null, null, null, null, null, null);
    }
  }

  private JsonNode firstLocalProfile(JsonNode... profiles) {
    for (JsonNode profile : profiles) {
      if (isLocal(profile)) {
        return profile;
      }
    }
    return null;
  }

  private boolean isLocal(JsonNode profile) {
    return profile != null && LocalLlmClient.isLocal(text(profile, "provider"));
  }

  private String openAiModel(JsonNode profile) {
    if (profile == null || !OPENAI.equals(text(profile, "provider"))) {
      return null;
    }
    return text(profile, "model");
  }

  private StoredConfig normalizeConfig(StoredConfig config) {
    Map<String, StoredBackend> backends = new LinkedHashMap<>();
    backends.put(OPENAI, defaultBackend(OPENAI));
    backends.put(LOCAL, defaultBackend(LOCAL));
    if (config != null && config.backends() != null) {
      for (StoredBackend backend : config.backends()) {
        String id = normalizeBackendId(backend.id());
        StoredBackend defaults = backends.get(id);
        String provider = OPENAI.equals(id) ? OPENAI : normalizeLocalProvider(firstNonBlank(backend.provider(), defaults.provider()));
        backends.put(id, new StoredBackend(
            id,
            firstNonBlank(backend.label(), defaults.label()),
            provider,
            OPENAI.equals(id) ? null : firstNonBlank(backend.baseUrl(), defaults.baseUrl()),
            firstNonBlank(backend.model(), defaults.model()),
            firstNonBlank(backend.apiMode(), defaults.apiMode(), RESPONSES),
            backend.timeoutSeconds() == null ? defaults.timeoutSeconds() : backend.timeoutSeconds(),
            OPENAI.equals(id) ? null : normalizeContextWindow(backend.contextWindow() == null ? defaults.contextWindow() : backend.contextWindow()),
            backend.lastValidatedAt(),
            backend.validationStatus(),
            backend.toolCapable(),
            backend.detail(),
            backend.encryptedApiKey()));
      }
    }
    Map<String, StoredRoute> routes = new LinkedHashMap<>();
    routes.put("chat", new StoredRoute("chat", "Hermes chat", OPENAI));
    routes.put("ai-command", new StoredRoute("ai-command", "Hermes AI command", OPENAI));
    if (config != null && config.routes() != null) {
      for (StoredRoute route : config.routes()) {
        if ("chat".equals(route.id()) || "ai-command".equals(route.id())) {
          routes.put(route.id(), new StoredRoute(
              route.id(),
              firstNonBlank(route.label(), defaultRouteLabel(route.id())),
              normalizeBackendId(route.backendId())));
        }
      }
    }
    return new StoredConfig(
        normalizeMode(config == null ? null : config.systemMode()),
        List.copyOf(backends.values()),
        List.copyOf(routes.values()));
  }

  private StoredConfig defaultConfig() {
    return new StoredConfig(
        MODE_ENABLED,
        List.of(defaultBackend(OPENAI), defaultBackend(LOCAL)),
        List.of(
            new StoredRoute("chat", "Hermes chat", OPENAI),
            new StoredRoute("ai-command", "Hermes AI command", OPENAI)));
  }

  private StoredBackend defaultBackend(String id) {
    if (OPENAI.equals(id)) {
      return new StoredBackend(
          OPENAI,
          "OpenAI backend",
          OPENAI,
          null,
          upstreamModel(OPENAI, "gpt-5.5"),
          RESPONSES,
          DEFAULT_TIMEOUT_SECONDS,
          null,
          null,
          null,
          true,
          null,
          null);
    }
    return new StoredBackend(
        LOCAL,
        "Local LLM backend",
        "ollama",
        properties.defaultBaseUrl(),
        properties.defaultModel(),
        RESPONSES,
        DEFAULT_TIMEOUT_SECONDS,
        MIN_CONTEXT_WINDOW,
        null,
        "NOT_VALIDATED",
        null,
        "Local backend has not been validated",
        null);
  }

  private void writeConfig(StoredConfig config) throws IOException {
    writeAtomically(configPath(), jsonMapper.writeValueAsBytes(normalizeConfig(config)));
  }

  private void applyHermesConfiguration(StoredConfig config) throws IOException {
    Map<Path, byte[]> backups = new LinkedHashMap<>();
    List<String> changedProfiles = new ArrayList<>();
    try {
      for (StoredRoute route : config.routes()) {
        updateProfileConfig(route.id(), backendById(config, route.backendId()), backups, changedProfiles, true);
      }
      for (String profile : changedProfiles) {
        gatewayService.restart(profile);
        waitForProfileHealth(profile);
      }
    } catch (Exception e) {
      rollbackProfiles(backups);
      throw e;
    }
  }

  private void updateProfileConfig(
      String profile,
      StoredBackend backend,
      Map<Path, byte[]> backups,
      List<String> changedProfiles,
      boolean required) throws IOException {
    Optional<Path> optionalPath = findProfileConfigIfPresent(profile);
    if (optionalPath.isEmpty()) {
      if (required) {
        throw new IllegalStateException("Could not find config YAML for Hermes profile " + profile);
      }
      log.debug("Skipping optional Hermes profile {} because config YAML was not found", profile);
      return;
    }
    Path path = optionalPath.get();
    backups.putIfAbsent(path, Files.readAllBytes(path));
    byte[] updated = updatedProfileYaml(path, backend);
    if (!java.util.Arrays.equals(backups.get(path), updated)) {
      writeAtomically(path, updated);
      changedProfiles.add(profile);
    }
  }

  private byte[] updatedProfileYaml(Path path, StoredBackend backend) throws IOException {
    Map<String, Object> yaml = yamlMapper.readValue(Files.readAllBytes(path), new TypeReference<>() {});
    if (yaml == null) {
      yaml = new LinkedHashMap<>();
    }
    Map<String, Object> model = mutableMap(yaml.get("model"));
    model.put("default", backend.id().equals(OPENAI) ? backend.model() : "hermes-" + backend.id());
    if (OPENAI.equals(backend.id())) {
      model.put("provider", "openai-codex");
      model.put("base_url", "https://chatgpt.com/backend-api/codex");
      model.remove("context_length");
      model.remove("api_key");
    } else {
      model.put("provider", "custom");
      model.put("base_url", trimTrailingSlash(backend.baseUrl()));
      model.put("default", backend.model());
      putOptionalApiKey(model, backend.encryptedApiKey());
      if (backend.contextWindow() != null) {
        model.put("context_length", backend.contextWindow());
      }
    }
    yaml.put("model", model);
    yaml.put("fallback_providers", List.of());
    return yamlMapper.writeValueAsBytes(yaml);
  }

  private Map<String, Object> mutableMap(Object value) {
    Map<String, Object> map = new LinkedHashMap<>();
    if (value instanceof Map<?, ?> values) {
      values.forEach((key, item) -> map.put(String.valueOf(key), item));
    }
    return map;
  }

  private Path findProfileConfig(String profile) throws IOException {
    return findProfileConfigIfPresent(profile)
        .orElseThrow(() -> new IllegalStateException("Could not find config YAML for Hermes profile " + profile));
  }

  private Optional<Path> findProfileConfigIfPresent(String profile) throws IOException {
    try (var paths = Files.walk(properties.dataDir())) {
      return paths.filter(Files::isRegularFile)
          .filter(path -> path.getFileName().toString().matches("config\\.ya?ml"))
          .filter(path -> path.toString().contains("/" + profile + "/"))
          .min(Comparator.comparingInt(Path::getNameCount));
    }
  }

  private String upstreamModel(String profile, String defaultModel) {
    try {
      Path config = findProfileConfig(profile);
      Map<String, Object> yaml = yamlMapper.readValue(Files.readAllBytes(config), new TypeReference<>() {});
      if (yaml != null && yaml.get("model") instanceof Map<?, ?> model) {
        Object value = model.get("default");
        if (value != null && !value.toString().isBlank()) {
          return value.toString();
        }
      }
    } catch (Exception ignored) {
    }
    return defaultModel;
  }

  private void waitForProfileHealth(String profile) {
    Integer port = properties.ports().get(profile);
    if (port == null) {
      throw new IllegalStateException("Hermes profile port is not configured: " + profile);
    }
    for (int attempt = 0; attempt < 30; attempt++) {
      try {
        restTemplate.getForEntity("http://127.0.0.1:" + port + "/health", String.class);
        return;
      } catch (Exception e) {
        try {
          Thread.sleep(1000);
        } catch (InterruptedException interrupted) {
          Thread.currentThread().interrupt();
          throw new IllegalStateException("Interrupted while waiting for Hermes profile " + profile, interrupted);
        }
      }
    }
    throw new IllegalStateException("Hermes profile " + profile + " did not become healthy");
  }

  private void rollbackProfiles(Map<Path, byte[]> backups) {
    backups.forEach((path, bytes) -> {
      try {
        writeAtomically(path, bytes);
      } catch (IOException ignored) {
      }
    });
    for (Path path : backups.keySet()) {
      String profile = profileName(path);
      if (profile != null) {
        try {
          gatewayService.restart(profile);
        } catch (Exception ignored) {
        }
      }
    }
  }

  private String profileName(Path path) {
    for (String profile : properties.profiles()) {
      if (path.toString().contains("/" + profile + "/")) {
        return profile;
      }
    }
    return null;
  }

  private void putOptionalApiKey(Map<String, Object> model, String encryptedApiKey) {
    String apiKey = decryptCredential(encryptedApiKey);
    if (apiKey == null || apiKey.isBlank()) {
      model.remove("api_key");
    } else {
      model.put("api_key", apiKey);
    }
  }

  private String updatedCredential(String existing, String supplied, boolean clear) {
    if (clear) {
      return null;
    }
    if (supplied == null || supplied.isBlank()) {
      return existing;
    }
    return credentialCipher.encrypt(supplied.trim());
  }

  private String decryptCredential(String encrypted) {
    return credentialCipher.decrypt(encrypted);
  }

  private boolean hasCredential(String encrypted) {
    String credential = decryptCredential(encrypted);
    return credential != null && !credential.isBlank();
  }

  private String discoveryApiKey(HermesModelDiscoveryRequest request) {
    if (request == null) {
      return null;
    }
    String supplied = blankToNull(request.apiKey());
    if (supplied != null) {
      return supplied;
    }
    String profileId = blankToNull(request.profileId());
    if (profileId == null) {
      return null;
    }
    StoredConfig config = readConfig();
    if (OPENAI.equals(profileId) || LOCAL.equals(profileId)) {
      return decryptCredential(backendById(config, profileId).encryptedApiKey());
    }
    StoredRoute route = routeById(config, profileId);
    return decryptCredential(backendById(config, route.backendId()).encryptedApiKey());
  }

  private StoredBackend backendById(StoredConfig config, String id) {
    String normalized = normalizeBackendId(id);
    return config.backends().stream()
        .filter(backend -> normalized.equals(backend.id()))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("Missing Hermes backend: " + normalized));
  }

  private StoredRoute routeById(StoredConfig config, String id) {
    return config.routes().stream()
        .filter(route -> id.equals(route.id()))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("Missing Hermes route: " + id));
  }

  private List<StoredBackend> replaceBackend(List<StoredBackend> backends, StoredBackend updated) {
    return backends.stream()
        .map(backend -> backend.id().equals(updated.id()) ? updated : backend)
        .toList();
  }

  private URI validatedBaseUrl(String value) {
    String trimmed = requireValue(value, "baseUrl");
    try {
      URI uri = URI.create(trimmed);
      if (uri.getScheme() == null || uri.getHost() == null) {
        throw new IllegalArgumentException("baseUrl must be absolute");
      }
      return uri;
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException("Invalid baseUrl: " + value, e);
    }
  }

  private Integer normalizeContextWindow(Integer contextWindow) {
    return contextWindow == null ? MIN_CONTEXT_WINDOW : Math.max(contextWindow, MIN_CONTEXT_WINDOW);
  }

  private void validateContextWindow(Integer contextWindow, String owner) {
    if (contextWindow == null || contextWindow < MIN_CONTEXT_WINDOW) {
      throw new IllegalArgumentException(owner + " contextWindow must be at least " + MIN_CONTEXT_WINDOW);
    }
  }

  private String normalizeProvider(String value) {
    String provider = firstNonBlank(value, "ollama").toLowerCase();
    if (OPENAI.equals(provider) || LocalLlmClient.isLocal(provider)) {
      return provider;
    }
    throw new IllegalArgumentException("Unsupported provider: " + value);
  }

  private String normalizeLocalProvider(String value) {
    String provider = normalizeProvider(value);
    if (OPENAI.equals(provider)) {
      throw new IllegalArgumentException("local backend provider must be ollama, lmstudio, or vllm");
    }
    return provider;
  }

  private String normalizeBackendId(String value) {
    String id = requireValue(value, "backend id").toLowerCase();
    if (!OPENAI.equals(id) && !LOCAL.equals(id)) {
      throw new IllegalArgumentException("backend id must be openai or local");
    }
    return id;
  }

  private String normalizeMode(String value) {
    String mode = firstNonBlank(value, MODE_ENABLED).toLowerCase();
    if (!MODE_ENABLED.equals(mode) && !MODE_OFF.equals(mode)) {
      throw new IllegalArgumentException("systemMode must be enabled or off");
    }
    return mode;
  }

  private String defaultRouteLabel(String id) {
    return switch (id) {
      case "ai-command" -> "Hermes AI command";
      default -> "Hermes chat";
    };
  }

  private String routeGatewayModelAlias(String routeId) {
    return switch (routeId) {
      case "chat" -> "hermes-chat";
      case AI_COMMAND -> "hermes-ai-command";
      default -> "hermes-" + routeId;
    };
  }

  private String routeGatewayBaseUrl(String routeId) {
    Integer port = properties.ports().get(routeId);
    return port == null ? null : "http://127.0.0.1:" + port;
  }

  private String routeGatewayApiMode(String routeId, String backendApiMode) {
    return AI_COMMAND.equals(routeId) ? "chat-completions" : firstNonBlank(backendApiMode, RESPONSES);
  }

  private String requireValue(String value, String name) {
    String trimmed = blankToNull(value);
    if (trimmed == null) {
      throw new IllegalArgumentException(name + " is required");
    }
    return trimmed;
  }

  private String firstNonBlank(String... values) {
    if (values == null) {
      return null;
    }
    for (String value : values) {
      String trimmed = blankToNull(value);
      if (trimmed != null) {
        return trimmed;
      }
    }
    return null;
  }

  @SafeVarargs
  private final <T> T firstNonNull(T... values) {
    if (values == null) {
      return null;
    }
    for (T value : values) {
      if (value != null) {
        return value;
      }
    }
    return null;
  }

  private String blankToNull(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    return value.trim();
  }

  private String trimTrailingSlash(String value) {
    if (value == null) {
      return null;
    }
    return value.trim().replaceFirst("/+$", "");
  }

  private String text(JsonNode node, String field) {
    if (node == null || !node.has(field) || node.get(field).isNull()) {
      return null;
    }
    String value = node.get(field).asText();
    return value == null || value.isBlank() ? null : value;
  }

  private int intValue(JsonNode node, String field, int defaultValue) {
    Integer value = intValueOrNull(node, field);
    return value == null ? defaultValue : value;
  }

  private Integer intValueOrNull(JsonNode node, String field) {
    if (node == null || !node.has(field) || node.get(field).isNull()) {
      return null;
    }
    return node.get(field).asInt();
  }

  private Path configPath() {
    return properties.dataDir().resolve(CONFIG_FILE);
  }

  private void writeAtomically(Path path, byte[] bytes) throws IOException {
    Files.createDirectories(path.getParent());
    Path tmp = path.resolveSibling(path.getFileName() + ".tmp");
    Files.write(tmp, bytes);
    try {
      Files.setPosixFilePermissions(tmp, EnumSet.of(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE));
    } catch (UnsupportedOperationException ignored) {
    }
    Files.move(tmp, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
  }

  private record StoredConfig(
      String systemMode,
      List<StoredBackend> backends,
      List<StoredRoute> routes) {
  }

  private record StoredBackend(
      String id,
      String label,
      String provider,
      String baseUrl,
      String model,
      String apiMode,
      Integer timeoutSeconds,
      Integer contextWindow,
      String lastValidatedAt,
      String validationStatus,
      Boolean toolCapable,
      String detail,
      String encryptedApiKey) {
  }

  private record StoredRoute(
      String id,
      String label,
      String backendId) {
  }

  private record LegacyFallback(
      String provider,
      String baseUrl,
      String model,
      Boolean enabled,
      Integer contextWindow,
      String lastValidatedAt,
      String validationStatus,
      Boolean toolCapable,
      String validationDetail,
      String encryptedApiKey) {
  }

  private record BackendHealth(Boolean healthy, Boolean toolCapable, String detail) {
  }

  private record CredentialStatus(boolean openAiAvailable, String detail) {
  }
}
