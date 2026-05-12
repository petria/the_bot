package org.freakz.web.controller;

import java.lang.management.ManagementFactory;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.micrometer.core.instrument.MeterRegistry;
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

  public SystemController(
      RestTemplate restTemplate,
      TheBotWebProperties properties,
      Environment environment,
      Optional<BuildProperties> buildProperties,
      MeterRegistry meterRegistry,
      ContainerStatusProvider containerStatusProvider) {
    this.restTemplate = restTemplate;
    this.properties = properties;
    this.environment = environment;
    this.buildProperties = buildProperties;
    this.meterRegistry = meterRegistry;
    this.containerStatusProvider = containerStatusProvider;
  }

  @GetMapping("/status")
  public SystemStatusResponse getStatus() {
    Instant checkedAt = Instant.now();
    List<SystemComponentStatus> components = new ArrayList<>();
    components.add(localComponentStatus(checkedAt));
    components.add(remoteComponentStatus("bot-io", properties.getBotIoBaseUrl(), checkedAt));
    components.add(remoteComponentStatus("bot-engine", properties.getBotEngineBaseUrl(), checkedAt));
    components.add(sidecarComponentStatus("bot-openclaw", properties.getBotOpenclawContainerName(), checkedAt));
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
    if (!isContainerRunning(containerStatus)) {
      return "DOWN";
    }
    return "UP".equalsIgnoreCase(appStatus) ? "UP" : "DEGRADED";
  }

  private String effectiveSidecarStatus(ContainerStatus containerStatus) {
    if (isContainerDisabled(containerStatus)) {
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

  private boolean isContainerRunning(ContainerStatus containerStatus) {
    return containerStatus != null && "running".equalsIgnoreCase(containerStatus.state());
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

  private String firstNonBlank(String first, String second) {
    if (first != null && !first.isBlank()) {
      return first;
    }
    return second != null && !second.isBlank() ? second : null;
  }

  public record SystemStatusResponse(
      Instant checkedAt,
      List<SystemComponentStatus> components) {
  }

  public record SystemComponentStatus(
      String name,
      String status,
      String componentType,
      String baseUrl,
      String profiles,
      String version,
      String artifact,
      Long uptimeSeconds,
      Instant startedAt,
      Long receivedCalls,
      Long requestedCalls,
      Long responseTimeMs,
      Instant checkedAt,
      String containerName,
      String containerState,
      String containerStatusText,
      String image,
      Instant containerStartedAt,
      Long restartCount,
      String containerError,
      String error) {
  }
}
