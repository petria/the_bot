package org.freakz.engine.services.ai.hermes;

public record HermesSettings(String baseUrl, String apiKey, String model, int timeoutSeconds, String apiMode) {

  public boolean configured() {
    return baseUrl != null && !baseUrl.isBlank();
  }

  public boolean useResponsesApi() {
    return apiMode == null || apiMode.isBlank() || "responses".equalsIgnoreCase(apiMode);
  }
}
