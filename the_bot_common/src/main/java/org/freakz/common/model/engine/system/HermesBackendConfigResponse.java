package org.freakz.common.model.engine.system;

import java.util.List;

public record HermesBackendConfigResponse(
    List<HermesProfile> profiles,
    HermesFallbackSettingsResponse fallback) {

  public HermesBackendConfigResponse(List<HermesProfile> profiles) {
    this(profiles, null);
  }
}
