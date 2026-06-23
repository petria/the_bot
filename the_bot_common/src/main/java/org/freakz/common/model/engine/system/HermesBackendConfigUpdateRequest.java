package org.freakz.common.model.engine.system;

import java.util.List;

public record HermesBackendConfigUpdateRequest(
    List<HermesProfileUpdate> profiles,
    HermesFallbackUpdateRequest fallback) {

  public HermesBackendConfigUpdateRequest(List<HermesProfileUpdate> profiles) {
    this(profiles, null);
  }
}
