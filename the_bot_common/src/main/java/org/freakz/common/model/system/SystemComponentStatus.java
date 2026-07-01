package org.freakz.common.model.system;

import java.time.Instant;

public record SystemComponentStatus(
    String name,
    String status,
    String componentType,
    String runtimeMode,
    String healthUrl,
    String healthStatus,
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

  public SystemComponentStatus withOpenClawDetails(String runtimeMode, String baseUrl, String healthStatus) {
    return new SystemComponentStatus(
        name,
        status,
        componentType,
        runtimeMode,
        healthUrl,
        healthStatus,
        baseUrl,
        profiles,
        version,
        artifact,
        uptimeSeconds,
        startedAt,
        receivedCalls,
        requestedCalls,
        responseTimeMs,
        checkedAt,
        containerName,
        containerState,
        containerStatusText,
        image,
        containerStartedAt,
        restartCount,
        containerError,
        error);
  }
}
