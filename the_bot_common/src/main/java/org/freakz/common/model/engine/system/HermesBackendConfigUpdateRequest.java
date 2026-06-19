package org.freakz.common.model.engine.system;

import java.util.List;

public record HermesBackendConfigUpdateRequest(
    List<HermesProfile> profiles,
    HermesFallbackUpdateRequest fallback) {

  public HermesBackendConfigUpdateRequest(List<HermesProfile> profiles) {
    this(profiles, null);
  }
}
