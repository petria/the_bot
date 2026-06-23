package org.freakz.engine.services.ai.hermes;

import org.freakz.common.model.engine.system.HermesBackendConfigResponse;
import org.freakz.common.model.engine.system.HermesFallbackProfileStatus;
import org.freakz.common.model.engine.system.HermesFallbackSettingsResponse;
import org.freakz.common.model.engine.system.HermesGlobalOverrideSettings;
import org.freakz.common.model.engine.system.HermesProfile;
import org.freakz.common.spring.rest.RestHermesManagerClient;
import org.freakz.engine.dto.ai.AiRoutesResponse;
import org.freakz.engine.services.api.ServiceMessageHandlerMethod;
import org.freakz.engine.services.api.ServiceRequest;
import org.freakz.engine.services.api.ServiceRequestType;
import org.freakz.engine.services.api.SpringServiceMethodHandler;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@SpringServiceMethodHandler
public class AiRoutesStatusService {

  private static final int MAX_MODEL_CHARS = 48;
  private static final int MAX_BASE_URL_CHARS = 72;
  private static final String CHAT_ROUTE_ID = "chat";
  private static final String AI_COMMAND_ROUTE_ID = "ai-command";
  private static final String CODER_ROUTE_ID = "coder";

  private final HermesSettingsService hermesSettingsService;
  private final RestHermesManagerClient hermesManagerClient;

  public AiRoutesStatusService(HermesSettingsService hermesSettingsService, RestHermesManagerClient hermesManagerClient) {
    this.hermesSettingsService = hermesSettingsService;
    this.hermesManagerClient = hermesManagerClient;
  }

  @ServiceMessageHandlerMethod(ServiceRequestType = ServiceRequestType.AiRoutesStatus)
  public AiRoutesResponse handleAiRoutesStatus(ServiceRequest request) {
    return new AiRoutesResponse(formatRoutes());
  }

  List<String> formatRoutes() {
    Map<String, HermesProfile> profiles = loadProfiles();
    HermesFallbackSettingsResponse fallback = loadFallback();
    HermesGlobalOverrideSettings globalOverride = loadGlobalOverride();
    List<String> lines = new ArrayList<>();

    if (globalOverride != null && Boolean.TRUE.equals(globalOverride.enabled())) {
      lines.add("GLOBAL_OVERRIDE: %s provider=%s model=%s base=%s".formatted(
          health(globalOverride.healthy()),
          valueOrUnknown(globalOverride.provider()),
          shortValue(globalOverride.model(), MAX_MODEL_CHARS),
          shortValue(globalOverride.baseUrl(), MAX_BASE_URL_CHARS)));
    }
    lines.add(formatRuntimeRoute(
        "chat",
        hermesSettingsService.resolveSettings(),
        profiles.get(CHAT_ROUTE_ID),
        fallback));
    lines.add(formatRuntimeRoute(
        "ai-command",
        hermesSettingsService.resolveAiCommandSettings(),
        profiles.get(AI_COMMAND_ROUTE_ID),
        fallback));
    lines.add(formatFallback(fallback));

    HermesProfile coderProfile = profiles.get(CODER_ROUTE_ID);
    if (coderProfile != null) {
      lines.add(formatProfileOnlyRoute(coderProfile));
    }
    return lines;
  }

  private Map<String, HermesProfile> loadProfiles() {
    if (hermesManagerClient == null) {
      return Map.of();
    }
    try {
      HermesBackendConfigResponse response = hermesManagerClient.getBackendConfig().getBody();
      if (response == null || response.profiles() == null) {
        return Map.of();
      }
      return response.profiles().stream()
          .filter(Objects::nonNull)
          .filter(profile -> profile.id() != null && !profile.id().isBlank())
          .collect(Collectors.toMap(
              profile -> profile.id().trim(),
              Function.identity(),
              (left, right) -> left));
    } catch (Exception e) {
      return Map.of();
    }
  }

  private HermesFallbackSettingsResponse loadFallback() {
    if (hermesManagerClient == null) {
      return null;
    }
    try {
      return hermesManagerClient.getFallback().getBody();
    } catch (Exception e) {
      return null;
    }
  }

  private HermesGlobalOverrideSettings loadGlobalOverride() {
    if (hermesManagerClient == null) {
      return null;
    }
    try {
      HermesBackendConfigResponse response = hermesManagerClient.getBackendConfig().getBody();
      return response == null ? null : response.globalOverride();
    } catch (Exception e) {
      return null;
    }
  }

