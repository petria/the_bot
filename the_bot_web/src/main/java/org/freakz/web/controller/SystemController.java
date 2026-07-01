package org.freakz.web.controller;

import java.lang.management.ManagementFactory;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.micrometer.core.instrument.MeterRegistry;
import org.freakz.common.model.system.SystemComponentStatus;
import org.freakz.common.model.system.SystemStatusResponse;
import org.freakz.common.model.engine.system.HermesSettingsResponse;
import org.freakz.common.model.engine.system.HermesBackendConfigResponse;
import org.freakz.common.model.engine.system.HermesProfile;
import org.freakz.common.model.engine.system.OpenClawSettingsResponse;
import org.freakz.common.spring.rest.RestEngineClient;
import org.freakz.web.config.TheBotWebProperties;
import org.freakz.web.system.ContainerStatus;
import org.freakz.web.system.ContainerStatusProvider;
import org.springframework.boot.info.BuildProperties;
import org.springframework.core.env.Environment;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@RestController
@RequestMapping("/api/web/system")
public class SystemController {

  private final RestTemplate restTemplate;
  private final TheBotWebProperties properties;
  private final Environment environment;
  private final Optional<BuildProperties> buildProperties;
  private final MeterRegistry meterRegistry;
  private final ContainerStatusProvider containerStatusProvider;
  private final RestEngineClient engineClient;

  public SystemController(
      RestTemplate restTemplate,
      TheBotWebProperties properties,
      Environment environment,
      Optional<BuildProperties> buildProperties,
      MeterRegistry meterRegistry,
      ContainerStatusProvider containerStatusProvider,
      RestEngineClient engineClient) {
    this.restTemplate = restTemplate;
    this.properties = properties;
    this.environment = environment;
    this.buildProperties = buildProperties;
    this.meterRegistry = meterRegistry;
    this.containerStatusProvider = containerStatusProvider;
    this.engineClient = engineClient;
  }

  @GetMapping("/status")
  public SystemStatusResponse getStatus() {
    Instant checkedAt = Instant.now();
    List<SystemComponentStatus> components = new ArrayList<>();
    components.add(localComponentStatus(checkedAt));
    components.add(remoteComponentStatus("bot-io", properties.getBotIoBaseUrl(), checkedAt));
    components.add(remoteComponentStatus("bot-engine", properties.getBotEngineBaseUrl(), checkedAt));
    components.add(openClawComponentStatus(checkedAt));
    components.add(hermesComponentStatus(checkedAt));
    components.add(sidecarComponentStatus("bot-whatsapp", properties.getBotWhatsappContainerName(), checkedAt));
    return new SystemStatusResponse(checkedAt, components);
  }

  private SystemComponentStatus localComponentStatus(Instant checkedAt) {
    long uptimeMillis = ManagementFactory.getRuntimeMXBean().getUptime();
    Instant startedAt = Instant.ofEpochMilli(ManagementFactory.getRuntimeMXBean().getStartTime());
    BuildProperties build = buildProperties.orElse(null);
    ContainerStatus containerStatus = containerStatusProvider.getStatus(properties.getBotWebContainerName());
    return new SystemComponentStatus(
        "bot-web",
        effectiveSpringBootStatus("UP", containerStatus),
        "SPRING_BOOT",
        null,
        null,
        null,
        "local",
        activeProfiles(),
        build == null ? null : build.getVersion(),
        build == null ? null : build.getName(),
        Math.round(uptimeMillis / 1000.0),
        startedAt,
        localCounter("http.server.requests"),
        localCounter("thebot.http.client.requests"),
        0L,
        checkedAt,
        containerStatus.containerName(),
        containerStatus.state(),
        containerStatus.statusText(),
        containerStatus.image(),
        containerStatus.startedAt(),
        containerStatus.restartCount(),
        containerStatus.error(),
        null);
  }

