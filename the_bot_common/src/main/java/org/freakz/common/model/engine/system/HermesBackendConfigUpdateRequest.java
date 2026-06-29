package org.freakz.common.model.engine.system;

import java.util.List;

public record HermesBackendConfigUpdateRequest(
    String systemMode,
    List<HermesBackendUpdate> backends,
    List<HermesRouteUpdate> routes,
    List<HermesProfileUpdate> profiles,
    HermesFallbackUpdateRequest fallback,
    HermesGlobalOverrideUpdate globalOverride,
    String requestedBy) {

  public HermesBackendConfigUpdateRequest(List<HermesProfileUpdate> profiles) {
    this(null, null, null, profiles, null, null, null);
  }

  public HermesBackendConfigUpdateRequest(
      List<HermesProfileUpdate> profiles,
      HermesFallbackUpdateRequest fallback) {
    this(null, null, null, profiles, fallback, null, null);
  }

  public HermesBackendConfigUpdateRequest(
      String systemMode,
      List<HermesBackendUpdate> backends,
      List<HermesRouteUpdate> routes) {
    this(systemMode, backends, routes, null, null, null, null);
  }

  public HermesBackendConfigUpdateRequest(
      List<HermesProfileUpdate> profiles,
      HermesFallbackUpdateRequest fallback,
      HermesGlobalOverrideUpdate globalOverride,
      String requestedBy) {
    this(null, null, null, profiles, fallback, globalOverride, requestedBy);
  }

  public HermesBackendConfigUpdateRequest withRequestedBy(String username) {
    return new HermesBackendConfigUpdateRequest(systemMode, backends, routes, profiles, fallback, globalOverride, username);
  }
}
