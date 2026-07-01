package org.freakz.engine.services.buildinfo;

import org.freakz.common.model.system.SystemComponentStatus;
import org.freakz.common.model.system.SystemStatusResponse;
import org.freakz.common.spring.rest.RestBotWebSystemClient;
import org.freakz.engine.config.ConfigService;
import org.freakz.engine.services.ai.claw.BotInstanceIdentityService;
import org.freakz.engine.services.api.*;
import org.springframework.boot.info.BuildProperties;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;

import java.util.Optional;
import java.util.stream.Collectors;

@Service
@SpringServiceMethodHandler
public class BuildInfoService extends AbstractSpringService {

  private final Optional<BuildProperties> buildProperties;
  private final RestBotWebSystemClient botWebSystemClient;
  private final BotInstanceIdentityService botInstanceIdentityService;
  private final ConfigService configService;

  public BuildInfoService(
      Optional<BuildProperties> buildProperties,
      RestBotWebSystemClient botWebSystemClient,
      BotInstanceIdentityService botInstanceIdentityService,
      ConfigService configService) {
    this.buildProperties = buildProperties;
    this.botWebSystemClient = botWebSystemClient;
    this.botInstanceIdentityService = botInstanceIdentityService;
    this.configService = configService;
  }

  @ServiceMessageHandlerMethod(ServiceRequestType = ServiceRequestType.BotInfoQuery)
  public ServiceResponse handleBotInfoQuery(ServiceRequest request) {
    ServiceResponse response = new ServiceResponse();
    response.setStatus(formatBotInfo());
    return response;
  }

  @ServiceMessageHandlerMethod(ServiceRequestType = ServiceRequestType.BuildInfoQuery)
  public ServiceResponse handleBuildInfoQuery(ServiceRequest request) {
    return handleBotInfoQuery(request);
  }

  private String formatBotInfo() {
    String instanceId = safeInstanceId();
    String activeProfile = configService.getActiveProfile();
    try {
      ResponseEntity<SystemStatusResponse> response = botWebSystemClient.getSystemStatus();
      SystemStatusResponse status = response.getBody();
      if (!response.getStatusCode().is2xxSuccessful() || status == null || status.components() == null) {
        throw new IllegalStateException("Invalid bot-web system status response");
      }
      String moduleLines = status.components().stream()
          .map(this::formatComponent)
          .collect(Collectors.joining("\n"));
      return "== BOT INFO ==\n"
          + "Instance: " + instanceId + " profile=" + valueOrDash(activeProfile) + "\n"
          + moduleLines;
    } catch (RestClientException | IllegalStateException e) {
      return "== BOT INFO ==\n"
          + "Instance: " + instanceId + " profile=" + valueOrDash(activeProfile) + "\n"
          + "bot-engine " + localBuildInfo() + "\n"
          + "System status unavailable: " + e.getMessage();
    }
  }

  private String formatComponent(SystemComponentStatus component) {
    if ("bot-hermes".equals(component.name())) {
      return component.name()
          + " " + valueOrDash(component.status())
          + " " + valueOrDash(component.componentType())
          + optional(" provider=", component.profiles())
          + optional(" model=", component.artifact())
          + optional(" endpoint=", component.baseUrl())
          + optional(" health=", component.healthStatus())
          + optional(" error=", firstNonBlank(component.error(), component.containerError()));
    }

    return component.name()
        + " " + valueOrDash(component.status())
        + " " + valueOrDash(firstNonBlank(component.artifact(), component.componentType()))
        + " version=" + valueOrDash(component.version())
        + " uptime=" + formatDuration(component.uptimeSeconds())
        + optional(" route=", firstNonBlank(component.profiles(), component.runtimeMode()))
        + optional(" calls.in=", component.receivedCalls())
        + optional(" calls.out=", component.requestedCalls())
        + optional(" error=", firstNonBlank(component.error(), component.containerError()));
  }

  private String localBuildInfo() {
    return buildProperties
        .map(build -> valueOrDash(build.getArtifact())
            + " version=" + valueOrDash(build.getVersion())
            + " java=" + valueOrDash(build.get("java.version"))
            + " os=" + valueOrDash(build.get("os.name")))
        .orElse("version=n/a");
  }

  private String safeInstanceId() {
    try {
      return botInstanceIdentityService.getInstanceId();
    } catch (RuntimeException e) {
      return "unknown";
    }
  }

  private String formatDuration(Long seconds) {
    if (seconds == null) {
      return "-";
    }
    long days = seconds / 86_400;
    long hours = (seconds % 86_400) / 3_600;
    long minutes = (seconds % 3_600) / 60;
    if (days > 0) {
      return days + "d" + hours + "h";
    }
    if (hours > 0) {
      return hours + "h" + minutes + "m";
    }
    return minutes + "m";
  }

  private String optional(String label, Object value) {
    if (value == null || value.toString().isBlank()) {
      return "";
    }
    return label + value;
  }

  private String firstNonBlank(String first, String second) {
    if (first != null && !first.isBlank()) {
      return first;
    }
    return second != null && !second.isBlank() ? second : null;
  }

  private String valueOrDash(String value) {
    return value == null || value.isBlank() ? "-" : value;
  }
}
