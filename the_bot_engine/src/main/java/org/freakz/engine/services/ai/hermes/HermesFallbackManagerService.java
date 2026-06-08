package org.freakz.engine.services.ai.hermes;

import org.freakz.common.model.engine.system.HermesFallbackModelsResponse;
import org.freakz.common.model.engine.system.HermesFallbackSettingsResponse;
import org.freakz.common.model.engine.system.HermesFallbackUpdateRequest;
import org.freakz.common.spring.rest.RestHermesManagerClient;
import org.springframework.stereotype.Service;

@Service
public class HermesFallbackManagerService {

  private final RestHermesManagerClient client;

  public HermesFallbackManagerService(RestHermesManagerClient client) {
    this.client = client;
  }

  public HermesFallbackSettingsResponse getSettings() {
    return requireBody(client.getFallback().getBody(), "load Hermes fallback settings");
  }

  public HermesFallbackModelsResponse getModels(String baseUrl) {
    return requireBody(client.getModels(baseUrl).getBody(), "load Ollama models");
  }

  public HermesFallbackSettingsResponse update(HermesFallbackUpdateRequest request) {
    return requireBody(client.updateFallback(request).getBody(), "update Hermes fallback settings");
  }

  private <T> T requireBody(T body, String action) {
    if (body == null) {
      throw new IllegalStateException("Could not " + action);
    }
    return body;
  }
}
