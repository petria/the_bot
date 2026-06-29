package org.freakz.engine.services.ai.hermes;

import java.util.List;
import java.util.Locale;

import org.freakz.common.model.engine.system.HermesBackend;
import org.freakz.common.model.engine.system.HermesBackendConfigResponse;
import org.freakz.common.model.engine.system.HermesRoute;
import org.freakz.common.spring.rest.RestHermesManagerClient;
import org.freakz.engine.dto.ai.AiRoutesResponse;
import org.freakz.engine.services.api.ServiceMessageHandlerMethod;
import org.freakz.engine.services.api.ServiceRequest;
import org.freakz.engine.services.api.ServiceRequestType;
import org.freakz.engine.services.api.SpringServiceMethodHandler;
import org.springframework.stereotype.Service;

@Service
@SpringServiceMethodHandler
public class AiRoutesStatusService {

  private static final int MAX_MODEL_CHARS = 48;
  private static final int MAX_BASE_URL_CHARS = 72;

  private final RestHermesManagerClient hermesManagerClient;

  public AiRoutesStatusService(HermesSettingsService hermesSettingsService, RestHermesManagerClient hermesManagerClient) {
    this.hermesManagerClient = hermesManagerClient;
  }

  @ServiceMessageHandlerMethod(ServiceRequestType = ServiceRequestType.AiRoutesStatus)
  public AiRoutesResponse handleAiRoutesStatus(ServiceRequest request) {
    return new AiRoutesResponse(formatRoutes());
  }

  List<String> formatRoutes() {
    HermesBackendConfigResponse config = loadConfig();
    if (config == null) {
      return List.of("ai: UNKNOWN manager unavailable");
    }
    List<String> lines = new java.util.ArrayList<>();
    lines.add("ai: mode=" + value(config.systemMode(), "enabled"));
    if (config.backends() != null) {
      for (HermesBackend backend : config.backends()) {
        lines.add("backend %s: %s provider=%s model=%s api=%s tools=%s base=%s".formatted(
            value(backend.id(), "unknown"),
            health(backend.healthy()),
            value(backend.provider(), "unknown"),
            shortValue(backend.model(), MAX_MODEL_CHARS),
            value(backend.apiMode(), "unknown"),
            yesNoUnknown(backend.toolCapable()),
            shortValue(backend.baseUrl(), MAX_BASE_URL_CHARS)));
      }
    }
    if (config.routes() != null) {
      for (HermesRoute route : config.routes()) {
        lines.add("route %s: backend=%s provider=%s model=%s status=%s".formatted(
            value(route.id(), "unknown"),
            value(route.backendId(), "unknown"),
            value(route.provider(), "unknown"),
            shortValue(route.model(), MAX_MODEL_CHARS),
            health(route.healthy())));
      }
    }
    return lines;
  }

  private HermesBackendConfigResponse loadConfig() {
    if (hermesManagerClient == null) {
      return null;
    }
    try {
      return hermesManagerClient.getBackendConfig().getBody();
    } catch (Exception e) {
      return null;
    }
  }

  private String health(Boolean healthy) {
    if (healthy == null) {
      return "UNKNOWN";
    }
    return healthy ? "UP" : "DOWN";
  }

  private String yesNoUnknown(Boolean value) {
    if (value == null) {
      return "unknown";
    }
    return value ? "yes" : "no";
  }

  private String shortValue(String value, int maxChars) {
    String safe = value(value, "unknown");
    if (safe.length() <= maxChars) {
      return safe;
    }
    return safe.substring(0, Math.max(0, maxChars - 1)).trim() + "...";
  }

  private String value(String value, String fallback) {
    return value == null || value.isBlank() ? fallback : value.trim().toLowerCase(Locale.ROOT);
  }
}