  private String formatRuntimeRoute(
      String label,
      HermesSettings settings,
      HermesProfile profile,
      HermesFallbackSettingsResponse fallback) {
    String provider = firstNonBlank(
        profile == null ? null : profile.activeProvider(),
        profileProvider(profile),
        "unknown");
    String health = profile == null ? "UNKNOWN" : health(profile.gatewayHealthy());
    String tools = profile == null ? "unknown" : yesNoUnknown(profile.toolCapable());
    String source = profile == null ? "local" : "manager";
    boolean fallbackActive = profile != null
        && "openai".equalsIgnoreCase(profile.provider())
        && fallback != null
        && fallback.provider() != null
        && fallback.provider().equalsIgnoreCase(profile.activeProvider());
    String model = fallbackActive && fallback != null
        ? fallback.model()
        : profile == null ? settings.model() : profile.model();

    return "%s: %s source=%s provider=%s model=%s api=%s tools=%s base=%s".formatted(
        label,
        health,
        source,
        provider,
        shortValue(model, MAX_MODEL_CHARS),
        valueOrUnknown(settings.apiMode()),
        tools,
        shortValue(settings.baseUrl(), MAX_BASE_URL_CHARS));
  }

  private String formatFallback(HermesFallbackSettingsResponse fallback) {
    if (fallback == null) {
      return "fallback: UNKNOWN";
    }
    if (!Boolean.TRUE.equals(fallback.enabled())) {
      return "fallback: off";
    }
    String profiles = formatFallbackProfiles(fallback.profiles());
    String suffix = profiles.isBlank() ? "" : " profiles=" + profiles;
    return "fallback: on provider=%s model=%s base=%s%s".formatted(
        valueOrUnknown(fallback.provider()),
        shortValue(fallback.model(), MAX_MODEL_CHARS),
        shortValue(fallback.baseUrl(), MAX_BASE_URL_CHARS),
        suffix);
  }

  private String formatFallbackProfiles(List<HermesFallbackProfileStatus> profiles) {
    if (profiles == null || profiles.isEmpty()) {
      return "";
    }
    return profiles.stream()
        .filter(Objects::nonNull)
        .sorted(Comparator.comparing(HermesFallbackProfileStatus::profileId, Comparator.nullsLast(String::compareTo)))
        .map(profile -> valueOrUnknown(profile.profileId()) + ":" + (profile.healthy() ? "UP" : "DOWN"))
        .collect(Collectors.joining(","));
  }

  private String formatProfileOnlyRoute(HermesProfile profile) {
    return "profile %s: %s provider=%s model=%s api=%s tools=%s base=%s".formatted(
        valueOrUnknown(profile.id()),
        health(profile.healthy()),
        valueOrUnknown(profileProvider(profile)),
        shortValue(profile.model(), MAX_MODEL_CHARS),
        valueOrUnknown(profile.apiMode()),
        yesNoUnknown(profile.toolCapable()),
        shortValue(profile.baseUrl(), MAX_BASE_URL_CHARS));
  }

  private boolean fallbackMatches(HermesSettings settings, HermesFallbackSettingsResponse fallback) {
    if (settings == null || fallback == null || !Boolean.TRUE.equals(fallback.enabled())) {
      return false;
    }
    return sameNormalized(settings.baseUrl(), fallback.baseUrl())
        || sameText(settings.model(), fallback.model());
  }

  private String inferFallbackProvider(HermesSettings settings, HermesFallbackSettingsResponse fallback) {
    return fallbackMatches(settings, fallback) ? fallback.provider() : null;
  }

  private boolean sameNormalized(String left, String right) {
    String normalizedLeft = normalizeApiRoot(left);
    String normalizedRight = normalizeApiRoot(right);
    return normalizedLeft != null && normalizedLeft.equalsIgnoreCase(normalizedRight);
  }

  private boolean sameText(String left, String right) {
    return left != null && right != null && left.trim().equalsIgnoreCase(right.trim());
  }

  private String normalizeApiRoot(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    return value.trim().replaceFirst("/+$", "").replaceFirst("(?i)/v1$", "");
  }

  private String profileProvider(HermesProfile profile) {
    return profile == null ? null : profile.provider();
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

  private String firstNonBlank(String... values) {
    if (values == null) {
      return null;
    }
    for (String value : values) {
      if (value != null && !value.isBlank()) {
        return value.trim().toLowerCase(Locale.ROOT);
      }
    }
    return null;
  }

  private String shortValue(String value, int maxChars) {
    String safe = valueOrUnknown(value);
    if (safe.length() <= maxChars) {
      return safe;
    }
    return safe.substring(0, Math.max(0, maxChars - 1)).trim() + "...";
  }

  private String valueOrUnknown(String value) {
    return Optional.ofNullable(value)
        .map(String::trim)
        .filter(candidate -> !candidate.isBlank())
        .orElse("unknown");
  }
}