  private SystemComponentStatus remoteComponentStatus(String name, String baseUrl, Instant checkedAt) {
    long startedNanos = System.nanoTime();
    String status = null;
    String version = null;
    String artifact = null;
    Long uptimeSeconds = null;
    Instant startedAt = null;
    Long receivedCalls = null;
    Long requestedCalls = null;
    String error = null;
    String containerName = switch (name) {
      case "bot-io" -> properties.getBotIoContainerName();
      case "bot-engine" -> properties.getBotEngineContainerName();
      default -> name;
    };
    ContainerStatus containerStatus = containerStatusProvider.getStatus(containerName);

    try {
      Map<?, ?> health = getActuatorMap(baseUrl, "/actuator/health");
      status = stringValue(health.get("status"));
      if (status == null || status.isBlank()) {
        throw new IllegalStateException("Missing actuator health status");
      }

      Map<?, ?> info = getActuatorMap(baseUrl, "/actuator/info");
      Map<?, ?> build = mapValue(info.get("build"));
      version = build == null ? null : stringValue(build.get("version"));
      artifact = build == null ? null : firstNonBlank(stringValue(build.get("artifact")), stringValue(build.get("name")));

      uptimeSeconds = Math.round(metricValue(baseUrl, "process.uptime"));
      Double startTime = metricValue(baseUrl, "process.start.time");
      startedAt = instantFromEpochSeconds(startTime);
      receivedCalls = optionalMetricCount(baseUrl, "http.server.requests");
      requestedCalls = optionalMetricCount(baseUrl, "thebot.http.client.requests");
    } catch (Exception e) {
      status = "DOWN";
      error = e.getMessage();
    }
    status = effectiveSpringBootStatus(status, containerStatus);

    long responseTimeMs = Math.max(1, Math.round((System.nanoTime() - startedNanos) / 1_000_000.0));
    return new SystemComponentStatus(
        name,
        status,
        "SPRING_BOOT",
        null,
        null,
        null,
        baseUrl,
        null,
        version,
        artifact,
        uptimeSeconds,
        startedAt,
        receivedCalls,
        requestedCalls,
        responseTimeMs,
        checkedAt,
        containerStatus.containerName(),
        containerStatus.state(),
        containerStatus.statusText(),
        containerStatus.image(),
        containerStatus.startedAt(),
        containerStatus.restartCount(),
        containerStatus.error(),
        error);
  }

  private SystemComponentStatus openClawComponentStatus(Instant checkedAt) {
    String mode = openClawDeploymentMode();
    if ("external".equalsIgnoreCase(mode)) {
      return externalOpenClawComponentStatus(mode, checkedAt);
    }
    return localOpenClawComponentStatus(mode, checkedAt);
  }

  private SystemComponentStatus localOpenClawComponentStatus(String mode, Instant checkedAt) {
    OpenClawRuntimeSelection selection = openClawRuntimeSelection();
    SystemComponentStatus status = sidecarComponentStatus(
        "bot-openclaw",
        properties.getBotOpenclawContainerName(),
        checkedAt);
    return status.withOpenClawDetails(mode, selection.wsUrl(), null);
  }

  private SystemComponentStatus externalOpenClawComponentStatus(String mode, Instant checkedAt) {
    long startedNanos = System.nanoTime();
    OpenClawRuntimeSelection selection = openClawRuntimeSelection();
    String healthUrl = null;
    String status = "UNKNOWN";
    String healthStatus = null;
    String error = null;
    ContainerStatus containerStatus = externalContainerStatus(properties.getBotOpenclawContainerName());

    try {
      healthUrl = selection.healthUrl();
      if (healthUrl == null || healthUrl.isBlank()) {
        throw new IllegalStateException("OpenClaw health URL is not configured");
      }
      GatewayHealthResult health = getGatewayHealth(healthUrl, "OpenClaw");
      status = health.componentStatus();
      healthStatus = health.healthStatus();
      error = health.error();
    } catch (Exception e) {
      status = "DOWN";
      error = e.getMessage();
    }

    long responseTimeMs = Math.max(1, Math.round((System.nanoTime() - startedNanos) / 1_000_000.0));
    return new SystemComponentStatus(
        "bot-openclaw",
        status,
        "OPENCLAW_GATEWAY",
        mode,
        healthUrl,
        healthStatus,
        selection.wsUrl(),
        null,
        null,
        null,
        containerStatus == null || containerStatus.startedAt() == null
            ? null
            : Math.max(0, Instant.now().getEpochSecond() - containerStatus.startedAt().getEpochSecond()),
        containerStatus == null ? null : containerStatus.startedAt(),
        null,
        null,
        responseTimeMs,
        checkedAt,
        containerStatus == null ? null : containerStatus.containerName(),
        containerStatus == null ? null : containerStatus.state(),
        containerStatus == null ? null : containerStatus.statusText(),
        containerStatus == null ? null : containerStatus.image(),
        containerStatus == null ? null : containerStatus.startedAt(),
        containerStatus == null ? null : containerStatus.restartCount(),
        containerStatus == null ? null : containerStatus.error(),
        error);
  }

