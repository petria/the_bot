package org.freakz.common.model.engine.system;

import java.util.List;

public record HermesBackendConfigUpdateRequest(
    List<HermesProfileUpdate> profiles,
    HermesFallbackUpdateRequest fallback,
    HermesGlobalOverrideUpdate globalOverride,
    String requestedBy) {

  public HermesBackendConfigUpdateRequest(List<HermesProfileUpdate> profiles) {
    this(profiles, null, null, null);
  }

  public HermesBackendConfigUpdateRequest(
      List<HermesProfileUpdate> profiles,
      HermesFallbackUpdateRequest fallback) {
    this(profiles, fallback, null, null);
  }

  public HermesBackendConfigUpdateRequest withRequestedBy(String username) {
    return new HermesBackendConfigUpdateRequest(profiles, fallback, globalOverride, username);
  }
}
