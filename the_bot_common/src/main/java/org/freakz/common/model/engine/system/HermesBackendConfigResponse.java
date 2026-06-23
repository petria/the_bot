package org.freakz.common.model.engine.system;

import java.util.List;

public record HermesBackendConfigResponse(
    List<HermesProfile> profiles,
    HermesFallbackSettingsResponse fallback,
    HermesGlobalOverrideSettings globalOverride) {

  public HermesBackendConfigResponse(List<HermesProfile> profiles) {
    this(profiles, null, null);
  }

  public HermesBackendConfigResponse(
      List<HermesProfile> profiles,
      HermesFallbackSettingsResponse fallback) {
    this(profiles, fallback, null);
  }
}