  private SystemComponentStatus hermesComponentStatus(Instant checkedAt) {
    long startedNanos = System.nanoTime();
    SystemComponentStatus backendStatus = hermesBackendComponentStatus(checkedAt, startedNanos);
    if (backendStatus != null) {
      return backendStatus;
    }

    String baseUrl = null;
    String healthUrl = null;
    String status = "UNKNOWN";
    String healthStatus = null;
    String model = null;
    String apiMode = null;
    String timeout = null;
    String error = null;

    try {
      ResponseEntity<HermesSettingsResponse> response = engineClient.getHermesSettings();
      HermesSettingsResponse settings = response.getBody();
      if (!response.getStatusCode().is2xxSuccessful() || settings == null) {
        throw new IllegalStateException("Invalid Hermes settings response from bot-engine");
      }
      baseUrl = settings.baseUrl();
      model = settings.model();
      apiMode = settings.apiMode();
      timeout = settings.timeoutSeconds() == null ? null : settings.timeoutSeconds() + "s";
      if (!settings.configured()) {
        throw new IllegalStateException("Hermes is not configured");
      }
      healthUrl = firstNonBlank(settings.healthUrl(), healthUrlFromBaseUrl(baseUrl));
      GatewayHealthResult health = getGatewayHealth(healthUrl, "Hermes");
      status = health.componentStatus();
      healthStatus = health.healthStatus();
      error = health.error();
    } catch (Exception e) {
      status = "DOWN";
      error = e.getMessage();
    }

    long responseTimeMs = Math.max(1, Math.round((System.nanoTime() - startedNanos) / 1_000_000.0));
    return new SystemComponentStatus(
        "bot-hermes",
        status,
        "HERMES_GATEWAY",
        "external",
        healthUrl,
        healthStatus,
        baseUrl,
        apiMode,
        timeout,
        model,
        null,
        null,
        null,
        null,
        responseTimeMs,
        checkedAt,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        error);
  }

  private SystemComponentStatus hermesBackendComponentStatus(Instant checkedAt, long startedNanos) {
    try {
      ResponseEntity<HermesBackendConfigResponse> response = engineClient.getHermesBackendConfig();
      HermesBackendConfigResponse config = response.getBody();
      if (!response.getStatusCode().is2xxSuccessful() || config == null || config.profiles() == null) {
        return null;
      }
      List<HermesProfile> profiles = config.profiles();
      long healthyCount = profiles.stream()
          .filter(profile -> !Boolean.FALSE.equals(profile.gatewayHealthy())
              && !Boolean.FALSE.equals(activeProviderHealthy(profile)))
          .count();
      HermesProfile chatProfile = profiles.stream()
          .filter(profile -> "chat".equals(profile.id()))
          .findFirst()
          .orElse(profiles.isEmpty() ? null : profiles.get(0));
      String status;
      String error = null;
      if (profiles.isEmpty()) {
        status = "DEGRADED";
        error = "No Hermes profiles configured";
      } else if (healthyCount == profiles.size()) {
        status = "UP";
      } else if (healthyCount > 0) {
        status = "DEGRADED";
        error = hermesProfileErrors(profiles);
      } else {
        status = "DOWN";
        error = hermesProfileErrors(profiles);
      }
      long responseTimeMs = Math.max(1, Math.round((System.nanoTime() - startedNanos) / 1_000_000.0));
      boolean fallbackActive = chatProfile != null
          && config.fallback() != null
          && config.fallback().provider() != null
          && config.fallback().provider().equalsIgnoreCase(chatProfile.activeProvider())
          && "openai".equalsIgnoreCase(chatProfile.provider());
      boolean globalOverride = config.globalOverride() != null
          && Boolean.TRUE.equals(config.globalOverride().enabled());
      String activeBaseUrl = (fallbackActive || globalOverride) && config.fallback() != null
          ? config.fallback().baseUrl()
          : chatProfile == null ? null : chatProfile.baseUrl();
      String activeModel = (fallbackActive || globalOverride) && config.fallback() != null
          ? config.fallback().model()
          : chatProfile == null ? null : chatProfile.model();
      if (globalOverride) {
        String overrideDetail = "GLOBAL_OVERRIDE active: all AI routes forced to local backend";
        error = error == null || error.isBlank() ? overrideDetail : error + "; " + overrideDetail;
      } else if (fallbackActive) {
        error = firstNonBlank(
            firstNonBlank(chatProfile.fallbackReason(), chatProfile.lastProviderError()),
            error);
      }
      return new SystemComponentStatus(
          "bot-hermes",
          status,
          "HERMES_MANAGER",
          "managed-profiles",
          null,
          "profiles healthy " + healthyCount + "/" + profiles.size(),
          activeBaseUrl,
          chatProfile == null ? null : firstNonBlank(chatProfile.activeProvider(), chatProfile.provider()),
          null,
          activeModel,
          null,
          null,
          null,
          null,
          responseTimeMs,
          checkedAt,
          null,
          null,
          null,
          null,
          null,
          null,
          null,
          error);
    } catch (RuntimeException ignored) {
      return null;
    }
  }

