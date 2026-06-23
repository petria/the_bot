package org.freakz.hermesmanager.service;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFilePermission;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

import org.freakz.common.model.engine.system.HermesBackendConfigResponse;
import org.freakz.common.model.engine.system.HermesBackendConfigUpdateRequest;
import org.freakz.common.model.engine.system.HermesFallbackModel;
import org.freakz.common.model.engine.system.HermesFallbackModelsResponse;
import org.freakz.common.model.engine.system.HermesFallbackProfileStatus;
import org.freakz.common.model.engine.system.HermesFallbackSettingsResponse;
import org.freakz.common.model.engine.system.HermesFallbackUpdateRequest;
import org.freakz.common.model.engine.system.HermesGlobalOverrideSettings;
import org.freakz.common.model.engine.system.HermesGlobalOverrideUpdate;
import org.freakz.common.model.engine.system.HermesModelDiscoveryRequest;
import org.freakz.common.model.engine.system.HermesProfile;
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

  private static final String SETTINGS_FILE = "the_bot_fallback.json";
  private static final String BACKEND_SETTINGS_FILE = "the_bot_hermes_backends.json";
  private static final String RUNTIME_STATE_FILE = "the_bot_hermes_runtime.json";
  private static final String OPENAI_PROVIDER = "openai";
  private static final String OLLAMA_PROVIDER = "ollama";
  private static final String CHAT_COMPLETIONS_API_MODE = "chat-completions";
  private static final String RESPONSES_API_MODE = "responses";
  private static final int DEFAULT_TIMEOUT_SECONDS = 120;
  private static final int MIN_CONTEXT_WINDOW = 65536;
  private static final Logger log = LoggerFactory.getLogger(HermesFallbackService.class);

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
    if (!Files.exists(settingsPath())) {
      try {
        writeFallbackConfig(defaultFallbackConfig());
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
    try {
      applyHermesConfiguration(readBackendConfig(), readFallbackConfig());
    } catch (Exception e) {
      log.warn("Could not reconcile Hermes profile configuration during startup: {}", e.getMessage());
    }
  }

  public HermesFallbackSettingsResponse getSettings() {
    StoredBackendConfig config = readBackendConfig();
    StoredFallbackConfig fallback = readFallbackConfig();
    return fallbackResponse(fallback, config, updateRuntimeState(config, fallback));
  }

  public HermesFallbackSettingsResponse update(HermesFallbackUpdateRequest request) {
    String provider = normalizeLocalProvider(request == null ? null : request.provider());
    URI baseUrl = validatedBaseUrl(request == null ? null : request.baseUrl());
    String model = requireValue(request == null ? null : request.model(), "model");
    boolean enabled = request != null && Boolean.TRUE.equals(request.enabled());
    StoredFallbackConfig current = readFallbackConfig();
    String encryptedApiKey = updatedCredential(
        current.encryptedApiKey(),
        request == null ? null : request.apiKey(),
        request != null && Boolean.TRUE.equals(request.clearApiKey()));
    String apiKey = decryptCredential(encryptedApiKey);
    localLlmClient.validateToolCall(baseUrl, model, apiKey);
    StoredBackendConfig config = readBackendConfig();
    if (config.globalOverride().enabled()) {
      localLlmClient.validateChat(baseUrl, model, apiKey);
    }

    StoredFallbackConfig saved = new StoredFallbackConfig(
        provider,
        trimTrailingSlash(baseUrl.toString()),
        model,
        enabled,
        request == null ? null : request.contextWindow(),
        Instant.now().toString(),
        "VALID",
        true,
        LocalLlmClient.displayName(provider) + " fallback validated successfully",
        encryptedApiKey);
    updateLock.lock();
    try {
      applyHermesConfiguration(config, saved);
      writeFallbackConfig(saved);
      return fallbackResponse(saved, config, updateRuntimeState(config, saved));
    } catch (Exception e) {
      throw new IllegalStateException("Could not apply Hermes fallback configuration: " + e.getMessage(), e);
    } finally {
      updateLock.unlock();
    }
  }

  public HermesFallbackModelsResponse getModels(HermesModelDiscoveryRequest request) {
    String provider = normalizeLocalProvider(request == null ? null : request.provider());
    URI uri = validatedBaseUrl(request == null ? null : request.baseUrl());
    return localLlmClient.discover(provider, uri, discoveryApiKey(request));
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
    if ("__fallback__".equals(profileId)) {
      return decryptCredential(readFallbackConfig().encryptedApiKey());
    }
    StoredProfile profile = profileById(readBackendConfig(), profileId);
    return profile == null ? null : decryptCredential(profile.encryptedApiKey());
  }

  public HermesBackendConfigResponse getBackendConfig() {
    StoredBackendConfig config = readBackendConfig();
    StoredFallbackConfig fallback = readFallbackConfig();
    return backendResponse(config, fallback, updateRuntimeState(config, fallback));
  }

  public HermesBackendConfigResponse updateBackendConfig(HermesBackendConfigUpdateRequest request) {
    StoredBackendConfig config = sanitizeBackendConfig(request);
    validateProfiles(config);
    StoredFallbackConfig fallback = sanitizeFallbackConfig(request == null ? null : request.fallback());
    StoredGlobalOverride override = sanitizeGlobalOverride(
        request == null ? null : request.globalOverride(),
        request == null ? null : request.requestedBy(),
        config.globalOverride());

    updateLock.lock();
    try {
      for (StoredProfile profile : config.profiles()) {
        if (LocalLlmClient.isLocal(profile.provider())) {
          URI baseUrl = validatedBaseUrl(profile.baseUrl());
          String apiKey = decryptCredential(profile.encryptedApiKey());
          localLlmClient.discover(profile.provider(), baseUrl, apiKey);
          if (requiresTools(profile.id())) {
            localLlmClient.validateToolCall(baseUrl, profile.model(), apiKey);
          } else {
            localLlmClient.validateChat(baseUrl, profile.model(), apiKey);
          }
        }
      }
      if (fallback.enabled() || override.enabled()) {
        URI baseUrl = validatedBaseUrl(fallback.baseUrl());
        String apiKey = decryptCredential(fallback.encryptedApiKey());
        localLlmClient.discover(fallback.provider(), baseUrl, apiKey);
        if (override.enabled()) {
          localLlmClient.validateToolCall(baseUrl, fallback.model(), apiKey);
          localLlmClient.validateChat(baseUrl, fallback.model(), apiKey);
          override = override.validated(Instant.now(), LocalLlmClient.displayName(fallback.provider())
              + " global override validated successfully");
        } else {
          localLlmClient.validateToolCall(baseUrl, fallback.model(), apiKey);
        }
        fallback = fallback.validated(
            Instant.now(),
            true,
            "VALID",
            LocalLlmClient.displayName(fallback.provider()) + " fallback validated successfully");
      }
      config = new StoredBackendConfig(config.profiles(), override);
      applyHermesConfiguration(config, fallback);
      writeBackendConfig(config);
      writeFallbackConfig(fallback);
      return backendResponse(config, fallback, updateRuntimeState(config, fallback));
    } catch (Exception e) {
      throw new IllegalStateException("Could not apply Hermes backend configuration: " + e.getMessage(), e);
    } finally {
      updateLock.unlock();
    }
  }

  private HermesFallbackSettingsResponse fallbackResponse(
      StoredFallbackConfig settings,
      StoredBackendConfig config,
      StoredRuntimeState runtimeState) {
    PassiveLocalHealth fallbackHealth = passiveLocalHealth(settings);
    List<HermesFallbackProfileStatus> statuses = properties.profiles().stream()
        .map(profileId -> {
          CredentialStatus credential = credentialStatus(profileId);
          StoredProfile profile = profileById(config, profileId);
          ProfileHealth health = openAiProfileHealth(profileId);
          RuntimeProfileState runtime = runtimeState.profiles().get(profileId);
          String activeProvider = runtime == null ? effectiveProvider(profile, settings, credential) : runtime.activeProvider();
          boolean globalOverride = runtime != null && "GLOBAL_OVERRIDE".equals(runtime.routeMode());
          boolean fallbackActive = !globalOverride && settings.provider().equals(activeProvider) && profile != null
              && OPENAI_PROVIDER.equals(profile.provider());
          return new HermesFallbackProfileStatus(
              profileId,
              globalOverride
                  ? "GLOBAL_OVERRIDE"
                  : fallbackActive ? activeProvider.toUpperCase() + "_FALLBACK" : activeProvider.toUpperCase() + "_PRIMARY",
              Boolean.TRUE.equals(health.healthy()),
              credential.openAiAvailable(),
              credential.cooldownUntil(),
              firstNonBlank(credential.detail(), health.detail()),
              activeProvider,
              fallbackActive ? credential.detail() : null,
              runtime == null ? null : runtime.fallbackActivatedAt(),
              runtime == null ? null : runtime.lastProviderError(),
              runtime == null ? null : runtime.lastProviderErrorAt());
        })
        .toList();
    return new HermesFallbackSettingsResponse(
        settings.enabled(),
        settings.provider(),
        settings.baseUrl(),
        settings.model(),
        statuses,
        settings.contextWindow(),
        fallbackHealth.healthy(),
        settings.toolCapable(),
        firstNonBlank(fallbackHealth.detail(), settings.validationDetail()),
        settings.lastValidatedAt(),
        settings.validationStatus(),
        hasCredential(settings.encryptedApiKey()));
  }

  private HermesBackendConfigResponse backendResponse(
      StoredBackendConfig config,
      StoredFallbackConfig fallback,
      StoredRuntimeState runtimeState) {
    HermesFallbackSettingsResponse fallbackResponse = fallbackResponse(fallback, config, runtimeState);
    return new HermesBackendConfigResponse(
        config.profiles().stream()
            .map(profile -> profileResponse(profile, fallback, config.globalOverride(), runtimeState))
            .toList(),
        fallbackResponse,
        globalOverrideResponse(config.globalOverride(), fallback));
  }

  private HermesProfile profileResponse(
      StoredProfile profile,
      StoredFallbackConfig fallback,
      StoredGlobalOverride override,
      StoredRuntimeState runtimeState) {
    ProfileHealth health = OPENAI_PROVIDER.equals(profile.provider())
        ? openAiProfileHealth(profile.id())
        : localProfileHealth(profile);
    CredentialStatus credential = credentialStatus(profile.id());
    PassiveLocalHealth fallbackHealth = passiveLocalHealth(fallback, override.enabled());
    RuntimeProfileState runtime = runtimeState.profiles().get(profile.id());
    String activeProvider = runtime == null ? effectiveProvider(profile, fallback, credential) : runtime.activeProvider();
    boolean overrideEnabled = configOverrideEnabled(runtime);
    boolean primaryHealthy = overrideEnabled
        ? Boolean.TRUE.equals(fallbackHealth.healthy())
        : OPENAI_PROVIDER.equals(profile.provider())
        ? credential.openAiAvailable()
        : Boolean.TRUE.equals(health.healthy());
    return new HermesProfile(
        profile.id(),
        profile.label(),
        profile.provider(),
        profile.baseUrl(),
        profile.model(),
        profile.apiMode(),
        profile.timeoutSeconds(),
        overrideEnabled ? fallbackHealth.healthy() : health.healthy(),
        health.toolCapable(),
        health.detail(),
        profile.contextWindow(),
        profile.fallbackAllowed(),
        activeProvider,
        overrideEnabled ? fallbackHealth.healthy() : health.healthy(),
        primaryHealthy,
        fallbackHealth.healthy(),
        credential.cooldownUntil(),
        runtime == null ? null : runtime.fallbackReason(),
        runtime == null ? null : runtime.fallbackActivatedAt(),
        runtime == null ? null : runtime.lastProviderError(),
        runtime == null ? null : runtime.lastProviderErrorAt(),
        profile.lastValidatedAt(),
        profile.validationStatus(),
        hasCredential(profile.encryptedApiKey()));
  }

  private boolean configOverrideEnabled(RuntimeProfileState runtime) {
    return runtime != null && "GLOBAL_OVERRIDE".equals(runtime.routeMode());
  }

  private HermesGlobalOverrideSettings globalOverrideResponse(
      StoredGlobalOverride override,
      StoredFallbackConfig fallback) {
    PassiveLocalHealth health = override.enabled()
        ? passiveLocalHealth(fallback, true)
        : new PassiveLocalHealth(null, "Global override disabled");
    return new HermesGlobalOverrideSettings(
        override.enabled(),
        fallback.provider(),
        fallback.baseUrl(),
        fallback.model(),
        fallback.contextWindow(),
        health.healthy(),
        firstNonBlank(health.detail(), override.detail()),
        override.activatedAt(),
        override.updatedBy(),
        override.validationStatus(),
        override.lastValidatedAt());
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

  private ProfileHealth localProfileHealth(StoredProfile profile) {
    try {
      URI baseUrl = validatedBaseUrl(profile.baseUrl());
      HermesFallbackModelsResponse models = localLlmClient.discover(
          profile.provider(),
          baseUrl,
          decryptCredential(profile.encryptedApiKey()));
      Boolean toolCapable = models.items().stream()
          .filter(model -> profile.model().equals(model.id()))
          .map(HermesFallbackModel::toolCapable)
          .findFirst()
          .orElse(profile.toolCapable());
      return new ProfileHealth(
          true,
          toolCapable,
          LocalLlmClient.displayName(profile.provider()) + " backend reachable");
    } catch (Exception e) {
      return new ProfileHealth(
          false,
          null,
          LocalLlmClient.displayName(profile.provider()) + " backend check failed");
    }
  }

  private PassiveLocalHealth passiveLocalHealth(StoredFallbackConfig fallback) {
    return passiveLocalHealth(fallback, false);
  }

  private PassiveLocalHealth passiveLocalHealth(StoredFallbackConfig fallback, boolean forceEnabled) {
    if (fallback == null || (!forceEnabled && !fallback.enabled())) {
      return new PassiveLocalHealth(null, "Local LLM fallback disabled");
    }
    try {
      URI baseUrl = validatedBaseUrl(fallback.baseUrl());
      HermesFallbackModelsResponse models = localLlmClient.discover(
          fallback.provider(),
          baseUrl,
          decryptCredential(fallback.encryptedApiKey()));
      if (!models.models().contains(fallback.model())) {
        return new PassiveLocalHealth(false, "Configured fallback model is not available");
      }
      return new PassiveLocalHealth(
          true,
          LocalLlmClient.displayName(fallback.provider()) + " fallback reachable");
    } catch (Exception e) {
      return new PassiveLocalHealth(
          false,
          LocalLlmClient.displayName(fallback.provider()) + " fallback is not reachable");
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
      String latestError = null;
      for (Object value : credentials) {
        if (!(value instanceof Map<?, ?> credential)) {
          continue;
        }
        Object accessToken = credential.get("access_token");
        Object refreshToken = credential.get("refresh_token");
        if ((accessToken == null || accessToken.toString().isBlank())
            && (refreshToken == null || refreshToken.toString().isBlank())) {
          latestError = firstNonBlank(
              stringValue(credential.get("last_error_message")),
              stringValue(credential.get("last_error_reason")),
              "OpenAI access token is missing");
          continue;
        }
        Object resetValue = credential.get("last_error_reset_at");
        if (resetValue == null || resetValue.toString().isBlank()) {
          String lastStatus = stringValue(credential.get("last_status"));
          if ("error".equalsIgnoreCase(lastStatus) || "failed".equalsIgnoreCase(lastStatus)) {
            latestError = firstNonBlank(
                stringValue(credential.get("last_error_message")),
                stringValue(credential.get("last_error_reason")),
                "OpenAI credential is in an error state");
            continue;
          }
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
          firstNonBlank(latestError, latestCooldown == null ? null : "OpenAI quota cooldown active", "OpenAI credential unavailable"));
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

  private StoredFallbackConfig readFallbackConfig() {
    Path path = settingsPath();
    if (!Files.exists(path)) {
      return defaultFallbackConfig();
    }
    try {
      StoredFallbackConfig config = jsonMapper.readValue(Files.readAllBytes(path), StoredFallbackConfig.class);
      StoredFallbackConfig normalized = config == null
          ? defaultFallbackConfig()
          : config.withDefaults(defaultFallbackConfig());
      return new StoredFallbackConfig(
          firstNonBlank(normalized.provider(), OLLAMA_PROVIDER),
          normalized.baseUrl(),
          normalized.model(),
          normalized.enabled(),
          normalizeContextWindow(normalized.contextWindow()),
          normalized.lastValidatedAt(),
          normalized.validationStatus(),
          normalized.toolCapable(),
          normalized.validationDetail(),
          normalized.encryptedApiKey());
    } catch (IOException e) {
      throw new IllegalStateException("Could not read " + path, e);
    }
  }

  private StoredBackendConfig sanitizeBackendConfig(HermesBackendConfigUpdateRequest request) {
    if (request == null || request.profiles() == null) {
      throw new IllegalArgumentException("profiles are required");
    }
    StoredBackendConfig current = readBackendConfig();
    return normalizeBackendConfig(new StoredBackendConfig(request.profiles().stream()
        .map(profile -> {
          StoredProfile existing = profileById(current, profile.id());
          String provider = normalizeProvider(profile.provider());
          return new StoredProfile(
            requireValue(profile.id(), "profile id"),
            requireValue(profile.label(), "profile label"),
            provider,
            blankToNull(profile.baseUrl()),
            requireValue(profile.model(), "profile model"),
            firstNonBlank(profile.apiMode(), RESPONSES_API_MODE),
            profile.timeoutSeconds() == null ? DEFAULT_TIMEOUT_SECONDS : profile.timeoutSeconds(),
            LocalLlmClient.isLocal(provider)
                ? normalizeContextWindow(profile.contextWindow())
                : profile.contextWindow(),
            Boolean.TRUE.equals(profile.fallbackAllowed()),
            existing == null ? null : existing.toolCapable(),
            existing == null ? null : existing.lastValidatedAt(),
            existing == null ? null : existing.validationStatus(),
            updatedCredential(
                existing == null ? null : existing.encryptedApiKey(),
                profile.apiKey(),
                Boolean.TRUE.equals(profile.clearApiKey())));
        })
        .toList(), current.globalOverride()));
  }

  private StoredGlobalOverride sanitizeGlobalOverride(
      HermesGlobalOverrideUpdate request,
      String requestedBy,
      StoredGlobalOverride current) {
    StoredGlobalOverride existing = current == null ? defaultGlobalOverride() : current;
    if (request == null || request.enabled() == null || request.enabled() == existing.enabled()) {
      return existing;
    }
    boolean enabled = Boolean.TRUE.equals(request.enabled());
    return new StoredGlobalOverride(
        enabled,
        enabled ? Instant.now().toString() : null,
        firstNonBlank(requestedBy, "unknown"),
        enabled ? "PENDING_VALIDATION" : "DISABLED",
        existing.lastValidatedAt(),
        enabled ? "Global override awaiting validation" : "Global override disabled");
  }

  private StoredFallbackConfig sanitizeFallbackConfig(HermesFallbackUpdateRequest request) {
    StoredFallbackConfig current = readFallbackConfig();
    if (request == null) {
      return current;
    }
    boolean enabled = Boolean.TRUE.equals(request.enabled());
    String provider = firstNonBlank(request.provider(), current.provider(), OLLAMA_PROVIDER);
    provider = normalizeLocalProvider(provider);
    String baseUrl = trimTrailingSlash(firstNonBlank(request.baseUrl(), current.baseUrl()));
    String model = firstNonBlank(request.model(), current.model());
    Integer contextWindow = normalizeContextWindow(
        request.contextWindow() == null ? current.contextWindow() : request.contextWindow());
    if (enabled) {
      validatedBaseUrl(baseUrl);
      requireValue(model, "fallback model");
      validateContextWindow(contextWindow, "fallback");
    }
    return new StoredFallbackConfig(
        provider,
        baseUrl,
        model,
        enabled,
        contextWindow,
        current.lastValidatedAt(),
        current.validationStatus(),
        current.toolCapable(),
        current.validationDetail(),
        updatedCredential(
            current.encryptedApiKey(),
            request.apiKey(),
            Boolean.TRUE.equals(request.clearApiKey())));
  }

  private StoredBackendConfig normalizeBackendConfig(StoredBackendConfig config) {
    Map<String, StoredProfile> merged = new LinkedHashMap<>();
    defaultBackendConfig().profiles().forEach(profile -> merged.put(profile.id(), profile));
    if (config != null && config.profiles() != null) {
      config.profiles().forEach(profile -> {
        StoredProfile defaults = merged.get(profile.id());
        String model = OPENAI_PROVIDER.equals(profile.provider()) && profile.model() != null
            && profile.model().startsWith("hermes-")
                ? upstreamModel(profile.id(), defaults == null ? "gpt-5.5" : defaults.model())
                : profile.model();
        merged.put(profile.id(), new StoredProfile(
            profile.id(),
            firstNonBlank(profile.label(), defaults == null ? profile.id() : defaults.label()),
            normalizeProvider(firstNonBlank(
                profile.provider(),
                defaults == null ? OPENAI_PROVIDER : defaults.provider())),
            profile.baseUrl(),
            firstNonBlank(model, defaults == null ? "gpt-5.5" : defaults.model()),
            firstNonBlank(profile.apiMode(), RESPONSES_API_MODE),
            profile.timeoutSeconds() == null ? DEFAULT_TIMEOUT_SECONDS : profile.timeoutSeconds(),
            LocalLlmClient.isLocal(profile.provider())
                ? normalizeContextWindow(profile.contextWindow())
                : profile.contextWindow(),
            profile.fallbackAllowed() == null
                ? defaults != null && Boolean.TRUE.equals(defaults.fallbackAllowed())
                : profile.fallbackAllowed(),
            profile.toolCapable(),
            profile.lastValidatedAt(),
            profile.validationStatus(),
            profile.encryptedApiKey()));
      });
    }
    StoredGlobalOverride override = config == null || config.globalOverride() == null
        ? defaultGlobalOverride()
        : config.globalOverride().withDefaults(defaultGlobalOverride());
    return new StoredBackendConfig(List.copyOf(merged.values()), override);
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
            null,
            false,
            null,
            null,
            null,
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
            null,
            true,
            null,
            null,
            null,
            null));
      }
    }
    return new StoredBackendConfig(profiles, defaultGlobalOverride());
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
      } else if (LocalLlmClient.isLocal(profile.provider())) {
        validatedBaseUrl(profile.baseUrl());
        requireValue(profile.model(), "profile model");
        validateContextWindow(profile.contextWindow(), "profile " + profile.id());
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
        new StoredProfile("chat", "Hermes chat", OPENAI_PROVIDER, null, upstreamModel("chat", "gpt-5.5"), RESPONSES_API_MODE, DEFAULT_TIMEOUT_SECONDS, null, true, null, null, null, null),
        new StoredProfile("coder", "Hermes coder", OPENAI_PROVIDER, null, upstreamModel("coder", "gpt-5.5"), RESPONSES_API_MODE, DEFAULT_TIMEOUT_SECONDS, null, true, null, null, null, null),
        new StoredProfile("ai-command", "Hermes AI command", OPENAI_PROVIDER, null, upstreamModel("ai-command", "gpt-5.5"), RESPONSES_API_MODE, DEFAULT_TIMEOUT_SECONDS, null, true, null, null, null, null)),
        defaultGlobalOverride());
  }

  private StoredGlobalOverride defaultGlobalOverride() {
    return new StoredGlobalOverride(false, null, null, "DISABLED", null, "Global override disabled");
  }

  private StoredFallbackConfig defaultFallbackConfig() {
    return new StoredFallbackConfig(
        OLLAMA_PROVIDER,
        properties.defaultBaseUrl(),
        properties.defaultModel(),
        false,
        MIN_CONTEXT_WINDOW,
        null,
        "NOT_VALIDATED",
        null,
        "Fallback has not been validated",
        null);
  }

  private Integer normalizeContextWindow(Integer contextWindow) {
    return contextWindow == null ? MIN_CONTEXT_WINDOW : Math.max(contextWindow, MIN_CONTEXT_WINDOW);
  }

  private void validateContextWindow(Integer contextWindow, String owner) {
    if (contextWindow == null || contextWindow < MIN_CONTEXT_WINDOW) {
      throw new IllegalArgumentException(
          owner + " contextWindow must be at least " + MIN_CONTEXT_WINDOW);
    }
  }

  private void writeBackendConfig(StoredBackendConfig config) throws IOException {
    writeAtomically(backendSettingsPath(), jsonMapper.writeValueAsBytes(normalizeBackendConfig(config)));
  }

  private void writeFallbackConfig(StoredFallbackConfig config) throws IOException {
    writeAtomically(settingsPath(), jsonMapper.writeValueAsBytes(config));
  }

  private void applyHermesConfiguration(StoredBackendConfig config, StoredFallbackConfig fallback) throws IOException {
    Map<Path, byte[]> backups = new LinkedHashMap<>();
    List<String> changedProfiles = new ArrayList<>();
    try {
      for (StoredProfile profile : config.profiles()) {
        Path path = findProfileConfig(profile.id());
        backups.put(path, Files.readAllBytes(path));
        byte[] updated = updatedProfileYaml(path, profile, fallback, config.globalOverride());
        if (!java.util.Arrays.equals(backups.get(path), updated)) {
          writeAtomically(path, updated);
          changedProfiles.add(profile.id());
        }
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

  private byte[] updatedProfileYaml(
      Path path,
      StoredProfile profile,
      StoredFallbackConfig fallback,
      StoredGlobalOverride override) throws IOException {
    Map<String, Object> yaml = yamlMapper.readValue(Files.readAllBytes(path), new TypeReference<>() {});
    if (yaml == null) {
      yaml = new LinkedHashMap<>();
    }
    Map<String, Object> model = mutableMap(yaml.get("model"));
    if (override.enabled()) {
      model.put("default", fallback.model());
      model.put("provider", "custom");
      model.put("base_url", trimTrailingSlash(fallback.baseUrl()));
      putOptionalApiKey(model, fallback.encryptedApiKey());
      if (fallback.contextWindow() != null) {
        model.put("context_length", fallback.contextWindow());
      }
      yaml.put("fallback_providers", List.of());
    } else if (LocalLlmClient.isLocal(profile.provider())) {
      model.put("default", profile.model());
      model.put("provider", "custom");
      model.put("base_url", trimTrailingSlash(profile.baseUrl()));
      putOptionalApiKey(model, profile.encryptedApiKey());
      if (profile.contextWindow() != null) {
        model.put("context_length", profile.contextWindow());
      }
      yaml.put("fallback_providers", List.of());
    } else {
      model.put("default", profile.model());
      model.put("provider", "openai-codex");
      model.put("base_url", "https://chatgpt.com/backend-api/codex");
      if (fallback.enabled() && profile.fallbackAllowed()) {
        Map<String, Object> fallbackProvider = new LinkedHashMap<>();
        fallbackProvider.put("provider", "custom");
        fallbackProvider.put("model", fallback.model());
        fallbackProvider.put("base_url", trimTrailingSlash(fallback.baseUrl()));
        putOptionalApiKey(fallbackProvider, fallback.encryptedApiKey());
        if (fallback.contextWindow() != null) {
          fallbackProvider.put("context_length", fallback.contextWindow());
        }
        yaml.put("fallback_providers", List.of(fallbackProvider));
      } else {
        yaml.put("fallback_providers", List.of());
      }
    }
    yaml.put("model", model);
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
    try (var paths = Files.walk(properties.dataDir())) {
      return paths.filter(Files::isRegularFile)
          .filter(path -> path.getFileName().toString().matches("config\\.ya?ml"))
          .filter(path -> path.toString().contains("/" + profile + "/"))
          .min(Comparator.comparingInt(Path::getNameCount))
          .orElseThrow(() -> new IllegalStateException("Could not find config YAML for Hermes profile " + profile));
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

  private String effectiveProvider(
      StoredProfile profile,
      StoredFallbackConfig fallback,
      CredentialStatus credential) {
    if (profile == null) {
      return OPENAI_PROVIDER;
    }
    if (LocalLlmClient.isLocal(profile.provider())) {
      return profile.provider();
    }
    if (!credential.openAiAvailable() && fallback.enabled() && profile.fallbackAllowed()) {
      return fallback.provider();
    }
    return OPENAI_PROVIDER;
  }

  private StoredRuntimeState updateRuntimeState(
      StoredBackendConfig config,
      StoredFallbackConfig fallback) {
    StoredRuntimeState previous = readRuntimeState();
    Map<String, RuntimeProfileState> profiles = new LinkedHashMap<>();
    Instant now = Instant.now();
    for (StoredProfile profile : config.profiles()) {
      CredentialStatus credential = credentialStatus(profile.id());
      boolean globalOverride = config.globalOverride().enabled();
      String activeProvider = globalOverride
          ? fallback.provider()
          : effectiveProvider(profile, fallback, credential);
      RuntimeProfileState old = previous.profiles().get(profile.id());
      boolean enteredFallback = !globalOverride && fallback.provider().equals(activeProvider)
          && OPENAI_PROVIDER.equals(profile.provider())
          && (old == null || !fallback.provider().equals(old.activeProvider()));
      String activatedAt = globalOverride
          ? config.globalOverride().activatedAt()
          : enteredFallback
          ? now.toString()
          : old == null ? null : old.fallbackActivatedAt();
      String reason = globalOverride
          ? "Global local LLM override enabled"
          : fallback.provider().equals(activeProvider) && OPENAI_PROVIDER.equals(profile.provider())
          ? credential.detail()
          : null;
      String lastError = credential.openAiAvailable()
          ? old == null ? null : old.lastProviderError()
          : credential.detail();
      String lastErrorAt = !credential.openAiAvailable()
          ? enteredFallback || old == null || old.lastProviderErrorAt() == null
              ? now.toString()
              : old.lastProviderErrorAt()
          : old == null ? null : old.lastProviderErrorAt();
      profiles.put(profile.id(), new RuntimeProfileState(
          activeProvider,
          reason,
          activatedAt,
          lastError,
          lastErrorAt,
          globalOverride ? "GLOBAL_OVERRIDE" : "NORMAL"));
    }
    StoredRuntimeState updated = new StoredRuntimeState(profiles);
    if (!updated.equals(previous)) {
      try {
        writeAtomically(runtimeStatePath(), jsonMapper.writeValueAsBytes(updated));
      } catch (IOException e) {
        log.warn("Could not persist Hermes runtime state: {}", e.getMessage());
      }
    }
    return updated;
  }

  private StoredRuntimeState readRuntimeState() {
    Path path = runtimeStatePath();
    if (!Files.exists(path)) {
      return new StoredRuntimeState(Map.of());
    }
    try {
      StoredRuntimeState state = jsonMapper.readValue(Files.readAllBytes(path), StoredRuntimeState.class);
      return state == null || state.profiles() == null ? new StoredRuntimeState(Map.of()) : state;
    } catch (Exception e) {
      return new StoredRuntimeState(Map.of());
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
    if (!OPENAI_PROVIDER.equals(normalized) && !LocalLlmClient.isLocal(normalized)) {
      throw new IllegalArgumentException(
          "provider must be openai, ollama, lmstudio, or vllm");
    }
    return normalized;
  }

  private String normalizeLocalProvider(String provider) {
    return LocalLlmClient.normalizeProvider(firstNonBlank(provider, OLLAMA_PROVIDER));
  }

  private String updatedCredential(String current, String replacement, boolean clear) {
    if (clear) {
      return null;
    }
    if (replacement == null || replacement.isBlank()) {
      return current;
    }
    return credentialCipher.encrypt(replacement.trim());
  }

  private String decryptCredential(String encrypted) {
    return encrypted == null || encrypted.isBlank() ? null : credentialCipher.decrypt(encrypted);
  }

  private boolean hasCredential(String encrypted) {
    return encrypted != null && !encrypted.isBlank();
  }

  private void putOptionalApiKey(Map<String, Object> target, String encrypted) {
    String apiKey = decryptCredential(encrypted);
    if (apiKey == null || apiKey.isBlank()) {
      target.remove("api_key");
    } else {
      target.put("api_key", apiKey);
    }
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

  private String stringValue(Object value) {
    return value == null || value.toString().isBlank() ? null : value.toString().trim();
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

  private Path runtimeStatePath() {
    return properties.dataDir().resolve(RUNTIME_STATE_FILE);
  }

  private void writeAtomically(Path path, byte[] bytes) throws IOException {
    Files.createDirectories(path.getParent());
    Path temporary = Files.createTempFile(path.getParent(), path.getFileName().toString(), ".tmp");
    restrictOwnerAccess(temporary);
    Files.write(temporary, bytes);
    try {
      Files.move(temporary, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
    } catch (java.nio.file.AtomicMoveNotSupportedException e) {
      Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING);
    }
    restrictOwnerAccess(path);
  }

  private void restrictOwnerAccess(Path path) throws IOException {
    try {
      Files.setPosixFilePermissions(
          path,
          EnumSet.of(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE));
    } catch (UnsupportedOperationException ignored) {
      // Non-POSIX filesystems do not expose Unix file permissions.
    }
  }

  private record CredentialStatus(boolean openAiAvailable, String cooldownUntil, String detail) {
  }

  private record ProfileHealth(Boolean healthy, Boolean toolCapable, String detail) {
  }

  private record PassiveLocalHealth(Boolean healthy, String detail) {
  }

  private record StoredBackendConfig(
      List<StoredProfile> profiles,
      StoredGlobalOverride globalOverride) {
  }

  private record StoredProfile(
      String id,
      String label,
      String provider,
      String baseUrl,
      String model,
      String apiMode,
      Integer timeoutSeconds,
      Integer contextWindow,
      Boolean fallbackAllowed,
      Boolean toolCapable,
      String lastValidatedAt,
      String validationStatus,
      String encryptedApiKey) {
  }

  private record StoredFallbackConfig(
      String provider,
      String baseUrl,
      String model,
      boolean enabled,
      Integer contextWindow,
      String lastValidatedAt,
      String validationStatus,
      Boolean toolCapable,
      String validationDetail,
      String encryptedApiKey) {

    StoredFallbackConfig withDefaults(StoredFallbackConfig defaults) {
      return new StoredFallbackConfig(
          provider == null ? defaults.provider : provider,
          baseUrl == null ? defaults.baseUrl : baseUrl,
          model == null ? defaults.model : model,
          enabled,
          contextWindow == null ? defaults.contextWindow : contextWindow,
          lastValidatedAt,
          validationStatus == null ? defaults.validationStatus : validationStatus,
          toolCapable,
          validationDetail == null ? defaults.validationDetail : validationDetail,
          encryptedApiKey);
    }

    StoredFallbackConfig validated(
        Instant at,
        Boolean validatedToolCapable,
        String status,
        String detail) {
      return new StoredFallbackConfig(
          provider,
          baseUrl,
          model,
          enabled,
          contextWindow,
          at.toString(),
          status,
          validatedToolCapable,
          detail,
          encryptedApiKey);
    }
  }

  private record StoredGlobalOverride(
      boolean enabled,
      String activatedAt,
      String updatedBy,
      String validationStatus,
      String lastValidatedAt,
      String detail) {

    StoredGlobalOverride withDefaults(StoredGlobalOverride defaults) {
      return new StoredGlobalOverride(
          enabled,
          activatedAt,
          updatedBy,
          validationStatus == null ? defaults.validationStatus : validationStatus,
          lastValidatedAt,
          detail == null ? defaults.detail : detail);
    }

    StoredGlobalOverride validated(Instant at, String validationDetail) {
      return new StoredGlobalOverride(
          enabled,
          activatedAt == null ? at.toString() : activatedAt,
          updatedBy,
          "VALID",
          at.toString(),
          validationDetail);
    }
  }

  private record StoredRuntimeState(Map<String, RuntimeProfileState> profiles) {
  }

  private record RuntimeProfileState(
      String activeProvider,
      String fallbackReason,
      String fallbackActivatedAt,
      String lastProviderError,
      String lastProviderErrorAt,
      String routeMode) {
  }
}
