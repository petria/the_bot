package org.freakz.common.model.engine.system;

import java.util.List;

public record HermesFallbackSettingsResponse(
    Boolean enabled,
    String provider,
    String baseUrl,
    String model,
    List<HermesFallbackProfileStatus> profiles,
    Integer contextWindow,
    Boolean healthy,
    Boolean toolCapable,
    String detail,
    String lastValidatedAt,
    String validationStatus,
    Boolean apiKeyConfigured) {

  public HermesFallbackSettingsResponse(
      Boolean enabled,
      String baseUrl,
      String model,
      List<HermesFallbackProfileStatus> profiles) {
    this(enabled, "ollama", baseUrl, model, profiles, null, null, null, null, null, null, false);
  }
}
