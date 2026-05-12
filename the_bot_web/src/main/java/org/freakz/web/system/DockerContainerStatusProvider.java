package org.freakz.web.system;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.transport.DockerHttpClient;
import com.github.dockerjava.zerodep.ZerodepDockerHttpClient;
import org.freakz.web.config.TheBotWebProperties;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

@Service
public class DockerContainerStatusProvider implements ContainerStatusProvider {

  private final TheBotWebProperties properties;
  private DockerClient dockerClient;

  public DockerContainerStatusProvider(TheBotWebProperties properties) {
    this.properties = properties;
  }

  @Override
  public ContainerStatus getStatus(String containerName) {
    if (!properties.isDockerStatusEnabled()) {
      return ContainerStatus.disabled(containerName);
    }
    if (containerName == null || containerName.isBlank()) {
      return ContainerStatus.error(containerName, "Container name is blank");
    }
    try {
      Container container = findContainer(containerName);
      if (container == null) {
        return ContainerStatus.missing(containerName);
      }
      InspectContainerResponse inspected = client().inspectContainerCmd(container.getId()).exec();
      InspectContainerResponse.ContainerState state = inspected.getState();
      String stateName = state == null ? "unknown" : normalizeState(state);
      return new ContainerStatus(
          containerName,
          stateName,
          container.getStatus(),
          firstNonBlank(inspected.getConfig() == null ? null : inspected.getConfig().getImage(), container.getImage()),
          parseDockerInstant(state == null ? null : state.getStartedAt()),
          inspected.getRestartCount() == null ? null : inspected.getRestartCount().longValue(),
          null);
    } catch (Exception e) {
      return ContainerStatus.error(containerName, e.getMessage());
    }
  }

  private Container findContainer(String containerName) {
    List<Container> containers = client().listContainersCmd()
        .withShowAll(true)
        .exec();
    return containers.stream()
        .filter(container -> matchesName(container, containerName))
        .findFirst()
        .orElse(null);
  }

  private boolean matchesName(Container container, String expectedName) {
    String normalizedExpected = expectedName.startsWith("/") ? expectedName : "/" + expectedName;
    String[] names = container.getNames();
    return names != null && Arrays.stream(names).anyMatch(name -> normalizedExpected.equals(name));
  }

  private DockerClient client() {
    if (dockerClient == null) {
      DockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder()
          .withDockerHost(properties.getDockerHost())
          .build();
      DockerHttpClient httpClient = new ZerodepDockerHttpClient.Builder()
          .dockerHost(config.getDockerHost())
          .sslConfig(config.getSSLConfig())
          .build();
      dockerClient = DockerClientImpl.getInstance(config, httpClient);
    }
    return dockerClient;
  }

  private String normalizeState(InspectContainerResponse.ContainerState state) {
    if (Boolean.TRUE.equals(state.getRestarting())) {
      return "restarting";
    }
    if (Boolean.TRUE.equals(state.getRunning())) {
      return "running";
    }
    if (Boolean.TRUE.equals(state.getPaused())) {
      return "paused";
    }
    if (Boolean.TRUE.equals(state.getOOMKilled())) {
      return "oom-killed";
    }
    if (state.getStatus() != null && !state.getStatus().isBlank()) {
      return state.getStatus();
    }
    return "unknown";
  }

  private Instant parseDockerInstant(String value) {
    if (value == null || value.isBlank() || value.startsWith("0001-")) {
      return null;
    }
    try {
      return Instant.parse(value);
    } catch (Exception e) {
      return null;
    }
  }

  private String firstNonBlank(String first, String second) {
    if (first != null && !first.isBlank()) {
      return first;
    }
    return second != null && !second.isBlank() ? second : null;
  }
}
