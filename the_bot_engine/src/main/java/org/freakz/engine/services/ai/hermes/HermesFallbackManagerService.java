package org.freakz.engine.services.ai.hermes;

import org.freakz.common.model.engine.system.HermesFallbackModelsResponse;
import org.freakz.common.model.engine.system.HermesFallbackSettingsResponse;
import org.freakz.common.model.engine.system.HermesFallbackUpdateRequest;
import org.freakz.common.spring.rest.RestHermesManagerClient;
import org.springframework.stereotype.Service;

@Service
public class HermesFallbackManagerService {

  private final RestHermesManagerClient client;
  private final HermesFallbackOverrideState overrideState;

  public HermesFallbackManagerService(RestHermesManagerClient client, HermesFallbackOverrideState overrideState) {
    this.client = client;
    this.overrideState = overrideState;
  }

  public HermesFallbackSettingsResponse getSettings() {
    return overrideState.apply(requireBody(client.getFallback().getBody(), "load Hermes fallback settings"));
  }

  public HermesFallbackModelsResponse getModels(String baseUrl) {
    return requireBody(client.getModels(baseUrl).getBody(), "load Ollama models");
  }

  public HermesFallbackSettingsResponse update(HermesFallbackUpdateRequest request) {
    overrideState.rememberRequestedEnabled(request == null ? null : request.enabled());
    return overrideState.apply(requireBody(client.updateFallback(request).getBody(), "update Hermes fallback settings"));
  }

  private <T> T requireBody(T body, String action) {
    if (body == null) {
      throw new IllegalStateException("Could not " + action);
    }
    return body;
  }
}
