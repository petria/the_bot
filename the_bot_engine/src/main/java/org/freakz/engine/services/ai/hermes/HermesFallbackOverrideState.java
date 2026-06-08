package org.freakz.engine.services.ai.hermes;

import java.util.concurrent.atomic.AtomicReference;

import org.freakz.common.model.engine.system.HermesFallbackSettingsResponse;
import org.springframework.stereotype.Component;

@Component
public class HermesFallbackOverrideState {

  private final AtomicReference<Boolean> lastKnownEnabled = new AtomicReference<>(false);

  public void rememberRequestedEnabled(Boolean enabled) {
    if (enabled != null) {
      lastKnownEnabled.set(enabled);
    }
  }

  public HermesFallbackSettingsResponse apply(HermesFallbackSettingsResponse response) {
    if (response == null) {
      return null;
    }
    Boolean enabled = response.enabled();
    if (enabled == null) {
      enabled = lastKnownEnabled.get();
    } else {
      lastKnownEnabled.set(enabled);
    }
    return new HermesFallbackSettingsResponse(
        enabled,
        response.baseUrl(),
        response.model(),
        response.profiles());
  }
}
