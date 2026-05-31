package org.freakz.engine.services.ai.hermes;

record HermesSettings(String baseUrl, String apiKey, String model, int timeoutSeconds, String apiMode) {

  boolean configured() {
    return baseUrl != null && !baseUrl.isBlank();
  }

  boolean useResponsesApi() {
    return apiMode == null || apiMode.isBlank() || "responses".equalsIgnoreCase(apiMode);
  }
}
