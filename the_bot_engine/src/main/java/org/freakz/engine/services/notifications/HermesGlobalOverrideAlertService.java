package org.freakz.engine.services.notifications;

import java.time.Instant;

import org.freakz.common.model.engine.system.HermesGlobalOverrideSettings;
import org.springframework.stereotype.Service;

@Service
public class HermesGlobalOverrideAlertService {

  private final PrivateChatAlertService alertService;

  public HermesGlobalOverrideAlertService(PrivateChatAlertService alertService) {
    this.alertService = alertService;
  }

  public void notifyStateChange(
      HermesGlobalOverrideSettings before,
      HermesGlobalOverrideSettings after) {
    boolean wasEnabled = before != null && Boolean.TRUE.equals(before.enabled());
    boolean isEnabled = after != null && Boolean.TRUE.equals(after.enabled());
    if (wasEnabled == isEnabled) {
      return;
    }
    String actor = value(after == null ? null : after.updatedBy(), "unknown");
    if (isEnabled) {
      alertService.sendAlertToConfiguredTargets(
          "ALERT: AI global local override enabled by %s. Forced route: %s model=%s endpoint=%s"
              .formatted(
                  actor,
                  displayProvider(after.provider()),
                  value(after.model(), "unknown"),
                  value(after.baseUrl(), "unknown"))
              + " at " + Instant.now());
    } else {
      alertService.sendAlertToConfiguredTargets(
          "ALERT: AI global local override disabled by %s. Normal AI routing restored."
              .formatted(actor)
              + " at " + Instant.now());
    }
  }

  private String displayProvider(String provider) {
    return switch (value(provider, "local LLM").toLowerCase()) {
      case "ollama" -> "Ollama";
      case "lmstudio" -> "LM Studio";
      case "vllm" -> "vLLM";
      default -> value(provider, "local LLM");
    };
  }

  private String value(String value, String fallback) {
    return value == null || value.isBlank() ? fallback : value;
  }
}