  private Boolean activeProviderHealthy(HermesProfile profile) {
    if (profile == null) {
      return false;
    }
    return "ollama".equalsIgnoreCase(profile.activeProvider())
        ? profile.fallbackHealthy()
        : profile.primaryProviderHealthy();
  }

  private String hermesProfileErrors(List<HermesProfile> profiles) {
    return profiles.stream()
        .filter(profile -> Boolean.FALSE.equals(profile.gatewayHealthy())
            || Boolean.FALSE.equals(activeProviderHealthy(profile)))
        .map(profile -> firstNonBlank(
            firstNonBlank(profile.fallbackReason(), profile.lastProviderError()),
            firstNonBlank(profile.detail(), profile.id() + " profile is unhealthy")))
        .filter(value -> value != null && !value.isBlank())
        .findFirst()
        .orElse(null);
  }

  private SystemComponentStatus sidecarComponentStatus(String name, String containerName, Instant checkedAt) {
    long startedNanos = System.nanoTime();
    ContainerStatus containerStatus = containerStatusProvider.getStatus(containerName);
    long responseTimeMs = Math.max(1, Math.round((System.nanoTime() - startedNanos) / 1_000_000.0));
    return new SystemComponentStatus(
        name,
        effectiveSidecarStatus(containerStatus),
        "SIDECAR",
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        containerStatus.startedAt() == null ? null : Math.max(0, Instant.now().getEpochSecond() - containerStatus.startedAt().getEpochSecond()),
        containerStatus.startedAt(),
        null,
        null,
        responseTimeMs,
        checkedAt,
        containerStatus.containerName(),
        containerStatus.state(),
        containerStatus.statusText(),
        containerStatus.image(),
        containerStatus.startedAt(),
        containerStatus.restartCount(),
        containerStatus.error(),
        containerStatus.error());
  }

  private String effectiveSpringBootStatus(String appStatus, ContainerStatus containerStatus) {
    if (isContainerDisabled(containerStatus)) {
      return appStatus == null ? "UNKNOWN" : appStatus;
    }
    if (isContainerStatusUnavailable(containerStatus)) {
      return appStatus == null ? "UNKNOWN" : appStatus;
    }
    if (!isContainerRunning(containerStatus)) {
      return "DOWN";
    }
    return "UP".equalsIgnoreCase(appStatus) ? "UP" : "DEGRADED";
  }

  private String effectiveSidecarStatus(ContainerStatus containerStatus) {
    if (isContainerDisabled(containerStatus)) {
      return "UNKNOWN";
    }
    if (isContainerStatusUnavailable(containerStatus)) {
      return "UNKNOWN";
    }
    String state = containerStatus.state();
    if ("running".equalsIgnoreCase(state)) {
      return "UP";
    }
    if ("restarting".equalsIgnoreCase(state)) {
      return "DEGRADED";
    }
    return "DOWN";
  }

