package org.freakz.common.model.engine.system;

public record HermesFallbackUpdateRequest(
    String provider,
    String baseUrl,
    String model,
    Boolean enabled,
    Integer contextWindow,
    String apiKey,
    Boolean clearApiKey) {

  public HermesFallbackUpdateRequest(String baseUrl, String model, Boolean enabled) {
    this("ollama", baseUrl, model, enabled, null, null, false);
  }

  public HermesFallbackUpdateRequest(
      String baseUrl,
      String model,
      Boolean enabled,
      Integer contextWindow) {
    this("ollama", baseUrl, model, enabled, contextWindow, null, false);
  }
}
