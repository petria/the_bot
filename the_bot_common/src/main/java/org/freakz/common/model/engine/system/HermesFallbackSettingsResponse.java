package org.freakz.common.model.engine.system;

import java.util.List;

public record HermesFallbackSettingsResponse(
    Boolean enabled,
    String baseUrl,
    String model,
    List<HermesFallbackProfileStatus> profiles,
    Integer contextWindow,
    Boolean healthy,
    Boolean toolCapable,
    String detail,
    String lastValidatedAt,
    String validationStatus) {

  public HermesFallbackSettingsResponse(
      Boolean enabled,
      String baseUrl,
      String model,
      List<HermesFallbackProfileStatus> profiles) {
    this(enabled, baseUrl, model, profiles, null, null, null, null, null, null);
  }
}
