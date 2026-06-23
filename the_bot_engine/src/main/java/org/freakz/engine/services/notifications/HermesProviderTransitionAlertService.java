package org.freakz.engine.services.notifications;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.freakz.common.model.engine.system.HermesBackendConfigResponse;
import org.freakz.common.model.engine.system.HermesFallbackSettingsResponse;
import org.freakz.common.model.engine.system.HermesProfile;
import org.freakz.common.spring.rest.RestHermesManagerClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class HermesProviderTransitionAlertService {

  private static final Logger log = LoggerFactory.getLogger(HermesProviderTransitionAlertService.class);

  private final RestHermesManagerClient managerClient;
  private final PrivateChatAlertService alertService;
  private final Map<String, ProviderState> previousStates = new ConcurrentHashMap<>();
  private Boolean previousOverrideHealthy;

  public HermesProviderTransitionAlertService(
      RestHermesManagerClient managerClient,
      PrivateChatAlertService alertService) {
    this.managerClient = managerClient;
    this.alertService = alertService;
  }

  @Scheduled(
      initialDelayString = "${the.bot.hermes-status.initial-delay-ms:15000}",
      fixedDelayString = "${the.bot.hermes-status.poll-interval-ms:30000}")
  public void poll() {
    try {
      HermesBackendConfigResponse response = managerClient.getBackendConfig().getBody();
      if (response == null || response.profiles() == null) {
        return;
      }
      if (response.globalOverride() != null && Boolean.TRUE.equals(response.globalOverride().enabled())) {
        pollGlobalOverrideHealth(response);
        return;
      }
      previousOverrideHealthy = null;
      for (HermesProfile profile : response.profiles()) {
        if (profile == null || profile.id() == null || profile.activeProvider() == null) {
          continue;
        }
        ProviderState current = new ProviderState(
            profile.activeProvider(),
            profile.fallbackActivatedAt(),
            profile.fallbackReason());
        ProviderState previous = previousStates.put(profile.id(), current);
        if (previous == null || previous.provider().equalsIgnoreCase(current.provider())) {
          continue;
        }
        String fallbackProvider = response.fallback() == null ? null : response.fallback().provider();
        if (fallbackProvider != null && fallbackProvider.equalsIgnoreCase(current.provider())) {
          alertService.sendAlertToConfiguredTargets(fallbackAlert(profile, response.fallback()));
        } else if (fallbackProvider != null && fallbackProvider.equalsIgnoreCase(previous.provider())
            && "openai".equalsIgnoreCase(current.provider())) {
          alertService.sendAlertToConfiguredTargets(recoveryAlert(profile, previous));
        }
      }
    } catch (Exception e) {
      log.debug("Could not poll Hermes provider state: {}", e.getMessage());
    }
  }

  private void pollGlobalOverrideHealth(HermesBackendConfigResponse response) {
    Boolean healthy = response.globalOverride().healthy();
    if (Boolean.FALSE.equals(healthy) && !Boolean.FALSE.equals(previousOverrideHealthy)) {
      alertService.sendAlertToConfiguredTargets(
          "ALERT: AI global local override backend is unavailable. AI requests are failing closed. "
              + "Provider=%s model=%s endpoint=%s"
                  .formatted(
                      displayProvider(firstNonBlank(response.globalOverride().provider(), "local LLM")),
                      firstNonBlank(response.globalOverride().model(), "unknown"),
                      firstNonBlank(response.globalOverride().baseUrl(), "unknown")));
    } else if (Boolean.TRUE.equals(healthy) && Boolean.FALSE.equals(previousOverrideHealthy)) {
      alertService.sendAlertToConfiguredTargets(
          "ALERT: AI global local override backend has recovered. Forced local routing remains active.");
    }
    previousOverrideHealthy = healthy;
  }

  private String fallbackAlert(HermesProfile profile, HermesFallbackSettingsResponse fallback) {
    String endpoint = fallback == null ? "unknown" : fallback.baseUrl();
    String model = fallback == null ? "unknown" : fallback.model();
    String provider = fallback == null
        ? "local LLM"
        : displayProvider(firstNonBlank(fallback.provider(), "local LLM"));
    return "ALERT: Hermes %s switched to %s. Reason: %s. Fallback: %s at %s"
        .formatted(
            profile.id(),
            provider,
            firstNonBlank(profile.fallbackReason(), profile.lastProviderError(), "OpenAI unavailable"),
            firstNonBlank(model, "unknown"),
            firstNonBlank(endpoint, "unknown"));
  }

  private String recoveryAlert(HermesProfile profile, ProviderState previous) {
    String duration = fallbackDuration(previous.activatedAt());
    return "ALERT: Hermes %s restored to OpenAI%s"
        .formatted(profile.id(), duration.isBlank() ? "" : ". Fallback duration: " + duration);
  }

  private String fallbackDuration(String activatedAt) {
    if (activatedAt == null || activatedAt.isBlank()) {
      return "";
    }
    try {
      Duration duration = Duration.between(Instant.parse(activatedAt), Instant.now());
      long minutes = Math.max(0, duration.toMinutes());
      return minutes < 60 ? minutes + " minutes" : "%dh %dm".formatted(minutes / 60, minutes % 60);
    } catch (Exception e) {
      return "";
    }
  }

  private String firstNonBlank(String... values) {
    for (String value : values) {
      if (value != null && !value.isBlank()) {
        return value;
      }
    }
    return "";
  }

  private String displayProvider(String provider) {
    return switch (provider.toLowerCase()) {
      case "ollama" -> "Ollama";
      case "lmstudio" -> "LM Studio";
      case "vllm" -> "vLLM";
      default -> provider;
    };
  }

  private record ProviderState(String provider, String activatedAt, String reason) {
  }
}
