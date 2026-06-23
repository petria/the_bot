package org.freakz.engine.services.ai.hermes;

import org.freakz.common.model.engine.system.HermesFallbackModelsResponse;
import org.freakz.common.model.engine.system.HermesFallbackSettingsResponse;
import org.freakz.common.model.engine.system.HermesFallbackUpdateRequest;
import org.freakz.common.model.engine.system.HermesModelDiscoveryRequest;
import org.freakz.common.model.engine.system.HermesBackendConfigResponse;
import org.freakz.common.model.engine.system.HermesBackendConfigUpdateRequest;
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

  public HermesFallbackModelsResponse getModels(HermesModelDiscoveryRequest request) {
    return requireBody(client.getModels(request).getBody(), "load local LLM models");
  }

  public HermesFallbackSettingsResponse update(HermesFallbackUpdateRequest request) {
    return requireBody(client.updateFallback(request).getBody(), "update Hermes fallback settings");
  }

  public HermesBackendConfigResponse getBackendConfig() {
    return requireBody(client.getBackendConfig().getBody(), "load Hermes backend configuration");
  }

  public HermesBackendConfigResponse updateBackendConfig(HermesBackendConfigUpdateRequest request) {
    return requireBody(client.updateBackendConfig(request).getBody(), "update Hermes backend configuration");
  }

  private <T> T requireBody(T body, String action) {
    if (body == null) {
      throw new IllegalStateException("Could not " + action);
    }
    return body;
  }
}