  private boolean isContainerDisabled(ContainerStatus containerStatus) {
    return containerStatus == null || "disabled".equalsIgnoreCase(containerStatus.state());
  }

  private boolean isContainerStatusUnavailable(ContainerStatus containerStatus) {
    return containerStatus != null
        && "unknown".equalsIgnoreCase(containerStatus.state())
        && containerStatus.error() != null
        && !containerStatus.error().isBlank();
  }

  private boolean isContainerRunning(ContainerStatus containerStatus) {
    return containerStatus != null && "running".equalsIgnoreCase(containerStatus.state());
  }

  private ContainerStatus externalContainerStatus(String containerName) {
    ContainerStatus containerStatus = containerStatusProvider.getStatus(containerName);
    if (containerStatus == null || isContainerDisabled(containerStatus)) {
      return null;
    }
    String state = containerStatus.state();
    if ("missing".equalsIgnoreCase(state) || isContainerStatusUnavailable(containerStatus)) {
      return null;
    }
    return containerStatus;
  }

  private String openClawDeploymentMode() {
    String mode = properties.getOpenclawDeploymentMode();
    if (mode == null || mode.isBlank()) {
      return "external";
    }
    return mode.trim().toLowerCase();
  }

  private String openClawHealthUrl() {
    String configuredHealthUrl = properties.getOpenclawHealthUrl();
    if (configuredHealthUrl != null && !configuredHealthUrl.isBlank()) {
      return configuredHealthUrl.trim();
    }
    return healthUrlFromGatewayUrl(properties.getOpenclawGatewayWsUrl());
  }

  private OpenClawRuntimeSelection openClawRuntimeSelection() {
    try {
      ResponseEntity<OpenClawSettingsResponse> response = engineClient.getOpenClawSettings();
      OpenClawSettingsResponse body = response.getBody();
      if (response.getStatusCode().is2xxSuccessful()
          && body != null
          && body.currentWsUrl() != null
          && !body.currentWsUrl().isBlank()) {
        String healthUrl = body.currentHealthUrl();
        if (healthUrl == null || healthUrl.isBlank()) {
          healthUrl = healthUrlFromGatewayUrl(body.currentWsUrl());
        }
        return new OpenClawRuntimeSelection(body.currentWsUrl(), healthUrl);
      }
    } catch (RuntimeException ignored) {
      // Keep System usable even when bot-engine is down.
    }
    return new OpenClawRuntimeSelection(properties.getOpenclawGatewayWsUrl(), openClawHealthUrl());
  }

  private String healthUrlFromGatewayUrl(String gatewayUrl) {
    if (gatewayUrl == null || gatewayUrl.isBlank()) {
      return null;
    }
    try {
      URI uri = new URI(gatewayUrl.trim());
      String scheme = switch (uri.getScheme() == null ? "" : uri.getScheme().toLowerCase()) {
        case "ws", "http" -> "http";
        case "wss", "https" -> "https";
        default -> throw new IllegalArgumentException("Unsupported OpenClaw gateway URL scheme: " + uri.getScheme());
      };
      return new URI(
          scheme,
          uri.getUserInfo(),
          uri.getHost(),
          uri.getPort(),
          "/health",
          null,
          null).toString();
    } catch (URISyntaxException | IllegalArgumentException e) {
      throw new IllegalStateException("Invalid OpenClaw gateway URL: " + gatewayUrl, e);
    }
  }

  private GatewayHealthResult getGatewayHealth(String healthUrl, String componentName) {
    try {
      ResponseEntity<Map> response = restTemplate.getForEntity(healthUrl, Map.class);
      Map<?, ?> body = response.getBody();
      if (!response.getStatusCode().is2xxSuccessful() || body == null) {
        throw new IllegalStateException("Invalid " + componentName + " health response");
      }
      String rawStatus = firstNonBlank(stringValue(body.get("status")), stringValue(body.get("state")));
      Boolean ok = booleanValue(body.get("ok"));
      String healthStatus = firstNonBlank(rawStatus, ok == null ? null : ok.toString());
      if (Boolean.FALSE.equals(ok)) {
        return new GatewayHealthResult("DEGRADED", healthStatus, componentName + " health returned ok=false");
      }
      if (rawStatus == null || rawStatus.isBlank()) {
        return new GatewayHealthResult("UP", healthStatus, null);
      }
      if ("UP".equalsIgnoreCase(rawStatus)
          || "ok".equalsIgnoreCase(rawStatus)
          || "live".equalsIgnoreCase(rawStatus)
          || "running".equalsIgnoreCase(rawStatus)) {
        return new GatewayHealthResult("UP", rawStatus, null);
      }
      return new GatewayHealthResult("DEGRADED", rawStatus, componentName + " health status is " + rawStatus);
    } catch (RestClientException e) {
      throw new IllegalStateException(e.getMessage(), e);
    }
  }

