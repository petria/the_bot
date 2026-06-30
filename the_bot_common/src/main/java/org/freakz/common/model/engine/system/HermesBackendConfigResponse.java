package org.freakz.common.model.engine.system;

import java.util.List;

public record HermesBackendConfigResponse(
    String systemMode,
    List<HermesBackend> backends,
    List<HermesRoute> routes,
    List<HermesProfile> profiles,
    HermesFallbackSettingsResponse fallback,
    HermesGlobalOverrideSettings globalOverride) {

  public HermesBackendConfigResponse(List<HermesProfile> profiles) {
    this("enabled", List.of(), List.of(), profiles, null, null);
  }

  public HermesBackendConfigResponse(
      List<HermesProfile> profiles,
      HermesFallbackSettingsResponse fallback) {
    this("enabled", List.of(), List.of(), profiles, fallback, null);
  }

  public HermesBackendConfigResponse(
      String systemMode,
      List<HermesBackend> backends,
      List<HermesRoute> routes) {
    this(systemMode, backends, routes, List.of(), null, null);
  }
}
