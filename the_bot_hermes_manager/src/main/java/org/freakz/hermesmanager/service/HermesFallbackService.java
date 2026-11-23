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
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.List;
import java.util.Locale;
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
  private static final String LOCAL_PREFIX = "local-";
  private static final String DEFAULT_LOCAL = "local-0";
  private static final String AI_COMMAND = "ai-command";
  private static final String MODE_ENABLED = "enabled";
  private static final String MODE_OFF = "off";
  private static final String RESPONSES = "responses";
  private static final int DEFAULT_TIMEOUT_SECONDS = 120;
  private static final int MIN_CONTEXT_WINDOW = 65536;
  private static final List<String> OPENAI_MODELS = List.of("gpt-6-luna", "gpt-6-sol", "gpt-6-astra", "gpt-6.1-sol");
  private static final String DEFAULT_OPENAI_MODEL = "gpt-6.1-sol";

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
    } catch (Exception e) {
      log.warn("Could not reconcile Hermes backend configuration during startup: {}", e.getMessage());
    }
  }

  public HermesFallbackSettingsResponse getSettings() {
    StoredConfig config = readConfig();
    StoredBackend local = firstLocalBackend(config);
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
    StoredBackend current = firstLocalBackend(config);
    String provider = normalizeLocalProvider(firstNonBlank(request == null ? null : request.provider(), current.provider()));
    String baseUrl = trimTrailingSlash(firstNonBlank(request == null ? null : request.baseUrl(), current.baseUrl()));
    StoredBackend updated = new StoredBackend(
        current.id(),
        current.label(),
        provider,
        baseUrl,
        firstNonBlank(request == null ? null : request.model(), current.model()),
        firstNonBlank(current.apiMode(), RESPONSES),
        current.timeoutSeconds(),
        normalizeContextWindow(request == null || request.contextWindow() == null ? current.contextWindow() : request.contextWindow()),
        current.lastValidatedAt(),
        current.validationStatus(),
        current.toolCapable(),
        current.detail(),
        credentialForDestination(
            current, provider, baseUrl,
            request == null ? null : request.apiKey(),
            request != null && Boolean.TRUE.equals(request.clearApiKey())),
        reasoningDisabled(
            normalizeLocalProvider(firstNonBlank(request == null ? null : request.provider(), current.provider())),
            null,
            current.reasoningDisabled()));
    StoredConfig saved = new StoredConfig(config.systemMode(), replaceBackend(config.backends(), updated), config.routes());
    updateLock.lock();
    try {
      validateBackend(updated);
      applyHermesConfiguration(saved);
      return getSettings();
    } catch (HermesValidationException | IllegalArgumentException e) {
      throw e;
    } catch (Exception e) {
      throw new IllegalStateException("Could not update local Hermes backend", e);
    } finally {
      updateLock.unlock();
    }
  }

  public HermesFallbackModelsResponse getModels(HermesModelDiscoveryRequest request) {
    String provider = normalizeProvider(request == null ? null : request.provider());
    if (OPENAI.equals(provider)) {
      return new HermesFallbackModelsResponse(OPENAI_MODELS, OPENAI_MODELS.stream()
          .map(model -> new HermesFallbackModel(
              model,
              "tool-capable",
              "OpenAI model",
              true,
              "Supported OpenAI model"))
          .toList());
    }
    URI uri = validatedBaseUrl(request == null ? null : request.baseUrl());
    return localLlmClient.discover(provider, uri, discoveryApiKey(request, provider, uri));
  }

  public HermesBackendConfigResponse getBackendConfig() {
    return response(readConfig());
  }

  public HermesBackendConfigResponse updateBackendConfig(HermesBackendConfigUpdateRequest request) {
    updateLock.lock();
    try {
      StoredConfig config = sanitizeConfig(request);
      validateConfig(config);
      applyHermesConfiguration(config, request != null && Boolean.TRUE.equals(request.purgeState()));
      return response(config);
    } catch (HermesValidationException | IllegalArgumentException e) {
      throw e;
    } catch (Exception e) {
      throw new IllegalStateException("Could not apply Hermes backend configuration", e);
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
              hasCredential(backend.encryptedApiKey()),
              backend.reasoningDisabled());
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
            .map(update -> sanitizeBackend(
                update,
                existingBackendById(current, update == null ? null : update.id()).orElse(null)))
            .toList();
    if (request.backends() != null
        && !request.backends().isEmpty()
        && backends.stream().noneMatch(backend -> isLocalBackendId(backend.id()))) {
      throw new IllegalArgumentException("at least one local backend is required");
    }
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
    String baseUrl = OPENAI.equals(id) ? null : trimTrailingSlash(firstNonBlank(update.baseUrl(), defaults.baseUrl()));
    return new StoredBackend(
        id,
        firstNonBlank(update.label(), defaults.label()),
        provider,
        baseUrl,
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
        credentialForDestination(
            defaults, provider, baseUrl,
            update.apiKey(),
            Boolean.TRUE.equals(update.clearApiKey())),
        reasoningDisabled(provider, update.reasoningDisabled(), defaults.reasoningDisabled()));
  }

  private void validateConfig(StoredConfig config) {
    if (!MODE_ENABLED.equals(config.systemMode()) && !MODE_OFF.equals(config.systemMode())) {
      throw new IllegalArgumentException("systemMode must be enabled or off");
    }
    config.backends().stream()
        .filter(backend -> OPENAI.equals(backend.id()))
        .forEach(this::validateBackend);
    config.backends().stream()
        .filter(backend -> !OPENAI.equals(backend.id()))
        .forEach(backend -> validatedBaseUrl(backend.baseUrl()));
    List<String> activeBackendIds = config.routes().stream()
        .map(StoredRoute::backendId)
        .distinct()
        .toList();
    for (String backendId : activeBackendIds) {
      if (!OPENAI.equals(backendId)) {
        validateBackend(backendById(config, backendId));
      }
    }
    for (StoredRoute route : config.routes()) {
      backendById(config, route.backendId());
    }
    if (config.backends().stream().noneMatch(backend -> isLocalBackendId(backend.id()))) {
      throw new IllegalArgumentException("at least one local backend is required");
    }
  }

  private void validateBackend(StoredBackend backend) {
    if (backend.timeoutSeconds() == null || backend.timeoutSeconds() <= 0) {
      throw new IllegalArgumentException("backend " + backend.id() + " timeoutSeconds must be positive");
    }
    if (OPENAI.equals(backend.id())) {
      String model = requireValue(backend.model(), "OpenAI model");
      if (!OPENAI_MODELS.contains(model)) {
        throw new IllegalArgumentException(
            "Unsupported OpenAI model: " + model + ". Supported models: " + String.join(", ", OPENAI_MODELS));
      }
      return;
    }
    URI baseUrl = validatedBaseUrl(backend.baseUrl());
    requireValue(backend.model(), "local model");
    validateContextWindow(backend.contextWindow(), "local backend");
    String apiKey = decryptCredential(backend.encryptedApiKey());
    try {
      localLlmClient.discover(backend.provider(), baseUrl, apiKey);
      localLlmClient.validateToolCall(backend.provider(), baseUrl, backend.model(), apiKey, backend.reasoningDisabled());
      localLlmClient.validateChat(backend.provider(), baseUrl, backend.model(), apiKey, backend.reasoningDisabled());
    } catch (HermesValidationException | IllegalArgumentException e) {
      throw new HermesValidationException("Hermes local backend validation failed", null, null);
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
        firstNonBlank(openAiModel(legacyChat), openAiModel(legacyAiCommand), upstreamModel(OPENAI, DEFAULT_OPENAI_MODEL)),
        firstNonBlank(text(legacyChat, "apiMode"), RESPONSES),
        intValue(legacyChat, "timeoutSeconds", DEFAULT_TIMEOUT_SECONDS),
        null,
        null,
        null,
        true,
        null,
        null,
        false);
    StoredBackend local = localFromLegacy(localSource);
    return normalizeConfig(new StoredConfig(
        MODE_ENABLED,
        List.of(openAi, local),
        List.of(
            new StoredRoute("chat", "Hermes chat", isLocal(legacyChat) ? DEFAULT_LOCAL : OPENAI),
            new StoredRoute("ai-command", "Hermes AI command", isLocal(legacyAiCommand) ? DEFAULT_LOCAL : OPENAI))));
  }

  private StoredBackend localFromLegacy(JsonNode source) {
    LegacyFallback legacyFallback = readLegacyFallback();
    return new StoredBackend(
        DEFAULT_LOCAL,
        localLabel(DEFAULT_LOCAL),
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
        legacyFallback.encryptedApiKey(),
        reasoningDisabled(
            normalizeLocalProvider(firstNonBlank(text(source, "provider"), legacyFallback.provider(), "ollama")),
            booleanValueOrNull(source, "reasoningDisabled"),
            null));
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
    if (config != null && config.backends() != null) {
      for (StoredBackend backend : config.backends()) {
        String id = normalizeBackendId(backend.id());
        StoredBackend defaults = backends.containsKey(id) ? backends.get(id) : defaultBackend(id);
        String provider = OPENAI.equals(id) ? OPENAI : normalizeLocalProvider(firstNonBlank(backend.provider(), defaults.provider()));
        backends.put(id, new StoredBackend(
            id,
            OPENAI.equals(id) ? firstNonBlank(backend.label(), defaults.label()) : localLabel(id),
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
            backend.encryptedApiKey(),
            reasoningDisabled(provider, backend.reasoningDisabled(), defaults.reasoningDisabled())));
      }
    }
    if (backends.values().stream().noneMatch(backend -> isLocalBackendId(backend.id()))) {
      backends.put(DEFAULT_LOCAL, defaultBackend(DEFAULT_LOCAL));
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
        orderedBackends(backends),
        List.copyOf(routes.values()));
  }

  private StoredConfig defaultConfig() {
    return new StoredConfig(
        MODE_ENABLED,
        List.of(defaultBackend(OPENAI), defaultBackend(DEFAULT_LOCAL)),
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
          DEFAULT_OPENAI_MODEL,
          RESPONSES,
          DEFAULT_TIMEOUT_SECONDS,
          null,
          null,
          null,
          true,
          null,
          null,
          false);
    }
    return new StoredBackend(
        normalizeBackendId(id),
        localLabel(id),
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
        null,
        true);
  }

  private void writeConfig(StoredConfig config) throws IOException {
    writeAtomically(configPath(), jsonMapper.writeValueAsBytes(normalizeConfig(config)));
  }

  private void applyHermesConfiguration(StoredConfig config) throws IOException {
    applyHermesConfiguration(config, false);
  }

  private void applyHermesConfiguration(StoredConfig config, boolean purgeState) throws IOException {
    Map<Path, byte[]> backups = new LinkedHashMap<>();
    Set<String> changedProfiles = new LinkedHashSet<>();
    Set<String> quiesced = new LinkedHashSet<>();
    Map<Path, Path> runtimeBackups = new LinkedHashMap<>();
    try {
      if (purgeState) {
        for (StoredRoute route : config.routes()) {
          String profile = route.id();
          quiesced.add(profile);
          gatewayService.stop(profile);
          backupRuntimeState(profile, runtimeBackups);
        }
      }
      for (StoredRoute route : config.routes()) {
        updateProfileConfig(route.id(), backendById(config, route.backendId()), backups, changedProfiles, true);
      }
      changedProfiles.addAll(quiesced);
      for (String profile : changedProfiles) {
        if (quiesced.contains(profile)) {
          gatewayService.start(profile);
        } else {
          gatewayService.restart(profile);
        }
        waitForProfileHealth(profile);
      }
      writeConfig(config);
    } catch (Exception e) {
      for (String profile : quiesced) {
        try {
          gatewayService.stop(profile);
        } catch (RuntimeException stopError) {
          e.addSuppressed(stopError);
          // Avoid restoring state files while a gateway could still be using them.
          Path directory = profileDirectory(profile);
          runtimeBackups.keySet().removeIf(path -> path.getParent().equals(directory));
        }
      }
      restoreRuntimeState(runtimeBackups, e);
      changedProfiles.addAll(quiesced);
      rollbackProfiles(backups, changedProfiles, quiesced, e);
      throw e;
    }
  }

  private static final List<String> RUNTIME_FILES = List.of(
      "state.db", "state.db-shm", "state.db-wal", "response_store.db",
      "response_store.db-shm", "response_store.db-wal", "sessions");

  private Path profileDirectory(String profile) {
    return properties.dataDir().resolve("profiles").resolve(profile);
  }

  private void backupRuntimeState(String profile, Map<Path, Path> backups) throws IOException {
    Path directory = profileDirectory(profile);
    Path root = directory.resolve("state-backup");
    Files.createDirectories(root);
    Path backup = Files.createTempDirectory(root, Instant.now().toEpochMilli() + "-");
    for (String name : RUNTIME_FILES) {
      Path source = directory.resolve(name);
      Path destination = backup.resolve(name);
      if (Files.exists(source, java.nio.file.LinkOption.NOFOLLOW_LINKS)) {
        Files.move(source, destination, StandardCopyOption.ATOMIC_MOVE);
        backups.put(source, destination);
      }
    }
  }

  private void restoreRuntimeState(Map<Path, Path> backups, Exception failure) {
    backups.forEach((source, backup) -> {
      try {
        if (Files.exists(source, java.nio.file.LinkOption.NOFOLLOW_LINKS)) {
          Files.move(source, backup.resolveSibling("new-" + backup.getFileName()), StandardCopyOption.ATOMIC_MOVE);
        }
        Files.move(backup, source, StandardCopyOption.ATOMIC_MOVE);
      } catch (IOException e) {
        failure.addSuppressed(e);
      }
    });
  }

  private void updateProfileConfig(
      String profile,
      StoredBackend backend,
      Map<Path, byte[]> backups,
      Set<String> changedProfiles,
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
    byte[] previous = Files.readAllBytes(path);
    byte[] updated = updatedProfileYaml(profile, path, backend);
    Path envFile = path.resolveSibling(".env");
    byte[] oldEnv = Files.exists(envFile) ? Files.readAllBytes(envFile) : null;
    byte[] newEnv = updatedProfileEnv(oldEnv, decryptCredential(backend.encryptedApiKey()));
    boolean envChanged = !java.util.Arrays.equals(oldEnv, newEnv);
    boolean configChanged = !java.util.Arrays.equals(previous, updated);
    if (envChanged || configChanged) {
      changedProfiles.add(profile);
      if (envChanged) {
        backups.put(envFile, oldEnv);
        writeAtomically(envFile, newEnv);
      }
      if (configChanged) {
        backups.put(path, previous);
        writeAtomically(path, updated);
      }
    }
  }

  private static final String PROFILE_API_KEY = "THE_BOT_HERMES_MANAGED_API_KEY";

  private byte[] updatedProfileEnv(byte[] current, String key) {
    String original = current == null ? "" : new String(current, java.nio.charset.StandardCharsets.UTF_8);
    String retained = original.replaceAll("(?m)^[ \t]*(?:export[ \t]+)?" + PROFILE_API_KEY + "[ \t]*=.*(?:\\R|$)", "");
    if (key == null || key.isBlank()) {
      return retained.equals(original) ? current : retained.getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }
    if (!key.matches("[A-Za-z0-9._~+/-]+=*")) {
      throw new HermesValidationException("Local API key must be a valid bearer token", null, null);
    }
    String newline = retained.isEmpty() || retained.endsWith("\n") ? "" : "\n";
    return (retained + newline + PROFILE_API_KEY + "=" + key + "\n")
        .getBytes(java.nio.charset.StandardCharsets.UTF_8);
  }

  private byte[] updatedProfileYaml(String profile, Path path, StoredBackend backend) throws IOException {
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
      model.remove("api_mode");
      yaml.remove("custom_providers");
      removeAgentReasoningEffort(yaml);
    } else {
      model.put("provider", "custom");
      model.put("base_url", validatedBaseUrl(backend.baseUrl()).toString());
      model.put("default", backend.model());
      model.put("api_mode", "chat_completions");
      putOptionalApiKey(model, backend.encryptedApiKey());
      if (backend.contextWindow() != null) {
        model.put("context_length", backend.contextWindow());
      }
      yaml.put("custom_providers", customProviders(backend));
      configureReasoning(yaml, backend);
    }
    yaml.put("model", model);
    yaml.put("fallback_providers", List.of());
    if (AI_COMMAND.equals(profile)) {
      configureNoNativeToolsProfile(yaml);
    }
    return yamlMapper.writeValueAsBytes(yaml);
  }

  private void configureNoNativeToolsProfile(Map<String, Object> yaml) {
    Map<String, Object> platformToolsets = mutableMap(yaml.get("platform_toolsets"));
    platformToolsets.put("api_server", List.of("no_mcp"));
    yaml.put("platform_toolsets", platformToolsets);

    Map<String, Object> agent = mutableMap(yaml.get("agent"));
    List<String> disabled = new ArrayList<>();
    Object existingDisabled = agent.get("disabled_toolsets");
    if (existingDisabled instanceof List<?> values) {
      values.stream().map(String::valueOf).forEach(disabled::add);
    }
    for (String toolset : noNativeToolsets()) {
      if (!disabled.contains(toolset)) {
        disabled.add(toolset);
      }
    }
    disabled.sort(String.CASE_INSENSITIVE_ORDER);
    agent.put("disabled_toolsets", disabled);
    yaml.put("agent", agent);
  }

  private List<Map<String, Object>> customProviders(StoredBackend backend) {
    Map<String, Object> provider = new LinkedHashMap<>();
    provider.put("name", "hermes-" + backend.id());
    provider.put("base_url", validatedBaseUrl(backend.baseUrl()).toString());
    provider.put("model", backend.model());
    provider.put("api_mode", "chat_completions");
    if (backend.contextWindow() != null) {
      provider.put("context_length", backend.contextWindow());
    }
    if (backend.contextWindow() != null && backend.model() != null && !backend.model().isBlank()) {
      provider.put("models", Map.of(backend.model(), Map.of("context_length", backend.contextWindow())));
    }
    putOptionalApiKey(provider, backend.encryptedApiKey());
    if (shouldDisableReasoning(backend)) {
      provider.put("extra_body", Map.of("reasoning_effort", "none"));
    }
    return List.of(provider);
  }

  private void configureReasoning(Map<String, Object> yaml, StoredBackend backend) {
    if (shouldDisableReasoning(backend)) {
      Map<String, Object> agent = mutableMap(yaml.get("agent"));
      agent.put("reasoning_effort", "none");
      yaml.put("agent", agent);
    } else {
      removeAgentReasoningEffort(yaml);
    }
  }

  private void removeAgentReasoningEffort(Map<String, Object> yaml) {
    Object existing = yaml.get("agent");
    if (existing instanceof Map<?, ?>) {
      Map<String, Object> agent = mutableMap(existing);
      agent.remove("reasoning_effort");
      yaml.put("agent", agent);
    }
  }

  private boolean shouldDisableReasoning(StoredBackend backend) {
    return LocalLlmClient.OLLAMA.equals(backend.provider()) && Boolean.TRUE.equals(backend.reasoningDisabled());
  }

  private List<String> noNativeToolsets() {
    return List.of(
        "browser",
        "clarify",
        "code_execution",
        "computer_use",
        "cronjob",
        "debugging",
        "delegation",
        "discord",
        "discord_admin",
        "feishu_doc",
        "feishu_drive",
        "file",
        "homeassistant",
        "image_gen",
        "kanban",
        "memory",
        "messaging",
        "moa",
        "rl",
        "search",
        "session_search",
        "skills",
        "spotify",
        "terminal",
        "todo",
        "tts",
        "vision",
        "web",
        "x_search",
        "yuanbao");
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
        restTemplate.getForEntity("http://" + properties.gatewayHealthHost() + ":" + port + "/health", String.class);
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

  private void rollbackProfiles(Map<Path, byte[]> backups, Set<String> changedProfiles,
      Set<String> quiesced, Exception failure) {
    backups.forEach((path, bytes) -> {
      try {
        if (bytes == null) {
          Files.deleteIfExists(path);
        } else {
          writeAtomically(path, bytes);
        }
      } catch (IOException e) {
        failure.addSuppressed(e);
      }
    });
    for (String profile : changedProfiles) {
      try {
        if (quiesced.contains(profile)) {
          gatewayService.start(profile);
        } else {
          gatewayService.restart(profile);
        }
      } catch (Exception e) {
        failure.addSuppressed(e);
      }
    }
  }

  private void putOptionalApiKey(Map<String, Object> model, String encryptedApiKey) {
    String apiKey = decryptCredential(encryptedApiKey);
    if (apiKey == null || apiKey.isBlank()) {
      model.remove("api_key");
    } else {
      model.put("api_key", "${" + PROFILE_API_KEY + "}");
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

  private String discoveryApiKey(HermesModelDiscoveryRequest request, String provider, URI destination) {
    String supplied = blankToNull(request.apiKey());
    if (supplied != null) {
      return supplied;
    }
    String profileId = blankToNull(request.profileId());
    if (profileId == null) {
      return null;
    }
    StoredConfig config = readConfig();
    StoredBackend backend = existingBackendById(config, profileId)
        .or(() -> existingRouteById(config, profileId).map(route -> backendById(config, route.backendId())))
        .orElse(null);
    if (backend == null || blankToNull(backend.encryptedApiKey()) == null) {
      return null;
    }
    if (!sameBackendDestination(backend, provider, destination.toString())) {
      throw new HermesValidationException("Stored credentials require the saved provider and base URL", null, null);
    }
    return decryptCredential(backend.encryptedApiKey());
  }

  private boolean sameBackendDestination(StoredBackend backend, String provider, String baseUrl) {
    return provider.equals(backend.provider())
        && canonicalBaseUrl(backend.baseUrl()).equals(canonicalBaseUrl(baseUrl));
  }

  private String credentialForDestination(StoredBackend existing, String provider, String baseUrl,
      String supplied, boolean clear) {
    if (!OPENAI.equals(provider) && blankToNull(existing.encryptedApiKey()) != null
        && !clear && blankToNull(supplied) == null && !sameBackendDestination(existing, provider, baseUrl)) {
      throw new HermesValidationException("Supply or clear the stored credential when changing the backend destination", null, null);
    }
    return updatedCredential(existing.encryptedApiKey(), supplied, clear);
  }

  private StoredBackend firstLocalBackend(StoredConfig config) {
    return config.backends().stream()
        .filter(backend -> isLocalBackendId(backend.id()))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("Missing Hermes local backend"));
  }

  private Optional<StoredBackend> existingBackendById(StoredConfig config, String id) {
    if (config == null || id == null || id.isBlank()) {
      return Optional.empty();
    }
    String normalized = normalizeBackendId(id);
    return config.backends().stream()
        .filter(backend -> normalized.equals(backend.id()))
        .findFirst();
  }

  private StoredBackend backendById(StoredConfig config, String id) {
    String normalized = normalizeBackendId(id);
    return config.backends().stream()
        .filter(backend -> normalized.equals(backend.id()))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("Missing Hermes backend: " + normalized));
  }

  private StoredRoute routeById(StoredConfig config, String id) {
    return existingRouteById(config, id)
        .orElseThrow(() -> new IllegalArgumentException("Missing Hermes route: " + id));
  }

  private Optional<StoredRoute> existingRouteById(StoredConfig config, String id) {
    if (config == null || id == null || id.isBlank()) {
      return Optional.empty();
    }
    return config.routes().stream()
        .filter(route -> id.equals(route.id()))
        .findFirst();
  }

  private List<StoredBackend> replaceBackend(List<StoredBackend> backends, StoredBackend updated) {
    return backends.stream()
        .map(backend -> backend.id().equals(updated.id()) ? updated : backend)
        .toList();
  }

  private List<StoredBackend> orderedBackends(Map<String, StoredBackend> backends) {
    List<StoredBackend> ordered = new ArrayList<>();
    ordered.add(backends.get(OPENAI));
    backends.values().stream()
        .filter(backend -> isLocalBackendId(backend.id()))
        .sorted(Comparator.comparingInt(backend -> localIndex(backend.id())))
        .forEach(ordered::add);
    return ordered;
  }

  private URI validatedBaseUrl(String value) {
    URI uri = canonicalBaseUrl(value);
    String origin = origin(uri);
    boolean isDefault = properties.defaultBaseUrl() != null
        && origin.equals(origin(canonicalBaseUrl(properties.defaultBaseUrl())));
    List<String> allowed = properties.allowedBackendOrigins();
    if (!isDefault && (allowed == null || allowed.stream().noneMatch(
        entry -> !entry.isBlank() && origin.equals(origin(canonicalBaseUrl(entry)))))) {
      throw new HermesValidationException("Backend origin is not allowed on this instance", null, null);
    }
    return uri;
  }

  private URI canonicalBaseUrl(String value) {
    try {
      URI uri = URI.create(requireValue(value, "baseUrl"));
      String scheme = uri.getScheme();
      if (!("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme))
          || uri.getHost() == null || uri.getRawUserInfo() != null || uri.getRawQuery() != null
          || uri.getRawFragment() != null || uri.getPort() == 0 || uri.getPort() > 65535) {
        throw new IllegalArgumentException();
      }
      String path = uri.normalize().getRawPath().replaceFirst("/+$", "");
      return URI.create(origin(uri) + path);
    } catch (IllegalArgumentException e) {
      throw new HermesValidationException("Provide an absolute HTTP or HTTPS backend URL without user details, query or fragment", null, null);
    }
  }

  private String origin(URI uri) {
    String scheme = uri.getScheme().toLowerCase(Locale.ROOT);
    int port = uri.getPort();
    boolean defaultPort = port == -1 || ("http".equals(scheme) && port == 80) || ("https".equals(scheme) && port == 443);
    return scheme + "://" + uri.getHost().toLowerCase(Locale.ROOT) + (defaultPort ? "" : ":" + port);
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
      throw new IllegalArgumentException("local backend provider must be ollama, lmstudio, vllm, or llamacpp");
    }
    return provider;
  }

  private String normalizeBackendId(String value) {
    String id = requireValue(value, "backend id").toLowerCase();
    if (LOCAL.equals(id)) {
      return DEFAULT_LOCAL;
    }
    if (!OPENAI.equals(id) && !isLocalBackendId(id)) {
      throw new IllegalArgumentException("backend id must be openai or local-N");
    }
    return id;
  }

  private boolean isLocalBackendId(String id) {
    if (id == null || !id.startsWith(LOCAL_PREFIX)) {
      return false;
    }
    try {
      return Integer.parseInt(id.substring(LOCAL_PREFIX.length())) >= 0;
    } catch (NumberFormatException e) {
      return false;
    }
  }

  private int localIndex(String id) {
    if (!isLocalBackendId(id)) {
      return Integer.MAX_VALUE;
    }
    return Integer.parseInt(id.substring(LOCAL_PREFIX.length()));
  }

  private String localLabel(String id) {
    return "#" + localIndex(normalizeBackendId(id)) + " local";
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

  private Boolean booleanValueOrNull(JsonNode node, String field) {
    if (node == null || !node.has(field) || node.get(field).isNull()) {
      return null;
    }
    return node.get(field).asBoolean();
  }

  private Boolean reasoningDisabled(String provider, Boolean supplied, Boolean existing) {
    if (!LocalLlmClient.OLLAMA.equals(provider)) {
      return false;
    }
    if (supplied != null) {
      return supplied;
    }
    if (existing != null) {
      return existing;
    }
    return true;
  }

  private Path configPath() {
    return properties.dataDir().resolve(CONFIG_FILE);
  }

  private void writeAtomically(Path path, byte[] bytes) throws IOException {
    Files.createDirectories(path.getParent());
    Path tmp = Files.createTempFile(path.getParent(), path.getFileName().toString(), ".tmp");
    try {
      Files.write(tmp, bytes);
      try {
        Files.setPosixFilePermissions(tmp, EnumSet.of(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE));
      } catch (UnsupportedOperationException ignored) {
        // Non-POSIX local test systems use the platform temporary-file permissions.
      }
      Files.move(tmp, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    } finally {
      Files.deleteIfExists(tmp);
    }
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
      String encryptedApiKey,
      Boolean reasoningDisabled) {
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
