package org.freakz.web.system;

import java.time.Instant;

public record ContainerStatus(
    String containerName,
    String state,
    String statusText,
    String image,
    Instant startedAt,
    Long restartCount,
    String error) {

  public static ContainerStatus disabled(String containerName) {
    return new ContainerStatus(containerName, "disabled", null, null, null, null, "Docker status is disabled");
  }

  public static ContainerStatus missing(String containerName) {
    return new ContainerStatus(containerName, "missing", null, null, null, null, "Container not found");
  }

  public static ContainerStatus error(String containerName, String error) {
    return new ContainerStatus(containerName, "unknown", null, null, null, null, error);
  }
}