  private String healthUrlFromBaseUrl(String baseUrl) {
    if (baseUrl == null || baseUrl.isBlank()) {
      return null;
    }
    try {
      URI uri = new URI(baseUrl.trim());
      return new URI(
          uri.getScheme(),
          uri.getUserInfo(),
          uri.getHost(),
          uri.getPort(),
          "/health",
          null,
          null).toString();
    } catch (URISyntaxException | IllegalArgumentException e) {
      throw new IllegalStateException("Invalid Hermes base URL: " + baseUrl, e);
    }
  }

  private Map<?, ?> getActuatorMap(String baseUrl, String path) {
    String url = UriComponentsBuilder
        .fromUriString(baseUrl)
        .path(path)
        .build()
        .toUriString();
    try {
      ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
      Map<?, ?> body = response.getBody();
      if (!response.getStatusCode().is2xxSuccessful() || body == null) {
        throw new IllegalStateException("Invalid response from " + path);
      }
      return body;
    } catch (RestClientException e) {
      throw new IllegalStateException(e.getMessage(), e);
    }
  }

  private Double metricValue(String baseUrl, String metricName) {
    Map<?, ?> response = getActuatorMap(baseUrl, "/actuator/metrics/" + metricName);
    Object measurementsObject = response.get("measurements");
    if (!(measurementsObject instanceof List<?> measurements) || measurements.isEmpty()) {
      throw new IllegalStateException("Missing metric " + metricName);
    }
    Map<?, ?> firstMeasurement = mapValue(measurements.getFirst());
    if (firstMeasurement == null) {
      throw new IllegalStateException("Invalid metric " + metricName);
    }
    Object value = firstMeasurement.get("value");
    if (value instanceof Number number) {
      return number.doubleValue();
    }
    throw new IllegalStateException("Invalid metric value " + metricName);
  }

  private Long optionalMetricCount(String baseUrl, String metricName) {
    try {
      return Math.round(metricValue(baseUrl, metricName));
    } catch (Exception e) {
      return null;
    }
  }

  private Long localCounter(String metricName) {
    try {
      return Math.round(meterRegistry.get(metricName).counter().count());
    } catch (Exception e) {
      return null;
    }
  }

  private Instant instantFromEpochSeconds(Double epochSeconds) {
    if (epochSeconds == null) {
      return null;
    }
    long epochMillis = Math.round(epochSeconds * 1000.0);
    return Instant.ofEpochMilli(epochMillis);
  }

  private String activeProfiles() {
    String[] profiles = environment.getActiveProfiles();
    if (profiles.length == 0) {
      return "default";
    }
    return String.join(",", profiles);
  }

  private Map<?, ?> mapValue(Object value) {
    if (value instanceof Map<?, ?> map) {
      return map;
    }
    return null;
  }

  private String stringValue(Object value) {
    return value == null ? null : value.toString();
  }

  private Boolean booleanValue(Object value) {
    if (value instanceof Boolean booleanValue) {
      return booleanValue;
    }
    if (value instanceof String stringValue) {
      if ("true".equalsIgnoreCase(stringValue)) {
        return true;
      }
      if ("false".equalsIgnoreCase(stringValue)) {
        return false;
      }
    }
    return null;
  }

  private String firstNonBlank(String first, String second) {
    if (first != null && !first.isBlank()) {
      return first;
    }
    return second != null && !second.isBlank() ? second : null;
  }

  private record GatewayHealthResult(
      String componentStatus,
      String healthStatus,
      String error) {
  }

  private record OpenClawRuntimeSelection(String wsUrl, String healthUrl) {
  }
}
